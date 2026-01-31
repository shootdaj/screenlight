package com.anshul.screenlight.data.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Data class representing device tilt angles.
 *
 * @property pitch Forward/back tilt in degrees (-90 face down to +90 face up)
 * @property roll Left/right tilt in degrees (-180 to +180)
 */
data class TiltData(
    val pitch: Float,
    val roll: Float
)

/**
 * Manager for device tilt gesture detection using game rotation vector sensor.
 *
 * Provides pitch and roll angles for gesture-based controls (brightness, color selection).
 * Uses TYPE_GAME_ROTATION_VECTOR for fused accelerometer+gyroscope data without magnetometer noise.
 */
@Singleton
class TiltGestureManager @Inject constructor(
    private val sensorManager: SensorManager
) {
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    /**
     * Check if device has a rotation vector sensor.
     */
    val hasSensor: Boolean
        get() = rotationSensor != null

    /**
     * Observe device tilt changes as a Flow.
     *
     * Emits TiltData with pitch/roll angles whenever device orientation changes.
     * Flow is cold - sensor only active while collected.
     * Uses SENSOR_DELAY_UI (~60ms) for responsive gesture detection.
     *
     * @return Flow emitting TiltData, or empty flow if sensor unavailable
     */
    fun observeTilt(): Flow<TiltData> = callbackFlow {
        val sensor = rotationSensor
        if (sensor == null) {
            close()
            return@callbackFlow
        }

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Convert rotation vector to rotation matrix
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                // Extract orientation angles from rotation matrix
                SensorManager.getOrientation(rotationMatrix, orientation)

                // Convert radians to degrees
                // orientation[0] = azimuth (compass direction, not used)
                // orientation[1] = pitch (forward/back tilt)
                // orientation[2] = roll (left/right tilt)
                val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

                trySend(TiltData(pitch, roll))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // Accuracy changes don't affect orientation calculation
            }
        }

        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_UI
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}

/**
 * Utility functions for mapping tilt angles to UI controls.
 */
object TiltMapper {
    /**
     * Map pitch angle to brightness level.
     *
     * Maps pitch from [-45, 0] degrees to [0, 1] brightness.
     * Phone upright-ish (-45 deg) = dim (0), flat (0 deg) = bright (1).
     *
     * @param pitch Forward/back tilt in degrees
     * @return Brightness value from 0.0 (dim) to 1.0 (bright)
     */
    fun pitchToBrightness(pitch: Float): Float {
        // Clamp pitch to range [-45, 0]
        val clampedPitch = pitch.coerceIn(-45f, 0f)

        // Normalize to [0, 1]
        // -45 deg -> 0 (dim), 0 deg -> 1 (bright)
        return (clampedPitch + 45f) / 45f
    }

    /**
     * Map roll angle to color index.
     *
     * Maps roll from [-45, +45] degrees to [0, colorCount-1] index.
     * Left tilt = warm colors (low index), right tilt = cool colors (high index).
     *
     * @param roll Left/right tilt in degrees
     * @param colorCount Number of colors in palette
     * @return Color index from 0 to colorCount-1
     */
    fun rollToColorIndex(roll: Float, colorCount: Int): Int {
        if (colorCount <= 0) return 0

        // Clamp roll to range [-45, +45]
        val clampedRoll = roll.coerceIn(-45f, 45f)

        // Normalize to [0, 1]
        // -45 deg -> 0 (warm/low index), +45 deg -> 1 (cool/high index)
        val normalized = (clampedRoll + 45f) / 90f

        // Map to color index and round
        val index = (normalized * (colorCount - 1)).roundToInt()

        // Clamp to valid range
        return index.coerceIn(0, colorCount - 1)
    }
}
