package com.gpsmapcamera.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpsmapcamera.app.core.connectivity.ConnectivityMode
import com.gpsmapcamera.app.core.connectivity.NetworkStatus
import com.gpsmapcamera.app.core.location.GpsData

@Composable
fun StatusIndicators(
    networkStatus: NetworkStatus,
    connectivityMode: ConnectivityMode,
    gpsData: GpsData,
    onToggleConnectivityMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Network Status Pill (Interactive to toggle Force Offline / Auto)
        val isEffectiveOnline = networkStatus == NetworkStatus.ONLINE && connectivityMode == ConnectivityMode.AUTO
        val netColor = if (isEffectiveOnline) Color(0xFF4CAF50) else Color(0xFFFF9800)
        val netText = when {
            connectivityMode == ConnectivityMode.FORCE_OFFLINE -> "OFFLINE (FORCED)"
            networkStatus == NetworkStatus.ONLINE -> "ONLINE"
            else -> "OFFLINE"
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC111827))
                .clickable { onToggleConnectivityMode() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(netColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = netText,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 2. GPS Signal Lock Pill
        val gpsLocked = gpsData.isLocked && gpsData.latitude != 0.0
        val gpsColor = if (gpsLocked) Color(0xFF00E676) else Color(0xFFFFEB3B)
        val satsText = if (gpsData.satellitesCount > 0) "${gpsData.satellitesCount} Sats • " else ""
        val accText = "±${gpsData.accuracyMeters.toInt().coerceAtLeast(1)}m"
        val gpsText = if (gpsLocked) {
            "GPS: Locked ($satsText$accText)"
        } else {
            if (gpsData.satellitesCount > 0) "GPS: Searching (${gpsData.satellitesCount} Sats)..." else "GPS: Acquiring..."
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xCC111827))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(gpsColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = gpsText,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
