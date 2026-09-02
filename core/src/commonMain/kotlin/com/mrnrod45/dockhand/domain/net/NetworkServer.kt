package com.mrnrod45.dockhand.domain.net

import com.mrnrod45.dockhand.domain.models.UnifiedFile

expect class NetworkServer() {
    fun start(
        files: List<UnifiedFile>,
        hostIp: String,        // blank = auto-detect via local network interfaces
        port: Int,
        hostExtra: String,     // extra URL path suffix appended to handshake URLs
        noRequestsServe: Boolean, // true = passive mode: skip handshake, just listen
        switchIp: String,
        onLog: (String) -> Unit
    )
    fun stop()
}
