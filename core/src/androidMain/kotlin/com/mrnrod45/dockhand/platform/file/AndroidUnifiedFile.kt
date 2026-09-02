package com.mrnrod45.dockhand.platform.file

import com.mrnrod45.dockhand.domain.models.UnifiedFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidUnifiedFile(private val file: File) : UnifiedFile {

    override val name: String
        get() = file.name

    override val size: Long
        get() = if (isDirectory) {
            file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else {
            file.length()
        }

    override val isDirectory: Boolean
        get() = file.isDirectory
        
    override val path: String
        get() = file.absolutePath

    override fun listFiles(): List<UnifiedFile> {
        val files = file.listFiles() ?: return emptyList()
        return files.map { AndroidUnifiedFile(it) }
    }

    override suspend fun readBytes(): ByteArray {
        return withContext(Dispatchers.IO) {
            file.readBytes()
        }
    }

    override suspend fun readChunk(offset: Long, size: Int): ByteArray {
        return withContext(Dispatchers.IO) {
            java.io.RandomAccessFile(file, "r").use { raf ->
                raf.seek(offset)
                val buffer = ByteArray(size)
                val bytesRead = raf.read(buffer)
                if (bytesRead < size) {
                     buffer.copyOf(bytesRead.coerceAtLeast(0))
                } else {
                     buffer
                }
            }
        }
    }
}
