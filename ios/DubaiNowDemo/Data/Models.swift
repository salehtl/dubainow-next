import Foundation

// MARK: - Persona

/// A demo resident you can sign in as. All their data is consistent across every screen.
struct Persona: Identifiable, Hashable {
    let id: String               // "sara"
    let firstName: String
    let lastName: String
    let initials: String
    let avatarHue: Hue
    /// One line for the persona picker: who they are and what's going on for them.
    let tagline: String
    /// "Al Barsha 1"
    let area: String
    /// "Residence visa" / "UAE national"
    let residency: String
    let emiratesID: String       // masked "784-1990-•••••••-3"
    let phone: String            // masked "+971 50 ••• 4821"
    /// Things the assistant suggests for this person (first is the showcase).
    let assistantPrompts: [String]
    /// Builds a fresh copy of their data (so "Reset demo" works).
    let makeState: @MainActor () -> PersonaState

    var fullName: String { "\(firstName) \(lastName)" }

    static func == (l: Persona, r: Persona) -> Bool { l.id == r.id }
    func hash(into h: inout Hasher) { h.combine(id) }
}

/// Everything about a resident that the demo shows or changes.
struct PersonaState {
    var needsYou: [NeedsYouItem] = []
    var favorites: [FavoriteItem] = []
    var widgets: [HomeWidget] = []
    /// One suggestion at a time ("Add Family visas to Home?"). nil when none.
    var widgetSuggestion: WidgetKind? = nil

    var vehicles: [Vehicle] = []
    var documents: [DocumentItem] = []
    var family: [FamilyMember] = []
    var home: HomeInfo? = nil
    var paymentMethods: [PaymentMethod] = [.applePay]

    var requests: [RequestItem] = []
    var drafts: [DraftItem] = []
    var history: [Receipt] = []
    var notifications: [NotificationItem] = []
    var linkedAccounts: [LinkedAccount] = []

    var salikBalance: Decimal = 0
    var nolBalance: Decimal? = nil
    var parking: ParkingSession? = nil
    var energy: EnergyReading? = nil
    var metro: MetroStation? = nil
    var weatherSpot: String = "Kite Beach"
    var journeys: [JourneyProgress] = []
    var familyVisas: FamilyVisasStatus? = nil
}

// MARK: - Payables

/// Something that can be paid: a bill, a fine, a fee or a top-up.
struct Payable: Identifiable, Hashable {
    enum Kind: String, Hashable { case bill, fine, fee, topUp }
    let id: String
    var kind: Kind
    var title: String            // "Electricity & water bill"
    var entity: String           // "DEWA"
    var detail: String           // "Al Barsha 1 · Account 2004 1187"
    var amount: Decimal
    var due: Date?
    var reference: String
    var symbol: String
    var hue: Hue
    /// Catalogue service this payment belongs to ("pay-electricity-water-bill").
    var serviceID: String?
    /// e.g. "Blocks your car renewal"
    var note: String? = nil
}

// MARK: - Needs you

/// A card in "Needs you" (Home anchor 1) and the Activity "Needs you" filter.
struct NeedsYouItem: Identifiable, Hashable {
    enum Kind: Hashable {
        /// Pay it (bill or fine). Pay all covers every payable item.
        case payment(Payable)
        /// A step in a request that's waiting for the resident.
        case requestStep(requestID: String)
        /// Something to renew (e.g. vehicle registration) — opens the service.
        case renewal(serviceID: String, thingID: String?)
        /// A journey stage waiting for the resident.
        case journeyStep(journeyID: String)
    }

    let id: String
    var kind: Kind
    var entity: String           // "DEWA", "GDRFA"
    var symbol: String
    var hue: Hue
    var title: String            // "Electricity & water bill"
    var detail: String           // "Al Barsha 1 · Account 2004 1187"
    var due: Date?
    var primaryAction: String    // "Pay now", "Upload now", "Renew", "Continue"
    var secondaryAction: String? // "Later", "Details"
    var progress: StepProgress? = nil   // request: step 3 of 5

    var payable: Payable? {
        if case .payment(let p) = kind { return p }
        return nil
    }

    var amount: Decimal? { payable?.amount }
}

struct StepProgress: Hashable {
    var current: Int   // 1-based step the resident is on
    var total: Int
}

// MARK: - Favorites and widgets

/// A favorite tile on Home (up to 8). Opens a catalogue record, a thing, or a route.
struct FavoriteItem: Identifiable, Hashable {
    enum Target: Hashable {
        case service(String)      // catalogue id
        case thing(String)        // catalogue thing id ("your-vehicles")
    }
    let id: String
    var title: String             // "Pay parking"
    var symbol: String
    var hue: Hue
    var target: Target
}

