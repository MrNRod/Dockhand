package com.mrnrod45.nstk.windows

import com.mrnrod45.nstk.domain.file.getFileSplitter
import com.mrnrod45.nstk.domain.models.FileStatus
import com.mrnrod45.nstk.domain.models.LogPrinter
import com.mrnrod45.nstk.domain.models.MsgType
import com.mrnrod45.nstk.domain.models.UnifiedFile
import com.mrnrod45.nstk.domain.net.NetworkServer
import com.mrnrod45.nstk.domain.protocols.Goldleaf
import com.mrnrod45.nstk.domain.protocols.Tinfoil
import com.mrnrod45.nstk.domain.rcm.RcmPayloadBuilder
import com.mrnrod45.nstk.domain.usb.UsbDevice
import com.mrnrod45.nstk.platform.file.DesktopUnifiedFile
import com.mrnrod45.nstk.platform.usb.DesktopUsbController
import com.mrnrod45.nstk.platform.usb.NoOpUsbController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.io.File

class RequestHandler(private val sendEvent: (Event) -> Unit) {

    private val usbController = try {
        DesktopUsbController()
    } catch (e: Throwable) {
        sendEvent(Event("log", "Failed to initialize USB controller (missing native libs?): ${e.message}"))
        NoOpUsbController()
    }
    private val fileSplitter = getFileSplitter()
    private val scope = CoroutineScope(Dispatchers.Default)

    // Retained so a running network upload session can actually be stopped later —
    // starting a new NetworkServer() per upload leaked the previous listener forever.
    private val networkServer = NetworkServer()

    fun handle(request: Request): JsonElement {
        val params = request.params
        return when (request.method) {
            "listDevices" -> JsonArray(usbController.listDevices().map { it.toJson() })

            "startUpload" -> {
                val protocolName = params.str("protocol")
                val transport = params.str("transport")
                val ip = params.str("ip", "")
                val filePaths = params.strList("files")
                scope.launch { runUpload(protocolName, transport, ip, filePaths) }
                buildJsonObject { put("status", "started") }
            }

            "stopUpload" -> {
                networkServer.stop()
                sendEvent(Event("log", "Network server stopped."))
                sendEvent(Event("uploadDone", "done"))
                buildJsonObject { put("status", "stopped") }
            }

            "findRcmDevice" -> usbController.findRcmDevice()?.toJson() ?: JsonNull

            "injectPayload" -> {
                val payloadPath = params.str("payloadPath")
                scope.launch { runInject(payloadPath) }
                buildJsonObject { put("status", "started") }
            }

            "convert" -> {
                val isSplit = params.obj["isSplit"]?.jsonPrimitive?.boolean ?: true
                val paths = params.strList("paths")
                val outputDir = params.str("outputDir")
                scope.launch { runConvert(isSplit, paths, outputDir) }
                buildJsonObject { put("status", "started") }
            }

            else -> throw IllegalArgumentException("Unknown method: ${request.method}")
        }
    }

    private suspend fun runUpload(protocolName: String, transport: String, ip: String, filePaths: List<String>) {
        val logPrinter = eventLogPrinter()
        val files = filePaths.map { DesktopUnifiedFile(File(it)) }
        val fileMap = files.associateBy { it.name }

        if (transport == "NET") {
            try {
                // Stop any previous session before starting a new one — the server
                // otherwise keeps listening on its old port forever.
                networkServer.stop()
                networkServer.start(
                    files = files, hostIp = "", port = 6042, hostExtra = "",
                    noRequestsServe = false, switchIp = ip
                ) { msg -> sendEvent(Event("log", msg)) }
            } catch (e: Exception) {
                sendEvent(Event("log", "[FAIL] Upload error: ${e.message}"))
                sendEvent(Event("uploadDone", "done"))
            }
            // No uploadDone here — the server keeps running until the client calls
            // "stopUpload", which is what actually signals completion for NET transport.
            return
        }

        try {
            val device = usbController.listDevices().firstOrNull()
            val connection = device?.open()
            if (connection == null) {
                sendEvent(Event("log", "[FAIL] No USB device found or failed to open connection."))
                return
            }
            try {
                if (!connection.claimInterface(connection.activeInterfaceIndex)) {
                    sendEvent(Event("log", "[FAIL] Failed to claim USB interface ${connection.activeInterfaceIndex}."))
                    return
                }
                if (protocolName == "Goldleaf") {
                    Goldleaf(connection, logPrinter).start(fileMap)
                } else {
                    Tinfoil(connection, logPrinter).start(fileMap, protocolName == "Sphaira")
                }
            } finally {
                connection.close()
            }
        } catch (e: Exception) {
            sendEvent(Event("log", "[FAIL] Upload error: ${e.message}"))
        } finally {
            sendEvent(Event("uploadDone", "done"))
        }
    }

    private suspend fun runInject(payloadPath: String) {
        try {
            sendEvent(Event("log", "Preparing payload..."))
            val payloadFile = DesktopUnifiedFile(File(payloadPath))
            val payloadResult = RcmPayloadBuilder().buildPayload(payloadFile)
            if (payloadResult.isFailure) {
                sendEvent(Event("log", "Error building payload: ${payloadResult.exceptionOrNull()?.message}"))
                return
            }
            val payloadBytes = payloadResult.getOrThrow()

            sendEvent(Event("log", "Checking for RCM device..."))
            val device = usbController.findRcmDevice()
            if (device == null) {
                sendEvent(Event("log", "Error: No Switch found in RCM mode (VID: 0955, PID: 7321)."))
                return
            }

            sendEvent(Event("log", "Found RCM device. Injecting..."))
            val success = usbController.injectPayload(device, payloadBytes)
            sendEvent(Event("log", if (success) "Success: Payload injected! The device should boot now." else "Error: Injection failed."))
        } catch (e: Exception) {
            sendEvent(Event("log", "Exception: ${e.message}"))
        } finally {
            sendEvent(Event("injectDone", "done"))
        }
    }

    private suspend fun runConvert(isSplit: Boolean, paths: List<String>, outputDir: String) {
        var success = 0
        var failed = 0
        for (path in paths) {
            val ok = try {
                val file = DesktopUnifiedFile(File(path))
                if (isSplit) fileSplitter.splitFile(file, outputDir) else fileSplitter.mergeFiles(file, outputDir)
            } catch (e: Exception) { false }
            if (ok) success++ else failed++
        }
        val message = if (failed == 0) "Success! Processed $success files." else "Done. Success: $success, Failed: $failed"
        sendEvent(Event("convertDone", message))
    }

    private fun eventLogPrinter() = object : LogPrinter {
        override fun print(message: String, type: MsgType) = sendEvent(Event("log", "[${type.name}] $message"))
        override fun updateProgress(value: Double) {}
        override fun update(files: Map<String, UnifiedFile>, status: FileStatus) {}
        override fun update(file: UnifiedFile, status: FileStatus) {}
        override fun close() {}
    }

    private fun UsbDevice.toJson(): JsonObject = buildJsonObject {
        put("name", name)
        put("vendorId", vendorId)
        put("productId", productId)
    }

    private fun JsonObject.str(key: String, default: String? = null): String =
        this[key]?.jsonPrimitive?.content ?: default ?: throw IllegalArgumentException("Missing param: $key")

    private val JsonObject.obj: JsonObject get() = this

    private fun JsonObject.strList(key: String): List<String> =
        this[key]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
}
