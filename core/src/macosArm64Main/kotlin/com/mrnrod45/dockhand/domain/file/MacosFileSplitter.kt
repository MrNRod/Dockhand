package com.mrnrod45.dockhand.domain.file

import com.mrnrod45.dockhand.domain.models.UnifiedFile
import com.mrnrod45.dockhand.domain.util.IoDispatcher
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.withContext
import platform.posix.closedir
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fwrite
import platform.posix.mkdir
import platform.posix.opendir
import platform.posix.readdir

class MacosFileSplitter : FileSplitter {
    // 0xffff0000 bytes (approx 3.99 GB) - Legacy NS-USBloader threshold
    private val SPLIT_SIZE = 0xFFFF0000L
    private val BUFFER_SIZE = 4 * 1024 * 1024 // 4MB
    private val chunkNameRegex = Regex(".*\\.[0-9]{2}$")

    override suspend fun splitFile(sourceFile: UnifiedFile, outputDir: String): Boolean = withContext(IoDispatcher) {
        try {
            mkdir(outputDir, 511u /* 0777 */)

            val input = fopen(sourceFile.path, "rb") ?: return@withContext false
            val buffer = ByteArray(BUFFER_SIZE)
            try {
                var partCounter = 0
                var currentPartSize = 0L
                var output = fopen(partPath(outputDir, sourceFile.name, partCounter), "wb")
                    ?: return@withContext false

                try {
                    while (true) {
                        val bytesRead = buffer.usePinned { pinned ->
                            fread(pinned.addressOf(0), 1u, BUFFER_SIZE.toULong(), input)
                        }.toInt()
                        if (bytesRead <= 0) break

                        buffer.usePinned { pinned ->
                            fwrite(pinned.addressOf(0), 1u, bytesRead.toULong(), output)
                        }
                        currentPartSize += bytesRead

                        if (currentPartSize >= SPLIT_SIZE) {
                            fclose(output)
                            partCounter++
                            currentPartSize = 0
                            output = fopen(partPath(outputDir, sourceFile.name, partCounter), "wb")
                                ?: return@withContext false
                        }
                    }
                } finally {
                    fclose(output)
                }
            } finally {
                fclose(input)
            }
            true
        } catch (e: Exception) {
            println("Split failed: ${e.message}")
            false
        }
    }

    override suspend fun mergeFiles(firstFile: UnifiedFile, outputDir: String): Boolean = withContext(IoDispatcher) {
        try {
            // firstFile may be the split-folder itself, or a chunk file inside it.
            val splitDir = if (firstFile.isDirectory) firstFile.path else firstFile.path.substringBeforeLast('/')
            val folderName = splitDir.substringAfterLast('/')

            // Chunks are named "baseName.00", "baseName.01", ... (see partPath below).
            val chunkNames = listChunkNames(splitDir).sorted()
            if (chunkNames.isEmpty()) return@withContext false

            var resultPath = "$outputDir/!_$folderName"
            var collision = 0
            while (fopen(resultPath, "rb")?.also { fclose(it) } != null && collision < 50) {
                resultPath = "$outputDir/!_${collision}_$folderName"
                collision++
            }

            val output = fopen(resultPath, "wb") ?: return@withContext false
            val buffer = ByteArray(BUFFER_SIZE)
            try {
                for (chunkName in chunkNames) {
                    val input = fopen("$splitDir/$chunkName", "rb") ?: continue
                    try {
                        while (true) {
                            val bytesRead = buffer.usePinned { pinned ->
                                fread(pinned.addressOf(0), 1u, BUFFER_SIZE.toULong(), input)
                            }.toInt()
                            if (bytesRead <= 0) break
                            buffer.usePinned { pinned ->
                                fwrite(pinned.addressOf(0), 1u, bytesRead.toULong(), output)
                            }
                        }
                    } finally {
                        fclose(input)
                    }
                }
                true
            } finally {
                fclose(output)
            }
        } catch (e: Exception) {
            println("Merge failed: ${e.message}")
            false
        }
    }

    private fun listChunkNames(directory: String): List<String> {
        val dir = opendir(directory) ?: return emptyList()
        val names = mutableListOf<String>()
        try {
            while (true) {
                val entry = readdir(dir) ?: break
                val childName = entry.pointed.d_name.toKString()
                if (chunkNameRegex.matches(childName)) names.add(childName)
            }
        } finally {
            closedir(dir)
        }
        return names
    }

    private fun partPath(dir: String, baseName: String, index: Int): String {
        val suffix = index.toString().padStart(2, '0')
        return "$dir/$baseName.$suffix"
    }
}
