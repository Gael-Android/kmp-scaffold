package com.crazyenough.unknown.feature.auth.presentation.login

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import co.touchlab.kermit.Logger
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.presentation.R
import com.crazyenough.unknown.feature.auth.domain.error.AuthError
import com.crazyenough.unknown.feature.auth.domain.model.AuthProvider
import com.crazyenough.unknown.feature.auth.domain.model.SocialAuthCredential
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.TokenManagerProvider
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
actual fun rememberAuthProviderLauncher(): AuthProviderLauncher {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val googleServerClientId = stringResource(R.string.google_server_client_id)
    val kakaoNativeAppKey = stringResource(R.string.kakao_native_app_key)

    return remember(
        activity,
        credentialManager,
        googleServerClientId,
        kakaoNativeAppKey,
    ) {
        AndroidAuthProviderLauncher(
            activity = activity,
            credentialManager = credentialManager,
            googleServerClientId = googleServerClientId,
            kakaoNativeAppKey = kakaoNativeAppKey,
        )
    }
}

private class AndroidAuthProviderLauncher(
    private val activity: ComponentActivity?,
    private val credentialManager: CredentialManager,
    private val googleServerClientId: String,
    private val kakaoNativeAppKey: String,
) : AuthProviderLauncher {

    override val supportedProviders: Set<AuthProvider> = setOf(
        AuthProvider.GOOGLE,
        AuthProvider.KAKAO,
    )

    override val enabledProviders: Set<AuthProvider> = buildSet {
        if (activity != null && googleServerClientId.isNotBlank()) {
            add(AuthProvider.GOOGLE)
        }
        if (activity != null && kakaoNativeAppKey.isNotBlank()) {
            add(AuthProvider.KAKAO)
        }
    }

    override fun readDevLocalAuthStatus(): DevLocalAuthStatus {
        val context = activity?.applicationContext
        val hasFirebaseCurrentUser = FirebaseAuth.getInstance().currentUser != null
        val hasKakaoToken = if (context != null && kakaoNativeAppKey.isNotBlank()) {
            KakaoSdk.init(context, kakaoNativeAppKey)
            AuthApiClient.instance.hasToken()
        } else {
            false
        }

        return DevLocalAuthStatus(
            hasFirebaseCurrentUser = hasFirebaseCurrentUser,
            hasKakaoToken = hasKakaoToken,
        )
    }

    override suspend fun restoreCredentials(): List<SocialAuthCredential> {
        val credentials = buildList {
            restoreCredentialSafely(providerName = "Firebase") {
                restoreFirebaseCredential()
            }?.let(::add)
            restoreCredentialSafely(providerName = "Kakao") {
                restoreKakaoCredential()
            }?.let(::add)
        }

        if (credentials.isEmpty()) {
            Logger.i("AuthLauncher") { "자동 로그인 복원 대상 없음" }
        }

        return credentials
    }

    private suspend fun restoreCredentialSafely(
        providerName: String,
        block: suspend () -> SocialAuthCredential?,
    ): SocialAuthCredential? {
        return try {
            block()
        } catch (error: Throwable) {
            Logger.e("AuthLauncher") {
                "자동 로그인 복원 실패($providerName): ${error.message}"
            }
            null
        }
    }

    private suspend fun restoreFirebaseCredential(): SocialAuthCredential? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        if (firebaseUser != null) {
            val restorableProvider = firebaseUser.toRestorableProvider()
            if (restorableProvider != null) {
                val firebaseIdToken = firebaseUser.getIdToken(true).awaitToken().orEmpty()
                if (firebaseIdToken.isNotBlank()) {
                    Logger.i("AuthLauncher") {
                        "자동 로그인 복원 성공(Firebase): provider=$restorableProvider"
                    }
                    return SocialAuthCredential(
                        provider = restorableProvider,
                        token = firebaseIdToken,
                    )
                }
            }
        }

        return null
    }

    private suspend fun restoreKakaoCredential(): SocialAuthCredential? {
        val kakaoAccessToken = resolveKakaoAccessToken()
        if (kakaoAccessToken.isNullOrBlank()) return null

        Logger.i("AuthLauncher") { "자동 로그인 복원 성공(Kakao 토큰)" }
        return SocialAuthCredential(
            provider = AuthProvider.KAKAO,
            token = kakaoAccessToken,
        )
    }

    private fun readKakaoAccessToken(): String? {
        val context = activity?.applicationContext ?: return null
        if (kakaoNativeAppKey.isBlank()) return null

        KakaoSdk.init(context, kakaoNativeAppKey)
        return TokenManagerProvider.instance.manager.getToken()?.accessToken
    }

    private suspend fun resolveKakaoAccessToken(): String? {
        val localToken = readKakaoAccessToken() ?: return null
        return suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.accessTokenInfo { _, error ->
                if (!continuation.isActive) return@accessTokenInfo

                if (error != null) {
                    Logger.w("AuthLauncher") {
                        "카카오 자동 로그인 토큰 확인 실패: ${error.message}"
                    }
                    continuation.resume(null)
                    return@accessTokenInfo
                }

                continuation.resume(
                    TokenManagerProvider.instance.manager.getToken()?.accessToken ?: localToken,
                )
            }
        }
    }

    override suspend fun launch(
        provider: AuthProvider,
    ): AuthProviderLaunchResult {
        return when (provider) {
            AuthProvider.GOOGLE -> launchGoogle()
            AuthProvider.KAKAO -> launchKakao()
            AuthProvider.APPLE -> AuthProviderLaunchResult.Failure(AuthError.Unavailable)
        }
    }

    override suspend fun signOut(provider: AuthProvider): Result<Unit, AuthError> {
        return when (provider) {
            AuthProvider.GOOGLE -> signOutGoogle()
            AuthProvider.KAKAO -> signOutKakao()
            AuthProvider.APPLE -> Result.Success(Unit)
        }
    }

    override suspend fun unlink(provider: AuthProvider): Result<Unit, AuthError> {
        return when (provider) {
            AuthProvider.GOOGLE -> unlinkGoogle()
            AuthProvider.KAKAO -> unlinkKakao()
            AuthProvider.APPLE -> Result.Success(Unit)
        }
    }

    private fun signOutFirebase() {
        FirebaseAuth.getInstance().signOut()
        Logger.i("AuthLauncher") { "Firebase 로그아웃 완료" }
    }

    private suspend fun signOutGoogle(): Result<Unit, AuthError> {
        signOutFirebase()

        val googleSignOutResult = signOutGoogleClient()
        val clearCredentialResult = clearGoogleCredentialState()

        return when {
            googleSignOutResult is Result.Failure -> googleSignOutResult
            clearCredentialResult is Result.Failure -> clearCredentialResult
            else -> Result.Success(Unit)
        }
    }

    private suspend fun signOutKakao(): Result<Unit, AuthError> {
        val context = activity?.applicationContext
        if (context == null || kakaoNativeAppKey.isBlank()) {
            Logger.w("AuthLauncher") { "카카오 로그아웃 불가: SDK 초기화 조건 미충족" }
            signOutFirebase()
            return Result.Failure(AuthError.Unavailable)
        }

        KakaoSdk.init(context, kakaoNativeAppKey)
        return suspendCancellableCoroutine { continuation: CancellableContinuation<Result<Unit, AuthError>> ->
            UserApiClient.instance.logout { error ->
                signOutFirebase()
                if (continuation.isActive) {
                    continuation.resume(
                        if (error == null) {
                            Logger.i("AuthLauncher") { "카카오 로그아웃 완료" }
                            Result.Success(Unit)
                        } else {
                            Logger.e("AuthLauncher") { "카카오 로그아웃 실패: ${error.message}" }
                            Result.Failure(AuthError.Unknown)
                        },
                    )
                }
            }
        }
    }

    private suspend fun unlinkGoogle(): Result<Unit, AuthError> {
        val currentActivity = activity ?: return Result.Failure(AuthError.Unavailable)

        when (val reauthResult = reauthenticateGoogleUser(currentActivity)) {
            is Result.Failure -> return reauthResult
            is Result.Success -> Unit
        }

        when (val deleteResult = deleteCurrentFirebaseUser()) {
            is Result.Failure -> return deleteResult
            is Result.Success -> Unit
        }

        revokeGoogleAccessSafely(currentActivity)
        clearGoogleCredentialStateSilently()
        return Result.Success(Unit)
    }

    private suspend fun unlinkKakao(): Result<Unit, AuthError> {
        val context = activity?.applicationContext
        if (context == null || kakaoNativeAppKey.isBlank()) {
            Logger.w("AuthLauncher") { "카카오 unlink 불가: SDK 초기화 조건 미충족" }
            return deleteCurrentFirebaseUser(
                fallbackMessage = "카카오 unlink 선행 실패: SDK 초기화 조건 미충족",
            )
        }

        KakaoSdk.init(context, kakaoNativeAppKey)
        return suspendCancellableCoroutine { continuation: CancellableContinuation<Result<Unit, AuthError>> ->
            UserApiClient.instance.unlink { error ->
                if (error != null) {
                    Logger.e("AuthLauncher") { "카카오 연결 끊기 실패: ${error.message}" }
                    if (continuation.isActive) {
                        continuation.resume(Result.Failure(AuthError.Unknown))
                    }
                    return@unlink
                } else {
                    Logger.i("AuthLauncher") { "카카오 연결 끊기 완료" }
                }

                val deleteResult = deleteCurrentFirebaseUserSync(
                    fallbackMessage = null,
                    onComplete = { result ->
                        if (continuation.isActive) {
                            continuation.resume(result)
                        }
                    },
                )
                if (deleteResult != null && continuation.isActive) {
                    continuation.resume(deleteResult)
                }
            }
        }
    }

    private suspend fun deleteCurrentFirebaseUser(
        fallbackMessage: String? = null,
    ): Result<Unit, AuthError> {
        return suspendCancellableCoroutine { continuation ->
            val immediateResult = deleteCurrentFirebaseUserSync(
                fallbackMessage = fallbackMessage,
                onComplete = { result ->
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                },
            )
            if (immediateResult != null && continuation.isActive) {
                continuation.resume(immediateResult)
            }
        }
    }

    private fun deleteCurrentFirebaseUserSync(
        fallbackMessage: String? = null,
        onComplete: (Result<Unit, AuthError>) -> Unit,
    ): Result<Unit, AuthError>? {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            if (fallbackMessage != null) {
                Logger.e("AuthLauncher") {
                    "Firebase 사용자 삭제 건너뜀: currentUser 없음, fallback=$fallbackMessage"
                }
            } else {
                Logger.w("AuthLauncher") { "Firebase 사용자 삭제 건너뜀: currentUser 없음" }
            }
            return Result.Failure(AuthError.Unauthorized)
        }

        user.delete().addOnCompleteListener { task ->
            val result = if (task.isSuccessful) {
                if (fallbackMessage != null) {
                    Logger.e("AuthLauncher") {
                        "Firebase 사용자 삭제 완료, 선행 오류=fallback=$fallbackMessage"
                    }
                } else {
                    Logger.i("AuthLauncher") { "Firebase 사용자 삭제 완료" }
                }
                Result.Success(Unit)
            } else {
                Logger.e("AuthLauncher") {
                    "Firebase 사용자 삭제 실패: ${task.exception?.message}, fallback=$fallbackMessage"
                }
                Result.Failure(task.exception.toAuthError())
            }
            onComplete(result)
        }
        return null
    }

    private suspend fun launchGoogle(): AuthProviderLaunchResult {
        val currentActivity = activity ?: return AuthProviderLaunchResult.Failure(AuthError.Unavailable)
        return when (val tokenResult = requestGoogleIdToken(currentActivity)) {
            is Result.Failure -> AuthProviderLaunchResult.Failure(tokenResult.error)
            is Result.Success -> {
                try {
                    val firebaseIdToken = signInWithFirebase(tokenResult.data)
                    if (firebaseIdToken.isBlank()) {
                        AuthProviderLaunchResult.Failure(AuthError.InvalidCredential)
                    } else {
                        AuthProviderLaunchResult.Success(
                            SocialAuthCredential(
                                provider = AuthProvider.GOOGLE,
                                token = firebaseIdToken,
                            ),
                        )
                    }
                } catch (e: FirebaseAuthInvalidCredentialsException) {
                    Logger.e("AuthLauncher") { "FirebaseInvalidCred: ${e.message}" }
                    AuthProviderLaunchResult.Failure(AuthError.InvalidCredential)
                } catch (e: FirebaseAuthException) {
                    Logger.e("AuthLauncher") { "FirebaseAuth: ${e.errorCode} ${e.message}" }
                    AuthProviderLaunchResult.Failure(AuthError.Unknown)
                } catch (e: Exception) {
                    Logger.e("AuthLauncher") { "Unknown: ${e::class.simpleName} ${e.message}" }
                    AuthProviderLaunchResult.Failure(AuthError.Unknown)
                }
            }
        }
    }

    private suspend fun requestGoogleIdToken(
        currentActivity: ComponentActivity,
    ): Result<String, AuthError> {
        if (googleServerClientId.isBlank()) {
            return Result.Failure(AuthError.Configuration)
        }

        return try {
            val signInOption = GetSignInWithGoogleOption.Builder(googleServerClientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()
            val result = credentialManager.getCredential(currentActivity, request)
            val credential = result.credential as? CustomCredential
                ?: return Result.Failure(AuthError.InvalidCredential)

            if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return Result.Failure(AuthError.InvalidCredential)
            }

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val googleIdToken = googleIdTokenCredential.idToken
            if (googleIdToken.isBlank()) {
                Result.Failure(AuthError.InvalidCredential)
            } else {
                Result.Success(googleIdToken)
            }
        } catch (_: GetCredentialCancellationException) {
            Result.Failure(AuthError.Cancelled)
        } catch (e: GoogleIdTokenParsingException) {
            Logger.e("AuthLauncher") { "GoogleIdTokenParsing: ${e.message}" }
            Result.Failure(AuthError.InvalidCredential)
        } catch (e: GetCredentialException) {
            Logger.e("AuthLauncher") { "GetCredential: ${e.type} ${e.message}" }
            Result.Failure(AuthError.Unknown)
        } catch (e: Exception) {
            Logger.e("AuthLauncher") { "Unknown: ${e::class.simpleName} ${e.message}" }
            Result.Failure(AuthError.Unknown)
        }
    }

    private suspend fun reauthenticateGoogleUser(
        currentActivity: ComponentActivity,
    ): Result<Unit, AuthError> {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: return Result.Failure(AuthError.Unauthorized)
        val googleIdTokenResult = requestGoogleIdToken(currentActivity)
        if (googleIdTokenResult is Result.Failure) {
            return googleIdTokenResult
        }

        val credential = GoogleAuthProvider.getCredential(
            (googleIdTokenResult as Result.Success).data,
            null,
        )
        return suspendCancellableCoroutine { continuation ->
            currentUser.reauthenticate(credential).addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener

                continuation.resume(
                    if (task.isSuccessful) {
                        Logger.i("AuthLauncher") { "Firebase 재인증 완료" }
                        Result.Success(Unit)
                    } else {
                        Logger.e("AuthLauncher") { "Firebase 재인증 실패: ${task.exception?.message}" }
                        Result.Failure(task.exception.toAuthError())
                    },
                )
            }
        }
    }

    private suspend fun signOutGoogleClient(): Result<Unit, AuthError> {
        val currentActivity = activity ?: return Result.Failure(AuthError.Unavailable)
        val googleClient = GoogleSignIn.getClient(
            currentActivity,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build(),
        )

        return suspendCancellableCoroutine { continuation ->
            googleClient.signOut().addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener

                continuation.resume(
                    if (task.isSuccessful) {
                        Logger.i("AuthLauncher") { "Google signOut 완료" }
                        Result.Success(Unit)
                    } else {
                        Logger.e("AuthLauncher") { "Google signOut 실패: ${task.exception?.message}" }
                        Result.Failure(task.exception.toAuthError())
                    },
                )
            }
        }
    }

    private suspend fun revokeGoogleAccessSafely(
        currentActivity: ComponentActivity,
    ) {
        val googleClient = GoogleSignIn.getClient(
            currentActivity,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build(),
        )

        suspendCancellableCoroutine<Unit> { continuation ->
            googleClient.revokeAccess().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Logger.i("AuthLauncher") { "Google revokeAccess 완료" }
                } else {
                    Logger.e("AuthLauncher") { "Google revokeAccess 실패: ${task.exception?.message}" }
                }
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
        }
    }

    private suspend fun clearGoogleCredentialState(): Result<Unit, AuthError> {
        return try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Logger.i("AuthLauncher") { "CredentialManager 상태 초기화 완료" }
            Result.Success(Unit)
        } catch (error: ClearCredentialException) {
            Logger.e("AuthLauncher") { "CredentialManager 상태 초기화 실패: ${error.message}" }
            Result.Failure(AuthError.Unknown)
        } catch (error: Exception) {
            Logger.e("AuthLauncher") { "CredentialManager 상태 초기화 실패: ${error.message}" }
            Result.Failure(AuthError.Unknown)
        }
    }

    private suspend fun clearGoogleCredentialStateSilently() {
        when (val result = clearGoogleCredentialState()) {
            is Result.Failure -> Logger.e("AuthLauncher") { "Google credential state 정리 실패: ${result.error}" }
            is Result.Success -> Unit
        }
    }

    private fun Throwable?.toAuthError(): AuthError {
        return when (this) {
            is FirebaseAuthRecentLoginRequiredException -> AuthError.ReauthenticationRequired
            is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredential
            is FirebaseAuthException -> when (errorCode) {
                "ERROR_NETWORK_REQUEST_FAILED" -> AuthError.Network
                "ERROR_REQUIRES_RECENT_LOGIN" -> AuthError.ReauthenticationRequired
                "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" -> AuthError.Unauthorized
                else -> AuthError.Unknown
            }
            else -> AuthError.Unknown
        }
    }

    private suspend fun signInWithFirebase(googleIdToken: String): String {
        return suspendCancellableCoroutine { continuation: CancellableContinuation<String> ->
            val firebaseAuth = FirebaseAuth.getInstance()
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val signInTask = firebaseAuth.signInWithCredential(credential)

            signInTask.addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener

                if (!task.isSuccessful) {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Firebase sign-in failed"),
                    )
                    return@addOnCompleteListener
                }

                val user = task.result.user
                    ?: return@addOnCompleteListener continuation.resumeWithException(
                        IllegalStateException("Firebase user is null"),
                    )

                user.getIdToken(true).addOnCompleteListener { tokenTask ->
                    if (!continuation.isActive) return@addOnCompleteListener

                    if (tokenTask.isSuccessful) {
                        val token = tokenTask.result?.token
                        if (token.isNullOrBlank()) {
                            continuation.resumeWithException(
                                IllegalStateException("Firebase id token is blank"),
                            )
                        } else {
                            Logger.i("AuthLauncher") {
                                "Firebase 인증 성공: firebaseUser=${user.toDebugString()}, tokenResult=${tokenTask.result?.toDebugString()}, firebaseIdToken=$token"
                            }
                            continuation.resume(token)
                        }
                    } else {
                        continuation.resumeWithException(
                            tokenTask.exception ?: IllegalStateException("Firebase token request failed"),
                        )
                    }
                }
            }
        }
    }

    private suspend fun launchKakao(): AuthProviderLaunchResult {
        val currentActivity = activity ?: return AuthProviderLaunchResult.Failure(AuthError.Unavailable)
        if (kakaoNativeAppKey.isBlank()) {
            return AuthProviderLaunchResult.Failure(AuthError.Configuration)
        }

        KakaoSdk.init(currentActivity.applicationContext, kakaoNativeAppKey)

        return suspendCancellableCoroutine { continuation: CancellableContinuation<AuthProviderLaunchResult> ->
            val callback = callback@ { token: OAuthToken?, error: Throwable? ->
                if (!continuation.isActive) return@callback

                when {
                    token?.accessToken?.isNotBlank() == true -> {
                        Logger.i("AuthLauncher") {
                            "카카오 로그인 성공: oauthToken=${token.toDebugString()}"
                        }
                        continueKakaoLoginWithEmailConsent(
                            currentActivity = currentActivity,
                            accessToken = token.accessToken,
                            continuation = continuation,
                        )
                    }

                    error is ClientError && error.reason == ClientErrorCause.Cancelled -> {
                        continuation.resume(AuthProviderLaunchResult.Failure(AuthError.Cancelled))
                    }

                    error != null -> {
                        Logger.e("AuthLauncher") {
                            "카카오 로그인 실패: ${error::class.simpleName} ${error.message}"
                        }
                        continuation.resume(AuthProviderLaunchResult.Failure(AuthError.Unknown))
                    }

                    else -> {
                        continuation.resume(AuthProviderLaunchResult.Failure(AuthError.InvalidCredential))
                    }
                }
            }

            if (UserApiClient.instance.isKakaoTalkLoginAvailable(currentActivity)) {
                UserApiClient.instance.loginWithKakaoTalk(
                    currentActivity,
                    callback = callback,
                )
            } else {
                UserApiClient.instance.loginWithKakaoAccount(
                    currentActivity,
                    callback = callback,
                )
            }
        }
    }

    private fun continueKakaoLoginWithEmailConsent(
        currentActivity: ComponentActivity,
        accessToken: String,
        continuation: CancellableContinuation<AuthProviderLaunchResult>,
    ) {
        UserApiClient.instance.me { user, userError ->
            if (!continuation.isActive) return@me

            if (userError != null) {
                Logger.e("AuthLauncher") {
                    "카카오 사용자 정보 조회 실패: ${userError.message}. 기존 토큰으로 진행합니다."
                }
                resumeKakaoLoginSuccess(
                    continuation = continuation,
                    accessToken = accessToken,
                )
                return@me
            }

            Logger.i("AuthLauncher") {
                "카카오 사용자 정보: id=${user?.id}, email=${user?.kakaoAccount?.email.orEmpty()}, emailNeedsAgreement=${user?.kakaoAccount?.emailNeedsAgreement}, nickname=${user?.kakaoAccount?.profile?.nickname.orEmpty()}, profileImageUrl=${user?.kakaoAccount?.profile?.profileImageUrl.orEmpty()}, thumbnailUrl=${user?.kakaoAccount?.profile?.thumbnailImageUrl.orEmpty()}"
            }

            if (user?.kakaoAccount?.emailNeedsAgreement == true) {
                Logger.i("AuthLauncher") {
                    "카카오 account_email 추가 동의를 요청합니다."
                }
                UserApiClient.instance.loginWithNewScopes(
                    currentActivity,
                    scopes = listOf("account_email"),
                ) { refreshedToken, scopeError ->
                    if (!continuation.isActive) return@loginWithNewScopes

                    when {
                        refreshedToken?.accessToken?.isNotBlank() == true -> {
                            Logger.i("AuthLauncher") {
                                "카카오 account_email 추가 동의 완료: 새 토큰으로 진행합니다."
                            }
                            resumeKakaoLoginSuccess(
                                continuation = continuation,
                                accessToken = refreshedToken.accessToken,
                            )
                        }

                        scopeError is ClientError && scopeError.reason == ClientErrorCause.Cancelled -> {
                            Logger.w("AuthLauncher") {
                                "카카오 account_email 추가 동의가 취소되었습니다. 기존 토큰으로 진행합니다."
                            }
                            resumeKakaoLoginSuccess(
                                continuation = continuation,
                                accessToken = accessToken,
                            )
                        }

                        scopeError != null -> {
                            Logger.e("AuthLauncher") {
                                "카카오 account_email 추가 동의 실패: ${scopeError.message}. 기존 토큰으로 진행합니다."
                            }
                            resumeKakaoLoginSuccess(
                                continuation = continuation,
                                accessToken = accessToken,
                            )
                        }

                        else -> {
                            Logger.w("AuthLauncher") {
                                "카카오 account_email 추가 동의 결과에 새 토큰이 없어 기존 토큰으로 진행합니다."
                            }
                            resumeKakaoLoginSuccess(
                                continuation = continuation,
                                accessToken = accessToken,
                            )
                        }
                    }
                }
                return@me
            }

            resumeKakaoLoginSuccess(
                continuation = continuation,
                accessToken = accessToken,
            )
        }
    }

    private fun resumeKakaoLoginSuccess(
        continuation: CancellableContinuation<AuthProviderLaunchResult>,
        accessToken: String,
    ) {
        if (!continuation.isActive) return
        continuation.resume(
            AuthProviderLaunchResult.Success(
                SocialAuthCredential(
                    provider = AuthProvider.KAKAO,
                    token = accessToken,
                ),
            ),
        )
    }
}

