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
                    VStack(alignment: .leading, spacing: MacMetrics.sectionSpacing) {
                        HStack(spacing: 20) {
                            HStack(spacing: 8) {
                                Text("Protocol:")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
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

                            HStack(spacing: 8) {
                                Text("Transport:")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
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
                            HStack(spacing: 8) {
                                Text("Switch IP:")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                                TextField("192.168.1.50", text: $appState.ipAddress)
                                    .textFieldStyle(.roundedBorder)
                                    .frame(maxWidth: 220)
                            }
                        }
                    }
                }

                MacCard(title: "Selected Files", systemImage: "doc.on.doc", fillHeight: true) {
                    if appState.files.isEmpty {
                        ContentUnavailableView(
                            "No Files Added",
                            systemImage: "doc.badge.plus",
                            description: Text("Click Add Files below to select NSP or XCI files")
                        )
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        List {
                            ForEach(appState.files) { entry in
                                HStack(spacing: 10) {
                                    Image(systemName: "doc.fill")
                                        .font(.system(size: 16))
                                        .foregroundStyle(.tint)
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(entry.name)
                                            .font(.body)
                                            .lineLimit(1)
                                        Text(entry.path)
                                            .font(.caption)
                                            .foregroundStyle(.secondary)
                                            .lineLimit(1)
                                            .truncationMode(.middle)
                                            .textSelection(.enabled)
                                    }
                                    Spacer(minLength: 8)
                                    Button(role: .destructive) {
                                        appState.removeFile(entry)
                                    } label: {
                                        Image(systemName: "trash")
                                            .font(.system(size: 12))
                                    }
                                    .buttonStyle(.borderless)
                                    .help("Remove file")
                                }
                                .padding(.vertical, 2)
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
                                    .padding(4)
                            }
                            .frame(height: 110)
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

            if !appState.files.isEmpty {
                Button("Clear All") {
                    appState.files.removeAll()
                }
                .macGlassButton()
            }

            Spacer()

            Button {
                appState.startUpload()
            } label: {
                if appState.servingOverNet {
                    Label("Stop Server", systemImage: "stop.fill")
                } else if appState.isUploading {
                    HStack(spacing: 6) {
                        ProgressView().controlSize(.small)
                        Text("Uploading…")
                    }
                } else {
                    Label(
                        appState.transport == "USB" ? "Upload to Switch" : "Upload over Network",
                        systemImage: "arrow.up.circle.fill"
                    )
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
            if panel.runModal() == .OK {
                let matched = panel.urls.filter { extensions.contains($0.pathExtension.lowercased()) }
                appState.addFiles(matched)
            }
        }
    }

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
