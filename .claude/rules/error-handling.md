# Error Handling and Result Pattern

## Use the Result Pattern (Required)

All error handling in the data/domain layers must use the `Result<D, E>` pattern.

### Core Types

```kotlin
// core/domain/src/commonMain/kotlin/com/crazyenough/unknown/core/domain/util/

// 1. Error marker interface
interface Error

// 2. Result sealed interface
sealed interface Result<out D, out E : Error> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Failure<out E : Error>(val error: E) : Result<Nothing, E>
}

// 3. DataError - network/local error definitions
sealed interface DataError : Error {
    enum class Remote : DataError {
        BAD_REQUEST,
        REQUEST_TIMEOUT,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        CONFLICT,
        TOO_MANY_REQUESTS,
        NO_INTERNET,
        PAYLOAD_TOO_LARGE,
        SERVER_ERROR,
        SERVICE_UNAVAILABLE,
        SERIALIZATION,
        UNKNOWN
    }

    enum class Local : DataError {
        DISK_FULL,
        NOT_FOUND,
        UNKNOWN
    }
}
```

## Result Extension Functions

### map
Use for data transformation:
```kotlin
userRepository.getUser(id)
    .map { user -> user.toUiModel() }
```

### onSuccess / onFailure
Use for side effects:
```kotlin
result
    .onSuccess { data ->
        logger.log("Success: $data")
    }
    .onFailure { error ->
        logger.error("Failed: $error")
    }
```

### asEmptyResult
Convert to Unit type:
```kotlin
userRepository.deleteUser(id)
    .asEmptyResult() // Result<Unit, Error>
```

## Usage Examples

### Repository Layer
```kotlin
interface UserRepository {
    suspend fun getUser(id: String): Result<User, DataError.Remote>
    suspend fun saveUser(user: User): Result<Unit, DataError.Local>
}

class UserRepositoryImpl(
    private val remoteDataSource: UserRemoteDataSource,
    private val localDataSource: UserLocalDataSource
) : UserRepository {
    override suspend fun getUser(id: String): Result<User, DataError.Remote> {
        return try {
            val response = remoteDataSource.fetchUser(id)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Failure(DataError.Remote.UNKNOWN)
        }
    }
}
```

### UseCase Layer
```kotlin
class GetUserUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(id: String): Result<User, DataError.Remote> {
        return repository.getUser(id)
            .map { user ->
                // Apply business logic
                user.copy(name = user.name.uppercase())
            }
    }
}
```

### Use in ViewModel
```kotlin
viewModelScope.launch {
    getUserUseCase(userId)
        .onSuccess { user ->
            _stateFlow.update { it.copy(user = user, isLoading = false) }
        }
        .onFailure { error ->
            _stateFlow.update { it.copy(error = error, isLoading = false) }
            when (error) {
                DataError.Remote.UNAUTHORIZED -> sendEvent(Event.NavigateToLogin)
                DataError.Remote.NO_INTERNET -> sendEvent(Event.ShowNoInternetError)
                else -> sendEvent(Event.ShowGenericError)
            }
        }
}
```

## Principles

1. **Do not throw exceptions**: return `Result.Failure` instead in data/domain layers
2. **Type safety**: explicitly define error types (`DataError.Remote`, `DataError.Local`)
3. **Chainable**: use `map`, `onSuccess`, `onFailure` for functional-style chaining
4. **UI layer separation**: convert `Result` to State in ViewModel before passing to UI

## Custom Error Definitions

When feature-specific domain errors are needed:

```kotlin
// feature/auth/domain/AuthError.kt
sealed interface AuthError : Error {
    data object InvalidCredentials : AuthError
    data object EmailAlreadyExists : AuthError
    data object WeakPassword : AuthError
}

// Usage
suspend fun login(email: String, password: String): Result<User, AuthError>
```
