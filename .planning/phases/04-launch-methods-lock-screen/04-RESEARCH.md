# Phase 4: Launch Methods & Lock Screen - Research

**Researched:** 2026-01-31
**Domain:** Android Quick Settings, App Widgets, Motion Sensors, Lock Screen APIs
**Confidence:** HIGH

## Summary

This phase adds three convenience launch methods to the Screenlight app: Quick Settings tile for notification shade access, home screen widget for one-tap toggle, and shake gesture detection for hands-free activation. The critical requirement is enabling full functionality from the lock screen without requiring device unlock.

Android provides mature, well-documented APIs for all these features: TileService for Quick Settings tiles, AppWidgetProvider for home screen widgets, SensorManager with TYPE_ACCELEROMETER for shake detection, and Activity.setShowWhenLocked() for lock screen display. The main challenges are battery-efficient background sensor listening (requires foreground service on Android 14+) and managing lifecycle properly across app/lock screen/background states.

**Primary recommendation:** Use standard Android framework classes (TileService, AppWidgetProvider, SensorManager) with no third-party libraries. Implement shake detection as a foreground service with proper lifecycle management. Use Activity.setShowWhenLocked(true) for lock screen access without attempting keyguard dismissal.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| TileService | Framework (API 24+) | Custom Quick Settings tiles | Official Android API, no alternatives |
| AppWidgetProvider | Framework (API 1+) | Home screen widgets | Official Android API, no alternatives |
| SensorManager | Framework (API 1+) | Accelerometer access for shake detection | Official Android API, direct sensor access |
| Activity.setShowWhenLocked() | Framework (API 27+) | Display over lock screen | Current non-deprecated method (replaces window flags) |
| HapticFeedbackConstants | Framework (API 1+) | Haptic feedback (no permissions) | Recommended over Vibrator for simple feedback |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| WorkManager | androidx.work:work-runtime-ktx:2.9.0 | Widget background updates | If widgets need periodic data updates (optional for this phase) |
| Foreground Service | Framework (API 26+) | Background sensor listening | Required for shake detection when app not visible (Android 14+) |
| VibrationEffect | Framework (API 26+) | Custom haptic patterns | Only if HapticFeedbackConstants insufficient (requires VIBRATE permission) |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Manual shake detection | Square Seismic library | Library deprecated (Nov 2024), archived as read-only; better to copy algorithm than depend on unmaintained library |
| setShowWhenLocked() | Window flags (deprecated API 27) | Old approach still works but deprecated; use modern Activity methods |
| Foreground service | Background sensor registration | Restricted on Android 14+; foreground service required for reliable background sensor access |

**Installation:**
```bash
# All core components are part of Android framework - no dependencies needed
# Optional: WorkManager if implementing widget periodic updates
./gradlew app:dependencies --configuration implementation
```

## Architecture Patterns

### Recommended Project Structure
```
app/src/main/java/com/anshul/screenlight/
├── service/
│   ├── QuickSettingsTileService.kt      # TileService implementation
│   ├── ShakeDetectionService.kt         # Foreground service for shake detection
│   └── ShakeDetector.kt                 # Shake algorithm (from Seismic, adapted)
├── widget/
│   ├── LightWidgetProvider.kt           # AppWidgetProvider implementation
│   └── LightWidgetReceiver.kt           # Handles widget click actions
├── ui/
│   └── LockScreenActivity.kt            # Alternative to modifying MainActivity
└── data/
    └── ShakePreferences.kt              # Shake sensitivity settings

app/src/main/res/
├── xml/
│   ├── light_widget_info.xml            # Widget metadata
│   └── tile_service_metadata.xml        # Optional: TileService metadata
└── layout/
    ├── widget_light_1x1.xml             # 1x1 widget layout
    ├── widget_light_2x1.xml             # 2x1 widget layout
    └── widget_light_2x2.xml             # 2x2 widget layout
```

