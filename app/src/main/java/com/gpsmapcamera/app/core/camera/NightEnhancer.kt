package com.gpsmapcamera.app.core.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Computational low-light & night mode enhancer for photo capture.
 * Lifts deep shadows, boosts low-light clarity, and enhances night visibility
 * purely via hardware-accelerated 2D canvas without touching low-level Camera2 HAL states.
 */
object NightEnhancer {

    fun enhanceLowLight(source: Bitmap): Bitmap {
        return try {
            val result = Bitmap.createBitmap(
                source.width,
                source.height,
                source.config ?: Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            // Night Mode ColorMatrix:
            // 1. Boosts RGB channels by +30% (gain boost for low-light scenes)
            // 2. Offsets dark pixels by +28 (shadow lifting so dark areas are visible)
            // 3. Leaves alpha untouched
            val nightMatrix = ColorMatrix(
                floatArrayOf(
                    1.30f, 0f, 0f, 0f, 28f,
                    0f, 1.30f, 0f, 0f, 28f,
                    0f, 0f, 1.30f, 0f, 28f,
                    0f, 0f, 0f, 1f, 0f
                )
            )

            paint.colorFilter = ColorMatrixColorFilter(nightMatrix)
            canvas.drawBitmap(source, 0f, 0f, paint)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            source
        }
    }
}
