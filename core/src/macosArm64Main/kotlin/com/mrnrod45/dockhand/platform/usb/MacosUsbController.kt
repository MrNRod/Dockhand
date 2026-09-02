package com.mrnrod45.dockhand.platform.usb

import com.mrnrod45.dockhand.domain.usb.UsbController
import com.mrnrod45.dockhand.domain.usb.UsbDevice
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import libusb.cinterop.LIBUSB_SUCCESS
import libusb.cinterop.libusb_bulk_transfer
import libusb.cinterop.libusb_close
import libusb.cinterop.libusb_control_transfer
import libusb.cinterop.libusb_claim_interface
import libusb.cinterop.libusb_detach_kernel_driver
import cnames.structs.libusb_device
import libusb.cinterop.libusb_device_descriptor
import cnames.structs.libusb_device_handle
import libusb.cinterop.libusb_free_device_list
import libusb.cinterop.libusb_get_device_descriptor
import libusb.cinterop.libusb_get_device_list
import libusb.cinterop.libusb_init
import libusb.cinterop.libusb_open
import libusb.cinterop.libusb_release_interface

class MacosUsbController : UsbController {

    init {
        val result = libusb_init(null)
        if (result != LIBUSB_SUCCESS) {
            println("Unable to initialize LibUsb. Result: $result")
        }
    }

    override val isRcmSupported: Boolean = true

    override fun listDevices(): List<UsbDevice> {
        return getDevices(0x057E, 0x0955)
    }

    override fun findRcmDevice(): UsbDevice? {
        // VID: 0x0955 (Nvidia), PID: 0x7321 (Switch RCM)
        return getDevices(0x0955).firstOrNull { it.productId == 0x7321 }
    }

    private fun getDevices(vararg allowedVendorIds: Int): List<UsbDevice> = memScoped {
        val listVar = alloc<CPointerVar<CPointerVar<libusb_device>>>()
        val count = libusb_get_device_list(null, listVar.ptr)
        if (count < 0) {
            println("Unable to get device list. Result: $count")
            return emptyList()
        }

        val list = listVar.value
        val devices = mutableListOf<UsbDevice>()
        try {
            if (list != null) {
                for (i in 0 until count.toInt()) {
                    val devicePtr = list[i] ?: continue
                    val descriptor = alloc<libusb_device_descriptor>()
                    val res = libusb_get_device_descriptor(devicePtr, descriptor.ptr)
                    if (res != LIBUSB_SUCCESS) continue

                    val vendorId = descriptor.idVendor.toInt() and 0xFFFF
                    val productId = descriptor.idProduct.toInt() and 0xFFFF

                    if (allowedVendorIds.contains(vendorId)) {
                        devices.add(
                            MacosUsbDevice(
                                device = devicePtr,
                                name = if (vendorId == 0x0955) "Switch RCM" else "Nintendo Switch",
                                vendorId = vendorId,
                                productId = productId
                            )
                        )
                    }
                }
            }
        } finally {
            libusb_free_device_list(list, 1)
        }
        devices
    }

    override suspend fun injectPayload(device: UsbDevice, payload: ByteArray): Boolean {
        if (device !is MacosUsbDevice) return false
        return memScoped {
            val handleVar = alloc<CPointerVar<libusb_device_handle>>()
            val openResult = libusb_open(device.device, handleVar.ptr)
            if (openResult != LIBUSB_SUCCESS) {
                println("Failed to open RCM device: $openResult")
                return false
            }
            val handle = handleVar.value ?: return false

            try {
                if (libusb_claim_interface(handle, 0) != LIBUSB_SUCCESS) {
                    libusb_detach_kernel_driver(handle, 0)
                    if (libusb_claim_interface(handle, 0) != LIBUSB_SUCCESS) {
                        println("Failed to claim interface")
                        return false
                    }
                }

                // Write Payload (Bulk Transfer to EP 1 OUT) in 4096-byte chunks
                val chunkSize = 4096
                var offset = 0
                val transferredVar = alloc<IntVar>()

                while (offset < payload.size) {
                    val end = minOf(offset + chunkSize, payload.size)
                    val length = end - offset
                    val chunk = payload.copyOfRange(offset, offset + length)

                    val res = chunk.usePinned { pinned ->
                        libusb_bulk_transfer(
                            handle,
                            0x01u,
                            pinned.addressOf(0).reinterpret(),
                            length,
                            transferredVar.ptr,
                            5000u
                        )
                    }
                    if (res != LIBUSB_SUCCESS) {
                        println("Bulk transfer failed at offset $offset: $res")
                        return false
                    }
                    offset += length
                }

                // Smash the stack (Trigger): GET_STATUS to EP0, Length 0x7000 (28672)
                val smashBuffer = ByteArray(28672)
                val smashResult = smashBuffer.usePinned { pinned ->
                    libusb_control_transfer(
                        handle,
                        0x82u,
                        0x00u, // LIBUSB_REQUEST_GET_STATUS
                        0u,
                        0u,
                        pinned.addressOf(0).reinterpret(),
                        smashBuffer.size.toUShort(),
                        1000u
                    )
                }

                // Result doesn't matter - the transfer itself triggers the exploit.
                println("Payload injected. Smash result: $smashResult")
                true
            } finally {
                libusb_release_interface(handle, 0)
                libusb_close(handle)
            }
        }
    }
}
