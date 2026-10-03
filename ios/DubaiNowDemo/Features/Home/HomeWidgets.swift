import SwiftUI
import UniformTypeIdentifiers

/// "Your widgets": a two-column canvas (small = half width square, medium = full width ×
/// small height, large = full width square), packed in order. In Edit Home the widgets
/// wobble, carry remove badges, can be dragged, or selected and moved without dragging.
struct HomeWidgetsSection: View {
    var grid: HomeGrid
    var isEditing: Bool
    @Binding var selectedID: String?
    var onEdit: () -> Void

    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(ToastCenter.self) private var toasts

    @State private var draggingID: String?

    private var widgets: [HomeWidget] { store.state.widgets }

    private var suggestion: WidgetKind? {
        guard !store.isGuest, !isEditing, let kind = store.state.widgetSuggestion, !store.hasWidget(kind) else { return nil }
        return kind
    }

    var body: some View {
        VStack(alignment: .leading, spacing: isEditing ? 14 : DNSpace.sm) {
            header
            if let suggestion {
                HomeWidgetSuggestion(kind: suggestion, grid: grid, onAdd: { add(suggestion) }, onDismiss: dismissSuggestion)
                    .padding(.bottom, 4)
                    .transition(.opacity.combined(with: .scale(scale: 0.96)))
            }
            if widgets.isEmpty && suggestion == nil && !isEditing {
                HomeWidgetsEmpty()
            } else {
                VStack(alignment: .leading, spacing: grid.gutter) {
                    ForEach(HomeWidgetRow.pack(widgets)) { row in
                        HStack(spacing: grid.gutter) {
                            ForEach(row.widgets) { widget in
                                cell(widget)
                            }
                        }
                    }
                }
            }
        }
        .padding(.horizontal, DNSpace.insetPage)
    }

    // MARK: Header

    @ViewBuilder
    private var header: some View {
        if isEditing {
            HStack {
                Text("Your widgets")
                    .textCase(.uppercase)
                    .dnFont(.overline)
                    .foregroundStyle(DNColor.textSecondary)
                    .accessibilityAddTraits(.isHeader)
                Spacer(minLength: 8)
                Text(widgets.isEmpty ? "Add one to start" : "Drag, or select and move")
                    .dnFont(.footnote)
                    .fontWeight(.medium)
                    .foregroundStyle(DNColor.textSecondary)
            }
            .frame(minHeight: 20)
        } else {
            HStack {
                Text("Your widgets")
                    .dnFont(.section)
                    .foregroundStyle(DNColor.textPrimary)
                    .accessibilityAddTraits(.isHeader)
                Spacer(minLength: 8)
                if !widgets.isEmpty {
                    HomeLinkButton(title: "Edit", action: onEdit)
                        .accessibilityLabel("Edit Home")
                }
            }
            .frame(minHeight: 44)
        }
    }

    // MARK: Cell

    @ViewBuilder
    private func cell(_ widget: HomeWidget) -> some View {
        let size = grid.size(of: widget.size)
        let index = widgets.firstIndex(of: widget) ?? 0
        if isEditing {
            HomeEditableWidget(
                widget: widget,
                size: size,
                seed: index,
                isSelected: selectedID == widget.id,
                onSelect: { select(widget) },
                onRemove: { remove(widget) }
            )
            .onDrag {
                draggingID = widget.id
                DNHaptics.medium()
                return NSItemProvider(object: widget.id as NSString)
            }
            .onDrop(of: [UTType.text], delegate: HomeWidgetDropDelegate(target: widget, store: store, dragging: $draggingID))
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("\(widget.kind.title) widget, \(HomeWidgetSizes.spoken(widget.size))")
            .accessibilityAddTraits(selectedID == widget.id ? [.isButton, .isSelected] : .isButton)
            .accessibilityHint("Selects it to move or resize")
            .accessibilityAction(named: "Remove") { remove(widget) }
            .accessibilityAction(named: "Move earlier") { store.homeMoveWidget(widget.id, by: -1) }
            .accessibilityAction(named: "Move later") { store.homeMoveWidget(widget.id, by: 1) }
        } else {
            // The widget owns its touch-and-hold menu (Open, Size, Edit Home, Remove);
            // "Edit Home" there reaches HomeView through `HomeView.editHomeRequested`.
            HomeWidgetContent(widget: widget)
                .frame(width: size.width, height: size.height)
        }
    }

