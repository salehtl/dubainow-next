import SwiftUI

/// All drafts (Paper EL5-0): kept for 30 days, swipe left to pin or delete.
struct DraftsView: View {
    @Environment(DemoStore.self) private var store
    @State private var draftToDelete: DraftItem?

    var body: some View {
        List {
            ActivityDraftsSections(expanded: true, onDelete: { draftToDelete = $0 })
        }
        .listStyle(.insetGrouped)
        .listSectionSpacing(12)
        .scrollContentBackground(.hidden)
        .dnCanvasBackground()
        .overlay {
            if store.state.drafts.isEmpty {
                ActivityEmptyState(
                    symbol: "doc.badge.clock",
                    title: "No drafts",
                    message: "If you leave a service before the end, we keep your answers here for 30 days."
                )
            }
        }
        .navigationTitle("Drafts")
        .navigationBarTitleDisplayMode(.large)
        .dnPushedToolbar()
        .activityDraftDeletion($draftToDelete)
    }
}

// MARK: - Sections (used by the Activity tab and the Drafts screen)

/// Drafts as list sections. Compact rows in the All list; expanded cards in the Drafts filter.
/// Swipe left (or VoiceOver custom actions, or touch and hold) to pin or delete.
struct ActivityDraftsSections: View {
    var expanded: Bool
    var showsHeader: Bool = false
    var onDelete: (DraftItem) -> Void

    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    private let pins = ActivityDraftPins.shared

    var body: some View {
        let drafts = pins.sorted(store.state.drafts)
        if expanded {
            if !drafts.isEmpty {
                Section {
                    ActivityInfoNotice(text: "Drafts are saved on every step and kept for 30 days after you last change them. We remind you 3 days before one is deleted.")
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets())
                }
            }
            ForEach(drafts) { draft in
                Section {
                    ActivityDraftCard(
                        draft: draft,
                        isPinned: pins.isPinned(draft.id),
                        onContinue: { open(draft) },
                        onStartOver: { open(draft) }
                    )
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
                    .modifier(ActivityDraftActions(draft: draft, isPinned: pins.isPinned(draft.id), onContinue: { open(draft) }, onPin: { pins.toggle(draft.id) }, onDelete: { onDelete(draft) }))
                } footer: {
                    if draft.id == drafts.last?.id {
                        Text("Swipe left on a draft to pin or delete it. Deleting asks to confirm, then you can undo for 5 seconds.")
                            .dnFont(.footnote)
                            .foregroundStyle(DNColor.textSecondary)
                            .padding(.top, 8)
                    }
                }
            }
        } else if !drafts.isEmpty {
            Section {
                ForEach(drafts) { draft in
                    Button { open(draft) } label: {
                        ActivityDraftRow(draft: draft, isPinned: pins.isPinned(draft.id))
                    }
                    .modifier(ActivityDraftActions(draft: draft, isPinned: pins.isPinned(draft.id), onContinue: { open(draft) }, onPin: { pins.toggle(draft.id) }, onDelete: { onDelete(draft) }))
                }
            } header: {
                ActivitySectionHeader(title: "Drafts") {
                    Text("kept for 30 days")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                }
                .activitySectionHeader()
            }
        }
    }

    private func open(_ draft: DraftItem) {
        router.present(.comingSoon(ComingSoon(
            title: draft.title,
            detail: "You've done \(draft.stepsDone) of \(draft.stepsTotal) steps. Picking up a draft opens the service itself, which comes in the next phase of the demo.",
            owner: draft.activityOwner,
            symbol: draft.symbol,
            hue: draft.hue,
            kind: .servicesPhase
        )))
    }
}

/// Swipe actions, context menu and VoiceOver actions for a draft.
private struct ActivityDraftActions: ViewModifier {
    let draft: DraftItem
    let isPinned: Bool
    var onContinue: () -> Void
    var onPin: () -> Void
    var onDelete: () -> Void

    func body(content: Content) -> some View {
        content
            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                Button(action: onDelete) {
                    Label("Delete", systemImage: "trash")
                }
                .tint(DNColor.actionDestructive)
                Button(action: onPin) {
                    Label(isPinned ? "Unpin" : "Pin", systemImage: isPinned ? "pin.slash" : "pin")
                }
                .tint(DNColor.neutral500)
            }
            .contextMenu {
                Button(action: onContinue) { Label("Continue", systemImage: "arrow.right.circle") }
                Button(action: onPin) { Label(isPinned ? "Unpin" : "Pin", systemImage: isPinned ? "pin.slash" : "pin") }
                Divider()
                Button(role: .destructive, action: onDelete) { Label("Delete draft", systemImage: "trash") }
            }
    }
}

// MARK: - Delete with confirmation, then Undo

extension View {
    func activityDraftDeletion(_ draft: Binding<DraftItem?>) -> some View {
        modifier(ActivityDraftDeletion(draft: draft))
    }

    /// Section headers flush with the group edge, as in Paper.
    func activitySectionHeader() -> some View {
        self
            .textCase(nil)
            .listRowInsets(EdgeInsets(top: 0, leading: 4, bottom: 8, trailing: 4))
    }
}

private struct ActivityDraftDeletion: ViewModifier {
    @Binding var draft: DraftItem?
    @Environment(DemoStore.self) private var store
    @Environment(ToastCenter.self) private var toasts

    func body(content: Content) -> some View {
        content.confirmationDialog(
            "Delete this draft?",
            isPresented: Binding(get: { draft != nil }, set: { if !$0 { draft = nil } }),
            titleVisibility: .visible,
            presenting: draft
        ) { d in
            Button("Delete draft", role: .destructive) { delete(d) }
            Button("Keep it", role: .cancel) {}
        } message: { d in
            Text("You'll lose the \(d.stepsDone) of \(d.stepsTotal) steps you've done for \(d.title).")
        }
    }

    private func delete(_ d: DraftItem) {
        let index = store.state.drafts.firstIndex(of: d) ?? 0
        store.deleteDraft(d)
        DNHaptics.light()
        toasts.show("Draft deleted", undo: { store.activityRestoreDraft(d, at: index) })
    }
}
