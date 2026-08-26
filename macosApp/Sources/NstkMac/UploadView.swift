import SwiftUI
import AppKit
import UniformTypeIdentifiers

struct UploadView: View {
    @EnvironmentObject private var appState: AppState
    @AppStorage("useRomFolder") private var useRomFolder = false
    @AppStorage("allowXci") private var allowXci = true

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            GroupBox("Connection") {
                HStack(spacing: 16) {
                    VStack(alignment: .leading) {
                        Text("Protocol").font(.caption).foregroundStyle(.secondary)
                        Picker("", selection: Binding(
                            get: { appState.selectedProtocol },
                            set: { appState.setProtocol($0) }
                        )) {
                            Text("Goldleaf").tag("Goldleaf")
                            Text("Tinfoil (Awoo)").tag("Awoo")
                            Text("Tinfoil (Sphaira)").tag("Sphaira")
                        }
                        .labelsHidden()
                    }
                    VStack(alignment: .leading) {
                        Text("Transport").font(.caption).foregroundStyle(.secondary)
                        Picker("", selection: $appState.transport) {
                            Text("USB").tag("USB")
                            Text("NET").tag("NET")
                        }
                        .labelsHidden()
                        .disabled(!appState.isTransportEnabled)
                    }
                }
                if appState.transport == "NET" {
                    TextField("Switch IP Address", text: $appState.ipAddress)
                        .textFieldStyle(.roundedBorder)
                        .padding(.top, 8)
                }
            }
            .padding(.horizontal)

            GroupBox("Selected Files") {
                if appState.files.isEmpty {
                    ContentUnavailableView("No files added", systemImage: "doc",
                                            description: Text("Click \"Add Files\" to get started"))
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        ForEach(appState.files) { entry in
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(entry.name)
                                    Text(entry.path).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                Button(role: .destructive) { appState.removeFile(entry) } label: {
                                    Image(systemName: "trash")
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
            }
            .padding(.horizontal)
            .frame(maxHeight: .infinity)

            if !appState.uploadLog.text.isEmpty {
                GroupBox("Logs") {
                    ScrollView {
                        Text(appState.uploadLog.text)
                            .font(.system(.caption, design: .monospaced))
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .frame(height: 120)
                }
                .padding(.horizontal)
            }

            HStack {
                Button {
                    pickFiles()
                } label: {
                    Label("Add Files…", systemImage: "plus")
                }

                Spacer()

                Button {
                    appState.startUpload()
                } label: {
                    if appState.isUploading {
                        ProgressView().controlSize(.small)
                    } else {
                        Text(appState.transport == "USB" ? "Upload to Switch" : "Upload over Network")
                    }
                }
                .keyboardShortcut(.defaultAction)
                .disabled(appState.files.isEmpty || appState.isUploading)
            }
            .padding([.horizontal, .bottom])
        }
        .padding(.top)
        .navigationTitle("Upload")
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
            if !extensions.isEmpty {
                panel.allowedContentTypes = extensions.compactMap { UTType(filenameExtension: $0) }
            }
            if panel.runModal() == .OK {
                appState.addFiles(panel.urls)
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
