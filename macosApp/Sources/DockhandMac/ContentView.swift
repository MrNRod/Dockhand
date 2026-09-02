import SwiftUI

enum AppScreen: String, CaseIterable, Identifiable {
    case upload = "Upload"
    case payload = "Payload"
    case splitMerge = "Split & Merge"

    var id: String { rawValue }

    var systemImage: String {
        switch self {
        case .upload: "arrow.up.circle"
        case .payload: "bolt.circle"
        case .splitMerge: "square.split.2x1"
        }
    }
}

struct ContentView: View {
    @State private var selection: AppScreen = .upload
    // Icon-rail vs titled sidebar. The system split-view toggle only fully
    // hides the column, so this button stays in `.navigation` and the default
    // sidebar toggle is removed.
    @State private var isSidebarCompact = false

    var body: some View {
        NavigationSplitView {
            List(AppScreen.allCases, selection: $selection) { screen in
                Group {
                    if isSidebarCompact {
                        Label(screen.rawValue, systemImage: screen.systemImage)
                            .labelStyle(.iconOnly)
                            .frame(maxWidth: .infinity)
                    } else {
                        Label(screen.rawValue, systemImage: screen.systemImage)
                            .labelStyle(.titleAndIcon)
                    }
                }
                .help(screen.rawValue)
                .tag(screen)
            }
            .listStyle(.sidebar)
            .navigationSplitViewColumnWidth(
                min: isSidebarCompact ? MacMetrics.compactSidebarWidth : 160,
                ideal: isSidebarCompact ? MacMetrics.compactSidebarWidth : MacMetrics.sidebarWidth,
                max: isSidebarCompact ? 72 : 240
            )
        } detail: {
            switch selection {
            case .upload: UploadView()
            case .payload: RcmView()
            case .splitMerge: SplitMergeView()
            }
        }
        .navigationSplitViewStyle(.balanced)
        .toolbar(removing: .sidebarToggle)
        .toolbar {
            ToolbarItem(placement: .navigation) {
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        isSidebarCompact.toggle()
                    }
                } label: {
                    Image(systemName: "sidebar.left")
                }
                .help(isSidebarCompact ? "Show Sidebar" : "Compact Sidebar")
            }
        }
    }
}
