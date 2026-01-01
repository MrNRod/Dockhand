package com.mrnrod45.nstk

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.ui.res.painterResource
import com.mrnrod45.nstk.platform.file.DesktopFilePicker
import com.mrnrod45.nstk.platform.usb.DesktopUsbController
import com.mrnrod45.nstk.platform.usb.NoOpUsbController
import com.mrnrod45.nstk.domain.usb.UsbController

fun createUsbController(): UsbController {
    return try {
        DesktopUsbController()
    } catch (e: Throwable) {
        println("Failed to initialize DesktopUsbController (likely missing native libs): ${e.message}")
        NoOpUsbController()
    }
}

@androidx.compose.runtime.Composable
fun rememberDesktopDarkTheme(): Boolean {
    var isDark by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            isDark = isSystemDarkTheme()
            kotlinx.coroutines.delay(1000) // Poll every second
        }
    }
    return isDark
}

fun isSystemDarkTheme(): Boolean {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("mac") -> {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("defaults", "read", "-g", "AppleInterfaceStyle"))
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                val output = reader.readLine()
                output != null && output.trim().equals("Dark", ignoreCase = true)
            } catch (e: Exception) {
                false
            }
        }
        os.contains("linux") -> {
            try {
                // Check GNOME/GTK setting
                val process = Runtime.getRuntime().exec(arrayOf("gsettings", "get", "org.gnome.desktop.interface", "color-scheme"))
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                val output = reader.readLine()
                // Output is usually "'prefer-dark'" or "'default'"
                output != null && output.lowercase().contains("dark")
            } catch (e: Exception) {
                false
            }
        }
        else -> false // Windows usually handled by Compose built-in, but our manual poll overrides it. TODO: Windows check?
    }
}

fun main() {
    // macOS: Enable dark mode for AWT/Swing elements (file picker, window bar)
    // MUST be set before any AWT/Compose init happens
    System.setProperty("apple.awt.application.appearance", "system")
    System.setProperty("apple.laf.useScreenMenuBar", "true")

    // Linux: Fix WM_CLASS to match .desktop file (packageName = "nstk")
    // This allows the Dock to associate the running app with the pinned icon.
    System.setProperty("sun.awt.wmclass", "nstk")
    
    // Linux: Fix UI Scaling on high-DPI screens (e.g. 4k)
    // We set this programmatically here because build-time conditional logic fails if building on macOS.
    val os = System.getProperty("os.name").lowercase()
    if (os.contains("linux")) {
        System.setProperty("sun.java2d.uiScale", "2.0")
    }

    application {
        val usbController = androidx.compose.runtime.remember { createUsbController() }
        val filePicker = androidx.compose.runtime.remember { DesktopFilePicker() }
        val isDark = rememberDesktopDarkTheme()
        
        Window(
            onCloseRequest = ::exitApplication,
            title = "NS-ToolKit",
            icon = painterResource("icon.png"),
            // macOS: Remove standard title bar and extend content to full window
            transparent = false, // We handle background in App, and Compose requires undecorated=true for transparent=true.
            // But for macOS fullWindowContent, we usually want decorated=true (so standard resize/shadows work) 
            // and use the AWT properties below to make the TITLE BAR transparent.
            undecorated = false, 
        ) {
            // Apply macOS specific window properties via Swing/AWT
            androidx.compose.runtime.LaunchedEffect(window) {
                window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
                window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
            }

            // For Desktop, we can just remember the instance for the window lifecycle
            val settingsViewModel = androidx.compose.runtime.remember { com.mrnrod45.nstk.ui.viewmodels.SettingsViewModel() }
            App(usbController, filePicker, settingsViewModel, darkTheme = isDark)
        }
    }
}
