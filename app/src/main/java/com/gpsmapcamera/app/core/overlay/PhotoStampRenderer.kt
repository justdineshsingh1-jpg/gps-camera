package com.gpsmapcamera.app.core.overlay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.gpsmapcamera.app.core.location.GpsData
import com.gpsmapcamera.app.core.location.LocationFormattingUtils
import kotlin.math.max

object PhotoStampRenderer {

    /** Overall stamp size multiplier (text, map, QR, padding). */
    private const val STAMP_SIZE_BOOST = 1.35f

    /**
     * Composites a permanent GPS stamp matching reference styling (Zero Empty Space):
     * - Top-Right Badge: [GPS Map Camera]
     * - Top Line: City, State, Country Flag (e.g. Guwahati, Assam, India 🇮🇳)
     * - Full Address: Multi-line wrapped with NO ellipsis (...)
     * - Coordinates: Lat xx.xxxxxx° Long xx.xxxxxx°
     * - Date/Time: Tuesday, 10/11/2026 11:28 AM
     * - Note: Note : Captured by GPS Map Camera
     * - Left: Square Map thumbnail with red pin & Google watermark (online mode)
     * - Right: Clean QR Code
     *
     * Height is tightly bounded to content so there is mathematically zero blank area.
     */
    /**
     * Burns the GPS stamp into a copy of [sourceBitmap] (saved photos and every video frame).
     * The live camera preview draws the very same stamp through [renderStampOverlay], so what
     * the user sees before the shot is exactly what gets saved (WYSIWYG).
     */
    fun createStampedBitmap(
        sourceBitmap: Bitmap,
        gpsData: GpsData,
        isOnline: Boolean,
        mapThumbnail: Bitmap? = null,
        mapTileType: MapTileType = MapTileType.GOOGLE_STREET,
        showQr: Boolean = true,
        useDms: Boolean = false,
        watermarkText: String = "GPS Map Camera",
        brandLogo: Bitmap? = null
    ): Bitmap {
        val result = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        drawStamp(
            canvas, result.width.toFloat(), result.height.toFloat(),
            gpsData, isOnline, mapThumbnail, mapTileType, showQr, useDms, watermarkText, brandLogo
        )
        return result
    }

    /**
     * Renders only the stamp, on a transparent bitmap [widthPx] wide and exactly as tall as the
     * stamp (badge + card + margins). Used by the live preview: drawn at the preview's width, it has
     * the same layout and the same size relative to the frame width as the saved photo's stamp.
     */
    fun renderStampOverlay(
        widthPx: Int,
        gpsData: GpsData,
        isOnline: Boolean,
        mapThumbnail: Bitmap? = null,
        mapTileType: MapTileType = MapTileType.GOOGLE_STREET,
        showQr: Boolean = true,
        useDms: Boolean = false,
        watermarkText: String = "GPS Map Camera",
        brandLogo: Bitmap? = null
    ): Bitmap {
        val w = widthPx.coerceAtLeast(1)
        val layout = computeLayout(w.toFloat(), gpsData, showQr, useDms, watermarkText, mapThumbnail)
        val h = (layout.totalHeight + layout.margin * 2f).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawStamp(
            Canvas(bitmap), w.toFloat(), h.toFloat(),
            gpsData, isOnline, mapThumbnail, mapTileType, showQr, useDms, watermarkText, brandLogo
        )
        return bitmap
    }

    /** Everything about the stamp's geometry that depends only on the frame width and the data. */
    private class StampLayout(
        val scale: Float,
        val margin: Float,
        val cardPadding: Float,
        val cornerRadius: Float,
        val titleTextSize: Float,
        val bodyTextSize: Float,
        val coordTextSize: Float,
        val metaTextSize: Float,
        val noteTextSize: Float,
        val badgeTextSize: Float,
        val lineSpacing: Float,
        val headline: String,
        val addressLines: List<String>,
        val coordsText: String,
        val dateTimeText: String,
        val noteText: String,
        val showMap: Boolean,
        val hasQr: Boolean,
        val sideSize: Float,
        val cardHeight: Float
    ) {
        /** Brand badge sits above the card, so the stamp's full height includes it. */
        val badgeHeight: Float get() = badgeTextSize + (3.5f * scale * 2)
        val badgeGap: Float get() = 3f * scale
        val totalHeight: Float get() = cardHeight + badgeHeight + badgeGap
    }

