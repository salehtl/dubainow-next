import SwiftUI

// Getting around: Parking, Salik, nol card, Metro departures (Paper C1 · 02 Widgets · Launch set).

// MARK: - Parking

/// A parking session at a moment in time: live → ending soon (10 min) → expired (counts up).
struct WidgetParkingModel {
    enum Phase { case live, endingSoon, expired }

    let session: ParkingSession
    let now: Date

    var remaining: TimeInterval { session.endsAt.timeIntervalSince(now) }
    var phase: Phase { remaining <= 0 ? .expired : (remaining <= 600 ? .endingSoon : .live) }
    var minutesLeft: Int { max(0, Int((remaining / 60).rounded(.up))) }
    var minutesOver: Int { max(0, Int((-remaining / 60).rounded(.down))) }
    var fraction: Double {
        let total = session.endsAt.timeIntervalSince(session.startedAt)
        return total > 0 ? min(max(remaining / total, 0), 1) : 0
    }
    var ends: String { DemoDate.time(session.endsAt) }
    var started: String { DemoDate.time(session.startedAt) }
    var paid: Decimal {
        let hours = session.endsAt.timeIntervalSince(session.startedAt) / 3600
        return (session.ratePerHour * Decimal(hours)).rounded(scale: 2)
    }

    /// "42 min" or "1 h 42 min" as figure parts.
    var figureParts: [WidgetFigure.Part] {
        let m = phase == .expired ? minutesOver : minutesLeft
        if m >= 60 {
            return [.init(value: "\(m / 60)", unit: "h"), .init(value: "\(m % 60)", unit: phase == .expired ? "min over" : "min")]
        }
        return [.init(value: "\(m)", unit: phase == .expired ? "min over" : "min")]
    }

    var meterColor: Color {
        switch phase {
        case .live: DNColor.signParking
        case .endingSoon: DNColor.signSun
        case .expired: DNColor.red200
        }
    }

    var spoken: String {
        switch phase {
        case .live:
            "Parking, Zone \(session.zone), \(WidgetFormat.spokenDuration(minutes: minutesLeft)) left, ends at \(ends)"
        case .endingSoon:
            "Parking ending soon, Zone \(session.zone), \(WidgetFormat.spokenDuration(minutes: minutesLeft)) left, ends at \(ends)"
        case .expired:
            "Parking ended at \(ends) in Zone \(session.zone), \(WidgetFormat.spokenDuration(minutes: minutesOver)) over. You could be fined."
        }
    }
}

struct WidgetParkingContent: View {
    var size: WidgetSize
    var now: Date
    @Environment(DemoStore.self) private var store

    var body: some View {
        if let session = store.state.parking {
            let model = WidgetParkingModel(session: session, now: now)
            if size == .small { small(model) } else { medium(model) }
        } else {
            WidgetCalmState(
                title: "Not parked",
                message: "Start a session when you park",
                quick: (WidgetQuickLook.capsule("Start parking", height: 36), WidgetQuickIntent.startParking)
            ) { WidgetParkingSign() }
        }
    }

    private func quickLook(_ m: WidgetParkingModel, small: Bool) -> WidgetQuickLook {
        switch m.phase {
        case .live: small ? .plus(.subtle) : .capsule("Extend", symbol: "plus")
        case .endingSoon: small ? .plus(.filled) : .capsule("Extend", symbol: "plus", tone: .filled)
        case .expired: small ? .capsule("Extend now", tone: .destructive, height: 30, fullWidth: false)
                             : .capsule("Extend now", tone: .destructive)
        }
    }

    private func figure(_ m: WidgetParkingModel) -> some View {
        WidgetFigure(
            parts: m.figureParts,
            color: m.phase == .expired ? DNColor.statusErrorForeground : DNColor.textPrimary,
            numeric: Double(m.phase == .expired ? m.minutesOver : m.minutesLeft)
        )
    }

