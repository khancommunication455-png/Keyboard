package com.stylekeyboard.app.keyboard

import androidx.compose.ui.graphics.Color

/**
 * Built-in keyboard themes (Gboard-style). The active theme is selected from
 * a long-press on the toolbar's theme button, or from the Host App's
 * Appearance screen.
 */
data class KeyboardTheme(
    val id: String,
    val name: String,
    val background: Color,
    val keyBg: Color,
    val keyBgActive: Color,
    val keyFg: Color,
    val keyBorder: Color?,
    val accent: Color,
    val scrimAlpha: Float = 0.0f
) {
    companion object {
        val Charcoal = KeyboardTheme(
            id = "charcoal",
            name = "Charcoal",
            background = Color(0xFF0A0A0A),
            keyBg = Color(0xFF1F1F22),
            keyBgActive = Color(0xFF2A2A2E),
            keyFg = Color(0xFFEDEDED),
            keyBorder = null,
            accent = Color(0xFF06E6FF)
        )

        val Midnight = KeyboardTheme(
            id = "midnight",
            name = "Midnight",
            background = Color(0xFF0B0F1F),
            keyBg = Color(0xFF1A1F33),
            keyBgActive = Color(0xFF242B45),
            keyFg = Color(0xFFDDE3FF),
            keyBorder = null,
            accent = Color(0xFF8B5CF6)
        )

        val Ocean = KeyboardTheme(
            id = "ocean",
            name = "Ocean",
            background = Color(0xFF04212A),
            keyBg = Color(0xFF0F3540),
            keyBgActive = Color(0xFF194B59),
            keyFg = Color(0xFFD7FAFF),
            keyBorder = null,
            accent = Color(0xFF06E6FF)
        )

        val Sunset = KeyboardTheme(
            id = "sunset",
            name = "Sunset",
            background = Color(0xFF1B0A0A),
            keyBg = Color(0xFF2E1414),
            keyBgActive = Color(0xFF45201F),
            keyFg = Color(0xFFFFE3E0),
            keyBorder = null,
            accent = Color(0xFFFF4D9D)
        )

        val Neon = KeyboardTheme(
            id = "neon",
            name = "Neon Border",
            background = Color(0xFF050608),
            keyBg = Color(0xFF000000),
            keyBgActive = Color(0xFF111318),
            keyFg = Color(0xFFE6FFFB),
            keyBorder = Color(0xFF06E6FF),
            accent = Color(0xFF06E6FF),
            scrimAlpha = 0.05f
        )

        val Light = KeyboardTheme(
            id = "light",
            name = "Light",
            background = Color(0xFFE8EAED),
            keyBg = Color(0xFFFFFFFF),
            keyBgActive = Color(0xFFD3D7DC),
            keyFg = Color(0xFF1B1B1B),
            keyBorder = null,
            accent = Color(0xFF1A73E8)
        )

        val all = listOf(Charcoal, Midnight, Ocean, Sunset, Neon, Light)

        fun byId(id: String): KeyboardTheme = all.firstOrNull { it.id == id } ?: Charcoal
    }
}
