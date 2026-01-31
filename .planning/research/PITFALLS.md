# Domain Pitfalls: Android Sensor/Utility Apps

**Domain:** Android utility apps with sensors and background services
**Researched:** 2026-01-31
**Focus Areas:** Shake detection, volume button capture, lock screen launch, always-on screen, Quick Settings tiles, widgets, CI/CD

---

## Critical Pitfalls

Mistakes that cause rewrites, Play Store rejections, or major battery drain issues.

### Pitfall 1: Excessive Partial Wake Lock Battery Drain

**What goes wrong:**
Starting March 1, 2026, Google Play Store flags and de-ranks apps that hold excessive partial wake locks (>2 cumulative hours in 24 hours for 5% of user sessions over 28 days). Your app gets warning labels, loses prominent discovery placement, and users see battery drain warnings before installation.

**Why it happens:**
Developers hold wake locks to keep sensors running in the background (especially shake detection) but fail to release them promptly or use them when WorkManager would suffice. Keeping the accelerometer sensor registered with a wake lock while the screen is off continuously drains battery.

**Consequences:**
- Play Store de-ranking starting March 2026
- Warning labels on your app listing
- Poor user reviews citing battery drain
- Potential removal from recommended surfaces
- Users uninstalling due to battery impact

**Prevention:**
1. **Never use wake locks for deferrable work** - Use WorkManager, JobScheduler, or AlarmManager instead
2. **Minimize wake lock duration** - Release immediately when sensor reading is complete
3. **Unregister sensors aggressively** - In `onPause()`, not `onDestroy()`
4. **Use sensor batching** - Collect sensor data in batches rather than continuously
5. **Implement smart detection windows** - Only activate shake detection during likely-use periods
6. **Monitor Android Vitals** - Track wake lock usage under Battery Usage metrics
7. **Test in Doze mode** - Verify your app behaves correctly during device idle

**Detection:**
- Android Vitals dashboard shows "Excessive partial wake locks" warnings
- Battery Historian shows long wake lock holds
- Users report battery drain in reviews
- `adb shell dumpsys batterystats` shows your app holding wake locks for hours

**Phase mapping:** Phase 1 (Core Services) - Build wake lock management correctly from the start, as refactoring later is painful.

