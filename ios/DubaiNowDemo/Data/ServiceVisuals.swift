import Foundation

/// How a catalogue record looks: its hue and SF Symbol.
///
/// Rule (C1 · Decisions): a service's hue belongs to its record and defaults to its topic.
/// The catalogue has no icon field, so each service uses its sub-topic's symbol unless it has
/// an override below (the ones drawn on the Paper screens).
enum ServiceVisuals {
    /// Topic hues, read from Paper "Services · Browse" topic tiles.
    static let topicHue: [String: Hue] = [
        "driving-and-transport": .sky,
        "visas-and-residency": .lilac,
        "housing-and-utilities": .sand,
        "health": .mint,
        "family-and-social-support": .peach,
        "births-marriages-and-deaths": .rose,
        "work-and-business": .sand,
        "education-and-learning": .sky,
        "faith-and-giving": .lilac,
        "city-and-environment": .mint,
        "police-justice-and-the-law": .rose,
        "leisure-culture-and-events": .peach,
    ]

    static let topicSymbol: [String: String] = [
        "driving-and-transport": "car.fill",
        "visas-and-residency": "person.text.rectangle.fill",
        "housing-and-utilities": "house.fill",
        "health": "heart.fill",
        "family-and-social-support": "person.2.fill",
        "births-marriages-and-deaths": "figure.2.and.child.holdinghands",
        "work-and-business": "briefcase.fill",
        "education-and-learning": "graduationcap.fill",
        "faith-and-giving": "moon.stars.fill",
        "city-and-environment": "leaf.fill",
        "police-justice-and-the-law": "building.columns.fill",
        "leisure-culture-and-events": "ticket.fill",
    ]

    /// "topic/subtopic" → SF Symbol.
    static let subtopicSymbol: [String: String] = [
        "driving-and-transport/vehicles": "car.fill",
        "driving-and-transport/licences": "person.text.rectangle.fill",
        "driving-and-transport/fines": "exclamationmark.triangle.fill",
        "driving-and-transport/parking": "p.square.fill",
        "driving-and-transport/plates": "number.square.fill",
        "driving-and-transport/roads": "road.lanes",
        "driving-and-transport/public-transport": "tram.fill",
        "driving-and-transport/air": "airplane",
        "visas-and-residency/your-visa": "person.text.rectangle.fill",
        "visas-and-residency/family": "person.2.fill",
        "visas-and-residency/ids": "person.crop.rectangle",
        "housing-and-utilities/renting": "key.fill",
        "housing-and-utilities/utilities": "bolt.fill",
        "housing-and-utilities/phone-internet": "wifi",
        "housing-and-utilities/owning": "house.fill",
        "housing-and-utilities/building": "hammer.fill",
        "housing-and-utilities/support": "building.2.fill",
        "health/care": "stethoscope",
        "health/records": "doc.text.fill",
        "health/insurance": "cross.case.fill",
        "health/emergencies": "staroflife.fill",
        "family-and-social-support/financial": "banknote.fill",
        "family-and-social-support/pod": "figure.roll",
        "family-and-social-support/older": "figure.walk",
        "family-and-social-support/family-life": "figure.2.and.child.holdinghands",
        "births-marriages-and-deaths/births": "stroller.fill",
        "births-marriages-and-deaths/marriage": "heart.circle.fill",
        "births-marriages-and-deaths/death": "leaf.fill",
        "work-and-business/your-job": "briefcase.fill",
        "work-and-business/business": "storefront.fill",
        "work-and-business/health-pros": "stethoscope",
        "work-and-business/money": "banknote.fill",
        "education-and-learning/schools": "backpack.fill",
        "education-and-learning/higher": "graduationcap.fill",
        "faith-and-giving/prayer": "moon.stars.fill",
        "faith-and-giving/giving": "heart.fill",
        "faith-and-giving/endowments": "building.columns.fill",
        "faith-and-giving/guidance": "book.fill",
        "city-and-environment/report": "exclamationmark.bubble.fill",
        "city-and-environment/waste": "trash.fill",
        "city-and-environment/parks": "tree.fill",
        "city-and-environment/animals": "pawprint.fill",
        "city-and-environment/streets": "map.fill",
        "city-and-environment/payments": "creditcard.fill",
        "city-and-environment/post": "envelope.fill",
        "police-justice-and-the-law/emergencies": "phone.fill",
        "police-justice-and-the-law/report": "exclamationmark.bubble.fill",
        "police-justice-and-the-law/police": "shield.fill",
        "police-justice-and-the-law/courts": "building.columns.fill",
        "leisure-culture-and-events/events": "ticket.fill",
        "leisure-culture-and-events/culture": "theatermasks.fill",
        "leisure-culture-and-events/sport": "sportscourt.fill",
        "leisure-culture-and-events/community": "person.3.fill",
        "leisure-culture-and-events/offers": "tag.fill",
    ]

