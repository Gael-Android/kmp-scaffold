package com.crazyenough.unknown.feature.auth.domain.model

enum class AuthProvider {
    GOOGLE,
    KAKAO,
    APPLE,
    ;

    companion object {
        fun fromNameOrNull(name: String): AuthProvider? {
            return entries.firstOrNull { provider ->
                provider.name == name.trim().uppercase()
            }
        }
    }
}