enum WidgetSize: String, Hashable, CaseIterable, Codable {
    /// 175 × 175 (half width)
    case small
    /// 362 × 175 (full width)
    case medium
    /// 362 × 362
    case large
}

/// Kinds of Home widget. Content widgets read from the persona's state.
enum WidgetKind: Hashable, Codable {
    case parking            // active parking session, Extend
    case salik              // balance, Top up
    case prayer             // next prayer + sun arc
    case weather            // weather & beach flag
    case metro              // next departures at a station
    case energy             // DEWA energy this month
    case nol                // nol card balance
    case billsAndFines      // bundle: Pay all your bills and fines
    case familyVisas        // bundle: Renew your family's visas
    case certificates       // bundle (no live state → no widget; kept for gallery explanations)
    case journey(String)    // journey widget for a catalogue journey id
    case visaTracker(String) // a request in progress (request id)

    var title: String {
        switch self {
        case .parking: "Parking"
        case .salik: "Salik"
        case .prayer: "Prayer times"
        case .weather: "Weather and beaches"
        case .metro: "Metro"
        case .energy: "Energy use"
        case .nol: "nol card"
        case .billsAndFines: "Bills and fines"
        case .familyVisas: "Family visas"
        case .certificates: "Certificates and letters"
        case .journey(let id): Catalogue.shared.service(id)?.shortName ?? "Journey"
        case .visaTracker: "Request tracker"
        }
    }
}

struct HomeWidget: Identifiable, Hashable {
    let id: String
    var kind: WidgetKind
    var size: WidgetSize
}

// MARK: - Things

struct Plate: Hashable {
    var emirate: String = "DUBAI"
    var code: String
    var number: String
}

struct Vehicle: Identifiable, Hashable {
    let id: String
    var make: String              // "Nissan"
    var model: String             // "Patrol"
    var year: Int
    var colour: String
    var plate: Plate
    var registrationExpiry: Date
    var insurer: String
    var insuranceExpiry: Date
    var lastInspection: Date?
    var salikTag: String?         // "Tag ending 3391"
    /// Payable ids of fines on this vehicle.
    var fineIDs: [String] = []

    var name: String { "\(make) \(model) \(year)" }
    var shortName: String { "\(make) \(model)" }
}

struct DocumentItem: Identifiable, Hashable {
    enum Status: Hashable { case valid, renewing, expiringSoon, expired }
    let id: String
    var title: String             // "Emirates ID"
    var number: String            // masked
    var expiry: Date?
    var status: Status = .valid
    var statusNote: String? = nil // "Renewing · your turn"
    var symbol: String
    var hue: Hue
}

struct FamilyMember: Identifiable, Hashable {
    let id: String
    var name: String
    var relation: String          // "Daughter"
    var age: Int?
    var visaExpiry: Date?
    var sponsoredByYou: Bool
    var initials: String
    var hue: Hue
}

struct HomeInfo: Hashable {
    var address: String           // "Al Barsha 1, Villa 23"
    var dewaAccount: String       // "2004 1187"
    var ejariExpiry: Date?
    var makani: String?
}

enum PaymentMethod: Hashable {
    case applePay
    case card(brand: String, last4: String)
    var label: String {
        switch self {
        case .applePay: "Apple Pay"
        case .card(let brand, let last4): "\(brand) ending \(last4)"
        }
    }
}

// MARK: - Activity

struct RequestStep: Hashable {
    enum State: Hashable { case done, current, upcoming }
    var title: String
    var detail: String
    var state: State
}

/// A request in progress with government (the request tracker).
struct RequestItem: Identifiable, Hashable {
    enum Status: Hashable {
        case yourTurn(due: Date)
        case inReview(answerBy: Date)
        case approved(note: String)
        case done
    }
    let id: String
    var serviceID: String         // catalogue id
    var title: String             // "Renew your residence visa"
    var entity: String
    var reference: String         // "GDRFA-R-26-0042817"
    var status: Status
    /// What the resident needs to know right now (top card on the tracker).
    var notice: (title: String, body: String)?
    var steps: [RequestStep]
    var startedOn: Date
    var symbol: String
    var hue: Hue
    /// Label for the action when it's your turn ("Upload now").
    var action: String? = nil

    var statusLine: String {
        switch status {
        case .yourTurn(let due): "Your turn · by \(DemoDate.long(due))"
        case .inReview(let by): "In review · answer by \(DemoDate.short(by))"
        case .approved(let note): "Approved · \(note)"
        case .done: "Done"
        }
    }

