import SwiftUI

// What each widget kind is: its sizes, who owns the data, how it reads in menus and the gallery.
// Members are prefixed `widget…` so they can't clash with other features' extensions.

extension WidgetKind {
    /// Sizes this kind is designed in (Paper launch set + bundles and journeys). First is the default.
    var widgetSizes: [WidgetSize] {
        switch self {
        case .parking, .salik, .billsAndFines, .familyVisas: [.small, .medium]
        case .prayer: [.small, .large]
        case .weather, .nol, .visaTracker: [.small]
        case .metro, .energy: [.medium]
        case .journey: [.medium, .large]
        case .certificates: []
        }
    }

    /// The size to draw when Home asks for one this kind isn't designed in.
    func widgetLayoutSize(for requested: WidgetSize) -> WidgetSize {
        let sizes = widgetSizes
        if sizes.contains(requested) || sizes.isEmpty { return requested }
        switch requested {
        case .small: return sizes.first ?? requested
        case .medium: return sizes.contains(.large) ? .large : (sizes.first ?? requested)
        case .large: return sizes.contains(.medium) ? .medium : (sizes.first ?? requested)
        }
    }

    /// Who owns the data ("Small · Parkin").
    var widgetOwner: String {
        switch self {
        case .parking: "Parkin"
        case .salik: "Salik"
        case .prayer: "IACAD"
        case .weather: "NCM and Dubai Municipality"
        case .metro, .nol: "RTA"
        case .energy: "DEWA"
        case .billsAndFines: "Pay all"
        case .familyVisas: "GDRFA"
        case .certificates: "DubaiNow"
        case .journey(let id):
            Catalogue.shared.service(id)?.ownerLabel ?? "DubaiNow"
        case .visaTracker: "Your request"
        }
    }

    var widgetSymbol: String {
        switch self {
        case .parking: "p.square.fill"
        case .salik: "road.lanes"
        case .prayer: "moon.stars.fill"
        case .weather: "sun.max.fill"
        case .metro: "tram.fill"
        case .energy: "bolt.fill"
        case .nol: "creditcard.fill"
        case .billsAndFines: "creditcard.fill"
        case .familyVisas: "person.2.fill"
        case .certificates: "doc.text.fill"
        case .journey: "point.topleft.down.to.point.bottomright.curvepath"
        case .visaTracker: "person.text.rectangle.fill"
        }
    }

    /// Words people might search the gallery with.
    var widgetSearchTerms: [String] {
        switch self {
        case .parking: ["parking", "parkin", "zone", "car", "extend"]
        case .salik: ["salik", "toll", "gate", "balance", "top up", "car"]
        case .prayer: ["prayer", "salah", "namaz", "adhan", "fajr", "dhuhr", "asr", "maghrib", "isha", "faith", "mosque"]
        case .weather: ["weather", "beach", "swim", "temperature", "uv", "flag", "sea"]
        case .metro: ["metro", "train", "rta", "station", "departures", "red line", "transport"]
        case .energy: ["energy", "electricity", "dewa", "kwh", "meter", "bill", "home"]
        case .nol: ["nol", "card", "metro", "bus", "balance", "top up"]
        case .billsAndFines: ["bills", "fines", "pay all", "dewa", "money", "due"]
        case .familyVisas: ["family", "visa", "visas", "residency", "renew", "children", "spouse"]
        case .certificates: ["certificates", "letters"]
        case .journey(let id): ["journey", "start to finish"] + (Catalogue.shared.service(id).map { [$0.name.lowercased()] } ?? [])
        case .visaTracker: ["request", "tracker", "visa", "application", "status"]
        }
    }

    /// The catalogue record a widget opens (for "Open …" and VoiceOver hints).
    var widgetServiceID: String? {
        switch self {
        case .parking: "pay-for-parking"
        case .salik: "top-up-salik-account"
        case .prayer: "check-prayer-times"
        case .energy: "check-utility-consumption"
        case .nol: "top-up-nol-card"
        case .billsAndFines: "pay-all"
        case .familyVisas: "renew-family-visas"
        case .certificates: "get-certificates"
        case .journey(let id): id
        case .weather, .metro, .visaTracker: nil
        }
    }
}

extension WidgetSize {
    var widgetLabel: String {
        switch self {
        case .small: "Small"
        case .medium: "Medium"
        case .large: "Large"
        }
    }

    var widgetSymbol: String {
        switch self {
        case .small: "square.fill"
        case .medium: "rectangle.fill"
        case .large: "square.grid.2x2.fill"
        }
    }
}

// MARK: - Store helpers (read-only views of existing state)

extension DemoStore {
    /// Salik is linked when the persona has a linked Salik account (or any balance).
    var widgetSalikLinked: Bool {
        state.linkedAccounts.contains { $0.id == "salik" && $0.isLinked } || state.salikBalance > 0
    }

    /// DEWA is linked when there's an energy reading.
    var widgetDewaLinked: Bool { state.energy != nil }

    /// The request a visa tracker widget follows: the given one, else the first one that's your turn.
    func widgetRequest(_ id: String) -> RequestItem? {
        state.requests.first { $0.id == id }
    }

