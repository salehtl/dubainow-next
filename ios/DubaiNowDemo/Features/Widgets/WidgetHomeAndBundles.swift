import SwiftUI

// Home and bills: Energy meter. All at once: Bills and fines (Pay all), Family visas.

// MARK: - Energy meter

struct WidgetEnergyContent: View {
    @Environment(DemoStore.self) private var store

    var body: some View {
        if let e = store.state.energy {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 8) {
                    WidgetGlyph(symbol: "bolt.fill", ink: DNColor.signAmber, fill: DNColor.amber50, size: 28)
                    WidgetTitle("Energy · \(e.monthLabel)")
                    Spacer(minLength: 6)
                    trend(e)
                }
                Spacer(minLength: 10)
                HStack(alignment: .bottom, spacing: 12) {
                    VStack(alignment: .leading, spacing: 8) {
                        WidgetOdometer(value: e.kWh)
                        WidgetCaption(e.comparison, lines: 2)
                    }
                    Spacer(minLength: 0)
                    WidgetMonthBars(values: e.lastSixMonths)
                }
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 16)
        } else {
            WidgetCalmState(
                title: "Energy use",
                message: "Link your DEWA account to see this month's use",
                quick: (WidgetQuickLook.capsule("Connect", height: 36), WidgetQuickIntent.connect("DEWA"))
            ) {
                WidgetGlyph(symbol: "bolt.fill", ink: DNColor.signAmber, fill: DNColor.amber50)
            }
        }
    }

    private func trend(_ e: EnergyReading) -> some View {
        let down = e.changeVsLastMonth <= 0
        return WidgetChip(
            text: "\(abs(e.changeVsLastMonth))% vs \(WidgetFormat.previousMonthShort(of: e.monthLabel))",
            symbol: down ? "arrow.down" : "arrow.up",
            foreground: down ? DNColor.statusSuccessForeground : DNColor.statusWarningForeground,
            background: down ? DNColor.statusSuccessSurface : DNColor.statusWarningSurface,
            height: 26
        )
    }
}

/// Odometer digits on dark meter tiles; leading zeros dimmed, the last digit lit in sun orange.
/// Each digit rolls on its own when the reading changes.
struct WidgetOdometer: View {
    var value: Int
    var digits: Int = 5

