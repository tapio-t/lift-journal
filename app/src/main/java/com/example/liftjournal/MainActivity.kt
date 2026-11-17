package com.example.liftjournal

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.liftjournal.ui.navigation.AppShell

/** Single activity hosting the Compose UI. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        useQuickRotation()
        setContent { AppShell() }
    }

    /** Progress switches to landscape and back often; a cut is quicker than the system's turning animation. */
    private fun useQuickRotation() {
        window.attributes = window.attributes.apply {
            rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT
        }
    }
}
