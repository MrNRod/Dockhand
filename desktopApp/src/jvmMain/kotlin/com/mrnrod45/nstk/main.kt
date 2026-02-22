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
            kotlinx.coroutines.delay(1000)
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
            } catch (e: Exception) { false }
        }
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

/**
 * Applies native NSWindow vibrancy and sidebar appearance on macOS.
 *
 * Uses only the macOS JDK's bundled private APIs (sun.lwawt.macosx) — no
 * additional libraries needed. Silently skips on non-macOS or if the
 * private API has changed in future JDK versions.
 *
 * Technique:
 *   1. Reflect into CWindow (the macOS AWTPeer) to get NSWindow pointer.
 *   2. Use Foundation.invoke() (shipped in the macOS JDK) to call
 *      NSWindow setAppearance: and setContentView: with an NSVisualEffectView.
 */
private val OS = System.getProperty("os.name").lowercase()

private fun applyVibrancy(awtWindow: java.awt.Window) {
    if (!OS.contains("mac")) return
    try {
        // The macOS JDK ships Foundation.class in sun.lwawt.macosx.
        // It exposes invokeVoid / invoke to call ObjC methods by selector name.
        val foundationClass = Class.forName("sun.lwawt.macosx.Foundation")

        // Get the NSWindow pointer from the AWT peer
        val peerField = java.awt.Component::class.java.getDeclaredField("peer")
        peerField.isAccessible = true
        val peer = peerField.get(awtWindow) ?: return
        // CWindow exposes getNSWindow() → Long
        val nsWin = peer.javaClass.getMethod("getNSWindow").also { it.isAccessible = true }
            .invoke(peer) as Long

        // Foundation.invoke(long receiver, String selector, args...)
        val invoke = foundationClass.getMethod("invoke", Long::class.java, String::class.java, Array<Any>::class.java)

        // 1. Set the window's appearance to NSAppearanceNameVibrantLight/Dark.
        //    We do this by creating the named appearance and setting it on the NSWindow.
        val nsAppearanceCls = foundationClass.getMethod("getClass", String::class.java)
            .invoke(null, "NSAppearance") as Long
        val appearanceName = if (isSystemDarkTheme()) "NSAppearanceNameVibrantDark" else "NSAppearanceNameVibrantLight"
        val nsStr = foundationClass.getMethod("nsString", String::class.java).invoke(null, appearanceName) as Long
        val appearance = invoke.invoke(null, nsAppearanceCls, "appearanceNamed:", arrayOf<Any>(nsStr)) as Long
        invoke.invoke(null, nsWin, "setAppearance:", arrayOf<Any>(appearance))

        // 2. Replace contentView with NSVisualEffectView (sidebar material, always active)
        val nsVevCls = foundationClass.getMethod("getClass", String::class.java)
            .invoke(null, "NSVisualEffectView") as Long
        val oldContentView = invoke.invoke(null, nsWin, "contentView", emptyArray<Any>()) as Long

        val vev = invoke.invoke(null, invoke.invoke(null, nsVevCls, "alloc", emptyArray<Any>()) as Long,
            "initWithFrame:", arrayOf<Any>(invoke.invoke(null, oldContentView, "frame", emptyArray<Any>()) as Long)) as Long
        invoke.invoke(null, vev, "setMaterial:", arrayOf<Any>(7L))     // sidebar
        invoke.invoke(null, vev, "setState:", arrayOf<Any>(1L))        // active
        invoke.invoke(null, vev, "setBlendingMode:", arrayOf<Any>(0L)) // behind window
        invoke.invoke(null, vev, "setAutoresizingMask:", arrayOf<Any>(18L)) // width+height

        // Move all existing subviews from old contentView into the VEV
        val subviews = invoke.invoke(null, oldContentView, "subviews", emptyArray<Any>()) as Long
        val count = invoke.invoke(null, subviews, "count", emptyArray<Any>()) as Long
        for (i in 0 until count) {
            val subview = invoke.invoke(null, subviews, "objectAtIndex:", arrayOf<Any>(i)) as Long
            invoke.invoke(null, vev, "addSubview:", arrayOf<Any>(subview))
        }
        invoke.invoke(null, nsWin, "setContentView:", arrayOf<Any>(vev))

        println("NSVisualEffectView vibrancy applied (sidebar material).")
    } catch (e: Throwable) {
        println("Vibrancy skipped (${e.javaClass.simpleName}): ${e.message}")
    }
}


fun main() {
    // macOS: Must be set before any AWT/Compose init
    System.setProperty("apple.awt.application.appearance", "system")
    System.setProperty("apple.laf.useScreenMenuBar", "true")

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
                // Full-window content + transparent title bar → native chrome draws our sidebar
                window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
                window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)

                window.minimumSize = java.awt.Dimension(900, 650)
                window.setSize(900, 650)

                // macOS 26 Liquid Glass: attach NSVisualEffectView for real frosted-glass blur
                applyVibrancy(window)
            }

            val settingsViewModel = androidx.compose.runtime.remember {
                com.mrnrod45.nstk.ui.viewmodels.SettingsViewModel()
            }
            App(usbController, filePicker, settingsViewModel, darkTheme = isDark)
        }
    }
}
