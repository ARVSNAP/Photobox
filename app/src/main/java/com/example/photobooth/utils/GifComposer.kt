package com.example.photobooth.utils

import android.graphics.*
import com.example.photobooth.model.WatermarkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GifComposer {

    /**
     * Composites and encodes a list of frames into an animated GIF byte array
     * with filter processing, watermarking, and dynamic loop animation preset overlays.
     */
    suspend fun createAnimatedGif(
        frames: List<Bitmap>,
        filter: String = "Normal",
        watermarkText: String = "",
        watermarkConfig: WatermarkConfig? = null,
        isBoomerang: Boolean = true,
        delayMs: Int = 150,
        loopPreset: String = "None",
        onProgress: (progress: Float, statusMessage: String) -> Unit = { _, _ -> }
    ): ByteArray = withContext(Dispatchers.Default) {
        if (frames.isEmpty()) return@withContext ByteArray(0)

        onProgress(0.05f, "Preparing frame sequence...")

        val processedFrames = mutableListOf<Bitmap>()
        val total = frames.size

        // Step 1: Filter grading, branding overlays, and animated loop preset effects
        for (i in frames.indices) {
            val progressVal = 0.05f + (i.toFloat() / total) * 0.25f
            onProgress(progressVal, "Applying styling & loop FX to frame ${i + 1} of $total...")

            val original = frames[i]
            val filtered = PhotoComposer.applyColorFilter(original, filter)

            val withWatermark = if (watermarkConfig != null && watermarkConfig.isEnabled) {
                WatermarkHelper.applyWatermark(filtered, watermarkConfig)
            } else {
                filtered
            }

            // Create a mutable copy to overlay our custom graphics and text safely
            val copy = withWatermark.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(copy)

            // --- Draw Loop Animation Preset overlay onto the GIF frame ---
            when (loopPreset) {
                "Neon Pulse" -> {
                    val t = i.toFloat() / total
                    val pulseScale = 0.8f + 0.3f * kotlin.math.sin(t * 2 * Math.PI.toFloat())
                    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(255, 0, 127) // Neon Rose
                        style = Paint.Style.FILL
                    }
                    val centerX = copy.width / 2f
                    val centerY = copy.height / 2f
                    val baseRadius = Math.min(copy.width, copy.height) * 0.25f
                    for (step in 1..5) {
                        val radius = baseRadius * pulseScale * (1f + step * 0.15f)
                        glowPaint.alpha = (30 / step)
                        canvas.drawCircle(centerX, centerY, radius, glowPaint)
                    }
                }
                "Disco Stars" -> {
                    val rand = java.util.Random(12345) // Same seed preserves static positions across frames
                    val starCount = 15
                    val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        style = Paint.Style.FILL
                    }
                    for (starIdx in 0 until starCount) {
                        val starX = rand.nextFloat() * copy.width
                        val starY = rand.nextFloat() * copy.height
                        val maxRadius = rand.nextFloat() * 20f + 10f
                        val phase = rand.nextFloat() * 2 * Math.PI.toFloat()
                        val t = i.toFloat() / total
                        val twinkle = 0.2f + 0.8f * (0.5f + 0.5f * kotlin.math.sin(t * 4 * Math.PI.toFloat() + phase))
                        starPaint.alpha = (twinkle * 255).toInt().coerceIn(0, 255)

                        val path = Path().apply {
                            moveTo(starX, starY - maxRadius)
                            lineTo(starX + maxRadius * 0.25f, starY - maxRadius * 0.25f)
                            lineTo(starX + maxRadius, starY)
                            lineTo(starX + maxRadius * 0.25f, starY + maxRadius * 0.25f)
                            lineTo(starX, starY + maxRadius)
                            lineTo(starX - maxRadius * 0.25f, starY + maxRadius * 0.25f)
                            lineTo(starX - maxRadius, starY)
                            lineTo(starX - maxRadius * 0.25f, starY - maxRadius * 0.25f)
                            close()
                        }
                        canvas.drawPath(path, starPaint)
                    }
                }
                "Retro Scanline" -> {
                    val scanProgress = i.toFloat() / total
                    val scanY = scanProgress * copy.height

                    // Cyber Grid lines
                    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(0, 245, 255) // Neon Cyan
                        strokeWidth = 2f
                        style = Paint.Style.STROKE
                        alpha = 30
                    }
                    val colSpacing = copy.width / 12f
                    var gx = 0f
                    while (gx < copy.width) {
                        canvas.drawLine(gx, 0f, gx, copy.height.toFloat(), linePaint)
                        gx += colSpacing
                    }
                    val rowSpacing = copy.height / 12f
                    var gy = 0f
                    while (gy < copy.height) {
                        canvas.drawLine(0f, gy, copy.width.toFloat(), gy, linePaint)
                        gy += rowSpacing
                    }

                    // Sweeping scanline neon glow
                    val scanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                    }
                    val scanGlowHeight = 50f
                    val scanShader = LinearGradient(
                        0f, scanY - scanGlowHeight,
                        0f, scanY + scanGlowHeight,
                        intArrayOf(Color.TRANSPARENT, Color.rgb(255, 0, 127), Color.TRANSPARENT), // Rose scanline
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    scanPaint.shader = scanShader
                    canvas.drawRect(0f, scanY - scanGlowHeight, copy.width.toFloat(), scanY + scanGlowHeight, scanPaint)
                }
            }

            // Draw Watermark text
            if (watermarkText.isNotBlank()) {
                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 36f
                    isFakeBoldText = true
                    setShadowLayer(6f, 2f, 2f, Color.BLACK)
                    isAntiAlias = true
                }
                canvas.drawText(watermarkText, 40f, copy.height - 40f, textPaint)
            }

            processedFrames.add(copy)
        }

        // Step 2: GIF Encoding
        onProgress(0.35f, "Starting high-speed GIF89a compression...")

        val encoder = GifEncoder().apply {
            setDelay(delayMs)
            setRepeat(0) // Infinite loop
            setQuality(10)
        }

        val gifBytes = encoder.encodeSequence(
            frames = processedFrames,
            isBoomerang = isBoomerang
        ) { encProgress, message ->
            val mappedProgress = 0.35f + (encProgress * 0.65f)
            onProgress(mappedProgress, message)
        }

        return@withContext gifBytes
    }
}
