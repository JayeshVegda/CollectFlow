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
    // Material 3 tints elevated surfaces with `surfaceTint`, which defaults to `primary`.
    // With primary = NothingRed that blended a red wash over every elevated surface — most
    // visibly a dark maroon band across the bottom navigation bar (3dp tonal elevation).
    // The Nothing design system expresses depth with surface steps and 1px edges, never with
    // accent-tinted elevation, so tonal tinting is disabled at the source.
    surfaceTint = NothingBlack,
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
        shapes = AppShapes,
        content = content
    )
}
