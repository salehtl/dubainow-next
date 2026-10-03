import SwiftUI

// Shared helpers for the Activity feature (Activity tab, request tracker, receipts, payments).
// Anything here that belongs in Data/ is listed in NOTES.md for the orchestrator to promote.

// MARK: - Store helpers (read existing state; small writes only to existing stored state)

extension DemoStore {
    /// Requests with government that aren't waiting on a Needs you card (so Needs you and
    /// In progress never show the same request twice).
    var activityInProgress: [RequestItem] {
        let waiting = Set(state.needsYou.compactMap { item -> String? in
            if case .requestStep(let id) = item.kind { return id }
            return nil
        })
        return state.requests.filter { r in
            if case .done = r.status { return false }
            return !waiting.contains(r.id)
        }
    }

    /// Puts a deleted draft back where it was (Undo on the "Draft deleted" toast).
    func activityRestoreDraft(_ draft: DraftItem, at index: Int) {
        guard !state.drafts.contains(where: { $0.id == draft.id }) else { return }
        withAnimation(DNMotion.resolve(DNMotion.smooth)) {
            state.drafts.insert(draft, at: min(max(index, 0), state.drafts.count))
        }
    }

    /// Adds rows to a receipt the store created without any (e.g. request fees list their parts).
    func activityAttachRows(_ rows: [ReceiptRow], toReceipt id: String) {
        guard let i = state.history.firstIndex(where: { $0.id == id }), state.history[i].rows.isEmpty else { return }
        state.history[i].rows = rows
    }
}

// MARK: - Request steps and status

enum ActivityStepKind { case upload, pay, waiting }

extension RequestStep {
    /// What the resident can do on this step when it's current.
    var activityKind: ActivityStepKind {
        let t = title.lowercased()
        if t.contains("upload") || t.contains("scan") { return .upload }
        if t.hasPrefix("pay ") { return .pay }
        return .waiting
    }
}

extension RequestItem {
    var activityCurrentStep: RequestStep? { currentStepIndex.map { steps[$0] } }

    /// True when the next move is the resident's (upload or pay).
    var activityNeedsResident: Bool {
        if case .yourTurn = status { return true }
        return activityCurrentStep.map { $0.activityKind != .waiting } ?? false
    }

    /// Status line and tone for rows ("Approved · card arrives Thursday") and the tracker chip
    /// ("Your turn · 3 days left").
    func activityStatus(chip: Bool = false) -> (text: String, tone: StatusChip.Tone) {
        if let step = activityCurrentStep, step.activityKind == .pay, !isYourTurn {
            return ("Your turn · pay the fees", .warning)
        }
        switch status {
        case .yourTurn(let due):
            return (chip ? "Your turn · \(ActivityFormat.daysLeft(due))" : "Your turn · by \(DemoDate.long(due))", .warning)
        case .inReview(let by):
            return ("In review · answer by \(DemoDate.short(by))", .info)
        case .approved(let note):
            return ("Approved · \(note)", .success)
        case .done:
            return ("Done", .success)
        }
    }

    private var isYourTurn: Bool {
        if case .yourTurn = status { return true }
        return false
    }

    /// The outcome row at the end of the tracker (the star).
    var activityOutcome: (title: String, detail: String) {
        switch serviceID {
        case "renew-residence-visa": ("New visa and Emirates ID", "Added to your Documents straight away")
        case "renew-driving-licence": ("New driving licence", "Your digital licence updates in Documents")
        case "top-up-salik-account": ("Decision from Salik", "We'll tell you as soon as it's made")
        default: ("All done", "We'll keep the record in your History")
        }
    }

    /// The exact fees when the "Pay the fees" step comes up.
    var activityFeeLines: [PaymentLine] {
        switch serviceID {
        case "renew-residence-visa":
            return [
                PaymentLine(title: "Residence visa, 2 years", detail: "GDRFA", amount: 650, symbol: "person.text.rectangle.fill", hue: .lilac),
                PaymentLine(title: "Emirates ID, 2 years", detail: "ICP", amount: 370, symbol: "creditcard.fill", hue: .mint),
                PaymentLine(title: "Service and knowledge fees", detail: "GDRFA", amount: 126.50, symbol: "doc.text.fill", hue: .sky),
            ]
        default:
            let amount = ActivityFormat.firstAmount(in: activityCurrentStep?.detail ?? "") ?? 100
            return [PaymentLine(title: "Fees for your request", detail: entity, amount: amount, symbol: symbol, hue: hue)]
        }
    }

