# Technology Stack

**Project:** Screenlight
**Researched:** 2026-01-31
**Confidence:** HIGH

## Recommended Stack

### Core Framework
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Kotlin | 2.3.0 | Primary language | Latest stable with Android support. Coroutines, Flow, and modern language features essential for sensor/gesture handling |
| Jetpack Compose | 1.10.2 | UI framework | Standard for new Android apps in 2025. 60% of top 1000 apps use it. Declarative UI perfect for dynamic color/brightness controls |
| Material 3 | 1.4.0 | Design system | Material You with dynamic theming. Essential for modern Android UX. Latest stable with expressive components |
| Android SDK | Min 26, Target 35 | Platform | Min: API 26 (Android 8) covers 95%+ devices. Target: API 35 required for Play Store as of Aug 2025 |

### Dependency Injection
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Hilt | 2.56 | Dependency injection | Android-optimized DI built on Dagger. Now uses KSP (2x faster than KAPT). Standard for Android DI in 2025 |

### Asynchronous & State Management
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| kotlinx-coroutines-android | 1.10.2 | Async operations | Standard for async in Kotlin. Essential for sensor listeners, gesture detection without blocking UI |
| kotlinx-coroutines-core | 1.10.2 | Flow/coroutines | Flow for reactive sensor data streams. Sequential by default, perfect for gesture state machines |
| Lifecycle ViewModel | 2.10.0 | State management | Lifecycle-aware state for Compose. Survives config changes. Essential for gesture state persistence |

### Android Platform APIs
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| CameraManager API | Platform | LED flashlight control | `setTorchMode()` for LED toggle. `turnOnTorchWithStrengthLevel()` for brightness (API 33+). No third-party lib needed |
| SensorManager API | Platform | Accelerometer/gyroscope | Direct access to motion sensors for tilt/shake detection. `onSensorChanged()` with custom gesture algorithms |
| MediaSessionCompat | AndroidX | Volume button interception | Standard approach for capturing volume buttons. Uses `VolumeProviderCompat` with `onAdjustVolume()` callback |
| TileService API | Platform | Quick settings tile | Official API for QS tiles. Lifecycle methods: `onTileAdded()`, `onStartListening()`, `onStopListening()` |
| GlanceAppWidget | Jetpack | Home screen widgets | Modern Compose-based widget framework. Write widgets using Compose syntax vs old RemoteViews |
| WindowManager.LayoutParams | Platform | Screen brightness control | `screenBrightness` float (0.0-1.0) for per-activity brightness. No permissions required. Resets on activity destroy |

### Background Processing
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Foreground Service | Platform | Shake detection | Required for sensor access in background on API 28+. Must declare service type in manifest (API 34+) |
| WorkManager | 2.9.x | Scheduled tasks (if needed) | For any deferred background work. Not needed for shake detection (uses foreground service instead) |

### Testing
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| JUnit | 4.13.2 | Unit test framework | Standard Java testing. Fast local JVM tests for business logic, gesture algorithms |
| MockK | 1.13.x | Mocking library | Kotlin-native mocking. Can mock final classes, objects. Better than Mockito for Kotlin |
| Espresso | 3.6.x | UI testing | Official Android UI testing framework. Real device/emulator tests for gesture interactions |
| Turbine | 1.1.x | Flow testing | Testing library for Kotlin Flow. Essential for testing sensor data streams, gesture state flows |

### Code Quality & CI/CD
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Android Gradle Plugin | 9.0.0 | Build system | Latest stable (Jan 2026). Built-in Kotlin support. No need for separate Kotlin plugin |
| Gradle | 8.13 | Build tool | Compatible with AGP 9.0. Kotlin DSL for build scripts |
| Detekt | 1.23.8 | Static analysis | Kotlin-native linter. Understands coroutines, flows, modern Kotlin. Deeper than formatting |
| ktlint | via Detekt | Code formatting | Kotlin formatting rules. Detekt wraps ktlint as rule set. Ensures consistency |
| GitHub Actions | Cloud | CI/CD platform | Native GitHub integration. Free for public repos. Standard for Android CI/CD in 2025 |
| Gradle Play Publisher | 3.x | Play Store deployment | Automates Play Store uploads. Integrates with GitHub Actions for release automation |

