package com.crazyenough.unknown.feature.auth.domain.error

import com.crazyenough.unknown.core.domain.util.Error

sealed interface AuthError : Error {
    data object Cancelled : AuthError

    data object Configuration : AuthError

    data object Unavailable : AuthError

    data object InvalidCredential : AuthError

    data object Network : AuthError

    data object Unauthorized : AuthError

    data object ReauthenticationRequired : AuthError

    data object Unknown : AuthError
}

fun authErrorFromCode(code: String?): AuthError {
    return when (code?.trim()?.uppercase()) {
        "CANCELLED" -> AuthError.Cancelled
        "CONFIGURATION" -> AuthError.Configuration
        "UNAVAILABLE" -> AuthError.Unavailable
        "INVALID_CREDENTIAL" -> AuthError.InvalidCredential
        "NETWORK" -> AuthError.Network
        "UNAUTHORIZED" -> AuthError.Unauthorized
        "REAUTHENTICATION_REQUIRED" -> AuthError.ReauthenticationRequired
        else -> AuthError.Unknown
    }
}
