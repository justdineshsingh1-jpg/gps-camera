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
    fun createStampedBitmap(
        sourceBitmap: Bitmap,
        gpsData: GpsData,
        isOnline: Boolean,
        mapThumbnail: Bitmap? = null,
        showQr: Boolean = true,
        useDms: Boolean = false,
        watermarkText: String = "GPS Map Camera"
    ): Bitmap {
        val result = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val imgWidth = result.width.toFloat()
        val imgHeight = result.height.toFloat()

        // Normalize scaling against 1080px reference
        val scale = max(imgWidth / 1080f, 0.6f)

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

        // Paints
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = titleTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249) // Slate 100
            textSize = bodyTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            textSize = coordTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225) // Slate 300
            textSize = metaTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184) // Slate 400
            textSize = noteTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = badgeTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // 1. Prepare Headline
        val countryFlag = getCountryFlag(gpsData.country)
        val headline = when {
            !gpsData.city.isNullOrBlank() && !gpsData.state.isNullOrBlank() && !gpsData.country.isNullOrBlank() ->
                "${gpsData.city}, ${gpsData.state}, ${gpsData.country} $countryFlag"
            !gpsData.city.isNullOrBlank() && !gpsData.state.isNullOrBlank() ->
                "${gpsData.city}, ${gpsData.state} $countryFlag"
            !gpsData.city.isNullOrBlank() -> gpsData.city
            !gpsData.addressLine.isNullOrBlank() -> gpsData.addressLine.take(40)
            gpsData.latitude != 0.0 -> "GPS Location: ${gpsData.plusCode}"
            else -> "GPS Map Camera"
        }

        // 2. Wrap address without ellipsis
        val showMap = isOnline && (mapThumbnail != null || gpsData.latitude != 0.0)
        val approxEstimatedHeight = 180f * scale
        val tempSideSize = approxEstimatedHeight - (cardPadding * 2)

        val cardLeft = margin
        val cardRight = imgWidth - margin
        val availableContentLeft = if (showMap) cardLeft + cardPadding + tempSideSize + (12f * scale) else cardLeft + cardPadding
        val availableContentRight = if (showQr && (gpsData.latitude != 0.0 || gpsData.longitude != 0.0)) {
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

        // Perfect square side for Map and QR code matching the exact inner content height
        val sideSize = contentHeight

        val finalContentLeft = if (showMap) cardLeft + cardPadding + sideSize + (12f * scale) else cardLeft + cardPadding
        val finalContentRight = if (showQr && (gpsData.latitude != 0.0 || gpsData.longitude != 0.0)) {
            cardRight - cardPadding - sideSize - (10f * scale)
        } else cardRight - cardPadding

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
                canvas.drawBitmap(mapThumbnail, null, mapRect, null)
            } else {
                val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(30, 41, 59)
                }
                canvas.drawRect(mapRect, placeholderPaint)
            }

            // Google Logo in bottom-left of map thumbnail with authentic brand colors & backdrop
            drawGoogleWatermark(canvas, mapRect.left + (5f * scale), mapRect.bottom - (5f * scale), scale)

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
        if (showQr && (gpsData.latitude != 0.0 || gpsData.longitude != 0.0)) {
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
        val badgePaddingV = 3.5f * scale
        val badgeTextW = badgePaint.measureText(badgeText)
        val badgeW = badgeTextW + (badgePaddingH * 2) + (12f * scale)
        val badgeH = badgeTextSize + (badgePaddingV * 2)
        val badgeRight = if (showQr && (gpsData.latitude != 0.0 || gpsData.longitude != 0.0)) {
            cardRect.right - cardPadding - sideSize - (10f * scale)
        } else cardRect.right - cardPadding

        val badgeRect = RectF(
            badgeRight - badgeW,
            cardRect.top + cardPadding - (1.5f * scale),
            badgeRight,
            cardRect.top + cardPadding - (1.5f * scale) + badgeH
        )

        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(170, 30, 41, 59)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, 5f * scale, 5f * scale, badgeBgPaint)

        // Draw camera icon dot in badge
        val badgeDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(56, 189, 248) // Sky blue
        }
        canvas.drawCircle(badgeRect.left + (7f * scale), badgeRect.centerY(), 3f * scale, badgeDotPaint)
        canvas.drawText(badgeText, badgeRect.left + (14f * scale), badgeRect.centerY() + (badgeTextSize * 0.35f), badgePaint)

        // 8. Draw Content Lines
        val contentX = finalContentLeft
        var currentY = cardRect.top + cardPadding + titleTextSize

        // Line 1: Headline (Guwahati, Assam, India 🇮🇳)
        canvas.drawText(headline, contentX, currentY, titlePaint)

        // Lines 2..N: Full Address wrapped without ellipsis (...)
        currentY += 4f * scale
        for (line in addressLines) {
            currentY += bodyTextSize + lineSpacing
            canvas.drawText(line, contentX, currentY, bodyPaint)
        }

        // Line: Coordinates
        currentY += coordTextSize + lineSpacing
        canvas.drawText(coordsText, contentX, currentY, coordPaint)

        // Line: Date and Time (Tuesday, 10/11/2026 11:28 AM)
        currentY += metaTextSize + lineSpacing
        canvas.drawText(dateTimeText, contentX, currentY, metaPaint)

        // Line: Note (Note : Captured by GPS Map Camera)
        currentY += noteTextSize + lineSpacing
        canvas.drawText(noteText, contentX, currentY, notePaint)

        return result
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
