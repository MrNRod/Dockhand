package com.mrnrod45.nstk.windows

import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket

private val json = Json { ignoreUnknownKeys = true }

/**
 * Entry point for the WinUI frontend's subprocess backend. Binds to an
 * OS-assigned loopback port, prints "PORT <n>" on the FIRST line of stdout
 * (the frontend reads this line to know where to connect), then serves
 * newline-delimited JSON-RPC requests from a single client connection.
 */
fun main() {
    val serverSocket = ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())
    println("PORT ${serverSocket.localPort}")
    System.out.flush()

    val socket = serverSocket.accept()
    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
    val writer = PrintWriter(socket.getOutputStream(), true)

    val handler = RequestHandler(sendEvent = { event ->
        synchronized(writer) { writer.println(json.encodeToString(Event.serializer(), event)) }
    })

    while (true) {
        val line = reader.readLine() ?: break
        if (line.isBlank()) continue
        val request = try {
            json.decodeFromString(Request.serializer(), line)
        } catch (e: Exception) {
            continue
        }
        try {
            val result = handler.handle(request)
            val response = Response(id = request.id, result = result)
            synchronized(writer) { writer.println(json.encodeToString(Response.serializer(), response)) }
        } catch (e: Exception) {
            val response = Response(id = request.id, error = e.message ?: e.toString())
            synchronized(writer) { writer.println(json.encodeToString(Response.serializer(), response)) }
        }
    }

    socket.close()
    serverSocket.close()
}
