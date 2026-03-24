package com.crazyenough.unknown.feature.auth.data.engine

import com.crazyenough.unknown.core.domain.util.DataError
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.data.dto.AuthSessionDto
import com.crazyenough.unknown.feature.auth.data.dto.ExchangeSocialTokenRequestDto

interface AuthNetworkEngine {
    suspend fun exchangeSocialToken(
        request: ExchangeSocialTokenRequestDto,
    ): Result<AuthSessionDto, DataError>
}
