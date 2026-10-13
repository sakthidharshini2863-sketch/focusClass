package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = FocusBluePrimary,
    onPrimary = Color.White,
    primaryContainer = FocusBlueContainer,
    onPrimaryContainer = FocusBlueDark,
    secondary = FocusBlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = FocusBlueSurface,
    onSecondaryContainer = FocusBlueDark,
    tertiary = FocusAccentTeal,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondary,
    outline = OutlineLight,
    error = FocusStatusRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = FocusBluePrimaryDark,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = FocusBlueContainerDark,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = FocusBlueLight,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = Color(0xFF93C5FD),
    tertiary = FocusAccentTeal,
    onTertiary = Color.White,
    background = BackgroundDark,
    onBackground = Color(0xFFF1F5F9),
    surface = SurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = FocusStatusRed,
    onError = Color.White
)

@Composable
fun FocusClassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Preserve custom blue-and-white brand aesthetic across devices
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
