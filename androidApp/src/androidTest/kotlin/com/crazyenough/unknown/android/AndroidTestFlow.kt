package com.crazyenough.unknown.android

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

private const val STEP_DELAY_MILLIS = 500L

var enableStepDelay = false

fun waitForStepDelay() {
    if (enableStepDelay) {
        Thread.sleep(STEP_DELAY_MILLIS)
    }
}

fun SemanticsNodeInteraction.performClickWithStepDelay(): SemanticsNodeInteraction {
    waitForStepDelay()
    return this.performClick()
}

fun SemanticsNodeInteraction.performTextInputWithStepDelay(value: String) {
    waitForStepDelay()
    return this.performTextInput(value)
}
