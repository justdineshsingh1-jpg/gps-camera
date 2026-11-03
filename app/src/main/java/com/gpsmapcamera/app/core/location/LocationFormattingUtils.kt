package com.gpsmapcamera.app.core.location

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object LocationFormattingUtils {

    fun formatDecimal(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "Lat %.6f°  Long %.6f°", latitude, longitude)
    }

    fun toDms(coordinate: Double, isLatitude: Boolean): String {
        val absolute = abs(coordinate)
        val degrees = absolute.toInt()
        val minutesDouble = (absolute - degrees) * 60.0
        val minutes = minutesDouble.toInt()
        val seconds = (minutesDouble - minutes) * 60.0

        val direction = if (isLatitude) {
            if (coordinate >= 0) "N" else "S"
        } else {
            if (coordinate >= 0) "E" else "W"
        }

        return String.format(Locale.US, "%d°%02d'%04.1f\" %s", degrees, minutes, seconds, direction)
    }

    fun formatDms(latitude: Double, longitude: Double): String {
        return "${toDms(latitude, true)}  ${toDms(longitude, false)}"
    }

    fun formatSpeedKmH(speedMps: Float): String {
        val kmh = speedMps * 3.6f
        return String.format(Locale.US, "%.1f km/h", kmh)
    }

    fun formatAltitude(altitudeMeters: Double): String {
        return String.format(Locale.US, "%.1f m", altitudeMeters)
    }

    fun formatAccuracy(accuracyMeters: Float): String {
        return String.format(Locale.US, "±%.1f m", accuracyMeters)
    }

    fun getCompassDirection(degrees: Float): String {
        val normalized = (degrees % 360 + 360) % 360
        val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                                 "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val index = ((normalized + 11.25f) / 22.5f).toInt() % 16
        return directions[index]
    }

    fun formatDateTimeWithGmt(timestamp: Long): String {
        val date = Date(timestamp)
        val weekdayFormatter = SimpleDateFormat("EEEE, dd/MM/yyyy hh:mm a", Locale.US)
        val baseDateStr = weekdayFormatter.format(date)

        val tz = TimeZone.getDefault()
        val offsetMillis = tz.getOffset(timestamp)
        val hours = abs(offsetMillis) / (1000 * 60 * 60)
        val minutes = (abs(offsetMillis) / (1000 * 60)) % 60
        val sign = if (offsetMillis >= 0) "+" else "-"
        val gmtString = String.format(Locale.US, "GMT %s%02d:%02d", sign, hours, minutes)

        return "$baseDateStr $gmtString"
    }
}