    /// The payment request for the "Pay the fees" step.
    var activityFeesRequest: PaymentRequest {
        let lines = activityFeeLines
        let total = lines.reduce(Decimal(0)) { $0 + $1.amount }
        return PaymentRequest(
            id: "fees-\(id)",
            title: "Pay the fees",
            lines: lines,
            purpose: .requestFees(requestID: id, amount: total)
        )
    }
}

// MARK: - Formatting

enum ActivityFormat {
    private static let dayMonthYearTime: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_GB"); f.dateFormat = "d MMM yyyy, HH:mm"; return f
    }()
    private static let dayMonthTime: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_GB"); f.dateFormat = "d MMM, HH:mm"; return f
    }()
    private static let monthName: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_GB"); f.dateFormat = "MMMM"; return f
    }()
    private static let monthKey: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_GB"); f.dateFormat = "yyyy-MM"; return f
    }()
    private static let isoish: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_GB"); f.dateFormat = "yyyy-MM-dd HH:mm"; return f
    }()

    /// "1 Oct 2026, 08:14"
    static func whenLong(_ d: Date) -> String { dayMonthYearTime.string(from: d) }
    /// "1 Oct, 08:14"
    static func whenShort(_ d: Date) -> String { dayMonthTime.string(from: d) }
    /// "2026-10-01 08:14" (CSV)
    static func csvDate(_ d: Date) -> String { isoish.string(from: d) }

    /// "3 days left", "1 day left", "due today", "overdue"
    static func daysLeft(_ due: Date) -> String {
        let n = DemoDate.daysFromToday(due)
        switch n {
        case ..<0: return "overdue"
        case 0: return "due today"
        case 1: return "1 day left"
        default: return "\(n) days left"
        }
    }

    /// Compact chip on a Needs you card: "Today", "Tomorrow", "3 days", "15 Oct".
    static func dueChip(_ due: Date) -> String {
        let n = DemoDate.daysFromToday(due)
        switch n {
        case ..<0: return "Overdue"
        case 0: return "Today"
        case 1: return "Tomorrow"
        case 2...13: return "\(n) days"
        default: return DemoDate.short(due)
        }
    }

    /// Receipts grouped by month, newest first ("October", "September", "August 2025").
    static func months(_ receipts: [Receipt]) -> [(id: String, title: String, receipts: [Receipt])] {
        let sorted = receipts.sorted { $0.date > $1.date }
        var groups: [(id: String, title: String, receipts: [Receipt])] = []
        let thisYear = Calendar.current.component(.year, from: .now)
        for r in sorted {
            let key = monthKey.string(from: r.date)
            if let i = groups.firstIndex(where: { $0.id == key }) {
                groups[i].receipts.append(r)
            } else {
                let year = Calendar.current.component(.year, from: r.date)
                let title = year == thisYear ? monthName.string(from: r.date) : DemoDate.monthYear(r.date)
                groups.append((key, title, [r]))
            }
        }
        return groups
    }

    /// The first "AED 1,150" style amount in a string.
    static func firstAmount(in text: String) -> Decimal? {
        guard let range = text.range(of: #"AED\s?[0-9][0-9,]*(\.[0-9]+)?"#, options: .regularExpression) else { return nil }
        let figure = text[range].replacingOccurrences(of: "AED", with: "").replacingOccurrences(of: ",", with: "").trimmingCharacters(in: .whitespaces)
        return Decimal(string: figure)
    }

    /// "DEWA and Dubai Police", "DEWA, RTA and Salik"
    static func list(_ items: [String]) -> String {
        var unique: [String] = []
        for i in items where !unique.contains(i) { unique.append(i) }
        switch unique.count {
        case 0: return ""
        case 1: return unique[0]
        default: return unique.dropLast().joined(separator: ", ") + " and " + (unique.last ?? "")
        }
    }
}

