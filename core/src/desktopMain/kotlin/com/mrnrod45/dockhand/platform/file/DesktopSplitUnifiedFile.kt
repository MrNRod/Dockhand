package com.mrnrod45.dockhand.platform.file

import com.mrnrod45.dockhand.domain.models.UnifiedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile

class DesktopSplitUnifiedFile(private val directory: File) : UnifiedFile {

    private val parts: List<File> = directory.listFiles()
        ?.filter { it.name.matches(Regex("\\d{2}")) }
        ?.sortedBy { it.name }
        ?: emptyList()

    override val name: String
        get() = directory.name

    override val size: Long
        get() = parts.sumOf { it.length() }

    override val isDirectory: Boolean
        get() = false // Treated as a file

    override val path: String
        get() = directory.absolutePath

    override fun listFiles(): List<UnifiedFile> = emptyList()

    override suspend fun readBytes(): ByteArray = withContext(Dispatchers.IO) {
        // Not recommended for large files, but implemented for interface compliance
        // Reads all parts into one array
        val totalSize = size
        if (totalSize > Int.MAX_VALUE) throw OutOfMemoryError("File too large for readBytes")
        
        val buffer = ByteArray(totalSize.toInt())
        var offset = 0
        parts.forEach { part ->
            part.readBytes().copyInto(buffer, offset)
            offset += part.length().toInt()
        }
        buffer
    }

    override suspend fun readChunk(offset: Long, size: Int): ByteArray = withContext(Dispatchers.IO) {
        if (parts.isEmpty()) return@withContext ByteArray(0)
        
        // Find which part contains the start offset
        var currentOffset = 0L
        var partIndex = 0
        
        // Calculate total size of previous parts to find the starting part
        while (partIndex < parts.size) {
            val partLen = parts[partIndex].length()
            if (offset < currentOffset + partLen) {
                break
            }
            currentOffset += partLen
            partIndex++
        }
        
        if (partIndex >= parts.size) return@withContext ByteArray(0) // Offset out of bounds
        
        val buffer = ByteArray(size)
        var bytesReadTotal = 0
        var internalSeek = offset - currentOffset
        
        while (bytesReadTotal < size && partIndex < parts.size) {
            val part = parts[partIndex]
            val partLen = part.length()
            val remainingInPart = partLen - internalSeek
            
            val toRead = minOf(size - bytesReadTotal, remainingInPart.toInt())
            
            RandomAccessFile(part, "r").use { raf ->
                raf.seek(internalSeek)
                val read = raf.read(buffer, bytesReadTotal, toRead)
                if (read > 0) {
                    bytesReadTotal += read
                }
            }
            
            internalSeek = 0 // Next parts start from 0
            partIndex++
        }
        
        if (bytesReadTotal < size) {
            buffer.copyOf(bytesReadTotal)
        } else {
            buffer
        }
    }
}
