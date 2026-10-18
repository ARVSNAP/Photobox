package com.example.photobooth.model

import android.graphics.Bitmap

enum class WatermarkCorner(val displayName: String, val shortLabel: String) {
    TOP_LEFT("Top Left", "TL"),
    TOP_RIGHT("Top Right", "TR"),
    BOTTOM_LEFT("Bottom Left", "BL"),
    BOTTOM_RIGHT("Bottom Right", "BR")
}

enum class WatermarkPreset(val displayName: String, val subtitle: String) {
    STUDIO_PRO("Studio Pro VIP", "Circular Neon Cyan VIP seal"),
    EVENT_VIP("Event 2026 Badge", "Geometric Rose & Violet emblem"),
    NEON_PARTY("Neon Party Logo", "Glowing Cyberpunk party mark"),
    RETRO_STAMP("Retro Golden Stamp", "Classic elegant photography crest"),
    CUSTOM_UPLOAD("Custom Uploaded PNG", "Operator custom transparent PNG")
}

data class WatermarkConfig(
    val isEnabled: Boolean = false,
    val corner: WatermarkCorner = WatermarkCorner.BOTTOM_RIGHT,
    val preset: WatermarkPreset = WatermarkPreset.STUDIO_PRO,
    val customBitmap: Bitmap? = null,
    val customFileName: String? = null,
    val scalePercent: Float = 0.16f, // 16% of photo width
    val opacity: Float = 0.90f, // 90% opacity
    val marginPercent: Float = 0.03f // 3% margin from photo edges
)
