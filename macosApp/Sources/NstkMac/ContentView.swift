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

    var body: some View {
        NavigationSplitView {
            List(AppScreen.allCases, selection: $selection) { screen in
                Label(screen.rawValue, systemImage: screen.systemImage)
                    .tag(screen)
            }
            .navigationSplitViewColumnWidth(200)
        } detail: {
            switch selection {
            case .upload: UploadView()
            case .payload: RcmView()
            case .splitMerge: SplitMergeView()
            case .none: Text("Select a screen")
            }
        }
    }
}
