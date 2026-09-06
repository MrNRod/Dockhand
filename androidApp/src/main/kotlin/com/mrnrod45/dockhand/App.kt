package com.mrnrod45.dockhand

import androidx.compose.runtime.Composable
import com.mrnrod45.dockhand.ui.navigation.AppNavigation
import com.mrnrod45.dockhand.ui.theme.AppTheme
import com.mrnrod45.dockhand.domain.usb.UsbController
import com.mrnrod45.dockhand.platform.file.FilePicker

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun App(
    usbController: UsbController,
    filePicker: FilePicker,
    settingsViewModel: com.mrnrod45.dockhand.ui.viewmodels.SettingsViewModel,
    darkTheme: Boolean? = null
) {
    // darkTheme is only passed explicitly by Desktop, which polls the OS itself; on Android the
    // default tracks the system setting.
    AppTheme(
        darkTheme = darkTheme ?: androidx.compose.foundation.isSystemInDarkTheme()
    ) {
        AppNavigation(usbController, filePicker, settingsViewModel)
    }
}
