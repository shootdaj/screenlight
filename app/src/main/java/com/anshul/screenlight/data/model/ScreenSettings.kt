package com.anshul.screenlight.data.model

import android.graphics.Color

/**
 * Data class representing screen brightness and color settings.
 *
 * @property brightness Screen brightness level from 0.0 (darkest) to 1.0 (brightest)
 * @property colorTemperature Color temperature from 0.0 (deep red) to 1.0 (pure white)
 */
data class ScreenSettings(
    val brightness: Float = DEFAULT_BRIGHTNESS,
    val colorTemperature: Float = DEFAULT_COLOR_TEMPERATURE
) {
    companion object {
        const val DEFAULT_BRIGHTNESS = 0.5f
        const val DEFAULT_COLOR_TEMPERATURE = 1.0f // Start with white
        const val NIGHT_VISION_TEMPERATURE = 0.0f // Deep red for night vision
    }

    /**
     * Convert color temperature to ARGB color.
     * 0.0 = deep red (139, 0, 0) - night vision
     * 0.5 = gold/warm white (255, 215, 0)
     * 1.0 = pure white (255, 255, 255)
     */
    fun toColor(): Int {
        val t = colorTemperature.coerceIn(0f, 1f)
        return when {
            t <= 0.5f -> {
                // Deep red to gold (0.0 -> 0.5)
                val ratio = t / 0.5f
                val r = (139 + (255 - 139) * ratio).toInt()
                val g = (0 + 215 * ratio).toInt()
                val b = 0
                Color.argb(255, r, g, b)
            }
            else -> {
                // Gold to white (0.5 -> 1.0)
                val ratio = (t - 0.5f) / 0.5f
                val r = 255
                val g = (215 + (255 - 215) * ratio).toInt()
                val b = (0 + 255 * ratio).toInt()
                Color.argb(255, r, g, b)
            }
        }
    }
}
