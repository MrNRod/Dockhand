package com.mrnrod45.nstk.domain.protocols

import com.mrnrod45.nstk.domain.models.UnifiedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class NcaInfo(
    val name: String,
    val offset: Long, // Relative to data section
    val size: Long,
    val isTicket: Boolean
)

class Pfs0(val file: UnifiedFile) {
    
    // Header size: 16
    // Entry size: 24
    
    private val PFS0_MAGIC = byteArrayOf(0x50, 0x46, 0x53, 0x30)
    
    var entries: List<NcaInfo> = emptyList()
        private set
        
    var headerSize: Long = 0
        private set

    suspend fun parse(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Read Header (16 bytes)
            val header = file.readChunk(0, 16)
            if (header.size != 16) return@withContext false
            
            // Check Magic
            if (!header.copyOfRange(0, 4).contentEquals(PFS0_MAGIC)) return@withContext false
            
            val filesCount = bytesToIntLE(header, 4)
            val stringTableSize = bytesToIntLE(header, 8)
            
            if (filesCount <= 0 || stringTableSize <= 0) return@withContext false
            
            // 2. Read File Entries (filesCount * 24)
            val entriesSize = filesCount * 24
            val entriesData = file.readChunk(16, entriesSize)
            if (entriesData.size != entriesSize) return@withContext false
            
            // 3. Read String Table
            val stringTableOffset = 16L + entriesSize
            val stringTable = file.readChunk(stringTableOffset, stringTableSize)
            if (stringTable.size != stringTableSize) return@withContext false
            
            headerSize = stringTableOffset + stringTableSize
            
            val tempEntries = mutableListOf<NcaInfo>()
            
            for (i in 0 until filesCount) {
                val offsetInEntries = i * 24
                val dataOffset = bytesToLongLE(entriesData, offsetInEntries)
                val dataSize = bytesToLongLE(entriesData, offsetInEntries + 8)
                val nameOffset = bytesToIntLE(entriesData, offsetInEntries + 16)
                
                // Parse Name from String Table
                // Name starts at nameOffset in stringTable, ends at null byte
                if (nameOffset >= stringTable.size) return@withContext false
                
                var nameEnd = nameOffset
                while (nameEnd < stringTable.size && stringTable[nameEnd] != 0.toByte()) {
                    nameEnd++
                }
                
                val nameBytes = stringTable.copyOfRange(nameOffset, nameEnd)
                val name = nameBytes.decodeToString()
                val isTicket = name.endsWith(".tik", ignoreCase = true)
                
                tempEntries.add(NcaInfo(name, dataOffset, dataSize, isTicket))
            }
            
            entries = tempEntries
            return@withContext true
            
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
    
    private fun bytesToIntLE(bytes: ByteArray, offset: Int): Int {
         var value = 0
         for (i in 0 until 4) {
             value = value or ((bytes[offset + i].toInt() and 0xFF) shl (8 * i))
         }
         return value
    }

    private fun bytesToLongLE(bytes: ByteArray, offset: Int): Long {
        var value = 0L
        for (i in 0 until 8) {
            value = value or ((bytes[offset + i].toLong() and 0xFF) shl (8 * i))
        }
        return value
    }
}
