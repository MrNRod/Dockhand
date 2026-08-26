package com.mrnrod45.nstk


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import android.hardware.usb.UsbManager
import androidx.activity.result.contract.ActivityResultContracts
import com.mrnrod45.nstk.platform.file.AndroidFilePicker
import com.mrnrod45.nstk.platform.usb.AndroidUsbController
    
class MainActivity : ComponentActivity() {
    private lateinit var usbManager: UsbManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usbManager = getSystemService(UsbManager::class.java)
        val usbController = AndroidUsbController(usbManager)

        val filePicker = AndroidFilePicker(this)
        handleUsbDeviceIntent(intent)
        
        val launcher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            filePicker.onResult(uris)
        }
        filePicker.launcher = launcher
        
        val dirLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            filePicker.onDirResult(uri)
        }
        filePicker.dirLauncher = dirLauncher
        
        setContent {
            // Using default viewModel factory which handles scoping to Activity
            val settingsViewModel: com.mrnrod45.nstk.ui.viewmodels.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            App(usbController, filePicker, settingsViewModel)
        }
    }
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleUsbDeviceIntent(intent)
    }

    /**
     * Handles both the ATTACHED intent (device plugged in while the app is running or
     * used to launch it) and a cold-launch intent that already carries EXTRA_DEVICE.
     * Requesting permission here — rather than waiting for the user to open the Upload
     * screen — means the system dialog is already resolved by the time they tap Upload.
     */
    private fun handleUsbDeviceIntent(intent: android.content.Intent?) {
        val usbDevice = intent?.getParcelableExtra<android.hardware.usb.UsbDevice>(UsbManager.EXTRA_DEVICE)
        if (usbDevice != null) {
            AndroidUsbController.requestPermissionIfNeeded(usbManager, usbDevice)
        }
    }
}
