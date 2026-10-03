import SwiftUI

// MARK: - What each Services screen lists

/// The lists every Services screen shows. Views and the reachability check
/// (`ServicesReachability`) read the same functions, so "every record is reachable" is
/// checked against what's actually on screen.
enum ServicesData {
    private static var catalogue: Catalogue { .shared }

    // MARK: Topics

    /// Sub-topics of a topic that have at least one record, in catalogue order.
    static func subtopics(of topic: CatalogueTopic) -> [CatalogueSubtopic] {
        topic.subtopics.filter { !services(topic: topic.id, subtopic: $0.id).isEmpty }
    }

    /// Everything in a sub-topic: standalone services (most used first), then the
    /// bundles and journeys that live in it.
    static func services(topic: String, subtopic: String) -> [CatalogueService] {
        let all = catalogue.services.filter { $0.topic == topic && $0.subtopic == subtopic }
        return all.filter { $0.type == .standalone }.sorted { $0.uniques30d > $1.uniques30d }
            + all.filter { $0.type != .standalone }.sorted { $0.name < $1.name }
    }

    /// The rows a topic page shows for a sub-topic before "See all": the 3 most used standalone services.
    static func preview(topic: String, subtopic: String, limit: Int = 3) -> [CatalogueService] {
        let all = services(topic: topic, subtopic: subtopic)
        let standalone = all.filter { $0.type == .standalone }
        return Array((standalone.isEmpty ? all : standalone).prefix(limit))
    }

    /// Bundles and journeys that belong to a topic (journeys first, as cards).
    static func bundlesAndJourneys(inTopic topic: String) -> [CatalogueService] {
        let multi = catalogue.services.filter { $0.topic == topic && $0.type != .standalone }
        return multi.filter { $0.type == .journey } + multi.filter { $0.type == .bundle }
    }

    /// Standalone services in a topic whose sub-topic isn't listed (a guard: none today).
    static func unfiled(inTopic topic: CatalogueTopic) -> [CatalogueService] {
        let known = Set(topic.subtopics.map(\.id))
        return catalogue.services.filter {
            $0.topic == topic.id && $0.type == .standalone && !known.contains($0.subtopic ?? "")
        }
    }

    /// "38 services from RTA, Dubai Police, Parkin and 6 others"
    static func topicLine(_ topic: CatalogueTopic) -> String {
        var counts: [String: Int] = [:]
        for s in catalogue.services where s.topic == topic.id && s.type == .standalone {
            if let o = s.owner, o != "DubaiNow" { counts[o, default: 0] += 1 }
        }
        let owners = counts.sorted { $0.value != $1.value ? $0.value > $1.value : $0.key < $1.key }.map(\.key)
        let n = "\(topic.services) \(topic.services == 1 ? "service" : "services")"
        switch owners.count {
        case 0: return n
        case 1: return "\(n) from \(owners[0])"
        case 2...4: return "\(n) from \(owners.dropLast().joined(separator: ", ")) and \(owners[owners.count - 1])"
        default: return "\(n) from \(owners.prefix(3).joined(separator: ", ")) and \(owners.count - 3) others"
        }
    }

    // MARK: A to Z

    enum Filter: String, CaseIterable, Identifiable {
        case all, standalone, bundle, journey
        var id: String { rawValue }
        var label: String {
            switch self {
            case .all: "All"
            case .standalone: "Standalone"
            case .bundle: "All at once"
            case .journey: "Start to finish"
            }
        }
    }

    static func aToZ(_ filter: Filter) -> [CatalogueService] {
        let all = catalogue.aToZ
        switch filter {
        case .all: return all
        case .standalone: return all.filter { $0.type == .standalone }
        case .bundle: return all.filter { $0.type == .bundle }
        case .journey: return all.filter { $0.type == .journey }
        }
    }

    struct LetterSection: Identifiable {
        var id: String { letter }
        let letter: String
        let services: [CatalogueService]
    }

    static func letterSections(_ services: [CatalogueService]) -> [LetterSection] {
        var order: [String] = []
        var groups: [String: [CatalogueService]] = [:]
        for s in services {
            let first = s.name.first.map { String($0).uppercased() } ?? "#"
            let letter = first.rangeOfCharacter(from: .letters) == nil ? "#" : first
            if groups[letter] == nil { order.append(letter) }
            groups[letter, default: []].append(s)
        }
        return order.map { LetterSection(letter: $0, services: groups[$0] ?? []) }
    }

    // MARK: Entities

