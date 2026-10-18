package com.example.photobooth.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PhotoShareHelper {

    fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val imagesFolder = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(imagesFolder, "photobooth_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveGifToCache(context: Context, gifBytes: ByteArray): Uri? {
        return try {
            val imagesFolder = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(imagesFolder, "photobooth_${System.currentTimeMillis()}.gif")
            FileOutputStream(file).use { out ->
                out.write(gifBytes)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareToWhatsApp(context: Context, bitmap: Bitmap, cloudinaryUrl: String?) {
        try {
            val uri = saveBitmapToCache(context, bitmap)
            val shareText = if (!cloudinaryUrl.isNullOrBlank()) {
                "Halo! Ini hasil foto Photo Booth kami: $cloudinaryUrl"
            } else {
                "Halo! Ini hasil foto Photo Booth kami 📸"
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_TEXT, shareText)
                if (uri != null) {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                setPackage("com.whatsapp")
            }

            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                // WhatsApp package not found, open chooser filtered or general
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    if (uri != null) {
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Bagikan foto via WhatsApp"))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareToEmail(context: Context, bitmap: Bitmap, cloudinaryUrl: String?) {
        try {
            val uri = saveBitmapToCache(context, bitmap)
            val subject = "Foto Studio Photo Booth"
            val bodyText = buildString {
                append("Halo!\n\nTerlampir foto dari sesi Photo Booth Anda.")
                if (!cloudinaryUrl.isNullOrBlank()) {
                    append("\n\nTautan download Cloudinary:\n$cloudinaryUrl")
                }
                append("\n\nTerima kasih telah berfoto bersama kami!")
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, bodyText)
                if (uri != null) {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(Intent.createChooser(intent, "Kirim foto via Email"))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka aplikasi email: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
