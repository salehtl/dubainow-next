import SwiftUI

/// "Add widgets" (Paper C1 · Widget gallery sheet, and Gallery with new shelves for Sara).
/// Search, shelf chips, and shelves of live previews. + adds the widget, the sheet lowers and
/// Home confirms with a toast that can be undone.
struct WidgetGallerySheet: View {
    @Environment(DemoStore.self) private var store
    @Environment(ToastCenter.self) private var toasts
    @Environment(\.dismiss) private var dismiss

    /// `-widgetsGalleryQuery salik` opens with a search typed in (for screenshots).
    @State private var query = UserDefaults.standard.string(forKey: "widgetsGalleryQuery") ?? ""
    @State private var selectedShelf = "for-you"
    @State private var position = ScrollPosition(edge: .top)
    @State private var shelfTops: [String: CGFloat] = [:]
    @State private var isJumping = false
    private static let contentSpace = "widgetGalleryContent"
    @State private var contentWidth: CGFloat = 362

    var body: some View {
        if ProcessInfo.processInfo.arguments.contains("-widgetsDebug") {
            WidgetDebugGrid()
        } else {
            gallery
        }
    }

    private var shelves: [WidgetGalleryShelf] { WidgetGalleryShelf.all(store: store) }

    /// Every widget the gallery offers once (For you repeats others, so it isn't counted).
    private var catalogue: [WidgetGalleryEntry] {
        var seen = Set<String>()
        return shelves.filter { $0.id != "for-you" }.flatMap(\.entries).filter { seen.insert($0.id).inserted }
    }

