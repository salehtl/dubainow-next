import SwiftUI

/// Who is using the app.
enum Session: Equatable {
    case signedIn(Persona)
    case guest
}

/// The demo's single source of truth: the signed-in persona and their data.
/// Every screen reads from here, so numbers agree everywhere (Home, Activity, Search,
/// the assistant). Actions mutate the state and return receipts where money moves.
@Observable
final class DemoStore {
    var session: Session?
    var state = PersonaState()

    /// Needs you is folded (stays folded until unfolded; new items don't unfold it).
    var isNeedsYouFolded = false
    /// Time-of-day override for the demo menu (nil = real time).
    var hourOverride: Int?
    /// First launch shows the tour stories; later only announcements.
    var hasSeenTour = false

    var persona: Persona? {
        if case .signedIn(let p) = session { return p }
        return nil
    }

    var isGuest: Bool { session == .guest }

    var now: Date {
        guard let h = hourOverride else { return .now }
        return Calendar.current.date(bySettingHour: h, minute: 41, second: 0, of: .now) ?? .now
    }

    var timeOfDay: TimeOfDay { TimeOfDay(date: now) }

    // MARK: Session

    func signIn(_ persona: Persona) {
        state = persona.makeState()
        isNeedsYouFolded = false
        session = .signedIn(persona)
    }

    func continueAsGuest() {
        state = Personas.guestState()
        session = .guest
    }

    func signOut() {
        session = nil
        state = PersonaState()
    }

    /// Restores the current persona's data to how it started.
    func resetDemo() {
        if let p = persona { signIn(p) } else if isGuest { continueAsGuest() }
    }

    // MARK: Needs you

    var payables: [Payable] { state.needsYou.compactMap(\.payable) }

    /// Total of everything payable in Needs you.
    var payAllTotal: Decimal { payables.reduce(0) { $0 + $1.amount } }

    /// Pay all shows when 2 or more payments are due.
    var showsPayAll: Bool { payables.count >= 2 }

    /// "2 due this week"
    var dueThisWeekCount: Int {
        state.needsYou.filter { item in
            guard let due = item.due else { return false }
            let d = DemoDate.daysFromToday(due)
            return d >= 0 && d <= 6
        }.count
    }

    /// The next thing due (for the folded Needs you line).
    var nextDue: NeedsYouItem? {
        state.needsYou.filter { $0.due != nil }.min { ($0.due ?? .distantFuture) < ($1.due ?? .distantFuture) }
    }

    func payAllRequest() -> PaymentRequest {
        let items = payables
        return PaymentRequest(
            id: "pay-all-\(items.map(\.id).joined(separator: "-"))",
            title: items.count == 2 ? "Pay both" : "Pay all \(items.count)",
            lines: items.map { PaymentLine(title: $0.title, detail: $0.entity, amount: $0.amount, symbol: $0.symbol, hue: $0.hue) },
            purpose: .payables(items.map(\.id))
        )
    }

    func paymentRequest(for payable: Payable) -> PaymentRequest {
        PaymentRequest(
            id: "pay-\(payable.id)",
            title: payable.title,
            lines: [PaymentLine(title: payable.title, detail: payable.entity, amount: payable.amount, symbol: payable.symbol, hue: payable.hue)],
            purpose: .payables([payable.id])
        )
    }

    /// "Later" on a Needs you card: moves it to the end (it stays in Activity).
    func later(_ item: NeedsYouItem) {
        guard let i = state.needsYou.firstIndex(of: item) else { return }
        withAnimation(DNMotion.resolve(DNMotion.smooth)) {
            let moved = state.needsYou.remove(at: i)
            state.needsYou.append(moved)
        }
    }

    // MARK: Payments (fake — no money moves)

