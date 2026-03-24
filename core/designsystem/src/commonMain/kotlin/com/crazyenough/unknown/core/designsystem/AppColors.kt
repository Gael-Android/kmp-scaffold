package com.crazyenough.unknown.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColorTokens(
    val primary50: Color = Color(0xFFE9F5FF),
    val primary100: Color = Color(0xFFD2ECFF),
    val primary200: Color = Color(0xFFA5D9FF),
    val primary300: Color = Color(0xFF79C6FF),
    val primary400: Color = Color(0xFF4CB3FF),
    val primary500: Color = Color(0xFF1FA0FF),
    val primary600: Color = Color(0xFF1980CC),
    val primary700: Color = Color(0xFF136099),
    val primary800: Color = Color(0xFF0C4066),
    val primary900: Color = Color(0xFF062033),
    val error50: Color = Color(0xFFFDECEC),
    val error100: Color = Color(0xFFFCDADA),
    val error200: Color = Color(0xFFF9B4B4),
    val error300: Color = Color(0xFFF58F8F),
    val error400: Color = Color(0xFFF26969),
    val error500: Color = Color(0xFFEF4444),
    val error600: Color = Color(0xFFC13737),
    val error700: Color = Color(0xFF932929),
    val error800: Color = Color(0xFF661C1C),
    val error900: Color = Color(0xFF4F1515),
    val warning50: Color = Color(0xFFFEFAE8),
    val warning100: Color = Color(0xFFFEF5D0),
    val warning200: Color = Color(0xFFFDEBA1),
    val warning300: Color = Color(0xFFFCE073),
    val warning400: Color = Color(0xFFFBD644),
    val warning500: Color = Color(0xFFFACC15),
    val warning600: Color = Color(0xFFC8A311),
    val warning700: Color = Color(0xFF967A0D),
    val warning800: Color = Color(0xFF645208),
    val warning900: Color = Color(0xFF322904),
    val success50: Color = Color(0xFFE9F9EF),
    val success100: Color = Color(0xFFD3F3DF),
    val success200: Color = Color(0xFFA7E8BF),
    val success300: Color = Color(0xFF7ADC9E),
    val success400: Color = Color(0xFF4ED17E),
    val success500: Color = Color(0xFF22C55E),
    val success600: Color = Color(0xFF1B9E4B),
    val success700: Color = Color(0xFF147638),
    val success800: Color = Color(0xFF0E4F26),
    val success900: Color = Color(0xFF0A3B1C),
    val information50: Color = Color(0xFFEBF2FE),
    val information100: Color = Color(0xFFD8E6FD),
    val information200: Color = Color(0xFFB1CDFB),
    val information300: Color = Color(0xFF89B4FA),
    val information400: Color = Color(0xFF629BF8),
    val information500: Color = Color(0xFF3B82F6),
    val information600: Color = Color(0xFF2F69C7),
    val information700: Color = Color(0xFF235097),
    val information800: Color = Color(0xFF183668),
    val information900: Color = Color(0xFF122A50),
    val gray50: Color = Color(0xFFF9F9F9),
    val gray100: Color = Color(0xFFF2F2F2),
    val gray200: Color = Color(0xFFE5E5E5),
    val gray300: Color = Color(0xFFD4D4D4),
    val gray400: Color = Color(0xFFA3A3A3),
    val gray500: Color = Color(0xFF737373),
    val gray600: Color = Color(0xFF525252),
    val gray700: Color = Color(0xFF404040),
    val gray800: Color = Color(0xFF262626),
    val gray900: Color = Color(0xFF171717),
    val white: Color = Color(0xFFFFFFFF),
    val black: Color = Color(0xFF000000),
    val backgroundBlue: Color = Color(0xFFF9FCFF),
    val backgroundGray: Color = Color(0xFFFCFCFC),
) {
    // 배경 색상
    val backgroundCanvas: Color
        get() = backgroundGray

    // 기본 카드 배경
    val cardBackground: Color
        get() = white

    // 블루 톤 배경
    val backgroundAccent: Color
        get() = backgroundBlue

    // 아주 연한 박스 테두리, 구분선
    val dividerSubtle: Color
        get() = gray100

    // 비활성 요소
    val disabled: Color
        get() = gray200

    // 버튼 비활성화(칠)
    val buttonDisabledContainer: Color
        get() = gray200

    // 버튼 활성화(선), 입력창 기본값(선)
    val controlStrokeDefault: Color
        get() = gray300

    // 중요도가 낮은 아이콘
    val supportGraphic: Color
        get() = gray400

    // 플레이스홀더
    val placeholder: Color
        get() = gray400

    // 중요도가 낮은 아이콘
    val iconSubtle: Color
        get() = gray400

    // 브랜드 핵심 컬러, 버튼 기본값(칠), 입력창에 입력 시(선)
    val brandPrimary: Color
        get() = primary500

    // 활성화(클릭) 상태
    val interactionActive: Color
        get() = primary600

    // 캡션이나 작게 들어가는 설명글
    val supportingText: Color
        get() = gray600

    // 버튼 호버(칠)
    val buttonHoverContainer: Color
        get() = primary600

    // 가장 안정적인 글자색
    val bodyTextPrimary: Color
        get() = gray700

    // 버튼 활성화(칠)
    val buttonActiveContainer: Color
        get() = primary700

    // 강조가 필요한 텍스트
    val headingText: Color
        get() = gray800

    // 대제목
    val displayText: Color
        get() = gray900

    // 확실한 콘텐츠 구분선
    val dividerStrong: Color
        get() = gray300

    // 읽을 필요가 적은 부가 정보
    val tertiaryText: Color
        get() = gray500

    // 시스템 오류 : 잘못된 정보 입력 등
    val systemError: Color
        get() = error500

    // 부정적이며 되돌릴 수 없는 액션 : 삭제 · 중지 등
    val destructiveAction: Color
        get() = error600

    // 주가 상승 · 투표 반대 등
    val marketRiseOrVoteAgainst: Color
        get() = error500

    // 사용자의 주의가 필요한 경우
    val caution: Color
        get() = warning500

    // 작업이 성공적으로 수행된 경우, 투표 찬성 등
    val success: Color
        get() = success500

    // 안내 메시지 제공 : 사용자에게 중요한 정보 · 변경 사항 등, 주가 하락 등
    val information: Color
        get() = information500
}

