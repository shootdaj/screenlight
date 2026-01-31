package com.anshul.screenlight.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreenSettingsTest {
    @Test
    fun `default constructor uses default brightness`() {
        val settings = ScreenSettings()
        assertEquals(ScreenSettings.DEFAULT_BRIGHTNESS, settings.brightness, 0.001f)
    }

    @Test
    fun `default brightness is 50 percent`() {
        assertEquals(0.5f, ScreenSettings.DEFAULT_BRIGHTNESS, 0.001f)
    }

    @Test
    fun `default constructor uses default color temperature`() {
        val settings = ScreenSettings()
        assertEquals(ScreenSettings.DEFAULT_COLOR_TEMPERATURE, settings.colorTemperature, 0.001f)
    }

    @Test
    fun `night vision temperature is defined as 0`() {
        assertEquals(0.0f, ScreenSettings.NIGHT_VISION_TEMPERATURE, 0.001f)
    }

    @Test
    fun `brightness can be set between 0 and 1`() {
        val settings = ScreenSettings(brightness = 0.75f)
        assertEquals(0.75f, settings.brightness, 0.001f)
    }

    @Test
    fun `color temperature is stored correctly`() {
        val settings = ScreenSettings(colorTemperature = 0.5f)
        assertEquals(0.5f, settings.colorTemperature, 0.001f)
    }

    @Test
    fun `data class copy works correctly`() {
        val original = ScreenSettings()
        val modified = original.copy(brightness = 0.3f)
        assertEquals(0.3f, modified.brightness, 0.001f)
        assertEquals(original.colorTemperature, modified.colorTemperature, 0.001f)
    }

    @Test
    fun `data class equality works`() {
        val settings1 = ScreenSettings(brightness = 0.5f, colorTemperature = 0.5f)
        val settings2 = ScreenSettings(brightness = 0.5f, colorTemperature = 0.5f)
        assertEquals(settings1, settings2)
    }

    @Test
    fun `toColor returns red for temperature 0`() {
        val settings = ScreenSettings(colorTemperature = 0.0f)
        val color = settings.toColor()
        // Deep red (139, 0, 0)
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        assertEquals(139, red)
        assertEquals(0, green)
        assertEquals(0, blue)
    }

    @Test
    fun `toColor returns white for temperature 1`() {
        val settings = ScreenSettings(colorTemperature = 1.0f)
        val color = settings.toColor()
        // Pure white (255, 255, 255)
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        assertEquals(255, red)
        assertEquals(255, green)
        assertEquals(255, blue)
    }

    @Test
    fun `toColor returns gold for temperature 0_5`() {
        val settings = ScreenSettings(colorTemperature = 0.5f)
        val color = settings.toColor()
        // Gold (255, 215, 0)
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        assertEquals(255, red)
        assertEquals(215, green)
        assertEquals(0, blue)
    }
}
