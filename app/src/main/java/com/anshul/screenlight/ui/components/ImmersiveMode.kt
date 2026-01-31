package com.anshul.screenlight.ui.components

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Composable that enables immersive mode by hiding system bars.
 *
 * Hides both status and navigation bars to prevent OLED burn-in.
 * Automatically restores system bars when composable leaves composition.
 */
@Composable
fun ImmersiveMode() {
    val view = LocalView.current

    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window ?: return@DisposableEffect onDispose {}

        // Configure window to draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.apply {
            // Hide both status and navigation bars
            hide(WindowInsetsCompat.Type.systemBars())
            // Set immersive sticky mode - swipe reveals bars temporarily
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            // Restore system bars
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }
    }
}