    private var gallery: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    if query.trimmingCharacters(in: .whitespaces).isEmpty {
                        chips
                            .padding(.bottom, 20)
                        VStack(alignment: .leading, spacing: 36) {
                            ForEach(shelves) { shelf in
                                shelfView(shelf)
                                    .onGeometryChange(for: CGFloat.self) { $0.frame(in: .named(Self.contentSpace)).minY } action: { y in
                                        shelfTops[shelf.id] = y
                                    }
                            }
                        }
                    } else {
                        results
                    }
                }
                .padding(.bottom, 32)
                .coordinateSpace(.named(Self.contentSpace))
                .onGeometryChange(for: CGFloat.self) { $0.size.width } action: { contentWidth = $0 }
            }
            .scrollPosition($position)
            .onScrollGeometryChange(for: CGFloat.self) { $0.contentOffset.y + $0.contentInsets.top } action: { _, y in
                // The chip follows the shelf you've scrolled to.
                let current = shelfTops.filter { $0.value <= y + 60 }.max { $0.value < $1.value }?.key
                if let current, current != selectedShelf, !isJumping { selectedShelf = current }
            }
            .scrollDismissesKeyboard(.immediately)
            .background(DNColor.surfaceSecondary)
            .onAppear(perform: jumpFromLaunchArgument)
            .navigationTitle("Add widgets")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always),
                        prompt: "Search \(catalogue.count) widgets")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(role: .close) { dismiss() }.tint(DNColor.textPrimary)
                        .tint(DNColor.textPrimary)
                        .accessibilityLabel("Close")
                }
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }

    // MARK: Chips

    private var chips: some View {
        ScrollViewReader { proxy in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DNSpace.chipGap) {
                    ForEach(shelves) { shelf in
                        let selected = selectedShelf == shelf.id
                        Button {
                            DNHaptics.selection()
                            jump(to: shelf.id)
                        } label: {
                            Text(shelf.chip)
                                .dnFont(.subheadline)
                    .fontWeight(selected ? .semibold : .medium)
                                .foregroundStyle(selected ? DNColor.chipTextSelected : DNColor.chipText)
                                .padding(.horizontal, DNSpace.chipInset)
                                .padding(.vertical, 6)
                                .frame(minHeight: DNSpace.chipHeight)
                                .background(selected ? DNColor.chipSurfaceSelected : DNColor.chipSurface, in: .capsule)
                                .overlay(Capsule().strokeBorder(selected ? .clear : DNColor.borderSubtle, lineWidth: 1))
                                .contentShape(.capsule)
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(selected ? .isSelected : [])
                        .accessibilityHint("Shows the \(shelf.title) shelf")
                    }
                }
                .padding(.horizontal, DNSpace.insetPage)
                .padding(.top, 4)
            }
            .scrollClipDisabled()
            .onChange(of: selectedShelf) { _, new in
                withAnimation(DNMotion.resolve(DNMotion.smooth)) { proxy.scrollTo(new, anchor: .center) }
            }
        }
    }

    // MARK: Shelves

    private func shelfView(_ shelf: WidgetGalleryShelf) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 2) {
                Text(shelf.title)
                    .dnFont(.section)
                    .foregroundStyle(DNColor.textPrimary)
                    .accessibilityAddTraits(.isHeader)
                Text(shelf.subtitle)
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textSecondary)
            }
            if shelf.entries.isEmpty, let empty = shelf.emptyMessage {
                Text(empty)
                    .dnFont(.callout)
                    .foregroundStyle(DNColor.textSecondary)
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(DNColor.surfacePrimary, in: .rect(cornerRadius: DNRadius.card, style: .continuous))
            } else {
                entriesGrid(shelf.entries)
            }
            if let footnote = shelf.footnote {
                Label(footnote, systemImage: "info.circle")
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
            }
        }
        .padding(.horizontal, DNSpace.insetPage)
    }

    private var unit: CGFloat { max(120, (contentWidth - DNSpace.insetPage * 2 - DNSpace.widgetGutter) / 2) }
    private var wide: CGFloat { unit * 2 + DNSpace.widgetGutter }

    private func entriesGrid(_ entries: [WidgetGalleryEntry]) -> some View {
        VStack(alignment: .leading, spacing: 22) {
            ForEach(WidgetGalleryEntry.rows(entries), id: \.self) { row in
                HStack(alignment: .top, spacing: DNSpace.widgetGutter) {
                    ForEach(row) { entry in item(entry) }
                }
            }
        }
    }

    private func item(_ entry: WidgetGalleryEntry) -> some View {
        let onHome = store.hasWidget(entry.kind)
        let size: CGSize = switch entry.size {
        case .small: CGSize(width: unit, height: unit)
        case .medium: CGSize(width: wide, height: unit)
        case .large: CGSize(width: wide, height: wide)
        }
        return VStack(alignment: .leading, spacing: 10) {
            Button {
                add(entry)
            } label: {
                WidgetView(widget: HomeWidget(id: "gallery-\(entry.id)", kind: entry.kind, size: entry.size))
                    .environment(\.widgetIsPreview, true)
                    .frame(width: size.width, height: size.height)
                    .overlay(alignment: .topLeading) {
                        Circle()
                            .fill(onHome ? DNColor.statusSuccessForeground : DNColor.actionAccent)
                            .overlay(
                                Image(systemName: onHome ? "checkmark" : "plus")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundStyle(DNColor.white)
                            )
                            .frame(width: 28, height: 28)
                            .overlay(Circle().strokeBorder(DNColor.surfaceSecondary, lineWidth: 2).padding(-1))
                            .offset(x: -8, y: -8)
                            .accessibilityHidden(true)
                    }
            }
            .buttonStyle(.dnPressable)
            .disabled(onHome)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(onHome ? "\(entry.title), \(entry.size.widgetLabel.lowercased()), on Home" : "Add \(entry.title), \(entry.size.widgetLabel.lowercased())")
            .accessibilityValue(entry.caption)
            VStack(alignment: .leading, spacing: 2) {
                Text(entry.title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(DNColor.textPrimary)
                Text(onHome ? "On Home" : entry.caption)
                    .font(.system(size: 13))
                    .foregroundStyle(onHome ? DNColor.statusSuccessForeground : DNColor.textSecondary)
            }
            .lineLimit(2)
            .padding(.horizontal, 4)
            .frame(width: size.width, alignment: .leading)
            .accessibilityHidden(true)
        }
    }

    // MARK: Search

    private var matches: [WidgetGalleryEntry] {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        return catalogue.filter { e in
            e.title.lowercased().contains(q) || e.kind.widgetOwner.lowercased().contains(q)
                || e.kind.widgetSearchTerms.contains { $0.contains(q) || q.contains($0) }
        }
    }

    @ViewBuilder
    private var results: some View {
        let found = matches
        if found.isEmpty {
            ContentUnavailableView.search(text: query)
                .padding(.top, 40)
        } else {
            VStack(alignment: .leading, spacing: 16) {
                Text(found.count == 1 ? "1 widget" : "\(found.count) widgets")
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textSecondary)
                entriesGrid(found)
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.top, 12)
        }
    }

    // MARK: Adding

    private func add(_ entry: WidgetGalleryEntry) {
        guard let added = store.widgetAdd(entry.kind, size: entry.size) else { return }
        DNHaptics.success()
        dismiss()
        let title = entry.title
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(350))
            toasts.show("\(title) added to Home", undo: { store.removeWidget(added) })
        }
    }

    /// Scrolls the page to a shelf (its chip follows).
    private func jump(to id: String) {
        selectedShelf = id
        guard let top = shelfTops[id] else { return }
        isJumping = true
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { position.scrollTo(y: max(0, top - 12)) }
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(700))
            isJumping = false
        }
    }

    /// `-widgetsGalleryShelf journeys` opens the gallery scrolled to a shelf (for screenshots).
    private func jumpFromLaunchArgument() {
        if let id = UserDefaults.standard.string(forKey: "widgetsGalleryShelf") {
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(600))
                jump(to: id)
            }
        }
        // TEMPORARY: `-widgetsGalleryAutoAdd 1` taps + on the first suggestion (checks add → toast).
        if UserDefaults.standard.string(forKey: "widgetsGalleryAutoAdd") != nil,
           let first = shelves.first?.entries.first {
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(1500))
                add(first)
            }
        }
    }
}

