---
phase: 03-gesture-controls-led-flashlight
plan: 02
subsystem: ui
tags: [viewmodel, gesture-controls, volume-buttons, double-tap, tilt-observation, hilt]

# Dependency graph
requires:
  - phase: 03-gesture-controls-led-flashlight
    plan: 01
    provides: TiltGestureManager, FlashlightController, TiltMapper utilities
  - phase: 02-core-services-screen-light
    provides: LightViewModel, SettingsRepository, Compose UI architecture
affects: [03-gesture-controls-led-flashlight, settings-ui, accessibility]

# Tech tracking
tech-stack:
  added: []
  patterns: [Volume button event interception, Double-click time window detection, Tilt observation lifecycle management]

key-files:
  created: []
  modified:
    - app/src/main/java/com/anshul/screenlight/ui/viewmodel/LightViewModel.kt
    - app/src/main/java/com/anshul/screenlight/MainActivity.kt
    - app/src/main/java/com/anshul/screenlight/ui/screen/LightScreen.kt

key-decisions:
  - "Volume button events consumed (return true) to prevent system volume popup during gesture control"
  - "Double-click time window of 300ms for volume button detection"
  - "Tilt observation starts only when volume button held, stops on release (prevents accidental adjustments)"
  - "Screen double-tap uses HapticFeedbackType.LongPress for tactile confirmation"
  - "GestureState.isLightOn tracks on/off state separate from brightness value (enables toggle with saved brightness)"
  - "lastNonZeroBrightness saved on volume release and light toggle-off for restore on toggle-on"

patterns-established:
  - "Gesture state management: Separate StateFlow for gesture-specific state (GestureState) vs UI state (LightUiState)"
  - "Volume button lifecycle: onKeyDown → startTiltObservation, onKeyUp → stopTiltObservation, prevents sensor leak"
  - "Double-click detection: Track lastClickTime with threshold comparison, reset on double-click success"
  - "App close signal: shouldCloseApp flag set by ViewModel, observed by Activity, cleared after consumption"

# Metrics
duration: 4min
completed: 2026-01-31
---

# Phase 3 Plan 02: Gesture UI Integration Summary

**Complete gesture control wiring: volume button events control tilt observation and flashlight, screen double-tap toggles light, volume double-click closes app, all with eyes-free operation**

## Performance

- **Duration:** 4 min
- **Started:** 2026-01-31T11:55:28Z
- **Completed:** 2026-01-31T11:59:48Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- Extended LightViewModel with GestureState for gesture-specific state management
- Injected TiltGestureManager and FlashlightController into LightViewModel
- Implemented five gesture handlers: onVolumeButtonDown, onVolumeButtonUp, onVolumeButtonLongPress, onVolumeButtonDoubleClick, onScreenDoubleTap
- Tilt observation lifecycle controlled by volume button hold (start on press, stop on release)
- MainActivity intercepts volume button events (onKeyDown/onKeyUp/onKeyLongPress) and prevents system volume changes
- Double-click detection with 300ms time window for volume button double-click
- LightScreen detects screen double-tap with detectTapGestures and haptic feedback
- App close signal via gestureState.shouldCloseApp observed in MainActivity LaunchedEffect
- Brightness/color saved and restored when toggling light on/off via double-tap

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend LightViewModel with gesture state management** - `6c70a5f` (feat)
2. **Task 2: Add volume button and double-tap gesture handling** - `711da13` (feat)

## Files Created/Modified
- `app/src/main/java/com/anshul/screenlight/ui/viewmodel/LightViewModel.kt` - Added GestureState, tilt observation control, gesture handlers, torch state observation
- `app/src/main/java/com/anshul/screenlight/MainActivity.kt` - Volume button event handling (onKeyDown/onKeyUp/onKeyLongPress), double-click detection, shouldCloseApp observer
- `app/src/main/java/com/anshul/screenlight/ui/screen/LightScreen.kt` - Screen double-tap detection with haptic feedback via pointerInput modifier

## Decisions Made

**1. Volume button event consumption**
- **Context:** Need to prevent Android system volume popup during gesture control
- **Decision:** Return `true` from onKeyDown/onKeyUp/onKeyLongPress to consume events
- **Impact:** Volume buttons no longer adjust system volume while app is foreground (expected behavior for dedicated light control)
- **Rationale:** Eyes-free operation requires gestures without visual distractions (volume popup would break UX)

**2. Tilt observation lifecycle tied to volume button hold**
- **Context:** Prevent accidental brightness/color changes during normal phone use
- **Decision:** Start tilt observation only when volume button pressed, stop when released
- **Impact:** Sensor only active during deliberate gesture, battery efficient, no interference with other apps
- **Rationale:** Cold Flow pattern enables lifecycle control, matches user expectation (explicit activation)

