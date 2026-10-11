package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudioColorScheme = darkColorScheme(
    primary = StudioCobalt,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = StudioCobaltMuted,
    onPrimaryContainer = Color(0xFFDBEAFE),

    secondary = StudioIndigo,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFE0E7FF),

    tertiary = StudioAmber,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),

    background = StudioBg,
    onBackground = TextPrimary,

    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondary,

    outline = StudioSurfaceBorder,
    outlineVariant = StudioSurfaceHighlight,

    error = StudioCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioColorScheme,
        typography = Typography,
        content = content
    )
}
