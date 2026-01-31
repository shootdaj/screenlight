package com.anshul.screenlight.ui.viewmodel

import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anshul.screenlight.data.model.ScreenSettings
import com.anshul.screenlight.data.device.FlashlightController
import com.anshul.screenlight.data.repository.SettingsRepository
import com.anshul.screenlight.data.sensor.AmbientLightManager
import com.anshul.screenlight.data.sensor.BatteryMonitor
import com.anshul.screenlight.data.sensor.TiltGestureManager
import com.anshul.screenlight.data.sensor.TiltMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val batteryMonitor: BatteryMonitor,
    private val tiltGestureManager: TiltGestureManager,
    private val flashlightController: FlashlightController
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

    private val _gestureState = MutableStateFlow(GestureState())
    val gestureState: StateFlow<GestureState> = _gestureState.asStateFlow()

    private var tiltJob: Job? = null

    init {
        observeSettings()
        checkBatteryStatus()
        checkAmbientLightOnLaunch()
        observeTorchState()
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
     * Observe torch state from FlashlightController.
     */
    private fun observeTorchState() {
        viewModelScope.launch {
            flashlightController.torchState.collect { enabled ->
                _gestureState.update { it.copy(isTorchOn = enabled) }
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

    /**
     * Handle volume button press down.
     * Start tilt observation for gesture-based brightness/color control.
     */
    fun onVolumeButtonDown() {
        _gestureState.update { it.copy(isVolumeHeld = true) }
        startTiltObservation()
    }

    /**
     * Handle volume button release.
     * Stop tilt observation and save current brightness.
     */
    fun onVolumeButtonUp() {
        _gestureState.update { it.copy(isVolumeHeld = false) }
        stopTiltObservation()

        // Save current brightness if non-zero
        val currentBrightness = _uiState.value.settings.brightness
        if (currentBrightness > 0f) {
            _gestureState.update { it.copy(lastNonZeroBrightness = currentBrightness) }
        }
    }

    /**
     * Handle volume button long press.
     * Toggle LED flashlight.
     */
    fun onVolumeButtonLongPress() {
        flashlightController.toggleTorch()
    }

    /**
     * Handle volume button double click.
     * Signal app should close.
     */
    fun onVolumeButtonDoubleClick() {
        _gestureState.update { it.copy(shouldCloseApp = true) }
    }

    /**
     * Handle screen double tap.
     * Toggle screen light on/off.
     */
    fun onScreenDoubleTap() {
        viewModelScope.launch {
            val currentlyOn = _gestureState.value.isLightOn
            if (currentlyOn) {
                // Turning off - save current brightness
                val current = _uiState.value.settings.brightness
                if (current > 0f) {
                    _gestureState.update { it.copy(lastNonZeroBrightness = current) }
                }
                settingsRepository.updateBrightness(0f)
            } else {
                // Turning on - restore saved brightness
                settingsRepository.updateBrightness(_gestureState.value.lastNonZeroBrightness)
            }
            _gestureState.update { it.copy(isLightOn = !currentlyOn) }
        }
    }

    /**
     * Clear app close flag after Activity reads it.
     */
    fun clearCloseAppFlag() {
        _gestureState.update { it.copy(shouldCloseApp = false) }
    }

    /**
     * Start observing tilt gestures.
     * Maps pitch to brightness and roll to color.
     */
    private fun startTiltObservation() {
        tiltJob?.cancel()
        tiltJob = viewModelScope.launch {
            tiltGestureManager.observeTilt().collect { tilt ->
                val brightness = TiltMapper.pitchToBrightness(tilt.pitch)
                val colorIndex = TiltMapper.rollToColorIndex(tilt.roll, COLOR_PRESETS.size)

                // Only update if gesture mode active
                if (_gestureState.value.isVolumeHeld) {
                    updateBrightness(brightness)
                    updateColor(COLOR_PRESETS[colorIndex])
                }
            }
        }
    }

    /**
     * Stop observing tilt gestures.
     */
    private fun stopTiltObservation() {
        tiltJob?.cancel()
        tiltJob = null
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

/**
 * State for gesture controls.
 */
data class GestureState(
    val isVolumeHeld: Boolean = false,
    val isTorchOn: Boolean = false,
    val shouldCloseApp: Boolean = false,
    val lastNonZeroBrightness: Float = 0.5f,
    val isLightOn: Boolean = true
)
