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

// macOS now ships as a separate native SwiftUI app (see /macosApp) consuming
// the :core module directly. This Compose Desktop path serves Windows/Linux only.

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
    fileSplitter: FileSplitter
) {
    SplitMergeScreen(filePicker, fileSplitter)
}

@Composable
actual fun PlatformSettingsScreen(
    viewModel: SettingsViewModel
) {
    SettingsScreen(viewModel)
}
