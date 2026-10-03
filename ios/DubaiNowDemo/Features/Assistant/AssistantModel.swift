import SwiftUI

// The assistant's vocabulary: what the face is doing, what the card shows, what you're
// asked to confirm. Paper C1 row 11 · "One assistant, on every screen" and "Its face, and how it moves".

/// Eight states, one mark. The mark never changes shape; the ring of light says what it's doing.
enum AssistantFaceState: Equatable {
    case resting, awake, listening, thinking, working, yourTurn, done, quiet

    /// Read out by VoiceOver when the state changes.
    var spoken: String {
        switch self {
        case .resting: "Resting"
        case .awake: "Ready"
        case .listening: "Listening"
        case .thinking: "Thinking"
        case .working: "Working on it"
        case .yourTurn: "Your turn"
        case .done: "Done"
        case .quiet: "Done"
        }
    }
}

/// Where the conversation is.
enum AssistantPhase: Equatable {
    /// Woken: suggestions and the ask bar.
    case idle
    /// Hold to talk: the transcript types itself out.
    case listening
    /// Working out what you need (a card with your words and a comet on the ring).
    case thinking
    /// A card hangs from the face.
    case card
    /// Closed after a task: the mark stretches into a short note for 5 s.
    case note
}

/// Text that may come in Mandarin with an English version (the assistant answers in the
/// language you use; "Show in English" flips it).
struct AssistantText: Equatable {
    var en: String
    var zh: String?

    init(_ en: String, zh: String? = nil) {
        self.en = en
        self.zh = zh
    }

    func resolved(english: Bool) -> String { english ? en : (zh ?? en) }
}

/// One glass card hangs from the face: what you asked, its answer, and the task it's running.
struct AssistantCard: Equatable {
    var id = UUID()
    var quote: AssistantText
    var headline: AssistantText
    var isThinking = false
    var surface: AssistantSurface? = nil
    var controls: AssistantControls = .none
    var rating: AssistantRatingPrompt? = nil

    /// Mandarin cards get the "Show in English" toggle.
    var isMandarin: Bool { headline.zh != nil }
}

/// The tinted block inside the card.
enum AssistantSurface: Equatable {
    case task(AssistantTaskBlock)
    case payables([Payable])
    case request(RequestItem)
    case journey(AssistantJourneyBlock)
    case complaint(AssistantComplaintBlock)
    case list([AssistantListRow])
    case energy(EnergyReading)
}

enum AssistantTaskStatus: Equatable {
    case working, yourTurn, done, stopped
}

/// A service the assistant is running, as a checklist ("Renew registration · 3 of 5").
struct AssistantTaskBlock: Equatable {
    var symbol: String
    var hue: Hue
    var title: AssistantText
    var subtitle: AssistantText
    var steps: [AssistantStep]
    var status: AssistantTaskStatus = .working
    /// Its receipt, once done.
    var receiptID: String? = nil
    var reference: String? = nil
    var paidLine: String? = nil

    /// "3 of 5": the step it's on.
    var position: Int {
        let i = steps.firstIndex { $0.state == .active || $0.state == .yourTurn } ?? steps.count - 1
        return min(i + 1, steps.count)
    }
}

struct AssistantStep: Equatable, Identifiable {
    enum State: Equatable { case pending, active, yourTurn, done }
    var id: Int
    var title: AssistantText
    var amount: Decimal? = nil
    var state: State = .pending
}

struct AssistantJourneyBlock: Equatable {
    var symbol: String
    var hue: Hue
    var title: String
    var subtitle: String
    var stage: Int          // 0-based
    var stageCount: Int
    var now: String
    var nowDetail: String
    var next: String?
}

/// "Report a problem with your employer", filled in from UAE PASS (Wei, in Mandarin).
struct AssistantComplaintBlock: Equatable {
    var fields: [AssistantField]
    var visibleCount: Int = 0
    var originalEnglish: String
    var originalArabic: String
    var status: AssistantTaskStatus = .working
    var reference: String? = nil
}

