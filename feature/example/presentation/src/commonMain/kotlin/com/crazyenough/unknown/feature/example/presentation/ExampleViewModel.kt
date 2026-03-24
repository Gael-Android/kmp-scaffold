package com.crazyenough.unknown.feature.example.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

class ExampleViewModel : ViewModel() {

    private val _stateFlow = MutableStateFlow(ExampleState())
    val stateFlow = _stateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = _stateFlow.value,
    )

    private val eventChannel = Channel<ExampleEvent>(Channel.BUFFERED)
    val eventFlow = eventChannel.receiveAsFlow()

    @Suppress("UNUSED_PARAMETER")
    fun onAction(action: ExampleAction) {
        Unit
    }
}
