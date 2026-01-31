# Requirements: Screenlight

**Defined:** 2025-01-31
**Core Value:** Instant, gesture-controlled light that works from lock screen — no fumbling with UI in the dark.

## v1 Requirements

### Screen Light

- [ ] **SCRN-01**: Screen displays full-screen solid color at adjustable brightness
- [ ] **SCRN-02**: Brightness controlled by tilting phone flat↔upright while holding volume button
- [ ] **SCRN-03**: Color controlled by tilting phone left↔right while holding volume button
- [ ] **SCRN-04**: Color range spans deep red (night vision) through warm to cool white
- [ ] **SCRN-05**: App starts in night vision mode (deep red) on launch
- [ ] **SCRN-06**: Brightness and color changes use smooth fade transitions (not instant)
- [ ] **SCRN-07**: Screen stays awake while app is active

### LED Flashlight

- [ ] **LED-01**: Long-press any volume button toggles camera LED flashlight on/off

### Launch Methods

- [ ] **LNCH-01**: Quick settings tile toggles light on/off from notification shade
- [ ] **LNCH-02**: Home screen widget toggles light on/off with single tap
- [ ] **LNCH-03**: Shake phone to launch app from anywhere (including lock screen)
- [ ] **LNCH-04**: Shake activation works without unlocking the phone
- [ ] **LNCH-05**: Shake phone to turn off light when app is active

### Controls

- [ ] **CTRL-01**: Double-tap screen toggles light on/off
- [ ] **CTRL-02**: Double-click volume button closes the app
- [ ] **CTRL-03**: Battery guard auto-dims screen and shows warning when battery < 15%

### CI/CD

- [ ] **CICD-01**: GitHub Actions runs tests on every push and pull request
- [ ] **CICD-02**: Pull requests blocked if code coverage falls below 80%
- [ ] **CICD-03**: Kotlin lint checks (Detekt) run on every PR
- [ ] **CICD-04**: Main branch is protected with required PR reviews
- [ ] **CICD-05**: No admin bypass allowed on main branch protection
- [ ] **CICD-06**: Pushing tag v*.*.* creates GitHub release with debug-signed APK
- [ ] **CICD-07**: GitHub Flow workflow with feature branches merged via PR

## v2 Requirements

### Enhancements

- **ENH-01**: Remember last brightness/color settings across sessions
- **ENH-02**: Preset slots for saving favorite brightness/color combinations
- **ENH-03**: Adjustable LED flashlight brightness (Android 16+ API)
- **ENH-04**: Haptic feedback when brightness/color reaches min/max limits

### Launch Methods

- **LNCH-10**: Voice control integration ("Hey Google, turn on Screenlight")

## Out of Scope

| Feature | Reason |
|---------|--------|
| Strobe/SOS mode | Seizure risk; liability concern |
| Morse code | <1% usage; adds complexity |
| Compass overlay | Scope creep; not core value |
| Production signing | Personal use only; debug signing sufficient |
| Play Store distribution | GitHub releases only for v1 |
| iOS version | Android only for v1 |
| Multiple languages | English only for v1 |
| Cloud sync | Fully local app |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| CICD-01 | Phase 1 | Pending |
| CICD-02 | Phase 1 | Pending |
| CICD-03 | Phase 1 | Pending |
| CICD-04 | Phase 1 | Pending |
| CICD-05 | Phase 1 | Pending |
| CICD-06 | Phase 1 | Pending |
| CICD-07 | Phase 1 | Pending |
| SCRN-01 | Phase 2 | Pending |
| SCRN-04 | Phase 2 | Pending |
| SCRN-05 | Phase 2 | Pending |
| SCRN-06 | Phase 2 | Pending |
| SCRN-07 | Phase 2 | Pending |
| CTRL-03 | Phase 2 | Pending |
| SCRN-02 | Phase 3 | Pending |
| SCRN-03 | Phase 3 | Pending |
| LED-01 | Phase 3 | Pending |
| CTRL-01 | Phase 3 | Pending |
| CTRL-02 | Phase 3 | Pending |
| LNCH-01 | Phase 4 | Pending |
| LNCH-02 | Phase 4 | Pending |
| LNCH-03 | Phase 4 | Pending |
| LNCH-04 | Phase 4 | Pending |
| LNCH-05 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 23 total
- Mapped to phases: 23
- Unmapped: 0 ✓

---
*Requirements defined: 2025-01-31*
*Last updated: 2026-01-31 after roadmap creation*
