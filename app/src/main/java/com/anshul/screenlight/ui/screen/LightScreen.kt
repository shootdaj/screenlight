package com.anshul.screenlight.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * - Translucent controls (tap to show/hide)
 */
@Composable
fun LightScreen(
    viewModel: LightViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    var showControls by remember { mutableStateOf(false) }

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

    // Full-screen colored box with tap/double-tap detection
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onScreenDoubleTap()
                    }
                )
            }
    ) {
        // Translucent controls overlay
        if (showControls) {
            LightControls(
                brightness = uiState.settings.brightness,
                currentColorIndex = LightViewModel.COLOR_PRESETS.indexOfFirst {
                    it == uiState.settings.colorArgb
                }.coerceAtLeast(0),
                onBrightnessChange = viewModel::updateBrightness,
                onColorChange = { index ->
                    viewModel.updateColor(LightViewModel.COLOR_PRESETS[index])
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Show battery warning if needed
    if (uiState.showBatteryWarning) {
        BatteryWarningDialog(
            onDismiss = viewModel::dismissBatteryWarning
        )
    }
}

/**
 * Translucent controls for brightness and color.
 */
@Composable
private fun LightControls(
    brightness: Float,
    currentColorIndex: Int,
    onBrightnessChange: (Float) -> Unit,
    onColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = {}) // Consume taps to prevent toggling controls
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brightness slider
        Text(
            text = "Brightness",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
        Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            valueRange = 0.05f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.8f),
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )

        // Color picker
        Text(
            text = "Color",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LightViewModel.COLOR_PRESETS.forEachIndexed { index, colorInt ->
                val isSelected = index == currentColorIndex
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .padding(if (isSelected) 3.dp else 0.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt))
                        .clickable { onColorChange(index) }
                )
            }
        }

        // Hint text
        Text(
            text = "Tap screen to hide \u2022 Double-tap to toggle",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
