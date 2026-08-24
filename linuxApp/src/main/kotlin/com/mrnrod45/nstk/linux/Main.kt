package com.mrnrod45.nstk.linux

import org.gnome.gio.ApplicationFlags
import org.gnome.gtk.Application

fun main(args: Array<String>) {
    val app = Application("com.mrnrod45.nstk.linux", ApplicationFlags.DEFAULT_FLAGS)
    val state = AppState()
    app.onActivate {
        MainWindow(app, state).present()
    }
    app.run(args)
}
