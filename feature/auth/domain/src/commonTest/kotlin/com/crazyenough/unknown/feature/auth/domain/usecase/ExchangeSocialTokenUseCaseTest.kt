package com.crazyenough.unknown.feature.auth.domain.usecase

import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.core.domain.util.onFailure
import com.crazyenough.unknown.core.domain.util.onSuccess
import com.crazyenough.unknown.feature.auth.domain.error.AuthError
import com.crazyenough.unknown.feature.auth.domain.fake.FakeAuthRepository
import com.crazyenough.unknown.feature.auth.domain.model.AuthProvider
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ExchangeSocialTokenUseCaseTest {

    @Test
    fun exchangeSocialToken_returnsSession_whenCredentialIsValid() = runTest {
        // 실제 로그인 플로우에서는 Google 또는 Kakao SDK에서 받은 토큰이 서버 교환 단계로 이어져야 한다.
        // 이 시나리오는 유즈케이스가 유효한 provider/token 조합을 그대로 repository에 위임하는지 확인하기 위해 필요하다.
        // 검증 포인트는 성공 결과가 유지되고, 마지막으로 전달된 credential이 입력값과 동일한지다.
        val repository = FakeAuthRepository()
        val useCase = ExchangeSocialTokenUseCase(repository)
        val credential = SocialAuthCredential(
            provider = AuthProvider.GOOGLE,
            token = "google-id-token",
        )

        val result = useCase(credential)

        result.onSuccess { session ->
            assertEquals(AuthProvider.GOOGLE, session.provider)
            assertEquals("테스트유저", session.displayName)
        }.onFailure {
            error("유효한 토큰 교환은 실패하면 안 된다: $it")
        }
        assertEquals(credential, repository.lastCredential)
    }

    @Test
    fun exchangeSocialToken_returnsFailure_whenCredentialTokenIsBlank() = runTest {
        // 실제 기기에서는 사용자가 로그인을 취소하거나 SDK 응답 파싱이 실패하면 빈 토큰이 들어올 수 있다.
        // 이런 값이 서버로 전송되면 원인 파악이 어려워지므로 유즈케이스에서 먼저 차단해야 한다.
        // 검증 포인트는 repository를 호출하지 않고 InvalidCredential 오류를 즉시 반환하는지다.
        val repository = FakeAuthRepository()
        val useCase = ExchangeSocialTokenUseCase(repository)

        val result = useCase(
            SocialAuthCredential(
                provider = AuthProvider.KAKAO,
                token = "   ",
            ),
        )

        result.onSuccess {
            error("빈 토큰은 성공으로 처리되면 안 된다.")
        }.onFailure { error ->
            assertEquals(AuthError.InvalidCredential, error)
        }
        assertEquals(null, repository.lastCredential)
    }

    @Test
    fun exchangeSocialToken_returnsRepositoryFailure_whenBackendRejectsCredential() = runTest {
        // 실제 서비스에서는 서버가 만료된 provider token을 거부할 수 있다.
        // 이 시나리오는 유즈케이스가 backend 계층의 실패를 덮어쓰지 않고 그대로 상위 계층에 전달하는지 검증한다.
        // 검증 포인트는 Unauthorized 오류가 손실되지 않고 유지되는지다.
        val repository = FakeAuthRepository().apply {
            nextResult = Result.Failure(AuthError.Unauthorized)
        }
        val useCase = ExchangeSocialTokenUseCase(repository)

        val result = useCase(
            SocialAuthCredential(
                provider = AuthProvider.KAKAO,
                token = "kakao-access-token",
            ),
        )

        result.onSuccess {
            error("인증 거절 시나리오는 성공으로 처리되면 안 된다.")
        }.onFailure { error ->
            assertEquals(AuthError.Unauthorized, error)
        }
    }
}
