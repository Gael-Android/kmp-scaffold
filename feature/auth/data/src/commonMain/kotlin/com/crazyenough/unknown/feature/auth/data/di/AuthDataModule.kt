package com.crazyenough.unknown.feature.auth.data.di

import com.crazyenough.unknown.feature.auth.data.engine.AuthNetworkEngine
import com.crazyenough.unknown.feature.auth.data.engine.provideAuthNetworkEngine
import com.crazyenough.unknown.feature.auth.data.local.AuthSessionLocalDataSource
import com.crazyenough.unknown.feature.auth.data.local.InMemoryAuthSessionLocalDataSource
import com.crazyenough.unknown.feature.auth.data.remote.AuthRemoteDataSource
import com.crazyenough.unknown.feature.auth.data.remote.DefaultAuthRemoteDataSource
import com.crazyenough.unknown.feature.auth.data.repository.DefaultAuthRepository
import com.crazyenough.unknown.feature.auth.domain.repository.AuthRepository
import com.crazyenough.unknown.feature.auth.domain.usecase.ClearAuthSessionUseCase
import com.crazyenough.unknown.feature.auth.domain.usecase.ExchangeSocialTokenUseCase
import com.crazyenough.unknown.feature.auth.domain.usecase.ObserveAuthSessionUseCase
import org.koin.dsl.module

val authDataModule = module {
    single<AuthNetworkEngine> { provideAuthNetworkEngine() }
    single<AuthRemoteDataSource> { DefaultAuthRemoteDataSource(get()) }
    single<AuthSessionLocalDataSource> { InMemoryAuthSessionLocalDataSource() }
    single<AuthRepository> { DefaultAuthRepository(get(), get()) }
    factory { ClearAuthSessionUseCase(get()) }
    factory { ExchangeSocialTokenUseCase(get()) }
    factory { ObserveAuthSessionUseCase(get()) }
}
