import SwiftUI

// Shared building blocks for the Home widgets (Paper C1 · 02 Widgets · Anatomy and grid):
// glyph (34 pt city cue) · optional quick action (34 pt circle in a 44 pt target) ·
// hero figure (SF Pro Rounded 44/46) · unit (17/22) · caption (13/18) · optional meter (6 pt).

// MARK: - Environment

extension EnvironmentValues {
    /// Gallery previews (and Home's edit mode) draw the widget without taps, quick actions or its menu.
    @Entry var widgetIsPreview: Bool = false
    /// Home sets this so "Edit Home" in a widget's menu enters edit mode:
    /// `.environment(\.widgetEditHomeAction, WidgetEditHomeAction { isEditing = true })`.
    @Entry var widgetEditHomeAction: WidgetEditHomeAction? = nil
}

/// "Edit Home" from a widget's menu. Wrapped so the environment can compare it.
struct WidgetEditHomeAction: Equatable {
    var run: () -> Void
    func callAsFunction() { run() }
    static func == (lhs: WidgetEditHomeAction, rhs: WidgetEditHomeAction) -> Bool { true }
}

extension Notification.Name {
    /// Posted by "Edit Home" in a widget's menu when Home hasn't set `widgetEditHomeAction`.
    static let widgetEditHomeRequested = Notification.Name("DNWidgetEditHomeRequested")
}

/// The app's launch moment: live values that tick in the demo (metro departures) count from here.
enum WidgetClock {
    static let launch = Date()
}

// MARK: - Surfaces

/// What a widget is painted on. Inverse surfaces are only for things that are dark in the city
/// (Salik gantries, departure boards).
enum WidgetSurface: Equatable {
    case plain
    case inverse
    case board
    case sky
    case hue(Hue)

    var isDark: Bool { self == .inverse || self == .board }

    @ViewBuilder var fill: some View {
        switch self {
        case .plain: DNColor.widgetSurface
        case .inverse: DNColor.widgetSurfaceInverse
        case .board: DNColor.widgetSurfaceBoard
        case .sky: LinearGradient(colors: [DNColor.widgetSkyTop, DNColor.widgetSkyBottom], startPoint: .top, endPoint: .bottom)
        case .hue(let hue): hue.gradient
        }
    }
}

// MARK: - Quick action slot

/// What a quick action does. `WidgetView` maps each to a router call or a store action.
enum WidgetQuickIntent: Hashable {
    case extendParking
    case startParking
    case topUpSalik
    case topUpNol
    case renewFamilyVisas
    case connect(String)   // account name ("Salik", "DEWA")
}

enum WidgetQuickTone {
    case subtle, filled, onInverse, destructive, white

    var fill: Color {
        switch self {
        case .subtle: DNColor.actionAccentSubtle
        case .filled: DNColor.actionAccent
        case .onInverse: DNColor.widgetOnInverseTrack
        case .destructive: DNColor.actionDestructive
        case .white: DNColor.white
        }
    }

    var ink: Color {
        switch self {
        case .subtle: DNColor.textLink
        case .filled, .onInverse, .destructive: DNColor.white
        case .white: DNColor.textPrimary
        }
    }
}

/// How a quick action looks: a + circle, or a capsule with a verb.
struct WidgetQuickLook: Equatable {
    enum Shape { case plus, capsule }
    var shape: Shape
    var title: String = ""
    var symbol: String? = nil
    var tone: WidgetQuickTone = .subtle
    var height: CGFloat = 40
    var fullWidth: Bool = true

    static func plus(_ tone: WidgetQuickTone = .subtle) -> WidgetQuickLook {
        WidgetQuickLook(shape: .plus, tone: tone, height: 44)
    }

    static func capsule(_ title: String, symbol: String? = nil, tone: WidgetQuickTone = .subtle,
                        height: CGFloat = 40, fullWidth: Bool = true) -> WidgetQuickLook {
        WidgetQuickLook(shape: .capsule, title: title, symbol: symbol, tone: tone, height: height, fullWidth: fullWidth)
    }
}

/// Where a quick action sits inside a widget. The widget's content reserves the space; `WidgetView`
/// draws the real button on top, outside the card's own button, so it is its own target for touch
/// and for VoiceOver.
struct WidgetQuickSlot {
    let anchor: Anchor<CGRect>
    let look: WidgetQuickLook
    let intent: WidgetQuickIntent
}

struct WidgetQuickSlotKey: PreferenceKey {
    static let defaultValue: [WidgetQuickSlot] = []
    static func reduce(value: inout [WidgetQuickSlot], nextValue: () -> [WidgetQuickSlot]) {
        value.append(contentsOf: nextValue())
    }
}

