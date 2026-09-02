package com.mrnrod45.dockhand.linux

import com.mrnrod45.dockhand.domain.file.FileSplitter
import com.mrnrod45.dockhand.domain.file.getFileSplitter
import com.mrnrod45.dockhand.domain.models.UnifiedFile
import com.mrnrod45.dockhand.domain.usb.UsbController
import com.mrnrod45.dockhand.platform.file.DesktopUnifiedFile
import com.mrnrod45.dockhand.platform.usb.DesktopUsbController
import com.mrnrod45.dockhand.platform.usb.NoOpUsbController
import java.io.File

data class FileEntry(val file: UnifiedFile) {
    val name: String get() = file.name
    val path: String get() = file.path
}

fun interface Listener {
    fun onChanged()
}

/** Simple observer pattern — GTK4/java-gi has no built-in reactive binding like SwiftUI. */
class AppState {
    val usbController: UsbController = try {
        DesktopUsbController()
    } catch (e: Throwable) {
        println("Failed to initialize DesktopUsbController (likely missing native libs): ${e.message}")
        NoOpUsbController()
    }

    val fileSplitter: FileSplitter = getFileSplitter()

    // Retained so a running network upload session can actually be stopped later —
    // starting a new NetworkServer() per upload leaked the previous listener forever.
    val networkServer = com.mrnrod45.dockhand.domain.net.NetworkServer()

    // Upload screen
    var selectedProtocol: String = "Goldleaf"
        set(value) {
            field = value
            if (value == "Goldleaf" || value == "Sphaira") transport = "USB"
            notifyChanged()
        }
    var transport: String = "USB"
        set(value) { field = value; notifyChanged() }
    var ipAddress: String = "192.168.1.1"
    val files = mutableListOf<FileEntry>()
    var isUploading: Boolean = false
        set(value) { field = value; notifyChanged() }
    val uploadLog = StringBuilder()

    // RCM screen
    var selectedPayload: FileEntry? = null
        set(value) { field = value; notifyChanged() }
    var isInjecting: Boolean = false
        set(value) { field = value; notifyChanged() }
    val rcmLog = StringBuilder()

    // Split & Merge screen
    var isSplitMode: Boolean = true
        set(value) { field = value; notifyChanged() }
    val selectedPaths = mutableListOf<FileEntry>()
    var outputDir: String = System.getProperty("user.home") + "/Downloads"
        set(value) { field = value; notifyChanged() }
    var isProcessing: Boolean = false
        set(value) { field = value; notifyChanged() }
    var statusMessage: String = ""
        set(value) { field = value; notifyChanged() }

    // Settings — persisted via java.util.prefs, read by UploadPage's file picker/upload logic.
    var useRomFolder: Boolean = SettingsStore.getBool("useRomFolder", false)
        set(value) { field = value; SettingsStore.setBool("useRomFolder", value); notifyChanged() }
    var allowXci: Boolean = SettingsStore.getBool("allowXci", true)
        set(value) { field = value; SettingsStore.setBool("allowXci", value); notifyChanged() }
    var autoCheckUpdates: Boolean = SettingsStore.getBool("autoCheckUpdates", true)
        set(value) { field = value; SettingsStore.setBool("autoCheckUpdates", value); notifyChanged() }

    val isTransportEnabled: Boolean get() = selectedProtocol == "Awoo"

    private val listeners = mutableListOf<Listener>()
    fun addListener(listener: Listener) = listeners.add(listener)
    fun notifyChanged() = listeners.forEach { it.onChanged() }

    fun addFiles(paths: List<String>) {
        files.addAll(paths.map { FileEntry(DesktopUnifiedFile(File(it))) })
        notifyChanged()
    }

    fun removeFile(entry: FileEntry) {
        files.remove(entry)
        notifyChanged()
    }
}
