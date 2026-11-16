package com.example.liftjournal.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The app's black-and-white palette. Names follow the prototype's CSS variables
 * (--bg, --fg, --muted, --faint, --line, --soft, --dim) so the two are easy to compare.
 */
@Immutable
data class Palette(
    val background: Color,
    val foreground: Color,
    val muted: Color,
    val faint: Color,
    val line: Color,
    val soft: Color,
    val dim: Color,
)

val LightPalette = Palette(
    background = Color(0xFFFFFFFF),
    foreground = Color(0xFF000000),
    muted = Color(0xFF6B6B6B),
    faint = Color(0xFFB5B5B5),
    line = Color(0xFFE3E3E3),
    soft = Color(0xFFF3F3F3),
    dim = Color(0xFFC9C9C9),
)

val DarkPalette = Palette(
    background = Color(0xFF000000),
    foreground = Color(0xFFFFFFFF),
    muted = Color(0xFF9A9A9A),
    faint = Color(0xFF555555),
    line = Color(0xFF262626),
    soft = Color(0xFF141414),
    dim = Color(0xFF4A4A4A),
)

/** Journals is always black, with slightly different greys than the dark theme (prototype body.tab-journals). */
val JournalsPalette = Palette(
    background = Color(0xFF000000),
    foreground = Color(0xFFFFFFFF),
    muted = Color(0xFF8C8C8C),
    faint = Color(0xFF444444),
    line = Color(0xFF1F1F1F),
    soft = Color(0xFF1A1A1A),
    dim = Color(0xFF4A4A4A),
)

/** Colours used only by the Journals search field. */
object JournalsSearchColors {
    val field = Color(0xFF1C1C1E)
    val placeholder = Color(0xFF8C8C8C)
    val clearButton = Color(0xFF3A3A3C)
    val tagBorder = Color(0xFF3A3A3A)
    val ghostText = Color(0xFF5C5C5C)
}

val LocalPalette = staticCompositionLocalOf { DarkPalette }