    // MARK: Actions

    private func select(_ widget: HomeWidget) {
        DNHaptics.selection()
        withAnimation(DNMotion.resolve(DNMotion.snappy)) {
            selectedID = selectedID == widget.id ? nil : widget.id
        }
    }

    private func remove(_ widget: HomeWidget) {
        let before = store.state.widgets
        DNHaptics.light()
        if selectedID == widget.id { selectedID = nil }
        store.removeWidget(widget)
        toasts.show("Removed \(widget.kind.title)", undo: { [store] in
            store.homeRestoreWidgets(before)
        })
    }

    private func add(_ kind: WidgetKind) {
        DNHaptics.success()
        let id = store.homeAddSuggestedWidget(kind, size: .medium)
        toasts.show("Added \(kind.title) to Home", undo: { [store] in
            if let w = store.state.widgets.first(where: { $0.id == id }) { store.removeWidget(w) }
            store.state.widgetSuggestion = kind
        })
    }

    private func dismissSuggestion() {
        store.dismissSuggestion()
        toasts.show("We won't suggest it again for 30 days", symbol: "clock")
    }
}

// MARK: - Editable widget (Edit Home)

struct HomeEditableWidget: View {
    var widget: HomeWidget
    var size: CGSize
    var seed: Int
    var isSelected: Bool
    var onSelect: () -> Void
    var onRemove: () -> Void

    var body: some View {
        HomeWidgetContent(widget: widget)
            .environment(\.widgetIsPreview, true)
            .frame(width: size.width, height: size.height)
            .allowsHitTesting(false)
            .overlay {
                // Tap anywhere on the widget selects it ("without dragging").
                Color.clear
                    .contentShape(.rect(cornerRadius: DNRadius.widget, style: .continuous))
                    .onTapGesture(perform: onSelect)
            }
            .overlay {
                if isSelected {
                    RoundedRectangle(cornerRadius: DNRadius.widget + 6, style: .continuous)
                        .strokeBorder(DNColor.borderFocus, lineWidth: 3)
                        .padding(-6)
                        .allowsHitTesting(false)
                        .transition(.opacity)
                }
            }
            .overlay(alignment: .topLeading) {
                Button(action: onRemove) {
                    Image(systemName: "minus")
                        .font(.system(size: 12, weight: .heavy))
                        .foregroundStyle(DNColor.textPrimary)
                        .frame(width: 28, height: 28)
                        .background(DNColor.neutral300, in: .circle)
                        .overlay(Circle().strokeBorder(DNColor.surfaceSecondary, lineWidth: 3).padding(-3))
                        .padding(8)
                        .contentShape(.rect)
                }
                .buttonStyle(.plain)
                .offset(x: -16, y: -16)
                .accessibilityLabel("Remove \(widget.kind.title)")
            }
            .modifier(HomeWobble(active: !isSelected, seed: seed))
            .zIndex(isSelected ? 1 : 0)
    }
}

/// Live reorder while dragging: the widget under the finger moves out of the way.
struct HomeWidgetDropDelegate: DropDelegate {
    let target: HomeWidget
    let store: DemoStore
    @Binding var dragging: String?

    func dropEntered(info: DropInfo) {
        guard let dragged = dragging, dragged != target.id,
              let from = store.state.widgets.firstIndex(where: { $0.id == dragged }),
              let to = store.state.widgets.firstIndex(where: { $0.id == target.id }) else { return }
        DNHaptics.selection()
        store.moveWidgets(from: IndexSet(integer: from), to: to > from ? to + 1 : to)
    }

    func dropUpdated(info: DropInfo) -> DropProposal? { DropProposal(operation: .move) }

    func performDrop(info: DropInfo) -> Bool {
        dragging = nil
        return true
    }
}

// MARK: - Suggested widget (JM5-0)

