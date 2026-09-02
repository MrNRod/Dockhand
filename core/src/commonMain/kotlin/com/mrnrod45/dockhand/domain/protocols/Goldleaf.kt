package com.mrnrod45.dockhand.domain.protocols

import com.mrnrod45.dockhand.domain.models.*
import com.mrnrod45.dockhand.domain.usb.UsbConnection
import com.mrnrod45.dockhand.domain.util.IoDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class Goldleaf(
    private val connection: UsbConnection,
    private val logPrinter: LogPrinter
) {
    private val CMD_GLUC = byteArrayOf(0x47, 0x4c, 0x55, 0x43) // "GLUC"
    private val CMD_CONNECTION_REQUEST = byteArrayOf(0x00, 0x00, 0x00, 0x00)

    suspend fun start(files: Map<String, UnifiedFile>) = withContext(IoDispatcher) {
        logPrinter.print("Starting Goldleaf protocol", MsgType.INFO)
        
        // This implementation only serves a single NSP per session (Goldleaf's commands
        // 1/3/5 all address content by NCA index within one file, not by name), so if more
        // than one file was selected, only the first is installable via this session.
        if (files.isEmpty()) {
            logPrinter.print("GL No file selected to install", MsgType.FAIL)
            return@withContext
        }
        if (files.size > 1) {
            logPrinter.print("GL Goldleaf install only supports one file at a time; using '${files.values.first().name}'", MsgType.WARNING)
        }
        val file = files.values.first()
        val pfs = Pfs0(file)
        
        // Parse PFS0 headers immediately
        if (!pfs.parse()) {
             logPrinter.print("Failed to parse NSP header", MsgType.FAIL)
             return@withContext
        }
        
        // 1. Handshake ?
        // Legacy GoldLeaf logic is event driven:
        // "usb.read()" -> if GLUC -> process.
        // There is NO initial WRITE from Host.
        // Host waits for Switch to say "I'm here, here is a command".
        
        logPrinter.print("GL Waiting for Switch command...", MsgType.INFO)
        
        // Command Loop
        // readUsb has a 5000ms bulk-transfer timeout, so a null/empty result means "timed out",
        // not "disconnected". Bound the number of consecutive timeouts so a Switch that never
        // sends anything doesn't spin this loop forever, and check cancellation each iteration
        // so stopping the upload from the UI actually stops the USB polling.
        var consecutiveTimeouts = 0
        val maxConsecutiveTimeouts = 60 // ~5 minutes at 5s per timeout
        while (true) {
            ensureActive()
            val rxBuffer = readUsb(0x200)
            if (rxBuffer == null || rxBuffer.isEmpty()) {
                consecutiveTimeouts++
                if (consecutiveTimeouts >= maxConsecutiveTimeouts) {
                    logPrinter.print("GL Timed out waiting for Switch command", MsgType.FAIL)
                    return@withContext
                }
                continue
            }
            consecutiveTimeouts = 0

            // Check Magic GLUC
            if (rxBuffer.size >= 4 && rxBuffer.copyOfRange(0, 4).contentEquals(CMD_GLUC)) {
                 // Command ID (4 bytes)
                 val cmdInt = bytesToIntLE(rxBuffer, 4)
                 
                 when (cmdInt) {
                     1 -> { // ConnectionResponse
                         if (!handleConnectionResponse(file)) return@withContext
                     }
                     3 -> { // Start
                         if (!handleStart(pfs)) return@withContext
                     }
                     5 -> { // NSPContent
                         if (!handleNspContent(pfs, rxBuffer, isTicket = false)) return@withContext
                     }
                     6 -> { // NSPTicket
                         if (!handleNspContent(pfs, rxBuffer, isTicket = true)) return@withContext
                     }
                     7 -> { // Finish
                         logPrinter.print("GL Install Finished Successfully", MsgType.PASS)
                         return@withContext
                     }
                     else -> {
                         logPrinter.print("GL Unknown Command: $cmdInt", MsgType.WARNING)
                     }
                 }
            }
        }
    }
    
    private fun handleConnectionResponse(file: UnifiedFile): Boolean {
         logPrinter.print("GL Connected", MsgType.INFO)
         // Send: GLUC, NSPName(2), NameLen, Name
         if (writeUsb(CMD_GLUC)) return false
         
         val cmdNspName = byteArrayOf(0x02, 0x00, 0x00, 0x00)
         if (writeUsb(cmdNspName)) return false
         
         // Use a sanitized name or just file name
         val nameBytes = file.name.encodeToByteArray()
         if (writeUsb(intToBytesLE(nameBytes.size))) return false
         if (writeUsb(nameBytes)) return false
         
         return true
    }
    
    // Command 3
    private fun handleStart(pfs: Pfs0): Boolean {
         // Send: GLUC, NSPData(4), NCACount
         if (writeUsb(CMD_GLUC)) return false
         
         val cmdNspData = byteArrayOf(0x04, 0x00, 0x00, 0x00)
         if (writeUsb(cmdNspData)) return false
         
         if (writeUsb(intToBytesLE(pfs.entries.size))) return false
         
         pfs.entries.forEach { nca ->
             // Name (String)
             val ncaNameBytes = nca.name.encodeToByteArray()
             if (writeUsb(intToBytesLE(ncaNameBytes.size))) return false
             if (writeUsb(ncaNameBytes)) return false
             
             // Offset in NSP (Header Size + NCA Offset)
             val realOffset = pfs.headerSize + nca.offset
             if (writeUsb(longToBytesLE(realOffset))) return false
             
             // Size
             if (writeUsb(longToBytesLE(nca.size))) return false
         }
         return true
    }
    
    // Command 5/6
    private suspend fun handleNspContent(pfs: Pfs0, rxBuffer: ByteArray, isTicket: Boolean): Boolean {
        val ncaId = if (!isTicket) {
             // ID is at offset 8 (after GLUC(4) + CMD(4))
             if (rxBuffer.size >= 12) {
                 bytesToIntLE(rxBuffer, 8)
             } else {
                 // Try reading extra?
                 val extra = readUsb(4) ?: return false
                 bytesToIntLE(extra, 0)
             }
        } else {
             // Find Ticket ID (ends with .tik)
             pfs.entries.indexOfFirst { it.isTicket }
        }
        
        if (ncaId < 0 || ncaId >= pfs.entries.size) {
             logPrinter.print("GL Invalid NCA ID: $ncaId", MsgType.FAIL)
             return false
        }
        
        val nca = pfs.entries[ncaId]
        val realOffset = pfs.headerSize + nca.offset
        
        logPrinter.print("GL Sending Content: ${nca.name}", MsgType.INFO)
        return sendNcaData(pfs.file, realOffset, nca.size)
    }
    
    private suspend fun sendNcaData(file: UnifiedFile, offset: Long, size: Long): Boolean {
        var remaining = size
        var currentOffset = offset
        val chunkSize = 65536 // 64KB
        
        while (remaining > 0) {
             val toRead = minOf(remaining, chunkSize.toLong()).toInt()
             val chunk = file.readChunk(currentOffset, toRead)
             if (writeUsb(chunk)) return false
             
             currentOffset += toRead
             remaining -= toRead
        }
        return true
    }

    private fun writeUsb(data: ByteArray): Boolean {
        // Returns FALSE on SUCCESS (Legacy logic logic: return error?)
        // Wait, looking at my Tinfoil.kt, I used "return result != data.size" (True = Fail).
        // My code checks "if (writeUsb(...)) return false" -> Meaning TRUE = ERROR.
        // So writeUsb should return TRUE if ERROR.
        
        // Goldleaf uses Endpoint 1 too?
        val result = connection.bulkTransfer(connection.bulkOutEndpoint, data, data.size, 5000)
        return result != data.size // True if bytes transferred != data.size (Error)
    }
    
    private fun readUsb(length: Int): ByteArray? {
        val buffer = ByteArray(length)
        val result = connection.bulkTransfer(connection.bulkInEndpoint, buffer, length, 5000)
        if (result <= 0) return null
        return buffer.copyOf(result)
    }
    
    private fun bytesToIntLE(bytes: ByteArray, offset: Int): Int {
         if (offset + 4 > bytes.size) return 0
         var value = 0
         for (i in 0 until 4) {
             value = value or ((bytes[offset + i].toInt() and 0xFF) shl (8 * i))
         }
         return value
    }

    private fun intToBytesLE(v: Int): ByteArray {
        return byteArrayOf(
            (v and 0xFF).toByte(),
            ((v shr 8) and 0xFF).toByte(),
            ((v shr 16) and 0xFF).toByte(),
            ((v shr 24) and 0xFF).toByte()
        )
    }

    private fun longToBytesLE(v: Long): ByteArray {
        val bytes = ByteArray(8)
        for (i in 0 until 8) {
            bytes[i] = ((v shr (i * 8)) and 0xFF).toByte()
        }
        return bytes
    }
}
