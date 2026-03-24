# Repository Guidelines

## Project Overview

Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP) project targeting Android and iOS.

- Package prefix: `com.crazyenough.unknown`
- Current catalog versions: AGP `9.0.0-rc02`, Kotlin `2.3.0`, Compose Multiplatform `1.10.0`
- Build logic: `build-logic/` Git submodule (convention plugins)

## Current Project Structure

```text
composeApp/           # Shared CMP app module (convention-cmp-application)
androidApp/           # Android entrypoint module (convention-android-application-compose)
iosApp/               # iOS Xcode entrypoint project
build-logic/          # Gradle convention plugins (submodule)

core/
  domain/             # Shared domain base types and contracts (convention-kmp-library)
  data/               # Shared data layer (convention-kmp-library)
  designsystem/       # Shared CMP UI components/theme (convention-cmp-library)
  navigation/         # NavigationState/Navigator — multiple back stacks (convention-cmp-library)
  presentation/       # Shared presentation utilities (convention-cmp-library)
  resources/          # Shared strings/resources module (convention-kmp-library)

feature/
  auth/{domain,data,presentation}
  example/{domain,data,presentation}

gradle/libs.versions.toml  # Single source of truth for versions/plugins/settings
```

Source sets follow standard KMP layout:

- `src/commonMain`, `src/commonTest`
- `src/androidMain` (+ Android tests where needed)
- `src/iosMain`

Platform-only APIs must stay out of `commonMain`.

## Build, Test, and Development Commands

```bash
# Build
./gradlew :composeApp:assembleDebug
./gradlew :androidApp:assembleDebug
./gradlew build

# Verification
./gradlew check

# Module tests (examples)
./gradlew :core:domain:allTests
./gradlew :feature:auth:domain:allTests

# Single test class
./gradlew :core:domain:allTests --tests "com.crazyenough.unknown.core.domain.SomeTest"

# Initialize submodules after clone
git submodule update --init --recursive
```

## Convention Plugins (build-logic)

Keep each module `build.gradle.kts` minimal: apply one convention plugin + explicit inter-module dependencies.

| Plugin | Use For | Auto Configuration |
|--------|---------|--------------------|
| `convention-kmp-library` | Core/feature `domain`, `data`, `testing` | KMP + Android KMP library target, serialization plugin, `kotlin-test`, namespace/minSdk/compileSdk from catalog |
| `convention-cmp-library` | UI shared modules (`designsystem`, `navigation`, `presentation`) | `convention-kmp-library` + Compose plugins + Compose UI/foundation/material/resources tooling |
| `convention-cmp-feature` | Feature `presentation` modules | `convention-cmp-library` + Koin + lifecycle/viewmodel/savedstate + navigation dependencies |
| `convention-cmp-application` | `composeApp` | KMP app-style shared module with Android library target + iOS targets + Compose runtime/foundation/material/ui/resources |
| `convention-android-application-compose` | `androidApp` | Android application plugin settings + Compose compiler setup |
| `convention-room` | Modules with Room database | KSP + Room plugin + Room runtime/compiler + bundled SQLite |
| `convention-buildkonfig` | Modules needing build-time constants | BuildKonfig setup; reads `API_KEY` from `local.properties` and fails fast when missing |

Namespace is generated from module path (e.g. `:feature:auth:presentation` -> `com.crazyenough.unknown.feature.auth.presentation`) unless explicitly overridden.

## Dependency Rules (Feature Layering)

- `feature:<name>:domain` -> `core:domain`
- `feature:<name>:data` -> `core:data` + `feature:<name>:domain`
- `feature:<name>:presentation` -> `core:designsystem` + `feature:<name>:domain`

Core module conventions in current codebase:

- `core:data` -> `core:domain`
- `core:presentation` -> `core:domain` + `core:designsystem`
- `core:navigation` contains `NavigationState` + `Navigator` for tab-based multiple back stacks (Navigation3 pattern)

## Architecture (Feature Presentation)

Feature presentation follows a 4-file MVI structure:

1. `*Contract.kt`
   - Defines `State`, `Action`, `Event` with sealed interfaces/data classes.
2. `*ViewModel.kt`
   - Holds `MutableStateFlow` state.
   - Exposes state as `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), ...)`.
   - Sends one-off events with `Channel<Event>(Channel.BUFFERED)` + `receiveAsFlow()`.
   - Update state with `_stateFlow.update { it.copy(...) }`.
3. `*Route.kt`
   - Collects state with `collectAsStateWithLifecycle()`.
   - Collects events in `LaunchedEffect(viewModel)` and maps to navigation callbacks.
   - Passes `state` + `onAction` to screen.
4. `*Screen.kt`
   - Stateless UI function that receives `state` and `onAction`.
   - Includes preview support (`@Preview`, `PreviewParameterProvider`) where applicable.

Data flow is unidirectional: `Action -> ViewModel -> State/Event -> UI/Route`.

## Coding Style

### Naming

| Element | Convention | Example |
|---------|------------|---------|
| Classes / Composables | PascalCase | `ExampleScreen`, `NavigationState` |
| Functions / Properties | camelCase | `onAction`, `stateFlow` |
| Constants | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Packages | lowercase | `com.crazyenough.unknown.feature.example.presentation` |

### Kotlin / Compose

- `kotlin.code.style=official`, 4-space indentation
- Prefer `val` over `var`
- Use constructor injection for concrete dependencies
- Keep imports explicit (no wildcard imports)
- Keep `modifier: Modifier = Modifier` as the last optional parameter in composables

### Error Handling (Current Domain Pattern)

`core/domain` defines generic result typing:

```kotlin
sealed interface Result<out D, out E : Error> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Failure<out E : Error>(val error: E) : Result<Nothing, E>
}
```

Use extension helpers like `map`, `onSuccess`, `onFailure`, and `asEmptyResult()` for flow-friendly transformations.

### Async

- Kotlin Flow/StateFlow for reactive state
- `viewModelScope` for lifecycle-aware coroutine execution
- `Channel` for one-time events

## DI, Networking, Logging

- DI: Koin (`koin-core`, `koin-compose`, `koin-compose-viewmodel`, plus Android bindings in feature modules)
- Networking: Ktor (`core`, `okhttp`, `darwin`, `content-negotiation`, `auth`, `logging`, JSON serialization)
- Logging: Kermit (`co.touchlab:kermit`)

## Testing Guidelines

- Shared tests: `src/commonTest/kotlin` with `kotlin-test`
- Android unit tests: `src/test/kotlin` where module type supports it
- Prefer behavior-oriented test names
- Add tests for non-trivial business logic and reducers

## Git Conventions

Conventional commits:

- `feat`: new feature
- `fix`: bug fix
- `refactor`: structural change without behavior change
- `test`: tests only
- `chore`: build/config/tooling
- `docs`: documentation only
- `style`: formatting only

Keep commits atomic and mention `build-logic` submodule pointer updates in PR descriptions.

## Security

- `local.properties` is gitignored; keep local secrets there
- Never commit API keys or signing artifacts
- If using `convention-buildkonfig`, ensure `API_KEY` exists in `local.properties`

## Practical Notes for This Repository State

- Layered module structure is reduced to `auth` plus a minimal `example` feature scaffold.
- `feature:auth` remains the concrete social-login reference.
- `feature:example:presentation` is the starter MVI 4-file template for new feature work.
- Replace Firebase placeholder files and OAuth values before shipping a real app build.
