package com.example.gamelock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GameLockColorScheme = darkColorScheme(
    primary          = AccentPrimary,
    onPrimary        = TextPrimary,
    primaryContainer = VioletDeep,
    onPrimaryContainer = AccentNeon,

    secondary        = VioletMedium,
    onSecondary      = TextPrimary,

    tertiary         = AccentNeon,
    onTertiary       = DarkBg,

    background       = DarkBg,
    onBackground     = TextPrimary,

    surface          = DarkSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = DarkCard,
    onSurfaceVariant = TextSecondary,

    outline          = CardBorder,
    outlineVariant   = VioletDeep,

    error            = Color(0xFFE11D48),
    onError          = TextPrimary
)

@Composable
fun GameLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GameLockColorScheme,
        typography  = Typography,
        content     = content
    )
}