private fun FirebaseUser.toRestorableProvider(): AuthProvider? {
    val providerIds = providerData.map { it.providerId }
    return when {
        providerIds.contains("google.com") -> AuthProvider.GOOGLE
        providerIds.contains("apple.com") -> AuthProvider.APPLE
        else -> null
    }
}

private suspend fun com.google.android.gms.tasks.Task<GetTokenResult>.awaitToken(): String? {
    return suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener

            if (task.isSuccessful) {
                continuation.resume(task.result?.token)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Token task failed"),
                )
            }
        }
    }
}

private fun String.redactToken(visibleLength: Int = 8): String {
    if (length <= visibleLength) return this
    return take(visibleLength) + "..."
}

private fun OAuthToken.toDebugString(): String {
    return toString()
}

private fun FirebaseUser.toDebugString(): String {
    val providerDataString = providerData.joinToString(
        prefix = "[",
        postfix = "]",
    ) { provider ->
        "{providerId=${provider.providerId}, uid=${provider.uid}, displayName=${provider.displayName}, email=${provider.email}, phoneNumber=${provider.phoneNumber}, photoUrl=${provider.photoUrl}}"
    }

    return "FirebaseUser(uid=$uid, providerId=$providerId, displayName=$displayName, email=$email, isEmailVerified=$isEmailVerified, phoneNumber=$phoneNumber, photoUrl=$photoUrl, tenantId=$tenantId, providerData=$providerDataString, metadata={creationTimestamp=${metadata?.creationTimestamp}, lastSignInTimestamp=${metadata?.lastSignInTimestamp}})"
}

private fun GetTokenResult.toDebugString(): String {
    return "GetTokenResult(token=$token, expirationTimestamp=$expirationTimestamp, authTimestamp=$authTimestamp, issuedAtTimestamp=$issuedAtTimestamp, signInProvider=$signInProvider, signInSecondFactor=$signInSecondFactor, claims=$claims)"
}

private fun Context.findActivity(): ComponentActivity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is ComponentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
