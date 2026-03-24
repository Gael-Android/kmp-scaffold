package com.crazyenough.unknown.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.domain.error.AuthError
import com.crazyenough.unknown.feature.auth.domain.model.AuthSession
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential
import com.crazyenough.unknown.feature.auth.domain.repository.AuthRepository
import com.crazyenough.unknown.feature.auth.domain.usecase.ExchangeSocialTokenUseCase
import com.crazyenough.unknown.feature.auth.domain.usecase.ObserveAuthSessionUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthLoginViewModel(
    private val exchangeSocialTokenUseCase: ExchangeSocialTokenUseCase,
    private val observeAuthSessionUseCase: ObserveAuthSessionUseCase,
) : ViewModel() {

    constructor(
        exchangeSocialTokenUseCase: ExchangeSocialTokenUseCase,
    ) : this(
        exchangeSocialTokenUseCase = exchangeSocialTokenUseCase,
        observeAuthSessionUseCase = ObserveAuthSessionUseCase(NoOpAuthRepository),
    )

    private val _stateFlow = MutableStateFlow(AuthLoginState())
    val stateFlow = _stateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = _stateFlow.value,
    )

    private val eventChannel = Channel<AuthLoginEvent>(Channel.BUFFERED)
    val eventFlow = eventChannel.receiveAsFlow()

    init {
        observeAuthSession()
    }

    fun onAction(action: AuthLoginAction) {
        when (action) {
            is AuthLoginAction.OnProviderAvailabilityChanged -> {
                _stateFlow.update {
                    it.copy(
                        supportedProviders = action.supportedProviders,
                        enabledProviders = action.enabledProviders,
                    )
                }
            }

            is AuthLoginAction.OnDevLocalAuthStatusChanged -> {
                _stateFlow.update {
                    it.copy(
                        devFirebaseCurrentUserExists = action.hasFirebaseCurrentUser,
                        devKakaoTokenExists = action.hasKakaoToken,
                    )
                }
            }

            AuthLoginAction.OnAutoLoginStarted -> {
                _stateFlow.update {
                    it.copy(
                        isLoading = true,
                        errorMessage = null,
                    )
                }
            }

            AuthLoginAction.OnAutoLoginFinished -> {
                _stateFlow.update { it.copy(isLoading = false) }
            }

            AuthLoginAction.OnGoogleClick -> {
                if (_stateFlow.value.isGoogleEnabled) {
                    viewModelScope.launch {
                        eventChannel.send(AuthLoginEvent.LaunchGoogleLogin)
                    }
                }
            }

            AuthLoginAction.OnKakaoClick -> {
                if (_stateFlow.value.isKakaoEnabled) {
                    viewModelScope.launch {
                        eventChannel.send(AuthLoginEvent.LaunchKakaoLogin)
                    }
                }
            }

            AuthLoginAction.OnAppleClick -> {
                if (_stateFlow.value.isAppleEnabled) {
                    viewModelScope.launch {
                        eventChannel.send(AuthLoginEvent.LaunchAppleLogin)
                    }
                }
            }

            is AuthLoginAction.OnProviderLoginResult -> {
                exchangeCredential(action.credential)
            }

            is AuthLoginAction.OnProviderLoginFailure -> {
                _stateFlow.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = action.error.toMessage(),
                    )
                }
            }

            is AuthLoginAction.OnSignOutFailure -> {
                _stateFlow.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = action.error.toSignOutMessage(),
                    )
                }
            }

            is AuthLoginAction.OnUnlinkFailure -> {
                _stateFlow.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = action.error.toUnlinkMessage(),
                    )
                }
            }

            AuthLoginAction.OnDismissError -> {
                _stateFlow.update { it.copy(errorMessage = null) }
            }

            AuthLoginAction.OnDevGoogleSignOut -> {
                viewModelScope.launch {
                    eventChannel.send(AuthLoginEvent.LaunchSignOut(
                        com.crazyenough.unknown.feature.auth.domain.model.AuthProvider.GOOGLE,
                    ))
                }
            }

            AuthLoginAction.OnDevKakaoSignOut -> {
                viewModelScope.launch {
                    eventChannel.send(AuthLoginEvent.LaunchSignOut(
                        com.crazyenough.unknown.feature.auth.domain.model.AuthProvider.KAKAO,
                    ))
                }
            }

            AuthLoginAction.OnDevAppleSignOut -> {
                viewModelScope.launch {
                    eventChannel.send(AuthLoginEvent.LaunchSignOut(
                        com.crazyenough.unknown.feature.auth.domain.model.AuthProvider.APPLE,
                    ))
                }
            }

            AuthLoginAction.OnDevGoogleUnlink -> {
                viewModelScope.launch {
                    eventChannel.send(AuthLoginEvent.LaunchUnlink(
                        com.crazyenough.unknown.feature.auth.domain.model.AuthProvider.GOOGLE,
                    ))
                }
            }

            AuthLoginAction.OnDevKakaoUnlink -> {
                viewModelScope.launch {
                    eventChannel.send(AuthLoginEvent.LaunchUnlink(
                        com.crazyenough.unknown.feature.auth.domain.model.AuthProvider.KAKAO,
                    ))
                }
            }

            AuthLoginAction.OnDevAppleUnlink -> {
                viewModelScope.launch {
                    eventChannel.send(AuthLoginEvent.LaunchUnlink(
                        com.crazyenough.unknown.feature.auth.domain.model.AuthProvider.APPLE,
                    ))
                }
            }
        }
    }

    private fun observeAuthSession() {
        viewModelScope.launch {
            observeAuthSessionUseCase().collectLatest { session ->
                _stateFlow.update { state ->
                    state.copy(
                        loggedInAccount = session?.let {
                            AuthLoginState.LoggedInAccount(
                                provider = it.provider,
                                displayName = it.displayName,
                                userId = it.userId,
                            )
                        },
                    )
                }
            }
        }
    }

    private fun exchangeCredential(
        credential: SocialAuthCredential,
    ) {
        viewModelScope.launch {
            _stateFlow.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            when (val result = exchangeSocialTokenUseCase(credential)) {
                is Result.Failure -> {
                    _stateFlow.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.toMessage(),
                        )
                    }
                }

                is Result.Success -> {
                    _stateFlow.update { it.copy(isLoading = false) }
                    eventChannel.send(AuthLoginEvent.NavigateToAppHome)
                }
            }
        }
    }
}

