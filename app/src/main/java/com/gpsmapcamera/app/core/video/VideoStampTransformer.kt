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
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.TransformationRequest
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
    ): File = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                // 1. Generate full-resolution transparent canvas stamp
                val referenceWidth = 1920
                val referenceHeight = 1080
                val blankBase = Bitmap.createBitmap(referenceWidth, referenceHeight, Bitmap.Config.ARGB_8888)

                val stampedFullFrame = PhotoStampRenderer.createStampedBitmap(
                    sourceBitmap = blankBase,
                    gpsData = gpsData,
                    isOnline = isOnline,
                    mapThumbnail = mapThumbnail,
                    showQr = showQr,
                    useDms = useDms,
                    watermarkText = "GPS MAP CAMERA"
                )

                // 2. Build Media3 BitmapOverlay
                val bitmapOverlay = BitmapOverlay.createStaticBitmapOverlay(stampedFullFrame)
                val overlayEffect = OverlayEffect(ImmutableList.of<TextureOverlay>(bitmapOverlay))

                // 3. Configure Media3 Transformer with H.264 / AAC hardware acceleration
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
                    transformer.cancel()
                }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }
}
