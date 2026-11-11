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
        assertEquals(2, types.size)
        assertTrue(types.contains(com.gpsmapcamera.app.core.overlay.MapTileType.GOOGLE_STREET))
        assertTrue(types.contains(com.gpsmapcamera.app.core.overlay.MapTileType.GOOGLE_HYBRID))
    }

    @Test
    fun testNortheastOfflineDatabase_Resolution() {
        // Test Guwahati City (Kailashpur Rd, Jyotinagar, Noonmati) offline
        val noonmati = com.gpsmapcamera.app.core.geocoder.NortheastOfflineDatabase.resolveOffline(26.1926, 91.7928)
        assertTrue("Must resolve Guwahati Noonmati offline", noonmati != null)
        assertEquals("Assam", noonmati?.state)
        assertEquals("Guwahati", noonmati?.city)
        assertEquals("India", noonmati?.country)
        assertTrue("Must contain Kailashpur Rd and 781021", noonmati?.formattedAddress?.contains("Kailashpur Rd") == true && noonmati?.formattedAddress?.contains("781021") == true)

        // Test Delhi (Connaught Place) offline
        val delhiCp = com.gpsmapcamera.app.core.geocoder.NortheastOfflineDatabase.resolveOffline(28.6304, 77.2177)
        assertTrue("Must resolve Delhi Connaught Place offline", delhiCp != null)
        assertEquals("Delhi", delhiCp?.state)
        assertEquals("India", delhiCp?.country)
        assertTrue("Must contain Connaught Place", delhiCp?.formattedAddress?.contains("Connaught Place") == true)

        // Test Remote Meghalaya Hill (Shillong Peak) offline
        val shillongHill = com.gpsmapcamera.app.core.geocoder.NortheastOfflineDatabase.resolveOffline(25.5348, 91.8542)
        assertTrue("Must resolve Meghalaya hill station offline", shillongHill != null)
        assertEquals("Meghalaya", shillongHill?.state)
        assertTrue("Must contain Shillong Peak", shillongHill?.formattedAddress?.contains("Shillong Peak") == true)

        // Test Arunachal Mountain (Tawang) offline
        val tawangMountain = com.gpsmapcamera.app.core.geocoder.NortheastOfflineDatabase.resolveOffline(27.5861, 91.8594)
        assertTrue("Must resolve Arunachal Pradesh mountain offline", tawangMountain != null)
        assertEquals("Arunachal Pradesh", tawangMountain?.state)
    }

    @Test
    fun testOpenLocationCode_ShortCode() {
        // Guwahati Kailashpur Rd: 26.1926, 91.7928
        val shortCode = OpenLocationCode.getShortCode(26.1926, 91.7928)
        assertTrue("Short code should contain '+'", shortCode.contains("+"))
        assertEquals("Short code should be 7 or 8 characters", true, shortCode.length in 6..8)
    }

    @Test
    fun testDeleteConfirmationText_AllPaths() {
        val ui = com.gpsmapcamera.app.ui.gallery.DeleteConfirmationText
        assertEquals("Delete this photo?", ui.title(1, 0))
        assertEquals("Delete this video?", ui.title(0, 1))
        assertEquals("Delete 3 photos?", ui.title(3, 0))
        assertEquals("Delete 2 videos?", ui.title(0, 2))
        assertEquals("Delete 4 items?", ui.title(3, 1))
        assertEquals("This can't be undone.", ui.BODY)
    }

    @Test
    fun testZoomMath_ClampAndDoubleTap() {
        val z = com.gpsmapcamera.app.ui.gallery.ZoomMath
        assertEquals(1f, z.clampScale(0.3f), 0.0001f)
        assertEquals(5f, z.clampScale(9f), 0.0001f)
        // At 1x there is no room to pan
        assertEquals(0f, z.clampOffset(120f, 1000f, 1f), 0.0001f)
        // At 2x a 1000px viewport can move 500px each way
        assertEquals(500f, z.maxOffset(1000f, 2f), 0.0001f)
        assertEquals(-500f, z.clampOffset(-900f, 1000f, 2f), 0.0001f)
        // Double-tap on the centre keeps it centred; on the left edge shifts content right
        assertEquals(0f, z.offsetForTap(500f, 1000f, 2.5f), 0.0001f)
        assertTrue(z.offsetForTap(0f, 1000f, 2.5f) > 0f)
    }

    @Test
    fun testStampPreviewKey_IgnoresSensorNoiseButTracksVisibleChanges() {
        val base = com.gpsmapcamera.app.core.location.GpsData(
            latitude = 26.149622, longitude = 91.880735, timestamp = 1794290880000L,
            addressLine = "Narengi Tinali, Guwahati", city = "Guwahati", state = "Assam", country = "India"
        )
        val noisy = base.copy(accuracyMeters = 3.1f, compassDegrees = 120f, speedMps = 0.4f, timestamp = base.timestamp + 20_000L)
        val sp = com.gpsmapcamera.app.core.overlay.StampPreview
        assertEquals(sp.displayData(base), sp.displayData(noisy))
        val nextMinute = base.copy(timestamp = base.timestamp + 60_000L)
        assertTrue(sp.displayData(base) != sp.displayData(nextMinute))
        val moved = base.copy(addressLine = "Kalitakuchi, Guwahati")
        assertTrue(sp.displayData(base) != sp.displayData(moved))
    }
}
