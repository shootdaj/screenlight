---
phase: 04-launch-methods-lock-screen
plan: 04
subsystem: launch-methods
completed: 2026-01-31
duration: 4 min
tags: [shake-detection, sensors, foreground-service, haptics, gestures]
requires:
  - 04-01-PLAN (LightStateManager, ACTION_CLOSE_LIGHT broadcast)
provides:
  - Shake-to-launch gesture from anywhere including lock screen
  - ShakeDetector with Seismic algorithm
  - ShakeDetectionService foreground service
  - ShakePreferences for sensitivity control
affects:
  - Future settings UI will configure shake sensitivity
  - Future onboarding may need to explain shake gesture
tech-stack:
  added:
    - Android SensorManager (accelerometer)
    - VibrationEffect API (haptic feedback)
  patterns:
    - Foreground service with specialUse type for background sensors
    - EntryPointAccessors for service-side Hilt injection
    - Seismic algorithm (3/4 samples in 0.25s window)
    - Cooldown pattern (1s) to prevent rapid toggling
key-files:
  created:
    - app/src/main/java/com/anshul/screenlight/service/ShakeDetector.kt
    - app/src/main/java/com/anshul/screenlight/data/preferences/ShakePreferences.kt
    - app/src/main/java/com/anshul/screenlight/service/ShakeDetectionService.kt
    - app/src/main/res/drawable/ic_notification_shake.xml
  modified:
    - app/src/main/AndroidManifest.xml
    - app/src/main/java/com/anshul/screenlight/MainActivity.kt
decisions:
  - decision: "Seismic algorithm with 3/4 samples rule"
    rationale: "Proven algorithm from Square library, good balance of sensitivity and false positives"
    phase: "04-04"
  - decision: "1-second shake cooldown"
    rationale: "Prevents accidental rapid toggling while allowing intentional re-shake"
    phase: "04-04"
  - decision: "SENSITIVITY_LIGHT (11) as default"
    rationale: "Lower barrier for gesture discovery, users can increase if too sensitive"
    phase: "04-04"
  - decision: "foregroundServiceType=\"specialUse\" for sensor access"
    rationale: "Android 14+ requirement for background sensor access, prevents crash"
    phase: "04-04"
  - decision: "EntryPointAccessors for LightStateManager in service"
    rationale: "Services can't use field injection, EntryPoint pattern is Hilt recommended approach"
    phase: "04-04"
  - decision: "50ms haptic vibration on shake detection"
    rationale: "Provides tactile confirmation gesture detected, works across API levels"
    phase: "04-04"
---

# Phase 4 Plan 4: Shake Gesture Detection Summary

**One-liner:** Background shake detection using Seismic algorithm with haptic confirmation and 1s cooldown

## What Was Built

Implemented shake-to-launch gesture detection that works from anywhere, including lock screen:

**ShakeDetector (Seismic Algorithm):**
- Copied proven shake detection algorithm from Square's Seismic library
- Calculates acceleration magnitude and compares to sensitivity threshold
- Maintains queue of 40 samples (~0.5s at SENSOR_DELAY_GAME)
- Triggers when 3/4 of samples in 0.25s+ window exceed threshold
- Three sensitivity levels: LIGHT (11), MEDIUM (13), HARD (15)

**ShakePreferences:**
- SharedPreferences storage for enabled state (default: true)
- Sensitivity level persistence (default: SENSITIVITY_LIGHT)
- Singleton injection via Hilt

**ShakeDetectionService:**
- Foreground service with IMPORTANCE_LOW notification
- SensorEventListener for accelerometer events
- 1-second cooldown prevents rapid toggling
- Haptic feedback on detection (API-versioned: EFFECT_CLICK on Q+, one-shot on O+, vibrate on older)
- Queries LightStateManager to decide action:
  - Light off → Launch MainActivity with FLAG_ACTIVITY_NEW_TASK
  - Light on → Send ACTION_CLOSE_LIGHT broadcast
- Uses EntryPointAccessors pattern for service-side Hilt injection

**Manifest & Permissions:**
- VIBRATE permission for haptic feedback
- FOREGROUND_SERVICE and FOREGROUND_SERVICE_SPECIAL_USE for background service
- POST_NOTIFICATIONS for foreground notification (Android 13+)
- Service declared with foregroundServiceType="specialUse" and shake_detection subtype

**MainActivity Integration:**
- Starts ShakeDetectionService on launch if enabled in preferences
- Uses ContextCompat.startForegroundService() for API compatibility

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 | bf44814 | Create ShakeDetector algorithm and ShakePreferences |
| 2 | c8d1298 | Create ShakeDetectionService foreground service |
| 3 | 9ce861b | Register service in manifest and add permissions |

## Decisions Made

**Seismic Algorithm Adoption:**
- Used proven 3/4 samples rule from Square's archived library
- Algorithm calculates magnitude squared (avoids expensive sqrt)
- Threshold = sensitivity × 2.5, compared against magnitude squared
- Window-based detection (0.25s minimum) prevents noise triggers

