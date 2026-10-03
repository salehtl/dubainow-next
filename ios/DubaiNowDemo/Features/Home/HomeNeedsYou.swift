import SwiftUI

/// Anchor 1 · Needs you. Heading, count, fold chevron, summary, See all, Pay all and the
/// rail of brief cards. Folded (C1 · Needs you, folded): one line that keeps the count,
/// the next thing due and Pay all; the whole header is the button that unfolds it.
struct HomeNeedsYouSection: View {
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(ToastCenter.self) private var toasts
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    @State private var appeared = false

    private var items: [NeedsYouItem] { store.state.needsYou }
    /// 272 pt; wider at accessibility text sizes so the card can reflow.
    private var cardWidth: CGFloat { dynamicTypeSize.isAccessibilitySize ? 320 : DNSize.briefCard }
    private var folded: Bool { store.isNeedsYouFolded }

    var body: some View {
        VStack(alignment: .leading, spacing: DNSpace.sm) {
            if items.isEmpty {
                caughtUp
            } else {
                header
                if store.showsPayAll {
                    payAll
                        .padding(.horizontal, DNSpace.insetPage)
                }
                if !folded {
                    rail
                        .transition(reduceMotion ? .opacity : .move(edge: .top).combined(with: .opacity))
                }
            }
        }
        .onAppear {
            guard !appeared else { return }
            appeared = true
        }
    }

    // MARK: Header

