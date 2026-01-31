# Project Research Summary

**Project:** Screenlight
**Domain:** Android utility app (screen light / flashlight with gesture controls)
**Researched:** 2026-01-31
**Confidence:** HIGH

## Executive Summary

Screenlight is an Android flashlight/screen light utility app with unique gesture-based controls (volume button + tilt). The Android flashlight ecosystem is mature and privacy-sensitive, with clear table stakes (LED toggle, Quick Settings tile, widgets) that users expect. To justify existence over native Android flashlight, third-party apps must excel at UX, screen light functionality, or gesture controls the system doesn't provide.

The recommended approach is **Modern Android Architecture** (MVVM + Clean Architecture) using Kotlin, Jetpack Compose, Hilt, and Coroutines. Architecture centers on a **Repository as SSOT**, **Foreground Service for hardware access** (sensors, wake lock, volume interception), and **State Machine for gesture detection**. This pattern provides lifecycle-safe sensor management, testable business logic, and consistent state across multiple entry points (app icon, widget, QS tile, shake detection).

Critical risks center on **battery drain** (Google Play flags apps with >2hr wake locks starting March 2026), **foreground service requirements** (Android 14+ requires type declarations), and **sensor memory leaks** (unregistered listeners leak Activities). Mitigation requires aggressive wake lock management, manifest declarations upfront, and strict lifecycle discipline (unregister sensors in onPause(), not onDestroy()).

## Key Findings

### Recommended Stack

Screenlight uses standard 2025-2026 Android stack with emphasis on modern, compile-time-safe tooling. The research shows strong consensus around Jetpack Compose (60% top app adoption), Hilt with KSP (2x faster than KAPT), and Flow-based reactive programming for sensor streams.

**Core technologies:**
- **Kotlin 2.3.0 + Jetpack Compose 1.10.2**: Modern declarative UI, standard for new apps. Material 3 dynamic theming essential for utility apps.
- **Hilt 2.56 with KSP**: Android-optimized DI, 2x faster annotation processing. Standard for 2025 Android apps.
- **Coroutines 1.10.2 + Flow**: Async operations and reactive sensor data streams. Flow's sequential-by-default execution perfect for gesture state machines.
- **Platform APIs (no third-party libs needed)**: CameraManager for LED flashlight with brightness control (API 33+), SensorManager for accelerometer/gyroscope, MediaSessionCompat for volume button interception, TileService for Quick Settings, GlanceAppWidget for modern Compose-based widgets.

**Critical version requirements:**
- Android SDK: Min API 26 (covers 95%+ devices), Target API 35 (required for Play Store Aug 2025)
- Foreground service type declarations required for Android 14+ (70%+ users in 2026)

### Expected Features

Research reveals clear hierarchy: table stakes for parity with native flashlight, differentiators to justify app installation, and anti-features that erode trust.

**Must have (table stakes):**
- LED Flashlight Toggle — Core function; native Android has had this since 2014
- Quick Settings Tile — Standard activation method, users expect it
- Screen Light with Brightness Control — Foundational value prop for screen-based lighting
- Minimal Permissions — Only camera (LED) and wake lock; privacy scandals (apps requesting 77 permissions) make users wary
- Works from Lock Screen — Emergency use case, native flashlight accessible when locked

**Should have (competitive differentiators):**
- Gesture-Based Control (Volume + Tilt) — PRIMARY DIFFERENTIATOR, unexplored territory (no app combines volume hold + tilt for brightness/color)
- Color Temperature Range — Deep red for night vision preservation → warm → cool white
- Smooth Transitions — Fade effects prevent eye strain in dark (most apps do instant changes)
- Shake to Activate — No-look activation, competitive with native shortcuts

**Defer (v2+):**
- Adjustable LED Brightness — Becoming table stakes when Android 16 QPR3 launches (March 2026), but can wait for MVP
- SOS/Morse Code — Niche (<1% usage), medical risk (strobes can trigger seizures)
- Compass/Level/Tools — Feature bloat, "Swiss army knife" apps feel unfocused

