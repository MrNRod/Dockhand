package com.mrnrod45.nstk.platform.file

import com.mrnrod45.nstk.domain.models.UnifiedFile

interface FilePicker {
    suspend fun pickFiles(allowedExtensions: List<String> = emptyList()): List<UnifiedFile>
    suspend fun pickDirectory(): String?
    suspend fun pickFolderAndListFiles(allowedExtensions: List<String> = emptyList()): List<UnifiedFile>
}
