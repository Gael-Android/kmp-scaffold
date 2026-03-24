# Project Structure

## Multi-Module Layout

```
project/
├── build-logic/          # Convention Plugins (Submodule)
├── composeApp/           # CMP main UI
├── androidApp/           # Android entry point
├── iosApp/               # iOS entry point
├── core/
│   ├── domain/          # Business logic
│   ├── data/            # Data sources
│   ├── navigation/      # Shared Navigation3 contracts/reducer
│   ├── presentation/    # Shared ViewModel utilities
│   └── designsystem/    # Shared UI components
└── feature/
    └── [feature-name]/
        ├── domain/      # Feature business logic
        ├── data/        # Feature data
        └── presentation/  # Feature UI (MVI 4-file structure)
```

## Feature Internal Structure (MVI Pattern)

Four files per screen:
- `*Contract.kt` - State, Action, Event
- `*ViewModel.kt` - Business logic
- `*Route.kt` - Navigation and event handling
- `*Screen.kt` - UI (Stateless)

## Module Creation Principles

1. When adding a feature: follow the order domain -> data -> presentation
2. Automate build configuration with Convention Plugins
3. Features automatically depend on core modules
4. Put shared navigation contracts in `core/navigation`; keep feature route entries in each feature `presentation`
