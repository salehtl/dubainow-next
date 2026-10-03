import SwiftUI

/// One Home widget (an in-app card, not WidgetKit). Paper C1 · 02 Widgets.
///
/// It fills whatever frame the parent gives it (Home: small = half width square, medium = full width
/// × small height, large = full width square) and draws every `WidgetKind` from the persona's live
/// state. The whole card is one button that opens the right place; the quick action (+ Extend,
/// + Top up…) is its own button on top. Touch and hold opens the menu: quick action, Open, Size,
/// Edit Home, Remove.
struct WidgetView: View {
    var widget: HomeWidget

    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(ToastCenter.self) private var toasts
    @Environment(\.widgetIsPreview) private var isPreview
    @Environment(\.widgetEditHomeAction) private var editHomeAction
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var isPressed = false

    private var shape: RoundedRectangle { RoundedRectangle(cornerRadius: DNRadius.widget, style: .continuous) }
    private var layoutSize: WidgetSize { widget.kind.widgetLayoutSize(for: widget.size) }

    var body: some View {
        TimelineView(.periodic(from: .now, by: tickInterval)) { context in
            let real = context.date
            let shown = store.hourOverride == nil ? real : store.now
            card(real: real, shown: shown)
        }
    }

    /// Countdowns tick every second; slower facts refresh every minute.
    private var tickInterval: TimeInterval {
        switch widget.kind {
        case .parking, .metro: 1
        case .prayer: 15
        default: 60
        }
    }

    // MARK: Card

    private func card(real: Date, shown: Date) -> some View {
        let spec = WidgetSpec.make(kind: widget.kind, size: layoutSize, store: store, real: real, shown: shown)
        return Button {
            open(spec.open)
        } label: {
            WidgetContent(kind: widget.kind, size: layoutSize, real: real, shown: shown)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .background { spec.surface.fill }
                .clipShape(shape)
                .overlay {
                    if let edge = spec.edge { shape.strokeBorder(edge, lineWidth: 1.5) }
                }
                .contentShape(shape)
        }
        .buttonStyle(WidgetPressReportingStyle(isPressed: $isPressed))
        .accessibilityLabel(spec.summary)
        .accessibilityHint(isPreview ? "" : spec.hint)
        .accessibilityActions {
            if !isPreview {
                if let quick = spec.quick {
                    Button(label(for: quick)) { perform(quick) }
                }
                if widget.kind.widgetSizes.count > 1 {
                    ForEach(widget.kind.widgetSizes.filter { $0 != widget.size }, id: \.self) { size in
                        Button("Make \(size.widgetLabel.lowercased())") { store.resizeWidget(widget, to: size) }
                    }
                }
                Button("Remove widget") { remove() }
            }
        }
        .overlayPreferenceValue(WidgetQuickSlotKey.self) { slots in
            GeometryReader { proxy in
                ForEach(Array(slots.enumerated()), id: \.offset) { _, slot in
                    let rect = proxy[slot.anchor]
                    Button(label(for: slot.intent)) { perform(slot.intent) }
                        .buttonStyle(WidgetQuickButtonStyle(look: slot.look))
                        .frame(width: rect.width, height: rect.height)
                        .position(x: rect.midX, y: rect.midY)
                        .accessibilityHidden(isPreview)
                }
            }
        }
        .scaleEffect(isPressed && !reduceMotion ? DNMotion.pressScale : 1)
        .brightness(isPressed ? -0.03 : 0)
        .animation(DNMotion.snappy, value: isPressed)
        .dnElevation()
        .contentShape(.contextMenuPreview, shape)
        .contextMenu { if !isPreview { menu(spec) } }
        .allowsHitTesting(!isPreview)
        .accessibilityElement(children: .contain)
        .dynamicTypeSize(...DynamicTypeSize.xxxLarge)
    }

    // MARK: Menu (touch and hold)

    @ViewBuilder
    private func menu(_ spec: WidgetSpec) -> some View {
        if let quick = spec.quick {
            Button { perform(quick) } label: { Label(label(for: quick), systemImage: "plus.circle") }
        }
        Button { open(spec.open) } label: {
            Label("Open \(widget.kind.title)", systemImage: "arrow.up.forward.app")
        }
        if widget.kind.widgetSizes.count > 1 {
            Divider()
            Picker("Size", selection: Binding(
                get: { widget.size },
                set: { new in
                    DNHaptics.selection()
                    store.resizeWidget(widget, to: new)
                }
            )) {
                ForEach(widget.kind.widgetSizes, id: \.self) { size in
                    Label(size.widgetLabel, systemImage: size.widgetSymbol).tag(size)
                }
            }
            .pickerStyle(.palette)
        }
        Divider()
        Button { editHome() } label: { Label("Edit Home", systemImage: "slider.horizontal.3") }
        Divider()
        Button(role: .destructive) { remove() } label: { Label("Remove widget", systemImage: "minus.circle") }
    }

    // MARK: Actions

