package com.crazyenough.unknown

import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools

actual fun initializeKoin() {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin {
            modules(appKoinModules)
        }
    }
}
