package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ImmersiveColorScheme = darkColorScheme(
    primary = ImmersiveAmber,
    secondary = WhatsappGreen,
    tertiary = ImmersiveOrange,
    background = ImmersiveBg,
    surface = ImmersiveSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = ImmersiveTextPrimary,
    onSurface = ImmersiveTextPrimary,
    surfaceVariant = ImmersiveSurfaceVariant,
    onSurfaceVariant = ImmersiveTextSecondary,
    outline = ImmersiveBorder
)

private val PlayfulLightColorScheme = lightColorScheme(
    primary = ComicBlue,
    secondary = WhatsappGreen,
    tertiary = PlayfulYellow,
    background = Color(0xFFFFFDF0), // Cheerful butter-cream
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF1E1E24),
    onSurface = Color(0xFF1E1E24),
    surfaceVariant = Color(0xFFFFECB3).copy(alpha = 0.5f), // Cheerful yellowish surface
    onSurfaceVariant = Color(0xFF5D4037),
    outline = Color(0xFFFFD54F)
)

@Composable
fun MemesTheme(
    isLight: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // We select the appropriate color scheme based on user settings
    val colorScheme = if (isLight) PlayfulLightColorScheme else ImmersiveColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
