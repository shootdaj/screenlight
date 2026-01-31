# Feature Landscape

**Domain:** Android Screen Light / Flashlight Apps
**Researched:** 2026-01-31
**Confidence:** MEDIUM (verified via WebSearch, cross-referenced multiple sources)

## Executive Summary

The Android flashlight/screen light app ecosystem is mature with clear table stakes and strong privacy concerns. Users expect instant activation methods (widgets, quick settings, shake-to-activate) and basic LED flashlight control. Screen light apps differentiate through color control (especially red light for night vision), gesture-based controls, and battery efficiency.

**Key insight:** The native Android flashlight (available since Android 5.0) handles basic LED use cases, so third-party apps must justify their existence through superior UX, screen light functionality, or gesture controls that the system doesn't provide.

**Privacy warning:** Flashlight apps have historically been privacy nightmares. Android flashlight apps request an average of 25 permissions, with some requesting up to 77 permissions. Starting March 2026, Google Play will flag battery-draining apps with public badges.

## Table Stakes

Features users expect. Missing these = product feels incomplete or users question why they shouldn't just use the native flashlight.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **LED Flashlight Toggle** | Core function; native Android has this since 2014 | Low | Simple camera flash API, must work instantly |
| **Quick Settings Tile** | Standard Android pattern; native flashlight uses this | Low | Requires TileService implementation |
| **Home Screen Widget** | Users expect instant access without opening app | Medium | 1x1 widget is standard size for flashlight apps |
| **Screen Light (White)** | Basic screen-as-light functionality | Low | Full-brightness white screen |
| **Screen Brightness Control** | Users need to adjust intensity | Low | Slider or gesture, range 0-100% |
| **Works from Lock Screen** | Emergency use case; native flashlight accessible when locked | Medium | Quick settings and widget must work when locked |
| **Minimal Permissions** | Users are wary after privacy scandals | Low | Only camera (for LED) and wake lock (to keep screen on) |
| **No Ads (or minimal)** | Intrusive ads are dealbreakers for utility apps | N/A | Business model consideration |
| **Battery Efficiency** | LED uses 300-600mAh/hour; apps must not add overhead | Medium | Avoid wake locks >2 hours/day (Google policy) |
| **Instant On/Off** | Flashlight must respond within 100ms | Low | No loading screens or animations blocking function |

## Differentiators

Features that set products apart. Not expected, but valued when present.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Gesture-Based Control** | **PRIMARY DIFFERENTIATOR for Screenlight** | High | Volume button + tilt is novel; most apps use shake or volume-only |
| **Color Temperature Range** | Night vision preservation (red) + warm/cool white options | Medium | Deep red (620-700nm) → warm (3000K) → cool (6500K) |
| **Red Light Mode** | Astronomy/camping use case; preserves night vision | Low | Full-screen red, adjustable brightness |
| **Shake to Activate** | No-look activation; competitive with native shortcuts | Medium | Accelerometer detection; Motorola/Vivo have this built-in |
| **Smooth Transitions** | **Screenlight differentiator:** Fade instead of instant changes | Medium | Prevents eye strain in dark; most apps do instant changes |
| **Battery Guard** | Auto-dim or warn at low battery levels | Low | Shows care for user's device |
| **Color Presets** | Quick access to common colors (red, orange, white) | Low | Saves users from manual color picking |
| **Adjustable Flashlight Brightness** | Android 16 QPR3 adding this to native flashlight (March 2026) | Medium | API available in Camera2; will become table stakes soon |
| **Tilt-Based Control** | **Screenlight unique:** Control brightness/color via phone angle | High | Requires gyroscope/accelerometer; novel interaction |
| **Volume Button Control** | Both volume buttons = toggle; works when screen off | Medium | Accessibility and convenience; apps like Torchie popularized this |
| **Offline Functionality** | Works without internet (obvious but worth stating) | Low | Users distrust apps that require network for flashlight |
| **Lightweight (<5MB)** | Respects device storage; simple tools should be small | Low | Torch app is <1MB and users love this |

## Anti-Features

Features to explicitly NOT build. Common mistakes in this domain.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **Excessive Permissions** | Avast found apps requesting 77+ permissions; kills trust immediately | Only request camera (LED), wake lock (screen-on). Nothing else. |
| **Location Tracking** | Brightest Flashlight FTC case: shared user location with advertisers | Never request location. No legitimate use case. |
| **Contact/SMS Access** | 180+ apps request contact lists; zero justification | Never request contacts, SMS, phone, or storage. |
| **Audio Recording** | 77 apps request audio recording permission | Never request microphone access. |
| **Intrusive Ads** | Users abandon utility apps with full-screen ads | If monetizing: small banner max, or one-time purchase |
| **Strobe/Party Modes** | LED strobe at high frequency can trigger seizures | Skip the club/party features. Medical risk not worth it. |
| **SOS/Morse Code** | Niche feature (emergency signaling) adds complexity for <1% use | Skip unless emergency focus; most users never use this |
| **Complex UI** | Flashlight should be one tap away | Maximum 2 taps to activate from launch |
| **Background Services** | March 2026 Google policy: >2hr wake locks = public warning badge | Only wake lock when flashlight/screen light actively in use |
| **Social Sharing** | No one shares flashlight activity; feature bloat | Skip all social integration |
| **Compass/Level/Tools** | "Swiss army knife" apps feel bloated | Stay focused: light control only |
| **Auto-Start on Boot** | Creepy and unnecessary for flashlight | Only start when user explicitly requests |
| **Network Requirement** | Some apps refused to work offline (for ads) | Work 100% offline; no network calls |

