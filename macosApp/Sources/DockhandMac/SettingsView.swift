import SwiftUI

struct SettingsView: View {
    @AppStorage("useRomFolder") private var useRomFolder = false
    @AppStorage("allowXci") private var allowXci = true
    @AppStorage("autoCheckUpdates") private var autoCheckUpdates = true

    var body: some View {
        Form {
            Section {
                Toggle("Auto-check for updates on launch", isOn: $autoCheckUpdates)
                Toggle("Select folder with ROM files instead of individually", isOn: $useRomFolder)
            } header: {
                Text("General")
                    .font(.headline)
            } footer: {
                Text("Folder selection automatically looks for NSP files (and XCI / NSZ / XCZ when enabled) within the chosen folder.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Section {
                Toggle("Allow XCI / NSZ / XCZ selection", isOn: $allowXci)
            } header: {
                Text("File Selection")
                    .font(.headline)
            } footer: {
                Text("Enables support for XCI, NSZ, and XCZ container formats across file selection dialogs.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .formStyle(.grouped)
        .frame(minWidth: 460, minHeight: 320)
        .padding(16)
    }
}