// MARK: - Receipt copy

extension Receipt {
    /// "How was topping up Salik?"
    var activityRatingQuestion: String {
        switch serviceID {
        case "top-up-salik-account": "How was topping up Salik?"
        case "top-up-nol-card": "How was topping up your nol card?"
        case "pay-for-parking": "How was paying for parking?"
        case "pay-electricity-water-bill": "How was paying your DEWA bill?"
        case "pay-traffic-fine": "How was paying your fine?"
        case "pay-all": "How was paying your bills and fines?"
        case "renew-vehicle-registration": "How was renewing your registration?"
        case "renew-driving-licence": "How was renewing your licence?"
        case "renew-residence-visa": "How was paying your visa fees?"
        case "donate-to-charity": "How was donating?"
        default: amount == nil ? "How was it?" : "How was paying?"
        }
    }

    /// Who the rating goes to ("It goes to Salik and the Happiness Meter.").
    var activityRatingRecipient: String { entity == "DubaiNow" ? "DubaiNow" : entity }

    var activitySymbolForMethod: String {
        switch method {
        case .applePay?: "wallet.pass.fill"
        case .card?: "creditcard.fill"
        case nil: "checkmark.circle"
        }
    }
}

extension PaymentMethod {
    var activitySymbol: String {
        switch self {
        case .applePay: "wallet.pass.fill"
        case .card: "creditcard.fill"
        }
    }
}

// MARK: - Filters

enum ActivityFilter: String, CaseIterable, Identifiable, Hashable {
    case all, needsYou, inProgress, drafts, history
    var id: String { rawValue }

    var title: String {
        switch self {
        case .all: "All"
        case .needsYou: "Needs you"
        case .inProgress: "In progress"
        case .drafts: "Drafts"
        case .history: "History"
        }
    }

    /// `-activityFilter drafts` (screenshots and demos).
    static var launchValue: ActivityFilter? {
        UserDefaults.standard.string(forKey: "activityFilter").flatMap(ActivityFilter.init(rawValue:))
    }
}

// MARK: - Draft pins (demo-only; see NOTES.md → DraftItem.isPinned)

@Observable
final class ActivityDraftPins {
    static let shared = ActivityDraftPins()
    private(set) var pinned: Set<String> = []

    func toggle(_ id: String) {
        withAnimation(DNMotion.resolve(DNMotion.smooth)) {
            if pinned.contains(id) { pinned.remove(id) } else { pinned.insert(id) }
        }
    }

    func isPinned(_ id: String) -> Bool { pinned.contains(id) }

    func sorted(_ drafts: [DraftItem]) -> [DraftItem] {
        drafts.filter { pinned.contains($0.id) } + drafts.filter { !pinned.contains($0.id) }
    }
}

extension DraftItem {
    /// The next thing to do when they continue (demo copy; falls back to the step count).
    var activityNextStep: String {
        switch serviceID {
        case "register-tenancy-contract": "Next: add your landlord's details"
        case "get-good-conduct-certificate": "Next: choose who the certificate is for"
        default: "Next: step \(min(stepsDone + 1, stepsTotal)) of \(stepsTotal)"
        }
    }

    /// "Dubai Police" (the record's owner, when the catalogue knows it).
    var activityOwner: String? {
        Catalogue.shared.service(serviceID).map(\.ownerLabel).flatMap { $0.isEmpty ? nil : $0 }
    }
}

// MARK: - Demo launch argument

/// `-activityDemo scan|shoot|send|uploaded|fees|pay` drives the flows for screenshots and rehearsals:
/// open the passport scan, take the photo, send it, start the tracker after the upload, or pay
/// automatically when the payment sheet opens.
enum ActivityDemoArg {
    static var value: String? { UserDefaults.standard.string(forKey: "activityDemo") }
}

// MARK: - Identifiable wrappers for local sheets

struct ActivityCaptureTarget: Identifiable, Hashable {
    let id: String   // request id
}
