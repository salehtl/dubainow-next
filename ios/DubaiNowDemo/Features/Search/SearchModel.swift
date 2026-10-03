import Foundation

// What Search shows for a query, in the Paper order (04 · Search · "How results work"):
// 1 Direct answer · 2 Top result · 3 Services (at most 3, then See all) · 4 Your things ·
// 5 Widgets you can add, then help and guides.

// MARK: - The resident's own things

/// Something of the resident's that search can find: an account, a vehicle, a document,
/// a request, a bill. Things with a `card` can be the Top result, with one useful action.
struct SearchThing: Identifiable, Hashable {
    enum Leading: Hashable {
        case icon(String, Hue)
        case plate(Plate)
        case avatar(String, Hue)
    }

    enum Action: Hashable {
        case push(Route)
        case sheet(SheetRoute)
        case pay(Payable)
        case service(String)
    }

    /// The compact Top result card: dark disc · name · "Balance AED 18 · low" · one action.
    struct Card: Hashable {
        var symbol: String
        var title: String
        var label: String
        var value: String
        var note: String? = nil
        var noteTone: StatusChip.Tone = .neutral
        var button: String
        var buttonAction: Action
    }

    let id: String
    var title: String
    var subtitle: String
    var tone: StatusChip.Tone = .neutral
    var leading: Leading
    var keywords: [String]
    var action: Action
    var card: Card? = nil
    /// The direct answer that already shows this thing (so it isn't listed twice).
    var coveredBy: SearchIntent? = nil
    var priority: Double = 0
}

// MARK: - Direct answers

/// Questions the demo can answer from the resident's own data.
enum SearchIntent: Hashable {
    case owe
    case request(String)      // request id
    case salikBalance
    case nolBalance
    case prayer(String?)      // a named prayer, or the next one
    case parking

    var isPersonal: Bool {
        if case .prayer = self { return false }
        return true
    }
}

enum SearchIntentDetector {
    private static let prayerNames: [String: String] = [
        "fajr": "Fajr", "dhuhr": "Dhuhr", "zuhr": "Dhuhr", "duhr": "Dhuhr", "dhuhur": "Dhuhr",
        "asr": "Asr", "maghrib": "Maghrib", "magrib": "Maghrib", "isha": "Isha", "ishaa": "Isha",
    ]
    private static let prayerWords: Set<String> = ["prayer", "prayers", "salah", "salat", "namaz", "adhan", "azan", "athan"]
    private static let statusWords: Set<String> = [
        "status", "when", "done", "ready", "progress", "track", "tracking", "renewal", "renewing", "expire", "expires",
        "expiry", "application", "arrive", "arriving", "update", "happening", "approved", "card",
    ]
    private static let oweWords: Set<String> = ["owe", "owed", "owing", "outstanding", "dues", "unpaid", "debt"]
    private static let billsOnly: Set<String> = ["bill", "bills", "fine", "fines", "my", "pay", "all", "total", "and", "the", "what", "are", "show", "me", "i", "have"]
    private static let parkingOnly: Set<String> = [
        "parking", "park", "my", "time", "left", "how", "long", "much", "session", "when", "does", "do", "end", "ends",
        "expire", "extend", "is", "the", "i", "have", "remaining", "current", "car", "parked",
    ]

    /// `words` are all the normalised words, stop words included.
    static func detect(_ words: [String], requests: [RequestItem]) -> SearchIntent? {
        let set = Set(words)
        guard !set.isEmpty else { return nil }

        if let named = words.compactMap({ prayerNames[$0] }).first { return .prayer(named) }
        if !set.isDisjoint(with: prayerWords) { return .prayer(nil) }

        if !set.isDisjoint(with: oweWords) { return .owe }
        if set.isSubset(of: billsOnly), !set.isDisjoint(with: ["bill", "bills", "fine", "fines"]) { return .owe }

        if set.contains("salik"), !set.isDisjoint(with: ["balance", "much", "left", "credit", "money"]) { return .salikBalance }
        if set.contains("nol"), !set.isDisjoint(with: ["balance", "much", "left", "credit", "money"]) { return .nolBalance }
        if set == ["balance"] || set == ["my", "balance"] { return .salikBalance }

        if !set.isDisjoint(with: ["parking", "park", "parked"]), set.isSubset(of: parkingOnly) { return .parking }

        if !set.isDisjoint(with: statusWords) {
            for r in requests {
                let keys = requestKeywords(r)
                if !set.isDisjoint(with: keys) { return .request(r.id) }
            }
        }
        return nil
    }

