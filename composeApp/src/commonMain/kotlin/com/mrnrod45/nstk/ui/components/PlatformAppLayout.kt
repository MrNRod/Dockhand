package com.mrnrod45.nstk.ui.components

import androidx.compose.runtime.Composable
import com.mrnrod45.nstk.ui.navigation.Screen

@Composable
expect fun PlatformAppLayout(
    screens: List<Screen>,
    currentDestination: String?,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
)
