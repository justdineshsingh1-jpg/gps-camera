package com.gpsmapcamera.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpsmapcamera.app.core.location.GpsData
import com.gpsmapcamera.app.core.location.LocationFormattingUtils
import com.gpsmapcamera.app.core.overlay.QrCodeGenerator

@Composable
fun LiveStampOverlay(
    gpsData: GpsData,
    isOnline: Boolean,
    mapThumbnail: Bitmap? = null,
    showQr: Boolean = true,
    useDms: Boolean = false,
    watermarkText: String = "GPS Map Camera",
    modifier: Modifier = Modifier
) {
    val cornerRadius = 14.dp
    val qrBitmap = remember(gpsData.latitude, gpsData.longitude, showQr) {
        if (showQr && (gpsData.latitude != 0.0 || gpsData.longitude != 0.0)) {
            QrCodeGenerator.generateLocationQr(gpsData.latitude, gpsData.longitude, 160)
        } else null
    }

    val countryFlag = when (gpsData.country?.trim()?.lowercase()) {
        "india", "in", "ind" -> "🇮🇳"
        "united states", "usa", "us" -> "🇺🇸"
        "united kingdom", "uk", "gb" -> "🇬🇧"
        "canada", "ca" -> "🇨🇦"
        "australia", "au" -> "🇦🇺"
        else -> "🇮🇳"
    }

    val headline = when {
        !gpsData.city.isNullOrBlank() && !gpsData.state.isNullOrBlank() && !gpsData.country.isNullOrBlank() ->
            "${gpsData.city}, ${gpsData.state}, ${gpsData.country} $countryFlag"
        !gpsData.city.isNullOrBlank() && !gpsData.state.isNullOrBlank() ->
            "${gpsData.city}, ${gpsData.state} $countryFlag"
        !gpsData.city.isNullOrBlank() -> gpsData.city
        !gpsData.addressLine.isNullOrBlank() -> gpsData.addressLine.take(40)
        gpsData.latitude != 0.0 -> "GPS Location: ${gpsData.plusCode}"
        else -> "GPS Map Camera"
    }

    val addressText = gpsData.addressLine ?: if (gpsData.latitude != 0.0) "Plus Code: ${gpsData.plusCode}" else "Acquiring location..."
    val coordsText = if (useDms) {
        LocationFormattingUtils.formatDms(gpsData.latitude, gpsData.longitude)
    } else {
        String.format(java.util.Locale.US, "Lat %.6f° Long %.6f°", gpsData.latitude, gpsData.longitude)
    }
    val dateTimeText = LocationFormattingUtils.formatDateTimeSimple(gpsData.timestamp)
    val noteText = "Note : Captured by $watermarkText"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xD90F172A)) // Slate 900 translucent
            .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(cornerRadius))
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Top-right App Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x991E293B))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = watermarkText,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Body: [Map Thumbnail] [Address Block (No '...')] [QR Code]
            // Using IntrinsicSize.Min ensures Map, Text Block, and QR Code match 1:1 in height with zero empty space
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Online Map Thumbnail with Red Pin & Google Logo
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (mapThumbnail != null) {
                            Image(
                                bitmap = mapThumbnail.asImageBitmap(),
                                contentDescription = "Map Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        } else {
                            Text(
                                text = "MAP",
                                color = Color(0x88FFFFFF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Google Watermark
                        Text(
                            text = "Google",
                            color = Color(0xDDFFFFFF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(3.dp)
                        )

                        // Center Red Pin
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                // 2. Center Text Block: Full Wrapped Address with NO ellipsis
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                ) {
                    // Headline: City, State, Country Flag
                    Text(
                        text = headline,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.5.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Full Address: Wrapped across multiple lines without '...'
                    Text(
                        text = addressText,
                        color = Color(0xFFF1F5F9),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 14.sp,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Coordinates
                    Text(
                        text = coordsText,
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Date and Time
                    Text(
                        text = dateTimeText,
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )

                    // Note
                    Text(
                        text = noteText,
                        color = Color(0xFF94A3B8),
                        fontSize = 9.5.sp
                    )
                }

                // 3. Right: QR Code
                if (showQr && qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Location QR Code",
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }
            }
        }
    }
}
