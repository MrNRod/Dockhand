package com.mrnrod45.nstk.ui.theme

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class DarkThemeConfig {
    STANDARD,
    AMOLED // Pure Black
}

data class AppThemeConfig(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.STANDARD,
    val useDynamicColor: Boolean = true // Android Monet
)
