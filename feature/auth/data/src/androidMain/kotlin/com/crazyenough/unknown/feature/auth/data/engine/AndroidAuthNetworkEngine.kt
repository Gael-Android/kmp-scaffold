package com.crazyenough.unknown.feature.auth.data.engine

import com.crazyenough.unknown.core.domain.util.DataError
import com.crazyenough.unknown.core.domain.util.Result
import com.crazyenough.unknown.feature.auth.data.dto.AuthSessionDto
import com.crazyenough.unknown.feature.auth.data.dto.ExchangeSocialTokenRequestDto
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

private const val FUNCTIONS_REGION = "asia-northeast3"

actual fun provideAuthNetworkEngine(): AuthNetworkEngine {
    return DefaultAuthNetworkEngine(
        firebaseSessionDataSource = AndroidFirebaseSessionDataSource(FirebaseAuth.getInstance()),
        socialTokenExchangeDataSource = AndroidSocialTokenExchangeDataSource(
            FirebaseFunctions.getInstance(FUNCTIONS_REGION),
        ),
    )
}

private class AndroidFirebaseSessionDataSource(
    private val firebaseAuth: FirebaseAuth,
) : FirebaseSessionDataSource {
    override suspend fun currentSession(
        request: FirebaseSessionRequest,
    ): Result<AuthSessionDto, DataError> {
        return runCatching {
            val user = firebaseAuth.currentUser ?: throw UnauthorizedException()
            val accessToken = request.preferredToken.ifBlank {
                user.getIdTokenSuspend(forceRefresh = true).token.orEmpty()
            }

            if (accessToken.isBlank()) {
                throw UnauthorizedException()
            }

            AuthSessionDto(
                accessToken = accessToken,
                refreshToken = "",
                userId = request.userIdOverride ?: user.uid,
                displayName = request.displayNameOverride ?: user.displayName ?: user.email.orEmpty(),
                provider = request.provider,
                isNewUser = request.isNewUserOverride ?: false,
            )
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Failure(it.toDataError()) },
        )
    }

    override suspend fun signInWithCustomToken(
        customToken: String,
    ): Result<CustomTokenSignInDto, DataError> {
        return runCatching {
            if (customToken.isBlank()) throw SerializationException()

            val signInResult = firebaseAuth.signInWithCustomToken(customToken).await()
            CustomTokenSignInDto(
                isNewUser = signInResult.additionalUserInfo?.isNewUser,
            )
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Failure(it.toDataError()) },
        )
    }
}

private class AndroidSocialTokenExchangeDataSource(
    private val firebaseFunctions: FirebaseFunctions,
) : SocialTokenExchangeDataSource {
    override suspend fun exchangeSocialToken(
        request: ExchangeSocialTokenRequestDto,
    ): Result<SocialTokenExchangeDto, DataError> {
        return runCatching {
            val response = firebaseFunctions
                .getHttpsCallable("exchangeSocialToken")
                .call(
                    mapOf(
                        "provider" to request.provider,
                        "token" to request.token,
                    ),
                )
                .await()

            val data = response.data as? Map<*, *> ?: throw SerializationException()
            val customToken = data["customToken"] as? String ?: throw SerializationException()
            SocialTokenExchangeDto(
                customToken = customToken,
                userId = data["userId"] as? String ?: "",
                displayName = data["displayName"] as? String ?: "",
                isNewUser = data["isNewUser"] as? Boolean,
            )
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Failure(it.toDataError()) },
        )
    }
}

private class UnauthorizedException : IllegalStateException()

private class SerializationException : IllegalStateException()

private suspend fun FirebaseUser.getIdTokenSuspend(forceRefresh: Boolean) =
    getIdToken(forceRefresh).await()

private suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener

            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Task failed"),
                )
            }
        }
    }
}

private fun Throwable.toDataError(): DataError {
    return when (this) {
        is UnauthorizedException -> DataError.Remote.UNAUTHORIZED
        is SerializationException -> DataError.Remote.SERIALIZATION
        is FirebaseFunctionsException -> code.toDataError()
        is FirebaseAuthException -> DataError.Remote.UNAUTHORIZED
        else -> DataError.Remote.UNKNOWN
    }
}

private fun FirebaseFunctionsException.Code.toDataError(): DataError {
    return when (this) {
        FirebaseFunctionsException.Code.OK -> DataError.Remote.UNKNOWN
        FirebaseFunctionsException.Code.INVALID_ARGUMENT -> DataError.Remote.BAD_REQUEST
        FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> DataError.Remote.REQUEST_TIMEOUT
        FirebaseFunctionsException.Code.UNAUTHENTICATED -> DataError.Remote.UNAUTHORIZED
        FirebaseFunctionsException.Code.PERMISSION_DENIED -> DataError.Remote.FORBIDDEN
        FirebaseFunctionsException.Code.NOT_FOUND -> DataError.Remote.NOT_FOUND
        FirebaseFunctionsException.Code.ALREADY_EXISTS -> DataError.Remote.CONFLICT
        FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> DataError.Remote.TOO_MANY_REQUESTS
        FirebaseFunctionsException.Code.OUT_OF_RANGE -> DataError.Remote.PAYLOAD_TOO_LARGE
        FirebaseFunctionsException.Code.INTERNAL,
        FirebaseFunctionsException.Code.DATA_LOSS,
        -> DataError.Remote.SERVER_ERROR

        FirebaseFunctionsException.Code.UNAVAILABLE -> DataError.Remote.SERVICE_UNAVAILABLE
        FirebaseFunctionsException.Code.CANCELLED,
        FirebaseFunctionsException.Code.UNKNOWN,
        FirebaseFunctionsException.Code.ABORTED,
        FirebaseFunctionsException.Code.FAILED_PRECONDITION,
        FirebaseFunctionsException.Code.UNIMPLEMENTED,
        -> DataError.Remote.UNKNOWN
    }
}
