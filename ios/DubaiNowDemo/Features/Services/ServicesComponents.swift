import SwiftUI

// MARK: - Backgrounds

/// The hue wash behind the top of a topic or service page (Paper: hue start → clear over 360 pt).
struct ServicesHueWash: View {
    var color: Color
    var base: Color = DNColor.surfaceSecondary
    var height: CGFloat = 360

    var body: some View {
        ZStack(alignment: .top) {
            base
            LinearGradient(colors: [color, color.opacity(0)], startPoint: .top, endPoint: .bottom)
                .frame(height: height)
        }
        .ignoresSafeArea()
        .accessibilityHidden(true)
    }
}

/// A hue disc with a white ring (page headers: 56–64 pt).
struct ServicesHeaderDisc: View {
    var symbol: String
    var hue: Hue
    var size: CGFloat = 64

    var body: some View {
        HueIcon(symbol: symbol, hue: hue, size: size)
            .padding(3)
            .background(DNColor.surfacePrimary, in: .circle)
            .dnElevation()
    }
}

// MARK: - Tiles and cards

/// Browse by topic tile: white, r24, hue disc, name, real service count (Paper DBS-0).
struct ServicesTopicTile: View {
    var topic: CatalogueTopic
    var compact: Bool = true

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HueIcon(symbol: topic.symbol, hue: topic.hue, size: 44)
            Spacer(minLength: 0)
            VStack(alignment: .leading, spacing: 2) {
                Text(topic.name)
                    .dnFont(.rowTitle)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
                Text(ServicesText.count(topic.services, "service"))
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, minHeight: compact ? 148 : nil, alignment: .topLeading)
        .background(DNColor.surfacePrimary, in: .rect(cornerRadius: DNRadius.brief, style: .continuous))
        .contentShape(.rect(cornerRadius: DNRadius.brief, style: .continuous))
        .accessibilityElement(children: .combine)
    }
}

/// "For you" card: hue disc + status chip, title, detail (Paper DBS-0, 248 wide).
struct ServicesForYouCard: View {
    var item: ServicesForYouItem
    var width: CGFloat? = 248

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .center) {
                HueIcon(symbol: item.symbol, hue: item.hue, size: 40)
                Spacer(minLength: 8)
                StatusChip(text: item.chip, tone: item.chipTone, onWhite: true)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(item.title)
                    .dnFont(.headline)
                    .foregroundStyle(DNColor.textPrimary)
                    .multilineTextAlignment(.leading)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
                Text(item.detail)
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                    .lineLimit(2)
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .frame(width: width, alignment: .topLeading)
        .frame(maxHeight: .infinity, alignment: .topLeading)
        .dnCard(radius: DNRadius.brief)
        .accessibilityElement(children: .combine)
    }
}

/// The personal banner on a topic page (Driving: plate · Registration due · in 12 days · Renew).
struct ServicesBannerCard: View {
    var banner: ServicesBanner
    @Environment(Router.self) private var router

    var body: some View {
        // Side by side when it fits (Paper), stacked at large text sizes.
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 12) {
                lead.frame(maxWidth: .infinity, alignment: .leading)
                button
            }
            VStack(alignment: .leading, spacing: 12) {
                lead
                button
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .dnCard()
    }

    private var lead: some View {
        HStack(spacing: 12) {
            if let plate = banner.plate {
                NumberPlate(code: plate.code, number: plate.number, emirate: plate.emirate, small: true)
                    .fixedSize()
            } else {
                HueIcon(symbol: banner.symbol, hue: banner.hue, size: 44)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(banner.title)
                    .dnFont(.rowTitle)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                Text(banner.detail)
                    .dnFont(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(ServicesTone.color(banner.detailTone))
            }
            .fixedSize()
            .accessibilityElement(children: .combine)
        }
    }

    private var button: some View {
        Button(banner.actionTitle) {
            DNHaptics.selection()
            router.servicesPerform(banner.action)
        }
        .buttonStyle(.dn(.primary, size: .small, fullWidth: false))
        .fixedSize()
    }
}

// MARK: - Rows

/// A catalogue row that opens its record, with the Favorites star as a separate button
/// beside it (never nested inside the row's button), so both are tappable and VoiceOver
/// reads them as two elements.
struct ServicesRow: View {
    var service: CatalogueService
    var trailing: CatalogueRow.Trailing = .favorite
    var showsIcon: Bool = true
    var showsContext: Bool = true

    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router

