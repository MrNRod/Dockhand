package com.mrnrod45.nstk.domain.protocols

import com.mrnrod45.nstk.domain.models.*
import com.mrnrod45.nstk.domain.usb.UsbConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Tinfoil(
    private val connection: UsbConnection,
    private val logPrinter: LogPrinter
) {
    // Standard Tinfoil Constants
    private val TUL0 = byteArrayOf(0x54, 0x55, 0x4c, 0x30) // "TUL0"
    private val TUC0 = byteArrayOf(0x54, 0x55, 0x43, 0x30) // "TUC0"
    
    // Sphaira Constants
    private val SPH_MAGIC = 0x53504830 // "SPH0" (Little Endian: 0x30, 0x48, 0x50, 0x53)
    private val SPH_PACKET_SIZE = 24
    
    // Commands (Standard & Sphaira Mapping)
    private val CMD_EXIT = 0x00.toByte()
    private val CMD_FILE_RANGE = 0x01.toByte()
    
    // Sphaira Specific Commands (From usb_api.hpp)
    private val SPH_CMD_QUIT = 0
    private val SPH_CMD_OPEN = 1
    private val SPH_RESULT_OK = 0
    
    // Protocol Mode
    private enum class Mode { TINFOIL, SPHAIRA }

    suspend fun start(files: Map<String, UnifiedFile>, isSphaira: Boolean) = withContext(Dispatchers.IO) {
        logPrinter.print("Starting ${if (isSphaira) "Sphaira" else "Standard Tinfoil/Awoo"} protocol...", MsgType.INFO)
        
        if (isSphaira) {
            // Sphaira Mode: Device Speaks First (Read)
            logPrinter.print("Mode: Sphaira (Reading Device Hello...)", MsgType.INFO)
            
            // Sphaira sends a Hello Packet (24 bytes) upon connection.
            // We MUST consume this before we can send our Reply.
            val hello = readUsb(SPH_PACKET_SIZE, 2000)
            if (hello == null || hello.size != SPH_PACKET_SIZE) {
                logPrinter.print("Warning: Sphaira Hello not received (Timeout/Size). Device might be waiting?", MsgType.WARNING)
            } else {
                val magic = bytesToIntLE(hello, 0)
                if (magic == SPH_MAGIC) {
                    logPrinter.print("Sphaira Hello Received (Magic: SPH0)", MsgType.PASS)
                } else {
                     logPrinter.print("Received Data (Magic: ${magic.toString(16)}), expecting SPH0.", MsgType.WARNING)
                }
            }
            
            runSphairaLoop(files)
        } else {
            // Standard Mode: Host Speaks First (Write)
            logPrinter.print("Mode: Standard (Sending Handshake...)", MsgType.INFO)
            runTinfoilLoop(files)
        }
    }
    
    // --- STANDARD TINFOIL IMPL ---
    
    private suspend fun runTinfoilLoop(files: Map<String, UnifiedFile>) {
        // Standard Tinfoil Handshake: Host Write First
        if (!sendListOfFilesTinfoil(files)) {
             logPrinter.print("TF Handshake failed", MsgType.FAIL)
             return
        }
        
        logPrinter.print("TF Handshake success. Waiting for commands...", MsgType.PASS)
        
        while (true) {
            val header = readUsb(0x200, 20000) 
            if (header == null) {
                // Keep alive check or timeout
                continue 
            }
            
            if (header.size < 12 || !header.copyOfRange(0, 4).contentEquals(TUC0)) {
                continue 
            }
            
            val cmdId = header[8]
            
            when (cmdId) {
                CMD_EXIT -> {
                    logPrinter.print("TF Received Exit Command", MsgType.PASS)
                    break 
                }
                CMD_FILE_RANGE -> {
                    if (!handleFileRangeTinfoil(header, files)) {
                        logPrinter.print("TF Handle File Range Failed", MsgType.FAIL)
                        break
                    }
                }
                else -> logPrinter.print("TF Unknown Command: $cmdId", MsgType.WARNING)
            }
        }
    }
    
    private fun sendListOfFilesTinfoil(files: Map<String, UnifiedFile>): Boolean {
        val sb = StringBuilder()
        files.values.forEach { sb.append(it.name).append('\n') }
        val namesBytes = sb.toString().toByteArray()
        
        // 1. Send Header (16 Bytes)
        val headerBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        headerBuffer.put(TUL0)
        headerBuffer.putInt(namesBytes.size)
        headerBuffer.put(ByteArray(8)) // Padding
        
        val headerPacket = headerBuffer.array()
        if (writeUsb(headerPacket)) { // writeUsb returns true on FAILURE
             logPrinter.print("Failed to send TUL0 Header", MsgType.FAIL)
             return false
        }
        
        // 2. Send Names
        if (writeUsb(namesBytes)) {
             logPrinter.print("Failed to send Names Data", MsgType.FAIL)
             return false
        }
        
        return true
    }

    private suspend fun handleFileRangeTinfoil(header: ByteArray, files: Map<String, UnifiedFile>): Boolean {
        val meta = readUsb(0x200) ?: return false
        val rangeSize = bytesToLongLE(meta, 0)
        val rangeOffset = bytesToLongLE(meta, 8)
        
        val nameData = readUsb(0x200) ?: return false
        val requestedName = nameData.decodeToString().trim { it <= ' ' }
        
        logPrinter.print("TF Req: $requestedName, Off: $rangeOffset, Size: $rangeSize", MsgType.INFO)
        val file = files[requestedName] ?: return false
        
        // Response Header
        val responseHeader = ByteArray(12)
        System.arraycopy(TUC0, 0, responseHeader, 0, 4)
        responseHeader[4] = 1; responseHeader[8] = 1
        
        if (writeUsb(responseHeader)) return false
        if (writeUsb(meta.copyOfRange(0, 8))) return false // Size
        if (writeUsb(ByteArray(12))) return false // Padding
        
        sendFileData(file, rangeOffset, rangeSize)
        return true
    }
    
    // --- SPHAIRA IMPL ---
    
    private suspend fun runSphairaLoop(files: Map<String, UnifiedFile>) {
        // We already detected SPH0 magic (consumed first packet).
        // That first packet was likely the "RESULT_OK" handshake from waitForConnection.
        // We need to reply to it with Names.
        
        // SPH Protocol Handshake:
        // 1. (Already Done) Switch -> Host: SendPacket(RESULT_OK) (Clean magic SPH0)
        // 2. Host -> Switch: ResultPacket(RESULT_OK, arg3=namesSize)
        // 3. Host -> Switch: Raw Names Data
        
        val sb = StringBuilder()
        files.values.forEach { sb.append(it.name).append('\n') }
        val namesBytes = sb.toString().toByteArray()
        
        // Send Reply Header
        logPrinter.print("SPH: Sending Handshake Reply (Size: ${namesBytes.size})", MsgType.INFO)
        if (!sendSphPacket(SPH_RESULT_OK, namesBytes.size, 0)) return
        
        // Send Raw Names (Not wrapped?)
        // In usb_installer.cpp: m_usb->TransferAll(true, names.data(), names.size()...)
        // TransferAll sends/reads RAW data usually.
        // But Sphaira always wraps?
        // Wait, "Read" method wraps data in result packet?
        // In WaitForConnection: "R_TRY(m_usb->TransferAll(true, names.data(), names.size(), timeout));"
        // This TransferAll (on Switch) reads from Host.
        // It does NOT verify CRC. It just reads raw bytes.
        // So YES, we send RAW names here.
        if (writeUsb(namesBytes)) {
             logPrinter.print("SPH: Failed to send name data", MsgType.FAIL)
             return
        }
        
        logPrinter.print("SPH: Handshake Complete. Waiting loop...", MsgType.PASS)
        
        while (true) {
            // Read next command packet (24 bytes)
            val packetData = readUsb(SPH_PACKET_SIZE, 0) // 0 = infinite wait (or long)
            if (packetData == null) break
            
            // Verify Magic & CRC
            val magic = bytesToIntLE(packetData, 0)
            if (magic != SPH_MAGIC) {
                logPrinter.print("SPH: Bad Magic in loop: ${magic.toString(16)}", MsgType.WARNING)
                continue
            }
            // Check CRC
            val receivedCrc = bytesToIntLE(packetData, 20)
            val calcCrc = Crc32c.calculate(packetData, 0, 20)
            if (receivedCrc != calcCrc) {
                 logPrinter.print("SPH: Bad CRC", MsgType.WARNING)
                 continue
            }
            
            val cmd = bytesToIntLE(packetData, 4) // arg2
            val arg3 = bytesToIntLE(packetData, 8)
            val arg4 = bytesToIntLE(packetData, 12)
            
            when (cmd) {
                SPH_CMD_OPEN -> {
                    // Open File Index
                    val index = arg3
                    // In older Tinfoil/Awoo, index matched the order in list.
                    // Let's find file by index.
                    val fileName = files.keys.toList().getOrNull(index)
                    if (fileName == null) {
                         sendSphPacket(1, 0, 0) // Error
                         continue
                    }
                    val file = files[fileName]!!
                    val size = file.size
                    logPrinter.print("SPH: Open $fileName (Size: $size)", MsgType.INFO)
                    
                    // Reply: ResultPacket(OK, arg3=flags|size_high, arg4=size_low)
                    // Flags = 0 (or 1 for stream?)
                    // Arg3 = (flags << 16) | (size >> 32)
                    // Arg4 = size & 0xFFFFFFFF
                    val flags = 0
                    val sizeHigh = (size ushr 32).toInt() and 0xFFFF
                    val arg3Resp = (flags shl 16) or sizeHigh
                    val arg4Resp = size.toInt()
                    
                    // Store current file for read operations
                    currentFile = file
                    
                    sendSphPacket(SPH_RESULT_OK, arg3Resp, arg4Resp)
                }
                else -> {
                    // Check for Data Read Request (Collision with CMD_QUIT=0)
                    // SendDataPacket puts OffsetHigh in arg2 (cmd).
                    // If Offset is < 4GB, arg2 is 0. 
                    // To distinguish from QUIT (also 0), check Size (arg4).
                    // QUIT has size=0. Read has size > 0.
                    
                    var isRead = false
                    if (cmd == 0 && arg4 > 0) {
                        isRead = true
                    } else if (cmd != 0) {
                         // Non-zero cmd might also be OffsetHigh > 0 (Very large file)
                         // We assume any non-mapped command with size > 0 is a read (or we check offset validity?)
                         // Realistically, cmd here is arg2.
                         // If it's not OPEN (1), and not QUIT (0 with size 0), it's likely part of an offset.
                         isRead = true
                    }
                    
                    if (isRead) {
                        // READ DATA
                        val offset = (cmd.toLong() shl 32) or (arg3.toLong() and 0xFFFFFFFFL)
                        val size = arg4
                        
                        // logPrinter.print("SPH: Read Off:$offset Size:$size", MsgType.INFO)
                        
                        if (currentFile == null) {
                             sendSphPacket(1, 0, 0)
                             continue
                        }
                        
                        val data = currentFile!!.readChunk(offset, size)
                        val crc = Crc32c.calculate(data)
                        
                        // Reply
                        sendSphPacket(SPH_RESULT_OK, data.size, crc)
                        
                        // Send Data (Raw)
                        writeUsb(data)
                    } else if (cmd == SPH_CMD_QUIT) {
                        logPrinter.print("SPH: Close File / Quit (Sending Ack)", MsgType.INFO)
                        sendSphPacket(SPH_RESULT_OK, 0, 0)
                        
                        // We continue the loop. If it was a real Quit, the device will disconnect 
                        // and next readUsb handles it.
                        continue
                    } else {
                        logPrinter.print("SPH: Unknown Cmd $cmd", MsgType.WARNING)
                    }
                }
            }
        }
    }
    
    private var currentFile: UnifiedFile? = null
    
    // --- UTILS ---
    
    private fun sendSphPacket(result: Int, arg3: Int, arg4: Int): Boolean {
        val buffer = ByteBuffer.allocate(SPH_PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(SPH_MAGIC)
        buffer.putInt(result) // arg2 (cmd/result)
        buffer.putInt(arg3)
        buffer.putInt(arg4)
        buffer.putInt(0) // arg5
        // CRC placeholder
        buffer.putInt(0)
        
        val packet = buffer.array()
        val crc = Crc32c.calculate(packet, 0, 20)
        
        // Put CRC at end
        val finalBuffer = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)
        finalBuffer.position(20)
        finalBuffer.putInt(crc)
        
        return writeUsb(packet) == false // writeUsb returns false on success (in old logic.. wait, checking below)
    }

    private fun writeUsb(data: ByteArray): Boolean {
        // Returns FALSE on SUCCESS (Legacy quirk I kept)
        val result = connection.bulkTransfer(connection.bulkOutEndpoint, data, data.size, 5000)
        if (result != data.size) {
            logPrinter.print("Write Failed: $result/${data.size}", MsgType.FAIL)
            return true // Fail
        }
        return false // Success
    }
    
    private fun readUsb(length: Int, timeout: Int = 5000): ByteArray? {
        val buffer = ByteArray(length)
        val result = connection.bulkTransfer(connection.bulkInEndpoint, buffer, length, timeout)
        if (result <= 0) return null
        return buffer.copyOf(result)
    }
    
    private suspend fun sendFileData(file: UnifiedFile, offset: Long, length: Long) {
        var currentOffset = offset
        var remaining = length
        val chunkSize = 16384
        while (remaining > 0) {
            val toRead = minOf(remaining, chunkSize.toLong()).toInt()
            val chunk = file.readChunk(currentOffset, toRead)
            if (writeUsb(chunk)) return
            currentOffset += toRead
            remaining -= toRead
        }
    }

    private fun intToBytesLE(v: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()
    }
    private fun bytesToIntLE(bytes: ByteArray, offset: Int): Int {
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
    }
    private fun bytesToLongLE(bytes: ByteArray, offset: Int): Long {
        return ByteBuffer.wrap(bytes, offset, 8).order(ByteOrder.LITTLE_ENDIAN).long
    }
}
