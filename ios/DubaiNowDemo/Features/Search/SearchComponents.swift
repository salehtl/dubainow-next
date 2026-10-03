import SwiftUI

// Building blocks for the Search screens (Paper p-W-0: Ready, Results as you type,
// Direct answer, Did you mean). Content sits on solid white surfaces; the only glass is
// the system's own search field and tab bar.

// MARK: - Recents (session-local)

/// Recent searches for this session, per resident. Not saved to disk.
@Observable
final class SearchRecents {
    static let shared = SearchRecents()
    static let limit = 6

    /// Sara starts with the searches drawn on the Paper "Ready" screen.
    private var byResident: [String: [String]] = ["sara": ["Salik top up", "Mulkiya", "good conduct", "Ejari"]]

    func items(for resident: String) -> [String] { byResident[resident] ?? [] }

    func add(_ query: String, for resident: String) {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard q.count >= 2 else { return }
        var list = byResident[resident] ?? []
        list.removeAll { $0.compare(q, options: [.caseInsensitive, .diacriticInsensitive]) == .orderedSame }
        list.insert(q, at: 0)
        byResident[resident] = Array(list.prefix(Self.limit))
    }

    func clear(for resident: String) { byResident[resident] = [] }
}

// MARK: - Section header

/// "TOP RESULT", "SERVICES · See all 6": 13 pt semibold overline, 10 pt above its group.
struct SearchSectionHeader: View {
    var title: String
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(title.uppercased())
                .dnFont(.overline)
                .foregroundStyle(DNColor.textSecondary)
                .accessibilityAddTraits(.isHeader)
            Spacer(minLength: 8)
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .dnFont(.subheadline)
                    .fontWeight(.medium)
                    .foregroundStyle(DNColor.textLink)
                    .buttonStyle(.plain)
                    .frame(minHeight: 32)
                    .contentShape(.rect)
            }
        }
        .padding(.leading, 4)
        .frame(minHeight: 20)
    }
}

/// A section: header, then its content 10 pt below.
struct SearchSection<Content: View>: View {
    var title: String
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            SearchSectionHeader(title: title, actionTitle: actionTitle, action: action)
            content
        }
    }
}

// MARK: - Group

/// A white group (radius 24) with hairline separators inset past the leading icon.
struct SearchGroup<Content: View>: View {
    var separatorInset: CGFloat = 64
    @ViewBuilder var content: Content
    @Environment(\.displayScale) private var displayScale

    var body: some View {
        VStack(spacing: 0) {
            Group(subviews: content) { subviews in
                ForEach(Array(subviews.enumerated()), id: \.offset) { index, subview in
                    subview
                    if index < subviews.count - 1 {
                        Rectangle()
                            .fill(DNColor.listRowSeparator)
                            .frame(height: 1 / displayScale)
                            .padding(.leading, separatorInset)
                    }
                }
            }
        }
        .background(DNColor.surfacePrimary, in: .rect(cornerRadius: DNRadius.brief, style: .continuous))
        .clipShape(.rect(cornerRadius: DNRadius.brief, style: .continuous))
    }
}

// MARK: - Highlight

enum SearchHighlight {
    /// Bolds every place a query word starts a word in `text` ("Top up your **Sal**ik account").
    static func text(_ text: String, words: [String]) -> Text {
        var attributed = AttributedString(text)
        let lower = text.lowercased()
        for w in words where w.count >= 2 {
            var searchStart = lower.startIndex
            while let r = lower.range(of: w, options: [.diacriticInsensitive], range: searchStart..<lower.endIndex) {
                let atWordStart = r.lowerBound == lower.startIndex || !lower[lower.index(before: r.lowerBound)].isLetter
                if atWordStart {
                    let offset = lower.distance(from: lower.startIndex, to: r.lowerBound)
                    let length = lower.distance(from: r.lowerBound, to: r.upperBound)
                    let start = attributed.index(attributed.startIndex, offsetByCharacters: offset)
                    let end = attributed.index(start, offsetByCharacters: length)
                    attributed[start..<end].inlinePresentationIntent = .stronglyEmphasized
                }
                searchStart = r.upperBound
            }
        }
        return Text(attributed)
    }
}

// MARK: - Zero state rows

