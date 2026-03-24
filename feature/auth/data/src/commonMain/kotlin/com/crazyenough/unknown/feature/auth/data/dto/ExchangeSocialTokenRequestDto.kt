package com.crazyenough.unknown.feature.auth.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExchangeSocialTokenRequestDto(
    val provider: String,
    val token: String,
)
