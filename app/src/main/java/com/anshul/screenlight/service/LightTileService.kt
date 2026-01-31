package com.anshul.screenlight.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.anshul.screenlight.MainActivity
import com.anshul.screenlight.R
import com.anshul.screenlight.data.state.LightStateManager
import dagger.hilt.android.EntryPointAccessors

/**
 * Quick Settings tile for toggling Screenlight on/off.
 *
 * Behavior:
 * - When light is OFF: tapping tile launches MainActivity
 * - When light is ON: tapping tile closes MainActivity via ACTION_CLOSE_LIGHT broadcast
 * - Tile state (active/inactive) reflects current light state
 * - Listens for ACTION_LIGHT_STATE_CHANGED to update tile when state changes externally
 */
class LightTileService : TileService() {

    private lateinit var lightStateManager: LightStateManager

    private val stateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LightStateManager.ACTION_LIGHT_STATE_CHANGED) {
                updateTileState()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Get LightStateManager from Hilt - TileService is not directly injectable
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            LightTileServiceEntryPoint::class.java
        )
        lightStateManager = hiltEntryPoint.lightStateManager()

        // Register receiver for state change broadcasts
        val filter = IntentFilter(LightStateManager.ACTION_LIGHT_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stateChangeReceiver, filter)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        val isLightOn = lightStateManager.isLightOn()

        if (isLightOn) {
            // Light is on - send broadcast to close MainActivity
            val closeIntent = Intent(LightStateManager.ACTION_CLOSE_LIGHT).apply {
                setPackage(packageName)
            }
            sendBroadcast(closeIntent)
        } else {
            // Light is off - launch MainActivity
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivityAndCollapse(launchIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(stateChangeReceiver)
    }

    /**
     * Updates tile state (active/inactive) and icon based on current light state.
     */
    private fun updateTileState() {
        val tile = qsTile ?: return
        val isLightOn = lightStateManager.isLightOn()

        tile.state = if (isLightOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_light)
        tile.updateTile()
    }
}

/**
 * Hilt entry point for accessing dependencies in TileService.
 * TileService cannot be directly annotated with @AndroidEntryPoint.
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface LightTileServiceEntryPoint {
    fun lightStateManager(): LightStateManager
}