// MARK: - Shelves and entries

struct WidgetGalleryEntry: Identifiable, Hashable {
    var kind: WidgetKind
    var size: WidgetSize
    var title: String
    var caption: String

    var id: String { "\(String(describing: kind))-\(size.rawValue)" }

    init(_ kind: WidgetKind, _ size: WidgetSize, title: String? = nil, caption: String? = nil) {
        self.kind = kind
        self.size = size
        self.title = title ?? kind.title
        self.caption = caption ?? "\(size.widgetLabel) · \(kind.widgetOwner)"
    }

    /// Smalls pair left to right; medium and large take a row each.
    static func rows(_ entries: [WidgetGalleryEntry]) -> [[WidgetGalleryEntry]] {
        var rows: [[WidgetGalleryEntry]] = []
        var pending: WidgetGalleryEntry?
        for e in entries {
            if e.size == .small {
                if let p = pending { rows.append([p, e]); pending = nil } else { pending = e }
            } else {
                if let p = pending { rows.append([p]); pending = nil }
                rows.append([e])
            }
        }
        if let p = pending { rows.append([p]) }
        return rows
    }
}

struct WidgetGalleryShelf: Identifiable {
    var id: String
    var chip: String
    var title: String
    var subtitle: String
    var entries: [WidgetGalleryEntry]
    var footnote: String? = nil
    var emptyMessage: String? = nil

