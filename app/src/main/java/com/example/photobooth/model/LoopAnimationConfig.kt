package com.example.photobooth.model

data class LoopAnimationConfig(
    val isEnabled: Boolean = true,
    val animationPreset: String = "Neon Pulse", // "Neon Pulse", "Disco Stars", "Retro Scanline", "Custom Loop"
    val externalFileUri: String? = null, // URI of selected external GIF or MP4 file
    val externalFileType: String? = null, // "gif" or "mp4"
    val customSpeed: Float = 1.0f,
    val customPresetName: String = "Custom Loop",
    val scaleMode: String = "fit" // "fit" (Full screen, intact / tidak terpotong), "fill" (100% full screen stretch), "crop" (Zoom)
)
