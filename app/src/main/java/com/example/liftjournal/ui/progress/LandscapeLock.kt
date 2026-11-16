package com.example.liftjournal.ui.progress

import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * Turns the screen to landscape (either way up) while this is in the composition, and puts the previous
 * orientation back when it leaves, e.g. when the user switches tabs (SPEC §5.1).
 */
@Composable
fun LockLandscapeWhileShown() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity) {
        val previous = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { activity.requestedOrientation = previous }
    }
}
