package com.example.liftjournal.ui.navigation

import android.content.pm.ActivityInfo

/**
 * The screen orientation each tab asks for: Progress is always landscape, either way up (SPEC §5.1); the other
 * tabs follow the device.
 */
val Destination.screenOrientation: Int
    get() = when (this) {
        Destination.PROGRESS -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        Destination.JOURNALS, Destination.SETTINGS -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
