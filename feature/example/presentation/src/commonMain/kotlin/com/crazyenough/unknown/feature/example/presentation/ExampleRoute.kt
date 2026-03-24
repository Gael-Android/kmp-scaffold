package com.crazyenough.unknown.feature.example.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crazyenough.unknown.core.presentation.util.ObserveAsEvents

@Composable
fun ExampleRoute(
    viewModel: ExampleViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    ObserveAsEvents(
        flow = viewModel.eventFlow,
        key1 = viewModel,
    ) { }

    ExampleScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}
