package com.mrnrod45.dockhand.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NativePrimaryDark,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = NativePrimaryDark.copy(alpha = 0.3f),
    onPrimaryContainer = NativePrimaryDark,
    secondary = NativeSecondaryDark,
    secondaryContainer = NativeSecondaryDark.copy(alpha = 0.3f),
    onSecondaryContainer = androidx.compose.ui.graphics.Color.White,
    tertiary = NativeTertiaryDark,
    tertiaryContainer = NativeTertiaryDark.copy(alpha = 0.3f),
    onTertiaryContainer = NativeTertiaryDark,
    background = GoogleDarkBackground,
    surface = GoogleDarkSurface,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = GoogleDarkSurfaceVariant,
    onSurfaceVariant = androidx.compose.ui.graphics.Color.White,
    surfaceContainerLowest = GoogleDarkSurfaceContainerLowest,
    surfaceContainerLow = GoogleDarkSurfaceContainerLow,
    surfaceContainer = GoogleDarkSurfaceContainer,
    surfaceContainerHigh = GoogleDarkSurfaceContainerHigh,
    surfaceContainerHighest = GoogleDarkSurfaceContainerHighest,
    outline = GoogleDarkOutline,
    surfaceTint = androidx.compose.ui.graphics.Color.Transparent
)

private val LightColorScheme = lightColorScheme(
    primary = NativePrimaryLight,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = NativePrimaryLight.copy(alpha = 0.1f),
    onPrimaryContainer = NativePrimaryLight,
    secondary = NativeSecondaryLight,
    secondaryContainer = NativeSecondaryLight.copy(alpha = 0.1f),
    onSecondaryContainer = androidx.compose.ui.graphics.Color.Black,
    tertiary = NativeTertiaryLight,
    tertiaryContainer = NativeTertiaryLight.copy(alpha = 0.1f),
    onTertiaryContainer = NativeTertiaryLight,
    background = androidx.compose.ui.graphics.Color(0xFFF8F9FA),
    surface = GoogleLightSurface,
    onBackground = androidx.compose.ui.graphics.Color.Black,
    onSurface = androidx.compose.ui.graphics.Color.Black,
    surfaceVariant = GoogleLightSurfaceVariant,
    onSurfaceVariant = androidx.compose.ui.graphics.Color.Black,
    surfaceContainerLowest = GoogleLightSurfaceContainerLowest,
    surfaceContainerLow = GoogleLightSurfaceContainerLow,
    surfaceContainer = GoogleLightSurfaceContainer,
    surfaceContainerHigh = GoogleLightSurfaceContainerHigh,
    surfaceContainerHighest = GoogleLightSurfaceContainerHighest,
    outline = GoogleLightOutline,
    outlineVariant = GoogleLightOutlineVariant,
    surfaceTint = androidx.compose.ui.graphics.Color.Transparent
)

@Composable
actual fun AppTheme(
    themeConfig: AppThemeConfig,
    darkTheme: Boolean, // System value passed from common default
    content: @Composable () -> Unit
) {
    // Determine effective dark mode
    val effectiveDarkTheme = when (themeConfig.mode) {
        ThemeMode.SYSTEM -> darkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        // Dynamic Color (Monet) - only if enabled AND on S+
        themeConfig.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (effectiveDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        effectiveDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Apply AMOLED Black if requested and in dark mode (Dynamic or Standard)
    val finalColorScheme = if (effectiveDarkTheme && themeConfig.darkThemeConfig == DarkThemeConfig.AMOLED) {
        colorScheme.copy(
            background = androidx.compose.ui.graphics.Color.Black,
            surface = androidx.compose.ui.graphics.Color.Black,
            surfaceVariant = androidx.compose.ui.graphics.Color.Black
        )
    } else {
        colorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = finalColorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !effectiveDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = Typography,
        content = content
    )
}
