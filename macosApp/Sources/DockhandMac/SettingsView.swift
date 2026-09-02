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
            } footer: {
                Text("Folder selection looks for NSP files, and XCI / NSZ / XCZ when those types are allowed.")
            }
            Section {
                Toggle("Allow XCI / NSZ / XCZ selection", isOn: $allowXci)
            } header: {
                Text("File Selection")
            }
        }
        .formStyle(.grouped)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .scenePadding()
    }
}
