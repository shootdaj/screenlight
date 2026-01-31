package com.anshul.screenlight.service

import android.hardware.SensorEvent
import android.util.Log

private const val TAG = "ShakeDetector"

/**
 * Shake detection algorithm adapted from Square's Seismic library.
 * Detects shake gestures by analyzing accelerometer data patterns.
 *
 * Algorithm: Tracks acceleration magnitude over time and triggers when
 * 3/4 of samples in a 0.25s+ window exceed the sensitivity threshold.
 */
class ShakeDetector {
    companion object {
        /**
         * Sensitivity constants for shake detection.
         * These are acceleration thresholds in m/s².
         * Gravity is ~9.8 m/s², so threshold should be above that.
         * Lower values = more sensitive (easier to trigger).
         */
        const val SENSITIVITY_LIGHT = 12    // ~1.2g - easy shake
        const val SENSITIVITY_MEDIUM = 15   // ~1.5g - moderate shake
        const val SENSITIVITY_HARD = 20     // ~2.0g - hard shake

        private const val MAX_QUEUE_SIZE = 40
        private const val MIN_WINDOW_SIZE_NS = 250_000_000L // 0.25 seconds in nanoseconds
    }

    private var sensitivity = SENSITIVITY_LIGHT
    private var listener: (() -> Unit)? = null
    private val queue = ArrayDeque<Sample>()
    private var lastLogTime = 0L
    private var eventCount = 0

    /**
     * Sets the sensitivity level for shake detection.
     * @param sensitivity One of SENSITIVITY_LIGHT, SENSITIVITY_MEDIUM, or SENSITIVITY_HARD
     */
    fun setSensitivity(sensitivity: Int) {
        this.sensitivity = sensitivity
        Log.d(TAG, "Sensitivity set to $sensitivity")
    }

    /**
     * Sets the callback to invoke when a shake is detected.
     * @param listener Callback function
     */
    fun setListener(listener: () -> Unit) {
        this.listener = listener
    }

    /**
     * Processes accelerometer sensor events to detect shakes.
     * Call this from SensorEventListener.onSensorChanged()
     *
     * @param event Sensor event from accelerometer
     */
    fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate magnitude squared (avoid sqrt for performance)
        val magnitudeSquared = (x * x + y * y + z * z).toDouble()

        // Check if acceleration exceeds threshold (direct comparison, no multiplier)
        val thresholdSquared = (sensitivity * sensitivity).toDouble()
        val isAccelerating = magnitudeSquared > thresholdSquared

        eventCount++

        // Log periodically (every 3 seconds) to confirm sensor is active
        val now = System.currentTimeMillis()
        if (now - lastLogTime > 3000) {
            val magnitude = kotlin.math.sqrt(magnitudeSquared)
            Log.d(TAG, "Sensor active: events=$eventCount, magnitude=${String.format("%.1f", magnitude)}, threshold=$sensitivity, accelerating=$isAccelerating")
            lastLogTime = now
        }

        // Add sample to queue
        val sample = Sample(event.timestamp, isAccelerating)
        queue.addLast(sample)

        // Maintain queue size
        if (queue.size > MAX_QUEUE_SIZE) {
            queue.removeFirst()
        }

        // Check if we have enough data
        if (queue.size < 4) {
            return
        }

        // Find oldest sample within minimum window
        val windowStartTime = event.timestamp - MIN_WINDOW_SIZE_NS
        var acceleratingCount = 0
        var totalCount = 0

        for (s in queue) {
            if (s.timestamp >= windowStartTime) {
                totalCount++
                if (s.isAccelerating) {
                    acceleratingCount++
                }
            }
        }

        // Shake detected if 3/4 of samples in window are accelerating
        if (totalCount >= 4 && acceleratingCount >= (totalCount * 3) / 4) {
            Log.d(TAG, "SHAKE DETECTED! accelerating=$acceleratingCount/$totalCount")
            listener?.invoke()
            queue.clear() // Clear queue to prevent repeated triggers
        }
    }

    /**
     * Stops shake detection and clears internal state.
     */
    fun stop() {
        queue.clear()
    }

    /**
     * Represents a single accelerometer sample.
     * @param timestamp Sensor event timestamp in nanoseconds
     * @param isAccelerating Whether acceleration exceeded threshold
     */
    private data class Sample(
        val timestamp: Long,
        val isAccelerating: Boolean
    )
}