/// One suggestion at a time: the widget itself with a + badge, the reason underneath and
/// "Not now". One tap adds it where it stands.
struct HomeWidgetSuggestion: View {
    var kind: WidgetKind
    var grid: HomeGrid
    var onAdd: () -> Void
    var onDismiss: () -> Void

    @Environment(DemoStore.self) private var store

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Button(action: onAdd) {
                HomeWidgetContent(widget: HomeWidget(id: "suggested", kind: kind, size: .medium))
                    .environment(\.widgetIsPreview, true)
                    .frame(width: grid.content, height: grid.small)
                    .allowsHitTesting(false)
                    .contentShape(.rect(cornerRadius: DNRadius.widget, style: .continuous))
            }
            .buttonStyle(.dnPressable)
            .overlay(alignment: .topLeading) {
                Button(action: onAdd) {
                    Image(systemName: "plus")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(DNColor.textOnAccent)
                        .frame(width: 30, height: 30)
                        .background(DNColor.actionAccent, in: .circle)
                        .overlay(Circle().strokeBorder(DNColor.surfaceSecondary, lineWidth: 3).padding(-3))
                        .padding(7)
                        .contentShape(.rect)
                }
                .buttonStyle(.plain)
                .offset(x: -15, y: -15)
                .accessibilityHidden(true)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Suggested widget: \(kind.title). \(reason)")
            .accessibilityHint("Adds it to Home")
            .accessibilityAddTraits(.isButton)
            .accessibilityAction(named: "Add to Home", onAdd)
            .accessibilityAction(named: "Not now", onDismiss)

            HStack(alignment: .firstTextBaseline) {
                Text("Suggested · \(reason)")
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                    .accessibilityHidden(true)
                Spacer(minLength: 8)
                Button("Not now", action: onDismiss)
                    .dnFont(.footnote)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textLink)
                    .buttonStyle(.plain)
                    .frame(minHeight: 32)
                    .contentShape(.rect)
                    .accessibilityHidden(true)
            }
            .padding(.horizontal, 4)
        }
    }

    private var reason: String {
        switch kind {
        case .familyVisas:
            if let fv = store.state.familyVisas {
                let days = max(0, DemoDate.daysFromToday(fv.expiresOn))
                let n = fv.dueMemberIDs.count
                return "\(n) \(n == 1 ? "visa expires" : "visas expire") in \(days) days"
            }
            return "Your family's visas in one place"
        case .billsAndFines:
            return "3 bill accounts are linked"
        case .journey:
            return "You started this journey"
        case .visaTracker:
            return "Follow your request"
        default:
            return "Picked for you"
        }
    }
}

// MARK: - Empty canvas (A3J-0)

struct HomeWidgetsEmpty: View {
    @Environment(Router.self) private var router

    var body: some View {
        VStack(spacing: 8) {
            Text("Make this space yours")
                .dnFont(.title3)
                .fontWeight(.bold)
                .foregroundStyle(DNColor.textPrimary)
                .multilineTextAlignment(.center)
            Text("Add live widgets for parking, Salik, the Metro, prayer times and more.")
                .dnFont(.subheadline)
                .foregroundStyle(DNColor.textSecondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            Button {
                router.present(.widgetGallery)
            } label: {
                Label("Add widget", systemImage: "plus")
            }
            .buttonStyle(.dn(.primary, size: .medium, fullWidth: false))
            .padding(.top, 8)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 28)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: DNRadius.widget, style: .continuous)
                .strokeBorder(DNColor.neutral400, style: StrokeStyle(lineWidth: 1.5, dash: [5, 4]))
        )
    }
}

// MARK: - Footer: Add widget slot, pull hint, pull indicator

struct HomeAddWidgetSlot: View {
    var height: CGFloat = 96
    @Environment(Router.self) private var router

