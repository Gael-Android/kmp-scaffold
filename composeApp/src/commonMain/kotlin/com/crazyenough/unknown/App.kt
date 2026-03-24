package com.crazyenough.unknown

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.crazyenough.unknown.core.designsystem.AppTheme
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.crazyenough.unknown.feature.auth.presentation.navigation.AuthNavigation
import com.crazyenough.unknown.navigation.Navigator
import com.crazyenough.unknown.navigation.rememberNavigationState
import com.crazyenough.unknown.navigation.toEntries
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Auth : Route
}

val serializersConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.Auth::class, Route.Auth.serializer())
        }
    }
}

@Composable
@Preview
fun App() {
    AppTheme {
        val topLevelRoutes: Set<NavKey> = setOf(Route.Auth)
        val navigationState = rememberNavigationState(
            startRoute = Route.Auth,
            topLevelRoutes = topLevelRoutes,
            configuration = serializersConfig,
        )
        val navigator = remember {
            Navigator(navigationState)
        }

        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            onBack = navigator::goBack,
            entries = navigationState.toEntries(
                entryProvider {
                    entry<Route.Auth> {
                        AuthNavigation(
                            onLoginSuccess = {},
                        )
                    }
                },
            ),
        )
    }
}
