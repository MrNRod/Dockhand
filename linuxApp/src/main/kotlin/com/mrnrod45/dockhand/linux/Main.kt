package com.mrnrod45.dockhand.linux

import org.gnome.gio.ApplicationFlags
import org.gnome.gtk.Application

fun main(args: Array<String>) {
    val app = Application("com.mrnrod45.dockhand.linux", ApplicationFlags.DEFAULT_FLAGS)
    val state = AppState()
    app.onActivate {
        MainWindow(app, state).present()
    }
    app.run(args)
}