/// A recent search: white capsule with a clock (40 pt).
struct SearchRecentChip: View {
    var text: String
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: "clock.arrow.circlepath")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(DNColor.textSecondary)
                Text(text)
                    .dnFont(.subheadline)
                    .fontWeight(.medium)
                    .foregroundStyle(DNColor.textPrimary)
                    .lineLimit(1)
            }
            .padding(.leading, 12)
            .padding(.trailing, 14)
            .frame(minHeight: DNSpace.chipHeight)
            .background(DNColor.surfacePrimary, in: .capsule)
            .contentShape(.capsule)
        }
        .buttonStyle(.dnPressable)
        .accessibilityLabel(text)
        .accessibilityHint("Searches again")
    }
}

/// A suggested question ("How much do I owe?") with an accent glyph (52 pt).
struct SearchSuggestionRow: View {
    var symbol: String
    var text: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(DNColor.actionAccent)
                .frame(width: 22)
                .accessibilityHidden(true)
            Text(text)
                .dnFont(.callout)
                .foregroundStyle(DNColor.textPrimary)
                .multilineTextAlignment(.leading)
            Spacer(minLength: 8)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(minHeight: 52)
        .contentShape(.rect)
    }
}

/// "Ask DubaiNow": the assistant's face (the logo) and what it can do.
struct SearchAskRow: View {
    var query: String?

    var body: some View {
        HStack(spacing: 12) {
            DNLogo(height: 18)
                .frame(width: 36, height: 36)
                .background(DNColor.surfaceSecondary, in: .circle)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text("Ask DubaiNow")
                    .dnFont(.rowTitle)
                    .foregroundStyle(DNColor.textPrimary)
                Text(query.map { "“\($0)”" } ?? "Say what you need and it does the rest")
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            Image(systemName: "chevron.forward")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(DNColor.listRowChevron)
                .accessibilityHidden(true)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 11)
        .frame(minHeight: 58)
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
        .accessibilityHint("Opens the DubaiNow assistant")
    }
}

/// "Popular in Dubai this week": rank · name · owner (60 pt).
struct SearchPopularRow: View {
    var rank: Int
    var service: CatalogueService

    var body: some View {
        HStack(spacing: 12) {
            Text("\(rank)")
                .font(.system(.headline, design: .rounded, weight: .bold))
                .foregroundStyle(DNColor.textSecondary)
                .frame(minWidth: 22, alignment: .leading)
            VStack(alignment: .leading, spacing: 1) {
                Text(service.name)
                    .dnFont(.rowTitle)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                    .multilineTextAlignment(.leading)
                HStack(spacing: 6) {
                    if service.type != .standalone { ServiceTypeBadge(type: service.type) }
                    Text(service.ownerLabel)
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textSecondary)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 8)
            Image(systemName: "chevron.forward")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(DNColor.listRowChevron)
                .accessibilityHidden(true)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .frame(minHeight: 60)
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(rank). \(service.name), \(service.ownerLabel)")
    }
}

// MARK: - Results rows

/// A catalogue record in results (Paper "Results as you type"): 36 pt disc · name with the
/// match in bold · owner and the resident's context. Bundles and journeys show their badge.
/// (CatalogueRow's `highlight` doesn't render bold yet — see NOTES.md.)
struct SearchServiceRow: View {
    var service: CatalogueService
    var words: [String]
    var showsIcon = true
    var context: String? = nil
    var contextTone: StatusChip.Tone = .neutral

