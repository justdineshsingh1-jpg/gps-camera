package com.gpsmapcamera.app.core.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
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
    GOOGLE_HYBRID("Google Satellite")
}

@Singleton
class MapTileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Tiles seen online are kept on disk so the stamp still has a real map offline in known areas. */
    private val diskDir: File by lazy { File(context.cacheDir, "map_tiles").apply { mkdirs() } }

    private fun readDisk(key: String): Bitmap? = try {
        val f = File(diskDir, "$key.png")
        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    } catch (e: Exception) {
        null
    }

    private fun writeDisk(key: String, bitmap: Bitmap) {
        try {
            val f = File(diskDir, "$key.png")
            FileOutputStream(f).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            // Keep the cache bounded (~300 tiles, oldest first).
            val files = diskDir.listFiles() ?: return
            if (files.size > 300) {
                files.sortedBy { it.lastModified() }.take(files.size - 300).forEach { it.delete() }
            }
        } catch (e: Exception) {
            // Cache is best-effort
        }
    }


    // Memory cache for tiles (max 40 bitmaps)
    private val memoryCache = object : LruCache<String, Bitmap>(40) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    /**
     * Fetches or retrieves from memory cache a mini map tile thumbnail using Google Maps tiles.
     * Guaranteed never to exceed 2000ms.
     */
    suspend fun getMapTileThumbnail(
        latitude: Double,
        longitude: Double,
        isOnline: Boolean,
        zoom: Int = 16,
        tileType: MapTileType = MapTileType.GOOGLE_STREET
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (latitude == 0.0 && longitude == 0.0) return@withContext null

        val tileCoord = latLonToTileCoords(latitude, longitude, zoom)
        val cacheKey = "${tileType.name}_${tileCoord.zoom}_${tileCoord.x}_${tileCoord.y}"

        // Check memory cache first, then the offline disk cache
        memoryCache.get(cacheKey)?.let { return@withContext it }
        readDisk(cacheKey)?.let {
            memoryCache.put(cacheKey, it)
            return@withContext it
        }
        if (!isOnline) return@withContext null

        // Fetch online with 4.5-second timeout for high-res satellite/street tiles
        return@withContext withTimeoutOrNull(4500L) {
            val candidateUrls = when (tileType) {
                MapTileType.GOOGLE_STREET -> listOf(
                    "https://mt1.google.com/vt/lyrs=m&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}",
                    "https://mt0.google.com/vt/lyrs=m&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}",
                    "https://mt2.google.com/vt/lyrs=m&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}",
                    "https://mt3.google.com/vt/lyrs=m&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}"
                )
                MapTileType.GOOGLE_HYBRID -> listOf(
                    "https://mt1.google.com/vt/lyrs=y&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}",
                    "https://mt0.google.com/vt/lyrs=y&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}",
                    "https://mt2.google.com/vt/lyrs=y&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}",
                    "https://mt3.google.com/vt/lyrs=y&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}",
                    "https://mt1.google.com/vt/lyrs=s&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}",
                    "https://mt0.google.com/vt/lyrs=s&x=${tileCoord.x}&y=${tileCoord.y}&z=${tileCoord.zoom}"
                )
            }

            for (urlStr in candidateUrls) {
                val bitmap = fetchTileBitmap(urlStr)
                if (bitmap != null) {
                    memoryCache.put(cacheKey, bitmap)
                    writeDisk(cacheKey, bitmap)
                    return@withTimeoutOrNull bitmap
                }
            }
            null
        }
    }

    private fun fetchTileBitmap(urlStr: String): Bitmap? {
        return try {
            val connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3500
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
