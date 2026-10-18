package com.example.photobooth.utils

import android.graphics.*
import com.example.photobooth.model.BrandingFrameConfig
import com.example.photobooth.model.FrameStyle
import com.example.photobooth.model.StickerPreset
import com.example.photobooth.model.WatermarkConfig

object PhotoComposer {

    fun applyColorFilter(bitmap: Bitmap, filterType: String): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val colorMatrix = ColorMatrix()

        when (filterType) {
            "B&W", "B&W Studio" -> {
                // High contrast noir monochrome
                colorMatrix.set(
                    floatArrayOf(
                        0.33f, 0.59f, 0.11f, 0f, -10f,
                        0.33f, 0.59f, 0.11f, 0f, -10f,
                        0.33f, 0.59f, 0.11f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            "Sepia", "Sepia Dream" -> {
                // Warm golden sepia tone
                colorMatrix.set(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 10f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            "Vintage", "Vintage Film" -> {
                // Retro analog film fade
                colorMatrix.set(
                    floatArrayOf(
                        0.9f, 0.05f, 0.05f, 0f, 25f,
                        0.02f, 0.85f, 0.05f, 0f, 15f,
                        0.02f, 0.05f, 0.7f, 0f, 35f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            "Cyberpunk" -> {
                // Vibrant magenta & cyan split tint
                colorMatrix.set(
                    floatArrayOf(
                        1.2f, 0.1f, 0.1f, 0f, 20f,
                        0.0f, 0.9f, 0.2f, 0f, -10f,
                        0.2f, 0.1f, 1.3f, 0f, 30f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            "Pop Art" -> {
                // Vibrant ultra-saturated pop
                colorMatrix.set(
                    floatArrayOf(
                        1.4f, -0.2f, -0.2f, 0f, 0f,
                        -0.2f, 1.4f, -0.2f, 0f, 0f,
                        -0.2f, -0.2f, 1.4f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            "Soft Portrait" -> {
                // Gentle soft glow with warm skin tones
                colorMatrix.set(
                    floatArrayOf(
                        1.05f, 0.02f, 0.02f, 0f, 12f,
                        0.02f, 1.02f, 0.02f, 0f, 8f,
                        0.02f, 0.02f, 0.95f, 0f, 5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            "Cool Drama" -> {
                // Cool deep shadows with crisp highlights
                colorMatrix.set(
                    floatArrayOf(
                        0.85f, 0.05f, 0.05f, 0f, -15f,
                        0.05f, 0.95f, 0.05f, 0f, -10f,
                        0.05f, 0.1f, 1.25f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            else -> colorMatrix.setSaturation(1.1f) // Vivid Normal
        }

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    fun composePhotoStrip(
        photos: List<Bitmap>,
        filter: String,
        overlayText: String = "PHOTO BOOTH",
        watermarkConfig: WatermarkConfig? = null,
        frameConfig: BrandingFrameConfig = BrandingFrameConfig(),
        layoutType: String = "",
        context: android.content.Context? = null
    ): Bitmap {
        val isBoothlab = layoutType == "Boothlab 8-Frame" ||
                layoutType == "Boothlab 4" ||
                layoutType == "Boothlab Frame" ||
                frameConfig.style == FrameStyle.BOOTH_LAB ||
                layoutType.contains("Boothlab", ignoreCase = true)

        if (isBoothlab) {
            return composeBoothlabPhotoStrip(
                photos = photos,
                filter = filter,
                overlayText = overlayText,
                watermarkConfig = watermarkConfig,
                frameConfig = frameConfig,
                context = context
            )
        }

        val width = 1200
        val height = 1800
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        val currentFrameStyle = frameConfig.style
        val bgColor = try { Color.parseColor(frameConfig.customBgColorHex) } catch (_: Exception) { Color.parseColor(currentFrameStyle.defaultBgHex) }
        val textColor = try { Color.parseColor(frameConfig.customTextColorHex) } catch (_: Exception) { Color.parseColor(currentFrameStyle.defaultTextHex) }
        
        val borderColor = if (frameConfig.customBorderColorHex.isNotBlank()) {
            try { Color.parseColor(frameConfig.customBorderColorHex) } catch (_: Exception) { Color.parseColor(currentFrameStyle.defaultBorderHex) }
        } else {
            try { Color.parseColor(currentFrameStyle.defaultBorderHex) } catch (_: Exception) { Color.GRAY }
        }

        // 1. Draw Background Canvas (Solid or Custom Image)
        when (frameConfig.canvasBgType) {
            com.example.photobooth.model.CanvasBgType.IMAGE -> {
                var imageDrawn = false
                if (context != null && !frameConfig.customBgImageUri.isNullOrBlank()) {
                    try {
                        val uri = android.net.Uri.parse(frameConfig.customBgImageUri)
                        val stream = if (uri.scheme == "file") {
                            java.io.FileInputStream(java.io.File(uri.path ?: ""))
                        } else {
                            context.contentResolver.openInputStream(uri)
                        }
                        stream?.use { inputStream ->
                            val bgBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                            if (bgBitmap != null) {
                                val destRect = android.graphics.Rect(0, 0, width, height)
                                canvas.drawBitmap(bgBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                                imageDrawn = true
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (!imageDrawn) {
                    canvas.drawColor(bgColor)
                }
            }
            else -> {
                canvas.drawColor(bgColor)
            }
        }

        // Draw Film Strip sprocket perforations if FILM_STRIP style is selected
        if (currentFrameStyle == FrameStyle.FILM_STRIP) {
            val perfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1F1F23") }
            val perfHolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#050505") }
            // Draw left & right film borders
            canvas.drawRect(0f, 0f, 50f, height.toFloat(), perfPaint)
            canvas.drawRect((width - 50).toFloat(), 0f, width.toFloat(), height.toFloat(), perfPaint)
            
            // Draw holes
            for (y in 40..height step 80) {
                canvas.drawRoundRect(RectF(12f, y.toFloat(), 38f, y + 45f), 8f, 8f, perfHolePaint)
                canvas.drawRoundRect(RectF((width - 38).toFloat(), y.toFloat(), (width - 12).toFloat(), y + 45f), 8f, 8f, perfHolePaint)
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header Title
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 52f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.05f
        }
        val displayTitle = if (frameConfig.headerTitle.isNotBlank()) frameConfig.headerTitle else overlayText
        canvas.drawText(displayTitle, (width / 2).toFloat(), 95f, textPaint)

        // Subtitle / Date stamp
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            alpha = 180
            textSize = 26f
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }
        canvas.drawText(frameConfig.subtitle.uppercase(), (width / 2).toFloat(), 140f, subPaint)

        // Calculate custom PNG layout transparent slots if available
        val customLayout = if (layoutType.startsWith("custom_layout_") && context != null) {
            com.example.photobooth.utils.CustomLayoutStorage.getCustomLayouts(context)
                .find { it.id == layoutType }
        } else null

        val customSlots = customLayout?.transparentSlots ?: emptyList()

        // Calculate layout slots based on photo count and user side padding / gaps
        val count = photos.size
        val topMargin = 170f
        val bottomMargin = 120f
        val availableHeight = height - topMargin - bottomMargin

        val (cols, rows) = when {
            count == 1 -> Pair(1, 1)
            count == 2 -> Pair(1, 2)
            count == 3 -> Pair(1, 3)
            count == 4 -> Pair(2, 2)
            else -> Pair(2, 3)
        }

        val sidePadding = if (currentFrameStyle == FrameStyle.FILM_STRIP) 70f else frameConfig.canvasSidePaddingDp.toFloat()
        val gap = frameConfig.canvasPhotoGapDp.toFloat()
        val slotWidth = (width - sidePadding * 2 - gap * (cols - 1)) / cols
        val slotHeight = (availableHeight - gap * (rows - 1)) / rows

        // Border paint for individual photo frames according to FrameBorderStyle
        val frameBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = frameConfig.photoBorderThicknessDp * 2.5f

            when (frameConfig.borderStyle) {
                com.example.photobooth.model.FrameBorderStyle.DASHED -> {
                    pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
                }
                com.example.photobooth.model.FrameBorderStyle.GLOW_OUTLINE -> {
                    strokeWidth = (frameConfig.photoBorderThicknessDp * 3.5f)
                    setShadowLayer(15f, 0f, 0f, borderColor)
                }
                else -> {}
            }
        }

        photos.forEachIndexed { index, bitmap ->
            val filtered = applyColorFilter(bitmap, filter)
            val processed = if (watermarkConfig != null && watermarkConfig.isEnabled) {
                WatermarkHelper.applyWatermark(filtered, watermarkConfig)
            } else {
                filtered
            }

            val destRect: RectF
            val currentSlotWidth: Float
            val currentSlotHeight: Float

            if (customSlots.isNotEmpty()) {
                val slot = customSlots[index % customSlots.size]
                val left = slot.left * width
                val top = slot.top * height
                val right = slot.right * width
                val bottom = slot.bottom * height
                destRect = RectF(left, top, right, bottom)
                currentSlotWidth = destRect.width()
                currentSlotHeight = destRect.height()
            } else {
                val col = index % cols
                val row = index / cols
                val left = sidePadding + col * (slotWidth + gap)
                val top = topMargin + row * (slotHeight + gap)
                destRect = RectF(left, top, left + slotWidth, top + slotHeight)
                currentSlotWidth = slotWidth
                currentSlotHeight = slotHeight
            }

            // Draw rounded or clipped photo card
            val cornerPx = if (customSlots.isNotEmpty()) 0f else frameConfig.photoCornerRadiusDp * 2.5f
            val path = Path().apply {
                addRoundRect(destRect, cornerPx, cornerPx, Path.Direction.CW)
            }

            canvas.save()
            canvas.clipPath(path)

            // Draw cropped photo to maintain aspect ratio without stretching
            val srcAspect = processed.width.toFloat() / processed.height.toFloat()
            val destAspect = if (currentSlotHeight > 0) currentSlotWidth / currentSlotHeight else 1f
            val srcRect = if (srcAspect > destAspect) {
                val cropW = (processed.height * destAspect).toInt()
                val leftCrop = (processed.width - cropW) / 2
                Rect(leftCrop, 0, leftCrop + cropW, processed.height)
            } else {
                val cropH = (processed.width / destAspect).toInt()
                val topCrop = (processed.height - cropH) / 2
                Rect(0, topCrop, processed.width, topCrop + cropH)
            }

            canvas.drawBitmap(processed, srcRect, destRect, paint)
            canvas.restore()

            // Draw frame stroke around the photo if not custom PNG
            if (customSlots.isEmpty() && frameConfig.photoBorderThicknessDp > 0) {
                canvas.drawRoundRect(destRect, cornerPx, cornerPx, frameBorderPaint)
                if (frameConfig.borderStyle == com.example.photobooth.model.FrameBorderStyle.DOUBLE_LINE) {
                    val innerRect = RectF(
                        destRect.left + 8f, destRect.top + 8f,
                        destRect.right - 8f, destRect.bottom - 8f
                    )
                    val innerBorderPaint = Paint(frameBorderPaint).apply { strokeWidth = 2f }
                    canvas.drawRoundRect(innerRect, Math.max(0f, cornerPx - 8f), Math.max(0f, cornerPx - 8f), innerBorderPaint)
                }
            }
        }

        // Draw Sticker / Badge Stamp if enabled
        if (frameConfig.showSticker && frameConfig.selectedSticker != StickerPreset.NONE) {
            val sticker = frameConfig.selectedSticker
            val labelText = if (sticker == StickerPreset.CUSTOM && frameConfig.customStickerText.isNotBlank()) {
                "${frameConfig.emojiSticker} ${frameConfig.customStickerText} ${frameConfig.emojiSticker}".trim()
            } else if (sticker.textLabel.isNotBlank()) {
                "${frameConfig.emojiSticker} ${sticker.textLabel} ${frameConfig.emojiSticker}".trim()
            } else {
                "${frameConfig.emojiSticker} STICKER ${frameConfig.emojiSticker}"
            }

            val stickerColor = try { Color.parseColor(sticker.primaryColorHex) } catch (_: Exception) { Color.RED }
            val stickerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stickerColor
                style = Paint.Style.FILL
            }
            val stickerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 28f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            val textWidth = stickerTextPaint.measureText(labelText)
            val badgeWidth = textWidth + 60f
            val badgeHeight = 50f

            val (badgeX, badgeY) = when (frameConfig.stickerPosition) {
                com.example.photobooth.model.StickerPosition.TOP_RIGHT -> Pair(width - badgeWidth / 2 - 30f, 90f)
                com.example.photobooth.model.StickerPosition.TOP_LEFT -> Pair(badgeWidth / 2 + 30f, 90f)
                com.example.photobooth.model.StickerPosition.FLOATING_CENTER -> Pair((width / 2).toFloat(), (height / 2).toFloat())
                else -> Pair((width / 2).toFloat(), (height - 65).toFloat()) // BOTTOM_CENTER
            }

            val badgeRect = RectF(
                badgeX - badgeWidth / 2,
                badgeY - badgeHeight / 2,
                badgeX + badgeWidth / 2,
                badgeY + badgeHeight / 2
            )

            canvas.drawRoundRect(badgeRect, 25f, 25f, stickerPaint)
            canvas.drawText(labelText, badgeX, badgeY + 10f, stickerTextPaint)
        }

        // --- DRAW CUSTOM LOGO IMAGE OVERLAY ---
        if (frameConfig.showCustomLogo && !frameConfig.customLogoImageUri.isNullOrBlank() && context != null) {
            try {
                val uri = android.net.Uri.parse(frameConfig.customLogoImageUri)
                val stream = if (uri.scheme == "file") {
                    java.io.FileInputStream(java.io.File(uri.path ?: ""))
                } else {
                    context.contentResolver.openInputStream(uri)
                }
                stream?.use { inputStream ->
                    val logoBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    if (logoBitmap != null) {
                        val scaleFactor = frameConfig.customLogoScalePercent.toFloat() / 100f
                        val destW = (width * scaleFactor).coerceIn(50f, 800f)
                        val aspect = logoBitmap.height.toFloat() / logoBitmap.width.toFloat()
                        val destH = destW * aspect

                        val (logoX, logoY) = when (frameConfig.customLogoPosition) {
                            com.example.photobooth.model.StickerPosition.TOP_RIGHT -> Pair(width - destW / 2f - 40f, 95f)
                            com.example.photobooth.model.StickerPosition.TOP_LEFT -> Pair(destW / 2f + 40f, 95f)
                            com.example.photobooth.model.StickerPosition.FLOATING_CENTER -> Pair((width / 2).toFloat(), (height / 2).toFloat())
                            else -> Pair((width / 2).toFloat(), (height - 65).toFloat()) // BOTTOM_CENTER
                        }

                        val destRect = RectF(
                            logoX - destW / 2f,
                            logoY - destH / 2f,
                            logoX + destW / 2f,
                            logoY + destH / 2f
                        )

                        canvas.drawBitmap(logoBitmap, null, destRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                        logoBitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. Draw Custom PNG Layout Overlay over the entire canvas (acting as overlay windows/frames)
        if (layoutType.startsWith("custom_layout_") && context != null) {
            try {
                val customLayout = com.example.photobooth.utils.CustomLayoutStorage.getCustomLayouts(context)
                    .find { it.id == layoutType }
                if (customLayout != null) {
                    val file = java.io.File(customLayout.imagePath)
                    if (file.exists()) {
                        val options = android.graphics.BitmapFactory.Options().apply {
                            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                        }
                        val overlayBitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
                        if (overlayBitmap != null) {
                            val destRect = android.graphics.Rect(0, 0, width, height)
                            canvas.drawBitmap(overlayBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return resultBitmap
    }

    /**
     * Composes the custom Boothlab 4 layout (2 columns x 4 rows = 8 browser window boxes)
     * with high-res mesh background gradient, fine cyber grid lines, macOS-style window title bars
     * with green, yellow, red control dots, transparent cutouts where user photos are positioned,
     * and cute 3D glossy fruit and jewelry stickers around the frames matching Free Frame Boothlab 4.
     */
    fun composeBoothlabPhotoStrip(
        photos: List<Bitmap>,
        filter: String,
        overlayText: String = "PHOTO BOOTH",
        watermarkConfig: WatermarkConfig? = null,
        frameConfig: BrandingFrameConfig = BrandingFrameConfig(),
        context: android.content.Context? = null
    ): Bitmap {
        val width = 1200
        val height = 1800
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // 1. Colorful Mesh Background Gradient (Lime Green -> Orange-Red -> Sunny Gold Yellow)
        val gradientColors = intArrayOf(
            Color.parseColor("#16A34A"), // Fresh green (bottom-left)
            Color.parseColor("#22C55E"), // Lime green
            Color.parseColor("#EA580C"), // Sunset orange (center)
            Color.parseColor("#EF4444"), // Coral red
            Color.parseColor("#F59E0B"), // Amber gold
            Color.parseColor("#FBBF24")  // Bright sun yellow (top-right)
        )
        val gradientPositions = floatArrayOf(0.0f, 0.20f, 0.46f, 0.64f, 0.84f, 1.0f)
        val bgShader = LinearGradient(
            0f, height.toFloat() * 0.85f,
            width.toFloat(), height.toFloat() * 0.15f,
            gradientColors,
            gradientPositions,
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = bgShader }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Fine Cyber Graph-Paper Grid Lines
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 24
            strokeWidth = 1.2f
        }
        val gridStep = 24f
        var gx = 0f
        while (gx <= width) {
            canvas.drawLine(gx, 0f, gx, height.toFloat(), gridPaint)
            gx += gridStep
        }
        var gy = 0f
        while (gy <= height) {
            canvas.drawLine(0f, gy, width.toFloat(), gy, gridPaint)
            gy += gridStep
        }

        // 3. The 8 Browser Window Boxes (2 Columns x 4 Rows)
        val canvasLeft = 55f
        val canvasRight = 1145f
        val colGap = 50f
        val slotWidth = (canvasRight - canvasLeft - colGap) / 2f // 520f

        val canvasTop = 80f
        val rowGap = 44f
        val slotHeight = 376f // 4 * 376 + 3 * 44 = 1636f

        val windowCornerRadius = 16f
        val headerHeight = 34f

        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
        }
        val windowBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 0, 0, 0)
        }

        val dotGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#10B981") }
        val dotYellow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F59E0B") }
        val dotRed = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EF4444") }

        val windowRects = mutableListOf<RectF>()

        var photoIdx = 0
        for (row in 0 until 4) {
            val top = canvasTop + row * (slotHeight + rowGap)
            val bottom = top + slotHeight

            for (col in 0 until 2) {
                val left = canvasLeft + col * (slotWidth + colGap)
                val right = left + slotWidth
                val windowRect = RectF(left, top, right, bottom)
                windowRects.add(windowRect)

                // 3a. Soft Drop Shadow
                val shadowRect = RectF(left + 2f, top + 4f, right + 4f, bottom + 6f)
                canvas.drawRoundRect(shadowRect, windowCornerRadius, windowCornerRadius, shadowPaint)

                // 3b. Header Bar (Rounded top corners)
                val headerPath = Path().apply {
                    addRoundRect(
                        RectF(left, top, right, top + headerHeight),
                        floatArrayOf(
                            windowCornerRadius, windowCornerRadius,
                            windowCornerRadius, windowCornerRadius,
                            0f, 0f,
                            0f, 0f
                        ),
                        Path.Direction.CW
                    )
                }
                canvas.drawPath(headerPath, headerBgPaint)

                val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#E2E8F0")
                    strokeWidth = 1f
                }
                canvas.drawLine(left, top + headerHeight, right, top + headerHeight, dividerPaint)

                // 3c. 3 Window Control Dots (Green, Yellow, Red)
                val dotY = top + headerHeight / 2f
                val dotR = 5f
                canvas.drawCircle(left + 20f, dotY, dotR, dotGreen)
                canvas.drawCircle(left + 36f, dotY, dotR, dotYellow)
                canvas.drawCircle(left + 52f, dotY, dotR, dotRed)

                // 3d. The Transparent Photo Slot (Content Area)
                val contentRect = RectF(left + 1.5f, top + headerHeight, right - 1.5f, bottom - 1.5f)
                val contentPath = Path().apply {
                    addRoundRect(
                        contentRect,
                        floatArrayOf(
                            0f, 0f,
                            0f, 0f,
                            windowCornerRadius, windowCornerRadius,
                            windowCornerRadius, windowCornerRadius
                        ),
                        Path.Direction.CW
                    )
                }

                // Clean white underlayer
                val whiteFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                canvas.drawPath(contentPath, whiteFillPaint)

                // Draw photo into the transparent slot
                if (photos.isNotEmpty()) {
                    val rawPhoto = photos[photoIdx % photos.size]
                    var filteredPhoto = applyColorFilter(rawPhoto, filter)

                    if (watermarkConfig != null && watermarkConfig.isEnabled) {
                        filteredPhoto = WatermarkHelper.applyWatermark(filteredPhoto, watermarkConfig)
                    }

                    canvas.save()
                    canvas.clipPath(contentPath)

                    // centerCrop scaling
                    val srcW = filteredPhoto.width.toFloat()
                    val srcH = filteredPhoto.height.toFloat()
                    val dstW = contentRect.width()
                    val dstH = contentRect.height()

                    val scale = maxOf(dstW / srcW, dstH / srcH)
                    val scaledW = srcW * scale
                    val scaledH = srcH * scale
                    val offX = contentRect.left + (dstW - scaledW) / 2f
                    val offY = contentRect.top + (dstH - scaledH) / 2f

                    val destRect = RectF(offX, offY, offX + scaledW, offY + scaledH)
                    val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    canvas.drawBitmap(filteredPhoto, null, destRect, photoPaint)

                    canvas.restore()
                }

                // 3e. Outer White Border
                val outerPath = Path().apply {
                    addRoundRect(windowRect, windowCornerRadius, windowCornerRadius, Path.Direction.CW)
                }
                canvas.drawPath(outerPath, windowBorderPaint)

                photoIdx++
            }
        }

        // 4. Decorative Fruits, Jewelry, and Flower Stickers (if stickers enabled)
        if (frameConfig.showSticker) {
            drawBoothlabStickers(canvas, windowRects)
        }

        // 5. Subtle Branding stamp if custom text provided
        if (overlayText.isNotBlank() && overlayText != "PHOTO BOOTH") {
            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 200
                textSize = 24f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.1f
            }
            canvas.drawText(overlayText.uppercase(), (width / 2).toFloat(), 48f, brandPaint)
        }

        // --- DRAW CUSTOM LOGO IMAGE OVERLAY ---
        if (frameConfig.showCustomLogo && !frameConfig.customLogoImageUri.isNullOrBlank() && context != null) {
            try {
                val uri = android.net.Uri.parse(frameConfig.customLogoImageUri)
                val stream = if (uri.scheme == "file") {
                    java.io.FileInputStream(java.io.File(uri.path ?: ""))
                } else {
                    context.contentResolver.openInputStream(uri)
                }
                stream?.use { inputStream ->
                    val logoBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    if (logoBitmap != null) {
                        val scaleFactor = frameConfig.customLogoScalePercent.toFloat() / 100f
                        val destW = (width * scaleFactor).coerceIn(50f, 800f)
                        val aspect = logoBitmap.height.toFloat() / logoBitmap.width.toFloat()
                        val destH = destW * aspect

                        val (logoX, logoY) = when (frameConfig.customLogoPosition) {
                            com.example.photobooth.model.StickerPosition.TOP_RIGHT -> Pair(width - destW / 2f - 40f, 95f)
                            com.example.photobooth.model.StickerPosition.TOP_LEFT -> Pair(destW / 2f + 40f, 95f)
                            com.example.photobooth.model.StickerPosition.FLOATING_CENTER -> Pair((width / 2).toFloat(), (height / 2).toFloat())
                            else -> Pair((width / 2).toFloat(), (height - 65).toFloat()) // BOTTOM_CENTER
                        }

                        val destRect = RectF(
                            logoX - destW / 2f,
                            logoY - destH / 2f,
                            logoX + destW / 2f,
                            logoY + destH / 2f
                        )

                        canvas.drawBitmap(logoBitmap, null, destRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                        logoBitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return resultBitmap
    }

    /**
     * Draws the cute fruit and jewelry stickers around the 8 window frames,
     * positioned accurately according to the user's uploaded template Free Frame Boothlab 4.
     */
    private fun drawBoothlabStickers(canvas: Canvas, windowRects: List<RectF>) {
        if (windowRects.size < 8) return

        // 1. Row 0 Left (Top-Left window): Pink Lily / Blossom on top-right corner
        drawStickerEmoji(canvas, "🌸", windowRects[0].right - 8f, windowRects[0].top + 4f, size = 80f, rotation = 12f)

        // 2. Row 0 Right (Top-Right window): Pink Blossom Flower on top-right corner
        drawStickerEmoji(canvas, "🌺", windowRects[1].right - 8f, windowRects[1].top + 4f, size = 78f, rotation = -10f)

        // 3. Row 1 Left (Second row, left window): Glossy Red Cherries on left edge
        drawStickerEmoji(canvas, "🍒", windowRects[2].left - 18f, windowRects[2].centerY() - 15f, size = 84f, rotation = -15f)

        // 4. Center Gap between Row 1 Left and Right: Diagonal Luxury Lipstick
        val centerRow1X = (windowRects[2].right + windowRects[3].left) / 2f
        drawStickerEmoji(canvas, "💄", centerRow1X, windowRects[2].centerY() - 10f, size = 88f, rotation = -32f)

        // 5. Row 2 Left (Third row, left window):
        //    - Whole Fresh Orange on top-right corner
        drawStickerEmoji(canvas, "🍊", windowRects[4].right - 10f, windowRects[4].top + 6f, size = 80f, rotation = 15f)
        //    - Sweet Red Strawberry at bottom-left corner
        drawStickerEmoji(canvas, "🍓", windowRects[4].left - 18f, windowRects[4].bottom - 10f, size = 82f, rotation = -12f)

        // 6. Row 2 Right (Third row, right window): Sliced Exotic Passion Fruit on right edge
        drawStickerEmoji(canvas, "🥭", windowRects[5].right + 16f, windowRects[5].centerY() - 5f, size = 82f, rotation = 18f)

        // 7. Row 3 Left (Bottom-left window): Ripe Wild Raspberries at bottom-right edge of box
        drawStickerEmoji(canvas, "🫐", windowRects[6].right - 12f, windowRects[6].bottom + 12f, size = 78f, rotation = 8f)

        // 8. Row 3 Right (Bottom-right window):
        //    - Sparkling Dual Diamond Jewelry Ring on top-left corner
        drawStickerEmoji(canvas, "💍", windowRects[7].left - 12f, windowRects[7].top + 6f, size = 82f, rotation = -18f)
        //    - Sliced Tropical Lychee at bottom-right corner
        drawStickerEmoji(canvas, "🥥", windowRects[7].right - 10f, windowRects[7].bottom + 12f, size = 76f, rotation = 12f)
    }

    /**
     * Renders an emoji sticker with drop shadow backing and optional rotation
     */
    private fun drawStickerEmoji(
        canvas: Canvas,
        emoji: String,
        cx: Float,
        cy: Float,
        size: Float = 78f,
        rotation: Float = 0f
    ) {
        canvas.save()
        canvas.translate(cx, cy)
        if (rotation != 0f) {
            canvas.rotate(rotation)
        }

        // Soft drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(100, 0, 0, 0)
            textAlign = Paint.Align.CENTER
            textSize = size
        }
        canvas.drawText(emoji, 0f, size * 0.35f + 4f, shadowPaint)

        // Native Color Vector Emoji
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = size
        }
        canvas.drawText(emoji, 0f, size * 0.35f, emojiPaint)
        canvas.restore()
    }
}