    private func open(_ target: WidgetOpen) {
        switch target {
        case .sheet(let sheet): router.present(sheet)
        case .route(let route): navigate(route)
        case .service(let id):
            guard let service = Catalogue.shared.service(id) else { return }
            switch service.type {
            case .standalone: navigate(.service(id))
            case .bundle: navigate(.bundle(id))
            case .journey: navigate(.journey(id))
            }
        case .comingSoon(let item): router.present(.comingSoon(item))
        }
    }

    /// Pushes on the current tab. From inside a sheet (the gallery's debug grid), closes the sheet first.
    private func navigate(_ route: Route) {
        if router.sheet != nil {
            router.dismissSheet()
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(350))
                router.push(route)
            }
        } else {
            router.push(route)
        }
    }

    private func perform(_ intent: WidgetQuickIntent) {
        switch intent {
        case .extendParking:
            router.present(.extendParking)
        case .startParking:
            router.present(.comingSoon(ComingSoon(
                title: "Pay for parking", owner: "Parkin", symbol: "p.square.fill", hue: .sky, kind: .servicesPhase
            )))
        case .topUpSalik:
            router.present(.topUpSalik)
        case .topUpNol:
            router.present(.payment(PaymentRequest(
                id: "widget-nol-\(UUID().uuidString.prefix(6))",
                title: "Top up your nol card",
                lines: [PaymentLine(title: "nol card top-up", detail: "RTA", amount: 50, symbol: "tram.fill", hue: .sky)],
                purpose: .nolTopUp(50)
            )))
        case .renewFamilyVisas:
            navigate(.bundle("renew-family-visas"))
        case .connect(let account):
            router.present(.comingSoon(ComingSoon(
                title: "Link your \(account) account",
                detail: "In the full app you confirm the link once with UAE PASS, and this widget fills in by itself.",
                owner: account, symbol: "link", hue: .sky, kind: .notInDemo
            )))
        }
    }

    private func label(for intent: WidgetQuickIntent) -> String {
        switch intent {
        case .extendParking: "Extend parking"
        case .startParking: "Start parking"
        case .topUpSalik: "Top up Salik"
        case .topUpNol: "Top up nol card"
        case .renewFamilyVisas: "Renew family visas"
        case .connect(let account): "Connect \(account)"
        }
    }

    private func remove() {
        let index = store.state.widgets.firstIndex { $0.id == widget.id } ?? store.state.widgets.count
        let removed = widget
        DNHaptics.light()
        store.removeWidget(widget)
        toasts.show("\(widget.kind.title) removed", symbol: "minus.circle.fill", undo: {
            store.widgetRestore(removed, at: index)
        })
    }

    private func editHome() {
        DNHaptics.medium()
        if let editHomeAction {
            editHomeAction()
        } else {
            NotificationCenter.default.post(name: .widgetEditHomeRequested, object: nil)
        }
    }
}

// MARK: - Content dispatch

/// The drawing for each kind at its layout size.
struct WidgetContent: View {
    var kind: WidgetKind
    var size: WidgetSize
    var real: Date
    var shown: Date

    var body: some View {
        switch kind {
        case .parking: WidgetParkingContent(size: size, now: real)
        case .salik: WidgetSalikContent(size: size)
        case .nol: WidgetNolContent()
        case .metro: WidgetMetroContent(now: real)
        case .prayer: WidgetPrayerContent(size: size, now: shown)
        case .weather: WidgetWeatherContent()
        case .energy: WidgetEnergyContent()
        case .billsAndFines: WidgetBillsContent(size: size, now: real)
        case .familyVisas: WidgetFamilyContent(size: size)
        case .journey(let id): WidgetJourneyContent(journeyID: id, size: size)
        case .visaTracker(let id): WidgetRequestContent(requestID: id)
        case .certificates: WidgetCertificatesContent()
        }
    }
}

// MARK: - Spec (surface, summary, actions)

/// Where tapping a widget goes.
enum WidgetOpen {
    case sheet(SheetRoute)
    case route(Route)
    case service(String)
    case comingSoon(ComingSoon)
}

/// Everything about a widget that isn't drawing: its surface, the one sentence VoiceOver reads,
/// its quick action and where it opens.
struct WidgetSpec {
    var surface: WidgetSurface = .plain
    var edge: Color? = nil
    var summary: String
    var hint: String = ""
    var quick: WidgetQuickIntent? = nil
    var open: WidgetOpen

    static func make(kind: WidgetKind, size: WidgetSize, store: DemoStore, real: Date, shown: Date) -> WidgetSpec {
        switch kind {
        case .parking: parking(store: store, now: real)
        case .salik: salik(store: store)
        case .nol: nol(store: store)
        case .metro: metro(store: store, now: real)
        case .prayer: prayer(now: shown)
        case .weather: weather(store: store)
        case .energy: energy(store: store)
        case .billsAndFines: bills(store: store, now: real)
        case .familyVisas: family(store: store, size: size)
        case .journey(let id): journey(id: id, store: store)
        case .visaTracker(let id): request(id: id, store: store)
        case .certificates:
            WidgetSpec(summary: "Certificates and letters. Nothing in progress.", open: .service("get-certificates"))
        }
    }
}
