package com.anshul.screenlight.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.anshul.screenlight.data.model.ScreenSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for screen settings persistence using DataStore.
 *
 * Provides reactive access to screen brightness and color settings.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_BRIGHTNESS = floatPreferencesKey("brightness")
        private val KEY_COLOR_ARGB = intPreferencesKey("color_argb")
    }

    /**
     * Observe current screen settings as a Flow.
     * Emits new values whenever settings change.
     */
    val settings: Flow<ScreenSettings> = dataStore.data.map { preferences ->
        ScreenSettings(
            brightness = preferences[KEY_BRIGHTNESS] ?: ScreenSettings.DEFAULT_BRIGHTNESS,
            colorArgb = preferences[KEY_COLOR_ARGB] ?: ScreenSettings.DEFAULT_COLOR_ARGB
        )
    }

    /**
     * Update brightness level.
     *
     * @param brightness Brightness value from 0.0 (darkest) to 1.0 (brightest)
     */
    suspend fun updateBrightness(brightness: Float) {
        dataStore.edit { preferences ->
            preferences[KEY_BRIGHTNESS] = brightness.coerceIn(0f, 1f)
        }
    }

    /**
     * Update screen overlay color.
     *
     * @param colorArgb Color in ARGB format
     */
    suspend fun updateColor(colorArgb: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_COLOR_ARGB] = colorArgb
        }
    }

    /**
     * Update both brightness and color atomically.
     *
     * @param brightness Brightness value from 0.0 to 1.0
     * @param colorArgb Color in ARGB format
     */
    suspend fun updateSettings(brightness: Float, colorArgb: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_BRIGHTNESS] = brightness.coerceIn(0f, 1f)
            preferences[KEY_COLOR_ARGB] = colorArgb
        }
    }

    /**
     * Reset settings to defaults.
     */
    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences[KEY_BRIGHTNESS] = ScreenSettings.DEFAULT_BRIGHTNESS
            preferences[KEY_COLOR_ARGB] = ScreenSettings.DEFAULT_COLOR_ARGB
        }
    }
}
