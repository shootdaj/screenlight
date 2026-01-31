# Architecture Patterns

**Domain:** Android utility app with sensor integration and background services
**Researched:** 2026-01-31
**Confidence:** HIGH

## Recommended Architecture

Screenlight should follow **Google's Modern App Architecture** with Clean Architecture principles, combining:
- **Layered architecture**: UI, Domain (optional), Data
- **MVVM pattern**: ViewModel + Compose UI
- **Repository pattern**: Single source of truth for state
- **Foreground service**: For wake lock and sensor monitoring
- **State machine**: For gesture detection logic

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│  PRESENTATION LAYER                                      │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  MainActivity │  │   Widget     │  │  QS Tile     │  │
│  │   (Compose)   │  │  Provider    │  │  Service     │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                  │                  │          │
│         └──────────────────┼──────────────────┘          │
│                            │                             │
│                   ┌────────▼────────┐                    │
│                   │  ScreenViewModel │                   │
│                   └────────┬─────────┘                   │
└────────────────────────────┼──────────────────────────────┘
                             │
┌────────────────────────────▼──────────────────────────────┐
│  DOMAIN LAYER (Use Cases)                                 │
│                                                           │
│  ┌──────────────────┐  ┌──────────────────┐             │
│  │ GestureStateMachine│  │ ShakeDetector   │             │
│  │  (volume + tilt)  │  │   Use Case      │             │
│  └──────────────────┘  └──────────────────┘             │
└────────────────────────────┬──────────────────────────────┘
                             │
