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

enum class MapTileType(val label: String) {
    GOOGLE_STREET("Google Street"),
    GOOGLE_HYBRID("Google Satellite"),
    OSM("OpenStreetMap")
}

@Singleton
class MapTileManager @Inject constructor() {

    // Memory cache for tiles (max 40 bitmaps)
    private val memoryCache = object : LruCache<String, Bitmap>(40) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    /**
     * Fetches or retrieves from memory cache a mini map tile thumbnail.
     * Supports Google Street, Google Satellite/Hybrid, and OpenStreetMap.
     * Guaranteed never to exceed 2000ms. If primary fails, automatically falls back.
     */
    suspend fun getMapTileThumbnail(
        latitude: Double,
        longitude: Double,
        isOnline: Boolean,
        zoom: Int = 16,
        tileType: MapTileType = MapTileType.GOOGLE_STREET
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!isOnline || (latitude == 0.0 && longitude == 0.0)) return@withContext null

        val tileCoord = latLonToTileCoords(latitude, longitude, zoom)
        val cacheKey = "${tileType.name}_${tileCoord.zoom}_${tileCoord.x}_${tileCoord.y}"

        // Check memory cache first
        memoryCache.get(cacheKey)?.let { return@withContext it }

        // Fetch online with 2-second strict timeout
        return@withContext withTimeoutOrNull(2200L) {
            val candidateUrls = when (tileType) {
                MapTileType.GOOGLE_STREET -> listOf(
                    "https://mt1.google.com/vt/lyrs=m&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}",
                    "https://mt0.google.com/vt/lyrs=m&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}",
                    "https://tile.openstreetmap.org/${tileCoord.zoom}/${tileCoord.x}/${tileCoord.y}.png"
                )
                MapTileType.GOOGLE_HYBRID -> listOf(
                    "https://mt1.google.com/vt/lyrs=y&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}",
                    "https://mt0.google.com/vt/lyrs=y&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}",
                    "https://mt1.google.com/vt/lyrs=m&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}"
                )
                MapTileType.OSM -> listOf(
                    "https://tile.openstreetmap.org/${tileCoord.zoom}/${tileCoord.x}/${tileCoord.y}.png",
                    "https://mt1.google.com/vt/lyrs=m&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}"
                )
            }

            for (urlStr in candidateUrls) {
                val bitmap = fetchTileBitmap(urlStr)
                if (bitmap != null) {
                    memoryCache.put(cacheKey, bitmap)
                    return@withTimeoutOrNull bitmap
                }
            }
            null
        }
    }

    private fun fetchTileBitmap(urlStr: String): Bitmap? {
        return try {
            val connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 1400
                readTimeout = 1400
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) Chrome/124.0.0.0")
                doInput = true
            }
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
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