### Pattern 1: TileService with Active Tile Mode
**What:** TileService that updates once per listening cycle and responds to user taps
**When to use:** Toggle-style Quick Settings tiles that reflect app state
**Example:**
```kotlin
// Source: https://developer.android.com/develop/ui/views/quicksettings-tiles
class LightTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        // Update tile state from current light state
        val isLightOn = getLightState() // Query from ViewModel/repository

        qsTile?.apply {
            state = if (isLightOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.tile_label)
            contentDescription = label
            icon = Icon.createWithResource(
                this@LightTileService,
                if (isLightOn) R.drawable.ic_light_on else R.drawable.ic_light_off
            )
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val isLightOn = getLightState()

        if (isLightOn) {
            // Close the app (broadcasts to MainActivity)
            sendBroadcast(Intent(ACTION_CLOSE_LIGHT))
        } else {
            // Launch MainActivity
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivityAndCollapse(intent)
        }
    }
}
```

**Manifest declaration:**
```xml
<service
    android:name=".service.LightTileService"
    android:exported="true"
    android:icon="@drawable/ic_tile"
    android:label="@string/tile_label"
    android:permission="android.permission.BIND_QUICK_SETTINGS_TILE">
    <intent-filter>
        <action android:name="android.service.quicksettings.action.QS_TILE" />
    </intent-filter>
    <meta-data
        android:name="android.service.quicksettings.ACTIVE_TILE"
        android:value="true" />
    <meta-data
        android:name="android.service.quicksettings.TOGGLEABLE_TILE"
        android:value="true" />
</service>
```

### Pattern 2: App Widget with PendingIntent Actions
**What:** RemoteViews-based widget that triggers app actions via PendingIntent
**When to use:** Simple widgets with tap-to-launch or tap-to-toggle behavior
**Example:**
```kotlin
// Source: https://developer.android.com/develop/ui/views/appwidgets
class LightWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val isLightOn = getLightState(context)

            // Create intent to toggle light
            val intent = Intent(context, LightWidgetReceiver::class.java).apply {
                action = ACTION_WIDGET_TOGGLE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.widget_light_1x1).apply {
                setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                setImageViewResource(
                    R.id.widget_icon,
                    if (isLightOn) R.drawable.ic_light_on else R.drawable.ic_light_off
                )
                setInt(
                    R.id.widget_container,
                    "setBackgroundResource",
                    if (isLightOn) R.drawable.widget_bg_on else R.drawable.widget_bg_off
                )
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
```

**Widget metadata (res/xml/light_widget_info.xml):**
```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="40dp"
    android:minHeight="40dp"
    android:targetCellWidth="1"
    android:targetCellHeight="1"
    android:maxResizeWidth="160dp"
    android:maxResizeHeight="160dp"
    android:updatePeriodMillis="0"
    android:description="@string/widget_description"
    android:initialLayout="@layout/widget_light_1x1"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen"
    android:previewLayout="@layout/widget_light_1x1" />
```

### Pattern 3: Shake Detection with Foreground Service
**What:** Background-running foreground service that monitors accelerometer and triggers app launch
**When to use:** Gesture detection that must work when app is backgrounded or screen is off
**Example:**
```kotlin
// Source: Adapted from https://github.com/square/seismic/blob/master/library/src/main/java/com/squareup/seismic/ShakeDetector.java
// and https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion

class ShakeDetectionService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val shakeDetector = ShakeDetector()
    private var lastShakeTime = 0L
    private val vibrator by lazy {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    companion object {
        private const val SHAKE_COOLDOWN_MS = 1000L
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_SENSITIVITY = "shake_sensitivity"
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Start as foreground service (required Android 14+)
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sensitivity = intent?.getIntExtra(EXTRA_SENSITIVITY, ShakeDetector.SENSITIVITY_LIGHT)
            ?: ShakeDetector.SENSITIVITY_LIGHT

        shakeDetector.setSensitivity(sensitivity)
        shakeDetector.setListener { onShakeDetected() }

        // Register sensor listener
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        shakeDetector.onSensorChanged(event)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed
    }

    private fun onShakeDetected() {
        val now = System.currentTimeMillis()
        if (now - lastShakeTime < SHAKE_COOLDOWN_MS) {
            return // Still in cooldown
        }
        lastShakeTime = now

        // Vibration feedback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }

        // Launch activity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        shakeDetector.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "shake_detection"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Shake Detection",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Shake to activate light")
            .setContentText("Shake detection is active")
            .setSmallIcon(R.drawable.ic_shake)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
```

