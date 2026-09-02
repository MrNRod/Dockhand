package com.mrnrod45.dockhand.domain.net

import com.mrnrod45.dockhand.domain.models.UnifiedFile
import com.mrnrod45.dockhand.domain.util.IoDispatcher
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

actual class NetworkServer {
    private val selectorManager = SelectorManager(IoDispatcher)
    private val scope = CoroutineScope(SupervisorJob() + IoDispatcher)
    private var serverSocket: ServerSocket? = null
    private var job: Job? = null

    actual fun start(
        files: List<UnifiedFile>,
        hostIp: String,
        port: Int,
        hostExtra: String,
        noRequestsServe: Boolean,
        switchIp: String,
        onLog: (String) -> Unit
    ) {
        if (job != null) return

        job = scope.launch {
            try {
                val server = aSocket(selectorManager).tcp().bind(InetSocketAddress("0.0.0.0", port))
                serverSocket = server
                onLog("macOS NetworkServer started on $port")

                if (noRequestsServe) {
                    onLog("Expert mode: passive — skipping handshake, waiting for Switch to connect...")
                } else {
                    sendHandshake(files, switchIp, hostIp, port, hostExtra, onLog)
                }

                while (isActive) {
                    val client = server.accept()
                    launch { handleClient(client, files) }
                }
            } catch (e: Throwable) {
                onLog("NetworkServer error: ${e.message}")
            }
        }
    }

    actual fun stop() {
        job?.cancel()
        job = null
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
        serverSocket = null
    }

    private suspend fun handleClient(socket: Socket, files: List<UnifiedFile>) {
        try {
            val readChannel = socket.openReadChannel()
            val writeChannel = socket.openWriteChannel(autoFlush = true)

            val packet = mutableListOf<String>()
            while (true) {
                val line = readChannel.readUTF8Line() ?: break
                if (line.isBlank()) {
                    processPacket(packet, files, writeChannel)
                    break
                } else {
                    packet.add(line)
                }
            }
        } catch (e: Exception) {
            // ignore - client likely disconnected mid-request
        } finally {
            socket.close()
        }
    }

    private suspend fun processPacket(packet: List<String>, files: List<UnifiedFile>, writeChannel: ByteWriteChannel) {
        if (packet.isEmpty()) return
        val firstLine = packet[0]
        val serverName = "Dockhand"
        val lastModified = "Thu, 01 Jan 1970 00:00:00 GMT"

        if (!firstLine.startsWith("GET") && !firstLine.startsWith("HEAD")) return

        val parts = firstLine.split(" ")
        if (parts.size < 2) return
        val reqName = decodeUrl(parts[1].removePrefix("/"))

        val file = files.find { it.name == reqName }
        if (file == null) {
            writeChannel.writeStringUtf8(
                "HTTP/1.0 404 Not Found\r\nServer: $serverName\r\nConnection: close\r\nContent-Type: text/html;charset=utf-8\r\nContent-Length: 0\r\n\r\n"
            )
            return
        }

        if (firstLine.startsWith("HEAD")) {
            val len = file.size
            writeChannel.writeStringUtf8(
                "HTTP/1.0 200 OK\r\nServer: $serverName\r\nContent-type: application/octet-stream\r\nAccept-Ranges: bytes\r\nContent-Range: bytes 0-${len - 1}/$len\r\nContent-Length: $len\r\nLast-Modified: $lastModified\r\n\r\n"
            )
            return
        }

        // GET request - Range handling
        val rangeLine = packet.find { it.startsWith("Range:", ignoreCase = true) }
        var start = 0L
        var end = file.size - 1

        if (rangeLine != null) {
            val ranges = rangeLine.substringAfter("=").split("-")
            if (ranges.isNotEmpty() && ranges[0].isNotEmpty()) start = ranges[0].toLong()
            if (ranges.size > 1 && ranges[1].isNotEmpty()) end = ranges[1].toLong()
        }
        if (end >= file.size) end = file.size - 1
        if (start > end) {
            writeChannel.writeStringUtf8(
                "HTTP/1.0 416 Requested Range Not Satisfiable\r\nServer: $serverName\r\nConnection: close\r\nContent-Length: 0\r\n\r\n"
            )
            return
        }

        val len = end - start + 1
        writeChannel.writeStringUtf8(
            "HTTP/1.0 206 Partial Content\r\nServer: $serverName\r\nContent-type: application/octet-stream\r\nAccept-Ranges: bytes\r\nContent-Range: bytes $start-$end/${file.size}\r\nContent-Length: $len\r\nLast-Modified: $lastModified\r\n\r\n"
        )

        var current = start
        var remaining = len
        val bufferSize = 65536
        while (remaining > 0) {
            val toRead = minOf(remaining, bufferSize.toLong()).toInt()
            val chunk = file.readChunk(current, toRead)
            writeChannel.writeFully(chunk)
            current += toRead
            remaining -= toRead
        }
    }

    private suspend fun sendHandshake(
        files: List<UnifiedFile>,
        switchIp: String,
        overrideHostIp: String,
        myPort: Int,
        hostExtra: String,
        onLog: (String) -> Unit
    ) {
        try {
            val myIp = if (overrideHostIp.isNotBlank()) overrideHostIp else detectLocalIp()
            if (myIp == null) {
                onLog("Could not auto-detect local IP. Please set Host IP manually in Expert Mode.")
                return
            }
            onLog("Detected Local IP: $myIp")

            val sb = StringBuilder()
            for (file in files) {
                val encodedName = encodeUrl(file.name)
                val extraSlash = if (hostExtra.isNotBlank()) "${hostExtra.trimStart('/')}/" else ""
                sb.append(myIp).append(":").append(myPort).append("/").append(extraSlash).append(encodedName).append("\n")
            }

            // Matches legacy behavior: plain 4-byte BIG-ENDIAN length prefix (java.nio.ByteBuffer default order).
            val content = sb.toString().encodeToByteArray()
            val sizeBytes = intToBytesBE(content.size)

            onLog("Sending handshake to $switchIp:2000...")
            val socket = aSocket(selectorManager).tcp().connect(InetSocketAddress(switchIp, 2000))
            val writeChannel = socket.openWriteChannel(autoFlush = true)
            writeChannel.writeFully(sizeBytes)
            writeChannel.writeFully(content)
            socket.close()
            onLog("Handshake sent successfully.")
        } catch (e: Exception) {
            onLog("Handshake Failed: ${e.message}")
        }
    }

    private suspend fun detectLocalIp(): String? {
        return try {
            val probe = aSocket(selectorManager).udp().connect(InetSocketAddress("8.8.8.8", 80))
            val local = probe.localAddress as? InetSocketAddress
            probe.close()
            local?.hostname
        } catch (e: Exception) {
            null
        }
    }

    private fun intToBytesBE(v: Int): ByteArray = byteArrayOf(
        ((v ushr 24) and 0xFF).toByte(),
        ((v ushr 16) and 0xFF).toByte(),
        ((v ushr 8) and 0xFF).toByte(),
        (v and 0xFF).toByte()
    )

    private fun encodeUrl(s: String): String {
        val sb = StringBuilder()
        for (b in s.encodeToByteArray()) {
            val c = (b.toInt() and 0xFF).toChar()
            if (c.isLetterOrDigit() || c == '-' || c == '_' || c == '.' || c == '~') {
                sb.append(c)
            } else if (c == ' ') {
                sb.append("%20")
            } else {
                sb.append('%')
                sb.append((b.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase())
            }
        }
        return sb.toString()
    }

    private fun decodeUrl(s: String): String {
        val bytes = mutableListOf<Byte>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%' && i + 2 < s.length) {
                val hex = s.substring(i + 1, i + 3)
                bytes.add(hex.toInt(16).toByte())
                i += 3
            } else if (c == '+') {
                bytes.add(' '.code.toByte())
                i++
            } else {
                bytes.add(c.code.toByte())
                i++
            }
        }
        return bytes.toByteArray().decodeToString()
    }
}