    private fun basePaint(size: Float, color: Int, bold: Boolean) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun computeLayout(
        imgWidth: Float,
        gpsData: GpsData,
        showQr: Boolean,
        useDms: Boolean,
        watermarkText: String,
        mapThumbnail: Bitmap?
    ): StampLayout {
        // Normalize scaling against 1080px reference
        // 1.35x readability boost so the saved stamp reads as large as the live preview
        val scale = max(imgWidth / 1080f, 0.6f) * STAMP_SIZE_BOOST

        val margin = 18f * scale
        val cardPadding = 13f * scale
        val cornerRadius = 20f * scale

        // Typography sizes (enlarged for crystal readability without empty gaps)
        val titleTextSize = 23f * scale
        val bodyTextSize = 15.5f * scale
        val coordTextSize = 14.5f * scale
        val metaTextSize = 13.5f * scale
        val noteTextSize = 13f * scale
        val badgeTextSize = 11.5f * scale
        val lineSpacing = 3.5f * scale

        val bodyPaint = basePaint(bodyTextSize, Color.WHITE, bold = false)

        // 1. Prepare Headline
        val countryFlag = getCountryFlag(gpsData.country)
        val headline = when {
            !gpsData.city.isNullOrBlank() && !gpsData.state.isNullOrBlank() && !gpsData.country.isNullOrBlank() ->
                if (gpsData.city.equals(gpsData.state, ignoreCase = true)) {
                    "${gpsData.city}, ${gpsData.country} $countryFlag"
                } else {
                    "${gpsData.city}, ${gpsData.state}, ${gpsData.country} $countryFlag"
                }
            !gpsData.city.isNullOrBlank() && !gpsData.state.isNullOrBlank() ->
                if (gpsData.city.equals(gpsData.state, ignoreCase = true)) {
                    "${gpsData.city} $countryFlag"
                } else {
                    "${gpsData.city}, ${gpsData.state} $countryFlag"
                }
            !gpsData.city.isNullOrBlank() -> gpsData.city
            !gpsData.addressLine.isNullOrBlank() -> gpsData.addressLine.take(40)
            gpsData.latitude != 0.0 -> "GPS Location: ${gpsData.plusCode}"
            else -> "GPS Map Camera"
        }

        val hasFix = gpsData.latitude != 0.0 || gpsData.longitude != 0.0
        // Map panel is always shown once there is a fix: the real tile when we have one (online or
        // from the offline tile cache), otherwise a drawn offline placeholder. Same in preview & photo.
        val showMap = hasFix || mapThumbnail != null
        val hasQr = showQr && hasFix

        // 2. Wrap address without ellipsis
        val approxEstimatedHeight = 180f * scale
        val tempSideSize = approxEstimatedHeight - (cardPadding * 2)

        val cardLeft = margin
        val cardRight = imgWidth - margin
        val availableContentLeft = if (showMap) cardLeft + cardPadding + tempSideSize + (12f * scale) else cardLeft + cardPadding
        val availableContentRight = if (hasQr) {
            cardRight - cardPadding - tempSideSize - (10f * scale)
        } else cardRight - cardPadding

        val maxTextWidth = (availableContentRight - availableContentLeft).coerceAtLeast(140f)

        val rawAddress = gpsData.addressLine ?: if (gpsData.latitude != 0.0) "Plus Code: ${gpsData.plusCode}" else "Acquiring location..."
        val addressLines = wrapTextWithoutEllipsis(rawAddress, bodyPaint, maxTextWidth, maxLines = 3)

        // Coordinates line
        val coordsText = if (useDms) {
            LocationFormattingUtils.formatDms(gpsData.latitude, gpsData.longitude)
        } else {
            String.format(java.util.Locale.US, "Lat %.6f° Long %.6f°", gpsData.latitude, gpsData.longitude)
        }

        // Date and Time (e.g. Tuesday, 10/11/2026 11:28 AM)
        val dateTimeText = LocationFormattingUtils.formatDateTimeSimple(gpsData.timestamp)
        val noteText = "Note : Captured by $watermarkText"

        // 3. Precise Content Height Calculation: NO EMPTY BLANK SPACE
        val titleH = titleTextSize
        val addressBlockH = addressLines.size * (bodyTextSize + lineSpacing)
        val coordH = coordTextSize + lineSpacing
        val dateTimeH = metaTextSize + lineSpacing
        val noteH = noteTextSize + lineSpacing

        val contentHeight = titleH + (4f * scale) + addressBlockH + coordH + dateTimeH + noteH
        val cardHeight = contentHeight + (cardPadding * 2)

        return StampLayout(
            scale, margin, cardPadding, cornerRadius,
            titleTextSize, bodyTextSize, coordTextSize, metaTextSize, noteTextSize, badgeTextSize, lineSpacing,
            headline, addressLines, coordsText, dateTimeText, noteText,
            showMap, hasQr,
            sideSize = contentHeight, // perfect square side for Map and QR = inner content height
            cardHeight = cardHeight
        )
    }

