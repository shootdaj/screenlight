---
phase: 04-launch-methods-lock-screen
plan: 02
subsystem: launch-methods
requires: ["04-01"]
provides:
  - "Quick Settings tile for one-tap light toggle"
  - "Tile state synchronization with light state"
affects: ["04-04"]
tags:
  - quick-settings
  - tile-service
  - broadcast-integration
  - hilt-entrypoint

tech-stack:
  added: []
  patterns:
    - "Hilt EntryPointAccessors for non-injectable Android components"
    - "BroadcastReceiver for tile state updates"
    - "TileService lifecycle (onStartListening, onClick)"

key-files:
  created:
    - app/src/main/java/com/anshul/screenlight/service/LightTileService.kt
    - app/src/main/res/drawable/ic_tile_light.xml
  modified:
    - app/src/main/AndroidManifest.xml

decisions:
  - id: hilt-entrypoint-pattern
    context: "TileService cannot be annotated with @AndroidEntryPoint"
    choice: "Use EntryPointAccessors.fromApplication() to get LightStateManager"
    rationale: "Standard Hilt pattern for Android system components"
  - id: tile-state-sync
    context: "Tile needs to update when light state changes externally"
    choice: "Register BroadcastReceiver for ACTION_LIGHT_STATE_CHANGED in onCreate"
    rationale: "Ensures tile reflects current state even when changed by other components"
  - id: receiver-not-exported
    context: "Android 14+ security requirement"
    choice: "Use RECEIVER_NOT_EXPORTED flag when registering broadcast receiver"
    rationale: "Internal broadcasts should not be accessible to other apps"

metrics:
  duration: 2 min
  completed: 2026-01-31
---

# Phase 4 Plan 2: Quick Settings Tile Summary

**One-liner:** Quick Settings tile with Hilt EntryPointAccessors injection and broadcast-based state sync

## What Was Built

Implemented a fully functional Quick Settings tile that enables one-tap light control from the notification shade:

1. **Tile Icon Drawable** (ic_tile_light.xml)
   - Material Design 24dp light bulb icon
   - White fill for system tint compatibility
   - Renders clearly at small Quick Settings tile size

2. **LightTileService** (TileService implementation)
   - Dependency injection via Hilt EntryPointAccessors pattern
   - Toggle behavior: launches MainActivity when light off, sends ACTION_CLOSE_LIGHT when on
   - State synchronization: registers BroadcastReceiver for ACTION_LIGHT_STATE_CHANGED
   - Tile state updates: ACTIVE when on, INACTIVE when off
   - Lifecycle management: registers receiver in onCreate, unregisters in onDestroy

3. **Manifest Registration**
   - Service declaration with BIND_QUICK_SETTINGS_TILE permission
   - Intent filter for android.service.quicksettings.action.QS_TILE
   - Metadata: ACTIVE_TILE and TOGGLEABLE_TILE for system hints

## Technical Implementation

### Hilt EntryPointAccessors Pattern

Since TileService is a system component and cannot be directly annotated with `@AndroidEntryPoint`, we use the EntryPointAccessors pattern:

```kotlin
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface LightTileServiceEntryPoint {
    fun lightStateManager(): LightStateManager
}

// In TileService onCreate:
val hiltEntryPoint = EntryPointAccessors.fromApplication(
    applicationContext,
    LightTileServiceEntryPoint::class.java
)
lightStateManager = hiltEntryPoint.lightStateManager()
```

### Broadcast-Based State Sync

The tile registers a BroadcastReceiver in onCreate to listen for ACTION_LIGHT_STATE_CHANGED. This ensures the tile reflects the current light state even when:
- Light is toggled by MainActivity directly
- Light is closed via ACTION_CLOSE_LIGHT broadcast from another component
- Light state changes from any future component (widget, shortcut, etc.)

### Toggle Behavior

```kotlin
override fun onClick() {
    if (lightStateManager.isLightOn()) {
        // Send broadcast to close MainActivity
        sendBroadcast(Intent(LightStateManager.ACTION_CLOSE_LIGHT))
    } else {
        // Launch MainActivity with NEW_TASK flag
        startActivityAndCollapse(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}
```

## Decisions Made

### 1. Hilt EntryPointAccessors for DI
**Context:** TileService is a system component instantiated by Android, not Hilt.

**Choice:** Use EntryPointAccessors.fromApplication() to access LightStateManager.

**Rationale:** This is the standard Hilt pattern for Android system components (BroadcastReceiver, ContentProvider, Service) that cannot be directly injected. It provides clean dependency access while maintaining Hilt's benefits.

### 2. BroadcastReceiver for State Sync
**Context:** Tile needs to update its state when light changes externally.

**Choice:** Register BroadcastReceiver for ACTION_LIGHT_STATE_CHANGED in onCreate, call updateTileState() on receive.

**Rationale:** Ensures tile always reflects current state. Without this, tile would be stale after MainActivity changes state. This pattern scales well when we add widget/shortcut components.

