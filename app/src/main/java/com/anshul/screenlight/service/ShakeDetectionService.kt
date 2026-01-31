package com.anshul.screenlight.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.anshul.screenlight.MainActivity

private const val TAG = "ShakeDetectionService"
import com.anshul.screenlight.R
import com.anshul.screenlight.data.preferences.ShakePreferences
import com.anshul.screenlight.data.state.LightStateManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject

/**
 * Foreground service that provides background shake detection.
 * Enables launching the light by shaking the phone from anywhere, including lock screen.
 *
 * Uses accelerometer sensor to detect shake gestures and responds by either:
 * - Launching MainActivity if light is off
 * - Sending ACTION_CLOSE_LIGHT broadcast if light is on
 *
 * Provides haptic feedback and implements 1-second cooldown to prevent rapid toggling.
 */
@AndroidEntryPoint
class ShakeDetectionService : Service(), SensorEventListener {
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "shake_detection"
        private const val SHAKE_COOLDOWN_MS = 2000L
    }

    @Inject
    lateinit var shakePreferences: ShakePreferences

    private lateinit var sensorManager: SensorManager
    private lateinit var vibrator: Vibrator
    private lateinit var notificationManager: NotificationManager
    private val shakeDetector = ShakeDetector()

    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L

    // LightStateManager accessed via EntryPoint (service injection pattern)
    private val lightStateManager: LightStateManager by lazy {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            LightStateManagerEntryPoint::class.java
        )
        hiltEntryPoint.lightStateManager()
    }

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface LightStateManagerEntryPoint {
        fun lightStateManager(): LightStateManager
    }

    override fun onCreate() {
        super.onCreate()

        // Get system services
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Get accelerometer sensor
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Create notification channel
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")

        // Load sensitivity from preferences
        val sensitivity = shakePreferences.sensitivity
        shakeDetector.setSensitivity(sensitivity)
        Log.d(TAG, "Shake sensitivity: $sensitivity")

        // Set shake listener
        shakeDetector.setListener {
            onShakeDetected()
        }

        // Register sensor listener
        if (accelerometer != null) {
            val registered = sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )
            Log.d(TAG, "Accelerometer registered: $registered")
        } else {
            Log.e(TAG, "No accelerometer available!")
        }

        // Start foreground with notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Foreground service started")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister sensor listener
        sensorManager.unregisterListener(this)
        shakeDetector.stop()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not a bound service
        return null
    }

    override fun onSensorChanged(event: SensorEvent) {
        shakeDetector.onSensorChanged(event)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for shake detection
    }

    /**
     * Called when shake gesture is detected.
     * Implements cooldown, haptic feedback, and light toggle logic.
     */
    private fun onShakeDetected() {
        val now = System.currentTimeMillis()
        Log.d(TAG, "onShakeDetected called")

        // Check cooldown to prevent rapid toggling
        if (now - lastShakeTime < SHAKE_COOLDOWN_MS) {
            Log.d(TAG, "Shake ignored - cooldown active")
            return
        }
        lastShakeTime = now

        // Provide haptic feedback
        provideHapticFeedback()

        // Query light state and respond accordingly
        val isLightOn = lightStateManager.isLightOn()
        Log.d(TAG, "Light is currently: ${if (isLightOn) "ON" else "OFF"}")

        if (isLightOn) {
            // Light is on - send close broadcast
            Log.d(TAG, "Sending close broadcast")
            val closeIntent = Intent(LightStateManager.ACTION_CLOSE_LIGHT).apply {
                setPackage(packageName)
            }
            sendBroadcast(closeIntent)
        } else {
            // Light is off - launch MainActivity
            Log.d(TAG, "Launching MainActivity")
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(launchIntent)
        }
    }

    /**
     * Provides haptic feedback using appropriate API for device version.
     */
    private fun provideHapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+: Use predefined click effect
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API 26+: Use one-shot effect
            val effect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            // Below API 26: Use deprecated vibrate
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    /**
     * Creates notification channel for shake detection service.
     * Required for Android O+ foreground services.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Shake Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background shake detection for light activation"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates the foreground service notification.
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Shake to activate light")
            .setContentText("Shake detection is active")
            .setSmallIcon(R.drawable.ic_notification_shake)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
