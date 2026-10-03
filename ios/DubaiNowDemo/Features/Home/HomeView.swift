import SwiftUI

/// Home — the Canvas (Paper C1 · Home · Default).
/// Two fixed anchors (Needs you, then Favorites), then the resident's own widgets on a
/// two-column canvas. A time-of-day wash sits behind the top and scrolls with the content.
/// Pull past the end to open the widget gallery. Edit Home rearranges the widgets.
struct HomeView: View {
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router

    @State private var width: CGFloat = 0
    @State private var safeTop: CGFloat = 0
    @State private var isEditing = false
    @State private var selectedWidgetID: String?
    @State private var pull: CGFloat = 0
    @State private var pullArmed = false
    @State private var isDragging = false
    @State private var didApplyLaunch = false
    @State private var position = ScrollPosition(edge: .top)

    /// Posted by "Edit Home" in a widget's touch-and-hold menu (Widgets feature).
    static let editHomeRequested = Notification.Name("DNWidgetEditHomeRequested")

    /// Pull past the end this far (rubber band) to open the gallery.
    private static let pullThreshold: CGFloat = 72

    private var grid: HomeGrid { HomeGrid(width: width) }
    private var showsFooter: Bool { !store.state.widgets.isEmpty }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if isEditing {
                    HomeEditPinnedGroup()
                        .padding(.top, 18)
                    widgets
                        .padding(.top, 22)
                } else {
                    if store.isGuest {
                        HomeGuestSignInCard()
                            .padding(.top, 12)
                    } else {
                        HomeNeedsYouSection()
                            .padding(.top, 12)
                    }
                    HomeFavoritesSection(grid: grid)
                        .padding(.top, DNSpace.gapScreenSection)
                        .id("favorites")
                    widgets
                        .padding(.top, DNSpace.gapScreenSection)
                        .id("widgets")
                    if showsFooter {
                        footer
                            .padding(.top, DNSpace.sm)
                    }
                }
            }
            .padding(.bottom, isEditing ? 24 : 28)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(alignment: .top) {
                // Starts at the top of the screen, behind the header, and scrolls with the content.
                AmbientWash(timeOfDay: store.timeOfDay, height: 420 + safeTop)
                    .offset(y: -safeTop)
                    .animation(DNMotion.wash, value: store.timeOfDay)
            }
        }
        .scrollPosition($position)
        .background(DNColor.surfaceSecondary.ignoresSafeArea())
        .onGeometryChange(for: CGFloat.self) { $0.size.width } action: { width = $0 }
        .onGeometryChange(for: CGFloat.self) { $0.safeAreaInsets.top } action: { safeTop = $0 }
        .onScrollGeometryChange(for: CGFloat.self) { geo in
            // How far past the end: the visible rect spans the whole scroll view (bars included),
            // so the resting bottom offset is content + bottom inset − visible height.
            let maxOffset = max(geo.contentSize.height + geo.contentInsets.bottom - geo.visibleRect.height, -geo.contentInsets.top)
            return max(0, geo.contentOffset.y - maxOffset).rounded()
        } action: { _, new in
            handlePull(new)
        }
        .onScrollPhaseChange { old, new in
            handlePhase(old, new)
        }
        .navigationBarTitleDisplayMode(.inline)
        // The main header (smiley · logo · bell · avatar) — swapped for Add · Edit Home · Done while editing.
        .background {
            if !isEditing {
                Color.clear.dnMainToolbar(showsBell: true)
            }
        }
        .toolbar {
            if isEditing {
                editToolbar
            }
        }
        .toolbar(isEditing ? .hidden : .visible, for: .tabBar)
        .safeAreaInset(edge: .bottom) {
            if isEditing {
                HomeEditBottomBar(selectedID: $selectedWidgetID)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .onAppear(perform: applyLaunchOptions)
        .onReceive(NotificationCenter.default.publisher(for: Self.editHomeRequested)) { _ in
            guard router.tab == .home else { return }
            enterEdit()
        }
        .onChange(of: store.state.widgets) { _, widgets in
            if let id = selectedWidgetID, !widgets.contains(where: { $0.id == id }) { selectedWidgetID = nil }
        }
        .onChange(of: store.persona?.id) { _, _ in
            isEditing = false
            selectedWidgetID = nil
        }
    }

    // MARK: Sections

    private var widgets: some View {
        HomeWidgetsSection(grid: grid, isEditing: isEditing, selectedID: $selectedWidgetID, onEdit: enterEdit)
    }

    private var footer: some View {
        VStack(spacing: 18) {
            HomeAddWidgetSlot()
            HomePullHint()
            // Revealed under the end of the content while pulling past it.
            Color.clear
                .frame(height: 0)
                .overlay(alignment: .top) {
                    HomePullIndicator(progress: pull / Self.pullThreshold, armed: pullArmed)
                        .padding(.top, 16)
                        .opacity(Double(min(1, max(0, (pull - 8) / 28))))
                        .fixedSize()
                }
        }
        .padding(.horizontal, DNSpace.insetPage)
    }

    @ToolbarContentBuilder
    private var editToolbar: some ToolbarContent {
        ToolbarItem(placement: .topBarLeading) {
            Button {
                router.present(.widgetGallery)
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "plus")
                    Text("Add")
                }
                .fontWeight(.semibold)
                .padding(.horizontal, 4)
            }
            .accessibilityLabel("Add widget")
        }
        ToolbarItem(placement: .principal) {
            Text("Edit Home")
                .font(.headline)
                .accessibilityAddTraits(.isHeader)
        }
        ToolbarItem(placement: .topBarTrailing) {
            Button("Done", action: exitEdit)
                .buttonStyle(.glassProminent)
                .tint(DNColor.actionAccent)
        }
    }

    // MARK: Edit Home

    private func enterEdit() {
        guard !isEditing else { return }
        DNHaptics.medium()
        withAnimation(DNMotion.resolve(DNMotion.smooth)) {
            isEditing = true
            selectedWidgetID = nil
        }
        position.scrollTo(edge: .top)
        UIAccessibility.post(notification: .announcement, argument: "Editing Home")
    }

    private func exitEdit() {
        DNHaptics.selection()
        withAnimation(DNMotion.resolve(DNMotion.smooth)) {
            isEditing = false
            selectedWidgetID = nil
        }
        position.scrollTo(edge: .top)
    }

    // MARK: Pull past the end

    private func handlePull(_ value: CGFloat) {
        guard !isEditing, showsFooter else {
            if pull != 0 { pull = 0 }
            return
        }
        pull = value
        guard isDragging else { return }
        if value >= Self.pullThreshold, !pullArmed {
            pullArmed = true
            DNHaptics.soft()
        } else if value < Self.pullThreshold, pullArmed {
            pullArmed = false
        }
    }

    private func handlePhase(_ old: ScrollPhase, _ new: ScrollPhase) {
        isDragging = new == .interacting || new == .tracking
        if old == .interacting, new != .interacting {
            if pullArmed {
                pullArmed = false
                router.present(.widgetGallery)
            }
        }
    }

    // MARK: Launch arguments (demo and screenshots)

    private func applyLaunchOptions() {
        guard !didApplyLaunch else { return }
        didApplyLaunch = true
        if HomeLaunch.folded { store.isNeedsYouFolded = true }
        if HomeLaunch.emptyNeedsYou { store.state.needsYou = [] }
        if HomeLaunch.fillFavorites {
            let extra = ["pay-electricity-water-bill", "top-up-nol-card", "your-family-residency", "your-documents"]
            for id in extra where store.state.favorites.count < DemoStore.favoritesLimit {
                if let s = Catalogue.shared.service(id), !store.isFavorite(serviceID: id) { store.toggleFavorite(s) }
            }
        }
        if HomeLaunch.editing {
            isEditing = true
            if let i = HomeLaunch.selectedWidgetIndex, store.state.widgets.indices.contains(i) {
                selectedWidgetID = store.state.widgets[i].id
            }
        }
        if let target = HomeLaunch.scrollTarget {
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(600))
                position.scrollTo(id: target, anchor: .top)
            }
        }
        if HomeLaunch.scrollBottom {
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(600))
                position.scrollTo(edge: .bottom)
            }
        }
    }
}