**Shake algorithm (adapted from Square Seismic):**
```kotlin
// Source: https://github.com/square/seismic/blob/master/library/src/main/java/com/squareup/seismic/ShakeDetector.java
class ShakeDetector {
    private var listener: (() -> Unit)? = null
    private var accelerationThreshold = SENSITIVITY_LIGHT
    private val queue = SampleQueue()

    companion object {
        const val SENSITIVITY_LIGHT = 11
        const val SENSITIVITY_MEDIUM = 13
        const val SENSITIVITY_HARD = 15

        private const val ACCELERATION_THRESHOLD_MULTIPLIER = 2.5
        private const val MAX_SAMPLES = 40  // 0.5s at SENSOR_DELAY_GAME
        private const val MIN_QUEUE_SIZE = 20  // 0.25s minimum
    }

    fun setSensitivity(sensitivity: Int) {
        accelerationThreshold = sensitivity
    }

    fun setListener(listener: () -> Unit) {
        this.listener = listener
    }

    fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitudeSquared = (x * x + y * y + z * z).toDouble()
        val threshold = accelerationThreshold * ACCELERATION_THRESHOLD_MULTIPLIER
        val thresholdSquared = threshold * threshold

        queue.add(event.timestamp, magnitudeSquared > thresholdSquared)

        if (queue.isShaking()) {
            queue.clear()
            listener?.invoke()
        }
    }

    fun stop() {
        queue.clear()
    }

    private class SampleQueue {
        private val samples = ArrayDeque<Sample>(MAX_SAMPLES)

        fun add(timestamp: Long, accelerating: Boolean) {
            if (samples.size >= MAX_SAMPLES) {
                samples.removeFirst()
            }
            samples.addLast(Sample(timestamp, accelerating))
        }

        fun isShaking(): Boolean {
            if (samples.size < MIN_QUEUE_SIZE) return false

            val first = samples.first().timestamp
            val last = samples.last().timestamp
            if (last - first < 250_000_000) return false  // 0.25s in nanoseconds

            val acceleratingCount = samples.count { it.accelerating }
            return acceleratingCount >= (samples.size * 3 / 4)
        }

        fun clear() {
            samples.clear()
        }
    }

    private data class Sample(val timestamp: Long, val accelerating: Boolean)
}
```

### Pattern 4: Lock Screen Activity Display
**What:** Activity configured to display over lock screen without dismissing keyguard
**When to use:** Apps that should function fully without requiring device unlock
**Example:**
```kotlin
// Source: https://developer.android.com/reference/android/app/Activity
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display over lock screen (API 27+)
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

        // Don't dismiss lock screen - leave it in place
        // User can still interact with light via gestures

        setContent {
            LightScreen()
        }
    }
}
```

**Alternative: Manifest attribute approach:**
```xml
<activity
    android:name=".MainActivity"
    android:showWhenLocked="true"
    android:turnScreenOn="true"
    android:exported="true">
    <!-- ... -->
</activity>
```

### Anti-Patterns to Avoid

- **Don't use deprecated window flags on API 27+:** Use `setShowWhenLocked()` and `setTurnScreenOn()` instead of WindowManager.LayoutParams flags
- **Don't create custom views in RemoteViews:** Widgets only support standard Android view types (TextView, ImageView, Button, etc.) - ConstraintLayout, RecyclerView, and custom views are not supported
- **Don't use Vibrator.vibrate(long) for haptic feedback:** Use `View.performHapticFeedback(HapticFeedbackConstants.CONFIRM)` for UI interactions (no permissions, better UX) or VibrationEffect for custom patterns
- **Don't hand-roll shake detection algorithm:** Copy the proven Square Seismic algorithm (3/4 samples accelerating in 0.5s window) rather than inventing sensitivity thresholds
- **Don't forget PendingIntent.FLAG_IMMUTABLE:** Android 12+ requires explicit mutability flag; use FLAG_IMMUTABLE for security (except specific cases like geofences)
- **Don't register sensors without foreground service on Android 14+:** Background sensor access restricted; use foreground service with notification
- **Don't use widget updatePeriodMillis for this use case:** Light state changes immediately on user action - widgets update on-demand via broadcasts, not periodic polling

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Shake detection algorithm | Custom accelerometer threshold logic | Square Seismic algorithm (copy source) | Proven thresholds (2.7G), handles sample timing (0.5s window), filters false positives (3/4 samples rule), device-independent normalization |
| Widget state management | Custom broadcast system | AppWidgetManager.updateAppWidget() | System-managed lifecycle, handles configuration changes, survives process death |
| Lock screen display | Custom overlay window | Activity.setShowWhenLocked(true) | Proper security integration, handles biometric unlock, respects user preferences |
| Haptic feedback | Vibrator with manual durations | HapticFeedbackConstants.CONFIRM/REJECT | No permissions required, consistent with system UX, respects user haptic settings |
| Background service restrictions | WorkManager for continuous sensors | Foreground Service with notification | Sensors require continuous listening, not periodic jobs; foreground service exempts from background restrictions |
| Tile state synchronization | Manual SharedPreferences polling | Active tile mode with onStartListening() | System manages binding lifecycle, one update per cycle, battery efficient |

