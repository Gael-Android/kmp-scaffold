package com.crazyenough.unknown

import com.crazyenough.unknown.feature.auth.data.engine.IosAuthNetworkBridge
import com.crazyenough.unknown.feature.auth.data.engine.registerIosAuthNetworkBridge
import com.crazyenough.unknown.feature.auth.presentation.login.IosAuthProviderBridge
import com.crazyenough.unknown.feature.auth.presentation.login.registerIosAuthProviderBridge

fun registerIosAuthBridges(
    providerBridge: IosAuthProviderBridge?,
    networkBridge: IosAuthNetworkBridge?,
) {
    registerIosAuthProviderBridge(providerBridge)
    registerIosAuthNetworkBridge(networkBridge)
}
