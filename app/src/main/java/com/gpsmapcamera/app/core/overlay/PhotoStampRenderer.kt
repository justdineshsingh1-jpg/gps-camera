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
     * Composites a permanent GPS stamp matching reference styling (Image 3):
     * - Top-Right Badge: [GPS Map Camera]
     * - Top Line: City, State, Country Flag (e.g. Guwahati, Assam, India 🇮🇳)
     * - Full Address: Multi-line wrapped with NO ellipsis (...)
     * - Coordinates: Lat xx.xxxxxx° Long xx.xxxxxx°
     * - Date/Time: Tuesday, 10/11/2026 11:28 AM
     * - Note: Note : Captured by GPS Map Camera
     * - Left: Square Map thumbnail with red pin & Google watermark (online mode)
     * - Right: Clean QR Code
     *
     * In Offline Mode:
     * - The map thumbnail is omitted, text reflows to full card width.
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

        val margin = 20f * scale
        val cardPadding = 18f * scale
        val cornerRadius = 24f * scale

        // Typography sizes
        val titleTextSize = 22f * scale
        val bodyTextSize = 14.5f * scale
        val metaTextSize = 13f * scale
        val noteTextSize = 12f * scale
        val badgeTextSize = 11f * scale

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
            textSize = bodyTextSize
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

        // 1. Prepare Text Data & Wrapped Address (NO '...' ELLIPSIS)
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

        // Calculate layout widths
        val showMap = isOnline && (mapThumbnail != null || gpsData.latitude != 0.0)
        val estimatedCardHeight = 245f * scale
        val mapSize = if (showMap) estimatedCardHeight - (cardPadding * 2) else 0f

        var qrSize = 0f
        var qrBitmap: Bitmap? = null
        if (showQr && (gpsData.latitude != 0.0 || gpsData.longitude != 0.0)) {
            qrSize = estimatedCardHeight - (cardPadding * 2.2f)
            qrBitmap = QrCodeGenerator.generateLocationQr(gpsData.latitude, gpsData.longitude, qrSize.toInt())
        }

        val cardLeft = margin
        val cardRight = imgWidth - margin
        val availableContentLeft = if (showMap) cardLeft + cardPadding + mapSize + (14f * scale) else cardLeft + cardPadding
        val availableContentRight = if (qrBitmap != null) cardRight - cardPadding - qrSize - (12f * scale) else cardRight - cardPadding
        val maxTextWidth = (availableContentRight - availableContentLeft).coerceAtLeast(120f)

        // Multi-line address wrapping without '...'
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

        // Dynamic card height calculation based on wrapped address line count
        val lineSpacing = 4f * scale
        val addressBlockHeight = addressLines.size * (bodyTextSize + lineSpacing)
        val textBlockTotalHeight = titleTextSize + (8f * scale) + addressBlockHeight + (bodyTextSize + lineSpacing) + (metaTextSize + lineSpacing) + noteTextSize + (12f * scale)
        val cardHeight = max(textBlockTotalHeight + (cardPadding * 2), 220f * scale)

        val cardRect = RectF(
            cardLeft,
            imgHeight - cardHeight - margin,
            cardRight,
            imgHeight - margin
        )

        // 2. Draw Card Background (Semi-transparent deep dark rounded rect)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(205, 15, 23, 42) // Slate 900 translucent
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        // Subtle Card Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(55, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * scale
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        // 3. Draw Map Thumbnail (Left side)
        if (showMap) {
            val mapRect = RectF(
                cardRect.left + cardPadding,
                cardRect.top + cardPadding,
                cardRect.left + cardPadding + mapSize,
                cardRect.top + cardPadding + mapSize
            )

            val mapPath = Path().apply {
                addRoundRect(mapRect, cornerRadius * 0.6f, cornerRadius * 0.6f, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(mapPath)

            if (mapThumbnail != null) {
                canvas.drawBitmap(mapThumbnail, null, mapRect, null)
            } else {
                // Modern satellite dark background placeholder
                val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(30, 41, 59)
                }
                canvas.drawRect(mapRect, placeholderPaint)
            }

            // Google Logo in bottom-left of map thumbnail
            val googleLogoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(220, 255, 255, 255)
                textSize = 10f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Google", mapRect.left + (6f * scale), mapRect.bottom - (6f * scale), googleLogoPaint)

            canvas.restore()

            // Map border
            val mapBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(80, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 1f * scale
            }
            canvas.drawRoundRect(mapRect, cornerRadius * 0.6f, cornerRadius * 0.6f, mapBorder)

            // Red Pin in Center of Map
            val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(239, 68, 68) // Bright red
                style = Paint.Style.FILL
            }
            canvas.drawCircle(mapRect.centerX(), mapRect.centerY(), 7f * scale, pinPaint)
            val pinHole = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(mapRect.centerX(), mapRect.centerY(), 2.8f * scale, pinHole)
        }

        // 4. Draw QR Code (Right side)
        if (qrBitmap != null) {
            val qrLeft = cardRect.right - cardPadding - qrSize
            val qrRect = RectF(
                qrLeft,
                cardRect.top + cardPadding + (scale * 2),
                qrLeft + qrSize,
                cardRect.top + cardPadding + (scale * 2) + qrSize
            )
            val qrBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(qrRect, 8f * scale, 8f * scale, qrBgPaint)
            canvas.drawBitmap(qrBitmap, null, qrRect, null)
        }

        // 5. Draw Top-Right Watermark Badge: [GPS Map Camera]
        val badgeText = watermarkText
        val badgePaddingH = 8f * scale
        val badgePaddingV = 4f * scale
        val badgeTextW = badgePaint.measureText(badgeText)
        val badgeW = badgeTextW + (badgePaddingH * 2) + (14f * scale)
        val badgeH = badgeTextSize + (badgePaddingV * 2)
        val badgeRight = if (qrBitmap != null) cardRect.right - cardPadding - qrSize - (10f * scale) else cardRect.right - cardPadding
        val badgeRect = RectF(
            badgeRight - badgeW,
            cardRect.top + cardPadding - (2f * scale),
            badgeRight,
            cardRect.top + cardPadding - (2f * scale) + badgeH
        )

        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 30, 41, 59)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, 6f * scale, 6f * scale, badgeBgPaint)

        // Draw small camera icon dot in badge
        val badgeDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(56, 189, 248) // Sky blue
        }
        canvas.drawCircle(badgeRect.left + (8f * scale), badgeRect.centerY(), 3.5f * scale, badgeDotPaint)
        canvas.drawText(badgeText, badgeRect.left + (16f * scale), badgeRect.centerY() + (badgeTextSize * 0.35f), badgePaint)

        // 6. Draw Content Lines
        val contentX = availableContentLeft
        var currentY = cardRect.top + cardPadding + titleTextSize

        // Line 1: Headline (Guwahati, Assam, India 🇮🇳)
        canvas.drawText(headline, contentX, currentY, titlePaint)

        // Lines 2-4: Full Address wrapped without ellipsis (...)
        currentY += titleTextSize * 0.3f
        for (line in addressLines) {
            currentY += bodyTextSize + lineSpacing
            canvas.drawText(line, contentX, currentY, bodyPaint)
        }

        // Line: Coordinates
        currentY += bodyTextSize + (6f * scale)
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