    static func all(store: DemoStore) -> [WidgetGalleryShelf] {
        let s = store.state
        var shelves: [WidgetGalleryShelf] = []

        // For you: what's live on the profile and not on Home yet.
        var picks: [WidgetKind] = []
        if let suggestion = s.widgetSuggestion { picks.append(suggestion) }
        if !store.isGuest, !store.payables.isEmpty { picks.append(.billsAndFines) }
        if let r = s.requests.first(where: { if case .yourTurn = $0.status { return true }; return false }) {
            picks.append(.visaTracker(r.id))
        }
        if s.nolBalance != nil { picks.append(.nol) }
        if s.parking != nil { picks.append(.parking) }
        if store.widgetSalikLinked { picks.append(.salik) }
        picks.append(contentsOf: [.prayer, .weather])
        var seen = Set<String>()
        let forYou = picks
            .filter { !store.hasWidget($0) && seen.insert(String(describing: $0)).inserted }
            .prefix(4)
            .map { kind in WidgetGalleryEntry(kind, kind.widgetSizes.first ?? .small) }
        if !forYou.isEmpty {
            shelves.append(WidgetGalleryShelf(
                id: "for-you", chip: "For you", title: "Suggested for you",
                subtitle: store.isGuest ? "Useful without an account" : "Based on what's live on your profile",
                entries: Array(forYou)
            ))
        }

        // All at once: bundles with something live for you.
        var bundles: [WidgetGalleryEntry] = []
        if !store.isGuest, !store.payables.isEmpty || !s.history.isEmpty {
            bundles += [WidgetGalleryEntry(.billsAndFines, .small), WidgetGalleryEntry(.billsAndFines, .medium)]
        }
        if s.familyVisas != nil {
            bundles += [WidgetGalleryEntry(.familyVisas, .small), WidgetGalleryEntry(.familyVisas, .medium)]
        }
        shelves.append(WidgetGalleryShelf(
            id: "all-at-once", chip: "All at once", title: "All at once",
            subtitle: "Bundles with something live for you",
            entries: bundles,
            footnote: "Certificates and letters isn't offered: it has nothing live to show until you ask for a certificate.",
            emptyMessage: store.isGuest ? "Sign in to see bundles with something live for you, like Pay all."
                                        : "None of your bundles has anything live right now."
        ))

        // Your journeys: only the ones you've started.
        let journeys = s.journeys.flatMap { j -> [WidgetGalleryEntry] in
            let kind = WidgetKind.journey(j.journeyID)
            let started = DemoDate.short(j.startedOn)
            return [
                WidgetGalleryEntry(kind, .medium, caption: "Medium · the journey you started on \(started)"),
                WidgetGalleryEntry(kind, .large, caption: "Large · adds every stage"),
            ]
        }
        shelves.append(WidgetGalleryShelf(
            id: "journeys", chip: "Your journeys", title: "Your journeys",
            subtitle: "Only the ones you've started",
            entries: journeys,
            emptyMessage: "Start a journey from Services, like Move home, and it shows up here."
        ))

        shelves.append(WidgetGalleryShelf(
            id: "getting-around", chip: "Getting around", title: "Getting around",
            subtitle: "Parking, tolls and the metro",
            entries: [
                WidgetGalleryEntry(.parking, .small), WidgetGalleryEntry(.salik, .small),
                WidgetGalleryEntry(.parking, .medium), WidgetGalleryEntry(.salik, .medium),
                WidgetGalleryEntry(.nol, .small), WidgetGalleryEntry(.metro, .medium),
            ]
        ))

        shelves.append(WidgetGalleryShelf(
            id: "your-home", chip: "Your home", title: "Your home",
            subtitle: "From your DEWA account",
            entries: [WidgetGalleryEntry(.energy, .medium)]
        ))

        let tracked = s.requests.filter {
            switch $0.status { case .yourTurn, .inReview: true; default: false }
        }
        if !tracked.isEmpty {
            shelves.append(WidgetGalleryShelf(
                id: "requests", chip: "Requests", title: "Your requests",
                subtitle: "Follow a request without opening it",
                entries: tracked.map {
                    WidgetGalleryEntry(.visaTracker($0.id), .small, title: $0.title.replacingOccurrences(of: "Renew your ", with: "").capitalizedFirst,
                                       caption: "Small · \($0.entity)")
                }
            ))
        }

        shelves.append(WidgetGalleryShelf(
            id: "faith", chip: "Faith", title: "Faith and family",
            subtitle: "Where you are, no account needed",
            entries: [WidgetGalleryEntry(.prayer, .small), WidgetGalleryEntry(.prayer, .large)]
        ))

        shelves.append(WidgetGalleryShelf(
            id: "city-life", chip: "City life", title: "City life",
            subtitle: "Weather and beach flags",
            entries: [WidgetGalleryEntry(.weather, .small)]
        ))
        return shelves
    }
}

private extension String {
    var capitalizedFirst: String { prefix(1).uppercased() + dropFirst() }
}
