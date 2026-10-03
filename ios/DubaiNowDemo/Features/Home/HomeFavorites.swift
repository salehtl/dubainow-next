import SwiftUI

/// Anchor 2 · Favorites. Four columns of hue tiles (they stretch with the screen), label
/// 13/16 medium under each. Up to eight; Edit opens the edit sheet. Touch and hold for
/// Open / Remove. At accessibility text sizes: two columns, label beside the tile.
struct HomeFavoritesSection: View {
    var grid: HomeGrid

    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(ToastCenter.self) private var toasts
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    private var favorites: [FavoriteItem] { store.state.favorites }

    var body: some View {
        VStack(alignment: .leading, spacing: DNSpace.sm) {
            HStack {
                Text("Favorites")
                    .dnFont(.section)
                    .foregroundStyle(DNColor.textPrimary)
                    .accessibilityAddTraits(.isHeader)
                Spacer(minLength: 8)
                HomeLinkButton(title: "Edit") { router.present(.editFavorites) }
                    .accessibilityLabel("Edit Favorites")
            }
            .frame(minHeight: 44)

            if dynamicTypeSize.isAccessibilitySize {
                accessibleGrid
            } else {
                tileGrid
            }
        }
        .padding(.horizontal, DNSpace.insetPage)
    }

    // MARK: Default grid (four columns)

    private var tileGrid: some View {
        let columns = Array(repeating: GridItem(.fixed(grid.tile), spacing: grid.gutter, alignment: .top), count: 4)
        return LazyVGrid(columns: columns, alignment: .leading, spacing: 16) {
            ForEach(favorites) { item in
                tileButton(item)
            }
            if favorites.count < 4 {
                addTile
            }
        }
    }

    private func tileButton(_ item: FavoriteItem) -> some View {
        Button {
            HomeFavoriteRouting.open(item, store: store, router: router)
        } label: {
            VStack(spacing: 8) {
                HueTile(symbol: item.symbol, hue: item.hue, size: grid.tile)
                    .overlay(alignment: .topTrailing) { HomeFavoriteBadge(item: item).offset(x: 6, y: -6) }
                Text(item.title)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(DNColor.textPrimary)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .frame(width: grid.tile + 8)
            }
            .contentShape(.rect)
        }
        .buttonStyle(.dnPressable)
        .contextMenu { contextMenu(for: item) }
        .accessibilityLabel(HomeFavoriteRouting.accessibilityLabel(item, store: store))
        .accessibilityAction(named: "Remove from Favorites") { remove(item) }
    }

    private var addTile: some View {
        Button {
            router.present(.editFavorites)
        } label: {
            VStack(spacing: 8) {
                RoundedRectangle(cornerRadius: DNRadius.tile, style: .continuous)
                    .strokeBorder(DNColor.neutral400, style: StrokeStyle(lineWidth: 1.5, dash: [5, 4]))
                    .frame(width: grid.tile, height: grid.tile)
                    .overlay(
                        Image(systemName: "plus")
                            .font(.system(size: 22, weight: .semibold))
                            .foregroundStyle(DNColor.textLink)
                    )
                Text("Add")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(DNColor.textLink)
            }
            .contentShape(.rect)
        }
        .buttonStyle(.dnPressable)
        .accessibilityLabel("Add a favorite")
    }

    // MARK: Accessibility sizes (two columns, label beside the tile)

