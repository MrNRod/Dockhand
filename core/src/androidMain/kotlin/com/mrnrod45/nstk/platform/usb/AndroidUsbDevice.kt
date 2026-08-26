package com.mrnrod45.nstk.platform.usb

import android.hardware.usb.UsbManager
import com.mrnrod45.nstk.domain.usb.UsbConnection
import com.mrnrod45.nstk.domain.usb.UsbDevice

class AndroidUsbDevice(
    private val usbManager: UsbManager,
    internal val device: android.hardware.usb.UsbDevice
) : UsbDevice {

    override val name: String
        get() = device.productName ?: "Unknown Device" // Requires API 21+

    override val vendorId: Int
        get() = device.vendorId

    override val productId: Int
        get() = device.productId

    override fun open(): UsbConnection? {
        if (!usbManager.hasPermission(device)) {
            // Best-effort: ask now so a subsequent retry succeeds. open() can't suspend
            // for the user's answer since it's part of the common, non-Android UsbDevice API.
            AndroidUsbController.requestPermissionIfNeeded(usbManager, device)
            return null
        }
        val connection = usbManager.openDevice(device) ?: return null
        
        // For simplicity in this port, we assume interface 0. 
        // Real implementation might need to search for the correct interface (e.g. mass storage or vendor specific).
        // Protocol logic usually dictates which interface to use. 
        // The original code claimed interface 0 for homebrew mode.
        if (device.interfaceCount > 0) {
            val iface = device.getInterface(0)
            return AndroidUsbConnection(connection, iface)
        }
        
        connection.close()
        return null
    }
}