### Supporting Libraries
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Accompanist Permissions | 0.36.x | Runtime permissions | Compose-friendly permission requests. Use for CAMERA permission (flashlight), activity recognition (shake) |
| Timber | 5.x | Logging | Better than Log.d(). Crash-safe, auto-tags. Use for debugging sensor data, gesture detection |
| LeakCanary | 2.x | Memory leak detection | Debug builds only. Critical for sensor listeners (easy to leak if not unregistered) |

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| UI Framework | Jetpack Compose | XML Views | Compose is standard for new apps. 60% adoption in top apps. XML is legacy |
| DI | Hilt | Koin | Hilt is compile-time safe, 2x faster with KSP. Koin is runtime DI (slower, less safe) |
| Mocking | MockK | Mockito | MockK is Kotlin-native, mocks finals/objects. Mockito needs workarounds for Kotlin |
| Brightness Control | WindowManager | Settings API | Settings API changes system brightness (requires WRITE_SETTINGS permission, poor UX). WindowManager is per-activity, no permissions |
| Volume Interception | MediaSessionCompat | KeyEvent.KEYCODE_VOLUME_* | KeyEvent requires accessibility service or root on modern Android. MediaSession is official approach |
| Shake Detection | Custom Sensor Code | Third-party libs | No maintained libraries for shake detection. Custom code with SensorManager is standard (threshold ~2.7G) |
| Widget Framework | Glance | RemoteViews | Glance uses Compose syntax, modern. RemoteViews is legacy XML-based approach |

## What NOT to Use

| Technology | Why Avoid | Use Instead |
|------------|-----------|-------------|
| RxJava | Legacy reactive library. Kotlin Coroutines/Flow are standard. Heavier, Java-oriented | Kotlin Coroutines + Flow |
| Dagger (without Hilt) | Complex setup, boilerplate-heavy. Hilt is Dagger for Android with simpler API | Hilt |
| KAPT | Slow annotation processing. KSP is 2x faster and standard in 2025 | KSP |
| ButterKnife | View binding library. Obsolete with Compose. Even with Views, use ViewBinding | Jetpack Compose / ViewBinding |
| EventBus | Global event bus anti-pattern. Hard to debug, tight coupling | StateFlow / SharedFlow |
| Gson | Slower JSON parsing. No Kotlin support (nullability) | Kotlinx Serialization or Moshi |
| Jitpack third-party sensor libs | Unmaintained libraries with low stars. SensorManager API is straightforward | Direct SensorManager API |

## Installation

### Project-level build.gradle.kts
```kotlin
plugins {
    id("com.android.application") version "9.0.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.0" apply false
    id("com.google.dagger.hilt.android") version "2.56" apply false
    id("com.google.devtools.ksp") version "2.3.0-1.0.29" apply false
}
```

### App-level build.gradle.kts
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

android {
    namespace = "com.screenlight.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.screenlight.app"
        minSdk = 26  // Android 8.0 - covers 95%+ devices
        targetSdk = 35  // Required for Play Store (Aug 2025)
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compose BOM for version management
    val composeBom = platform("androidx.compose:compose-bom:2026.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Jetpack Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.x")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.56")
    ksp("com.google.dagger:hilt-compiler:2.56")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Glance for Widgets
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    // MediaSession for volume buttons
    implementation("androidx.media:media:1.7.0")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Debug
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/config/detekt.yml")
}
```

### AndroidManifest.xml Key Declarations
```xml
<manifest>
    <!-- Permissions -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

    <!-- Features -->
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-feature android:name="android.hardware.camera.flash" android:required="false" />
    <uses-feature android:name="android.hardware.sensor.accelerometer" android:required="true" />

    <application>
        <!-- Main activity that shows over lock screen -->
        <activity
            android:name=".MainActivity"
            android:showWhenLocked="true"
            android:turnScreenOn="true" />

        <!-- Quick Settings Tile -->
        <service
            android:name=".tile.ScreenlightTileService"
            android:exported="true"
            android:icon="@drawable/ic_tile"
            android:label="@string/tile_label"
            android:permission="android.permission.BIND_QUICK_SETTINGS_TILE">
            <intent-filter>
                <action android:name="android.service.quicksettings.action.QS_TILE" />
            </intent-filter>
        </service>

        <!-- Widget Receiver -->
        <receiver
            android:name=".widget.ScreenlightWidgetReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/widget_info" />
        </receiver>

        <!-- Foreground Service for Shake Detection -->
        <service
            android:name=".service.ShakeDetectionService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="shake_gesture_detection" />
        </service>
    </application>
