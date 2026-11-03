package com.gpsmapcamera.app.core.overlay

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

@Singleton
class MapTileManager @Inject constructor() {

    // Memory cache for tiles (max 30 bitmaps)
    private val memoryCache = object : LruCache<String, Bitmap>(30) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    /**
     * Fetches or retrieves from memory cache a mini map tile thumbnail.
     * Guaranteed never to exceed 2000ms. If timeout expires or device is offline, returns null.
     */
    suspend fun getMapTileThumbnail(
        latitude: Double,
        longitude: Double,
        isOnline: Boolean,
        zoom: Int = 16
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!isOnline || latitude == 0.0 && longitude == 0.0) return@withContext null

        val tileCoord = latLonToTileCoords(latitude, longitude, zoom)
        val cacheKey = "${tileCoord.zoom}_${tileCoord.x}_${tileCoord.y}"

        // Check memory cache first
        memoryCache.get(cacheKey)?.let { return@withContext it }

        // Fetch online with 2-second strict timeout
        return@withContext withTimeoutOrNull(2000L) {
            try {
                val tileUrl = "https://tile.openstreetmap.org/${tileCoord.zoom}/${tileCoord.x}/${tileCoord.y}.png"
                val connection = (URL(tileUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 1500
                    readTimeout = 1500
                    setRequestProperty("User-Agent", "GPSMapCameraAndroid/1.0 (contact@gpsmapcamera.local)")
                    doInput = true
                }
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { input ->
                        val bitmap = BitmapFactory.decodeStream(input)
                        if (bitmap != null) {
                            memoryCache.put(cacheKey, bitmap)
                            bitmap
                        } else null
                    }
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private data class TileCoords(val zoom: Int, val x: Int, val y: Int)

    private fun latLonToTileCoords(lat: Double, lon: Double, zoom: Int): TileCoords {
        val n = 2.0.pow(zoom.toDouble())
        val x = floor((lon + 180.0) / 360.0 * n).toInt()
        val latRad = Math.toRadians(lat)
        val y = floor((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n).toInt()
        return TileCoords(zoom, x, y)
    }
}
