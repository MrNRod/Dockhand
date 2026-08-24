package com.mrnrod45.nstk.platform.usb

import com.mrnrod45.nstk.domain.usb.UsbConnection
import org.usb4java.DeviceHandle
import org.usb4java.LibUsb
import java.nio.ByteBuffer
import java.nio.IntBuffer

class DesktopUsbConnection(private val deviceHandle: DeviceHandle) : UsbConnection {
    override var bulkInEndpoint: Byte = 0x81.toByte()
    override var bulkOutEndpoint: Byte = 0x01.toByte()
    override var activeInterfaceIndex: Int = 0

    init {
        // Simple endpoint discovery for Interface 0
        val device = LibUsb.getDevice(deviceHandle)
        val config = org.usb4java.ConfigDescriptor()
        if (LibUsb.getActiveConfigDescriptor(device, config) == LibUsb.SUCCESS) {
            // Find Interface 0
             // usb4java: iface is array of InterfaceHeader? No. 
             // ConfigDescriptor.iface() returns array of Interface arrays (alt settings)
             // We want Interface 0, Alt Setting 0
             
            // Iterate ALL Interfaces to see what's available
            println("USB: Scanning Interfaces...")
            for (i in 0 until config.bNumInterfaces()) {
                val iface = config.iface()[i.toInt()]
                if (iface.numAltsetting().toInt() > 0) {
                     val ifaceDesc = iface.altsetting()[0]
                     println("USB: Interface ${ifaceDesc.bInterfaceNumber()} (Class: ${ifaceDesc.bInterfaceClass()}, Sub: ${ifaceDesc.bInterfaceSubClass()}, Proto: ${ifaceDesc.bInterfaceProtocol()})")
                     
                     for (e in 0 until ifaceDesc.bNumEndpoints()) {
                         val ep = ifaceDesc.endpoint()[e.toInt()]
                         val addr = ep.bEndpointAddress()
                         val attr = ep.bmAttributes()
                         val type = attr.toInt() and LibUsb.TRANSFER_TYPE_MASK.toInt()
                         val dir = addr.toInt() and LibUsb.ENDPOINT_DIR_MASK.toInt()
                         val typeStr = when(type) {
                             LibUsb.TRANSFER_TYPE_BULK.toInt() -> "BULK"
                             LibUsb.TRANSFER_TYPE_INTERRUPT.toInt() -> "INTR"
                             LibUsb.TRANSFER_TYPE_ISOCHRONOUS.toInt() -> "ISO"
                             LibUsb.TRANSFER_TYPE_CONTROL.toInt() -> "CTRL"
                             else -> "UNKNOWN"
                         }
                         val dirStr = if (dir == LibUsb.ENDPOINT_IN.toInt()) "IN" else "OUT"
                         println("  - Endpoint 0x${(addr.toInt() and 0xFF).toString(16)} ($dirStr $typeStr)")
                     }
                     
                }
            }
             // Safer iteration
             var foundInterface = -1
             
             // Scan all interfaces to find one with Bulk IN and OUT
             for (i in 0 until config.bNumInterfaces()) {
                 val iface = config.iface()[i.toInt()]
                 if (iface.numAltsetting().toInt() > 0) {
                      val ifaceDesc = iface.altsetting()[0]
                      
                      var hasIn = false
                      var hasOut = false
                      var tempIn: Byte = 0
                      var tempOut: Byte = 0
                      
                      // Iterate Endpoints
                      for (e in 0 until ifaceDesc.bNumEndpoints()) {
                          val ep = ifaceDesc.endpoint()[e.toInt()]
                          val addr = ep.bEndpointAddress()
                          val attr = ep.bmAttributes()
                          
                          if ((attr.toInt() and LibUsb.TRANSFER_TYPE_MASK.toInt()) == LibUsb.TRANSFER_TYPE_BULK.toInt()) {
                              if ((addr.toInt() and LibUsb.ENDPOINT_DIR_MASK.toInt()) == LibUsb.ENDPOINT_IN.toInt()) {
                                  tempIn = addr
                                  hasIn = true
                              } else {
                                  tempOut = addr
                                  hasOut = true
                              }
                          }
                      }
                      
                      if (hasIn && hasOut) {
                          // Found a candidate!
                          bulkInEndpoint = tempIn
                          bulkOutEndpoint = tempOut
                          foundInterface = ifaceDesc.bInterfaceNumber().toInt()
                          println("USB: Found valid Bulk Interface: $foundInterface with Endpoints -> IN: 0x${(tempIn.toInt() and 0xFF).toString(16)}, OUT: 0x${(tempOut.toInt() and 0xFF).toString(16)}")
                          break 
                      }
                 }
             }
             
             if (foundInterface != -1) {
                 activeInterfaceIndex = foundInterface
             } else {
                 println("USB: No valid Bulk Interface found. Defaulting to 0.")
             }
             
             LibUsb.freeConfigDescriptor(config)
        }
        
        println("USB: Initialized Connection. Active Interface: $activeInterfaceIndex. Endpoints -> IN: 0x${(bulkInEndpoint.toInt() and 0xFF).toString(16)}, OUT: 0x${(bulkOutEndpoint.toInt() and 0xFF).toString(16)}")
    }
    override fun claimInterface(interfaceNumber: Int): Boolean {
        // Ensure configuration is set (usually 1)
        val currentConfig = IntBuffer.allocate(1)
        if (LibUsb.getConfiguration(deviceHandle, currentConfig) == LibUsb.SUCCESS) {
             if (currentConfig.get() != 1) {
                 println("USB: Setting Configuration to 1. Current: ${currentConfig.get(0)}")
                 LibUsb.setConfiguration(deviceHandle, 1)
             }
        }
    
        var result = LibUsb.claimInterface(deviceHandle, interfaceNumber)
        if (result != LibUsb.SUCCESS) {
            println("USB: Claim failed ($result), attempting detach kernel driver...")
            // Try detaching kernel driver
            val detachRes = LibUsb.detachKernelDriver(deviceHandle, interfaceNumber)
            if (detachRes != LibUsb.SUCCESS && detachRes != LibUsb.ERROR_NOT_FOUND) {
                // ERROR_NOT_FOUND means no kernel driver was attached, which is fine.
                println("USB: Detach Kernel Driver failed: $detachRes")
            }
            
            result = LibUsb.claimInterface(deviceHandle, interfaceNumber)
        }
        
        if (result != LibUsb.SUCCESS) {
            println("USB: Final Claim Interface failed: $result (${LibUsb.errorName(result)})")
        } else {
            // Success! 
            // We do NOT automatically reset endpoints here.
            // Resetting OUT endpoint can disturb legacy devices (Awoo) waiting for data.
            // Resetting IN endpoint is usually harmless but unnecessary on fresh claim.
            // val clearIn = LibUsb.clearHalt(deviceHandle, bulkInEndpoint)
            // if (clearIn != LibUsb.SUCCESS) println("USB: Clear Halt IN failed: $clearIn")
            
            // val clearOut = LibUsb.clearHalt(deviceHandle, bulkOutEndpoint)
            // if (clearOut != LibUsb.SUCCESS) println("USB: Clear Halt OUT failed: $clearOut")
            
            println("USB: Interface Claimed")
        }
        return result == LibUsb.SUCCESS
    }

