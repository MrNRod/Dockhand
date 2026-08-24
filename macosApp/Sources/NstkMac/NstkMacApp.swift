import SwiftUI

@main
struct NstkMacApp: App {
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
                .frame(width: 420, height: 240)
        }
    }
}
