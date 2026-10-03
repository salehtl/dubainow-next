import SwiftUI

// Direct answers (Paper "Search · Direct answer"): worked out from the resident's own
// linked accounts, always saying where the numbers come from and when they were updated.

/// The white answer card: radius 28, 20 pt padding, 18 pt between parts.
struct SearchAnswerCard<Content: View>: View {
    var source: String
    var sourceSymbol: String = "lock.fill"
    var trailing: String?
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(alignment: .firstTextBaseline) {
                Label {
                    Text(source.uppercased())
                        .dnFont(.overline)
                } icon: {
                    Image(systemName: sourceSymbol)
                        .font(.system(size: 11, weight: .semibold))
                }
                .labelStyle(SearchTightLabelStyle())
                .foregroundStyle(DNColor.textSecondary)
                Spacer(minLength: 8)
                if let trailing {
                    Text(trailing)
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textSecondary)
                }
            }
            .accessibilityElement(children: .combine)
            content
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DNColor.surfacePrimary, in: .rect(cornerRadius: 28, style: .continuous))
        .dnElevation()
    }
}

struct SearchTightLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: 6) { configuration.icon; configuration.title }
    }
}

/// "You owe" · hero amount · a line under it.
private struct SearchHeadline: View {
    var title: String
    var amount: Decimal?
    var figure: String?
    var unit: String?
    var detail: String?
    var detailTone: StatusChip.Tone = .neutral

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .dnFont(.headline)
                .foregroundStyle(DNColor.textPrimary)
            if let amount {
                MoneyText(amount: amount, size: .numericHero, unit: .numericUnit)
            } else if let figure {
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Text(figure).dnFont(.numericHero)
                    if let unit { Text(unit).dnFont(.numericUnit) }
                }
                .foregroundStyle(DNColor.textPrimary)
                .accessibilityElement(children: .combine)
            }
            if let detail {
                Text(detail)
                    .dnFont(.subheadline)
                    .fontWeight(detailTone == .neutral ? .regular : .semibold)
                    .foregroundStyle(SearchTone.color(detailTone))
            }
        }
        .accessibilityElement(children: .combine)
    }
}

private struct SearchAnswerDivider: View {
    @Environment(\.displayScale) private var displayScale
    var body: some View {
        Rectangle().fill(DNColor.listRowSeparator).frame(height: 1 / displayScale)
    }
}

// MARK: - How much do I owe?

struct SearchOweAnswer: View {
    var payables: [Payable]
    var updated: String
    var payAll: () -> Void
    var pay: (Payable) -> Void
    var payOneAtATime: () -> Void

    private var total: Decimal { payables.reduce(0) { $0 + $1.amount } }

    var body: some View {
        SearchAnswerCard(source: "From your accounts", trailing: "Updated \(updated)") {
            if payables.isEmpty {
                SearchHeadline(title: "You owe", amount: 0, detail: "Nothing to pay right now. We'll tell you when a bill or fine arrives.")
            } else {
                SearchHeadline(title: "You owe", amount: total, detail: "across \(SearchFormat.payablesSummary(payables))")

                VStack(spacing: 0) {
                    ForEach(payables) { p in
                        SearchAnswerDivider()
                        Button { pay(p) } label: { row(p) }
                            .buttonStyle(.plain)
                            .accessibilityHint("Pays this one")
                    }
                }

                VStack(spacing: 4) {
                    Button(payAllTitle, action: payAll)
                        .buttonStyle(.dn(.primary, size: .large))
                    if payables.count > 1 {
                        Button("Pay one at a time", action: payOneAtATime)
                            .dnFont(.callout)
                            .fontWeight(.medium)
                            .foregroundStyle(DNColor.textLink)
                            .buttonStyle(.plain)
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .contentShape(.rect)
                    }
                }
            }
        }
    }

    private var payAllTitle: String {
        switch payables.count {
        case 1: "Pay \(Money.aed(total))"
        case 2: "Pay both · \(Money.aed(total))"
        default: "Pay all \(payables.count) · \(Money.aed(total))"
        }
    }

    @Environment(\.dynamicTypeSize) private var typeSize