┌────────────────────────────▼──────────────────────────────┐
│  DATA LAYER                                               │
│                                                           │
│  ┌──────────────────┐  ┌──────────────────┐             │
│  │ ScreenRepository │  │ PreferencesRepo  │             │
│  │  (state SSOT)    │  │   (settings)     │             │
│  └────────┬─────────┘  └──────────────────┘             │
│           │                                              │
│  ┌────────▼────────────────────────────┐                │
│  │    ForegroundService                │                │
│  │  (sensors + wake lock + volume)     │                │
│  │                                     │                │
│  │  ┌─────────────┐  ┌──────────────┐ │                │
│  │  │SensorManager│  │ WakeLock     │ │                │
│  │  │ (accel/gyro)│  │ Manager      │ │                │
│  │  └─────────────┘  └──────────────┘ │                │
│  └─────────────────────────────────────┘                │
└───────────────────────────────────────────────────────────┘
```

## Component Boundaries

| Component | Responsibility | Communicates With | Lifecycle |
|-----------|---------------|-------------------|-----------|
| **MainActivity** | Display UI, collect user inputs | ScreenViewModel | Activity lifecycle |
| **ScreenViewModel** | Hold UI state, coordinate use cases | Repository, Use Cases | Survives config changes |
| **Widget Provider** | Render widget, handle widget clicks | Repository (via WorkManager) | Broadcast receiver |
| **QS Tile Service** | Render tile, handle tile clicks | Repository | TileService lifecycle |
| **GestureStateMachine** | Process volume+tilt into actions | SensorRepository | Owned by ViewModel |
| **ShakeDetector** | Detect shake from sensor data | SensorRepository | Owned by Service |
| **ScreenRepository** | Maintain screen state (color, brightness, on/off) | ForegroundService, Preferences | Singleton (Hilt) |
| **PreferencesRepository** | Persist user settings | DataStore | Singleton (Hilt) |
| **ForegroundService** | Sensors, wake lock, volume interception | SensorManager, PowerManager | Service lifecycle |

### Key Architectural Decisions

**1. Foreground Service as Data Source**
- Service owns hardware access (sensors, wake lock, volume buttons)
- Repository communicates with service via bound service connection
- Service emits sensor data as Flow, consumed by repository

**2. Repository as Single Source of Truth (SSOT)**
- All components read state from ScreenRepository
- Only repository modifies state (unidirectional data flow)
- State exposed as StateFlow for reactive updates

**3. Multiple Entry Points, Single State**
- MainActivity, Widget, QS Tile, Shake all modify same repository
- Repository doesn't care about caller (separation of concerns)
- Deep links route to MainActivity with action parameter

**4. State Machine for Gestures**
- Volume button state (held/released) + tilt direction = gesture
- Implemented as sealed class hierarchy + when expression
- Owned by ViewModel, not repository (UI-specific logic)

## Data Flow

### Normal Launch (App Icon)
```
User taps icon → MainActivity starts → ViewModel observes Repository
→ Repository binds to ForegroundService → Service acquires wake lock
→ Screen turns on → User sees UI
```

### Shake Launch (Lock Screen)
```
Shake occurs → ForegroundService detects → Service checks shake threshold
→ Service notifies Repository → Repository updates state (isActive = true)
→ Repository launches MainActivity with FLAG_SHOW_WHEN_LOCKED
→ MainActivity displays over lock screen
```

### Widget Toggle
```
User taps widget → WidgetProvider receives broadcast
→ WidgetProvider updates Repository via enqueueWork()
→ Repository toggles state → All observers react (Widget, MainActivity if open)
```

### Gesture Control
```
User holds volume → Service detects dispatchKeyEvent
→ Service sets volumeHeld = true → User tilts phone
→ Service emits tilt data → Repository forwards to ViewModel
→ ViewModel runs GestureStateMachine → State updated
→ UI recomposes with new color/brightness
```

### Sensor Data Flow (Detailed)
```
Accelerometer/Gyroscope → SensorEventListener.onSensorChanged()
→ ForegroundService filters data → Publishes to MutableStateFlow
→ Repository collects Flow → Transforms to domain model
→ ViewModel/UseCase subscribes → UI recomposes
```

## Patterns to Follow

### Pattern 1: Bound Service with Repository Mediator

**What:** Repository binds to ForegroundService, mediates between UI and hardware

**When:** Service manages hardware (sensors, wake lock), UI needs reactive state

**Why:** Separates lifecycle concerns (service can outlive activity), testable (mock service binding)

**Example:**
```kotlin
@Singleton
class ScreenRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _screenState = MutableStateFlow(ScreenState.default())
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private var serviceBinder: ScreenServiceBinder? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceBinder = service as? ScreenServiceBinder
            serviceBinder?.sensorDataFlow?.let { flow ->
                flow.collect { sensorData ->
                    // Process sensor data, update state
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBinder = null
        }
    }

    fun bindService() {
        val intent = Intent(context, ScreenForegroundService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun updateBrightness(level: Float) {
        serviceBinder?.setBrightness(level)
        _screenState.update { it.copy(brightness = level) }
    }
}
```

### Pattern 2: State Machine with Sealed Classes

**What:** Model gesture states as sealed class, transitions as pure functions

**When:** Complex state transitions based on multiple inputs (volume + tilt)

**Why:** Exhaustive when expressions, compile-time safety, easy to test

**Example:**
```kotlin
sealed class GestureState {
    object Idle : GestureState()
    data class VolumeHeld(val timestamp: Long) : GestureState()
    data class AdjustingBrightness(val initialValue: Float) : GestureState()
    data class AdjustingColor(val initialHue: Float) : GestureState()
}

sealed class GestureEvent {
    object VolumeDown : GestureEvent()
    object VolumeUp : GestureEvent()
    data class TiltVertical(val angle: Float) : GestureEvent()
    data class TiltHorizontal(val angle: Float) : GestureEvent()
}

class GestureStateMachine {
    fun reduce(state: GestureState, event: GestureEvent): GestureState {
        return when (state) {
            is GestureState.Idle -> when (event) {
                is GestureEvent.VolumeDown ->
                    GestureState.VolumeHeld(System.currentTimeMillis())
                else -> state
            }
            is GestureState.VolumeHeld -> when (event) {
                is GestureEvent.VolumeUp -> GestureState.Idle
                is GestureEvent.TiltVertical ->
                    GestureState.AdjustingBrightness(currentBrightness)
                is GestureEvent.TiltHorizontal ->
                    GestureState.AdjustingColor(currentHue)
                else -> state
            }
            is GestureState.AdjustingBrightness -> when (event) {
                is GestureEvent.VolumeUp -> GestureState.Idle
                is GestureEvent.TiltVertical -> {
                    // Update brightness based on tilt
                    state.copy(initialValue = calculateBrightness(event.angle))
                }
                else -> state
            }
            is GestureState.AdjustingColor -> when (event) {
                is GestureEvent.VolumeUp -> GestureState.Idle
                is GestureEvent.TiltHorizontal -> {
                    // Update color based on tilt
                    state.copy(initialHue = calculateHue(event.angle))
                }
                else -> state
            }
        }
    }
}
```

### Pattern 3: LaunchedEffect for Sensor Lifecycle

**What:** Use LaunchedEffect to collect sensor Flow in Compose UI

**When:** UI needs to react to sensor data while visible

**Why:** Automatically cancels when composable leaves composition (prevents leaks)

**Example:**
```kotlin
@Composable
fun ScreenlightScreen(viewModel: ScreenViewModel = hiltViewModel()) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    // Collect sensor data only while UI is visible
    LaunchedEffect(Unit) {
        viewModel.startSensorMonitoring()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSensorMonitoring()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(screenState.color))
    )
}
```

### Pattern 4: Widget Updates via Repository

**What:** Widget reads state from repository, updates via WorkManager

**When:** Widget needs to stay in sync with app state

**Why:** Avoids direct database access from widget, consistent state source

**Example:**
```kotlin
class ScreenlightWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            // Schedule work to read from repository
            val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInputData(workDataOf("widgetId" to widgetId))
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}

