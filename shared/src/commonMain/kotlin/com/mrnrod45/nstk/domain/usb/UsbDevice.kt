package com.mrnrod45.nstk.domain.usb

interface UsbDevice {
    val name: String
    val vendorId: Int
    val productId: Int
    
    fun open(): UsbConnection?
}