    private fun drawStamp(
        canvas: Canvas,
        imgWidth: Float,
        imgHeight: Float,
        gpsData: GpsData,
        isOnline: Boolean,
        mapThumbnail: Bitmap?,
        mapTileType: MapTileType,
        showQr: Boolean,
        useDms: Boolean,
        watermarkText: String,
        brandLogo: Bitmap?
    ) {
        val l = computeLayout(imgWidth, gpsData, showQr, useDms, watermarkText, mapThumbnail)
        val scale = l.scale
        val margin = l.margin
        val cardPadding = l.cardPadding
        val cornerRadius = l.cornerRadius
        val titleTextSize = l.titleTextSize
        val bodyTextSize = l.bodyTextSize
        val coordTextSize = l.coordTextSize
        val metaTextSize = l.metaTextSize
        val noteTextSize = l.noteTextSize
        val badgeTextSize = l.badgeTextSize
        val lineSpacing = l.lineSpacing
        val sideSize = l.sideSize
        val cardHeight = l.cardHeight
        val showMap = l.showMap

        val titlePaint = basePaint(titleTextSize, Color.WHITE, bold = true)
        val bodyPaint = basePaint(bodyTextSize, Color.rgb(241, 245, 249), bold = false) // Slate 100
        val coordPaint = basePaint(coordTextSize, Color.rgb(226, 232, 240), bold = false) // Slate 200
        val metaPaint = basePaint(metaTextSize, Color.rgb(203, 213, 225), bold = false) // Slate 300
        val notePaint = basePaint(noteTextSize, Color.rgb(148, 163, 184), bold = false) // Slate 400
        val badgePaint = basePaint(badgeTextSize, Color.WHITE, bold = true)

        val cardLeft = margin
        val cardRight = imgWidth - margin

        val finalContentLeft = if (showMap) cardLeft + cardPadding + sideSize + (12f * scale) else cardLeft + cardPadding

        val cardRect = RectF(
            cardLeft,
            imgHeight - cardHeight - margin,
            cardRight,
            imgHeight - margin
        )

        // 4. Draw Card Background (Semi-transparent deep dark rounded rect)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 15, 23, 42) // Slate 900 translucent
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        // Subtle Card Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * scale
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        // 5. Draw Map Thumbnail (Left side)
        if (showMap) {
            val mapRect = RectF(
                cardRect.left + cardPadding,
                cardRect.top + cardPadding,
                cardRect.left + cardPadding + sideSize,
                cardRect.top + cardPadding + sideSize
            )

            val mapPath = Path().apply {
                addRoundRect(mapRect, cornerRadius * 0.5f, cornerRadius * 0.5f, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(mapPath)

            if (mapThumbnail != null) {
                canvas.drawBitmap(mapThumbnail, null, mapRect, Paint(Paint.FILTER_BITMAP_FLAG))
                // Google Logo in bottom-left of map thumbnail with authentic brand colors & backdrop
                drawGoogleWatermark(canvas, mapRect.left + (5f * scale), mapRect.bottom - (5f * scale), scale)
            } else {
                // When tile is loading or offline: match user's selected map tile type!
                if (mapTileType == MapTileType.GOOGLE_HYBRID) {
                    drawOfflineSatellitePlaceholder(canvas, mapRect, scale, gpsData, isOnline)
                } else {
                    drawOfflineMapPlaceholder(canvas, mapRect, scale, gpsData, isOnline)
                }
            }

            canvas.restore()

            // Map border
            val mapBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(80, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 1f * scale
            }
            canvas.drawRoundRect(mapRect, cornerRadius * 0.5f, cornerRadius * 0.5f, mapBorder)

            // Authentic Teardrop Google Maps Pin with dark red center dot
            drawGoogleMapPin(canvas, mapRect, scale)
        }

        // 6. Draw QR Code (Right side)
        if (l.hasQr) {
            val qrBitmap = QrCodeGenerator.generateLocationQr(gpsData.latitude, gpsData.longitude, sideSize.toInt())
            if (qrBitmap != null) {
                val qrLeft = cardRect.right - cardPadding - sideSize
                val qrRect = RectF(
                    qrLeft,
                    cardRect.top + cardPadding,
                    qrLeft + sideSize,
                    cardRect.top + cardPadding + sideSize
                )
                val qrBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(qrRect, cornerRadius * 0.45f, cornerRadius * 0.45f, qrBgPaint)
                canvas.drawBitmap(qrBitmap, null, qrRect, null)
            }
        }

        // 7. Draw Top-Right Watermark Badge: [GPS Map Camera]
        val badgeText = watermarkText
        val badgePaddingH = 7f * scale
        val badgeTextW = badgePaint.measureText(badgeText)
        val badgeH = l.badgeHeight
        val logoExtra = if (brandLogo != null) (badgeH - (3f * scale)) + (7f * scale) - badgePaddingH else 12f * scale
        val badgeW = badgeTextW + (badgePaddingH * 2) + logoExtra
        // Floats just above the card's top-right corner so it never collides with a long headline.
        val badgeRight = cardRect.right
        val badgeBottom = cardRect.top - l.badgeGap

        val badgeRect = RectF(
            badgeRight - badgeW,
            badgeBottom - badgeH,
            badgeRight,
            badgeBottom
        )

        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 15, 23, 42) // same translucent slate as the card
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, 5f * scale, 5f * scale, badgeBgPaint)

        // Brand logo (same artwork as the launcher icon) on the left of the badge text
        if (brandLogo != null) {
            val logoSize = badgeH - (3f * scale)
            val logoRect = RectF(
                badgeRect.left + (3f * scale),
                badgeRect.centerY() - logoSize / 2f,
                badgeRect.left + (3f * scale) + logoSize,
                badgeRect.centerY() + logoSize / 2f
            )
            canvas.drawBitmap(brandLogo, null, logoRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.drawText(badgeText, logoRect.right + (4f * scale), badgeRect.centerY() + (badgeTextSize * 0.35f), badgePaint)
        } else {
            val badgeDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(56, 189, 248) // Sky blue
            }
            canvas.drawCircle(badgeRect.left + (7f * scale), badgeRect.centerY(), 3f * scale, badgeDotPaint)
            canvas.drawText(badgeText, badgeRect.left + (14f * scale), badgeRect.centerY() + (badgeTextSize * 0.35f), badgePaint)
        }

        // 8. Draw Content Lines
        val contentX = finalContentLeft
        var currentY = cardRect.top + cardPadding + titleTextSize

        // Line 1: Headline (Guwahati, Assam, India 🇮🇳)
        canvas.drawText(l.headline, contentX, currentY, titlePaint)

        // Lines 2..N: Full Address wrapped without ellipsis (...)
        currentY += 4f * scale
        for (line in l.addressLines) {
            currentY += bodyTextSize + lineSpacing
            canvas.drawText(line, contentX, currentY, bodyPaint)
        }

        // Line: Coordinates
        currentY += coordTextSize + lineSpacing
        canvas.drawText(l.coordsText, contentX, currentY, coordPaint)

        // Line: Date and Time (Tuesday, 10/11/2026 11:28 AM)
        currentY += metaTextSize + lineSpacing
        canvas.drawText(l.dateTimeText, contentX, currentY, metaPaint)

        // Line: Note (Note : Captured by GPS Map Camera)
        currentY += noteTextSize + lineSpacing
        canvas.drawText(l.noteText, contentX, currentY, notePaint)

    }


