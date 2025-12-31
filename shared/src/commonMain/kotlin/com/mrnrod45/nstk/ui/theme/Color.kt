package com.mrnrod45.nstk.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Fallback / Desktop Colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Native / System Colors (Desktop)
val SystemBlueLight = Color(0xFF007AFF) // macOS Blue
val SystemBlueDark = Color(0xFF0A84FF)
val SystemGrayLight = Color(0xFF8E8E93)
val SystemGrayDark = Color(0xFF8E8E93)

// Brand Colors (Optional override)
val NativePrimaryLight = SystemBlueLight
val NativeSecondaryLight = SystemGrayLight
val NativeTertiaryLight = Color(0xFF5AC8FA) // System Teal

// Google / Material Neutral Colors
val GoogleDarkBackground = Color(0xFF121212)
val GoogleDarkSurface = Color(0xFF303134)
val GoogleDarkSurfaceVariant = Color(0xFF424242) // Neutral Dark Grey
val GoogleDarkSurfaceContainerLowest = Color(0xFF0F0F10)
val GoogleDarkSurfaceContainerLow = Color(0xFF1D1D1F)
val GoogleDarkSurfaceContainer = Color(0xFF202124) // Reinstating the lighter dark for container
val GoogleDarkSurfaceContainerHigh = Color(0xFF2B2B2D)
val GoogleDarkSurfaceContainerHighest = Color(0xFF363638)
val GoogleDarkOutline = Color(0xFF919194)
val GoogleLightSurfaceVariant = Color(0xFFF1F3F4)
val GoogleLightSurface = Color(0xFFFFFFFF)
val GoogleLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val GoogleLightSurfaceContainerLow = Color(0xFFF7F7F9)
val GoogleLightSurfaceContainer = Color(0xFFF1F3F4)
val GoogleLightSurfaceContainerHigh = Color(0xFFECEEF0)
val GoogleLightSurfaceContainerHighest = Color(0xFFE1E3E5)
val GoogleLightOutline = Color(0xFF74777F)
val GoogleLightOutlineVariant = Color(0xFFC4C7C5)

val NativePrimaryDark = SystemBlueDark
val NativeSecondaryDark = SystemGrayDark
val NativeTertiaryDark = Color(0xFF64D2FF)

val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = GoogleDarkBackground,
    surface = GoogleDarkSurface,
)

val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = GoogleLightSurface,
    surface = GoogleLightSurface,
)