class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ScreenRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val widgetId = inputData.getInt("widgetId", -1)
        val state = repository.screenState.value

        val remoteViews = RemoteViews(
            applicationContext.packageName,
            R.layout.widget_screenlight
        ).apply {
            setTextViewText(R.id.widget_status,
                if (state.isActive) "ON" else "OFF")
        }

        AppWidgetManager.getInstance(applicationContext)
            .updateAppWidget(widgetId, remoteViews)

        return Result.success()
    }
}
```

### Pattern 5: Service Foreground Type Declaration

**What:** Declare `foregroundServiceType="health"` for sensor access

**When:** App needs accelerometer/gyroscope in background

**Why:** Required by Android 14+ for background sensor access

**Example:**
```xml
<!-- AndroidManifest.xml -->
<manifest>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_HEALTH" />
    <uses-permission android:name="android.permission.HIGH_SAMPLING_RATE_SENSORS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <application>
        <service
            android:name=".service.ScreenForegroundService"
            android:foregroundServiceType="health"
            android:exported="false" />
    </application>
</manifest>
```

```kotlin
class ScreenForegroundService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)

        // Register sensor listeners
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        return START_STICKY
    }
}
```

## Anti-Patterns to Avoid

### Anti-Pattern 1: ViewModel in Widget

**What:** Attempting to use ViewModel in AppWidgetProvider

**Why bad:** Widgets don't have lifecycle, ViewModel requires LifecycleOwner

**Consequences:** Crashes, memory leaks, unpredictable behavior

**Instead:** Use Repository directly or via WorkManager (as shown in Pattern 4)

### Anti-Pattern 2: Heavy Work in onSensorChanged()

**What:** Processing sensor data synchronously in callback

**Why bad:** onSensorChanged() runs on main thread, heavy work causes jank

**Consequences:** Dropped frames, ANRs, poor UX

**Instead:** Emit data to Flow, process on background dispatcher
```kotlin
private val _sensorData = MutableSharedFlow<SensorEvent>(extraBufferCapacity = 64)

override fun onSensorChanged(event: SensorEvent) {
    // Just emit, don't process
    _sensorData.tryEmit(event)
}

