import SwiftUI
import AppKit

// When launched as a bare Mach-O (e.g. `swift run`), LaunchServices won't
// register a Dock tile or load CFBundleIconFile from a bundle on disk.
// In a real .app bundle, macOS handles the Dock tile and native AppIcon.icns
// automatically.
final class AppDelegate: NSObject, NSApplicationDelegate {
    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.regular)
        NSApp.activate(ignoringOtherApps: true)

        // Only explicitly set applicationIconImage if running outside a .app bundle
        if Bundle.main.bundleURL.pathExtension != "app" {
            if let url = Bundle.module.url(forResource: "AppIcon", withExtension: "png"),
               let image = NSImage(contentsOf: url) {
                NSApp.applicationIconImage = image
            }
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
            CommandGroup(replacing: .sidebar) {
                Button(appState.isSidebarCompact ? "Expand Sidebar" : "Compact Sidebar") {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        appState.isSidebarCompact.toggle()
                    }
                }
                .keyboardShortcut("s", modifiers: [.command, .option])
            }
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