    var body: some View {
        HStack(spacing: 12) {
            if showsIcon {
                switch service.type {
                case .standalone: HueIcon(symbol: service.symbol, hue: service.hue, size: 36)
                case .bundle: BundleDiscs(service: service, size: 36)
                case .journey: HueIcon(symbol: "point.topleft.down.to.point.bottomright.curvepath", hue: service.hue, size: 36)
                }
            }
            VStack(alignment: .leading, spacing: 2) {
                SearchHighlight.text(service.name, words: words)
                    .dnFont(.rowTitle)
                    .fontWeight(.regular)
                    .foregroundStyle(DNColor.textPrimary)
                    .multilineTextAlignment(.leading)
                if service.type != .standalone {
                    HStack(spacing: 6) {
                        ServiceTypeBadge(type: service.type)
                        Text(service.ownerLabel)
                            .dnFont(.footnote)
                            .foregroundStyle(DNColor.textSecondary)
                            .lineLimit(1)
                    }
                } else {
                    meta
                }
            }
            Spacer(minLength: 8)
            Image(systemName: "chevron.forward")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(DNColor.listRowChevron)
                .accessibilityHidden(true)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .frame(minHeight: 58)
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
        .accessibilityHint(service.type == .bundle ? "All at once" : service.type == .journey ? "Start to finish" : "")
    }

    private var meta: some View {
        var parts: [Text] = [Text(service.ownerLabel).foregroundStyle(DNColor.textSecondary)]
        if let status = service.statusLabel {
            parts.append(Text(status).foregroundStyle(service.status == "maintenance" ? DNColor.statusWarningForeground : DNColor.statusInfoForeground))
        }
        if let context {
            parts.append(Text(context).foregroundStyle(SearchTone.color(contextTone)))
        }
        let dot = Text(" · ").foregroundStyle(DNColor.textSecondary)
        let line = parts.dropFirst().reduce(parts[0]) { Text("\($0)\(dot)\($1)") }
        return line
            .dnFont(.footnote)
            .lineLimit(2)
            .multilineTextAlignment(.leading)
    }
}

/// One of the resident's things: plate or hue disc · title (match in bold) · status.
struct SearchThingRow: View {
    var thing: SearchThing
    var words: [String]

    var body: some View {
        HStack(spacing: 12) {
            SearchThingLeading(leading: thing.leading)
            VStack(alignment: .leading, spacing: 1) {
                SearchHighlight.text(thing.title, words: words)
                    .dnFont(.rowTitle)
                    .fontWeight(.regular)
                    .foregroundStyle(DNColor.textPrimary)
                    .multilineTextAlignment(.leading)
                Text(thing.subtitle)
                    .dnFont(.footnote)
                    .fontWeight(thing.tone == .neutral ? .regular : .semibold)
                    .foregroundStyle(SearchTone.color(thing.tone))
                    .multilineTextAlignment(.leading)
            }
            Spacer(minLength: 8)
            Image(systemName: "chevron.forward")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(DNColor.listRowChevron)
                .accessibilityHidden(true)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .frame(minHeight: 58)
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }
}

struct SearchThingLeading: View {
    var leading: SearchThing.Leading

    var body: some View {
        switch leading {
        case .icon(let symbol, let hue):
            HueIcon(symbol: symbol, hue: hue, size: 36)
        case .plate(let plate):
            NumberPlate(code: plate.code, number: plate.number, emirate: plate.emirate, small: true)
                .accessibilityHidden(true)
        case .avatar(let initials, let hue):
            Avatar(initials: initials, hue: hue, size: 36)
        }
    }
}

enum SearchTone {
    static func color(_ tone: StatusChip.Tone) -> Color {
        switch tone {
        case .warning: DNColor.statusWarningForeground
        case .error: DNColor.statusErrorForeground
        case .success: DNColor.statusSuccessForeground
        case .info: DNColor.statusInfoForeground
        case .neutral: DNColor.textSecondary
        }
    }
}

/// A widget the resident can add to Home from search.
struct SearchWidgetRow: View {
    var kind: WidgetKind
    var isOnHome: Bool
    var add: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            HueIcon(symbol: Self.symbol(kind), hue: Self.hue(kind), size: 36)
            VStack(alignment: .leading, spacing: 1) {
                Text("\(kind.title) widget")
                    .dnFont(.rowTitle)
                    .fontWeight(.regular)
                    .foregroundStyle(DNColor.textPrimary)
                Text(isOnHome ? "On your Home" : "Add it to Home")
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
            }
            Spacer(minLength: 8)
            if isOnHome {
                Image(systemName: "checkmark")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(DNColor.statusSuccessForeground)
                    .accessibilityHidden(true)
            } else {
                Button("Add", action: add)
                    .buttonStyle(.dn(.secondary, size: .small, fullWidth: false))
                    .accessibilityLabel("Add the \(kind.title) widget to Home")
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .frame(minHeight: 58)
        .contentShape(.rect)
    }

    static func symbol(_ kind: WidgetKind) -> String {
        switch kind {
        case .parking: "p.square.fill"
        case .salik: "road.lanes"
        case .prayer: "moon.stars.fill"
        case .weather: "sun.max.fill"
        case .metro, .nol: "tram.fill"
        case .energy: "bolt.fill"
        default: "square.grid.2x2.fill"
        }
    }

