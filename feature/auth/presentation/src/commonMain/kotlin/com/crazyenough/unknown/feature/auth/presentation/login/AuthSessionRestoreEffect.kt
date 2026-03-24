package com.crazyenough.unknown.feature.auth.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun AuthSessionRestoreEffect(
    socialLoginLauncher: AuthProviderLauncher = rememberAuthProviderLauncher(),
) {
    val authSessionCoordinator = rememberAuthSessionCoordinator(socialLoginLauncher)

    LaunchedEffect(socialLoginLauncher) {
        authSessionCoordinator.tryRestoreSession()
    }
}
