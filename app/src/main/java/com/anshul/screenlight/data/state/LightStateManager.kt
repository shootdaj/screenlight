package com.anshul.screenlight.data.state

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages centralized light state for the application.
 * Tracks whether the light is currently on and broadcasts state changes.
 * Persists state across process death using SharedPreferences.
 */
@Singleton
class LightStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /**
         * Broadcast action sent when light state changes.
         * Sent as both local and global broadcast.
         */
        const val ACTION_LIGHT_STATE_CHANGED = "com.anshul.screenlight.LIGHT_STATE_CHANGED"

        /**
         * Broadcast action for external components to request light closure.
         * MainActivity listens for this to finish itself.
         */
        const val ACTION_CLOSE_LIGHT = "com.anshul.screenlight.CLOSE_LIGHT"

        /**
         * Intent extra key for light on/off state (Boolean).
         */
        const val EXTRA_LIGHT_ON = "light_on"

        private const val PREFS_NAME = "app_state"
        private const val KEY_LIGHT_ON = "light_on"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns whether the light is currently on.
     */
    fun isLightOn(): Boolean {
        return prefs.getBoolean(KEY_LIGHT_ON, false)
    }

    /**
     * Updates the light state and broadcasts the change.
     * @param isOn true if light is now on, false if off
     */
    fun setLightOn(isOn: Boolean) {
        val currentState = isLightOn()
        if (currentState != isOn) {
            // Persist state
            prefs.edit().putBoolean(KEY_LIGHT_ON, isOn).apply()

            // Broadcast state change
            val intent = Intent(ACTION_LIGHT_STATE_CHANGED).apply {
                putExtra(EXTRA_LIGHT_ON, isOn)
            }
            context.sendBroadcast(intent)
        }
    }
}
