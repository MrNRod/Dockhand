package com.mrnrod45.nstk.domain.file

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.mrnrod45.nstk.NSTKApplication
import com.mrnrod45.nstk.domain.models.UnifiedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class AndroidFileSplitter : FileSplitter {
    // 0xffff0000 bytes (approx 3.99 GB) - Legacy NS-USBloader threshold
    private val SPLIT_SIZE = 0xFFFF0000L

    override suspend fun splitFile(sourceFile: UnifiedFile, outputDir: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(sourceFile.path)
            if (!file.exists()) return@withContext false

            val context = NSTKApplication.context
            
            if (outputDir.startsWith("content://")) {
                // SAF (Storage Access Framework) Logic
                val treeUri = Uri.parse(outputDir)
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
                
                // No subfolder, write directly to rootDoc
                
                val buffer = ByteArray(4 * 1024 * 1024) // 4MB buffer
                FileInputStream(file).use { fis ->
                    var partCounter = 0
                    var currentPartSize = 0L
                    var outStream: java.io.OutputStream? = null
                    
                    try {
                        while (true) {
                            if (outStream == null) {
                                // Filename: "OriginalName.00", "OriginalName.01"
                                val partExtension = partCounter.toString().padStart(2, '0')
                                val partName = "${file.name}.$partExtension"
                                
                                // Check if exists to delete (force overwrite behavior requested?)
                                val existing = rootDoc.findFile(partName)
                                if (existing != null && existing.exists()) {
                                    existing.delete()
                                }
                                
                                val partFile = rootDoc.createFile("application/octet-stream", partName) ?: return@withContext false
                                outStream = context.contentResolver.openOutputStream(partFile.uri)
                                if (outStream == null) return@withContext false
                            }

                            val bytesRead = fis.read(buffer)
                            if (bytesRead == -1) break

                            outStream.write(buffer, 0, bytesRead)
                            currentPartSize += bytesRead

                            if (currentPartSize >= SPLIT_SIZE) {
                                outStream.close()
                                outStream = null
                                currentPartSize = 0
                                partCounter++
                            }
                        }
                    } finally {
                        outStream?.close()
                    }
                }
                true
            } else {
                // Legacy / Direct File Logic (Fallback or cache path)
                val outDirFile = if (outputDir.isNotBlank()) {
                    File(outputDir)
                } else {
                    val downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    File(downloads, "NS-ToolKit/Split")
                }
                
                if (!outDirFile.exists()) outDirFile.mkdirs()

                // No subfolder
                
                val buffer = ByteArray(4 * 1024 * 1024) 
                FileInputStream(file).use { fis ->
                    var partCounter = 0
                    var currentPartSize = 0L
                    var fos: FileOutputStream? = null
                    
                    try {
                        while (true) {
                            if (fos == null) {
                                val partExtension = partCounter.toString().padStart(2, '0')
                                val partName = "${file.name}.$partExtension"
                                val partFile = File(outDirFile, partName)
                                fos = FileOutputStream(partFile)
                            }

                            val bytesRead = fis.read(buffer)
                            if (bytesRead == -1) break

                            fos!!.write(buffer, 0, bytesRead)
                            currentPartSize += bytesRead

                            if (currentPartSize >= SPLIT_SIZE) {
                                fos!!.close()
                                fos = null
                                currentPartSize = 0
                                partCounter++
                            }
                        }
                    } finally {
                        fos?.close()
                    }
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun mergeFiles(firstFile: UnifiedFile, outputDir: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Logic: firstFile could be the folder OR a chunk file inside the folder
            val inputObj = File(firstFile.path)
            
            val splitDir = if (inputObj.isDirectory) {
                inputObj
            } else {
                inputObj.parentFile
            }

            if (splitDir == null || !splitDir.isDirectory) return@withContext false

            // Collect "*.00", "*.01"... chunk files (see splitFile's naming above)
            val chunks = splitDir.listFiles { _, name -> name.matches(Regex(".*\\.[0-9]{2}$")) }
                ?.sortedBy { it.name }
                ?: return@withContext false
                
            if (chunks.isEmpty()) return@withContext false

            // Output file: "!_FolderName" (Legacy behavior matches MergeSubTask.java)
            var resultFile = File(outputDir, "!_${splitDir.name}")
            for (i in 0..49) {
                if (!resultFile.exists()) break
                resultFile = File(outputDir, "!_${i}_${splitDir.name}")
            }

            val buffer = ByteArray(4 * 1024 * 1024)
            FileOutputStream(resultFile).use { fos ->
                for (chunk in chunks) {
                    FileInputStream(chunk).use { fis ->
                        var bytesRead: Int
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            fos.write(buffer, 0, bytesRead)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
