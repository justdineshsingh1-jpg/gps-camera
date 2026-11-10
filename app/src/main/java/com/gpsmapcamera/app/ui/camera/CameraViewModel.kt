package com.gpsmapcamera.app.ui.camera

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.gpsmapcamera.app.core.camera.CameraController
import com.gpsmapcamera.app.core.connectivity.ConnectivityMode
import com.gpsmapcamera.app.core.connectivity.NetworkObserver
import com.gpsmapcamera.app.core.connectivity.NetworkStatus
import com.gpsmapcamera.app.core.data.MediaStorageManager
import com.gpsmapcamera.app.core.geocoder.AddressRepository
import com.gpsmapcamera.app.core.location.GpsData
import com.gpsmapcamera.app.core.location.LocationTracker
import com.gpsmapcamera.app.core.overlay.MapTileManager
import com.gpsmapcamera.app.core.overlay.PhotoStampRenderer
import com.gpsmapcamera.app.core.video.VideoStampTransformer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.math.abs

enum class CameraMode {
    PHOTO,
    VIDEO
}

enum class AppFlashMode {
    OFF,
    AUTO,
    ON,
    TORCH
}

data class CameraUiState(
    val cameraMode: CameraMode = CameraMode.PHOTO,
    val gpsData: GpsData = GpsData(),
    val networkStatus: NetworkStatus = NetworkStatus.OFFLINE,
    val connectivityMode: ConnectivityMode = ConnectivityMode.AUTO,
    val mapThumbnail: Bitmap? = null,
    val isCapturing: Boolean = false,
    val isRecording: Boolean = false,
    val isRecordingPaused: Boolean = false,
    val recordingDurationSeconds: Int = 0,
    val lastCapturedUri: Uri? = null,
    val showQr: Boolean = true,
    val useDms: Boolean = false,
    val flashMode: AppFlashMode = AppFlashMode.AUTO,
    val isNightMode: Boolean = false,
    val isScreenFlashing: Boolean = false,
    val exposureCompensation: Int = 0,
    val errorMessage: String? = null
) {
    val isTorchOn: Boolean
        get() = flashMode == AppFlashMode.TORCH

    val isEffectiveOnline: Boolean
        get() = connectivityMode == ConnectivityMode.AUTO && networkStatus == NetworkStatus.ONLINE
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    application: Application,
    val cameraController: CameraController,
    private val locationTracker: LocationTracker,
    private val networkObserver: NetworkObserver,
    private val addressRepository: AddressRepository,
    private val mapTileManager: MapTileManager,
    private val mediaStorageManager: MediaStorageManager,
    private val videoStampTransformer: VideoStampTransformer
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var lastGeocodedLat = 0.0
    private var lastGeocodedLon = 0.0
    private var currentRecordingFile: File? = null
    private var timerJob: Job? = null

    init {
        // Collect live GPS & Compass coordinates (1s interval)
        viewModelScope.launch {
            locationTracker.getLocationUpdates(1000L).collect { gps ->
                _uiState.update { it.copy(gpsData = it.gpsData.copy(
                    latitude = gps.latitude,
                    longitude = gps.longitude,
                    altitudeMeters = gps.altitudeMeters,
                    accuracyMeters = gps.accuracyMeters,
                    speedMps = gps.speedMps,
                    bearingDegrees = gps.bearingDegrees,
                    compassDegrees = gps.compassDegrees,
                    compassDirection = gps.compassDirection,
                    plusCode = gps.plusCode,
                    timestamp = gps.timestamp,
                    isLocked = gps.isLocked
                )) }

                checkAndTriggerGeocoding(gps.latitude, gps.longitude)
            }
        }

        // Collect real-time network connectivity changes
        viewModelScope.launch {
            networkObserver.observe().collect { status ->
                _uiState.update { it.copy(networkStatus = status) }
                val current = _uiState.value
                if (status == NetworkStatus.ONLINE && current.gpsData.latitude != 0.0) {
                    checkAndTriggerGeocoding(current.gpsData.latitude, current.gpsData.longitude, force = true)
                }
            }
        }
    }

    private fun checkAndTriggerGeocoding(lat: Double, lon: Double, force: Boolean = false) {
        if (lat == 0.0 && lon == 0.0) return

        val moved = abs(lat - lastGeocodedLat) > 0.0002 || abs(lon - lastGeocodedLon) > 0.0002
        if (moved || force || _uiState.value.gpsData.addressLine == null) {
            lastGeocodedLat = lat
            lastGeocodedLon = lon

            viewModelScope.launch {
                val isOnline = _uiState.value.isEffectiveOnline
                val resolved = addressRepository.resolveAddress(lat, lon, isOnline)
                _uiState.update {
                    it.copy(
                        gpsData = it.gpsData.copy(
                            addressLine = resolved.formattedAddress,
                            city = resolved.city,
                            state = resolved.state,
                            country = resolved.country
                        )
                    )
                }

                if (isOnline) {
                    val tile = mapTileManager.getMapTileThumbnail(lat, lon, true)
                    _uiState.update { it.copy(mapThumbnail = tile) }
                } else {
                    _uiState.update { it.copy(mapThumbnail = null) }
                }
            }
        }
    }

    fun setCameraMode(mode: CameraMode) {
        if (_uiState.value.isRecording) return
        _uiState.update { it.copy(cameraMode = mode) }
    }

    fun initializeCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        viewModelScope.launch {
            try {
                cameraController.initializeCamera(lifecycleOwner, previewView)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Camera initialization failed: ${e.message}") }
            }
        }
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (_uiState.value.isRecording) return
        cameraController.switchCamera(lifecycleOwner, previewView)
    }

    fun toggleConnectivityMode() {
        _uiState.update {
            val nextMode = if (it.connectivityMode == ConnectivityMode.AUTO) {
                ConnectivityMode.FORCE_OFFLINE
            } else {
                ConnectivityMode.AUTO
            }
            it.copy(
                connectivityMode = nextMode,
                mapThumbnail = if (nextMode == ConnectivityMode.FORCE_OFFLINE) null else it.mapThumbnail
            )
        }
    }

    fun toggleQr() {
        _uiState.update { it.copy(showQr = !it.showQr) }
    }

    fun toggleDms() {
        _uiState.update { it.copy(useDms = !it.useDms) }
    }

    fun cycleFlashMode() {
        val nextMode = when (_uiState.value.flashMode) {
            AppFlashMode.AUTO -> AppFlashMode.ON
            AppFlashMode.ON -> AppFlashMode.TORCH
            AppFlashMode.TORCH -> AppFlashMode.OFF
            AppFlashMode.OFF -> AppFlashMode.AUTO
        }

        when (nextMode) {
            AppFlashMode.AUTO -> {
                cameraController.setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                cameraController.toggleTorch(false)
            }
            AppFlashMode.ON -> {
                cameraController.setFlashMode(ImageCapture.FLASH_MODE_ON)
                cameraController.toggleTorch(false)
            }
            AppFlashMode.TORCH -> {
                cameraController.setFlashMode(ImageCapture.FLASH_MODE_OFF)
                cameraController.toggleTorch(true)
            }
            AppFlashMode.OFF -> {
                cameraController.setFlashMode(ImageCapture.FLASH_MODE_OFF)
                cameraController.toggleTorch(false)
            }
        }

        _uiState.update { it.copy(flashMode = nextMode) }
    }

    fun toggleTorch() {
        cycleFlashMode()
    }

    fun toggleNightMode(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val next = !_uiState.value.isNightMode
        cameraController.setNightMode(next, lifecycleOwner, previewView)
        _uiState.update { it.copy(isNightMode = next) }
    }

    fun adjustExposure(delta: Int) {
        val newIndex = (_uiState.value.exposureCompensation + delta).coerceIn(-4, 4)
        cameraController.setExposureCompensation(newIndex)
        _uiState.update { it.copy(exposureCompensation = newIndex) }
    }

    fun onPreviewTapped(x: Float, y: Float, width: Float, height: Float) {
        cameraController.focusAndExposeOnPoint(x, y, width, height)
    }

    fun capturePhoto() {
        val currentState = _uiState.value
        if (currentState.isCapturing || currentState.isRecording) return

        _uiState.update { it.copy(isCapturing = true, errorMessage = null) }

        viewModelScope.launch {
            val isFrontCamera = cameraController.lensFacing == CameraSelector.LENS_FACING_FRONT
            val shouldScreenFlash = isFrontCamera && (currentState.flashMode == AppFlashMode.ON || currentState.flashMode == AppFlashMode.AUTO)

            try {
                // If front camera in selfie mode with flash, trigger display softbox flash
                if (shouldScreenFlash) {
                    _uiState.update { it.copy(isScreenFlashing = true) }
                    delay(250) // Allow front sensor auto-exposure to adapt to bright screen
                }

                val rawBitmap = cameraController.capturePhoto()

                val stampedBitmap = PhotoStampRenderer.createStampedBitmap(
                    sourceBitmap = rawBitmap,
                    gpsData = currentState.gpsData,
                    isOnline = currentState.isEffectiveOnline,
                    mapThumbnail = if (currentState.isEffectiveOnline) currentState.mapThumbnail else null,
                    showQr = currentState.showQr,
                    useDms = currentState.useDms
                )

                val saveResult = mediaStorageManager.saveStampedPhoto(stampedBitmap, currentState.gpsData)

                if (saveResult.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isCapturing = false,
                            lastCapturedUri = saveResult.getOrNull()
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isCapturing = false,
                            errorMessage = saveResult.exceptionOrNull()?.message ?: "Save failed"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCapturing = false,
                        errorMessage = "Capture failed: ${e.message}"
                    )
                }
            } finally {
                if (shouldScreenFlash) {
                    _uiState.update { it.copy(isScreenFlashing = false) }
                }
            }
        }
    }

    fun toggleVideoRecording() {
        if (_uiState.value.isRecording) {
            stopVideoRecording()
        } else {
            startVideoRecording()
        }
    }

    private fun startVideoRecording() {
        val currentState = _uiState.value
        if (currentState.isRecording || currentState.isCapturing) return

        val rawVideoFile = File(getApplication<Application>().cacheDir, "raw_video_${System.currentTimeMillis()}.mp4")
        currentRecordingFile = rawVideoFile

        _uiState.update {
            it.copy(
                isRecording = true,
                isRecordingPaused = false,
                recordingDurationSeconds = 0,
                errorMessage = null
            )
        }

        // Start duration timer
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (!_uiState.value.isRecordingPaused) {
                    _uiState.update { it.copy(recordingDurationSeconds = it.recordingDurationSeconds + 1) }
                }
            }
        }

        cameraController.startVideoRecording(rawVideoFile, enableAudio = true) { event ->
            when (event) {
                is VideoRecordEvent.Finalize -> {
                    timerJob?.cancel()
                    if (event.hasError()) {
                        _uiState.update {
                            it.copy(
                                isRecording = false,
                                isRecordingPaused = false,
                                errorMessage = "Recording error code: ${event.error}"
                            )
                        }
                    } else {
                        burnStampAndSaveVideo(rawVideoFile)
                    }
                }
            }
        }
    }

    fun pauseResumeVideoRecording() {
        val currentState = _uiState.value
        if (!currentState.isRecording) return

        if (currentState.isRecordingPaused) {
            cameraController.resumeRecording()
            _uiState.update { it.copy(isRecordingPaused = false) }
        } else {
            cameraController.pauseRecording()
            _uiState.update { it.copy(isRecordingPaused = true) }
        }
    }

    private fun stopVideoRecording() {
        timerJob?.cancel()
        cameraController.stopRecording()
    }

    private fun burnStampAndSaveVideo(rawFile: File) {
        val currentState = _uiState.value
        _uiState.update { it.copy(isCapturing = true, isRecording = false) }

        viewModelScope.launch {
            try {
                val outputVideoFile = File(getApplication<Application>().cacheDir, "stamped_${System.currentTimeMillis()}.mp4")

                val stampedFile = videoStampTransformer.burnStampIntoVideo(
                    rawVideoFile = rawFile,
                    outputVideoFile = outputVideoFile,
                    gpsData = currentState.gpsData,
                    isOnline = currentState.isEffectiveOnline,
                    mapThumbnail = if (currentState.isEffectiveOnline) currentState.mapThumbnail else null,
                    showQr = currentState.showQr,
                    useDms = currentState.useDms
                )

                val saveResult = mediaStorageManager.saveStampedVideo(stampedFile, System.currentTimeMillis())

                rawFile.delete()
                outputVideoFile.delete()

                if (saveResult.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isCapturing = false,
                            lastCapturedUri = saveResult.getOrNull()
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isCapturing = false,
                            errorMessage = saveResult.exceptionOrNull()?.message ?: "Failed to save video"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCapturing = false,
                        errorMessage = "Video stamp failed: ${e.message}"
                    )
                }
            }
        }
    }
}
