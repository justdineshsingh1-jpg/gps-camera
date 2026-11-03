package com.gpsmapcamera.app.core.overlay

import androidx.exifinterface.media.ExifInterface
import com.gpsmapcamera.app.core.location.GpsData
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object ExifWriter {

    /**
     * Writes GPS and DateTime EXIF tags into the saved JPEG file.
     */
    fun writeGpsExif(imageFile: File, gpsData: GpsData) {
        try {
            val exif = ExifInterface(imageFile.absolutePath)

            // Latitude and Longitude
            exif.setLatLong(gpsData.latitude, gpsData.longitude)

            // Altitude
            if (gpsData.altitudeMeters != 0.0) {
                exif.setAltitude(gpsData.altitudeMeters)
            }

            // Timestamp in UTC format for GPS
            val gpsDateFormatter = SimpleDateFormat("yyyy:MM:dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val gpsTimeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = Date(gpsData.timestamp)
            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, gpsDateFormatter.format(date))
            exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, gpsTimeFormatter.format(date))

            // Standard DateTime
            val localDateFormatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            exif.setAttribute(ExifInterface.TAG_DATETIME, localDateFormatter.format(date))
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, localDateFormatter.format(date))
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, localDateFormatter.format(date))

            // User comment with Plus Code
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "GPS Map Camera - Plus Code: ${gpsData.plusCode}")

            exif.saveAttributes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
