package com.mrnrod45.dockhand.linux

import org.gnome.gdk.Display
import org.gnome.gtk.Align
import org.gnome.gtk.Application
import org.gnome.gtk.ApplicationWindow
import org.gnome.gtk.Box
import org.gnome.gtk.CssProvider
import org.gnome.gtk.Image
import org.gnome.gtk.Label
import org.gnome.gtk.ListBox
import org.gnome.gtk.ListBoxRow
import org.gnome.gtk.Orientation
import org.gnome.gtk.Paned
import org.gnome.gtk.Stack

class MainWindow(app: Application, private val state: AppState) : Listener {

    val window: ApplicationWindow = ApplicationWindow(app)
    private val stack = Stack()

    private data class NavItem(val id: String, val label: String, val iconName: String)

    private val navItems = listOf(
        NavItem("upload", "Upload", "document-send-symbolic"),
        NavItem("payload", "Payload", "drive-removable-media-symbolic"),
        NavItem("split-merge", "Split & Merge", "edit-cut-symbolic"),
        NavItem("settings", "Settings", "preferences-system-symbolic")
    )

    init {
        window.title = "Dockhand"
        window.setDefaultSize(960, 640)

        // Gtk.ApplicationWindow only rounds the corners the compositor draws for the
        // titlebar — without libadwaita, the bottom corners stay square unless the
        // content's own background is explicitly rounded to match.
        val cssProvider = CssProvider()
        cssProvider.loadFromString(
            """
            window.background {
                border-radius: 12px;
            }
            """.trimIndent()
        )
        Display.getDefault()?.let {
            // GTK_STYLE_PROVIDER_PRIORITY_APPLICATION — a fixed #define (600) in GTK's
            // own headers, not exposed as a named constant in these bindings.
            org.gnome.gtk.StyleContext.addProviderForDisplay(it, cssProvider, 600)
        }

        stack.addTitled(UploadPage(state).root, "upload", "Upload")
        stack.addTitled(RcmPage(state).root, "payload", "Payload")
        stack.addTitled(SplitMergePage(state).root, "split-merge", "Split & Merge")
        stack.addTitled(SettingsPage(state).root, "settings", "Settings")

        // StackSidebar (GTK4's built-in nav widget) only renders plain text
        // labels with no room for icons, so a custom ListBox is used here
        // instead to match the icon+label sidebar look of the other platforms.
        val sidebarList = ListBox()
        sidebarList.addCssClass("navigation-sidebar")
        navItems.forEach { item ->
            val row = Box(Orientation.HORIZONTAL, 12)
            row.marginTop = 8; row.marginBottom = 8; row.marginStart = 12; row.marginEnd = 12
            row.append(Image.fromIconName(item.iconName))
            row.append(Label.builder().setLabel(item.label).setHalign(Align.START).build())
            val listBoxRow = ListBoxRow()
            listBoxRow.setChild(row)
            sidebarList.append(listBoxRow)
        }
        sidebarList.onRowSelected { row ->
            if (row != null) stack.visibleChildName = navItems[row.index].id
        }
        sidebarList.selectRow(sidebarList.getRowAtIndex(0))

        val paned = Paned(Orientation.HORIZONTAL)
        paned.startChild = sidebarList
        paned.endChild = stack
        paned.position = 200
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