**Anti-features (explicitly avoid):**
- Excessive Permissions — Never request location, contacts, SMS, audio recording (zero legitimate use cases)
- Intrusive Ads — Users abandon utility apps with full-screen ads
- Background Services without justification — March 2026 Google policy flags >2hr wake locks with public warning badges

### Architecture Approach

Screenlight follows Google's Modern App Architecture with layered design: UI (Compose + ViewModel), Domain (Use Cases + State Machines), Data (Repository + Foreground Service). The architecture prioritizes lifecycle safety, testability, and unidirectional data flow.

**Major components:**

1. **ScreenRepository (SSOT)** — Singleton that holds screen state (color, brightness, on/off). Binds to Foreground Service, exposes StateFlow to all consumers. All components (MainActivity, Widget, QS Tile, Shake) modify same repository state.

2. **Foreground Service** — Owns hardware access (SensorManager for accelerometer/gyroscope, PowerManager for wake lock, MediaSession for volume buttons). Emits sensor data as Flow. Required for background shake detection (API 28+) and must declare `foregroundServiceType="specialUse"` for Android 14+.

3. **GestureStateMachine** — Sealed class hierarchy modeling gesture states (Idle → VolumeHeld → AdjustingBrightness/Color). Pure functions for state transitions, exhaustive when expressions. Owned by ViewModel for UI-specific logic.

4. **Multiple Entry Points, Single State** — MainActivity (Compose UI), Widget (GlanceAppWidget), QS Tile (TileService), and Shake Detection all interact with same Repository. Deep links route shake/widget actions to MainActivity with FLAG_SHOW_WHEN_LOCKED.

5. **ViewModel + Lifecycle Observer** — Holds UI state, coordinates use cases, survives config changes. LaunchedEffect for sensor Flow collection in Compose, DisposableEffect for cleanup.

**Key architectural decisions:**
- **Service owns hardware, Repository owns business state** — Clear separation prevents race conditions
- **Flow for reactive sensor data** — SensorEventListener emits to Flow, processed on background dispatcher
- **Bound Service pattern** — Repository binds to Service via ServiceConnection, mediates between UI and hardware

### Critical Pitfalls

Research identified 13 pitfalls across critical, moderate, and minor severity. Top 5 for roadmap planning:

1. **Excessive Partial Wake Lock Battery Drain** — Starting March 1, 2026, Google Play flags and de-ranks apps holding wake locks >2 cumulative hours in 24hrs for 5% of sessions over 28 days. Prevention: Unregister sensors in onPause() (not onDestroy()), minimize wake lock duration, use sensor batching, monitor Android Vitals. **Phase mapping: Phase 1 (Core Services)** — Build wake lock management correctly from start; refactoring later is painful.

2. **Foreground Service Type Not Declared (Android 14+)** — Apps crash with MissingForegroundServiceTypeException if foreground service type not declared in manifest. Must declare `foregroundServiceType="specialUse"` and request runtime permission. Android 15 adds stricter requirements (visible overlay window before starting service). **Phase mapping: Phase 1 (Core Services)** — Critical to get right before building any background functionality.

3. **Sensor Listeners Not Unregistered - Memory Leaks** — Registering SensorEventListener in onCreate()/onResume() but forgetting to unregister in onPause() causes memory leaks. SensorManager holds reference to Activity, preventing garbage collection. After screen rotations, leak entire Activity. **Phase mapping: Phase 1 (Core Services)** — Memory leaks compound over time; fix immediately.

4. **Doze Mode Kills Background Service (Android 14-16)** — During Doze mode (device idle 30+ min), Android suspends sensor callbacks, ignores alarms. Android 16 introduces execution quotas even for "active" bucket apps. Shake detection stops working after device idle. **Phase mapping: Phase 1 (Core Services)** — Design architecture assuming Doze will happen; don't rely on continuous background execution.

5. **Always-On Screen Causes OLED Burn-In** — Keeping screen on at max brightness with static UI (status bar, navigation bar) causes permanent image retention on OLED displays. Users report "This app ruined my screen." Prevention: Enable immersive mode (hide system bars), implement pixel shifting every 2 minutes, auto-timeout warning after 30 minutes, default to 70% brightness. **Phase mapping: Phase 2 (Always-On Features)** — Implement burn-in prevention before launching always-on screen features.

