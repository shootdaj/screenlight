# Phase 1: Project Setup & CI/CD - Research

**Researched:** 2026-01-31
**Domain:** Android CI/CD with GitHub Actions, Gradle, Kotlin
**Confidence:** HIGH

## Summary

This research covers the setup of a complete CI/CD pipeline for an Android Kotlin project using GitHub Actions. The standard approach leverages official GitHub Actions for Gradle, dedicated actions for code quality (JaCoCo for coverage, Detekt for linting), and GitHub's native branch protection features to enforce quality gates.

The Android CI/CD ecosystem has matured significantly, with well-established patterns for automated testing, coverage enforcement, static analysis, and release automation. The official `gradle/actions/setup-gradle` action (formerly gradle-build-action) is now the recommended way to run Gradle in GitHub Actions, replacing manual caching strategies. JaCoCo remains the de facto standard for Android code coverage, while Detekt is the most widely adopted Kotlin-specific linting tool.

For the Screenlight project requirements, the stack includes: GitHub Actions for workflow automation, Gradle with JaCoCo plugin for test coverage, Detekt for Kotlin linting, GitHub branch protection rules with admin enforcement, and automated release creation triggered by semantic version tags (v*.*.*).

**Primary recommendation:** Use official GitHub Actions (`gradle/actions/setup-gradle`, `actions/checkout@v4`, `actions/setup-java@v4`) with JaCoCo for coverage enforcement and Detekt for linting. Configure branch protection via GitHub UI or API with "Do not allow bypassing the above settings" enabled. Trigger releases on tag push using `on.push.tags: v*.*.*` pattern.

## Standard Stack

The established libraries/tools for Android CI/CD with GitHub Actions:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| gradle/actions/setup-gradle | v4 | Gradle build execution & caching | Official Gradle action, intelligent caching, build visibility |
| actions/setup-java@v4 | v4 | JDK setup | Official GitHub action, supports multiple distributions |
| JaCoCo | 0.8.12 | Code coverage measurement | De facto standard for Java/Kotlin coverage, Gradle built-in support |
| Detekt | 1.23.8 | Kotlin static analysis | Most popular Kotlin linter, official Gradle plugin, SARIF support |
| actions/checkout@v4 | v4 | Repository checkout | Official GitHub action, required for all workflows |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Madrapps/jacoco-report | v1.9.0 | JaCoCo PR comments & checks | Automated coverage reporting in PRs |
| actions/github-script@v7 | v7 | Custom PR failure logic | Enforce coverage thresholds programmatically |
| softprops/action-gh-release | v2 | GitHub release creation | Simplified release creation vs actions/create-release |
| actions/upload-artifact@v4 | v4 | Artifact storage (optional) | Store APKs as build artifacts (separate from releases) |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| JaCoCo | Kover (JetBrains) | Kover is Kotlin-native but less mature, fewer CI integrations |
| Detekt | ktlint | ktlint focuses on formatting only, Detekt does deeper analysis |
| gradle/actions/setup-gradle | Manual Gradle + actions/cache | Official action has better caching intelligence and build insights |
| softprops/action-gh-release | actions/create-release + actions/upload-release-asset | Official actions deprecated, third-party simpler |

**Installation:**
```bash
# No npm packages - all tools are Gradle plugins or GitHub Actions
# Gradle plugins added to build.gradle.kts (see Architecture Patterns)
```

## Architecture Patterns

### Recommended Workflow Structure
```
.github/
├── workflows/
│   ├── ci.yml              # PR checks: tests, coverage, lint
│   ├── release.yml         # Tag-triggered release with APK
│   └── (optional) nightly.yml  # Scheduled comprehensive checks
```

