package com.mrnrod45.dockhand.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mrnrod45.dockhand.ui.theme.oneui.BundledOneUiPalette
import com.mrnrod45.dockhand.ui.theme.oneui.MaterialShapes
import com.mrnrod45.dockhand.ui.theme.oneui.OneUi
import com.mrnrod45.dockhand.ui.theme.oneui.OneUiShapes
import com.mrnrod45.dockhand.ui.theme.oneui.oneUiDarkColorScheme
import com.mrnrod45.dockhand.ui.theme.oneui.oneUiLightColorScheme
import com.mrnrod45.dockhand.ui.theme.oneui.oneUiTypography
import com.mrnrod45.dockhand.ui.theme.oneui.withOneUiSurfaceLayering

@Composable
actual fun AppTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val oneUi = remember(context) { OneUi.isOneUi(context) }

    val colorScheme = when {
        // Android 12+: the platform palette. On a Galaxy this is Samsung's adaptive palette, on
        // everything else it is Material You — one call covers both, and Compose already picks the
        // right resources per API level behind it.
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val system =
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (oneUi) system.withOneUiSurfaceLayering(darkTheme) else system
        }

        // Below Android 12 there is no system palette to follow.
        oneUi -> if (darkTheme) oneUiDarkColorScheme(BundledOneUiPalette)
        else oneUiLightColorScheme(BundledOneUiPalette)

        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    // The activity draws edge to edge, so the app's own background shows through the system bars
    // and only the icon tint needs setting here. Window.statusBarColor / navigationBarColor are
    // deprecated and are no-ops under edge to edge anyway.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAppStyle provides if (oneUi) AppStyle.OneUi else AppStyle.Material
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = if (oneUi) oneUiTypography(Typography) else Typography,
            shapes = if (oneUi) OneUiShapes else MaterialShapes,
            content = content
        )
    }
}
