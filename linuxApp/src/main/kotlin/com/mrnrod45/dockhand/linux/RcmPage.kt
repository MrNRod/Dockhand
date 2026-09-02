package com.mrnrod45.dockhand.linux

import com.mrnrod45.dockhand.domain.rcm.RcmPayloadBuilder
import com.mrnrod45.dockhand.platform.file.DesktopUnifiedFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.gnome.gtk.*
import java.io.File

class RcmPage(private val state: AppState) : Listener {

    val root: Box = Box(Orientation.VERTICAL, 12)

    private val payloadLabel = Label.builder().setLabel("No file selected").setHalign(Align.START).build()
    private val injectButton = Button.withLabel("Inject Payload")
    private val logView = TextView()
    private val logBuffer = logView.buffer
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        root.marginTop = 16; root.marginBottom = 16; root.marginStart = 16; root.marginEnd = 16

        val payloadRow = Box(Orientation.HORIZONTAL, 8)
        val selectButton = Button.withLabel("Select Payload (.bin)")
        selectButton.onClicked { pickPayload() }
        payloadRow.append(selectButton)
        payloadRow.append(payloadLabel)
        root.append(Frame.builder().setLabel("Payload Selection").setChild(payloadRow.withMargin()).build())

        injectButton.addCssClass("suggested-action")
        injectButton.onClicked { injectPayload() }
        root.append(injectButton)

        logView.editable = false
        logView.monospace = true
        val logScroller = ScrolledWindow()
        logScroller.setChild(logView)
        logScroller.vexpand = true
        root.append(Frame.builder().setLabel("Logs").setChild(logScroller.withMargin()).build())

        state.addListener(this)
        onChanged()
    }

    override fun onChanged() {
        payloadLabel.label = state.selectedPayload?.name ?: "No file selected"
        injectButton.sensitive = state.selectedPayload != null && !state.isInjecting
        injectButton.label = if (state.isInjecting) "Injecting…" else "Inject Payload"
        logBuffer.setText(state.rcmLog.toString().ifEmpty { "Ready for RCM injection." }, -1)
    }

    private fun log(message: String) {
        state.rcmLog.append(message).append('\n')
        org.gnome.glib.GLib.idleAddOnce { onChanged() }
    }

    private fun pickPayload() {
        val filter = FileFilter()
        filter.addPattern("*.bin")
        val filters = org.gnome.gio.ListStore<FileFilter>(FileFilter.getType())
        filters.append(filter)

        val dialog = FileDialog.builder().setTitle("Select Payload").setFilters(filters).build()
        dialog.open(null, null) { _, result, _ ->
            try {
                val file = dialog.openFinish(result)
                val path = file.path ?: return@open
                state.selectedPayload = FileEntry(DesktopUnifiedFile(File(path)))
            } catch (e: Exception) {
                // User cancelled or dialog error — nothing to do.
            }
        }
    }

    private fun injectPayload() {
        val payload = state.selectedPayload ?: return
        if (state.isInjecting) return
        state.isInjecting = true
        state.rcmLog.clear()

        scope.launch {
            try {
                log("Preparing payload...")
                val payloadResult = RcmPayloadBuilder().buildPayload(payload.file)
                if (payloadResult.isFailure) {
                    log("Error building payload: ${payloadResult.exceptionOrNull()?.message}")
                    return@launch
                }
                val payloadBytes = payloadResult.getOrThrow()

                log("Checking for RCM device...")
                val device = state.usbController.findRcmDevice()
                if (device == null) {
                    log("Error: No Switch found in RCM mode (VID: 0955, PID: 7321).")
                    return@launch
                }

                log("Found RCM device. Injecting...")
                val success = state.usbController.injectPayload(device, payloadBytes)
                log(if (success) "Success: Payload injected! The device should boot now." else "Error: Injection failed.")
            } catch (e: Exception) {
                log("Exception: ${e.message}")
            } finally {
                state.isInjecting = false
            }
        }
    }
}