    var body: some View {
        let s = String(format: "%0\(digits)d", min(value, Int(pow(10, Double(digits))) - 1))
        let chars = Array(s)
        let firstSignificant = chars.firstIndex { $0 != "0" } ?? chars.count - 1
        HStack(alignment: .bottom, spacing: 4) {
            ForEach(Array(chars.enumerated()), id: \.offset) { i, c in
                let isLast = i == chars.count - 1
                Text(String(c))
                    .font(.system(size: 26, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(isLast ? DNColor.textPrimary : (i < firstSignificant ? DNColor.widgetOnInverseTertiary : DNColor.widgetOnInverse))
                    .contentTransition(.numericText(value: Double(String(c)) ?? 0))
                    .frame(width: 30, height: 42)
                    .background(isLast ? DNColor.signSun : DNColor.widgetMeterTile, in: .rect(cornerRadius: 7, style: .continuous))
            }
            Text("kWh")
                .font(.system(size: 15, weight: .semibold, design: .rounded))
                .foregroundStyle(DNColor.textSecondary)
                .padding(.leading, 4)
        }
        .animation(DNMotion.resolve(DNMotion.smooth), value: value)
        .accessibilityHidden(true)
    }
}

/// Six months of use, oldest first; this month in sun orange, last month a shade darker.
struct WidgetMonthBars: View {
    var values: [Int]

    var body: some View {
        let shown = Array(values.suffix(6))
        let top = CGFloat(shown.max() ?? 1), bottom = CGFloat(shown.min() ?? 0)
        HStack(alignment: .bottom, spacing: 6) {
            ForEach(Array(shown.enumerated()), id: \.offset) { i, v in
                // Scaled between the lowest and highest month so the change reads at a glance.
                let t = top > bottom ? (CGFloat(v) - bottom) / (top - bottom) : 1
                RoundedRectangle(cornerRadius: 4, style: .continuous)
                    .fill(i == shown.count - 1 ? DNColor.signSun : (i == shown.count - 2 ? DNColor.neutral300 : DNColor.neutral200))
                    .frame(width: 11, height: 26 + 38 * t)
            }
        }
        .frame(height: 64, alignment: .bottom)
        .accessibilityHidden(true)
    }
}

extension WidgetSpec {
    static func energy(store: DemoStore) -> WidgetSpec {
        guard let e = store.state.energy else {
            return WidgetSpec(summary: "Energy use. Not connected. Link your DEWA account to see it.",
                              quick: .connect("DEWA"), open: .service("check-utility-consumption"))
        }
        let change = e.changeVsLastMonth <= 0 ? "down \(abs(e.changeVsLastMonth))%" : "up \(e.changeVsLastMonth)%"
        let last = WidgetFormat.previousMonthShort(of: e.monthLabel)
        return WidgetSpec(
            summary: "Energy use in \(e.monthLabel), \(e.kWh.formatted()) kilowatt hours, \(change) on \(last). \(e.comparison).",
            hint: "Opens your electricity and water use",
            open: .service("check-utility-consumption")
        )
    }
}

// MARK: - Bills and fines (bundle: Pay all)

struct WidgetBillsContent: View {
    var size: WidgetSize
    var now: Date
    @Environment(DemoStore.self) private var store

    var body: some View {
        let m = WidgetBillsModel(store: store, now: now)
        if store.isGuest {
            WidgetCalmState(title: "Bills and fines", message: "Sign in to see what's due across your accounts") {
                WidgetBundleMark()
            }
        } else if m.nothingYet {
            WidgetCalmState(title: "Nothing yet this month", message: "New bills and fines show up here as they arrive") {
                WidgetBundleMark()
            }
        } else if size == .small {
            small(m)
        } else {
            medium(m)
        }
    }

    private var checkDisc: some View {
        WidgetGlyph(symbol: "checkmark", ink: DNColor.statusSuccessForeground, fill: DNColor.statusSuccessSurface)
    }

    private func paidLine(_ m: WidgetBillsModel) -> String {
        if m.allPaid { return "\(Money.aed(m.paidThisMonth, trimWhole: true)) paid" }
        if m.paidThisMonth > 0 { return "\(Money.aed(m.paidThisMonth, trimWhole: true)) paid so far" }
        return "\(Money.aed(m.total, trimWhole: true)) in all"
    }

    private func small(_ m: WidgetBillsModel) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                if m.allPaid { checkDisc } else { WidgetBundleMark() }
                Spacer(minLength: 0)
            }
            .frame(height: 44)
            WidgetFigure("\(m.toCome.count)", unit: "to come", numeric: Double(m.toCome.count)).padding(.top, 6)
            WidgetCaption(paidLine(m))
            Spacer(minLength: 8)
            if m.allPaid {
                WidgetChip(text: "Receipts in Activity", foreground: DNColor.statusSuccessForeground, background: DNColor.statusSuccessSurface)
            } else if let chip = m.nextChip {
                WidgetChip(text: chip,
                           foreground: m.overdueCount > 0 ? DNColor.statusErrorForeground : DNColor.statusWarningForeground,
                           background: m.overdueCount > 0 ? DNColor.statusErrorSurface : DNColor.statusWarningSurface)
            }
        }
        .padding(EdgeInsets(top: 12, leading: 16, bottom: 16, trailing: 12))
    }

    private func medium(_ m: WidgetBillsModel) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 10) {
                if m.allPaid { checkDisc } else { WidgetBundleMark() }
                WidgetTitle("Bills and fines")
                Spacer(minLength: 6)
                if m.overdueCount > 0 {
                    WidgetChip(text: m.overdueCount == 1 ? "1 thing needs you" : "\(m.overdueCount) things need you",
                               symbol: "arrow.up", foreground: DNColor.statusWarningForeground, background: DNColor.statusWarningSurface)
                } else {
                    Text(m.monthName)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(DNColor.textSecondary)
                }
            }
            .frame(height: 44)
            if m.allPaid {
                WidgetFigure("0", unit: "to come", numeric: 0).padding(.top, 2)
            } else {
                WidgetFigure(Money.figure(m.total), leadingUnit: "AED", numeric: NSDecimalNumber(decimal: m.total).doubleValue)
                    .padding(.top, 2)
            }
            Spacer(minLength: 6)
            VStack(alignment: .leading, spacing: 6) {
                WidgetMeter(fraction: m.allPaid ? 1 : m.paidFraction, color: DNColor.statusSuccessForeground)
                VStack(alignment: .leading, spacing: 0) {
                    if m.allPaid {
                        WidgetCaption("\(Money.aed(m.paidThisMonth, trimWhole: true)) paid in \(m.monthName) · receipts in Activity")
                    } else {
                        WidgetCaption(m.paidThisMonth > 0
                                      ? "Still to come this month · \(Money.aed(m.paidThisMonth, trimWhole: true)) paid so far"
                                      : "Still to come this month · nothing paid yet")
                        if let next = m.next {
                            WidgetCaption(nextLine(next, when: m.nextWhen), color: DNColor.statusWarningForeground, weight: .semibold)
                        }
                    }
                }
            }
        }
        .padding(EdgeInsets(top: 12, leading: 16, bottom: 16, trailing: 16))
    }

    private func nextLine(_ p: Payable, when: String?) -> String {
        let what = WidgetFormat.lowerFirst(p.title).replacingOccurrences(of: " bill", with: "")
        guard let when else { return "Next: \(what)" }
        return "Next: \(what), \(when)"
    }
}

extension WidgetSpec {
    static func bills(store: DemoStore, now: Date) -> WidgetSpec {
        let m = WidgetBillsModel(store: store, now: now)
        let summary: String
        if store.isGuest {
            summary = "Bills and fines. Sign in to see what's due."
        } else if m.nothingYet {
            summary = "Bills and fines. Nothing yet this month."
        } else if m.allPaid {
            summary = "Bills and fines. All paid: \(Money.spoken(m.paidThisMonth)) in \(m.monthName). Receipts are in Activity."
        } else {
            var s = "Bills and fines, \(m.toCome.count) to come this month, \(Money.spoken(m.total))."
            if let next = m.next {
                s += " Next: \(WidgetFormat.lowerFirst(next.title))" + (m.nextWhen.map { ", due \($0)." } ?? ".")
            }
            summary = s
        }
        return WidgetSpec(summary: summary, hint: "Opens Pay all your bills and fines", open: .route(.bundle("pay-all")))
    }
}

