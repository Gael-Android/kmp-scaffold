package com.crazyenough.unknown.feature.auth.data.local

import com.crazyenough.unknown.feature.auth.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthLocalDataSource : AuthSessionLocalDataSource {
    private val sessionFlow = MutableStateFlow<AuthSession?>(null)
    var savedSession: AuthSession? = null
    var clearSessionCalled = false

    override suspend fun saveSession(session: AuthSession) {
        savedSession = session
        sessionFlow.value = session
    }

    override suspend fun getSession(): AuthSession? = savedSession

    override suspend fun clearSession() {
        clearSessionCalled = true
        savedSession = null
        sessionFlow.value = null
    }

    override fun observeSession(): Flow<AuthSession?> = sessionFlow
}