private object NoOpAuthRepository : AuthRepository {
    override suspend fun exchangeSocialToken(
        credential: SocialAuthCredential,
    ): Result<AuthSession, AuthError> {
        return Result.Failure(AuthError.Unknown)
    }

    override suspend fun clearSession() = Unit

    override fun observeSession() = emptyFlow<AuthSession?>()
}

private fun AuthError.toMessage(): String? {
    return when (this) {
        AuthError.Cancelled -> null
        AuthError.Configuration -> "로그인 설정이 아직 완료되지 않았어요."
        AuthError.Unavailable -> "이 기기에서는 해당 로그인을 사용할 수 없어요."
        AuthError.InvalidCredential -> "로그인 정보를 확인하지 못했어요. 다시 시도해 주세요."
        AuthError.Network -> "네트워크 상태를 확인한 뒤 다시 시도해 주세요."
        AuthError.Unauthorized -> "로그인 인증이 만료되었어요. 다시 로그인해 주세요."
        AuthError.ReauthenticationRequired -> "다시 인증이 필요해요. Google 계정을 한 번 더 확인해 주세요."
        AuthError.Unknown -> "로그인 처리 중 문제가 발생했어요."
    }
}

private fun AuthError.toUnlinkMessage(): String? {
    return when (this) {
        AuthError.Cancelled -> null
        AuthError.Configuration -> "탈퇴 설정이 아직 완료되지 않았어요."
        AuthError.Unavailable -> "이 기기에서는 탈퇴를 진행할 수 없어요."
        AuthError.InvalidCredential -> "계정 재인증에 실패했어요. 다시 시도해 주세요."
        AuthError.Network -> "네트워크 상태를 확인한 뒤 다시 시도해 주세요."
        AuthError.Unauthorized,
        AuthError.ReauthenticationRequired,
        -> "탈퇴하려면 Google 계정을 다시 한 번 인증해 주세요."
        AuthError.Unknown -> "탈퇴 처리 중 문제가 발생했어요. 다시 시도해 주세요."
    }
}

private fun AuthError.toSignOutMessage(): String? {
    return when (this) {
        AuthError.Cancelled -> null
        AuthError.Configuration -> "로그아웃 설정이 아직 완료되지 않았어요."
        AuthError.Unavailable -> "이 기기에서는 로그아웃을 진행할 수 없어요."
        AuthError.InvalidCredential -> "로그아웃 중 계정 확인에 실패했어요. 다시 시도해 주세요."
        AuthError.Network -> "네트워크 상태를 확인한 뒤 다시 시도해 주세요."
        AuthError.Unauthorized,
        AuthError.ReauthenticationRequired,
        -> "Google 로그아웃을 마무리하지 못했어요. 다시 시도해 주세요."
        AuthError.Unknown -> "로그아웃 처리 중 문제가 발생했어요. 다시 시도해 주세요."
    }
}
