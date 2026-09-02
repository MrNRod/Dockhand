import AppKit
import SwiftUI

/// Unified sizing and spacing tokens for macOS native UI
enum MacMetrics {
    static let cardRadius: CGFloat = 14
    static let cardPadding: CGFloat = 16
    static let scenePadding: CGFloat = 18
    static let stackSpacing: CGFloat = 14
    static let sectionSpacing: CGFloat = 10
    static let minConcentricRadius: CGFloat = 8
    static let compactSidebarWidth: CGFloat = 52
    static let sidebarWidth: CGFloat = 190

    static func concentricRadius(
        outer: CGFloat = cardRadius,
        padding: CGFloat = cardPadding
    ) -> CGFloat {
        max(minConcentricRadius, outer - padding)
    }
}

// MARK: - Glass / material surface

struct MacGlassBackground: NSViewRepresentable {
    var cornerRadius: CGFloat
    var tint: NSColor? = nil

    func makeNSView(context: Context) -> NSVisualEffectView {
        let effect = NSVisualEffectView()
        effect.material = .headerView
        effect.blendingMode = .withinWindow
        effect.state = .followsWindowActiveState
        effect.wantsLayer = true
        effect.layer?.cornerRadius = cornerRadius
        effect.layer?.cornerCurve = .continuous
        effect.layer?.masksToBounds = true
        return effect
    }

    func updateNSView(_ nsView: NSVisualEffectView, context: Context) {
        nsView.layer?.cornerRadius = cornerRadius
    }
}

private struct MacGlassSurfaceModifier: ViewModifier {
    var radius: CGFloat
    var padding: CGFloat

    func body(content: Content) -> some View {
        content
            .padding(padding)
            .background {
                MacGlassBackground(cornerRadius: radius)
            }
            .overlay {
                RoundedRectangle(cornerRadius: radius, style: .continuous)
                    .strokeBorder(Color.primary.opacity(0.06), lineWidth: 1)
            }
            .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
            .shadow(color: .black.opacity(0.04), radius: 6, y: 1)
    }
}

extension View {
    func macGlassSurface(
        radius: CGFloat = MacMetrics.cardRadius,
        padding: CGFloat = MacMetrics.cardPadding
    ) -> some View {
        modifier(MacGlassSurfaceModifier(radius: radius, padding: padding))
    }

    @ViewBuilder
    func macConcentricClip(
        outer: CGFloat = MacMetrics.cardRadius,
        padding: CGFloat = 10
    ) -> some View {
        clipShape(RoundedRectangle(
            cornerRadius: MacMetrics.concentricRadius(outer: outer, padding: padding),
            style: .continuous
        ))
    }

    @ViewBuilder
    func macProminentButton() -> some View {
        buttonStyle(.borderedProminent)
            .controlSize(.regular)
    }

    @ViewBuilder
    func macGlassButton() -> some View {
        buttonStyle(.bordered)
            .controlSize(.regular)
    }
}

// MARK: - Card

struct MacCard<Content: View>: View {
    var title: String
    var systemImage: String
    var fillHeight: Bool = false
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: MacMetrics.sectionSpacing) {
            Label(title, systemImage: systemImage)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(.primary)
            content()
        }
        .frame(maxWidth: .infinity, maxHeight: fillHeight ? .infinity : nil, alignment: .topLeading)
        .macGlassSurface()
    }
}

struct MacInsetWell<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .padding(10)
            .background {
                RoundedRectangle(
                    cornerRadius: MacMetrics.concentricRadius(outer: MacMetrics.cardRadius, padding: 8),
                    style: .continuous
                )
                .fill(Color(nsColor: .controlBackgroundColor).opacity(0.5))
            }
            .overlay {
                RoundedRectangle(
                    cornerRadius: MacMetrics.concentricRadius(outer: MacMetrics.cardRadius, padding: 8),
                    style: .continuous
                )
                .strokeBorder(Color.primary.opacity(0.05), lineWidth: 1)
            }
            .macConcentricClip(outer: MacMetrics.cardRadius, padding: 8)
    }
}

// MARK: - Screen Scaffold

struct MacScreenScaffold<Content: View, Actions: View>: View {
    @ViewBuilder var content: () -> Content
    @ViewBuilder var actions: () -> Actions

    var body: some View {
        VStack(spacing: 0) {
            content()
                .padding(MacMetrics.scenePadding)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)

            Divider()

            HStack(spacing: 12) {
                actions()
            }
            .padding(.horizontal, MacMetrics.scenePadding)
            .padding(.vertical, 12)
            .background(.bar)
        }
    }
}
