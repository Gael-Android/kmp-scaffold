package com.crazyenough.unknown.feature.auth.data.repository

import com.crazyenough.unknown.core.domain.util.DataError
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.core.domain.util.onFailure
import com.crazyenough.unknown.core.domain.util.onSuccess
import com.crazyenough.unknown.feature.auth.data.dto.AuthSessionDto
import com.crazyenough.unknown.feature.auth.data.local.FakeAuthLocalDataSource
import com.crazyenough.unknown.feature.auth.data.remote.FakeAuthRemoteDataSource
import com.crazyenough.unknown.feature.auth.domain.error.AuthError
import com.crazyenough.unknown.feature.auth.domain.model.AuthProvider
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultAuthRepositoryTest {

    @Test
    fun exchangeSocialToken_savesSession_whenRemoteExchangeSucceeds() = runTest {
        // 실제 로그인 플로우에서는 provider token 교환이 성공하면 앱 세션이 로컬에 저장되어야 이후 화면 진입이 안정적이다.
        // 이 시나리오는 repository가 remote 응답을 domain 모델로 바꾼 뒤 local data source에 저장하는지 확인하기 위해 필요하다.
        // 검증 포인트는 성공 결과의 provider/displayName과 로컬 저장 세션이 동일하게 유지되는지다.
        val remote = FakeAuthRemoteDataSource().apply {
            nextResult = Result.Success(
                AuthSessionDto(
                    accessToken = "app-access",
                    refreshToken = "app-refresh",
                    userId = "google-user",
                    displayName = "구글유저",
                    provider = "GOOGLE",
                    isNewUser = false,
                ),
            )
        }
        val local = FakeAuthLocalDataSource()
        val repository = DefaultAuthRepository(remote, local)

        val result = repository.exchangeSocialToken(
            SocialAuthCredential(
                provider = AuthProvider.GOOGLE,
                token = "google-id-token",
            ),
        )

        result.onSuccess { session ->
            assertEquals(AuthProvider.GOOGLE, session.provider)
            assertEquals("구글유저", session.displayName)
        }.onFailure {
            error("정상 교환은 실패하면 안 된다: $it")
        }
        assertEquals("구글유저", local.savedSession?.displayName)
    }

    @Test
    fun exchangeSocialToken_returnsMappedFailure_whenRemoteExchangeFails() = runTest {
        // 실제 서비스에서는 서버 응답 지연이나 네트워크 단절로 토큰 교환이 실패할 수 있다.
        // 이 경우 presentation 계층은 provider 오류가 아니라 네트워크 오류로 안내해야 하므로 repository의 에러 매핑이 중요하다.
        // 검증 포인트는 DataError.Remote.NO_INTERNET이 AuthError.Network로 변환되는지다.
        val remote = FakeAuthRemoteDataSource().apply {
            nextResult = Result.Failure(DataError.Remote.NO_INTERNET)
        }
        val local = FakeAuthLocalDataSource()
        val repository = DefaultAuthRepository(remote, local)

        val result = repository.exchangeSocialToken(
            SocialAuthCredential(
                provider = AuthProvider.KAKAO,
                token = "kakao-token",
            ),
        )

        result.onSuccess {
            error("네트워크 실패는 성공으로 처리되면 안 된다.")
        }.onFailure { error ->
            assertEquals(AuthError.Network, error)
        }
        assertEquals(null, local.savedSession)
    }

    @Test
    fun clearSession_clearsLocalSession() = runTest {
        val remote = FakeAuthRemoteDataSource()
        val local = FakeAuthLocalDataSource().apply {
            saveSession(
                com.crazyenough.unknown.feature.auth.domain.model.AuthSession(
                    accessToken = "app-access",
                    refreshToken = "app-refresh",
                    userId = "user-1",
                    displayName = "테스트유저",
                    provider = AuthProvider.GOOGLE,
                    isNewUser = false,
                ),
            )
        }
        val repository = DefaultAuthRepository(remote, local)

        repository.clearSession()

        assertEquals(true, local.clearSessionCalled)
        assertEquals(null, local.savedSession)
    }
}
