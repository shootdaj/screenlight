package com.anshul.screenlight.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anshul.screenlight.ui.components.BatteryWarningDialog
import com.anshul.screenlight.ui.components.ImmersiveMode
import com.anshul.screenlight.ui.components.KeepScreenOn
import com.anshul.screenlight.ui.viewmodel.LightViewModel

/**
 * Full-screen light display with smooth color transitions.
 *
 * Features:
 * - Animated color transitions (300ms tween)
 * - Screen wake lock (prevents sleep)
 * - Immersive mode (hides system bars)
 * - Battery warning when low
 */
@Composable
fun LightScreen(
    viewModel: LightViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Enable lifecycle-aware features
    KeepScreenOn()
    ImmersiveMode()

    // Apply brightness as alpha to the color
    val targetColor = Color(uiState.settings.colorArgb).copy(
        alpha = uiState.settings.brightness
    )

    // Animate color changes smoothly (including brightness via alpha)
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "light_color"
    )

    // Full-screen colored box
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedColor)
    )

    // Show battery warning if needed
    if (uiState.showBatteryWarning) {
        BatteryWarningDialog(
            onDismiss = viewModel::dismissBatteryWarning
        )
    }
}
