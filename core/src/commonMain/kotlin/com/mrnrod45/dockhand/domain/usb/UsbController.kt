package com.mrnrod45.dockhand.domain.usb

interface UsbController {
    fun listDevices(): List<UsbDevice>
    
    // RCM methods
    val isRcmSupported: Boolean
    fun findRcmDevice(): UsbDevice?
    suspend fun injectPayload(device: UsbDevice, payload: ByteArray): Boolean
}
