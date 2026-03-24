package com.crazyenough.unknown

import com.crazyenough.unknown.feature.auth.data.di.authDataModule
import com.crazyenough.unknown.feature.auth.presentation.di.authPresentationModule
import org.koin.core.module.Module

val appKoinModules: List<Module> = listOf(
    authDataModule,
    authPresentationModule,
)

expect fun initializeKoin()