struct AssistantField: Equatable, Identifiable {
    var id: Int
    var label: AssistantText
    var value: AssistantText
}

struct AssistantListRow: Equatable, Identifiable {
    var id: String
    var symbol: String
    var hue: Hue
    var title: String
    var detail: String
    var value: String? = nil
}

/// Buttons at the foot of the card.
enum AssistantControls: Equatable {
    case none
    /// Stop and "I'll do it myself" sit on every task card.
    case task
    /// One plain next step ("Upload now", "Open the journey").
    case action(AssistantText, AssistantFollowUp)
}

/// Where the assistant hands over after it closes.
enum AssistantFollowUp: Equatable {
    case push(Route)
    case present(SheetRoute)
    case openService(String)
    /// The tracker's fake passport camera, over the assistant; it closes once the request moves on.
    case scanPassport(requestID: String)
}

/// Inline Happiness faces after a task.
struct AssistantRatingPrompt: Equatable {
    var question: String
    var detail: String
    var receiptID: String
    var thanks: String
}

/// The one thing for you to do, under your thumb.
struct AssistantConfirm: Equatable {
    enum Style: Equatable {
        /// Face ID style payment ("Pay AED 1,020").
        case pay(Decimal)
        /// Sent to an authority in your name.
        case uaePass
    }
    var style: Style
    var title: AssistantText
    var caption: AssistantText? = nil
    var reason: String
}

/// After closing, the mark stretches into a note for 5 s. Tap = receipt.
struct AssistantNote: Equatable {
    var text: String
    var receiptID: String?
}

/// A suggestion chip.
struct AssistantSuggestion: Identifiable, Equatable {
    var id: String { text }
    var text: String
    var symbol: String
    var hue: Hue
}

// MARK: - What you asked for

/// Keyword matching for the scripted demo (no network, no AI).
enum AssistantIntent: Equatable {
    case renewRegistration
    case owe(business: Bool)
    case needsYou
    case visaStatus
    case salikTopUp(Decimal)
    case nolTopUp(Decimal)
    case journey(String)
    case vehicleFines
    case energy
    case metro
    case fees
    case remind
    case scanPassport
    case employerComplaint(mandarin: Bool)
    case continueInEnglish
    case unknown(mandarin: Bool)

    static func match(_ raw: String) -> AssistantIntent {
        let t = raw.lowercased()
        if AssistantIntent.isMandarin(raw) {
            let workWords = ["工资", "薪", "投诉", "雇主", "老板", "公司"]
            if workWords.contains(where: { raw.contains($0) }) { return .employerComplaint(mandarin: true) }
            if t.contains("nol") || raw.contains("交通卡") { return .nolTopUp(amount(in: t) ?? 50) }
            return .unknown(mandarin: true)
        }
        func has(_ words: String...) -> Bool { words.contains { t.contains($0) } }
        if has("english") { return .continueInEnglish }
        if has("salik") { return .salikTopUp(amount(in: t) ?? 100) }
        if has("nol card", "nol ") || t.hasSuffix("nol") { return .nolTopUp(amount(in: t) ?? 50) }
        if has("employer", "wages", "salary", "not been paid", "haven't been paid") { return .employerComplaint(mandarin: false) }
        if has("registration", "mulkiya") || (has("renew") && has("car", "vehicle", "patrol", "corolla")) { return .renewRegistration }
        if has("metro", "train") { return .metro }
        if has("power", "energy", "kwh", "electricity use") { return .energy }
        if has("fine"), !has("pay", "owe"), has("which", "van", "car", "vehicle", "truck") { return .vehicleFines }
        if has("remind") { return .remind }
        if has("fee") { return .fees }
        if has("scan") { return .scanPassport }
        if has("visa") { return .visaStatus }
        if has("owe", "pay all", "pay everything", "bills", "fines", "pay my") {
            return .owe(business: has("café", "cafe", "business", "company", "shop", "van"))
        }
        if has("what do i need", "needs me", "need to do", "anything due", "what's due") { return .needsYou }
        if has("house", "home", "journey", "what's next", "next for", "move", "build", "villa", "drawing") { return .journey(t) }
        return .unknown(mandarin: false)
    }

