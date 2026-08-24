import SwiftUI
import AppKit

struct RcmView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            GroupBox("Payload Selection") {
                HStack {
                    Button("Select Payload (.bin)") { pickPayload() }
                    Text(appState.selectedPayload?.name ?? "No file selected")
                        .foregroundStyle(appState.selectedPayload == nil ? .secondary : .primary)
                }
            }
            .padding(.horizontal)

            Button {
                appState.injectPayload()
            } label: {
                if appState.isInjecting {
                    HStack {
                        ProgressView().controlSize(.small)
                        Text("Injecting…")
                    }
                } else {
                    Text("Inject Payload")
                }
            }
            .keyboardShortcut(.defaultAction)
            .disabled(appState.selectedPayload == nil || appState.isInjecting)
            .padding(.horizontal)

            GroupBox("Logs") {
                ScrollView {
                    Text(appState.rcmLog.text.isEmpty ? "Ready for RCM injection." : appState.rcmLog.text)
                        .font(.system(.caption, design: .monospaced))
                        .foregroundStyle(appState.rcmLog.text.isEmpty ? .secondary : .primary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(.horizontal)
            .frame(maxHeight: .infinity)
        }
        .padding(.top)
        .navigationTitle("Payload")
        .toolbar {
            ToolbarItem {
                Button {
                    appState.rcmLog.clear()
                } label: {
                    Image(systemName: "trash")
                }
            }
        }
    }

    private func pickPayload() {
        let panel = NSOpenPanel()
        panel.allowedContentTypes = []
        panel.allowsOtherFileTypes = true
        panel.canChooseDirectories = false
        if panel.runModal() == .OK, let url = panel.urls.first {
            appState.selectedPayload = FileEntry(file: .init(filePath: url.path))
        }
    }
}
