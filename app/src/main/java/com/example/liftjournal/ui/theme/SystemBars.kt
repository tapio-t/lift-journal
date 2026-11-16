package com.example.liftjournal.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Edge-to-edge picks status and navigation bar icon colours from the system theme, but the app's own appearance
 * setting can differ (Light while the phone is dark, and Journals is always black). This keeps the icons readable
 * on the current [palette].
 */
@Composable
fun SystemBarIcons(palette: Palette) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val hasLightBackground = palette == LightPalette
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = hasLightBackground
            isAppearanceLightNavigationBars = hasLightBackground
        }
    }
}
