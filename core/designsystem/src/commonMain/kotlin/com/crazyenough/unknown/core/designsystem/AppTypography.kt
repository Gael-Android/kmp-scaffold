package com.crazyenough.unknown.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

private val BaseTypography = Typography()

@Immutable
data class AppTypographyTokens(
    val display1: TextStyle,
    val display2: TextStyle,
    val display3: TextStyle,
    val heading1: TextStyle,
    val heading2: TextStyle,
    val heading3: TextStyle,
    val heading4: TextStyle,
    val heading5: TextStyle,
    val heading6: TextStyle,
    val body1: TextStyle,
    val body2: TextStyle,
    val body3: TextStyle,
    val body4: TextStyle,
    val body5: TextStyle,
    val body6: TextStyle,
    val caption1: TextStyle,
    val caption2: TextStyle,
    val caption3: TextStyle,
    val caption4: TextStyle,
    val label1: TextStyle,
    val label2: TextStyle,
    val label3: TextStyle,
    val label4: TextStyle,
    val label5: TextStyle,
    val onboardingDescription: TextStyle,
    val onboardingTitle: TextStyle,
)

object AppTextStyles {
    // 짧고 중요한 텍스트 · 숫자에 사용하며, 시선을 집중하는 요소.
    fun display1(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 48.sp,
        letterSpacing = 1.sp,
    )

    // 짧고 중요한 텍스트 · 숫자에 사용하며, 시선을 집중하는 요소.
    fun display2(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 44.sp,
        letterSpacing = 1.sp,
    )

    // 짧고 중요한 텍스트 · 숫자에 사용하며, 시선을 집중하는 요소.
    fun display3(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 40.sp,
        letterSpacing = 1.sp,
    )

    // 콘텐츠 제목, 페이지 내 가장 중요한 정보에 사용.
    fun heading1(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    )

    // 콘텐츠 제목, 페이지 내 가장 중요한 정보에 사용.
    fun heading2(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    )

    // 콘텐츠 제목, 페이지 내 가장 중요한 정보에 사용.
    fun heading3(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    )

    // 콘텐츠 및 카테고리 제목, 작은 섹션의 텍스트에 사용.
    fun heading4(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    )

    // 콘텐츠 및 카테고리 제목, 작은 섹션의 텍스트에 사용.
    fun heading5(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    )

    // 콘텐츠 및 카테고리 제목, 작은 섹션의 텍스트에 사용.
    fun heading6(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    )

    // 콘텐츠 부제목과 긴 텍스트 본문에 사용.
    fun body1(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    )

    // 콘텐츠 부제목과 긴 텍스트 본문에 사용.
    fun body2(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    )

    // 긴 텍스트 본문에 사용.
    fun body3(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    )

    // 긴 텍스트 본문에 사용.
    fun body4(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    )

    // 긴 텍스트 본문에 사용.
    fun body5(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    )

    // 긴 텍스트 본문에 사용.
    fun body6(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    )

    // 부가 안내, 추가 상황, 시각 자료 설명, 메뉴에 사용.
    fun caption1(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    )

    // 부가 안내, 추가 상황, 시각 자료 설명, 메뉴에 사용.
    fun caption2(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    )

    // 부가 안내, 추가 상황, 시각 자료 설명, 메뉴에 사용.
    fun caption3(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    )

    // 부가 안내, 추가 상황, 시각 자료 설명, 메뉴에 사용.
    fun caption4(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    )

    // 버튼, 탭, 리스트, 메뉴, 토스트, 뱃지에 사용.
    fun label1(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    )

    // 버튼, 탭, 리스트, 메뉴, 토스트, 뱃지에 사용.
    fun label2(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    )

    // 버튼, 탭, 리스트, 메뉴, 토스트, 뱃지에 사용.
    fun label3(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    )

    // 버튼, 탭, 리스트, 메뉴, 토스트, 뱃지에 사용.
    fun label4(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    )

    // 버튼, 탭, 리스트, 메뉴, 토스트, 뱃지에 사용.
    fun label5(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    )

    fun onboardingDescription(fontFamily: FontFamily): TextStyle = body5(fontFamily = fontFamily).copy(
        lineHeight = 22.4.sp,
        textAlign = TextAlign.Center,
    )

