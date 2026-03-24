package com.crazyenough.unknown.feature.auth.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crazyenough.unknown.feature.auth.presentation.login.AuthLoginRoute
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthNavigation(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthLoginRoute(
        viewModel = koinViewModel(),
        onLoginSuccess = onLoginSuccess,
        modifier = modifier,
    )
}
