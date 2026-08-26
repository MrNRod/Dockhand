import SwiftUI
import AppKit

struct SplitMergeView: View {
    @EnvironmentObject private var appState: AppState
    @AppStorage("allowXci") private var allowXci = true

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            GroupBox("Operation") {
                VStack(alignment: .leading, spacing: 12) {
                    Picker("", selection: $appState.isSplitMode) {
                        Text("Split").tag(true)
                        Text("Merge").tag(false)
                    }
                    .pickerStyle(.segmented)
                    .labelsHidden()
                    .frame(width: 200)

                    HStack {
                        Button(appState.isSplitMode ? "Select File…" : "Add Files…") { pickFiles() }
                        Button("Clear") { appState.selectedPaths.removeAll() }
                            .disabled(appState.selectedPaths.isEmpty)
                    }

                    HStack {
                        Text("Save to:").foregroundStyle(.secondary)
                        Text(appState.outputDir).lineLimit(1).truncationMode(.middle)
                        Spacer()
                        Button("Change…") { pickOutputDir() }
                    }
                }
            }
            .padding(.horizontal)

            GroupBox("Files to Process") {
                if appState.selectedPaths.isEmpty {
                    ContentUnavailableView("No files selected", systemImage: "doc.on.doc")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List(appState.selectedPaths) { entry in
                        Text(entry.path).font(.system(.body, design: .monospaced))
                    }
                }
            }
            .padding(.horizontal)
            .frame(maxHeight: .infinity)

            if !appState.statusMessage.isEmpty {
                Text(appState.statusMessage)
                    .padding(.horizontal)
            }

            HStack {
                Spacer()
                Button {
                    appState.startConversion()
                } label: {
                    if appState.isProcessing {
                        ProgressView().controlSize(.small)
                    } else {
                        Text(appState.isSplitMode ? "Split" : "Merge")
                    }
                }
                .keyboardShortcut(.defaultAction)
                .disabled(appState.selectedPaths.isEmpty || appState.isProcessing)
            }
            .padding([.horizontal, .bottom])
        }
        .padding(.top)
        .navigationTitle("Split & Merge")
    }

    /// Mirrors UploadView's allowedExtensions(): .nsp always, XCI/NSZ/XCZ only when allowXci
    /// is on. Only meaningful in split mode — merge mode selects split chunks (e.g.
    /// "game.nsp.00"), which don't carry these extensions, so it's left unfiltered.
    private func allowedExtensions() -> [String] {
        var extensions = ["nsp"]
        if allowXci {
            extensions += ["xci", "nsz", "xcz"]
        }
        return extensions
    }

    private func pickFiles() {
        let panel = NSOpenPanel()
        panel.allowsMultipleSelection = !appState.isSplitMode
        panel.canChooseDirectories = true
        panel.canChooseFiles = true
        // Not using allowedContentTypes: combining several dynamically-synthesized UTTypes
        // for non-registered extensions (nsp/xci/nsz/xcz) can make NSOpenPanel refuse to let
        // the user select anything at all on some macOS versions. Filter the result instead.
        if panel.runModal() == .OK {
            var urls = panel.urls
            if appState.isSplitMode {
                let extensions = allowedExtensions()
                urls = urls.filter { url in
                    var isDirectory: ObjCBool = false
                    FileManager.default.fileExists(atPath: url.path, isDirectory: &isDirectory)
                    return isDirectory.boolValue || extensions.contains(url.pathExtension.lowercased())
                }
            }
            let entries = urls.map { FileEntry(file: .init(filePath: $0.path)) }
            if appState.isSplitMode {
                appState.selectedPaths = entries
            } else {
                let existing = Set(appState.selectedPaths.map(\.path))
                appState.selectedPaths.append(contentsOf: entries.filter { !existing.contains($0.path) })
            }
        }
    }

    private func pickOutputDir() {
        let panel = NSOpenPanel()
        panel.canChooseDirectories = true
        panel.canChooseFiles = false
        if panel.runModal() == .OK, let url = panel.urls.first {
            appState.outputDir = url.path
        }
    }
}
