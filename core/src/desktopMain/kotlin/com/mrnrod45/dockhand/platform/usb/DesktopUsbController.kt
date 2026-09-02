package com.mrnrod45.dockhand.platform.usb

import com.mrnrod45.dockhand.domain.usb.UsbController
import com.mrnrod45.dockhand.domain.usb.UsbDevice
import org.usb4java.DeviceDescriptor
import org.usb4java.DeviceList
import org.usb4java.LibUsb

class DesktopUsbController : UsbController {
    
    init {
        // Force use of system libusb to avoid missing native library issues on Apple Silicon (darwin-aarch64)
        // since usb4java 1.3.0 bundled natives don't support it.
        try {
            System.setProperty("org.usb4java.libusb.useSystemLibUsb", "true")
        } catch (e: Exception) {
            println("Failed to set system property: ${e.message}")
        }

        // Initialize LibUsb context (default)
        val result = LibUsb.init(null)
        if (result != LibUsb.SUCCESS) {
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

    private fun getDevices(vararg allowedVendorIds: Int): List<UsbDevice> {
        val list = DeviceList()
        val result = LibUsb.getDeviceList(null, list)
        if (result < 0) {
            println("Unable to get device list. Result: $result")
            return emptyList()
        }

        val devices = mutableListOf<UsbDevice>()
        try {
            for (device in list) {
                val descriptor = DeviceDescriptor()
                val res = LibUsb.getDeviceDescriptor(device, descriptor)
                if (res != LibUsb.SUCCESS) continue

                val vendorId = descriptor.idVendor().toInt() and 0xFFFF
                val productId = descriptor.idProduct().toInt() and 0xFFFF

                if (allowedVendorIds.contains(vendorId)) {
                    LibUsb.refDevice(device)
                    devices.add(
                        DesktopUsbDevice(
                            device = device,
                            name = if (vendorId == 0x0955) "Switch RCM" else "Nintendo Switch",
                            vendorId = vendorId,
                            productId = productId
                        )
                    )
                }
            }
        } finally {
            LibUsb.freeDeviceList(list, true)
        }
        return devices
    }

    override suspend fun injectPayload(device: UsbDevice, payload: ByteArray): Boolean {
        if (device !is DesktopUsbDevice) return false
        
        val handle = org.usb4java.DeviceHandle()
        val result = LibUsb.open(device.device, handle)
        if (result != LibUsb.SUCCESS) {
            println("Failed to open RCM device: $result")
            return false
        }

        try {
            // Claim interface 0
            if (LibUsb.claimInterface(handle, 0) != LibUsb.SUCCESS) {
                // Try detach kernel driver if needed
                LibUsb.detachKernelDriver(handle, 0)
                if (LibUsb.claimInterface(handle, 0) != LibUsb.SUCCESS) {
                     println("Failed to claim interface")
                     return false
                }
            }

            // Write Payload (Bulk Transfer to EP 1 OUT)
            val chunkSize = 4096
            var offset = 0
            val writeBuffer = java.nio.ByteBuffer.allocateDirect(chunkSize)
            val transferred = java.nio.IntBuffer.allocate(1)
            
            // Loop through payload in 4096 byte chunks
            while (offset < payload.size) {
                 val end = minOf(offset + chunkSize, payload.size)
                 val length = end - offset

                 writeBuffer.clear()
                 writeBuffer.put(payload, offset, length)
                 writeBuffer.flip() // bulkTransfer sends [position, limit); without this, remaining() is 0 and nothing is sent.

                 val res = LibUsb.bulkTransfer(handle, 0x01.toByte(), writeBuffer, transferred, 5000)
                 if (res != LibUsb.SUCCESS) {
                     println("Bulk transfer failed at offset $offset: $res")
                     return false
                 }
                 offset += length
            }

            // Smash the stack (Trigger)
            // MacOS method: GET_STATUS (0x82) to EP 0, Length 0x7000 (28672)
            // Legacy Rcm.java smashMacOS():
            // LibUsb.controlTransfer(handler, (byte) 0x82, LibUsb.REQUEST_GET_STATUS, (short) 0, (short) 0, writeBuffer, 1000);
            
            // Wait, writeBuffer for Control Transfer? 
            // The buffer size is the "Length" requested.
            // Length 28672 triggers the smash.
            val smashBuffer = java.nio.ByteBuffer.allocateDirect(28672)
            val smashRes = LibUsb.controlTransfer(
                handle, 
                0x82.toByte(), // bmRequestType: Device-to-Host | Standard | Endpoint (0x80 | 0x00 | 0x02 = 0x82 seems wrong for Endpoint?)
                // Legacy says 0x82: Standard Request (0), Interface? No.
                // 0x80 (Dir=In) | 0x00 (Type=Standard) | 0x02 (Rec=Endpoint) = 0x82. Correct.
                LibUsb.REQUEST_GET_STATUS, // bRequest (0x00)
                0.toShort(), // wValue
                0.toShort(), // wIndex
                smashBuffer, // Buffer (determines wLength)
                1000 // Timeout
            )
            
            // The result doesn't matter (it usually errors or returns partial), the transfer itself triggers the exploit.
            
            println("Payload injected. Smash result: $smashRes")
            return true

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            LibUsb.releaseInterface(handle, 0)
            LibUsb.close(handle)
        }
    }
}
