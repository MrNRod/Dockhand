package com.mrnrod45.nstk.domain.net

import com.mrnrod45.nstk.domain.models.UnifiedFile

expect class NetworkServer() {
    fun start(files: List<UnifiedFile>, hostIp: String, port: Int, onLog: (String) -> Unit)
    fun stop()
}
