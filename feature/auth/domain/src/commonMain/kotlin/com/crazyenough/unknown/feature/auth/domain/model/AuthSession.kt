package com.crazyenough.unknown.feature.auth.domain.model

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val displayName: String,
    val provider: AuthProvider,
    val isNewUser: Boolean,
)
