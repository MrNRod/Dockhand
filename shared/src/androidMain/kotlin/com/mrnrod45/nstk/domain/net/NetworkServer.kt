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

    actual fun start(files: List<UnifiedFile>, hostIp: String, port: Int, onLog: (String) -> Unit) {
        if (isRunning.get()) return
        isRunning.set(true)
        
        executor = Executors.newSingleThreadExecutor()
        executor?.submit {
            try {
                // TinfoilNET.java Logic Parity
                // 1. Resolve IP/Port (Passed in)
                // 2. Open ServerSocket
                serverSocket = ServerSocket(port)
                onLog("NetworkServer started on port $port")
                
                // Send Handshake to Switch (hostIp:2000)
                sendHandshake(files, hostIp, port, onLog)

                while (isRunning.get()) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        handleClient(client, files)
                    } catch (e: Exception) {
                        if (isRunning.get()) {
                            // Only log stacktrace if we expect to be running (not during stop)
                             e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onLog("Server Error: ${e.message}")
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
            // TinfoilNET uses simple line-based protocol over TCP
            // 1. Handshake? No, TinfoilNET sends handshake TO Switch.
            // Wait, TinfoilNET.java: 
            // - "sendHandshake(handshakeCommandSize, handshakeCommand)" -> Connects to Switch IP:2000.
            // - "serveRequestsLoop()" -> Accepts connections FROM Switch (Server Socket).
            
            // My NetworkServer.start() opens the Server Socket.
            // But I ALSO need to send the Handshake to the Switch to tell it we are here!
            
            // Parity check:
            // TinfoilNET constructor resolves IP.
            // run() -> sendHandshake() -> serveRequestsLoop().
            
            // So NetworkServer.start() must do the same.
            // My current start() launches a thread that opens ServerSocket.
            // I should ALSO send the handshake in that thread before looping.
            
            // Handshake Logic:
            // Connect to hostIp:2000.
            // Send size (4 bytes).
            // Send content: "myIp:myPort/fileName\n..."
            
            // Note: hostIp is NS IP.
            // I need "myIp". Android: WifiManager. Desktop: InetAddress.getLocalHost().
            // For now, I'll use socket.localAddress if possible or just the IP interface.
            
            // Since this is handleClient (Server Loop), the Handshake must happen BEFORE this loop in start().
            // I will implement sendHandshake in start().
            
            val input = socket.getInputStream().bufferedReader()
            val output = socket.getOutputStream()
            val writer = java.io.PrintWriter(output)
            
            var line: String? = input.readLine()
            val packet = mutableListOf<String>()
            
            while (line != null) {
                if (line.trim().isEmpty()) { // End of headers
                    processPacket(packet, files, output, writer)
                    packet.clear()
                    // Keep connection open? TinfoilNET seems to handle one request per connection or keep-alive?
                    // TinfoilNET.java: "while ((line = br.readLine()) != null) ... handleRequest ... tcpPacket.clear()"
                    // It stays in loop.
                } else {
                    packet.add(line)
                }
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
    
    private fun sendHandshake(files: List<UnifiedFile>, switchIp: String, myPort: Int, onLog: (String) -> Unit) {
        try {
            // Get local IP
            val myIp = getLocalIpAddress()
            if (myIp == null) {
                onLog("MsgType.FAIL: Could not determine Local IP Address!")
                return
            }
            onLog("Detected Local IP: $myIp")
            
            val sb = StringBuilder()
            for (file in files) {
                val encodedName = java.net.URLEncoder.encode(file.name, "UTF-8").replace("+", "%20")
                sb.append(myIp).append(":").append(myPort).append("/").append(encodedName).append("\n")
            }
            
            val content = sb.toString().toByteArray(Charsets.UTF_8)
            val sizeBytes = java.nio.ByteBuffer.allocate(4).putInt(content.size).array()
            
            onLog("Sending handshake to $switchIp:2000...")
            Socket(switchIp, 2000).use { socket ->
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
    
    private fun getLocalIpAddress(): String? {
        try {
            // Priority: Try WiFi Manager first (Legacy behavior)
            val context = com.mrnrod45.nstk.NSTKApplication.context
            val wm = context.getSystemService(android.content.Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            
            if (wm != null) {
                val ipInt = wm.connectionInfo.ipAddress
                // If connected to Wifi
                if (ipInt != 0) {
                    return String.format(java.util.Locale.US, "%d.%d.%d.%d",
                        (ipInt and 0xff),
                        (ipInt shr 8 and 0xff),
                        (ipInt shr 16 and 0xff),
                        (ipInt shr 24 and 0xff))
                }
            }
        
            // Fallback to NetworkInterfaces
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                
                // Prefer wlan0 if WifiManager failed but interface exists
                // But generally stick to first valid IPv4
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
