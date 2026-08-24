package com.mrnrod45.nstk.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Windows/Linux windows are always decorated (native title bar provides dragging),
// so no custom drag handling is needed here. macOS ships as a separate native
// SwiftUI app (see /macosApp) which gets real window dragging for free.
@Composable
actual fun PlatformDraggableArea(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}
