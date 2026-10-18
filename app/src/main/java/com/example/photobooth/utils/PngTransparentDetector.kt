package com.example.photobooth.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import java.io.File

data class TransparentSlot(
    val left: Float,   // normalized 0.0f .. 1.0f
    val top: Float,    // normalized 0.0f .. 1.0f
    val right: Float,  // normalized 0.0f .. 1.0f
    val bottom: Float  // normalized 0.0f .. 1.0f
)

object PngTransparentDetector {
    private const val TAG = "PngTransparentDetector"

    /**
     * Scans a PNG file and automatically detects transparent cutouts/windows.
     * Returns normalized TransparentSlot list (0.0f .. 1.0f).
     */
    fun detectTransparentSlots(
        imagePath: String,
        alphaThreshold: Int = 60,
        minAreaFraction: Float = 0.008f
    ): List<TransparentSlot> {
        val file = File(imagePath)
        if (!file.exists()) return emptyList()

        return try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return emptyList()
            val slots = detectTransparentSlots(bitmap, alphaThreshold, minAreaFraction)
            bitmap.recycle()
            slots
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting transparent slots", e)
            emptyList()
        }
    }

    /**
     * Scans bitmap pixels to identify contiguous transparent regions (alpha < alphaThreshold).
     */
    fun detectTransparentSlots(
        bitmap: Bitmap,
        alphaThreshold: Int = 60,
        minAreaFraction: Float = 0.008f
    ): List<TransparentSlot> {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return emptyList()

        // Adaptive sampling step size (1 to 4 px for high precision)
        val step = Math.max(1, Math.min(width, height) / 350)
        val sampledW = width / step
        val sampledH = height / step

        val visited = BooleanArray(sampledW * sampledH)
        val rawBoxes = mutableListOf<Rect>()

        val minPixels = (sampledW * sampledH * minAreaFraction).toInt()

        for (sy in 0 until sampledH) {
            for (sx in 0 until sampledW) {
                val index = sy * sampledW + sx
                if (visited[index]) continue

                val px = (sx * step).coerceAtMost(width - 1)
                val py = (sy * step).coerceAtMost(height - 1)
                val pixel = bitmap.getPixel(px, py)
                val alpha = (pixel shr 24) and 0xFF

                if (alpha < alphaThreshold) {
                    // BFS Flood Fill to find extent of transparent cutout
                    var minX = sx
                    var maxX = sx
                    var minY = sy
                    var maxY = sy
                    var count = 0
                    var touchesCanvasBorder = false

                    val queue = IntArray(sampledW * sampledH)
                    var head = 0
                    var tail = 0

                    queue[tail++] = index
                    visited[index] = true

                    while (head < tail) {
                        val curr = queue[head++]
                        val cx = curr % sampledW
                        val cy = curr / sampledW
                        count++

                        if (cx == 0 || cx == sampledW - 1 || cy == 0 || cy == sampledH - 1) {
                            touchesCanvasBorder = true
                        }

                        if (cx < minX) minX = cx
                        if (cx > maxX) maxX = cx
                        if (cy < minY) minY = cy
                        if (cy > maxY) maxY = cy

                        // Check 4-connected neighbors
                        val neighbors = intArrayOf(
                            if (cx > 0) curr - 1 else -1,
                            if (cx < sampledW - 1) curr + 1 else -1,
                            if (cy > 0) curr - sampledW else -1,
                            if (cy < sampledH - 1) curr + sampledW else -1
                        )

                        for (n in neighbors) {
                            if (n in 0 until (sampledW * sampledH) && !visited[n]) {
                                val nx = n % sampledW
                                val ny = n / sampledW
                                val nPx = (nx * step).coerceAtMost(width - 1)
                                val nPy = (ny * step).coerceAtMost(height - 1)
                                val nAlpha = (bitmap.getPixel(nPx, nPy) shr 24) and 0xFF

                                if (nAlpha < alphaThreshold) {
                                    visited[n] = true
                                    queue[tail++] = n
                                }
                            }
                        }
                    }

                    val boxW = (maxX - minX + 1) * step
                    val boxH = (maxY - minY + 1) * step
                    val isEntireCanvas = (boxW >= width * 0.92f && boxH >= height * 0.92f)

                    // Ignore regions that are the outer canvas border background
                    if (count >= minPixels && !isEntireCanvas) {
                        // Refine boundaries to 1:1 pixel accuracy
                        val refinedRect = refineRectToExactPixelEdges(
                            bitmap = bitmap,
                            approxLeft = minX * step,
                            approxTop = minY * step,
                            approxRight = Math.min(width, (maxX + 1) * step),
                            approxBottom = Math.min(height, (maxY + 1) * step),
                            alphaThreshold = alphaThreshold
                        )
                        rawBoxes.add(refinedRect)
                    }
                }
            }
        }

        // Iterative Box Merging until stable
        var mergedBoxes = mergeOverlappingBoxes(rawBoxes, width, height)
        var previousSize = -1
        while (mergedBoxes.size != previousSize) {
            previousSize = mergedBoxes.size
            mergedBoxes = mergeOverlappingBoxes(mergedBoxes, width, height)
        }

        // Fallback: If no transparent slots detected, attempt Chroma/Key / Solid Color window detection
        if (mergedBoxes.isEmpty()) {
            mergedBoxes = detectSolidColorSlots(bitmap)
        }

        // Filter out tiny noise boxes
        val validBoxes = mergedBoxes.filter { rect ->
            val area = rect.width() * rect.height()
            area >= (width * height * minAreaFraction)
        }.toMutableList()

        // Sort top-to-bottom, left-to-right cleanly
        validBoxes.sortWith { r1, r2 ->
            val yDiff = Math.abs(r1.top - r2.top)
            if (yDiff < height * 0.05f) {
                r1.left.compareTo(r2.left)
            } else {
                r1.top.compareTo(r2.top)
            }
        }

        // Convert to normalized TransparentSlot (0.0f..1.0f)
        val result = validBoxes.map { rect ->
            TransparentSlot(
                left = (rect.left.toFloat() / width).coerceIn(0f, 1f),
                top = (rect.top.toFloat() / height).coerceIn(0f, 1f),
                right = (rect.right.toFloat() / width).coerceIn(0f, 1f),
                bottom = (rect.bottom.toFloat() / height).coerceIn(0f, 1f)
            )
        }

        Log.d(TAG, "Highly accurate detection found ${result.size} slots in PNG: $result")
        return result
    }

    /**
     * Refines bounding rect coordinates to exact 1:1 pixel boundaries.
     */
    private fun refineRectToExactPixelEdges(
        bitmap: Bitmap,
        approxLeft: Int,
        approxTop: Int,
        approxRight: Int,
        approxBottom: Int,
        alphaThreshold: Int
    ): Rect {
        val width = bitmap.width
        val height = bitmap.height

        var exactLeft = approxLeft
        var exactTop = approxTop
        var exactRight = approxRight
        var exactBottom = approxBottom

        // Expand/shrink left edge
        while (exactLeft > 0) {
            var hasTransparentPixel = false
            for (y in exactTop until exactBottom) {
                val alpha = (bitmap.getPixel(exactLeft - 1, y) shr 24) and 0xFF
                if (alpha < alphaThreshold) {
                    hasTransparentPixel = true
                    break
                }
            }
            if (hasTransparentPixel) exactLeft-- else break
        }

        // Expand/shrink right edge
        while (exactRight < width) {
            var hasTransparentPixel = false
            for (y in exactTop until exactBottom) {
                val alpha = (bitmap.getPixel(exactRight, y) shr 24) and 0xFF
                if (alpha < alphaThreshold) {
                    hasTransparentPixel = true
                    break
                }
            }
            if (hasTransparentPixel) exactRight++ else break
        }

        // Expand/shrink top edge
        while (exactTop > 0) {
            var hasTransparentPixel = false
            for (x in exactLeft until exactRight) {
                val alpha = (bitmap.getPixel(x, exactTop - 1) shr 24) and 0xFF
                if (alpha < alphaThreshold) {
                    hasTransparentPixel = true
                    break
                }
            }
            if (hasTransparentPixel) exactTop-- else break
        }

        // Expand/shrink bottom edge
        while (exactBottom < height) {
            var hasTransparentPixel = false
            for (x in exactLeft until exactRight) {
                val alpha = (bitmap.getPixel(x, exactBottom) shr 24) and 0xFF
                if (alpha < alphaThreshold) {
                    hasTransparentPixel = true
                    break
                }
            }
            if (hasTransparentPixel) exactBottom++ else break
        }

        return Rect(exactLeft, exactTop, exactRight, exactBottom)
    }

    /**
     * Iteratively merges adjacent or slightly overlapping boxes.
     */
    private fun mergeOverlappingBoxes(boxes: List<Rect>, width: Int, height: Int): MutableList<Rect> {
        val result = mutableListOf<Rect>()
        val toleranceX = (width * 0.02f).toInt()
        val toleranceY = (height * 0.02f).toInt()

        for (b in boxes) {
            var merged = false
            for (i in result.indices) {
                val existing = result[i]
                val expandedExisting = Rect(
                    existing.left - toleranceX,
                    existing.top - toleranceY,
                    existing.right + toleranceX,
                    existing.bottom + toleranceY
                )
                if (Rect.intersects(expandedExisting, b)) {
                    result[i] = Rect(
                        Math.min(existing.left, b.left),
                        Math.min(existing.top, b.top),
                        Math.max(existing.right, b.right),
                        Math.max(existing.bottom, b.bottom)
                    )
                    merged = true
                    break
                }
            }
            if (!merged) {
                result.add(Rect(b))
            }
        }
        return result
    }

    /**
     * Fallback detector for frames using solid key colors (e.g. pure green #00FF00 or pure white #FFFFFF windows).
     */
    private fun detectSolidColorSlots(bitmap: Bitmap): MutableList<Rect> {
        val width = bitmap.width
        val height = bitmap.height
        val results = mutableListOf<Rect>()

        // Check center region pixels for solid white or pure green placeholders
        val step = Math.max(2, Math.min(width, height) / 100)
        val sampledW = width / step
        val sampledH = height / step

        val visited = BooleanArray(sampledW * sampledH)

        for (sy in 1 until sampledH - 1) {
            for (sx in 1 until sampledW - 1) {
                val index = sy * sampledW + sx
                if (visited[index]) continue

                val px = (sx * step).coerceAtMost(width - 1)
                val py = (sy * step).coerceAtMost(height - 1)
                val pixel = bitmap.getPixel(px, py)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // Check for pure green (0,255,0) or pure white (255,255,255) window placeholders
                val isChromaGreen = (g > 200 && r < 50 && b < 50)
                val isSolidWhite = (r > 250 && g > 250 && b > 250)

                if (isChromaGreen || isSolidWhite) {
                    var minX = sx
                    var maxX = sx
                    var minY = sy
                    var maxY = sy
                    var count = 0

                    val queue = IntArray(sampledW * sampledH)
                    var head = 0
                    var tail = 0

                    queue[tail++] = index
                    visited[index] = true

                    while (head < tail) {
                        val curr = queue[head++]
                        val cx = curr % sampledW
                        val cy = curr / sampledW
                        count++

                        if (cx < minX) minX = cx
                        if (cx > maxX) maxX = cx
                        if (cy < minY) minY = cy
                        if (cy > maxY) maxY = cy

                        val neighbors = intArrayOf(
                            if (cx > 0) curr - 1 else -1,
                            if (cx < sampledW - 1) curr + 1 else -1,
                            if (cy > 0) curr - sampledW else -1,
                            if (cy < sampledH - 1) curr + sampledW else -1
                        )

                        for (n in neighbors) {
                            if (n in 0 until (sampledW * sampledH) && !visited[n]) {
                                val nx = n % sampledW
                                val ny = n / sampledW
                                val nPx = (nx * step).coerceAtMost(width - 1)
                                val nPy = (ny * step).coerceAtMost(height - 1)
                                val nPixel = bitmap.getPixel(nPx, nPy)
                                val nR = (nPixel shr 16) and 0xFF
                                val nG = (nPixel shr 8) and 0xFF
                                val nB = nPixel and 0xFF

                                val matchesColor = if (isChromaGreen) {
                                    (nG > 180 && nR < 70 && nB < 70)
                                } else {
                                    (nR > 240 && nG > 240 && nB > 240)
                                }

                                if (matchesColor) {
                                    visited[n] = true
                                    queue[tail++] = n
                                }
                            }
                        }
                    }

                    if (count >= (sampledW * sampledH * 0.015f)) {
                        results.add(
                            Rect(
                                minX * step,
                                minY * step,
                                Math.min(width, (maxX + 1) * step),
                                Math.min(height, (maxY + 1) * step)
                            )
                        )
                    }
                }
            }
        }
        return results
    }
}

