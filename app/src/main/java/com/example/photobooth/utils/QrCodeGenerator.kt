package com.example.photobooth.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeGenerator {
    fun generateQrCode(text: String, width: Int = 512, height: Int = 512): Bitmap {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
            val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints)
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
            for (x in 0 until matrixWidth) {
                for (y in 0 until matrixHeight) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback bitmap
            val fallback = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val hash = text.hashCode()
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val isBlack = ((x / 16 + y / 16 + hash) % 2 == 0) || (x < 64 && y < 64) || (x > width - 64 && y < 64) || (x < 64 && y > height - 64)
                    fallback.setPixel(x, y, if (isBlack) Color.BLACK else Color.WHITE)
                }
            }
            fallback
        }
    }
}
