import SwiftUI
import AppKit

struct RcmView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        MacScreenScaffold {
            VStack(alignment: .leading, spacing: MacMetrics.stackSpacing) {
                MacCard(title: "Payload Selection", systemImage: "bolt") {
                    HStack(spacing: 12) {
                        Button {
                            pickPayload()
                        } label: {
                            Label("Select Payload (.bin)…", systemImage: "folder")
                        }
                        .macGlassButton()

                        if let payload = appState.selectedPayload {
                            HStack(spacing: 6) {
                                Image(systemName: "doc.fill")
                                    .foregroundStyle(.tint)
                                Text(payload.name)
                                    .font(.body)
                                    .lineLimit(1)
                                    .truncationMode(.middle)
                                    .textSelection(.enabled)
                            }
                        } else {
                            Text("No payload selected")
                                .font(.callout)
                                .foregroundStyle(.secondary)
                        }
                        Spacer(minLength: 0)
                    }
                }

                MacCard(title: "Logs", systemImage: "text.alignleft", fillHeight: true) {
                    MacInsetWell {
                        ScrollView {
                            Text(appState.rcmLog.text.isEmpty ? "Ready for RCM injection." : appState.rcmLog.text)
                                .font(.caption.monospaced())
                                .foregroundStyle(appState.rcmLog.text.isEmpty ? .secondary : .primary)
                                .textSelection(.enabled)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(4)
                        }
                    }
                }
            }
        } actions: {
            if !appState.rcmLog.text.isEmpty {
                Button("Clear Log") {
                    appState.rcmLog.clear()
                }
                .macGlassButton()
            }

            Spacer()

            Button {
                appState.injectPayload()
            } label: {
                if appState.isInjecting {
                    HStack(spacing: 6) {
                        ProgressView().controlSize(.small)
                        Text("Injecting…")
                    }
                } else {
                    Label("Inject Payload", systemImage: "bolt.fill")
                }
            }
            .keyboardShortcut(.defaultAction)
            .macProminentButton()
            .disabled(appState.selectedPayload == nil || appState.isInjecting)
        }
        .navigationTitle("Payload")
    }

    private func pickPayload() {
        let panel = NSOpenPanel()
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = false
        guard panel.runModal() == .OK, let url = panel.urls.first else { return }
        if url.pathExtension.lowercased() == "bin" {
            appState.selectedPayload = FileEntry(file: .init(filePath: url.path))
        } else {
            appState.rcmLog.append("[FAIL] Please select a .bin payload file.")
        }
    }
}