## Implications for Roadmap

Based on research, suggested phase structure prioritizes core infrastructure, hardware access, and risk mitigation before gesture features and convenience polish.

### Phase 1: Core Infrastructure & Services
**Rationale:** Establishes data layer, testable in isolation. Foreground service setup is foundational for all features and must handle wake locks, service types, and lifecycle correctly from start (pitfalls compound if wrong).

**Delivers:**
- ScreenRepository with StateFlow state management
- PreferencesRepository with DataStore for settings persistence
- Foreground Service skeleton with notification, wake lock management, sensor listener registration
- Service binding (Repository ↔ Service connection)
- Unit tests for repositories and service lifecycle

**Addresses features:** Table stakes foundation — minimal permissions, battery efficiency baseline

**Avoids pitfalls:**
- Pitfall #1: Excessive wake lock battery drain (aggressive unregistration)
- Pitfall #2: Foreground service type not declared (manifest declarations upfront)
- Pitfall #3: Sensor listener memory leaks (unregister in onPause())
- Pitfall #4: Doze mode kills service (design for intermittent operation)

### Phase 2: Basic UI & Screen Light
**Rationale:** Depends on ViewModel which depends on Repository. Validates that state flows from Repository → ViewModel → UI. Delivers minimum viable product (screen light) before complex gesture features.

**Delivers:**
- ScreenViewModel observing repository state
- MainActivity + Compose UI displaying color, reacting to state changes
- FLAG_KEEP_SCREEN_ON in window for always-on screen
- Screen brightness control via WindowManager.LayoutParams
- Color temperature range (deep red → warm → cool white)
- Smooth transitions with animateColorAsState

**Addresses features:**
- Must have: Screen Light with Brightness Control
- Should have: Color Temperature Range, Smooth Transitions

**Avoids pitfalls:**
- Pitfall #5: OLED burn-in (immersive mode, pixel shifting, auto-timeout warning)
- Pitfall #11: Screen brightness doesn't persist (re-apply in onCreate, save to preferences)

### Phase 3: LED Flashlight & Quick Settings Tile
**Rationale:** Adds table stakes parity with native flashlight. Quick Settings Tile is standard activation method users expect. Simpler than gesture controls.

**Delivers:**
- LED flashlight toggle via CameraManager API
- LED brightness control (API 33+)
- TileService for Quick Settings integration
- Tile state updates synced with repository
- Deep link handling (tile click → MainActivity)

**Addresses features:**
- Must have: LED Flashlight Toggle, Quick Settings Tile

**Avoids pitfalls:**
- Pitfall #8: Quick Settings Tile lifecycle crashes (proper onStartListening/onStopListening, tile state updates)

### Phase 4: Gesture Detection (Primary Differentiator)
**Rationale:** Depends on sensor data flowing through Service/Repository. This is Screenlight's unique value proposition but highest risk (volume button conflicts, sensor processing complexity).

**Delivers:**
- GestureStateMachine (sealed classes, pure state transitions)
- Volume button interception via dispatchKeyEvent() in Activity
- Tilt calculation from accelerometer data
- Wire gesture events: ViewModel → StateMachine → Repository
- Smooth brightness/color adjustments based on tilt angle

**Addresses features:**
- Should have: Gesture-Based Control (Volume + Tilt) — PRIMARY DIFFERENTIATOR
- Should have: Tilt-Based Control

**Avoids pitfalls:**
- Pitfall #4: Volume button conflicts with MediaSession (check isMusicActive, make optional in settings)
- Pitfall #2: Heavy work in onSensorChanged (emit to Flow, process on background dispatcher)

### Phase 5: Lock Screen Launch & Shake Detection
**Rationale:** Most complex feature, requires working app + service. High security risk (lock screen bypass) if implemented incorrectly. Defer until core functionality solid.

**Delivers:**
- ShakeDetector use case (threshold ~2.7G)
- Service shake monitoring with always-on listener
- Launch activity from service (FLAG_SHOW_WHEN_LOCKED, FLAG_TURN_SCREEN_ON)
- Lock screen security constraints (no navigation, limited functionality)

