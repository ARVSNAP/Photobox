package com.example.photobooth.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import com.example.photobooth.model.WatermarkConfig
import com.example.photobooth.model.WatermarkCorner
import com.example.photobooth.model.WatermarkPreset
import kotlin.math.max
import kotlin.math.min

object WatermarkHelper {

    /**
     * Applies the configured PNG watermark to the corner of any input image.
     */
    fun applyWatermark(source: Bitmap, config: WatermarkConfig): Bitmap {
        if (!config.isEnabled) return source

        val watermarkBitmap = getWatermarkBitmap(config) ?: return source

        // Create mutable output bitmap
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        // Calculate watermark dimensions relative to source width
        val targetWidth = max(40, (source.width * config.scalePercent).toInt())
        val aspectRatio = watermarkBitmap.height.toFloat() / watermarkBitmap.width.toFloat()
        val targetHeight = max(40, (targetWidth * aspectRatio).toInt())

        val scaledWatermark = if (watermarkBitmap.width != targetWidth || watermarkBitmap.height != targetHeight) {
            Bitmap.createScaledBitmap(watermarkBitmap, targetWidth, targetHeight, true)
        } else {
            watermarkBitmap
        }

        val marginX = (source.width * config.marginPercent).toInt()
        val marginY = (source.height * config.marginPercent).toInt()

        val posX = when (config.corner) {
            WatermarkCorner.TOP_LEFT, WatermarkCorner.BOTTOM_LEFT -> marginX.toFloat()
            WatermarkCorner.TOP_RIGHT, WatermarkCorner.BOTTOM_RIGHT -> (source.width - targetWidth - marginX).toFloat()
        }

        val posY = when (config.corner) {
            WatermarkCorner.TOP_LEFT, WatermarkCorner.TOP_RIGHT -> marginY.toFloat()
            WatermarkCorner.BOTTOM_LEFT, WatermarkCorner.BOTTOM_RIGHT -> (source.height - targetHeight - marginY).toFloat()
        }

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            alpha = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
        }

        canvas.drawBitmap(scaledWatermark, posX, posY, paint)

        if (scaledWatermark != watermarkBitmap && scaledWatermark != config.customBitmap) {
            scaledWatermark.recycle()
        }

