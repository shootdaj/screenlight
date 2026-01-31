# Roadmap: Screenlight

## Overview

Screenlight development progresses from foundational infrastructure through gesture-controlled lighting features. Starting with CI/CD and core Android services, then building screen light with tilt-based controls, adding LED flashlight and launch methods, culminating in lock screen shake activation. Each phase delivers verifiable user capabilities while managing battery, lifecycle, and security risks identified in research.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Project Setup & CI/CD** - Build pipeline and quality gates
- [x] **Phase 2: Core Services & Screen Light** - Foundation services and basic screen illumination
- [x] **Phase 3: Gesture Controls & LED Flashlight** - Tilt-based brightness/color and LED toggle
- [x] **Phase 4: Launch Methods & Lock Screen** - Widgets, tiles, and shake activation

## Phase Details

### Phase 1: Project Setup & CI/CD
**Goal**: Establish automated quality gates and release pipeline before building features
**Depends on**: Nothing (first phase)
**Requirements**: CICD-01, CICD-02, CICD-03, CICD-04, CICD-05, CICD-06, CICD-07
**Success Criteria** (what must be TRUE):
  1. Pull request cannot merge to main without passing tests and 80% coverage
  2. Kotlin lint checks (Detekt) block PRs with style violations
  3. Pushing a tag matching v*.*.* creates a GitHub release with debug-signed APK attached
  4. Main branch is protected with no admin bypass allowed
**Plans**: 2 plans

Plans:
- [x] 01-01-PLAN.md — Android project scaffold with Gradle, JaCoCo, and Detekt
- [x] 01-02-PLAN.md — CI/CD workflows and branch protection

### Phase 2: Core Services & Screen Light
**Goal**: Build lifecycle-safe service foundation and deliver basic screen light with color/brightness control
**Depends on**: Phase 1
**Requirements**: SCRN-01, SCRN-04, SCRN-05, SCRN-06, SCRN-07, SCRN-08, CTRL-03
**Success Criteria** (what must be TRUE):
  1. App displays full-screen solid color that stays visible while app is active
  2. App detects ambient light on launch — if dark, starts in night vision (deep red); otherwise uses last used color
  3. App remembers last used brightness and color settings across sessions
  4. Color range spans deep red through warm to cool white
  5. Brightness and color changes use smooth fade transitions (not instant jumps)
  6. Screen auto-dims and shows warning when battery drops below 15%
  7. Wake lock releases properly when app pauses (no battery drain)
**Plans**: 3 plans

Plans:
- [x] 02-01-PLAN.md — Add Compose, Hilt, and DataStore dependencies with DI foundation
- [x] 02-02-PLAN.md — Create data layer with settings persistence and sensor utilities
- [x] 02-03-PLAN.md — Build full-screen light UI with transitions, ambient detection, and battery warning

### Phase 3: Gesture Controls & LED Flashlight
**Goal**: Enable eyes-free brightness/color adjustment via tilt gestures and add LED flashlight toggle
**Depends on**: Phase 2
**Requirements**: SCRN-02, SCRN-03, LED-01, CTRL-01, CTRL-02
**Success Criteria** (what must be TRUE):
  1. While holding volume button, tilting phone flat to upright adjusts screen brightness smoothly
  2. While holding volume button, tilting phone left to right changes color temperature
  3. Long-pressing any volume button toggles camera LED flashlight on/off
  4. Double-tapping screen toggles light on/off
  5. Double-clicking volume button closes the app
  6. Sensor listeners unregister properly when app pauses (no memory leaks)
**Plans**: 2 plans

Plans:
- [x] 03-01-PLAN.md — Create tilt gesture manager and flashlight controller data layer
- [x] 03-02-PLAN.md — Wire gesture controls into ViewModel and UI

### Phase 4: Launch Methods & Lock Screen
**Goal**: Add convenience launch methods (widget, QS tile) and enable shake-to-launch from lock screen
**Depends on**: Phase 3
**Requirements**: LNCH-01, LNCH-02, LNCH-03, LNCH-04, LNCH-05
**Success Criteria** (what must be TRUE):
  1. Quick Settings tile toggles light on/off from notification shade
  2. Home screen widget toggles light on/off with single tap
  3. Shaking phone launches app from home screen, lock screen, or other apps
  4. Shake activation works without unlocking the phone (bypasses lock screen)
  5. Shaking phone while app is active turns off the light
**Plans**: 4 plans

Plans:
- [x] 04-01-PLAN.md — Shared light state management and lock screen display
- [x] 04-02-PLAN.md — Quick Settings tile for notification shade toggle
- [x] 04-03-PLAN.md — Home screen widget with visual state feedback
- [x] 04-04-PLAN.md — Shake detection foreground service

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Project Setup & CI/CD | 2/2 | Complete | 2026-01-31 |
| 2. Core Services & Screen Light | 3/3 | Complete | 2026-01-31 |
| 3. Gesture Controls & LED Flashlight | 2/2 | Complete | 2026-01-31 |
| 4. Launch Methods & Lock Screen | 4/4 | Complete | 2026-01-31 |

---
*Roadmap created: 2026-01-31*
*Last updated: 2026-01-31*
