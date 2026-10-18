package com.example.photobooth.utils

import android.content.Context
import android.net.Uri
import com.example.photobooth.model.*
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream

object BrandingFrameStorage {
    private const val PREFS_NAME = "photobooth_branding_prefs"
    
    fun saveConfig(context: Context, config: BrandingFrameConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("style", config.style.name)
            .putString("borderStyle", config.borderStyle.name)
            .putString("customBorderColorHex", config.customBorderColorHex)
            .putInt("photoCornerRadiusDp", config.photoCornerRadiusDp)
            .putInt("photoBorderThicknessDp", config.photoBorderThicknessDp)
            .putString("headerTitle", config.headerTitle)
            .putString("subtitle", config.subtitle)
            .putString("canvasBgType", config.canvasBgType.name)
            .putString("customBgColorHex", config.customBgColorHex)
            .putString("canvasGradientPreset", config.canvasGradientPreset.name)
            .putString("canvasPattern", config.canvasPattern.name)
            .putString("customTextColorHex", config.customTextColorHex)
            .putInt("canvasSidePaddingDp", config.canvasSidePaddingDp)
            .putInt("canvasPhotoGapDp", config.canvasPhotoGapDp)
            .putString("customBgImageUri", config.customBgImageUri)
            .putString("uploadedBgImageUris", JSONArray(config.uploadedBgImageUris).toString())
            .putBoolean("showSticker", config.showSticker)
            .putString("selectedSticker", config.selectedSticker.name)
            .putString("stickerPosition", config.stickerPosition.name)
            .putString("customStickerText", config.customStickerText)
            .putString("emojiSticker", config.emojiSticker)
            .putBoolean("showCustomLogo", config.showCustomLogo)
            .putString("customLogoImageUri", config.customLogoImageUri)
            .putString("customLogoPosition", config.customLogoPosition.name)
            .putInt("customLogoScalePercent", config.customLogoScalePercent)
            .putString("uploadedLogoImageUris", JSONArray(config.uploadedLogoImageUris).toString())
            .apply()
    }

    fun loadConfig(context: Context): BrandingFrameConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val styleStr = prefs.getString("style", FrameStyle.CLASSIC_WHITE.name) ?: FrameStyle.CLASSIC_WHITE.name
        val style = try { FrameStyle.valueOf(styleStr) } catch (_: Exception) { FrameStyle.CLASSIC_WHITE }
        
        val borderStyleStr = prefs.getString("borderStyle", FrameBorderStyle.SOLID.name) ?: FrameBorderStyle.SOLID.name
        val borderStyle = try { FrameBorderStyle.valueOf(borderStyleStr) } catch (_: Exception) { FrameBorderStyle.SOLID }
        
        val customBorderColorHex = prefs.getString("customBorderColorHex", "") ?: ""
        val photoCornerRadiusDp = prefs.getInt("photoCornerRadiusDp", 16)
        val photoBorderThicknessDp = prefs.getInt("photoBorderThicknessDp", 4)
        
        val headerTitle = prefs.getString("headerTitle", "PHOTO BOOTH 2026") ?: "PHOTO BOOTH 2026"
        val subtitle = prefs.getString("subtitle", "BEST MEMORIES") ?: "BEST MEMORIES"
        
        val canvasBgTypeStr = prefs.getString("canvasBgType", CanvasBgType.SOLID.name) ?: CanvasBgType.SOLID.name
        val canvasBgType = try { CanvasBgType.valueOf(canvasBgTypeStr) } catch (_: Exception) { CanvasBgType.SOLID }
        
        val customBgColorHex = prefs.getString("customBgColorHex", "#FFFFFF") ?: "#FFFFFF"
        
        val canvasGradientPresetStr = prefs.getString("canvasGradientPreset", CanvasGradientPreset.SUNSET_NEON.name) ?: CanvasGradientPreset.SUNSET_NEON.name
        val canvasGradientPreset = try { CanvasGradientPreset.valueOf(canvasGradientPresetStr) } catch (_: Exception) { CanvasGradientPreset.SUNSET_NEON }
        
        val canvasPatternStr = prefs.getString("canvasPattern", CanvasPattern.NONE.name) ?: CanvasPattern.NONE.name
        val canvasPattern = try { CanvasPattern.valueOf(canvasPatternStr) } catch (_: Exception) { CanvasPattern.NONE }
        
        val customTextColorHex = prefs.getString("customTextColorHex", "#1E293B") ?: "#1E293B"
        val canvasSidePaddingDp = prefs.getInt("canvasSidePaddingDp", 50)
        val canvasPhotoGapDp = prefs.getInt("canvasPhotoGapDp", 30)
        
        val customBgImageUri = prefs.getString("customBgImageUri", null)
        
