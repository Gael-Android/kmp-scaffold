package com.crazyenough.unknown.feature.auth.data.engine

interface IosAuthNetworkBridge {
    suspend fun currentFirebaseSession(
        providerName: String,
        preferredToken: String,
        userIdOverride: String?,
        displayNameOverride: String?,
        isNewUserOverride: Boolean?,
    ): IosFirebaseSessionPayload

    suspend fun signInWithCustomToken(
        customToken: String,
    ): IosCustomTokenSignInPayload

    suspend fun exchangeSocialToken(
        providerName: String,
        token: String,
    ): IosSocialTokenExchangePayload
}

data class IosFirebaseSessionPayload(
    val accessToken: String = "",
    val refreshToken: String = "",
    val userId: String = "",
    val displayName: String = "",
    val providerName: String = "",
    val isNewUser: Boolean = false,
    val errorCode: String? = null,
)

data class IosCustomTokenSignInPayload(
    val isNewUser: Boolean? = null,
    val errorCode: String? = null,
)

data class IosSocialTokenExchangePayload(
    val customToken: String = "",
    val userId: String = "",
    val displayName: String = "",
    val isNewUser: Boolean? = null,
    val errorCode: String? = null,
)

private var iosAuthNetworkBridge: IosAuthNetworkBridge? = null

fun registerIosAuthNetworkBridge(bridge: IosAuthNetworkBridge?) {
    iosAuthNetworkBridge = bridge
}

internal fun getIosAuthNetworkBridge(): IosAuthNetworkBridge? = iosAuthNetworkBridge