    struct Owner: Identifiable, Hashable {
        var id: String { key }
        /// The owner value in the catalogue ("RTA", "DubaiNow", "To confirm").
        let key: String
        let label: String
        let count: Int
        let detail: String?
    }

    /// Owners as listed on "By government entity": DubaiNow (bundles, journeys and its own
    /// services) first, then every entity A to Z, then services whose owner is still to confirm.
    static var owners: [Owner] {
        let named = catalogue.owners.filter { $0.name != "To confirm" }
            .map { Owner(key: $0.name, label: $0.name, count: $0.count, detail: fullName($0.name) == $0.name ? nil : fullName($0.name)) }
        let dubaiNow = catalogue.services(ownedBy: "DubaiNow").count
        let unconfirmed = catalogue.services(ownedBy: "To confirm").count
        var list: [Owner] = []
        if dubaiNow > 0 {
            list.append(Owner(key: "DubaiNow", label: "DubaiNow", count: dubaiNow, detail: "Bundles, journeys and DubaiNow's own services"))
        }
        list += named
        if unconfirmed > 0 {
            list.append(Owner(key: "To confirm", label: "Owner to confirm", count: unconfirmed, detail: "Being checked with government"))
        }
        return list
    }

    /// Government entities and providers that run services (DubaiNow included, "to confirm" not).
    static var entityCount: Int { owners.filter { $0.key != "To confirm" }.count }

    static func services(ownedBy owner: String) -> [CatalogueService] {
        catalogue.services(ownedBy: owner)
    }

    /// The owner's full name for footers ("Roads and Transport Authority (RTA)").
    static func fullName(_ owner: String?) -> String {
        guard let owner else { return "an owner still to be confirmed" }
        let names: [String: String] = [
            "RTA": "Roads and Transport Authority (RTA)",
            "DEWA": "Dubai Electricity and Water Authority (DEWA)",
            "GDRFA": "General Directorate of Residency and Foreign Affairs (GDRFA)",
            "DHA": "Dubai Health Authority (DHA)",
            "DLD": "Dubai Land Department (DLD)",
            "KHDA": "Knowledge and Human Development Authority (KHDA)",
            "CDA": "Community Development Authority (CDA)",
            "IACAD": "Islamic Affairs and Charitable Activities Department (IACAD)",
            "MBRHE": "Mohammed Bin Rashid Housing Establishment (MBRHE)",
            "ICP": "Federal Authority for Identity, Citizenship, Customs and Port Security (ICP)",
            "DET": "Department of Economy and Tourism (DET)",
        ]
        return names[owner] ?? owner
    }

    // MARK: Service pages

    /// Other services in the same sub-topic, most used first.
    static func related(to service: CatalogueService, limit: Int = 4) -> [CatalogueService] {
        guard let t = service.topic, let sub = service.subtopic else { return [] }
        return Array(services(topic: t, subtopic: sub).filter { $0.id != service.id && $0.type == .standalone }.prefix(limit))
    }

    /// Bundles and journeys that include this service as a part.
    static func partOf(_ service: CatalogueService) -> [CatalogueService] {
        catalogue.services.filter { ($0.parts ?? []).contains(service.id) }
    }

    /// Aliases worth showing in English ("Mulkiya, vehicle renewal").
    static func alsoCalled(_ service: CatalogueService) -> String? {
        let latin = service.aliases.filter { alias in
            !alias.unicodeScalars.contains { (0x0600...0x06FF).contains($0.value) }
                && alias.caseInsensitiveCompare(service.name) != .orderedSame
                && alias.caseInsensitiveCompare(service.shortName) != .orderedSame
        }
        guard !latin.isEmpty else { return nil }
        return latin.prefix(3).joined(separator: ", ")
    }

    /// The right "coming next" sheet for a record, by its status.
    static func comingSoon(_ service: CatalogueService, kind: ComingSoon.Kind? = nil, detail: String? = nil) -> ComingSoon {
        let k = kind ?? (service.status == "maintenance" ? .maintenance : service.isLive ? .servicesPhase : .notLiveYet)
        return ComingSoon(
            title: service.name,
            detail: detail,
            owner: service.type == .standalone ? service.ownerLabel : nil,
            symbol: service.type == .journey ? "point.topleft.down.to.point.bottomright.curvepath" : service.symbol,
            hue: service.hue,
            kind: k
        )
    }
}

// MARK: - Personal context (from the persona's state)

/// A short personal note on a catalogue row ("you have 1 unpaid", "due in 12 days").
struct ServicesRowContext {
    var text: String
    var tone: StatusChip.Tone
}

