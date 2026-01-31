package com.anshul.screenlight

import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.anshul.screenlight.ui.screen.LightScreen
import com.anshul.screenlight.ui.viewmodel.LightViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for Screenlight app.
 * Entry point for Compose UI and Hilt dependency injection.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: LightViewModel by viewModels()
    private var lastVolumeClickTime = 0L

    companion object {
        /**
         * Time window for detecting volume button double-click (milliseconds).
         */
        private const val DOUBLE_CLICK_THRESHOLD_MS = 300L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            event.startTracking() // Enable long-press detection
            viewModel.onVolumeButtonDown()
            return true // Consume event - prevent volume change
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            viewModel.onVolumeButtonLongPress()
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            viewModel.onVolumeButtonUp()

            // Check for double-click (only if not long-press)
            if (!event.isCanceled) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastVolumeClickTime < DOUBLE_CLICK_THRESHOLD_MS) {
                    viewModel.onVolumeButtonDoubleClick()
                    lastVolumeClickTime = 0L
                } else {
                    lastVolumeClickTime = now
                }
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}

@Composable
fun ScreenlightTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
