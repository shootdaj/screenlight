# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-31)

**Core value:** Instant, gesture-controlled light that works from lock screen — no fumbling with UI in the dark.
**Current focus:** Phase 2 - Core Services & Screen Light

## Current Position

Phase: 2 of 4 (Core Services & Screen Light)
Plan: 1 of 4
Status: In progress
Last activity: 2026-01-31 — Completed 02-01-PLAN.md

Progress: [███░░░░░░░] 38%

## Performance Metrics

**Velocity:**
- Total plans completed: 3
- Average duration: 7.0 min
- Total execution time: 0.4 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-project-setup-ci-cd | 2 | 13 min | 6.5 min |
| 02-core-services-screen-light | 1 | 8 min | 8.0 min |

**Recent Trend:**
- Last 5 plans: 01-01 (11 min), 01-02 (2 min), 02-01 (8 min)
- Trend: Stable

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

| Decision | Context | Plan |
|----------|---------|------|
| KSP over KAPT for Hilt | Required for Kotlin 2.x, 2x faster builds | 02-01 |
| activity-compose replaces activity-ktx | Compose-specific Activity support | 02-01 |
| Detekt FunctionNaming exemption for @Composable | PascalCase is Compose convention | 02-01 |
| PreferencesDataStore delegate pattern | Single instance per process, prevents corruption | 02-01 |
| JaCoCo execution data path uses AGP 8.x location | outputs/unit_test_code_coverage/debugUnitTest/ | 01-01 |
| Detekt buildUponDefaultConfig=true | Minimal override config, not full generated config | 01-01 |
| Heredoc JSON for GitHub API | Shell bracket escaping issues with --field | 01-02 |
| No CI wait before protection | Status check context recognized from workflow file definition | 01-02 |

### Pending Todos

None yet.

### Blockers/Concerns

**Research Flags for Future Phases:**
- Phase 3: Custom gesture algorithms for tilt-to-brightness mapping need user testing (start with +/-45 deg = 0-100%, adjust based on UX)
- Phase 3: Volume button conflicts with "Select to Speak" accessibility (Google bug Jan 2026) — make gestures optional in settings
- Phase 4: Doze mode suspends sensors during idle — design for intermittent operation, educate users shake works best during active use

**Critical Pitfalls to Avoid:**
- Phase 2: Wake lock management must unregister in onPause() not onDestroy() (battery drain + March 2026 Google Play enforcement)
- Phase 2: Foreground service type "specialUse" must be declared in manifest upfront (Android 14+ crashes without it)
- Phase 2: OLED burn-in prevention needed (immersive mode, pixel shifting, auto-timeout warning)
- Phase 3: Sensor listeners must unregister in onPause() (memory leaks compound after rotations)
- Phase 4: Lock screen security constraints (setShowWhenLocked() only, no keyguard dismissal)

## Session Continuity

Last session: 2026-01-31 03:40 UTC
Stopped at: Completed 02-01-PLAN.md
Resume file: None

---
*Next step: Continue with 02-02, 02-03, 02-04 plans to complete Phase 2*
