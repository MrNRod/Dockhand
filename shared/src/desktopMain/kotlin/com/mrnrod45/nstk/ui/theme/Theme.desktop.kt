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


private val MacLightColorScheme = lightColorScheme(
    primary = MacLightPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = MacLightPrimary.copy(alpha = 0.1f),
    onPrimaryContainer = MacLightPrimary,
    secondary = SystemGrayLight,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    background = MacLightBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFF000000),
    surface = MacLightSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFF000000), // NSColor.labelColor
    surfaceVariant = MacLightSurfaceVariant,
    onSurfaceVariant = MacLightSecondaryLabel,  // NSColor.secondaryLabelColor
    surfaceContainerHigh = MacLightSurfaceVariant,
    outline = SystemGrayLight,
    outlineVariant = MacLightOutlineVariant,
)

private val MacDarkColorScheme = darkColorScheme(
    primary = MacDarkPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = MacDarkPrimary.copy(alpha = 0.3f),
    onPrimaryContainer = MacDarkPrimary,
    secondary = SystemGrayDark,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = MacDarkBackground,
    onBackground = androidx.compose.ui.graphics.Color.White,
    surface = MacDarkSurface,
    onSurface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = MacDarkSurfaceVariant,
    onSurfaceVariant = MacDarkSecondaryLabel,   // NSColor.secondaryLabelColor (dark)
    surfaceContainerHigh = MacDarkSurfaceVariant,
    outline = SystemGrayDark,
    outlineVariant = MacDarkOutlineVariant,
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

    val isMac = System.getProperty("os.name").lowercase().contains("mac")

    val baseColorScheme = if (isMac) {
        if (effectiveDarkTheme) MacDarkColorScheme else MacLightColorScheme
    } else {
        if (effectiveDarkTheme) DarkColorScheme else LightColorScheme
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

    val finalTypography = if (isMac) {
        // San Francisco is the system sans-serif font on macOS
        androidx.compose.material3.Typography(
            displayLarge = Typography.displayLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
            displayMedium = Typography.displayMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
            displaySmall = Typography.displaySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
            headlineLarge = Typography.headlineLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
            headlineMedium = Typography.headlineMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
            headlineSmall = Typography.headlineSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
            titleLarge = Typography.titleLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
            titleMedium = Typography.titleMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
            titleSmall = Typography.titleSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
            bodyLarge = Typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
            bodyMedium = Typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
            bodySmall = Typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
            labelLarge = Typography.labelLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
            labelMedium = Typography.labelMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
            labelSmall = Typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif)
        )
    } else {
        Typography
    }

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = finalTypography,
        content = content
    )
}
