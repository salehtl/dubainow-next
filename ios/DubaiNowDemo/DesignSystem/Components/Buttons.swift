import SwiftUI

/// Canvas buttons are flat pills (v0.5): Large 54, Medium 44, Small 36.
struct DNButtonStyle: ButtonStyle {
    enum Kind { case primary, secondary, tertiary, destructive, inverse }
    enum Size { case large, medium, small }

    var kind: Kind = .primary
    var size: Size = .large
    var fullWidth: Bool = true

    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        let pressed = configuration.isPressed
        configuration.label
            .dnFont(size == .small ? .subheadline : .button)
            .fontWeight(.semibold)
            .lineLimit(1)
            .minimumScaleFactor(0.8)
            .foregroundStyle(foreground)
            .padding(.horizontal, horizontalInset)
            .frame(maxWidth: fullWidth ? .infinity : nil)
            .frame(minHeight: height)
            .background(background(pressed: pressed), in: .capsule)
            .contentShape(.capsule)
            .opacity(isEnabled ? 1 : 0.6)
            .animation(DNMotion.fast, value: pressed)
    }

    private var height: CGFloat {
        switch size {
        case .large: DNSpace.buttonHeightLarge
        case .medium: DNSpace.buttonHeightMedium
        case .small: DNSpace.buttonHeightSmall
        }
    }

    private var horizontalInset: CGFloat {
        switch size {
        case .large: DNSpace.buttonInsetLarge
        case .medium: DNSpace.buttonInsetMedium
        case .small: DNSpace.buttonInsetSmall
        }
    }

    private var foreground: Color {
        guard isEnabled else { return DNColor.textDisabled }
        switch kind {
        case .primary: return DNColor.textOnAccent
        case .secondary: return DNColor.textLink
        case .tertiary: return DNColor.textLink
        case .destructive: return DNColor.actionDestructive
        case .inverse: return DNColor.textOnInverse
        }
    }

    private func background(pressed: Bool) -> Color {
        guard isEnabled else { return DNColor.actionDisabled }
        switch kind {
        case .primary: return pressed ? DNColor.actionAccentPressed : DNColor.actionAccent
        case .secondary: return pressed ? DNColor.actionAccentSubtlePressed : DNColor.actionAccentSubtle
        case .tertiary: return pressed ? DNColor.actionAccentSubtle : .clear
        case .destructive: return pressed ? DNColor.red50.opacity(0.8) : DNColor.surfacePrimary
        case .inverse: return pressed ? DNColor.ink850 : DNColor.surfaceInverse
        }
    }
}

extension ButtonStyle where Self == DNButtonStyle {
    static func dn(_ kind: DNButtonStyle.Kind = .primary, size: DNButtonStyle.Size = .large, fullWidth: Bool = true) -> DNButtonStyle {
        DNButtonStyle(kind: kind, size: size, fullWidth: fullWidth)
    }
}

/// A round Liquid Glass icon button for places outside a toolbar (sheet close, overlays).
/// Inside toolbars, use plain toolbar buttons — iOS gives them glass automatically.
struct GlassIconButton: View {
    var symbol: String
    var label: String
    var size: CGFloat = 44
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: size * 0.4, weight: .semibold))
                .foregroundStyle(DNColor.textPrimary)
                .frame(width: size, height: size)
        }
        .buttonStyle(.plain)
        .glassEffect(.regular.interactive(), in: .circle)
        .accessibilityLabel(label)
    }
}

/// "See all", "Edit", "Change" – inline text actions in the accent colour, 44 pt tall target.
struct DNTextButton: View {
    var title: String
    var action: () -> Void
    var body: some View {
        Button(title, action: action)
            .dnFont(.callout)
            .fontWeight(.regular)
            .foregroundStyle(DNColor.textLink)
            .frame(minHeight: 44)
            .contentShape(.rect)
            .buttonStyle(.plain)
    }
}