    var currentStepIndex: Int? { steps.firstIndex { $0.state == .current } }

    static func == (l: RequestItem, r: RequestItem) -> Bool { l.id == r.id && l.steps == r.steps && l.status == r.status }
    func hash(into h: inout Hasher) { h.combine(id) }
}

struct DraftItem: Identifiable, Hashable {
    let id: String
    var serviceID: String
    var title: String
    var stepsDone: Int
    var stepsTotal: Int
    var savedOn: Date
    var symbol: String
    var hue: Hue
}

/// A completed payment or submission (History, receipts).
struct Receipt: Identifiable, Hashable {
    let id: String
    var title: String             // "Topped up your Salik account"
    var shortTitle: String        // "Topped up Salik"
    var entity: String
    var amount: Decimal?
    var date: Date
    var method: PaymentMethod?
    var reference: String
    /// Extra rows on the receipt ("New balance" → "AED 118.00").
    var rows: [ReceiptRow] = []
    var symbol: String
    var hue: Hue
    var serviceID: String?
    var count: Int? = nil         // "×6" for grouped parking payments
    var rating: HappinessFace? = nil
    var doneByAssistant: Bool = false
}

struct ReceiptRow: Hashable {
    var label: String
    var value: String
}

struct NotificationItem: Identifiable, Hashable {
    let id: String
    var title: String
    var body: String
    var date: Date
    var symbol: String
    var hue: Hue
    var isRead: Bool
    var link: Route?
}

struct LinkedAccount: Identifiable, Hashable {
    let id: String
    var name: String              // "DEWA"
    var detail: String            // "Electricity and water · Account 2004 1187"
    var symbol: String
    var hue: Hue
    var isLinked: Bool
}

// MARK: - Live things for widgets

struct ParkingSession: Hashable {
    var zone: String              // "365C"
    var area: String              // "Al Barsha"
    var plate: Plate
    var startedAt: Date
    var endsAt: Date
    var ratePerHour: Decimal
}

struct EnergyReading: Hashable {
    var monthLabel: String        // "September"
    var kWh: Int
    var changeVsLastMonth: Int    // -12 (percent)
    var comparison: String        // "Lower than 68% of similar homes in Al Barsha"
    var lastSixMonths: [Int]      // kWh, oldest first
}

struct MetroStation: Hashable {
    var name: String              // "Business Bay"
    var line: String              // "Red Line"
    var departures: [MetroDeparture]
}

struct MetroDeparture: Hashable {
    var towards: String           // "Centrepoint"
    var inMinutes: Int
    var nextInMinutes: Int
}

/// A journey the resident has started (Build a house, Move home…).
struct JourneyProgress: Identifiable, Hashable {
    var id: String { journeyID }
    var journeyID: String         // catalogue id
    var stageIndex: Int           // 0-based index into the catalogue stages
    var headline: String          // "Sign the tenancy"
    var waitingOn: String         // "Waiting for your landlord to sign · sent yesterday"
    var startedOn: Date
    var destination: String?      // "to Jumeirah Village Circle"
    /// True when the current step needs the resident (then the button lives in Needs you).
    var needsYou: Bool = false
}

struct FamilyVisasStatus: Hashable {
    var memberIDs: [String]       // FamilyMember ids covered
    var dueMemberIDs: [String]    // those expiring soon
    var expiresOn: Date           // earliest expiry
}

// MARK: - Payment requests (fake payment confirmations)

/// What a payment is for; the store applies its effects when it's confirmed.
enum PaymentPurpose: Hashable {
    /// Pay these payables (one or many — Pay all).
    case payables([String])
    case salikTopUp(Decimal)
    case nolTopUp(Decimal)
    case extendParking(minutes: Int)
    /// Pay the fees for a request step (e.g. visa fees).
    case requestFees(requestID: String, amount: Decimal)
    /// Renew a vehicle registration (plus any fines that block it).
    case vehicleRenewal(vehicleID: String, fee: Decimal, fineIDs: [String])
    case donation(charity: String, amount: Decimal)
}

struct PaymentRequest: Identifiable, Hashable {
    let id: String
    var title: String             // "Pay 2 bills and fines"
    var lines: [PaymentLine]
    var purpose: PaymentPurpose
    var doneByAssistant: Bool = false

    var total: Decimal { lines.reduce(0) { $0 + $1.amount } }
}

struct PaymentLine: Hashable {
    var title: String
    var detail: String
    var amount: Decimal
    var symbol: String
    var hue: Hue
}
