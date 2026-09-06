package com.mrnrod45.dockhand.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mrnrod45.dockhand.ui.navigation.Screen
import com.mrnrod45.dockhand.ui.theme.LocalAppStyle
import androidx.compose.ui.res.painterResource

@Composable
fun AppLayout(
    screens: List<Screen>,
    currentDestination: String?,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val style = LocalAppStyle.current
    Scaffold(
        bottomBar = {
            NavigationBar(
                // One UI's bar sits flush on the page with no elevation tint; Material keeps its
                // own surface container colour.
                containerColor = if (style.isOneUi) MaterialTheme.colorScheme.background
                else NavigationBarDefaults.containerColor
            ) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(painterResource(screen.icon), contentDescription = screen.label) },
                        label = null, // Icon-only style, as requested for the bottom bar
                        selected = currentDestination == screen.route,
                        onClick = { onNavigate(screen.route) },
                        colors = if (style.isOneUi) {
                            // One UI marks the active tab by tinting the icon rather than drawing a
                            // pill behind it.
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = Color.Transparent
                            )
                        } else {
                            NavigationBarItemDefaults.colors()
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            content()
        }
    }
}