/// Reserves the space for a quick action and publishes where it is.
struct WidgetQuickSlotView: View {
    var look: WidgetQuickLook
    var intent: WidgetQuickIntent

    var body: some View {
        Group {
            switch look.shape {
            case .plus:
                Color.clear.frame(width: 44, height: 44)
            case .capsule:
                if look.fullWidth {
                    Color.clear.frame(maxWidth: .infinity).frame(height: look.height)
                } else {
                    WidgetQuickVisual(look: look).hidden()
                }
            }
        }
        .anchorPreference(key: WidgetQuickSlotKey.self, value: .bounds) {
            [WidgetQuickSlot(anchor: $0, look: look, intent: intent)]
        }
        .accessibilityHidden(true)
    }
}

/// The drawn quick action (used by the real button and to size a slot).
struct WidgetQuickVisual: View {
    var look: WidgetQuickLook
    var pressed: Bool = false

    var body: some View {
        switch look.shape {
        case .plus:
            Circle()
                .fill(look.tone.fill)
                .overlay(
                    Image(systemName: "plus")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(look.tone.ink)
                )
                .frame(width: 34, height: 34)
                .scaleEffect(pressed ? 0.9 : 1)
                .frame(width: 44, height: 44)
        case .capsule:
            HStack(spacing: 6) {
                if let symbol = look.symbol {
                    Image(systemName: symbol).font(.system(size: 13, weight: .bold))
                }
                Text(look.title)
                    .font(.system(size: look.height < 34 ? 13 : 15, weight: .semibold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .foregroundStyle(look.tone.ink)
            .padding(.horizontal, look.height < 34 ? 12 : 14)
            .frame(maxWidth: look.fullWidth ? .infinity : nil)
            .frame(height: look.height)
            .background(look.tone.fill.opacity(pressed ? 0.8 : 1), in: .capsule)
            .scaleEffect(pressed ? 0.96 : 1)
        }
    }
}

/// Reports a button's pressed state so the whole widget (card and quick action) can scale together.
struct WidgetPressReportingStyle: ButtonStyle {
    @Binding var isPressed: Bool

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .contentShape(.rect)
            .onChange(of: configuration.isPressed) { _, new in isPressed = new }
    }
}

/// The quick action button's own press feedback.
struct WidgetQuickButtonStyle: ButtonStyle {
    var look: WidgetQuickLook

    func makeBody(configuration: Configuration) -> some View {
        WidgetQuickVisual(look: look, pressed: configuration.isPressed && !UIAccessibility.isReduceMotionEnabled)
            .contentShape(.rect)
            .animation(DNMotion.snappy, value: configuration.isPressed)
    }
}

// MARK: - Figures and captions

/// The hero figure with its unit. Currency leads ("AED 18"), time trails ("42 min").
/// Changed digits roll (`.numericText`).
struct WidgetFigure: View {
    struct Part: Hashable {
        var value: String
        var unit: String?
    }

    var parts: [Part]
    var leadingUnit: String? = nil
    var color: Color = DNColor.textPrimary
    var unitColor: Color? = nil
    var size: CGFloat = 44
    /// Drives the roll direction (down for decreases, up for increases).
    var numeric: Double = 0

    init(_ value: String, unit: String? = nil, leadingUnit: String? = nil, color: Color = DNColor.textPrimary,
         unitColor: Color? = nil, size: CGFloat = 44, numeric: Double = 0) {
        self.parts = [Part(value: value, unit: unit)]
        self.leadingUnit = leadingUnit
        self.color = color
        self.unitColor = unitColor
        self.size = size
        self.numeric = numeric
    }

    init(parts: [Part], color: Color = DNColor.textPrimary, size: CGFloat = 44, numeric: Double = 0) {
        self.parts = parts
        self.color = color
        self.size = size
        self.numeric = numeric
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 4) {
            if let leadingUnit {
                Text(leadingUnit)
                    .font(.system(size: 17, weight: .semibold, design: .rounded))
                    .foregroundStyle(unitColor ?? color)
                    .padding(.trailing, 1)
            }
            ForEach(Array(parts.enumerated()), id: \.offset) { _, part in
                Text(part.value)
                    .font(.system(size: size, weight: .bold, design: .rounded))
                    .tracking(size >= 40 ? -0.8 : -0.3)
                    .monospacedDigit()
                    .foregroundStyle(color)
                    .contentTransition(.numericText(value: numeric))
                if let unit = part.unit {
                    Text(unit)
                        .font(.system(size: 17, weight: .semibold, design: .rounded))
                        .foregroundStyle(unitColor ?? color)
                        .padding(.trailing, 2)
                }
            }
        }
        .lineLimit(1)
        .minimumScaleFactor(0.6)
        .animation(DNMotion.resolve(DNMotion.smooth), value: numeric)
    }
}

/// 13/18 caption under the figure.
struct WidgetCaption: View {
    var text: String
    var color: Color = DNColor.textSecondary
    var weight: Font.Weight = .regular
    var lines: Int = 1

