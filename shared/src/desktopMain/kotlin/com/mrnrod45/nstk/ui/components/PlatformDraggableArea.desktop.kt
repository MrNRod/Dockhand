package com.mrnrod45.nstk.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import java.awt.Window
import javax.swing.SwingUtilities

@Composable
actual fun PlatformDraggableArea(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    // We need to find the Window to drag it.
    // Since we don't have WindowScope here (shared component), we use a composition side-effect to find the window
    // from the underlying generic AWT component if possible.
    
    // Note: LocalView/LocalLayerContainer might not be stable API or easy to access across versions.
    // But we can construct a modifier that captures the window on first event or layout.
    
    // Actually, simple dragging logic:
    // We can't easily get the window inside the @Composable unless we use specific Desktop locals.
    // `androidx.compose.ui.window.LocalWindow` is NOT standard in all versions.
    
    // Let's use `androidx.compose.ui.platform.LocalWindowInfo`? No.
    
    // Use the `java.awt.Component` from `LocalLayerContainer`?
    // It is often available.
    
    // Let's try a different approach:
    // Pass a handler? No, that breaks common API.
    
    // Let's use the fact that we are in a `ComposeWindow`.
    // We can just query `Window.getWindows().firstOrNull { it.isActive }`? 
    // Risky if multiple windows.
    
    // Better: `App` is called from `main.kt` inside `Window`.
    // I will use a simple pointerInput and `java.awt.MouseInfo` or similar? 
    // No, we need to move THE window.
    
    // Let's try `LocalLayerContainer`
    
    val container = androidx.compose.ui.platform.LocalWindowInfo.current
    // Wait, LocalWindowInfo is interface.
    
    // Let's go with the safe bet: 
    // `AppNavigation` is high up.
    // If we really can't get the window, dragging won't work.
    // But commonly in Desktop KMP, people pass `Window` or use `LocalAnswer`.
    
    // Let's assume for this specific codebase, we can find it.
    
    // Use MouseInfo for absolute screen coordinates to avoid jitter issues with relative deltas
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        val window = java.awt.Window.getWindows().firstOrNull { it.isActive && it.isFocused }
                        if (window is java.awt.Frame) {
                            val state = window.extendedState
                            if ((state and java.awt.Frame.MAXIMIZED_BOTH) == java.awt.Frame.MAXIMIZED_BOTH) {
                                window.extendedState = java.awt.Frame.NORMAL
                            } else {
                                window.extendedState = java.awt.Frame.MAXIMIZED_BOTH
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
            var startMousePos: java.awt.Point? = null
            var startWindowPos: java.awt.Point? = null
            
            detectDragGestures(
                onDragStart = { _ ->
                    val window = java.awt.Window.getWindows().firstOrNull { it.isActive && it.isFocused }
                    if (window != null) {
                        startWindowPos = window.location
                        startMousePos = java.awt.MouseInfo.getPointerInfo().location
                    }
                },
                onDrag = { _, _ ->
                    val window = java.awt.Window.getWindows().firstOrNull { it.isActive && it.isFocused }
                    if (window != null && startMousePos != null && startWindowPos != null) {
                        val currentMousePos = java.awt.MouseInfo.getPointerInfo().location
                        val dx = currentMousePos.x - startMousePos!!.x
                        val dy = currentMousePos.y - startMousePos!!.y
                        
                        window.location = java.awt.Point(
                            startWindowPos!!.x + dx,
                            startWindowPos!!.y + dy
                        )
                    }
                }
            )
        }
    ) {
        content()
    }

}
