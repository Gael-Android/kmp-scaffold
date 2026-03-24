package com.crazyenough.unknown.feature.auth.data.local

import com.crazyenough.unknown.feature.auth.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryAuthSessionLocalDataSource : AuthSessionLocalDataSource {
    private val sessionFlow = MutableStateFlow<AuthSession?>(null)

    override suspend fun saveSession(session: AuthSession) {
        sessionFlow.value = session
    }

    override suspend fun getSession(): AuthSession? = sessionFlow.value

    override suspend fun clearSession() {
        sessionFlow.value = null
    }

    override fun observeSession(): Flow<AuthSession?> = sessionFlow.asStateFlow()
}
