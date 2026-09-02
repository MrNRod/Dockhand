package com.mrnrod45.dockhand.windows

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Minimal newline-delimited JSON-RPC protocol between this JVM backend and the
 * WinUI 3 C# frontend, which spawns this process as a subprocess and talks to
 * it over a loopback TCP socket. See README.md for the full method list.
 */
@Serializable
data class Request(
    val id: Int,
    val method: String,
    val params: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class Response(
    val id: Int,
    val result: JsonElement? = null,
    val error: String? = null
)

/** One-way push message (log lines during an upload/inject/split operation). */
@Serializable
data class Event(
    val event: String,
    val message: String
)