// MARK: - Family visas (bundle: Renew your family's visas)

struct WidgetFamilyContent: View {
    var size: WidgetSize
    @Environment(DemoStore.self) private var store

    var body: some View {
        if let m = WidgetFamilyModel(store: store) {
            if size == .small { small(m) } else { medium(m) }
        } else {
            WidgetCalmState(title: "Family visas", message: "No family visas are due. We'll show them here 90 days before") {
                glyph
            }
        }
    }

    private var glyph: some View {
        WidgetGlyph(symbol: "person.text.rectangle", ink: Hue.lilac.ink, fill: Hue.lilac.gradient)
    }

    private func small(_ m: WidgetFamilyModel) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                glyph
                Spacer(minLength: 0)
                WidgetBundleMark(badge: true)
            }
            .frame(height: 44)
            WidgetFigure("\(m.days)", unit: m.days == 1 ? "day" : "days", numeric: Double(m.days)).padding(.top, 6)
            WidgetCaption(m.dueCount == 1 ? "until 1 visa expires" : "until \(m.dueCount) visas expire")
            Spacer(minLength: 8)
            HStack(spacing: 4) {
                ForEach(m.members.prefix(4)) { member in
                    Text(member.initial)
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Hue.lilac.ink)
                        .frame(width: 24, height: 24)
                        .background(Hue.lilac.start, in: .circle)
                        .overlay {
                            if member.isDue { Circle().strokeBorder(DNColor.signSun, lineWidth: 2) }
                        }
                }
                Text("\(m.dueCount) of \(m.members.count) due")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(DNColor.statusWarningForeground)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                    .padding(.leading, 4)
            }
        }
        .padding(EdgeInsets(top: 12, leading: 16, bottom: 16, trailing: 12))
    }

    private func medium(_ m: WidgetFamilyModel) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                glyph
                VStack(alignment: .leading, spacing: 0) {
                    WidgetTitle("Family visas")
                    Text("All at once · \(m.dueCount) due")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(DNColor.actionAccent)
                        .lineLimit(1)
                }
                Spacer(minLength: 6)
                if m.dueCount > 0 {
                    WidgetQuickSlotView(look: .capsule("Renew \(m.dueCount)", height: 34, fullWidth: false), intent: .renewFamilyVisas)
                }
            }
            .frame(height: 44)
            Spacer(minLength: 0)
            HStack(alignment: .top, spacing: 0) {
                ForEach(m.members.prefix(4)) { member in
                    VStack(spacing: 4) {
                        WidgetVisaRing(initial: member.initial, days: member.days, isDue: member.isDue)
                        Text(member.name)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(DNColor.textPrimary)
                            .lineLimit(1)
                        Text(member.days.map { $0 == 1 ? "1 day" : "\($0) days" } ?? "–")
                            .font(.system(size: 12, weight: member.isDue ? .semibold : .regular))
                            .foregroundStyle(member.isDue ? DNColor.statusWarningForeground : DNColor.textSecondary)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal, 6)
            Spacer(minLength: 0)
        }
        .padding(EdgeInsets(top: 12, leading: 16, bottom: 16, trailing: 16))
    }
}

/// A member's initial in a ring; for visas that are due the ring is the time left (out of 90 days).
struct WidgetVisaRing: View {
    var initial: String
    var days: Int?
    var isDue: Bool

    var body: some View {
        let fraction = isDue ? min(max(Double(days ?? 0) / 90, 0.04), 1) : 1
        ZStack {
            Circle().stroke(isDue ? DNColor.neutral200 : Hue.lilac.end, lineWidth: 4)
            if isDue {
                Circle()
                    .trim(from: 0, to: fraction)
                    .stroke(DNColor.signSun, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                    .rotationEffect(.degrees(-90))
            }
            Text(initial)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(Hue.lilac.ink)
        }
        .frame(width: 44, height: 44)
        .padding(2)
        .accessibilityHidden(true)
    }
}

extension WidgetSpec {
    static func family(store: DemoStore, size: WidgetSize) -> WidgetSpec {
        guard let m = WidgetFamilyModel(store: store) else {
            return WidgetSpec(summary: "Family visas. None due.", open: .route(.bundle("renew-family-visas")))
        }
        let names = m.members.filter(\.isDue).map(\.name).joined(separator: " and ")
        return WidgetSpec(
            summary: "Family visas, \(m.dueCount) of \(m.members.count) due. \(names) expire in \(m.days) days.",
            hint: "Opens Renew your family's visas",
            quick: m.dueCount > 0 && size != .small ? .renewFamilyVisas : nil,
            open: .route(.bundle("renew-family-visas"))
        )
    }
}