**Addresses features:**
- Must have: Works from Lock Screen
- Should have: Shake to Activate

**Avoids pitfalls:**
- Pitfall #7: Lock screen security holes (setShowWhenLocked() only, no keyguard dismissal, no navigation)
- Pitfall #4: Doze mode kills shake detection (educate users, accept intermittent operation)

### Phase 6: Home Screen Widget & Polish
**Rationale:** Convenience features that depend on working repository and state. Widgets have ANR risks and battery drain potential, defer until core solid.

**Delivers:**
- GlanceAppWidget for Compose-based 1x1 widget
- Widget updates via WorkManager + Repository
- Partial widget updates for battery efficiency
- Battery Guard (auto-dim below 20% battery)
- Color presets (red, warm, cool)

**Addresses features:**
- Must have: Home Screen Widget
- Should have: Battery Guard

**Avoids pitfalls:**
- Pitfall #9: Widget update ANRs and battery drain (goAsync(), appropriate update frequency, partial updates, WorkManager for async)

### Phase 7: Testing, CI/CD & Release Preparation
**Rationale:** Ensures quality before launch. ProGuard/R8 issues only surface in release builds, must test early.

**Delivers:**
- Espresso IdlingResources for sensor-based flaky tests
- GitHub Actions CI/CD with gradle-build-action
- Test artifact uploads on failure
- SDK license acceptance in CI
- ProGuard/R8 keep rules for reflection-based code
- Release build testing

**Addresses features:**
- No Ads (or minimal) — Business model consideration

**Avoids pitfalls:**
- Pitfall #10: GitHub Actions Android CI failures (gradle-build-action, license acceptance, artifact uploads)
- Pitfall #12: Instrumented tests flaky due to background services (IdlingResources)
- Pitfall #13: ProGuard/R8 breaking release builds (keep rules, test release builds)

### Phase Ordering Rationale

- **Phase 1 before all others:** Wake lock management, service types, sensor lifecycle discipline must be correct from start. Refactoring later requires touching entire codebase.
- **Phase 2 before gestures:** Validate basic state flow (Repository → ViewModel → UI) with simple screen light before adding gesture complexity.
- **Phase 3 before Phase 4:** Quick Settings Tile is simpler than gesture controls, provides learning for TileService lifecycle before tackling volume button interception.
- **Phase 4 before Phase 5:** Lock screen launch is highest security risk, needs solid gesture foundation to be useful.
- **Phase 5 is highest risk:** Defer shake-to-launch until core flashlight and gesture controls work reliably. Accept that Doze mode makes 100% reliability impossible.
- **Phase 6 after core:** Widgets are convenience, not essential. Battery drain risks make sense to address after core features optimized.
- **Phase 7 throughout:** Set up CI in Phase 0, but comprehensive testing/release prep happens after features complete.

### Research Flags

**Phases likely needing deeper research during planning:**
- **Phase 4 (Gesture Detection):** Custom gesture algorithms for tilt → brightness/color mapping. Research optimal tilt angle thresholds, smoothing/filtering sensor noise, debouncing rapid changes.
- **Phase 5 (Lock Screen Launch):** Security model for lock screen activities. Research SYSTEM_ALERT_WINDOW permission requirements across Android versions, keyguard manager API nuances.

