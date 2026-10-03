import Foundation

/// The refactored DubaiNow service catalogue (catalogue/dubainow-services.json, 2 Oct 2026):
/// 219 services in 12 topics — 210 standalone, 4 bundles, 5 journeys — plus things, guides
/// and options. Loaded once from the app bundle.
struct Catalogue: Decodable {
    let name: String
    let version: String
    let date: String
    let topics: [CatalogueTopic]
    let services: [CatalogueService]
    let things: [CatalogueService]
    let guides: [CatalogueService]

    // MARK: Loading

    static let shared: Catalogue = {
        guard let url = Bundle.main.url(forResource: "dubainow-services", withExtension: "json"),
              let data = try? Data(contentsOf: url) else {
            fatalError("dubainow-services.json is missing from the app bundle")
        }
        do {
            return try JSONDecoder().decode(Catalogue.self, from: data)
        } catch {
            fatalError("Catalogue failed to decode: \(error)")
        }
    }()

    // MARK: Lookup

    private static let byID: [String: CatalogueService] = {
        var map: [String: CatalogueService] = [:]
        for s in shared.services + shared.things + shared.guides { map[s.id] = s }
        return map
    }()

    func service(_ id: String) -> CatalogueService? { Catalogue.byID[id] }
    func topic(_ id: String) -> CatalogueTopic? { topics.first { $0.id == id } }

    /// Standalone services in a topic (and optionally a sub-topic), most used first.
    func standalone(inTopic topicID: String, subtopic: String? = nil) -> [CatalogueService] {
        services
            .filter { $0.type == .standalone && $0.topic == topicID && (subtopic == nil || $0.subtopic == subtopic) }
            .sorted { $0.uniques30d > $1.uniques30d }
    }

    /// Every service in a topic including bundles and journeys that belong to it.
    func all(inTopic topicID: String) -> [CatalogueService] {
        services.filter { $0.topic == topicID }.sorted { $0.uniques30d > $1.uniques30d }
    }

    var bundles: [CatalogueService] { services.filter { $0.type == .bundle }.sorted { $0.uniques30d > $1.uniques30d } }
    var journeys: [CatalogueService] { services.filter { $0.type == .journey } }

    /// All 219 services A to Z (standalone, bundles and journeys).
    var aToZ: [CatalogueService] {
        services.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    /// Owners shown as entities ("By government entity"), with their service counts.
    var owners: [(name: String, count: Int)] {
        var counts: [String: Int] = [:]
        for s in services where s.owner != "DubaiNow" {
            counts[s.owner ?? "To confirm", default: 0] += 1
        }
        return counts.map { ($0.key, $0.value) }.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    func services(ownedBy owner: String) -> [CatalogueService] {
        services.filter { ($0.owner ?? "To confirm") == owner }.sorted { $0.name < $1.name }
    }

    /// Simple search over names, short names, aliases, descriptions and owners.
    /// Returns ranked results (prefix and alias matches first). Feature code can do better.
    func search(_ query: String) -> [CatalogueService] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return [] }
        func score(_ s: CatalogueService) -> Int {
            let name = s.name.lowercased()
            if name.hasPrefix(q) { return 100 }
            if name.split(separator: " ").contains(where: { $0.hasPrefix(q) }) { return 80 }
            if s.aliases.contains(where: { $0.lowercased().hasPrefix(q) }) { return 70 }
            if name.contains(q) { return 60 }
            if s.aliases.contains(where: { $0.lowercased().contains(q) }) { return 50 }
            if (s.owner ?? "").lowercased().hasPrefix(q) { return 40 }
            if s.description.lowercased().contains(q) { return 20 }
            return 0
        }
        return (services + things)
            .map { ($0, score($0)) }
            .filter { $0.1 > 0 }
            .sorted { $0.1 != $1.1 ? $0.1 > $1.1 : $0.0.uniques30d > $1.0.uniques30d }
            .map(\.0)
    }
}

struct CatalogueTopic: Decodable, Identifiable, Hashable {
    let id: String
    let name: String
    let nameAr: String
    /// Count of services in this topic (from the catalogue build).
    let services: Int
    let things: [String]
    let journeys: [String]
    let bundles: [String]
    let owners: [String]
    let subtopics: [CatalogueSubtopic]
}

struct CatalogueSubtopic: Decodable, Identifiable, Hashable {
    let id: String
    let name: String
    let services: Int
}

/// A service record (also used for things and guides, which share the shape).
struct CatalogueService: Decodable, Identifiable, Hashable {
    enum ServiceType: String, Decodable, Hashable { case standalone, bundle, journey }

    let id: String
    let kind: String            // service | thing | info
    let type: ServiceType
    let name: String
    let nameAr: String
    let shortName: String
    let description: String
    let topic: String?
    let subtopic: String?
    let owner: String?
    let ownerConfirmed: Bool
    let audience: String
    let aliases: [String]
    let status: String          // live | beta | to publish | to confirm | proposed | maintenance
    let uniques30d: Int

