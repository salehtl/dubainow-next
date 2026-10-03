import SwiftUI

/// Request tracker (Paper EE3-0): status chip, title, copyable reference, what you need to know,
/// the steps (done ✓, current numbered, upcoming outlined, the outcome with a star), and the
/// current step's actions: scan or upload a passport, or pay the fees.
struct RequestTrackerView: View {
    var requestID: String

    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(ToastCenter.self) private var toasts
    @State private var scan: ActivityCaptureTarget?
    @State private var upload: ActivityCaptureTarget?
    @State private var showsCancel = false
    @State private var showsSent = false

    var body: some View {
        Group {
            if let request = store.request(requestID) {
                content(request)
            } else {
                ContentUnavailableView(
                    "Request not found",
                    systemImage: "doc.questionmark",
                    description: Text("It may belong to another resident, or the demo was reset.")
                )
                .dnCanvasBackground()
            }
        }
        .navigationTitle(store.request(requestID)?.title ?? "Request")
        .navigationBarTitleDisplayMode(.inline)
        .dnPushedToolbar()
        .toolbar {
            if let request = store.request(requestID) {
                ToolbarItem(placement: .topBarTrailing) { moreMenu(request) }
            }
        }
        .activityPassportCapture(scan: $scan, upload: $upload)
        .confirmationDialog("Cancel this application?", isPresented: $showsCancel, titleVisibility: .visible) {
            Button("Cancel application", role: .destructive) {
                present(.notInDemo, title: "Cancel this application", detail: "Cancelling an application with GDRFA isn't part of this demo. Nothing has been cancelled.")
            }
            Button("Keep it", role: .cancel) {}
        } message: {
            Text("Any fees you've paid may not be refunded.")
        }
        .sheet(isPresented: $showsSent) {
            if let request = store.request(requestID) {
                ActivitySentSheet(request: request)
            }
        }
        .task {
            switch ActivityDemoArg.value {
            case "scan", "shoot", "send":
                try? await Task.sleep(for: .milliseconds(600))
                scan = ActivityCaptureTarget(id: requestID)
            case "uploaded", "fees":
                if store.request(requestID)?.activityCurrentStep?.activityKind == .upload { store.advanceRequest(requestID) }
                if ActivityDemoArg.value == "fees", let r = store.request(requestID) {
                    try? await Task.sleep(for: .milliseconds(600))
                    router.present(.payment(r.activityFeesRequest))
                }
            default:
                break
            }
        }
    }

    // MARK: Content

