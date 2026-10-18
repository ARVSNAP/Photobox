package com.example.photobooth.utils

import android.content.Context
import android.net.Uri
import com.example.photobooth.model.LoopAnimationConfig
import java.io.File
import java.io.FileOutputStream

object LoopAnimationStorage {
    private const val PREFS_NAME = "photobooth_loop_prefs"
    private const val KEY_IS_ENABLED = "loop_is_enabled"
    private const val KEY_PRESET = "loop_preset"
    private const val KEY_FILE_URI = "loop_file_uri"
    private const val KEY_FILE_TYPE = "loop_file_type"
    private const val KEY_SPEED = "loop_speed"
    private const val KEY_CUSTOM_NAME = "loop_custom_name"
    private const val KEY_SCALE_MODE = "loop_scale_mode"

    private const val PERMANENT_FILE_PREFIX = "permanent_custom_loop"

    fun saveLoopConfig(context: Context, config: LoopAnimationConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_ENABLED, config.isEnabled)
            .putString(KEY_PRESET, config.animationPreset)
            .putString(KEY_FILE_URI, config.externalFileUri)
            .putString(KEY_FILE_TYPE, config.externalFileType)
            .putFloat(KEY_SPEED, config.customSpeed)
            .putString(KEY_CUSTOM_NAME, config.customPresetName)
            .putString(KEY_SCALE_MODE, config.scaleMode)
            .apply()
    }

    fun loadLoopConfig(context: Context): LoopAnimationConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(KEY_IS_ENABLED, true)
        var preset = prefs.getString(KEY_PRESET, "Neon Pulse") ?: "Neon Pulse"
        var fileUri = prefs.getString(KEY_FILE_URI, null)
        var fileType = prefs.getString(KEY_FILE_TYPE, null)
        val speed = prefs.getFloat(KEY_SPEED, 1.0f)
        val customName = prefs.getString(KEY_CUSTOM_NAME, "Custom Loop") ?: "Custom Loop"
        val scaleMode = prefs.getString(KEY_SCALE_MODE, "fit") ?: "fit"

        // Verify if saved file exists in internal storage
        val existingPermanentFile = getSavedCustomLoopFile(context)
        if (existingPermanentFile != null) {
            // Always keep the valid permanent URI available
            if (fileUri == null || !File(Uri.parse(fileUri).path ?: "").exists()) {
                fileUri = existingPermanentFile.first.toString()
                fileType = existingPermanentFile.second
            }
        } else {
            // If the file was removed from storage, fall back to Neon Pulse if preset was Custom
            if (preset.startsWith("Custom")) {
                preset = "Neon Pulse"
                fileUri = null
                fileType = null
            }
        }

        return LoopAnimationConfig(
            isEnabled = isEnabled,
            animationPreset = preset,
            externalFileUri = fileUri,
            externalFileType = fileType,
            customSpeed = speed,
            customPresetName = customName,
            scaleMode = scaleMode
        )
    }

    /**
     * Copies the content from [sourceUri] into internal permanent storage,
     * ensuring it survives process death, app restarts, and permission revokes.
     */
    fun saveExternalFilePermanently(context: Context, sourceUri: Uri, fileType: String): Uri? {
        return try {
            val extension = if (fileType.lowercase().contains("mp4")) "mp4" else "gif"
            val targetFile = File(context.filesDir, "$PERMANENT_FILE_PREFIX.$extension")

            // Remove previous permanent file of opposite extension if any
            val oppositeExt = if (extension == "mp4") "gif" else "mp4"
            val oldFile = File(context.filesDir, "$PERMANENT_FILE_PREFIX.$oppositeExt")
            if (oldFile.exists()) {
                oldFile.delete()
            }

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            Uri.fromFile(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Checks if a permanent custom loop file exists in internal storage.
     * Returns Pair(Uri, fileType) if present.
     */
    fun getSavedCustomLoopFile(context: Context): Pair<Uri, String>? {
        val gifFile = File(context.filesDir, "$PERMANENT_FILE_PREFIX.gif")
        if (gifFile.exists() && gifFile.length() > 0) {
            return Pair(Uri.fromFile(gifFile), "gif")
        }
        val mp4File = File(context.filesDir, "$PERMANENT_FILE_PREFIX.mp4")
        if (mp4File.exists() && mp4File.length() > 0) {
            return Pair(Uri.fromFile(mp4File), "mp4")
        }
        return null
    }

    /**
     * Deletes any saved custom loop files and clears associated preferences.
     */
    fun deleteSavedCustomLoop(context: Context) {
        val gifFile = File(context.filesDir, "$PERMANENT_FILE_PREFIX.gif")
        if (gifFile.exists()) gifFile.delete()
        val mp4File = File(context.filesDir, "$PERMANENT_FILE_PREFIX.mp4")
        if (mp4File.exists()) mp4File.delete()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_FILE_URI)
            .remove(KEY_FILE_TYPE)
            .putString(KEY_PRESET, "Neon Pulse")
            .apply()
    }
}
