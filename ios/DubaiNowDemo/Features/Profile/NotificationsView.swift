import SwiftUI

/// Notifications, grouped Today / Earlier with unread dots. Shown as a sheet (from the bell)
/// and pushed (Route.notifications); both live inside a NavigationStack, so rows with a link
/// push their destination in place.
struct NotificationsView: View {
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(\.dismiss) private var dismiss

    /// Presented as the bell sheet (rather than pushed in a tab).
    private var isSheet: Bool { router.sheet == .notifications }

    @State private var openedLink: Route?

    private var sorted: [NotificationItem] { store.state.notifications.sorted { $0.date > $1.date } }
    private var today: [NotificationItem] { sorted.filter { DemoDate.daysFromToday($0.date) == 0 } }
    private var earlier: [NotificationItem] { sorted.filter { DemoDate.daysFromToday($0.date) != 0 } }

    var body: some View {
        List {
            if !store.state.notifications.isEmpty && store.unreadCount == 0 {
                Section {
                    Label("You're all caught up", systemImage: "checkmark.circle.fill")
                        .dnFont(.subheadline)
                        .fontWeight(.medium)
                        .foregroundStyle(DNColor.statusSuccessForeground)
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets(top: 0, leading: 4, bottom: 0, trailing: 4))
                }
            }
            if !today.isEmpty {
                Section("Today") {
                    ForEach(today) { row($0) }
                }
            }
            if !earlier.isEmpty {
                Section("Earlier") {
                    ForEach(earlier) { row($0) }
                }
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .dnCanvasBackground()
        .overlay {
            if store.state.notifications.isEmpty {
                ContentUnavailableView(
                    "You're all caught up",
                    systemImage: "bell",
                    description: Text("We'll tell you when something is due, when it's your turn, and when there's a result.")
                )
            }
        }
        .navigationDestination(item: $openedLink) { RouteDestinationView(route: $0) }
        .task {
            // Demo hook: `-profileOpenNotification <id>` taps a notification on launch.
            guard let id = UserDefaults.standard.string(forKey: "profileOpenNotification"),
                  let n = store.state.notifications.first(where: { $0.id == id }), !n.isRead || n.link != nil else { return }
            try? await Task.sleep(for: .milliseconds(600))
            store.markRead(id)
            openedLink = n.link
        }
        .animation(DNMotion.resolve(DNMotion.smooth), value: store.unreadCount)
        .profileChrome("Notifications", large: !isSheet, forceSheet: isSheet)
        .toolbar {
            if isSheet {
                ToolbarItem(placement: .topBarLeading) {
                    Button(role: .close) { dismiss() }.tint(DNColor.textPrimary)
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button("Mark all read") {
                    DNHaptics.selection()
                    withAnimation(DNMotion.resolve(DNMotion.smooth)) { store.markAllRead() }
                }
                .disabled(store.unreadCount == 0)
            }
        }
    }

    @ViewBuilder private func row(_ n: NotificationItem) -> some View {
        // A button (not a bare NavigationLink) so one tap both marks it read and opens its
        // link, pushed in place on whichever stack shows this list (sheet or tab).
        Button {
            withAnimation(DNMotion.resolve(DNMotion.snappy)) { store.markRead(n.id) }
            if let link = n.link { openedLink = link }
        } label: {
            ProfileNotificationRow(item: n)
        }
        .buttonStyle(.plain)
        .accessibilityHint(n.link == nil ? "Marks it as read" : "Opens it")
        .listRowInsets(EdgeInsets(top: 12, leading: 12, bottom: 12, trailing: 16))
        .swipeActions(edge: .leading, allowsFullSwipe: true) {
            Button {
                withAnimation(DNMotion.resolve(DNMotion.snappy)) {
                    if n.isRead { store.profileMarkUnread(n.id) } else { store.markRead(n.id) }
                }
            } label: {
                Label(n.isRead ? "Unread" : "Read", systemImage: n.isRead ? "envelope.badge" : "envelope.open")
            }
            .tint(DNColor.actionAccent)
        }
        .accessibilityAction(named: n.isRead ? "Mark as unread" : "Mark as read") {
            if n.isRead { store.profileMarkUnread(n.id) } else { store.markRead(n.id) }
        }
    }
}

private struct ProfileNotificationRow: View {
    var item: NotificationItem

    private var time: String {
        switch DemoDate.daysFromToday(item.date) {
        case 0: DemoDate.time(item.date)
        default: DemoDate.relative(item.date)
        }
    }

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Circle()
                .fill(item.isRead ? Color.clear : DNColor.actionAccent)
                .frame(width: 8, height: 8)
                .padding(.top, 16)
                .accessibilityHidden(true)
            HueIcon(symbol: item.symbol, hue: item.hue, size: 40)
                .padding(.trailing, 4)
            VStack(alignment: .leading, spacing: 2) {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(item.title)
                        .dnFont(.rowTitle)
                        .fontWeight(item.isRead ? .medium : .semibold)
                        .foregroundStyle(DNColor.textPrimary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(time)
                        .dnFont(.caption1)
                        .foregroundStyle(item.isRead ? DNColor.textSecondary : DNColor.textLink)
                        .fixedSize()
                }
                Text(item.body)
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if item.link != nil {
                Image(systemName: "chevron.forward")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(DNColor.listRowChevron)
                    .frame(maxHeight: .infinity)
                    .accessibilityHidden(true)
            }
        }
        .contentShape(.rect)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(item.isRead ? "" : "Unread. ")\(item.title). \(item.body). \(time)")
    }
}