    static func requestKeywords(_ r: RequestItem) -> Set<String> {
        var keys = Set(SearchText.tokens(r.title + " " + r.entity)).subtracting(SearchText.stopWords)
        keys.subtract(["renew", "apply", "get", "check"])
        if keys.contains("visa") || keys.contains("residence") { keys.formUnion(["visa", "residency", "iqama", "amer"]) }
        if keys.contains("licence") { keys.formUnion(["license", "licence"]) }
        return keys
    }
}

// MARK: - Results

struct SearchResults {
    var analysis: SearchQueryAnalysis
    var intent: SearchIntent?
    /// True when a guest asked something personal: show "Sign in to see answers…".
    var needsSignIn = false
    var topThing: SearchThing?
    var topService: SearchMatch?
    var services: [SearchMatch] = []
    var servicesTitle = "Services"
    var things: [SearchThing] = []
    var widgets: [WidgetKind] = []
    var guides: [SearchMatch] = []

    var hasAnswer: Bool { intent != nil || needsSignIn }
    var isEmpty: Bool { !hasAnswer && topThing == nil && topService == nil && services.isEmpty && things.isEmpty && widgets.isEmpty && guides.isEmpty }

    /// For VoiceOver: "6 results".
    var count: Int {
        (hasAnswer ? 1 : 0) + (topThing == nil ? 0 : 1) + (topService == nil ? 0 : 1) + services.count + things.count + widgets.count + guides.count
    }
}

extension DemoStore {
    /// Everything Search needs for one query. Pure: reads the catalogue and the resident's state.
    func searchResults(for raw: String, exact: Bool) -> SearchResults {
        let things = isGuest ? [] : searchThings()
        let ownWords = Set(things.flatMap(\.keywords))
        let analysis = SearchIndex.shared.analyse(raw, extraWords: ownWords, allowCorrection: !exact)
        var r = SearchResults(analysis: analysis)
        guard !analysis.isEmpty else { return r }

        // 1 · Direct answer
        if let intent = SearchIntentDetector.detect(analysis.allWords, requests: state.requests) {
            if isGuest, intent.isPersonal {
                r.needsSignIn = true
            } else if searchCanAnswer(intent) {
                r.intent = intent
            }
        }

        // Services, ranked; the resident's own services get a small boost.
        let personal = Set(state.needsYou.compactMap(\.searchServiceID) + state.requests.map(\.serviceID) + state.drafts.map(\.serviceID))
        var matches = SearchIndex.shared.services(for: analysis, personal: personal)
        let guides = matches.filter { $0.service.kind == "info" }
        let catalogueThings = matches.filter { $0.service.kind == "thing" }
        matches.removeAll { $0.service.kind != "service" }
        r.guides = Array(guides.prefix(2))

        // 4 · Your things (and catalogue things like "Your vehicles")
        var ownMatches = searchMatch(things, words: analysis.words)
        if let intent = r.intent { ownMatches.removeAll { $0.coveredBy == intent } }
        if !isGuest {
            ownMatches += catalogueThings.prefix(2).map { m in
                SearchThing(id: "cat-\(m.service.id)", title: m.service.name, subtitle: m.service.ownerLabel,
                            leading: .icon(m.service.symbol, m.service.hue), keywords: [], action: .service(m.service.id))
            }
        }

        // 2 · Top result: a local term's service, else one of the resident's accounts, else a clear service match.
        if r.intent == nil, !r.needsSignIn {
            let cardIndex = ownMatches.firstIndex { $0.card != nil && $0.priority >= 80 }
            // A local term names a service ("Mulkiya") unless it names one of your accounts ("Salik").
            let termNamesThing: Bool = {
                guard let term = analysis.localTerm, let i = cardIndex, let card = ownMatches[i].card else { return false }
                return SearchText.normalise(card.title).contains(term.phrase.joined(separator: " "))
            }()
            let clearWinner: SearchMatch? = {
                guard let first = matches.first, first.isExact else { return nil }
                return matches.count == 1 || first.score - matches[1].score >= 8 || !matches[1].isExact ? first : nil
            }()
            if analysis.localTerm != nil, !termNamesThing, let first = matches.first, first.isExact {
                r.topService = first
            } else if let winner = clearWinner, searchContext(for: winner.service) != nil {
                // "Renew my car registration": the service, with the resident's Patrol on it.
                r.topService = winner
            } else if let i = cardIndex {
                r.topThing = ownMatches.remove(at: i)
            } else if let winner = clearWinner {
                r.topService = winner
            }
        }
        if let top = r.topService { matches.removeAll { $0.id == top.id } }

        // 3 · Services (Related services after an answer; Also matching under a top service)
        if let intent = r.intent {
            let related = searchRelatedServiceIDs(for: intent).compactMap { id -> SearchMatch? in
                guard let s = Catalogue.shared.service(id) else { return nil }
                return SearchMatch(service: s, score: 0, highlight: nil, alsoCalled: nil, isExact: false)
            }
            var seen = Set<String>()
            r.services = (related + matches).filter { seen.insert($0.id).inserted }
            r.servicesTitle = "Related services"
        } else {
            r.services = matches
            r.servicesTitle = r.topService == nil ? "Services" : "Also matching"
        }

        r.things = ownMatches
        r.widgets = searchWidgets(for: analysis.words)
        return r
    }

