package com.crazyenough.unknown.feature.auth.presentation.login

import androidx.compose.runtime.Immutable
import com.crazyenough.unknown.feature.auth.domain.error.AuthError
import com.crazyenough.unknown.feature.auth.domain.model.AuthProvider
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential

@Immutable
data class AuthLoginState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loggedInAccount: LoggedInAccount? = null,
    val devFirebaseCurrentUserExists: Boolean? = null,
    val devKakaoTokenExists: Boolean? = null,
    val supportedProviders: Set<AuthProvider> = setOf(
        AuthProvider.GOOGLE,
        AuthProvider.KAKAO,
    ),
    val enabledProviders: Set<AuthProvider> = setOf(
        AuthProvider.GOOGLE,
        AuthProvider.KAKAO,
    ),
) {
    val isGoogleVisible: Boolean
        get() = supportedProviders.contains(AuthProvider.GOOGLE)

    val isGoogleEnabled: Boolean
        get() = !isLoading && enabledProviders.contains(AuthProvider.GOOGLE)

    val isKakaoVisible: Boolean
        get() = supportedProviders.contains(AuthProvider.KAKAO)

    val isKakaoEnabled: Boolean
        get() = !isLoading && enabledProviders.contains(AuthProvider.KAKAO)

    val isAppleVisible: Boolean
        get() = supportedProviders.contains(AuthProvider.APPLE)

    val isAppleEnabled: Boolean
        get() = !isLoading && enabledProviders.contains(AuthProvider.APPLE)

    @Immutable
    data class LoggedInAccount(
        val provider: AuthProvider,
        val displayName: String,
        val userId: String,
    )
}

sealed interface AuthLoginAction {
    data class OnProviderAvailabilityChanged(
        val supportedProviders: Set<AuthProvider>,
        val enabledProviders: Set<AuthProvider>,
    ) : AuthLoginAction

    data class OnDevLocalAuthStatusChanged(
        val hasFirebaseCurrentUser: Boolean,
        val hasKakaoToken: Boolean,
    ) : AuthLoginAction

    data object OnAutoLoginStarted : AuthLoginAction

    data object OnAutoLoginFinished : AuthLoginAction

    data object OnGoogleClick : AuthLoginAction

    data object OnKakaoClick : AuthLoginAction

    data object OnAppleClick : AuthLoginAction

    data class OnProviderLoginResult(
        val credential: SocialAuthCredential,
    ) : AuthLoginAction

    data class OnProviderLoginFailure(
        val error: AuthError,
    ) : AuthLoginAction

    data class OnSignOutFailure(
        val error: AuthError,
    ) : AuthLoginAction

    data class OnUnlinkFailure(
        val error: AuthError,
    ) : AuthLoginAction

    data object OnDismissError : AuthLoginAction

    data object OnDevGoogleSignOut : AuthLoginAction

    data object OnDevKakaoSignOut : AuthLoginAction

    data object OnDevAppleSignOut : AuthLoginAction

    data object OnDevGoogleUnlink : AuthLoginAction

    data object OnDevKakaoUnlink : AuthLoginAction

    data object OnDevAppleUnlink : AuthLoginAction
}

sealed interface AuthLoginEvent {
    data object LaunchGoogleLogin : AuthLoginEvent

    data object LaunchKakaoLogin : AuthLoginEvent

    data object LaunchAppleLogin : AuthLoginEvent

    data object NavigateToAppHome : AuthLoginEvent

    data class LaunchSignOut(val provider: AuthProvider) : AuthLoginEvent

    data class LaunchUnlink(val provider: AuthProvider) : AuthLoginEvent
}
