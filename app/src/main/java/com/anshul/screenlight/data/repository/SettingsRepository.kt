package com.anshul.screenlight.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
        private val KEY_COLOR_TEMPERATURE = floatPreferencesKey("color_temperature")
    }

    /**
     * Observe current screen settings as a Flow.
     * Emits new values whenever settings change.
     */
    val settings: Flow<ScreenSettings> = dataStore.data.map { preferences ->
        ScreenSettings(
            brightness = preferences[KEY_BRIGHTNESS] ?: ScreenSettings.DEFAULT_BRIGHTNESS,
            colorTemperature = preferences[KEY_COLOR_TEMPERATURE] ?: ScreenSettings.DEFAULT_COLOR_TEMPERATURE
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
     * Update color temperature.
     *
     * @param temperature Color temperature from 0.0 (red) to 1.0 (white)
     */
    suspend fun updateColorTemperature(temperature: Float) {
        dataStore.edit { preferences ->
            preferences[KEY_COLOR_TEMPERATURE] = temperature.coerceIn(0f, 1f)
        }
    }

    /**
     * Update both brightness and color atomically.
     *
     * @param brightness Brightness value from 0.0 to 1.0
     * @param colorTemperature Color temperature from 0.0 to 1.0
     */
    suspend fun updateSettings(brightness: Float, colorTemperature: Float) {
        dataStore.edit { preferences ->
            preferences[KEY_BRIGHTNESS] = brightness.coerceIn(0f, 1f)
            preferences[KEY_COLOR_TEMPERATURE] = colorTemperature.coerceIn(0f, 1f)
        }
    }

    /**
     * Reset settings to defaults.
     */
    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences[KEY_BRIGHTNESS] = ScreenSettings.DEFAULT_BRIGHTNESS
            preferences[KEY_COLOR_TEMPERATURE] = ScreenSettings.DEFAULT_COLOR_TEMPERATURE
        }
    }
}
