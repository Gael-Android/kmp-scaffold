package com.crazyenough.unknown.feature.auth.domain.usecase

import com.crazyenough.unknown.feature.auth.domain.repository.AuthRepository

class ObserveAuthSessionUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke() = authRepository.observeSession()
}
