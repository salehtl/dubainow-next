import SwiftUI

/// One glass card hangs from the face: what you asked, its answer, and the task it's running
/// (Paper TA7-0 / TQX-0 / U2V-0 / UXH-0). Cards drop down out of the face.
struct AssistantCardView: View {
    var card: AssistantCard
    var english: Bool
    var onToggleEnglish: () -> Void
    var onStop: () -> Void
    var onTakeOver: () -> Void
    var onAction: (AssistantFollowUp) -> Void
    var onReceipt: (String) -> Void
    var onRate: (HappinessFace, String) -> Void

    @AccessibilityFocusState private var headlineFocused: Bool
    @State private var rating: HappinessFace?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            said
            if let surface = card.surface {
                AssistantSurfaceView(surface: surface, english: english, onReceipt: onReceipt)
                    .padding(.horizontal, 8)
                    .transition(.opacity.combined(with: .scale(scale: 0.98, anchor: .top)))
            }
            if let prompt = card.rating {
                ratingView(prompt)
            }
            controls
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DNColor.white.opacity(0.62), in: .rect(cornerRadius: 30, style: .continuous))
        .glassEffect(.regular.tint(DNColor.white.opacity(0.5)), in: .rect(cornerRadius: 30, style: .continuous))
        .shadow(color: DNColor.blue950.opacity(0.16), radius: 18, y: 14)
        .accessibilityElement(children: .contain)
        .onChange(of: card.id) { _, _ in rating = nil }
        .onChange(of: card.headline) { _, _ in focusHeadline() }
        .onAppear { focusHeadline() }
    }

    private func focusHeadline() {
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(150))
            headlineFocused = true
        }
    }

    // MARK: Said and reply

    private var said: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text("“\(card.quote.resolved(english: english))”")
                    .dnFont(.subheadline)
                    .fontWeight(.medium)
                    .foregroundStyle(DNColor.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityLabel("You asked: \(card.quote.resolved(english: english))")
                if card.isMandarin {
                    Button(english ? "显示中文" : "Show in English", action: onToggleEnglish)
                        .dnFont(.footnote)
                        .fontWeight(.semibold)
                        .foregroundStyle(DNColor.textLink)
                        .buttonStyle(.plain)
                        .frame(minHeight: 44)
                        .padding(.vertical, -12)
                        .contentShape(.rect)
                        .accessibilityLabel(english ? "Show in Chinese" : "Show in English")
                }
            }
            if card.isThinking {
                AssistantThinkingDots()
                    .frame(height: 25)
                    .accessibilityLabel("Thinking")
            } else {
                Text(card.headline.resolved(english: english))
                    .dnFont(.title3)
                    .foregroundStyle(DNColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                    .contentTransition(.opacity)
                    .accessibilityFocused($headlineFocused)
                    .accessibilityAddTraits(.isHeader)
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 18)
        .padding(.bottom, 14)
    }

    // MARK: Happiness (inline)

    private func ratingView(_ prompt: AssistantRatingPrompt) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(prompt.question)
                    .dnFont(.rowTitle)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                Text(rating == nil ? prompt.detail : prompt.thanks)
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                    .contentTransition(.opacity)
            }
            HappinessFacesPicker(
                selection: Binding(
                    get: { rating },
                    set: { face in
                        rating = face
                        if let face { onRate(face, prompt.receiptID) }
                    }
                ),
                size: 56
            )
        }
        .padding(.horizontal, 20)
        .padding(.top, 14)
        .padding(.bottom, 16)
    }

    // MARK: Controls

    @ViewBuilder private var controls: some View {
        switch card.controls {
        case .none:
            if card.surface != nil && card.rating == nil { Spacer().frame(height: 8) }
        case .task:
            HStack(spacing: 8) {
                Button(action: onStop) {
                    HStack(spacing: 8) {
                        Image(systemName: "stop.fill").font(.system(size: 11, weight: .bold))
                        Text(english || !card.isMandarin ? "Stop" : "停止")
                    }
                    .foregroundStyle(DNColor.textPrimary)
                    .modifier(AssistantSoftButton())
                }
                .buttonStyle(.dnPressable)
                .accessibilityLabel("Stop")
                .accessibilityHint("Nothing is paid or sent")
                Button(action: onTakeOver) {
                    Text(english || !card.isMandarin ? "I'll do it myself" : "我自己来")
                        .foregroundStyle(DNColor.textLink)
                        .modifier(AssistantSoftButton())
                }
                .buttonStyle(.dnPressable)
                .accessibilityLabel("I'll do it myself")
                .accessibilityHint("Opens the service at the same step")
            }
            .padding(8)
        case .action(let title, let target):
            Button(title.resolved(english: english)) { onAction(target) }
                .buttonStyle(.dn(.primary, size: .medium))
                .padding(8)
        }
    }
}

