package com.crazyenough.unknown.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val typography = rememberAppTypography()
    val appTypographyTokens = rememberAppTypographyTokens()
    val colors = LightAppColorTokens
    MaterialTheme(
        colorScheme = appLightColorScheme(colors),
        typography = typography,
    ) {
        CompositionLocalProvider(
            LocalAppColorTokens provides colors,
            LocalAppTypography provides appTypographyTokens,
        ) {
            ProvideTextStyle(
                value = MaterialTheme.typography.bodyLarge,
                content = content,
            )
        }
    }
}
