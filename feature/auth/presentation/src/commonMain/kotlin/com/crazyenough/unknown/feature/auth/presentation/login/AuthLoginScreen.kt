package com.crazyenough.unknown.feature.auth.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crazyenough.unknown.feature.auth.domain.model.AuthProvider

private val AuthBackgroundTop = Color(0xFFF9FAFF)
private val AuthBackgroundBottom = Color(0xFFF4F0E7)
private val GoogleButtonColor = Color(0xFF1F1F1F)
private val KakaoButtonColor = Color(0xFFFEE500)
private val KakaoButtonTextColor = Color(0xFF191919)
private val AppleButtonColor = Color(0xFFFFFFFF)
private val AppleButtonTextColor = Color(0xFF111111)
private val ErrorContainerColor = Color(0xFFFFF1F0)
private val ErrorTextColor = Color(0xFFB42318)
private val SessionContainerColor = Color(0xFFF0F9FF)
private val SessionLabelColor = Color(0xFF0C4A6E)
private val SessionBodyColor = Color(0xFF075985)

@Composable
fun AuthLoginScreen(
    state: AuthLoginState,
    onAction: (AuthLoginAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AuthBackgroundTop, AuthBackgroundBottom),
                ),
            )
            .testTag(AuthLoginTestTags.SCREEN),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Unknown",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "계정을 연결하고\n투자 여정을 이어가세요",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = Color(0xFF475467),
            )

            state.loggedInAccount?.let { account ->
                Spacer(modifier = Modifier.height(24.dp))
                LoggedInAccountCard(account = account)
            }

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(20.dp))
                ErrorMessageCard(
                    message = state.errorMessage,
                    onDismiss = { onAction(AuthLoginAction.OnDismissError) },
                )
            } else {
                Spacer(
                    modifier = Modifier.height(
                        if (state.loggedInAccount != null) 24.dp else 40.dp,
                    ),
                )
            }

            val visibleProviders = rememberVisibleProviders(state)
            visibleProviders.forEachIndexed { index, provider ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                when (provider) {
                    AuthProvider.GOOGLE -> SocialLoginButton(
                        label = "Google로 계속하기",
                        backgroundColor = GoogleButtonColor,
                        contentColor = Color.White,
                        enabled = state.isGoogleEnabled,
                        testTag = AuthLoginTestTags.GOOGLE_BUTTON,
                        onClick = { onAction(AuthLoginAction.OnGoogleClick) },
                    )

                    AuthProvider.KAKAO -> SocialLoginButton(
                        label = "카카오로 계속하기",
                        backgroundColor = KakaoButtonColor,
                        contentColor = KakaoButtonTextColor,
                        enabled = state.isKakaoEnabled,
                        testTag = AuthLoginTestTags.KAKAO_BUTTON,
                        onClick = { onAction(AuthLoginAction.OnKakaoClick) },
                    )

                    AuthProvider.APPLE -> SocialLoginButton(
                        label = "Apple로 계속하기",
                        backgroundColor = AppleButtonColor,
                        contentColor = AppleButtonTextColor,
                        enabled = state.isAppleEnabled,
                        testTag = AuthLoginTestTags.APPLE_BUTTON,
                        onClick = { onAction(AuthLoginAction.OnAppleClick) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = buildSupportText(state),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF667085),
                textAlign = TextAlign.Center,
            )
        }

        // 개발용 로그아웃/탈퇴 버튼
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (state.isLoading) 80.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val devProviders = rememberVisibleProviders(state)
            if (devProviders.isNotEmpty()) {
                DevLocalStatusText(
                    label = "[DEV] Firebase currentUser",
                    value = state.devFirebaseCurrentUserExists,
                    testTag = AuthLoginTestTags.DEV_FIREBASE_STATUS,
                )
                DevLocalStatusText(
                    label = "[DEV] Kakao local token",
                    value = state.devKakaoTokenExists,
                    testTag = AuthLoginTestTags.DEV_KAKAO_STATUS,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    devProviders.forEach { provider ->
                        DevActionButton(
                            label = "[DEV] ${provider.displayName()} 로그아웃",
                            color = Color.Gray,
                            onClick = {
                                onAction(
                                    when (provider) {
                                        AuthProvider.GOOGLE -> AuthLoginAction.OnDevGoogleSignOut
                                        AuthProvider.KAKAO -> AuthLoginAction.OnDevKakaoSignOut
                                        AuthProvider.APPLE -> AuthLoginAction.OnDevAppleSignOut
                                    },
                                )
                            },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    devProviders.forEach { provider ->
                        DevActionButton(
                            label = "[DEV] ${provider.displayName()} 탈퇴",
                            color = Color(0xFFB42318),
                            onClick = {
                                onAction(
                                    when (provider) {
                                        AuthProvider.GOOGLE -> AuthLoginAction.OnDevGoogleUnlink
                                        AuthProvider.KAKAO -> AuthLoginAction.OnDevKakaoUnlink
                                        AuthProvider.APPLE -> AuthLoginAction.OnDevAppleUnlink
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .testTag(AuthLoginTestTags.LOADING),
                color = GoogleButtonColor,
            )
        }
    }
}

@Composable
private fun DevLocalStatusText(
    label: String,
    value: Boolean?,
    testTag: String,
) {
    Text(
        text = when (value) {
            true -> "$label: 있음"
            false -> "$label: 없음"
            null -> "$label: 확인 중"
        },
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF475467),
        modifier = Modifier.testTag(testTag),
    )
}

private fun rememberVisibleProviders(state: AuthLoginState): List<AuthProvider> {
    return listOfNotNull(
        AuthProvider.GOOGLE.takeIf { state.isGoogleVisible },
        AuthProvider.KAKAO.takeIf { state.isKakaoVisible },
        AuthProvider.APPLE.takeIf { state.isAppleVisible },
    )
}

@Composable
private fun LoggedInAccountCard(
    account: AuthLoginState.LoggedInAccount,
) {
    val displayName = account.displayName.ifBlank { "이름 없는 사용자" }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AuthLoginTestTags.SESSION_CARD),
        shape = RoundedCornerShape(18.dp),
        color = SessionContainerColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "현재 로그인된 계정",
                style = MaterialTheme.typography.labelLarge,
                color = SessionLabelColor,
            )
            Text(
                text = "${account.provider.displayName()} 계정",
                style = MaterialTheme.typography.titleSmall,
                color = SessionBodyColor,
                modifier = Modifier.testTag(AuthLoginTestTags.SESSION_PROVIDER),
            )
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF0F172A),
                modifier = Modifier.testTag(AuthLoginTestTags.SESSION_NAME),
            )
            Text(
                text = "ID ${account.userId}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475467),
                modifier = Modifier.testTag(AuthLoginTestTags.SESSION_USER_ID),
            )
        }
    }
}

