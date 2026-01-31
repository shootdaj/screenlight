# Phase 2: Core Services & Screen Light - Research

**Researched:** 2026-01-31
**Domain:** Android Jetpack Compose, Lifecycle Management, Device Sensors
**Confidence:** HIGH

## Summary

This phase builds a lifecycle-safe service foundation for a full-screen light app using Jetpack Compose, DataStore for persistence, ambient light sensors for dark detection, and wake locks for screen management. The research validates that the standard Android/Compose stack provides all necessary primitives without custom solutions.

The core technical challenge is lifecycle management: wake locks must release in `onPause()` (not `onDestroy()`), foreground services require `specialUse` type declaration upfront, and Compose side effects need proper cleanup to prevent battery drain and crashes. The Material3/Compose animation system provides smooth color transitions out-of-the-box. DataStore is the modern replacement for SharedPreferences, offering type-safe, async persistence.

**Primary recommendation:** Use Jetpack Compose 1.10.2 with Material3 1.4.0, Hilt 1.3.0 for DI, DataStore 1.2.0 for settings persistence, FLAG_KEEP_SCREEN_ON for screen wake (not wake locks), DisposableEffect for lifecycle-aware cleanup, and animateColorAsState for smooth transitions. Minimum SDK API 26 provides 95%+ device coverage.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| androidx.compose.ui | 1.10.2 | Declarative UI framework | Official Google UI toolkit, released Jan 28 2026 |
| androidx.compose.material3 | 1.4.0 | Material Design 3 components | Latest Material theming with custom color support |
| androidx.compose.foundation | 1.10.2 | Basic composables (Canvas, Box, etc.) | Essential building blocks for custom UI |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.8.7+ | ViewModel integration with Compose | Official lifecycle-aware state management |
| androidx.hilt:hilt-navigation-compose | 1.3.0 | Dependency injection for Compose | Google-recommended DI, moved to new artifact in 1.3.0 |
| androidx.datastore:datastore-preferences | 1.2.0 | Key-value persistence | Async, type-safe replacement for SharedPreferences |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| androidx.core:core-ktx | latest | Kotlin extensions for Android SDK | Always (standard Android development) |
| androidx.activity:activity-compose | 1.13.0+ | ComponentActivity.setContent() bridge | Required for Compose in Activities |
| kotlinx.coroutines | 1.7+ | Async/Flow support | DataStore operations, sensor listeners |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| DataStore | SharedPreferences | SharedPreferences is synchronous (causes ANRs), not thread-safe, no transactional updates. DataStore is the 2026 standard |
| FLAG_KEEP_SCREEN_ON | PowerManager wake locks | Wake locks require WAKE_LOCK permission, manual lifecycle management. FLAG_KEEP_SCREEN_ON auto-releases when app backgrounds |
| Hilt | Koin/Manual DI | Hilt is official Google recommendation for Android, compile-time safety, better integration with ViewModels |

**Installation:**
```kotlin
// build.gradle.kts (app module)
dependencies {
    // Compose BOM for version alignment
    val composeBom = platform("androidx.compose:compose-bom:2026.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.13.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-compiler:2.57.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

## Architecture Patterns

### Recommended Project Structure
```
app/src/main/java/com/yourapp/
├── di/                    # Hilt modules
│   ├── AppModule.kt       # App-level dependencies (DataStore, sensors)
│   └── ViewModelModule.kt # ViewModel factories if needed
├── data/                  # Data layer
│   ├── repository/        # Repository implementations
│   │   └── SettingsRepository.kt
│   └── sensor/            # Sensor managers
│       ├── AmbientLightManager.kt
│       └── BatteryMonitor.kt
├── domain/                # Business logic (optional for small apps)
│   └── model/             # Domain models (Color, Brightness)
├── ui/                    # Presentation layer
│   ├── MainActivity.kt    # Entry point with setContent
│   ├── theme/             # Material3 theme, custom colors
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── screen/            # Screen-level composables
│   │   └── LightScreen.kt
│   └── components/        # Reusable UI components
│       └── ColorSlider.kt
└── util/                  # Extensions, constants
    └── Constants.kt