### Pattern 1: PR Quality Gate Workflow
**What:** Single workflow that runs tests, enforces 80% coverage, and runs Detekt on every PR
**When to use:** Every pull request and push to main
**Example:**
```yaml
# Source: https://docs.github.com/actions/using-workflows/workflow-syntax-for-github-actions
# + https://blog.gradle.org/gh-actions
name: CI

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Run tests with coverage
        run: ./gradlew testDebugUnitTest jacocoTestReport

      - name: Generate JaCoCo Badge
        id: jacoco
        uses: Madrapps/jacoco-report@v1.9.0
        with:
          paths: ${{ github.workspace }}/**/build/reports/jacoco/**/jacocoTestReport.xml
          token: ${{ secrets.GITHUB_TOKEN }}
          min-coverage-overall: 80
          min-coverage-changed-files: 80

      - name: Fail if coverage < 80%
        if: ${{ steps.jacoco.outputs.coverage-overall < 80.0 }}
        uses: actions/github-script@v7
        with:
          script: core.setFailed('Overall coverage is less than 80%!')

      - name: Run Detekt
        run: ./gradlew detekt
```

### Pattern 2: Detekt Gradle Configuration
**What:** Configure Detekt plugin to fail builds on violations and generate SARIF for GitHub
**When to use:** In app/build.gradle.kts
**Example:**
```kotlin
// Source: https://github.com/detekt/detekt
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

detekt {
    buildUponDefaultConfig = true // Use default config as baseline
    allRules = false // Don't enable experimental rules
    config.setFrom(files("$rootDir/config/detekt.yml")) // Custom config
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        sarif.required.set(true) // For GitHub code scanning
        xml.required.set(false)
    }
    // Build fails on violations
    jvmTarget = "17"
}
```

### Pattern 3: JaCoCo Gradle Configuration
**What:** Enable JaCoCo coverage reporting for Android tests
**When to use:** In app/build.gradle.kts
**Example:**
```kotlin
// Source: https://docs.gradle.org/current/userguide/jacoco_plugin.html
// + https://medium.com/@saadkhan.cdz/generating-code-coverage-report-with-jacoco-a-complete-guide-for-android-part-3-a7c57561bcda
plugins {
    id("jacoco")
}

android {
    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.apply {
                    extensions.configure<JacocoTaskExtension> {
                        isIncludeNoLocationClasses = true
                        excludes = listOf("jdk.internal.*")
                    }
                }
            }
        }
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    classDirectories.setFrom(files(
        fileTree("build/intermediates/javac/debug") {
            exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*")
        },
        fileTree("build/tmp/kotlin-classes/debug") {
            exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*")
        }
    ))
    executionData.setFrom(fileTree(buildDir) {
        include("jacoco/testDebugUnitTest.exec")
    })
}
```

### Pattern 4: Tag-Triggered Release Workflow
**What:** Automatically create GitHub release with debug APK when pushing v*.*.* tag
**When to use:** For version releases
**Example:**
```yaml
# Source: https://docs.github.com/actions/using-workflows/workflow-syntax-for-github-actions
# + https://github.com/softprops/action-gh-release
name: Release

on:
  push:
    tags:
      - 'v*.*.*'

jobs:
  build-and-release:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build debug APK
        run: ./gradlew assembleDebug

      - name: Create Release
        uses: softprops/action-gh-release@v2
        with:
          files: app/build/outputs/apk/debug/*.apk
          generate_release_notes: true
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### Pattern 5: Branch Protection Configuration
**What:** Configure main branch protection via GitHub API or CLI
**When to use:** Once, during CI/CD setup
**Example:**
```bash
# Source: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/managing-a-branch-protection-rule
# Using GitHub CLI to configure branch protection
gh api repos/{owner}/{repo}/branches/main/protection \
  --method PUT \
  --field required_status_checks='{"strict":true,"contexts":["test"]}' \
  --field enforce_admins=true \
  --field required_pull_request_reviews='{"required_approving_review_count":1}' \
  --field restrictions=null
