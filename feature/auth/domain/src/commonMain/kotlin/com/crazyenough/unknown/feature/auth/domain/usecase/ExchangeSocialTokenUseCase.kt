package com.crazyenough.unknown.feature.auth.domain.usecase

import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.domain.error.AuthError
import com.crazyenough.unknown.feature.auth.domain.model.AuthSession
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential
import com.crazyenough.unknown.feature.auth.domain.repository.AuthRepository

class ExchangeSocialTokenUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        credential: SocialAuthCredential,
    ): Result<AuthSession, AuthError> {
        if (credential.token.isBlank()) {
            return Result.Failure(AuthError.InvalidCredential)
        }

        return authRepository.exchangeSocialToken(credential)
    }
}