    private func row(_ p: Payable) -> some View {
        let note = p.note?.lowercased().replacingOccurrences(of: "your ", with: "")
        let due = p.due.map { DemoDate.dueLabel($0).lowercased() }
        let tone: StatusChip.Tone = note != nil ? .error : (p.due.map { DemoDate.daysFromToday($0) <= 2 } ?? false) ? .warning : .neutral
        let ax = typeSize.isAccessibilitySize
        let amount = Text(Money.figure(p.amount))
            .font(.system(.callout, design: .rounded, weight: .bold))
            .foregroundStyle(DNColor.textPrimary)
        return HStack(spacing: 12) {
            HueIcon(symbol: p.symbol, hue: p.hue, size: 36)
            VStack(alignment: .leading, spacing: 1) {
                Text(p.title.replacingOccurrences(of: " & ", with: " and "))
                    .dnFont(.rowTitle)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                Text([p.entity, note ?? due].compactMap { $0 }.joined(separator: " · "))
                    .dnFont(.footnote)
                    .fontWeight(.semibold)
                    .foregroundStyle(SearchTone.color(tone))
                if ax { amount }
            }
            Spacer(minLength: 8)
            if !ax { amount }
        }
        .padding(.vertical, ax ? 8 : 0)
        .frame(minHeight: 62)
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(p.title), \(p.entity), \(Money.spoken(p.amount))")
    }
}

// MARK: - Visa status (any request)

struct SearchRequestAnswer: View {
    var request: RequestItem
    var document: DocumentItem?
    var updated: String
    var open: () -> Void

    var body: some View {
        SearchAnswerCard(source: "From your application", trailing: "Updated \(updated)") {
            VStack(alignment: .leading, spacing: 10) {
                Text(request.title)
                    .dnFont(.title3)
                    .foregroundStyle(DNColor.textPrimary)
                StatusChip(text: request.statusLine, tone: tone, onWhite: true)
            }
            .accessibilityElement(children: .combine)

            if let i = request.currentStepIndex ?? (request.steps.allSatisfy { $0.state == .done } ? request.steps.count - 1 : nil) {
                VStack(alignment: .leading, spacing: 8) {
                    SegmentedProgress(total: request.steps.count, done: request.steps.filter { $0.state == .done }.count, currentColor: tone == .warning ? DNColor.statusWarningForeground : DNColor.actionAccent)
                    Text("Step \(i + 1) of \(request.steps.count) · \(request.steps[i].title)")
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textSecondary)
                }
                .accessibilityElement(children: .combine)
            }

            if let notice = request.notice {
                VStack(alignment: .leading, spacing: 4) {
                    Text(notice.title)
                        .dnFont(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(DNColor.statusWarningForeground)
                    Text(notice.body)
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(DNColor.statusWarningSurface, in: .rect(cornerRadius: 16, style: .continuous))
                .accessibilityElement(children: .combine)
            }

            if let document, let expiry = document.expiry {
                let days = DemoDate.daysFromToday(expiry)
                HStack(spacing: 10) {
                    Image(systemName: "calendar")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(DNColor.textSecondary)
                        .accessibilityHidden(true)
                    Text("Your current \(document.title.lowercased()) expires on \(DemoDate.long(expiry)), in \(days) days.")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .accessibilityElement(children: .combine)
            }

            Button(request.action ?? "Track your request", action: open)
                .buttonStyle(.dn(.primary, size: .large))
        }
    }

    private var tone: StatusChip.Tone {
        switch request.status {
        case .yourTurn: .warning
        case .inReview: .info
        case .approved, .done: .success
        }
    }
}

// MARK: - Salik / nol balance

struct SearchBalanceAnswer: View {
    var title: String
    var balance: Decimal
    var detail: String?
    var low: Bool
    var lastTopUp: String?
    var updated: String
    var topUp: () -> Void

    var body: some View {
        SearchAnswerCard(source: "From your accounts", trailing: "Updated \(updated)") {
            SearchHeadline(title: title, amount: balance, detail: detail, detailTone: low ? .warning : .neutral)
            if let lastTopUp {
                Text(lastTopUp)
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
            }
            Button("Top up", action: topUp)
                .buttonStyle(.dn(.primary, size: .large))
        }
    }
}

// MARK: - Prayer times

struct SearchPrayerAnswer: View {
    var named: String?
    var now: Date
    var widgetOnHome: Bool
    var addWidget: () -> Void

    var body: some View {
        let (prayer, date) = target
        let isToday = Calendar.current.isDate(date, inSameDayAs: now)
        SearchAnswerCard(source: "Prayer times · Dubai", sourceSymbol: "moon.stars.fill", trailing: isToday ? "Today" : "Tomorrow") {
            VStack(alignment: .leading, spacing: 2) {
                Text(named == nil ? "Next prayer · \(prayer.name)" : "\(prayer.name) is at")
                    .dnFont(.headline)
                    .foregroundStyle(DNColor.textPrimary)
                Text(String(format: "%02d:%02d", prayer.hour, prayer.minute))
                    .dnFont(.numericHero)
                    .foregroundStyle(DNColor.textPrimary)
                Text("\(isToday ? "Today" : "Tomorrow") · \(DemoDate.countdown(to: date, from: now))")
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textSecondary)
            }
            .accessibilityElement(children: .combine)

            SearchAnswerDivider()

            ViewThatFits(in: .horizontal) {
                HStack(spacing: 0) {
                    ForEach(prayers, id: \.name) { p in
                        VStack(spacing: 2) { prayerName(p); prayerTime(p) }
                            .frame(maxWidth: .infinity)
                            .accessibilityElement(children: .combine)
                    }
                }
                VStack(spacing: 6) {
                    ForEach(prayers, id: \.name) { p in
                        HStack { prayerName(p); Spacer(); prayerTime(p) }
                            .accessibilityElement(children: .combine)
                    }
                }
            }

            if !widgetOnHome {
                Button("Add prayer times to Home", action: addWidget)
                    .buttonStyle(.dn(.secondary, size: .medium))
            }
        }
    }

    private var prayers: [PrayerTimes.Prayer] { PrayerTimes.today.filter { $0.name != "Sunrise" } }

    private func prayerName(_ p: PrayerTimes.Prayer) -> some View {
        Text(p.name)
            .dnFont(.caption1)
            .foregroundStyle(p.name == target.0.name ? DNColor.textLink : DNColor.textSecondary)
            .fixedSize()
    }

    private func prayerTime(_ p: PrayerTimes.Prayer) -> some View {
        let isTarget = p.name == target.0.name
        return Text(String(format: "%02d:%02d", p.hour, p.minute))
            .font(.system(.subheadline, design: .rounded, weight: isTarget ? .bold : .medium))
            .foregroundStyle(isTarget ? DNColor.textLink : DNColor.textPrimary)
            .fixedSize()
    }

    private var target: (PrayerTimes.Prayer, Date) {
        guard let named, let p = PrayerTimes.today.first(where: { $0.name == named }) else {
            return PrayerTimes.next(after: now)
        }
        let cal = Calendar.current
        let today = cal.date(bySettingHour: p.hour, minute: p.minute, second: 0, of: now) ?? now
        if today > now { return (p, today) }
        let tomorrow = cal.date(byAdding: .day, value: 1, to: today) ?? today
        return (p, tomorrow)
    }
}

// MARK: - Parking

struct SearchParkingAnswer: View {
    var session: ParkingSession?
    var updated: String
    var extend: () -> Void
    var payForParking: () -> Void

