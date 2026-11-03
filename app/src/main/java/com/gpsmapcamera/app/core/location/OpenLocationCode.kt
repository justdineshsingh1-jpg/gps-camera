package com.gpsmapcamera.app.core.location

import kotlin.math.floor
import kotlin.math.min

/**
 * Pure Kotlin implementation of Google's Open Location Code (Plus Codes).
 * Generates offline, on-device geocodes without requiring any network calls or external libraries.
 */
object OpenLocationCode {

    private const val CODE_ALPHABET = "23456789CFGHJMPQRVWX"
    private const val ENCODING_BASE = 20
    private const val LATITUDE_MAX = 90.0
    private const val LONGITUDE_MAX = 180.0
    private const val PAIR_CODE_LENGTH = 10
    private const val SEPARATOR = '+'
    private const val SEPARATOR_POSITION = 8

    /**
     * Encodes latitude and longitude into a standard 10-character Open Location Code (e.g. "7JVW52GR+PQ").
     */
    fun encode(latitude: Double, longitude: Double, codeLength: Int = PAIR_CODE_LENGTH): String {
        var lat = latitude.coerceIn(-LATITUDE_MAX, LATITUDE_MAX)
        var lon = longitude

        while (lon < -LONGITUDE_MAX) lon += 360.0
        while (lon >= LONGITUDE_MAX) lon -= 360.0

        if (lat == LATITUDE_MAX) {
            lat -= 0.0000001
        }

        lat += LATITUDE_MAX
        lon += LONGITUDE_MAX

        val code = StringBuilder()

        // Compute digit pairs
        var latVal = lat
        var lonVal = lon

        var latResolution = 20.0
        var lonResolution = 20.0

        for (i in 0 until (codeLength / 2)) {
            val latDigit = floor(latVal / latResolution).toInt()
            val lonDigit = floor(lonVal / lonResolution).toInt()

            latVal -= latDigit * latResolution
            lonVal -= lonDigit * lonResolution

            latResolution /= ENCODING_BASE
            lonResolution /= ENCODING_BASE

            code.append(CODE_ALPHABET[latDigit.coerceIn(0, ENCODING_BASE - 1)])
            code.append(CODE_ALPHABET[lonDigit.coerceIn(0, ENCODING_BASE - 1)])

            if (code.length == SEPARATOR_POSITION) {
                code.append(SEPARATOR)
            }
        }

        if (code.length <= SEPARATOR_POSITION) {
            while (code.length < SEPARATOR_POSITION) {
                code.append('0')
            }
            code.append(SEPARATOR)
        }

        return code.toString()
    }
}