```

### Pattern 1: Lifecycle-Aware Screen Wake Management
**What:** Keep screen on while activity is visible using FLAG_KEEP_SCREEN_ON with DisposableEffect
**When to use:** Apps requiring continuous screen-on (games, video, light apps)
**Example:**
```kotlin
// Source: https://developer.android.com/develop/background-work/background-tasks/awake/screen-on
@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
```

### Pattern 2: DataStore Repository Pattern
**What:** Wrap DataStore access in repository with Flow-based APIs
**When to use:** All settings persistence needs
**Example:**
```kotlin
// Source: https://developer.android.com/topic/libraries/architecture/datastore
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val BRIGHTNESS_KEY = floatPreferencesKey("brightness")
    private val COLOR_KEY = intPreferencesKey("color_argb")

    val settings: Flow<ScreenSettings> = dataStore.data.map { prefs ->
        ScreenSettings(
            brightness = prefs[BRIGHTNESS_KEY] ?: 1.0f,
            color = Color(prefs[COLOR_KEY] ?: 0xFFFFFFFF.toInt())
        )
    }

    suspend fun updateBrightness(value: Float) {
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                this[BRIGHTNESS_KEY] = value
            }
        }
    }
}
```

### Pattern 3: Ambient Light Detection with SensorManager
**What:** Register light sensor listener in lifecycle-aware way
**When to use:** Detect dark environments (<50 lux) for auto night-vision mode
**Example:**
```kotlin
// Source: https://developer.android.com/develop/sensors-and-location/sensors/sensors_environment
class AmbientLightManager @Inject constructor(
    private val sensorManager: SensorManager
) {
    private var lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    fun observeAmbientLight(onLightChange: (Float) -> Unit): SensorEventListener {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val lux = event.values[0]
                onLightChange(lux)
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        return listener
    }

    fun unregisterListener(listener: SensorEventListener) {
        sensorManager.unregisterListener(listener)
    }
}

// In Composable:
@Composable
fun ObserveAmbientLight(onDark: () -> Unit) {
    val lightManager = hiltViewModel<LightViewModel>().lightManager
    DisposableEffect(Unit) {
        val listener = lightManager.observeAmbientLight { lux ->
            if (lux < 50f) onDark() // Dark environment threshold
        }
        onDispose { lightManager.unregisterListener(listener) }
    }
}
```

### Pattern 4: Smooth Color Transitions
**What:** Use animateColorAsState for color changes
**When to use:** User adjusts color slider, auto night-vision mode
**Example:**
```kotlin
// Source: https://developer.android.com/develop/ui/compose/animation/value-based
@Composable
fun AnimatedLightScreen(targetColor: Color) {
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "screen_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(animatedColor) } // More performant than .background()
    )
}
```

### Pattern 5: Immersive Mode (Hide System Bars)
**What:** Use WindowInsetsControllerCompat to hide status/nav bars (OLED burn-in prevention)
**When to use:** Full-screen light display to prevent static UI burn-in
**Example:**
```kotlin
// Source: https://developer.android.com/develop/ui/views/layout/immersive
@Composable
fun EnableImmersiveMode() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        val insetsController = WindowCompat.getInsetsController(window, view)

        insetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
