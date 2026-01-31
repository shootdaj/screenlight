package com.anshul.screenlight.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anshul.screenlight.ui.components.BatteryWarningDialog
import com.anshul.screenlight.ui.components.ImmersiveMode
import com.anshul.screenlight.ui.components.KeepScreenOn
import com.anshul.screenlight.ui.viewmodel.LightViewModel

/**
 * Full-screen light display with instant color updates.
 *
 * Features:
 * - Instant color/brightness changes (no animation delay)
 * - X/Y pad control (X = color temp, Y = brightness)
 * - Screen wake lock and immersive mode
 * - Battery warning when low
 */
@Composable
fun LightScreen(
    viewModel: LightViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    var showControls by remember { mutableStateOf(false) }

    KeepScreenOn()
    ImmersiveMode()

    // Get the actual color from temperature (no animation - instant response)
    val colorArgb = uiState.settings.toColor()
    val baseColor = Color(colorArgb)

    // Apply brightness by scaling RGB (instant, no animation)
    val displayColor = Color(
        red = baseColor.red * uiState.settings.brightness,
        green = baseColor.green * uiState.settings.brightness,
        blue = baseColor.blue * uiState.settings.brightness,
        alpha = 1f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(displayColor)
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
        if (showControls) {
            LightControls(
                brightness = uiState.settings.brightness,
                colorTemperature = uiState.settings.colorTemperature,
                onBrightnessChange = viewModel::updateBrightness,
                onColorTemperatureChange = viewModel::updateColorTemperature,
                onBothChange = viewModel::updateBrightnessAndColor,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (uiState.showBatteryWarning) {
        BatteryWarningDialog(
            onDismiss = viewModel::dismissBatteryWarning
        )
    }
}

/**
 * X/Y pad control for brightness and color temperature.
 */
@Composable
private fun LightControls(
    brightness: Float,
    colorTemperature: Float,
    onBrightnessChange: (Float) -> Unit,
    onColorTemperatureChange: (Float) -> Unit,
    onBothChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                // Consume taps to prevent toggling controls
                detectTapGestures { }
            }
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Color / Brightness",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )

        // X/Y Pad
        var padSize by remember { mutableStateOf(IntSize.Zero) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .clip(RoundedCornerShape(12.dp))
                .onSizeChanged { padSize = it }
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF8B0000), // Deep red at left (temp 0)
                            Color(0xFFFFD700), // Gold in middle
                            Color.White        // White at right (temp 1)
                        )
                    )
                )
                .background(
                    // Vertical gradient for brightness visualization
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (padSize.width > 0 && padSize.height > 0) {
                                val newTemp = (offset.x / padSize.width).coerceIn(0f, 1f)
                                val newBrightness = (1f - offset.y / padSize.height).coerceIn(0.05f, 1f)
                                onBothChange(newBrightness, newTemp)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (padSize.width > 0 && padSize.height > 0) {
                                val newTemp = (change.position.x / padSize.width).coerceIn(0f, 1f)
                                val newBrightness = (1f - change.position.y / padSize.height).coerceIn(0.05f, 1f)
                                onBothChange(newBrightness, newTemp)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (padSize.width > 0 && padSize.height > 0) {
                            val newTemp = (offset.x / padSize.width).coerceIn(0f, 1f)
                            val newBrightness = (1f - offset.y / padSize.height).coerceIn(0.05f, 1f)
                            onBothChange(newBrightness, newTemp)
                        }
                    }
                }
        ) {
            // Position indicator
            if (padSize.width > 0 && padSize.height > 0) {
                val indicatorX = (colorTemperature * padSize.width).toInt()
                val indicatorY = ((1f - brightness) * padSize.height).toInt()

                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorX - 12.dp.roundToPx(), indicatorY - 12.dp.roundToPx()) }
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, Color.Black.copy(alpha = 0.5f), CircleShape)
                )
            }
        }

        // Labels
        Text(
            text = "← Red · White →",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )

        Text(
            text = "↑ Bright · ↓ Dim",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )

        Text(
            text = "Tap screen to hide · Double-tap to toggle\nHold volume + tilt to adjust",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