**Sources:**
- [Google Play Store to flag battery-draining apps March 2026](https://android-developers.googleblog.com/2025/11/raising-bar-on-battery-performance.html)
- [Excessive partial wake locks metric](https://developer.android.com/topic/performance/vitals/wakelock)
- [Wake lock crackdown starting March 1, 2026](https://internetprotocol.co/hitech/2025/11/17/googles-new-android-policy-the-wake-lock-crackdown-starts-march-1/)

---

### Pitfall 2: Foreground Service Type Not Declared (Android 14+)

**What goes wrong:**
Android 14+ **requires** you to declare foreground service types in the manifest AND request the appropriate runtime permission. Apps crash with `MissingForegroundServiceTypeException` or `SecurityException` if you start a foreground service without declaring its type. For shake detection from lock screen, you need `specialUse` type (for Android 14+) and potentially `SYSTEM_ALERT_WINDOW` permission for Android 15+.

**Why it happens:**
Developers upgrade target SDK to 34+ without understanding new foreground service requirements. The old "just start a foreground service" approach no longer works. Android 15 adds even stricter requirements - you must have a visible overlay window if using `SYSTEM_ALERT_WINDOW` before starting foreground services.

**Consequences:**
- App crashes on Android 14+ devices (70%+ of users by 2026)
- Play Console rejects your build for missing declarations
- Background shake detection fails completely
- `dataSync` services auto-terminate after 6 hours (Android 15+)

**Prevention:**
1. **Declare service type in manifest:**
```xml
<service
    android:name=".ShakeDetectionService"
    android:foregroundServiceType="specialUse"
    android:exported="false" />
```

2. **Request runtime permission (Android 14+):**
```kotlin
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```

3. **Provide Play Console justification** - Describe why your app needs this foreground service type in Play Console App Content page

4. **Android 15 overlay requirement** - If using `SYSTEM_ALERT_WINDOW`, you must launch a `TYPE_APPLICATION_OVERLAY` window and make it visible BEFORE starting foreground service

5. **Handle dataSync timeout** - If using `dataSync` type, plan for 6-hour auto-termination in Android 15+

6. **Use notification priority PRIORITY_LOW or higher** - System alerts users if notification priority is too low

**Detection:**
- Crash logs showing `MissingForegroundServiceTypeException`
- Crash logs showing `SecurityException: Permission Denial`
- Play Console build rejection messages
- Service stops unexpectedly after 6 hours (dataSync type)

**Phase mapping:** Phase 1 (Core Services) - Critical to get right before building any background functionality.

**Sources:**
- [Foreground service types required Android 14+](https://developer.android.com/about/versions/14/changes/fgs-types-required)
- [Foreground service changes overview](https://developer.android.com/develop/background-work/services/fgs/changes)
- [Android 15 foreground service restrictions](https://developer.android.com/about/versions/15/behavior-changes-15)
- [Play Console foreground service declaration](https://support.google.com/googleplay/android-developer/answer/13392821?hl=en)

---

### Pitfall 3: Sensor Listeners Not Unregistered - Memory Leaks

**What goes wrong:**
You register `SensorEventListener` for accelerometer (shake detection) in `onCreate()` or `onResume()` but forget to unregister in `onPause()` or `onDestroy()`. The sensor manager holds a reference to your Activity/Fragment, preventing garbage collection. After screen rotations or backgrounding, you leak the entire Activity, causing memory pressure, eventual OutOfMemoryError crashes, and continuous battery drain from sensor callbacks.

**Why it happens:**
1. **Lifecycle mismatch** - Registering in `onCreate()` but unregistering in `onDestroy()` doesn't account for `onPause()`
2. **Configuration changes** - Screen rotation creates new Activity instance, but old listener remains registered
3. **Application context confusion** - Getting SensorManager with activity context instead of application context
4. **Compose DisposableEffect missing** - Jetpack Compose sensor listeners without proper `onDispose` cleanup

**Consequences:**
- Memory leaks after every screen rotation
- Accelerometer continues firing callbacks when app is backgrounded (battery drain)
- OutOfMemoryError crashes after multiple orientation changes
- Battery drain from unnecessary sensor processing
- LeakCanary warnings in development builds

**Prevention:**
1. **Always unregister in onPause():**
```kotlin
override fun onPause() {
    super.onPause()
    sensorManager.unregisterListener(sensorEventListener)
}

override fun onResume() {
    super.onResume()
    sensorManager.registerListener(
        sensorEventListener,
        accelerometer,
        SensorManager.SENSOR_DELAY_NORMAL
    )
}
```

2. **Use DisposableEffect in Compose:**
```kotlin
DisposableEffect(Unit) {
    sensorManager.registerListener(listener, accelerometer, delay)
    onDispose {
        sensorManager.unregisterListener(listener)
    }
}
```

3. **Match lifecycle methods** - `onCreate` → `onDestroy`, `onStart` → `onStop`, `onResume` → `onPause`

4. **Use application context** - Get SensorManager with `applicationContext`, not activity context when possible

5. **Check registration state** - Track whether listener is registered to avoid double-registration

6. **Integrate LeakCanary** - Catch memory leaks during development

**Detection:**
- LeakCanary reports Activity leaks
- Android Profiler shows memory growing after rotations
- Battery Historian shows sensor usage when app backgrounded
- `adb shell dumpsys sensorservice` shows your app still registered
- Users report app getting sluggish over time

**Phase mapping:** Phase 1 (Core Services) - Memory leaks compound over time; fix immediately.

**Sources:**
- [Top 7 Android memory leaks 2025](https://artemasoyan.medium.com/top-7-android-memory-leaks-and-how-to-avoid-them-in-2025-b77e15a7b62e)
- [Memory leak prevention best practices](https://medium.com/android-development-hub/understanding-and-preventing-memory-leaks-in-android-a-comprehensive-guide-75ad5751aa3c)
- [Sensor listener memory leaks](https://www.geeksforgeeks.org/android-memory-leak-causes/)

---

### Pitfall 4: Volume Button Conflicts with System Media Session

**What goes wrong:**
You override `dispatchKeyEvent()` to intercept volume button presses for shake activation/flashlight toggle. This conflicts with active `MediaSession` instances (music players, Bluetooth audio, accessibility features like "Select to Speak"). Users find their volume buttons don't control media anymore, or your interception gets ignored entirely. Google confirmed a major bug in January 2026 where "Select to Speak" accessibility service breaks volume button functionality completely.

**Why it happens:**
1. **MediaSession priority** - Active media sessions intercept volume events before your activity
2. **Accessibility conflicts** - "Select to Speak" and other accessibility services override volume button routing
3. **System audio routing confusion** - Android's audio routing system doesn't know which volume stream to control
4. **Bluetooth audio active** - Remote audio devices capture volume events through their own protocols
5. **Wrong implementation** - Forgetting to return `true` or call `super.dispatchKeyEvent()` appropriately

**Consequences:**
- Volume buttons don't adjust media volume (user confusion and frustration)
- Your volume button shortcut doesn't work when music is playing
- Conflicts with accessibility users (potential accessibility compliance issues)
- Camera apps can't use volume buttons as shutter (if that's your intent)
- Inconsistent behavior across devices/Android versions

**Prevention:**
1. **Check for active audio sessions before intercepting:**
```kotlin
override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    if (audioManager.isMusicActive) {
        return super.dispatchKeyEvent(event) // Let system handle it
    }

    if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN &&
        event.action == KeyEvent.ACTION_DOWN) {
        // Your custom handling
        return true
    }
    return super.dispatchKeyEvent(event)
}
```

2. **Provide toggle in Settings** - Let users enable/disable volume button shortcuts

3. **Document conflicts** - Warn users that volume shortcuts won't work during media playback

4. **Test with accessibility enabled** - Enable "Select to Speak" and test volume button behavior

5. **Consider alternative triggers** - Shake detection, notification actions, Quick Settings tile instead of volume buttons

6. **For Google Cast apps** - Call `CastContext.onDispatchVolumeKeyEventBeforeJellyBean()` on older devices

**Detection:**
- User reports: "Volume buttons don't work for music"
- Volume buttons adjust wrong stream (e.g., ringer instead of media)
- Inconsistent behavior when Bluetooth connected
- Accessibility users report conflicts
- Media playback volume can't be controlled

**Phase mapping:** Phase 2 (Volume Button Integration) - Defer until core shake detection works; volume buttons are a nice-to-have.

**Sources:**
- [Google confirms Android volume button bug January 2026](https://piunikaweb.com/2026/01/08/android-google-pixel-long-press-volume-controls-not-working-workaround/)
- [Android volume bug with Select to Speak](https://cybersecuritynews.com/android-bug-impacts-volume-buttons-function/)
- [Volume button interception conflicts](https://discuss.kotlinlang.org/t/handling-media-buttons/9811)

---

### Pitfall 5: Doze Mode Kills Background Service (Android 14-16)

**What goes wrong:**
Your background shake detection service works perfectly during active use, but users report it "stops working after a while." During Doze mode (device idle for 30+ minutes), Android suspends WorkManager jobs, stops sensor callbacks, and ignores alarms. In Android 14-15, adaptive battery restrictions are even more aggressive - apps opened once or twice per month can't run in background at all. Android 16 (2026) introduces execution quotas **even for active standby bucket apps**.

**Why it happens:**
1. **WorkManager jobs don't run during Doze** - They wait for maintenance windows or Doze exit
2. **Sensor listeners stop firing** - System conserves battery by suspending sensor callbacks
3. **App standby buckets** - Apps in "rare" bucket get severe restrictions (10 jobs/day max)
4. **Android 16 quota limits** - Even "active" apps now face job runtime quotas (previously unlimited)
5. **No exemptions for sensor services** - Unlike alarms, sensors don't have `setAndAllowWhileIdle()` equivalent

**Consequences:**
- Shake detection stops working after device idle for 30+ minutes
- Users shake phone repeatedly, nothing happens, leave negative reviews
- Service restarts when device wakes, but missed events during Doze
- Background jobs don't execute as scheduled in Doze
- App gets placed in restricted bucket, limiting to 1 job per day

**Prevention:**
1. **Request battery optimization exemption (use sparingly):**
```kotlin
val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
    data = Uri.parse("package:$packageName")
}
startActivity(intent)
```
**Warning:** Only for user-visible features (e.g., alarm clock apps). Play Store may reject if not justified.

2. **Use inexact alarms with setAndAllowWhileIdle():**
```kotlin
// Wake up periodically during Doze to check for shake
alarmManager.setAndAllowWhileIdle(
    AlarmManager.RTC_WAKEUP,
    triggerTime,
    pendingIntent
)
```

3. **Use high-priority FCM messages** - Wake your app during Doze for critical events (but don't abuse)

4. **Educate users about Doze** - Explain that shake detection works best when phone is in use

5. **Test in Doze mode:**
```bash
adb shell dumpsys deviceidle force-idle
adb shell dumpsys battery unplug
```

6. **Design for intermittent operation** - Accept that background services can't be 100% reliable on modern Android

7. **Monitor standby bucket** - Track which bucket your app is in and adjust behavior

**Detection:**
- `adb shell dumpsys deviceidle` shows device in idle/doze state
- Service logs stop appearing after 30+ minutes idle
- User reports: "Worked at first, then stopped"
- Battery Historian shows jobs deferred during Doze
- Android Vitals shows high crash rate during Doze exit

**Phase mapping:** Phase 1 (Core Services) - Design architecture assuming Doze will happen; don't rely on continuous background execution.

**Sources:**
- [Doze mode and app standby optimization](https://developer.android.com/training/monitoring-device-state/doze-standby)
- [WorkManager doesn't run during Doze](https://medium.com/@androidlab/the-future-of-background-tasks-on-android-what-post-doze-evolution-means-for-developers-1225e4792863)
- [Android 16 execution quota changes](https://developer.android.com/about/versions/16/behavior-changes-all)
- [App standby buckets restrictions](https://developer.android.com/topic/performance/appstandby)

---

## Moderate Pitfalls

Mistakes that cause delays, technical debt, or poor user experience.

### Pitfall 6: Always-On Screen Causes OLED Burn-In

**What goes wrong:**
Your flashlight/screenlight feature keeps the screen on at max brightness with static UI elements (navigation bar, status icons). After extended use, users get permanent image retention (burn-in) on OLED displays. Status bar icons, navigation buttons, or your app's UI become "ghost images" visible even in other apps.

**Why it happens:**
1. **Static images at high brightness** - Certain pixels emit light continuously while others don't
2. **No pixel shifting** - UI elements stay in exact same position for hours
3. **Navigation bar/status bar always visible** - System UI is most prone to burn-in
4. **User leaves app on overnight** - Unintended extended use cases

**Consequences:**
- User complaints about screen damage
- Potential warranty claims blaming your app
- One-star reviews: "This app ruined my screen"
- App gets flagged in reviews as causing hardware damage

**Prevention:**
1. **Enable immersive mode** - Hide status bar and navigation bar:
```kotlin
window.insetsController?.apply {
    hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}
```

2. **Implement pixel shifting** - Move UI elements slightly every few minutes:
```kotlin
// Shift content by a few pixels every 2 minutes
handler.postDelayed({
    contentView.translationX = Random.nextInt(-5, 5).toFloat()
    contentView.translationY = Random.nextInt(-5, 5).toFloat()
}, 120_000)
```

3. **Auto-timeout warning** - Show warning after 30 minutes of continuous use:
```kotlin
"Screen has been on for 30 minutes. Consider taking a break to prevent screen burn-in."
```

4. **Reduce brightness recommendation** - Default to 70% brightness instead of 100%

5. **Use dark mode** - Emit less light from fewer pixels

6. **Animate/vary content** - Slowly change color or pattern to vary pixel usage

7. **Add disclaimer** - "Prolonged use may cause temporary image retention on some displays"

**Detection:**
- User reviews mentioning "screen burn," "ghost images," or "permanent damage"
- Support requests about screen issues
- Testing: Run app continuously for 2+ hours on OLED display, check for retention

**Phase mapping:** Phase 2 (Always-On Features) - Implement burn-in prevention before launching always-on screen features.

**Sources:**
- [OLED burn-in prevention techniques](https://www.androidauthority.com/screen-burn-in-801760/)
- [Samsung AOD and burn-in mitigation](https://eu.community.samsung.com/t5/galaxy-s25-series/aod-and-screen-burn-in/td-p/11818044)
- [Immersive mode to prevent burn-in](https://www.croma.com/unboxed/what-is-screen-burn-in-how-to-prevent-it)

---

### Pitfall 7: Lock Screen Launch Security Holes

**What goes wrong:**
You launch your app from the lock screen (shake to flashlight) without proper security checks. Users can bypass lock screen authentication to access phone contents, view notifications, or use other apps. This creates a security vulnerability where anyone can shake the locked phone and escape the lock screen.

**Why it happens:**
1. **FLAG_DISMISS_KEYGUARD misuse** - Dismisses lock screen without requiring authentication
2. **showWhenLocked without proper constraints** - Activity appears on lock screen but allows navigation away
3. **Notification actions too permissive** - Notification actions that launch activities without re-authentication
4. **Forgotten setShowWhenLocked cleanup** - Activity remains showWhenLocked even after feature disabled

**Consequences:**
- Security researchers flag your app as lock screen bypass
- Play Store security review rejection
- User data exposed to unauthorized access
- Enterprise/corporate policy violations (app banned from managed devices)

**Prevention:**
1. **Use setShowWhenLocked() correctly:**
```kotlin
// Only for the specific flashlight activity
class FlashlightActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    }
}
```

2. **Don't allow navigation from lock screen activity:**
```kotlin
// Disable back button, no navigation to other screens
override fun onBackPressed() {
    // Finish activity, return to lock screen
    finish()
}
```

3. **Don't dismiss keyguard automatically:**
```kotlin
// NEVER do this from lock screen:
window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD) // BAD
```

4. **Limit functionality on lock screen** - Only show flashlight controls, no app navigation

5. **Require authentication for sensitive features:**
```kotlin
val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
if (keyguardManager.isKeyguardLocked) {
    // Limited functionality only
} else {
    // Full app access
}
```

6. **Test with lock screen enabled** - Verify you can't escape lock screen through your app

7. **Hide sensitive data** - No notification content visible on lock screen from your app

**Detection:**
- Security audit tools flag lock screen bypass
- Manual test: Launch app from lock screen, try to navigate to Settings/other apps
- Android's built-in lock screen policy checks
- Enterprise MDM tools reject app

**Phase mapping:** Phase 3 (Lock Screen Integration) - Build minimal lock screen functionality; defer advanced features.

**Sources:**
- [Lock screen security in AOSP](https://source.android.com/docs/core/display/multi_display/lock-screen)
- [setShowWhenLocked() documentation](https://developer.android.com/reference/android/app/Activity#setShowWhenLocked(boolean))
- [Common permission mistakes in Android](https://blog.oversecured.com/Common-mistakes-when-using-permissions-in-Android/)

---

### Pitfall 8: Quick Settings Tile Lifecycle Crashes

**What goes wrong:**
Your Quick Settings tile for toggling flashlight crashes or becomes unresponsive because you assumed `TileService` lifecycle works like a normal Service. The tile state doesn't update when toggled, or it crashes when clicked. Users swipe down Quick Settings, tap your tile, nothing happens or the entire system UI freezes briefly.

**Why it happens:**
1. **Lifecycle assumptions** - `TileService` lifecycle methods run in separate asynchronous threads from Service lifecycle
2. **State outside onStartListening/onStopListening** - Assuming service stays alive between these calls
3. **Blocking onClick()** - Performing long operations in `onClick()` instead of launching Activity or starting Service
4. **Not updating tile state** - Forgetting to call `tile.updateTile()` after changing state
5. **Multiple binding/unbinding** - Service may be unbound and rebound multiple times during lifecycle

**Consequences:**
- Tile shows wrong state (says "On" when flashlight is off)
- Tapping tile does nothing
- System UI becomes unresponsive
- Tile disappears from Quick Settings panel
- Crash logs with `NullPointerException` on tile access

**Prevention:**
1. **Never assume service persists outside onStartListening/onStopListening:**
```kotlin
override fun onStartListening() {
    super.onStartListening()
    // Update tile state from persistent storage
    updateTileState()
}

override fun onStopListening() {
    super.onStopListening()
    // Don't assume we'll still be alive
}
```

2. **Update tile state properly:**
```kotlin
override fun onClick() {
    super.onClick()

    // Toggle state
    isActive = !isActive

    // Update tile immediately
    qsTile?.apply {
        state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        updateTile()
    }

    // Perform action (start service, etc.)
    startFlashlightService()
}
```

3. **Don't block onClick()** - Launch activity or start service, return immediately:
```kotlin
override fun onClick() {
    super.onClick()
    // Quick state update
    toggleState()
    // Launch activity for complex operations
    val intent = Intent(this, FlashlightActivity::class.java)
    startActivityAndCollapse(intent)
}
```

4. **Use active mode for frequent updates** - Request `android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"` in manifest

5. **Handle null tile gracefully:**
```kotlin
qsTile?.let { tile ->
    tile.state = newState
    tile.updateTile()
} ?: Log.w(TAG, "Tile is null, service may be unbound")
```

6. **Test lifecycle transitions:**
- Add tile, remove tile, add again
- Toggle tile multiple times rapidly
- Open shade, close shade repeatedly
- Reboot device with tile installed

**Detection:**
- Logcat shows multiple `onStartListening()` calls without matching `onCreate()`
- Tile state visually wrong in Quick Settings
- `NullPointerException` when accessing `qsTile` field
- System UI crash logs mentioning your tile service
- Tile becomes unresponsive or disappears

**Phase mapping:** Phase 4 (Quick Settings Tile) - Defer until core flashlight functionality is solid; tiles are convenience feature.

**Sources:**
- [TileService lifecycle documentation](https://developer.android.com/develop/ui/views/quicksettings-tiles)
- [Quick Settings tile lifecycle (AOSP)](https://android.googlesource.com/platform/frameworks/base/+/master/packages/SystemUI/docs/qs-tiles.md)
- [TileService state refresh issues](https://github.com/home-assistant/android/issues/3120)

---

### Pitfall 9: Widget Update ANRs and Battery Drain

**What goes wrong:**
Your home screen widget updates too frequently or performs expensive operations in the broadcast receiver, causing Application Not Responding (ANR) errors. Widgets account for 23% of battery drain complaints in app reviews. Starting March 2026, Google Play flags apps with excessive battery drain from widgets.

**Why it happens:**
1. **Updates too frequent** - Updating every minute instead of every 30+ minutes
2. **Blocking the main thread** - Performing heavy work in `onUpdate()` without using `goAsync()`
3. **Full updates instead of partial** - Recreating entire widget layout instead of updating specific views
4. **No WorkManager for async work** - Trying to do network/database operations synchronously
5. **Broadcasting receivers timeout** - System allows only 10 seconds before ANR

**Consequences:**
- ANR dialog: "Widget is not responding"
- Battery drain warnings in Play Store (March 2026+)
- Widget shows stale data or blank views
- Users remove widget due to poor performance
- Play Store de-ranking due to battery consumption

**Prevention:**
1. **Use appropriate update frequency:**
```xml
<!-- Update every hour, not every minute -->
<appwidget-provider
    android:updatePeriodMillis="3600000"
    android:minWidth="40dp"
    android:minHeight="40dp" />
```

2. **Use goAsync() for long operations:**
```kotlin
override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    val pendingResult = goAsync()

    CoroutineScope(Dispatchers.Default).launch {
        try {
            // Heavy work here
            updateWidgetContent()
        } finally {
            pendingResult.finish()
        }
    }
}
```

3. **Prefer partial updates:**
```kotlin
// Partial update (faster)
val views = RemoteViews(context.packageName, R.layout.widget)
views.setTextViewText(R.id.widget_text, newText)
appWidgetManager.partiallyUpdateAppWidget(widgetIds, views)

// Instead of full update
appWidgetManager.updateAppWidget(widgetIds, views) // Slower
```

4. **Use WorkManager for async updates:**
```kotlin
override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
        .build()
    WorkManager.getInstance(context).enqueue(workRequest)
}
```

5. **Optimize update timing:**
- Only update when widget is actually visible
- Use `AppWidgetManager.getInstance(context).getAppWidgetIds()` to check if widget exists
- Batch updates instead of individual calls

6. **Consider Jetpack Glance** - Modern widget framework with better battery efficiency (up to 80% less consumption reported)

7. **Test ANR scenarios:**
```bash
adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE
```

**Detection:**
- ANR traces in logcat: `executing service [your widget receiver]`
- Android Vitals shows ANR rate
- Battery Historian shows frequent widget updates
- March 2026+: Play Console shows battery drain warnings
- Users report widget causing lag or phone slowdown

**Phase mapping:** Phase 5 (Home Screen Widget) - Widgets are nice-to-have; focus on core functionality first.

**Sources:**
- [Widget battery drain accounts for 23% of complaints](https://medium.com/@hiren6997/glance-widgets-in-2025-how-i-built-a-battery-friendly-android-widget-in-2-hours-14262e4e6f84)
- [Android widget performance optimization](https://developer.android.com/develop/ui/views/appwidgets/advanced)
- [Google Play to flag battery-draining apps March 2026](https://www.india.com/technology/tired-of-fast-battery-drain-androids-new-update-will-expose-power-hungry-apps-by-march-2026-8180515/)

---

### Pitfall 10: GitHub Actions Android CI Failures

**What goes wrong:**
Your Android builds work perfectly in Android Studio locally but fail mysteriously in GitHub Actions CI. Common issues: SDK license acceptance failures, Gradle cache corruption, build tools version mismatches, test report artifacts deleted before inspection, and environment-specific flakiness.

**Why it happens:**
1. **SDK licenses not accepted** - GitHub runners don't have Android SDK licenses pre-accepted
2. **Cache corruption** - Gradle cache gets corrupted between builds
3. **Limited GitHub Actions debugging** - Errors buried deep in build logs, no structured output
4. **Test artifacts deleted** - Test reports deleted when runner exits, can't inspect failures
5. **Flaky instrumented tests** - Tests fail in CI but pass locally due to timing differences
6. **Gradle version mismatches** - Local Gradle wrapper different from CI

**Consequences:**
- Pull requests blocked by failing CI checks
- Hours spent debugging CI-specific issues
- Flaky tests reduce team confidence in CI
- Can't reproduce failures locally
- Build times excessive due to missing caches

**Prevention:**
1. **Use gradle-build-action for deep integration:**
```yaml
- name: Build with Gradle
  uses: gradle/gradle-build-action@v2
  with:
    arguments: build
    cache-read-only: ${{ github.ref != 'refs/heads/main' }}
```

2. **Accept SDK licenses in CI:**
```yaml
- name: Accept Android SDK licenses
  run: yes | sdkmanager --licenses
```

3. **Upload test results on failure:**
```yaml
- name: Upload test results
  if: failure()
  uses: actions/upload-artifact@v4
  with:
    name: test-results
    path: |
      **/build/reports/tests/
      **/build/test-results/
```

4. **Cache Gradle dependencies:**
```yaml
- name: Cache Gradle packages
  uses: actions/cache@v3
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
```

5. **Use Gradle Managed Devices for instrumented tests:**
```kotlin
testOptions {
    managedDevices {
        devices {
            pixel2api30(ManagedVirtualDevice) {
                device = "Pixel 2"
                apiLevel = 30
                systemImageSource = "aosp"
            }
        }
    }
}
```

6. **Retry flaky tests:**
```yaml
- name: Run tests with retry
  uses: gradle/gradle-build-action@v2
  with:
    arguments: test --rerun-tasks
```

7. **Match local environment:**
- Use same JDK version as local
- Use same Gradle version (check `gradle-wrapper.properties`)
- Use same Android Gradle Plugin version

**Detection:**
- CI builds fail with "License not accepted"
- Builds succeed locally but fail in CI
- Test reports show "No tests found" or empty results
- Gradle daemon errors in CI logs
- Excessive build times (>20 minutes for simple app)

**Phase mapping:** Phase 0 (Setup) - Set up robust CI from the start; fixes later are disruptive.

**Sources:**
- [GitHub Actions Gradle build failures](https://github.com/actions/runner-images/issues/7506)
- [Gradle build action for deep integration](https://blog.gradle.org/gh-actions)
- [Debugging GitHub Actions failures with Develocity](https://gradle.com/blog/determine-the-root-cause-of-github-actions-failures-faster-with-gradle-enterprise/)
- [Extract test results on failure](https://github.com/gradle/gradle-build-action/issues/619)

---

## Minor Pitfalls

Mistakes that cause annoyance but are fixable with moderate effort.

### Pitfall 11: Screen Brightness Control Doesn't Persist

**What goes wrong:**
You set `WindowManager.LayoutParams.screenBrightness` to max for your flashlight screen, but when the activity is destroyed and recreated (rotation, backgrounding), brightness returns to system default. Users rotate phone and screen dims unexpectedly.

**Why it happens:**
`screenBrightness` is a per-window setting, not a persistent system setting. When the activity is destroyed, the window is destroyed, and the brightness override is lost.

**Prevention:**
1. **Re-apply brightness in onCreate():**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setBrightness(1.0f) // Max brightness
}

private fun setBrightness(brightness: Float) {
    val layoutParams = window.attributes
    layoutParams.screenBrightness = brightness
    window.attributes = layoutParams
}
```

2. **Save brightness preference** - Store user's chosen brightness level in SharedPreferences

3. **Handle configuration changes** - Brightness persists through rotation if you handle config changes yourself:
```xml
<activity
    android:name=".FlashlightActivity"
    android:configChanges="orientation|screenSize" />
```

**Detection:**
- Screen dims after rotation
- Users report: "Brightness resets when I turn the phone"

**Phase mapping:** Phase 2 (Always-On Features) - Minor UX issue, fix when refining features.

---

### Pitfall 12: Instrumented Tests Flaky Due to Background Services

**What goes wrong:**
Your Espresso UI tests pass sometimes and fail other times (flaky tests). Background services (shake detection) trigger UI changes asynchronously, but Espresso doesn't wait for them. Tests occasionally click buttons before background work completes.

**Why it happens:**
Espresso synchronizes with main thread operations but doesn't automatically wait for background services, custom threads, or async callbacks. Your shake detection service might trigger a UI update 500ms later, but Espresso expects immediate response.

**Prevention:**
1. **Use Espresso Idling Resources:**
```kotlin
class ServiceIdlingResource(private val service: ShakeDetectionService) : IdlingResource {
    private var callback: IdlingResource.ResourceCallback? = null

    override fun isIdleNow(): Boolean {
        val idle = !service.isProcessing
        if (idle && callback != null) {
            callback?.onTransitionToIdle()
        }
        return idle
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.callback = callback
    }

    override fun getName(): String = "ShakeDetectionServiceIdlingResource"
}

// Register in test
IdlingRegistry.getInstance().register(serviceIdlingResource)
```

2. **Disable background services in tests:**
```kotlin
@Before
fun setUp() {
    // Inject mock service that doesn't do background work
    (app as MyApp).serviceModule = MockServiceModule()
}
```

3. **Use test-specific build variants** - Disable real sensor listening in debug/test builds

4. **Avoid arbitrary Thread.sleep()** - Use IdlingResource or waitUntil instead

**Detection:**
- Tests pass 80% of the time, fail 20%
- Failures with "View not found" or "expected state X but was Y"
- Tests fail in CI more often than locally

**Phase mapping:** Phase 6 (Testing) - Add IdlingResources when test suite becomes flaky.

**Sources:**
- [Espresso Idling Resources for background work](https://developer.android.com/training/testing/instrumented-tests/stability)
- [Reducing Espresso test flakiness](https://pspdfkit.com/blog/2020/reduce-flakiness-automated-ui-testing-android/)
- [Background operations cause flaky tests](https://android-ui-testing.github.io/Cookbook/practices/flakiness/)

---

### Pitfall 13: ProGuard/R8 Breaking Reflection-Based Code

**What goes wrong:**
Your production release build crashes with `ClassNotFoundException` or `NoSuchMethodException` despite working perfectly in debug builds. ProGuard/R8 obfuscates or removes classes/methods that are accessed via reflection (e.g., sensor event callbacks registered dynamically, JSON serialization libraries).

**Why it happens:**
R8 optimizes aggressively by default, removing "unused" code and renaming classes/methods. If you access code via reflection, R8 doesn't know it's used and removes it. Debug builds typically don't enable R8, so issues only surface in release builds.

**Prevention:**
1. **Add keep rules for reflection-based code:**
```proguard
# Keep sensor-related classes if using reflection
-keep class com.yourapp.sensors.** { *; }

# Keep data classes for serialization
-keep class com.yourapp.models.** { *; }

# Keep specific methods called via reflection
-keepclassmembers class com.yourapp.SensorManager {
    public void registerListener(...);
}
```

2. **Test release builds regularly:**
```bash
./gradlew assembleRelease
adb install app/build/outputs/apk/release/app-release.apk
```

3. **Use R8 full mode** - More aggressive optimization, catches issues earlier:
```gradle
android {
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

4. **Avoid reflection when possible** - Use direct method calls instead of reflection

5. **Review R8 mappings** - Check `build/outputs/mapping/release/mapping.txt` to see what got renamed

**Detection:**
- Crashes only in release builds, not debug
- `ClassNotFoundException` or `NoSuchMethodException` in production
- App works in debug, crashes immediately in release

**Phase mapping:** Phase 7 (Release Preparation) - Test release builds before first Play Store upload.

**Sources:**
- [ProGuard vs R8 guide](https://medium.com/@manishkumar_75473/proguard-vs-r8-in-android-complete-guide-to-code-shrinking-and-obfuscation-5a34a64adbb7)
- [R8 keep rules configuration](https://android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html)
- [Common R8 configuration mistakes](https://www.guardsquare.com/blog/android-security-and-obfuscation-realities-of-r8)

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| **Phase 1: Core Background Service** | Wake lock battery drain, foreground service type missing, sensor listeners not unregistered | Implement wake lock minimization from start; declare service types in manifest; always unregister sensors in onPause() |
| **Phase 2: Volume Button Integration** | Conflicts with MediaSession, accessibility features | Make volume shortcuts optional; test with "Select to Speak" enabled; provide alternative triggers |
| **Phase 3: Lock Screen Launch** | Security holes allowing lock screen bypass | Use setShowWhenLocked() only; don't allow navigation; limit functionality on lock screen |
| **Phase 4: Always-On Screen** | OLED burn-in from static UI | Enable immersive mode; implement pixel shifting; add auto-timeout warnings |
| **Phase 5: Quick Settings Tile** | Lifecycle crashes, state not updating | Don't assume persistence outside onStartListening/onStopListening; update tile state properly |
| **Phase 6: Home Screen Widget** | ANR errors, battery drain | Use goAsync(); optimize update frequency; prefer partial updates; consider Jetpack Glance |
| **Phase 7: Testing & CI** | Flaky instrumented tests, GitHub Actions failures | Use Espresso Idling Resources; set up gradle-build-action; upload test artifacts on failure |
| **Phase 8: Release** | ProGuard/R8 breaking release builds | Add keep rules for reflection code; test release builds regularly |

---

## Research Confidence Assessment

| Area | Confidence | Basis |
|------|------------|-------|
| Battery Drain (Wake Locks) | **HIGH** | Official Google policy announcements for March 2026 |
| Foreground Service Types | **HIGH** | Android 14-15 official documentation |
| Sensor Memory Leaks | **HIGH** | Multiple sources + standard Android lifecycle practices |
| Volume Button Conflicts | **MEDIUM** | Confirmed bug in Jan 2026, but workarounds incomplete |
| Doze Mode Behavior | **HIGH** | Android 14-16 official documentation |
| Lock Screen Security | **MEDIUM** | Official docs + security blog posts (2017-2025) |
| OLED Burn-in | **MEDIUM** | Community knowledge + manufacturer guidelines |
| Quick Settings Tiles | **MEDIUM** | AOSP documentation + GitHub issues (2022-2024) |
| Widget Performance | **HIGH** | Official Android documentation + 2025-2026 Glance articles |
| GitHub Actions CI | **MEDIUM** | GitHub issues + Gradle blog posts (2020-2026) |
| Brightness Control | **HIGH** | Official Android API documentation |
| Instrumented Test Flakiness | **HIGH** | Official Espresso documentation |
| ProGuard/R8 Issues | **MEDIUM** | Multiple articles + official R8 docs (Nov 2025) |

---

## Key Takeaways for Roadmap Planning

1. **Battery optimization is critical** - March 2026 Play Store changes make wake lock management non-negotiable from Phase 1

2. **Foreground service types must be declared upfront** - Affects architecture decisions; can't retrofit easily

3. **Doze mode is unavoidable** - Design assuming background services will be suspended; don't promise 100% reliability

4. **Lock screen and volume buttons are high-risk features** - Defer to later phases; core flashlight functionality is safer to start with

5. **Testing and CI setup pays dividends early** - Set up GitHub Actions and Espresso IdlingResources in Phase 0/1

6. **OLED burn-in prevention is table stakes** - Implement immersive mode and pixel shifting before launching always-on features

7. **Release builds must be tested** - ProGuard/R8 issues only surface in release mode; test early and often

---

## Sources

**Battery & Wake Locks:**
- [Google Play Store to flag battery-draining apps March 2026](https://android-developers.googleblog.com/2025/11/raising-bar-on-battery-performance.html)
- [Excessive partial wake locks metric](https://developer.android.com/topic/performance/vitals/wakelock)
- [Wake lock crackdown starting March 1, 2026](https://internetprotocol.co/hitech/2025/11/17/googles-new-android-policy-the-wake-lock-crackdown-starts-march-1/)
- [Optimize battery use for task scheduling](https://developer.android.com/develop/background-work/background-tasks/optimize-battery)

**Foreground Services:**
- [Foreground service types required Android 14+](https://developer.android.com/about/versions/14/changes/fgs-types-required)
- [Foreground service changes overview](https://developer.android.com/develop/background-work/services/fgs/changes)
- [Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-15)
- [Play Console foreground service requirements](https://support.google.com/googleplay/android-developer/answer/13392821?hl=en)

**Sensors & Memory:**
- [Top 7 Android memory leaks 2025](https://artemasoyan.medium.com/top-7-android-memory-leaks-and-how-to-avoid-them-in-2025-b77e15a7b62e)
- [Motion sensors documentation](https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion)
- [Shake detection implementation](https://medium.com/@pratistha_05/shake-detector-in-android-using-accelerometer-08aff705927a)

**Volume Buttons:**
- [Google confirms Android volume button bug Jan 2026](https://piunikaweb.com/2026/01/08/android-google-pixel-long-press-volume-controls-not-working-workaround/)
- [Volume button bug with Select to Speak](https://cybersecuritynews.com/android-bug-impacts-volume-buttons-function/)

**Doze Mode & App Standby:**
- [Optimize for Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)
- [App standby buckets](https://developer.android.com/topic/performance/appstandby)
- [Android 16 execution quota changes](https://developer.android.com/about/versions/16/behavior-changes-all)
- [Future of background tasks on Android](https://medium.com/@androidlab/the-future-of-background-tasks-on-android-what-post-doze-evolution-means-for-developers-1225e4792863)

**Lock Screen & Security:**
- [Lock screen in AOSP](https://source.android.com/docs/core/display/multi_display/lock-screen)
- [Common permission mistakes](https://blog.oversecured.com/Common-mistakes-when-using-permissions-in-Android/)
- [SYSTEM_ALERT_WINDOW permission changes](https://developer.android.com/about/versions/15/behavior-changes-15)

**OLED Burn-in:**
- [Screen burn-in prevention](https://www.androidauthority.com/screen-burn-in-801760/)
- [OLED burn-in mitigation guide](https://xdaforums.com/t/optimization-guide-mitigating-oled-burn-in-and-stress-testing-mobile-panels.4775507/)
- [AOD and screen burn-in](https://eu.community.samsung.com/t5/galaxy-s25-series/aod-and-screen-burn-in/td-p/11818044)

**Quick Settings Tiles:**
- [Create custom Quick Settings tiles](https://developer.android.com/develop/ui/views/quicksettings-tiles)
- [Quick Settings tiles documentation (AOSP)](https://android.googlesource.com/platform/frameworks/base/+/master/packages/SystemUI/docs/qs-tiles.md)

**Widgets:**
- [Widget performance optimization](https://developer.android.com/develop/ui/views/appwidgets/advanced)
- [Battery-friendly widgets with Glance (2025)](https://medium.com/@hiren6997/glance-widgets-in-2025-how-i-built-a-battery-friendly-android-widget-in-2-hours-14262e4e6f84)

**Testing & CI:**
- [Espresso test stability](https://developer.android.com/training/testing/instrumented-tests/stability)
- [Reducing UI test flakiness](https://pspdfkit.com/blog/2020/reduce-flakiness-automated-ui-testing-android/)
- [GitHub Actions for Gradle](https://blog.gradle.org/gh-actions)
- [Debugging GitHub Actions failures](https://gradle.com/blog/determine-the-root-cause-of-github-actions-failures-faster-with-gradle-enterprise/)

**ProGuard/R8:**
- [ProGuard vs R8 guide](https://medium.com/@manishkumar_75473/proguard-vs-r8-in-android-complete-guide-to-code-shrinking-and-obfuscation-5a34a64adbb7)
- [Configure R8 keep rules (Nov 2025)](https://android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html)
