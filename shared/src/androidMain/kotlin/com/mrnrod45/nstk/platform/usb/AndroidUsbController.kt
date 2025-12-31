package com.mrnrod45.nstk.platform.usb

import android.hardware.usb.UsbManager
import com.mrnrod45.nstk.domain.usb.UsbController
import com.mrnrod45.nstk.domain.usb.UsbDevice

class AndroidUsbController(
    private val usbManager: UsbManager
) : UsbController {

    override val isRcmSupported: Boolean = true

    override fun listDevices(): List<UsbDevice> {
        val deviceList = usbManager.deviceList
        return deviceList.values
            .filter { it.vendorId == 0x057E } // Filter for Nintendo Switch
            .map { device ->
                AndroidUsbDevice(usbManager, device)
            }
            .toList()
    }

    override fun findRcmDevice(): UsbDevice? {
        val deviceList = usbManager.deviceList
        val rcmDevice = deviceList.values
            .firstOrNull { it.vendorId == 0x0955 && it.productId == 0x7321 }
        
        return if (rcmDevice != null) {
            AndroidUsbDevice(usbManager, rcmDevice)
        } else {
            null
        }
    }

    override suspend fun injectPayload(device: UsbDevice, payload: ByteArray): Boolean {
        if (device !is AndroidUsbDevice) return false
        
        val androidDevice = device.device
        val connection = usbManager.openDevice(androidDevice) ?: return false
        
        try {
            val validInterface = (0 until androidDevice.interfaceCount)
                .map { androidDevice.getInterface(it) }
                .firstOrNull() ?: return false // Usually interface 0
                
            if (!connection.claimInterface(validInterface, true)) {
                return false
            }

            // Find Endpoint OUT (Bulk)
             val endpointOut = (0 until validInterface.endpointCount)
                .map { validInterface.getEndpoint(it) }
                .firstOrNull { it.direction == android.hardware.usb.UsbConstants.USB_DIR_OUT && 
                               it.type == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK }
                ?: return false // Should ideally check for specific EP address if known, but first Bulk OUT is usually correct for RCM

            // Transfer Payload
            val chunkSize = 4096
            var offset = 0
            
            while (offset < payload.size) {
                 val end = minOf(offset + chunkSize, payload.size)
                 val length = end - offset
                 
                 // Android bulkTransfer uses byte[] directly
                 val chunk = payload.copyOfRange(offset, end) // copy is safer/easier here though alloc heavy
                 // Or better: use bulkTransfer(endpoint, buffer, offset, length, timeout) if API level allows?
                 // API 18+ has bulkTransfer(endpoint, byte[] buffer, int offset, int length, int timeout)
                 
                 // Let's assume standard API level (minSdk is usually high enough)
                 // Wait, KMP androidMain usage might restrict API.
                 // safe approach: copy range to avoid offset issues if method signature varies.
                 
                 val res = connection.bulkTransfer(endpointOut, chunk, length, 5000)
                 if (res < 0) {
                     return false
                 }
                 offset += length
            }

            // Smash the stack (Control Transfer)
            // 0x82 GET_STATUS, val 0, idx 0, len 0x7000
            val smashLen = 0x7000
            val smashBuffer = ByteArray(smashLen)
            
            val smashRes = connection.controlTransfer(
                0x82, // requestType: Dir=In(0x80) | Type=Std(0x00) | Rec=Ep(0x02)
                0,    // request: GET_STATUS
                0,    // value
                0,    // index
                smashBuffer,
                smashLen,
                1000
            )
            
            // Result logic similar to Desktop: just firing it is enough.
            return true

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            connection.close()
        }
    }
}
