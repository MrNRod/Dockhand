package com.mrnrod45.dockhand.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformDraggableArea(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)
