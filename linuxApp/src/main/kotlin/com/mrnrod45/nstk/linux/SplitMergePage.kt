package com.mrnrod45.nstk.linux

import com.mrnrod45.nstk.platform.file.DesktopUnifiedFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.gnome.gtk.*
import java.io.File

class SplitMergePage(private val state: AppState) : Listener {

    val root: Box = Box(Orientation.VERTICAL, 12)

    private val splitToggle = ToggleButton.withLabel("Split")
    private val mergeToggle = ToggleButton.withLabel("Merge")
    private val selectButton = Button.withLabel("Select File…")
    private val clearButton = Button.withLabel("Clear")
    private val outputLabel = Label.builder().setHalign(Align.START).setEllipsize(org.gnome.pango.EllipsizeMode.MIDDLE).build()
    private val pathsListBox = ListBox()
    private val statusLabel = Label.builder().setHalign(Align.START).build()
    private val actionButton = Button.withLabel("Split")
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        root.marginTop = 16; root.marginBottom = 16; root.marginStart = 16; root.marginEnd = 16

        val modeRow = Box(Orientation.HORIZONTAL, 0)
        modeRow.addCssClass("linked") // GTK "linked" style = native segmented-control look
        mergeToggle.setGroup(splitToggle)
        splitToggle.active = true
        modeRow.append(splitToggle)
        modeRow.append(mergeToggle)
        splitToggle.onToggled { if (splitToggle.active) state.isSplitMode = true }
        mergeToggle.onToggled { if (mergeToggle.active) state.isSplitMode = false }

        val filesRow = Box(Orientation.HORIZONTAL, 8)
        selectButton.onClicked { pickFiles() }
        clearButton.onClicked { state.selectedPaths.clear(); state.notifyChanged() }
        filesRow.append(selectButton)
        filesRow.append(clearButton)

        val outputRow = Box(Orientation.HORIZONTAL, 8)
        outputRow.append(Label.builder().setLabel("Save to:").build())
        outputLabel.hexpand = true
        outputRow.append(outputLabel)
        val changeButton = Button.withLabel("Change…")
        changeButton.onClicked { pickOutputDir() }
        outputRow.append(changeButton)

        val operationBox = Box(Orientation.VERTICAL, 12)
        operationBox.append(modeRow)
        operationBox.append(filesRow)
        operationBox.append(outputRow)
        root.append(Frame.builder().setLabel("Operation").setChild(operationBox).build())

        val pathsScroller = ScrolledWindow()
        pathsScroller.setChild(pathsListBox)
        pathsScroller.vexpand = true
        pathsListBox.setPlaceholder(Label.builder().setLabel("No files selected").build())
        root.append(Frame.builder().setLabel("Files to Process").setChild(pathsScroller).build())

        root.append(statusLabel)

        val actionRow = Box(Orientation.HORIZONTAL, 0)
        val spacer = Box(Orientation.HORIZONTAL, 0)
        spacer.hexpand = true
        actionRow.append(spacer)
        actionButton.addCssClass("suggested-action")
        actionButton.onClicked { startConversion() }
        actionRow.append(actionButton)
        root.append(actionRow)

        state.addListener(this)
        onChanged()
    }

    override fun onChanged() {
        selectButton.label = if (state.isSplitMode) "Select File…" else "Add Files…"
        clearButton.sensitive = state.selectedPaths.isNotEmpty()
        outputLabel.label = state.outputDir
        statusLabel.label = state.statusMessage
        actionButton.label = if (state.isProcessing) "Processing…" else if (state.isSplitMode) "Split" else "Merge"
        actionButton.sensitive = state.selectedPaths.isNotEmpty() && !state.isProcessing

        var child = pathsListBox.firstChild
        while (child != null) {
            val next = child.nextSibling
            pathsListBox.remove(child)
            child = next
        }
        state.selectedPaths.forEach { entry ->
            pathsListBox.append(Label.builder().setLabel(entry.path).setHalign(Align.START).build())
        }
    }

    private fun pickFiles() {
        val dialog = FileDialog.builder().setTitle(if (state.isSplitMode) "Select File" else "Add Files").build()
        val onPicked: (List<String>) -> Unit = { paths ->
            val entries = paths.map { FileEntry(DesktopUnifiedFile(File(it))) }
            if (state.isSplitMode) {
                state.selectedPaths.clear()
                state.selectedPaths.addAll(entries)
            } else {
                val existing = state.selectedPaths.map { it.path }.toSet()
                state.selectedPaths.addAll(entries.filter { it.path !in existing })
            }
            state.notifyChanged()
        }

        if (state.isSplitMode) {
            dialog.open(null, null) { _, result, _ ->
                try {
                    val file = dialog.openFinish(result)
                    file.path?.let { onPicked(listOf(it)) }
                } catch (e: Exception) { /* cancelled */ }
            }
        } else {
            dialog.openMultiple(null, null) { _, result, _ ->
                try {
                    val list = dialog.openMultipleFinish(result)
                    val paths = (0 until list.nItems).mapNotNull { i -> (list.getItem(i) as? org.gnome.gio.File)?.path }
                    onPicked(paths)
                } catch (e: Exception) { /* cancelled */ }
            }
        }
    }

    private fun pickOutputDir() {
        val dialog = FileDialog.builder().setTitle("Choose Output Folder").build()
        dialog.selectFolder(null, null) { _, result, _ ->
            try {
                val folder = dialog.selectFolderFinish(result)
                folder.path?.let { state.outputDir = it }
            } catch (e: Exception) { /* cancelled */ }
        }
    }

    private fun startConversion() {
        if (state.selectedPaths.isEmpty() || state.isProcessing) return
        state.isProcessing = true
        state.statusMessage = if (state.isSplitMode) "Splitting..." else "Merging..."
        val splitting = state.isSplitMode
        val outDir = state.outputDir
        val items = state.selectedPaths.toList()

        scope.launch {
            var success = 0
            var failed = 0
            for (entry in items) {
                val ok = try {
                    if (splitting) state.fileSplitter.splitFile(entry.file, outDir)
                    else state.fileSplitter.mergeFiles(entry.file, outDir)
                } catch (e: Exception) { false }
                if (ok) success++ else failed++
            }
            state.isProcessing = false
            state.statusMessage = if (failed == 0) "Success! Processed $success files."
                else "Done. Success: $success, Failed: $failed"
        }
    }
}
