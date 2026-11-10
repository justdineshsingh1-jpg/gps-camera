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

@Singleton
class VideoStampTransformer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Burns the GPS Map Camera stamp into every video frame using hardware-accelerated
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
        useDms: Boolean
    ): File {
        // 1. Generate full-resolution transparent canvas stamp on background thread
        val stampedFullFrame = withContext(Dispatchers.Default) {
            var targetWidth = 1080
            var targetHeight = 1920

            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(rawVideoFile.absolutePath)
                val w = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                val h = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                val rot = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                retriever.release()

                if (w != null && h != null) {
                    val isRotated = rot == 90 || rot == 270
                    targetWidth = if (isRotated) h else w
                    targetHeight = if (isRotated) w else h
                }
            } catch (e: Exception) {
                // Fallback to portrait default
            }

            val blankBase = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

            PhotoStampRenderer.createStampedBitmap(
                sourceBitmap = blankBase,
                gpsData = gpsData,
                isOnline = isOnline,
                mapThumbnail = mapThumbnail,
                showQr = showQr,
                useDms = useDms,
                watermarkText = "GPS Map Camera"
            )
        }

        // 2. Build and start Media3 Transformer strictly on Dispatchers.Main
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                try {
                    val bitmapOverlay = BitmapOverlay.createStaticBitmapOverlay(stampedFullFrame)
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
