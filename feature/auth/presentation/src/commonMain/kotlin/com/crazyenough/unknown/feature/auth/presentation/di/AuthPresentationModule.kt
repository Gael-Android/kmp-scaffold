package com.crazyenough.unknown.feature.auth.presentation.di

import com.crazyenough.unknown.feature.auth.presentation.login.AuthLoginViewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel

val authPresentationModule: Module = module {
    viewModel {
        AuthLoginViewModel(
            exchangeSocialTokenUseCase = get(),
            observeAuthSessionUseCase = get(),
        )
    }
}