    /// Puts a removed widget back where it was (Undo).
    func widgetRestore(_ widget: HomeWidget, at index: Int) {
        guard !state.widgets.contains(where: { $0.id == widget.id }) else { return }
        withAnimation(DNMotion.resolve(DNMotion.bouncy)) {
            state.widgets.insert(widget, at: min(max(index, 0), state.widgets.count))
        }
    }

    /// Adds a widget and returns it (so Undo can remove exactly that one).
    @discardableResult
    func widgetAdd(_ kind: WidgetKind, size: WidgetSize) -> HomeWidget? {
        let before = Set(state.widgets.map(\.id))
        addWidget(kind, size: size)
        return state.widgets.last { !before.contains($0.id) }
    }
}

// MARK: - Bills and fines (Pay all)

/// The month at a glance for the Bills and fines widget: what's still to come, what's paid.
struct WidgetBillsModel {
    var toCome: [Payable]
    var total: Decimal
    var paidThisMonth: Decimal
    var monthName: String
    var next: Payable?
    var overdueCount: Int

    init(store: DemoStore, now: Date = .now) {
        toCome = store.payables
        total = store.payAllTotal
        let cal = Calendar.current
        let parts = Set((Catalogue.shared.service("pay-all")?.parts ?? []) + ["pay-all"])
        paidThisMonth = store.state.history
            .filter { cal.isDate($0.date, equalTo: now, toGranularity: .month) && parts.contains($0.serviceID ?? "") }
            .reduce(0) { $0 + ($1.amount ?? 0) }
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_GB")
        f.dateFormat = "MMMM"
        monthName = f.string(from: now)
        next = toCome.filter { $0.due != nil }.min { ($0.due ?? .distantFuture) < ($1.due ?? .distantFuture) } ?? toCome.first
        overdueCount = toCome.filter { ($0.due.map { DemoDate.daysFromToday($0) } ?? 0) < 0 }.count
    }

    var allPaid: Bool { toCome.isEmpty && paidThisMonth > 0 }
    var nothingYet: Bool { toCome.isEmpty && paidThisMonth == 0 }

    /// "tomorrow", "in 3 days", "13 Oct", or nil when it has no due date.
    var nextWhen: String? {
        guard let due = next?.due else { return nil }
        let label = DemoDate.dueLabel(due)
        if label == "Overdue" { return "overdue" }
        return label.replacingOccurrences(of: "Due ", with: "")
    }

    /// "Next due tomorrow"
    var nextChip: String? {
        guard let when = nextWhen else { return toCome.isEmpty ? nil : "Pay when ready" }
        return when == "overdue" ? "Overdue" : "Next due \(when)"
    }

    var paidFraction: Double {
        let paid = NSDecimalNumber(decimal: paidThisMonth).doubleValue
        let left = NSDecimalNumber(decimal: total).doubleValue
        return paid + left > 0 ? paid / (paid + left) : 0
    }
}

// MARK: - Family visas

struct WidgetFamilyModel {
    struct Member: Identifiable {
        var id: String
        var name: String
        var initial: String
        var days: Int?
        var isDue: Bool
    }

    var members: [Member]
    var dueCount: Int
    var days: Int

    init?(store: DemoStore) {
        guard let status = store.state.familyVisas else { return nil }
        let family = store.state.family
        let all: [Member] = status.memberIDs.compactMap { id in
            guard let m = family.first(where: { $0.id == id }) else { return nil }
            return Member(
                id: id,
                name: m.name.split(separator: " ").first.map(String.init) ?? m.name,
                initial: m.initials,
                days: m.visaExpiry.map { DemoDate.daysFromToday($0) },
                isDue: status.dueMemberIDs.contains(id)
            )
        }
        members = all.filter(\.isDue) + all.filter { !$0.isDue }
        dueCount = status.dueMemberIDs.count
        days = DemoDate.daysFromToday(status.expiresOn)
    }
}

// MARK: - Journeys

struct WidgetJourneyModel {
    var service: CatalogueService
    var progress: JourneyProgress
    var stages: [CatalogueService.Stage]

    init?(id: String, store: DemoStore) {
        guard let service = Catalogue.shared.service(id),
              let progress = store.state.journeys.first(where: { $0.journeyID == id }) else { return nil }
        self.service = service
        self.progress = progress
        self.stages = service.stages ?? []
    }

    var total: Int { max(stages.count, 1) }
    var number: Int { min(progress.stageIndex + 1, total) }
    var stageName: String {
        progress.stageIndex < stages.count ? stages[progress.stageIndex].name : progress.headline
    }

    /// Sensitive journeys use the quiet tone: white, no hue, no big figure, no countdown.
    var isQuiet: Bool { service.id == "after-a-death" }

    var caption: String {
        if let d = progress.destination { return "Start to finish · \(d)" }
        return "Start to finish"
    }

    /// "Day 4", "Week 2", "Month 9" since it started.
    var age: String {
        let d = max(0, -DemoDate.daysFromToday(progress.startedOn))
        if d < 7 { return "Day \(d + 1)" }
        if d < 60 { return "Week \(d / 7 + 1)" }
        return "Month \(d / 30 + 1)"
    }

    var spoken: String {
        let turn = progress.needsYou ? "1 thing needs you" : progress.waitingOn
        return "\(service.shortName), stage \(number) of \(total), \(stageName), \(turn)"
    }
}
