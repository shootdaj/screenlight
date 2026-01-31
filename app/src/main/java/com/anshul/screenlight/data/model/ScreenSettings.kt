package com.anshul.screenlight.data.model

import android.graphics.Color

/**
 * Data class representing screen brightness and color settings.
 *
 * @property brightness Screen brightness level from 0.0 (darkest) to 1.0 (brightest)
 * @property colorArgb Screen overlay color in ARGB format
 */
data class ScreenSettings(
    val brightness: Float = DEFAULT_BRIGHTNESS,
    val colorArgb: Int = DEFAULT_COLOR_ARGB
) {
    companion object {
        /**
         * Default brightness level (50%)
         */
        const val DEFAULT_BRIGHTNESS = 0.5f

        /**
         * Night vision red overlay color optimized for dark adaptation.
         * Pure red (no green/blue) at 80% opacity to preserve night vision.
         */
        val NIGHT_VISION_COLOR: Int = Color.argb(204, 255, 0, 0) // 80% opacity red

        /**
         * Default color (white, fully transparent - no overlay)
         */
        val DEFAULT_COLOR_ARGB: Int = Color.argb(0, 255, 255, 255)
    }
}
