package com.anshul.screenlight.data.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manager for ambient light sensor readings.
 *
 * Provides access to device light sensor for detecting dark environments.
 */
@Singleton
class AmbientLightManager @Inject constructor(
    private val sensorManager: SensorManager
) {
    companion object {
        /**
         * Lux threshold below which environment is considered dark.
         * Typical values: bright office ~400 lux, dim room ~50 lux, darkness ~0-10 lux
         */
        const val DARK_THRESHOLD_LUX = 50f
    }

    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    /**
     * Check if device has an ambient light sensor.
     */
    val hasSensor: Boolean
        get() = lightSensor != null

    /**
     * Get current ambient light level in lux.
     *
     * @return Current lux value, or null if sensor unavailable or reading failed
     */
    suspend fun getCurrentLux(): Float? = suspendCancellableCoroutine { continuation ->
        val sensor = lightSensor
        if (sensor == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (continuation.isActive) {
                    sensorManager.unregisterListener(this)
                    continuation.resume(event.values[0])
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // Not needed for one-shot reading
            }
        }

        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        continuation.invokeOnCancellation {
            sensorManager.unregisterListener(listener)
        }
    }

    /**
     * Observe ambient light changes as a Flow.
     *
     * Emits lux values whenever light level changes.
     * Flow is cold - sensor only active while collected.
     *
     * @return Flow emitting lux values, or empty flow if sensor unavailable
     */
    fun observeAmbientLight(): Flow<Float> = callbackFlow {
        val sensor = lightSensor
        if (sensor == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values[0])
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // Accuracy changes don't affect lux reading
            }
        }

        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    /**
     * Check if current environment is considered dark.
     *
     * @return true if lux < DARK_THRESHOLD_LUX, false if bright, null if sensor unavailable
     */
    suspend fun isDark(): Boolean? {
        val lux = getCurrentLux() ?: return null
        return lux < DARK_THRESHOLD_LUX
    }
}
