package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FeelkarDarkColorScheme = darkColorScheme(
    primary = FeelkarCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D1323),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = FeelkarViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF281A45),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = FeelkarAmber,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val FeelkarLightColorScheme = lightColorScheme(
    primary = FeelkarCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5EC),
    onPrimaryContainer = Color(0xFF4A071A),
    secondary = FeelkarViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEE6FF),
    onSecondaryContainer = Color(0xFF280D59),
    tertiary = FeelkarAmber,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

val FeelkarShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

enum class ThemePreference { SYSTEM, LIGHT, DARK }

@Composable
fun FeelkarTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val colorScheme = if (darkTheme) FeelkarDarkColorScheme else FeelkarLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FeelkarTypography,
        shapes = FeelkarShapes,
        content = content
    )
}
