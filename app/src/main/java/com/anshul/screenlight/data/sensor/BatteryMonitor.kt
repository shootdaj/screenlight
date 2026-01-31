package com.anshul.screenlight.data.sensor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitor for battery level and charging state.
 *
 * Uses sticky intent pattern to read battery without registering BroadcastReceiver.
 */
@Singleton
class BatteryMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /**
         * Battery percentage threshold below which battery is considered low.
         */
        const val LOW_BATTERY_THRESHOLD = 15

        /**
         * Invalid value indicator for battery extras.
         */
        private const val INVALID_VALUE = -1

        /**
         * Multiplier to convert battery level to percentage.
         */
        private const val PERCENTAGE_MULTIPLIER = 100
    }

    /**
     * Get current battery level as a percentage (0-100).
     *
     * @return Battery percentage, or null if unable to read
     */
    fun getBatteryPercentage(): Int? {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return null

        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, INVALID_VALUE)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, INVALID_VALUE)

        return if (level >= 0 && scale > 0) {
            (level * PERCENTAGE_MULTIPLIER / scale)
        } else {
            null
        }
    }

    /**
     * Check if battery is currently low (below LOW_BATTERY_THRESHOLD).
     *
     * @return true if battery low, false if adequate, null if unable to read
     */
    fun isLowBattery(): Boolean? {
        val percentage = getBatteryPercentage() ?: return null
        return percentage < LOW_BATTERY_THRESHOLD
    }

    /**
     * Check if device is currently charging.
     *
     * @return true if charging, false if not charging, null if unable to read
     */
    fun isCharging(): Boolean? {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return null

        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, INVALID_VALUE)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    /**
     * Get battery health status.
     *
     * @return BatteryManager health constant, or null if unable to read
     */
    fun getBatteryHealth(): Int? {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return null

        return batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, INVALID_VALUE)
            .takeIf { it != INVALID_VALUE }
    }

    /**
     * Get comprehensive battery info.
     *
     * @return BatteryInfo object with percentage, charging state, health, or null if unable to read
     */
    fun getBatteryInfo(): BatteryInfo? {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return null

        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, INVALID_VALUE)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, INVALID_VALUE)
        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, INVALID_VALUE)
        val health = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, INVALID_VALUE)

        return if (level >= 0 && scale > 0 && status >= 0) {
            val percentage = level * PERCENTAGE_MULTIPLIER / scale
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            BatteryInfo(
                percentage = percentage,
                isCharging = charging,
                isLow = percentage < LOW_BATTERY_THRESHOLD,
                health = health.takeIf { it != INVALID_VALUE }
            )
        } else {
            null
        }
    }

    /**
     * Data class holding battery information.
     */
    data class BatteryInfo(
        val percentage: Int,
        val isCharging: Boolean,
        val isLow: Boolean,
        val health: Int?
    )
}
