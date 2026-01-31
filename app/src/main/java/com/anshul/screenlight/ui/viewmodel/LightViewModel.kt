package com.anshul.screenlight.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anshul.screenlight.data.model.ScreenSettings
import com.anshul.screenlight.data.device.FlashlightController
import com.anshul.screenlight.data.repository.SettingsRepository
import com.anshul.screenlight.data.sensor.AmbientLightManager
import com.anshul.screenlight.data.sensor.BatteryMonitor
import com.anshul.screenlight.data.sensor.TiltGestureManager
import com.anshul.screenlight.data.sensor.TiltSensorType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "LightViewModel"

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
        private const val LOW_BATTERY_MAX_BRIGHTNESS = 0.3f

        // Tilt sensitivity: degrees needed for full range (0 to 1)
        private const val TILT_DEGREES_FOR_FULL_RANGE = 60f
    }

    private val _uiState = MutableStateFlow(LightUiState())
    val uiState: StateFlow<LightUiState> = _uiState.asStateFlow()

    private val _gestureState = MutableStateFlow(GestureState(
        tiltSensorAvailable = tiltGestureManager.hasSensor,
        tiltSensorType = tiltGestureManager.sensorType
    ))
    val gestureState: StateFlow<GestureState> = _gestureState.asStateFlow()

    private var tiltJob: Job? = null

    init {
        Log.d(TAG, "ViewModel init - tilt sensor: ${tiltGestureManager.sensorType}, available: ${tiltGestureManager.hasSensor}")
        observeSettings()
        checkBatteryStatus()
        checkAmbientLightOnLaunch()
        observeTorchState()
    }

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

    private fun checkBatteryStatus() {
        viewModelScope.launch {
            val isLow = batteryMonitor.isLowBattery() ?: false
            _uiState.update { state ->
                state.copy(
                    isLowBattery = isLow,
                    showBatteryWarning = isLow && !state.batteryWarningDismissed
                )
            }

            if (isLow) {
                val currentBrightness = _uiState.value.settings.brightness
                if (currentBrightness > LOW_BATTERY_MAX_BRIGHTNESS) {
                    settingsRepository.updateBrightness(LOW_BATTERY_MAX_BRIGHTNESS)
                }
            }
        }
    }

    private fun checkAmbientLightOnLaunch() {
        viewModelScope.launch {
            if (!ambientLightManager.hasSensor) return@launch

            val isDark = ambientLightManager.isDark() ?: return@launch
            if (isDark) {
                val currentTemp = _uiState.value.settings.colorTemperature
                if (currentTemp == ScreenSettings.DEFAULT_COLOR_TEMPERATURE) {
                    settingsRepository.updateColorTemperature(ScreenSettings.NIGHT_VISION_TEMPERATURE)
                }
            }
        }
    }

    private fun observeTorchState() {
        viewModelScope.launch {
            flashlightController.torchState.collect { enabled ->
                _gestureState.update { it.copy(isTorchOn = enabled) }
            }
        }
    }

    fun dismissBatteryWarning() {
        _uiState.update { it.copy(batteryWarningDismissed = true, showBatteryWarning = false) }
    }

    /**
     * Update brightness level (immediate, no persistence delay).
     */
    fun updateBrightness(brightness: Float) {
        val constrainedBrightness = if (_uiState.value.isLowBattery) {
            brightness.coerceAtMost(LOW_BATTERY_MAX_BRIGHTNESS)
        } else {
            brightness
        }
        // Update local state immediately for instant feedback
        _uiState.update { it.copy(settings = it.settings.copy(brightness = constrainedBrightness)) }
        // Persist asynchronously
        viewModelScope.launch {
            settingsRepository.updateBrightness(constrainedBrightness)
        }
    }

    /**
     * Update color temperature (immediate, no persistence delay).
     */
    fun updateColorTemperature(temperature: Float) {
        val constrainedTemp = temperature.coerceIn(0f, 1f)
        // Update local state immediately for instant feedback
        _uiState.update { it.copy(settings = it.settings.copy(colorTemperature = constrainedTemp)) }
        // Persist asynchronously
        viewModelScope.launch {
            settingsRepository.updateColorTemperature(constrainedTemp)
        }
    }

    /**
     * Update both brightness and color at once (for X/Y pad).
     */
    fun updateBrightnessAndColor(brightness: Float, colorTemperature: Float) {
        val constrainedBrightness = if (_uiState.value.isLowBattery) {
            brightness.coerceAtMost(LOW_BATTERY_MAX_BRIGHTNESS)
        } else {
            brightness
        }
        val constrainedTemp = colorTemperature.coerceIn(0f, 1f)

        // Update local state immediately
        _uiState.update {
            it.copy(settings = it.settings.copy(
                brightness = constrainedBrightness,
                colorTemperature = constrainedTemp
            ))
        }
        // Persist asynchronously
        viewModelScope.launch {
            settingsRepository.updateSettings(constrainedBrightness, constrainedTemp)
        }
    }

    fun onVolumeButtonDown() {
        Log.d(TAG, "onVolumeButtonDown - tilt sensor available: ${tiltGestureManager.hasSensor}")

        val currentBrightness = _uiState.value.settings.brightness
        val currentColorTemp = _uiState.value.settings.colorTemperature

        _gestureState.update {
            it.copy(
                isVolumeHeld = true,
                initialPitch = null,
                initialRoll = null,
                initialBrightness = currentBrightness,
                initialColorTemperature = currentColorTemp
            )
        }

        if (tiltGestureManager.hasSensor) {
            startTiltObservation()
        } else {
            Log.w(TAG, "Tilt gestures disabled - no suitable sensor available")
        }
    }

    fun onVolumeButtonUp() {
        _gestureState.update { it.copy(isVolumeHeld = false) }
        stopTiltObservation()

        val currentBrightness = _uiState.value.settings.brightness
        if (currentBrightness > 0f) {
            _gestureState.update { it.copy(lastNonZeroBrightness = currentBrightness) }
        }
    }

    fun onVolumeButtonTripleClick() {
        flashlightController.toggleTorch()
    }

    fun onVolumeButtonDoubleClick() {
        _gestureState.update { it.copy(shouldCloseApp = true) }
    }

    fun onScreenDoubleTap() {
        viewModelScope.launch {
            val currentlyOn = _gestureState.value.isLightOn
            if (currentlyOn) {
                val current = _uiState.value.settings.brightness
                if (current > 0f) {
                    _gestureState.update { it.copy(lastNonZeroBrightness = current) }
                }
                settingsRepository.updateBrightness(0f)
            } else {
                settingsRepository.updateBrightness(_gestureState.value.lastNonZeroBrightness)
            }
            _gestureState.update { it.copy(isLightOn = !currentlyOn) }
        }
    }

    fun clearCloseAppFlag() {
        _gestureState.update { it.copy(shouldCloseApp = false) }
    }

    /**
     * Continuous tilt observation - works like a knob.
     * Pitch (tilt forward/back) = brightness
     * Roll (tilt left/right) = color temperature
     */
    private fun startTiltObservation() {
        Log.d(TAG, "startTiltObservation called")
        tiltJob?.cancel()
        tiltJob = viewModelScope.launch {
            Log.d(TAG, "Starting to collect tilt data...")
            var eventCount = 0
            tiltGestureManager.observeTilt().collect { tilt ->
                eventCount++
                if (eventCount <= 3) {
                    Log.d(TAG, "Tilt event #$eventCount: pitch=${tilt.pitch}, roll=${tilt.roll}")
                }

                if (!_gestureState.value.isVolumeHeld) {
                    Log.d(TAG, "Volume not held, ignoring tilt")
                    return@collect
                }

                val state = _gestureState.value

                // Capture initial position on first reading
                if (state.initialPitch == null || state.initialRoll == null) {
                    Log.d(TAG, "Capturing initial position: pitch=${tilt.pitch}, roll=${tilt.roll}")
                    _gestureState.update {
                        it.copy(initialPitch = tilt.pitch, initialRoll = tilt.roll)
                    }
                    return@collect
                }

                // Calculate delta from initial position
                val pitchDelta = tilt.pitch - state.initialPitch
                val rollDelta = tilt.roll - state.initialRoll

                // Continuous brightness: pitch delta / degrees = change in brightness
                // Positive pitch (tilt back/up) = brighter
                val brightnessDelta = pitchDelta / TILT_DEGREES_FOR_FULL_RANGE
                val rawBrightness = state.initialBrightness + brightnessDelta
                val newBrightness = rawBrightness.coerceIn(0.05f, 1f)

                // Continuous color: roll delta / degrees = change in color temp
                // Positive roll (tilt right) = warmer (towards red)
                // Negative roll (tilt left) = cooler (towards white)
                val colorDelta = -rollDelta / TILT_DEGREES_FOR_FULL_RANGE
                val rawColorTemp = state.initialColorTemperature + colorDelta
                val newColorTemp = rawColorTemp.coerceIn(0f, 1f)

                // Re-anchor when hitting boundaries so continuous movement works
                // This prevents getting "stuck" at min/max values
                val needsReanchor = rawBrightness != newBrightness || rawColorTemp != newColorTemp
                if (needsReanchor) {
                    _gestureState.update {
                        it.copy(
                            initialPitch = tilt.pitch,
                            initialRoll = tilt.roll,
                            initialBrightness = newBrightness,
                            initialColorTemperature = newColorTemp
                        )
                    }
                }

                updateBrightnessAndColor(newBrightness, newColorTemp)
            }
            Log.d(TAG, "Tilt collection ended after $eventCount events")
        }
    }

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
    val isLightOn: Boolean = true,
    // Tilt sensor availability
    val tiltSensorAvailable: Boolean = false,
    val tiltSensorType: TiltSensorType = TiltSensorType.NONE,
    // For continuous tilt gestures
    val initialPitch: Float? = null,
    val initialRoll: Float? = null,
    val initialBrightness: Float = 0.5f,
    val initialColorTemperature: Float = 1.0f
)
