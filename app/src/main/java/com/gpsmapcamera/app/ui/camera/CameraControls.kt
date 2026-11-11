package com.gpsmapcamera.app.ui.camera

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/** Shared palette for the camera chrome. */
object CameraColors {
    val Glass = Color(0x66000000)
    val GlassBorder = Color(0x33FFFFFF)
    val Amber = Color(0xFFFFC107)
    val Orange = Color(0xFFFF9100)
    val Sky = Color(0xFF38BDF8)
    val Emerald = Color(0xFF22C55E)
    val Indigo = Color(0xFF4F46E5)
    val Moon = Color(0xFFFDE68A)
    val RecordRed = Color(0xFFEF4444)
}

/** Round glass control with a coloured icon; [activeBrush] fills it when the feature is on. */
@Composable
fun ControlChip(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    activeBrush: Brush? = null,
    enabled: Boolean = true,
    size: Int = 42,
    iconSize: Int = 22,
    iconRotation: Float = 0f
) {
    val bg: Modifier = if (active && activeBrush != null) Modifier.background(activeBrush) else Modifier.background(CameraColors.Glass)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .then(bg)
            .border(1.dp, if (active) Color(0x55FFFFFF) else CameraColors.GlassBorder, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else Color(0x88FFFFFF),
            modifier = Modifier
                .size(iconSize.dp)
                .rotate(iconRotation)
        )
    }
}

/** Pill-shaped glass control with an icon and a short label (EV, map type). */
@Composable
fun ControlPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    activeColor: Color = CameraColors.Sky
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(if (active) activeColor.copy(alpha = 0.85f) else CameraColors.Glass)
            .border(1.dp, if (active) Color(0x55FFFFFF) else CameraColors.GlassBorder, RoundedCornerShape(19.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if (active) Color.White else tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = if (active) Color.White else tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Professional shutter: white outer ring with a white inner disc for photos; for video a red
 * inner disc that morphs into a rounded red square while recording.
 */
@Composable
fun ShutterButton(
    mode: CameraMode,
    isRecording: Boolean,
    isCapturing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "shutterPress"
    )
    val isVideo = mode == CameraMode.VIDEO
    val innerSize by animateDpAsState(
        targetValue = if (isVideo && isRecording) 30.dp else 62.dp,
        animationSpec = tween(220),
        label = "shutterInnerSize"
    )
    val innerCorner by animateDpAsState(
        targetValue = if (isVideo && isRecording) 8.dp else 31.dp,
        animationSpec = tween(220),
        label = "shutterInnerCorner"
    )
    val innerColor by animateColorAsState(
        targetValue = when {
            isVideo -> CameraColors.RecordRed
            isCapturing -> Color(0xFFBDBDBD)
            else -> Color.White
        },
        label = "shutterInnerColor"
    )
    val ringColor by animateColorAsState(
        targetValue = if (isVideo && isRecording) Color(0xFFFFCDD2) else Color.White,
        label = "shutterRing"
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .border(4.dp, ringColor, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = !isCapturing || (isVideo && isRecording),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(innerSize)
                .scale(pressScale)
                .clip(RoundedCornerShape(innerCorner))
                .background(innerColor)
        )
        if (isCapturing && !isRecording) {
            CircularProgressIndicator(
                color = if (isVideo) Color.White else Color(0xFF1E88E5),
                strokeWidth = 3.dp,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

/**
 * Round thumbnail of the newest photo (or a frame of the newest video) next to the shutter.
 * Pops with a small spring each time a new item lands.
 */
@Composable
fun GalleryThumbnailButton(
    uri: Uri?,
    isVideo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pop = remember { Animatable(1f) }
    LaunchedEffect(uri) {
        if (uri != null) {
            pop.snapTo(0.6f)
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    Box(
        modifier = modifier
            .size(56.dp)
            .scale(pop.value)
            .clip(CircleShape)
            .background(CameraColors.Glass)
            .border(2.dp, Color.White, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = if (isVideo) "Last video" else "Last photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
            if (isVideo) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        } else {
            Icon(
                Icons.Rounded.PhotoLibrary,
                contentDescription = "Gallery",
                tint = CameraColors.Sky,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