    @ViewBuilder
    private var header: some View {
        if folded {
            Button(action: toggleFold) {
                HStack(alignment: .bottom) {
                    VStack(alignment: .leading, spacing: 2) {
                        headingRow
                        foldedLine
                    }
                    Spacer(minLength: 0)
                }
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .padding(.horizontal, DNSpace.insetPage)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Needs you, \(countPhrase), folded. \(foldedLineText)")
            .accessibilityHint("Unfolds Needs you")
            .accessibilityAddTraits(.isButton)
            .accessibilityAction(named: "Unfold") { toggleFold() }
        } else {
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 2) {
                    headingRow
                    summaryLine
                }
                Spacer(minLength: 8)
                HomeLinkButton(title: "See all", color: DNColor.textOnWashLink) {
                    router.select(.activity)
                }
                .accessibilityLabel("See all in Activity")
            }
            .padding(.horizontal, DNSpace.insetPage)
        }
    }

    private var headingRow: some View {
        HStack(spacing: 8) {
            Text("Needs you")
                .dnFont(.section)
                .foregroundStyle(DNColor.textPrimary)
                .accessibilityAddTraits(.isHeader)
            Text("\(items.count)")
                .font(.system(size: 13, weight: .bold))
                .monospacedDigit()
                .foregroundStyle(DNColor.textOnInverse)
                .padding(.horizontal, 7)
                .frame(minWidth: 24, minHeight: 24)
                .background(DNColor.textPrimary, in: .capsule)
                .accessibilityLabel(countPhrase)
            foldControl
        }
    }

    @ViewBuilder
    private var foldControl: some View {
        let chevron = Image(systemName: "chevron.up")
            .font(.system(size: 12, weight: .bold))
            .foregroundStyle(DNColor.textPrimary)
            .rotationEffect(.degrees(folded ? 180 : 0))
            .frame(width: 28, height: 28)
            .background(Color(.tertiarySystemFill), in: .circle)
        if folded {
            chevron.accessibilityHidden(true)
        } else {
            Button(action: toggleFold) {
                chevron
                    .padding(8)
                    .contentShape(.rect)
                    .padding(-8)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Fold Needs you")
            .accessibilityHint("Keeps the count, the next thing due and Pay all")
        }
    }

    @ViewBuilder
    private var summaryLine: some View {
        if let today = store.homeDueToday, let due = today.due {
            Label(DemoDate.daysFromToday(due) < 0 ? "Something's overdue" : "Something's due today", systemImage: "clock")
                .labelStyle(HomeTightLabelStyle())
                .dnFont(.subheadline)
                .fontWeight(.semibold)
                .foregroundStyle(DNColor.statusWarningForeground)
        } else {
            Text(store.dueThisWeekCount > 0 ? "\(store.dueThisWeekCount) due this week" : "Nothing due this week")
                .dnFont(.subheadline)
                .foregroundStyle(DNColor.textOnWashSecondary)
        }
    }

    @ViewBuilder
    private var foldedLine: some View {
        if store.homeDueToday != nil {
            Label(foldedLineText, systemImage: "clock")
                .labelStyle(HomeTightLabelStyle())
                .dnFont(.subheadline)
                .fontWeight(.semibold)
                .foregroundStyle(DNColor.statusWarningForeground)
                .lineLimit(2)
        } else {
            Text(foldedLineText)
                .dnFont(.subheadline)
                .foregroundStyle(DNColor.textOnWashSecondary)
                .lineLimit(2)
        }
    }

    private var foldedLineText: String {
        if let today = store.homeDueToday {
            return "Due today: \(HomeCopy.inline(today.title))"
        }
        if let next = store.nextDue, let due = next.due {
            return "Next: \(HomeCopy.inline(next.title)), \(HomeCopy.when(due))"
        }
        if let first = items.first {
            return "Next: \(HomeCopy.inline(first.title))"
        }
        return ""
    }

    private var countPhrase: String { items.count == 1 ? "1 item" : "\(items.count) items" }

    private func toggleFold() {
        DNHaptics.selection()
        withAnimation(DNMotion.resolve(DNMotion.smooth)) {
            store.isNeedsYouFolded.toggle()
        }
        UIAccessibility.post(notification: .announcement, argument: store.isNeedsYouFolded ? "Needs you folded" : "Needs you unfolded")
    }

    // MARK: Pay all

    private var payAll: some View {
        Button {
            router.present(.payment(store.payAllRequest()))
        } label: {
            HStack(spacing: 8) {
                HomePayAllGlyph()
                Text("Pay all · \(Money.aed(store.payAllTotal))")
                    .dnFont(.callout)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textOnAccent)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .padding(.leading, 14)
            .padding(.trailing, 16)
            .frame(minHeight: 40)
            .background(DNColor.actionAccent, in: .capsule)
            .contentShape(.capsule)
        }
        .buttonStyle(.dnPressable)
        .accessibilityLabel("Pay all, \(Money.spoken(store.payAllTotal)), \(store.payables.count) payments")
    }

    // MARK: Rail

    private var rail: some View {
        ScrollView(.horizontal) {
            HStack(alignment: .top, spacing: DNSpace.widgetGutter) {
                ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                    HomeBriefCard(item: item, onLater: { later(item) })
                        .frame(width: cardWidth)
                        .frame(maxHeight: .infinity, alignment: .top)
                        .opacity(appeared ? 1 : 0)
                        .offset(y: appeared || reduceMotion ? 0 : 12)
                        .animation(DNMotion.resolve(DNMotion.smooth).delay(DNMotion.stagger(index)), value: appeared)
                }
            }
            .scrollTargetLayout()
            .fixedSize(horizontal: false, vertical: true)
            .padding(.bottom, 4)
        }
        .scrollTargetBehavior(.viewAligned)
        .contentMargins(.horizontal, DNSpace.insetPage, for: .scrollContent)
        .scrollIndicators(.hidden)
        .scrollClipDisabled()
    }

    private func later(_ item: NeedsYouItem) {
        let before = store.state.needsYou
        store.later(item)
        toasts.show("Moved to the back. It's still in Activity.", undo: { [store] in
            store.homeRestoreNeedsYou(before)
        })
    }

    // MARK: All caught up (A3J-0)

    private var caughtUp: some View {
        VStack(alignment: .leading, spacing: DNSpace.sm) {
            Text("Needs you")
                .dnFont(.section)
                .foregroundStyle(DNColor.textPrimary)
                .accessibilityAddTraits(.isHeader)
                .frame(minHeight: 44, alignment: .bottomLeading)
            HStack(alignment: .top, spacing: 14) {
                Image(systemName: "checkmark")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(DNColor.statusSuccessForeground)
                    .frame(width: 40, height: 40)
                    .background(DNColor.statusSuccessSurface, in: .circle)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    Text("You're all caught up")
                        .dnFont(.headline)
                        .foregroundStyle(DNColor.textPrimary)
                    Text("Bills, fines and renewals show up here when they need you.")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 0)
            }
            .padding(18)
            .dnCard(radius: DNRadius.brief)
            .accessibilityElement(children: .combine)
        }
        .padding(.horizontal, DNSpace.insetPage)
    }
}