```

### Anti-Patterns to Avoid
- **Don't use generic `actions/cache`** - Use `gradle/actions/setup-gradle` which has intelligent Gradle-specific caching
- **Don't cache too much** - Avoid caching entire build directories, focus on dependencies
- **Don't skip `chmod +x gradlew`** - GitHub runners don't preserve execute permissions from git
- **Don't use deprecated actions** - `actions/create-release` is deprecated, use `softprops/action-gh-release`
- **Don't put secrets in workflow files** - Always use GitHub Secrets, even for debug keystores
- **Don't use `gradle build`** - Use specific tasks like `assembleDebug` or `testDebugUnitTest` for faster builds

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Gradle caching | Manual cache key calculation | gradle/actions/setup-gradle | Understands Gradle structure, handles wrapper, cache invalidation |
| JaCoCo PR comments | Parse XML, post comment | Madrapps/jacoco-report | Handles diff coverage, threshold checks, comment updates |
| GitHub releases | Manual API calls | softprops/action-gh-release | Handles asset upload, release notes, idempotency |
| Coverage threshold enforcement | Custom bash/python scripts | GitHub Actions with jacoco-report outputs | Reliable parsing, proper exit codes, PR annotations |
| Detekt baseline management | Track violations manually | Detekt baseline feature | Auto-generates baseline file, ignores existing issues |
| APK signing for debug | Custom signing scripts | Gradle signingConfigs | Built-in, testable locally, consistent across CI/local |

**Key insight:** The GitHub Actions ecosystem for Android is mature. Most CI/CD tasks have battle-tested official or community actions. Custom scripts increase maintenance burden and are prone to edge cases these actions already handle (rate limits, retry logic, pagination, authentication).

## Common Pitfalls

### Pitfall 1: JaCoCo Reports 0% Coverage
**What goes wrong:** JaCoCo generates report files but shows 0% coverage, particularly with JDK 17+ and multi-module projects
**Why it happens:**
- Incompatibility between JaCoCo version and JDK version
- Missing `isIncludeNoLocationClasses = true` in test options
- Incorrect class directories path (Kotlin classes vs Java classes)
- Test tasks not running before report generation
**How to avoid:**
- Use JaCoCo 0.8.11+ for JDK 17 compatibility
- Set `isIncludeNoLocationClasses = true` in JacocoTaskExtension
- Include both `build/intermediates/javac/debug` and `build/tmp/kotlin-classes/debug`
- Make jacocoTestReport depend on test task: `dependsOn("testDebugUnitTest")`
**Warning signs:**
- Coverage XML file exists but shows no coverage
- Works with JDK 11 but breaks with JDK 17
- Library modules show coverage but app module doesn't

### Pitfall 2: Branch Protection Doesn't Block Admins
**What goes wrong:** Admin users can merge PRs that fail CI checks, bypassing quality gates
**Why it happens:** Branch protection rules don't apply to admins by default
**How to avoid:**
- Select "Do not allow bypassing the above settings" in branch protection UI
- Set `enforce_admins: true` via GitHub API/CLI
- Verify with a test PR as an admin user
**Warning signs:**
- Admins see "Merge" button on failing PRs
- Status checks show as "Required" but can still merge
- Protection rules show but have admin exception note

### Pitfall 3: Detekt Configuration Overwhelm
**What goes wrong:**
- Using full generated config file becomes hard to maintain on updates
- Setting `failThreshold` in config file interferes with CI
- Reports configured in Gradle extension instead of tasks cause conflicts
**Why it happens:**
- `detekt --generate-config` creates 400+ line file with all rules
- Multiple ways to configure Detekt (extension vs tasks) cause confusion
**How to avoid:**
- Use `buildUponDefaultConfig = true` and only override specific rules
- Don't set `failThreshold` in detekt.yml, let Gradle task handle it
- Configure reports on tasks: `tasks.withType<Detekt>().configureEach { reports { ... } }`
- Generate baseline for legacy code: `./gradlew detektBaseline`
**Warning signs:**
- Config file merge conflicts on Detekt updates
- CI passes locally but fails in GitHub Actions
- Same rule violations reported multiple times

### Pitfall 4: GitHub Actions Cache Limit Exceeded
**What goes wrong:** Workflows fail or become slower over time as cache fills up (10GB per repo limit)
**Why it happens:** Caching entire `.gradle` or `build/` directories saves too much
**How to avoid:**
- Use `gradle/actions/setup-gradle` which caches intelligently
- If manual caching, only cache `~/.gradle/caches` and `~/.gradle/wrapper`
- Never cache `build/` directories
- Set cache cleanup policies (GitHub auto-deletes unused caches after 7 days)
**Warning signs:**
- Workflows slower over time despite caching
- "Cache size exceeded" warnings in logs
- Cache restore takes longer than dependency download

### Pitfall 5: Workflow Doesn't Trigger on Tags
**What goes wrong:** Push tags matching `v*.*.*` but release workflow doesn't run
**Why it happens:**
- Pushing multiple tags simultaneously can cause missed triggers
- Wrong tag filter syntax (using branches filter instead of tags)
- Tags pushed before workflow file exists on main branch
**How to avoid:**
- Use correct syntax: `on.push.tags: - 'v*.*.*'` (not `branches`)
- Push tags individually, not batched
- Ensure workflow file is on main branch before creating tags
- Test with a throwaway tag first
**Warning signs:**
- Workflow shows in Actions tab but not triggered
- Manual workflow trigger works but tag push doesn't
- Single tag pushes work but batch pushes don't

### Pitfall 6: Gradle Wrapper Validation Skipped
**What goes wrong:** Malicious code injected into gradle-wrapper.jar goes undetected
**Why it happens:** Developers add wrapper validation step but don't realize it needs to run before any Gradle execution
**How to avoid:**
- Add `gradle/actions/wrapper-validation@v3` as FIRST step after checkout
- Run before `setup-gradle` or any `./gradlew` commands
**Warning signs:**
- Validation step runs after Gradle tasks
- Validation step missing from workflow entirely
- Using outdated validation action version

### Pitfall 7: APK Path Hardcoded Incorrectly
**What goes wrong:** Release workflow can't find built APK, upload fails
**Why it happens:**
- APK path varies by build variant (debug/release) and flavor
- Using wildcards incorrectly in `files:` parameter
- Build variant name doesn't match assumed path
**How to avoid:**
- Use glob patterns: `app/build/outputs/apk/debug/*.apk`
- Verify path with `ls -la app/build/outputs/apk/` in workflow
- Or use Gradle task to copy APK to known location
**Warning signs:**
- "No files found" error in upload step
- APK builds successfully but release has no assets
- Works locally but fails in CI

### Pitfall 8: YAML Indentation Errors
**What goes wrong:** Workflow syntax error, workflow doesn't run
**Why it happens:** YAML is whitespace-sensitive, mixing tabs/spaces or wrong indentation breaks parsing
**How to avoid:**
- Use 2-space indentation consistently
- Use YAML linter (GitHub editor has built-in validation)
- Copy from official examples, don't hand-write from scratch
**Warning signs:**
- Workflow doesn't appear in Actions tab
- "Invalid workflow file" error on push
- Workflow file shows syntax highlighting errors in GitHub

## Code Examples

Verified patterns from official sources:

### Complete build.gradle.kts (App Module)
```kotlin
// Source: https://github.com/detekt/detekt
// + https://docs.gradle.org/current/userguide/jacoco_plugin.html
// + Android official docs
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("jacoco")
}

android {
    namespace = "com.example.screenlight"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.screenlight"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.apply {
                    extensions.configure<JacocoTaskExtension> {
                        isIncludeNoLocationClasses = true
                        excludes = listOf("jdk.internal.*")
                    }
                }
            }
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        sarif.required.set(true)
        xml.required.set(false)
    }
    jvmTarget = "17"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    classDirectories.setFrom(files(
        fileTree("build/intermediates/javac/debug") {
            exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*")
        },
        fileTree("build/tmp/kotlin-classes/debug") {
            exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*")
        }
    ))
    executionData.setFrom(fileTree(buildDir) {
        include("jacoco/testDebugUnitTest.exec")
    })
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
```

### Minimal detekt.yml
```yaml
# Source: https://detekt.dev/docs/introduction/configurations/
# Only override defaults, don't replicate entire config
build:
  maxIssues: 0  # Fail on any issues

style:
  MaxLineLength:
    maxLineLength: 120  # Android standard
```

### Complete CI Workflow with All Checks
```yaml
# Source: https://docs.github.com/actions/using-workflows/workflow-syntax-for-github-actions
# + https://blog.gradle.org/gh-actions
# + https://github.com/Madrapps/jacoco-report
name: CI

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest

    permissions:
      checks: write
      pull-requests: write
      contents: read

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Validate Gradle wrapper
        uses: gradle/actions/wrapper-validation@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Run tests with coverage
        run: ./gradlew testDebugUnitTest jacocoTestReport

      - name: Generate JaCoCo Badge
        id: jacoco
        uses: Madrapps/jacoco-report@v1.9.0
        with:
          paths: ${{ github.workspace }}/**/build/reports/jacoco/**/jacocoTestReport.xml
          token: ${{ secrets.GITHUB_TOKEN }}
          min-coverage-overall: 80
          min-coverage-changed-files: 80
          title: Code Coverage

      - name: Fail if coverage < 80%
        if: ${{ steps.jacoco.outputs.coverage-overall < 80.0 }}
        uses: actions/github-script@v7
        with:
          script: core.setFailed('Overall coverage is less than 80%!')

      - name: Run Detekt
        run: ./gradlew detekt

      - name: Upload Detekt SARIF
        if: always()
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: build/reports/detekt/detekt.sarif
```

### Branch Protection Setup via GitHub CLI
```bash
# Source: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/managing-a-branch-protection-rule
# Requires: gh CLI installed and authenticated

