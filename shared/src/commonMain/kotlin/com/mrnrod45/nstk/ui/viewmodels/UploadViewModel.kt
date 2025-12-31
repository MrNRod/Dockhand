package com.mrnrod45.nstk.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrnrod45.nstk.domain.models.UnifiedFile
import com.mrnrod45.nstk.domain.usb.UsbController
import com.mrnrod45.nstk.platform.file.FilePicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UploadViewModel(
    private val usbController: UsbController,
    private val filePicker: FilePicker,
    private val settingsViewModel: SettingsViewModel
) : ViewModel() {
    private val _files = MutableStateFlow<List<UnifiedFile>>(emptyList())
    val files: StateFlow<List<UnifiedFile>> = _files.asStateFlow()

    private val _selectedProtocol = MutableStateFlow("Goldleaf")
    val selectedProtocol: StateFlow<String> = _selectedProtocol.asStateFlow()
    
    // TODO: Add device selection state
    
    // Initial load
    init {
        refreshDevices()
    }

    fun addFiles(newFiles: List<UnifiedFile>) {
        _files.value = _files.value + newFiles
    }

    fun removeFile(file: UnifiedFile) {
        _files.value = _files.value - file
    }

    private val _transport = MutableStateFlow("USB")
    val transport: StateFlow<String> = _transport.asStateFlow()

    private val _ipAddress = MutableStateFlow("192.168.1.1")
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()
    
    // Console Logs for UI
    private val _logs = MutableStateFlow("")
    val logs: StateFlow<String> = _logs.asStateFlow()

    fun setTransport(transport: String) {
        _transport.value = transport
    }

    fun setIpAddress(ip: String) {
        _ipAddress.value = ip
    }

    fun setProtocol(protocol: String) {
        _selectedProtocol.value = protocol
        if (protocol == "Goldleaf" || protocol == "Sphaira") {
            _transport.value = "USB"
        }
    }
    
    fun refreshDevices() {
        viewModelScope.launch {
            // For now just logging/print, we need to expose this state
            val devices = usbController.listDevices()
            println("Found ${devices.size} devices")
            devices.forEach { println("Device: ${it.name} (${it.vendorId}:${it.productId})") }
        }
    }
    
    private val networkServer = com.mrnrod45.nstk.domain.net.NetworkServer()

    override fun onCleared() {
        super.onCleared()
        networkServer.stop()
    }

    fun startUpload() {
        viewModelScope.launch {
            if (_files.value.isEmpty()) {
                println("No files selected")
                return@launch
            }

            val fileMap = _files.value.associateBy { it.name }
            
            // Console Printer for debugging/verification
            val logPrinter = object : com.mrnrod45.nstk.domain.models.LogPrinter {
                override fun print(message: String, type: com.mrnrod45.nstk.domain.models.MsgType) {
                    val logMsg = "[${type.name}] $message"
                    println(logMsg)
                    _logs.value = logMsg + "\n" + _logs.value // Prepend for newest top or Append? Typically Append.
                    // Let's Append but auto-scroll in UI. Actually Prepend is easier to read without scrolled.
                    // Let's Append: `_logs.value += ...`
                    _logs.value += logMsg + "\n"
                }
                override fun updateProgress(value: Double) {
                    val percent = (value * 100).toInt()
                    if (percent % 10 == 0) print("$percent%.. ")
                }
                override fun update(files: Map<String, UnifiedFile>, status: com.mrnrod45.nstk.domain.models.FileStatus) {}
                override fun update(file: UnifiedFile, status: com.mrnrod45.nstk.domain.models.FileStatus) {}
                override fun close() { println("\nLogPrinter Closed") }
            }

            if (_transport.value == "NET") {
                val switchIp = _ipAddress.value
                val hostPort = 6042 
                
                println("Stopping any existing Network Server...")
                networkServer.stop()
                
                println("Starting Network Server on port $hostPort. Handshaking to $switchIp")
                
                try {
                     networkServer.start(_files.value, switchIp, hostPort) { msg ->
                         val type = if (msg.contains("FAIL")) com.mrnrod45.nstk.domain.models.MsgType.FAIL else com.mrnrod45.nstk.domain.models.MsgType.INFO
                         logPrinter.print(msg, type)
                     }
                     // logPrinter.print("Network Server init...", com.mrnrod45.nstk.domain.models.MsgType.INFO) // Handled by callback
                } catch (e: Exception) {
                    e.printStackTrace()
                    logPrinter.print("Network Error: ${e.message}", com.mrnrod45.nstk.domain.models.MsgType.FAIL)
                }
                return@launch
            }

            // USB Logic
            val deviceList = usbController.listDevices()
            if (deviceList.isEmpty()) {
                println("No devices found")
                logPrinter.print("No USB Devices found", com.mrnrod45.nstk.domain.models.MsgType.FAIL)
                return@launch
            }
            
            val device = deviceList.first()
            val connection = device.open()
            if (connection == null) {
                println("Failed to open connection")
                logPrinter.print("Failed to open USB connection. Check permissions or cable.", com.mrnrod45.nstk.domain.models.MsgType.FAIL)
                return@launch
            }
            
            println("Starting upload to ${device.name} using ${_selectedProtocol.value}")
            logPrinter.print("Starting USB Upload protocol: ${_selectedProtocol.value}", com.mrnrod45.nstk.domain.models.MsgType.INFO)
            
            // Claim Interface (Dynamic)
            if (!connection.claimInterface(connection.activeInterfaceIndex)) {
                logPrinter.print("Failed to claim USB Interface ${connection.activeInterfaceIndex}", com.mrnrod45.nstk.domain.models.MsgType.FAIL)
                connection.close()
                return@launch
            }
            
            // Give the device a moment to settle after Claim/ClearHalt
            logPrinter.print("Initializing connection...", com.mrnrod45.nstk.domain.models.MsgType.INFO)
            try {
                kotlinx.coroutines.delay(500)
            } catch (e: Exception) { /* ignore */ }
            
            try {
                if (_selectedProtocol.value == "Goldleaf") {
                    val protocol = com.mrnrod45.nstk.domain.protocols.Goldleaf(connection, logPrinter)
                    protocol.start(fileMap)
                } else {
                    // Awoo / Tinfoil / Sphaira
                    val protocol = com.mrnrod45.nstk.domain.protocols.Tinfoil(connection, logPrinter)
                    val isSphaira = _selectedProtocol.value == "Sphaira"
                    protocol.start(fileMap, isSphaira)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logPrinter.print("Upload Error: ${e.message}", com.mrnrod45.nstk.domain.models.MsgType.FAIL)
            } finally {
                 try {
                     connection.close()
                     println("USB Connection Closed")
                 } catch (e: Exception) {
                     e.printStackTrace()
                 }
            }
        }
    }
    fun openFilePicker() {
        viewModelScope.launch {
            val allowXci = settingsViewModel.allowXci.value
            val showOnlyNsp = settingsViewModel.showOnlyNsp.value // For Goldleaf if we want to be strict, but user asked about XCI/Check
            
            // Logic based on user request:
            // "Allow XCI / NSZ / XCZ files selection for Awoo/TinWoo Installer and Sphaira."
            // If checked: allow everything (NSP + those).
            // If unchecked: allow only NSP? Or strictly block those?
            // The text implies that unchecked means "Don't allow XCI/NSZ/XCZ".
            // So we assume NSP is always allowed.
            
            val allowedExtensions = mutableListOf<String>()
            allowedExtensions.add("nsp") // Always allowed
            
            if (allowXci) {
                allowedExtensions.add("xci")
                allowedExtensions.add("nsz")
                allowedExtensions.add("xcz")
                // Maybe others? The setting text specifies these. 
            }
            
            val picked = filePicker.pickFiles(allowedExtensions)
            addFiles(picked)
        }
    }
}
