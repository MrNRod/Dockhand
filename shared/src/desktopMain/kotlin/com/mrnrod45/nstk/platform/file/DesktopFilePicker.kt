package com.mrnrod45.nstk.platform.file

import com.mrnrod45.nstk.domain.models.UnifiedFile
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

    override suspend fun pickDirectory(): String? = withContext(Dispatchers.IO) {
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
}
