package com.crazyenough.unknown.feature.auth.data.engine

import com.crazyenough.unknown.core.domain.util.DataError
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.data.dto.AuthSessionDto
import com.crazyenough.unknown.feature.auth.data.dto.ExchangeSocialTokenRequestDto

class FakeAuthEngine : AuthNetworkEngine {
    var nextResult: Result<AuthSessionDto, DataError> = Result.Success(
        AuthSessionDto(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            userId = "user-1",
            displayName = "테스트유저",
            provider = "GOOGLE",
            isNewUser = false,
        ),
    )

    var lastRequest: ExchangeSocialTokenRequestDto? = null

    override suspend fun exchangeSocialToken(
        request: ExchangeSocialTokenRequestDto,
    ): Result<AuthSessionDto, DataError> {
        lastRequest = request
        return nextResult
    }
}
