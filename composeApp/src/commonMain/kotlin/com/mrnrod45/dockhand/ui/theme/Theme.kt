package com.mrnrod45.dockhand.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/**
 * The app has no appearance settings: colours, light/dark and the design language are all taken
 * from the device.
 */
@Composable
expect fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
)
