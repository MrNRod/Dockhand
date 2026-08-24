package com.mrnrod45.nstk.linux

import org.gnome.gtk.Application
import org.gnome.gtk.ApplicationWindow
import org.gnome.gtk.Orientation
import org.gnome.gtk.Paned
import org.gnome.gtk.Stack
import org.gnome.gtk.StackSidebar

class MainWindow(app: Application, private val state: AppState) : Listener {

    val window: ApplicationWindow = ApplicationWindow(app)
    private val stack = Stack()

    init {
        window.title = "NS-ToolKit"
        window.setDefaultSize(960, 640)

        val sidebar = StackSidebar()
        sidebar.setStack(stack)
        sidebar.setSizeRequest(180, -1)

        stack.addTitled(UploadPage(state).root, "upload", "Upload")
        stack.addTitled(RcmPage(state).root, "payload", "Payload")
        stack.addTitled(SplitMergePage(state).root, "split-merge", "Split & Merge")
        stack.addTitled(SettingsPage(state).root, "settings", "Settings")

        val paned = Paned(Orientation.HORIZONTAL)
        paned.startChild = sidebar
        paned.endChild = stack
        paned.position = 180
        paned.setResizeStartChild(false)

        window.setChild(paned)
        state.addListener(this)
    }

    override fun onChanged() {
        // Pages own their own widget updates; this hook exists for any
        // window-chrome-level reactions (e.g. title changes) in the future.
    }

    fun present() = window.present()
}