    private func content(_ r: RequestItem) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                header(r)
                if let notice = r.notice {
                    noticeCard(notice)
                }
                steps(r)
                    .padding(.top, 4)
                links(r)
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.top, 8)
            .padding(.bottom, 32)
            .animation(DNMotion.resolve(DNMotion.smooth), value: r)
        }
        .background(alignment: .top) {
            LinearGradient(colors: [r.hue.start, DNColor.surfaceSecondary.opacity(0)], startPoint: .top, endPoint: .bottom)
                .frame(height: 360)
                .ignoresSafeArea(edges: .top)
        }
        .dnCanvasBackground()
    }

    private func header(_ r: RequestItem) -> some View {
        let status = r.activityStatus(chip: true)
        return VStack(alignment: .leading, spacing: 10) {
            StatusChip(text: status.text, tone: status.tone, onWhite: true)
            Text(r.title)
                .dnFont(.title1)
                .foregroundStyle(DNColor.textPrimary)
                .accessibilityAddTraits(.isHeader)
            Button {
                UIPasteboard.general.string = r.reference
                toasts.show("Reference copied", symbol: "doc.on.doc.fill")
            } label: {
                HStack(spacing: 8) {
                    Text("Reference")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                    Text(r.reference)
                        .dnFont(.footnote)
                        .monospaced()
                        .foregroundStyle(DNColor.textPrimary)
                    Image(systemName: "doc.on.doc")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(DNColor.textLink)
                }
                .frame(minHeight: 32)
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Reference \(r.reference.map { String($0) }.joined(separator: " "))")
            .accessibilityHint("Copies the reference")
        }
    }

    private func noticeCard(_ notice: (title: String, body: String)) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(notice.title)
                .dnFont(.rowTitle)
                .fontWeight(.semibold)
                .foregroundStyle(DNColor.textPrimary)
            Text(notice.body)
                .dnFont(.subheadline)
                .foregroundStyle(DNColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .dnCard()
        .accessibilityElement(children: .combine)
    }

    // MARK: Steps

    private func steps(_ r: RequestItem) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(r.steps.enumerated()), id: \.offset) { index, step in
                ActivityStepRow(
                    number: index + 1,
                    step: step,
                    connector: connectorColor(after: step),
                    isLast: false,
                    currentDetail: currentDetail(r, step: step)
                ) {
                    if step.state == .current { stepActions(r, step: step) }
                }
            }
            ActivityOutcomeRow(outcome: r.activityOutcome, achieved: { if case .done = r.status { return true }; return false }())
        }
    }

    private func connectorColor(after step: RequestStep) -> Color {
        step.state == .done ? DNColor.statusSuccessForeground : DNColor.neutral300
    }

    private func currentDetail(_ r: RequestItem, step: RequestStep) -> String? {
        guard step.state == .current else { return nil }
        switch step.activityKind {
        case .upload:
            if case .yourTurn(let due) = r.status { return "Your turn · by \(DemoDate.long(due))" }
            return "Your turn"
        case .pay:
            return "Your turn · the exact amount is \(Money.aed(r.activityFeesRequest.total))"
        case .waiting:
            return nil
        }
    }

    @ViewBuilder private func stepActions(_ r: RequestItem, step: RequestStep) -> some View {
        switch step.activityKind {
        case .upload:
            ActivityAdaptiveStack {
                Button {
                    scan = ActivityCaptureTarget(id: r.id)
                } label: {
                    Label("Scan passport", systemImage: "camera")
                }
                .buttonStyle(.dn(.primary, size: .medium))
                Button("Upload file") { upload = ActivityCaptureTarget(id: r.id) }
                    .buttonStyle(.dn(.secondary, size: .medium, fullWidth: false))
            }
        case .pay:
            Button {
                router.present(.payment(r.activityFeesRequest))
            } label: {
                Label("Pay \(Money.aed(r.activityFeesRequest.total))", systemImage: "creditcard")
            }
            .buttonStyle(.dn(.primary, size: .medium))
        case .waiting:
            EmptyView()
        }
    }

    // MARK: Links

    private func links(_ r: RequestItem) -> some View {
        DNListGroup {
            Button { showsSent = true } label: {
                DNListRow(title: "What you've sent")
            }
            .buttonStyle(.plain)
            Button {
                present(.notInDemo, title: "Get help with this application", detail: "Chat, call-backs and help from \(r.entity) come in a later phase of the demo.")
            } label: {
                DNListRow(title: "Get help with this application")
            }
            .buttonStyle(.plain)
            if canCancel(r) {
                Button { showsCancel = true } label: {
                    DNListRow(title: "Cancel this application", showsChevron: false, titleColor: DNColor.actionDestructive)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func canCancel(_ r: RequestItem) -> Bool {
        switch r.status {
        case .yourTurn, .inReview: true
        case .approved, .done: false
        }
    }

    private func moreMenu(_ r: RequestItem) -> some View {
        Menu {
            Button {
                UIPasteboard.general.string = r.reference
                toasts.show("Reference copied", symbol: "doc.on.doc.fill")
            } label: {
                Label("Copy reference", systemImage: "doc.on.doc")
            }
            ShareLink(item: shareText(r)) {
                Label("Share status", systemImage: "square.and.arrow.up")
            }
            if r.serviceID == "renew-residence-visa" {
                Button {
                    router.openAssistant(seed: "When will my visa renewal be done?")
                } label: {
                    Label("Ask your assistant", systemImage: "sparkles")
                }
            }
            if canCancel(r) {
                Divider()
                Button(role: .destructive) { showsCancel = true } label: {
                    Label("Cancel this application", systemImage: "xmark.circle")
                }
            }
        } label: {
            Image(systemName: "ellipsis")
        }
        .accessibilityLabel("More")
    }

    private func shareText(_ r: RequestItem) -> String {
        "\(r.title) (\(r.entity))\n\(r.activityStatus().text)\nReference \(r.reference)"
    }

    private func present(_ kind: ComingSoon.Kind, title: String, detail: String? = nil) {
        guard let r = store.request(requestID) else { return }
        router.present(.comingSoon(ComingSoon(title: title, detail: detail, owner: r.entity, symbol: r.symbol, hue: r.hue, kind: kind)))
    }
}

// MARK: - Step row

/// One step on the vertical line: a 28 pt marker, the connector below it, title, detail, actions.
struct ActivityStepRow<Actions: View>: View {
    let number: Int
    let step: RequestStep
    let connector: Color
    var isLast: Bool
    var currentDetail: String?
    @ViewBuilder var actions: Actions

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            VStack(spacing: 0) {
                marker
                if !isLast {
                    Rectangle()
                        .fill(connector)
                        .frame(width: 2)
                        .frame(maxHeight: .infinity)
                }
            }
            .frame(width: 28)

            VStack(alignment: .leading, spacing: 10) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(step.title)
                        .dnFont(.rowTitle)
                        .fontWeight(step.state == .current ? .bold : .semibold)
                        .foregroundStyle(DNColor.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                    if let currentDetail {
                        Text(currentDetail)
                            .dnFont(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundStyle(DNColor.statusWarningForeground)
                            .fixedSize(horizontal: false, vertical: true)
                    } else {
                        Text(displayDetail)
                            .dnFont(.subheadline)
                            .foregroundStyle(DNColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .accessibilityElement(children: .combine)
                .accessibilityLabel("Step \(number), \(stateLabel). \(step.title). \(currentDetail ?? displayDetail)")
                actions
            }
            .padding(.top, 3)
            .padding(.bottom, 20)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    /// A step that's done no longer says "Your turn".
    private var displayDetail: String {
        if step.state == .done, step.detail.hasPrefix("Your turn") {
            return step.activityKind == .upload ? "Sent · being checked" : "Done"
        }
        return step.detail
    }

    private var stateLabel: String {
        switch step.state {
        case .done: "done"
        case .current: "current"
        case .upcoming: "to do"
        }
    }

    @ViewBuilder private var marker: some View {
        switch step.state {
        case .done:
            Circle()
                .fill(DNColor.statusSuccessForeground)
                .overlay(
                    Image(systemName: "checkmark")
                        .font(.system(size: 12, weight: .heavy))
                        .foregroundStyle(DNColor.white)
                )
                .frame(width: 28, height: 28)
                .transition(.scale.combined(with: .opacity))
                .accessibilityHidden(true)
        case .current:
            Circle()
                .fill(DNColor.statusWarningForeground)
                .overlay(
                    Text("\(number)")
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundStyle(DNColor.white)
                )
                .frame(width: 28, height: 28)
                .background(Circle().fill(DNColor.signSun.opacity(0.3)).padding(-5))
                .accessibilityHidden(true)
        case .upcoming:
            Circle()
                .fill(DNColor.surfacePrimary)
                .overlay(Circle().strokeBorder(DNColor.neutral300, lineWidth: 1.5))
                .overlay(
                    Text("\(number)")
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundStyle(DNColor.textPrimary)
                )
                .frame(width: 28, height: 28)
                .accessibilityHidden(true)
        }
    }
}

/// The last row: what you get at the end, marked with a star.
struct ActivityOutcomeRow: View {
    let outcome: (title: String, detail: String)
    var achieved: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            Circle()
                .fill(achieved ? DNColor.statusSuccessForeground : DNColor.surfacePrimary)
                .overlay(Circle().strokeBorder(achieved ? .clear : DNColor.neutral300, lineWidth: 1.5))
                .overlay(
                    Image(systemName: achieved ? "star.fill" : "star")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(achieved ? DNColor.white : DNColor.textSecondary)
                )
                .frame(width: 28, height: 28)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(outcome.title)
                    .dnFont(.rowTitle)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                Text(outcome.detail)
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.top, 3)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("At the end: \(outcome.title). \(outcome.detail)")
    }
}

/// Side by side; stacked at accessibility text sizes.
struct ActivityAdaptiveStack<Content: View>: View {
    @ViewBuilder var content: Content
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        let layout = typeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: 8))
            : AnyLayout(HStackLayout(spacing: 8))
        layout { content }
    }
}

// MARK: - What you've sent

/// What the resident has already sent for this request (from the steps that are done).
struct ActivitySentSheet: View {
    let request: RequestItem
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                        HStack(spacing: 12) {
                            DNRowIcon(symbol: item.symbol, color: DNColor.textPrimary)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.title)
                                    .dnFont(.rowTitle)
                                    .foregroundStyle(DNColor.textPrimary)
                                Text(item.detail)
                                    .dnFont(.footnote)
                                    .foregroundStyle(DNColor.textSecondary)
                            }
                        }
                        .padding(.vertical, 2)
                        .accessibilityElement(children: .combine)
                    }
                } footer: {
                    Text("\(request.entity) has these. Reference \(request.reference).")
                }
            }
            .overlay {
                if items.isEmpty {
                    ContentUnavailableView("Nothing sent yet", systemImage: "tray", description: Text("What you send for this application shows here."))
                }
            }
            .navigationTitle("What you've sent")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(role: .close) { dismiss() }.tint(DNColor.textPrimary)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    private var items: [(title: String, detail: String, symbol: String)] {
        request.steps.filter { $0.state == .done }.map { step in
            let t = step.title.lowercased()
            let detail = step.detail.components(separatedBy: " · ").first ?? step.detail
            if t.contains("medical") { return ("Medical fitness result", "Shared by DHA · \(detail)", "cross.case.fill") }
            if t.contains("insurance") { return ("Health insurance policy", "Found automatically · \(detail)", "shield.fill") }
            if t.contains("passport") { return ("Passport copy", "Photo page · sent \(DemoDate.relative(.now).lowercased())", "person.text.rectangle.fill") }
            if t.contains("eye") { return ("Eye test result", "Shared by the optician · \(detail)", "eye.fill") }
            if t.hasPrefix("pay") { return ("Payment", detail, "creditcard.fill") }
            if t.contains("tell") { return ("What happened", "Your description · \(detail)", "text.bubble.fill") }
            return (step.title, step.detail, "doc.fill")
        }
    }
}
