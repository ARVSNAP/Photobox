package com.example.photobooth.model

data class LayoutCardCustomization(
    val layoutId: String,
    val customImageUri: String? = null,
    val visualStyle: String = "default", // "default", "custom_image", "blueprint", "neon_glow", "polaroid", "monolith"
    val colorFilter: String = "none",    // "none", "rose", "cyan", "amber", "noir"
    val customTitle: String? = null,
    val customBadge: String? = null
)