    init(_ text: String, color: Color = DNColor.textSecondary, weight: Font.Weight = .regular, lines: Int = 1) {
        self.text = text
        self.color = color
        self.weight = weight
        self.lines = lines
    }

    var body: some View {
        Text(text)
            .font(.system(size: 13, weight: weight))
            .lineSpacing(1)
            .foregroundStyle(color)
            .lineLimit(lines)
            .minimumScaleFactor(0.85)
            .fixedSize(horizontal: false, vertical: true)
    }
}

/// 15/20 semibold title used in widget headers.
struct WidgetTitle: View {
    var text: String
    var color: Color = DNColor.textPrimary

    init(_ text: String, color: Color = DNColor.textPrimary) {
        self.text = text
        self.color = color
    }

    var body: some View {
        Text(text)
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(color)
            .lineLimit(1)
            .minimumScaleFactor(0.8)
    }
}

/// Small status pill inside widgets ("Next due tomorrow", "Safe to swim").
struct WidgetChip: View {
    var text: String
    var symbol: String? = nil
    var dot: Color? = nil
    var foreground: Color
    var background: Color
    var height: CGFloat = 24

    var body: some View {
        HStack(spacing: 5) {
            if let dot {
                Circle().fill(dot).frame(width: 6, height: 6)
            }
            if let symbol {
                Image(systemName: symbol).font(.system(size: 11, weight: .bold))
            }
            Text(text)
                .font(.system(size: height > 24 ? 13 : 12, weight: .semibold))
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .foregroundStyle(foreground)
        .padding(.horizontal, 8)
        .frame(height: height)
        .background(background, in: .capsule)
    }
}

/// 6 pt meter: neutral track, coloured fill. The caption always states the level too.
struct WidgetMeter: View {
    var fraction: Double
    var color: Color
    var track: Color = DNColor.neutral200

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(track)
                Capsule()
                    .fill(color)
                    .frame(width: max(fraction > 0 ? 6 : 0, proxy.size.width * min(max(fraction, 0), 1)))
            }
        }
        .frame(height: 6)
        .animation(UIAccessibility.isReduceMotionEnabled ? DNMotion.base : .easeOut(duration: 0.6), value: fraction)
        .animation(.easeInOut(duration: 0.4), value: color)
        .accessibilityHidden(true)
    }
}

/// A segmented stages bar where the current stage is half filled.
struct WidgetStagesBar: View {
    var total: Int
    var current: Int          // 0-based index of the current stage
    var ink: Color
    var track: Color

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<max(total, 1), id: \.self) { i in
                Capsule()
                    .fill(i < current ? ink : track)
                    .overlay(alignment: .leading) {
                        if i == current {
                            GeometryReader { p in Capsule().fill(ink).frame(width: p.size.width / 2) }
                        }
                    }
                    .frame(height: 6)
            }
        }
        .accessibilityHidden(true)
    }
}

// MARK: - Glyphs (the city cues)

/// The parking sign: white P on Parkin blue with a white inset rule.
struct WidgetParkingSign: View {
    var size: CGFloat = 34
    var dimmed: Bool = false

    var body: some View {
        RoundedRectangle(cornerRadius: size * 0.26, style: .continuous)
            .fill(dimmed ? DNColor.neutral200 : DNColor.signParking)
            .overlay(
                RoundedRectangle(cornerRadius: size * 0.26 - 1, style: .continuous)
                    .strokeBorder(dimmed ? .clear : DNColor.white, lineWidth: 2)
                    .padding(1)
            )
            .overlay(
                Text("P")
                    .font(.system(size: size * 0.65, weight: .heavy, design: .rounded))
                    .foregroundStyle(dimmed ? DNColor.neutral500 : DNColor.white)
            )
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }
}

/// A round glyph: an SF Symbol on a tinted disc.
struct WidgetGlyph: View {
    var symbol: String
    var ink: Color
    var fill: AnyShapeStyle
    var size: CGFloat = 34

    init(symbol: String, ink: Color, fill: some ShapeStyle, size: CGFloat = 34) {
        self.symbol = symbol
        self.ink = ink
        self.fill = AnyShapeStyle(fill)
        self.size = size
    }

    var body: some View {
        Circle()
            .fill(fill)
            .overlay(
                Image(systemName: symbol)
                    .font(.system(size: size * 0.48, weight: .semibold))
                    .foregroundStyle(ink)
            )
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }
}

