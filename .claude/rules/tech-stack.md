# Tech Stack

## Project-Specific Choices

### UI and Platform
- **Compose Multiplatform (CMP)** - maximize shared code
- Follow Material Design 3

### Architecture
- **Clean Architecture** (Presentation / Domain / Data)
- **MVI Pattern** (4-file structure required)
- **Navigation3** (state-driven BackStack architecture)

### Networking
- **Ktor Client** + kotlinx.serialization

### Dependency Injection
- **Hilt** (Android)
- **Koin** (shared KMP)

### Async
- Prefer **Kotlin Flow**
- StateFlow/SharedFlow for state