**Key insight:** Android's framework APIs are designed to handle edge cases you won't anticipate - process death, configuration changes, multi-user environments, battery optimization, security policies. Use framework patterns rather than custom solutions.

## Common Pitfalls

### Pitfall 1: Background Sensor Access Fails Silently on Android 14+
**What goes wrong:** Shake detection stops working when app is backgrounded, but no error is thrown
**Why it happens:** Android 14+ restricts background sensor access; `registerListener()` succeeds but doesn't deliver events without foreground service
**How to avoid:**
- Declare foreground service type in manifest: `<service android:foregroundServiceType="specialUse">`
- Request permission: `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />`
- Call `startForeground()` before registering sensor listener
- Show persistent notification explaining shake detection is active
**Warning signs:**
- Shake works when app is visible but not when backgrounded
- No crash or exception, just silent failure
- Battery optimization killing service

### Pitfall 2: PendingIntent Missing Immutability Flag Crashes on Android 12+
**What goes wrong:** App crashes with IllegalArgumentException: "Targeting S+ requires FLAG_IMMUTABLE or FLAG_MUTABLE"
**Why it happens:** Android 12 (API 31) requires explicit mutability declaration for security; old code didn't specify flags
**How to avoid:**
```kotlin
PendingIntent.getBroadcast(
    context,
    requestCode,
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE  // Always add
)
```
**Warning signs:**
- Crash only on Android 12+ devices
- Works fine on Android 11 and below
- Error message explicitly mentions FLAG_IMMUTABLE/FLAG_MUTABLE

### Pitfall 3: Widget Layout Uses Unsupported Views
**What goes wrong:** Widget doesn't render or shows "Problem loading widget" error
**Why it happens:** RemoteViews only supports specific view types; ConstraintLayout, RecyclerView, CardView, custom views are not supported
**How to avoid:**
- Use only: FrameLayout, LinearLayout, RelativeLayout, GridLayout
- Use only: TextView, ImageView, Button, ProgressBar, Chronometer
- Test on multiple launcher apps (some launchers more permissive than others)
- Check lint warnings: "RemoteView layout should only use allowed views"
**Warning signs:**
- Widget renders on one launcher (e.g., Pixel Launcher) but not another (e.g., Samsung One UI)
- Layout preview looks fine in Android Studio but fails on device
- No helpful error message, just generic "loading" failure

### Pitfall 4: Tile State Out of Sync with App State
**What goes wrong:** QS tile shows "on" when light is actually off, or vice versa
**Why it happens:** Tile state updated in `onClick()` but app state changes elsewhere (widget, shake gesture, etc.), and tile isn't notified
**How to avoid:**
- Centralize state in ViewModel/Repository
- Use broadcast receiver in TileService to listen for state changes
- Call `requestListeningState()` to force onStartListening() when state changes
- Active tile mode: tile updates when Quick Settings opens, which usually happens after user toggles
**Warning signs:**
- Tile correct after opening Quick Settings but wrong before
- Tile state drifts after using widget or shake gesture
- Tile requires manual refresh to sync

### Pitfall 5: Shake Detection Too Sensitive or Not Sensitive Enough
**What goes wrong:** False positives (triggers when placing phone down) or false negatives (requires vigorous shaking)
**Why it happens:** Wrong sensitivity constant, wrong sample window timing, or not normalizing accelerometer values
**How to avoid:**
- Use Seismic constants: SENSITIVITY_LIGHT = 11 (default for fumbling in dark), MEDIUM = 13, HARD = 15
- Implement cooldown period (1 second) to prevent rapid re-triggering
- Normalize values: divide raw acceleration by SensorManager.GRAVITY_EARTH
- Require 3/4 of samples accelerating in 0.5s window (Seismic algorithm)
- Test with users unfamiliar with app (they shake differently than you)
**Warning signs:**
- Users report "activates in my pocket"
- Users report "doesn't work" (shaking too hard)
- Inconsistent behavior across different devices
- No vibration confirmation, so users don't know if shake was detected