/// Something the resident can do next, shown on Services (For you) and topic banners.
enum ServicesAction {
    case push(Route)
    case pay(PaymentRequest)
    case comingSoon(ComingSoon)
}

struct ServicesForYouItem: Identifiable {
    let id: String
    var symbol: String
    var hue: Hue
    var chip: String
    var chipTone: StatusChip.Tone
    var title: String
    var detail: String
    var action: ServicesAction
}

/// The personal banner at the top of a topic page (Driving: the Patrol's plate + Renew).
struct ServicesBanner {
    var plate: Plate?
    var symbol: String
    var hue: Hue
    var title: String
    var detail: String
    var detailTone: StatusChip.Tone
    var actionTitle: String
    var action: ServicesAction
}

extension Router {
    /// Performs a Services action (push, pay or a "coming next" sheet).
    func servicesPerform(_ action: ServicesAction) {
        switch action {
        case .push(let route): push(route)
        case .pay(let request): present(.payment(request))
        case .comingSoon(let item): present(.comingSoon(item))
        }
    }
}

/// Everything here only reads existing state, so it can be promoted to the store later.
extension DemoStore {
    func servicesPayables(for serviceID: String) -> [Payable] {
        payables.filter { $0.serviceID == serviceID }
    }

    /// Requests that are this service (the Salik dispute is filed under top-ups, so names must match).
    func servicesRequests(for service: CatalogueService) -> [RequestItem] {
        if service.id == "check-visa-status" {
            return state.requests.filter { r in
                Catalogue.shared.service(r.serviceID)?.topic == "visas-and-residency" && r.status != .done
            }
        }
        return state.requests.filter { $0.serviceID == service.id && $0.title == service.name }
    }

    func servicesDraft(for serviceID: String) -> DraftItem? {
        state.drafts.first { $0.serviceID == serviceID }
    }

    func servicesJourney(_ journeyID: String) -> JourneyProgress? {
        state.journeys.first { $0.journeyID == journeyID }
    }

    /// A renewal waiting in Needs you for this service (e.g. the Patrol's registration).
    func servicesRenewal(for serviceID: String) -> NeedsYouItem? {
        state.needsYou.first {
            if case .renewal(let sid, _) = $0.kind { return sid == serviceID }
            return false
        }
    }

    /// The vehicle a renewal refers to, else the one whose registration ends first.
    func servicesVehicleForRenewal() -> Vehicle? {
        if let item = servicesRenewal(for: "renew-vehicle-registration"),
           case .renewal(_, let thing) = item.kind, let thing,
           let v = state.vehicles.first(where: { $0.id == thing }) { return v }
        return state.vehicles.min { $0.registrationExpiry < $1.registrationExpiry }
    }

    /// Personal note for a row, from the persona's bills, renewals, requests and drafts.
    func servicesContext(for service: CatalogueService) -> ServicesRowContext? {
        guard persona != nil else { return nil }
        let owed = servicesPayables(for: service.id)
        if !owed.isEmpty {
            if owed.allSatisfy({ $0.kind == .fine }) {
                return ServicesRowContext(text: "you have \(owed.count) unpaid", tone: .error)
            }
            let total = owed.reduce(Decimal(0)) { $0 + $1.amount }
            if let due = owed.compactMap(\.due).min() {
                return ServicesRowContext(text: "\(Money.aed(total)) \(DemoDate.dueLabel(due).lowercased())", tone: .warning)
            }
            return ServicesRowContext(text: "\(Money.aed(total)) to pay", tone: .warning)
        }
        if let renewal = servicesRenewal(for: service.id), let due = renewal.due {
            return ServicesRowContext(text: DemoDate.dueLabel(due).lowercased(), tone: .warning)
        }
        if let r = servicesRequests(for: service).first {
            switch r.status {
            case .yourTurn: return ServicesRowContext(text: "your turn", tone: .warning)
            case .inReview: return ServicesRowContext(text: "in review", tone: .info)
            case .approved: return ServicesRowContext(text: "approved", tone: .success)
            case .done: break
            }
        }
        if let d = servicesDraft(for: service.id) {
            return ServicesRowContext(text: "draft, \(d.stepsDone) of \(d.stepsTotal) done", tone: .info)
        }
        switch service.id {
        case "pay-for-parking":
            if let p = state.parking, p.endsAt > .now {
                return ServicesRowContext(text: "parked, ends \(DemoDate.countdown(to: p.endsAt))", tone: .info)
            }
        case "top-up-salik-account":
            if !state.vehicles.isEmpty, state.salikBalance < 50 {
                return ServicesRowContext(text: "\(Money.aed(state.salikBalance, trimWhole: true)) left", tone: .warning)
            }
        default: break
        }
        return nil
    }

