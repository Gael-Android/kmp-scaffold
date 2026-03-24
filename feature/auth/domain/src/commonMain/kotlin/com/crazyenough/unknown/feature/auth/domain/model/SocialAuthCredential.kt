package com.crazyenough.unknown.feature.auth.domain.model

data class SocialAuthCredential(
    val provider: AuthProvider,
    val token: String,
)
