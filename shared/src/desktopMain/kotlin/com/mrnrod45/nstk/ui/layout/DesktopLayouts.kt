package com.mrnrod45.nstk.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mrnrod45.nstk.ui.navigation.Screen
import org.jetbrains.compose.resources.painterResource

// macOS ships as a separate native SwiftUI app (see /macosApp) — this file now
// serves Windows/Linux only.

// -----------------------------------------------------------------------------
// Windows Layout: NavView (Hamburger) Style
// -----------------------------------------------------------------------------
@Composable
fun WindowsLayout(
    screens: List<Screen>,
    currentDestination: String?,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Navigation Rail (Collapsed NavView style)
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.height(12.dp)) // Spacing from top

            screens.forEach { screen ->
                NavigationRailItem(
                    selected = currentDestination == screen.route,
                    onClick = { onNavigate(screen.route) },
                    icon = { Icon(painterResource(screen.icon), contentDescription = screen.label) },
                    label = { Text(screen.label) }
                )
            }
        }

        // Content
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            content()
        }
    }
}

// -----------------------------------------------------------------------------
// Linux Layout: Standard
// -----------------------------------------------------------------------------
@Composable
fun LinuxLayout(
    screens: List<Screen>,
    currentDestination: String?,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    // Similar to Windows for now
    WindowsLayout(screens, currentDestination, onNavigate, content)
}