// Process on background thread
init {
    _sensorData
        .flowOn(Dispatchers.Default)
        .map { event -> processSensorData(event) }
        .collect { processed -> updateState(processed) }
}
```

### Anti-Pattern 3: Holding Wake Lock Without Foreground Service

**What:** Acquiring wake lock directly in Activity or ViewModel

**Why bad:** System will kill wake lock when app goes to background (Android 10+)

**Consequences:** Screen turns off unexpectedly, feature doesn't work from lock screen

**Instead:** Hold wake lock in Foreground Service with persistent notification

### Anti-Pattern 4: Broadcast Receiver for Volume Button

**What:** Using BroadcastReceiver to intercept volume button events

**Why bad:** VOLUME_CHANGED_ACTION fires after volume changes, can't prevent default action

**Consequences:** Volume actually changes, can't distinguish hold vs press

**Instead:** Override dispatchKeyEvent() in Activity or use Accessibility Service (requires user permission)
```kotlin
// In Activity
override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    return when (event.keyCode) {
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_UP -> {
            if (gestureMode) {
                viewModel.handleVolumeEvent(event)
                true // Consume event
            } else {
                super.dispatchKeyEvent(event)
            }
        }
        else -> super.dispatchKeyEvent(event)
    }
}
```

### Anti-Pattern 5: Screen Wake Lock via FLAG_KEEP_SCREEN_ON in Service

**What:** Trying to use WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON in Service

**Why bad:** Services don't have windows, flag requires window context

**Consequences:** Crashes or silently fails

**Instead:** Use PowerManager.WakeLock with SCREEN_BRIGHT_WAKE_LOCK (deprecated but works) or FLAG_KEEP_SCREEN_ON in Activity
```kotlin
// Correct approach in Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ScreenlightTheme {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                ScreenlightScreen()
            }
        }
    }
}
```

### Anti-Pattern 6: Sharing Mutable State Between Service and Repository

**What:** Service and Repository both mutating same MutableStateFlow

**Why bad:** Race conditions, lost updates, unpredictable state

**Consequences:** UI shows wrong state, gestures don't work reliably

**Instead:** Service owns hardware state, emits events; Repository owns business state, handles events
```kotlin
// Service emits events
class ScreenForegroundService : Service() {
    private val _events = MutableSharedFlow<ServiceEvent>()
    val events: SharedFlow<ServiceEvent> = _events.asSharedFlow()

    override fun onSensorChanged(event: SensorEvent) {
        _events.tryEmit(ServiceEvent.SensorUpdate(event))
    }
}

// Repository handles events, updates state
@Singleton
class ScreenRepository @Inject constructor() {
    private val _state = MutableStateFlow(ScreenState())
    val state: StateFlow<ScreenState> = _state.asStateFlow()

    init {
        serviceEvents.collect { event ->
            when (event) {
                is ServiceEvent.SensorUpdate -> handleSensor(event)
            }
        }
    }