**Phases with standard patterns (skip research-phase):**
- **Phase 1 (Core Infrastructure):** Well-documented Repository pattern, Foreground Service setup, Hilt DI. Official Android docs + Modern App Architecture guide cover 95% of decisions.
- **Phase 2 (Basic UI):** Jetpack Compose patterns for state management. Official Compose docs, animateColorAsState, WindowManager API all well-documented.
- **Phase 3 (LED Flashlight & QS Tile):** CameraManager API, TileService lifecycle in official docs. Tile state management has clear examples in AOSP documentation.
- **Phase 6 (Widget):** GlanceAppWidget has 2025 tutorials, WorkManager integration well-documented.
- **Phase 7 (Testing/CI):** GitHub Actions for Android has established patterns (gradle-build-action), Espresso IdlingResources in official testing docs.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Verified via official Android docs, GitHub releases, Google Play requirements. All version numbers cross-referenced with developer.android.com (updated Jan 2026). Hilt + KSP standard for 2025. |
| Features | MEDIUM | Verified via WebSearch (Google Play store listings, feature discovery from top apps, user reviews, Android Authority articles). Privacy pitfalls confirmed via Avast study (77 permissions), FTC case (location tracking). Color temperature and gesture features validated across multiple sources but less authoritative than official docs. |
| Architecture | HIGH | Official Android "Guide to app architecture" (updated Jan 26, 2026), Modern App Architecture principles, foreground service types documentation. AOSP source code for TileService lifecycle. Bounded service pattern established in Android community. |
| Pitfalls | HIGH | Critical pitfalls (#1-5) verified with official Google blog posts (March 2026 wake lock policy), Android 14-15-16 behavior changes documentation, official Android Vitals metrics. Volume button bug confirmed by Google (Jan 2026). Moderate pitfalls sourced from developer blogs + GitHub issues (2022-2026). |

**Overall confidence:** HIGH

Research benefited from recent (Jan 2026) Android documentation updates, official Google policy announcements for March 2026, and Android 16 behavior changes. Stack and architecture decisions rest on official sources. Feature research less authoritative (relies on app store analysis, third-party articles) but cross-referenced multiple sources for consistency.

### Gaps to Address

- **Optimal tilt angle thresholds for gesture controls:** Research found shake detection threshold (~2.7G) but tilt-to-brightness mapping is novel. Needs user testing during Phase 4 to determine intuitive angle ranges. Start with ±45° tilt = 0-100% brightness, adjust based on UX feedback.

- **Volume button interception conflicts with accessibility:** January 2026 Google bug with "Select to Speak" impacts volume buttons. Workaround incomplete in research. Plan for Phase 4: Make volume shortcuts optional in settings, provide alternative triggers (shake, QS tile), document known conflicts. Monitor Google issue tracker for official fix.

- **Doze mode mitigation strategies:** Research confirms Doze will suspend sensors but exemption strategies (battery optimization whitelist, setAndAllowWhileIdle alarms) have Play Store rejection risk if not justified. Plan: Design for intermittent operation (educate users shake works best during active use), avoid battery optimization exemption request unless user-initiated. Consider high-priority FCM for future "remote shake" feature if justified.

- **LED brightness control API availability:** Android 13 (API 33) added `turnOnTorchWithStrengthLevel()` but device support varies. Research shows Android 16 QPR3 (March 2026) adding UI for this in native flashlight. Plan: Check device support at runtime, fall back to binary on/off if unsupported. Add feature to Phase 3 or defer to v2 based on device support testing.

- **Jetpack Glance stability:** GlanceAppWidget is newer framework (1.1.0 current), fewer battle-tested examples than legacy RemoteViews. Medium confidence in battery efficiency claims (80% reduction reported in one 2025 blog). Plan: Prototype widget in Phase 6, compare battery usage vs RemoteViews if issues arise. Have fallback plan to use RemoteViews if Glance proves unstable.

## Sources

### Primary (HIGH confidence)

**Official Android Documentation:**
- [Guide to app architecture](https://developer.android.com/topic/architecture) — Updated Jan 26, 2026. Modern App Architecture, MVVM, Repository pattern.
- [Jetpack Compose Releases](https://developer.android.com/jetpack/androidx/releases/compose) — Version verification, BOM management
- [Android Gradle Plugin 9.0.0](https://developer.android.com/build/releases/agp-9-0-0-release-notes) — Latest stable build tooling
- [Hilt Documentation](https://dagger.dev/hilt/) — DI setup, KSP integration
- [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types) — Android 14+ requirements, specialUse type
- [Motion sensors](https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion) — Updated Jan 26, 2026. Accelerometer/gyroscope APIs
- [CameraManager API](https://developer.android.com/reference/android/hardware/camera2/CameraManager) — LED flashlight control, brightness API
- [TileService API](https://developer.android.com/develop/ui/views/quicksettings-tiles) — Quick Settings integration
- [Jetpack Glance](https://developer.android.com/develop/ui/compose/glance) — Modern widget framework
- [Side-effects in Compose](https://developer.android.com/develop/ui/compose/side-effects) — Updated Jan 30, 2026. LaunchedEffect, DisposableEffect

**Official Google Policy:**
- [Target SDK Requirements](https://support.google.com/googleplay/android-developer/answer/11926878) — API 35 required Aug 2025
- [Excessive partial wake locks metric](https://developer.android.com/topic/performance/vitals/wakelock) — March 2026 enforcement
- [Google Play to flag battery-draining apps March 2026](https://android-developers.googleblog.com/2025/11/raising-bar-on-battery-performance.html) — Wake lock crackdown announcement
- [Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-15) — Foreground service overlay requirements
- [Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-all) — Execution quota for active apps

**GitHub Official Releases:**
- [kotlinx-coroutines Releases](https://github.com/Kotlin/kotlinx.coroutines/releases) — Version 1.10.2 verification
- [Detekt Releases](https://github.com/detekt/detekt/releases) — Version 1.23.8 verification

### Secondary (MEDIUM confidence)

**Feature & Privacy Research:**
- [Avast: Flashlight Apps Request Up to 77 Permissions](https://blog.avast.com/flashlight-apps-on-google-play-request-up-to-77-permissions-avast-finds) — Privacy anti-patterns
- [The best Android flashlight apps with no extra permissions](https://www.androidauthority.com/best-android-flashlight-apps-with-no-extra-permissions-568939/) — Feature baseline research
- [Android 16 QPR3 adjustable flashlight brightness](https://9to5google.com/2025/12/17/android-16-qpr3-flashlight/) — Upcoming native feature
- [Google Play Store battery-draining apps warning 2026](https://www.webpronews.com/google-play-store-to-warn-users-of-battery-draining-apps-in-2026/) — Policy confirmation

**Architecture Patterns:**
- [Better Android Apps Using MVVM with Clean Architecture](https://www.toptal.com/android/android-apps-mvvm-with-clean-architecture) — Toptal authoritative source
- [Modern Android App Architecture (2025 Edition)](https://medium.com/design-bootcamp/modern-android-app-architecture-with-clean-code-principles-2025-edition-95f4c2afeadb) — Recent best practices
- [Finite State Machines + Android + Kotlin](https://thoughtbot.com/blog/finite-state-machines-android-kotlin-good-times) — Thoughtbot industry blog
- [Mastering Global State Management in Jetpack Compose](https://www.droidcon.com/2025/01/23/mastering-global-state-management-in-android-with-jetpack-compose/) — Jan 23, 2025 article

**Pitfalls & Testing:**
- [Top 7 Android memory leaks 2025](https://artemasoyan.medium.com/top-7-android-memory-leaks-and-how-to-avoid-them-in-2025-b77e15a7b62e) — Sensor listener leaks
- [Google confirms Android volume button bug Jan 2026](https://piunikaweb.com/2026/01/08/android-google-pixel-long-press-volume-controls-not-working-workaround/) — Select to Speak conflict
- [Glance widgets battery efficiency](https://medium.com/@hiren6997/glance-widgets-in-2025-how-i-built-a-battery-friendly-android-widget-in-2-hours-14262e4e6f84) — 23% of battery complaints from widgets
- [GitHub Actions for Gradle deep integration](https://blog.gradle.org/gh-actions) — CI/CD patterns
- [Configure R8 keep rules](https://android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html) — Nov 2025 official guidance

### Tertiary (LOW confidence)

- [Building Animated Widgets with Jetpack Glance 2025](https://medium.com/@snishanthdeveloper/building-animated-home-screen-productivity-widgets-with-jetpack-glance-in-2025-40261364f1ee) — Glance practical guide, single source
- [How to listen volume buttons in background service](https://www.tutorialspoint.com/how-to-listen-volume-buttons-in-android-background-service) — Educational resource, not authoritative

---
*Research completed: 2026-01-31*
*Ready for roadmap: yes*
