package com.crazyenough.unknown.feature.auth.data.remote

import com.crazyenough.unknown.core.domain.util.DataError
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential
import com.crazyenough.unknown.feature.auth.data.dto.AuthSessionDto

interface AuthRemoteDataSource {
    suspend fun exchangeSocialToken(
        credential: SocialAuthCredential,
    ): Result<AuthSessionDto, DataError>
}
