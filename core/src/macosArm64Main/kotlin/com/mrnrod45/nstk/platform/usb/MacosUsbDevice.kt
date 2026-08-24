package com.mrnrod45.nstk.platform.usb

import com.mrnrod45.nstk.domain.usb.UsbConnection
import com.mrnrod45.nstk.domain.usb.UsbDevice
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import cnames.structs.libusb_device
import cnames.structs.libusb_device_handle
import libusb.cinterop.libusb_open
import libusb.cinterop.libusb_ref_device

class MacosUsbDevice(
    internal val device: CPointer<libusb_device>,
    override val name: String,
    override val vendorId: Int,
    override val productId: Int
) : UsbDevice {

    init {
        libusb_ref_device(device)
    }

    override fun open(): UsbConnection? = memScoped {
        val handleVar = alloc<CPointerVar<libusb_device_handle>>()
        val result = libusb_open(device, handleVar.ptr)
        if (result != 0) return null
        val handle = handleVar.value ?: return null
        MacosUsbConnection(handle)
    }
}