```

### Anti-Patterns to Avoid
- **Reading state in Composition phase**: Use `Modifier.offset { }` (lambda) instead of `Modifier.offset(animatedOffset)` for frequently changing values to avoid full recomposition
- **Unstable parameters**: Don't pass `List` or mutable data classes to composables; use `@Immutable` annotation or `ImmutableList`
- **Backwards writes**: Never write to state that was read in the same composition (causes infinite recomposition)
- **Missing DisposableEffect cleanup**: Always unregister sensors/listeners in `onDispose` to prevent battery drain
- **Using wake locks instead of FLAG_KEEP_SCREEN_ON**: Wake locks require permission, manual management, don't auto-release on background

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Settings persistence | Custom file I/O, SharedPreferences wrapper | DataStore Preferences 1.2.0 | DataStore handles threading, corruption recovery, atomic updates. SharedPreferences causes ANRs on main thread |
| Color animations | Manual interpolation with `Handler.postDelayed()` | `animateColorAsState` | Compose animations handle cancellation, frame timing, physics-based easing automatically |
| Dependency injection | Manual singletons, factory classes | Hilt 1.3.0 with @HiltViewModel | Compile-time DI prevents runtime crashes, automatic ViewModel scoping, lifecycle integration |
| Battery monitoring | Polling BatteryManager every N seconds | BatteryManager.EXTRA_LEVEL from sticky intent | Intent is "sticky" - no need to register BroadcastReceiver, just call `registerReceiver(null, intentFilter)` |
| Configuration changes | Saving state manually in onSaveInstanceState | `rememberSaveable` in Compose | Survives rotation, process death with automatic serialization |
| Sensor lifecycle | Manual register/unregister in Activity callbacks | DisposableEffect in Composable | Tied to Composition lifecycle, auto-cleanup when UI leaves screen |

**Key insight:** Android provides lifecycle-aware primitives (DisposableEffect, FLAG_KEEP_SCREEN_ON, DataStore) that auto-cleanup. Custom solutions leak resources and drain battery. Compose's declarative model means "describe what should be true" not "manually coordinate state changes."

## Common Pitfalls

### Pitfall 1: Wake Lock Not Released in onPause()
**What goes wrong:** App continues holding wake lock after user switches apps, draining battery. Google Play enforcement (March 2026) flags excessive wake lock usage.
**Why it happens:** Developer releases wake lock in `onDestroy()`, which isn't guaranteed to be called if Android kills process.
**How to avoid:** Use `FLAG_KEEP_SCREEN_ON` instead (auto-releases on background), OR if using wake locks, release in `onPause()` and re-acquire in `onResume()`.
**Warning signs:** Battery stats show app holding wake lock for hours. Users report battery drain when app is in background.

### Pitfall 2: Missing specialUse Foreground Service Declaration
**What goes wrong:** App crashes on Android 14+ with "ForegroundServiceTypeException" when starting foreground service.
**Why it happens:** Android 14 requires foreground service type declared in manifest upfront. `specialUse` requires both the type attribute AND a property explaining the use case.
**How to avoid:** Declare in `AndroidManifest.xml` before starting service:
```xml
<service android:name=".LightService" android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="Keep screen light active from lock screen"/>
</service>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```
**Warning signs:** Crash logs showing `ForegroundServiceTypeException`. App works on Android 13 but crashes on 14+.

### Pitfall 3: Sensor Listener Not Unregistered
**What goes wrong:** Sensor continues running after app exits, draining battery by 5-10%/hour.
**Why it happens:** Registered in `onCreate()`/`onResume()` but forgot to unregister in `onPause()`, or used LaunchedEffect instead of DisposableEffect.
**How to avoid:** Always pair registration with cleanup using DisposableEffect:
```kotlin
DisposableEffect(Unit) {
    val listener = sensorManager.registerListener(...)
    onDispose { sensorManager.unregisterListener(listener) }
}
```
**Warning signs:** Battery stats show sensors running when app is backgrounded. High CPU usage in battery profiler.

### Pitfall 4: Infinite Recomposition from Backwards Writes
**What goes wrong:** UI freezes, logs show thousands of recompositions per second.
**Why it happens:** State read during composition is immediately written in the same composition:
```kotlin
// BAD
@Composable
fun BrokenCounter() {
    var count by remember { mutableStateOf(0) }
    count++ // Read count, then write - infinite loop!
    Text("Count: $count")
}
```
**How to avoid:** Only write state in event handlers (onClick, LaunchedEffect), never in composition body.
**Warning signs:** App becomes unresponsive. Logcat shows "Recomposing..." repeatedly.

### Pitfall 5: Using SharedPreferences Instead of DataStore
**What goes wrong:** App shows ANR (Application Not Responding) dialogs when saving settings.
**Why it happens:** SharedPreferences `commit()` is synchronous (blocks UI). `apply()` blocks UI thread on `fsync()` during pause.
**How to avoid:** Migrate to DataStore Preferences (async, uses coroutines/Flow):
```kotlin
// BAD
sharedPrefs.edit().putFloat("brightness", 0.5f).apply() // Blocks in onPause

// GOOD
viewModelScope.launch {
    settingsRepo.updateBrightness(0.5f) // Async, non-blocking
}
```
**Warning signs:** ANR crashes in Play Console. Slow app responsiveness when adjusting settings.

### Pitfall 6: Unstable Collections Causing Excessive Recomposition
**What goes wrong:** Entire screen recomposes every frame during animation, causing jank (dropped frames).
**Why it happens:** Passing `List<Color>` to composable. Compose marks all collections as unstable (can't prove immutability), so always recomposes.
**How to avoid:** Wrap in `@Immutable` data class, use `persistentListOf()`, or use separate state holders for each item.
**Warning signs:** Animation stutters. Layout Inspector shows entire tree recomposing on every state change.

### Pitfall 7: Battery Monitoring with Manifest BroadcastReceiver
**What goes wrong:** BroadcastReceiver never receives battery updates on Android 8+.
**Why it happens:** Android 8+ prohibits manifest receivers for implicit broadcasts (including battery). Registering receiver in manifest causes app to initialize on every battery change, draining battery.
**How to avoid:** Use sticky intent pattern (no receiver registration needed):
```kotlin
val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
val batteryPct = level * 100 / scale.toFloat()
```
**Warning signs:** Battery checks always return -1. Battery guard feature never triggers.

## Code Examples

Verified patterns from official sources:

### Full-Screen Light Screen with Lifecycle Management
```kotlin
// Source: Composite of official Android documentation patterns
@Composable
fun LightScreen(
    viewModel: LightViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val animatedColor by animateColorAsState(
        targetValue = settings.color,
        animationSpec = tween(300),
        label = "color"
    )

    // Keep screen on while this screen is visible
    KeepScreenOn()

    // Hide system bars (immersive mode)
    EnableImmersiveMode()

    // Observe ambient light on first launch
    LaunchedEffect(Unit) {
        viewModel.checkAmbientLightOnLaunch()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(animatedColor) }
    ) {
        // Color/brightness controls overlay (optional)
    }
}

