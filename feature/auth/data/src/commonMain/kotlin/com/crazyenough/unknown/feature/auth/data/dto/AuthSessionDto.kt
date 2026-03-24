package com.crazyenough.unknown.feature.auth.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthSessionDto(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val displayName: String,
    val provider: String,
    val isNewUser: Boolean,
)
