package com.mrnrod45.dockhand.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrnrod45.dockhand.domain.models.UnifiedFile
import com.mrnrod45.dockhand.domain.rcm.RcmPayloadBuilder
import com.mrnrod45.dockhand.domain.usb.UsbController
import com.mrnrod45.dockhand.platform.file.FilePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RcmViewModel(
    private val usbController: UsbController,
    private val filePicker: FilePicker
) : ViewModel() {

    var selectedPayload: UnifiedFile? by mutableStateOf(null)
        private set

    var logText: String by mutableStateOf("Ready for RCM injection.\nSelect a payload (bin) to start.")
        private set
        
    var isBusy: Boolean by mutableStateOf(false)
        private set

    private val payloadBuilder = RcmPayloadBuilder()

    fun selectPayload() {
        viewModelScope.launch {
            val files = filePicker.pickFiles(allowedExtensions = listOf("bin"))
            if (files.isNotEmpty()) {
                selectedPayload = files.first()
                log("Selected payload: ${selectedPayload?.name}")
            }
        }
    }

    fun injectPayload() {
        val payloadFile = selectedPayload
        if (payloadFile == null) {
            log("Error: No payload selected.")
            return
        }

        viewModelScope.launch {
            isBusy = true
            try {
                log("Preparing payload...")
                val payloadResult = payloadBuilder.buildPayload(payloadFile)
                
                if (payloadResult.isFailure) {
                    log("Error building payload: ${payloadResult.exceptionOrNull()?.message}")
                    return@launch
                }
                
                val payloadBytes = payloadResult.getOrThrow()
                
                log("Checking for RCM device...")
                // We might want to list all RCM devices or just find one
                val device = withContext(Dispatchers.IO) {
                    usbController.findRcmDevice()
                }
                
                if (device == null) {
                    log("Error: No Switch found in RCM mode (VID: 0955, PID: 7321).")
                    log("Please ensure the device is in RCM mode and connected via USB.")
                    return@launch
                }
                
                log("Found RCM device. Injecting...")
                val success = withContext(Dispatchers.IO) {
                    usbController.injectPayload(device, payloadBytes)
                }
                
                if (success) {
                    log("Success: Payload injected! The device should boot now.")
                } else {
                    log("Error: Injection failed. Check connection.")
                }
                
            } catch (e: Exception) {
                log("Exception: ${e.message}")
                e.printStackTrace()
            } finally {
                isBusy = false
            }
        }
    }

    private fun log(message: String) {
        logText += "\n$message"
    }

    fun clearLog() {
        logText = ""
    }
}
