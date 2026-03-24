package com.crazyenough.unknown.feature.auth.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.domain.error.AuthError
import com.crazyenough.unknown.feature.auth.domain.error.authErrorFromCode
import com.crazyenough.unknown.feature.auth.domain.model.AuthProvider
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential

@Composable
actual fun rememberAuthProviderLauncher(): AuthProviderLauncher {
    return remember {
        IosAuthProviderLauncher()
    }
}

private class IosAuthProviderLauncher : AuthProviderLauncher {

    override val supportedProviders: Set<AuthProvider>
        get() = getIosAuthProviderBridge()
            ?.supportedProviderNames()
            ?.mapNotNull(AuthProvider::fromNameOrNull)
            ?.toSet()
            ?: emptySet()

    override val enabledProviders: Set<AuthProvider>
        get() = getIosAuthProviderBridge()
            ?.enabledProviderNames()
            ?.mapNotNull(AuthProvider::fromNameOrNull)
            ?.toSet()
            ?: emptySet()

    override fun readDevLocalAuthStatus(): DevLocalAuthStatus {
        val bridge = getIosAuthProviderBridge()
        return DevLocalAuthStatus(
            hasFirebaseCurrentUser = bridge?.hasFirebaseCurrentUser() == true,
            hasKakaoToken = bridge?.hasKakaoToken() == true,
        )
    }

    override suspend fun restoreCredentials(): List<SocialAuthCredential> {
        return listOf(
            restoreCredentialSafely(providerName = "Firebase") {
                getIosAuthProviderBridge()
                    ?.restoreFirebaseCredential()
                    ?.toSocialAuthCredentialOrNull()
            },
            restoreCredentialSafely(providerName = "Kakao") {
                getIosAuthProviderBridge()
                    ?.restoreKakaoCredential()
                    ?.toSocialAuthCredentialOrNull()
            },
        ).filterNotNull()
    }

    override suspend fun launch(provider: AuthProvider): AuthProviderLaunchResult {
        val bridge = getIosAuthProviderBridge()
            ?: run {
                Logger.e("AuthLauncher") { "iOS auth bridge is unavailable for $provider" }
                return AuthProviderLaunchResult.Failure(AuthError.Unavailable)
            }
        Logger.i("AuthLauncher") { "iOS launch started provider=$provider" }
        val payload = bridge.launch(provider.name)
        Logger.i("AuthLauncher") {
            "iOS launch finished provider=$provider payloadProvider=${payload.providerName} tokenEmpty=${payload.token.isBlank()} errorCode=${payload.errorCode}"
        }

        if (payload.token.isNotBlank()) {
            val payloadProvider = AuthProvider.fromNameOrNull(payload.providerName) ?: provider
            return AuthProviderLaunchResult.Success(
                SocialAuthCredential(
                    provider = payloadProvider,
                    token = payload.token,
                ),
            )
        }

        Logger.e("AuthLauncher") { "iOS launch failed provider=$provider errorCode=${payload.errorCode}" }
        return AuthProviderLaunchResult.Failure(
            authErrorFromCode(payload.errorCode),
        )
    }

    override suspend fun signOut(provider: AuthProvider): Result<Unit, AuthError> {
        val bridge = getIosAuthProviderBridge()
        if (bridge == null) {
            Logger.w("AuthLauncher") { "iOS signOut bridge is unavailable for $provider" }
            return Result.Failure(AuthError.Unavailable)
        }
        return runCatching {
            bridge.signOut(provider.name)
            Result.Success(Unit)
        }.getOrElse {
            Logger.e("AuthLauncher") { "iOS signOut failed provider=$provider error=${it.message}" }
            Result.Failure(AuthError.Unknown)
        }
    }

    override suspend fun unlink(provider: AuthProvider): Result<Unit, AuthError> {
        val bridge = getIosAuthProviderBridge()
        if (bridge == null) {
            Logger.w("AuthLauncher") { "iOS unlink bridge is unavailable for $provider" }
            return Result.Failure(AuthError.Unavailable)
        }
        return runCatching {
            bridge.unlink(provider.name)
            Result.Success(Unit)
        }.getOrElse {
            Logger.e("AuthLauncher") { "iOS unlink failed provider=$provider error=${it.message}" }
            Result.Failure(AuthError.Unknown)
        }
    }
}

private suspend fun restoreCredentialSafely(
    providerName: String,
    block: suspend () -> SocialAuthCredential?,
): SocialAuthCredential? {
    return try {
        block()
    } catch (error: Throwable) {
        Logger.e("AuthLauncher") { "iOS auto login restore failed provider=$providerName error=${error.message}" }
        null
    }
}

private fun IosAuthProviderLaunchPayload.toSocialAuthCredentialOrNull(): SocialAuthCredential? {
    if (token.isBlank()) return null

    val resolvedProvider = AuthProvider.fromNameOrNull(providerName) ?: return null
    return SocialAuthCredential(
        provider = resolvedProvider,
        token = token,
    )
}