/// Stop · I'll do it myself: 44 pt, a soft tint on the glass.
private struct AssistantSoftButton: ViewModifier {
    func body(content: Content) -> some View {
        content
            .dnFont(.rowTitle)
            .fontWeight(.semibold)
            .lineLimit(1)
            .minimumScaleFactor(0.8)
            .frame(maxWidth: .infinity, minHeight: 44)
            .background(DNColor.blue950.opacity(0.06), in: .capsule)
            .contentShape(.capsule)
    }
}

private struct AssistantThinkingDots: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    var body: some View {
        TimelineView(.animation(minimumInterval: 1 / 30, paused: reduceMotion)) { context in
            let t = context.date.timeIntervalSinceReferenceDate
            HStack(spacing: 6) {
                ForEach(0..<3, id: \.self) { i in
                    Circle()
                        .fill(DNColor.blue600)
                        .frame(width: 8, height: 8)
                        .opacity(reduceMotion ? 0.6 : 0.3 + 0.7 * max(0, sin(t * 5 - Double(i) * 0.7)))
                }
            }
        }
    }
}

// MARK: - The tinted block inside the card

struct AssistantSurfaceView: View {
    var surface: AssistantSurface
    var english: Bool
    var onReceipt: (String) -> Void

    @State private var showsOriginal = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            switch surface {
            case .task(let t): task(t)
            case .payables(let items): payables(items)
            case .request(let r): request(r)
            case .journey(let j): journey(j)
            case .complaint(let b): complaint(b)
            case .list(let rows): list(rows)
            case .energy(let e): energy(e)
            }
        }
        .padding(.horizontal, 14)
        .padding(.top, 12)
        .padding(.bottom, 6)
        .background(DNColor.blue50.opacity(0.92), in: .rect(cornerRadius: 22, style: .continuous))
    }

    // MARK: Task

    @ViewBuilder private func task(_ t: AssistantTaskBlock) -> some View {
        header(symbol: t.symbol, hue: t.hue, title: t.title.resolved(english: english), subtitle: t.subtitle.resolved(english: english)) {
            switch t.status {
            case .done:
                Text("Done").foregroundStyle(DNColor.statusSuccessForeground)
            case .stopped:
                Text("Stopped").foregroundStyle(DNColor.textSecondary)
            case .working, .yourTurn:
                Text("\(t.position) of \(t.steps.count)").foregroundStyle(DNColor.textLink)
                    .contentTransition(.numericText())
            }
        }
        ForEach(t.steps) { step in
            AssistantStepRow(step: step, english: english)
                .transition(.opacity)
        }
        if t.status == .done, let id = t.receiptID {
            AssistantRule()
            HStack(spacing: 12) {
                Image(systemName: "doc.text")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(DNColor.iconSecondary)
                    .frame(width: 36)
                VStack(alignment: .leading, spacing: 0) {
                    if let paid = t.paidLine {
                        Text(paid).dnFont(.footnote).fontWeight(.semibold).foregroundStyle(DNColor.textPrimary)
                    }
                    if let ref = t.reference {
                        Text("Reference \(ref)").dnFont(.footnote).foregroundStyle(DNColor.textSecondary)
                            .lineLimit(1).minimumScaleFactor(0.8)
                    }
                }
                Spacer(minLength: 8)
                Button("Receipt") { onReceipt(id) }
                    .dnFont(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textLink)
                    .buttonStyle(.plain)
                    .frame(minWidth: 44, minHeight: 44)
                    .contentShape(.rect)
                    .accessibilityLabel("View the receipt")
            }
            .accessibilityElement(children: .combine)
        }
    }

    // MARK: Payables

    @ViewBuilder private func payables(_ items: [Payable]) -> some View {
        let total = items.reduce(Decimal(0)) { $0 + $1.amount }
        header(symbol: "creditcard.fill", hue: .sky, title: items.count == 1 ? "1 payment due" : "\(items.count) payments due", subtitle: "From your linked accounts") {
            EmptyView()
        }
        ForEach(items) { p in
            AssistantRule()
            HStack(spacing: 12) {
                HueIcon(symbol: p.symbol, hue: p.hue, size: 28)
                    .frame(width: 36)
                VStack(alignment: .leading, spacing: 0) {
                    Text(p.title).dnFont(.subheadline).foregroundStyle(DNColor.textPrimary)
                    Text([p.entity, p.due.map { DemoDate.dueLabel($0) } ?? p.note].compactMap { $0 }.joined(separator: " · "))
                        .dnFont(.footnote).foregroundStyle(DNColor.textSecondary)
                }
                Spacer(minLength: 8)
                Text(Money.aed(p.amount))
                    .dnFont(.subheadline).fontWeight(.medium).monospacedDigit()
                    .foregroundStyle(DNColor.textSecondary)
            }
            .padding(.vertical, 7)
            .accessibilityElement(children: .combine)
        }
        AssistantRule()
        HStack {
            Text("Total").dnFont(.subheadline).fontWeight(.semibold)
            Spacer()
            Text(Money.aed(total)).dnFont(.subheadline).fontWeight(.semibold).monospacedDigit()
        }
        .foregroundStyle(DNColor.textPrimary)
        .padding(.leading, 48)
        .frame(minHeight: 38)
        .accessibilityElement(children: .combine)
    }

    // MARK: Request tracker

    @ViewBuilder private func request(_ r: RequestItem) -> some View {
        let current = r.currentStepIndex ?? r.steps.count - 1
        header(symbol: r.symbol, hue: r.hue, title: r.title, subtitle: "\(r.entity) · \(r.reference)") {
            Text("\(min(current + 1, r.steps.count)) of \(r.steps.count)").foregroundStyle(DNColor.textLink)
        }
        ForEach(Array(r.steps.enumerated()), id: \.offset) { i, s in
            let state: AssistantStep.State = switch s.state {
            case .done: .done
            case .current: .yourTurn
            case .upcoming: .pending
            }
            AssistantStepRow(step: AssistantStep(id: i, title: AssistantText(s.title), state: state), english: true, detail: s.state == .current ? s.detail : nil)
        }
    }

    // MARK: Journey

    @ViewBuilder private func journey(_ j: AssistantJourneyBlock) -> some View {
        header(symbol: j.symbol, hue: j.hue, title: j.title, subtitle: j.subtitle) {
            Text("\(j.stage + 1) of \(j.stageCount)").foregroundStyle(DNColor.textLink)
        }
        SegmentedProgress(total: j.stageCount, done: j.stage)
            .padding(.bottom, 10)
        AssistantRule()
        labelled("Now", j.now, detail: j.nowDetail)
        if let next = j.next {
            AssistantRule()
            labelled("Next", next, detail: nil)
        }
    }

    private func labelled(_ label: String, _ value: String, detail: String?) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 12) {
            Text(label)
                .dnFont(.footnote).fontWeight(.semibold)
                .foregroundStyle(DNColor.textSecondary)
                .frame(width: 36, alignment: .leading)
            VStack(alignment: .leading, spacing: 0) {
                Text(value).dnFont(.subheadline).foregroundStyle(DNColor.textPrimary)
                if let detail { Text(detail).dnFont(.footnote).foregroundStyle(DNColor.textSecondary) }
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, 9)
        .accessibilityElement(children: .combine)
    }

    // MARK: Report a problem with your employer

    @ViewBuilder private func complaint(_ b: AssistantComplaintBlock) -> some View {
        header(
            symbol: "briefcase.fill", hue: .sand,
            title: english ? "Report a problem with your employer" : "投诉雇主",
            subtitle: english ? "Permanent Committee for Labour Affairs" : "Report a problem with your employer"
        ) {
            switch b.status {
            case .done: Text(english ? "Sent" : "已提交").foregroundStyle(DNColor.textSecondary)
            case .stopped: Text(english ? "Stopped" : "已停止").foregroundStyle(DNColor.textSecondary)
            case .working, .yourTurn: EmptyView()
            }
        }
        ForEach(b.fields.prefix(b.visibleCount)) { f in
            AssistantRule()
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                Text(f.label.resolved(english: english))
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                    .frame(width: english ? 64 : 36, alignment: .leading)
                Text(f.value.resolved(english: english))
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.vertical, 9)
            .transition(.opacity.combined(with: .move(edge: .top)))
            .accessibilityElement(children: .combine)
        }
        if let ref = b.reference {
            AssistantRule()
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                Text(english ? "Ref" : "编号")
                    .dnFont(.footnote).foregroundStyle(DNColor.textSecondary)
                    .frame(width: english ? 64 : 36, alignment: .leading)
                Text(ref).dnFont(.subheadline).monospacedDigit().foregroundStyle(DNColor.textPrimary)
                Spacer(minLength: 0)
            }
            .padding(.vertical, 9)
            .accessibilityElement(children: .combine)
        }
        if b.visibleCount == b.fields.count {
            AssistantRule()
            Button {
                withAnimation(DNMotion.resolve(DNMotion.smooth)) { showsOriginal.toggle() }
            } label: {
                HStack {
                    Text(english ? "See the English and Arabic text" : "查看英文和阿拉伯文原文")
                        .dnFont(.subheadline).fontWeight(.semibold)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 13, weight: .semibold))
                        .rotationEffect(.degrees(showsOriginal ? 90 : 0))
                }
                .foregroundStyle(DNColor.textLink)
                .frame(minHeight: 44)
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .accessibilityValue(showsOriginal ? "Shown" : "Hidden")
            if showsOriginal {
                VStack(alignment: .leading, spacing: 10) {
                    Text(b.originalEnglish)
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textPrimary)
                    Text(b.originalArabic)
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textPrimary)
                        .multilineTextAlignment(.trailing)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                        .environment(\.layoutDirection, .rightToLeft)
                        .environment(\.locale, Locale(identifier: "ar_AE@numbers=latn"))
                }
                .fixedSize(horizontal: false, vertical: true)
                .padding(.bottom, 8)
                .transition(.opacity)
            }
        }
    }

    // MARK: Energy

    @ViewBuilder private func energy(_ e: EnergyReading) -> some View {
        let peak = max(1, e.lastSixMonths.max() ?? 1)
        header(symbol: "bolt.fill", hue: .sand, title: "Energy use", subtitle: e.comparison) { EmptyView() }
        HStack(alignment: .bottom, spacing: 8) {
            ForEach(Array(e.lastSixMonths.enumerated()), id: \.offset) { i, kwh in
                let last = i == e.lastSixMonths.count - 1
                RoundedRectangle(cornerRadius: 4, style: .continuous)
                    .fill(last ? DNColor.actionAccent : DNColor.blue200)
                    .frame(height: max(6, 64 * CGFloat(kwh) / CGFloat(peak)))
                    .frame(maxWidth: .infinity)
            }
        }
        .frame(height: 64, alignment: .bottom)
        .padding(.vertical, 8)
        .accessibilityElement()
        .accessibilityLabel("Last six months, in kilowatt hours")
        .accessibilityValue(e.lastSixMonths.map(String.init).joined(separator: ", "))
        HStack {
            Text("6 months ago")
            Spacer()
            Text(e.monthLabel)
        }
        .dnFont(.caption1)
        .foregroundStyle(DNColor.textSecondary)
        .padding(.bottom, 6)
        .accessibilityHidden(true)
    }

    // MARK: List (what needs you)

    @ViewBuilder private func list(_ rows: [AssistantListRow]) -> some View {
        ForEach(Array(rows.enumerated()), id: \.element.id) { i, row in
            if i > 0 { AssistantRule() }
            HStack(spacing: 12) {
                HueIcon(symbol: row.symbol, hue: row.hue, size: 28)
                    .frame(width: 36)
                VStack(alignment: .leading, spacing: 0) {
                    Text(row.title).dnFont(.subheadline).foregroundStyle(DNColor.textPrimary)
                    Text(row.detail).dnFont(.footnote).foregroundStyle(DNColor.textSecondary)
                }
                Spacer(minLength: 8)
                if let value = row.value {
                    Text(value).dnFont(.subheadline).fontWeight(.medium).monospacedDigit().foregroundStyle(DNColor.textSecondary)
                }
            }
            .padding(.vertical, 7)
            .accessibilityElement(children: .combine)
        }
    }

    // MARK: Pieces

    private func header<Trailing: View>(symbol: String, hue: Hue, title: String, subtitle: String, @ViewBuilder trailing: () -> Trailing) -> some View {
        HStack(spacing: 12) {
            HueIcon(symbol: symbol, hue: hue, size: 36)
            VStack(alignment: .leading, spacing: 0) {
                Text(title)
                    .dnFont(.rowTitle)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Text(subtitle)
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 8)
            trailing()
                .dnFont(.footnote)
                .fontWeight(.semibold)
        }
        .padding(.bottom, 8)
        .accessibilityElement(children: .combine)
    }
}

