package com.crazyenough.unknown.feature.auth.data.engine

import com.crazyenough.unknown.core.domain.util.DataError
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.data.dto.AuthSessionDto
import com.crazyenough.unknown.feature.auth.data.dto.ExchangeSocialTokenRequestDto

actual fun provideAuthNetworkEngine(): AuthNetworkEngine {
    return DefaultAuthNetworkEngine(
        firebaseSessionDataSource = IosFirebaseSessionDataSource(),
        socialTokenExchangeDataSource = IosSocialTokenExchangeDataSource(),
    )
}

private class IosFirebaseSessionDataSource : FirebaseSessionDataSource {
    override suspend fun currentSession(
        request: FirebaseSessionRequest,
    ): Result<AuthSessionDto, DataError> {
        val bridge = getIosAuthNetworkBridge()
            ?: return Result.Failure(DataError.Remote.UNKNOWN)

        val payload = bridge.currentFirebaseSession(
            providerName = request.provider,
            preferredToken = request.preferredToken,
            userIdOverride = request.userIdOverride,
            displayNameOverride = request.displayNameOverride,
            isNewUserOverride = request.isNewUserOverride,
        )

        if (payload.errorCode != null) {
            return Result.Failure(payload.errorCode.toDataError())
        }

        if (
            payload.accessToken.isBlank() ||
            payload.userId.isBlank() ||
            payload.providerName.isBlank()
        ) {
            return Result.Failure(DataError.Remote.SERIALIZATION)
        }

        return Result.Success(
            AuthSessionDto(
                accessToken = payload.accessToken,
                refreshToken = payload.refreshToken,
                userId = payload.userId,
                displayName = payload.displayName,
                provider = payload.providerName,
                isNewUser = payload.isNewUser,
            ),
        )
    }

    override suspend fun signInWithCustomToken(
        customToken: String,
    ): Result<CustomTokenSignInDto, DataError> {
        val bridge = getIosAuthNetworkBridge()
            ?: return Result.Failure(DataError.Remote.UNKNOWN)

        val payload = bridge.signInWithCustomToken(customToken)
        if (payload.errorCode != null) {
            return Result.Failure(payload.errorCode.toDataError())
        }

        return Result.Success(
            CustomTokenSignInDto(
                isNewUser = payload.isNewUser,
            ),
        )
    }
}

private class IosSocialTokenExchangeDataSource : SocialTokenExchangeDataSource {
    override suspend fun exchangeSocialToken(
        request: ExchangeSocialTokenRequestDto,
    ): Result<SocialTokenExchangeDto, DataError> {
        val bridge = getIosAuthNetworkBridge()
            ?: return Result.Failure(DataError.Remote.UNKNOWN)

        val payload = bridge.exchangeSocialToken(
            providerName = request.provider,
            token = request.token,
        )

        if (payload.errorCode != null) {
            return Result.Failure(payload.errorCode.toDataError())
        }

        if (payload.customToken.isBlank()) {
            return Result.Failure(DataError.Remote.SERIALIZATION)
        }

        return Result.Success(
            SocialTokenExchangeDto(
                customToken = payload.customToken,
                userId = payload.userId,
                displayName = payload.displayName,
                isNewUser = payload.isNewUser,
            ),
        )
    }
}

private fun String.toDataError(): DataError {
    return when (trim().uppercase()) {
        "NETWORK" -> DataError.Remote.SERVICE_UNAVAILABLE
        "UNAUTHORIZED" -> DataError.Remote.UNAUTHORIZED
        "INVALID_CREDENTIAL" -> DataError.Remote.BAD_REQUEST
        "CONFIGURATION" -> DataError.Remote.UNKNOWN
        "UNAVAILABLE" -> DataError.Remote.UNKNOWN
        "CANCELLED" -> DataError.Remote.UNKNOWN
        else -> DataError.Remote.UNKNOWN
    }
}