    private func searchCanAnswer(_ intent: SearchIntent) -> Bool {
        switch intent {
        case .owe: return true
        case .request(let id): return request(id) != nil
        case .salikBalance: return state.linkedAccounts.contains { $0.id == "salik" && $0.isLinked } || state.salikBalance > 0
        case .nolBalance: return state.nolBalance != nil
        case .prayer: return true
        case .parking: return true
        }
    }

    // MARK: Things

    /// The resident's own things as search items, with the words that find them.
    func searchThings() -> [SearchThing] {
        var out: [SearchThing] = []
        let s = state

        // Accounts with balances
        if s.linkedAccounts.contains(where: { $0.id == "salik" && $0.isLinked }) || s.salikBalance > 0 {
            let low = s.salikBalance <= 20
            let trips = NSDecimalNumber(decimal: s.salikBalance / 4).intValue
            out.append(SearchThing(
                id: "acct-salik", title: "Salik account",
                subtitle: "Balance \(Money.aed(s.salikBalance, trimWhole: true))\(low ? " · low, about \(trips) trips" : "")",
                tone: low ? .warning : .neutral, leading: .icon("road.lanes", .sand),
                keywords: ["salik", "toll", "tolls", "gate", "account", "balance", "recharge", "top", "up", "darb"],
                action: .sheet(.topUpSalik),
                card: .init(symbol: "road.lanes", title: "Salik", label: "Balance", value: Money.aed(s.salikBalance, trimWhole: true),
                            note: low ? "low" : nil, noteTone: .warning, button: "Top up", buttonAction: .sheet(.topUpSalik)),
                coveredBy: .salikBalance, priority: 6
            ))
        }
        if let nol = s.nolBalance {
            out.append(SearchThing(
                id: "acct-nol", title: "nol card", subtitle: "Balance \(Money.aed(nol))",
                leading: .icon("tram.fill", .sky),
                keywords: ["nol", "card", "metro", "bus", "tram", "balance", "top", "up", "recharge"],
                action: .service("top-up-nol-card"),
                card: .init(symbol: "tram.fill", title: "nol card", label: "Balance", value: Money.aed(nol),
                            button: "Top up", buttonAction: .service("top-up-nol-card")),
                coveredBy: .nolBalance, priority: 4
            ))
        }
        if let p = s.parking {
            let left = max(0, Int(p.endsAt.timeIntervalSince(.now) / 60))
            out.append(SearchThing(
                id: "acct-parking", title: "Parking in zone \(p.zone)",
                subtitle: "\(left) min left · \(p.area)", tone: left <= 15 ? .warning : .neutral,
                leading: .icon("p.square.fill", .sky),
                keywords: ["parking", "park", "parkin", "zone", SearchText.normalise(p.zone), "session", "extend"] + SearchText.tokens(p.area),
                action: .sheet(.extendParking),
                card: .init(symbol: "p.square.fill", title: "Parking", label: "Zone \(p.zone) ·", value: "\(left) min left",
                            button: "Extend", buttonAction: .sheet(.extendParking)),
                coveredBy: .parking, priority: 5
            ))
        }

        // Bills and fines
        for p in payables {
            let due = p.due.map { DemoDate.dueLabel($0).lowercased() }
            let note = p.note?.lowercased()
            var keys = SearchText.tokens("\(p.title) \(p.entity) \(p.detail)")
            keys += p.kind == .bill ? ["bill", "bills", "pay"] : ["fine", "fines", "pay", "traffic"]
            if p.entity == "DEWA" { keys += ["dewa", "electricity", "water", "utility", "utilities"] }
            out.append(SearchThing(
                id: "pay-\(p.id)", title: p.title,
                subtitle: [p.entity, Money.aed(p.amount), due ?? note].compactMap { $0 }.joined(separator: " · "),
                tone: note != nil ? .error : (due != nil ? .warning : .neutral),
                leading: .icon(p.symbol, p.hue), keywords: keys, action: .pay(p),
                card: .init(symbol: p.symbol, title: p.entity == "DEWA" ? "DEWA bill" : p.title, label: due == nil ? "To pay" : "Due",
                            value: Money.aed(p.amount), note: due ?? note, noteTone: note != nil ? .error : .warning,
                            button: "Pay", buttonAction: .pay(p)),
                coveredBy: .owe, priority: 7
            ))
        }

        // Vehicles, and their Salik tags
        for v in s.vehicles {
            let days = DemoDate.daysFromToday(v.registrationExpiry)
            let due = days <= 30
            let reg = due ? "registration due in \(days) days" : "registered to \(DemoDate.short(v.registrationExpiry))"
            let keys = SearchText.tokens("\(v.make) \(v.model) \(v.year) \(v.colour) \(v.plate.code) \(v.plate.number) \(v.insurer)")
                + ["car", "cars", "vehicle", "vehicles", "registration", "mulkiya", "insurance", "plate"]
                + (due ? ["renew", "renewal", "due"] : [])
            out.append(SearchThing(
                id: "veh-\(v.id)", title: v.name, subtitle: "\(v.plate.code) \(v.plate.number) · \(reg)",
                tone: due ? .warning : .neutral, leading: .plate(v.plate), keywords: keys, action: .push(.vehicle(v.id)),
                card: .init(symbol: "car.fill", title: v.shortName, label: "Registration", value: due ? "due in \(days) days" : "valid",
                            noteTone: .warning, button: due ? "Renew" : "Open",
                            buttonAction: due ? .service("renew-vehicle-registration") : .push(.vehicle(v.id))),
                priority: due ? 3 : 1
            ))
            if let tag = v.salikTag {
                out.append(SearchThing(
                    id: "tag-\(v.id)", title: "Salik tag on your \(v.model)", subtitle: "\(tag) · active",
                    leading: .plate(v.plate), keywords: ["salik", "tag", "tags", "toll"] + SearchText.tokens("\(v.make) \(v.model) \(v.plate.number)"),
                    action: .push(.vehicle(v.id))
                ))
            }
        }

        // Requests in progress
        for r in s.requests {
            let tone: StatusChip.Tone = switch r.status {
            case .yourTurn: .warning
            case .inReview: .info
            case .approved, .done: .success
            }
            let keys = Array(SearchIntentDetector.requestKeywords(r)) + ["request", "requests", "application", "status", "track"]
            out.append(SearchThing(
                id: "req-\(r.id)", title: r.title, subtitle: r.statusLine, tone: tone, leading: .icon(r.symbol, r.hue),
                keywords: keys, action: .push(.request(r.id)),
                card: .init(symbol: r.symbol, title: r.title, label: "", value: r.statusLine, noteTone: tone,
                            button: r.action ?? "Track", buttonAction: .push(.request(r.id))),
                coveredBy: .request(r.id), priority: tone == .warning ? 4 : 2
            ))
        }

        // Documents
        for d in s.documents {
            var keys = SearchText.tokens("\(d.title) \(d.number)")
            let t = d.title.lowercased()
            if t.contains("emirates") { keys += ["eid", "id", "hawiya", "identity", "card"] }
            if t.contains("visa") { keys += ["visa", "residency", "residence", "iqama"] }
            if t.contains("licence") { keys += ["license", "licence", "driving"] }
            if t.contains("registration") { keys += ["mulkiya", "registration", "car", "vehicle"] }
            if t.contains("tenancy") { keys += ["ejari", "tenancy", "rent", "lease"] }
            keys += ["document", "documents"]
            let subtitle = d.statusNote ?? d.expiry.map { "Valid to \(DemoDate.monthYear($0))" } ?? d.number
            out.append(SearchThing(
                id: "doc-\(d.id)", title: d.title, subtitle: subtitle,
                tone: d.status == .valid ? .neutral : .warning, leading: .icon(d.symbol, d.hue),
                keywords: keys, action: .push(.document(d.id))
            ))
        }

        // Drafts
        for d in s.drafts {
            let name = Catalogue.shared.service(d.serviceID)?.name ?? ""
            out.append(SearchThing(
                id: "draft-\(d.id)", title: d.title, subtitle: "Draft · \(d.stepsDone) of \(d.stepsTotal) steps done",
                leading: .icon(d.symbol, d.hue), keywords: SearchText.tokens("\(d.title) \(name)") + ["draft", "drafts"],
                action: .service(d.serviceID)
            ))
        }

        // Family
        for m in s.family {
            let visa = m.visaExpiry.map { " · visa to \(DemoDate.short($0))" } ?? ""
            out.append(SearchThing(
                id: "fam-\(m.id)", title: m.name, subtitle: "\(m.relation)\(visa)", leading: .avatar(m.initials, m.hue),
                keywords: SearchText.tokens("\(m.name) \(m.relation)") + ["family"], action: .push(.family)
            ))
        }

        // Journeys started
        for j in s.journeys {
            guard let journey = Catalogue.shared.service(j.journeyID) else { continue }
            let stages = journey.stages?.count ?? 0
            out.append(SearchThing(
                id: "jrn-\(j.journeyID)", title: journey.name + (j.destination.map { " \($0)" } ?? ""),
                subtitle: "Stage \(j.stageIndex + 1) of \(stages) · \(j.headline)", leading: .icon(journey.symbol, journey.hue),
                keywords: SearchText.tokens("\(journey.name) \(j.destination ?? "") \(j.headline)") + journey.aliases.flatMap(SearchText.tokens) + ["journey"],
                action: .push(.journey(j.journeyID))
            ))
        }
        return out
    }

