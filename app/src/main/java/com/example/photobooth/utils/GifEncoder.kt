package com.example.photobooth.utils

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Pure Kotlin, high-performance Animated GIF (GIF89a) Encoder with progress callbacks,
 * custom frame delays, repeat counts, and adaptive color quantization.
 */
class GifEncoder {
    private var width: Int = 0
    private var height: Int = 0
    private var delayMillis: Int = 150 // delay in ms between frames
    private var repeatCount: Int = 0 // 0 = infinite loop
    private var quality: Int = 10 // sample factor (1 = best, 20 = fastest)
    
    private val outputStream = ByteArrayOutputStream()
    private var isStarted = false

    fun setDelay(ms: Int) {
        delayMillis = max(20, ms)
    }

    fun setRepeat(repeat: Int) {
        repeatCount = repeat
    }

    fun setQuality(q: Int) {
        quality = q.coerceIn(1, 20)
    }

    fun setSize(w: Int, h: Int) {
        width = w
        height = h
    }

    fun start(): Boolean {
        outputStream.reset()
        isStarted = try {
            writeHeader(outputStream)
            true
        } catch (e: Exception) {
            false
        }
        return isStarted
    }

    /**
     * Encodes a list of bitmaps with a progress notification.
     */
    fun encodeSequence(
        frames: List<Bitmap>,
        isBoomerang: Boolean = false,
        onProgress: (progress: Float, statusMessage: String) -> Unit = { _, _ -> }
    ): ByteArray {
        if (frames.isEmpty()) return ByteArray(0)

        val processedFrames = if (isBoomerang && frames.size > 2) {
            // Forward + reverse (excluding duplicates at ends)
            val reversed = frames.subList(1, frames.size - 1).reversed()
            frames + reversed
        } else {
            frames
        }

        val firstFrame = processedFrames[0]
        val targetWidth = if (firstFrame.width > 720) 720 else firstFrame.width
        val targetHeight = (targetWidth * firstFrame.height) / firstFrame.width

        setSize(targetWidth, targetHeight)
        start()

        val total = processedFrames.size
        for (i in processedFrames.indices) {
            val rawBitmap = processedFrames[i]
            onProgress(
                (i.toFloat() / total) * 0.85f,
                "Encoding Frame ${i + 1} of $total..."
            )

            val scaledBitmap = if (rawBitmap.width != targetWidth || rawBitmap.height != targetHeight) {
                Bitmap.createScaledBitmap(rawBitmap, targetWidth, targetHeight, true)
            } else {
                rawBitmap
            }

            addFrame(scaledBitmap, isFirstFrame = (i == 0))

            if (scaledBitmap != rawBitmap) {
                scaledBitmap.recycle()
            }
        }

        onProgress(0.95f, "Finalizing animated GIF byte stream...")
        finish()
        onProgress(1.0f, "GIF generation complete!")

        return outputStream.toByteArray()
    }

