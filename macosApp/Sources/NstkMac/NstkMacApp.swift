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
    }
}

@main
struct NstkMacApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
                .frame(minWidth: 820, minHeight: 560)
        }
        .windowResizability(.contentSize)
        .commands {
            CommandGroup(replacing: .newItem) { }
        }

        // SwiftUI's dedicated Preferences scene — automatically wired to
        // Cmd+, and the app menu's "Preferences…" item, no custom plumbing needed.
        Settings {
            SettingsView()
                .environmentObject(appState)
                .frame(width: 480, height: 420)
        }
    }
}