internal val LightAppColorTokens = AppColorTokens()

internal val LocalAppColorTokens = staticCompositionLocalOf { LightAppColorTokens }

internal fun appLightColorScheme(
    tokens: AppColorTokens = LightAppColorTokens,
): ColorScheme = lightColorScheme(
    primary = tokens.brandPrimary,
    onPrimary = Color.White,
    primaryContainer = tokens.backgroundAccent,
    onPrimaryContainer = tokens.displayText,
    secondary = tokens.interactionActive,
    onSecondary = tokens.displayText,
    secondaryContainer = tokens.primary50,
    onSecondaryContainer = tokens.displayText,
    tertiary = tokens.interactionActive,
    onTertiary = Color.White,
    tertiaryContainer = tokens.disabled,
    onTertiaryContainer = tokens.displayText,
    background = tokens.backgroundCanvas,
    onBackground = tokens.bodyTextPrimary,
    surface = tokens.cardBackground,
    onSurface = tokens.bodyTextPrimary,
    surfaceVariant = tokens.cardBackground,
    onSurfaceVariant = tokens.headingText,
    outline = tokens.dividerStrong,
    outlineVariant = tokens.dividerSubtle,
    inverseSurface = tokens.displayText,
    inverseOnSurface = tokens.backgroundCanvas,
    inversePrimary = tokens.primary300,
    error = tokens.systemError,
    onError = Color.White,
    errorContainer = tokens.error100,
    onErrorContainer = tokens.error900,
    surfaceTint = tokens.brandPrimary,
    scrim = tokens.displayText,
)

val appColors: AppColorTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColorTokens.current