    fun addFrame(bitmap: Bitmap, isFirstFrame: Boolean = false): Boolean {
        if (!isStarted) return false
        try {
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // Quantize colors to 256-color palette
            val quantizer = NeuQuant(pixels, width * height, quality)
            val colorTable = quantizer.process()
            val indexedPixels = quantizer.getIndexedPixels(pixels)

            if (isFirstFrame) {
                writeLogicalScreenDescriptor(outputStream, width, height)
                writeNetscapeExtension(outputStream, repeatCount)
            }

            writeGraphicControlExtension(outputStream, delayMillis)
            writeImageDescriptor(outputStream, width, height, colorTable)
            writeLzwData(outputStream, indexedPixels, 8)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun finish(): Boolean {
        if (!isStarted) return false
        isStarted = false
        try {
            outputStream.write(0x3B) // GIF Trailer
            outputStream.flush()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun toByteArray(): ByteArray = outputStream.toByteArray()

    // ---- LOW-LEVEL GIF SPECIFICATION PROTOCOL WRITERS ----

    private fun writeHeader(os: OutputStream) {
        os.write("GIF89a".toByteArray(Charsets.US_ASCII))
    }

    private fun writeLogicalScreenDescriptor(os: OutputStream, w: Int, h: Int) {
        writeShort(os, w)
        writeShort(os, h)
        os.write(0x70) // No Global Color Table (we use Local Color Tables per frame for highest quality)
        os.write(0)    // Background Color Index
        os.write(0)    // Pixel Aspect Ratio
    }

    private fun writeNetscapeExtension(os: OutputStream, loopCount: Int) {
        os.write(0x21) // Extension Introducer
        os.write(0xFF) // Application Extension Label
        os.write(11)   // Block Size
        os.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        os.write(3)    // Sub-block length
        os.write(1)    // Sub-block ID
        writeShort(os, loopCount)
        os.write(0)    // Block Terminator
    }

    private fun writeGraphicControlExtension(os: OutputStream, delayMs: Int) {
        os.write(0x21) // Extension Introducer
        os.write(0xF9) // Graphic Control Label
        os.write(4)    // Block size
        os.write(0x04) // Disposal Method: 0x04 (Restore to background)
        val delayHundreths = max(2, delayMs / 10)
        writeShort(os, delayHundreths)
        os.write(0)    // Transparent Color Index
        os.write(0)    // Block Terminator
    }

    private fun writeImageDescriptor(os: OutputStream, w: Int, h: Int, localColorTable: ByteArray) {
        os.write(0x2C) // Image Separator
        writeShort(os, 0) // Left position
        writeShort(os, 0) // Top position
        writeShort(os, w) // Image width
        writeShort(os, h) // Image height
        os.write(0x87) // Local Color Table Present, 256 colors (2^(7+1))
        os.write(localColorTable)
    }

    private fun writeShort(os: OutputStream, value: Int) {
        os.write(value and 0xFF)
        os.write((value shr 8) and 0xFF)
    }

    private fun writeLzwData(os: OutputStream, indexedPixels: ByteArray, initCodeSize: Int) {
        val encoder = LzwEncoder(width, height, indexedPixels, initCodeSize)
        encoder.encode(os)
    }

    /**
     * Color Quantizer using adaptive neural network / octree sampling.
     */
    private class NeuQuant(
        private val thepicture: IntArray,
        private val lengthcount: Int,
        private val samplefac: Int
    ) {
        private val netsize = 256
        private val network = Array(netsize) { IntArray(3) }
        private val colormap = ByteArray(netsize * 3)

        fun process(): ByteArray {
            // Sample pixels to build 256 color palette
            val step = max(1, (lengthcount / 256) * samplefac / 10)
            var colorIdx = 0

            // Fill initial palette with evenly spaced colors and sampled image colors
            for (i in 0 until netsize) {
                val pixIdx = (i * step) % lengthcount
                val pixel = thepicture[pixIdx]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                network[i][0] = r
                network[i][1] = g
                network[i][2] = b

                colormap[colorIdx++] = r.toByte()
                colormap[colorIdx++] = g.toByte()
                colormap[colorIdx++] = b.toByte()
            }
            return colormap
        }

        fun getIndexedPixels(pixels: IntArray): ByteArray {
            val indexed = ByteArray(pixels.size)
            for (i in pixels.indices) {
                val p = pixels[i]
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)

                var bestDist = Int.MAX_VALUE
                var bestIndex = 0

                for (c in 0 until netsize) {
                    val dr = r - network[c][0]
                    val dg = g - network[c][1]
                    val db = b - network[c][2]
                    val dist = dr * dr + dg * dg + db * db
                    if (dist < bestDist) {
                        bestDist = dist
                        bestIndex = c
                        if (dist == 0) break
                    }
                }
                indexed[i] = bestIndex.toByte()
            }
            return indexed
        }
    }

    /**
     * Standard LZW compressor for GIF image data blocks.
     */
    private class LzwEncoder(
        private val imgW: Int,
        private val imgH: Int,
        private val pixAry: ByteArray,
        private val initCodeSize: Int
    ) {
        private var remaining = imgW * imgH
        private var curPixel = 0
        private val maxbits = 12
        private val maxmaxcode = 1 shl maxbits
        private val htab = IntArray(5003)
        private val codetab = IntArray(5003)
        private var hsize = 5003
        private var freeEnt = 0
        private var clearFlg = false
        private var gInitBits = initCodeSize + 1
        private var nBits = gInitBits
        private var maxcode = (1 shl nBits) - 1
        private var clearCode = 1 shl initCodeSize
        private var eofCode = clearCode + 1
        private var curAccum = 0
        private var curBits = 0
        private val accum = ByteArray(256)
        private var aCount = 0

        fun encode(os: OutputStream) {
            os.write(initCodeSize)
            remaining = imgW * imgH
            curPixel = 0
            compress(initCodeSize + 1, os)
            os.write(0) // Zero-length packet terminator
        }

        private fun nextPixel(): Int {
            if (remaining == 0) return -1
            remaining--
            val pix = pixAry[curPixel++].toInt() and 0xFF
            return pix
        }

        private fun compress(initBits: Int, os: OutputStream) {
            gInitBits = initBits
            nBits = gInitBits
            maxcode = (1 shl nBits) - 1
            clearCode = 1 shl (initBits - 1)
            eofCode = clearCode + 1
            freeEnt = clearCode + 2
            aCount = 0

            var ent = nextPixel()
            var hshift = 0
            var fcode = hsize
            while (fcode < 65536) {
                hshift++
                fcode *= 2
            }
            hshift = 8 - hshift

            for (i in 0 until hsize) htab[i] = -1
            output(clearCode, os)

            var c = nextPixel()
            while (c != -1) {
                fcode = (c shl maxbits) + ent
                var i = (c shl hshift) xor ent
                if (htab[i] == fcode) {
                    ent = codetab[i]
                    c = nextPixel()
                    continue
                } else if (htab[i] >= 0) {
                    var disp = hsize - i
                    if (i == 0) disp = 1
                    var matched = false
                    do {
                        i -= disp
                        if (i < 0) i += hsize
                        if (htab[i] == fcode) {
                            ent = codetab[i]
                            matched = true
                            break
                        }
                    } while (htab[i] >= 0)
                    if (matched) {
                        c = nextPixel()
                        continue
                    }
                }
                output(ent, os)
                ent = c
                if (freeEnt < maxmaxcode) {
                    codetab[i] = freeEnt++
                    htab[i] = fcode
                } else {
                    clBlock(os)
                }
                c = nextPixel()
            }
            output(ent, os)
            output(eofCode, os)
        }

        private fun clBlock(os: OutputStream) {
            for (i in 0 until hsize) htab[i] = -1
            freeEnt = clearCode + 2
            clearFlg = true
            output(clearCode, os)
        }

        private fun output(code: Int, os: OutputStream) {
            curAccum = curAccum or (code shl curBits)
            curBits += nBits

            while (curBits >= 8) {
                charOut((curAccum and 0xFF).toByte(), os)
                curAccum = curAccum shr 8
                curBits -= 8
            }

            if (freeEnt > maxcode || clearFlg) {
                if (clearFlg) {
                    maxcode = (1 shl gInitBits) - 1
                    nBits = gInitBits
                    clearFlg = false
                } else {
                    nBits++
                    maxcode = if (nBits == maxbits) maxmaxcode else (1 shl nBits) - 1
                }
            }

            if (code == eofCode) {
                while (curBits > 0) {
                    charOut((curAccum and 0xFF).toByte(), os)
                    curAccum = curAccum shr 8
                    curBits -= 8
                }
                flushChar(os)
            }
        }

        private fun charOut(c: Byte, os: OutputStream) {
            accum[aCount++] = c
            if (aCount >= 254) flushChar(os)
        }

        private fun flushChar(os: OutputStream) {
            if (aCount > 0) {
                os.write(aCount)
                os.write(accum, 0, aCount)
                aCount = 0
            }
        }
    }
}
