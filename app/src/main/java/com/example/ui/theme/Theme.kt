package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val displayName: String) {
    DARK("Modo Oscuro (Grafito)"),
    AMOLED("Negro Puro AMOLED"),
    LIGHT("Modo Claro"),
    SYSTEM("Seguir el Sistema")
}

enum class AccentColorChoice(val displayName: String, val color: Color) {
    TURQUOISE("Turquesa MeloTune", AccentTurquoise),
    COBALT("Azul Océano", AccentCobalt),
    PURPLE("Violeta Neón", AccentPurple),
    EMERALD("Esmeralda", AccentEmerald),
    AMBER("Ámbar", AccentAmber),
    RED("Rojo Rubí", Color(0xFFEF4444))
}

enum class BackgroundGradientChoice(val displayName: String, val colors: List<Color>?) {
    NONE("Color Sólido", null),
    TURQUOISE("Degradado Turquesa", listOf(Color(0xFF00382E), Color(0xFF05070B))),
    BLUE("Degradado Azul Océano", listOf(Color(0xFF0F172A), Color(0xFF030712))),
    PURPLE("Degradado Violeta", listOf(Color(0xFF2E1065), Color(0xFF090315))),
    RED("Degradado Rubí", listOf(Color(0xFF450A0A), Color(0xFF0A0202)))
}

enum class ControlSizeChoice(val displayName: String, val playButtonDp: Int, val iconButtonDp: Int) {
    COMPACT("Compacto", 56, 42),
    NORMAL("Estándar", 64, 48),
    LARGE("Grande", 74, 54)
}

fun createMeloDarkColorScheme(accent: Color = MeloTurquoise, isAmoled: Boolean = false) = darkColorScheme(
    primary = accent,
    onPrimary = Color(0xFF000000),
    primaryContainer = accent.copy(alpha = 0.25f),
    onPrimaryContainer = Color(0xFFE6FFFA),

    secondary = Color(0xFFA855F7),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF3B0764),
    onSecondaryContainer = Color(0xFFF3E8FF),

    tertiary = AccentAmber,
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),

    background = if (isAmoled) Color(0xFF000000) else MeloBgDark,
    onBackground = TextPrimaryDark,

    surface = if (isAmoled) Color(0xFF080808) else MeloSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = if (isAmoled) Color(0xFF121212) else MeloSurfaceElevatedDark,
    onSurfaceVariant = TextSecondaryDark,

    outline = if (isAmoled) Color(0xFF1E1E1E) else MeloSurfaceBorderDark,
    outlineVariant = if (isAmoled) Color(0xFF282828) else MeloSurfaceHighlightDark,

    error = StudioCrimson,
    onError = Color.White
)

fun createMeloLightColorScheme(accent: Color = MeloTurquoise) = lightColorScheme(
    primary = accent,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = accent.copy(alpha = 0.15f),
    onPrimaryContainer = Color(0xFF00382E),

    secondary = Color(0xFF8B5CF6),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF3B0764),

    tertiary = AccentAmber,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),

    background = MeloBgLight,
    onBackground = TextPrimaryLight,

    surface = MeloSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = MeloSurfaceElevatedLight,
    onSurfaceVariant = TextSecondaryLight,

    outline = MeloSurfaceBorderLight,
    outlineVariant = MeloSurfaceHighlightLight,

    error = StudioCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    accentColorChoice: AccentColorChoice = AccentColorChoice.TURQUOISE,
    content: @Composable () -> Unit
) {
    val isAmoled = themeMode == AppThemeMode.AMOLED
    val isDark = when (themeMode) {
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) {
        createMeloDarkColorScheme(accentColorChoice.color, isAmoled)
    } else {
        createMeloLightColorScheme(accentColorChoice.color)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