    /// Things whose words match every query word, best first. `priority` becomes the match score.
    private func searchMatch(_ things: [SearchThing], words: [String]) -> [SearchThing] {
        guard !words.isEmpty else { return [] }
        var scored: [SearchThing] = []
        for var t in things {
            let keys = t.keywords.map { ($0, SearchText.stem($0)) }
            var total = 0.0
            var ok = true
            for (i, w) in words.enumerated() {
                let ws = SearchText.stem(w)
                let syn = SearchVocabulary.synonyms[ws] ?? []
                var best = 0.0
                for (k, ks) in keys {
                    if k == w || ks == ws { best = 1; break }
                    if w.count >= 2, k.hasPrefix(w) { best = max(best, i == words.count - 1 ? 0.9 : 0.75) }
                    else if syn.contains(ks) { best = max(best, 0.8) }
                    else if w.count >= 5, SearchText.levenshtein(k, w, limit: 1) <= 1 { best = max(best, 0.55) }
                }
                if best == 0 { ok = false; break }
                total += best
            }
            guard ok else { continue }
            t.priority = total / Double(words.count) * 100 + t.priority
            if t.priority >= 50 { scored.append(t) }
        }
        return scored.sorted { $0.priority > $1.priority }
    }

    // MARK: Widgets

    /// Widgets matching the query that the resident can add to Home.
    private func searchWidgets(for words: [String]) -> [WidgetKind] {
        guard !words.isEmpty else { return [] }
        var candidates: [(WidgetKind, Set<String>)] = [
            (.prayer, ["prayer", "prayers", "fajr", "dhuhr", "asr", "maghrib", "isha", "salah"]),
            (.weather, ["weather", "beach", "beaches", "temperature", "forecast", "flag"]),
            (.metro, ["metro", "train", "departures"]),
        ]
        if !isGuest {
            if state.parking != nil { candidates.append((.parking, ["parking"])) }
            if state.salikBalance > 0 { candidates.append((.salik, ["salik"])) }
            if state.energy != nil { candidates.append((.energy, ["energy", "consumption", "electricity", "usage"])) }
            if state.nolBalance != nil { candidates.append((.nol, ["nol"])) }
        }
        return candidates.compactMap { kind, keys in
            words.allSatisfy { w in keys.contains { ($0.hasPrefix(w) && w.count >= 4) || SearchText.stem($0) == SearchText.stem(w) } } ? kind : nil
        }
    }

