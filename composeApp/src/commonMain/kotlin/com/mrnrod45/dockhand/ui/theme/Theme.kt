package com.mrnrod45.dockhand.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@Composable
expect fun AppTheme(
    themeConfig: AppThemeConfig,
    darkTheme: Boolean = isSystemInDarkTheme(), // Keep for fallback/polling if needed
    content: @Composable () -> Unit
)