    private fun wrapTextWithoutEllipsis(text: String, paint: Paint, maxWidth: Float, maxLines: Int = 3): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                if (lines.size >= maxLines - 1) {
                    val remaining = words.subList(words.indexOf(word), words.size).joinToString(" ")
                    lines.add(remaining)
                    return lines
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty() && lines.size < maxLines) {
            lines.add(currentLine)
        }
        return lines
    }

    /**
     * Offline map panel: a light street-map look (blocks, roads, a river) generated from the
     * coordinates so it stays stable frame to frame, with the pin and an "Offline map" tag in place
     * of the Google logo (it is not a Google tile).
     */
    private fun drawOfflineMapPlaceholder(canvas: Canvas, rect: RectF, scale: Float, gpsData: GpsData, isOnline: Boolean) {
        val base = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(236, 239, 241) }
        canvas.drawRect(rect, base)

        val seed = ((gpsData.latitude * 1000).toLong() * 31 + (gpsData.longitude * 1000).toLong()).toInt()
        val rnd = java.util.Random(seed.toLong())
        val w = rect.width()
        val h = rect.height()

        // Park block
        val park = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(200, 230, 201) }
        val px = rect.left + w * (0.05f + rnd.nextFloat() * 0.45f)
        val py = rect.top + h * (0.05f + rnd.nextFloat() * 0.45f)
        canvas.drawRoundRect(RectF(px, py, px + w * 0.32f, py + h * 0.26f), 3f * scale, 3f * scale, park)

        // River
        val river = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(170, 218, 255)
            style = Paint.Style.STROKE
            strokeWidth = 7f * scale
            strokeCap = Paint.Cap.ROUND
        }
        val ry = rect.top + h * (0.65f + rnd.nextFloat() * 0.2f)
        val riverPath = Path().apply {
            moveTo(rect.left - 4f * scale, ry)
            cubicTo(rect.left + w * 0.3f, ry - h * 0.18f, rect.left + w * 0.6f, ry + h * 0.15f, rect.right + 4f * scale, ry - h * 0.05f)
        }
        canvas.drawPath(riverPath, river)

        // Roads: casing + white fill
        val casing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(207, 212, 218)
            style = Paint.Style.STROKE
            strokeWidth = 6f * scale
        }
        val road = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f * scale
        }
        val major = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 224, 130)
            style = Paint.Style.STROKE
            strokeWidth = 5f * scale
        }
        val v1 = rect.left + w * (0.2f + rnd.nextFloat() * 0.15f)
        val v2 = rect.left + w * (0.62f + rnd.nextFloat() * 0.15f)
        val h1 = rect.top + h * (0.22f + rnd.nextFloat() * 0.12f)
        val diag = Path().apply {
            moveTo(rect.left, rect.top + h * 0.95f)
            lineTo(rect.right, rect.top + h * (0.3f + rnd.nextFloat() * 0.2f))
        }
        for (p in listOf(casing, road)) {
            canvas.drawLine(v1, rect.top, v1, rect.bottom, p)
            canvas.drawLine(v2, rect.top, v2, rect.bottom, p)
            canvas.drawLine(rect.left, h1, rect.right, h1, p)
        }
        canvas.drawPath(diag, casing.apply { strokeWidth = 7f * scale })
        canvas.drawPath(diag, major)

        // "Offline map" tag where the Google logo would be
        // Same label in preview and saved media (the tile is simply not available yet).
        val tagText = if (isOnline) "Map loading" else "Offline map"
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 8.5f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tw = tagPaint.measureText(tagText)
        val tx = rect.left + 5f * scale
        val ty = rect.bottom - 5f * scale
        val tagBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(215, 255, 255, 255) }
        canvas.drawRoundRect(
            RectF(tx - 2.5f * scale, ty - tagPaint.textSize - 1.5f * scale, tx + tw + 2.5f * scale, ty + 2f * scale),
            3.5f * scale, 3.5f * scale, tagBg
        )
        canvas.drawText(tagText, tx, ty, tagPaint)
    }

    /**
     * Offline Satellite map panel: authentic dark aerial photograph simulation (dark terrain,
     * canopy greens, dark river channel, and subtle roads) with authentic Google watermark.
     * Guarantees that selecting SATELLITE mode visibly transforms the map immediately,
     * both offline and online while tiles are loading.
     */
    private fun drawOfflineSatellitePlaceholder(canvas: Canvas, rect: RectF, scale: Float, gpsData: GpsData, isOnline: Boolean) {
        val base = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(38, 45, 42) }
        canvas.drawRect(rect, base)

        val seed = ((gpsData.latitude * 1000).toLong() * 31 + (gpsData.longitude * 1000).toLong()).toInt()
        val rnd = java.util.Random(seed.toLong())
        val w = rect.width()
        val h = rect.height()

        val forest = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(28, 40, 32) }
        val f1x = rect.left + w * (0.05f + rnd.nextFloat() * 0.4f)
        val f1y = rect.top + h * (0.05f + rnd.nextFloat() * 0.4f)
        canvas.drawRoundRect(RectF(f1x, f1y, f1x + w * 0.45f, f1y + h * 0.42f), 6f * scale, 6f * scale, forest)

        val canopy = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(46, 58, 48) }
        val c2x = rect.left + w * (0.45f + rnd.nextFloat() * 0.2f)
        val c2y = rect.top + h * (0.45f + rnd.nextFloat() * 0.2f)
        canvas.drawRoundRect(RectF(c2x, c2y, c2x + w * 0.4f, c2y + h * 0.35f), 6f * scale, 6f * scale, canopy)

        val water = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 36, 48)
            style = Paint.Style.STROKE
            strokeWidth = 6f * scale
            strokeCap = Paint.Cap.ROUND
        }
        val ry = rect.top + h * (0.6f + rnd.nextFloat() * 0.2f)
        val riverPath = Path().apply {
            moveTo(rect.left - 4f * scale, ry)
            cubicTo(rect.left + w * 0.3f, ry - h * 0.15f, rect.left + w * 0.6f, ry + h * 0.12f, rect.right + 4f * scale, ry - h * 0.05f)
        }
        canvas.drawPath(riverPath, water)

        val road = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 240, 220, 160)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * scale
        }
        val v1 = rect.left + w * (0.35f + rnd.nextFloat() * 0.15f)
        val h1 = rect.top + h * (0.45f + rnd.nextFloat() * 0.15f)
        canvas.drawLine(v1, rect.top, v1, rect.bottom, road)
        canvas.drawLine(rect.left, h1, rect.right, h1, road)

        drawGoogleWatermark(canvas, rect.left + (5f * scale), rect.bottom - (5f * scale), scale)
    }

    private fun drawGoogleMapPin(canvas: Canvas, mapRect: RectF, scale: Float) {
        val tipX = mapRect.centerX()
        val tipY = mapRect.centerY() + (3.5f * scale) // tip points directly to location center
        val pinWidth = 20f * scale
        val pinHeight = 28f * scale
        val headRadius = pinWidth / 2f
        val headCenterY = tipY - pinHeight + headRadius

        // 1. Subtle drop shadow beneath the pin tip
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 0, 0, 0)
            style = Paint.Style.FILL
        }
        val shadowRect = RectF(
            tipX - (headRadius * 0.55f),
            tipY - (1.5f * scale),
            tipX + (headRadius * 0.55f),
            tipY + (3.5f * scale)
        )
        canvas.drawOval(shadowRect, shadowPaint)

        // 2. Red Pin Body (Google Maps Red: #EA4335)
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(234, 67, 53)
            style = Paint.Style.FILL
        }

        val pinPath = Path().apply {
            moveTo(tipX, tipY)
            // Left curve up into circular head
            cubicTo(
                tipX - (headRadius * 0.75f), tipY - (pinHeight * 0.35f),
                tipX - headRadius, headCenterY + (headRadius * 0.5f),
                tipX - headRadius, headCenterY
            )
            // Top semi-circle
            arcTo(
                RectF(tipX - headRadius, headCenterY - headRadius, tipX + headRadius, headCenterY + headRadius),
                180f,
                180f,
                false
            )
            // Right curve down into tip
            cubicTo(
                tipX + headRadius, headCenterY + (headRadius * 0.5f),
                tipX + (headRadius * 0.75f), tipY - (pinHeight * 0.35f),
                tipX, tipY
            )
            close()
        }
        canvas.drawPath(pinPath, pinPaint)

        // Subtle dark pin border for high contrast
        val pinStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(35, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f * scale
        }
        canvas.drawPath(pinPath, pinStroke)

        // 3. Inner Dark Red/Maroon Center Dot (Google Maps: #B31217)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(179, 18, 23)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(tipX, headCenterY, headRadius * 0.42f, dotPaint)
    }

    private fun drawGoogleWatermark(canvas: Canvas, x: Float, y: Float, scale: Float) {
        val logoTextSize = 9.5f * scale
        val letters = listOf(
            "G" to Color.rgb(66, 133, 244),  // Google Blue #4285F4
            "o" to Color.rgb(234, 67, 53),   // Google Red #EA4335
            "o" to Color.rgb(251, 188, 5),   // Google Yellow #FBBC05
            "g" to Color.rgb(66, 133, 244),  // Google Blue #4285F4
            "l" to Color.rgb(52, 168, 83),   // Google Green #34A853
            "e" to Color.rgb(234, 67, 53)    // Google Red #EA4335
        )

        // White backdrop pill for crystal clarity on both light and dark tiles
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(215, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = logoTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val totalWidth = letters.sumOf { tempPaint.measureText(it.first).toDouble() }.toFloat()

        val pillRect = RectF(
            x - (2.5f * scale),
            y - logoTextSize - (1.5f * scale),
            x + totalWidth + (2.5f * scale),
            y + (2f * scale)
        )
        canvas.drawRoundRect(pillRect, 3.5f * scale, 3.5f * scale, bgPaint)

        var curX = x
        for ((char, col) in letters) {
            tempPaint.color = col
            canvas.drawText(char, curX, y, tempPaint)
            curX += tempPaint.measureText(char)
        }
    }

    private fun getCountryFlag(country: String?): String {
        if (country.isNullOrBlank()) return "🇮🇳"
        return when (country.trim().lowercase()) {
            "india", "in", "ind" -> "🇮🇳"
            "united states", "usa", "us" -> "🇺🇸"
            "united kingdom", "uk", "gb" -> "🇬🇧"
            "canada", "ca" -> "🇨🇦"
            "australia", "au" -> "🇦🇺"
            "uae", "united arab emirates" -> "🇦🇪"
            else -> "🇮🇳"
        }
    }
}
