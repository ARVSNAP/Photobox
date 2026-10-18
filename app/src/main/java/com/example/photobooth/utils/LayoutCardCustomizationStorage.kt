package com.example.photobooth.utils

import android.content.Context
import android.net.Uri
import com.example.photobooth.model.LayoutCardCustomization
import java.io.File
import java.io.FileOutputStream

object LayoutCardCustomizationStorage {
    private const val PREFS_NAME = "photobooth_layout_customizations"
    private const val KEY_PREFIX_URI = "uri_"
    private const val KEY_PREFIX_STYLE = "style_"
    private const val KEY_PREFIX_FILTER = "filter_"
    private const val KEY_PREFIX_TITLE = "title_"
    private const val KEY_PREFIX_BADGE = "badge_"

    private fun sanitizeId(layoutId: String): String {
        return layoutId.replace(" ", "_").replace("/", "_").lowercase()
    }

    fun saveCustomImagePermanently(context: Context, layoutId: String, sourceUri: Uri): Uri? {
        return try {
            val safeId = sanitizeId(layoutId)
            val destFile = File(context.filesDir, "custom_layout_icon_${safeId}.jpg")
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

    fun saveCustomization(context: Context, customization: LayoutCardCustomization) {
        val safeId = sanitizeId(customization.layoutId)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PREFIX_URI + safeId, customization.customImageUri)
            .putString(KEY_PREFIX_STYLE + safeId, customization.visualStyle)
            .putString(KEY_PREFIX_FILTER + safeId, customization.colorFilter)
            .putString(KEY_PREFIX_TITLE + safeId, customization.customTitle)
            .putString(KEY_PREFIX_BADGE + safeId, customization.customBadge)
            .apply()
    }

    fun getCustomization(context: Context, layoutId: String): LayoutCardCustomization {
        val safeId = sanitizeId(layoutId)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        var imageUri = prefs.getString(KEY_PREFIX_URI + safeId, null)
        // Verify file existence if it's a file URI
        if (imageUri != null && imageUri.startsWith("file:")) {
            val file = File(Uri.parse(imageUri).path ?: "")
            if (!file.exists()) {
                imageUri = null
            }
        }

        val style = prefs.getString(KEY_PREFIX_STYLE + safeId, "default") ?: "default"
        val filter = prefs.getString(KEY_PREFIX_FILTER + safeId, "none") ?: "none"
        val title = prefs.getString(KEY_PREFIX_TITLE + safeId, null)
        val badge = prefs.getString(KEY_PREFIX_BADGE + safeId, null)

        return LayoutCardCustomization(
            layoutId = layoutId,
            customImageUri = imageUri,
            visualStyle = style,
            colorFilter = filter,
            customTitle = title,
            customBadge = badge
        )
    }

    fun getAllCustomizations(context: Context, layoutIds: List<String>): Map<String, LayoutCardCustomization> {
        return layoutIds.associateWith { layoutId ->
            getCustomization(context, layoutId)
        }
    }

    fun resetCustomization(context: Context, layoutId: String) {
        val safeId = sanitizeId(layoutId)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_PREFIX_URI + safeId)
            .remove(KEY_PREFIX_STYLE + safeId)
            .remove(KEY_PREFIX_FILTER + safeId)
            .remove(KEY_PREFIX_TITLE + safeId)
            .remove(KEY_PREFIX_BADGE + safeId)
            .apply()

        // Clean up internal image file if present
        try {
            val destFile = File(context.filesDir, "custom_layout_icon_${safeId}.jpg")
            if (destFile.exists()) {
                destFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        try {
            context.filesDir.listFiles { file ->
                file.name.startsWith("custom_layout_icon_")
            }?.forEach { it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
