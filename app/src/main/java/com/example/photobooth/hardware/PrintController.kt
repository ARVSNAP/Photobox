package com.example.photobooth.hardware

import android.content.Context
import android.graphics.Bitmap
import androidx.print.PrintHelper
import java.io.OutputStream
import java.net.Socket

class PrintController(private val context: Context) {

    fun printWithAndroidPrintHelper(
        bitmap: Bitmap,
        jobName: String = "PhotoBooth Print",
        scaleModeVal: Int = PrintHelper.SCALE_MODE_FIT,
        colorModeVal: Int = PrintHelper.COLOR_MODE_COLOR,
        orientationVal: Int = PrintHelper.ORIENTATION_PORTRAIT
    ) {
        val printHelper = PrintHelper(context).apply {
            scaleMode = scaleModeVal
            colorMode = colorModeVal
            orientation = orientationVal
        }
        printHelper.printBitmap(jobName, bitmap)
    }

    fun sendEscPosCommands(printerIp: String, port: Int = 9100, bitmap: Bitmap): Boolean {
        return try {
            Socket(printerIp, port).use { socket ->
                val outputStream: OutputStream = socket.getOutputStream()
                outputStream.write(byteArrayOf(0x1B, 0x40))
                outputStream.write(byteArrayOf(0x1B, 0x61, 0x01))
                outputStream.write("=== PHOTO BOOTH KIOSK ===\n\n".toByteArray())
                outputStream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
                outputStream.flush()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
