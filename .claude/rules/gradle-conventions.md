# Gradle Build Rules

## Use Convention Plugins (Required)

- Repository: https://github.com/Gael-Android/build-logic
- Included in the project as a Git submodule
- Each module's `build.gradle.kts` should only declare plugins and namespace

Convention Plugins automatically configure:
- Dependencies (ViewModel, Navigation, Koin, core modules)
- Compose Multiplatform setup
- Shared build options
