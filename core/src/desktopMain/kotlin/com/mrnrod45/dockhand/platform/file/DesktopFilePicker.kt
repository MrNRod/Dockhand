package com.mrnrod45.dockhand.platform.file

import com.mrnrod45.dockhand.domain.models.UnifiedFile
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DesktopFilePicker : FilePicker {
    override suspend fun pickFiles(allowedExtensions: List<String>): List<UnifiedFile> = withContext(Dispatchers.Main) {
        try {
            val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Select Files", java.awt.FileDialog.LOAD)
            dialog.isMultipleMode = true
            
            if (allowedExtensions.isNotEmpty()) {
                dialog.filenameFilter = java.io.FilenameFilter { _, name ->
                    allowedExtensions.any { ext -> name.endsWith(ext, ignoreCase = true) }
                }
            }
            
            dialog.isVisible = true
            dialog.files.map { DesktopUnifiedFile(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun pickDirectory(): String? = withContext(Dispatchers.Main) {
        try {
            // macOS hack for native folder picker
            if (System.getProperty("os.name").contains("Mac")) {
                System.setProperty("apple.awt.fileDialogForDirectories", "true")
            }
            
            val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Select Folder", java.awt.FileDialog.LOAD)
            dialog.isVisible = true
            
            // Reset property immediately
            if (System.getProperty("os.name").contains("Mac")) {
                System.setProperty("apple.awt.fileDialogForDirectories", "false")
            }

            if (dialog.directory != null && dialog.file != null) {
                File(dialog.directory, dialog.file).absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun pickFolderAndListFiles(allowedExtensions: List<String>): List<UnifiedFile> = withContext(Dispatchers.Main) {
        val path = pickDirectory() ?: return@withContext emptyList()
        withContext(Dispatchers.IO) {
            val root = File(path)
            val result = mutableListOf<UnifiedFile>()
            collectFiles(root, allowedExtensions, result)
            result
        }
    }

    private fun collectFiles(current: File, allowedExtensions: List<String>, result: MutableList<UnifiedFile>) {
        if (!current.exists()) return

        // Check if current directory is a "split file" (folder ending in extension)
        val isSplitFolder = current.isDirectory && allowedExtensions.any { current.name.endsWith(it, ignoreCase = true) }
        
        if (isSplitFolder) {
            // Treat as a single file
            result.add(DesktopSplitUnifiedFile(current))
            return // Do not recurse inside
        }

        if (current.isFile) {
            val isAllowed = allowedExtensions.isEmpty() || allowedExtensions.any { current.name.endsWith(it, ignoreCase = true) }
            if (isAllowed) {
                result.add(DesktopUnifiedFile(current))
            }
            return
        }

        if (current.isDirectory) {
            current.listFiles()?.forEach { child ->
                collectFiles(child, allowedExtensions, result)
            }
        }
    }
}
