package com.anshul.screenlight.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.anshul.screenlight.MainActivity
import com.anshul.screenlight.R
import com.anshul.screenlight.data.state.LightStateManager
import dagger.hilt.android.EntryPointAccessors

/**
 * Home screen widget for toggling Screenlight on/off.
 *
 * Behavior:
 * - When light is OFF: tapping widget launches MainActivity
 * - When light is ON: tapping widget closes MainActivity via ACTION_CLOSE_LIGHT broadcast
 * - Widget appearance (background and icon) reflects current light state
 * - Supports multiple sizes (1x1, 2x1, 2x2) with resizing
 */
class LightWidgetProvider : AppWidgetProvider() {

    companion object {
        /**
         * Action for widget click broadcasts.
         */
        const val ACTION_WIDGET_TOGGLE = "com.anshul.screenlight.WIDGET_TOGGLE"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Get LightStateManager from Hilt
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            LightWidgetProviderEntryPoint::class.java
        )
        val lightStateManager = hiltEntryPoint.lightStateManager()

        // Update all widgets
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, lightStateManager)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_WIDGET_TOGGLE) {
            // Get LightStateManager from Hilt
            val hiltEntryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                LightWidgetProviderEntryPoint::class.java
            )
            val lightStateManager = hiltEntryPoint.lightStateManager()

            val isLightOn = lightStateManager.isLightOn()

            if (isLightOn) {
                // Light is on - send broadcast to close MainActivity
                val closeIntent = Intent(LightStateManager.ACTION_CLOSE_LIGHT).apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(closeIntent)
                lightStateManager.setLightOn(false)
            } else {
                // Light is off - launch MainActivity
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(launchIntent)
                lightStateManager.setLightOn(true)
            }

            // Update all widgets immediately
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, LightWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, lightStateManager)
            }
        }
    }

    /**
     * Updates a single widget instance with current light state.
     */
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        lightStateManager: LightStateManager
    ) {
        val isLightOn = lightStateManager.isLightOn()

        // Create PendingIntent for widget click
        val toggleIntent = Intent(context, LightWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_TOGGLE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            toggleIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        // Build RemoteViews
        val views = RemoteViews(context.packageName, R.layout.widget_light)

        // Set click listener
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        // Update background based on state
        val backgroundRes = if (isLightOn) R.drawable.widget_bg_on else R.drawable.widget_bg_off
        views.setInt(R.id.widget_container, "setBackgroundResource", backgroundRes)

        // Update widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

/**
 * Hilt entry point for accessing dependencies in AppWidgetProvider.
 * AppWidgetProvider cannot be directly annotated with @AndroidEntryPoint.
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface LightWidgetProviderEntryPoint {
    fun lightStateManager(): LightStateManager
}
