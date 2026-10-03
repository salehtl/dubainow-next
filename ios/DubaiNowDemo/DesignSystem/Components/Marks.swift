import SwiftUI

// MARK: - Hue icon (disc) and hue tile (squircle)

/// A gradient disc with a glyph in the hue's ink. Sizes: 56 (feature), 44 (topic tile),
/// 40 (list rows, search results), 32 (poster badges).
struct HueIcon: View {
    var symbol: String
    var hue: Hue
    var size: CGFloat = 40

    var body: some View {
        Circle()
            .fill(hue.gradient)
            .overlay(
                Image(systemName: symbol)
                    .font(.system(size: size * 0.42, weight: .semibold))
                    .foregroundStyle(hue.ink)
            )
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }
}

/// The favorite tile squircle (r20) with gradient, top highlight and widget shadow.
struct HueTile: View {
    var symbol: String
    var hue: Hue
    var size: CGFloat = DNSize.favoriteTile

    var body: some View {
        RoundedRectangle(cornerRadius: DNRadius.tile, style: .continuous)
            .fill(hue.gradient)
            .overlay(
                RoundedRectangle(cornerRadius: DNRadius.tile, style: .continuous)
                    .strokeBorder(
                        LinearGradient(colors: [DNColor.tileHighlight, .clear], startPoint: .top, endPoint: .center),
                        lineWidth: 1
                    )
            )
            .overlay(
                Image(systemName: symbol)
                    .font(.system(size: size * 0.36, weight: .semibold))
                    .foregroundStyle(hue.ink)
            )
            .frame(width: size, height: size)
            .dnElevation()
            .accessibilityHidden(true)
    }
}

// MARK: - Section header

/// "Needs you", "Favorites", "Your widgets": 22/28 bold with an optional trailing action.
/// 12 pt below the header, 32 pt between sections.
struct SectionHeader<Accessory: View>: View {
    var title: String
    var subtitle: String? = nil
    @ViewBuilder var accessory: Accessory

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .dnFont(.section)
                    .foregroundStyle(DNColor.textPrimary)
                    .accessibilityAddTraits(.isHeader)
                if let subtitle {
                    Text(subtitle)
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                }
            }
            Spacer(minLength: 8)
            accessory
        }
        .frame(minHeight: 44)
    }
}

extension SectionHeader where Accessory == EmptyView {
    init(title: String, subtitle: String? = nil) {
        self.init(title: title, subtitle: subtitle) { EmptyView() }
    }
}

// MARK: - Status chip

/// A small status pill with a dot: "Due tomorrow", "In review", "Approved".
struct StatusChip: View {
    enum Tone { case warning, info, success, error, neutral }
    var text: String
    var tone: Tone = .neutral
    /// White background for use on posters/hues; tinted surface for use on white.
    var onWhite: Bool = false

    var body: some View {
        HStack(spacing: 5) {
            Circle().fill(foreground).frame(width: 7, height: 7)
            Text(text)
                .dnFont(.footnote)
                .fontWeight(.semibold)
                .foregroundStyle(foreground)
                .lineLimit(1)
        }
        .padding(.horizontal, 10)
        .frame(height: 26)
        .background(onWhite ? surface : Color.white, in: .capsule)
    }

    private var foreground: Color {
        switch tone {
        case .warning: DNColor.statusWarningForeground
        case .info: DNColor.statusInfoForeground
        case .success: DNColor.statusSuccessForeground
        case .error: DNColor.statusErrorForeground
        case .neutral: DNColor.textSecondary
        }
    }

    private var surface: Color {
        switch tone {
        case .warning: DNColor.statusWarningSurface
        case .info: DNColor.statusInfoSurface
        case .success: DNColor.statusSuccessSurface
        case .error: DNColor.statusErrorSurface
        case .neutral: DNColor.surfaceSecondary
        }
    }
}

// MARK: - Number plate

/// Dubai number plate. Always laid out left to right, even inside Arabic.
struct NumberPlate: View {
    var code: String
    var number: String
    var emirate: String = "DUBAI"
    var small: Bool = false

