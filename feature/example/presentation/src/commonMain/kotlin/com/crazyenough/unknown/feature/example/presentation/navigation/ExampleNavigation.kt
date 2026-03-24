package com.crazyenough.unknown.feature.example.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.crazyenough.unknown.feature.example.presentation.ExampleRoute
import com.crazyenough.unknown.navigation.rememberNavigationState
import com.crazyenough.unknown.navigation.toEntries
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.koinViewModel

@Serializable
internal sealed interface ExampleNavigationRoute : NavKey {
    @Serializable
    data object Example : ExampleNavigationRoute
}

private val exampleSerializersConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(
                ExampleNavigationRoute.Example::class,
                ExampleNavigationRoute.Example.serializer(),
            )
        }
    }
}

@Composable
fun ExampleNavigation(
    modifier: Modifier = Modifier,
) {
    val navigationState = rememberNavigationState(
        startRoute = ExampleNavigationRoute.Example,
        topLevelRoutes = setOf(ExampleNavigationRoute.Example),
        configuration = exampleSerializersConfig,
    )

    val entries = navigationState.toEntries(
        entryProvider = entryProvider {
            entry<ExampleNavigationRoute.Example> {
                ExampleRoute(
                    viewModel = koinViewModel(),
                )
            }
        },
    )

    NavDisplay(
        modifier = modifier,
        onBack = {},
        entries = entries,
    )
}
