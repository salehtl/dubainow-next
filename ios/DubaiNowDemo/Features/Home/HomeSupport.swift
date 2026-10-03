import SwiftUI
import UIKit

// MARK: - Grid

/// The Home grid (Paper C1 · Favorites spec): 20 pt page insets, 12 pt gutters.
/// Favorites are four columns, widgets two. Everything stretches with the screen width,
/// so tiles, widgets and cards share the same left and right edges.
struct HomeGrid {
    var width: CGFloat
    var inset: CGFloat = DNSpace.insetPage
    var gutter: CGFloat = DNSpace.widgetGutter

    /// Width of the content column (screen − 2 × 20).
    var content: CGFloat { max(0, (width > 0 ? width : 402) - inset * 2) }
    /// One favorite column: (width − 40 − 36) / 4 (81.5 on a 402 pt screen).
    var tile: CGFloat { max(56, ((content - gutter * 3) / 4).rounded(.down)) }
    /// One small widget: (width − 40 − 12) / 2 (175 on a 402 pt screen).
    var small: CGFloat { max(120, ((content - gutter) / 2).rounded(.down)) }

    func size(of size: WidgetSize) -> CGSize {
        switch size {
        case .small: CGSize(width: small, height: small)
        case .medium: CGSize(width: content, height: small)
        case .large: CGSize(width: content, height: content)
        }
    }
}

// MARK: - Widget rows

/// Widgets packed in order: two smalls side by side, mediums and larges on their own row.
struct HomeWidgetRow: Identifiable {
    var widgets: [HomeWidget]
    var id: String { widgets.map(\.id).joined(separator: "+") }

    static func pack(_ widgets: [HomeWidget]) -> [HomeWidgetRow] {
        var rows: [HomeWidgetRow] = []
        var i = 0
        while i < widgets.count {
            let w = widgets[i]
            if w.size == .small, i + 1 < widgets.count, widgets[i + 1].size == .small {
                rows.append(HomeWidgetRow(widgets: [w, widgets[i + 1]]))
                i += 2
            } else {
                rows.append(HomeWidgetRow(widgets: [w]))
                i += 1
            }
        }
        return rows
    }
}

/// Which sizes each widget offers (C1 · Bundles and journeys on Home: bundles Small/Medium,
/// journeys Medium/Large; standalone widgets offer all three).
enum HomeWidgetSizes {
    /// Mirrors the Widgets feature's `WidgetKind.widgetSizes` (the sizes each kind is designed in).
    static func allowed(for kind: WidgetKind) -> [WidgetSize] { kind.widgetSizes }

    static func label(_ size: WidgetSize) -> String {
        switch size {
        case .small: "Small"
        case .medium: "Wide"
        case .large: "Large"
        }
    }

    static func symbol(_ size: WidgetSize) -> String {
        switch size {
        case .small: "square"
        case .medium: "rectangle"
        case .large: "square.fill"
        }
    }

    static func spoken(_ size: WidgetSize) -> String {
        switch size {
        case .small: "small"
        case .medium: "wide"
        case .large: "large"
        }
    }
}

// MARK: - Store helpers (only use existing stored state)

extension DemoStore {
    /// Restores favorites exactly (Undo, or Cancel in the edit sheet).
    func homeRestoreFavorites(_ favorites: [FavoriteItem]) {
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { state.favorites = favorites }
    }

    /// Restores widgets exactly (Undo after removing one).
    func homeRestoreWidgets(_ widgets: [HomeWidget]) {
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { state.widgets = widgets }
    }

    /// Restores the Needs you order (Undo after Later).
    func homeRestoreNeedsYou(_ items: [NeedsYouItem]) {
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { state.needsYou = items }
    }

    /// Moves one widget a step earlier (-1) or later (+1) — "without dragging".
    func homeMoveWidget(_ id: String, by step: Int) {
        guard let from = state.widgets.firstIndex(where: { $0.id == id }) else { return }
        let to = from + step
        guard state.widgets.indices.contains(to) else { return }
        moveWidgets(from: IndexSet(integer: from), to: step > 0 ? to + 1 : to)
    }

