package com.crazyenough.unknown.feature.auth.data.engine

import com.crazyenough.unknown.core.domain.util.DataError
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.data.dto.AuthSessionDto
import com.crazyenough.unknown.feature.auth.data.dto.ExchangeSocialTokenRequestDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultAuthNetworkEngineTest {

    @Test
    fun exchangeSocialToken_usesFirebaseSessionDirectly_forGoogle() = runTest {
        val firebaseSessionDataSource = FakeFirebaseSessionDataSource().apply {
            nextCurrentSessionResult = Result.Success(
                AuthSessionDto(
                    accessToken = "firebase-token",
                    refreshToken = "",
                    userId = "google-user",
                    displayName = "Google",
                    provider = "GOOGLE",
                    isNewUser = false,
                ),
            )
        }
        val socialTokenExchangeDataSource = FakeSocialTokenExchangeDataSource()
        val engine = DefaultAuthNetworkEngine(
            firebaseSessionDataSource = firebaseSessionDataSource,
            socialTokenExchangeDataSource = socialTokenExchangeDataSource,
        )

        val result = engine.exchangeSocialToken(
            ExchangeSocialTokenRequestDto(
                provider = "GOOGLE",
                token = "preferred-token",
            ),
        )

        assertTrue(result is Result.Success && result.data.provider == "GOOGLE")
        assertEquals("GOOGLE", firebaseSessionDataSource.lastCurrentSessionRequest?.provider)
        assertEquals("preferred-token", firebaseSessionDataSource.lastCurrentSessionRequest?.preferredToken)
        assertEquals(null, socialTokenExchangeDataSource.lastRequest)
    }

    @Test
    fun exchangeSocialToken_orchestratesKakaoExchange_inCommonEngine() = runTest {
        val firebaseSessionDataSource = FakeFirebaseSessionDataSource().apply {
            nextSignInResult = Result.Success(CustomTokenSignInDto(isNewUser = false))
            nextCurrentSessionResult = Result.Success(
                AuthSessionDto(
                    accessToken = "firebase-id-token",
                    refreshToken = "",
                    userId = "kakao:1234",
                    displayName = "카카오유저",
                    provider = "KAKAO",
                    isNewUser = true,
                ),
            )
        }
        val socialTokenExchangeDataSource = FakeSocialTokenExchangeDataSource().apply {
            nextResult = Result.Success(
                SocialTokenExchangeDto(
                    customToken = "custom-token",
                    userId = "kakao:1234",
                    displayName = "카카오유저",
                    isNewUser = true,
                ),
            )
        }
        val engine = DefaultAuthNetworkEngine(
            firebaseSessionDataSource = firebaseSessionDataSource,
            socialTokenExchangeDataSource = socialTokenExchangeDataSource,
        )

        val result = engine.exchangeSocialToken(
            ExchangeSocialTokenRequestDto(
                provider = "KAKAO",
                token = "kakao-access-token",
            ),
        )

        assertTrue(result is Result.Success && result.data.isNewUser)
        assertEquals("custom-token", firebaseSessionDataSource.lastCustomToken)
        assertEquals("KAKAO", firebaseSessionDataSource.lastCurrentSessionRequest?.provider)
        assertEquals("kakao:1234", firebaseSessionDataSource.lastCurrentSessionRequest?.userIdOverride)
        assertEquals(true, firebaseSessionDataSource.lastCurrentSessionRequest?.isNewUserOverride)
    }

    @Test
    fun exchangeSocialToken_returnsFailure_whenKakaoRemoteExchangeFails() = runTest {
        val firebaseSessionDataSource = FakeFirebaseSessionDataSource()
        val socialTokenExchangeDataSource = FakeSocialTokenExchangeDataSource().apply {
            nextResult = Result.Failure(DataError.Remote.UNAUTHORIZED)
        }
        val engine = DefaultAuthNetworkEngine(
            firebaseSessionDataSource = firebaseSessionDataSource,
            socialTokenExchangeDataSource = socialTokenExchangeDataSource,
        )

        val result = engine.exchangeSocialToken(
            ExchangeSocialTokenRequestDto(
                provider = "KAKAO",
                token = "bad-token",
            ),
        )

        assertTrue(result is Result.Failure && result.error == DataError.Remote.UNAUTHORIZED)
        assertEquals(null, firebaseSessionDataSource.lastCustomToken)
    }

    @Test
    fun exchangeSocialToken_ignoresBlankKakaoOverrides_whenExchangePayloadOmitsProfileFields() = runTest {
        val firebaseSessionDataSource = FakeFirebaseSessionDataSource().apply {
            nextSignInResult = Result.Success(CustomTokenSignInDto(isNewUser = true))
            nextCurrentSessionResult = Result.Success(
                AuthSessionDto(
                    accessToken = "firebase-id-token",
                    refreshToken = "",
                    userId = "firebase-user",
                    displayName = "Firebase User",
                    provider = "KAKAO",
                    isNewUser = true,
                ),
            )
        }
        val socialTokenExchangeDataSource = FakeSocialTokenExchangeDataSource().apply {
            nextResult = Result.Success(
                SocialTokenExchangeDto(
                    customToken = "custom-token",
                    userId = "",
                    displayName = "",
                    isNewUser = true,
                ),
            )
        }
        val engine = DefaultAuthNetworkEngine(
            firebaseSessionDataSource = firebaseSessionDataSource,
            socialTokenExchangeDataSource = socialTokenExchangeDataSource,
        )

        val result = engine.exchangeSocialToken(
            ExchangeSocialTokenRequestDto(
                provider = "KAKAO",
                token = "kakao-access-token",
            ),
        )

        assertTrue(result is Result.Success && result.data.userId == "firebase-user")
        assertEquals(null, firebaseSessionDataSource.lastCurrentSessionRequest?.userIdOverride)
        assertEquals(null, firebaseSessionDataSource.lastCurrentSessionRequest?.displayNameOverride)
    }
}

private class FakeFirebaseSessionDataSource : FirebaseSessionDataSource {
    var nextCurrentSessionResult: Result<AuthSessionDto, DataError> = Result.Failure(DataError.Remote.UNKNOWN)
    var nextSignInResult: Result<CustomTokenSignInDto, DataError> = Result.Failure(DataError.Remote.UNKNOWN)
    var lastCurrentSessionRequest: FirebaseSessionRequest? = null
    var lastCustomToken: String? = null

    override suspend fun currentSession(
        request: FirebaseSessionRequest,
    ): Result<AuthSessionDto, DataError> {
        lastCurrentSessionRequest = request
        return nextCurrentSessionResult
    }

    override suspend fun signInWithCustomToken(
        customToken: String,
    ): Result<CustomTokenSignInDto, DataError> {
        lastCustomToken = customToken
        return nextSignInResult
    }
}

private class FakeSocialTokenExchangeDataSource : SocialTokenExchangeDataSource {
    var nextResult: Result<SocialTokenExchangeDto, DataError> = Result.Failure(DataError.Remote.UNKNOWN)
    var lastRequest: ExchangeSocialTokenRequestDto? = null

    override suspend fun exchangeSocialToken(
        request: ExchangeSocialTokenRequestDto,
    ): Result<SocialTokenExchangeDto, DataError> {
        lastRequest = request
        return nextResult
    }
}