@Composable
private fun ErrorMessageCard(
    message: String,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ErrorContainerColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ErrorTextColor,
                modifier = Modifier.testTag(AuthLoginTestTags.ERROR_MESSAGE),
            )
            Text(
                text = "닫기",
                style = MaterialTheme.typography.labelLarge,
                color = ErrorTextColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.8f))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .align(Alignment.End)
                    .testTag("${AuthLoginTestTags.ERROR_MESSAGE}_dismiss"),
            )
        }
    }
}

@Composable
private fun SocialLoginButton(
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .testTag(testTag),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.35f),
            disabledContentColor = contentColor.copy(alpha = 0.55f),
        ),
        shape = RoundedCornerShape(22.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DevActionButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

private fun buildSupportText(state: AuthLoginState): String {
    val unavailableProviders = buildList {
        if (state.isGoogleVisible && !state.enabledProviders.contains(AuthProvider.GOOGLE)) add("Google")
        if (state.isKakaoVisible && !state.enabledProviders.contains(AuthProvider.KAKAO)) add("카카오")
        if (state.isAppleVisible && !state.enabledProviders.contains(AuthProvider.APPLE)) add("Apple")
    }

    return if (unavailableProviders.isEmpty()) {
        "소셜 로그인 후 앱 계정이 자동으로 연결됩니다."
    } else {
        "${unavailableProviders.joinToString(", ")} 로그인을 사용하려면 앱 설정값을 먼저 확인해 주세요."
    }
}

private fun AuthProvider.displayName(): String {
    return when (this) {
        AuthProvider.GOOGLE -> "Google"
        AuthProvider.KAKAO -> "카카오"
        AuthProvider.APPLE -> "Apple"
    }
}

@Preview
@Composable
private fun AuthLoginScreenPreview() {
    MaterialTheme {
        AuthLoginScreen(
            state = AuthLoginState(),
            onAction = {},
        )
    }
}
