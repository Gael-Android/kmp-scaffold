package com.crazyenough.unknown.feature.auth.presentation.login

interface IosAuthProviderBridge {
    fun supportedProviderNames(): List<String>

    fun enabledProviderNames(): List<String>

    fun hasFirebaseCurrentUser(): Boolean

    fun hasKakaoToken(): Boolean

    suspend fun restoreFirebaseCredential(): IosAuthProviderLaunchPayload

    suspend fun restoreKakaoCredential(): IosAuthProviderLaunchPayload

    suspend fun launch(providerName: String): IosAuthProviderLaunchPayload

    suspend fun signOut(providerName: String)

    suspend fun unlink(providerName: String)
}

data class IosAuthProviderLaunchPayload(
    val providerName: String,
    val token: String = "",
    val errorCode: String? = null,
)

private var iosAuthProviderBridge: IosAuthProviderBridge? = null

fun registerIosAuthProviderBridge(bridge: IosAuthProviderBridge?) {
    iosAuthProviderBridge = bridge
}

internal fun getIosAuthProviderBridge(): IosAuthProviderBridge? = iosAuthProviderBridge