    private var accessibleGrid: some View {
        let columns = [GridItem(.flexible(), spacing: grid.gutter, alignment: .leading), GridItem(.flexible(), spacing: grid.gutter, alignment: .leading)]
        return LazyVGrid(columns: columns, alignment: .leading, spacing: 12) {
            ForEach(favorites) { item in
                Button {
                    HomeFavoriteRouting.open(item, store: store, router: router)
                } label: {
                    HStack(spacing: 10) {
                        HueTile(symbol: item.symbol, hue: item.hue, size: 52)
                        Text(item.title)
                            .dnFont(.footnote)
                            .fontWeight(.medium)
                            .foregroundStyle(DNColor.textPrimary)
                            .multilineTextAlignment(.leading)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(.rect)
                }
                .buttonStyle(.dnPressable)
                .contextMenu { contextMenu(for: item) }
                .accessibilityLabel(HomeFavoriteRouting.accessibilityLabel(item, store: store))
            }
            if favorites.count < 4 {
                Button("Add a favorite") { router.present(.editFavorites) }
                    .dnFont(.body)
                    .foregroundStyle(DNColor.textLink)
                    .buttonStyle(.plain)
                    .frame(minHeight: 44)
            }
        }
    }

    // MARK: Menu and actions

    @ViewBuilder
    private func contextMenu(for item: FavoriteItem) -> some View {
        Button {
            HomeFavoriteRouting.open(item, store: store, router: router)
        } label: {
            Label("Open", systemImage: "arrow.up.forward.app")
        }
        Button {
            router.present(.editFavorites)
        } label: {
            Label("Edit Favorites", systemImage: "square.grid.2x2")
        }
        Button(role: .destructive) {
            remove(item)
        } label: {
            Label("Remove from Favorites", systemImage: "minus.circle")
        }
    }

    private func remove(_ item: FavoriteItem) {
        let before = store.state.favorites
        DNHaptics.light()
        store.removeFavorite(item)
        toasts.show("Removed \(item.title) from Favorites", undo: { [store] in
            store.homeRestoreFavorites(before)
        })
    }
}

// MARK: - Badge (one per tile)

/// Green with a dot = something running (a parking session). Red with a count = things in
/// Needs you for that hub (Vehicles).
struct HomeFavoriteBadge: View {
    var item: FavoriteItem
    @Environment(DemoStore.self) private var store

    var body: some View {
        if let badge = HomeFavoriteRouting.badge(for: item, store: store) {
            HStack(spacing: 4) {
                if badge.live {
                    Circle().fill(DNColor.textOnAccent).frame(width: 5, height: 5)
                }
                Text(badge.text)
                    .font(.system(size: 12, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(DNColor.textOnAccent)
            }
            .padding(.horizontal, 6)
            .frame(minWidth: 22, minHeight: 22)
            .background(badge.live ? DNColor.statusSuccessForeground : DNColor.actionDestructive, in: .capsule)
            .overlay(Capsule().strokeBorder(DNColor.surfaceSecondary, lineWidth: 2))
            .accessibilityHidden(true)
        }
    }
}

// MARK: - Routing

/// Where a favorite goes. Pay parking opens Extend when a session is running; Salik opens
/// Top up; hubs (things) open their own screens; everything else opens its catalogue record.
enum HomeFavoriteRouting {
    static func open(_ item: FavoriteItem, store: DemoStore, router: Router) {
        switch item.target {
        case .service(let id):
            switch id {
            case "pay-for-parking":
                if store.state.parking != nil { router.present(.extendParking) } else { router.open(serviceID: id) }
            case "top-up-salik-account":
                router.present(.topUpSalik)
            default:
                router.open(serviceID: id)
            }
        case .thing(let id):
            switch id {
            case "your-vehicles": router.push(.vehicles)
            case "your-documents": router.push(.documents)
            case "your-family-residency": router.push(.family)
            default: router.open(serviceID: id)
            }
        }
    }

    static func service(for item: FavoriteItem) -> CatalogueService? {
        switch item.target {
        case .service(let id), .thing(let id): Catalogue.shared.service(id)
        }
    }

    /// The full GOV.UK-style name is the accessibility label (plus the badge).
    static func accessibilityLabel(_ item: FavoriteItem, store: DemoStore) -> String {
        var label = service(for: item)?.name ?? item.title
        if let badge = badge(for: item, store: store) {
            label += ", \(badge.spoken)"
        }
        return label
    }

    static func badge(for item: FavoriteItem, store: DemoStore) -> (text: String, live: Bool, spoken: String)? {
        switch item.target {
        case .service("pay-for-parking"):
            guard let p = store.state.parking else { return nil }
            let m = Int(p.endsAt.timeIntervalSinceNow / 60)
            guard m > 0 else { return nil }
            let text = m >= 60 ? "\(m / 60)h" : "\(m)m"
            return (text, true, "parking running, \(m) minutes left")
        case .thing("your-vehicles"):
            let n = store.homeVehicleAttentionCount
            guard n > 0 else { return nil }
            return ("\(n)", false, n == 1 ? "1 needs attention" : "\(n) need attention")
        default:
            return nil
        }
    }
}