    static func hue(_ kind: WidgetKind) -> Hue {
        switch kind {
        case .parking, .metro, .nol: .sky
        case .salik, .weather, .energy: .sand
        case .prayer: .lilac
        default: .mint
        }
    }

    static func size(_ kind: WidgetKind) -> WidgetSize {
        switch kind {
        case .metro, .energy: .medium
        default: .small
        }
    }
}

// MARK: - Top result cards

/// Top result for one of the resident's accounts (Paper "Results as you type"):
/// dark disc · "Salik" · "Balance AED 18 · low" · one action.
struct SearchTopThingCard: View {
    var card: SearchThing.Card
    var words: [String]
    var open: () -> Void
    var act: () -> Void
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        let layout = typeSize.isAccessibilitySize ? AnyLayout(VStackLayout(alignment: .leading, spacing: 12)) : AnyLayout(HStackLayout(spacing: 14))
        layout {
            Button(action: open) {
                HStack(spacing: 14) {
                    Image(systemName: card.symbol)
                        .font(.system(size: 21, weight: .semibold))
                        .foregroundStyle(DNColor.widgetOnInverse)
                        .frame(width: 48, height: 48)
                        .background(DNColor.surfaceWidgetInverse, in: .circle)
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 2) {
                        SearchHighlight.text(card.title, words: words)
                            .dnFont(.headline)
                            .fontWeight(.bold)
                            .foregroundStyle(DNColor.textPrimary)
                            .multilineTextAlignment(.leading)
                        valueLine
                    }
                    Spacer(minLength: 0)
                }
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .combine)

            Button(card.button, action: act)
                .buttonStyle(.dn(.secondary, size: .small, fullWidth: false))
        }
        .padding(16)
        .background(DNColor.surfacePrimary, in: .rect(cornerRadius: DNRadius.brief, style: .continuous))
        .dnElevation()
    }

    private var valueLine: some View {
        let label = Text(card.label.isEmpty ? "" : card.label + " ").foregroundStyle(DNColor.textSecondary)
        let value = Text(card.value)
            .font(.system(.subheadline, design: .rounded, weight: .bold))
            .foregroundStyle(card.label.isEmpty ? SearchTone.color(card.noteTone) : DNColor.textPrimary)
        let note = Text(card.note.map { " · \($0)" } ?? "").fontWeight(.semibold).foregroundStyle(SearchTone.color(card.noteTone))
        return Text("\(label)\(value)\(note)")
            .dnFont(.subheadline)
            .multilineTextAlignment(.leading)
    }
}

/// Top result for a service (Paper "Did you mean"): disc · name · "Also called…" and owner
/// chips · the resident's own context · Start now.
struct SearchTopServiceCard: View {
    var match: SearchMatch
    var strip: SearchContextStrip?
    var start: () -> Void

    var body: some View {
        let s = match.service
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 14) {
                Group {
                    switch s.type {
                    case .bundle: BundleDiscs(service: s, size: 48)
                    case .journey: HueIcon(symbol: "point.topleft.down.to.point.bottomright.curvepath", hue: s.hue, size: 48)
                    case .standalone: HueIcon(symbol: s.symbol, hue: s.hue, size: 48)
                    }
                }
                VStack(alignment: .leading, spacing: 6) {
                    Text(s.name)
                        .dnFont(.headline)
                        .fontWeight(.bold)
                        .foregroundStyle(DNColor.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                    SearchFlowLayout(spacing: 6) {
                        if let also = match.alsoCalled {
                            SearchChip(text: "Also called \(also)", fill: DNColor.hueLilacStart, ink: DNColor.hueLilacInk)
                        }
                        if s.type != .standalone { ServiceTypeBadge(type: s.type) }
                        SearchChip(text: s.ownerLabel, fill: DNColor.surfaceSecondary, ink: DNColor.textSecondary)
                        if let status = s.statusLabel {
                            SearchChip(text: status, fill: DNColor.statusInfoSurface, ink: DNColor.statusInfoForeground)
                        }
                    }
                }
            }
            .accessibilityElement(children: .combine)

            if let strip {
                HStack(spacing: 12) {
                    if let plate = strip.plate {
                        NumberPlate(code: plate.code, number: plate.number, emirate: plate.emirate, small: true)
                    } else if let symbol = strip.symbol {
                        Image(systemName: symbol)
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(DNColor.statusWarningForeground)
                            .accessibilityHidden(true)
                    }
                    Text(strip.text)
                        .dnFont(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(DNColor.statusWarningForeground)
                    Spacer(minLength: 0)
                }
                .padding(12)
                .background(DNColor.statusWarningSurface, in: .rect(cornerRadius: 16, style: .continuous))
                .accessibilityElement(children: .combine)
            }

            Button(strip?.buttonTitle ?? Self.startTitle(s), action: start)
                .buttonStyle(.dn(.primary, size: .large))
        }
        .padding(18)
        .background(DNColor.surfacePrimary, in: .rect(cornerRadius: DNRadius.brief, style: .continuous))
        .dnElevation()
    }