    var body: some View {
        Button {
            router.present(.widgetGallery)
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "plus")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(DNColor.textLink)
                    .frame(width: 44, height: 44)
                    .background(DNColor.surfacePrimary, in: .circle)
                    .shadow(color: DNColor.elevationNear, radius: 1, y: 1)
                    .shadow(color: DNColor.elevationFar, radius: 6, y: 4)
                Text("Add widget")
                    .dnFont(.headline)
                    .foregroundStyle(DNColor.textLink)
            }
            .frame(maxWidth: .infinity, minHeight: height)
            .background(
                RoundedRectangle(cornerRadius: DNRadius.widget, style: .continuous)
                    .strokeBorder(DNColor.neutral400, style: StrokeStyle(lineWidth: 1.5, dash: [5, 4]))
            )
            .contentShape(.rect(cornerRadius: DNRadius.widget, style: .continuous))
        }
        .buttonStyle(.dnPressable)
        .accessibilityLabel("Add widget")
        .accessibilityHint("Opens the widget gallery")
    }
}

struct HomePullHint: View {
    @Environment(Router.self) private var router

    var body: some View {
        Button {
            router.present(.widgetGallery)
        } label: {
            VStack(spacing: 2) {
                Image(systemName: "chevron.up")
                    .font(.system(size: 13, weight: .semibold))
                Text("Pull past the end to browse widgets")
                    .dnFont(.footnote)
                    .fontWeight(.medium)
            }
            .foregroundStyle(DNColor.textSecondary)
            .frame(minHeight: 44)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Browse widgets")
    }
}

/// Revealed under the end of Home while pulling past it (A7N-0). The ring fills to the
/// 72 pt threshold; past it, a soft haptic and "Release to add widgets".
struct HomePullIndicator: View {
    var progress: CGFloat
    var armed: Bool

    var body: some View {
        VStack(spacing: 10) {
            ZStack {
                Circle()
                    .fill(DNColor.actionAccent.opacity(armed ? 0.12 : 0.06))
                    .frame(width: 96, height: 96)
                    .blur(radius: 10)
                Circle()
                    .stroke(DNColor.neutral200, lineWidth: 4)
                Circle()
                    .trim(from: 0, to: max(0.001, min(progress, 1)))
                    .stroke(DNColor.actionAccent, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                Circle()
                    .fill(DNColor.surfacePrimary)
                    .padding(8)
                    .shadow(color: DNColor.elevationFar, radius: 8, y: 4)
                Image(systemName: "widget.small.badge.plus")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(DNColor.textPrimary)
            }
            .frame(width: 64, height: 64)
            .scaleEffect(armed ? 1.06 : 1)
            .animation(DNMotion.resolve(DNMotion.snappy), value: armed)
            VStack(spacing: 2) {
                Text(armed ? "Release to add widgets" : "Pull to add widgets")
                    .dnFont(.headline)
                    .foregroundStyle(DNColor.textPrimary)
                Text("Opens the widget gallery")
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
            }
        }
        .accessibilityHidden(true)
    }
}

// MARK: - Edit Home: pinned anchors and the bottom bar

/// "Pinned · always first": Needs you and Favorites can't move.
struct HomeEditPinnedGroup: View {
    @Environment(DemoStore.self) private var store
    @Environment(\.displayScale) private var displayScale

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Pinned · always first")
                .textCase(.uppercase)
                .dnFont(.overline)
                .foregroundStyle(DNColor.textSecondary)
                .accessibilityAddTraits(.isHeader)
            VStack(spacing: 0) {
                row(symbol: "bell", hue: .sand, title: "Needs you", detail: "Appears when something needs you")
                Rectangle()
                    .fill(DNColor.listRowSeparator)
                    .frame(height: 1 / displayScale)
                    .padding(.leading, 44)
                row(symbol: "heart", hue: .lilac, title: "Favorites", detail: favoritesLine)
            }
            .padding(.horizontal, 16)
            .background(DNColor.surfaceOnWash, in: .rect(cornerRadius: DNRadius.card, style: .continuous))
        }
        .padding(.horizontal, DNSpace.insetPage)
    }

    private var favoritesLine: String {
        let names = store.state.favorites.map(\.title)
        return names.isEmpty ? "Your shortcuts" : names.joined(separator: ", ")
    }

    private func row(symbol: String, hue: Hue, title: String, detail: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(hue.ink)
                .frame(width: 32, height: 32)
                .background(hue.start, in: .circle)
            VStack(alignment: .leading, spacing: 0) {
                Text(title)
                    .dnFont(.headline)
                    .foregroundStyle(DNColor.textPrimary)
                Text(detail)
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            Image(systemName: "lock.fill")
                .font(.system(size: 14))
                .foregroundStyle(DNColor.iconSecondary)
        }
        .frame(minHeight: 60)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(title), pinned. \(detail)")
    }
}

