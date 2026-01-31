# Screenlight

## What This Is

An Android app that turns your phone screen into a controllable light source. Designed for quick use in the dark — launch instantly from lock screen with a shake, adjust brightness and color by tilting the phone while holding the volume button. Includes LED flashlight toggle for when you need more power.

## Core Value

Instant, gesture-controlled light that works from lock screen — no fumbling with UI in the dark.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Screen illuminates with adjustable brightness (full screen solid color)
- [ ] Brightness controlled by tilting phone flat↔upright while holding volume
- [ ] Color controlled by tilting phone left↔right while holding volume
- [ ] Color range from deep red (night vision) through warm to cool white
- [ ] Smooth fade transitions (not instant changes)
- [ ] LED flashlight toggle via long-press volume
- [ ] Double-tap screen to toggle light on/off
- [ ] Battery guard: auto-dim and warn when battery < 15%
- [ ] Screen stays awake while app is open
- [ ] Home screen widget for one-tap launch/toggle
- [ ] Quick settings tile for toggle from notification shade
- [ ] Shake to launch from anywhere (including lock screen, bypasses lock)
- [ ] Shake to turn off when app is active
- [ ] Double-click volume to close app
- [ ] GitHub Actions CI/CD pipeline
- [ ] Tests run on every PR/push
- [ ] 80% code coverage threshold enforced
- [ ] Kotlin lint checks enforced
- [ ] Protected main branch (no admin bypass)
- [ ] GitHub Flow workflow
- [ ] Tag-based releases (v1.x.x → creates release)
- [ ] Debug-signed APK attached to releases

### Out of Scope

- iOS version — Android only for v1
- Play Store distribution — GitHub releases only
- Production keystore signing — debug signing sufficient for personal use
- Multiple languages/i18n — English only
- User accounts/cloud sync — fully local app
- Wear OS companion — not needed for core use case

## Context

Primary use case: nighttime light when "bumbling around in the dark." Speed and convenience are paramount — the app must be usable without looking at the screen or navigating UI. All interactions are gesture-based for eyes-free operation.

Red color option specifically for night vision preservation (camping, astronomy, etc.).

Battery guard ensures the always-on screen doesn't drain battery unexpectedly.

## Constraints

- **Platform**: Android only (Kotlin, Jetpack Compose)
- **Min SDK**: TBD during research (likely API 26+ for modern sensor APIs)
- **Launch method**: Must work from lock screen without unlock
- **Gesture detection**: Volume button state + accelerometer/gyroscope data
- **Testing**: 80% coverage minimum, must pass before merge
- **CI/CD**: GitHub Actions only (no external services)

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Debug signing for releases | Personal use, simpler setup, no key management | — Pending |
| Shake works from lock screen | Core value is instant access in the dark | — Pending |
| Volume button as gesture modifier | Hands-free operation without looking at screen | — Pending |
| Deep red as color extreme | Night vision preservation for outdoor use | — Pending |
| GitHub Flow | Simple branching model, protected main | — Pending |

---
*Last updated: 2025-01-31 after initialization*
