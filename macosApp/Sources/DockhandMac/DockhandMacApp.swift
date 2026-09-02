import SwiftUI
import AppKit

// `swift run` launches a bare Mach-O with no real .app bundle on disk, so
// LaunchServices won't register a Dock tile for it and won't raise it above
// whatever app (e.g. an editor) currently has focus — both need to be forced
// explicitly here rather than relying on the embedded Info.plist alone.
final class AppDelegate: NSObject, NSApplicationDelegate {
    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.regular)
        NSApp.activate(ignoringOtherApps: true)

        // No .app bundle means no CFBundleIconFile lookup either — set the
        // Dock/Cmd+Tab icon directly from the bundled resource instead.
        if let url = Bundle.module.url(forResource: "AppIcon", withExtension: "png"),
           let image = NSImage(contentsOf: url) {
            NSApp.applicationIconImage = image
        }
    }
}

@main
struct DockhandMacApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
                .frame(minWidth: 820, minHeight: 560)
        }
        .windowToolbarStyle(.unified)
        .defaultSize(width: 920, height: 640)
        .commands {
            CommandGroup(replacing: .newItem) { }
            SidebarCommands()
        }

        // SwiftUI's dedicated Preferences scene — automatically wired to
        // Cmd+, and the app menu's "Settings…" item, no custom plumbing needed.
        Settings {
            SettingsView()
                .environmentObject(appState)
                .frame(width: 480, height: 380)
        }
    }
}