    /// Moves one favorite a step earlier (-1) or later (+1).
    func homeMoveFavorite(_ id: String, by step: Int) {
        guard let from = state.favorites.firstIndex(where: { $0.id == id }) else { return }
        let to = from + step
        guard state.favorites.indices.contains(to) else { return }
        withAnimation(DNMotion.resolve(DNMotion.smooth)) {
            moveFavorites(from: IndexSet(integer: from), to: step > 0 ? to + 1 : to)
        }
    }

    /// Adds a suggested widget in place of the suggestion (top of the canvas) and returns its id.
    @discardableResult
    func homeAddSuggestedWidget(_ kind: WidgetKind, size: WidgetSize) -> String? {
        addWidget(kind, size: size)
        guard let last = state.widgets.last, last.kind == kind else { return nil }
        if state.widgets.count > 1 {
            moveWidgets(from: IndexSet(integer: state.widgets.count - 1), to: 0)
        }
        state.widgetSuggestion = nil
        return last.id
    }

    /// Things in Needs you that belong to the resident's vehicles (fines and renewals).
    var homeVehicleAttentionCount: Int {
        let fineIDs = Set(state.vehicles.flatMap(\.fineIDs))
        let vehicleIDs = Set(state.vehicles.map(\.id))
        return state.needsYou.filter { item in
            switch item.kind {
            case .payment(let p): return fineIDs.contains(p.id)
            case .renewal(_, let thing): return thing.map { vehicleIDs.contains($0) } ?? false
            default: return false
            }
        }.count
    }

    /// Something due today (or overdue) — turns the Needs you line amber.
    var homeDueToday: NeedsYouItem? {
        state.needsYou.first { item in
            guard let due = item.due else { return false }
            return DemoDate.daysFromToday(due) <= 0
        }
    }
}

// MARK: - Copy helpers

enum HomeCopy {
    /// "today", "tomorrow", "in 3 days", "on 14 Oct".
    static func when(_ date: Date) -> String {
        let n = DemoDate.daysFromToday(date)
        switch n {
        case ..<0: return "overdue"
        case 0: return "today"
        case 1: return "tomorrow"
        case 2...6: return "in \(n) days"
        default: return "on \(DemoDate.short(date))"
        }
    }

    /// "Electricity & water bill" → "electricity and water bill" (for a running sentence).
    static func inline(_ title: String) -> String {
        let t = title.replacingOccurrences(of: " & ", with: " and ")
        guard let first = t.first else { return t }
        // Keep acronyms ("DEWA", "RTA") as they are.
        let firstWord = t.split(separator: " ").first.map(String.init) ?? ""
        if firstWord.count > 1, firstWord == firstWord.uppercased() { return t }
        return first.lowercased() + t.dropFirst()
    }
}

// MARK: - Launch arguments (demo and screenshots only)

/// Extra launch arguments handled inside Home, e.g. `-homeEditing`, `-homeSelectWidget 1`,
/// `-homeFolded`, `-homeEmptyNeedsYou`, `-homeScrollBottom`, `-homeLimit`.
enum HomeLaunch {
    static var args: [String] { ProcessInfo.processInfo.arguments }
    static var editing: Bool { args.contains("-homeEditing") }
    static var folded: Bool { args.contains("-homeFolded") }
    static var emptyNeedsYou: Bool { args.contains("-homeEmptyNeedsYou") }
    static var scrollBottom: Bool { args.contains("-homeScrollBottom") }
    static var scrollTarget: String? { UserDefaults.standard.string(forKey: "homeScrollTo") }
    static var standInWidgets: Bool { args.contains("-homeStandInWidgets") }
    static var fillFavorites: Bool { args.contains("-homeLimit") }
    static var selectedWidgetIndex: Int? {
        UserDefaults.standard.object(forKey: "homeSelectWidget") == nil ? nil : UserDefaults.standard.integer(forKey: "homeSelectWidget")
    }
}

// MARK: - Small shared pieces

/// "See all", "Edit": 17 medium link, 44 pt target.
struct HomeLinkButton: View {
    var title: String
    var color: Color = DNColor.textLink
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .dnFont(.body)
                .fontWeight(.medium)
                .foregroundStyle(color)
                .frame(minWidth: 44, minHeight: 44)
                .contentShape(.rect)
        }
        .buttonStyle(.plain)
    }
}

