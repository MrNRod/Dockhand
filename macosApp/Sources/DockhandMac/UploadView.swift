import SwiftUI
import AppKit

struct UploadView: View {
    @EnvironmentObject private var appState: AppState
    @AppStorage("useRomFolder") private var useRomFolder = false
    @AppStorage("allowXci") private var allowXci = true

    var body: some View {
        MacScreenScaffold {
            VStack(alignment: .leading, spacing: MacMetrics.stackSpacing) {
                MacCard(title: "Connection", systemImage: "link") {
                    HStack(alignment: .top, spacing: 24) {
                        LabeledContent("Protocol") {
                            Picker("Protocol", selection: Binding(
                                get: { appState.selectedProtocol },
                                set: { appState.setProtocol($0) }
                            )) {
                                Text("Goldleaf").tag("Goldleaf")
                                Text("Tinfoil (Awoo)").tag("Awoo")
                                Text("Tinfoil (Sphaira)").tag("Sphaira")
                            }
                            .labelsHidden()
                            .fixedSize()
                        }
                        LabeledContent("Transport") {
                            Picker("Transport", selection: $appState.transport) {
                                Text("USB").tag("USB")
                                Text("NET").tag("NET")
                            }
                            .labelsHidden()
                            .fixedSize()
                            .disabled(!appState.isTransportEnabled)
                        }
                        Spacer(minLength: 0)
                    }
                    if appState.transport == "NET" {
                        TextField("Switch IP Address", text: $appState.ipAddress)
                            .textFieldStyle(.roundedBorder)
                    }
                }

                MacCard(title: "Selected Files", systemImage: "doc.on.doc", fillHeight: true) {
                    if appState.files.isEmpty {
                        ContentUnavailableView(
                            "No files added",
                            systemImage: "doc",
                            description: Text("Click Add Files to get started")
                        )
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        List {
                            ForEach(appState.files) { entry in
                                HStack {
                                    Label {
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(entry.name)
                                            Text(entry.path)
                                                .font(.caption)
                                                .foregroundStyle(.secondary)
                                                .lineLimit(1)
                                                .truncationMode(.middle)
                                                .textSelection(.enabled)
                                        }
                                    } icon: {
                                        Image(systemName: "doc")
                                    }
                                    Spacer()
                                    Button(role: .destructive) { appState.removeFile(entry) } label: {
                                        Image(systemName: "trash")
                                    }
                                    .buttonStyle(.borderless)
                                    .help("Remove")
                                }
                                .contextMenu {
                                    Button("Remove", role: .destructive) { appState.removeFile(entry) }
                                }
                            }
                        }
                        .listStyle(.plain)
                        .scrollContentBackground(.hidden)
                        .macConcentricClip()
                    }
                }

                if !appState.uploadLog.text.isEmpty {
                    MacCard(title: "Logs", systemImage: "text.alignleft") {
                        MacInsetWell {
                            ScrollView {
                                Text(appState.uploadLog.text)
                                    .font(.caption.monospaced())
                                    .textSelection(.enabled)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .frame(height: 120)
                        }
                    }
                }
            }
        } actions: {
            Button {
                pickFiles()
            } label: {
                Label("Add Files…", systemImage: "plus")
            }
            .macGlassButton()

            Spacer()

            Button {
                appState.startUpload()
            } label: {
                if appState.servingOverNet {
                    Text("Stop Server")
                } else if appState.isUploading {
                    ProgressView().controlSize(.small)
                } else {
                    Text(appState.transport == "USB" ? "Upload to Switch" : "Upload over Network")
                }
            }
            .keyboardShortcut(.defaultAction)
            .macProminentButton()
            .disabled(!appState.servingOverNet && (appState.files.isEmpty || appState.isUploading))
        }
        .navigationTitle("Upload")
        .navigationSubtitle(appState.selectedProtocol)
    }

    private func pickFiles() {
        let extensions = allowedExtensions()

        if useRomFolder {
            let panel = NSOpenPanel()
            panel.canChooseDirectories = true
            panel.canChooseFiles = false
            panel.allowsMultipleSelection = false
            guard panel.runModal() == .OK, let folder = panel.urls.first else { return }
            appState.addFiles(enumerateFiles(in: folder, extensions: extensions))
        } else {
            let panel = NSOpenPanel()
            panel.allowsMultipleSelection = true
            panel.canChooseDirectories = false
            panel.canChooseFiles = true
            // Not using allowedContentTypes here: combining several dynamically-synthesized
            // UTTypes for non-registered extensions (nsp/xci/nsz/xcz) can make NSOpenPanel
            // refuse to let the user select *any* file at all on some macOS versions, rather
            // than just narrowing the list. Filter the result instead, like every other
            // picker in this codebase already does.
            if panel.runModal() == .OK {
                let matched = panel.urls.filter { extensions.contains($0.pathExtension.lowercased()) }
                appState.addFiles(matched)
            }
        }
    }

    /// Mirrors composeApp's UploadViewModel.openFilePicker() extension logic:
    /// .nsp is always allowed; XCI/NSZ/XCZ only when allowXci is on.
    private func allowedExtensions() -> [String] {
        var extensions = ["nsp"]
        if allowXci {
            extensions += ["xci", "nsz", "xcz"]
        }
        return extensions
    }

    private func enumerateFiles(in folder: URL, extensions: [String]) -> [URL] {
        guard let enumerator = FileManager.default.enumerator(
            at: folder, includingPropertiesForKeys: [.isRegularFileKey]
        ) else { return [] }

        var results: [URL] = []
        for case let url as URL in enumerator {
            if extensions.contains(url.pathExtension.lowercased()) {
                results.append(url)
            }
        }
        return results
    }
}
