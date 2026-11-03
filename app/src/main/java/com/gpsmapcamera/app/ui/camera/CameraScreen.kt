package com.gpsmapcamera.app.ui.camera

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.gpsmapcamera.app.ui.components.LiveStampOverlay
import com.gpsmapcamera.app.ui.components.StatusIndicators
import java.util.Locale

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    var previewView: PreviewView? by remember { androidx.compose.runtime.mutableStateOf(null) }
    var currentZoom by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {

        // 1. CameraX PreviewView with Gestures (Tap-to-Focus & Pinch-to-Zoom)
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
                    detectTransformGestures { _, _, zoom, _ ->
                        currentZoom = (currentZoom + (zoom - 1f) * 0.5f).coerceIn(0f, 1f)
                        viewModel.cameraController.setLinearZoom(currentZoom)
                    }
                }
        )

        // 2. Top Bar: Status Indicators & Quick Controls
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
                // Flash / Torch toggle
                IconButton(
                    onClick = { viewModel.toggleTorch() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x80000000))
                ) {
                    Icon(
                        imageVector = if (uiState.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = if (uiState.isTorchOn) Color(0xFFFFCC00) else Color.White
                    )
                }

                Spacer(modifier = Modifier.size(10.dp))

                // QR Code toggle
                IconButton(
                    onClick = { viewModel.toggleQr() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x80000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "Toggle QR",
                        tint = if (uiState.showQr) Color(0xFF00E676) else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.size(10.dp))

                // DMS / Decimal Toggle Pill
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x80000000))
                        .clickable { viewModel.toggleDms() }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.useDms) "DMS" else "DEC",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Bottom Layer: Live Stamp Overlay + Mode Tabs + Shutter Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // WYSIWYG Stamp Overlay Card (Reflows automatically in offline mode!)
            LiveStampOverlay(
                gpsData = uiState.gpsData,
                isOnline = uiState.isEffectiveOnline,
                mapThumbnail = uiState.mapThumbnail,
                showQr = uiState.showQr,
                useDms = uiState.useDms,
                watermarkText = "GPS MAP CAMERA"
            )

            Spacer(modifier = Modifier.height(12.dp))

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

            // Action Buttons Row (Shutter, Pause/Resume, Switch Camera)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left slot: Pause/Resume if recording, else gallery indicator
                if (uiState.isRecording) {
                    IconButton(
                        onClick = {
                            if (uiState.isRecordingPaused) {
                                viewModel.resumeVideoRecording()
                            } else {
                                viewModel.pauseVideoRecording()
                            }
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                    ) {
                        Icon(
                            imageVector = if (uiState.isRecordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause/Resume",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x66000000))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { onOpenGallery() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.lastCapturedUri != null) {
                            Text("Saved", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Gallery", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Shutter Button
                if (uiState.cameraMode == CameraMode.PHOTO) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isCapturing) Color.Gray else Color.White)
                            .clickable(enabled = !uiState.isCapturing) {
                                viewModel.capturePhoto()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isCapturing) {
                            CircularProgressIndicator(
                                color = Color(0xFF1E88E5),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                } else {
                    // Video Shutter Button (Red with Pause/Stop morphing)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .clip(if (uiState.isRecording) RoundedCornerShape(12.dp) else CircleShape)
                            .background(Color(0xFFE53935))
                            .clickable {
                                viewModel.toggleVideoRecording()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isRecording) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Recording",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Switch Camera Button
                IconButton(
                    onClick = {
                        previewView?.let { viewModel.switchCamera(lifecycleOwner, it) }
                    },
                    enabled = !uiState.isRecording,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = if (uiState.isRecording) Color.Gray else Color.White,
                        modifier = Modifier.size(28.dp)
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
    }
}