/// The Pay all mark: three overlapping discs (the bundle's stacked circles, white on accent).
struct HomePayAllGlyph: View {
    var color: Color = .white
    var body: some View {
        Canvas { ctx, size in
            let r = size.height * 0.36
            let xs: [CGFloat] = [0.25, 0.5, 0.75]
            let alphas: [Double] = [0.5, 0.78, 1]
            for (x, a) in zip(xs, alphas) {
                let c = CGPoint(x: size.width * x, y: size.height / 2)
                ctx.fill(Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2)), with: .color(color.opacity(a)))
            }
        }
        .frame(width: 20, height: 11)
        .accessibilityHidden(true)
    }
}

/// Small text chip on a hue poster ("Due tomorrow", "Your turn").
struct HomeBriefChip: View {
    enum Tone { case warning, error, neutral }
    var text: String
    var tone: Tone = .neutral

    var body: some View {
        HStack(spacing: 5) {
            if tone != .neutral {
                Circle().fill(color).frame(width: 7, height: 7)
            }
            Text(text)
                .dnFont(.footnote)
                .fontWeight(.semibold)
                .foregroundStyle(color)
                .lineLimit(1)
        }
        .padding(.horizontal, 10)
        .frame(minHeight: 26)
        .background(DNColor.surfacePrimary, in: .capsule)
    }

    private var color: Color {
        switch tone {
        case .warning: DNColor.statusWarningForeground
        case .error: DNColor.statusErrorForeground
        case .neutral: DNColor.textPrimary
        }
    }
}

/// Gentle ±1.5° wobble for widgets in Edit Home. Off with Reduce Motion.
struct HomeWobble: ViewModifier {
    var active: Bool
    var seed: Int
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        if active && !reduceMotion {
            content.phaseAnimator([-1.5, 1.5]) { view, angle in
                view.rotationEffect(.degrees(seed.isMultiple(of: 2) ? angle : -angle))
            } animation: { _ in
                .easeInOut(duration: 0.13 + Double(seed % 3) * 0.012)
            }
        } else {
            content
        }
    }
}

// MARK: - Widget content

/// Draws a widget into the frame Home gives it. In the app this is always `WidgetView`;
/// `-homeStandInWidgets` swaps in simple stand-ins for screenshots while the widget
/// views are being built.
struct HomeWidgetContent: View {
    var widget: HomeWidget

    var body: some View {
        if HomeLaunch.standInWidgets {
            HomeWidgetStandIn(widget: widget)
        } else {
            WidgetView(widget: widget)
        }
    }
}

private struct HomeWidgetStandIn: View {
    var widget: HomeWidget

    private var dark: Bool {
        switch widget.kind {
        case .salik, .metro: true
        default: false
        }
    }

    private var symbol: String {
        switch widget.kind {
        case .parking: "p.square.fill"
        case .salik: "road.lanes"
        case .prayer: "moon.fill"
        case .weather: "sun.max.fill"
        case .metro: "tram.fill"
        case .energy: "bolt.fill"
        case .nol: "creditcard.fill"
        case .billsAndFines: "creditcard"
        case .familyVisas: "person.2.fill"
        case .certificates: "doc.text"
        case .journey: "point.topleft.down.to.point.bottomright.curvepath"
        case .visaTracker: "person.text.rectangle"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Image(systemName: symbol)
                .font(.system(size: 18, weight: .semibold))
                .frame(width: 34, height: 34)
                .background(dark ? Color.white.opacity(0.15) : DNColor.actionAccentSubtle, in: .circle)
            Spacer(minLength: 0)
            Text(widget.kind.title)
                .dnFont(.headline)
            Text(HomeWidgetSizes.label(widget.size))
                .dnFont(.footnote)
                .opacity(0.7)
        }
        .foregroundStyle(dark ? DNColor.widgetOnInverse : DNColor.textPrimary)
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(dark ? DNColor.widgetSurfaceInverse : DNColor.widgetSurface, in: .rect(cornerRadius: DNRadius.widget, style: .continuous))
        .dnElevation()
    }
}
