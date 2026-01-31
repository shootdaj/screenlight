package com.anshul.screenlight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.anshul.screenlight.data.preferences.ShakePreferences
import com.anshul.screenlight.data.state.LightStateManager
import com.anshul.screenlight.service.ShakeDetectionService
import com.anshul.screenlight.ui.screen.LightScreen
import com.anshul.screenlight.ui.viewmodel.LightViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main activity for Screenlight app.
 * Entry point for Compose UI and Hilt dependency injection.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var lightStateManager: LightStateManager

    @Inject
    lateinit var shakePreferences: ShakePreferences

    private val viewModel: LightViewModel by viewModels()
    private var lastVolumeClickTime = 0L
    private var volumeClickCount = 0

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LightStateManager.ACTION_CLOSE_LIGHT) {
                finish()
            }
        }
    }

    companion object {
        /**
         * Time window for detecting volume button multi-click (milliseconds).
         * Double-click = close app, Triple-click = toggle flashlight.
         */
        private const val MULTI_CLICK_THRESHOLD_MS = 400L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable lock screen display and screen turn on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Register close action receiver
        val filter = IntentFilter(LightStateManager.ACTION_CLOSE_LIGHT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(closeReceiver, filter)
        }

        // Update light state to ON
        lightStateManager.setLightOn(true)

        // Start shake detection service if enabled
        if (shakePreferences.isEnabled) {
            val serviceIntent = Intent(this, ShakeDetectionService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        }

        enableEdgeToEdge()
        setContent {
            // Observe shouldCloseApp state
            LaunchedEffect(Unit) {
                viewModel.gestureState.collect { state ->
                    if (state.shouldCloseApp) {
                        viewModel.clearCloseAppFlag()
                        finish()
                    }
                }
            }

            ScreenlightTheme {
                LightScreen()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            viewModel.onVolumeButtonDown()
            return true // Consume event - prevent volume change
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            viewModel.onVolumeButtonUp()

            // Track multi-click: double-click = close, triple-click = flashlight
            val now = SystemClock.elapsedRealtime()
            if (now - lastVolumeClickTime < MULTI_CLICK_THRESHOLD_MS) {
                volumeClickCount++
                when (volumeClickCount) {
                    2 -> viewModel.onVolumeButtonDoubleClick()
                    3 -> {
                        viewModel.onVolumeButtonTripleClick()
                        volumeClickCount = 0
                    }
                }
            } else {
                volumeClickCount = 1
            }
            lastVolumeClickTime = now
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Update light state to OFF
        lightStateManager.setLightOn(false)
        // Unregister close action receiver
        unregisterReceiver(closeReceiver)
    }
}

@Composable
fun ScreenlightTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