/// A checklist row: done ✓ · working (spinner) · your OK (radio) · to do.
struct AssistantStepRow: View {
    var step: AssistantStep
    var english: Bool
    var detail: String? = nil

    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        VStack(spacing: 0) {
            AssistantRule()
            HStack(spacing: 12) {
                AssistantStepMark(state: step.state)
                    .frame(width: 36)
                VStack(alignment: .leading, spacing: 0) {
                    Text(step.title.resolved(english: english))
                        .dnFont(.subheadline)
                        .fontWeight(step.state == .active || step.state == .yourTurn ? .semibold : .regular)
                        .foregroundStyle(step.state == .pending ? DNColor.textSecondary : DNColor.textPrimary)
                        .contentTransition(.opacity)
                    if let detail {
                        Text(detail).dnFont(.footnote).foregroundStyle(DNColor.statusWarningForeground)
                    }
                    // Large text: the amount moves under the title.
                    if typeSize.isAccessibilitySize, let amount = step.amount {
                        Text(Money.aed(amount, trimWhole: true))
                            .dnFont(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundStyle(DNColor.textSecondary)
                    }
                }
                Spacer(minLength: 8)
                if !typeSize.isAccessibilitySize, let amount = step.amount {
                    Text(Money.aed(amount, trimWhole: true))
                        .dnFont(.subheadline)
                        .fontWeight(step.state == .yourTurn ? .semibold : .medium)
                        .monospacedDigit()
                        .foregroundStyle(step.state == .yourTurn ? DNColor.textPrimary : DNColor.textSecondary)
                        .fixedSize()
                }
            }
            .frame(minHeight: 38)
            .padding(.vertical, detail == nil ? 0 : 4)
        }
        .accessibilityElement(children: .combine)
        .accessibilityValue(spokenState)
    }

