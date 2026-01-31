# Phase 4: Launch Methods & Lock Screen - Context

**Gathered:** 2026-01-31
**Status:** Ready for planning

<domain>
## Phase Boundary

Add convenience launch methods for the screen light app: Quick Settings tile, home screen widget, and shake gesture activation. Enable launching from lock screen without unlocking. The app already has full gesture controls (Phase 3) — this phase adds more ways to start it.

</domain>

<decisions>
## Implementation Decisions

### Quick Settings Tile
- Tap toggles light on/off (launches app when off, closes when on)
- Tile shows active/inactive state visually
- Long-press behavior: Claude's discretion

### Home Screen Widget
- Multiple sizes available (1x1, 2x1, etc.)
- All sizes have same function: toggle screen light
- Larger sizes = bigger tap target, not more controls
- Widget shows visual state (lit vs off appearance)

### Shake Detection
- Default sensitivity: light shake (easy to trigger)
- Sensitivity adjustable in settings
- Short cooldown (~1 second) after activation to prevent rapid on/off
- Vibration pulse confirms shake detection before launch
- User can toggle when shake detection is active in settings

### Lock Screen Behavior
- Light shows immediately over lock screen — no unlock required
- Full gestures enabled (tilt for brightness/color, double-tap toggle)
- No visual indicator that device is locked — clean light interface
- Return behavior after dismissing: Claude's discretion

### Claude's Discretion
- QS tile long-press action
- Where to return after dismissing light from lock screen
- Exact shake detection thresholds
- Widget visual design details

</decisions>

<specifics>
## Specific Ideas

- Light shake as default because the use case is "fumbling in the dark" — should be easy to trigger
- Vibration feedback important because user may not be looking at screen when shaking

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 04-launch-methods-lock-screen*
*Context gathered: 2026-01-31*
