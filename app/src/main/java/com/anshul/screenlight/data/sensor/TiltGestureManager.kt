package com.anshul.screenlight.data.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val TAG = "TiltGestureManager"

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
 * Type of sensor being used for tilt detection.
 */
enum class TiltSensorType {
    GAME_ROTATION_VECTOR,  // Best: fused accel+gyro, no magnetometer noise
    ROTATION_VECTOR,       // Good: fused accel+gyro+magnetometer
    ACCELEROMETER,         // Fallback: raw accelerometer only
    NONE                   // No suitable sensor available
}

/**
 * Manager for device tilt gesture detection.
 *
 * Provides pitch and roll angles for gesture-based controls (brightness, color selection).
 * Uses sensor fallback chain:
 * 1. TYPE_GAME_ROTATION_VECTOR (best - fused accel+gyro without magnetometer)
 * 2. TYPE_ROTATION_VECTOR (good - includes magnetometer)
 * 3. TYPE_ACCELEROMETER (fallback - raw accelerometer)
 */
@Singleton
class TiltGestureManager @Inject constructor(
    private val sensorManager: SensorManager
) {
    // Try sensors in order of preference
    private val gameRotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /**
     * The type of sensor being used for tilt detection.
     */
    val sensorType: TiltSensorType = when {
        gameRotationSensor != null -> TiltSensorType.GAME_ROTATION_VECTOR
        rotationSensor != null -> TiltSensorType.ROTATION_VECTOR
        accelerometer != null -> TiltSensorType.ACCELEROMETER
        else -> TiltSensorType.NONE
    }

    /**
     * Check if device has any suitable sensor for tilt detection.
     */
    val hasSensor: Boolean
        get() = sensorType != TiltSensorType.NONE

    init {
        Log.d(TAG, "Tilt sensor type: $sensorType")
        Log.d(TAG, "  - GAME_ROTATION_VECTOR: ${gameRotationSensor != null}")
        Log.d(TAG, "  - ROTATION_VECTOR: ${rotationSensor != null}")
        Log.d(TAG, "  - ACCELEROMETER: ${accelerometer != null}")
    }

    /**
     * Observe device tilt changes as a Flow.
     *
     * Emits TiltData with pitch/roll angles whenever device orientation changes.
     * Flow is cold - sensor only active while collected.
     * Uses SENSOR_DELAY_GAME for responsive gesture detection.
     *
     * @return Flow emitting TiltData, or empty flow if no sensor available
     */
    fun observeTilt(): Flow<TiltData> = callbackFlow<TiltData> {
        val (sensor, isRotationVector) = when (sensorType) {
            TiltSensorType.GAME_ROTATION_VECTOR -> gameRotationSensor to true
            TiltSensorType.ROTATION_VECTOR -> rotationSensor to true
            TiltSensorType.ACCELEROMETER -> accelerometer to false
            TiltSensorType.NONE -> {
                Log.w(TAG, "No tilt sensor available - tilt gestures disabled")
                close()
                return@callbackFlow
            }
        }

        if (sensor == null) {
            Log.e(TAG, "Sensor was null despite sensorType=$sensorType")
            close()
            return@callbackFlow
        }

        Log.d(TAG, "Starting tilt observation with $sensorType")

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        var eventCount = 0
        var lastLogTime = System.currentTimeMillis()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                try {
                    val (pitch, roll) = if (isRotationVector) {
                        // Rotation vector sensors: use rotation matrix
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)

                        val p = Math.toDegrees(orientation[1].toDouble()).toFloat()
                        val r = Math.toDegrees(orientation[2].toDouble()).toFloat()
                        p to r
                    } else {
                        // Accelerometer fallback: calculate from gravity
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]

                        // Calculate pitch and roll from accelerometer
                        // Pitch: rotation around X axis (tilt forward/back)
                        // Roll: rotation around Y axis (tilt left/right)
                        val p = Math.toDegrees(atan2(y.toDouble(), sqrt((x * x + z * z).toDouble()))).toFloat()
                        val r = Math.toDegrees(atan2(-x.toDouble(), z.toDouble())).toFloat()
                        p to r
                    }

                    eventCount++
                    val result = trySend(TiltData(pitch, roll))

                    // Log periodically (every 2 seconds) to confirm sensor is still active
                    val now = System.currentTimeMillis()
                    if (now - lastLogTime > 2000) {
                        Log.d(TAG, "Sensor active: $eventCount events, last send success=${result.isSuccess}, pitch=$pitch, roll=$roll")
                        lastLogTime = now
                    }

                    if (result.isFailure) {
                        Log.w(TAG, "trySend failed: ${result.exceptionOrNull()?.message ?: "channel closed"}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in onSensorChanged: ${e.message}", e)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                Log.d(TAG, "Sensor accuracy changed: $accuracy")
            }
        }

        val registered = sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_GAME  // Faster than UI for responsive gestures
        )

        if (!registered) {
            Log.e(TAG, "Failed to register sensor listener for $sensorType")
            close()
            return@callbackFlow
        }

        Log.d(TAG, "Sensor listener registered successfully")

        awaitClose {
            Log.d(TAG, "Stopping tilt observation after $eventCount events")
            sensorManager.unregisterListener(listener)
        }
    }.buffer(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
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
