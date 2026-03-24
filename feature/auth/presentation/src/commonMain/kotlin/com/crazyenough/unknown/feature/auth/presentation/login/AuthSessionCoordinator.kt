package com.crazyenough.unknown.feature.auth.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.domain.error.AuthError
import com.crazyenough.unknown.feature.auth.domain.model.AuthProvider
import com.crazyenough.unknown.feature.auth.domain.model.AuthSession
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential
import com.crazyenough.unknown.feature.auth.domain.usecase.ClearAuthSessionUseCase
import com.crazyenough.unknown.feature.auth.domain.usecase.ExchangeSocialTokenUseCase
import org.koin.compose.koinInject

class AuthSessionCoordinator(
    private val authProviderLauncher: AuthProviderLauncher,
    private val exchangeSocialTokenUseCase: ExchangeSocialTokenUseCase,
    private val clearAuthSessionUseCase: ClearAuthSessionUseCase,
) {
    suspend fun tryRestoreSession(): Result<AuthSession?, AuthError> {
        val restorableCredentials = try {
            authProviderLauncher.restoreCredentials()
                .sortedBy { it.restorePriority() }
        } catch (_: Throwable) {
            return Result.Success(null)
        }

        if (restorableCredentials.isEmpty()) {
            return Result.Success(null)
        }

        var lastError: AuthError? = null
        for (credential in restorableCredentials) {
            val result = try {
                exchangeSocialTokenUseCase(credential)
            } catch (_: Throwable) {
                lastError = AuthError.Unknown
                continue
            }

            when (result) {
                is Result.Failure -> lastError = result.error
                is Result.Success -> return Result.Success(result.data)
            }
        }

        return Result.Failure(lastError ?: AuthError.Unknown)
    }

    suspend fun signOut(provider: AuthProvider): Result<Unit, AuthError> {
        val result = authProviderLauncher.signOut(provider)
        clearAuthSessionUseCase()
        return result
    }

    suspend fun unlink(provider: AuthProvider): Result<Unit, AuthError> {
        return when (val result = authProviderLauncher.unlink(provider)) {
            is Result.Failure -> result
            is Result.Success -> {
                clearAuthSessionUseCase()
                Result.Success(Unit)
            }
        }
    }
}

@Composable
fun rememberAuthSessionCoordinator(
    authProviderLauncher: AuthProviderLauncher = rememberAuthProviderLauncher(),
    exchangeSocialTokenUseCase: ExchangeSocialTokenUseCase = koinInject(),
    clearAuthSessionUseCase: ClearAuthSessionUseCase = koinInject(),
): AuthSessionCoordinator {
    return remember(
        authProviderLauncher,
        exchangeSocialTokenUseCase,
        clearAuthSessionUseCase,
    ) {
        AuthSessionCoordinator(
            authProviderLauncher = authProviderLauncher,
            exchangeSocialTokenUseCase = exchangeSocialTokenUseCase,
            clearAuthSessionUseCase = clearAuthSessionUseCase,
        )
    }
}

private fun SocialAuthCredential.restorePriority(): Int {
    return when (provider) {
        AuthProvider.GOOGLE -> 0
        AuthProvider.APPLE -> 1
        AuthProvider.KAKAO -> 2
    }
}