</manifest>
```

## GitHub Actions CI/CD Setup

### .github/workflows/android-ci.yml
```yaml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Run detekt
      run: ./gradlew detekt

    - name: Run unit tests
      run: ./gradlew testDebugUnitTest

    - name: Run instrumentation tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 34
        target: google_apis
        arch: x86_64
        script: ./gradlew connectedDebugAndroidTest

    - name: Build debug APK
      run: ./gradlew assembleDebug

    - name: Upload build artifacts
      uses: actions/upload-artifact@v4
      with:
        name: debug-apk
        path: app/build/outputs/apk/debug/*.apk
```

## Architecture Implications

### Sensor Management Pattern
- **Challenge:** Sensor listeners leak if not unregistered
- **Solution:** Use ViewModel + Lifecycle observer. Register in `onResume()`, unregister in `onPause()`
- **Testing:** LeakCanary in debug builds to catch leaks

### Volume Button Interception Pattern
- **Challenge:** MediaSession only works when app is "playing media"
- **Solution:** Create invisible MediaSession with `setPlaybackToRemote()` + `VolumeProviderCompat`
- **Lifecycle:** Create in foreground service, destroy when gesture detection stops

### Lock Screen Access Pattern
- **Challenge:** Activity must show over keyguard without user unlock
- **Solution:** `android:showWhenLocked="true"` + `android:turnScreenOn="true"` in manifest
- **Security:** No SYSTEM_ALERT_WINDOW needed (blocked on modern Android)

### Background Shake Detection Pattern
- **Challenge:** Sensor access restricted in background (API 28+)
- **Solution:** Foreground service with persistent notification
- **Type:** `foregroundServiceType="specialUse"` (API 34+) with property explaining use case

### Brightness Control Pattern
- **Challenge:** Per-screen vs system brightness
- **Solution:** `WindowManager.LayoutParams.screenBrightness` (per-activity, no permissions)
- **Lifecycle:** Reset automatically on activity destroy

## Confidence Assessment

| Area | Confidence | Source |
|------|------------|--------|
| Kotlin/Compose/Material 3 versions | HIGH | Verified via official Android docs (developer.android.com) |
| Android SDK levels | HIGH | Google Play requirements (support.google.com/googleplay) |
| Hilt version | HIGH | Official Dagger docs + recent 2025 Medium articles |
| Coroutines version | HIGH | Official GitHub releases (github.com/Kotlin/kotlinx.coroutines) |
| Platform APIs | HIGH | Official Android developer documentation |
| Testing libraries | HIGH | Official docs + 2025 best practices articles |
| Detekt version | HIGH | Official GitHub releases (github.com/detekt/detekt) |
| AGP/Gradle versions | HIGH | Official Android Studio release notes |
| CI/CD patterns | MEDIUM | Multiple 2025 tutorials + GitHub examples |

## Sources

### Official Documentation
- [Jetpack Compose Releases](https://developer.android.com/jetpack/androidx/releases/compose)
- [Kotlin Releases](https://kotlinlang.org/docs/releases.html)
- [Android Gradle Plugin 9.0.0](https://developer.android.com/build/releases/agp-9-0-0-release-notes)
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Lifecycle Releases](https://developer.android.com/jetpack/androidx/releases/lifecycle)
- [Target SDK Requirements](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Sensors Overview](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview)
- [CameraManager API](https://developer.android.com/reference/android/hardware/camera2/CameraManager)
- [TileService API](https://developer.android.com/develop/ui/views/quicksettings-tiles)
- [Jetpack Glance](https://developer.android.com/develop/ui/compose/glance)
- [WindowManager.LayoutParams](https://developer.android.com/reference/android/view/WindowManager.LayoutParams)

### GitHub Releases
- [kotlinx-coroutines Releases](https://github.com/Kotlin/kotlinx.coroutines/releases)
- [Detekt Releases](https://github.com/detekt/detekt/releases)

### 2025 Guides & Best Practices
- [Mastering Material 3 in Jetpack Compose — The 2025 Guide](https://medium.com/@hiren6997/mastering-material-3-in-jetpack-compose-the-2025-guide-1c1bd5acc480)
- [Building Animated Home Screen Productivity Widgets with Jetpack Glance in 2025](https://medium.com/@snishanthdeveloper/building-animated-home-screen-productivity-widgets-with-jetpack-glance-in-2025-40261364f1ee)
- [How to Properly Setup Hilt in Android Jetpack Compose Project in 2025](https://dev.to/abdul_rehman_2050/how-to-properly-setup-hilt-in-android-jetpack-compose-project-in-2025-o56)
- [Setup of Hilt with KSP in an Android Project 2025](https://medium.com/@mohit2656422/setup-of-hilt-with-ksp-in-an-android-project-2025-e76e42bb261a)
- [Android Testing Guide: JUnit, MockK, Robolectric, Espresso & WireMock](https://medium.com/@boobalaninfo/a-complete-introduction-to-android-app-testing-junit-mockk-robolectric-espresso-wiremock-2723ca07796b)
- [Choosing the Right Static Code Analysis Tool for Android in 2025](https://medium.com/@cristian.torrado/choosing-the-right-static-code-analysis-tool-for-android-in-2025-aefafaad70ce)
- [How to set up a CI/CD pipeline for your Android app using fastlane and GitHub Actions](https://www.runway.team/blog/ci-cd-pipeline-android-app-fastlane-github-actions)
- [Android 13 Adds Support for Controlling the Brightness of the Flashlight](https://www.esper.io/blog/android-13-flashlight-brightness-control)
- [Torch strength control - Android Open Source Project](https://source.android.com/docs/core/camera/torch-strength-control)
- [Changes to foreground services](https://developer.android.com/develop/background-work/services/fgs/changes)
