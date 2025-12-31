package com.mrnrod45.nstk.platform.usb

import com.mrnrod45.nstk.domain.usb.UsbController
import com.mrnrod45.nstk.domain.usb.UsbDevice

class NoOpUsbController : UsbController {
    
    override val isRcmSupported: Boolean = false

    override fun listDevices(): List<UsbDevice> {
        println("Warning: USB functionality is disabled on this platform (Missing native libraries).")
        return emptyList()
    }

    override fun findRcmDevice(): UsbDevice? {
        return null
    }

    override suspend fun injectPayload(device: UsbDevice, payload: ByteArray): Boolean {
        return false
    }
}
