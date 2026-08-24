package com.mrnrod45.nstk

import androidx.compose.runtime.Composable
import com.mrnrod45.nstk.ui.navigation.AppNavigation
import com.mrnrod45.nstk.ui.theme.AppTheme
import com.mrnrod45.nstk.domain.usb.UsbController
import com.mrnrod45.nstk.platform.file.FilePicker

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun App(
    usbController: UsbController,
    filePicker: FilePicker,
    settingsViewModel: com.mrnrod45.nstk.ui.viewmodels.SettingsViewModel,
    darkTheme: Boolean? = null
) {
    val themeConfig by settingsViewModel.themeConfig.collectAsState()

    // If darkTheme is explicitly passed (e.g. from Desktop polling), use it as fallback for SYSTEM mode.
    // Otherwise let AppTheme decide based on config.
    AppTheme(
        themeConfig = themeConfig,
        darkTheme = darkTheme ?: androidx.compose.foundation.isSystemInDarkTheme()
    ) {
        AppNavigation(usbController, filePicker, settingsViewModel)
    }
}
