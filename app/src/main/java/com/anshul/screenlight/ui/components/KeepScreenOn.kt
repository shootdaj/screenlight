package com.anshul.screenlight.ui.components

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Composable that keeps the screen on while active.
 *
 * Uses FLAG_KEEP_SCREEN_ON to prevent screen dimming/sleeping.
 * Automatically clears flag when composable leaves composition.
 */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current

    DisposableEffect(Unit) {
        // Set wake flag to keep screen on
        view.keepScreenOn = true

        onDispose {
            // Clear wake flag when composable is disposed
            view.keepScreenOn = false
        }
    }
}
