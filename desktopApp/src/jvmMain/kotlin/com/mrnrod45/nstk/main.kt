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

// Windows/Linux only — macOS ships as a separate native SwiftUI app (see /macosApp).

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
            kotlinx.coroutines.delay(1000)
        }
    }
    return isDark
}

fun isSystemDarkTheme(): Boolean {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("linux") -> {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("gsettings", "get", "org.gnome.desktop.interface", "color-scheme"))
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                val output = reader.readLine()
                output != null && output.lowercase().contains("dark")
            } catch (e: Exception) { false }
        }
        os.contains("windows") -> {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("powershell", "-Command",
                    "Get-ItemProperty -Path HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize -Name AppsUseLightTheme"))
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                var line: String?
                var isDark = false
                while (reader.readLine().also { line = it } != null) {
                    if (line?.contains("AppsUseLightTheme") == true && line?.contains("0") == true) {
                        isDark = true; break
                    }
                }
                isDark
            } catch (e: Exception) { false }
        }
        else -> false
    }
}

private val OS = System.getProperty("os.name").lowercase()

fun main() {
    // Linux: Fix WM_CLASS so taskbar associates the running app with the pinned icon
    System.setProperty("sun.awt.wmclass", "nstk")

    // Linux: Fix UI scaling on HiDPI/4K displays
    if (OS.contains("linux")) {
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
            transparent = false,
            undecorated = false
        ) {
            androidx.compose.runtime.LaunchedEffect(window) {
                window.minimumSize = java.awt.Dimension(900, 650)
                window.setSize(900, 650)
            }

            val settingsViewModel = androidx.compose.runtime.remember {
                com.mrnrod45.nstk.ui.viewmodels.SettingsViewModel()
            }
            App(usbController, filePicker, settingsViewModel, darkTheme = isDark)
        }
    }
}
