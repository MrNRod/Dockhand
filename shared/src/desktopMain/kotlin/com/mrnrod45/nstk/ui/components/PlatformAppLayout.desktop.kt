package com.mrnrod45.nstk.ui.components

import androidx.compose.runtime.Composable
import com.mrnrod45.nstk.ui.layout.MacLayout
import com.mrnrod45.nstk.ui.layout.WindowsLayout
import com.mrnrod45.nstk.ui.layout.LinuxLayout
import com.mrnrod45.nstk.ui.navigation.Screen

@Composable
actual fun PlatformAppLayout(
    screens: List<Screen>,
    currentDestination: String?,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val os = System.getProperty("os.name").lowercase()
    when {
        os.contains("mac") -> MacLayout(screens, currentDestination, onNavigate, content)
        os.contains("windows") -> WindowsLayout(screens, currentDestination, onNavigate, content)
        else -> LinuxLayout(screens, currentDestination, onNavigate, content)
    }
}