# Get repository info
OWNER=$(gh repo view --json owner -q .owner.login)
REPO=$(gh repo view --json name -q .name)

# Configure branch protection for main
gh api repos/$OWNER/$REPO/branches/main/protection \
  --method PUT \
  --field required_status_checks="$(cat <<EOF
{
  "strict": true,
  "contexts": ["test"]
}
EOF
)" \
  --field enforce_admins=true \
  --field required_pull_request_reviews="$(cat <<EOF
{
  "required_approving_review_count": 1,
  "dismiss_stale_reviews": true
}
EOF
)" \
  --field restrictions=null \
  --field allow_force_pushes=false \
  --field allow_deletions=false

echo "Branch protection configured for main"
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual `actions/cache` | gradle/actions/setup-gradle | 2023 (Gradle official action) | Better caching, build insights, less config |
| actions/create-release | softprops/action-gh-release | 2021 (official deprecated) | Simpler syntax, better maintenance |
| Hardcoded Java setup | actions/setup-java@v4 | 2024 (v4 release) | Multiple JDK distributions, better caching |
| JaCoCo 0.8.7 | JaCoCo 0.8.11+ | 2023 (JDK 17 support) | Required for JDK 17+ compatibility |
| Detekt via custom scripts | Detekt Gradle plugin | 2020 (plugin maturity) | IDE integration, consistent local/CI |
| Workflow-level permissions | Job-level permissions | 2022 (security best practice) | Principle of least privilege |

