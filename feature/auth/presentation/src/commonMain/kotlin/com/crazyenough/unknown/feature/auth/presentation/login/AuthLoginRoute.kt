package com.crazyenough.unknown.feature.auth.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.core.presentation.util.ObserveAsEvents
import com.crazyenough.unknown.feature.auth.domain.model.AuthProvider
import kotlinx.coroutines.launch

@Composable
fun AuthLoginRoute(
    viewModel: AuthLoginViewModel,
    onLoginSuccess: () -> Unit = {},
    socialLoginLauncher: AuthProviderLauncher = rememberAuthProviderLauncher(),
    authSessionCoordinator: AuthSessionCoordinator = rememberAuthSessionCoordinator(socialLoginLauncher),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var hasAttemptedAutoLogin by remember { mutableStateOf(false) }

    LaunchedEffect(
        socialLoginLauncher.supportedProviders,
        socialLoginLauncher.enabledProviders,
    ) {
        viewModel.onAction(
            AuthLoginAction.OnProviderAvailabilityChanged(
                supportedProviders = socialLoginLauncher.supportedProviders,
                enabledProviders = socialLoginLauncher.enabledProviders,
            ),
        )
        viewModel.onAction(
            socialLoginLauncher.readDevLocalAuthStatus().toAction(),
        )
        if (!hasAttemptedAutoLogin) {
            hasAttemptedAutoLogin = true
            attemptAutoLogin(
                authSessionCoordinator = authSessionCoordinator,
                socialLoginLauncher = socialLoginLauncher,
                onAction = viewModel::onAction,
                onLoginSuccess = onLoginSuccess,
            )
        }
    }

    ObserveAsEvents(flow = viewModel.eventFlow) { event ->
        when (event) {
            AuthLoginEvent.LaunchGoogleLogin -> {
                coroutineScope.launch {
                    handleLaunchResult(
                        provider = AuthProvider.GOOGLE,
                        socialLoginLauncher = socialLoginLauncher,
                        onAction = viewModel::onAction,
                    )
                }
            }

            AuthLoginEvent.LaunchKakaoLogin -> {
                coroutineScope.launch {
                    handleLaunchResult(
                        provider = AuthProvider.KAKAO,
                        socialLoginLauncher = socialLoginLauncher,
                        onAction = viewModel::onAction,
                    )
                }
            }

            AuthLoginEvent.LaunchAppleLogin -> {
                coroutineScope.launch {
                    handleLaunchResult(
                        provider = AuthProvider.APPLE,
                        socialLoginLauncher = socialLoginLauncher,
                        onAction = viewModel::onAction,
                    )
                }
            }

            AuthLoginEvent.NavigateToAppHome -> onLoginSuccess()

            is AuthLoginEvent.LaunchSignOut -> {
                coroutineScope.launch {
                    when (val result = authSessionCoordinator.signOut(event.provider)) {
                        is Result.Failure -> {
                            viewModel.onAction(AuthLoginAction.OnSignOutFailure(result.error))
                        }

                        is Result.Success -> Unit
                    }
                    viewModel.onAction(socialLoginLauncher.readDevLocalAuthStatus().toAction())
                }
            }

            is AuthLoginEvent.LaunchUnlink -> {
                coroutineScope.launch {
                    when (val result = authSessionCoordinator.unlink(event.provider)) {
                        is Result.Failure -> {
                            viewModel.onAction(AuthLoginAction.OnUnlinkFailure(result.error))
                        }

                        is Result.Success -> Unit
                    }
                    viewModel.onAction(socialLoginLauncher.readDevLocalAuthStatus().toAction())
                }
            }
        }
    }

    AuthLoginScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

private suspend fun handleLaunchResult(
    provider: AuthProvider,
    socialLoginLauncher: AuthProviderLauncher,
    onAction: (AuthLoginAction) -> Unit,
) {
    when (val result = socialLoginLauncher.launch(provider)) {
        is AuthProviderLaunchResult.Failure -> {
            onAction(AuthLoginAction.OnProviderLoginFailure(result.error))
        }

        is AuthProviderLaunchResult.Success -> {
            onAction(AuthLoginAction.OnProviderLoginResult(result.credential))
        }
    }
    onAction(socialLoginLauncher.readDevLocalAuthStatus().toAction())
}

private suspend fun attemptAutoLogin(
    authSessionCoordinator: AuthSessionCoordinator,
    socialLoginLauncher: AuthProviderLauncher,
    onAction: (AuthLoginAction) -> Unit,
    onLoginSuccess: () -> Unit,
) {
    onAction(AuthLoginAction.OnAutoLoginStarted)
    when (val result = authSessionCoordinator.tryRestoreSession()) {
        is Result.Failure -> {
            onAction(AuthLoginAction.OnProviderLoginFailure(result.error))
            onAction(AuthLoginAction.OnAutoLoginFinished)
        }

        is Result.Success -> {
            onAction(AuthLoginAction.OnAutoLoginFinished)
            if (result.data != null) {
                onLoginSuccess()
            }
        }
    }
    onAction(socialLoginLauncher.readDevLocalAuthStatus().toAction())
}

private fun DevLocalAuthStatus.toAction(): AuthLoginAction {
    return AuthLoginAction.OnDevLocalAuthStatusChanged(
        hasFirebaseCurrentUser = hasFirebaseCurrentUser,
        hasKakaoToken = hasKakaoToken,
    )
}
