package com.example.liftjournal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.liftjournal.domain.settings.ThemeMode

/** Picks the palette for Progress and Goals & Settings from the theme setting. */
@Composable
fun paletteFor(themeMode: ThemeMode): Palette = when (themeMode) {
    ThemeMode.LIGHT -> LightPalette
    ThemeMode.DARK -> DarkPalette
    ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkPalette else LightPalette
}

/** Monochrome Material 3 theme built from a [Palette]. There are no colour accents anywhere. */
@Composable
fun LiftJournalTheme(palette: Palette, content: @Composable () -> Unit) {
    val isDark = palette.background == DarkPalette.background
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    val colorScheme = base.copy(
        primary = palette.foreground,
        onPrimary = palette.background,
        primaryContainer = palette.soft,
        onPrimaryContainer = palette.foreground,
        secondary = palette.foreground,
        onSecondary = palette.background,
        secondaryContainer = palette.soft,
        onSecondaryContainer = palette.foreground,
        tertiary = palette.foreground,
        background = palette.background,
        onBackground = palette.foreground,
        surface = palette.background,
        onSurface = palette.foreground,
        surfaceVariant = palette.soft,
        onSurfaceVariant = palette.muted,
        surfaceContainer = palette.background,
        surfaceContainerHigh = palette.background,
        surfaceContainerHighest = palette.soft,
        surfaceContainerLow = palette.background,
        surfaceContainerLowest = palette.background,
        inverseSurface = palette.foreground,
        inverseOnSurface = palette.background,
        inversePrimary = palette.background,
        outline = palette.faint,
        outlineVariant = palette.line,
        error = palette.foreground,
        onError = palette.background,
    )
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, typography = LiftJournalTypography, content = content)
    }
}
