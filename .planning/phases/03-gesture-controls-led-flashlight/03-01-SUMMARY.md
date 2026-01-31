---
phase: 03-gesture-controls-led-flashlight
plan: 01
subsystem: sensors
tags: [sensor-manager, camera-manager, tilt-gesture, flashlight, cold-flow, hilt]

# Dependency graph
requires:
  - phase: 02-core-services-screen-light
    provides: Cold Flow pattern from AmbientLightManager, Hilt DI architecture
provides:
  - TiltGestureManager for pitch/roll tilt detection using game rotation vector sensor
  - FlashlightController for LED torch control with state tracking
  - TiltMapper utilities for angle-to-brightness and angle-to-color-index conversion
  - CameraManager DI provider in AppModule
affects: [03-gesture-controls-led-flashlight, gesture-ui, settings]

# Tech tracking
tech-stack:
  added: []
  patterns: [Cold Flow for sensor observation, TorchCallback for state sync]

key-files:
  created:
    - app/src/main/java/com/anshul/screenlight/data/sensor/TiltGestureManager.kt
    - app/src/main/java/com/anshul/screenlight/data/device/FlashlightController.kt
  modified:
    - app/src/main/java/com/anshul/screenlight/di/AppModule.kt

key-decisions:
  - "Use TYPE_GAME_ROTATION_VECTOR for fused accelerometer+gyroscope data without magnetometer noise"
  - "SENSOR_DELAY_UI (~60ms) for responsive gesture detection"
  - "TorchCallback for automatic state synchronization across apps"
  - "Pitch range [-45, 0] maps to brightness [0, 1] (upright to flat = dim to bright)"
  - "Roll range [-45, +45] maps to color index (left tilt = warm, right tilt = cool)"

patterns-established:
  - "TiltMapper pattern: Angle-to-value mapping utilities as companion object"
  - "Silent failure pattern: Graceful handling of CameraAccessException when camera in use"

# Metrics
duration: 2min
completed: 2026-01-31
---

# Phase 3 Plan 01: Data Layer Components Summary

**TiltGestureManager with game rotation vector sensor and FlashlightController with CameraManager torch control, following Cold Flow pattern for lifecycle-aware operation**

## Performance

- **Duration:** 2 min
- **Started:** 2026-01-31T11:49:33Z
- **Completed:** 2026-01-31T11:51:27Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- TiltGestureManager emits pitch/roll angles via Cold Flow using TYPE_GAME_ROTATION_VECTOR
- FlashlightController controls LED torch via CameraManager with TorchCallback state tracking
- TiltMapper utilities for converting tilt angles to brightness (0-1) and color index (0 to N-1)
- Both managers injectable as singletons via Hilt DI

## Task Commits

Each task was committed atomically:

1. **Task 1: Create TiltGestureManager with Cold Flow pattern** - `2a8a374` (feat)
2. **Task 2: Create FlashlightController with TorchCallback** - `5ca3570` (feat)

## Files Created/Modified
- `app/src/main/java/com/anshul/screenlight/data/sensor/TiltGestureManager.kt` - Tilt gesture detection with pitch/roll angles from game rotation vector sensor
- `app/src/main/java/com/anshul/screenlight/data/device/FlashlightController.kt` - LED torch control with CameraManager and state tracking via TorchCallback
- `app/src/main/java/com/anshul/screenlight/di/AppModule.kt` - Added CameraManager provider for dependency injection

## Decisions Made
- **Use TYPE_GAME_ROTATION_VECTOR over deprecated TYPE_ORIENTATION:** Provides fused accelerometer+gyroscope data without magnetometer noise for stable tilt detection
- **SENSOR_DELAY_UI (~60ms) for gesture response:** Balances responsiveness with battery efficiency for UI-driven gesture controls
- **TorchCallback for state synchronization:** Automatically tracks torch state changes from other apps or system, ensuring accurate state representation
- **Pitch mapping [-45, 0] to brightness [0, 1]:** Phone upright-ish (-45 deg) = dim, flat (0 deg) = bright - natural gesture for brightness control
- **Roll mapping [-45, +45] to color index:** Left tilt = warm colors (low index), right tilt = cool colors (high index) - intuitive color selection
- **Silent failure for CameraAccessException:** Gracefully handle camera-in-use scenarios without crashing or user-facing errors

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - implementation followed existing Cold Flow pattern from AmbientLightManager with no complications.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

**Ready for next plans:**
- TiltGestureManager ready for UI integration to map pitch to brightness and roll to color selection
- FlashlightController ready for lock screen toggle integration
- Both managers follow established Cold Flow pattern for consistent lifecycle management

**No blockers.**

**Notes:**
- Tilt angle ranges [-45, 0] for pitch and [-45, +45] for roll are initial values based on research - may need UX adjustment after user testing
- FlashlightController requires no CAMERA permission since API 23 (setTorchMode() doesn't require it)
- Sensor listeners will need proper unregistration in onPause() to prevent memory leaks (already handled by Cold Flow awaitClose)

---
*Phase: 03-gesture-controls-led-flashlight*
*Completed: 2026-01-31*