/// Icon and title 6 pt apart (the inline summary lines).
struct HomeTightLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: 6) {
            configuration.icon.imageScale(.small)
            configuration.title
        }
    }
}

// MARK: - Brief card

/// A 272 pt brief card (radius 24): a 120 pt poster in the item's hue with the entity badge,
/// a status chip and the figure (amount, steps or stage), then title, detail and actions.
struct HomeBriefCard: View {
    var item: NeedsYouItem
    var onLater: () -> Void

    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    private var isLarge: Bool { dynamicTypeSize.isAccessibilitySize }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            poster
            VStack(alignment: .leading, spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(item.title)
                        .dnFont(.headline)
                        .foregroundStyle(DNColor.textPrimary)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(item.detail)
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .accessibilityElement(children: .combine)
                .accessibilityLabel(accessibilitySummary)
                Spacer(minLength: 0)
                actions
            }
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 16)
            .frame(maxHeight: .infinity, alignment: .top)
        }
        .frame(maxHeight: .infinity, alignment: .top)
        .dnCard(radius: DNRadius.brief)
        .accessibilityElement(children: .contain)
    }

    // MARK: Poster

    private var poster: some View {
        ZStack(alignment: .bottomLeading) {
            posterBackground
            VStack(alignment: .leading, spacing: 0) {
                let topRow = isLarge ? AnyLayout(VStackLayout(alignment: .leading, spacing: 8)) : AnyLayout(HStackLayout(spacing: 8))
                topRow {
                    HStack(spacing: 8) {
                        Group {
                            if case .journeyStep = item.kind {
                                RouteMark(color: item.hue.ink, size: 16)
                            } else {
                                Image(systemName: item.symbol)
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(item.hue.ink)
                            }
                        }
                        .frame(width: 32, height: 32)
                        .background(DNColor.posterGlyph, in: .circle)
                        Text(badgeTitle)
                            .dnFont(.footnote)
                            .fontWeight(.semibold)
                            .foregroundStyle(DNColor.textPrimary)
                            .lineLimit(1)
                    }
                    if !isLarge { Spacer(minLength: 4) }
                    if let chip {
                        HomeBriefChip(text: chip.text, tone: chip.tone)
                            .layoutPriority(1)
                    }
                }
                Spacer(minLength: isLarge ? 10 : 6)
                figure
            }
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, progressFraction == nil ? 14 : 18)

            if let fraction = progressFraction {
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Rectangle().fill(progressTrack)
                        Rectangle().fill(progressFill).frame(width: geo.size.width * fraction)
                    }
                }
                .frame(height: 5)
                .accessibilityHidden(true)
            }
        }
        .frame(minHeight: 120, maxHeight: isLarge ? nil : 120)
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private var posterBackground: some View {
        if case .journeyStep = item.kind {
            Rectangle().fill(item.hue.gradient)
        } else {
            Rectangle().fill(item.hue.start)
        }
    }

    @ViewBuilder
    private var figure: some View {
        switch item.kind {
        case .payment(let p):
            MoneyText(amount: p.amount, size: .numericLarge, unit: .numericUnit)
        case .requestStep(let requestID):
            let progress = requestProgress(requestID)
            stepsFigure(String(progress.current), "of \(progress.total) steps")
        case .journeyStep(let journeyID):
            let j = journeyInfo(journeyID)
            stepsFigure(String(j.current), j.stage.map { "of \(j.total) · \($0)" } ?? "of \(j.total)")
        case .renewal(_, let thingID):
            if let v = vehicle(thingID) {
                NumberPlate(code: v.plate.code, number: v.plate.number, emirate: v.plate.emirate)
            } else if let due = item.due {
                Text(DemoDate.short(due))
                    .dnFont(.numericMedium)
                    .foregroundStyle(DNColor.textPrimary)
            }
        }
    }

    private func stepsFigure(_ big: String, _ rest: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            Text(big).dnFont(.numericLarge)
            Text(rest).dnFont(.numericUnit).lineLimit(1)
        }
        .foregroundStyle(DNColor.textPrimary)
    }

    private var badgeTitle: String {
        if case .journeyStep(let id) = item.kind {
            return Catalogue.shared.service(id)?.shortName ?? item.entity
        }
        return item.entity
    }

    private var chip: (text: String, tone: HomeBriefChip.Tone)? {
        if case .journeyStep = item.kind { return ("Your turn", .neutral) }
        if let due = item.due {
            let days = DemoDate.daysFromToday(due)
            let tone: HomeBriefChip.Tone = days < 0 ? .error : (days <= 1 ? .warning : .neutral)
            return (DemoDate.dueLabel(due), tone)
        }
        if let p = item.payable, p.kind == .fine { return ("Unpaid", .error) }
        return nil
    }

    private var progressFraction: Double? {
        switch item.kind {
        case .requestStep(let id):
            let p = requestProgress(id)
            return Double(max(p.current - 1, 0)) / Double(max(p.total, 1))
        case .journeyStep(let id):
            let j = journeyInfo(id)
            return (Double(j.current) - 0.5) / Double(max(j.total, 1))
        default:
            return nil
        }
    }

    private var progressTrack: Color {
        if case .journeyStep = item.kind { return item.hue.ink.opacity(0.2) }
        return DNColor.progressTrackOnHue
    }

    private var progressFill: Color {
        if case .journeyStep = item.kind { return item.hue.ink }
        return DNColor.actionAccent
    }

    // MARK: Actions

    private var actions: some View {
        // Side by side; stacked at accessibility text sizes.
        let layout = isLarge ? AnyLayout(VStackLayout(spacing: 8)) : AnyLayout(HStackLayout(spacing: 8))
        return layout {
            Button(item.primaryAction, action: primary)
                .buttonStyle(.dn(.primary, size: .medium))
            if let secondary = item.secondaryAction {
                if isLarge {
                    Button(secondary) { secondaryTapped(secondary) }
                        .buttonStyle(.dn(.secondary, size: .medium))
                } else {
                    Button(secondary) { secondaryTapped(secondary) }
                        .buttonStyle(.dn(.secondary, size: .medium, fullWidth: false))
                        .frame(minWidth: 88)
                        .fixedSize()
                }
            }
        }
    }

    private func primary() {
        switch item.kind {
        case .payment(let p):
            router.present(.payment(store.paymentRequest(for: p)))
        case .requestStep(let id):
            router.push(.request(id))
        case .renewal(let serviceID, _):
            router.open(serviceID: serviceID)
        case .journeyStep(let id):
            router.push(.journey(id))
        }
    }

    private func secondaryTapped(_ title: String) {
        if title == "Later" {
            onLater()
            return
        }
        switch item.kind {
        case .payment(let p):
            if let sid = p.serviceID { router.open(serviceID: sid) } else { onLater() }
        case .requestStep(let id):
            router.push(.request(id))
        case .renewal(let serviceID, let thingID):
            if let v = vehicle(thingID) { router.push(.vehicle(v.id)) } else { router.open(serviceID: serviceID) }
        case .journeyStep(let id):
            router.push(.journey(id))
        }
    }

    // MARK: Data

    private func requestProgress(_ id: String) -> (current: Int, total: Int) {
        if let p = item.progress { return (p.current, p.total) }
        if let r = store.request(id) {
            return ((r.currentStepIndex ?? 0) + 1, r.steps.count)
        }
        return (1, 1)
    }

    private func journeyInfo(_ id: String) -> (current: Int, total: Int, stage: String?) {
        let stages = Catalogue.shared.service(id)?.stages ?? []
        let index = store.state.journeys.first { $0.journeyID == id }?.stageIndex ?? 0
        let total = max(stages.count, index + 1)
        let stage = stages.indices.contains(index) ? stages[index].name : nil
        return (index + 1, total, stage)
    }

    private func vehicle(_ thingID: String?) -> Vehicle? {
        guard let thingID else { return nil }
        return store.state.vehicles.first { $0.id == thingID }
    }

    private var accessibilitySummary: String {
        var parts = [item.title, item.detail]
        if let amount = item.amount { parts.append(Money.spoken(amount)) }
        if let chip { parts.append(chip.text) }
        return parts.joined(separator: ", ")
    }
}
