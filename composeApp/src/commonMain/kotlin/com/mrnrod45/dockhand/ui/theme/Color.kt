package com.mrnrod45.dockhand.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Static Material palette. Only reached below Android 12, where the platform publishes no colours
// to follow; from Android 12 up the theme uses the device palette instead.
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val GoogleDarkBackground = Color(0xFF121212)
val GoogleDarkSurface = Color(0xFF1E1E1E)
val GoogleDarkSurfaceVariant = Color(0xFF2D2D2D)
val GoogleLightSurface = Color(0xFFFFFFFF)
val GoogleLightSurfaceVariant = Color(0xFFF1F3F4)

val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = GoogleDarkBackground,
    surface = GoogleDarkSurface,
    surfaceVariant = GoogleDarkSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White
)

val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = GoogleLightSurface,
    surface = GoogleLightSurface,
    surfaceVariant = GoogleLightSurfaceVariant,
    onBackground = Color.Black,
    onSurface = Color.Black
)
