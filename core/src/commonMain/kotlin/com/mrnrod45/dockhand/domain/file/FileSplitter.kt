package com.mrnrod45.dockhand.domain.file

import com.mrnrod45.dockhand.domain.models.UnifiedFile

interface FileSplitter {
    suspend fun splitFile(sourceFile: UnifiedFile, outputDir: String): Boolean
    suspend fun mergeFiles(firstFile: UnifiedFile, outputDir: String): Boolean
}
