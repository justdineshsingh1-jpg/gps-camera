package com.gpsmapcamera.app.ui.gallery

/** Pure pinch/pan/double-tap math for the photo viewer (unit-tested, no Android types). */
object ZoomMath {
    const val MIN_SCALE = 1f
    const val MAX_SCALE = 5f
    const val DOUBLE_TAP_SCALE = 2.5f

    fun clampScale(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

    /** Largest translation on one axis that keeps the scaled content covering the viewport. */
    fun maxOffset(viewportSize: Float, scale: Float): Float = ((viewportSize * scale) - viewportSize).coerceAtLeast(0f) / 2f

    fun clampOffset(offset: Float, viewportSize: Float, scale: Float): Float {
        val m = maxOffset(viewportSize, scale)
        return offset.coerceIn(-m, m)
    }

    /**
     * Translation that keeps the tapped point under the finger when zooming from scale 1 to
     * [targetScale] (graphicsLayer scales around the centre).
     */
    fun offsetForTap(tap: Float, viewportSize: Float, targetScale: Float): Float {
        val raw = (viewportSize / 2f - tap) * (targetScale - 1f)
        return clampOffset(raw, viewportSize, targetScale)
    }
}
