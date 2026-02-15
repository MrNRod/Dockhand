package com.mrnrod45.nstk.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrnrod45.nstk.ui.components.PlatformDraggableArea
import com.mrnrod45.nstk.ui.navigation.Screen
import org.jetbrains.compose.resources.painterResource

// -----------------------------------------------------------------------------
// macOS Layout: Sidebar + Content
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
            .background(MaterialTheme.colorScheme.background) // Main background
    ) {
        // --- Sidebar ---
        Column(
            modifier = Modifier
                .width(180.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) // Translucent Surface
                .padding(end = 1.dp) // Divider line space
        ) {
            // Traffic Light Area (Draggable)
            PlatformDraggableArea(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp) // Standard macOS title bar height approx
            ) {
                // Empty, just draggable area
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation Items
            screens.forEach { screen ->
                val isSelected = currentDestination == screen.route
                MacSidebarItem(
                    screen = screen,
                    isSelected = isSelected,
                    onClick = { onNavigate(screen.route) }
                )
            }
        }
        
        // Vertical Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // --- Main Content ---
        Column(modifier = Modifier.weight(1f)) {
            // Top Draggable Area for Main Content (to match title bar)
            PlatformDraggableArea(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                // Could put page title here if needed
            }
            
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
    val backgroundColor = if (isSelected) com.mrnrod45.nstk.ui.theme.SystemBlueLight else Color.Transparent
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(screen.icon),
            contentDescription = screen.label,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
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
