package com.gpsmapcamera.app.core.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class CameraController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var preview: Preview? = null
    private var activeRecording: Recording? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    var lensFacing: Int = CameraSelector.LENS_FACING_BACK
        private set
    var flashMode: Int = ImageCapture.FLASH_MODE_AUTO
        private set
    var isNightMode: Boolean = false
        private set

    suspend fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) = suspendCancellableCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val extFuture = ExtensionsManager.getInstanceAsync(context, provider)
                extFuture.addListener({
                    try {
                        extensionsManager = extFuture.get()
                    } catch (e: Exception) {
                        extensionsManager = null
                    }
                    bindCameraUseCases(lifecycleOwner, previewView)
                    if (continuation.isActive) continuation.resume(Unit)
                }, ContextCompat.getMainExecutor(context))

            } catch (e: Exception) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val provider = cameraProvider ?: return
        val baseSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // 1. Configure Preview with Face Priority & Auto Exposure
        val previewBuilder = Preview.Builder()
        try {
            val camera2Preview = Camera2Interop.Extender(previewBuilder)
            camera2Preview.setCaptureRequestOption(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_USE_SCENE_MODE
            )
            camera2Preview.setCaptureRequestOption(
                CaptureRequest.CONTROL_SCENE_MODE,
                CameraMetadata.CONTROL_SCENE_MODE_FACE_PRIORITY
            )
            camera2Preview.setCaptureRequestOption(
                CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE
            )
        } catch (e: Exception) {
            // Graceful fallback for non-Camera2 compliant HALs
        }

        preview = previewBuilder.build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        // 2. Configure ImageCapture with Face Priority, High Quality & Flash Mode
        val imageCaptureBuilder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)

        try {
            val camera2Capture = Camera2Interop.Extender(imageCaptureBuilder)
            camera2Capture.setCaptureRequestOption(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_USE_SCENE_MODE
            )
            camera2Capture.setCaptureRequestOption(
                CaptureRequest.CONTROL_SCENE_MODE,
                CameraMetadata.CONTROL_SCENE_MODE_FACE_PRIORITY
            )
            camera2Capture.setCaptureRequestOption(
                CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE
            )
        } catch (e: Exception) {
            // Fallback
        }

        imageCapture = imageCaptureBuilder.build()

        // 3. Configure VideoCapture
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        // 4. Resolve Extension-enabled selector (Night Mode / HDR / Auto)
        val ext = extensionsManager
        val extensionSelector = if (ext != null) {
            if (isNightMode && ext.isExtensionAvailable(baseSelector, ExtensionMode.NIGHT)) {
                ext.getExtensionEnabledCameraSelector(baseSelector, ExtensionMode.NIGHT)
            } else if (ext.isExtensionAvailable(baseSelector, ExtensionMode.HDR)) {
                ext.getExtensionEnabledCameraSelector(baseSelector, ExtensionMode.HDR)
            } else if (ext.isExtensionAvailable(baseSelector, ExtensionMode.AUTO)) {
                ext.getExtensionEnabledCameraSelector(baseSelector, ExtensionMode.AUTO)
            } else {
                baseSelector
            }
        } else {
            baseSelector
        }

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                extensionSelector,
                preview,
                imageCapture,
                videoCapture
            )
        } catch (e: Exception) {
            // If binding with extension fails (e.g. video capture restriction on limited devices), fallback to baseSelector
            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    baseSelector,
                    preview,
                    imageCapture,
                    videoCapture
                )
            } catch (fallbackEx: Exception) {
                // If 3 concurrent streams exceed hardware capabilities, guarantee preview + photo capture works
                try {
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        baseSelector,
                        preview,
                        imageCapture
                    )
                } catch (lastEx: Exception) {
                    lastEx.printStackTrace()
                }
            }
        }
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCameraUseCases(lifecycleOwner, previewView)
    }

    fun setFlashMode(mode: Int) {
        flashMode = mode
        imageCapture?.flashMode = mode
    }

    fun toggleTorch(enable: Boolean) {
        camera?.cameraControl?.enableTorch(enable)
    }

    fun setNightMode(enable: Boolean, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        isNightMode = enable
        bindCameraUseCases(lifecycleOwner, previewView)
    }

    fun setLinearZoom(zoom: Float) {
        camera?.cameraControl?.setLinearZoom(zoom.coerceIn(0f, 1f))
    }

    /**
     * 3A Tap-to-Focus and Tap-to-Expose:
     * Focuses and recalculates Auto-Exposure & Auto-White-Balance for the tapped subject (e.g. human face).
     */
    fun focusAndExposeOnPoint(x: Float, y: Float, previewWidth: Float, previewHeight: Float) {
        val factory = SurfaceOrientedMeteringPointFactory(previewWidth, previewHeight)
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        )
        .setAutoCancelDuration(3, TimeUnit.SECONDS)
        .build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    fun setExposureCompensation(index: Int) {
        camera?.cameraControl?.setExposureCompensationIndex(index)
    }

    fun getExposureCompensationRange(): Range<Int>? {
        return camera?.cameraInfo?.exposureState?.exposureCompensationRange
    }

    suspend fun capturePhoto(): Bitmap = suspendCancellableCoroutine { continuation ->
        val capture = imageCapture ?: run {
            continuation.resumeWithException(IllegalStateException("ImageCapture not bound"))
            return@suspendCancellableCoroutine
        }

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val rotationDegrees = image.imageInfo.rotationDegrees
                        val bitmap = imageProxyToBitmap(image, rotationDegrees, lensFacing == CameraSelector.LENS_FACING_FRONT)
                        image.close()
                        continuation.resume(bitmap)
                    } catch (e: Exception) {
                        image.close()
                        continuation.resumeWithException(e)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun startVideoRecording(
        outputFile: File,
        enableAudio: Boolean = true,
        onEvent: (VideoRecordEvent) -> Unit
    ) {
        val capture = videoCapture ?: return
        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        var pending = capture.output.prepareRecording(context, outputOptions)
        if (enableAudio) {
            pending = pending.withAudioEnabled()
        }

        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            onEvent(event)
        }
    }

    fun pauseRecording() {
        activeRecording?.pause()
    }

    fun resumeRecording() {
        activeRecording?.resume()
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    private fun imageProxyToBitmap(image: ImageProxy, rotationDegrees: Int, isFrontCamera: Boolean): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
            if (isFrontCamera) {
                // Mirror front camera photo horizontally
                postScale(-1f, 1f)
            }
        }

        return Bitmap.createBitmap(
            originalBitmap,
            0,
            0,
            originalBitmap.width,
            originalBitmap.height,
            matrix,
            true
        )
    }
}