    var body: some View {
        HStack(spacing: small ? 6 : 8) {
            VStack(spacing: 0) {
                Text(emirate)
                    .font(.system(size: small ? 5 : 6.5, weight: .heavy))
                    .tracking(0.3)
                Text(code)
                    .font(.system(size: small ? 11 : 15, weight: .heavy))
            }
            .foregroundStyle(DNColor.plateInk)
            Rectangle().fill(DNColor.plateRule).frame(width: 1)
                .padding(.vertical, small ? 3 : 4)
            Text(number)
                .font(.system(size: small ? 15 : 21, weight: .heavy, design: .default).monospacedDigit())
                .foregroundStyle(DNColor.plateInk)
        }
        .padding(.horizontal, small ? 7 : 9)
        .frame(height: small ? 26 : 34)
        .background(DNColor.plateSurface, in: .rect(cornerRadius: small ? 4 : 5, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: small ? 4 : 5, style: .continuous)
                .strokeBorder(DNColor.plateEdge, lineWidth: 1.5)
        )
        .environment(\.layoutDirection, .leftToRight)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Plate \(emirate.capitalized) \(code) \(number)")
    }
}

// MARK: - Money

/// "AED 412.60" with a small unit and a large rounded figure.
struct MoneyText: View {
    var amount: Decimal
    var size: DNTextStyle = .numericLarge
    var unit: DNTextStyle = .numericUnit
    var color: Color = DNColor.textPrimary

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 5) {
            Text("AED").dnFont(unit)
            Text(Money.figure(amount)).dnFont(size)
        }
        .foregroundStyle(color)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Money.spoken(amount))
    }
}

// MARK: - Progress

/// Segmented steps bar (e.g. 3 of 5): done segments solid, the current one in accent.
struct SegmentedProgress: View {
    var total: Int
    var done: Int
    var currentIsActive: Bool = true
    var doneColor: Color = DNColor.statusSuccessForeground
    var currentColor: Color = DNColor.actionAccent
    var trackColor: Color = DNColor.neutral200
    var height: CGFloat = 5

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<max(total, 1), id: \.self) { i in
                Capsule()
                    .fill(i < done ? doneColor : (i == done && currentIsActive ? currentColor : trackColor))
                    .frame(height: height)
            }
        }
        .accessibilityElement()
        .accessibilityLabel("Step \(min(done + 1, total)) of \(total)")
    }
}

// MARK: - Service type marks

/// Bundle mark: three stacked circles ("All at once").
struct StackedCirclesMark: View {
    var color: Color = DNColor.actionAccent
    var size: CGFloat = 20
    var body: some View {
        HStack(spacing: -size * 0.32) {
            ForEach(0..<3, id: \.self) { i in
                Circle()
                    .fill(color.opacity(1 - Double(i) * 0.25))
                    .overlay(Circle().strokeBorder(.white, lineWidth: size * 0.08))
                    .frame(width: size * 0.62, height: size * 0.62)
            }
        }
        .frame(height: size)
        .accessibilityHidden(true)
    }
}

/// Journey mark: a dotted route between two points ("Start to finish").
struct RouteMark: View {
    var color: Color = DNColor.textPrimary
    var size: CGFloat = 20
    var body: some View {
        Image(systemName: "point.topleft.down.to.point.bottomright.curvepath")
            .font(.system(size: size * 0.8, weight: .semibold))
            .foregroundStyle(color)
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }
}

/// "All at once" / "Start to finish" label for bundles and journeys. Standalone services have no badge.
struct ServiceTypeBadge: View {
    var type: CatalogueService.ServiceType
    var onHue: Bool = false

    var body: some View {
        switch type {
        case .bundle:
            HStack(spacing: 6) {
                StackedCirclesMark(size: 16)
                Text("All at once").dnFont(.caption1).fontWeight(.semibold)
            }
            .foregroundStyle(DNColor.textLink)
            .padding(.horizontal, 8)
            .frame(height: 24)
            .background(onHue ? DNColor.chipOnHue : DNColor.actionAccentSubtle, in: .capsule)
        case .journey:
            HStack(spacing: 5) {
                RouteMark(color: DNColor.textPrimary, size: 14)
                Text("Start to finish").dnFont(.caption1).fontWeight(.semibold)
            }
            .foregroundStyle(DNColor.textPrimary)
            .padding(.horizontal, 8)
            .frame(height: 24)
            .background(onHue ? DNColor.chipOnHue : DNColor.surfaceSecondary, in: .capsule)
        case .standalone:
            EmptyView()
        }
    }
}

// MARK: - Happiness faces

/// The three Happiness Meter faces. Drawn (not SF Symbols) because SF Symbols has no
/// neutral or unhappy face. Behaves as one radio group with spoken labels.
enum HappinessFace: String, CaseIterable, Hashable, Codable {
    case happy, neutral, unhappy

