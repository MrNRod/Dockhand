package com.mrnrod45.dockhand.platform.file

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.mrnrod45.dockhand.domain.models.UnifiedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.IOException

/**
 * Wraps a SAF DocumentFile in place, reading via ParcelFileDescriptor so multi-GB NSPs
 * picked in folder mode don't have to be copied into cacheDir first.
 */
class AndroidDocumentUnifiedFile(
    private val context: Context,
    private val doc: DocumentFile
) : UnifiedFile {

    override val name: String get() = doc.name ?: "unknown"
    override val size: Long get() = doc.length()
    override val isDirectory: Boolean get() = doc.isDirectory
    override val path: String get() = doc.uri.toString()

    override fun listFiles(): List<UnifiedFile> =
        doc.listFiles().map { AndroidDocumentUnifiedFile(context, it) }

    override suspend fun readBytes(): ByteArray = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(doc.uri)?.use { it.readBytes() }
            ?: throw IOException("Unable to open ${doc.uri}")
    }

    override suspend fun readChunk(offset: Long, size: Int): ByteArray = withContext(Dispatchers.IO) {
        context.contentResolver.openFileDescriptor(doc.uri, "r")?.use { pfd ->
            FileInputStream(pfd.fileDescriptor).use { input ->
                input.channel.position(offset)
                val buffer = ByteArray(size)
                var totalRead = 0
                while (totalRead < size) {
                    val read = input.read(buffer, totalRead, size - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                if (totalRead < size) buffer.copyOf(totalRead.coerceAtLeast(0)) else buffer
            }
        } ?: throw IOException("Unable to open ${doc.uri}")
    }
}
