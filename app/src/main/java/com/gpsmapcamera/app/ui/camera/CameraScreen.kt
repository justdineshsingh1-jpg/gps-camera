package com.gpsmapcamera.app.ui.camera

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Exposure
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Satellite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.gpsmapcamera.app.core.update.UpdateState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gpsmapcamera.app.core.overlay.MapTileType
import com.gpsmapcamera.app.ui.components.StatusIndicators
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val previewStamp by viewModel.previewStamp.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var currentZoom by remember { mutableFloatStateOf(0f) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    val focusRingScale = remember { Animatable(1.4f) }

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            focusRingScale.snapTo(1.4f)
            focusRingScale.animateTo(1.0f, animationSpec = tween(200))
            delay(2000)
            focusPoint = null
        }
    }

    // Newest saved photo/video for the round gallery thumbnail (app start and back from Gallery)
    LaunchedEffect(Unit) {
        viewModel.refreshLastMedia()
    }

    var switchTurns by remember { mutableFloatStateOf(0f) }
    val switchRotation by animateFloatAsState(
        targetValue = switchTurns * 180f,
        animationSpec = tween(350),
        label = "switchCameraRotation"
    )

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { previewSize = it.size }
    ) {

        // 1. CameraX PreviewView with Gestures: 3A Tap-to-Focus/Expose & Pinch-to-Zoom
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                    viewModel.initializeCamera(lifecycleOwner, this)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        focusPoint = tapOffset
                        val w = if (previewSize.width > 0) previewSize.width.toFloat() else 1080f
                        val h = if (previewSize.height > 0) previewSize.height.toFloat() else 1920f
                        viewModel.onPreviewTapped(tapOffset.x, tapOffset.y, w, h)
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val newRatio = (uiState.currentZoomRatio * zoom).coerceIn(1.0f, 6.0f)
                        viewModel.setZoomRatio(newRatio)
                    }
                }
        )

        // 2. Animated Tap-to-Focus & Auto-Exposure Indicator Ring
        focusPoint?.let { pt ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (pt.x - 30.dp.toPx()).roundToInt(),
                            (pt.y - 30.dp.toPx()).roundToInt()
                        )
                    }
                    .size(60.dp * focusRingScale.value)
                    .border(2.dp, Color(0xFFFFD54F), CircleShape)
            )
        }

        // 3. Front Camera Display Softbox / Screen Flash Overlay (for Night Selfies)
        AnimatedVisibility(
            visible = uiState.isScreenFlashing,
            enter = fadeIn(animationSpec = tween(100)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFF8E7))
            )
        }

        // 4. Top Bar: Status Indicators & Quick Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            StatusIndicators(
                networkStatus = uiState.networkStatus,
                connectivityMode = uiState.connectivityMode,
                gpsData = uiState.gpsData,
                onToggleConnectivityMode = { viewModel.toggleConnectivityMode() }
            )

            // Video Recording Banner
            AnimatedVisibility(visible = uiState.isRecording) {
                val mins = uiState.recordingDurationSeconds / 60
                val secs = uiState.recordingDurationSeconds % 60
                val timeStr = String.format(Locale.US, "%02d:%02d", mins, secs)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xCCB71C1C))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isRecordingPaused) Color.Yellow else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isRecordingPaused) "PAUSED $timeStr" else "REC $timeStr",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Toolbar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Night Mode Toggle
                ControlChip(
                    icon = Icons.Rounded.NightsStay,
                    contentDescription = "Night Mode",
                    tint = if (uiState.isNightMode) CameraColors.Moon else Color(0xFFC7D2FE),
                    active = uiState.isNightMode,
                    activeBrush = Brush.linearGradient(listOf(CameraColors.Indigo, Color(0xFF7C3AED))),
                    onClick = { previewView?.let { viewModel.toggleNightMode(lifecycleOwner, it) } }
                )

                Spacer(modifier = Modifier.size(8.dp))

                // Flash Mode cycle (Auto / On / Torch / Off)
                val (flashIcon, flashTint) = when (uiState.flashMode) {
                    AppFlashMode.AUTO -> Icons.Rounded.FlashAuto to CameraColors.Amber
                    AppFlashMode.ON -> Icons.Rounded.FlashOn to Color(0xFFFFEB3B)
                    AppFlashMode.TORCH -> Icons.Rounded.FlashlightOn to CameraColors.Orange
                    AppFlashMode.OFF -> Icons.Rounded.FlashOff to Color.White
                }
                ControlChip(
                    icon = flashIcon,
                    contentDescription = "Flash: ${uiState.flashMode.name.lowercase()}",
                    tint = flashTint,
                    onClick = { viewModel.cycleFlashMode() }
                )

                Spacer(modifier = Modifier.size(8.dp))

                // Exposure Compensation (+1EV, +2EV, -1EV, 0)
                ControlPill(
                    icon = Icons.Rounded.Exposure,
                    label = when {
                        uiState.exposureCompensation > 0 -> "+${uiState.exposureCompensation}"
                        uiState.exposureCompensation < 0 -> "${uiState.exposureCompensation}"
                        else -> "EV"
                    },
                    tint = Color.White,
                    active = uiState.exposureCompensation != 0,
                    activeColor = Color(0xFF0EA5E9),
                    onClick = {
                        val next = when (uiState.exposureCompensation) {
                            0 -> 1
                            1 -> 2
                            2 -> -1
                            else -> 0
                        }
                        viewModel.adjustExposure(next - uiState.exposureCompensation)
                    }
                )

                Spacer(modifier = Modifier.size(8.dp))

                // QR Code toggle
                ControlChip(
                    icon = Icons.Rounded.QrCode2,
                    contentDescription = if (uiState.showQr) "Hide QR" else "Show QR",
                    tint = if (uiState.showQr) CameraColors.Emerald else Color(0xFF9CA3AF),
                    onClick = { viewModel.toggleQr() }
                )

                Spacer(modifier = Modifier.size(8.dp))

                // Map Type (STREET / SATELLITE)
                ControlPill(
                    icon = when (uiState.mapTileType) {
                        MapTileType.GOOGLE_STREET -> Icons.Rounded.Map
                        MapTileType.GOOGLE_HYBRID -> Icons.Rounded.Satellite
                    },
                    label = when (uiState.mapTileType) {
                        MapTileType.GOOGLE_STREET -> "STREET"
                        MapTileType.GOOGLE_HYBRID -> "SATELLITE"
                    },
                    tint = when (uiState.mapTileType) {
                        MapTileType.GOOGLE_STREET -> CameraColors.Sky
                        MapTileType.GOOGLE_HYBRID -> CameraColors.Emerald
                    },
                    onClick = { viewModel.cycleMapTileType() }
                )
            }
        }

        // 5. Bottom Layer: Live Stamp (WYSIWYG) + 1x/2x/3x Zoom + Mode Tabs + Shutter Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(vertical = 8.dp)
        ) {
            // Live stamp: the exact bitmap PhotoStampRenderer burns into the photo, drawn at the
            // preview's full width, so preview and saved stamp match in layout, size and elements.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { viewModel.setPreviewStampWidth(it.width) }
            ) {
                previewStamp?.let { stamp ->
                    val image = remember(stamp) { stamp.asImageBitmap() }
                    Image(
                        bitmap = image,
                        contentDescription = "Live GPS stamp preview",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                // 1x, 2x, 3x Quick Zoom Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x99000000))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1.0f to "1x", 2.0f to "2x", 3.0f to "3x").forEach { (ratio, label) ->
                            val isSelected = kotlin.math.abs(uiState.currentZoomRatio - ratio) < 0.25f
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFFFFD54F) else Color.Transparent)
                                    .clickable { viewModel.setZoomRatio(ratio) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Photo / Video Mode Selector Tabs
                if (!uiState.isRecording) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "PHOTO",
                            color = if (uiState.cameraMode == CameraMode.PHOTO) Color(0xFFFFCC00) else Color(0x99FFFFFF),
                            fontWeight = if (uiState.cameraMode == CameraMode.PHOTO) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { viewModel.setCameraMode(CameraMode.PHOTO) }
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "VIDEO",
                            color = if (uiState.cameraMode == CameraMode.VIDEO) Color(0xFFFFCC00) else Color(0x99FFFFFF),
                            fontWeight = if (uiState.cameraMode == CameraMode.VIDEO) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { viewModel.setCameraMode(CameraMode.VIDEO) }
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons Row (Gallery/Pause, Shutter, Switch Camera)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left slot: Pause/Resume while recording, else round thumbnail of the last capture
                    if (uiState.isRecording) {
                        ControlChip(
                            icon = if (uiState.isRecordingPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = if (uiState.isRecordingPaused) "Resume recording" else "Pause recording",
                            tint = if (uiState.isRecordingPaused) CameraColors.Emerald else CameraColors.Amber,
                            onClick = { viewModel.pauseResumeVideoRecording() },
                            size = 56,
                            iconSize = 28
                        )
                    } else {
                        GalleryThumbnailButton(
                            uri = uiState.lastCapturedUri,
                            isVideo = uiState.lastCapturedIsVideo,
                            onClick = onOpenGallery
                        )
                    }

                    ShutterButton(
                        mode = uiState.cameraMode,
                        isRecording = uiState.isRecording,
                        isCapturing = uiState.isCapturing,
                        onClick = {
                            if (uiState.cameraMode == CameraMode.PHOTO) {
                                viewModel.capturePhoto(previewView?.bitmap)
                            } else {
                                viewModel.toggleVideoRecording()
                            }
                        }
                    )

                    // Switch Camera Button (flips with a half-turn)
                    ControlChip(
                        icon = Icons.Rounded.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        enabled = !uiState.isRecording,
                        size = 56,
                        iconSize = 28,
                        iconRotation = switchRotation,
                        onClick = {
                            previewView?.let {
                                switchTurns += 1f
                                viewModel.switchCamera(lifecycleOwner, it)
                            }
                        }
                    )
                }
            }
        }

        // Snackbar for feedback & alerts
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 150.dp)
        )

        // Mock Location / GPS Spoofing Alert Prompt (Image 2)
        if (uiState.isMockLocationDetected) {
            MockLocationDialog(
                onCloseApp = {
                    (context as? Activity)?.finishAffinity()
                },
                onGoToSettings = {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = Intent(Settings.ACTION_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }
            )
        }

        // Over-The-Air (OTA) In-App Auto-Updater Dialogs
        when (val update = uiState.updateState) {
            is UpdateState.UpdateAvailable -> {
                AppUpdateAvailableDialog(
                    version = update.latestVersion,
                    apkSize = update.apkSize,
                    onUpdate = { viewModel.startAppUpdate(update.downloadUrl) },
                    onDismiss = { viewModel.dismissAppUpdate() }
                )
            }
            is UpdateState.Downloading -> {
                AppUpdateDownloadingDialog(
                    progress = update.progressPercent,
                    downloadedBytes = update.downloadedBytes,
                    totalBytes = update.totalBytes
                )
            }
            is UpdateState.PermissionNeeded -> {
                AppUpdatePermissionDialog(
                    onGrant = { viewModel.requestInstallPermission() },
                    onRetryInstall = { viewModel.retryInstallApk(update.apkFile) },
                    onDismiss = { viewModel.dismissAppUpdate() }
                )
            }
            is UpdateState.NoUpdateAvailable -> {
                LaunchedEffect(Unit) {
                    Toast.makeText(context, "GPS Map Camera is up to date!", Toast.LENGTH_SHORT).show()
                    viewModel.dismissAppUpdate()
                }
            }
            is UpdateState.Error -> {
                LaunchedEffect(update.message) {
                    Toast.makeText(context, update.message, Toast.LENGTH_LONG).show()
                    viewModel.dismissAppUpdate()
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun MockLocationDialog(
    onCloseApp: () -> Unit,
    onGoToSettings: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF151D2A),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Can't Detect Your Real Location",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your device is using a mock location.\nThis app needs your real location to work properly. Let's fix it together.\n\nOpen Setting -> Developer Options -> Turn Off",
                    color = Color(0xFFCDD5E0),
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCloseApp) {
                        Text(
                            text = "CLOSE APP",
                            color = Color(0xFF90CAF9),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onGoToSettings) {
                        Text(
                            text = "GO TO SETTINGS",
                            color = Color(0xFF90CAF9),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUpdateAvailableDialog(
    version: String,
    apkSize: Long,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF151D2A),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Update Available",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                val sizeText = if (apkSize > 0) " (${String.format(java.util.Locale.US, "%.1f MB", apkSize / (1024f * 1024f))})" else ""
                Text(
                    text = "A new version of GPS Map Camera (v$version)$sizeText is available over-the-air with the latest fixes and improvements.\n\nWould you like to download and update now?",
                    color = Color(0xFFCDD5E0),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "LATER",
                            color = Color(0xFF9E9E9E),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onUpdate,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text(
                            text = "UPDATE NOW",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUpdateDownloadingDialog(
    progress: Int,
    downloadedBytes: Long,
    totalBytes: Long
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF151D2A),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Downloading Update...",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { (progress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Color(0xFF3B82F6),
                    trackColor = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(12.dp))
                val downloadedMb = String.format(java.util.Locale.US, "%.1f", downloadedBytes / (1024f * 1024f))
                val totalMb = if (totalBytes > 0) String.format(java.util.Locale.US, "%.1f", totalBytes / (1024f * 1024f)) else "--"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$progress%",
                        color = Color(0xFF60A5FA),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "$downloadedMb MB / $totalMb MB",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Please keep the app open. The installer will prompt automatically once finished.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun AppUpdatePermissionDialog(
    onGrant: () -> Unit,
    onRetryInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF151D2A),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Permission Required",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "To install updates over-the-air, Android requires you to enable 'Allow from this source' for GPS Map Camera.\n\n1. Tap 'Settings' below.\n2. Turn on the switch.\n3. Return here and tap 'Install Now'.",
                    color = Color(0xFFCDD5E0),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "CANCEL",
                            color = Color(0xFF9E9E9E),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(onClick = onGrant) {
                        Text(
                            text = "SETTINGS",
                            color = Color(0xFF60A5FA),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onRetryInstall,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text(
                            text = "INSTALL NOW",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
