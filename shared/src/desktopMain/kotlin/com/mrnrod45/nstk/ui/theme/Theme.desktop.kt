package com.mrnrod45.nstk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NativePrimaryDark,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = NativePrimaryDark.copy(alpha = 0.3f), // Darker Blue container
    onPrimaryContainer = NativePrimaryDark,
    secondary = NativeSecondaryDark,
    secondaryContainer = NativeSecondaryDark.copy(alpha = 0.3f),
    onSecondaryContainer = androidx.compose.ui.graphics.Color.White,
    tertiary = NativeTertiaryDark,
    tertiaryContainer = NativeTertiaryDark.copy(alpha = 0.3f),
    onTertiaryContainer = NativeTertiaryDark,
    background = GoogleDarkBackground, // Google Dark Grey
    surface = GoogleDarkSurface,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = GoogleDarkSurfaceVariant, // Lighter Grey for Cards
    onSurfaceVariant = androidx.compose.ui.graphics.Color.White,
    surfaceContainerLowest = GoogleDarkSurfaceContainerLowest,
    surfaceContainerLow = GoogleDarkSurfaceContainerLow,
    surfaceContainer = GoogleDarkSurfaceContainer,
    surfaceContainerHigh = GoogleDarkSurfaceContainerHigh,
    surfaceContainerHighest = GoogleDarkSurfaceContainerHighest,
    outline = GoogleDarkOutline,
    surfaceTint = androidx.compose.ui.graphics.Color.Transparent // Disable Elevation Tint
)

private val LightColorScheme = lightColorScheme(
    primary = NativePrimaryLight,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = NativePrimaryLight.copy(alpha = 0.1f), // Light Blue container
    onPrimaryContainer = NativePrimaryLight,
    secondary = NativeSecondaryLight,
    secondaryContainer = NativeSecondaryLight.copy(alpha = 0.1f),
    onSecondaryContainer = androidx.compose.ui.graphics.Color.Black,
    tertiary = NativeTertiaryLight,
    tertiaryContainer = NativeTertiaryLight.copy(alpha = 0.1f),
    onTertiaryContainer = NativeTertiaryLight,
    background = androidx.compose.ui.graphics.Color(0xFFF8F9FA), // Slightly off-white
    surface = GoogleLightSurface,
    onBackground = androidx.compose.ui.graphics.Color.Black,
    onSurface = androidx.compose.ui.graphics.Color.Black,
    surfaceVariant = GoogleLightSurfaceVariant, // Neutral Grey (No Tint)
    onSurfaceVariant = androidx.compose.ui.graphics.Color.Black,
    surfaceContainerLowest = GoogleLightSurfaceContainerLowest,
    surfaceContainerLow = GoogleLightSurfaceContainerLow,
    surfaceContainer = GoogleLightSurfaceContainer,
    surfaceContainerHigh = GoogleLightSurfaceContainerHigh,
    surfaceContainerHighest = GoogleLightSurfaceContainerHighest,
    outline = GoogleLightOutline,
    outlineVariant = GoogleLightOutlineVariant,
    surfaceTint = androidx.compose.ui.graphics.Color.Transparent // Disable Elevation Tint
)

@Composable
actual fun AppTheme(
    themeConfig: AppThemeConfig,
    darkTheme: Boolean, // Polled system value passed from App
    content: @Composable () -> Unit
) {
    // Determine effective dark mode
    val effectiveDarkTheme = when (themeConfig.mode) {
        ThemeMode.SYSTEM -> darkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val baseColorScheme = if (effectiveDarkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    // Optional: Apply Amoled Black on Desktop too if desired, though less common.
    // The user asked for "options with dynamic theme... for android app". 
    // But for Desktop, "AMOLED" might be desired for OLED screens properly?
    // Let's support it for consistency if set.
    val finalColorScheme = if (effectiveDarkTheme && themeConfig.darkThemeConfig == DarkThemeConfig.AMOLED) {
        baseColorScheme.copy(
            background = androidx.compose.ui.graphics.Color.Black,
            surface = androidx.compose.ui.graphics.Color.Black,
            surfaceVariant = androidx.compose.ui.graphics.Color.Black
        )
    } else {
        baseColorScheme
    }

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = Typography,
        content = content
    )
}