    /// The personal count for a bundle's count line (nil when there's nothing personal to say).
    func servicesBundleCount(_ service: CatalogueService) -> Int? {
        guard persona != nil else { return nil }
        switch service.id {
        case "pay-all": return payables.isEmpty ? nil : payables.count
        case "renew-family-visas":
            guard let fv = state.familyVisas, !fv.dueMemberIDs.isEmpty else { return nil }
            return fv.dueMemberIDs.count
        default: return nil
        }
    }

    /// "For you" on Services: renewals, your-turn requests, journeys, family visas, drafts, other requests.
    var servicesForYou: [ServicesForYouItem] {
        guard persona != nil else { return [] }
        var items: [ServicesForYouItem] = []
        let cat = Catalogue.shared

        for item in state.needsYou {
            guard case .renewal(let sid, let thing) = item.kind, let s = cat.service(sid) else { continue }
            let vehicle = state.vehicles.first { $0.id == thing }
            items.append(ServicesForYouItem(
                id: "fy-\(item.id)", symbol: s.symbol, hue: s.hue,
                chip: item.due.map { DemoDate.dueLabel($0) } ?? "Due soon", chipTone: .warning,
                title: s.name,
                detail: vehicle.map { "\($0.shortName) · \($0.plate.code) \($0.plate.number)" } ?? item.detail,
                action: .push(.service(sid))
            ))
        }
        let yourTurn = state.requests.filter { if case .yourTurn = $0.status { return true }; return false }
        for r in yourTurn {
            let step = r.currentStepIndex.map { $0 + 1 } ?? 1
            items.append(ServicesForYouItem(
                id: "fy-\(r.id)", symbol: r.symbol, hue: r.hue,
                chip: "\(step) of \(r.steps.count) steps", chipTone: .info,
                title: r.title, detail: "Your turn · continue where you left off",
                action: .push(.request(r.id))
            ))
        }
        for j in state.journeys {
            guard let s = cat.service(j.journeyID) else { continue }
            let total = s.stages?.count ?? 0
            items.append(ServicesForYouItem(
                id: "fy-\(j.journeyID)", symbol: s.symbol, hue: s.hue,
                chip: "Stage \(j.stageIndex + 1) of \(total)", chipTone: .info,
                title: s.name, detail: [j.headline, j.destination].compactMap { $0 }.joined(separator: " · "),
                action: .push(.journey(j.journeyID))
            ))
        }
        if let fv = state.familyVisas, !fv.dueMemberIDs.isEmpty, let s = cat.service("renew-family-visas") {
            let names = state.family.filter { fv.dueMemberIDs.contains($0.id) }.map { $0.name.components(separatedBy: " ").first ?? $0.name }
            items.append(ServicesForYouItem(
                id: "fy-family-visas", symbol: s.symbol, hue: s.hue,
                chip: "\(fv.dueMemberIDs.count) of \(fv.memberIDs.count) due", chipTone: .warning,
                title: s.name, detail: "\(ServicesText.list(names)) · expire \(DemoDate.short(fv.expiresOn))",
                action: .push(.bundle(s.id))
            ))
        }
        for d in state.drafts {
            items.append(ServicesForYouItem(
                id: "fy-\(d.id)", symbol: d.symbol, hue: d.hue,
                chip: "Draft · \(d.stepsDone) of \(d.stepsTotal)", chipTone: .neutral,
                title: cat.service(d.serviceID)?.name ?? d.title, detail: "Saved \(ServicesText.relativeInSentence(d.savedOn))",
                action: .push(.service(d.serviceID))
            ))
        }
        for r in state.requests where !yourTurn.contains(r) {
            let chip: (String, StatusChip.Tone)
            let detail: String
            switch r.status {
            case .inReview(let by): chip = ("In review", .info); detail = "Answer by \(DemoDate.short(by))"
            case .approved(let note): chip = ("Approved", .success); detail = note.prefix(1).uppercased() + note.dropFirst()
            case .done, .yourTurn: continue
            }
            items.append(ServicesForYouItem(
                id: "fy-\(r.id)", symbol: r.symbol, hue: r.hue, chip: chip.0, chipTone: chip.1,
                title: r.title, detail: detail, action: .push(.request(r.id))
            ))
        }
        return Array(items.prefix(6))
    }

