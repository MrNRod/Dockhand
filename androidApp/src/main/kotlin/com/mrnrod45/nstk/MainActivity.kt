package com.mrnrod45.nstk


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import android.hardware.usb.UsbManager
import androidx.activity.result.contract.ActivityResultContracts
import com.mrnrod45.nstk.platform.file.AndroidFilePicker
import com.mrnrod45.nstk.platform.usb.AndroidUsbController
    
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val usbManager = getSystemService(UsbManager::class.java)
        val usbController = AndroidUsbController(usbManager)
        
        val filePicker = AndroidFilePicker(this)
        
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
        val usbDevice = intent.getParcelableExtra<android.hardware.usb.UsbDevice>(UsbManager.EXTRA_DEVICE)
        // Intent consumed silently.
    }
}
