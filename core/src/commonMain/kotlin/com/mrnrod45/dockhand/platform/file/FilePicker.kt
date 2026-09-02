package com.mrnrod45.dockhand.platform.file

import com.mrnrod45.dockhand.domain.models.UnifiedFile

interface FilePicker {
    suspend fun pickFiles(allowedExtensions: List<String> = emptyList()): List<UnifiedFile>
    suspend fun pickDirectory(): String?
    suspend fun pickFolderAndListFiles(allowedExtensions: List<String> = emptyList()): List<UnifiedFile>
}
