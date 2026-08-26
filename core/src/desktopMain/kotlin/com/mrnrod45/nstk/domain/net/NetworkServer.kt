package com.mrnrod45.nstk.domain.net

import com.mrnrod45.nstk.domain.models.UnifiedFile
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

actual class NetworkServer {
    
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private var executor: java.util.concurrent.ExecutorService? = null

    actual fun start(
        files: List<UnifiedFile>,
        hostIp: String,
        port: Int,
        hostExtra: String,
        noRequestsServe: Boolean,
        switchIp: String,
        onLog: (String) -> Unit
    ) {
        if (isRunning.get()) return
        isRunning.set(true)

        onLog("Desktop Server starting...")

        // Daemon thread: if a caller forgets to stop() this server (or the app is killed
        // while it's still listening), it must not keep the JVM alive on its own.
        executor = java.util.concurrent.Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "NetworkServer").apply { isDaemon = true }
        }
        executor?.submit {
             try {
                serverSocket = ServerSocket(port)
                onLog("Desktop NetworkServer started on $port")

                // noRequestsServe = passive mode: skip the handshake, Switch already knows our IP
                if (noRequestsServe) {
                    onLog("Expert mode: passive — skipping handshake, waiting for Switch to connect...")
                } else {
                    sendHandshake(files, switchIp, hostIp, port, hostExtra, onLog)
                }

                while (isRunning.get()) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client, files)
                }
             } catch (e: Exception) {
                 if (isRunning.get()) e.printStackTrace()
             }
        }
    }

    actual fun stop() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        executor?.shutdownNow()
        executor = null
    }

    private fun handleClient(socket: Socket, files: List<UnifiedFile>) {
        try {
            val input = socket.getInputStream().bufferedReader()
            val output = socket.getOutputStream()
            val writer = java.io.PrintWriter(output)
            
            var line: String? = input.readLine()
            val packet = mutableListOf<String>()

            while (line != null) {
                if (line.trim().isEmpty()) { // End of headers
                    processPacket(packet, files, output, writer)
                    packet.clear()
                } else {
                    packet.add(line)
                }
                // Block for the next line rather than polling input.ready(): a slow client can
                // send a header line, then pause before the next one arrives, which would make
                // ready() return false and truncate the request before it's fully read.
                line = input.readLine()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket.close()
        }
    }
    
    private fun processPacket(packet: List<String>, files: List<UnifiedFile>, output: java.io.OutputStream, writer: java.io.PrintWriter) {
        if (packet.isEmpty()) return
        val firstLine = packet[0]
        
        // Simple Date Formatter (RFC 1123)
        val dateFormat = java.text.SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", java.util.Locale.US)
        dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val dateStr = dateFormat.format(java.util.Date())
        val serverName = "NS-USBloader-KMP"

        if (firstLine.startsWith("GET") || firstLine.startsWith("HEAD")) {
            // Extract filename
            val parts = firstLine.split(" ")
            if (parts.size < 2) return
            var reqName = parts[1].removePrefix("/")
            reqName = java.net.URLDecoder.decode(reqName, "UTF-8")
            
            val file = files.find { it.name == reqName }
            if (file == null) {
                writer.print("HTTP/1.0 404 Not Found\r\n")
                writer.print("Server: $serverName\r\n")
                writer.print("Date: $dateStr\r\n")
                writer.print("Connection: close\r\n")
                writer.print("Content-Type: text/html;charset=utf-8\r\n")
                writer.print("Content-Length: 0\r\n\r\n")
                writer.flush()
                return
            }

            // HEAD Request
            if (firstLine.startsWith("HEAD")) {
                val len = file.size
                writer.print("HTTP/1.0 200 OK\r\n")
                writer.print("Server: $serverName\r\n")
                writer.print("Date: $dateStr\r\n")
                writer.print("Content-type: application/octet-stream\r\n")
                writer.print("Accept-Ranges: bytes\r\n")
                writer.print("Content-Range: bytes 0-${len - 1}/$len\r\n")
                writer.print("Content-Length: $len\r\n")
                writer.print("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT\r\n\r\n")
                writer.flush()
                return
            }

            // GET Request (Range Handling)
            // Check Range
            val rangeLine = packet.find { it.startsWith("Range:", ignoreCase = true) }
            
            var start = 0L
            var end = file.size - 1
            
            if (rangeLine != null) {
                 val ranges = rangeLine.substringAfter("=").split("-")
                 if (ranges.isNotEmpty() && ranges[0].isNotEmpty()) {
                     start = ranges[0].toLong()
                 }
                 if (ranges.size > 1 && ranges[1].isNotEmpty()) {
                     end = ranges[1].toLong()
                 }
            }
            // Cap end
            if (end >= file.size) end = file.size - 1
            if (start > end) {
                 // 416 Range Not Satisfiable
                writer.print("HTTP/1.0 416 Requested Range Not Satisfiable\r\n")
                writer.print("Server: $serverName\r\n")
                writer.print("Date: $dateStr\r\n")
                writer.print("Connection: close\r\n")
                writer.print("Content-Length: 0\r\n\r\n")
                writer.flush()
                return
            }
            
            val len = end - start + 1
            
            // Send 206
            writer.print("HTTP/1.0 206 Partial Content\r\n")
            writer.print("Server: $serverName\r\n")
            writer.print("Date: $dateStr\r\n")
            writer.print("Content-type: application/octet-stream\r\n")
            writer.print("Accept-Ranges: bytes\r\n")
            writer.print("Content-Range: bytes $start-$end/${file.size}\r\n")
            writer.print("Content-Length: $len\r\n")
            writer.print("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT\r\n\r\n")
            writer.flush()
            
            // Send Data
            kotlinx.coroutines.runBlocking {
                var current = start
                var remaining = len
                val bufferSize = 65536
                
                while (remaining > 0) {
                    val toRead = minOf(remaining, bufferSize.toLong()).toInt()
                    val chunk = file.readChunk(current, toRead)
                    output.write(chunk)
                    current += toRead
                    remaining -= toRead
                }
                output.flush()
            }
        }
    }

    private fun sendHandshake(
        files: List<UnifiedFile>,
        switchIp: String,
        overrideHostIp: String,
        myPort: Int,
        hostExtra: String,
        onLog: (String) -> Unit
    ) {
        try {
            val myIp = if (overrideHostIp.isNotBlank()) overrideHostIp
                       else java.net.InetAddress.getLocalHost().hostAddress
            onLog("Detected Local IP: $myIp")

            val sb = StringBuilder()
            for (file in files) {
                // Format: ip:port/extra/filename (extra is the path suffix from expert mode)
                val encodedName = java.net.URLEncoder.encode(file.name, "UTF-8").replace("+", "%20")
                val extraSlash = if (hostExtra.isNotBlank()) "${hostExtra.trimStart('/')}/" else ""
                sb.append(myIp).append(":").append(myPort).append("/").append(extraSlash).append(encodedName).append("\n")
            }

            val content = sb.toString().toByteArray(Charsets.UTF_8)
            val sizeBytes = java.nio.ByteBuffer.allocate(4).putInt(content.size).array()

            onLog("Sending handshake to $switchIp:2000...")
            java.net.Socket(switchIp, 2000).use { socket ->
                val out = socket.getOutputStream()
                out.write(sizeBytes)
                out.write(content)
                out.flush()
            }
            onLog("Handshake sent successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            onLog("MsgType.FAIL: Handshake Failed: ${e.message}")
        }
    }
}
