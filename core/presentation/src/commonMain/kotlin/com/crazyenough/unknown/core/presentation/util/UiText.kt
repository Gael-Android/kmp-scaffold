// UI 레이어에서 문자열 리소스와 동적 문자열을 공통으로 다루기 위한 유틸 유니온 타입.
// Composable 환경에서는 `asString()`으로 동기 변환하고, suspend 컨텍스트에서는
// `asStringAsync()`로 비동기 변환할 수 있도록 한다.

package com.crazyenough.unknown.core.presentation.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    class Resource(
        val id: StringResource,
        val args: Array<Any> = arrayOf()
    ) : UiText

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is Resource -> stringResource(
                resource = id,
                *args
            )
        }
    }

    suspend fun asStringAsync(): String {
        return when (this) {
            is DynamicString -> value
            is Resource -> getString(
                resource = id,
                *args
            )
        }
    }
}
