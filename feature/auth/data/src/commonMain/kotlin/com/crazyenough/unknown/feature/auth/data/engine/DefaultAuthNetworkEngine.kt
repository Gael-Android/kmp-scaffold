package com.crazyenough.unknown.feature.auth.data.engine

import com.crazyenough.unknown.core.domain.util.DataError
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.data.dto.AuthSessionDto
import com.crazyenough.unknown.feature.auth.data.dto.ExchangeSocialTokenRequestDto

private const val KAKAO_PROVIDER = "KAKAO"

class DefaultAuthNetworkEngine(
    private val firebaseSessionDataSource: FirebaseSessionDataSource,
    private val socialTokenExchangeDataSource: SocialTokenExchangeDataSource,
) : AuthNetworkEngine {
    override suspend fun exchangeSocialToken(
        request: ExchangeSocialTokenRequestDto,
    ): Result<AuthSessionDto, DataError> {
        return when (request.provider.uppercase()) {
            KAKAO_PROVIDER -> exchangeKakaoToken(request)
            else -> firebaseSessionDataSource.currentSession(
                FirebaseSessionRequest(
                    provider = request.provider,
                    preferredToken = request.token,
                ),
            )
        }
    }

    private suspend fun exchangeKakaoToken(
        request: ExchangeSocialTokenRequestDto,
    ): Result<AuthSessionDto, DataError> {
        val tokenExchangeResult = socialTokenExchangeDataSource.exchangeSocialToken(request)
        if (tokenExchangeResult is Result.Failure) {
            return Result.Failure(tokenExchangeResult.error)
        }

        val exchangePayload = (tokenExchangeResult as Result.Success).data
        val signInResult = firebaseSessionDataSource.signInWithCustomToken(exchangePayload.customToken)
        if (signInResult is Result.Failure) {
            return Result.Failure(signInResult.error)
        }

        val isNewUser = exchangePayload.isNewUser ?: (signInResult as Result.Success).data.isNewUser
        return firebaseSessionDataSource.currentSession(
            FirebaseSessionRequest(
                provider = request.provider,
                userIdOverride = exchangePayload.userId.takeIf { it.isNotBlank() },
                displayNameOverride = exchangePayload.displayName.takeIf { it.isNotBlank() },
                isNewUserOverride = isNewUser,
            ),
        )
    }
}

interface FirebaseSessionDataSource {
    suspend fun currentSession(
        request: FirebaseSessionRequest,
    ): Result<AuthSessionDto, DataError>

    suspend fun signInWithCustomToken(
        customToken: String,
    ): Result<CustomTokenSignInDto, DataError>
}

interface SocialTokenExchangeDataSource {
    suspend fun exchangeSocialToken(
        request: ExchangeSocialTokenRequestDto,
    ): Result<SocialTokenExchangeDto, DataError>
}

data class FirebaseSessionRequest(
    val provider: String,
    val preferredToken: String = "",
    val userIdOverride: String? = null,
    val displayNameOverride: String? = null,
    val isNewUserOverride: Boolean? = null,
)

data class CustomTokenSignInDto(
    val isNewUser: Boolean? = null,
)

data class SocialTokenExchangeDto(
    val customToken: String,
    val userId: String = "",
    val displayName: String = "",
    val isNewUser: Boolean? = null,
)
