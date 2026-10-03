import SwiftUI

/// DubaiNow's own assistant, "like Siri": it covers the whole UI, but you never leave the
/// screen you're on. The logo is its face, top centre; one glass card hangs from it; your side
/// (suggestions, the ask bar, the one confirm) is at the bottom, under your thumb.
/// Paper C1 row 11 (V83-0, W1D-0, RUW-0 … UXH-0). Scripted for the demo.
///
/// Shown by `RootView` when `router.isAssistantPresented`. `router.assistantSeed`:
/// `"__listen__"` = opened by hold-to-talk; any other text = a question to run straight away.
struct AssistantOverlay: View {
    @Environment(Router.self) private var router
    @Environment(DemoStore.self) private var store
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var engine = AssistantEngine()
    @State private var awake = false
    @FocusState private var fieldFocused: Bool
    /// "Start scanning": the tracker's passport camera, presented over the assistant.
    @State private var scan: ActivityCaptureTarget?
    @State private var started = false

    var body: some View {
        GeometryReader { geo in
            let faceY: CGFloat = 27
            let cardTop = faceY + 45
            ZStack(alignment: .top) {
                AssistantBackdrop(edge: edgeIntensity, showsFrost: engine.phase != .note)
                    .opacity(engine.phase == .note ? 0 : (awake ? 1 : 0))
                    .contentShape(.rect)
                    .onTapGesture {
                        if fieldFocused { fieldFocused = false } else { engine.close() }
                    }
                    .allowsHitTesting(engine.phase != .note)

                if engine.phase != .note {
                    if let card = engine.card {
                        cardView(card, maxHeight: max(160, geo.size.height - cardTop - bottomReserve))
                            .padding(.horizontal, 16)
                            .padding(.top, cardTop)
                            .id(card.id)
                            .transition(.asymmetric(
                                insertion: .scale(scale: 0.92, anchor: .top).combined(with: .opacity).combined(with: .offset(y: -18)),
                                removal: .opacity
                            ))
                    }

                    AssistantBottomBar(engine: engine, suggestions: engine.suggestions(store: store, router: router), fieldFocus: $fieldFocused)
                        .padding(.horizontal, DNSpace.insetPage)
                        .padding(.bottom, fieldFocused ? 8 : (geo.safeAreaInsets.bottom > 0 ? -5 : 12))
                        .frame(maxHeight: .infinity, alignment: .bottom)
                        .opacity(awake ? 1 : 0)
                        .offset(y: awake || reduceMotion ? 0 : 40)

                    AssistantFace(state: engine.face, progress: engine.progress, isAwake: awake)
                        .position(x: geo.size.width / 2, y: faceY)
                        .allowsHitTesting(false)
                } else if let note = engine.note {
                    AssistantNoteView(note: note) { engine.openNote() }
                        .position(x: geo.size.width / 2, y: faceY)
                        .transition(.scale(scale: 0.4).combined(with: .opacity))
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityAddTraits(engine.phase == .note ? [] : .isModal)
        .accessibilityAction(.escape) { engine.close() }
        .activityPassportCapture(scan: $scan, upload: .constant(nil)) { sent in
            // The photo went to the entity, so the assistant's job is done. Closed without sending: stay open.
            if sent { engine.close() }
        }
        .onAppear {
            // Once per opening: closing the camera can bring the overlay back on screen.
            guard !started else { return }
            started = true
            engine.attach(store: store, router: router)
            let target = $scan
            engine.onScanPassport = { id in target.wrappedValue = ActivityCaptureTarget(id: id) }
            withAnimation(DNMotion.resolve(DNMotion.bouncy)) { awake = true }
            AccessibilityNotification.Announcement("DubaiNow assistant").post()
            engine.start(seed: router.assistantSeed)
        }
        .onChange(of: engine.phase) { _, phase in
            if phase != .idle && phase != .card { fieldFocused = false }
        }
    }

    /// Space kept for your side under the card.
    private var bottomReserve: CGFloat { engine.confirm != nil ? 112 : 96 }

    /// The edge light calms when it's your turn and goes out when it's quiet.
    private var edgeIntensity: Double {
        switch engine.face {
        case .yourTurn: 0.55
        case .quiet, .resting: 0
        default: 1
        }
    }

    private func cardView(_ card: AssistantCard, maxHeight: CGFloat) -> some View {
        let view = AssistantCardView(
            card: card,
            english: engine.showsEnglish,
            onToggleEnglish: {
                DNHaptics.selection()
                withAnimation(DNMotion.resolve(DNMotion.smooth)) { engine.showsEnglish.toggle() }
            },
            onStop: { engine.stop() },
            onTakeOver: { engine.takeOver() },
            onAction: { engine.perform($0) },
            onReceipt: { router.present(.receipt($0)) },
            onRate: { engine.rate($0, receiptID: $1) }
        )
        return ViewThatFits(in: .vertical) {
            view
            ScrollView { view }
                .scrollBounceBehavior(.basedOnSize)
                .clipShape(.rect(cornerRadius: 30, style: .continuous))
        }
        .frame(maxHeight: maxHeight, alignment: .top)
    }
}

/// After a task, the orb shrinks back into the mark and the mark stretches into a note for
/// 5 s ("Patrol renewed", Paper UFG-0). Tap = receipt. Everything else is live again.
private struct AssistantNoteView: View {
    var note: AssistantNote
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 22))
                    .foregroundStyle(DNColor.white, DNColor.actionAccent)
                Text(note.text)
                    .dnFont(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                    .lineLimit(1)
            }
            .padding(.leading, 10)
            .padding(.trailing, 15)
            .frame(height: 44)
            .background(DNColor.white.opacity(0.6), in: .capsule)
            .glassEffect(.regular.tint(DNColor.white.opacity(0.5)).interactive(), in: .capsule)
            .shadow(color: DNColor.blue950.opacity(0.14), radius: 13, y: 10)
            .contentShape(.capsule)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(note.text)
        .accessibilityHint(note.receiptID == nil ? "" : "Shows the receipt")
    }
}