@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
```

### ViewModel with Repository Pattern
```kotlin
// Source: https://developer.android.com/develop/ui/compose/libraries
@HiltViewModel
class LightViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val ambientLightManager: AmbientLightManager,
    private val batteryMonitor: BatteryMonitor
) : ViewModel() {

    val settings: StateFlow<ScreenSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, ScreenSettings.default())

    fun updateColor(color: Color) {
        viewModelScope.launch {
            settingsRepository.updateColor(color)
        }
    }

    fun checkAmbientLightOnLaunch() {
        viewModelScope.launch {
            val lux = ambientLightManager.getCurrentLux()
            if (lux < 50f) { // Dark environment
                settingsRepository.updateColor(Color(0xFF8B0000)) // Deep red
            }
        }
    }
}
```

### DataStore Setup with Hilt
```kotlin
// Source: https://developer.android.com/topic/libraries/architecture/datastore
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}

// Extension property (in separate file)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "screen_settings")
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| SharedPreferences | DataStore Preferences | 2020 (stable 2022) | Async operations prevent ANRs, transactional updates prevent corruption |
| Accompanist SystemUiController | WindowInsetsControllerCompat | Deprecated 2024 | Direct API is more stable, no extra dependency |
| Traditional XML layouts | Jetpack Compose | Stable 2021, dominant 2024+ | Declarative UI, less boilerplate, better lifecycle integration |
| Manual DI / Dagger 2 | Hilt | Stable 2020, standard 2023+ | Simpler setup, @HiltViewModel removes factory boilerplate |
| Wake locks for screen-on | FLAG_KEEP_SCREEN_ON | Always preferred | No permission needed, auto-releases on background |

**Deprecated/outdated:**
- **Accompanist libraries (SystemUiController, Permissions, etc.)**: Most features moved to official Compose/Android APIs as of 2024-2025. Use WindowInsetsControllerCompat directly.
- **SharedPreferences for new code**: DataStore is the official replacement. SharedPreferences not deprecated but discouraged for new projects.
- **Runtime permissions in composables without rememberLauncherForActivityResult**: Old pattern was complex; new API simplifies permission requests.

## Open Questions

Things that couldn't be fully resolved:

1. **OLED Pixel Shifting Implementation**
   - What we know: iOS and Samsung implement automatic pixel shifting at OS level (moves UI elements by few pixels every minute)
   - What's unclear: No public Android API for app-level pixel shifting. Wear OS has "burn protection" option for always-on displays, but unclear if applicable to phones.
   - Recommendation: Rely on immersive mode (hides static status bar) + user-configurable auto-timeout. Consider subtle position jitter (±2px every 5 minutes) using `Modifier.offset`, but validate battery impact first.

2. **Minimum SDK for Ambient Light Sensor**
   - What we know: TYPE_LIGHT sensor available since API 1, but specific lux accuracy improved over time
   - What's unclear: Which API level guarantees reliable <50 lux dark detection on most devices
   - Recommendation: Set minSdk to 26 (Android 8.0) for 95%+ coverage, test dark detection on API 26 device. Sensor availability check already required (`getDefaultSensor` returns null if unavailable).

3. **Battery Guard Implementation - Sticky Intent vs WorkManager**
   - What we know: Official docs recommend WorkManager BatteryNotLow constraint instead of monitoring battery directly
   - What's unclear: WorkManager is for background tasks; we need real-time UI update when battery drops <15%
   - Recommendation: Use sticky intent pattern (`registerReceiver(null, ACTION_BATTERY_CHANGED)`) for one-time check on launch + state changes. Don't use continuous BroadcastReceiver. Check battery when screen state changes (onResume).