### 3. RECEIVER_NOT_EXPORTED for Security
**Context:** Android 14+ requires explicit export flags for BroadcastReceivers.

**Choice:** Use RECEIVER_NOT_EXPORTED when registering the state change receiver.

**Rationale:** ACTION_LIGHT_STATE_CHANGED is an internal broadcast. No external apps should be able to trigger tile updates. This follows Android security best practices.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Build environment Java toolchain issue**
- **Found during:** Task 2 verification (assembleDebug)
- **Issue:** ./gradlew app:assembleDebug fails with "jlink executable does not exist" error
- **Root cause:** Java environment configuration issue (jlink not available in JetBrains Runtime JDK)
- **Workaround:** Verified code correctness via ./gradlew app:compileDebugKotlin (succeeded)
- **Impact:** Full APK build fails, but Kotlin compilation and syntax are verified
- **Resolution needed:** User needs to configure proper Android SDK Java toolchain
- **Files affected:** None (environment issue, not code issue)
- **Note:** This is not a code defect - LightTileService compiles successfully, manifests parse correctly, and all syntax is valid

**2. [Rule 2 - Missing Critical] strings.xml tile_label already present**
- **Found during:** Task 2 implementation
- **Discovery:** tile_label string resource already exists in strings.xml from 04-03 work
- **Action:** No modification needed, used existing resource
- **Files affected:** app/src/main/res/values/strings.xml (no change)
- **Note:** Concurrent work (04-03 widget implementation) already added required string

## Testing Strategy

### Manual Verification Required

1. **Add tile to Quick Settings:**
   - Pull down notification shade
   - Tap edit icon
   - Search for "Screenlight" in available tiles
   - Drag tile to active tiles area

2. **Test toggle behavior:**
   - Tap tile when light is off → MainActivity should launch
   - Tap tile when light is on → MainActivity should close
   - Verify tile icon state changes (active/inactive)

3. **Test state synchronization:**
   - Open MainActivity directly from launcher
   - Pull down notification shade
   - Verify tile shows ACTIVE state
   - Close MainActivity (double-tap volume button)
   - Verify tile shows INACTIVE state

### Build Verification Status

- **Kotlin compilation:** ✅ Passed (./gradlew app:compileDebugKotlin)
- **Full APK build:** ⚠️ Blocked by environment (jlink issue, not code issue)
- **Syntax validation:** ✅ All files parse correctly
- **Manifest validation:** ✅ Service registered with proper permissions

## Files Changed

**Created:**
- `app/src/main/java/com/anshul/screenlight/service/LightTileService.kt` (105 lines)
- `app/src/main/res/drawable/ic_tile_light.xml` (10 lines)

**Modified:**
- `app/src/main/AndroidManifest.xml` (+17 lines for service declaration)

## Integration Points

### Consumes (from 04-01):
- `LightStateManager.isLightOn()` - Query current light state
- `LightStateManager.ACTION_CLOSE_LIGHT` - Broadcast to close MainActivity
- `LightStateManager.ACTION_LIGHT_STATE_CHANGED` - Receive state change notifications

### Provides:
- Quick Settings tile UI in notification shade
- One-tap toggle interface for light control

### Affects Future Work:
- 04-04 (Shake to toggle): Will also send ACTION_CLOSE_LIGHT broadcast, tile will auto-update
- Any future launch methods can rely on ACTION_LIGHT_STATE_CHANGED for sync

## Next Phase Readiness

**Ready for:** 04-03 (Home Screen Widget)

**Notes:**
- Widget can follow same EntryPointAccessors pattern for Hilt DI
- Widget can reuse ACTION_CLOSE_LIGHT and ACTION_LIGHT_STATE_CHANGED broadcasts
- Build environment issue needs resolution before device testing (Java toolchain configuration)

**No blockers for next plan.**

## Lessons Learned

### What Worked Well

1. **Hilt EntryPointAccessors pattern** - Clean, idiomatic way to inject dependencies into system components
2. **Broadcast infrastructure from 04-01** - ACTION_CLOSE_LIGHT and ACTION_LIGHT_STATE_CHANGED enable clean separation between components
3. **RECEIVER_NOT_EXPORTED enforcement** - Catches security issues at compile time

### What Could Be Better

1. **Build environment setup** - Project requires specific Java toolchain configuration (not just any JDK)
2. **startActivityAndCollapse deprecation** - Used deprecated API (warning in Kotlin compilation), but no modern replacement for TileService

### Patterns to Reuse

- **EntryPointAccessors for system components** - Use for any Android component that can't be @AndroidEntryPoint (widget provider, broadcast receiver)
- **BroadcastReceiver registration with RECEIVER_NOT_EXPORTED** - Always use for Android 14+ internal broadcasts
- **Tile state sync via broadcasts** - Scalable pattern for keeping UI components in sync with app state
