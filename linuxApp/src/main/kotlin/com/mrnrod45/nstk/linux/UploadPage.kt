package com.mrnrod45.nstk.linux

import com.mrnrod45.nstk.domain.models.LogPrinter
import com.mrnrod45.nstk.domain.models.MsgType
import com.mrnrod45.nstk.domain.models.UnifiedFile
import com.mrnrod45.nstk.domain.models.FileStatus
import com.mrnrod45.nstk.domain.net.NetworkServer
import com.mrnrod45.nstk.domain.protocols.Goldleaf
import com.mrnrod45.nstk.domain.protocols.Tinfoil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.gnome.gio.ListStore
import org.gnome.gtk.*

class UploadPage(private val state: AppState) : Listener {

    val root: Box = Box(Orientation.VERTICAL, 12)

    private val protocolDropdown = DropDown.fromStrings(arrayOf("Goldleaf", "Awoo", "Sphaira"))
    private val transportDropdown = DropDown.fromStrings(arrayOf("USB", "NET"))
    private val ipEntry = Entry()
    private val ipRow = Box(Orientation.HORIZONTAL, 8)
    private val filesListBox = ListBox()
    private val emptyLabel = Label.builder().setLabel("No files added").build()
    private val uploadButton = Button.withLabel("Upload to Switch")
    private val logView = TextView()
    private val logBuffer = logView.buffer
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        root.marginTop = 16; root.marginBottom = 16; root.marginStart = 16; root.marginEnd = 16

        val connectionRow = Box(Orientation.HORIZONTAL, 16)
        val protocolBox = Box(Orientation.VERTICAL, 4)
        protocolBox.append(Label.builder().setLabel("Protocol").setHalign(Align.START).build())
        protocolBox.append(protocolDropdown)
        val transportBox = Box(Orientation.VERTICAL, 4)
        transportBox.append(Label.builder().setLabel("Transport").setHalign(Align.START).build())
        transportBox.append(transportDropdown)
        connectionRow.append(protocolBox)
        connectionRow.append(transportBox)
        root.append(Frame.builder().setLabel("Connection").setChild(connectionRow).build())

        ipRow.append(Label.builder().setLabel("Switch IP Address").build())
        ipRow.append(ipEntry)
        ipEntry.setText(state.ipAddress)
        ipRow.visible = false
        root.append(ipRow)

        val filesScroller = ScrolledWindow()
        filesScroller.setChild(filesListBox)
        filesScroller.vexpand = true
        filesListBox.setPlaceholder(emptyLabel)
        root.append(Frame.builder().setLabel("Selected Files").setChild(filesScroller).build())

        logView.editable = false
        logView.monospace = true
        val logScroller = ScrolledWindow()
        logScroller.setChild(logView)
        logScroller.setSizeRequest(-1, 120)
        root.append(logScroller)

        val actionsRow = Box(Orientation.HORIZONTAL, 8)
        val addButton = Button.withLabel("Add Files…")
        addButton.onClicked { pickFiles() }
        actionsRow.append(addButton)
        val spacer = Box(Orientation.HORIZONTAL, 0)
        spacer.hexpand = true
        actionsRow.append(spacer)
        uploadButton.addCssClass("suggested-action")
        uploadButton.onClicked { startUpload() }
        actionsRow.append(uploadButton)
        root.append(actionsRow)

        protocolDropdown.onNotify("selected") {
            val names = arrayOf("Goldleaf", "Awoo", "Sphaira")
            state.selectedProtocol = names[protocolDropdown.selected.toInt()]
        }
        transportDropdown.onNotify("selected") {
            val names = arrayOf("USB", "NET")
            state.transport = names[transportDropdown.selected.toInt()]
        }

        state.addListener(this)
        onChanged()
    }

    override fun onChanged() {
        transportDropdown.sensitive = state.isTransportEnabled
        ipRow.visible = state.transport == "NET"
        uploadButton.label = if (state.transport == "USB") "Upload to Switch" else "Upload over Network"
        uploadButton.sensitive = state.files.isNotEmpty() && !state.isUploading

        var child = filesListBox.firstChild
        while (child != null) {
            val next = child.nextSibling
            filesListBox.remove(child)
            child = next
        }
        state.files.forEach { entry ->
            val row = Box(Orientation.HORIZONTAL, 8)
            row.marginTop = 4; row.marginBottom = 4; row.marginStart = 8; row.marginEnd = 8
            val labelBox = Box(Orientation.VERTICAL, 2)
            labelBox.append(Label.builder().setLabel(entry.name).setHalign(Align.START).build())
            labelBox.append(Label.builder().setLabel(entry.path).setHalign(Align.START).setCssClasses(arrayOf("dim-label")).build())
            labelBox.hexpand = true
            row.append(labelBox)
            val removeButton = Button.fromIconName("edit-delete-symbolic")
            removeButton.onClicked { state.removeFile(entry) }
            row.append(removeButton)
            filesListBox.append(row)
        }

        logBuffer.setText(state.uploadLog.toString(), -1)
    }

    private fun pickFiles() {
        val dialog = FileDialog.builder().setTitle("Select Files").build()
        dialog.openMultiple(null, null) { _, result, _ ->
            try {
                val list = dialog.openMultipleFinish(result)
                val paths = (0 until list.nItems).mapNotNull { i ->
                    (list.getItem(i) as? org.gnome.gio.File)?.path
                }
                state.addFiles(paths)
            } catch (e: Exception) {
                // User cancelled or dialog error — nothing to do.
            }
        }
    }

    private fun startUpload() {
        if (state.files.isEmpty() || state.isUploading) return
        state.isUploading = true
        state.uploadLog.clear()

        val logPrinter = object : LogPrinter {
            override fun print(message: String, type: MsgType) {
                val line = "[${type.name}] $message"
                println(line)
                state.uploadLog.append(line).append('\n')
                javaFxSafeNotify()
            }
            override fun updateProgress(value: Double) {}
            override fun update(files: Map<String, UnifiedFile>, status: FileStatus) {}
            override fun update(file: UnifiedFile, status: FileStatus) {}
            override fun close() {}
        }

        val fileMap = state.files.associate { it.name to it.file }
        val protocolName = state.selectedProtocol
        val transportMode = state.transport
        val switchIp = state.ipAddress

        scope.launch {
            try {
                if (transportMode == "NET") {
                    NetworkServer().start(
                        files = fileMap.values.toList(),
                        hostIp = "",
                        port = 6042,
                        hostExtra = "",
                        noRequestsServe = false,
                        switchIp = switchIp
                    ) { msg -> logPrinter.print(msg, MsgType.INFO) }
                    return@launch
                }

                val device = state.usbController.listDevices().firstOrNull()
                val connection = device?.open()
                if (connection == null) {
                    logPrinter.print("No USB device found or failed to open connection.", MsgType.FAIL)
                    return@launch
                }
                try {
                    if (!connection.claimInterface(0)) {
                        logPrinter.print("Failed to claim USB interface.", MsgType.FAIL)
                        return@launch
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
                logPrinter.print("Upload error: ${e.message}", MsgType.FAIL)
            } finally {
                state.isUploading = false
            }
        }
    }

    // GTK is not thread-safe; UI updates triggered from coroutine/background
    // threads must be marshalled back onto the main loop.
    private fun javaFxSafeNotify() {
        org.gnome.glib.GLib.idleAddOnce { onChanged() }
    }
}