**Sensitivity Default:**
- SENSITIVITY_LIGHT (11) chosen as default for easy gesture discovery
- Users can increase sensitivity in settings if too sensitive
- Range from 11-15 covers most use cases without being extreme

**Cooldown Implementation:**
- 1-second cooldown prevents accidental rapid toggling
- Long enough to prevent "double shake" issues
- Short enough to allow intentional re-shake if needed

**Foreground Service Type:**
- specialUse with shake_detection subtype required for Android 14+
- Alternative would be location/camera/microphone types (inappropriate)
- IMPORTANCE_LOW notification minimizes user distraction

**EntryPoint Pattern:**
- Services can't use @Inject field injection in Hilt
- EntryPointAccessors is recommended pattern for accessing singletons from services
- Lazy initialization ensures LightStateManager available when needed

**Haptic Feedback:**
- Provides tactile confirmation without visual requirement
- API-versioned implementation ensures compatibility back to API 21
- 50ms duration is Material Design recommended for quick feedback

## Issues Encountered

**None** - Plan executed exactly as written.

## Testing Notes

**Build Verification:**
- ✅ `./gradlew app:assembleDebug` completes without errors
- ✅ ShakeDetector implements Seismic algorithm with 3/4 samples rule
- ✅ ShakeDetectionService uses foreground service with notification
- ✅ AndroidManifest includes all required permissions
- ✅ Service declaration includes foregroundServiceType="specialUse"

**Manual Testing Required:**
- Device testing needed to verify shake sensitivity levels
- Lock screen shake-to-launch requires physical device
- Haptic feedback confirmation needs real hardware
- Cooldown timing verification (1-second feels natural)

## Next Phase Readiness

**Ready for:**
- Settings UI to configure shake sensitivity (LIGHT/MEDIUM/HARD)
- Settings toggle to enable/disable shake detection
- Onboarding flow explaining shake gesture

**Dependencies for future work:**
- Settings UI plan should read/write ShakePreferences
- Shake sensitivity selector (RadioGroup or Slider)
- Toggle to stop/start ShakeDetectionService

**Potential Issues:**
- **Doze mode:** Android suspends sensors during deep sleep, shake may not work when screen off for extended periods - This is expected behavior, document in help/FAQ
- **Battery impact:** Accelerometer sensor is low-power, but foreground service does consume some battery - Monitor in testing
- **Accessibility conflict:** Some accessibility features use shake gestures - Consider making shake optional in settings
- **False positives:** Walking/running may trigger shake detection - User can increase sensitivity or disable if problematic

## Architecture Notes

**Service Lifecycle:**
- ShakeDetectionService starts from MainActivity.onCreate()
- Returns START_STICKY to survive process death
- Runs indefinitely until explicitly stopped
- Future: Settings toggle should stopService() when disabled

**State Query Pattern:**
- Service queries LightStateManager.isLightOn() on each shake
- Decides whether to launch or close based on current state
- Clean separation: service doesn't track state, only queries it

**Sensor Registration:**
- SENSOR_DELAY_GAME (~20ms) balances responsiveness with battery
- Faster than UI (60ms) but slower than FASTEST (0ms)
- Seismic library recommendation for gesture detection

**EntryPoint Pattern (Hilt Service Injection):**
```kotlin
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface LightStateManagerEntryPoint {
    fun lightStateManager(): LightStateManager
}

private val lightStateManager: LightStateManager by lazy {
    val hiltEntryPoint = EntryPointAccessors.fromApplication(
        applicationContext,
        LightStateManagerEntryPoint::class.java
    )
    hiltEntryPoint.lightStateManager()
}
```
This pattern required because Service @Inject fields not supported in Hilt.

## Deviations from Plan

**None** - Plan executed exactly as written.

## Knowledge Captured

**Seismic Algorithm Implementation:**
- Queue-based approach maintains recent history for pattern detection
- Magnitude squared avoids expensive Math.sqrt() operation
- Timestamp-based windowing ensures minimum duration
- Clear queue after trigger prevents repeated callbacks

**Android Foreground Service Best Practices:**
- IMPORTANCE_LOW notification for non-critical background tasks
- setOngoing(true) prevents user dismissal during active service
- Notification channel creation gated by Build.VERSION.SDK_INT >= O
- foregroundServiceType declaration required in manifest for Android 14+

**Haptic Feedback API Evolution:**
- API 29+: VibrationEffect.EFFECT_CLICK (predefined tactile pattern)
- API 26-28: VibrationEffect.createOneShot(duration, amplitude)
- API 21-25: Vibrator.vibrate(duration) - deprecated but still works
- Material Design guideline: 50ms for quick confirmation feedback

**EntryPointAccessors Pattern:**
- Hilt's solution for injecting into classes not directly supported (Services, BroadcastReceivers)
- Define @EntryPoint interface in singleton component
- Access via EntryPointAccessors.fromApplication()
- Lazy initialization recommended to defer creation

---

**Execution time:** 4 minutes
**Plan type:** Autonomous execution
**Wave:** 2 (Lock screen integration wave)
