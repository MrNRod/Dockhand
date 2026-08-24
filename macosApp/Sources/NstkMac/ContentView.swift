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
    @State private var selection: AppScreen? = .upload
    // A plain HStack instead of NavigationSplitView: SwiftUI's automatic
    // sidebar toggle only supports fully hiding the column, and
    // `.toolbar(removing: .sidebarToggle)` is unreliable on macOS for
    // suppressing it — this sidesteps that entirely with one custom button.
    @State private var isSidebarCompact = false

    var body: some View {
        HStack(spacing: 0) {
            List(AppScreen.allCases, selection: $selection) { screen in
                if isSidebarCompact {
                    HStack {
                        Spacer(minLength: 0)
                        Image(systemName: screen.systemImage)
                            .font(.system(size: 17))
                            .frame(height: 22)
                        Spacer(minLength: 0)
                    }
                    .tag(screen)
                } else {
                    Label(screen.rawValue, systemImage: screen.systemImage)
                        .labelStyle(.titleAndIcon)
                        .tag(screen)
                }
            }
            .listStyle(.sidebar)
            .frame(width: isSidebarCompact ? 56 : 200)

            Divider()

            Group {
                switch selection {
                case .upload: UploadView()
                case .payload: RcmView()
                case .splitMerge: SplitMergeView()
                case .none: Text("Select a screen")
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .toolbar {
            ToolbarItem(placement: .navigation) {
                Button {
                    withAnimation { isSidebarCompact.toggle() }
                } label: {
                    Image(systemName: "sidebar.left")
                }
            }
        }
    }
}
