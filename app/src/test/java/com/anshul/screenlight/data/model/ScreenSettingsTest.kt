package com.anshul.screenlight.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ScreenSettingsTest {
    @Test
    fun `default settings have full brightness`() {
        val settings = ScreenSettings.default()
        assertEquals(1.0f, settings.brightness, 0.001f)
    }

    @Test
    fun `default settings use warm white color`() {
        val settings = ScreenSettings.default()
        assertEquals(ScreenSettings.DEFAULT_COLOR_ARGB, settings.colorArgb)
    }

    @Test
    fun `night vision settings use deep red`() {
        val settings = ScreenSettings.nightVision()
        assertEquals(ScreenSettings.NIGHT_VISION_COLOR_ARGB, settings.colorArgb)
    }

    @Test
    fun `color property converts from ARGB`() {
        val settings = ScreenSettings(colorArgb = 0xFFFF0000.toInt())
        val color = settings.color
        assertNotNull(color)
    }

    @Test
    fun `brightness can be set between 0 and 1`() {
        val settings = ScreenSettings(brightness = 0.5f)
        assertEquals(0.5f, settings.brightness, 0.001f)
    }

    @Test
    fun `night vision color constant is defined`() {
        assertNotNull(ScreenSettings.NIGHT_VISION_COLOR)
        assertEquals(ScreenSettings.NIGHT_VISION_COLOR_ARGB, 0xFF8B0000.toInt())
    }

    @Test
    fun `data class copy works correctly`() {
        val original = ScreenSettings.default()
        val modified = original.copy(brightness = 0.3f)
        assertEquals(0.3f, modified.brightness, 0.001f)
        assertEquals(original.colorArgb, modified.colorArgb)
    }
}
