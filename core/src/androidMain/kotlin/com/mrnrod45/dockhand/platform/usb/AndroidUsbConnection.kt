package com.mrnrod45.dockhand.platform.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import com.mrnrod45.dockhand.domain.usb.UsbConnection

class AndroidUsbConnection(
    private val deviceConnection: UsbDeviceConnection,
    private val usbInterface: UsbInterface
) : UsbConnection {
    // Dynamic endpoint finding for Android
    override val bulkInEndpoint: Byte
    override val bulkOutEndpoint: Byte
    override val activeInterfaceIndex: Int
        get() = usbInterface.id

    init {
        var inEp: Byte = 0x81.toByte()
        var outEp: Byte = 0x01.toByte()
        
        for (i in 0 until usbInterface.endpointCount) {
             val ep = usbInterface.getEndpoint(i)
             if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                 if (ep.direction == UsbConstants.USB_DIR_IN) {
                     inEp = ep.address.toByte()
                 } else {
                     outEp = ep.address.toByte()
                 }
             }
        }
        bulkInEndpoint = inEp
        bulkOutEndpoint = outEp
    }

    override fun claimInterface(interfaceNumber: Int): Boolean {
        // In Android, we usually claim the interface we already have via UsbManager.
        // But the API allows claiming.
        return deviceConnection.claimInterface(usbInterface, true)
    }

    override fun releaseInterface(interfaceNumber: Int): Boolean {
        return deviceConnection.releaseInterface(usbInterface)
    }

    override fun bulkTransfer(endpoint: Byte, data: ByteArray, length: Int, timeout: Int): Int {
        // Android endpoint address includes direction.
        // We need to find the UsbEndpoint instance on the interface that matches the address.
        var targetEndpoint: android.hardware.usb.UsbEndpoint? = null
        for (i in 0 until usbInterface.endpointCount) {
             val ep = usbInterface.getEndpoint(i)
             if (ep.address.toByte() == endpoint) {
                 targetEndpoint = ep
                 break
             }
        }
        
        if (targetEndpoint == null) {
            return -1
        }
        
        // Android bulkTransfer uses offset 0 by default for byteArray
        return deviceConnection.bulkTransfer(targetEndpoint, data, length, timeout)
    }

    override fun close() {
        deviceConnection.close()
    }
    
    override suspend fun resetEndpoints() {
        // Android USB stack generally handles data toggles automatically upon claimed interface
        // But we can't force CLEAR_FEATURE calls easily via UsbDeviceConnection without controlTransfer
        // For now, assume Android handles it or no-op
        println("Android USB: Reset Endpoints requested (No-op implementation)")
    }
}
