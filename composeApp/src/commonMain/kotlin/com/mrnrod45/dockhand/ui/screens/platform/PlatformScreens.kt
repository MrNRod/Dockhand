package com.mrnrod45.dockhand.ui.screens.platform

import androidx.compose.runtime.Composable
import com.mrnrod45.dockhand.domain.usb.UsbController
import com.mrnrod45.dockhand.platform.file.FilePicker
import com.mrnrod45.dockhand.ui.viewmodels.SettingsViewModel
import com.mrnrod45.dockhand.ui.viewmodels.UploadViewModel
import com.mrnrod45.dockhand.ui.viewmodels.RcmViewModel
import com.mrnrod45.dockhand.ui.viewmodels.SplitMergeViewModel
import com.mrnrod45.dockhand.domain.file.FileSplitter

@Composable
expect fun PlatformUploadScreen(
    usbController: UsbController,
    filePicker: FilePicker,
    settingsViewModel: SettingsViewModel
)

@Composable
expect fun PlatformRcmScreen(
    viewModel: RcmViewModel
)

@Composable
expect fun PlatformSplitMergeScreen(
    filePicker: FilePicker,
    fileSplitter: FileSplitter,
    settingsViewModel: SettingsViewModel
)

@Composable
expect fun PlatformSettingsScreen(
    viewModel: SettingsViewModel
)