    override fun releaseInterface(interfaceNumber: Int): Boolean {
        val result = LibUsb.releaseInterface(deviceHandle, interfaceNumber)
        if (result != LibUsb.SUCCESS) {
             println("USB: Release Interface failed: $result")
        }
        return result == LibUsb.SUCCESS
    }

    override fun bulkTransfer(endpoint: Byte, data: ByteArray, length: Int, timeout: Int): Int {
        val buffer = ByteBuffer.allocateDirect(length)
        buffer.put(data, 0, length)
        buffer.rewind() // Important for reading from buffer? No, for writing to device? 
        // Logic check: bulkTransfer reads from buffer if OUT, writes to buffer if IN.
        // For OUT (host to device): we put data into buffer.
        // For IN (device to host): buffer is empty, we read into it.
        // But the signature of the interface takes 'data' for both? 
        // The interface was: fun bulkTransfer(endpoint: Byte, data: ByteArray, length: Int, timeout: Int): Int
        // If it's IN endpoint, 'data' is where we write result? Yes.
        
        // Wait, usb4java ByteBuffer must be direct.
        // If OUT (endpoint & 0x80 == 0): Copy data -> DirectBuffer -> LibUsb
        // If IN (endpoint & 0x80 == 0x80): LibUsb -> DirectBuffer -> Copy to data
        
        val isIn = (endpoint.toInt() and 0x80) != 0
        if (!isIn) {
             // OUT: Data to Device
             // buffer already populated above
        } else {
             // IN: Device to Data
             buffer.clear()
        }

        val transferred = IntBuffer.allocate(1)
        val result = LibUsb.bulkTransfer(deviceHandle, endpoint, buffer, transferred, timeout.toLong())

        if (result != LibUsb.SUCCESS) {
            return result // -1, -2, etc. error codes
        }

        val bytesTransferred = transferred.get()
        
        if (isIn) {
            if (bytesTransferred > 0) {
                 buffer.get(data, 0, bytesTransferred)
            }
        }
        
        return bytesTransferred
    }

    override fun close() {
        LibUsb.close(deviceHandle)
    }
    
    override suspend fun resetEndpoints() {
        // Only reset IN endpoint because that's the one that timed out during detection.
        // Resetting OUT might confuse the device if it's waiting for data.
        val clearIn = LibUsb.clearHalt(deviceHandle, bulkInEndpoint)
        if (clearIn != LibUsb.SUCCESS) println("USB: Clear Halt IN failed: $clearIn")
        
        // val clearOut = LibUsb.clearHalt(deviceHandle, bulkOutEndpoint)
        // if (clearOut != LibUsb.SUCCESS) println("USB: Clear Halt OUT failed: $clearOut")
    }
}