### Pitfall 6: Lock Screen Activity Dismisses Keyguard Unintentionally
**What goes wrong:** Device unlocks when launching light from lock screen, defeating the purpose
**Why it happens:** Using `KeyguardManager.requestDismissKeyguard()` or `FLAG_DISMISS_KEYGUARD` when you don't need to
**How to avoid:**
- Use ONLY `setShowWhenLocked(true)` - do NOT call requestDismissKeyguard()
- Do NOT use FLAG_DISMISS_KEYGUARD window flag
- Activity appears over lock screen but keyguard remains active underneath
- When user dismisses activity (back button, gesture), they return to lock screen
**Warning signs:**
- Device unlocks when launching from lock screen
- Secure lock screen bypassed
- User security expectations violated

### Pitfall 7: Vibration Feedback Too Loud or Ignored by Users
**What goes wrong:** Shake confirmation vibration either annoys users or doesn't match system haptic settings
**Why it happens:** Using `Vibrator` with custom durations instead of system-aligned haptics
**How to avoid:**
- Use `View.performHapticFeedback(HapticFeedbackConstants.CONFIRM)` when possible (no permissions)
- For service context, use `VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)`
- Respect user's haptic feedback setting: check Settings.System.HAPTIC_FEEDBACK_ENABLED
- Avoid createOneShot() and createWaveform() - too loud for regular UI feedback
**Warning signs:**
- Users disable haptics immediately after trying shake detection
- Vibration pattern inconsistent with rest of Android UI
- Vibration works on some devices but not others (OEM differences)

### Pitfall 8: Widget Click Launches App Instead of Toggling Light
**What goes wrong:** Widget tap opens MainActivity but doesn't turn light on/off
**Why it happens:** PendingIntent launches Activity instead of sending Broadcast to toggle state
**How to avoid:**
- Widget should send Broadcast to custom BroadcastReceiver
- Receiver toggles state in ViewModel/Repository
- Receiver then either launches Activity (if turning on) or broadcasts close signal (if turning off)
- Update widget appearance immediately via AppWidgetManager.updateAppWidget()
**Warning signs:**
- Widget always launches app regardless of state
- No immediate visual feedback on widget when tapped
- Two taps required (one to launch, one to toggle)

## Code Examples

Verified patterns from official sources:

### Quick Settings Tile Toggle Implementation
```kotlin
// Source: https://developer.android.com/develop/ui/views/quicksettings-tiles
class LightTileService : TileService() {

    private val lightStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_LIGHT_STATE_CHANGED) {
                updateTileState()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Register for state change broadcasts
        registerReceiver(
            lightStateReceiver,
            IntentFilter(ACTION_LIGHT_STATE_CHANGED),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(lightStateReceiver)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val isLightOn = getLightState()
        qsTile?.apply {
            state = if (isLightOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.app_name)
            icon = Icon.createWithResource(
                this@LightTileService,
                if (isLightOn) R.drawable.ic_light_on else R.drawable.ic_light_off
            )
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val isLightOn = getLightState()

        if (isLightOn) {
            // Send broadcast to close app
            sendBroadcast(Intent(ACTION_CLOSE_LIGHT))
        } else {
            // Launch activity
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivityAndCollapse(intent)
        }
    }

    private fun getLightState(): Boolean {
        // Query from shared preferences, repository, or ViewModel
        return getSharedPreferences("app_state", MODE_PRIVATE)
            .getBoolean("light_on", false)
    }

    companion object {
        const val ACTION_LIGHT_STATE_CHANGED = "com.anshul.screenlight.LIGHT_STATE_CHANGED"
        const val ACTION_CLOSE_LIGHT = "com.anshul.screenlight.CLOSE_LIGHT"
    }
}
```

