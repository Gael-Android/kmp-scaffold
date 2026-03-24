package com.crazyenough.unknown.feature.auth.domain.usecase

import com.crazyenough.unknown.feature.auth.domain.repository.AuthRepository

class ClearAuthSessionUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() {
        authRepository.clearSession()
    }
}