## Feature Dependencies

```
Core Foundation
├── LED Flashlight Toggle ──→ Adjustable LED Brightness (coming in Android 16)
├── Screen Light (White) ──→ Color Temperature Range
│                         ──→ Red Light Mode
│                         ──→ Screen Brightness Control
└── Screen Brightness Control ──→ Smooth Transitions (fade in/out)
                              ──→ Battery Guard (auto-dim)

Activation Methods (independent, choose multiple)
├── Quick Settings Tile ──→ Works from Lock Screen
├── Home Screen Widget ──→ Works from Lock Screen
├── Shake to Activate ──→ Works from Lock Screen (requires permission)
└── Volume Button Control ──→ Works from Lock Screen

Screenlight Unique Features
├── Tilt-Based Control ──→ Screen Brightness Control (foundation)
│                      ──→ Color Temperature Range (foundation)
└── Volume Button + Tilt ──→ Gesture-Based Control (combined system)
```

## MVP Recommendation

For Screenlight MVP, prioritize features that justify choosing this app over native Android flashlight:

### Phase 1: Core Functionality (Table Stakes)
1. **LED Flashlight Toggle** - Baseline parity with native
2. **Screen Light with Brightness Control** - Core value prop
3. **Quick Settings Tile** - Essential activation method
4. **Color Temperature Range** - Deep red → warm → cool (differentiator)
5. **Minimal Permissions** - Camera + wake lock only

### Phase 2: Gesture Controls (Primary Differentiator)
6. **Volume Button Control** - Hold both volume buttons = toggle
7. **Tilt-Based Brightness/Color** - Novel interaction (Screenlight unique)
8. **Smooth Transitions** - Fade effects for eye comfort

### Phase 3: Convenience & Polish
9. **Home Screen Widget** - 1x1 widget for instant access
10. **Shake to Activate** - Alternative gesture method
11. **Battery Guard** - Auto-dim below 20% battery
12. **Color Presets** - Quick access to red, warm, cool

### Defer to Post-MVP
- **Adjustable LED Brightness**: Will become table stakes when Android 16 QPR3 launches (March 2026), but can wait for MVP
- **Multiple Widget Sizes**: Start with 1x1, add 2x1 later if users request
- **Advanced Color Picker**: Color presets sufficient for MVP
- **Strobe/SOS**: Anti-feature per research; skip entirely
- **Monetization**: Launch free, add optional tip jar later

## Feature Complexity Estimates

| Complexity | Features | Effort |
|------------|----------|--------|
| **Low** | LED toggle, white screen, brightness slider, red mode, battery guard, color presets, permissions | 1-2 days each |
| **Medium** | Quick settings tile, widget, shake activation, color temperature range, smooth transitions, LED brightness API | 3-5 days each |
| **High** | Tilt-based control, volume button + tilt gesture system | 1-2 weeks each |

## Competitive Analysis Insights

Based on research of existing apps (Color Flashlight, Torch, Simple Flashlight, Torchie, Flashlight by Millenium):

**What works:**
- Simplicity wins: Torch app (<1MB, minimal features) has strong user love
- Gesture controls popular: Shake and volume button features heavily used
- Red light mode: Astronomy/camping communities specifically seek this
- No-ad versions: Users willing to pay $1-3 to remove ads

**What fails:**
- Feature bloat: Compasses, levels, neon signs feel gimmicky
- Privacy invasion: Historical scandals make users scrutinize permissions
- Battery drain: Apps that use wake locks excessively get uninstalled
- Slow activation: Any delay between tap and light = user frustration

**Screenlight's opportunity:**
The gesture-based control (volume button + tilt) is unexplored territory. Existing apps offer:
- Shake only (common)
- Volume button only (Torchie)
- Proximity sensor wave (Millenium Apps)

No app combines volume button hold + tilt gestures for brightness/color control. This is Screenlight's unique value proposition.

## Sources