**Deprecated/outdated:**
- `actions/create-release`: Deprecated in favor of third-party or manual API calls
- `actions/upload-release-asset`: Use `softprops/action-gh-release` which handles both creation and upload
- Manual Gradle caching with `actions/cache`: Use `gradle/actions/setup-gradle` instead
- `gradle build`: Too broad, use specific tasks like `assembleDebug`, `testDebugUnitTest`
- Branch protection bypass via API tricks: GitHub enforces `enforce_admins` strictly since 2022

## Open Questions

Things that couldn't be fully resolved:

1. **Instrumentation Test Coverage in CI**
   - What we know: Instrumentation tests require Android emulator, CI runners need special setup
   - What's unclear: Performance/cost tradeoff of running instrumentation tests on every PR vs nightly
   - Recommendation: Start with unit tests only (per requirements), add instrumentation tests in later phase if needed. For now, focus on 80% coverage threshold with unit tests only.

2. **Debug Keystore Distribution**
   - What we know: Android auto-generates debug keystore, but CI needs consistent signing across builds
   - What's unclear: Whether to generate custom debug keystore for CI or use default auto-generated one
   - Recommendation: For debug builds only (per requirements), use Gradle's default debug signing. No custom keystore needed unless distributing debug builds to external testers.

3. **Detekt Baseline Strategy**
   - What we know: Baseline files let you suppress existing violations while blocking new ones
   - What's unclear: For a new project (Screenlight has no existing code), whether to create baseline proactively or start strict
   - Recommendation: Start strict (no baseline) since project is new. Baseline is for legacy code migration.

