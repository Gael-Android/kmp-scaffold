package com.crazyenough.unknown.feature.auth.domain.fake

import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.domain.error.AuthError
import com.crazyenough.unknown.feature.auth.domain.model.AuthProvider
import com.crazyenough.unknown.feature.auth.domain.model.AuthSession
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential
import com.crazyenough.unknown.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository : AuthRepository {
    private val sessionFlow = MutableStateFlow<AuthSession?>(null)

    var nextResult: Result<AuthSession, AuthError> = Result.Success(
        AuthSession(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            userId = "user-1",
            displayName = "테스트유저",
            provider = AuthProvider.GOOGLE,
            isNewUser = false,
        ),
    )

    var lastCredential: SocialAuthCredential? = null

    override suspend fun exchangeSocialToken(
        credential: SocialAuthCredential,
    ): Result<AuthSession, AuthError> {
        lastCredential = credential
        (nextResult as? Result.Success)?.data?.let { sessionFlow.value = it }
        return nextResult
    }

    override suspend fun clearSession() {
        sessionFlow.value = null
    }

    override fun observeSession(): Flow<AuthSession?> = sessionFlow
}
