package com.example.photobooth.model

data class BrandingFrameConfig(
    // Frame Styling
    val style: FrameStyle = FrameStyle.CLASSIC_WHITE,
    val borderStyle: FrameBorderStyle = FrameBorderStyle.SOLID,
    val customBorderColorHex: String = "",
    val photoCornerRadiusDp: Int = 16,
    val photoBorderThicknessDp: Int = 4,

    // Header & Subtitle
    val headerTitle: String = "PHOTO BOOTH 2026",
    val subtitle: String = "BEST MEMORIES",

    // Canvas Customization
    val canvasBgType: CanvasBgType = CanvasBgType.SOLID,
    val customBgColorHex: String = "#FFFFFF",
    val canvasGradientPreset: CanvasGradientPreset = CanvasGradientPreset.SUNSET_NEON,
    val canvasPattern: CanvasPattern = CanvasPattern.NONE,
    val customTextColorHex: String = "#1E293B",
    val canvasSidePaddingDp: Int = 50,
    val canvasPhotoGapDp: Int = 30,
    val customBgImageUri: String? = null,
    val uploadedBgImageUris: List<String> = emptyList(),

    // Sticker Customization
    val showSticker: Boolean = false,
    val selectedSticker: StickerPreset = StickerPreset.PARTY_VIBES,
    val stickerPosition: StickerPosition = StickerPosition.BOTTOM_CENTER,
    val customStickerText: String = "",
    val emojiSticker: String = "🎉",

    // Custom Logo Image Customization
    val showCustomLogo: Boolean = false,
    val customLogoImageUri: String? = null,
    val customLogoPosition: StickerPosition = StickerPosition.BOTTOM_CENTER,
    val customLogoScalePercent: Int = 15,
    val uploadedLogoImageUris: List<String> = emptyList()
)

enum class FrameStyle(
    val displayName: String,
    val defaultBgHex: String,
    val defaultTextHex: String,
    val defaultBorderHex: String,
    val badgeTag: String
) {
    CLASSIC_WHITE("Studio White", "#FFFFFF", "#0F172A", "#CBD5E1", "STUDIO"),
    DARK_OBSIDIAN("Dark Obsidian", "#0F172A", "#F8FAFC", "#334155", "OBSIDIAN"),
    NEON_CYBER("Neon Cyber", "#070B14", "#00F0FF", "#FF007A", "CYBER"),
    RETRO_CREAM("Retro Cream", "#FFFBEB", "#78350F", "#FDE68A", "RETRO"),
    GOLD_VIP("Gold VIP", "#121212", "#FACC15", "#CA8A04", "VIP"),
    FILM_STRIP("Cinematic Film", "#050505", "#FFFFFF", "#3F3F46", "FILM"),
    PASTEL_DREAM("Pastel Dream", "#FCE7F3", "#831843", "#F472B6", "PASTEL"),
    VINTAGE_POLAROID("Polaroid Vintage", "#FAFAF9", "#1C1917", "#E7E5E4", "POLAROID"),
    COMIC_POP("Comic Pop Art", "#FEF08A", "#000000", "#000000", "POP ART"),
    FLORAL_ROMANCE("Floral Romance", "#FFF1F2", "#9F1239", "#FDA4AF", "ROMANCE"),
    BOOTH_LAB("Boothlab 4", "#F59E0B", "#1E293B", "#FFFFFF", "BOOTH LAB")
}

enum class FrameBorderStyle(val displayName: String) {
    SOLID("Solid Border"),
    DASHED("Dashed Border"),
    DOUBLE_LINE("Double Line"),
    GLOW_OUTLINE("Glow Outline")
}

enum class CanvasBgType(val displayName: String) {
    SOLID("Warna Polos"),
    IMAGE("Gambar Custom")
}

enum class CanvasGradientPreset(
    val displayName: String,
    val startHex: String,
    val endHex: String
) {
    SUNSET_NEON("Sunset Neon", "#FF007A", "#FACC15"),
    CYBER_PUNK("Cyberpunk Cyan", "#00F0FF", "#8B5CF6"),
    DEEP_MIDNIGHT("Deep Midnight", "#0F172A", "#312E81"),
    EMERALD_MINT("Emerald Mint", "#059669", "#06B6D4"),
    ROYAL_GOLD("Royal Gold", "#18181B", "#CA8A04"),
    PASTEL_SUNRISE("Pastel Sunrise", "#FDE68A", "#F472B6")
}

enum class CanvasPattern(val displayName: String) {
    NONE("Clean Smooth"),
    POLKA_DOTS("Polka Dots"),
    DIAGONAL_STRIPES("Diagonal Stripes"),
    STARRY_SPARKLES("Starry Sparkles"),
    HEART_PATTERN("Cute Hearts")
}

enum class StickerPreset(
    val displayName: String,
    val textLabel: String,
    val primaryColorHex: String
) {
    NONE("No Badge", "", "#000000"),
    PARTY_VIBES("Party Vibes", "★ PARTY VIBES ★", "#FF007A"),
    VIP_ACCESS("VIP Access", "⚡ VIP ACCESS ⚡", "#FACC15"),
    BEST_DAY_EVER("Best Day Ever", "✨ BEST DAY EVER ✨", "#00F0FF"),
    LOVE_XOXO("Love & XOXO", "♡ XOXO ♡", "#EC4899"),
    MEMORIES_2026("Memories 2026", "📷 MEMORIES 2026", "#10B981"),
    BIRTHDAY_BASH("Birthday Bash 🎂", "🎂 HAPPY BIRTHDAY 🎂", "#8B5CF6"),
    WEDDING_BLISS("Wedding Bliss 💍", "💍 JUST MARRIED 💍", "#F43F5E"),
    CUSTOM("Custom Text ✏️", "", "#FF007A")
}

enum class StickerPosition(val displayName: String) {
    BOTTOM_CENTER("Bottom Footer"),
    TOP_RIGHT("Top Right Corner"),
    TOP_LEFT("Top Left Corner"),
    FLOATING_CENTER("Center Badge")
}
