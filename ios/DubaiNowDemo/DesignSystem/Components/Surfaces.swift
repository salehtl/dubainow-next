import SwiftUI

// MARK: - Card

/// White card with the Canvas elevation (near 0 1 2 · far 0 8 24). Content sits on solid
/// surfaces; Liquid Glass is reserved for navigation.
struct DNCardModifier: ViewModifier {
    var radius: CGFloat = DNRadius.card
    var fill: Color = DNColor.surfacePrimary
    var elevated: Bool = true

    func body(content: Content) -> some View {
        content
            .background(fill, in: .rect(cornerRadius: radius, style: .continuous))
            .clipShape(.rect(cornerRadius: radius, style: .continuous))
            .modifier(DNElevation(enabled: elevated))
    }
}

/// The two-shadow Canvas elevation. CSS blur ≈ 2 × SwiftUI radius.
struct DNElevation: ViewModifier {
    var enabled: Bool = true
    func body(content: Content) -> some View {
        if enabled {
            content
                .shadow(color: DNColor.elevationNear, radius: 1, x: 0, y: 1)
                .shadow(color: DNColor.elevationFar, radius: 12, x: 0, y: 8)
        } else {
            content
        }
    }
}

extension View {
    /// A white card (radius 20 by default) with the Canvas elevation.
    func dnCard(radius: CGFloat = DNRadius.card, fill: Color = DNColor.surfacePrimary, elevated: Bool = true) -> some View {
        modifier(DNCardModifier(radius: radius, fill: fill, elevated: elevated))
    }

    func dnElevation(_ enabled: Bool = true) -> some View { modifier(DNElevation(enabled: enabled)) }

    /// The grey canvas behind grouped screens (Home, Services, Activity, sheets).
    func dnCanvasBackground() -> some View {
        background(DNColor.surfaceSecondary.ignoresSafeArea())
    }
}

// MARK: - Grouped list

/// A white rounded group of rows (radius 24) with inset separators — the Canvas list group.
/// Rows are separated automatically; pass any views.
struct DNListGroup<Content: View>: View {
    @Environment(\.displayScale) private var displayScale
    var header: String? = nil
    var footer: String? = nil
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let header {
                Text(header.uppercased())
                    .dnFont(.overline)
                    .foregroundStyle(DNColor.textGroupedHeader)
                    .padding(.horizontal, 4)
                    .accessibilityAddTraits(.isHeader)
            }
            VStack(spacing: 0) {
                Group(subviews: content) { subviews in
                    ForEach(Array(subviews.enumerated()), id: \.offset) { index, subview in
                        subview
                        if index < subviews.count - 1 {
                            Rectangle()
                                .fill(DNColor.listRowSeparator)
                                .frame(height: 1 / displayScale)
                                .padding(.leading, 16)
                        }
                    }
                }
            }
            .background(DNColor.surfacePrimary, in: .rect(cornerRadius: DNRadius.listGroup, style: .continuous))
            .clipShape(.rect(cornerRadius: DNRadius.listGroup, style: .continuous))
            if let footer {
                Text(footer)
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                    .padding(.horizontal, 4)
            }
        }
    }
}

/// A standard list row: optional leading view, title, optional supporting line,
/// optional trailing value, chevron by default. Minimum height 52 (two-line 64).
struct DNListRow<Leading: View>: View {
    var title: String
    var subtitle: String? = nil
    var value: String? = nil
    var showsChevron: Bool = true
    var titleColor: Color = DNColor.listRowTitle
    @ViewBuilder var leading: Leading

    var body: some View {
        HStack(spacing: 12) {
            leading
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .dnFont(.rowTitle)
                    .foregroundStyle(titleColor)
                if let subtitle {
                    Text(subtitle)
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.listRowSupporting)
                }
            }
            Spacer(minLength: 8)
            if let value {
                Text(value)
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textSecondary)
                    .multilineTextAlignment(.trailing)
            }
            if showsChevron {
                Image(systemName: "chevron.forward")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(DNColor.listRowChevron)
                    .accessibilityHidden(true)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(minHeight: subtitle == nil ? 52 : 64)
        .contentShape(.rect)
    }
}

extension DNListRow where Leading == EmptyView {
    init(title: String, subtitle: String? = nil, value: String? = nil, showsChevron: Bool = true, titleColor: Color = DNColor.listRowTitle) {
        self.init(title: title, subtitle: subtitle, value: value, showsChevron: showsChevron, titleColor: titleColor) { EmptyView() }
    }
}

/// A plain SF Symbol in a 28 pt slot for list rows (settings-style rows).
struct DNRowIcon: View {
    var symbol: String
    var color: Color = DNColor.listRowIcon
    var body: some View {
        Image(systemName: symbol)
            .font(.system(size: 18, weight: .regular))
            .foregroundStyle(color)
            .frame(width: 28, height: 28)
            .accessibilityHidden(true)
    }
}