    @ViewBuilder
    private func caption(_ m: WidgetParkingModel, medium: Bool) -> some View {
        switch m.phase {
        case .live:
            WidgetCaption(medium ? "left · ends \(m.ends)" : "left in Zone \(m.session.zone)")
        case .endingSoon:
            HStack(spacing: 5) {
                Image(systemName: "clock.fill").font(.system(size: 12, weight: .semibold))
                WidgetCaption("Ends at \(m.ends)", color: DNColor.statusWarningForeground, weight: .semibold)
            }
            .foregroundStyle(DNColor.statusWarningForeground)
        case .expired:
            WidgetCaption("Ended \(m.ends) · fine risk", color: DNColor.statusErrorForeground, weight: .semibold)
        }
    }

    private func small(_ m: WidgetParkingModel) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                WidgetParkingSign()
                Spacer(minLength: 0)
                if m.phase != .expired { WidgetQuickSlotView(look: quickLook(m, small: true), intent: .extendParking) }
            }
            .frame(height: 44)
            figure(m).padding(.top, 6)
            caption(m, medium: false)
            Spacer(minLength: 8)
            if m.phase == .expired {
                WidgetQuickSlotView(look: quickLook(m, small: true), intent: .extendParking)
            } else {
                WidgetMeter(fraction: m.fraction, color: m.meterColor)
            }
        }
        .padding(EdgeInsets(top: 12, leading: 16, bottom: 16, trailing: 12))
    }

    private func medium(_ m: WidgetParkingModel) -> some View {
        HStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 10) {
                    WidgetParkingSign()
                    WidgetTitle("Zone \(m.session.zone)")
                }
                .frame(height: 44)
                figure(m).padding(.top, 6)
                caption(m, medium: true)
                Spacer(minLength: 8)
                WidgetMeter(fraction: m.fraction, color: m.meterColor)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Rectangle().fill(DNColor.neutral200).frame(width: 1)
            VStack(alignment: .leading, spacing: 8) {
                WidgetFactRow(label: "Plate", value: "\(m.session.plate.code) \(m.session.plate.number)")
                WidgetFactRow(label: "Started", value: m.started)
                WidgetFactRow(label: "Paid", value: Money.aed(m.paid, trimWhole: true))
                Spacer(minLength: 6)
                WidgetQuickSlotView(look: quickLook(m, small: false), intent: .extendParking)
            }
            .padding(.top, 6)
            .frame(maxWidth: .infinity)
        }
        .padding(EdgeInsets(top: 12, leading: 16, bottom: 16, trailing: 16))
    }
}

/// "Plate   M 48213" — a label and a rounded value on one line.
struct WidgetFactRow: View {
    var label: String
    var value: String
    var onDark: Bool = false

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            Text(label)
                .font(.system(size: 13))
                .foregroundStyle(onDark ? DNColor.widgetOnInverseSecondary : DNColor.textSecondary)
            Spacer(minLength: 6)
            Text(value)
                .font(.system(size: 15, weight: .semibold, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(onDark ? DNColor.widgetOnInverse : DNColor.textPrimary)
                .contentTransition(.numericText())
        }
        .lineLimit(1)
        .minimumScaleFactor(0.8)
    }
}

extension WidgetSpec {
    static func parking(store: DemoStore, now: Date) -> WidgetSpec {
        guard let session = store.state.parking else {
            return WidgetSpec(summary: "Parking. Not parked right now.", hint: "Opens parking", quick: .startParking,
                              open: .sheet(.extendParking))
        }
        let m = WidgetParkingModel(session: session, now: now)
        return WidgetSpec(
            edge: m.phase == .expired ? DNColor.statusErrorForeground : nil,
            summary: m.spoken,
            hint: "Opens Extend parking",
            quick: .extendParking,
            open: .sheet(.extendParking)
        )
    }
}

// MARK: - Salik

struct WidgetSalikModel {
    var balance: Decimal
    /// Salik charges AED 4 a gate in the demo.
    var trips: Int { max(0, Int(NSDecimalNumber(decimal: balance).doubleValue / 4)) }
    var isLow: Bool { trips < 6 }
    var tripsLine: String {
        let t = trips == 1 ? "1 trip" : "\(trips) trips"
        return isLow ? "Low · about \(t)" : "About \(t)"
    }
}

struct WidgetSalikContent: View {
    var size: WidgetSize
    @Environment(DemoStore.self) private var store