    var label: String {
        switch self { case .happy: "Happy"; case .neutral: "Neutral"; case .unhappy: "Unhappy" }
    }

    var ink: Color {
        switch self { case .happy: DNColor.hueMintInk; case .neutral: DNColor.hueSandInk; case .unhappy: DNColor.hueRoseInk }
    }

    var fill: Color {
        switch self { case .happy: DNColor.hueMintStart; case .neutral: DNColor.hueSandStart; case .unhappy: DNColor.hueRoseStart }
    }
}

struct FaceGlyph: View {
    var face: HappinessFace
    var size: CGFloat = 32
    var color: Color? = nil

    var body: some View {
        Canvas { ctx, sz in
            let w = sz.width, ink = color ?? face.ink
            let line = w * 0.075
            ctx.stroke(Path(ellipseIn: CGRect(x: line / 2, y: line / 2, width: w - line, height: w - line)), with: .color(ink), lineWidth: line)
            let eyeR = w * 0.06
            for x in [w * 0.36, w * 0.64] {
                ctx.fill(Path(ellipseIn: CGRect(x: x - eyeR, y: w * 0.38 - eyeR, width: eyeR * 2, height: eyeR * 2)), with: .color(ink))
            }
            var mouth = Path()
            let y = w * 0.64
            switch face {
            case .happy:
                mouth.move(to: CGPoint(x: w * 0.32, y: y - w * 0.03))
                mouth.addQuadCurve(to: CGPoint(x: w * 0.68, y: y - w * 0.03), control: CGPoint(x: w * 0.5, y: y + w * 0.14))
            case .neutral:
                mouth.move(to: CGPoint(x: w * 0.34, y: y))
                mouth.addLine(to: CGPoint(x: w * 0.66, y: y))
            case .unhappy:
                mouth.move(to: CGPoint(x: w * 0.32, y: y + w * 0.07))
                mouth.addQuadCurve(to: CGPoint(x: w * 0.68, y: y + w * 0.07), control: CGPoint(x: w * 0.5, y: y - w * 0.09))
            }
            ctx.stroke(mouth, with: .color(ink), style: StrokeStyle(lineWidth: line, lineCap: .round))
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

/// Three faces in a row. One tap rates; the chosen face pops (bouncy) and the others dim.
struct HappinessFacesPicker: View {
    @Binding var selection: HappinessFace?
    var size: CGFloat = 56
    var showsLabels: Bool = true

    var body: some View {
        HStack(spacing: 0) {
            ForEach(HappinessFace.allCases, id: \.self) { face in
                Button {
                    DNHaptics.selection()
                    withAnimation(DNMotion.resolve(DNMotion.bouncy)) { selection = face }
                } label: {
                    VStack(spacing: 8) {
                        Circle()
                            .fill(face.fill)
                            .overlay(FaceGlyph(face: face, size: size * 0.55))
                            .overlay(Circle().strokeBorder(face.ink, lineWidth: selection == face ? 2.5 : 0))
                            .frame(width: size, height: size)
                            .scaleEffect(selection == face ? 1.12 : 1)
                        if showsLabels {
                            Text(face.label)
                                .dnFont(.footnote)
                                .fontWeight(.semibold)
                                .foregroundStyle(DNColor.textPrimary)
                        }
                    }
                    .opacity(selection == nil || selection == face ? 1 : 0.45)
                    .frame(maxWidth: .infinity)
                    .contentShape(.rect)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(face.label)
                .accessibilityAddTraits(selection == face ? [.isSelected, .isButton] : .isButton)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("How do you feel about it?")
    }
}

// MARK: - Logo and avatar

/// The DubaiNow mark (vector asset). The assistant's face.
struct DNLogo: View {
    var height: CGFloat = 30
    var body: some View {
        Image("DubaiNowMark")
            .resizable()
            .scaledToFit()
            .frame(height: height)
            .accessibilityLabel("DubaiNow")
    }
}

/// Monogram avatar on a hue gradient (personas don't have photos in the demo).
struct Avatar: View {
    var initials: String
    var hue: Hue = .peach
    var size: CGFloat = 36

    var body: some View {
        Circle()
            .fill(LinearGradient(colors: [hue.end, Hue.lilac.end], startPoint: .topLeading, endPoint: .bottomTrailing))
            .overlay(
                Text(initials)
                    .font(.system(size: size * 0.42, weight: .semibold, design: .rounded))
                    .foregroundStyle(hue.ink)
            )
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }
}
