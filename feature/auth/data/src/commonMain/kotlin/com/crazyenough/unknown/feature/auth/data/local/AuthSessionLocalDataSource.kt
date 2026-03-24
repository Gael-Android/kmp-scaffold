package com.crazyenough.unknown.feature.auth.data.local

import com.crazyenough.unknown.feature.auth.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface AuthSessionLocalDataSource {
    suspend fun saveSession(session: AuthSession)

    suspend fun getSession(): AuthSession?

    suspend fun clearSession()

    fun observeSession(): Flow<AuthSession?>
}