    /// Applies a confirmed payment and returns its receipt.
    @discardableResult
    func completePayment(_ request: PaymentRequest, method: PaymentMethod = .applePay) -> Receipt {
        let reference = Self.reference(prefix: "DN")
        var receipt = Receipt(
            id: "r-\(UUID().uuidString.prefix(8))",
            title: request.title,
            shortTitle: request.title,
            entity: "DubaiNow",
            amount: request.total,
            date: .now,
            method: method,
            reference: reference,
            rows: [],
            symbol: "checkmark.circle.fill",
            hue: .mint,
            serviceID: nil,
            doneByAssistant: request.doneByAssistant
        )

        switch request.purpose {
        case .payables(let ids):
            let paid = payables.filter { ids.contains($0.id) }
            withAnimation(DNMotion.resolve(DNMotion.smooth)) {
                state.needsYou.removeAll { item in ids.contains(item.payable?.id ?? "") }
                for i in state.vehicles.indices { state.vehicles[i].fineIDs.removeAll { ids.contains($0) } }
            }
            if paid.count == 1, let p = paid.first {
                receipt.title = Self.paidTitle(for: p)
                receipt.shortTitle = receipt.title
                receipt.entity = p.entity
                receipt.symbol = p.symbol
                receipt.hue = p.hue
                receipt.serviceID = p.serviceID
                receipt.reference = p.reference
                receipt.rows = [ReceiptRow(label: "For", value: p.detail)]
            } else {
                receipt.title = "Paid \(paid.count) bills and fines"
                receipt.shortTitle = receipt.title
                receipt.symbol = "creditcard.fill"
                receipt.hue = .sky
                receipt.serviceID = "pay-all"
                receipt.rows = paid.map { ReceiptRow(label: "\($0.entity) · \($0.title)", value: Money.aed($0.amount)) }
            }
        case .salikTopUp(let amount):
            state.salikBalance += amount
            receipt.title = "Topped up your Salik account"
            receipt.shortTitle = "Topped up Salik"
            receipt.entity = "Salik"
            receipt.symbol = "road.lanes"
            receipt.hue = .sand
            receipt.serviceID = "top-up-salik-account"
            receipt.rows = [ReceiptRow(label: "New balance", value: Money.aed(state.salikBalance))]
        case .nolTopUp(let amount):
            state.nolBalance = (state.nolBalance ?? 0) + amount
            receipt.title = "Topped up your nol card"
            receipt.shortTitle = "Topped up nol"
            receipt.entity = "RTA"
            receipt.symbol = "tram.fill"
            receipt.hue = .sky
            receipt.serviceID = "top-up-nol-card"
            receipt.rows = [ReceiptRow(label: "New balance", value: Money.aed(state.nolBalance ?? 0))]
        case .extendParking(let minutes):
            if var p = state.parking {
                p.endsAt = max(p.endsAt, .now).addingTimeInterval(TimeInterval(minutes * 60))
                state.parking = p
                receipt.rows = [ReceiptRow(label: "Zone", value: p.zone), ReceiptRow(label: "Now ends", value: DemoDate.time(p.endsAt))]
            }
            receipt.title = "Extended your parking"
            receipt.shortTitle = "Extended parking"
            receipt.entity = "Parkin"
            receipt.symbol = "p.square.fill"
            receipt.hue = .sky
            receipt.serviceID = "pay-for-parking"
        case .requestFees(let requestID, _):
            advanceRequest(requestID)
            receipt.title = "Paid the fees for your request"
            receipt.shortTitle = "Paid request fees"
            if let r = state.requests.first(where: { $0.id == requestID }) {
                receipt.entity = r.entity
                receipt.symbol = r.symbol
                receipt.hue = r.hue
                receipt.serviceID = r.serviceID
                receipt.title = "Paid the fees · \(r.title)"
                receipt.shortTitle = receipt.title
            }
        case .vehicleRenewal(let vehicleID, let fee, let fineIDs):
            withAnimation(DNMotion.resolve(DNMotion.smooth)) {
                state.needsYou.removeAll { item in
                    if fineIDs.contains(item.payable?.id ?? "") { return true }
                    if case .renewal(let sid, let thing) = item.kind, sid == "renew-vehicle-registration", thing == vehicleID { return true }
                    return false
                }
                if let i = state.vehicles.firstIndex(where: { $0.id == vehicleID }) {
                    state.vehicles[i].fineIDs = []
                    let old = state.vehicles[i].registrationExpiry
                    state.vehicles[i].registrationExpiry = Calendar.current.date(byAdding: .year, value: 1, to: old) ?? old
                }
            }
            let v = state.vehicles.first { $0.id == vehicleID }
            receipt.title = "Renewed your vehicle registration"
            receipt.shortTitle = "Renewed registration"
            receipt.entity = "RTA"
            receipt.symbol = "car.fill"
            receipt.hue = .sky
            receipt.serviceID = "renew-vehicle-registration"
            receipt.rows = [
                ReceiptRow(label: "Vehicle", value: v.map { "\($0.shortName) · \($0.plate.code) \($0.plate.number)" } ?? ""),
                ReceiptRow(label: "Registration fee", value: Money.aed(fee)),
            ] + (fineIDs.isEmpty ? [] : [ReceiptRow(label: "Fines cleared", value: "\(fineIDs.count)")])
            if let v { receipt.rows.append(ReceiptRow(label: "Valid until", value: DemoDate.full(v.registrationExpiry))) }
        case .donation(let charity, _):
            receipt.title = "Donated to \(charity)"
            receipt.shortTitle = receipt.title
            receipt.entity = charity
            receipt.symbol = "heart.fill"
            receipt.hue = .lilac
            receipt.serviceID = "donate-to-charity"
        }

        state.history.insert(receipt, at: 0)
        return receipt
    }

    func receipt(_ id: String) -> Receipt? { state.history.first { $0.id == id } }

    func rate(receiptID: String, _ face: HappinessFace) {
        guard let i = state.history.firstIndex(where: { $0.id == receiptID }) else { return }
        state.history[i].rating = face
    }

    // MARK: Requests

    func request(_ id: String) -> RequestItem? { state.requests.first { $0.id == id } }