    /// Record-level overrides (drawn on the Paper screens).
    static let serviceOverrides: [String: (symbol: String, hue: Hue?)] = [
        "pay-for-parking": ("p.square.fill", .sky),
        "top-up-salik-account": ("road.lanes", .sand),
        "activate-salik-tag": ("road.lanes", .sand),
        "donate-to-charity": ("heart.fill", .lilac),
        "your-vehicles": ("car.fill", .rose),
        "renew-vehicle-registration": ("car.fill", .sky),
        "pay-traffic-fine": ("exclamationmark.triangle.fill", .sky),
        "pay-electricity-water-bill": ("bolt.fill", .sand),
        "renew-residence-visa": ("person.text.rectangle.fill", .lilac),
        "check-visa-status": ("person.text.rectangle.fill", .lilac),
        "top-up-nol-card": ("tram.fill", .sky),
        "pay-du-bill": ("phone.fill", .sand),
        "pay-etisalat-bill": ("phone.fill", .sand),
        "check-prayer-times": ("moon.stars.fill", .lilac),
        "report-employer-problem": ("exclamationmark.bubble.fill", .sand),
        "get-good-conduct-certificate": ("checkmark.shield.fill", .rose),
        "register-tenancy-contract": ("key.fill", .sand),
        // Bundles and journeys
        "pay-all": ("creditcard.fill", .sky),
        "get-certificates": ("doc.on.doc.fill", .lilac),
        "renew-family-visas": ("person.3.fill", .lilac),
        "apply-all-pod-benefits": ("figure.roll", .peach),
        "build-a-house": ("house.lodge.fill", .sand),
        "move-home": ("shippingbox.fill", .sand),
        "get-married": ("heart.circle.fill", .rose),
        "after-a-death": ("leaf.fill", .rose),
        "learn-to-drive": ("steeringwheel", .sky),
    ]

    static func hue(for service: CatalogueService) -> Hue {
        if let h = serviceOverrides[service.id]?.hue { return h }
        return hue(forTopic: service.topic)
    }

    static func hue(forTopic topic: String?) -> Hue {
        guard let topic else { return .sky }
        return topicHue[topic] ?? .sky
    }

    static func symbol(for service: CatalogueService) -> String {
        if let s = serviceOverrides[service.id]?.symbol { return s }
        if let t = service.topic, let sub = service.subtopic, let s = subtopicSymbol["\(t)/\(sub)"] { return s }
        if let t = service.topic, let s = topicSymbol[t] { return s }
        return "square.grid.2x2.fill"
    }

    static func symbol(forTopic topic: String) -> String { topicSymbol[topic] ?? "square.grid.2x2.fill" }

    static func symbol(topic: String, subtopic: String) -> String {
        subtopicSymbol["\(topic)/\(subtopic)"] ?? symbol(forTopic: topic)
    }
}

extension CatalogueService {
    var hue: Hue { ServiceVisuals.hue(for: self) }
    var symbol: String { ServiceVisuals.symbol(for: self) }
}

extension CatalogueTopic {
    var hue: Hue { ServiceVisuals.hue(forTopic: id) }
    var symbol: String { ServiceVisuals.symbol(forTopic: id) }
}
