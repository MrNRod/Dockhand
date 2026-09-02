package com.mrnrod45.dockhand.domain.models

interface UnifiedFile {
    val name: String
    val size: Long
    val isDirectory: Boolean
    val path: String
    
    // For split files support
    fun listFiles(): List<UnifiedFile>
    
    suspend fun readBytes(): ByteArray
    
    suspend fun readChunk(offset: Long, size: Int): ByteArray
}
