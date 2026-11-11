package com.gpsmapcamera.app.core.video

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import com.gpsmapcamera.app.core.location.GpsData
import com.gpsmapcamera.app.core.overlay.PhotoStampRenderer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** One GPS reading captured while recording, keyed to the video's own timeline (pauses excluded). */
data class StampSample(
    val presentationTimeUs: Long,
    val gpsData: GpsData,
    val mapThumbnail: Bitmap?
)

/**
 * Overlay whose bitmap follows the GPS track: for each video frame it shows the stamp of the
 * latest sample at or before that frame's time. The stamp is re-rendered only when the sample
 * changes (about once a second), so export stays fast.
 */
@OptIn(UnstableApi::class)
private class DynamicGpsStampOverlay(
    private val samples: List<StampSample>,
    private val width: Int,
    private val height: Int,
    private val isOnline: Boolean,
    private val showQr: Boolean,
    private val useDms: Boolean
) : BitmapOverlay() {

    private val blankBase: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private var cachedIndex = -1
    private var cachedBitmap: Bitmap? = null

    private fun indexFor(ptsUs: Long): Int {
        var lo = 0
        var hi = samples.size - 1
        var ans = 0
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (samples[mid].presentationTimeUs <= ptsUs) { ans = mid; lo = mid + 1 } else hi = mid - 1
        }
        return ans
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val idx = indexFor(presentationTimeUs)
        val cached = cachedBitmap
        if (idx == cachedIndex && cached != null) return cached
        val sample = samples[idx]
        val rendered = PhotoStampRenderer.createStampedBitmap(
            sourceBitmap = blankBase,
            gpsData = sample.gpsData,
            isOnline = isOnline && sample.mapThumbnail != null,
            mapThumbnail = sample.mapThumbnail,
            showQr = showQr,
            useDms = useDms,
            watermarkText = "GPS Map Camera"
        )
        cachedIndex = idx
        cachedBitmap = rendered
        return rendered
    }
}

@Singleton
class VideoStampTransformer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Burns a moving GPS Map Camera stamp into every video frame (it follows the recorded GPS track) using hardware-accelerated
     * Media3 Transformer and OpenGL OverlayEffect.
     * Media3 Transformer MUST be started on Dispatchers.Main (requires Android Looper).
     */
    @OptIn(UnstableApi::class)
    suspend fun burnStampIntoVideo(
        rawVideoFile: File,
        outputVideoFile: File,
        gpsData: GpsData,
        isOnline: Boolean,
        mapThumbnail: Bitmap?,
        showQr: Boolean,
        useDms: Boolean,
        samples: List<StampSample> = emptyList()
    ): File {
        // Track to follow: the recorded samples, or the single end-of-recording reading as fallback.
        val track: List<StampSample> = samples
            .filter { it.gpsData.latitude != 0.0 || it.gpsData.longitude != 0.0 }
            .sortedBy { it.presentationTimeUs }
            .ifEmpty { listOf(StampSample(0L, gpsData, mapThumbnail)) }

        // 1. Read the video's display size on a background thread
        val (targetWidth, targetHeight) = withContext(Dispatchers.Default) {
            var w0 = 1080
            var h0 = 1920
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(rawVideoFile.absolutePath)
                val w = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                val h = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                val rot = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                retriever.release()
                if (w != null && h != null) {
                    val isRotated = rot == 90 || rot == 270
                    w0 = if (isRotated) h else w
                    h0 = if (isRotated) w else h
                }
            } catch (e: Exception) {
                // Fallback to portrait default
            }
            Pair(w0, h0)
        }

        // 2. Build and start Media3 Transformer strictly on Dispatchers.Main
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                try {
                    val bitmapOverlay = DynamicGpsStampOverlay(
                        samples = track,
                        width = targetWidth,
                        height = targetHeight,
                        isOnline = isOnline,
                        showQr = showQr,
                        useDms = useDms
                    )
                    val overlayEffect = OverlayEffect(ImmutableList.of<TextureOverlay>(bitmapOverlay))

                    val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(rawVideoFile)))
                        .setEffects(Effects(emptyList(), listOf(overlayEffect)))
                        .build()

                    val transformer = Transformer.Builder(context)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                continuation.resume(outputVideoFile)
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException
                            ) {
                                continuation.resumeWithException(exportException)
                            }
                        })
                        .build()

                    transformer.start(editedMediaItem, outputVideoFile.absolutePath)

                    continuation.invokeOnCancellation {
                        try {
                            transformer.cancel()
                        } catch (e: Exception) {
                            // Ignored on cancellation
                        }
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }
}
