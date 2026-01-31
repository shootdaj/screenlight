package com.anshul.screenlight.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.anshul.screenlight.data.preferences.ShakePreferences
import com.anshul.screenlight.service.ShakeDetectionService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Starts shake detection service on device boot if enabled.
 */
class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun shakePreferences(): ShakePreferences
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootReceiverEntryPoint::class.java
            )
            val shakePreferences = entryPoint.shakePreferences()

            if (shakePreferences.isEnabled) {
                val serviceIntent = Intent(context, ShakeDetectionService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
