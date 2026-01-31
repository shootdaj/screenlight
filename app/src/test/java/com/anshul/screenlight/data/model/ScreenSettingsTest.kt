package com.anshul.screenlight.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
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
    fun `default constructor uses default color`() {
        val settings = ScreenSettings()
        assertEquals(ScreenSettings.DEFAULT_COLOR_ARGB, settings.colorArgb)
    }

    @Test
    fun `night vision color constant is defined`() {
        assertNotNull(ScreenSettings.NIGHT_VISION_COLOR)
        // Night vision should be red-ish (high red, low green/blue)
        assertNotEquals(0, ScreenSettings.NIGHT_VISION_COLOR)
    }

    @Test
    fun `brightness can be set between 0 and 1`() {
        val settings = ScreenSettings(brightness = 0.75f)
        assertEquals(0.75f, settings.brightness, 0.001f)
    }

    @Test
    fun `custom colorArgb is stored correctly`() {
        val testColor = 0xFFFF0000.toInt()
        val settings = ScreenSettings(colorArgb = testColor)
        assertEquals(testColor, settings.colorArgb)
    }

    @Test
    fun `data class copy works correctly`() {
        val original = ScreenSettings()
        val modified = original.copy(brightness = 0.3f)
        assertEquals(0.3f, modified.brightness, 0.001f)
        assertEquals(original.colorArgb, modified.colorArgb)
    }

    @Test
    fun `data class equality works`() {
        val settings1 = ScreenSettings(brightness = 0.5f, colorArgb = 0xFF000000.toInt())
        val settings2 = ScreenSettings(brightness = 0.5f, colorArgb = 0xFF000000.toInt())
        assertEquals(settings1, settings2)
    }
}
