import SwiftUI

struct SettingsView: View {
    @AppStorage("useRomFolder") private var useRomFolder = false
    @AppStorage("showOnlyNsp") private var showOnlyNsp = false
    @AppStorage("allowXci") private var allowXci = true
    @AppStorage("autoCheckUpdates") private var autoCheckUpdates = true

    var body: some View {
        Form {
            Section("General") {
                Toggle("Auto-check for updates on launch", isOn: $autoCheckUpdates)
                Toggle("Select folder with ROM files instead of individually", isOn: $useRomFolder)
            }
            Section("Goldleaf") {
                Toggle("Show only *.nsp files", isOn: $showOnlyNsp)
            }
            Section("Awoo / Tinfoil / Sphaira") {
                Toggle("Allow XCI / NSZ / XCZ selection", isOn: $allowXci)
            }
        }
        .formStyle(.grouped)
        .padding()
    }
}
