package com.example.gamelock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GameLockColorScheme = darkColorScheme(
    primary          = AccentCold,
    onPrimary        = TextPrimary,
    primaryContainer = Violet20,
    onPrimaryContainer = AccentLight,

    secondary        = Violet60,
    onSecondary      = TextPrimary,

    background       = DarkBg,
    onBackground     = TextPrimary,

    surface          = DarkSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = DarkCard,
    onSurfaceVariant = TextSecondary,

    tertiary         = AccentLight,
    onTertiary       = DarkBg
)

@Composable
fun GameLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GameLockColorScheme,
        typography  = Typography,
        content     = content
    )
}