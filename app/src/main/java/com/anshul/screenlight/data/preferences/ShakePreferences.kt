package com.anshul.screenlight.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.anshul.screenlight.service.ShakeDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages preferences for shake detection feature.
 * Stores shake sensitivity level and enabled state.
 */
@Singleton
class ShakePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val PREFS_NAME = "shake_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SENSITIVITY = "sensitivity"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Whether shake detection is enabled.
     * Default: true
     */
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /**
     * Shake sensitivity level.
     * One of ShakeDetector.SENSITIVITY_LIGHT, SENSITIVITY_MEDIUM, or SENSITIVITY_HARD.
     * Default: SENSITIVITY_LIGHT
     */
    var sensitivity: Int
        get() = prefs.getInt(KEY_SENSITIVITY, ShakeDetector.SENSITIVITY_MEDIUM)
        set(value) = prefs.edit().putInt(KEY_SENSITIVITY, value).apply()
}
