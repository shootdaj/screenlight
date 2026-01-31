package com.anshul.screenlight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.anshul.screenlight.ui.screen.LightScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for Screenlight app.
 * Entry point for Compose UI and Hilt dependency injection.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScreenlightTheme {
                LightScreen()
            }
        }
    }
}

@Composable
fun ScreenlightTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
