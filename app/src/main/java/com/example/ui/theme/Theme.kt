package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GamingColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = NeonPurpleGlow,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = NeonCyanGlow,
    tertiary = NeonPink,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = NeonCardBorder,
    error = NeonRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = GamingColorScheme,
        typography = Typography,
        content = content
    )
}