**3. Double-click time window of 300ms**
- **Context:** Balance between easy double-click detection and accidental triggers
- **Decision:** Use 300ms threshold between volume clicks
- **Impact:** Fast double-click required to close app (prevents accidental closure)
- **Rationale:** Standard Android double-click timing (shorter than long-press 500ms), fast enough for deliberate gesture

**4. Separate GestureState from LightUiState**
- **Context:** Gesture-specific state (isVolumeHeld, isTorchOn, shouldCloseApp) distinct from UI rendering state
- **Decision:** Create separate GestureState data class with dedicated StateFlow
- **Impact:** Clean separation of concerns, easier testing, clearer state ownership
- **Rationale:** UI state represents "what to render", gesture state represents "what gesture mode is active"

## Deviations from Plan

None - plan executed exactly as written.

## Technical Notes

**Volume Button Event Flow:**
1. User presses volume button
2. `MainActivity.onKeyDown()` called → `event.startTracking()` enables long-press
3. `viewModel.onVolumeButtonDown()` called → `isVolumeHeld = true`, starts tilt observation
4. While held: TiltGestureManager emits tilt data → TiltMapper converts to brightness/color → ViewModel updates settings
5a. **Short press:** User releases → `MainActivity.onKeyUp()` → `viewModel.onVolumeButtonUp()` → stops tilt observation
5b. **Long press:** `MainActivity.onKeyLongPress()` → `viewModel.onVolumeButtonLongPress()` → toggles flashlight
5c. **Double-click:** Second click within 300ms → `viewModel.onVolumeButtonDoubleClick()` → `shouldCloseApp = true` → Activity finishes

**Screen Double-Tap Flow:**
1. User double-taps screen
2. `detectTapGestures(onDoubleTap)` callback triggered
3. Haptic feedback performed (HapticFeedbackType.LongPress)
4. `viewModel.onScreenDoubleTap()` called
5a. **If light on:** Save current brightness to `lastNonZeroBrightness`, set brightness to 0, `isLightOn = false`
5b. **If light off:** Restore `lastNonZeroBrightness`, `isLightOn = true`

**Sensor Lifecycle Safety:**
- Tilt observation uses `viewModelScope.launch` → cancels automatically on ViewModel clear
- `tiltJob?.cancel()` called on volume release → prevents sensor leak if job still running
- Cold Flow pattern ensures sensor unregisters when flow collection stops
- No explicit onPause handling needed (ViewModel scoped, Activity finish clears ViewModel)

## Known Limitations

**Testing:**
- Manual device testing required for gesture verification (unit tests cannot simulate volume buttons or screen taps)
- Automated UI tests could cover double-tap but not volume button events (KeyEvent injection requires rooted device)

**Accessibility:**
- Volume button gestures conflict with "Select to Speak" accessibility feature (Google bug as of Jan 2026)
- Mitigation: Future plan should add settings toggle to disable volume gestures
- Alternative: Use shake gesture for brightness/color instead (Phase 3 plan 03)

**Edge Cases:**
- Multiple rapid volume clicks could trigger both double-click and long-press (current impl cancels long-press on event.isCanceled check)
- Tilt observation continues if volume button released during long-press (harmless, stops on next release)
- Screen double-tap while volume held will toggle light but not affect tilt-based brightness (expected behavior)

## Integration Points

**With Plan 03-01 (Data Layer):**
- LightViewModel consumes TiltGestureManager.observeTilt() Flow
- LightViewModel consumes FlashlightController.torchState StateFlow
- TiltMapper.pitchToBrightness() and rollToColorIndex() used for gesture mapping

**With Phase 2 (Core Services):**
- SettingsRepository.updateBrightness() and updateColor() persist gesture changes
- LightUiState.settings drives UI rendering with gesture-modified values
- BatteryMonitor LOW_BATTERY_MAX_BRIGHTNESS constraint still applied during tilt gestures

**With Future Plans:**
- Plan 03-03: Shake gesture will need similar observation lifecycle pattern
- Plan 04-01: Lock screen launch will need to preserve gesture state across Activity recreation
- Settings UI: Will expose toggle for enabling/disabling gesture controls (accessibility fallback)

## Next Phase Readiness

**Blockers:** None

**Concerns:**
- Gesture controls need manual testing on physical device with sensors
- Volume button double-click may be hard to discover (no UI hint) - consider adding tutorial on first launch
- Tilt angle ranges [-45, 0] for pitch and [-45, +45] for roll are untested assumptions - may need tuning based on user feedback

**Recommendations for Plan 03-03 (Shake Gesture):**
1. Follow same Cold Flow observation pattern for shake detection
2. Consider adding gesture sensitivity settings (low/medium/high) for tilt and shake
3. Add haptic feedback for all gesture activations (not just double-tap)
4. Consider vibration pattern to indicate gesture mode entry (e.g., short buzz on volume press)