    private var spokenState: String {
        switch step.state {
        case .done: "Done"
        case .active: "In progress"
        case .yourTurn: "Waiting for you"
        case .pending: "To do"
        }
    }
}

struct AssistantStepMark: View {
    var state: AssistantStep.State
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var spin = false

    var body: some View {
        ZStack {
            switch state {
            case .done:
                Circle().fill(DNColor.actionAccent)
                Image(systemName: "checkmark")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(DNColor.white)
            case .active:
                Circle().stroke(DNColor.blue100, lineWidth: 2.2)
                Circle()
                    .trim(from: 0, to: 0.7)
                    .stroke(DNColor.actionAccent, style: StrokeStyle(lineWidth: 2.2, lineCap: .round))
                    .rotationEffect(.degrees(spin ? 360 : 0))
                    .onAppear {
                        guard !reduceMotion else { return }
                        withAnimation(.linear(duration: 0.9).repeatForever(autoreverses: false)) { spin = true }
                    }
            case .yourTurn:
                Circle().fill(DNColor.actionAccent.opacity(0.12))
                Circle().strokeBorder(DNColor.actionAccent, lineWidth: 2.2)
                Circle().fill(DNColor.actionAccent).frame(width: 8, height: 8)
            case .pending:
                Circle().strokeBorder(DNColor.neutral300, lineWidth: 1.5)
            }
        }
        .frame(width: 22, height: 22)
        .transition(.scale.combined(with: .opacity))
        .accessibilityHidden(true)
    }
}

/// 0.5 pt hairline between rows.
struct AssistantRule: View {
    var body: some View {
        Rectangle()
            .fill(DNColor.blue950.opacity(0.09))
            .frame(height: 0.5)
    }
}