### Feature Discovery
- [Flashlight - Apps on Google Play](https://play.google.com/store/apps/details?id=com.peacock.flashlight&hl=en_US)
- [Color Torch (LED flash light) – Apps on Google Play](https://play.google.com/store/apps/details?id=com.socialnmobile.hd.flashlight&hl=en_IN)
- [Simple Flashlight | F-Droid](https://f-droid.org/en/packages/com.simplemobiletools.flashlight/)
- [The best Android flashlight apps with no extra permissions - Android Authority](https://www.androidauthority.com/best-android-flashlight-apps-with-no-extra-permissions-568939/)
- [Top 9 Flashlight Apps - Hyperlink InfoSystem](https://www.hyperlinkinfosystem.com/blog/top-9-flashlight-apps)

### Night Vision / Red Light
- [Red Light - Apps on Google Play](https://play.google.com/store/apps/details?id=com.apporilla.redlight&hl=en_US)
- [Light for Night vision - Apps on Google Play](https://play.google.com/store/apps/details?id=jp.co.vixen.nightvisionlight&hl=en_US)
- [Android App to Turn Screen Dark Red - Cloudy Nights](https://www.cloudynights.com/topic/878310-android-app-to-turn-screen-dark-red/)

### Privacy & Permissions
- [Flashlight Apps on Google Play Request Up to 77 Permissions - Avast](https://blog.avast.com/flashlight-apps-on-google-play-request-up-to-77-permissions-avast-finds)
- [Flashlight App Steals Data - McAfee Blog](https://www.mcafee.com/blogs/mobile-security/android-flashlight-app-steals-data/)
- [Android Security Alert: Delete These Flashlight Apps Now - Tom's Guide](https://www.tomsguide.com/news/these-android-flashlight-apps-could-be-spying-on-you)

### Gesture Controls
- [Flashlight: Volume button LED – Apps on Google Play](https://play.google.com/store/apps/details?id=erfanrouhani.flashlight&hl=en_GB)
- [Turn Your Phone's Flashlight on Instantly - Gadget Hacks](https://android.gadgethacks.com/how-to/turn-your-phones-flashlight-instantly-with-one-these-6-tricks-0167811/)
- [How to turn on your Android phone's flashlight by shaking it - Gadget Bridge](https://www.gadgetbridge.com/how-to/how-to-turn-on-your-android-phones-flashlight-by-shaking-it/)

### Quick Settings & Widgets
- [How to Turn Flashlight On and Off on Android - Make Tech Easier](https://www.maketecheasier.com/turn-on-flashlight-android/)
- [6 ways to turn on the flashlight on your Android phone - Android Police](https://www.androidpolice.com/how-to-turn-on-android-flashlight/)
- [Android 16 QPR3 lets you adjust flashlight brightness on Pixel - 9to5Google](https://9to5google.com/2025/12/17/android-16-qpr3-flashlight/)

### Battery & Performance
- [Google Play to Warn Users of Battery-Draining Apps in 2026 - WebProNews](https://www.webpronews.com/google-play-store-to-warn-users-of-battery-draining-apps-in-2026/)
- [How do phone flashlights work and affect battery? - SamMobile](https://www.sammobile.com/news/how-do-phone-flashlights-work-and-affect-battery/)

### Emergency Features (SOS/Morse)
- [SOS Flashlight: Advanced Morse Code Tool - F-Droid](https://f-droid.org/en/packages/org.wbftw.weil.sos_flashlight/)
- [Flashlight Strobe Light & LED - Apps on Google Play](https://play.google.com/store/apps/details?id=net.wellyglobal.led.flashlight.torchlight.colorlight.pro&hl=en_US)

### Color Temperature
- [Color Flashlight - Apps on Google Play](https://play.google.com/store/apps/details?id=com.socialnmobile.hd.flashlight&hl=en_US)
- [Flashlight Color Temperature - Flashlight University](https://www.flashlightuniversity.com/flashlight-color-temperature/)
- [How to Choose Cool White Flashlight and Neutral White Flashlight - Olight](https://www.olight.com/blog/cool-white-flashlight-and-neutral-white-flashlight)

### Lock Screen Access
- [6 ways to turn on the flashlight on your Android phone - Android Police](https://www.androidpolice.com/how-to-turn-on-android-flashlight/)
- [Samsung's New Lock Screen Shortcut - Gadget Hacks](https://android.gadgethacks.com/how-to/samsungs-new-lock-screen-shortcut-makes-your-galaxys-flashlight-ridiculously-fast-0236340/)

### Technical Implementation
- [How to Maximize/Minimize Screen Brightness Programmatically in Android - GeeksforGeeks](https://www.geeksforgeeks.org/android/how-to-maximize-minimize-screen-brightness-programmatically-in-android/)
- [Programmatically changing screen brightness on Android - GitHub](https://gist.github.com/melihmucuk/6786011)
- [Getting screen brightness right for every user - Android Developers Blog](https://android-developers.googleblog.com/2018/11/getting-screen-brightness-right-for.html)
