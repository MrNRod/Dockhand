package com.mrnrod45.dockhand.domain.usb

interface UsbConnection {
    fun claimInterface(interfaceNumber: Int): Boolean
    fun releaseInterface(interfaceNumber: Int): Boolean
    
    
    val bulkInEndpoint: Byte
    val bulkOutEndpoint: Byte
    val activeInterfaceIndex: Int
    
    /**
     * Performs a bulk transfer.
     * @param endpoint The endpoint address (e.g. 0x01 for OUT, 0x81 for IN)
     * @param data The buffer to send or receive data into
     * @param length The length of data to send or receive
     * @param timeout Timeout in milliseconds
     * @return The number of bytes transferred, or a negative error code
     */
    fun bulkTransfer(endpoint: Byte, data: ByteArray, length: Int, timeout: Int): Int
    
    fun close()
    suspend fun resetEndpoints()
}
