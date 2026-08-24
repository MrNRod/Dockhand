package com.mrnrod45.nstk.domain.file

import com.mrnrod45.nstk.domain.models.UnifiedFile

interface FileSplitter {
    suspend fun splitFile(sourceFile: UnifiedFile, outputDir: String): Boolean
    suspend fun mergeFiles(firstFile: UnifiedFile, outputDir: String): Boolean
}
