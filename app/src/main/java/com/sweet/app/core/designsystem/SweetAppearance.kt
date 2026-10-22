package com.sweet.app.core.designsystem

enum class SweetAppearance(val displayName: String, val description: String) {
    SWEET_LIGHT("Sweet Light", "Clean, bright, minimalist default light theme"),
    SWEET_DARK("Sweet Dark", "Dark charcoal backgrounds with low glare"),
    AMOLED_BLACK("AMOLED Black", "Pure #000000 background for OLED screens"),
    MATERIAL_YOU("Material You", "Dynamic colors on API 31+ or tonal seed palette on API 30"),
    MIDNIGHT_BLUE("Midnight Blue", "Deep navy surfaces with electric blue accents"),
    TOKYO_NIGHT("Tokyo Night", "Muted violet and neon lavender aesthetic"),
    NORD("Nord", "Official arctic, north-bluish palette"),
    CATPPUCCIN("Catppuccin", "Soothing pastel theme with Latte, Frappe, Macchiato, Mocha"),
    FOREST("Forest", "Muted emerald green and warm neutral tones"),
    SUNSET("Sunset", "Burnt orange, coral, and warm amber highlights"),
    ROSE("Rose", "Muted rose accents on deep plum or soft cream surfaces"),
    MONOCHROME("Monochrome", "Strictly black, grey, and white with strong contrast"),
    HIGH_CONTRAST("High Contrast", "WCAG AAA compliant high contrast theme"),
    WARM_PAPER("Warm Paper", "Cream and warm off-white reading-oriented theme"),
    CYBERPUNK("Cyberpunk", "Near-black surfaces with neon cyan and magenta accents")
}

enum class CatppuccinVariant(val displayName: String, val isDark: Boolean) {
    LATTE("Latte (Light)", false),
    FRAPPE("Frappe (Dark)", true),
    MACCHIATO("Macchiato (Dark)", true),
    MOCHA("Mocha (Dark)", true)
}

enum class CornerRadiusPreset(val displayName: String) {
    COMPACT("Compact (4dp)"),
    DEFAULT("Default (12dp)"),
    ROUNDED("Rounded (20dp)")
}