## Sources

### Primary (HIGH confidence)
- [GitHub Actions Workflow Syntax](https://docs.github.com/actions/using-workflows/workflow-syntax-for-github-actions) - Official documentation for workflow configuration
- [GitHub Branch Protection Rules](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/managing-a-branch-protection-rule) - Official documentation for branch protection
- [Gradle with GitHub Actions](https://blog.gradle.org/gh-actions) - Official Gradle blog on setup-gradle action
- [Detekt GitHub Repository](https://github.com/detekt/detekt) - Official Detekt documentation and versions
- [JaCoCo Gradle Plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html) - Official Gradle documentation
- [Madrapps/jacoco-report Action](https://github.com/Madrapps/jacoco-report) - GitHub Actions marketplace, verified coverage enforcement

### Secondary (MEDIUM confidence)
- [Complete Guide: Setting Up CI/CD for Android Apps](https://medium.com/@satyamkrjha85/complete-guide-setting-up-ci-cd-for-android-apps-with-github-actions-firebase-play-store-5b942208473a) - January 2026 comprehensive guide
- [Android CI/CD using GitHub Actions - LogRocket](https://blog.logrocket.com/android-ci-cd-using-github-actions/) - Community best practices
- [JaCoCo Android Complete Guide](https://medium.com/@saadkhan.cdz/generating-code-coverage-report-with-jacoco-a-complete-guide-for-android-part-3-a7c57561bcda) - Detailed JaCoCo setup
- [Android Code Coverage with JaCoCo - Codecov](https://about.codecov.io/blog/code-coverage-for-android-development-using-kotlin-jacoco-github-actions-and-codecov/) - Coverage integration patterns
- [Detekt Configuration](https://detekt.dev/docs/introduction/configurations/) - Official configuration documentation

### Tertiary (LOW confidence - requires validation)
- [GitHub Actions for Android Developers](https://medium.com/google-developer-experts/github-actions-for-android-developers-6b54c8a32f55) - Expert opinions, may be dated
- [Faster Android builds using GitHub Actions](https://aboutfilipe.com/posts/faster-android-builds-using-github-actions/) - Optimization tips, needs testing
- [Android Lint and Detekt in PRs](https://medium.com/bumble-tech/android-lint-and-detekt-warnings-in-github-pull-requests-2880df5d32af) - SARIF integration pattern

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All tools are official or widely adopted (Gradle action is official, JaCoCo is built into Gradle, Detekt has 6k+ stars)
- Architecture: HIGH - Patterns verified from official documentation (GitHub, Gradle, Detekt)
- Pitfalls: MEDIUM - Based on GitHub issue threads and community reports, not always officially documented

**Research date:** 2026-01-31
**Valid until:** 2026-03-31 (60 days - relatively stable ecosystem, but GitHub Actions and Gradle plugins update quarterly)

**Key compatibility notes:**
- Gradle: 8.12.1+
- JDK: 17 (Temurin distribution recommended)
- Android Gradle Plugin: 8.8.1+
- Kotlin: 2.0.21+
- JaCoCo: 0.8.11+ (for JDK 17 compatibility)
- Detekt: 1.23.8
