package com.mrnrod45.nstk.linux

import org.gnome.gtk.FileFilter
import org.gnome.gtk.Widget

/** Frame draws a border but adds zero internal padding — content sits flush against
 *  it otherwise, so anything going directly into a Frame needs this explicitly. */
fun <T : Widget> T.withMargin(margin: Int = 12): T {
    marginTop = margin
    marginBottom = margin
    marginStart = margin
    marginEnd = margin
    return this
}

/** Builds a GTK file-chooser filter list (one pattern per extension) for FileDialog.setFilters. */
fun extensionFilterStore(extensions: List<String>): org.gnome.gio.ListStore<FileFilter> {
    val filter = FileFilter()
    extensions.forEach { ext -> filter.addPattern("*.$ext") }
    val filters = org.gnome.gio.ListStore<FileFilter>(FileFilter.getType())
    filters.append(filter)
    return filters
}