    var body: some View {
        let context = showsContext ? store.servicesContext(for: service) : nil
        HStack(spacing: 0) {
            Button {
                router.open(service: service)
            } label: {
                CatalogueRow(
                    service: service, trailing: trailing == .favorite ? .none : trailing, showsIcon: showsIcon,
                    context: context?.text, contextTone: context?.tone ?? .neutral
                )
            }
            .buttonStyle(ServicesRowButtonStyle())
            if trailing == .favorite {
                ServicesStarButton(service: service)
                    .padding(.trailing, 10)
            }
        }
        .background(DNColor.surfacePrimary)
    }
}

/// The Favorites star for a row (toast + haptics, "Favorites is full" at 8).
struct ServicesStarButton: View {
    var service: CatalogueService
    @Environment(DemoStore.self) private var store
    @Environment(ToastCenter.self) private var toasts

    var body: some View {
        let isFavorite = store.isFavorite(serviceID: service.id)
        Button {
            ServicesFavorites.toggle(service, store: store, toasts: toasts)
        } label: {
            Image(systemName: isFavorite ? "star.fill" : "star")
                .font(.system(size: 18, weight: .medium))
                .foregroundStyle(isFavorite ? DNColor.actionAccent : DNColor.iconSecondary)
                .contentTransition(.symbolEffect(.replace))
                .frame(width: 44, height: 44)
                .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(isFavorite ? "Remove \(service.shortName) from Favorites" : "Add \(service.shortName) to Favorites")
    }
}

/// Row press highlight on white groups.
struct ServicesRowButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? DNColor.listRowSurfacePressed : DNColor.surfacePrimary)
    }
}

/// Favorites toggle with the toast and haptics from the motion spec.
enum ServicesFavorites {
    static func toggle(_ service: CatalogueService, store: DemoStore, toasts: ToastCenter) {
        let wasFavorite = store.isFavorite(serviceID: service.id)
        if store.toggleFavorite(service) {
            if wasFavorite {
                DNHaptics.light()
                toasts.show("Removed from Favorites", symbol: "star.slash")
            } else {
                DNHaptics.success()
                toasts.show("Added to Favorites", symbol: "star.fill", undo: { store.toggleFavorite(service) })
            }
        } else {
            DNHaptics.warning()
            toasts.show("Favorites is full (8 of 8). Remove one first.", symbol: "exclamationmark.circle.fill")
        }
    }
}

/// A white rounded group of rows with inset separators (like `DNListGroup`, but lazy-friendly
/// and with a separator inset that clears the row icon).
struct ServicesRowGroup<Content: View>: View {
    var separatorInset: CGFloat = 16
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
        .background(DNColor.surfacePrimary, in: .rect(cornerRadius: DNRadius.listGroup, style: .continuous))
        .clipShape(.rect(cornerRadius: DNRadius.listGroup, style: .continuous))
    }
}

// MARK: - Headings, chips and links

/// Section heading inside service pages: 20/26 bold ("What you need").
struct ServicesPageHeading: View {
    var title: String
    var body: some View {
        Text(title)
            .dnFont(.title3)
            .fontWeight(.bold)
            .foregroundStyle(DNColor.textPrimary)
            .accessibilityAddTraits(.isHeader)
    }
}

/// A section header with an optional "See all N" link (topic pages, shelves).
struct ServicesSectionHeader: View {
    var title: String
    var subtitle: String? = nil
    var linkTitle: String? = nil
    var action: (() -> Void)? = nil

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
            if let linkTitle, let action {
                DNTextButton(title: linkTitle, action: action)
            }
        }
        .frame(minHeight: 44)
    }
}

/// Grey info chip with a glyph ("5 minutes", "AED 420", "RTA").
struct ServicesInfoChip: View {
    var symbol: String
    var text: String
    var tint: Color = DNColor.textPrimary
    var fill: Color = DNColor.surfaceSecondary

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: symbol)
                .font(.system(size: 13, weight: .semibold))
                .accessibilityHidden(true)
            Text(text)
                .dnFont(.subheadline)
                .fontWeight(.semibold)
                .lineLimit(1)
        }
        .foregroundStyle(tint)
        .padding(.leading, 10)
        .padding(.trailing, 12)
        .frame(minHeight: 32)
        .background(fill, in: .capsule)
    }
}

/// Inline link (accent text, 44 pt target) used in Related lists and journey stages.
struct ServicesLink: View {
    var title: String
    var weight: Font.Weight = .regular
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .dnFont(.callout)
                .fontWeight(weight)
                .foregroundStyle(DNColor.textLink)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, minHeight: 36, alignment: .leading)
                .contentShape(.rect)
        }
        .buttonStyle(.plain)
    }
}