    static func startTitle(_ s: CatalogueService) -> String {
        guard s.isLive else { return "Find out more" }
        switch s.type {
        case .standalone: return s.kind == "thing" ? "Open" : "Start now"
        case .bundle: return "See what it covers"
        case .journey: return "See the stages"
        }
    }
}

/// The resident's own context on a service card ("M 48213 · Your Patrol is due in 12 days").
struct SearchContextStrip: Hashable {
    var plate: Plate?
    var symbol: String?
    var text: String
    /// "Continue" when the resident has already started it.
    var buttonTitle: String?
    /// Where the button goes instead of the service ("Track your request").
    var route: Route?
}

struct SearchChip: View {
    var text: String
    var fill: Color
    var ink: Color

    var body: some View {
        Text(text)
            .dnFont(.caption1)
            .fontWeight(.semibold)
            .foregroundStyle(ink)
            .lineLimit(1)
            .padding(.horizontal, 8)
            .frame(minHeight: 24)
            .background(fill, in: .capsule)
    }
}

// MARK: - Correction, footnote, browse

/// "Showing results for **mulkiya** · Search instead for mulkia".
struct SearchCorrectionHeader: View {
    var corrected: String
    var original: String
    var searchOriginal: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Showing results for \(Text(corrected).fontWeight(.bold))")
                .dnFont(.body)
                .foregroundStyle(DNColor.textPrimary)
            HStack(spacing: 0) {
                Text("Search instead for ")
                    .foregroundStyle(DNColor.textSecondary)
                Button(original, action: searchOriginal)
                    .fontWeight(.medium)
                    .foregroundStyle(DNColor.textLink)
                    .buttonStyle(.plain)
                    .accessibilityLabel("Search instead for \(original)")
            }
            .dnFont(.subheadline)
        }
        .padding(.leading, 4)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// "ⓘ Answers are worked out from your linked … accounts. Only you can see them."
struct SearchFootnote: View {
    var text: String

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 10) {
            Image(systemName: "info.circle")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(DNColor.textSecondary)
                .accessibilityHidden(true)
            Text(text)
                .dnFont(.footnote)
                .foregroundStyle(DNColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 4)
    }
}

/// White capsule for "Browse by topic" and "A to Z" (44 pt).
struct SearchCapsuleButton: View {
    var title: String
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .dnFont(.subheadline)
                .fontWeight(.semibold)
                .foregroundStyle(DNColor.textLink)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                .frame(maxWidth: .infinity, minHeight: 44)
                .background(DNColor.surfacePrimary, in: .capsule)
                .contentShape(.capsule)
        }
        .buttonStyle(.dnPressable)
    }
}

// MARK: - Flow layout

/// Wraps children onto new lines (recent chips, service chips).
struct SearchFlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, lineHeight: CGFloat = 0, maxX: CGFloat = 0
        for v in subviews {
            let size = v.sizeThatFits(ProposedViewSize(width: width, height: nil))
            if x > 0, x + size.width > width { x = 0; y += lineHeight + spacing; lineHeight = 0 }
            x += size.width + spacing
            maxX = max(maxX, x - spacing)
            lineHeight = max(lineHeight, size.height)
        }
        return CGSize(width: proposal.width ?? maxX, height: y + lineHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, lineHeight: CGFloat = 0
        for v in subviews {
            let size = v.sizeThatFits(ProposedViewSize(width: bounds.width, height: nil))
            if x > bounds.minX, x + size.width > bounds.maxX { x = bounds.minX; y += lineHeight + spacing; lineHeight = 0 }
            v.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(width: min(size.width, bounds.width), height: size.height))
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }
    }
}
