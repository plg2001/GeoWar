package com.example.geowar.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyberpunkPink,
    secondary = CyberpunkBlue,
    tertiary = CyberpunkPink,
    background = CyberpunkBackground,
    surface = CyberpunkSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = CyberpunkText,
    onSurface = CyberpunkText,
    error = CyberpunkPink
)

private val LightColorScheme = lightColorScheme(
    primary = CyberpunkPink,
    secondary = CyberpunkBlue,
    tertiary = CyberpunkPink,
    background = CyberpunkBackground,
    surface = CyberpunkSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = CyberpunkText,
    onSurface = CyberpunkText,
    error = CyberpunkPink
)

@Composable
fun GeoWarTheme(
    darkTheme: Boolean = true, // Force dark theme for cyberpunk style
    dynamicColor: Boolean = false, // Disable dynamic color to enforce our theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
