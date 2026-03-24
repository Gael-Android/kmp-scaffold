package com.crazyenough.unknown.feature.auth.data.remote

import com.crazyenough.unknown.core.domain.util.DataError
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.data.dto.AuthSessionDto
import com.crazyenough.unknown.feature.auth.data.engine.AuthNetworkEngine
import com.crazyenough.unknown.feature.auth.data.mapper.toRequestDto
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential

class DefaultAuthRemoteDataSource(
    private val authNetworkEngine: AuthNetworkEngine,
) : AuthRemoteDataSource {
    override suspend fun exchangeSocialToken(
        credential: SocialAuthCredential,
    ): Result<AuthSessionDto, DataError> {
        return authNetworkEngine.exchangeSocialToken(credential.toRequestDto())
    }
}
