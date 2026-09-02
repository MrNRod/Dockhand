package com.mrnrod45.dockhand.platform.usb

import com.mrnrod45.dockhand.domain.usb.UsbConnection
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import libusb.cinterop.LIBUSB_ENDPOINT_IN
import libusb.cinterop.LIBUSB_ENDPOINT_OUT
import libusb.cinterop.LIBUSB_SUCCESS
import libusb.cinterop.LIBUSB_TRANSFER_TYPE_BULK
import libusb.cinterop.LIBUSB_TRANSFER_TYPE_MASK
import libusb.cinterop.libusb_clear_halt
import libusb.cinterop.libusb_close
import libusb.cinterop.libusb_config_descriptor
import libusb.cinterop.libusb_detach_kernel_driver
import cnames.structs.libusb_device_handle
import libusb.cinterop.libusb_get_active_config_descriptor
import libusb.cinterop.libusb_get_configuration
import libusb.cinterop.libusb_get_device
import libusb.cinterop.libusb_bulk_transfer
import libusb.cinterop.libusb_claim_interface
import libusb.cinterop.libusb_free_config_descriptor
import libusb.cinterop.libusb_release_interface
import libusb.cinterop.libusb_set_configuration

class MacosUsbConnection(private val deviceHandle: CPointer<libusb_device_handle>) : UsbConnection {
    override var bulkInEndpoint: Byte = 0x81.toByte()
    override var bulkOutEndpoint: Byte = 0x01.toByte()
    override var activeInterfaceIndex: Int = 0

    init {
        // Simple endpoint discovery for Interface 0 - scan all interfaces for a Bulk IN + Bulk OUT pair.
        memScoped {
            val device = libusb_get_device(deviceHandle)
            val configVar = alloc<CPointerVar<libusb_config_descriptor>>()
            if (device != null && libusb_get_active_config_descriptor(device, configVar.ptr) == LIBUSB_SUCCESS) {
                val config = configVar.value?.pointed
                if (config != null) {
                    var foundInterface = -1
                    var tempIn: Byte = 0
                    var tempOut: Byte = 0

                    for (i in 0 until config.bNumInterfaces.toInt()) {
                        val iface = config.`interface`!![i]
                        if (iface.num_altsetting > 0) {
                            val ifaceDesc = iface.altsetting!![0]

                            var hasIn = false
                            var hasOut = false
                            var candidateIn: Byte = 0
                            var candidateOut: Byte = 0

                            for (e in 0 until ifaceDesc.bNumEndpoints.toInt()) {
                                val ep = ifaceDesc.endpoint!![e]
                                val addr = ep.bEndpointAddress.toUInt() and 0xFFu
                                val attr = ep.bmAttributes.toInt() and 0xFF

                                if ((attr and LIBUSB_TRANSFER_TYPE_MASK) == LIBUSB_TRANSFER_TYPE_BULK.toInt()) {
                                    if ((addr and LIBUSB_ENDPOINT_IN) == LIBUSB_ENDPOINT_IN) {
                                        candidateIn = addr.toByte()
                                        hasIn = true
                                    } else {
                                        candidateOut = addr.toByte()
                                        hasOut = true
                                    }
                                }
                            }

                            if (hasIn && hasOut) {
                                tempIn = candidateIn
                                tempOut = candidateOut
                                foundInterface = ifaceDesc.bInterfaceNumber.toInt()
                                break
                            }
                        }
                    }

                    if (foundInterface != -1) {
                        bulkInEndpoint = tempIn
                        bulkOutEndpoint = tempOut
                        activeInterfaceIndex = foundInterface
                    }

                    libusb_free_config_descriptor(configVar.value)
                }
            }
        }
        println("USB: Initialized Connection. Active Interface: $activeInterfaceIndex. Endpoints -> IN: 0x${(bulkInEndpoint.toInt() and 0xFF).toString(16)}, OUT: 0x${(bulkOutEndpoint.toInt() and 0xFF).toString(16)}")
    }

    override fun claimInterface(interfaceNumber: Int): Boolean = memScoped {
        val currentConfig = alloc<IntVar>()
        if (libusb_get_configuration(deviceHandle, currentConfig.ptr) == LIBUSB_SUCCESS) {
            if (currentConfig.value != 1) {
                libusb_set_configuration(deviceHandle, 1)
            }
        }

        var result = libusb_claim_interface(deviceHandle, interfaceNumber)
        if (result != LIBUSB_SUCCESS) {
            libusb_detach_kernel_driver(deviceHandle, interfaceNumber)
            result = libusb_claim_interface(deviceHandle, interfaceNumber)
        }

        if (result == LIBUSB_SUCCESS) {
            println("USB: Interface Claimed")
        } else {
            println("USB: Final Claim Interface failed: $result")
        }
        result == LIBUSB_SUCCESS
    }

    override fun releaseInterface(interfaceNumber: Int): Boolean {
        val result = libusb_release_interface(deviceHandle, interfaceNumber)
        if (result != LIBUSB_SUCCESS) {
            println("USB: Release Interface failed: $result")
        }
        return result == LIBUSB_SUCCESS
    }

    override fun bulkTransfer(endpoint: Byte, data: ByteArray, length: Int, timeout: Int): Int = memScoped {
        val transferredVar = alloc<IntVar>()
        val result = data.usePinned { pinned ->
            libusb_bulk_transfer(
                deviceHandle,
                endpoint.toUByte(),
                pinned.addressOf(0).reinterpret(),
                length,
                transferredVar.ptr,
                timeout.toUInt()
            )
        }

        if (result != LIBUSB_SUCCESS) {
            return result
        }

        transferredVar.value
    }

    override fun close() {
        libusb_close(deviceHandle)
    }

    override suspend fun resetEndpoints() {
        // Only reset IN endpoint - resetting OUT can confuse a device waiting for data.
        val clearIn = libusb_clear_halt(deviceHandle, bulkInEndpoint.toUByte())
        if (clearIn != LIBUSB_SUCCESS) println("USB: Clear Halt IN failed: $clearIn")
    }
}