        val uploadedBgImageUrisStr = prefs.getString("uploadedBgImageUris", "[]") ?: "[]"
        val uploadedBgImageUris = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(uploadedBgImageUrisStr)
            for (i in 0 until jsonArray.length()) {
                val path = jsonArray.getString(i)
                if (path.startsWith("file:")) {
                    val file = File(Uri.parse(path).path ?: "")
                    if (file.exists()) {
                        uploadedBgImageUris.add(path)
                    }
                } else {
                    uploadedBgImageUris.add(path)
                }
            }
        } catch (_: Exception) {}

        val showSticker = prefs.getBoolean("showSticker", false)
        val selectedStickerStr = prefs.getString("selectedSticker", StickerPreset.PARTY_VIBES.name) ?: StickerPreset.PARTY_VIBES.name
        val selectedSticker = try { StickerPreset.valueOf(selectedStickerStr) } catch (_: Exception) { StickerPreset.PARTY_VIBES }
        
        val stickerPositionStr = prefs.getString("stickerPosition", StickerPosition.BOTTOM_CENTER.name) ?: StickerPosition.BOTTOM_CENTER.name
        val stickerPosition = try { StickerPosition.valueOf(stickerPositionStr) } catch (_: Exception) { StickerPosition.BOTTOM_CENTER }
        
        val customStickerText = prefs.getString("customStickerText", "") ?: ""
        val emojiSticker = prefs.getString("emojiSticker", "🎉") ?: "🎉"

        val showCustomLogo = prefs.getBoolean("showCustomLogo", false)
        val customLogoImageUri = prefs.getString("customLogoImageUri", null)
        val customLogoPositionStr = prefs.getString("customLogoPosition", StickerPosition.BOTTOM_CENTER.name) ?: StickerPosition.BOTTOM_CENTER.name
        val customLogoPosition = try { StickerPosition.valueOf(customLogoPositionStr) } catch (_: Exception) { StickerPosition.BOTTOM_CENTER }
        val customLogoScalePercent = prefs.getInt("customLogoScalePercent", 15)

        val uploadedLogoImageUrisStr = prefs.getString("uploadedLogoImageUris", "[]") ?: "[]"
        val uploadedLogoImageUris = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(uploadedLogoImageUrisStr)
            for (i in 0 until jsonArray.length()) {
                val path = jsonArray.getString(i)
                if (path.startsWith("file:")) {
                    val file = File(Uri.parse(path).path ?: "")
                    if (file.exists()) {
                        uploadedLogoImageUris.add(path)
                    }
                } else {
                    uploadedLogoImageUris.add(path)
                }
            }
        } catch (_: Exception) {}

        var verifiedBgImageUri = customBgImageUri
        if (verifiedBgImageUri != null && verifiedBgImageUri.startsWith("file:")) {
            val file = File(Uri.parse(verifiedBgImageUri).path ?: "")
            if (!file.exists()) {
                verifiedBgImageUri = uploadedBgImageUris.firstOrNull()
            }
        }

        var verifiedLogoImageUri = customLogoImageUri
        if (verifiedLogoImageUri != null && verifiedLogoImageUri.startsWith("file:")) {
            val file = File(Uri.parse(verifiedLogoImageUri).path ?: "")
            if (!file.exists()) {
                verifiedLogoImageUri = uploadedLogoImageUris.firstOrNull()
            }
        }

        return BrandingFrameConfig(
            style = style,
            borderStyle = borderStyle,
            customBorderColorHex = customBorderColorHex,
            photoCornerRadiusDp = photoCornerRadiusDp,
            photoBorderThicknessDp = photoBorderThicknessDp,
            headerTitle = headerTitle,
            subtitle = subtitle,
            canvasBgType = canvasBgType,
            customBgColorHex = customBgColorHex,
            canvasGradientPreset = canvasGradientPreset,
            canvasPattern = canvasPattern,
            customTextColorHex = customTextColorHex,
            canvasSidePaddingDp = canvasSidePaddingDp,
            canvasPhotoGapDp = canvasPhotoGapDp,
            customBgImageUri = verifiedBgImageUri,
            uploadedBgImageUris = uploadedBgImageUris,
            showSticker = showSticker,
            selectedSticker = selectedSticker,
            stickerPosition = stickerPosition,
            customStickerText = customStickerText,
            emojiSticker = emojiSticker,
            showCustomLogo = showCustomLogo,
            customLogoImageUri = verifiedLogoImageUri,
            customLogoPosition = customLogoPosition,
            customLogoScalePercent = customLogoScalePercent,
            uploadedLogoImageUris = uploadedLogoImageUris
        )
    }

    fun saveUploadedBgImage(context: Context, sourceUri: Uri): Uri? {
        return try {
            val filename = "custom_bg_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            val destFile = File(context.filesDir, filename)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveUploadedLogoImage(context: Context, sourceUri: Uri): Uri? {
        return try {
            val filename = "custom_logo_${System.currentTimeMillis()}_${(1000..9999).random()}.png"
            val destFile = File(context.filesDir, filename)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun deleteUploadedBgImage(context: Context, path: String) {
        try {
            if (path.startsWith("file:")) {
                val file = File(Uri.parse(path).path ?: "")
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