    // Bundles and journeys
    let parts: [String]?
    let entities: [String]?
    let stages: [Stage]?
    let lasts: String?
    let scaleLine: String?
    let countLine: String?
    let covers: String?
    let doesTheWork: String?
    let views: [String]?
    let options: [Option]?

    struct Stage: Decodable, Hashable {
        let name: String
        let takes: String?
        let parts: [String]
        let notInDubaiNowYet: [Missing]
    }

    struct Missing: Decodable, Hashable {
        let name: String
        let owner: String?
    }

    struct Option: Decodable, Hashable, Identifiable {
        let id: String
        let name: String
        let description: String?
        let owner: String?
    }

    enum CodingKeys: String, CodingKey {
        case id, kind, type, name, nameAr, shortName, description, topic, subtopic, owner, ownerConfirmed,
             audience, aliases, status, uniques30d, parts, entities, stages, lasts, scaleLine, countLine,
             covers, doesTheWork, views, options
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(String.self, forKey: .id)
        kind = try c.decodeIfPresent(String.self, forKey: .kind) ?? "service"
        type = try c.decodeIfPresent(ServiceType.self, forKey: .type) ?? .standalone
        name = try c.decode(String.self, forKey: .name)
        nameAr = try c.decodeIfPresent(String.self, forKey: .nameAr) ?? ""
        shortName = try c.decodeIfPresent(String.self, forKey: .shortName) ?? name
        description = try c.decodeIfPresent(String.self, forKey: .description) ?? ""
        topic = try c.decodeIfPresent(String.self, forKey: .topic)
        subtopic = try c.decodeIfPresent(String.self, forKey: .subtopic)
        owner = try c.decodeIfPresent(String.self, forKey: .owner)
        ownerConfirmed = try c.decodeIfPresent(Bool.self, forKey: .ownerConfirmed) ?? true
        audience = try c.decodeIfPresent(String.self, forKey: .audience) ?? "all"
        aliases = try c.decodeIfPresent([String].self, forKey: .aliases) ?? []
        status = try c.decodeIfPresent(String.self, forKey: .status) ?? "live"
        uniques30d = try c.decodeIfPresent(Int.self, forKey: .uniques30d) ?? 0
        parts = try c.decodeIfPresent([String].self, forKey: .parts)
        entities = try c.decodeIfPresent([String].self, forKey: .entities)
        stages = try c.decodeIfPresent([Stage].self, forKey: .stages)
        lasts = try c.decodeIfPresent(String.self, forKey: .lasts)
        scaleLine = try c.decodeIfPresent(String.self, forKey: .scaleLine)
        countLine = try c.decodeIfPresent(String.self, forKey: .countLine)
        covers = try c.decodeIfPresent(String.self, forKey: .covers)
        doesTheWork = try c.decodeIfPresent(String.self, forKey: .doesTheWork)
        views = try c.decodeIfPresent([String].self, forKey: .views)
        options = try c.decodeIfPresent([Option].self, forKey: .options)
    }

    // MARK: Display helpers

    /// The owner as shown beside the name ("RTA"). Bundles and journeys show their entities.
    var ownerLabel: String {
        switch type {
        case .standalone: return owner ?? "Owner to confirm"
        case .bundle, .journey:
            let e = entities ?? []
            return e.count > 2 ? "\(e.prefix(2).joined(separator: ", ")) and \(e.count - 2) more" : e.joined(separator: " and ")
        }
    }

    var isLive: Bool { status == "live" || status == "beta" }

    /// Plain-English status for records that aren't simply live.
    var statusLabel: String? {
        switch status {
        case "beta": "Beta"
        case "maintenance": "Under maintenance"
        case "to publish", "proposed", "to confirm": "Coming soon"
        default: nil
        }
    }

    var audienceLabel: String? {
        switch audience {
        case "all": nil
        case "dubai-citizens": "For Dubai citizens"
        case "nationals": "For UAE nationals"
        case "property-owners": "For property owners"
        case "vehicle-owners": "For vehicle owners"
        case "parents": "For parents"
        case "drivers": "For drivers"
        case "pod": "For people of determination"
        case "residents": "For residents"
        case "tenants": "For tenants"
        case "seniors": "For senior citizens"
        case "healthcare-pros": "For healthcare professionals"
        case "pensioners": "For pensioners"
        case "businesses": "For businesses"
        case "pet-owners": "For pet owners"
        case "learners": "For learner drivers"
        case "visitors": "For visitors"
        case "guardians": "For guardians"
        case "workers": "For workers"
        case "students": "For students"
        case "donors": "For donors"
        default: nil
        }
    }

    /// Resolved parts (some referenced parts aren't records yet and are skipped).
    var resolvedParts: [CatalogueService] {
        (parts ?? []).compactMap { Catalogue.shared.service($0) }
    }

    /// "{n} bills and fines, one payment" with n filled in.
    func countLine(n: Int) -> String? {
        countLine?.replacingOccurrences(of: "{n}", with: "\(n)")
    }

    static func == (l: CatalogueService, r: CatalogueService) -> Bool { l.id == r.id }
    func hash(into h: inout Hasher) { h.combine(id) }
}