    var body: some View {
        if store.widgetSalikLinked {
            let m = WidgetSalikModel(balance: store.state.salikBalance)
            ZStack {
                WidgetRoadMarking()
                if size == .small { small(m) } else { medium(m) }
            }
        } else {
            WidgetCalmState(
                title: "Salik",
                message: "Link your account to see your balance",
                onDark: true,
                quick: (WidgetQuickLook.capsule("Connect", tone: .white, height: 36), WidgetQuickIntent.connect("Salik"))
            ) { glyph }
        }
    }

    private var glyph: some View {
        WidgetGlyph(symbol: "road.lanes", ink: DNColor.widgetOnInverse, fill: DNColor.widgetOnInverseRule)
    }

    private func figure(_ m: WidgetSalikModel) -> some View {
        WidgetFigure(
            Money.figure(m.balance, trimWhole: true),
            leadingUnit: "AED",
            color: DNColor.widgetOnInverse,
            unitColor: DNColor.widgetOnInverseSecondary,
            numeric: NSDecimalNumber(decimal: m.balance).doubleValue
        )
    }

    private func tripsLine(_ m: WidgetSalikModel) -> some View {
        HStack(spacing: 6) {
            Circle().fill(m.isLow ? DNColor.signLow : DNColor.signLive).frame(width: 7, height: 7)
            WidgetCaption(m.tripsLine, color: m.isLow ? DNColor.signLow : DNColor.widgetOnInverseSecondary, weight: .semibold)
        }
    }

    private func pips(_ m: WidgetSalikModel) -> some View {
        HStack(spacing: 3) {
            ForEach(0..<10, id: \.self) { i in
                Capsule()
                    .fill(i < min(m.trips, 10) ? (m.isLow ? DNColor.signLow : DNColor.widgetOnInverse) : DNColor.widgetOnInverseTrack)
                    .frame(height: 6)
            }
        }
        .animation(DNMotion.resolve(DNMotion.smooth), value: m.trips)
        .accessibilityHidden(true)
    }

    private func small(_ m: WidgetSalikModel) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                glyph
                Spacer(minLength: 0)
                WidgetQuickSlotView(look: .plus(.onInverse), intent: .topUpSalik)
            }
            .frame(height: 44)
            figure(m).padding(.top, 6)
            tripsLine(m)
            Spacer(minLength: 8)
            pips(m)
        }
        .padding(EdgeInsets(top: 12, leading: 16, bottom: 16, trailing: 12))
    }

    private func medium(_ m: WidgetSalikModel) -> some View {
        let lastTopUp = store.state.history.first { $0.serviceID == "top-up-salik-account" && $0.amount != nil }
        return HStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 10) {
                    glyph
                    WidgetTitle("Salik", color: DNColor.widgetOnInverse)
                }
                .frame(height: 44)
                figure(m).padding(.top, 6)
                tripsLine(m)
                Spacer(minLength: 8)
                pips(m)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Rectangle().fill(DNColor.widgetOnInverseRule).frame(width: 1)
            VStack(alignment: .leading, spacing: 8) {
                WidgetFactRow(label: "Last top-up", value: lastTopUp?.amount.map { Money.aed($0, trimWhole: true) } ?? "None", onDark: true)
                WidgetFactRow(label: "On", value: lastTopUp.map { DemoDate.short($0.date) } ?? "–", onDark: true)
                WidgetFactRow(label: "Each gate", value: "AED 4", onDark: true)
                Spacer(minLength: 6)
                WidgetQuickSlotView(look: .capsule("Top up", symbol: "plus", tone: .white), intent: .topUpSalik)
            }
            .padding(.top, 6)
            .frame(maxWidth: .infinity)
        }
        .padding(EdgeInsets(top: 12, leading: 16, bottom: 16, trailing: 16))
    }
}

