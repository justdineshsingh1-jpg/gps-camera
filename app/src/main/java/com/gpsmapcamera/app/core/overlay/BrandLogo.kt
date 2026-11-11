package com.gpsmapcamera.app.core.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.gpsmapcamera.app.R

/**
 * The app's brand mark (camera + map pin on a gradient), rasterised from the same vector artwork
 * as the launcher icon (res/drawable/ic_brand_logo.xml) so the stamp badge shows the real logo.
 */
object BrandLogo {

    private const val SIZE_PX = 128

    @Volatile
    private var cached: Bitmap? = null

    fun bitmap(context: Context): Bitmap? {
        cached?.let { return it }
        return try {
            val drawable = ContextCompat.getDrawable(context.applicationContext, R.drawable.ic_brand_logo) ?: return null
            val bmp = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, SIZE_PX, SIZE_PX)
            drawable.draw(canvas)
            cached = bmp
            bmp
        } catch (e: Exception) {
            null
        }
    }
}
