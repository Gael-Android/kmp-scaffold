# kmp-scaffold

Kotlin Multiplatform + Compose Multiplatform scaffold for Android and iOS.

This template is meant to be copied into a new product and renamed from the placeholder app identity:

- project name: `Unknown`
- package prefix: `com.crazyenough.unknown`

It already includes:

- KMP + CMP multi-module structure
- Android + iOS entrypoints
- Navigation3-based shared app navigation
- Koin DI setup
- Ktor networking baseline
- `auth` feature as a concrete reference implementation
- `example` feature as a minimal 4-file MVI scaffold

## Structure

```text
composeApp/      Shared CMP app module
androidApp/      Android entrypoint
iosApp/          iOS Xcode project
build-logic/     Convention plugins as Git submodule

core/
  domain/
  data/
  designsystem/
  navigation/
  presentation/
  resources/

feature/
  auth/{domain,data,presentation}
  example/{domain,data,presentation}
```

## Quick Start

1. Clone the repository.
2. Initialize the submodule.
3. Replace `unknown` naming and placeholder configs.
4. Build Android and shared modules.
5. Open `iosApp/` in Xcode when you are ready to wire iOS credentials.

```bash
git submodule update --init --recursive
./gradlew :androidApp:assembleDebug
./gradlew :composeApp:assemble
./gradlew :feature:auth:domain:allTests
```

## What To Rename First

Search and replace these values before starting real feature work:

- `Unknown`
- `unknown`
- `com.crazyenough.unknown`

Primary files to review:

- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `androidApp/google-services.json`
- `iosApp/Configuration/Config.xcconfig`
- `iosApp/iosApp/GoogleService-Info.plist`
- `iosApp/iosApp.xcodeproj/project.pbxproj`

## Firebase And OAuth Placeholders

This scaffold keeps auth wiring in place, but all app-specific credentials are placeholders.

Replace these before shipping:

- `androidApp/google-services.json`
- `iosApp/iosApp/GoogleService-Info.plist`
- `iosApp/Configuration/Config.xcconfig`

Notes:

- `google-services` stays enabled for Android, so the placeholder `google-services.json` is intentionally buildable.
- Kakao repository is kept in `settings.gradle.kts` because the `auth` reference feature still depends on Kakao SDK.
- iOS auth/Firebase files are placeholders only; real login flows require real console values.

## Build Commands

```bash
# Android app
./gradlew :androidApp:assembleDebug

# Shared compose app module and iOS frameworks
./gradlew :composeApp:assemble

# Full verification
./gradlew check

# Example module tests
./gradlew :feature:auth:domain:allTests
```

## Feature Template

Use `feature/example` as the starting point for new features.

Presentation follows the repo's 4-file MVI structure:

1. `*Contract.kt`
2. `*ViewModel.kt`
3. `*Route.kt`
4. `*Screen.kt`

Related references:

- `feature/example/presentation`
- `feature/auth/presentation`
- `core/navigation`
- `core/designsystem`

## iOS

Open [iosApp](/Users/kwakkun/AndroidStudioProjects/kmp-scaffold/iosApp) in Xcode for iOS runs.

If you change the package/app identity, also update:

- bundle identifier
- display name
- URL schemes
- Firebase/OAuth client values

## Current Status

The scaffold was verified with:

- `./gradlew :androidApp:assembleDebug`
- `./gradlew :composeApp:assemble`
- `./gradlew :feature:auth:domain:allTests`

Known warning:

- Kotlin/Native framework linking may warn that bundle ID inference falls back to `ComposeApp`. If you want a fixed iOS framework bundle ID, set it explicitly in the native binary configuration.