/// The faint dashed lane marking behind the Salik balance.
struct WidgetRoadMarking: View {
    var body: some View {
        Canvas { ctx, size in
            var lane = Path()
            lane.move(to: CGPoint(x: size.width * 0.674, y: size.height))
            lane.addLine(to: CGPoint(x: size.width * 0.857, y: size.height * 0.526))
            ctx.stroke(lane, with: .color(DNColor.white.opacity(0.07)), style: StrokeStyle(lineWidth: 10, dash: [16, 14]))
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

extension WidgetSpec {
    static func salik(store: DemoStore) -> WidgetSpec {
        guard store.widgetSalikLinked else {
            return WidgetSpec(surface: .inverse, summary: "Salik. Not connected. Link your account to see your balance.",
                              quick: .connect("Salik"), open: .sheet(.topUpSalik))
        }
        let m = WidgetSalikModel(balance: store.state.salikBalance)
        return WidgetSpec(
            surface: .inverse,
            summary: "Salik balance, \(Money.spoken(m.balance)). \(m.tripsLine.replacingOccurrences(of: " · ", with: ", ")).",
            hint: "Opens Top up Salik",
            quick: .topUpSalik,
            open: .sheet(.topUpSalik)
        )
    }
}

// MARK: - nol card

struct WidgetNolContent: View {
    @Environment(DemoStore.self) private var store

    var body: some View {
        if let balance = store.state.nolBalance {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    WidgetNolCard()
                    Spacer(minLength: 0)
                    WidgetQuickSlotView(look: .plus(.subtle), intent: .topUpNol)
                }
                .frame(height: 44)
                WidgetFigure(Money.figure(balance, trimWhole: true), leadingUnit: "AED",
                             numeric: NSDecimalNumber(decimal: balance).doubleValue)
                    .padding(.top, 6)
                WidgetCaption("On your nol card")
                Spacer(minLength: 8)
                if balance < 15 {
                    WidgetChip(text: "Low for a return", symbol: "exclamationmark.triangle.fill",
                               foreground: DNColor.statusWarningForeground, background: DNColor.statusWarningSurface, height: 26)
                } else {
                    WidgetChip(text: "Enough for today", dot: DNColor.statusSuccessForeground,
                               foreground: DNColor.statusSuccessForeground, background: DNColor.statusSuccessSurface, height: 26)
                }
            }
            .padding(EdgeInsets(top: 12, leading: 16, bottom: 16, trailing: 12))
        } else {
            WidgetCalmState(
                title: "nol card",
                message: "Add your card to see its balance",
                quick: (WidgetQuickLook.capsule("Add card", height: 36), WidgetQuickIntent.connect("nol"))
            ) { WidgetNolCard() }
        }
    }
}

extension WidgetSpec {
    static func nol(store: DemoStore) -> WidgetSpec {
        guard let balance = store.state.nolBalance else {
            return WidgetSpec(summary: "nol card. Not added yet.", quick: .connect("nol"), open: .service("top-up-nol-card"))
        }
        let low = balance < 15 ? " Low for a return trip." : ""
        return WidgetSpec(summary: "nol card balance, \(Money.spoken(balance)).\(low)", hint: "Opens Top up your nol card",
                          quick: .topUpNol, open: .service("top-up-nol-card"))
    }
}

// MARK: - Metro departures

enum WidgetMetroTick {
    /// Minutes until this departure and the one after, counting down from launch and wrapping
    /// by the line's headway, so the board keeps moving in a long demo.
    static func minutes(_ d: MetroDeparture, now: Date) -> (now: Int, next: Int) {
        let elapsed = max(0, Int(now.timeIntervalSince(WidgetClock.launch) / 60))
        let headway = max(1, d.nextInMinutes - d.inMinutes)
        var m = d.inMinutes - elapsed
        if m < 0 { m = ((m % headway) + headway) % headway }
        return (m, m + headway)
    }

    static func elapsed(_ now: Date) -> Int { max(0, Int(now.timeIntervalSince(WidgetClock.launch) / 60)) }
}

struct WidgetMetroContent: View {
    var now: Date
    @Environment(DemoStore.self) private var store

