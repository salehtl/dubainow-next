import SwiftUI
import UniformTypeIdentifiers

/// Favorites edit sheet (C1 · Favorites edit sheet, Limit reached 8 of 8).
/// Mirrors the Home grid: your favorites in order (remove, drag to reorder), empty slots up
/// to eight, then things to add — suggested for you, your things (hubs), the services of
/// your main topic, or search results. Changes show on Home straight away; × puts them back,
/// ✓ keeps them.
struct EditFavoritesSheet: View {
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(\.dismiss) private var dismiss
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    @State private var snapshot: [FavoriteItem]?
    @State private var query = ""
    @State private var draggingID: String?
    @State private var width: CGFloat = 0
    @State private var limitNudge = false
    @State private var detent: PresentationDetent = HomeLaunch.args.contains("-homeSheetLarge") ? .large : .medium

    private var favorites: [FavoriteItem] { store.state.favorites }
    private var limit: Int { DemoStore.favoritesLimit }
    private var isFull: Bool { favorites.count >= limit }
    private var hasChanges: Bool { snapshot.map { $0 != favorites } ?? false }
    private var grid: HomeGrid { HomeGrid(width: width) }
    private var trimmedQuery: String { query.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        NavigationStack {
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        onYourHome
                        if isFull {
                            limitNotice
                                .id("limit")
                                .padding(.top, 16)
                                .transition(.opacity.combined(with: .move(edge: .top)))
                        }
                        searchField
                            .padding(.top, 24)
                        if trimmedQuery.isEmpty {
                            if !suggested.isEmpty {
                                group("Suggested for you", rows: suggested, proxy: proxy)
                            }
                            if !things.isEmpty {
                                group("Your things", trailing: "hubs from your profile", rows: things, proxy: proxy)
                            }
                            if let topic = mainTopic {
                                group(topic.name, rows: topicRows(topic), proxy: proxy)
                            }
                        } else {
                            let found = results
                            if found.isEmpty {
                                Text("No services match \u{201C}\(trimmedQuery)\u{201D}. Try a different word, like \u{201C}parking\u{201D} or \u{201C}visa\u{201D}.")
                                    .dnFont(.subheadline)
                                    .foregroundStyle(DNColor.textSecondary)
                                    .padding(.top, 24)
                                    .padding(.horizontal, 4)
                            } else {
                                group("Results", rows: found, proxy: proxy)
                            }
                        }
                        browseAll
                            .padding(.top, 20)
                    }
                    .padding(.horizontal, DNSpace.insetPage)
                    .padding(.top, 8)
                    .padding(.bottom, 24)
                    .animation(DNMotion.resolve(DNMotion.smooth), value: isFull)
                }
                .scrollDismissesKeyboard(.interactively)
                // Glass at the medium height; the grouped grey canvas once it's full height.
                .background {
                    if detent == .large {
                        DNColor.surfaceSecondary.ignoresSafeArea()
                    }
                }
                .animation(DNMotion.base, value: detent)
                .onGeometryChange(for: CGFloat.self) { $0.size.width } action: { width = $0 }
            }
            .navigationTitle("Favorites")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .close) {
                        if let snapshot, hasChanges { store.homeRestoreFavorites(snapshot) }
                        dismiss()
                    }
                    .tint(DNColor.textPrimary)
                    .accessibilityLabel(hasChanges ? "Cancel changes" : "Close")
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(role: .confirm) {
                        if hasChanges { DNHaptics.success() }
                        dismiss()
                    }
                    .accessibilityLabel("Done")
                }
            }
        }
        .presentationDetents([.medium, .large], selection: $detent)
        .presentationDragIndicator(.visible)
        .interactiveDismissDisabled(hasChanges)
        .onAppear {
            if snapshot == nil { snapshot = favorites }
        }
    }

    // MARK: On your Home

    private var onYourHome: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                overline("On your Home")
                Spacer(minLength: 8)
                Text("\(favorites.count) of \(limit)")
                    .dnFont(.footnote)
                    .fontWeight(.semibold)
                    .monospacedDigit()
                    .foregroundStyle(isFull ? DNColor.statusWarningForeground : DNColor.textSecondary)
                    .accessibilityLabel("\(favorites.count) of \(limit) favorites")
            }
            .padding(.horizontal, 4)

            if dynamicTypeSize.isAccessibilitySize {
                accessibleList
            } else {
                let columns = Array(repeating: GridItem(.fixed(grid.tile), spacing: grid.gutter, alignment: .top), count: 4)
                LazyVGrid(columns: columns, alignment: .leading, spacing: 16) {
                    ForEach(favorites) { item in
                        tile(item)
                    }
                    ForEach(0..<max(0, limit - favorites.count), id: \.self) { _ in
                        RoundedRectangle(cornerRadius: DNRadius.tile, style: .continuous)
                            .strokeBorder(DNColor.neutral400, style: StrokeStyle(lineWidth: 1.5, dash: [5, 4]))
                            .frame(width: grid.tile, height: grid.tile)
                            .accessibilityHidden(true)
                    }
                }
            }

            Text("Touch and hold a favorite to drag it. This is the order on Home.")
                .dnFont(.footnote)
                .foregroundStyle(DNColor.textSecondary)
                .padding(.horizontal, 4)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private func tile(_ item: FavoriteItem) -> some View {
        VStack(spacing: 8) {
            HueTile(symbol: item.symbol, hue: item.hue, size: grid.tile)
            Text(item.title)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(DNColor.textPrimary)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .frame(width: grid.tile + 8)
        }
        .overlay(alignment: .topLeading) {
            Button {
                remove(item)
            } label: {
                Image(systemName: "minus")
                    .font(.system(size: 12, weight: .heavy))
                    .foregroundStyle(DNColor.textPrimary)
                    .frame(width: 26, height: 26)
                    .background(DNColor.neutral300, in: .circle)
                    .overlay(Circle().strokeBorder(DNColor.surfaceSecondary, lineWidth: 3).padding(-3))
                    .padding(9)
                    .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .offset(x: -18, y: -18)
        }
        .contentShape(.dragPreview, .rect(cornerRadius: DNRadius.tile, style: .continuous))
        .onDrag {
            draggingID = item.id
            DNHaptics.medium()
            return NSItemProvider(object: item.id as NSString)
        }
        .onDrop(of: [UTType.text], delegate: HomeFavoriteDropDelegate(target: item, store: store, dragging: $draggingID))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(HomeFavoriteRouting.service(for: item)?.name ?? item.title)
        .accessibilityValue(position(of: item))
        .accessibilityAction(named: "Remove from Favorites") { remove(item) }
        .accessibilityAction(named: "Move earlier") { store.homeMoveFavorite(item.id, by: -1) }
        .accessibilityAction(named: "Move later") { store.homeMoveFavorite(item.id, by: 1) }
    }

    /// Accessibility sizes: a plain list with Remove and Move buttons instead of the grid.
    private var accessibleList: some View {
        VStack(spacing: 8) {
            ForEach(favorites) { item in
                HStack(spacing: 12) {
                    HueTile(symbol: item.symbol, hue: item.hue, size: 48)
                    Text(item.title)
                        .dnFont(.rowTitle)
                        .foregroundStyle(DNColor.textPrimary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button {
                        remove(item)
                    } label: {
                        Image(systemName: "minus.circle.fill")
                            .font(.system(size: 24))
                            .foregroundStyle(DNColor.actionDestructive)
                            .frame(minWidth: 44, minHeight: 44)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Remove \(item.title)")
                }
                .padding(12)
                .background(DNColor.surfacePrimary, in: .rect(cornerRadius: DNRadius.card, style: .continuous))
                .accessibilityElement(children: .contain)
                .accessibilityAction(named: "Move earlier") { store.homeMoveFavorite(item.id, by: -1) }
                .accessibilityAction(named: "Move later") { store.homeMoveFavorite(item.id, by: 1) }
            }
        }
    }

    private func position(of item: FavoriteItem) -> String {
        guard let i = favorites.firstIndex(of: item) else { return "" }
        return "\(i + 1) of \(favorites.count)"
    }

    // MARK: Limit (8 of 8)

    private var limitNotice: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "exclamationmark.circle.fill")
                .font(.system(size: 20))
                .foregroundStyle(DNColor.statusWarningForeground)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text("Your Home has \(limit) favorites")
                    .dnFont(.headline)
                    .foregroundStyle(DNColor.statusWarningForeground)
                Text("Remove one to add another. Eight keeps Home quick to scan.")
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .background(DNColor.statusWarningSurface, in: .rect(cornerRadius: DNRadius.card, style: .continuous))
        .scaleEffect(limitNudge ? 1.03 : 1)
        .accessibilityElement(children: .combine)
    }

    // MARK: Search

    private var searchField: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(DNColor.iconSecondary)
                .accessibilityHidden(true)
            TextField("Search services to add", text: $query)
                .dnFont(.body)
                .foregroundStyle(DNColor.textPrimary)
                .submitLabel(.search)
                .autocorrectionDisabled()
            if !query.isEmpty {
                Button {
                    query = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 17))
                        .foregroundStyle(DNColor.iconSecondary)
                        .frame(minWidth: 44, minHeight: 44)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Clear search")
            }
        }
        .padding(.leading, 16)
        .padding(.trailing, query.isEmpty ? 16 : 4)
        .frame(minHeight: 52)
        .background(DNColor.surfacePrimary, in: .capsule)
    }

    // MARK: Groups and rows

    private func group(_ title: String, trailing: String? = nil, rows: [HomeFavoriteCandidate], proxy: ScrollViewProxy) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline) {
                overline(title)
                Spacer(minLength: 8)
                if let trailing {
                    Text(trailing)
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textSecondary)
                }
            }
            .padding(.horizontal, 4)
            DNListGroup {
                ForEach(rows) { row in
                    candidateRow(row, proxy: proxy)
                }
            }
        }
        .padding(.top, 24)
    }

    private func candidateRow(_ row: HomeFavoriteCandidate, proxy: ScrollViewProxy) -> some View {
        let added = store.isFavorite(serviceID: row.service.id)
        return Button {
            toggle(row.service, proxy: proxy)
        } label: {
            HStack(spacing: 12) {
                HueIcon(symbol: row.service.symbol, hue: row.service.hue, size: 40)
                VStack(alignment: .leading, spacing: 2) {
                    Text(row.title)
                        .dnFont(.headline)
                        .foregroundStyle(DNColor.listRowTitle)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(row.subtitle)
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.listRowSupporting)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 8)
                toggleMark(added: added)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(minHeight: 64)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(row.title), \(row.subtitle)")
        .accessibilityValue(added ? "On Home" : (isFull ? "Favorites are full" : "Not on Home"))
        .accessibilityHint(added ? "Removes it from Favorites" : "Adds it to Favorites")
        .accessibilityAddTraits(added ? [.isButton, .isSelected] : .isButton)
    }

    private func toggleMark(added: Bool) -> some View {
        Group {
            if added {
                Image(systemName: "checkmark")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(DNColor.textOnAccent)
                    .frame(width: 30, height: 30)
                    .background(DNColor.actionAccent, in: .circle)
                    .transition(.scale.combined(with: .opacity))
            } else {
                Image(systemName: "plus")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(isFull ? DNColor.neutral400 : DNColor.actionAccent)
                    .frame(width: 30, height: 30)
                    .background(isFull ? DNColor.neutral100 : DNColor.actionAccentSubtle, in: .circle)
                    .transition(.scale.combined(with: .opacity))
            }
        }
        .frame(minWidth: 44, minHeight: 44)
        .accessibilityHidden(true)
    }

    private var browseAll: some View {
        Button {
            dismiss()
            router.select(.services)
        } label: {
            Text("Browse all \(Catalogue.shared.services.count) services")
                .dnFont(.body)
                .fontWeight(.medium)
                .foregroundStyle(DNColor.textLink)
                .frame(maxWidth: .infinity, minHeight: 44)
                .contentShape(.rect)
        }
        .buttonStyle(.plain)
    }

    private func overline(_ text: String) -> some View {
        Text(text)
            .textCase(.uppercase)
            .dnFont(.overline)
            .foregroundStyle(DNColor.textGroupedHeader)
            .accessibilityAddTraits(.isHeader)
    }

    // MARK: Actions

    private func toggle(_ service: CatalogueService, proxy: ScrollViewProxy) {
        if store.isFavorite(serviceID: service.id) {
            DNHaptics.light()
            store.toggleFavorite(service)
        } else if isFull {
            DNHaptics.warning()
            withAnimation(DNMotion.resolve(DNMotion.smooth)) { proxy.scrollTo("limit", anchor: .center) }
            withAnimation(DNMotion.resolve(DNMotion.snappy)) { limitNudge = true }
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(220))
                withAnimation(DNMotion.resolve(DNMotion.snappy)) { limitNudge = false }
            }
            UIAccessibility.post(notification: .announcement, argument: "Your Home has \(limit) favorites. Remove one to add another.")
        } else if store.toggleFavorite(service) {
            DNHaptics.success()
        }
    }

    private func remove(_ item: FavoriteItem) {
        DNHaptics.light()
        store.removeFavorite(item)
    }

    // MARK: Candidates

    /// Personal suggestions first (renewals coming up, a nol balance), then popular services.
    private var suggested: [HomeFavoriteCandidate] {
        guard !store.isGuest else { return popular(3) }
        var out: [HomeFavoriteCandidate] = []
        func add(_ id: String, _ subtitle: String) {
            guard out.count < 3, !out.contains(where: { $0.id == id }), let s = Catalogue.shared.service(id) else { return }
            out.append(HomeFavoriteCandidate(service: s, title: s.name, subtitle: subtitle))
        }
        for item in store.state.needsYou {
            if case .renewal(let sid, let thing) = item.kind {
                let v = store.state.vehicles.first { $0.id == thing }
                let when = item.due.map { DemoDate.dueLabel($0).lowercased() } ?? "due soon"
                add(sid, v.map { "Your \($0.model) is \(when)" } ?? "Yours is \(when)")
            }
        }
        if let nol = store.state.nolBalance { add("top-up-nol-card", "Balance \(Money.aed(nol))") }
        if !store.state.journeys.isEmpty || !store.state.vehicles.isEmpty { add("check-prayer-times", "Popular in Dubai this week") }
        for item in store.state.needsYou {
            if case .payment(let p) = item.kind, let sid = p.serviceID {
                add(sid, item.due.map { "Your bill is \(DemoDate.dueLabel($0).lowercased())" } ?? "\(p.entity) · \(Money.aed(p.amount))")
            }
        }
        for c in popular(6) { add(c.id, c.subtitle) }
        return out
    }

    private func popular(_ n: Int) -> [HomeFavoriteCandidate] {
        Catalogue.shared.services
            .filter { $0.type == .standalone && $0.isLive }
            .sorted { $0.uniques30d > $1.uniques30d }
            .prefix(n)
            .map { HomeFavoriteCandidate(service: $0, title: $0.name, subtitle: "\($0.ownerLabel) · popular in Dubai") }
    }

    /// Hubs from the resident's profile.
    private var things: [HomeFavoriteCandidate] {
        let s = store.state
        var out: [HomeFavoriteCandidate] = []
        func add(_ id: String, _ title: String, _ subtitle: String) {
            guard let service = Catalogue.shared.service(id) else { return }
            out.append(HomeFavoriteCandidate(service: service, title: title, subtitle: subtitle))
        }
        if !s.vehicles.isEmpty {
            add("your-vehicles", "Vehicles", s.vehicles.map(\.shortName).joined(separator: ", "))
            add("top-up-salik-account", "Salik", "Balance, trips and top-ups")
        }
        if !s.documents.isEmpty {
            add("your-documents", "Documents", s.documents.prefix(4).map(\.title).joined(separator: ", "))
        }
        if !s.family.isEmpty {
            add("your-family-residency", "Family residency", s.family.map { $0.name.split(separator: " ").first.map(String.init) ?? $0.name }.joined(separator: ", "))
        }
        if let home = s.home {
            add("your-electricity-water-account", "DEWA account", "Electricity and water · \(home.dewaAccount)")
        }
        return out
    }

    /// The topic most of the resident's favorites come from (Driving and transport for Sara).
    private var mainTopic: CatalogueTopic? {
        var counts: [String: Int] = [:]
        for f in favorites {
            if let t = HomeFavoriteRouting.service(for: f)?.topic { counts[t, default: 0] += 1 }
        }
        let id = counts.max { $0.value < $1.value }?.key ?? "driving-and-transport"
        return Catalogue.shared.topic(id)
    }

    private func topicRows(_ topic: CatalogueTopic) -> [HomeFavoriteCandidate] {
        Catalogue.shared.standalone(inTopic: topic.id)
            .filter(\.isLive)
            .prefix(4)
            .map { s in
                var subtitle = s.ownerLabel
                if let fav = favorites.first(where: { $0.target == .service(s.id) }), fav.title != s.name {
                    subtitle += " · on Home as \u{201C}\(fav.title)\u{201D}"
                }
                return HomeFavoriteCandidate(service: s, title: s.name, subtitle: subtitle)
            }
    }

    private var results: [HomeFavoriteCandidate] {
        Catalogue.shared.search(trimmedQuery)
            .filter { $0.type == .standalone || $0.kind == "thing" }
            .prefix(20)
            .map { HomeFavoriteCandidate(service: $0, title: $0.name, subtitle: $0.ownerLabel) }
    }
}

/// A row you can add to (or remove from) Favorites.
struct HomeFavoriteCandidate: Identifiable {
    let service: CatalogueService
    var title: String
    var subtitle: String
    var id: String { service.id }
}

/// Live reorder of favorites while dragging in the edit sheet.
struct HomeFavoriteDropDelegate: DropDelegate {
    let target: FavoriteItem
    let store: DemoStore
    @Binding var dragging: String?

    func dropEntered(info: DropInfo) {
        guard let dragged = dragging, dragged != target.id,
              let from = store.state.favorites.firstIndex(where: { $0.id == dragged }),
              let to = store.state.favorites.firstIndex(where: { $0.id == target.id }) else { return }
        DNHaptics.selection()
        withAnimation(DNMotion.resolve(DNMotion.smooth)) {
            store.moveFavorites(from: IndexSet(integer: from), to: to > from ? to + 1 : to)
        }
    }

    func dropUpdated(info: DropInfo) -> DropProposal? { DropProposal(operation: .move) }

    func performDrop(info: DropInfo) -> Bool {
        dragging = nil
        return true
    }
}
