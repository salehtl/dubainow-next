import SwiftUI

/// Text styles from the Paper type scale, set in SF Pro (and SF Pro Rounded for numbers).
/// Sizes are the Paper sizes at the default text size; they scale with Dynamic Type
/// relative to the matching iOS text style.
///
/// Use `.dnFont(.title2)` on views. For `Text` concatenation use `DNTextStyle.title2.font`.
enum DNTextStyle {
    case largeTitle, title1, title2, title3, headline, body, callout, subheadline, footnote, caption1, caption2
    /// Section headings on Home and lists ("Needs you", "Favorites") – 22/28 bold.
    case section
    /// Buttons – 17/24 semibold.
    case button
    /// Small caps-style overline ("FROM YOUR ACCOUNTS") – 13 semibold, tracked.
    case overline
    /// List row title – 16/22 medium. Supporting line uses `.footnote`.
    case rowTitle
    /// Numbers (SF Pro Rounded): unit 17, medium 28, large 34, hero 44.
    case numericUnit, numericMedium, numericLarge, numericHero

    struct Spec {
        let size: CGFloat
        let weight: Font.Weight
        let leading: CGFloat
        let tracking: CGFloat
        let design: Font.Design
        let relativeTo: Font.TextStyle
    }

    var spec: Spec {
        switch self {
        case .largeTitle:   Spec(size: 34, weight: .bold, leading: 40, tracking: -0.8, design: .default, relativeTo: .largeTitle)
        case .title1:       Spec(size: 28, weight: .bold, leading: 34, tracking: -0.55, design: .default, relativeTo: .title)
        case .title2:       Spec(size: 22, weight: .semibold, leading: 28, tracking: -0.35, design: .default, relativeTo: .title2)
        case .section:      Spec(size: 22, weight: .bold, leading: 28, tracking: -0.35, design: .default, relativeTo: .title2)
        case .title3:       Spec(size: 20, weight: .semibold, leading: 26, tracking: -0.25, design: .default, relativeTo: .title3)
        case .headline:     Spec(size: 17, weight: .semibold, leading: 24, tracking: -0.15, design: .default, relativeTo: .headline)
        case .button:       Spec(size: 17, weight: .semibold, leading: 24, tracking: -0.15, design: .default, relativeTo: .headline)
        case .body:         Spec(size: 17, weight: .regular, leading: 24, tracking: -0.15, design: .default, relativeTo: .body)
        case .callout:      Spec(size: 16, weight: .regular, leading: 22, tracking: -0.1, design: .default, relativeTo: .callout)
        case .rowTitle:     Spec(size: 16, weight: .medium, leading: 22, tracking: -0.1, design: .default, relativeTo: .callout)
        case .subheadline:  Spec(size: 15, weight: .regular, leading: 20, tracking: 0, design: .default, relativeTo: .subheadline)
        case .footnote:     Spec(size: 13, weight: .regular, leading: 18, tracking: 0, design: .default, relativeTo: .footnote)
        case .overline:     Spec(size: 13, weight: .semibold, leading: 18, tracking: 0.8, design: .default, relativeTo: .footnote)
        case .caption1:     Spec(size: 12, weight: .medium, leading: 16, tracking: 0, design: .default, relativeTo: .caption)
        case .caption2:     Spec(size: 11, weight: .medium, leading: 16, tracking: 0, design: .default, relativeTo: .caption2)
        case .numericUnit:  Spec(size: 17, weight: .semibold, leading: 22, tracking: 0, design: .rounded, relativeTo: .headline)
        case .numericMedium: Spec(size: 28, weight: .bold, leading: 32, tracking: -0.3, design: .rounded, relativeTo: .title)
        case .numericLarge: Spec(size: 34, weight: .bold, leading: 40, tracking: -0.5, design: .rounded, relativeTo: .largeTitle)
        case .numericHero:  Spec(size: 44, weight: .bold, leading: 46, tracking: -0.8, design: .rounded, relativeTo: .largeTitle)
        }
    }

    /// A font scaled for the current content size (for `Text` concatenation and places
    /// where a view modifier can't be used). Prefer `.dnFont(_:)` on views.
    var font: Font {
        let s = spec
        let scaled = UIFontMetrics(forTextStyle: s.relativeTo.uiTextStyle).scaledValue(for: s.size)
        return .system(size: scaled, weight: s.weight, design: s.design)
    }
}

private struct DNFontModifier: ViewModifier {
    let style: DNTextStyle
    @ScaledMetric private var size: CGFloat
    @ScaledMetric private var leading: CGFloat

    init(style: DNTextStyle) {
        self.style = style
        let s = style.spec
        _size = ScaledMetric(wrappedValue: s.size, relativeTo: s.relativeTo)
        _leading = ScaledMetric(wrappedValue: s.leading, relativeTo: s.relativeTo)
    }

    func body(content: Content) -> some View {
        let s = style.spec
        // SF Pro's natural line height is ~1.19× the size; Paper sets taller leading.
        let extra = max(0, leading - size * 1.19)
        content
            .font(.system(size: size, weight: s.weight, design: s.design))
            .tracking(s.tracking)
            .lineSpacing(extra)
    }
}

extension View {
    /// Applies a DubaiNow text style (SF Pro at Paper sizes, scaled with Dynamic Type).
    func dnFont(_ style: DNTextStyle) -> some View {
        modifier(DNFontModifier(style: style))
    }
}

extension Font.TextStyle {
    var uiTextStyle: UIFont.TextStyle {
        switch self {
        case .largeTitle: .largeTitle
        case .title: .title1
        case .title2: .title2
        case .title3: .title3
        case .headline: .headline
        case .subheadline: .subheadline
        case .body: .body
        case .callout: .callout
        case .footnote: .footnote
        case .caption: .caption1
        case .caption2: .caption2
        default: .body
        }
    }
}
