package com.kwakwonjo.cryptoorderbook.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalColors: LocalExtensionColors
    @Composable
    get() = LocalColorScheme.current

private val LocalColorScheme = staticCompositionLocalOf { DarkLocalExtensionColors }

@Composable
fun CryptoAppTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
    ) {
        CompositionLocalProvider(
            LocalColorScheme provides DarkLocalExtensionColors,
        ) {
            content()
        }
    }
}
