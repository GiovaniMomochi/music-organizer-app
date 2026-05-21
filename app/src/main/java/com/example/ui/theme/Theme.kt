package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = IndigoGlow,
    primaryContainer = IndigoDust,
    secondary = VioletHighlight,
    background = SpaceBackground,
    surface = SpaceSurface,
    surfaceVariant = SpaceSurfaceVariant,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = TextSecondary,
    error = ErrorGlow,
    errorContainer = ErrorContainer,
    onError = TextPrimary,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
