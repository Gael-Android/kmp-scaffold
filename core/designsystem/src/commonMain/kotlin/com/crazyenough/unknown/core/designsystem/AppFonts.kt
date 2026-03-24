package com.crazyenough.unknown.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import resources.generated.resources.Res
import resources.generated.resources.a2z_black
import resources.generated.resources.a2z_bold
import resources.generated.resources.a2z_extrabold
import resources.generated.resources.a2z_extralight
import resources.generated.resources.a2z_light
import resources.generated.resources.a2z_medium
import resources.generated.resources.a2z_regular
import resources.generated.resources.a2z_semibold
import resources.generated.resources.a2z_thin
import resources.generated.resources.pretendard_bold
import resources.generated.resources.pretendard_medium
import resources.generated.resources.pretendard_regular
import resources.generated.resources.pretendard_semibold

@Composable
fun rememberAppFontFamily(): FontFamily = FontFamily(
    Font(
        resource = Res.font.pretendard_regular,
        weight = FontWeight.Normal,
    ),
    Font(
        resource = Res.font.pretendard_medium,
        weight = FontWeight.Medium,
    ),
    Font(
        resource = Res.font.pretendard_semibold,
        weight = FontWeight.SemiBold,
    ),
    Font(
        resource = Res.font.pretendard_bold,
        weight = FontWeight.Bold,
    ),
)

@Composable
fun rememberA2zFontFamily(): FontFamily = FontFamily(
    Font(
        resource = Res.font.a2z_thin,
        weight = FontWeight.Thin,
    ),
    Font(
        resource = Res.font.a2z_extralight,
        weight = FontWeight.ExtraLight,
    ),
    Font(
        resource = Res.font.a2z_light,
        weight = FontWeight.Light,
    ),
    Font(
        resource = Res.font.a2z_regular,
        weight = FontWeight.Normal,
    ),
    Font(
        resource = Res.font.a2z_medium,
        weight = FontWeight.Medium,
    ),
    Font(
        resource = Res.font.a2z_semibold,
        weight = FontWeight.SemiBold,
    ),
    Font(
        resource = Res.font.a2z_bold,
        weight = FontWeight.Bold,
    ),
    Font(
        resource = Res.font.a2z_extrabold,
        weight = FontWeight.ExtraBold,
    ),
    Font(
        resource = Res.font.a2z_black,
        weight = FontWeight.Black,
    ),
)