    // MARK: Related services and context

    func searchRelatedServiceIDs(for intent: SearchIntent) -> [String] {
        switch intent {
        case .owe:
            var ids = payables.compactMap(\.serviceID)
            if !state.vehicles.isEmpty { ids.append("get-traffic-clearance") }
            return ids.isEmpty ? ["pay-all", "pay-traffic-fine", "pay-electricity-water-bill"] : ids
        case .request(let id):
            guard let r = request(id) else { return [] }
            var ids = [r.serviceID]
            if r.serviceID.contains("visa") { ids += ["check-visa-status", "sponsor-family-member"] }
            if r.serviceID.contains("licence") { ids += ["replace-driving-licence", "change-driving-licence-details"] }
            return ids
        case .salikBalance: return ["top-up-salik-account", "activate-salik-tag"]
        case .nolBalance: return ["top-up-nol-card", "pay-public-transport-fine"]
        case .prayer: return ["check-prayer-times", "find-mosque", "find-qibla"]
        case .parking: return ["pay-for-parking", "buy-parking-subscription", "get-parking-permit"]
        }
    }

    /// The accounts an answer was worked out from ("DEWA, RTA and Dubai Police").
    func searchAnswerSources(for intent: SearchIntent) -> String {
        switch intent {
        case .owe:
            var names = Set(payables.map(\.entity))
            if !state.vehicles.isEmpty { names.insert("RTA") }
            let ordered = state.linkedAccounts.map(\.name).filter { names.contains($0) }
            let list = ordered.isEmpty ? Array(names).sorted() : ordered
            return "Answers are worked out from your linked \(SearchFormat.list(list)) accounts. Only you can see them."
        case .request(let id):
            let entity = request(id)?.entity ?? "government"
            return "Answers are worked out from your \(entity) application. Only you can see them."
        case .salikBalance: return "Answers are worked out from your linked Salik account. Only you can see them."
        case .nolBalance: return "Answers are worked out from your linked nol card. Only you can see them."
        case .parking: return "Answers are worked out from your linked Parkin account. Only you can see them."
        case .prayer: return "Prayer times are for Dubai, from IACAD."
        }
    }

