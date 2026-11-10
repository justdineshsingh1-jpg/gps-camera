package com.gpsmapcamera.app

import com.gpsmapcamera.app.core.location.LocationFormattingUtils
import com.gpsmapcamera.app.core.location.OpenLocationCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsAppUnitTests {

    @Test
    fun testOpenLocationCode_KnownCoordinates() {
        // Google Building 43: 37.4219999, -122.0840575 -> 849VCWC8+R5
        val code1 = OpenLocationCode.encode(37.4219999, -122.0840575)
        assertTrue("Plus code should contain '+' separator", code1.contains("+"))
        assertEquals("Plus code length should be 11 characters (10 code + 1 '+')", 11, code1.length)

        // Equator & Prime Meridian (0.0, 0.0) -> 6FG22222+22
        val codeEquator = OpenLocationCode.encode(0.0, 0.0)
        assertEquals("6FG22222+22", codeEquator)
    }

    @Test
    fun testCoordinateDmsFormatting() {
        val lat = 19.0760
        val lon = 72.8777
        val dmsLat = LocationFormattingUtils.toDms(lat, true)
        val dmsLon = LocationFormattingUtils.toDms(lon, false)

        assertTrue(dmsLat.contains("N"))
        assertTrue(dmsLon.contains("E"))
        assertTrue(dmsLat.contains("19°"))
        assertTrue(dmsLon.contains("72°"))
    }

    @Test
    fun testSpeedAndAltitudeFormatting() {
        assertEquals("36.0 km/h", LocationFormattingUtils.formatSpeedKmH(10.0f))
        assertEquals("124.5 m", LocationFormattingUtils.formatAltitude(124.5))
        assertEquals("±4.2 m", LocationFormattingUtils.formatAccuracy(4.2f))
    }

    @Test
    fun testCompassDirectionCalculations() {
        assertEquals("N", LocationFormattingUtils.getCompassDirection(0f))
        assertEquals("E", LocationFormattingUtils.getCompassDirection(90f))
        assertEquals("S", LocationFormattingUtils.getCompassDirection(180f))
        assertEquals("W", LocationFormattingUtils.getCompassDirection(270f))
        assertEquals("NE", LocationFormattingUtils.getCompassDirection(45f))
    }

    @Test
    fun testDateTimeSimpleFormatting() {
        val timestamp = 1794290880000L // 10 Nov 2026 11:28:00
        val str = LocationFormattingUtils.formatDateTimeSimple(timestamp)
        assertTrue(str.contains("Tuesday") || str.contains("2026") || str.contains("11:28"))
    }

    @Test
    fun testMapTileTypes() {
        val types = com.gpsmapcamera.app.core.overlay.MapTileType.values()
        assertEquals(3, types.size)
        assertTrue(types.contains(com.gpsmapcamera.app.core.overlay.MapTileType.GOOGLE_STREET))
        assertTrue(types.contains(com.gpsmapcamera.app.core.overlay.MapTileType.GOOGLE_HYBRID))
        assertTrue(types.contains(com.gpsmapcamera.app.core.overlay.MapTileType.OSM))
    }
}