    var body: some View {
        SearchAnswerCard(source: "From your accounts", trailing: "Updated \(updated)") {
            if let session {
                TimelineView(.periodic(from: .now, by: 30)) { context in
                    let left = max(0, Int(session.endsAt.timeIntervalSince(context.date) / 60))
                    SearchHeadline(
                        title: "Your parking",
                        figure: left >= 60 ? "\(left / 60)h \(left % 60)" : "\(left)",
                        unit: "min left",
                        detail: "Zone \(session.zone) · \(session.area) · ends \(DemoDate.time(session.endsAt))",
                        detailTone: left <= 15 ? .warning : .neutral
                    )
                }
                HStack(spacing: 10) {
                    NumberPlate(code: session.plate.code, number: session.plate.number, emirate: session.plate.emirate, small: true)
                    Text("\(Money.aed(session.ratePerHour, trimWhole: true)) an hour")
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textSecondary)
                }
                Button("Extend", action: extend)
                    .buttonStyle(.dn(.primary, size: .large))
            } else {
                SearchHeadline(title: "You're not parked anywhere right now", detail: "Start a session when you park and we'll remind you before it ends.")
                Button("Pay for parking", action: payForParking)
                    .buttonStyle(.dn(.secondary, size: .large))
            }
        }
    }
}

// MARK: - Guest

struct SearchSignInCard: View {
    var signIn: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top, spacing: 14) {
                HueIcon(symbol: "lock.fill", hue: .sky, size: 44)
                VStack(alignment: .leading, spacing: 4) {
                    Text("Sign in to see answers about your bills and fines")
                        .dnFont(.headline)
                        .foregroundStyle(DNColor.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                    Text("With UAE PASS, DubaiNow works out what you owe, your visa status and your balances from your linked accounts.")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .accessibilityElement(children: .combine)
            Button("Sign in with UAE PASS", action: signIn)
                .buttonStyle(.dn(.primary, size: .large))
        }
        .padding(20)
        .background(DNColor.surfacePrimary, in: .rect(cornerRadius: 28, style: .continuous))
        .dnElevation()
    }
}
