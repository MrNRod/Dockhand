package com.mrnrod45.dockhand.platform.file

import com.mrnrod45.dockhand.domain.models.UnifiedFile
import com.mrnrod45.dockhand.domain.util.IoDispatcher
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.withContext
import platform.posix.SEEK_SET
import platform.posix.closedir
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseeko
import platform.posix.opendir
import platform.posix.readdir
import platform.posix.stat

class MacUnifiedFile(private val filePath: String) : UnifiedFile {

    override val name: String
        get() = filePath.substringAfterLast('/')

    override val path: String
        get() = filePath

    override val isDirectory: Boolean
        get() = memScoped {
            val st = alloc<stat>()
            if (platform.posix.stat(filePath, st.ptr) != 0) return@memScoped false
            (st.st_mode.toInt() and platform.posix.S_IFMT) == platform.posix.S_IFDIR
        }

    override val size: Long
        get() = if (isDirectory) {
            listFiles().filterNot { it.isDirectory }.sumOf { it.size }
        } else {
            memScoped {
                val st = alloc<stat>()
                if (stat(filePath, st.ptr) != 0) 0L else st.st_size
            }
        }

    override fun listFiles(): List<UnifiedFile> {
        val dir = opendir(filePath) ?: return emptyList()
        val results = mutableListOf<UnifiedFile>()
        try {
            while (true) {
                val entry = readdir(dir) ?: break
                val childName = entry.pointed.d_name.toKString()
                if (childName == "." || childName == "..") continue
                results.add(MacUnifiedFile("$filePath/$childName"))
            }
        } finally {
            closedir(dir)
        }
        return results
    }

    override suspend fun readBytes(): ByteArray = withContext(IoDispatcher) {
        readChunk(0, size.toInt())
    }

    override suspend fun readChunk(offset: Long, size: Int): ByteArray = withContext(IoDispatcher) {
        if (size <= 0) return@withContext ByteArray(0)
        val file = fopen(filePath, "rb") ?: return@withContext ByteArray(0)
        try {
            fseeko(file, offset, SEEK_SET)
            val buffer = ByteArray(size)
            val bytesRead = buffer.usePinned { pinned ->
                fread(pinned.addressOf(0), 1u, size.toULong(), file)
            }
            if (bytesRead.toInt() < size) buffer.copyOf(bytesRead.toInt()) else buffer
        } finally {
            fclose(file)
        }
    }
}
