package com.mrnrod45.nstk.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrnrod45.nstk.ui.components.PlatformDraggableArea
import com.mrnrod45.nstk.ui.navigation.Screen
import com.mrnrod45.nstk.ui.theme.MacLightTertiaryLabel
import com.mrnrod45.nstk.ui.theme.MacDarkTertiaryLabel
import com.mrnrod45.nstk.ui.theme.SystemBlueLight
import org.jetbrains.compose.resources.painterResource

// -----------------------------------------------------------------------------
// macOS Layout: Sidebar + Content  (Finder / Mail / Notes style)
// -----------------------------------------------------------------------------
@Composable
fun MacLayout(
    screens: List<Screen>,
    currentDestination: String?,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Sidebar ---
        // Background is transparent on macOS so the native NSVisualEffectView
        // vibrancy/blur applied in main.kt shows through underneath Compose.
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(
                    if (System.getProperty("os.name").lowercase().contains("mac"))
                        androidx.compose.ui.graphics.Color.Transparent
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            // Unified toolbar / traffic-light zone — draggable
            PlatformDraggableArea(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp) // macOS 26: taller zone gives traffic lights more prominence
            ) { /* Traffic light buttons are rendered by the OS */ }

            // Navigation Items — no extra top spacer; toolbar height provides the offset
            screens.forEach { screen ->
                val isSelected = currentDestination == screen.route
                MacSidebarItem(
                    screen = screen,
                    isSelected = isSelected,
                    onClick = { onNavigate(screen.route) }
                )
            }
        }

        // --- Main Content ---
        // Content pane starts immediately; no extra draggable strip needed because
        // the sidebar draggable area already acts as window drag.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 88.dp) // Push content below toolbar height
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun MacSidebarItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val unselectedIconTint = if (isDark) MacDarkTertiaryLabel else MacLightTertiaryLabel

    val backgroundColor = if (isSelected) SystemBlueLight else Color.Transparent
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    val iconTint = if (isSelected) Color.White else unselectedIconTint

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 7.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(screen.icon),
            contentDescription = screen.label,
            tint = iconTint,
            modifier = Modifier.size(16.dp) // SF Symbols sidebar icon size
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = screen.label,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            lineHeight = 13.sp
        )
    }
}

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
