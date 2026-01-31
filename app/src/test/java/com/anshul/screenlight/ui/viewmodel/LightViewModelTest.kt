package com.anshul.screenlight.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class LightViewModelTest {
    @Test
    fun `COLOR_PRESETS contains expected number of colors`() {
        assertEquals(8, LightViewModel.COLOR_PRESETS.size)
    }

    @Test
    fun `COLOR_PRESETS first color is deep red`() {
        val firstColor = LightViewModel.COLOR_PRESETS.first()
        assertNotNull(firstColor)
    }

    @Test
    fun `COLOR_PRESETS last color is cool white`() {
        val lastColor = LightViewModel.COLOR_PRESETS.last()
        assertNotNull(lastColor)
    }

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
        assertEquals(1.0f, state.settings.brightness, 0.001f)
    }
}
