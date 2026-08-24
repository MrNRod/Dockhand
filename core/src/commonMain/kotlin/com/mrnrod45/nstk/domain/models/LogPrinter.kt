package com.mrnrod45.nstk.domain.models

interface LogPrinter {
    fun print(message: String, type: MsgType)
    fun updateProgress(value: Double)
    // Using Map for batch updates, similar to original code
    fun update(files: Map<String, UnifiedFile>, status: FileStatus)
    fun update(file: UnifiedFile, status: FileStatus)
    fun close()
}
