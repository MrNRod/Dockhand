package com.mrnrod45.dockhand.linux

import org.gnome.gtk.Align
import org.gnome.gtk.Box
import org.gnome.gtk.Label
import org.gnome.gtk.Orientation
import org.gnome.gtk.Switch

class SettingsPage(private val state: AppState) {

    val root: Box = Box(Orientation.VERTICAL, 16)

    init {
        root.marginTop = 16; root.marginBottom = 16; root.marginStart = 16; root.marginEnd = 16

        root.append(sectionLabel("General"))
        root.append(switchRow("Auto-check for updates on launch", initial = state.autoCheckUpdates) {
            state.autoCheckUpdates = it
        })
        root.append(switchRow("Select folder with ROM files instead of individually", initial = state.useRomFolder) {
            state.useRomFolder = it
        })

        root.append(sectionLabel("File Selection"))
        root.append(switchRow("Allow XCI / NSZ / XCZ selection", initial = state.allowXci) {
            state.allowXci = it
        })
    }

    private fun sectionLabel(text: String): Label =
        Label.builder().setLabel(text).setHalign(Align.START).setCssClasses(arrayOf("heading")).build()

    private fun switchRow(text: String, initial: Boolean, onToggle: (Boolean) -> Unit): Box {
        val row = Box(Orientation.HORIZONTAL, 12)
        val label = Label.builder().setLabel(text).setHalign(Align.START).build()
        label.hexpand = true
        val toggle = Switch()
        toggle.active = initial
        toggle.onNotify("active") { onToggle(toggle.active) }
        row.append(label)
        row.append(toggle)
        return row
    }
}
