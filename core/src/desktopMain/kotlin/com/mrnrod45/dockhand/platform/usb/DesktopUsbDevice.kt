package com.mrnrod45.dockhand.platform.usb

import com.mrnrod45.dockhand.domain.usb.UsbConnection
import com.mrnrod45.dockhand.domain.usb.UsbDevice
import org.usb4java.Device
import org.usb4java.DeviceHandle
import org.usb4java.LibUsb

class DesktopUsbDevice(
    internal val device: Device,
    override val name: String,
    override val vendorId: Int,
    override val productId: Int
) : UsbDevice {

    override fun open(): UsbConnection? {
        val handle = DeviceHandle()
        val result = LibUsb.open(device, handle)
        if (result != LibUsb.SUCCESS) {
            return null
        }
        return DesktopUsbConnection(handle)
    }
}
