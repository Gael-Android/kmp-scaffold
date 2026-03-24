# Koin Migration (CMP + Koin Compose)

## Context

- Koin dependencies (`koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-bom`) already existed in the project catalog.
- DI had not been initialized at app startup, and feature screens were creating ViewModels directly via `viewModel { ... }`.
- The phase-1 goal is to standardize on:
  - initializing Koin from app entry
  - registering feature modules
  - resolving ViewModels with `koinViewModel()` in navigation.

## 1) Shared DI module aggregation

- Add a single entry point in `commonMain` that collects feature presentation modules.
- Expose module list as `appKoinModules: List<Module>`.
- Register all feature-level dependencies once via `startKoin { modules(appKoinModules) }`.

```kotlin
// composeApp/src/commonMain/.../KoinInit.kt
val appKoinModules: List<Module> = listOf(
    authPresentationModule,
    examplePresentationModule,
)

expect fun initializeKoin()
```

## 2) Platform-specific initialization actuals

- Split initialization per target with `expect/actual`.
- Guard against duplicate initialization using `GlobalContext.getOrNull()`.

```kotlin
actual fun initializeKoin() {
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            modules(appKoinModules)
        }
    }
}
```

## 3) Call from app entrypoints

- Android: call `initializeKoin()` in `MainActivity.onCreate`.
- iOS: call `initializeKoin()` in the `ComposeUIViewController` entrypoint.

```kotlin
initializeKoin()
setContent {
    App()
}
```

## 4) Feature-scoped ViewModel registration

- Add a `di` package in each feature `presentation` module.
- Register ViewModels with `viewModelOf(::XxxViewModel)`.

```kotlin
val authPresentationModule: Module = module {
    viewModelOf(::AuthLoginViewModel)
}

val examplePresentationModule: Module = module {
    viewModelOf(::ExampleViewModel)
}
```

## 5) Resolve ViewModels through DI in navigation entries

- Replace direct `androidx.lifecycle.viewmodel.compose.viewModel { ... }` creation.
- Use `koinViewModel()` in Navigation3 entries.

```kotlin
AuthLoginRoute(
    viewModel = koinViewModel(),
    onLoginSuccess = { },
)
```

## 6) Gradle dependency setup

- Ensure modules using Koin APIs (currently `composeApp`) include at minimum:
  - `platform(libs.koin.bom)`
  - `libs.koin.core`
  - `libs.koin.compose.viewmodel`

## Verification checklist

- `./gradlew :androidApp:assembleDebug` passes.
- Static verification confirms ViewModels defined in `appKoinModules` are obtained via `koinViewModel()` in feature navigation.
- If a future Hilt migration is planned, define that as a separate phase (current scope is Koin-only).

## Reusable Rule

- Koin should be initialized exactly once, and that responsibility belongs to the platform entrypoint.
- Do not instantiate ViewModels directly inside navigation/route composables with `viewModel { ... }`; always resolve from the DI container.
