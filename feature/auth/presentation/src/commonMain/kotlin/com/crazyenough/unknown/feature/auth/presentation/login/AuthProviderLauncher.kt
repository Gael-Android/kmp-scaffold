package com.crazyenough.unknown.feature.auth.presentation.login

import androidx.compose.runtime.Composable
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.domain.error.AuthError
import com.crazyenough.unknown.feature.auth.domain.model.AuthProvider
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential

data class DevLocalAuthStatus(
    val hasFirebaseCurrentUser: Boolean,
    val hasKakaoToken: Boolean,
)

interface AuthProviderLauncher {
    val supportedProviders: Set<AuthProvider>

    val enabledProviders: Set<AuthProvider>

    fun readDevLocalAuthStatus(): DevLocalAuthStatus

    suspend fun restoreCredentials(): List<SocialAuthCredential>

    suspend fun restoreCredential(): SocialAuthCredential? = restoreCredentials().firstOrNull()

    suspend fun launch(
        provider: AuthProvider,
    ): AuthProviderLaunchResult

    suspend fun signOut(provider: AuthProvider): Result<Unit, AuthError>

    suspend fun unlink(provider: AuthProvider): Result<Unit, AuthError>
}

sealed interface AuthProviderLaunchResult {
    data class Success(
        val credential: SocialAuthCredential,
    ) : AuthProviderLaunchResult

    data class Failure(
        val error: AuthError,
    ) : AuthProviderLaunchResult
}

@Composable
expect fun rememberAuthProviderLauncher(): AuthProviderLauncher
