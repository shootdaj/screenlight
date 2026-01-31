package com.anshul.screenlight.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Dialog warning user that battery is low.
 *
 * Informs user that brightness is capped to conserve power.
 */
@Composable
fun BatteryWarningDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Low Battery") },
        text = {
            Text(
                "Battery is below 15%. Brightness is capped at 30% to conserve power. " +
                        "Charge your device for full brightness."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
