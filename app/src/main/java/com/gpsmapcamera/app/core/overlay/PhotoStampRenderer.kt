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
import kotlin.math.min

object PhotoStampRenderer {

    /**
     * Composites a permanent GPS stamp directly onto the pixels of [sourceBitmap].
     * Dynamically scales to any camera resolution (1080p, 4K, 12MP, etc.).
     *
     * In Offline Mode:
     * - The map thumbnail is completely omitted.
     * - The text block seamlessly expands to occupy the full available card width.
     * - Zero blank space or broken placeholders are rendered.
     */
    fun createStampedBitmap(
        sourceBitmap: Bitmap,
        gpsData: GpsData,
        isOnline: Boolean,
        mapThumbnail: Bitmap? = null,
        showQr: Boolean = true,
        useDms: Boolean = false,
        watermarkText: String = "GPS MAP CAMERA"
    ): Bitmap {
        val result = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val imgWidth = result.width.toFloat()
        val imgHeight = result.height.toFloat()

        // Scaling factor normalized against a 1080px wide reference
        val scale = max(imgWidth / 1080f, 0.5f)

        val margin = 20f * scale
        val cardPadding = 18f * scale
        val cornerRadius = 24f * scale

        // Typography scale
        val titleTextSize = 24f * scale
        val bodyTextSize = 17f * scale
        val subTextSize = 14f * scale
        val badgeTextSize = 12f * scale

        // Card Dimensions
        val cardHeight = 220f * scale
        val cardRect = RectF(
            margin,
            imgHeight - cardHeight - margin,
            imgWidth - margin,
            imgHeight - margin
        )

        // 1. Draw Card Background (Semi-transparent dark rounded rect)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 15, 20, 26) // Deep modern slate dark
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        // Subtle border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * scale
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        // 2. Determine Map Thumbnail dimensions
        val showMap = isOnline && mapThumbnail != null
        val mapWidth = if (showMap) (cardHeight - (cardPadding * 2)) else 0f
        val mapHeight = mapWidth

        if (showMap && mapThumbnail != null) {
            val mapRect = RectF(
                cardRect.left + cardPadding,
                cardRect.top + cardPadding,
                cardRect.left + cardPadding + mapWidth,
                cardRect.top + cardPadding + mapHeight
            )

            // Clip rounded rectangle for map
            val mapPath = Path().apply {
                addRoundRect(mapRect, cornerRadius * 0.7f, cornerRadius * 0.7f, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(mapPath)
            canvas.drawBitmap(mapThumbnail, null, mapRect, null)
            canvas.restore()

            // Draw center pin on map
            val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.RED
                style = Paint.Style.FILL
            }
            canvas.drawCircle(mapRect.centerX(), mapRect.centerY(), 6f * scale, pinPaint)
            val pinHolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(mapRect.centerX(), mapRect.centerY(), 2.5f * scale, pinHolePaint)
        }

        // 3. Right side: QR Code
        var qrSize = 0f
        var qrBitmap: Bitmap? = null
        if (showQr) {
            qrSize = cardHeight - (cardPadding * 2.2f)
            qrBitmap = QrCodeGenerator.generateLocationQr(gpsData.latitude, gpsData.longitude, qrSize.toInt())
        }

        val qrLeft = if (qrBitmap != null) {
            cardRect.right - cardPadding - qrSize
        } else {
            cardRect.right - cardPadding
        }

        if (qrBitmap != null) {
            val qrRect = RectF(
                qrLeft,
                cardRect.top + cardPadding + (scale * 2),
                qrLeft + qrSize,
                cardRect.top + cardPadding + (scale * 2) + qrSize
            )
            // QR white background pad
            val qrBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(qrRect, 8f * scale, 8f * scale, qrBgPaint)
            canvas.drawBitmap(qrBitmap, null, qrRect, null)
        }

        // 4. Text block: Reflows seamlessly
        // If offline: contentStartX starts immediately at cardPadding
        // If online: starts after mapWidth + extra spacing
        val contentStartX = if (showMap) {
            cardRect.left + cardPadding + mapWidth + (14f * scale)
        } else {
            cardRect.left + cardPadding + (4f * scale)
        }

        val maxTextWidth = (qrLeft - contentStartX - (10f * scale)).coerceAtLeast(100f)

        // Paints
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = titleTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(230, 235, 240)
            textSize = bodyTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(200, 210, 220)
            textSize = subTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 204, 0) // Warm gold for coordinates & Plus Code
            textSize = bodyTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var currentY = cardRect.top + cardPadding + titleTextSize

        // Line 1: City, State or Location Headline
        val cityState = when {
            !gpsData.city.isNullOrBlank() && !gpsData.state.isNullOrBlank() -> "${gpsData.city}, ${gpsData.state}"
            !gpsData.city.isNullOrBlank() -> gpsData.city
            !gpsData.addressLine.isNullOrBlank() -> gpsData.addressLine.take(35)
            else -> "GPS Location: ${gpsData.plusCode}"
        }
        canvas.drawText(ellipsize(cityState, titlePaint, maxTextWidth), contentStartX, currentY, titlePaint)

        // Line 2: Full Address or Fallback
        currentY += bodyTextSize * 1.35f
        val addressText = gpsData.addressLine ?: "Plus Code: ${gpsData.plusCode}"
        canvas.drawText(ellipsize(addressText, bodyPaint, maxTextWidth), contentStartX, currentY, bodyPaint)

        // Line 3: Lat/Long + Plus Code
        currentY += bodyTextSize * 1.35f
        val coordsText = if (useDms) {
            LocationFormattingUtils.formatDms(gpsData.latitude, gpsData.longitude)
        } else {
            LocationFormattingUtils.formatDecimal(gpsData.latitude, gpsData.longitude)
        }
        val coordsAndCode = "$coordsText | ${gpsData.plusCode}"
        canvas.drawText(ellipsize(coordsAndCode, highlightPaint, maxTextWidth), contentStartX, currentY, highlightPaint)

        // Line 4: Date, Time with Weekday & GMT
        currentY += subTextSize * 1.4f
        val dateTimeGmt = LocationFormattingUtils.formatDateTimeWithGmt(gpsData.timestamp)
        canvas.drawText(ellipsize(dateTimeGmt, metaPaint, maxTextWidth), contentStartX, currentY, metaPaint)

        // Line 5: Telemetry (Alt, Speed, Compass, Accuracy)
        currentY += subTextSize * 1.35f
        val altStr = LocationFormattingUtils.formatAltitude(gpsData.altitudeMeters)
        val speedStr = LocationFormattingUtils.formatSpeedKmH(gpsData.speedMps)
        val compassStr = "${gpsData.compassDegrees.toInt()}° ${gpsData.compassDirection}"
        val accStr = LocationFormattingUtils.formatAccuracy(gpsData.accuracyMeters)
        val telemetry = "Elev: $altStr  |  Speed: $speedStr  |  $compassStr  |  Acc: $accStr"
        canvas.drawText(ellipsize(telemetry, metaPaint, maxTextWidth), contentStartX, currentY, metaPaint)

        // 5. App Watermark Badge (Top-right of card)
        if (watermarkText.isNotBlank()) {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(160, 255, 255, 255)
                textSize = badgeTextSize
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(watermarkText, cardRect.right - cardPadding, cardRect.top + cardPadding + badgeTextSize * 0.5f, badgePaint)
        }

        return result
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated..."
    }
}