/// The bundle mark: three overlapping accent circles on a subtle disc.
struct WidgetBundleMark: View {
    var size: CGFloat = 34
    var badge: Bool = false

    var body: some View {
        let dots = HStack(spacing: -3.2) {
            ForEach(0..<3, id: \.self) { _ in
                Circle().fill(DNColor.actionAccent).frame(width: 7.2, height: 7.2)
            }
        }
        Group {
            if badge {
                dots.padding(.horizontal, 6).frame(height: 22).background(DNColor.actionAccentSubtle, in: .capsule)
            } else {
                Circle().fill(DNColor.actionAccentSubtle).overlay(dots).frame(width: size, height: size)
            }
        }
        .accessibilityHidden(true)
    }
}

/// The nol card glyph: a small silver card with a red dot.
struct WidgetNolCard: View {
    var body: some View {
        RoundedRectangle(cornerRadius: 6, style: .continuous)
            .fill(LinearGradient(colors: [DNColor.neutral50, DNColor.neutral300], startPoint: .topLeading, endPoint: .bottomTrailing))
            .overlay(
                RoundedRectangle(cornerRadius: 6, style: .continuous).strokeBorder(DNColor.plateRule, lineWidth: 0.5)
            )
            .overlay(alignment: .bottom) {
                HStack(alignment: .bottom) {
                    Text("nol")
                        .font(.system(size: 11, weight: .heavy))
                        .tracking(-0.2)
                        .foregroundStyle(DNColor.textPrimary)
                    Spacer(minLength: 0)
                    Circle().fill(DNColor.signMetroRed).frame(width: 7, height: 7)
                }
                .padding(.horizontal, 5)
                .padding(.bottom, 4)
            }
            .frame(width: 44, height: 30)
            .shadow(color: DNColor.shadowLight, radius: 1, y: 1)
            .accessibilityHidden(true)
    }
}

// MARK: - Calm states

/// "Not connected" / "Not parked" / nothing-live layout: glyph · title + line · action (optional).
struct WidgetCalmState<Glyph: View>: View {
    var title: String
    var message: String
    var onDark: Bool = false
    var quick: (look: WidgetQuickLook, intent: WidgetQuickIntent)? = nil
    @ViewBuilder var glyph: Glyph

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            glyph.frame(height: 44, alignment: .leading)
            Spacer(minLength: 6)
            VStack(alignment: .leading, spacing: 2) {
                WidgetTitle(title, color: onDark ? DNColor.widgetOnInverse : DNColor.textPrimary)
                WidgetCaption(message, color: onDark ? DNColor.widgetOnInverseSecondary : DNColor.textSecondary, lines: 3)
            }
            Spacer(minLength: 8)
            if let quick {
                WidgetQuickSlotView(look: quick.look, intent: quick.intent)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(EdgeInsets(top: 12, leading: 16, bottom: 16, trailing: 16))
    }
}

// MARK: - Formatting

enum WidgetFormat {
    /// "12:12" style without AM/PM, as the prayer widget shows it ("3:35").
    static func clock(_ date: Date) -> String {
        let c = Calendar.current.dateComponents([.hour, .minute], from: date)
        let h = (c.hour ?? 0) % 12 == 0 ? 12 : (c.hour ?? 0) % 12
        return String(format: "%d:%02d", h, c.minute ?? 0)
    }

    static func clock(hour: Int, minute: Int) -> String {
        let h = hour % 12 == 0 ? 12 : hour % 12
        return String(format: "%d:%02d", h, minute)
    }

    /// "2 hours 31 minutes" for VoiceOver.
    static func spokenDuration(minutes: Int) -> String {
        let h = minutes / 60, m = minutes % 60
        var parts: [String] = []
        if h > 0 { parts.append(h == 1 ? "1 hour" : "\(h) hours") }
        if m > 0 || h == 0 { parts.append(m == 1 ? "1 minute" : "\(m) minutes") }
        return parts.joined(separator: " ")
    }

    /// "Aug" from "September".
    static func previousMonthShort(of monthName: String) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_GB")
        let months = f.monthSymbols ?? []
        let short = f.shortMonthSymbols ?? []
        guard let i = months.firstIndex(of: monthName), !short.isEmpty else { return "last month" }
        return short[(i + 11) % 12]
    }

    /// "Electricity & water bill" → "electricity and water bill".
    static func lowerFirst(_ s: String) -> String {
        let t = s.replacingOccurrences(of: " & ", with: " and ")
        guard let first = t.first else { return t }
        return first.lowercased() + t.dropFirst()
    }
}
