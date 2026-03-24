package com.crazyenough.unknown.feature.auth.data.remote

import com.crazyenough.unknown.core.domain.util.DataError
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.data.dto.AuthSessionDto
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential

class FakeAuthRemoteDataSource : AuthRemoteDataSource {
    var nextResult: Result<AuthSessionDto, DataError> = Result.Success(
        AuthSessionDto(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            userId = "remote-user",
            displayName = "리모트유저",
            provider = "KAKAO",
            isNewUser = true,
        ),
    )

    var lastCredential: SocialAuthCredential? = null

    override suspend fun exchangeSocialToken(
        credential: SocialAuthCredential,
    ): Result<AuthSessionDto, DataError> {
        lastCredential = credential
        return nextResult
    }
}