### Widget with Multiple Sizes (Same Functionality)
```kotlin
// Source: https://developer.android.com/develop/ui/views/appwidgets
class LightWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_WIDGET_TOGGLE) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            handleWidgetToggle(context, widgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val isLightOn = getLightState(context)

        // Determine widget size and select appropriate layout
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)

        val layoutId = when {
            width >= 160 -> R.layout.widget_light_2x2
            width >= 80 -> R.layout.widget_light_2x1
            else -> R.layout.widget_light_1x1
        }

        val intent = Intent(context, LightWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_TOGGLE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val views = RemoteViews(context.packageName, layoutId).apply {
            setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            setImageViewResource(
                R.id.widget_icon,
                if (isLightOn) R.drawable.ic_light_on else R.drawable.ic_light_off
            )
            // Update background or other visual state
            setInt(
                R.id.widget_container,
                "setBackgroundResource",
                if (isLightOn) R.drawable.widget_bg_active else R.drawable.widget_bg_inactive
            )
        }

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun handleWidgetToggle(context: Context, widgetId: Int) {
        val isLightOn = getLightState(context)

        if (isLightOn) {
            // Close the light
            context.sendBroadcast(Intent(ACTION_CLOSE_LIGHT))
            setLightState(context, false)
        } else {
            // Launch activity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
            setLightState(context, true)
        }

        // Update this widget
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateWidget(context, appWidgetManager, widgetId)

        // Notify tile to update
        context.sendBroadcast(Intent(LightTileService.ACTION_LIGHT_STATE_CHANGED))
    }

    private fun getLightState(context: Context): Boolean {
        return context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
            .getBoolean("light_on", false)
    }

    private fun setLightState(context: Context, isOn: Boolean) {
        context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("light_on", isOn)
            .apply()
    }

    companion object {
        const val ACTION_WIDGET_TOGGLE = "com.anshul.screenlight.WIDGET_TOGGLE"
        const val ACTION_CLOSE_LIGHT = "com.anshul.screenlight.CLOSE_LIGHT"
    }
}
```

### Haptic Feedback for Shake Detection (Service Context)
```kotlin
// Source: https://developer.android.com/develop/ui/views/haptics/haptic-feedback
private fun provideHapticFeedback() {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // Use predefined effect for consistency with system
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator.vibrate(
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(50)
    }
}
```

### Lock Screen Activity Configuration (Both Methods)
```kotlin
// Source: https://developer.android.com/reference/android/app/Activity
// Method 1: Programmatic (more flexible)
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

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

    // DO NOT call requestDismissKeyguard() - keep lock screen intact

    setContent {
        LightScreen()
    }
}
```