    /// The personal banner on a topic page: a renewal first, then a step that's your turn,
    /// then a bill or fine (soonest first), then a journey in progress.
    func servicesBanner(forTopic topicID: String) -> ServicesBanner? {
        guard persona != nil else { return nil }
        let cat = Catalogue.shared
        func inTopic(_ id: String?) -> Bool { id.flatMap { cat.service($0)?.topic } == topicID }

        for item in state.needsYou {
            guard case .renewal(let sid, let thing) = item.kind, inTopic(sid) else { continue }
            let vehicle = state.vehicles.first { $0.id == thing }
            return ServicesBanner(
                plate: vehicle?.plate, symbol: item.symbol, hue: item.hue,
                title: vehicle == nil ? item.title : "Registration due",
                detail: item.due.map { ServicesText.inDays($0) } ?? item.detail, detailTone: .warning,
                actionTitle: item.primaryAction, action: .push(.service(sid))
            )
        }
        for item in state.needsYou {
            guard case .requestStep(let rid) = item.kind, let r = request(rid), inTopic(r.serviceID) else { continue }
            return ServicesBanner(
                plate: nil, symbol: item.symbol, hue: item.hue, title: item.title,
                detail: item.due.map { "\(r.title) · \(DemoDate.dueLabel($0).lowercased())" } ?? r.title,
                detailTone: .warning, actionTitle: item.primaryAction, action: .push(.request(rid))
            )
        }
        let owed = payables.filter { inTopic($0.serviceID) }
            .sorted { ($0.due ?? .distantFuture) < ($1.due ?? .distantFuture) }
        if let p = owed.first {
            return ServicesBanner(
                plate: nil, symbol: p.symbol, hue: p.hue, title: p.title,
                detail: p.due.map { "\(Money.aed(p.amount)) · \(DemoDate.dueLabel($0).lowercased())" } ?? "\(Money.aed(p.amount)) · \(p.note ?? "unpaid")",
                detailTone: p.kind == .fine ? .error : .warning,
                actionTitle: "Pay", action: .pay(paymentRequest(for: p))
            )
        }
        if let j = state.journeys.first(where: { cat.service($0.journeyID)?.topic == topicID }), let s = cat.service(j.journeyID) {
            return ServicesBanner(
                plate: nil, symbol: s.symbol, hue: s.hue, title: s.name,
                detail: "Stage \(j.stageIndex + 1) of \(s.stages?.count ?? 0) · \(j.headline)", detailTone: .info,
                actionTitle: "Continue", action: .push(.journey(s.id))
            )
        }
        return nil
    }

    /// One payment for every payable that belongs to a service.
    func servicesPaymentRequest(for owed: [Payable]) -> PaymentRequest? {
        guard let first = owed.first else { return nil }
        if owed.count == 1 { return paymentRequest(for: first) }
        return PaymentRequest(
            id: "pay-\(owed.map(\.id).joined(separator: "-"))",
            title: "Pay \(owed.count) \(first.kind == .fine ? "fines" : "bills")",
            lines: owed.map { PaymentLine(title: $0.title, detail: $0.entity, amount: $0.amount, symbol: $0.symbol, hue: $0.hue) },
            purpose: .payables(owed.map(\.id))
        )
    }
}

// MARK: - Text helpers

enum ServicesText {
    /// "Layla and Omar", "Ahmed, Layla and Omar"
    static func list(_ items: [String]) -> String {
        switch items.count {
        case 0: return ""
        case 1: return items[0]
        default: return items.dropLast().joined(separator: ", ") + " and " + (items.last ?? "")
        }
    }

    /// "in 12 days", "tomorrow", "today", "3 days ago"
    static func inDays(_ date: Date) -> String {
        let n = DemoDate.daysFromToday(date)
        switch n {
        case ..<(-1): return "\(-n) days ago"
        case -1: return "yesterday"
        case 0: return "today"
        case 1: return "tomorrow"
        default: return "in \(n) days"
        }
    }

    /// "today", "yesterday", "3 days ago", "on 25 Sep" — for use mid-sentence.
    static func relativeInSentence(_ date: Date) -> String {
        let r = DemoDate.relative(date)
        let n = DemoDate.daysFromToday(date)
        return (-6...0).contains(n) ? r.lowercased() : "on \(r)"
    }

    static func count(_ n: Int, _ singular: String, _ plural: String? = nil) -> String {
        "\(n) \(n == 1 ? singular : (plural ?? singular + "s"))"
    }
}
