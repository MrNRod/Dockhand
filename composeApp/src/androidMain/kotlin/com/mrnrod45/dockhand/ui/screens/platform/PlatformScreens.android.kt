package com.mrnrod45.dockhand.ui.screens.platform

import androidx.compose.runtime.Composable
import com.mrnrod45.dockhand.domain.usb.UsbController
import com.mrnrod45.dockhand.platform.file.FilePicker
import com.mrnrod45.dockhand.ui.viewmodels.SettingsViewModel
import com.mrnrod45.dockhand.ui.viewmodels.RcmViewModel
import com.mrnrod45.dockhand.domain.file.FileSplitter
import com.mrnrod45.dockhand.ui.screens.UploadScreen
import com.mrnrod45.dockhand.ui.screens.RcmScreen
import com.mrnrod45.dockhand.ui.screens.SplitMergeScreen
import com.mrnrod45.dockhand.ui.screens.SettingsScreen

@Composable
actual fun PlatformUploadScreen(
    usbController: UsbController,
    filePicker: FilePicker,
    settingsViewModel: SettingsViewModel
) {
    UploadScreen(usbController, filePicker, settingsViewModel)
}

@Composable
actual fun PlatformRcmScreen(
    viewModel: RcmViewModel
) {
    RcmScreen(viewModel)
}

@Composable
actual fun PlatformSplitMergeScreen(
    filePicker: FilePicker,
    fileSplitter: FileSplitter,
    settingsViewModel: SettingsViewModel
) {
    SplitMergeScreen(filePicker, fileSplitter, settingsViewModel)
}

@Composable
actual fun PlatformSettingsScreen(
    viewModel: SettingsViewModel
) {
    SettingsScreen(viewModel)
}
