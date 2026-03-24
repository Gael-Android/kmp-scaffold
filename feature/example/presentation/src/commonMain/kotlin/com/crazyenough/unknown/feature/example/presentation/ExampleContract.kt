package com.crazyenough.unknown.feature.example.presentation

data class ExampleState(
    val title: String = "Example Screen",
)

sealed interface ExampleAction

sealed interface ExampleEvent
