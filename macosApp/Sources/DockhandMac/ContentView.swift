import SwiftUI
import AppKit

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

/// Hooks into the macOS window toolbar to intercept the native leading sidebar toggle
/// button and repurpose it to toggle compact icon-only mode instead of collapsing to 0 width.
struct SidebarConfigurator: NSViewRepresentable {
    @Binding var isCompact: Bool

    func makeCoordinator() -> Coordinator {
        Coordinator(isCompact: $isCompact)
    }

    func makeNSView(context: Context) -> NSView {
        let view = NSView()
        DispatchQueue.main.async {
            context.coordinator.attach(to: view)
        }
        return view
    }

    func updateNSView(_ nsView: NSView, context: Context) {
        context.coordinator.isCompact = $isCompact
        if context.coordinator.lastCompactState != isCompact {
            context.coordinator.lastCompactState = isCompact
            context.coordinator.updateTooltipOnly()
        }
    }

    final class Coordinator: NSObject {
        var isCompact: Binding<Bool>
        var lastCompactState: Bool
        private weak var window: NSWindow?
        private var observers: [NSObjectProtocol] = []
        private var isConfigured = false

        init(isCompact: Binding<Bool>) {
            self.isCompact = isCompact
            self.lastCompactState = isCompact.wrappedValue
            super.init()
        }

        deinit {
            for obs in observers {
                NotificationCenter.default.removeObserver(obs)
            }
        }

        func attach(to view: NSView) {
            guard let window = view.window else {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { [weak self, weak view] in
                    guard let view = view else { return }
                    self?.attach(to: view)
                }
                return
            }
            self.window = window

            if !isConfigured {
                configureWindowAndToolbar()
                isConfigured = true
            }

            observers.append(
                NotificationCenter.default.addObserver(
                    forName: NSWindow.didBecomeKeyNotification,
                    object: window,
                    queue: .main
                ) { [weak self] _ in
                    self?.configureWindowAndToolbar()
                }
            )
        }

        func configureWindowAndToolbar() {
            guard let window = window else { return }

            if let splitVC = findSplitViewController(in: window.contentViewController) {
                if let sidebarItem = splitVC.splitViewItems.first, sidebarItem.canCollapse {
                    sidebarItem.canCollapse = false
                }
            }

            if let toolbar = window.toolbar {
                for item in toolbar.items {
                    if item.itemIdentifier.rawValue.contains("ToggleSidebar") || item.itemIdentifier == .toggleSidebar {
                        if item.target !== self {
                            item.target = self
                            item.action = #selector(handleSidebarToggle)
                        }
                        item.toolTip = isCompact.wrappedValue ? "Expand Sidebar" : "Compact Sidebar"
                    }
                }
            }
        }

        func updateTooltipOnly() {
            guard let window = window, let toolbar = window.toolbar else { return }
            for item in toolbar.items {
                if item.itemIdentifier.rawValue.contains("ToggleSidebar") || item.itemIdentifier == .toggleSidebar {
                    item.toolTip = isCompact.wrappedValue ? "Expand Sidebar" : "Compact Sidebar"
                }
            }
        }

        @objc func handleSidebarToggle() {
            withAnimation(.easeInOut(duration: 0.2)) {
                isCompact.wrappedValue.toggle()
            }
        }

        private func findSplitViewController(in vc: NSViewController?) -> NSSplitViewController? {
            if let split = vc as? NSSplitViewController { return split }
            for child in vc?.children ?? [] {
                if let found = findSplitViewController(in: child) { return found }
            }
            return nil
        }
    }
}

struct ContentView: View {
    @EnvironmentObject private var appState: AppState
    @State private var selection: AppScreen = .upload

    var body: some View {
        NavigationSplitView {
            List(AppScreen.allCases, selection: $selection) { screen in
                Group {
                    if appState.isSidebarCompact {
                        Image(systemName: screen.systemImage)
                            .font(.system(size: 16, weight: .medium))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 4)
                    } else {
                        Label(screen.rawValue, systemImage: screen.systemImage)
                            .font(.system(size: 13, weight: .medium))
                            .padding(.vertical, 2)
                    }
                }
                .help(screen.rawValue)
                .tag(screen)
            }
            .listStyle(.sidebar)
            .navigationSplitViewColumnWidth(
                min: appState.isSidebarCompact ? MacMetrics.compactSidebarWidth : 160,
                ideal: appState.isSidebarCompact ? MacMetrics.compactSidebarWidth : MacMetrics.sidebarWidth,
                max: appState.isSidebarCompact ? 64 : 240
            )
        } detail: {
            switch selection {
            case .upload: UploadView()
            case .payload: RcmView()
            case .splitMerge: SplitMergeView()
            }
        }
        .navigationSplitViewStyle(.balanced)
        .background(SidebarConfigurator(isCompact: $appState.isSidebarCompact))
    }
}
