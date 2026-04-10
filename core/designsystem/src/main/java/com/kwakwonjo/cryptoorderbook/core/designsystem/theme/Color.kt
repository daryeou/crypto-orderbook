package com.kwakwonjo.cryptoorderbook.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB08CFF),
    onPrimary = Color(0xFF1F1F22),
    background = Color(0xFF141416),
    onBackground = Color(0xFFF1F1F1),
    surface = Color(0xFF1F1F22),
    onSurface = Color(0xFFF1F1F1),
    surfaceVariant = Color(0xFF2C2C30),
    onSurfaceVariant = Color(0xFF88888F),
    error = Color(0xFFE84855),
    outline = Color(0xFF333338),
)

data class LocalExtensionColors(
    val tradeUpRed: Color,
    val tradeDownGreen: Color
)

internal val DarkLocalExtensionColors = LocalExtensionColors(
    tradeUpRed = Color(0xFFE84855),
    tradeDownGreen = Color(0xFF2DC496)
)