    var body: some View {
        if let station = store.state.metro {
            VStack(spacing: 0) {
                HStack(spacing: 8) {
                    Image(systemName: "tram.fill")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(DNColor.widgetOnInverse)
                    WidgetTitle(station.name, color: DNColor.widgetOnInverse)
                    Text(station.line)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(DNColor.white)
                        .lineLimit(1)
                        .padding(.horizontal, 7)
                        .frame(height: 20)
                        .background(lineColor(station.line), in: .rect(cornerRadius: 6, style: .continuous))
                    Spacer(minLength: 4)
                    WidgetLivePulse(trigger: WidgetMetroTick.elapsed(now))
                    Text("Live")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(DNColor.widgetOnInverseSecondary)
                }
                .frame(height: 32)
                ForEach(Array(station.departures.prefix(2).enumerated()), id: \.offset) { i, d in
                    if i > 0 { Rectangle().fill(DNColor.widgetOnInverseRule).frame(height: 1) }
                    row(d, towardsEnd: i > 0)
                }
            }
            .padding(EdgeInsets(top: 14, leading: 18, bottom: 12, trailing: 18))
        } else {
            WidgetCalmState(title: "Metro", message: "Choose a station to see its next trains", onDark: true) {
                Image(systemName: "tram.fill").font(.system(size: 20, weight: .semibold)).foregroundStyle(DNColor.widgetOnInverse)
            }
        }
    }

    private func lineColor(_ line: String) -> Color {
        line.lowercased().contains("green") ? DNColor.signMetroGreen : DNColor.signMetroRed
    }

    private func row(_ d: MetroDeparture, towardsEnd: Bool) -> some View {
        let t = WidgetMetroTick.minutes(d, now: now)
        return HStack(spacing: 10) {
            Image(systemName: towardsEnd ? "arrow.left" : "arrow.right")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(DNColor.widgetOnInverseSecondary)
            Text(d.towards)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(DNColor.widgetOnInverse)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Spacer(minLength: 8)
            HStack(alignment: .firstTextBaseline, spacing: 3) {
                if t.now == 0 {
                    Text("Now").font(.system(size: 24, weight: .bold, design: .rounded))
                } else {
                    Text("\(t.now)")
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .contentTransition(.numericText(value: Double(t.now)))
                    Text("min").font(.system(size: 15, weight: .semibold, design: .rounded))
                }
            }
            .foregroundStyle(DNColor.signLed)
            .animation(DNMotion.resolve(DNMotion.smooth), value: t.now)
            Text("\(t.next) min")
                .font(.system(size: 15, weight: .medium, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(DNColor.widgetOnInverseSecondary)
                .frame(width: 56, alignment: .trailing)
                .contentTransition(.numericText(value: Double(t.next)))
                .animation(DNMotion.resolve(DNMotion.smooth), value: t.next)
        }
        .frame(maxHeight: .infinity)
    }
}

/// The green Live dot. When a pushed update arrives, one ring expands 1 → 2.4× and fades (900 ms).
/// Never loops; Reduce Motion keeps a static dot.
struct WidgetLivePulse: View {
    var trigger: Int
    @State private var pulsed = true
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Circle()
            .fill(DNColor.signLive)
            .frame(width: 7, height: 7)
            .background {
                if !reduceMotion {
                    Circle()
                        .stroke(DNColor.signLive, lineWidth: 1.5)
                        .scaleEffect(pulsed ? 2.4 : 1)
                        .opacity(pulsed ? 0 : 0.9)
                }
            }
            .onChange(of: trigger) { _, _ in pulse() }
            .onAppear { pulse() }
            .accessibilityHidden(true)
    }

    private func pulse() {
        guard !reduceMotion else { return }
        pulsed = false
        withAnimation(.easeOut(duration: 0.9)) { pulsed = true }
    }
}

extension WidgetSpec {
    static func metro(store: DemoStore, now: Date) -> WidgetSpec {
        let soon = ComingSoon(title: "Metro departures", detail: "Live departures come from RTA. They aren't part of this demo yet.",
                              owner: "RTA", symbol: "tram.fill", hue: .sky, kind: .notInDemo)
        guard let station = store.state.metro else {
            return WidgetSpec(surface: .board, summary: "Metro. No station chosen.", open: .comingSoon(soon))
        }
        let lines = station.departures.prefix(2).map { d -> String in
            let t = WidgetMetroTick.minutes(d, now: now)
            return t.now == 0 ? "Towards \(d.towards) now, then in \(t.next) minutes"
                              : "Towards \(d.towards) in \(t.now) minutes, then \(t.next)"
        }
        return WidgetSpec(surface: .board, summary: "\(station.name) metro, \(station.line), live. " + lines.joined(separator: ". "),
                          open: .comingSoon(soon))
    }
}
