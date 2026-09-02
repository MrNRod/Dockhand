import SwiftUI
import AppKit

struct SplitMergeView: View {
    @EnvironmentObject private var appState: AppState
    @AppStorage("allowXci") private var allowXci = true

    var body: some View {
        MacScreenScaffold {
            VStack(alignment: .leading, spacing: MacMetrics.stackSpacing) {
                MacCard(title: "Operation", systemImage: "square.split.2x1") {
                    VStack(alignment: .leading, spacing: MacMetrics.sectionSpacing) {
                        HStack(spacing: 12) {
                            Picker("", selection: $appState.isSplitMode) {
                                Text("Split").tag(true)
                                Text("Merge").tag(false)
                            }
                            .pickerStyle(.segmented)
                            .labelsHidden()
                            .fixedSize()

                            Button {
                                pickFiles()
                            } label: {
                                Label(appState.isSplitMode ? "Select File…" : "Add Chunks…", systemImage: "plus")
                            }
                            .macGlassButton()

                            if !appState.selectedPaths.isEmpty {
                                Button("Clear All") {
                                    appState.selectedPaths.removeAll()
                                }
                                .macGlassButton()
                            }

                            Spacer(minLength: 0)
                        }

                        HStack(spacing: 8) {
                            Text("Save to:")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                            Text(appState.outputDir)
                                .font(.callout)
                                .lineLimit(1)
                                .truncationMode(.middle)
                                .foregroundStyle(.primary)
                                .textSelection(.enabled)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Button("Change…") {
                                pickOutputDir()
                            }
                            .macGlassButton()
                        }
                    }
                }

                MacCard(title: "Files to Process", systemImage: "doc.on.doc", fillHeight: true) {
                    if appState.selectedPaths.isEmpty {
                        ContentUnavailableView(
                            appState.isSplitMode ? "No File Selected" : "No Files Added",
                            systemImage: "doc.badge.plus",
                            description: Text(
                                appState.isSplitMode
                                    ? "Select an NSP or XCI file to split into chunks for FAT32"
                                    : "Add split chunk files (e.g. .00, .01) to merge back into a single file"
                            )
                        )
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        List {
                            ForEach(appState.selectedPaths) { entry in
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
                                        appState.selectedPaths.removeAll { $0.id == entry.id }
                                    } label: {
                                        Image(systemName: "trash")
                                            .font(.system(size: 12))
                                    }
                                    .buttonStyle(.borderless)
                                    .help("Remove file")
                                }
                                .padding(.vertical, 2)
                                .contextMenu {
                                    Button("Remove", role: .destructive) {
                                        appState.selectedPaths.removeAll { $0.id == entry.id }
                                    }
                                }
                            }
                        }
                        .listStyle(.plain)
                        .scrollContentBackground(.hidden)
                        .macConcentricClip()
                    }
                }

                if !appState.statusMessage.isEmpty {
                    Text(appState.statusMessage)
                        .font(.callout)
                        .foregroundStyle(.secondary)
                        .padding(.horizontal, 4)
                }
            }
        } actions: {
            Spacer()
            Button {
                appState.startConversion()
            } label: {
                if appState.isProcessing {
                    HStack(spacing: 6) {
                        ProgressView().controlSize(.small)
                        Text("Processing…")
                    }
                } else {
                    Label(
                        appState.isSplitMode ? "Split File" : "Merge Files",
                        systemImage: appState.isSplitMode ? "square.split.2x1.fill" : "square.split.1x2.fill"
                    )
                }
            }
            .keyboardShortcut(.defaultAction)
            .macProminentButton()
            .disabled(appState.selectedPaths.isEmpty || appState.isProcessing)
        }
        .navigationTitle("Split & Merge")
        .navigationSubtitle(appState.isSplitMode ? "Split" : "Merge")
    }

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
