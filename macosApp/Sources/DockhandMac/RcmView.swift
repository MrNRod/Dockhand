import SwiftUI
import AppKit

struct RcmView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        MacScreenScaffold {
            VStack(alignment: .leading, spacing: MacMetrics.stackSpacing) {
                MacCard(title: "Payload", systemImage: "bolt") {
                    HStack(spacing: 12) {
                        Button("Select Payload (.bin)") { pickPayload() }
                            .macGlassButton()
                        Text(appState.selectedPayload?.name ?? "No file selected")
                            .foregroundStyle(appState.selectedPayload == nil ? .secondary : .primary)
                            .lineLimit(1)
                            .truncationMode(.middle)
                            .textSelection(.enabled)
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
                        }
                    }
                }
            }
        } actions: {
            Spacer()
            Button {
                appState.injectPayload()
            } label: {
                if appState.isInjecting {
                    HStack {
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
        .navigationSubtitle(appState.selectedPayload?.name ?? "No payload selected")
        .toolbar {
            ToolbarItem {
                Button {
                    appState.rcmLog.clear()
                } label: {
                    Image(systemName: "trash")
                }
                .help("Clear Log")
            }
        }
    }

    private func pickPayload() {
        let panel = NSOpenPanel()
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = false
        // Not using allowedContentTypes: a dynamically-synthesized UTType for a
        // non-registered extension like "bin" can make NSOpenPanel refuse to let the user
        // select anything at all on some macOS versions. Filter the result instead.
        guard panel.runModal() == .OK, let url = panel.urls.first else { return }
        if url.pathExtension.lowercased() == "bin" {
            appState.selectedPayload = FileEntry(file: .init(filePath: url.path))
        } else {
            appState.rcmLog.append("[FAIL] Please select a .bin payload file.")
        }
    }
}
