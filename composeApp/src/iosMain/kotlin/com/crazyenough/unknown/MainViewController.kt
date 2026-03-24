package com.crazyenough.unknown

import androidx.compose.ui.window.ComposeUIViewController
import com.crazyenough.unknown.initializeKoin

fun MainViewController() = ComposeUIViewController {
    initializeKoin()
    App()
}
