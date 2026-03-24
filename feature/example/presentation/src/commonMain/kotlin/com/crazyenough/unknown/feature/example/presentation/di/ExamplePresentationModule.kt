package com.crazyenough.unknown.feature.example.presentation.di

import com.crazyenough.unknown.feature.example.presentation.ExampleViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val examplePresentationModule: Module = module {
    viewModelOf(::ExampleViewModel)
}