enum ServicesTone {
    static func color(_ tone: StatusChip.Tone) -> Color {
        switch tone {
        case .warning: DNColor.statusWarningForeground
        case .info: DNColor.statusInfoForeground
        case .success: DNColor.statusSuccessForeground
        case .error: DNColor.statusErrorForeground
        case .neutral: DNColor.textSecondary
        }
    }
}

// MARK: - Layout

/// Wraps chips onto new lines (entities, info chips).
struct ServicesFlowLayout: Layout {
    var spacing: CGFloat = 8
    var lineSpacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, lineHeight: CGFloat = 0, widest: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(ProposedViewSize(width: maxWidth, height: nil))
            if x > 0, x + size.width > maxWidth + 0.5 {
                y += lineHeight + lineSpacing
                x = 0
                lineHeight = 0
            }
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
            widest = max(widest, x - spacing)
        }
        return CGSize(width: min(widest, maxWidth), height: y + lineHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, lineHeight: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(ProposedViewSize(width: bounds.width, height: nil))
            if x > bounds.minX, x + size.width > bounds.maxX + 0.5 {
                y += lineHeight + lineSpacing
                x = bounds.minX
                lineHeight = 0
            }
            view.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(width: min(size.width, bounds.width), height: size.height))
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }
    }
}

/// A horizontal shelf of cards that snaps card to card and lets shadows breathe.
struct ServicesShelf<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            // A plain HStack (shelves hold a handful of cards) so cards can share one height.
            HStack(alignment: .top, spacing: 12) {
                content
            }
            .fixedSize(horizontal: false, vertical: true)
            .scrollTargetLayout()
            .padding(.vertical, 4)
        }
        .contentMargins(.horizontal, DNSpace.insetPage, for: .scrollContent)
        .scrollTargetBehavior(.viewAligned)
        .scrollClipDisabled()
    }
}

// MARK: - Missing record

/// Shown if a route names a record that isn't in the catalogue (some parts aren't records yet).
struct ServicesMissingRecord: View {
    var id: String
    var body: some View {
        ContentUnavailableView {
            Label("Not in DubaiNow yet", systemImage: "square.dashed")
        } description: {
            Text("This service isn't in the catalogue yet. It will appear here when its owner publishes it.")
        }
        .dnCanvasBackground()
        .navigationTitle("Service")
        .navigationBarTitleDisplayMode(.inline)
        .dnPushedToolbar()
    }
}

// MARK: - Record toolbar

/// Star (Favorites, with a toast) and ⋯ (Share, Report a problem) for start, bundle and journey pages.
struct ServicesRecordToolbar: ToolbarContent {
    var service: CatalogueService
    var store: DemoStore
    var router: Router
    var toasts: ToastCenter

    var body: some ToolbarContent {
        let isFavorite = store.isFavorite(serviceID: service.id)
        ToolbarItem(placement: .topBarTrailing) {
            Button {
                ServicesFavorites.toggle(service, store: store, toasts: toasts)
            } label: {
                Image(systemName: isFavorite ? "star.fill" : "star")
                    .foregroundStyle(isFavorite ? DNColor.actionAccent : DNColor.textPrimary)
                    .contentTransition(.symbolEffect(.replace))
            }
            .accessibilityLabel(isFavorite ? "Remove from Favorites" : "Add to Favorites")
        }
        ToolbarSpacer(.fixed, placement: .topBarTrailing)
        ToolbarItem(placement: .topBarTrailing) {
            Menu {
                ShareLink(item: "\(service.name) on DubaiNow") {
                    Label("Share", systemImage: "square.and.arrow.up")
                }
                Button {
                    router.present(.comingSoon(ComingSoon(
                        title: "Report a problem",
                        detail: "Soon you'll be able to tell us what's wrong with \"\(service.name)\" and we'll pass it to whoever runs it.",
                        symbol: "exclamationmark.bubble.fill", hue: service.hue, kind: .notInDemo
                    )))
                } label: {
                    Label("Report a problem", systemImage: "exclamationmark.bubble")
                }
            } label: {
                Image(systemName: "ellipsis")
            }
            .accessibilityLabel("More")
        }
    }
}

// MARK: - Demo/screenshot aid

extension View {
    /// `-servicesScroll <id>` scrolls a Services screen to a section on launch (for demos and
    /// screenshots, since the Simulator can't be scrolled from the command line).
    func servicesLaunchScroll(_ proxy: ScrollViewProxy) -> some View {
        onAppear {
            guard let id = UserDefaults.standard.string(forKey: "servicesScroll") else { return }
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(700))
                proxy.scrollTo(id, anchor: .top)
            }
        }
    }
}