    /// Personal context beside a service row ("due tomorrow", "your Patrol, due in 12 days").
    func searchContext(for service: CatalogueService) -> (text: String, tone: StatusChip.Tone)? {
        guard !isGuest else { return nil }
        for item in state.needsYou where item.searchServiceID == service.id {
            if let p = item.payable {
                let due = p.due.map { DemoDate.dueLabel($0).lowercased() } ?? "to pay"
                return ("\(Money.aed(p.amount, trimWhole: true)) \(due)", p.due == nil ? .error : .warning)
            }
            let due = item.due.map { DemoDate.dueLabel($0).lowercased() } ?? "needs you"
            if case .renewal(_, let thingID) = item.kind, let v = state.vehicles.first(where: { $0.id == thingID }) {
                return ("your \(v.model), \(due)", .warning)
            }
            return (due, .warning)
        }
        if service.id == "top-up-salik-account", state.salikBalance > 0 {
            return ("balance \(Money.aed(state.salikBalance, trimWhole: true))", state.salikBalance <= 20 ? .warning : .neutral)
        }
        if let r = state.requests.first(where: { $0.serviceID == service.id && $0.title == service.name }) {
            switch r.status {
            case .yourTurn: return ("your turn", .warning)
            case .inReview: return ("in review", .info)
            case .approved: return ("approved", .success)
            case .done: return nil
            }
        }
        if let d = state.drafts.first(where: { $0.serviceID == service.id }) {
            return ("draft, \(d.stepsDone) of \(d.stepsTotal) done", .info)
        }
        return nil
    }
}

extension NeedsYouItem {
    /// The catalogue service this item belongs to.
    var searchServiceID: String? {
        switch kind {
        case .payment(let p): p.serviceID
        case .renewal(let id, _): id
        case .requestStep: nil
        case .journeyStep(let id): id
        }
    }
}

enum SearchFormat {
    /// "A", "A and B", "A, B and C".
    static func list(_ items: [String]) -> String {
        switch items.count {
        case 0: ""
        case 1: items[0]
        default: items.dropLast().joined(separator: ", ") + " and " + (items.last ?? "")
        }
    }

    /// "1 bill and 1 fine", "2 bills".
    static func payablesSummary(_ items: [Payable]) -> String {
        let groups: [(Payable.Kind, String, String)] = [(.bill, "bill", "bills"), (.fine, "fine", "fines"), (.fee, "fee", "fees"), (.topUp, "top-up", "top-ups")]
        let parts = groups.compactMap { kind, one, many -> String? in
            let n = items.filter { $0.kind == kind }.count
            return n == 0 ? nil : "\(n) \(n == 1 ? one : many)"
        }
        return list(parts)
    }
}