        return output
    }

    /**
     * Resolves the active watermark bitmap (either custom uploaded PNG or crisp vector-generated preset).
     */
    fun getWatermarkBitmap(config: WatermarkConfig): Bitmap? {
        if (config.preset == WatermarkPreset.CUSTOM_UPLOAD && config.customBitmap != null) {
            return config.customBitmap
        }
        return generatePresetWatermark(config.preset)
    }

    /**
     * Dynamically generates transparent PNG badge assets for built-in presets.
     */
    fun generatePresetWatermark(preset: WatermarkPreset): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (preset) {
            WatermarkPreset.STUDIO_PRO -> {
                // Circular Studio VIP Badge (Neon Cyan / Electric Violet)
                val bgPaint = Paint().apply {
                    color = Color.parseColor("#E60F172A")
                    isAntiAlias = true
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(size / 2f, size / 2f, size / 2f - 20f, bgPaint)

                val borderPaint = Paint().apply {
                    color = Color.parseColor("#00E5FF")
                    strokeWidth = 14f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                canvas.drawCircle(size / 2f, size / 2f, size / 2f - 20f, borderPaint)

                val innerBorderPaint = Paint().apply {
                    color = Color.parseColor("#FF2A5F")
                    strokeWidth = 6f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                canvas.drawCircle(size / 2f, size / 2f, size / 2f - 40f, innerBorderPaint)

                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 48f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("PHOTO BOOTH", size / 2f, size / 2f - 30f, textPaint)

                val subPaint = Paint().apply {
                    color = Color.parseColor("#00E5FF")
                    textSize = 34f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("★ STUDIO VIP ★", size / 2f, size / 2f + 30f, subPaint)

                val yearPaint = Paint().apply {
                    color = Color.parseColor("#94A3B8")
                    textSize = 28f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("2026 EDITION", size / 2f, size / 2f + 80f, yearPaint)
            }

            WatermarkPreset.EVENT_VIP -> {
                // Rounded Shield Badge (Neon Rose)
                val rect = RectF(20f, 20f, size - 20f, size - 20f)
                val bgPaint = Paint().apply {
                    color = Color.parseColor("#EE1E1B4B")
                    isAntiAlias = true
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(rect, 48f, 48f, bgPaint)

                val borderPaint = Paint().apply {
                    color = Color.parseColor("#FF2A5F")
                    strokeWidth = 12f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                canvas.drawRoundRect(rect, 48f, 48f, borderPaint)

                val starPaint = Paint().apply {
                    color = Color.parseColor("#FFB703")
                    textSize = 52f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("✦ ✦ ✦", size / 2f, 130f, starPaint)

                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 54f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("EVENT PASS", size / 2f, 240f, textPaint)

                val linePaint = Paint().apply {
                    color = Color.parseColor("#FF2A5F")
                    strokeWidth = 4f
                }
                canvas.drawLine(80f, 280f, size - 80f, 280f, linePaint)

                val subPaint = Paint().apply {
                    color = Color.parseColor("#00E5FF")
                    textSize = 34f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("EXCLUSIVE 2026", size / 2f, 350f, subPaint)

                val footerPaint = Paint().apply {
                    color = Color.parseColor("#CBD5E1")
                    textSize = 26f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("OFFICIAL MEMORY", size / 2f, 420f, footerPaint)
            }

            WatermarkPreset.NEON_PARTY -> {
                // Diamond Cyberpunk Party Stamp
                val rect = RectF(30f, 30f, size - 30f, size - 30f)
                val bgPaint = Paint().apply {
                    color = Color.parseColor("#EE030712")
                    isAntiAlias = true
                }
                canvas.drawRoundRect(rect, 32f, 32f, bgPaint)

                val borderPaint = Paint().apply {
                    color = Color.parseColor("#8A2BE2")
                    strokeWidth = 12f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                canvas.drawRoundRect(rect, 32f, 32f, borderPaint)

                val textPaint = Paint().apply {
                    color = Color.parseColor("#00E5FF")
                    textSize = 50f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("⚡ NEON NIGHTS ⚡", size / 2f, 180f, textPaint)

                val titlePaint = Paint().apply {
                    color = Color.parseColor("#FF2A5F")
                    textSize = 62f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("PARTY LIVE", size / 2f, 290f, titlePaint)

                val footerPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 30f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("MEMORIES UNLOCKED", size / 2f, 390f, footerPaint)
            }

            WatermarkPreset.RETRO_STAMP -> {
                // Classic Golden Vintage Seal
                val bgPaint = Paint().apply {
                    color = Color.parseColor("#E6292524")
                    isAntiAlias = true
                }
                canvas.drawCircle(size / 2f, size / 2f, size / 2f - 20f, bgPaint)

                val borderPaint = Paint().apply {
                    color = Color.parseColor("#FFB703")
                    strokeWidth = 14f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                canvas.drawCircle(size / 2f, size / 2f, size / 2f - 20f, borderPaint)

                val innerDashed = Paint().apply {
                    color = Color.parseColor("#FDE047")
                    strokeWidth = 4f
                    style = Paint.Style.STROKE
                    pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f)
                    isAntiAlias = true
                }
                canvas.drawCircle(size / 2f, size / 2f, size / 2f - 46f, innerDashed)

                val textPaint = Paint().apply {
                    color = Color.parseColor("#FFB703")
                    textSize = 46f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("VINTAGE BOOTH", size / 2f, size / 2f - 20f, textPaint)

                val subPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 30f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("AUTHENTIC PRINT", size / 2f, size / 2f + 40f, subPaint)
            }

            WatermarkPreset.CUSTOM_UPLOAD -> {
                // Fallback icon placeholder if no custom bitmap provided
                val rect = RectF(20f, 20f, size - 20f, size - 20f)
                val bgPaint = Paint().apply { color = Color.parseColor("#CC1E293B"); isAntiAlias = true }
                canvas.drawRoundRect(rect, 32f, 32f, bgPaint)
                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 44f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("CUSTOM PNG", size / 2f, size / 2f, textPaint)
            }
        }

        return bitmap
    }

    /**
     * Decodes a custom PNG from a content URI with full transparency preserved.
     */
    fun decodePngFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inPremultiplied = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
