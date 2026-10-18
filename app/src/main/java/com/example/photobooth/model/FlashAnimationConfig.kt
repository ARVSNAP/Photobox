package com.example.photobooth.model

enum class FlashStyle(val displayName: String, val description: String) {
    SOLID_BURST("Solid Burst", "Layar menyala penuh instan dan memudar halus"),
    RING_RIPPLE("Ring Light Ripple", "Gelombang cincin cahaya memancar dari tengah"),
    NEON_EDGE("Neon Border Glow", "Bingkai neon menyala tajam di tepian layar"),
    DOUBLE_STROBE("Double Strobe", "Dua kilatan cepat berturut-turut seperti blitz studio"),
    SOFT_DIFFUSE("Soft Studio Diffuser", "Peredupan lembut sinematik dengan gradasi hangat")
}

data class FlashColorOption(
    val name: String,
    val colorHex: Long
)

val FLASH_COLOR_PRESETS = listOf(
    FlashColorOption("Pure White", 0xFFFFFFFF),
    FlashColorOption("Warm Studio", 0xFFFFE5B4),
    FlashColorOption("Neon Rose", 0xFFFF2A5F),
    FlashColorOption("Cyber Cyan", 0xFF00E5FF),
    FlashColorOption("Electric Violet", 0xFF8A2BE2),
    FlashColorOption("Golden Sunset", 0xFFFFB703),
    FlashColorOption("Emerald Aura", 0xFF10B981)
)

data class FlashAnimationConfig(
    val style: FlashStyle = FlashStyle.SOLID_BURST,
    val colorHex: Long = 0xFFFFFFFF,
    val durationMillis: Int = 300,
    val maxIntensity: Float = 0.95f,
    val playSoundEffect: Boolean = true
)