/// The glass bar at the bottom of Edit Home: "Add widget" when nothing is selected;
/// Earlier · Later · Size for the selected widget (moving without dragging, I9L-0).
struct HomeEditBottomBar: View {
    @Binding var selectedID: String?

    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(\.displayScale) private var displayScale

    private var selected: HomeWidget? { store.state.widgets.first { $0.id == selectedID } }

    var body: some View {
        Group {
            if let widget = selected {
                moveBar(widget)
            } else {
                Button {
                    router.present(.widgetGallery)
                } label: {
                    Label("Add widget", systemImage: "plus")
                        .dnFont(.headline)
                        .foregroundStyle(DNColor.textLink)
                        .padding(.horizontal, 24)
                        .frame(minHeight: 52)
                        .contentShape(.capsule)
                }
                .buttonStyle(.plain)
                .glassEffect(.regular.interactive(), in: .capsule)
            }
        }
        .padding(.horizontal, DNSpace.insetPage)
        .padding(.bottom, 8)
        .animation(DNMotion.resolve(DNMotion.smooth), value: selectedID)
    }

    private func moveBar(_ widget: HomeWidget) -> some View {
        let index = store.state.widgets.firstIndex(of: widget) ?? 0
        let sizes = HomeWidgetSizes.allowed(for: widget.kind)
        return HStack(spacing: 0) {
            Button {
                DNHaptics.selection()
                store.homeMoveWidget(widget.id, by: -1)
            } label: {
                segment { Label("Earlier", systemImage: "chevron.left") }
            }
            .disabled(index == 0)
            divider
            Button {
                DNHaptics.selection()
                store.homeMoveWidget(widget.id, by: 1)
            } label: {
                segment {
                    HStack(spacing: 6) {
                        Text("Later")
                        Image(systemName: "chevron.right")
                    }
                }
            }
            .disabled(index >= store.state.widgets.count - 1)
            divider
            // Tap: the next size (as in Paper, "Wide"). Touch and hold: every size.
            let next = nextSize(widget, sizes)
            Menu {
                Picker("Size", selection: Binding(get: { widget.size }, set: { store.resizeWidget(widget, to: $0) })) {
                    ForEach(sizes, id: \.self) { s in
                        Label(HomeWidgetSizes.label(s), systemImage: HomeWidgetSizes.symbol(s)).tag(s)
                    }
                }
            } label: {
                segment { Label(HomeWidgetSizes.label(next), systemImage: HomeWidgetSizes.symbol(next)) }
            } primaryAction: {
                DNHaptics.selection()
                store.resizeWidget(widget, to: next)
            }
            .disabled(sizes.count < 2)
            .accessibilityLabel("Make it \(HomeWidgetSizes.spoken(next))")
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 8)
        .frame(minHeight: 64)
        .glassEffect(.regular.interactive(), in: .capsule)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Move or resize \(widget.kind.title)")
    }

    private func nextSize(_ widget: HomeWidget, _ sizes: [WidgetSize]) -> WidgetSize {
        guard let i = sizes.firstIndex(of: widget.size) else { return sizes.first ?? widget.size }
        return sizes[(i + 1) % sizes.count]
    }

    private func segment<C: View>(@ViewBuilder _ content: () -> C) -> some View {
        HomeBarSegment(content: content())
    }

    private var divider: some View {
        Rectangle().fill(DNColor.borderSubtle).frame(width: 1, height: 24)
    }
}

/// One segment of the Edit Home bar; dims when disabled (first or last widget).
struct HomeBarSegment<Content: View>: View {
    var content: Content
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        content
            .dnFont(.subheadline)
            .fontWeight(.semibold)
            .foregroundStyle(isEnabled ? DNColor.textPrimary : DNColor.textDisabled.opacity(0.6))
            .frame(maxWidth: .infinity, minHeight: 48)
            .contentShape(.rect)
    }
}
