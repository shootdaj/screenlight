# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-31)

**Core value:** Instant, gesture-controlled light that works from lock screen — no fumbling with UI in the dark.
**Current focus:** Phase 4 Complete — All launch methods implemented

## Current Position

Phase: 4 of 4 (Launch Methods & Lock Screen)
Plan: 4 of 4 complete
Status: Phase complete
Last activity: 2026-01-31 — Completed 04-04-PLAN.md

Progress: [██████████] 100%

## Performance Metrics

**Velocity:**
- Total plans completed: 11
- Average duration: 4.2 min
- Total execution time: 0.8 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-project-setup-ci-cd | 2 | 13 min | 6.5 min |
| 02-core-services-screen-light | 3 | 18 min | 6.0 min |
| 03-gesture-controls-led-flashlight | 2 | 6 min | 3.0 min |
| 04-launch-methods-lock-screen | 4 | 12 min | 3.0 min |

**Recent Trend:**
- Last 5 plans: 04-01 (3 min), 04-02 (2 min), 04-03 (3 min), 04-04 (4 min)
- Trend: Excellent velocity, 3min average for Phase 4

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

| Decision | Context | Plan |
|----------|---------|------|
| Seismic algorithm with 3/4 samples rule | Proven algorithm from Square library, good balance of sensitivity and false positives | 04-04 |
| 1-second shake cooldown | Prevents accidental rapid toggling while allowing intentional re-shake | 04-04 |
| SENSITIVITY_LIGHT (11) as default | Lower barrier for gesture discovery, users can increase if too sensitive | 04-04 |
| foregroundServiceType="specialUse" for sensor access | Android 14+ requirement for background sensor access, prevents crash | 04-04 |
| EntryPointAccessors for LightStateManager in service | Services can't use field injection, EntryPoint pattern is Hilt recommended approach | 04-04 |
| 50ms haptic vibration on shake detection | Provides tactile confirmation gesture detected, works across API levels | 04-04 |
| RemoteViews-compatible views only | AppWidget layouts must use FrameLayout/LinearLayout/ImageView (no ConstraintLayout) | 04-03 |
| PendingIntent.FLAG_IMMUTABLE | Android 12+ security requirement for all PendingIntents | 04-03 |
| Hilt EntryPointAccessors for AppWidgetProvider | AppWidgetProvider cannot be @AndroidEntryPoint, use EntryPointAccessors.fromApplication() | 04-03 |
| Hilt EntryPointAccessors for TileService | TileService cannot be @AndroidEntryPoint, use EntryPointAccessors.fromApplication() | 04-02 |
| BroadcastReceiver for tile state sync | Tile updates when light state changes externally via ACTION_LIGHT_STATE_CHANGED | 04-02 |
| RECEIVER_NOT_EXPORTED flag | Android 14+ security requirement for internal broadcasts | 04-01 |
| setShowWhenLocked without keyguard dismissal | Preserves lock screen security while displaying light | 04-01 |
| ACTION_CLOSE_LIGHT broadcast pattern | Enables external components to close light without activity reference | 04-01 |
| SharedPreferences state persistence | Survives process death with zero overhead synchronous reads | 04-01 |
| Volume button events consumed | Prevents system volume popup during gesture control | 03-02 |
| Tilt observation lifecycle tied to volume hold | Start on press, stop on release, prevents accidental adjustments | 03-02 |
| Double-click threshold 300ms | Balance between easy double-click and accidental triggers | 03-02 |
| Separate GestureState from LightUiState | Gesture state vs UI rendering state, clean separation | 03-02 |
| Pitch range [-45, 0] to brightness [0, 1] | Upright to flat = dim to bright, natural gesture | 03-01 |
| Roll range [-45, +45] to color index | Left tilt warm, right tilt cool, intuitive selection | 03-01 |
| SENSOR_DELAY_UI for gesture detection | ~60ms balances responsiveness with battery efficiency | 03-01 |
| TYPE_GAME_ROTATION_VECTOR for tilt | Fused accel+gyro without magnetometer noise | 03-01 |
| TorchCallback for state sync | Auto-track torch state changes from other apps | 03-01 |
| Coverage threshold 20% | UI/Compose hard to unit test, increase incrementally | Phase 2 |
| Brightness via alpha channel | Color(argb).copy(alpha=brightness) for visual effect | 02-03 |
| Color transition duration 300ms | Smooth but responsive, Material Design recommended range | 02-03 |
| Low battery brightness cap 30% | Usable light while preserving battery | 02-03 |
| Immersive sticky swipe behavior | OLED burn-in prevention with accessibility | 02-03 |
| Night vision auto-activation on launch | Smart default for dark environment use | 02-03 |
| Ambient light dark threshold 50 lux | Dim room boundary, auto-activation trigger | 02-02 |
| Low battery threshold 15% | Matches Android system warning, feature gating | 02-02 |
| Sticky intent battery pattern | Zero overhead, instant reads without receiver | 02-02 |
| Cold Flow for sensor observation | Battery efficient, lifecycle-aware, automatic cleanup | 02-02 |

### Pending Todos

None yet.

### Blockers/Concerns

**Research Flags for Future Phases:**
- Phase 3: Tilt angle ranges [-45, 0] for pitch and [-45, +45] for roll are untested - may need tuning based on user feedback (03-02)
- Phase 3: Volume button double-click may be hard to discover (no UI hint) - consider tutorial on first launch (03-02)
- Phase 3: Volume button conflicts with "Select to Speak" accessibility (Google bug Jan 2026) — make gestures optional in settings
- Phase 4: Doze mode suspends sensors during idle — shake detection may not work when screen off for extended periods, this is expected Android behavior (04-04)
- Phase 4: Shake detection false positives possible during walking/running — user can increase sensitivity or disable if problematic (04-04)
- Phase 4: Lock screen display tested via build only - needs manual device testing (04-01)
- Phase 4: Build environment requires proper Java toolchain configuration (jlink) - assembleDebug blocked but Kotlin compilation works (04-02, 04-03)
- Phase 4: Quick Settings tile needs manual device testing to verify toggle behavior (04-02)
- Phase 4: Widget behavior cannot be tested until deployed to physical device (widgets don't work in emulator home screen) (04-03)
- Phase 4: Shake gesture needs manual device testing to verify sensitivity levels and lock screen operation (04-04)

**Critical Pitfalls to Avoid:**
- Phase 2: Wake lock management must unregister in onPause() not onDestroy() (battery drain + March 2026 Google Play enforcement)
- Phase 2: Foreground service type "specialUse" must be declared in manifest upfront (Android 14+ crashes without it)
- Phase 2: OLED burn-in prevention needed (immersive mode, pixel shifting, auto-timeout warning)
- Phase 3: Tilt observation uses viewModelScope - automatic cleanup on ViewModel clear, no explicit onPause needed (03-02)
- Phase 3: Volume button events must be consumed (return true) to prevent system volume interference (03-02)
- Phase 4: Lock screen security constraints (setShowWhenLocked() only, no keyguard dismissal)
- Phase 4: RemoteViews layouts must use compatible views only (FrameLayout, LinearLayout, ImageView, TextView) (04-03)

## Session Continuity

Last session: 2026-01-31 13:10 UTC
Stopped at: Completed 04-04-PLAN.md
Resume file: None

---
*Next step: All phases complete! Project ready for device testing and deployment.*
