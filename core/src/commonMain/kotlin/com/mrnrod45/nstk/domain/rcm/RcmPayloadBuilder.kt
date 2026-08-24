package com.mrnrod45.nstk.domain.rcm

import com.mrnrod45.nstk.domain.models.UnifiedFile
import com.mrnrod45.nstk.domain.util.IoDispatcher
import kotlinx.coroutines.withContext

class RcmPayloadBuilder {

    private val initSeq = byteArrayOf(0x98.toByte(), 0x02.toByte(), 0x03.toByte())

    private val mezzo = byteArrayOf(
        0x5c.toByte(), 0x00.toByte(), 0x9f.toByte(), 0xe5.toByte(), 0x5c.toByte(), 0x10.toByte(), 0x9f.toByte(), 0xe5.toByte(),   0x5c.toByte(), 0x20.toByte(), 0x9f.toByte(), 0xe5.toByte(), 0x01.toByte(), 0x20.toByte(), 0x42.toByte(), 0xe0.toByte(),
        0x0e.toByte(), 0x00.toByte(), 0x00.toByte(), 0xeb.toByte(), 0x48.toByte(), 0x00.toByte(), 0x9f.toByte(), 0xe5.toByte(),   0x10.toByte(), 0xff.toByte(), 0x2f.toByte(), 0xe1.toByte(), 0x00.toByte(), 0x00.toByte(), 0xa0.toByte(), 0xe1.toByte(),
        0x48.toByte(), 0x00.toByte(), 0x9f.toByte(), 0xe5.toByte(), 0x48.toByte(), 0x10.toByte(), 0x9f.toByte(), 0xe5.toByte(),   0x01.toByte(), 0x29.toByte(), 0xa0.toByte(), 0xe3.toByte(), 0x07.toByte(), 0x00.toByte(), 0x00.toByte(), 0xeb.toByte(),
        0x38.toByte(), 0x00.toByte(), 0x9f.toByte(), 0xe5.toByte(), 0x01.toByte(), 0x19.toByte(), 0xa0.toByte(), 0xe3.toByte(),   0x01.toByte(), 0x00.toByte(), 0x80.toByte(), 0xe0.toByte(), 0x34.toByte(), 0x10.toByte(), 0x9f.toByte(), 0xe5.toByte(),
        0x03.toByte(), 0x28.toByte(), 0xa0.toByte(), 0xe3.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0xeb.toByte(),   0x20.toByte(), 0x00.toByte(), 0x9f.toByte(), 0xe5.toByte(), 0x10.toByte(), 0xff.toByte(), 0x2f.toByte(), 0xe1.toByte(),
        0x04.toByte(), 0x30.toByte(), 0x91.toByte(), 0xe4.toByte(), 0x04.toByte(), 0x30.toByte(), 0x80.toByte(), 0xe4.toByte(),   0x04.toByte(), 0x20.toByte(), 0x52.toByte(), 0xe2.toByte(), 0xfb.toByte(), 0xff.toByte(), 0xff.toByte(), 0x1a.toByte(),
        0x1e.toByte(), 0xff.toByte(), 0x2f.toByte(), 0xe1.toByte(), 0x00.toByte(), 0xf0.toByte(), 0x00.toByte(), 0x40.toByte(),   0x20.toByte(), 0x00.toByte(), 0x01.toByte(), 0x40.toByte(), 0x7c.toByte(), 0x00.toByte(), 0x01.toByte(), 0x40.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x40.toByte(), 0x40.toByte(), 0x0e.toByte(), 0x01.toByte(), 0x40.toByte(),   0x00.toByte(), 0x70.toByte(), 0x01.toByte(), 0x40.toByte()
    ) // 124 bytes

    private val sprayPttrn = byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x40.toByte())

    suspend fun buildPayload(payloadFile: UnifiedFile): Result<ByteArray> = withContext(IoDispatcher) {
        try {
            val bytes = payloadFile.readBytes()
            val payload = buildFromBytes(bytes)
            Result.success(payload)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Helper that takes raw bytes (so we can solve the I/O problem elsewhere for now)
    fun buildFromBytes(payloadBytes: ByteArray): ByteArray {
        val pldFileSize = payloadBytes.size
        
        // Validation per legacy code
        if (pldFileSize > 126296 || pldFileSize < 16384) {
             throw IllegalArgumentException("Payload size invalid: $pldFileSize. Must be between 16384 and 126296 bytes.")
        }

        var totalSize = 4328 + pldFileSize + 8640
        totalSize += 4096 - (totalSize % 4096)
        
        if ((totalSize / 4096 % 2) == 0) {
            totalSize += 4096
        }

        if (totalSize > 0x30298) {
            throw IllegalArgumentException("Total payload size too big: $totalSize")
        }

        val fullPayload = ByteArray(totalSize)

        // Assembly
        initSeq.copyInto(fullPayload, destinationOffset = 0, startIndex = 0, endIndex = 3)
        mezzo.copyInto(fullPayload, destinationOffset = 680, startIndex = 0, endIndex = 124)
        payloadBytes.copyInto(fullPayload, destinationOffset = 4328, startIndex = 0, endIndex = 16384)

        for (i in 0 until 2160) {
            sprayPttrn.copyInto(fullPayload, destinationOffset = 20712 + i * 4, startIndex = 0, endIndex = 4)
        }

        payloadBytes.copyInto(fullPayload, destinationOffset = 29352, startIndex = 16384, endIndex = pldFileSize)

        return fullPayload
    }
}