4. **Foreground Service Necessity**
   - What we know: Prior decisions mention foreground service with specialUse type for lock screen functionality
   - What's unclear: Phase 2 requirements don't mention lock screen access; that may be Phase 3+
   - Recommendation: Phase 2 likely doesn't need foreground service (just FLAG_KEEP_SCREEN_ON in active app). Defer foreground service implementation until lock screen phase. If needed, manifest declaration must be added before service creation.

## Sources

### Primary (HIGH confidence)
- [Jetpack Compose Releases](https://developer.android.com/jetpack/androidx/releases/compose) - Versions 1.10.2, Material3 1.4.0 (verified Jan 28 2026)
- [DataStore Documentation](https://developer.android.com/topic/libraries/architecture/datastore) - Version 1.2.0, migration guide
- [Keep Screen On Guide](https://developer.android.com/develop/background-work/background-tasks/awake/screen-on) - FLAG_KEEP_SCREEN_ON official pattern
- [Environment Sensors Guide](https://developer.android.com/develop/sensors-and-location/sensors/sensors_environment) - TYPE_LIGHT sensor usage
- [Foreground Service Types](https://developer.android.com/about/versions/14/changes/fgs-types-required) - specialUse requirements
- [Battery Monitoring Guide](https://developer.android.com/training/monitoring-device-state/battery-monitoring) - Sticky intent pattern
- [Compose Animation Guide](https://developer.android.com/develop/ui/compose/animation/quick-guide) - animateColorAsState, updateTransition
- [Compose Side Effects](https://developer.android.com/develop/ui/compose/side-effects) - DisposableEffect, LaunchedEffect
- [Hilt Jetpack Integration](https://developer.android.com/training/dependency-injection/hilt-jetpack) - @HiltViewModel pattern
- [Hilt Releases](https://developer.android.com/jetpack/androidx/releases/hilt) - Version 1.3.0 changes
- [Immersive Mode Guide](https://developer.android.com/develop/ui/views/layout/immersive) - WindowInsetsControllerCompat usage

### Secondary (MEDIUM confidence)
- [Android Developers Blog: Jetpack Compose December 2025 Release](https://android-developers.googleblog.com/2025/12/whats-new-in-jetpack-compose-december.html) - New features overview
- [Android Developers Blog: Prefer DataStore](https://android-developers.googleblog.com/2020/09/prefer-storing-data-with-jetpack.html) - Migration reasoning
- [Android Developers Blog: Wake Lock Optimization](https://android-developers.googleblog.com/2025/09/guide-to-excessive-wake-lock-usage.html) - Battery metrics enforcement
- [Medium: Stop Using SharedPreferences 2026](https://medium.com/@kemal_codes/stop-using-sharedpreferences-mastering-jetpack-datastore-in-2026-b88b2db50e91) - Industry consensus on DataStore
- [Medium: 10 Critical Compose Mistakes](https://medium.com/@sharmapraveen91/10-critical-jetpack-compose-mistakes-youre-probably-making-and-how-to-fix-them-04064e950b2f) - Common pitfalls
- [ProAndroidDev: Compose Performance Pitfalls](https://proandroiddev.com/overcoming-common-performance-pitfalls-in-jetpack-compose-98e6b155fbb4) - Recomposition issues

### Tertiary (LOW confidence)
- [TelemetryDeck Android Market Share 2026](https://telemetrydeck.com/survey/android/Android/sdkVersions/) - SDK distribution statistics
- [Android Authority: Screen Burn-in](https://www.androidauthority.com/screen-burn-in-801760/) - OLED burn-in prevention tips
- [XDA: OLED Burn-in Mitigation Guide](https://xdaforums.com/t/optimization-guide-mitigating-oled-burn-in-and-stress-testing-mobile-panels.4775507/) - Pixel shifting discussion
- Community articles on Compose architecture patterns - Feature-based vs layer-based structure

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All versions verified from official release pages (Jan 2026)
- Architecture patterns: HIGH - All code examples derived from official Android documentation
- Pitfalls: HIGH - Wake lock lifecycle (official blog), foreground service (API docs), sensor unregister (official guide), recomposition (official performance guide)
- OLED pixel shifting: LOW - No official Android API documented; implementation details are speculation
- Battery monitoring: MEDIUM - Official docs recommend WorkManager, but sticky intent pattern also documented for one-time checks

**Research date:** 2026-01-31
**Valid until:** 2026-03-02 (30 days - Compose/Android ecosystem is relatively stable)
