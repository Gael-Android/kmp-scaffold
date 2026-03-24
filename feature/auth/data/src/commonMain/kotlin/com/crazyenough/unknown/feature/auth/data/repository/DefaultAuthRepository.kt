package com.crazyenough.unknown.feature.auth.data.repository

import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.data.local.AuthSessionLocalDataSource
import com.crazyenough.unknown.feature.auth.data.mapper.toAuthError
import com.crazyenough.unknown.feature.auth.data.mapper.toDomain
import com.crazyenough.unknown.feature.auth.data.remote.AuthRemoteDataSource
import com.crazyenough.unknown.feature.auth.domain.error.AuthError
import com.crazyenough.unknown.feature.auth.domain.model.AuthSession
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential
import com.crazyenough.unknown.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class DefaultAuthRepository(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val authSessionLocalDataSource: AuthSessionLocalDataSource,
) : AuthRepository {
    override suspend fun exchangeSocialToken(
        credential: SocialAuthCredential,
    ): Result<AuthSession, AuthError> {
        return when (val result = authRemoteDataSource.exchangeSocialToken(credential)) {
            is Result.Failure -> Result.Failure(result.error.toAuthError())
            is Result.Success -> {
                val session = result.data.toDomain()
                authSessionLocalDataSource.saveSession(session)
                Result.Success(session)
            }
        }
    }

    override suspend fun clearSession() {
        authSessionLocalDataSource.clearSession()
    }

    override fun observeSession(): Flow<AuthSession?> = authSessionLocalDataSource.observeSession()
}
