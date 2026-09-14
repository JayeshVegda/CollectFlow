package com.jayesh.cashcollect.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NothingColorScheme = darkColorScheme(
    primary = NothingRed,
    onPrimary = Color.White,
    primaryContainer = NothingRedBg,
    onPrimaryContainer = NothingWhite,
    secondary = NothingAmber,
    onSecondary = Color.Black,
    secondaryContainer = NothingAmberBg,
    onSecondaryContainer = NothingAmber,
    tertiary = NothingGreen,
    onTertiary = Color.Black,
    background = NothingBlack,
    onBackground = NothingWhite,
    surface = NothingBlack,
    onSurface = NothingWhite,
    surfaceVariant = NothingCard,
    onSurfaceVariant = NothingGray,
    outline = NothingBorder,
    outlineVariant = Color(0xFF23252B)
)

@Composable
fun CashCollectTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NothingColorScheme,
        typography = Typography,
        content = content
    )
}
