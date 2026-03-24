package com.crazyenough.unknown.feature.auth.data.engine

import com.crazyenough.unknown.core.domain.util.DataError
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.data.dto.AuthSessionDto
import com.crazyenough.unknown.feature.auth.data.dto.ExchangeSocialTokenRequestDto

class StubAuthNetworkEngine : AuthNetworkEngine {
    override suspend fun exchangeSocialToken(
        request: ExchangeSocialTokenRequestDto,
    ): Result<AuthSessionDto, DataError> {
        if (request.token.isBlank()) {
            return Result.Failure(DataError.Remote.UNAUTHORIZED)
        }

        return Result.Success(
            AuthSessionDto(
                accessToken = "app-access-${request.provider.lowercase()}",
                refreshToken = "app-refresh-${request.provider.lowercase()}",
                userId = "${request.provider.lowercase()}-user",
                displayName = when (request.provider.uppercase()) {
                    "GOOGLE" -> "Google 사용자"
                    "KAKAO" -> "카카오 사용자"
                    else -> "소셜 사용자"
                },
                provider = request.provider,
                isNewUser = false,
            ),
        )
    }
}