    static func isMandarin(_ s: String) -> Bool {
        s.unicodeScalars.contains { (0x4E00...0x9FFF).contains($0.value) }
    }

    /// "AED 100" → 100
    static func amount(in s: String) -> Decimal? {
        guard let r = s.range(of: #"\d[\d,]*(\.\d+)?"#, options: .regularExpression) else { return nil }
        return Decimal(string: s[r].replacingOccurrences(of: ",", with: ""))
    }

    /// Done-once key (so a finished task isn't suggested again).
    var key: String {
        switch self {
        case .renewRegistration: "renew"
        case .owe: "owe"
        case .needsYou: "needsYou"
        case .visaStatus: "visa"
        case .salikTopUp: "salik"
        case .nolTopUp: "nol"
        case .journey: "journey"
        case .vehicleFines: "vehicleFines"
        case .energy: "energy"
        case .metro: "metro"
        case .fees: "fees"
        case .remind: "remind"
        case .scanPassport: "scan"
        case .employerComplaint: "complaint"
        case .continueInEnglish: "english"
        case .unknown: "unknown"
        }
    }

    /// Suggestions after a task prefer the same part of life ("one after a task").
    var topic: String {
        switch self {
        case .renewRegistration, .salikTopUp, .nolTopUp, .vehicleFines, .metro: "driving"
        case .energy: "home"
        case .owe, .needsYou: "money"
        case .visaStatus, .fees, .scanPassport, .remind: "visa"
        case .journey: "home"
        case .employerComplaint: "work"
        case .continueInEnglish, .unknown: "other"
        }
    }

    var chipSymbol: String {
        switch self {
        case .renewRegistration: "car.fill"
        case .owe: "creditcard.fill"
        case .needsYou: "checklist"
        case .visaStatus: "person.text.rectangle"
        case .salikTopUp: "tag.fill"
        case .nolTopUp: "tram.fill"
        case .journey: "house.fill"
        case .vehicleFines: "exclamationmark.triangle.fill"
        case .energy: "bolt.fill"
        case .metro: "tram.fill"
        case .fees: "banknote"
        case .remind: "bell"
        case .scanPassport: "camera"
        case .employerComplaint: "briefcase.fill"
        case .continueInEnglish: "character.bubble"
        case .unknown: "sparkles"
        }
    }

    var chipHue: Hue {
        switch self {
        case .visaStatus, .scanPassport, .fees, .remind: .lilac
        case .journey, .employerComplaint, .energy: .sand
        default: .sky
        }
    }
}

// MARK: - Formatting

enum AssistantFormat {
    private static let dayMonthYear: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_GB"); f.dateFormat = "d MMM yyyy"; return f
    }()
    private static let monthEN: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_GB"); f.dateFormat = "MMMM"; return f
    }()
    private static let monthAR: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "ar_AE@numbers=latn"); f.dateFormat = "MMMM"; return f
    }()

    /// "13 Oct 2027"
    static func dayMonthYear(_ d: Date) -> String { dayMonthYear.string(from: d) }

    /// The two months before this one (unpaid wages): (date, date).
    static func lastTwoMonths(from now: Date = .now) -> (Date, Date) {
        let cal = Calendar.current
        let a = cal.date(byAdding: .month, value: -2, to: now) ?? now
        let b = cal.date(byAdding: .month, value: -1, to: now) ?? now
        return (a, b)
    }

    static func monthNumber(_ d: Date) -> Int { Calendar.current.component(.month, from: d) }
    static func year(_ d: Date) -> Int { Calendar.current.component(.year, from: d) }
    static func monthEnglish(_ d: Date) -> String { monthEN.string(from: d) }
    static func monthArabic(_ d: Date) -> String { monthAR.string(from: d) }

    /// "Upload a clearer passport copy" → "upload a clearer passport copy"
    static func lowerFirst(_ s: String) -> String {
        guard let f = s.first else { return s }
        return f.lowercased() + s.dropFirst()
    }
}