```xml
<!-- Method 2: Manifest attribute (simpler, static) -->
<!-- Source: https://developer.android.com/reference/android/R.attr#showWhenLocked -->
<activity
    android:name=".MainActivity"
    android:showWhenLocked="true"
    android:turnScreenOn="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Window flags (FLAG_SHOW_WHEN_LOCKED) | Activity.setShowWhenLocked(true) | API 27 (Android 8.1) | Window flags deprecated but still work; use Activity methods for future compatibility |
| Vibrator.vibrate(long) | VibrationEffect.createPredefined() | API 26 (Android 8.0) | Old approach deprecated API 26; new approach better UX consistency |
| PendingIntent without mutability flag | FLAG_IMMUTABLE/FLAG_MUTABLE required | API 31 (Android 12) | Crash on Android 12+ without flag; security improvement |
| Background sensor access unrestricted | Foreground service required | API 34 (Android 14) | Silent failure without foreground service; battery optimization |
| Seismic library maintained | Seismic archived (read-only) | Nov 2024 | Copy algorithm source instead of depending on library |
| TileService unbound mode | Active tile mode (ACTIVE_TILE metadata) | API 24 (Android 7.0) | Active mode more battery efficient, single update per cycle |
| Widget updatePeriodMillis <30min | Minimum 30 minutes | API 1 (always) | Use on-demand updates via broadcasts for immediate feedback |
| RemoteViews with ConstraintLayout | Only classic layouts supported | Current (2026) | ConstraintLayout never officially supported despite appearing in some examples |

**Deprecated/outdated:**
- **Square Seismic library**: Archived Nov 2024 as read-only; copy the 200-line ShakeDetector.java source file instead
- **FLAG_SHOW_WHEN_LOCKED window flag**: Deprecated API 27; use Activity.setShowWhenLocked()
- **Vibrator.vibrate(long)**: Deprecated API 26; use VibrationEffect or HapticFeedbackConstants
- **Background sensor registration without foreground service**: Restricted API 34; requires foreground service with notification
- **KeyguardManager.KeyguardLock**: Deprecated API 13; don't use for this use case anyway

## Open Questions

Things that couldn't be fully resolved:

1. **Widget responsiveness with app state sync**
   - What we know: Widgets can update via broadcasts, SharedPreferences, or direct AppWidgetManager calls
   - What's unclear: Best pattern for syncing widget state when MainActivity changes state (broadcast from Activity vs polling from widget vs StateFlow observation)
   - Recommendation: Use broadcast from ViewModel when light state changes; widget receiver updates all widget instances. Simple, immediate, battery-efficient.

2. **Shake detection service lifecycle**
   - What we know: Foreground service required on Android 14+ for background sensor access
   - What's unclear: Should service run continuously or start/stop based on user preference toggle? Impact on battery life for continuous accelerometer monitoring at SENSOR_DELAY_GAME.
   - Recommendation: Start service when user enables shake detection in settings, stop when disabled. Include "Disable shake detection" action in foreground notification. Users who enable feature expect some battery trade-off.

3. **Lock screen return behavior**
   - What we know: setShowWhenLocked() shows activity over lock screen; when user dismisses activity, they return to lock screen
   - What's unclear: Whether to explicitly call finish() when light is turned off from lock screen, or let user back button out naturally
   - Recommendation: Call finish() when double-tap turns off light; preserves existing double-tap-to-close behavior. User can also use back gesture/button if preferred. Both routes lead to lock screen.

4. **QS Tile long-press behavior (Claude's discretion)**
   - What we know: TileService supports onClick() and can launch activities
   - What's unclear: What long-press should do - open settings, show more options, or nothing
   - Recommendation: Long-press opens app settings (specifically shake sensitivity and widget config). Standard Android pattern for tiles with configurable behavior. Implement by overriding onClick() and checking for long-press event duration (not directly supported, may need custom solution or just use click).

## Sources

### Primary (HIGH confidence)
- [Create custom Quick Settings tiles](https://developer.android.com/develop/ui/views/quicksettings-tiles) - TileService implementation, lifecycle, metadata
- [Create a simple widget](https://developer.android.com/develop/ui/views/appwidgets) - AppWidgetProvider, RemoteViews, update mechanisms
- [Motion sensors](https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion) - Accelerometer, SensorManager, best practices
- [Add haptic feedback to events](https://developer.android.com/develop/ui/views/haptics/haptic-feedback) - HapticFeedbackConstants, VibrationEffect
- [Activity.setShowWhenLocked()](https://developer.android.com/reference/android/app/Activity#setShowWhenLocked(boolean)) - Lock screen display API
- [PendingIntent](https://developer.android.com/reference/android/app/PendingIntent) - Immutability flags requirement
- [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types) - Android 14+ requirements

### Secondary (MEDIUM confidence)
- [Square Seismic ShakeDetector.java](https://github.com/square/seismic/blob/master/library/src/main/java/com/squareup/seismic/ShakeDetector.java) - Algorithm implementation (archived but proven)
- [All About PendingIntents](https://medium.com/androiddevelopers/all-about-pendingintents-748c8eb8619) - Android Developers Medium, immutability explanation
- [Implementing Android Widgets: A Practical Guide](https://medium.com/@serhii-tereshchenko/implementing-android-widgets-a-practical-guide-396b28325b9a) - RemoteViews limitations
- [How to Show an Activity on Lock Screen](https://victorbrandalise.com/how-to-show-activity-on-lock-screen-instead-of-notification/) - setShowWhenLocked examples

### Tertiary (LOW confidence)
- [Android Tutorial: Implement A Shake Listener](https://jasonmcreynolds.com/?p=388) - Shake threshold (2.7G) source
- [Shake detector in Android using Accelerometer](https://medium.com/@pratistha_05/shake-detector-in-android-using-accelerometer-08aff705927a) - Community implementation
- WebSearch results for general Android development patterns (multiple sources)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All framework APIs with official documentation
- Architecture: HIGH - Official Android documentation and proven patterns (Seismic)
- Pitfalls: HIGH - Based on official docs, API changes, and known Android 12/14 restrictions
- Shake algorithm: MEDIUM - Seismic source code verified but library archived
- Lock screen behavior: HIGH - Official Activity API documentation

**Research date:** 2026-01-31
**Valid until:** 2026-03-31 (60 days - Android framework stable, but foreground service requirements may evolve with Android 16 beta)
