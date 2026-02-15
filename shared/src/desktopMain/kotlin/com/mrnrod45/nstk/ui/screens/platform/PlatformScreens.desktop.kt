package com.mrnrod45.nstk.ui.screens.platform

import androidx.compose.runtime.Composable
import com.mrnrod45.nstk.domain.usb.UsbController
import com.mrnrod45.nstk.platform.file.FilePicker
import com.mrnrod45.nstk.ui.viewmodels.SettingsViewModel
import com.mrnrod45.nstk.ui.viewmodels.RcmViewModel
import com.mrnrod45.nstk.domain.file.FileSplitter
import com.mrnrod45.nstk.ui.screens.UploadScreen
import com.mrnrod45.nstk.ui.screens.RcmScreen
import com.mrnrod45.nstk.ui.screens.SplitMergeScreen
import com.mrnrod45.nstk.ui.screens.SettingsScreen
import com.mrnrod45.nstk.ui.screens.mac.MacUploadScreen
import com.mrnrod45.nstk.ui.screens.mac.MacRcmScreen
import com.mrnrod45.nstk.ui.screens.mac.MacSettingsScreen
import com.mrnrod45.nstk.ui.screens.mac.MacSplitMergeScreen

private val isMac = System.getProperty("os.name").lowercase().contains("mac")

@Composable
actual fun PlatformUploadScreen(
    usbController: UsbController,
    filePicker: FilePicker,
    settingsViewModel: SettingsViewModel
) {
    if (isMac) {
        MacUploadScreen(usbController, filePicker, settingsViewModel)
    } else {
        UploadScreen(usbController, filePicker, settingsViewModel)
    }
}

@Composable
actual fun PlatformRcmScreen(
    viewModel: RcmViewModel
) {
    if (isMac) {
        MacRcmScreen(viewModel)
    } else {
        RcmScreen(viewModel)
    }
}

@Composable
actual fun PlatformSplitMergeScreen(
    filePicker: FilePicker,
    fileSplitter: FileSplitter
) {
    if (isMac) {
        MacSplitMergeScreen(filePicker, fileSplitter)
    } else {
        SplitMergeScreen(filePicker, fileSplitter)
    }
}

@Composable
actual fun PlatformSettingsScreen(
    viewModel: SettingsViewModel
) {
    if (isMac) {
        MacSettingsScreen(viewModel)
    } else {
        SettingsScreen(viewModel)
    }
}
