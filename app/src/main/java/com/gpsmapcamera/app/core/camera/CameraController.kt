package com.gpsmapcamera.app.core.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.RecordEvent
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class CameraController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var preview: Preview? = null
    private var activeRecording: Recording? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    var lensFacing: Int = CameraSelector.LENS_FACING_BACK
        private set
    var flashMode: Int = ImageCapture.FLASH_MODE_OFF
        private set

    suspend fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) = suspendCancellableCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView)
                continuation.resume(Unit)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val provider = cameraProvider ?: return
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
            .build()

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture
            )
        } catch (e: Exception) {
            e.printStackTrace()
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

    fun setLinearZoom(zoom: Float) {
        camera?.cameraControl?.setLinearZoom(zoom.coerceIn(0f, 1f))
    }

    fun focusOnPoint(x: Float, y: Float, previewWidth: Float, previewHeight: Float) {
        val factory = SurfaceOrientedMeteringPointFactory(previewWidth, previewHeight)
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).build()
        camera?.cameraControl?.startFocusAndMetering(action)
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
