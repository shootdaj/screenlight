package com.anshul.screenlight.ui.viewmodel

import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anshul.screenlight.data.model.ScreenSettings
import com.anshul.screenlight.data.repository.SettingsRepository
import com.anshul.screenlight.data.sensor.AmbientLightManager
import com.anshul.screenlight.data.sensor.BatteryMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for screen light UI.
 *
 * Manages settings, ambient light detection, and battery monitoring.
 */
@HiltViewModel
class LightViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val ambientLightManager: AmbientLightManager,
    private val batteryMonitor: BatteryMonitor
) : ViewModel() {

    companion object {
        /**
         * Brightness cap when battery is low to conserve power.
         */
        private const val LOW_BATTERY_MAX_BRIGHTNESS = 0.3f

        /**
         * Color presets from deep red through warm to cool white.
         * Designed for smooth color temperature transitions.
         */
        val COLOR_PRESETS = listOf(
            Color.argb(255, 139, 0, 0),    // Deep red (night vision)
            Color.argb(255, 255, 69, 0),   // Red-orange
            Color.argb(255, 255, 140, 0),  // Dark orange
            Color.argb(255, 255, 215, 0),  // Gold
            Color.argb(255, 255, 255, 224), // Light yellow
            Color.argb(255, 255, 250, 240), // Floral white (warm)
            Color.argb(255, 255, 255, 255)  // Pure white (cool)
        )
    }

    private val _uiState = MutableStateFlow(LightUiState())
    val uiState: StateFlow<LightUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        checkBatteryStatus()
        checkAmbientLightOnLaunch()
    }

    /**
     * Observe settings repository and update UI state.
     */
    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        settings = settings,
                        isInitialized = true
                    )
                }
            }
        }
    }

    /**
     * Check battery status and apply constraints if low.
     */
    private fun checkBatteryStatus() {
        viewModelScope.launch {
            val isLow = batteryMonitor.isLowBattery() ?: false
            _uiState.update { state ->
                state.copy(
                    isLowBattery = isLow,
                    showBatteryWarning = isLow && !state.batteryWarningDismissed
                )
            }

            // Auto-dim to 30% if battery is low
            if (isLow) {
                val currentBrightness = _uiState.value.settings.brightness
                if (currentBrightness > LOW_BATTERY_MAX_BRIGHTNESS) {
                    settingsRepository.updateBrightness(LOW_BATTERY_MAX_BRIGHTNESS)
                }
            }
        }
    }

    /**
     * Check ambient light on launch - if dark, start in night vision.
     */
    private fun checkAmbientLightOnLaunch() {
        viewModelScope.launch {
            if (!ambientLightManager.hasSensor) return@launch

            val isDark = ambientLightManager.isDark() ?: return@launch
            if (isDark) {
                // Only switch to night vision if not already set to a custom color
                val currentColor = _uiState.value.settings.colorArgb
                if (currentColor == ScreenSettings.DEFAULT_COLOR_ARGB) {
                    settingsRepository.updateColor(ScreenSettings.NIGHT_VISION_COLOR)
                }
            }
        }
    }

    /**
     * Dismiss battery warning dialog.
     */
    fun dismissBatteryWarning() {
        _uiState.update { it.copy(batteryWarningDismissed = true, showBatteryWarning = false) }
    }

    /**
     * Update brightness level.
     */
    fun updateBrightness(brightness: Float) {
        viewModelScope.launch {
            val constrainedBrightness = if (_uiState.value.isLowBattery) {
                brightness.coerceAtMost(LOW_BATTERY_MAX_BRIGHTNESS)
            } else {
                brightness
            }
            settingsRepository.updateBrightness(constrainedBrightness)
        }
    }

    /**
     * Update screen color.
     */
    fun updateColor(colorArgb: Int) {
        viewModelScope.launch {
            settingsRepository.updateColor(colorArgb)
        }
    }
}

/**
 * UI state for light screen.
 */
data class LightUiState(
    val settings: ScreenSettings = ScreenSettings(),
    val isLowBattery: Boolean = false,
    val showBatteryWarning: Boolean = false,
    val batteryWarningDismissed: Boolean = false,
    val isInitialized: Boolean = false
)