    private fun handleSensor(event: SensorEvent) {
        _state.update { /* update based on event */ }
    }
}
```

## Build Order Implications

Recommended build order based on dependency graph:

### Phase 1: Core Infrastructure (No UI)
**Build first because:** Establishes data layer, testable in isolation

1. **Data models** (ScreenState, GestureState, SensorData)
2. **ScreenRepository** (with fake/in-memory state)
3. **PreferencesRepository** (DataStore setup)
4. **Unit tests** for repositories

**Validation:** Repository tests pass, state updates correctly

### Phase 2: Service Layer
**Build second because:** Depends on repositories, provides hardware access

1. **ForegroundService** skeleton (notification, lifecycle)
2. **WakeLock management** (acquire/release in service)
3. **Sensor listener registration** (emit to Flow)
4. **Service binding** (Repository ↔ Service connection)

**Validation:** Service starts, wake lock acquired, sensor data flows to repository

### Phase 3: Basic UI
**Build third because:** Depends on ViewModel which depends on Repository

1. **ScreenViewModel** (observe repository state)
2. **MainActivity + Compose UI** (display color, react to state)
3. **FLAG_KEEP_SCREEN_ON** in window
4. **App launch** flow (icon → activity → service → wake)

**Validation:** App launches, screen stays on, shows color

### Phase 4: Gesture Detection
**Build fourth because:** Depends on sensor data flowing through service/repository

1. **GestureStateMachine** (state transitions)
2. **Volume button interception** (dispatchKeyEvent in Activity)
3. **Tilt calculation** (accelerometer → angle)
4. **Wire gesture events** (ViewModel → StateMachine → Repository)

**Validation:** Hold volume + tilt changes brightness/color

### Phase 5: Lock Screen Launch
**Build fifth because:** Most complex, requires working app + service

1. **ShakeDetector use case** (threshold detection)
2. **Service shake monitoring** (always-on listener)
3. **Launch activity from service** (FLAG_SHOW_WHEN_LOCKED, FLAG_TURN_SCREEN_ON)
4. **Lock screen permissions** (SYSTEM_ALERT_WINDOW on older APIs)

**Validation:** Shake from lock screen launches app

### Phase 6: Alternative Entry Points
**Build sixth because:** Depends on working repository and state

1. **Quick Settings Tile** (TileService + repository)
2. **Home Screen Widget** (WidgetProvider + WorkManager + repository)
3. **Deep links** (navigate to MainActivity with action)

**Validation:** Tile toggles state, widget shows current state, all entry points work

### Phase 7: Polish
**Build last because:** Depends on all features working

1. **Smooth transitions** (animateColorAsState, animateFloatAsState)
2. **Battery guard** (monitor battery level, auto-dim)
3. **Double-tap to toggle** (gesture detector in UI)
4. **LED flashlight** (Camera2 API, long-press volume)

**Validation:** App feels polished, no jank, battery-conscious

## Dependency Graph

```
Phase 1: Data Models & Repositories
         (no dependencies)
                ↓
Phase 2: ForegroundService
         (depends on: ScreenRepository)
                ↓
Phase 3: UI (MainActivity + ViewModel)
         (depends on: ScreenRepository, ForegroundService)
                ↓
Phase 4: Gesture Detection
         (depends on: ViewModel, ForegroundService sensor data)
                ↓
Phase 5: Lock Screen Launch
         (depends on: ForegroundService, ShakeDetector)
                ↓
Phase 6: Widget & QS Tile
         (depends on: ScreenRepository)
                ↓
Phase 7: Polish
         (depends on: all above features)
