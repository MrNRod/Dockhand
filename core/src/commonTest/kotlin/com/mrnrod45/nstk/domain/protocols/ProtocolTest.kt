package com.mrnrod45.nstk.domain.protocols

import com.mrnrod45.nstk.domain.models.*
import com.mrnrod45.nstk.domain.usb.UsbConnection
import kotlin.test.Test
import kotlin.test.assertTrue

class ProtocolTest {

    class MockUsbConnection : UsbConnection {
        val sentData = mutableListOf<ByteArray>()
        val readResponses = mutableListOf<ByteArray>()
        
        override val bulkInEndpoint: Byte = 0x81.toByte()
        override val bulkOutEndpoint: Byte = 0x01.toByte()
        override val activeInterfaceIndex: Int = 0

        override fun claimInterface(interfaceNumber: Int): Boolean = true
        override fun releaseInterface(interfaceNumber: Int): Boolean = true
        override fun close() {}
        override suspend fun resetEndpoints() {}
        
        override fun bulkTransfer(endpoint: Byte, data: ByteArray, length: Int, timeout: Int): Int {
            if (endpoint == bulkOutEndpoint) { // OUT
                sentData.add(data.copyOf())
                return length
            } else if (endpoint == bulkInEndpoint) { // IN
                if (readResponses.isNotEmpty()) {
                    val resp = readResponses.removeAt(0)
                    if (resp.size <= length) {
                        resp.copyInto(data)
                        return resp.size
                    }
                }
                return 0 // No data / Timeout
            }
            return -1
        }
    }

    class MockLogPrinter : LogPrinter {
        val messages = mutableListOf<Pair<String, MsgType>>()
        override fun print(message: String, type: MsgType) {
            messages.add(message to type)
            println("MockLog: $message")
        }
        override fun updateProgress(value: Double) {}
        override fun update(files: Map<String, UnifiedFile>, status: FileStatus) {}
        override fun update(file: UnifiedFile, status: FileStatus) {}
        override fun close() {}
    }

    /** A minimal single-entry PFS0 (NSP) backed entirely by an in-memory byte array. */
    class MockNspFile(override val name: String = "test.nsp") : UnifiedFile {
        private val ncaName = "test.nca"
        private val ncaData = ByteArray(16) { it.toByte() }
        val bytes: ByteArray = run {
            val nameBytes = ncaName.encodeToByteArray()
            val stringTableSize = nameBytes.size + 1
            val entriesSize = 24
            val headerSize = 16 + entriesSize + stringTableSize
            val buf = ByteArray(headerSize + ncaData.size)

            "PFS0".encodeToByteArray().copyInto(buf, 0)
            leInt(1).copyInto(buf, 4) // filesCount
            leInt(stringTableSize).copyInto(buf, 8) // stringTableSize
            // bytes 12..15: reserved, left zero

            // Single entry: dataOffset(8) + dataSize(8) + nameOffset(4) + reserved(4)
            leLong(0).copyInto(buf, 16)
            leLong(ncaData.size.toLong()).copyInto(buf, 24)
            leInt(0).copyInto(buf, 32)

            nameBytes.copyInto(buf, 16 + entriesSize)
            ncaData.copyInto(buf, headerSize)
            buf
        }

        override val size: Long = bytes.size.toLong()
        override val isDirectory: Boolean = false
        override val path: String = "mock://$name"
        override fun listFiles(): List<UnifiedFile> = emptyList()
        override suspend fun readBytes(): ByteArray = bytes
        override suspend fun readChunk(offset: Long, size: Int): ByteArray {
            val start = offset.toInt()
            val end = minOf(start + size, bytes.size)
            return if (start >= bytes.size) ByteArray(0) else bytes.copyOfRange(start, end)
        }

        private fun leInt(v: Int) = byteArrayOf(
            (v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(),
            ((v shr 16) and 0xFF).toByte(), ((v shr 24) and 0xFF).toByte()
        )
        private fun leLong(v: Long) = ByteArray(8) { i -> ((v shr (i * 8)) and 0xFF).toByte() }
    }

    private fun goldleafCommandBuffer(cmd: Int): ByteArray {
        val buffer = ByteArray(512)
        byteArrayOf(0x47, 0x4c, 0x55, 0x43).copyInto(buffer, 0) // "GLUC"
        byteArrayOf(cmd.toByte(), 0, 0, 0).copyInto(buffer, 4)
        return buffer
    }

    @Test
    fun testGoldleafEmptyFileMapLogsFailureAndExits() = kotlinx.coroutines.test.runTest {
        val mockConnection = MockUsbConnection()
        val mockLogger = MockLogPrinter()
        val goldleaf = Goldleaf(mockConnection, mockLogger)

        goldleaf.start(emptyMap())

        // Must bail without touching the USB connection at all.
        assertTrue(mockConnection.sentData.isEmpty())
        assertTrue(mockLogger.messages.any { it.second == MsgType.FAIL })
    }

    @Test
    fun testGoldleafHandshake() = kotlinx.coroutines.test.runTest {
        val mockConnection = MockUsbConnection()
        val mockLogger = MockLogPrinter()
        val goldleaf = Goldleaf(mockConnection, mockLogger)
        val file = MockNspFile()

        // Switch sends ConnectionResponse (cmd 1), then Finish (cmd 7).
        mockConnection.readResponses.add(goldleafCommandBuffer(1))
        mockConnection.readResponses.add(goldleafCommandBuffer(7))

        goldleaf.start(mapOf(file.name to file))

        // ConnectionResponse handling writes GLUC, then a NSPName command with the file name.
        assertTrue(mockConnection.sentData.isNotEmpty())
        assertTrue(mockConnection.sentData[0].contentEquals(byteArrayOf(0x47, 0x4c, 0x55, 0x43)))
        assertTrue(mockLogger.messages.any { it.second == MsgType.PASS })
    }

    @Test
    fun testTinFoilHandshake() = kotlinx.coroutines.test.runTest {
        val mockConnection = MockUsbConnection()
        val mockLogger = MockLogPrinter()
        val tinfoil = Tinfoil(mockConnection, mockLogger)
        
        // Mock Exit command
        // Tinfoil check: header size >= 12 && starts with TUC0.
        // Cmd ID is at index 8. CMD_EXIT = 0.
        // TUC0 = 0x54, 0x55, 0x43, 0x30
        val tuc0 = byteArrayOf(0x54, 0x55, 0x43, 0x30)
        val buffer = ByteArray(512)
        tuc0.copyInto(buffer, 0)
        buffer[8] = 0x00 // Exit
        
        mockConnection.readResponses.add(buffer)
        
        tinfoil.start(emptyMap<String, UnifiedFile>(), isSphaira = false)
        
        assertTrue(mockConnection.sentData.isNotEmpty())
        // Should have sent TUL0 header
        assertTrue(mockConnection.sentData[0].size >= 4)
        assertTrue(mockConnection.sentData[0].copyOfRange(0, 4).contentEquals(byteArrayOf(0x54, 0x55, 0x4c, 0x30)))
    }
}