    /// Completes the current step (e.g. passport uploaded) and moves to the next.
    func advanceRequest(_ id: String) {
        guard let r = state.requests.firstIndex(where: { $0.id == id }) else { return }
        var req = state.requests[r]
        if let c = req.currentStepIndex {
            req.steps[c].state = .done
            if c + 1 < req.steps.count { req.steps[c + 1].state = .current }
        }
        req.notice = nil
        if let next = req.currentStepIndex, req.steps[next].title.lowercased().hasPrefix("pay") {
            // The next step needs the resident again (e.g. pay the fees).
            req.status = .yourTurn(due: DemoDate.day(3))
            req.action = "Pay the fees"
        } else {
            req.status = .inReview(answerBy: DemoDate.day(2))
            req.action = nil
        }
        withAnimation(DNMotion.resolve(DNMotion.smooth)) {
            state.requests[r] = req
            state.needsYou.removeAll { if case .requestStep(let rid) = $0.kind { return rid == id }; return false }
        }
    }

    /// Records a request sent without a payment (e.g. a report the assistant submitted),
    /// so it shows in Activity › In progress.
    func addRequest(_ request: RequestItem) {
        withAnimation(DNMotion.resolve(DNMotion.smooth)) {
            state.requests.removeAll { $0.id == request.id }
            state.requests.insert(request, at: 0)
        }
    }

    // MARK: Favorites (up to 8)

    static let favoritesLimit = 8

    func isFavorite(serviceID: String) -> Bool {
        state.favorites.contains { $0.target == .service(serviceID) || $0.target == .thing(serviceID) }
    }

    /// Adds or removes a catalogue record as a favorite. Returns false when full (8 of 8).
    @discardableResult
    func toggleFavorite(_ service: CatalogueService) -> Bool {
        if let i = state.favorites.firstIndex(where: { $0.target == .service(service.id) || $0.target == .thing(service.id) }) {
            withAnimation(DNMotion.resolve(DNMotion.smooth)) { _ = state.favorites.remove(at: i) }
            return true
        }
        guard state.favorites.count < Self.favoritesLimit else { return false }
        let item = FavoriteItem(
            id: "fav-\(service.id)",
            title: service.shortName,
            symbol: service.symbol,
            hue: service.hue,
            target: service.kind == "thing" ? .thing(service.id) : .service(service.id)
        )
        withAnimation(DNMotion.resolve(DNMotion.bouncy)) { state.favorites.append(item) }
        return true
    }

    func removeFavorite(_ item: FavoriteItem) {
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { state.favorites.removeAll { $0.id == item.id } }
    }

    func moveFavorites(from: IndexSet, to: Int) {
        state.favorites.move(fromOffsets: from, toOffset: to)
    }

    // MARK: Widgets

    func addWidget(_ kind: WidgetKind, size: WidgetSize) {
        let w = HomeWidget(id: "w-\(UUID().uuidString.prefix(6))", kind: kind, size: size)
        withAnimation(DNMotion.resolve(DNMotion.bouncy)) { state.widgets.append(w) }
        if state.widgetSuggestion == kind { state.widgetSuggestion = nil }
    }

    func removeWidget(_ widget: HomeWidget) {
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { state.widgets.removeAll { $0.id == widget.id } }
    }

    func resizeWidget(_ widget: HomeWidget, to size: WidgetSize) {
        guard let i = state.widgets.firstIndex(where: { $0.id == widget.id }) else { return }
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { state.widgets[i].size = size }
    }

    func moveWidgets(from: IndexSet, to: Int) {
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { state.widgets.move(fromOffsets: from, toOffset: to) }
    }

    func hasWidget(_ kind: WidgetKind) -> Bool { state.widgets.contains { $0.kind == kind } }

    /// "Not now" hides a suggestion for 30 days.
    func dismissSuggestion() {
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { state.widgetSuggestion = nil }
    }

    // MARK: Notifications

    var unreadCount: Int { state.notifications.filter { !$0.isRead }.count }

    func markAllRead() {
        for i in state.notifications.indices { state.notifications[i].isRead = true }
    }

    func markRead(_ id: String) {
        if let i = state.notifications.firstIndex(where: { $0.id == id }) { state.notifications[i].isRead = true }
    }

    // MARK: Drafts

    func deleteDraft(_ draft: DraftItem) {
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { state.drafts.removeAll { $0.id == draft.id } }
    }

    // MARK: Helpers

    static func reference(prefix: String) -> String {
        let n = Int.random(in: 1_000_000...9_999_999)
        let d = DemoDate.short(.now).replacingOccurrences(of: " ", with: "").uppercased()
        return "\(prefix)-\(d)-\(n)"
    }

    private static func paidTitle(for p: Payable) -> String {
        switch p.kind {
        case .bill: "Paid your \(p.title.lowercased())"
        case .fine: "Paid a \(p.title.lowercased())"
        case .fee: "Paid \(p.title.lowercased())"
        case .topUp: "Topped up \(p.title)"
        }
    }
}
