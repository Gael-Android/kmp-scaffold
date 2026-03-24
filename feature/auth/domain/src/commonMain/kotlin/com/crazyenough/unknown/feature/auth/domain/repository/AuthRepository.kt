package com.crazyenough.unknown.feature.auth.domain.repository

import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.domain.error.AuthError
import com.crazyenough.unknown.feature.auth.domain.model.AuthSession
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun exchangeSocialToken(
        credential: SocialAuthCredential,
    ): Result<AuthSession, AuthError>

    suspend fun clearSession()

    fun observeSession(): Flow<AuthSession?>
}