```

**Critical path:** Repository → Service → UI → Gestures → Lock Screen
**Parallel work:** Widget/Tile can be built anytime after Repository exists

## Scalability Considerations

| Concern | Current (1 user) | Future (optimization) | Strategy |
|---------|------------------|----------------------|----------|
| **Battery drain** | Always-on sensor monitoring | Adaptive sampling rate | Lower sensor delay when screen off, higher when gesturing |
| **Memory usage** | All state in memory | Persist critical state | Save last color/brightness to DataStore on change |
| **Sensor accuracy** | Raw accelerometer data | Sensor fusion | Use RotationVector sensor instead of accelerometer for tilt |
| **Wake lock abuse** | Held while app active | Timeout protection | Auto-release after 10 minutes, require user re-activation |
| **Service lifecycle** | START_STICKY restarts | Persistent notification | Ensure notification can't be swiped away while light active |
| **Multi-instance** | One activity instance | Launch mode handling | `singleTop` or `singleTask` to prevent duplicate activities |

## Testing Strategy

### Unit Tests
- **Repositories**: Mock service binder, verify state updates
- **GestureStateMachine**: Test all state transitions, pure functions
- **ShakeDetector**: Test threshold detection with mock sensor data
- **ViewModels**: Test use case orchestration, state exposure

### Integration Tests
- **Service binding**: Verify repository connects to service
- **Sensor flow**: Verify data propagates from service to repository to ViewModel
- **Widget updates**: Verify WorkManager schedules correctly

### Instrumented Tests
- **Foreground service**: Verify notification appears, wake lock acquired
- **Activity launch**: Verify FLAG_SHOW_WHEN_LOCKED works
- **UI state**: Verify Compose UI reflects repository state changes

### Manual Testing
- **Lock screen shake**: Must be tested on physical device
- **Volume button**: Must be tested on physical device
- **Screen wake**: Must be tested with screen locked

## Sources

**Architecture & Best Practices:**
- [Guide to app architecture | Android Developers](https://developer.android.com/topic/architecture) (HIGH confidence, official docs, updated 2026-01-26)
- [Better Android Apps Using MVVM with Clean Architecture | Toptal](https://www.toptal.com/android/android-apps-mvvm-with-clean-architecture) (MEDIUM confidence, authoritative source)
- [Modern Android App Architecture with Clean Code Principles (2025 Edition) | Medium](https://medium.com/design-bootcamp/modern-android-app-architecture-with-clean-code-principles-2025-edition-95f4c2afeadb) (MEDIUM confidence, recent article)

**Foreground Services & Sensors:**
- [Foreground service types | Android Developers](https://developer.android.com/develop/background-work/services/fgs/service-types) (HIGH confidence, official docs)
- [Motion sensors | Android Developers](https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion) (HIGH confidence, official docs, updated 2026-01-26)
- [Set a wake lock | Android Developers](https://developer.android.com/develop/background-work/background-tasks/awake/wakelock/set) (HIGH confidence, official docs)

**Quick Settings Tiles & Widgets:**
- [Create custom Quick Settings tiles | Android Developers](https://developer.android.com/develop/ui/views/quicksettings-tiles) (HIGH confidence, official docs)
- [App widgets overview | Android Developers](https://developer.android.com/develop/ui/views/appwidgets/overview) (HIGH confidence, official docs, updated 2026-01-26)
- [Modern Android Widgets Using Glance | Medium](https://syedovaiss.medium.com/modern-android-widgets-using-glance-a-jetpack-compose-approach-clean-architecture-2fb72d6e319c) (MEDIUM confidence, modern approach)

**Jetpack Compose & State Management:**
- [Side-effects in Compose | Android Developers](https://developer.android.com/develop/ui/compose/side-effects) (HIGH confidence, official docs, updated 2026-01-30)
- [Navigation with Compose | Android Developers](https://developer.android.com/develop/ui/compose/navigation) (HIGH confidence, official docs, updated 2026-01-30)
- [State and Jetpack Compose | Android Developers](https://developer.android.com/develop/ui/compose/state) (HIGH confidence, official docs)

**State Machines & Gestures:**
- [Finite State Machines + Android + Kotlin | Thoughtbot](https://thoughtbot.com/blog/finite-state-machines-android-kotlin-good-times) (MEDIUM confidence, industry blog)
- [Understanding the State Machine Pattern | Medium](https://medium.com/kotlin-android-chronicle/understanding-the-state-machine-pattern-and-how-to-use-it-in-android-development-64e4d9fe3397) (MEDIUM confidence)
- [Detect common gestures | Android Developers](https://developer.android.com/develop/ui/views/touch-and-input/gestures/detector) (HIGH confidence, official docs)

**Component Communication:**
- [Different ways Activities communicating with Services | xizzhu](https://xizzhu.me/post/2020-05-18-android-activity-service-communication/) (MEDIUM confidence, technical blog)
- [Mastering Global State Management in Jetpack Compose | droidcon](https://www.droidcon.com/2025/01/23/mastering-global-state-management-in-android-with-jetpack-compose/) (MEDIUM confidence, recent article from 2025-01-23)

**Volume Button & Lock Screen:**
- [How to listen volume buttons in background service | TutorialsPoint](https://www.tutorialspoint.com/how-to-listen-volume-buttons-in-android-background-service) (LOW confidence, educational resource)
- [Restrictions on starting activities from background | Android Developers](https://developer.android.com/guide/components/activities/background-starts) (HIGH confidence, official docs, updated 2026-01-26)
