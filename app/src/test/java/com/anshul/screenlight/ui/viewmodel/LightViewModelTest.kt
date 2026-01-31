package com.anshul.screenlight.ui.viewmodel

import com.anshul.screenlight.data.model.ScreenSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LightViewModelTest {
    @Test
    fun `LightUiState default is not initialized`() {
        val state = LightUiState()
        assertFalse(state.isInitialized)
    }

    @Test
    fun `LightUiState default has no battery warning`() {
        val state = LightUiState()
        assertFalse(state.showBatteryWarning)
    }

    @Test
    fun `LightUiState default is not low battery`() {
        val state = LightUiState()
        assertFalse(state.isLowBattery)
    }

    @Test
    fun `LightUiState uses default settings`() {
        val state = LightUiState()
        assertEquals(ScreenSettings.DEFAULT_BRIGHTNESS, state.settings.brightness, 0.001f)
        assertEquals(ScreenSettings.DEFAULT_COLOR_TEMPERATURE, state.settings.colorTemperature, 0.001f)
    }

    @Test
    fun `GestureState default is not volume held`() {
        val state = GestureState()
        assertFalse(state.isVolumeHeld)
    }

    @Test
    fun `GestureState default is light on`() {
        val state = GestureState()
        assertTrue(state.isLightOn)
    }

    @Test
    fun `GestureState default should not close app`() {
        val state = GestureState()
        assertFalse(state.shouldCloseApp)
    }

    @Test
    fun `GestureState default initial tilt values are null`() {
        val state = GestureState()
        assertEquals(null, state.initialPitch)
        assertEquals(null, state.initialRoll)
    }

    @Test
    fun `GestureState default color temperature is 1`() {
        val state = GestureState()
        assertEquals(1.0f, state.initialColorTemperature, 0.001f)
    }
}