    fun onboardingTitle(fontFamily: FontFamily): TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.8.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun rememberAppTypography(): Typography {
    val pretendardFontFamily = rememberAppFontFamily()
    val a2zFontFamily = rememberA2zFontFamily()
    return remember(pretendardFontFamily, a2zFontFamily) {
        Typography(
            displayLarge = AppTextStyles.display1(fontFamily = a2zFontFamily),
            displayMedium = AppTextStyles.display2(fontFamily = a2zFontFamily),
            displaySmall = AppTextStyles.display3(fontFamily = a2zFontFamily),
            headlineLarge = AppTextStyles.heading1(fontFamily = pretendardFontFamily),
            headlineMedium = AppTextStyles.heading3(fontFamily = pretendardFontFamily),
            headlineSmall = AppTextStyles.heading5(fontFamily = pretendardFontFamily),
            titleLarge = BaseTypography.titleLarge.withAppFont(pretendardFontFamily),
            titleMedium = BaseTypography.titleMedium.withAppFont(pretendardFontFamily),
            titleSmall = BaseTypography.titleSmall.withAppFont(pretendardFontFamily),
            bodyLarge = AppTextStyles.body1(fontFamily = pretendardFontFamily),
            bodyMedium = AppTextStyles.body3(fontFamily = pretendardFontFamily),
            bodySmall = AppTextStyles.body5(fontFamily = pretendardFontFamily),
            labelLarge = BaseTypography.labelLarge.withAppFont(pretendardFontFamily),
            labelMedium = BaseTypography.labelMedium.withAppFont(pretendardFontFamily),
            labelSmall = BaseTypography.labelSmall.withAppFont(pretendardFontFamily),
        )
    }
}

@Composable
fun rememberAppTypographyTokens(): AppTypographyTokens {
    val pretendardFontFamily = rememberAppFontFamily()
    val a2zFontFamily = rememberA2zFontFamily()
    return remember(pretendardFontFamily, a2zFontFamily) {
        AppTypographyTokens(
            display1 = AppTextStyles.display1(fontFamily = a2zFontFamily),
            display2 = AppTextStyles.display2(fontFamily = a2zFontFamily),
            display3 = AppTextStyles.display3(fontFamily = a2zFontFamily),
            heading1 = AppTextStyles.heading1(fontFamily = pretendardFontFamily),
            heading2 = AppTextStyles.heading2(fontFamily = pretendardFontFamily),
            heading3 = AppTextStyles.heading3(fontFamily = pretendardFontFamily),
            heading4 = AppTextStyles.heading4(fontFamily = pretendardFontFamily),
            heading5 = AppTextStyles.heading5(fontFamily = pretendardFontFamily),
            heading6 = AppTextStyles.heading6(fontFamily = pretendardFontFamily),
            body1 = AppTextStyles.body1(fontFamily = pretendardFontFamily),
            body2 = AppTextStyles.body2(fontFamily = pretendardFontFamily),
            body3 = AppTextStyles.body3(fontFamily = pretendardFontFamily),
            body4 = AppTextStyles.body4(fontFamily = pretendardFontFamily),
            body5 = AppTextStyles.body5(fontFamily = pretendardFontFamily),
            body6 = AppTextStyles.body6(fontFamily = pretendardFontFamily),
            caption1 = AppTextStyles.caption1(fontFamily = pretendardFontFamily),
            caption2 = AppTextStyles.caption2(fontFamily = pretendardFontFamily),
            caption3 = AppTextStyles.caption3(fontFamily = pretendardFontFamily),
            caption4 = AppTextStyles.caption4(fontFamily = pretendardFontFamily),
            label1 = AppTextStyles.label1(fontFamily = pretendardFontFamily),
            label2 = AppTextStyles.label2(fontFamily = pretendardFontFamily),
            label3 = AppTextStyles.label3(fontFamily = pretendardFontFamily),
            label4 = AppTextStyles.label4(fontFamily = pretendardFontFamily),
            label5 = AppTextStyles.label5(fontFamily = pretendardFontFamily),
            onboardingDescription = AppTextStyles.onboardingDescription(fontFamily = pretendardFontFamily),
            onboardingTitle = AppTextStyles.onboardingTitle(fontFamily = a2zFontFamily),
        )
    }
}

internal val LocalAppTypography = staticCompositionLocalOf<AppTypographyTokens> {
    error("AppTypographyTokens not provided")
}

val appTypography: AppTypographyTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalAppTypography.current

private fun TextStyle.withAppFont(fontFamily: FontFamily): TextStyle = copy(
    fontFamily = fontFamily,
)
