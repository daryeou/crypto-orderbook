package com.kwakwonjo.cryptoorderbook.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        letterSpacing = (-0.5).sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
    ),
)
