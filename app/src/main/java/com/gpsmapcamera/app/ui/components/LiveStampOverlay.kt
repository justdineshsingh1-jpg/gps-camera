package com.gpsmapcamera.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextOverflow
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
    watermarkText: String = "GPS MAP CAMERA",
    modifier: Modifier = Modifier
) {
    val cornerRadius = 14.dp
    val qrBitmap = remember(gpsData.latitude, gpsData.longitude, showQr) {
        if (showQr) QrCodeGenerator.generateLocationQr(gpsData.latitude, gpsData.longitude, 140) else null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xD90E141B))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(cornerRadius))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Watermark Row
            if (watermarkText.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = watermarkText,
                        color = Color(0x99FFFFFF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Online Map Thumbnail (Strictly omitted when offline)
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF263238))
                            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (mapThumbnail != null) {
                            Image(
                                bitmap = mapThumbnail.asImageBitmap(),
                                contentDescription = "Map Tile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        } else {
                            Text(
                                text = "MAP",
                                color = Color(0x88FFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Center Red Pin
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                // 2. Text Information Block (Reflows to full card width when offline!)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    // Line 1: City, State or Location Name
                    val cityState = when {
                        !gpsData.city.isNullOrBlank() && !gpsData.state.isNullOrBlank() -> "${gpsData.city}, ${gpsData.state}"
                        !gpsData.city.isNullOrBlank() -> gpsData.city
                        !gpsData.addressLine.isNullOrBlank() -> gpsData.addressLine.take(30)
                        else -> "Location: ${gpsData.plusCode}"
                    }
                    Text(
                        text = cityState,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Line 2: Full Address / Plus code fallback
                    val addressText = gpsData.addressLine ?: "Plus Code: ${gpsData.plusCode}"
                    Text(
                        text = addressText,
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Line 3: Lat/Long + Plus Code (High Visibility Warm Gold)
                    val coordsText = if (useDms) {
                        LocationFormattingUtils.formatDms(gpsData.latitude, gpsData.longitude)
                    } else {
                        LocationFormattingUtils.formatDecimal(gpsData.latitude, gpsData.longitude)
                    }
                    Text(
                        text = "$coordsText | ${gpsData.plusCode}",
                        color = Color(0xFFFFCC00),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Line 4: Date, Time with Weekday & GMT
                    Text(
                        text = LocationFormattingUtils.formatDateTimeWithGmt(gpsData.timestamp),
                        color = Color(0xFFB0BEC5),
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Line 5: Telemetry
                    val altStr = LocationFormattingUtils.formatAltitude(gpsData.altitudeMeters)
                    val speedStr = LocationFormattingUtils.formatSpeedKmH(gpsData.speedMps)
                    val compassStr = "${gpsData.compassDegrees.toInt()}° ${gpsData.compassDirection}"
                    val accStr = LocationFormattingUtils.formatAccuracy(gpsData.accuracyMeters)
                    Text(
                        text = "Elev: $altStr | Speed: $speedStr | $compassStr | Acc: $accStr",
                        color = Color(0xFF90A4AE),
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3. Right Side: On-device QR Code
                if (showQr && qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Location QR Code",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
        }
    }
}
