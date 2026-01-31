---
phase: 01-project-setup-ci-cd
plan: 01
subsystem: infra
tags: [gradle, android, jacoco, detekt, kotlin]

# Dependency graph
requires: []
provides:
  - Android project scaffold with Gradle 8.12.1 wrapper
  - JaCoCo coverage reporting with XML/HTML output
  - Detekt static analysis with SARIF support
  - Debug APK build capability
affects: [01-02, 02-core-light-engine, phase-1-ci-cd]

# Tech tracking
tech-stack:
  added:
    - Gradle 8.12.1
    - Android Gradle Plugin 8.8.1
    - Kotlin 2.0.21
    - JaCoCo (built-in)
    - Detekt 1.23.8
    - AndroidX Core KTX 1.15.0
    - AndroidX Activity KTX 1.9.3
    - AndroidX Lifecycle Runtime KTX 2.8.7
  patterns:
    - Version catalog (libs.versions.toml) for dependency management
    - Plugin aliases in root build.gradle.kts
    - JaCoCo coverage integrated with AGP debug build type

key-files:
  created:
    - build.gradle.kts
    - settings.gradle.kts
    - gradle.properties
    - gradle/libs.versions.toml
    - gradlew
    - gradlew.bat
    - gradle/wrapper/gradle-wrapper.jar
    - gradle/wrapper/gradle-wrapper.properties
    - app/build.gradle.kts
    - app/src/main/AndroidManifest.xml
    - app/src/main/java/com/anshul/screenlight/MainActivity.kt
    - app/src/test/java/com/anshul/screenlight/ExampleUnitTest.kt
    - config/detekt/detekt.yml
    - .gitignore
  modified: []

key-decisions:
  - "JaCoCo execution data path uses AGP 8.x location (outputs/unit_test_code_coverage/debugUnitTest/)"
  - "Detekt configured with buildUponDefaultConfig=true for minimal override config"
  - "Using activity-ktx instead of activity-compose for minimal placeholder (Compose in Phase 2)"

patterns-established:
  - "Version catalog: All versions in gradle/libs.versions.toml, referenced via libs.* in build files"
  - "Plugin aliases: Declared in version catalog, applied via alias() in build.gradle.kts"
  - "Coverage: enableUnitTestCoverage=true in debug buildType, custom jacocoTestReport task"

# Metrics
duration: 11min
completed: 2026-01-31
---

# Phase 1 Plan 1: Android Project Scaffold Summary

**Gradle 8.12.1 Android project with JaCoCo coverage and Detekt linting, all tasks pass**

## Performance

- **Duration:** 11 min
- **Started:** 2026-01-31T02:34:42Z
- **Completed:** 2026-01-31T02:46:06Z
- **Tasks:** 2
- **Files modified:** 18

## Accomplishments

- Created complete Android project scaffold with Gradle 8.12.1 wrapper
- Configured JaCoCo for unit test coverage with XML/HTML reports
- Set up Detekt static analysis with strict maxIssues: 0 policy
- All verification tasks pass: testDebugUnitTest, jacocoTestReport, detekt, assembleDebug
- Debug APK builds successfully at app/build/outputs/apk/debug/app-debug.apk

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Android project scaffold with Gradle wrapper** - `90af062` (feat)
2. **Task 2: Configure JaCoCo coverage and Detekt linting** - `a2a0317` (feat)

## Files Created/Modified

- `build.gradle.kts` - Root build file with plugin aliases
- `settings.gradle.kts` - Project settings with pluginManagement and dependencyResolutionManagement
- `gradle.properties` - Gradle JVM args and Android settings
- `gradle/libs.versions.toml` - Version catalog with all dependencies
- `gradlew` + `gradlew.bat` - Gradle wrapper scripts
- `gradle/wrapper/gradle-wrapper.jar` - Gradle wrapper JAR
- `gradle/wrapper/gradle-wrapper.properties` - Gradle 8.12.1 distribution
- `app/build.gradle.kts` - App module with Android, JaCoCo, and Detekt configuration
- `app/src/main/AndroidManifest.xml` - Android manifest with MainActivity
- `app/src/main/java/com/anshul/screenlight/MainActivity.kt` - Placeholder activity
- `app/src/test/java/com/anshul/screenlight/ExampleUnitTest.kt` - Sample unit test
- `config/detekt/detekt.yml` - Detekt config with maxIssues: 0
- `.gitignore` - Ignores local.properties

## Decisions Made

1. **JaCoCo execution data path:** AGP 8.x stores coverage at `outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec`, not `jacoco/testDebugUnitTest.exec` as in older versions
2. **activity-ktx vs activity-compose:** Used activity-ktx for minimal placeholder MainActivity; Compose dependencies will be added in Phase 2
3. **Detekt buildUponDefaultConfig:** Set to true so we only override specific rules (MaxLineLength: 120) rather than managing full config

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added activity-ktx dependency**
- **Found during:** Task 2 (verification)
- **Issue:** MainActivity uses ComponentActivity which requires activity-ktx library not in original dependency list
- **Fix:** Added activity-ktx 1.9.3 to version catalog and app dependencies
- **Files modified:** gradle/libs.versions.toml, app/build.gradle.kts
- **Verification:** Build compiles successfully
- **Committed in:** a2a0317 (Task 2 commit)

**2. [Rule 1 - Bug] Fixed JaCoCo executionData path**
- **Found during:** Task 2 (verification)
- **Issue:** JaCoCo report was SKIPPED because exec file path didn't match AGP 8.x location
- **Fix:** Updated path from `jacoco/testDebugUnitTest.exec` to `outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec`
- **Files modified:** app/build.gradle.kts
- **Verification:** jacocoTestReport task generates XML and HTML reports
- **Committed in:** a2a0317 (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 bug)
**Impact on plan:** Both auto-fixes necessary for project to compile and generate coverage. No scope creep.

## Issues Encountered

- **Java not installed:** OpenJDK 17 was installed via Homebrew (`brew install openjdk@17`) to run Gradle wrapper
- **Android SDK path:** Created local.properties pointing to Homebrew SDK at `/opt/homebrew/share/android-commandlinetools`

## User Setup Required

None - no external service configuration required. The CI environment (GitHub Actions) will use `actions/setup-java@v4` and the SDK is available in ubuntu-latest runners.

## Next Phase Readiness

- Project scaffold complete, ready for CI/CD workflow creation (Plan 01-02)
- All four verification commands work: `./gradlew testDebugUnitTest jacocoTestReport detekt assembleDebug`
- Coverage threshold enforcement and branch protection will be configured in Plan 01-02

---
*Phase: 01-project-setup-ci-cd*
*Completed: 2026-01-31*
