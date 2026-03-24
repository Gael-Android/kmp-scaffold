# Coding Style

## Naming Conventions

- Class: PascalCase (e.g., `UserRepository`)
- Function/variable: camelCase (e.g., `getUserData`)
- Constant: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`)
- Package: lowercase (e.g., `com.example.feature.user`)

## StateFlow Updates

- When updating `MutableStateFlow`, always use `update { it.copy() }`
- Do not use `_stateFlow.value = _stateFlow.value.copy()` (not thread-safe)

```kotlin
// Good: atomic update via CAS loop
_uiStateFlow.update { it.copy(isLoading = true) }
```

## Code Quality Principles

- Prefer `val` (`var` should be minimized)
- Use constructor injection
- Use interface-based abstractions
- Add KDoc for public APIs
- Explain complex logic with comments
