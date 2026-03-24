package com.crazyenough.unknown.feature.auth.data.mapper

import com.crazyenough.unknown.core.domain.util.DataError
import com.crazyenough.unknown.feature.auth.data.dto.AuthSessionDto
import com.crazyenough.unknown.feature.auth.data.dto.ExchangeSocialTokenRequestDto
import com.crazyenough.unknown.feature.auth.domain.error.AuthError
import com.crazyenough.unknown.feature.auth.domain.model.AuthProvider
import com.crazyenough.unknown.feature.auth.domain.model.AuthSession
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential

internal fun SocialAuthCredential.toRequestDto(): ExchangeSocialTokenRequestDto {
    return ExchangeSocialTokenRequestDto(
        provider = provider.name,
        token = token,
    )
}

internal fun AuthSessionDto.toDomain(): AuthSession {
    return AuthSession(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = userId,
        displayName = displayName,
        provider = provider.toAuthProvider(),
        isNewUser = isNewUser,
    )
}

internal fun DataError.toAuthError(): AuthError {
    return when (this) {
        DataError.Remote.NO_INTERNET,
        DataError.Remote.REQUEST_TIMEOUT,
        DataError.Remote.SERVICE_UNAVAILABLE,
        DataError.Remote.SERVER_ERROR,
        -> AuthError.Network

        DataError.Remote.UNAUTHORIZED,
        DataError.Remote.FORBIDDEN,
        -> AuthError.Unauthorized

        DataError.Remote.BAD_REQUEST,
        DataError.Remote.NOT_FOUND,
        DataError.Remote.CONFLICT,
        DataError.Remote.TOO_MANY_REQUESTS,
        DataError.Remote.PAYLOAD_TOO_LARGE,
        DataError.Remote.SERIALIZATION,
        DataError.Remote.UNKNOWN,
        DataError.Local.DISK_FULL,
        DataError.Local.NOT_FOUND,
        DataError.Local.UNKNOWN,
        -> AuthError.Unknown
    }
}

private fun String.toAuthProvider(): AuthProvider {
    return when (uppercase()) {
        "GOOGLE" -> AuthProvider.GOOGLE
        "KAKAO" -> AuthProvider.KAKAO
        else -> AuthProvider.APPLE
    }
}
