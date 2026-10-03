import SwiftUI
import LocalAuthentication

/// The scripted assistant (demo only: no network, no AI). It runs on the same rails as the
/// screens: everything it does goes through `DemoStore`, so the screen underneath updates
/// in place while it works.
@Observable
final class AssistantEngine {
    static let listenSeed = "__listen__"

    var phase: AssistantPhase = .idle
    var face: AssistantFaceState = .awake
    /// Working: how far round the ring the light has filled (0…1).
    var progress: Double = 0
    var card: AssistantCard?
    var confirm: AssistantConfirm?
    var isConfirming = false
    var draft = ""
    /// Listening: words heard so far, and the word still being recognised (grey).
    var heard = ""
    var hearing = ""
    /// Mandarin answers: show the English version instead.
    var showsEnglish = false
    var note: AssistantNote?
    var methodIndex = 0
    /// Done-once intents (not suggested again in this session).
    var completed: Set<String> = []
    /// After a task, the single suggestion prefers this part of life.
    var lastTopic: String?
    /// Offer "Continue in English" after a Mandarin task.
    var offersEnglish = false

    @ObservationIgnored private var store: DemoStore?
    @ObservationIgnored private var router: Router?
    @ObservationIgnored private var job: Task<Void, Never>?
    @ObservationIgnored private var onConfirm: ((PaymentMethod) async -> Void)?
    @ObservationIgnored private var takeOverTarget: AssistantFollowUp?
    /// Set by the overlay: presents the passport camera for a request ("Start scanning").
    @ObservationIgnored var onScanPassport: ((String) -> Void)?
    @ObservationIgnored private var listeningSentence = ""

    func attach(store: DemoStore, router: Router) {
        self.store = store
        self.router = router
    }

    // MARK: Ways in

    /// Called when the overlay appears: hold-to-talk, a seeded question, or the launch argument.
    func start(seed: String?) {
        let launch = AssistantLaunch.take()
        if seed == Self.listenSeed || launch.listen {
            listen()
        } else if let seed, !seed.isEmpty {
            run(seed)
        } else if let text = launch.run {
            run(text)
        }
    }

    /// Hold to talk (or the mic): a live fake transcript types itself out, then sends.
    func listen(_ sentence: String? = nil) {
        cancelJob()
        let text = sentence ?? listenSentence()
        listeningSentence = text
        card = nil
        confirm = nil
        heard = ""
        hearing = ""
        set(phase: .listening, face: .listening)
        let mandarin = AssistantIntent.isMandarin(text)
        let tokens = Self.tokens(text, mandarin: mandarin)
        job = Task { [weak self] in
            try? await Task.sleep(for: .milliseconds(450))
            for (i, token) in tokens.enumerated() {
                guard let self, !Task.isCancelled else { return }
                self.hearing = token
                try? await Task.sleep(for: .milliseconds(mandarin ? 150 : 240))
                guard !Task.isCancelled else { return }
                self.heard += token
                self.hearing = i + 1 < tokens.count ? tokens[i + 1] : ""
            }
            try? await Task.sleep(for: .milliseconds(650))
            guard let self, !Task.isCancelled else { return }
            self.run(text)
        }
    }

    /// Send what's been heard straight away.
    func sendListening() {
        run(listeningSentence)
    }

    func sendDraft() {
        let text = draft
        draft = ""
        run(text)
    }

    /// Runs a question or a task.
    func run(_ text: String) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        let intent = AssistantIntent.match(trimmed)
        if intent == .continueInEnglish, card?.isMandarin == true {
            withAnimation(DNMotion.resolve(DNMotion.smooth)) { showsEnglish = true }
            offersEnglish = false
            announce(card?.headline.en ?? "")
            return
        }
        cancelJob()
        draft = ""
        confirm = nil
        onConfirm = nil
        takeOverTarget = nil
        note = nil
        offersEnglish = false
        progress = 0
        if case .employerComplaint(let m) = intent { showsEnglish = !m }
        else if case .unknown(let m) = intent { showsEnglish = !m }
        else { showsEnglish = true }
        if AssistantLaunch.forceEnglish { showsEnglish = true }
        let quote = AssistantText(trimmed)
        withAnimation(DNMotion.resolve(DNMotion.smooth)) {
            card = AssistantCard(quote: quote, headline: AssistantText("Thinking", zh: AssistantIntent.isMandarin(trimmed) ? "思考中" : nil), isThinking: true)
        }
        set(phase: .thinking, face: .thinking)
        job = Task { [weak self] in
            try? await Task.sleep(for: .milliseconds(1100))
            guard let self, !Task.isCancelled else { return }
            await self.perform(intent, quote: trimmed)
        }
    }

    // MARK: Ways out

    /// Close (×), swipe the ask bar down, tap the screen, or the VoiceOver escape gesture.
    func close() {
        cancelJob()
        if let note, face == .done || face == .quiet {
            withAnimation(DNMotion.resolve(DNMotion.smooth)) { phase = .note }
            self.note = note
            job = Task { [weak self] in
                try? await Task.sleep(for: .seconds(5))
                guard let self, !Task.isCancelled else { return }
                self.router?.closeAssistant()
            }
        } else {
            router?.closeAssistant()
        }
    }

    /// The note after a task: tap = receipt.
    func openNote() {
        cancelJob()
        let id = note?.receiptID
        router?.closeAssistant()
        if let id { router?.present(.receipt(id)) }
    }

    /// Stop sits on every task card. Nothing is paid or sent.
    func stop() {
        cancelJob()
        onConfirm = nil
        confirm = nil
        isConfirming = false
        guard var c = card else { return }
        switch c.surface {
        case .task(var t):
            t.status = .stopped
            for i in t.steps.indices where t.steps[i].state != .done { t.steps[i].state = .pending }
            c.surface = .task(t)
        case .complaint(var b):
            b.status = .stopped
            c.surface = .complaint(b)
        default: break
        }
        c.headline = AssistantText("Stopped. Nothing was paid or sent.", zh: c.isMandarin ? "已停止。没有付款，也没有提交任何内容。" : nil)
        c.controls = .none
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { card = c }
        progress = 0
        set(phase: .card, face: .awake)
        announce(c.headline.resolved(english: showsEnglish))
    }

    /// "I'll do it myself": opens the service at the same step.
    func takeOver() {
        let target = takeOverTarget
        cancelJob()
        hand(over: target)
    }

    func perform(_ followUp: AssistantFollowUp) {
        cancelJob()
        guard case .scanPassport(let id) = followUp else { hand(over: followUp); return }
        // An older card can outlive the passport step: only open the camera while the request still waits on it.
        if let r = store?.request(id), waitsOnPassport(r) {
            onScanPassport?(id)
        } else {
            hand(over: .present(.comingSoon(Self.scanComingSoon(owner: store?.request(id)?.entity ?? "GDRFA"))))
        }
    }

    private func hand(over target: AssistantFollowUp?) {
        guard let router else { return }
        router.closeAssistant()
        guard let target else { return }
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(350))
            switch target {
            case .push(let route):
                if router.assistantCurrentPath.last != route { router.push(route) }
            case .present(let sheet):
                router.present(sheet)
            case .openService(let id):
                router.open(serviceID: id)
            case .scanPassport:
                break // presented over the assistant by perform(_:)
            }
        }
    }

    // MARK: Your turn

    var paymentMethod: PaymentMethod {
        let methods = store?.state.paymentMethods ?? [.applePay]
        guard !methods.isEmpty else { return .applePay }
        return methods[methodIndex % methods.count]
    }

    var canChangeMethod: Bool { (store?.state.paymentMethods.count ?? 0) > 1 }

    func changeMethod() {
        DNHaptics.selection()
        methodIndex += 1
    }

    /// The one confirm per task: Face ID (LocalAuthentication) or a short confirm in the Simulator.
    func confirmTapped() {
        guard let action = onConfirm, let confirm, !isConfirming else { return }
        isConfirming = true
        let method = paymentMethod
        job = Task { [weak self] in
            let ok = await AssistantAuth.confirm(reason: confirm.reason)
            guard let self else { return }
            self.isConfirming = false
            guard ok, !Task.isCancelled else { return }
            self.onConfirm = nil
            withAnimation(DNMotion.resolve(DNMotion.smooth)) { self.confirm = nil }
            await action(method)
        }
    }

    func rate(_ face: HappinessFace, receiptID: String) {
        store?.rate(receiptID: receiptID, face)
    }

    // MARK: Suggestions

    /// Up to three for the screen you're on, and one after a task. Never a button already on screen.
    func suggestions(store: DemoStore, router: Router) -> [AssistantSuggestion] {
        switch phase {
        case .listening, .thinking, .note: return []
        case .idle: return Array(available(store: store, router: router).prefix(3))
        case .card:
            guard confirm == nil else { return [] }
            if offersEnglish && !showsEnglish {
                return [AssistantSuggestion(text: "Continue in English", symbol: "character.bubble", hue: .sky)]
            }
            if case .task(let t) = card?.surface, t.status == .working { return [] }
            if case .complaint(let b) = card?.surface, b.status == .working { return [] }
            let all = available(store: store, router: router)
            if card?.rating != nil || face == .done || face == .quiet {
                let sameTopic = all.first { AssistantIntent.match($0.text).topic == lastTopic }
                return [sameTopic ?? all.first].compactMap { $0 }
            }
            if card?.headline.en.hasPrefix("I can't") == true { return Array(all.prefix(3)) }
            return Array(all.prefix(1))
        }
    }

    private func available(store: DemoStore, router: Router) -> [AssistantSuggestion] {
        var texts: [String] = []
        // On a request tracker that's waiting for you: help with that step (Paper UR2-0).
        if case .request(let id)? = router.assistantCurrentPath.last, let r = store.request(id), case .yourTurn = r.status {
            let step = r.currentStepIndex.map { r.steps[$0].title.lowercased() } ?? ""
            if step.contains("passport") { texts.append("Scan my passport with me") }
            if r.steps.contains(where: { $0.title.lowercased().contains("fee") && $0.state == .upcoming }) { texts.append("What will the fees be?") }
            texts.append("Remind me tonight at 8")
            return texts.map { Self.suggestion($0) }
        }
        var prompts = store.persona?.assistantPrompts ?? []
        if store.persona?.id == "wei" {
            // Wei's showcase is Mandarin; independent of his (placeholder) data.
            if !prompts.contains(where: { AssistantIntent.isMandarin($0) }) { prompts.insert(Self.weiPrompt, at: 0) }
            if !prompts.contains(where: { $0.lowercased().contains("nol") }) { prompts.append("Top up my nol card with AED 50") }
        }
        let onHomeRoot = router.tab == .home && router.homePath.isEmpty
        for p in prompts {
            let intent = AssistantIntent.match(p)
            if completed.contains(intent.key) { continue }
            // Never repeat a button that's already on the screen (Pay all on Home).
            if case .owe = intent, onHomeRoot, store.showsPayAll { continue }
            if intent == .renewRegistration, store.assistantRenewalPlan() == nil { continue }
            texts.append(p)
        }
        return texts.map { Self.suggestion($0) }
    }

    private static func suggestion(_ text: String) -> AssistantSuggestion {
        let intent = AssistantIntent.match(text)
        return AssistantSuggestion(text: text, symbol: intent.chipSymbol, hue: intent.chipHue)
    }

    /// What hold-to-talk "hears": the first suggestion for this screen.
    private func listenSentence() -> String {
        if store?.persona?.id == "wei" { return Self.weiPrompt }
        if let store, let router, let first = available(store: store, router: router).first { return first.text }
        return store?.persona?.assistantPrompts.first ?? "How much do I owe?"
    }

    static let weiPrompt = "我的工资两个月没发了，怎么办？"

    private static func tokens(_ s: String, mandarin: Bool) -> [String] {
        if mandarin { return s.map(String.init) }
        let words = s.split(separator: " ", omittingEmptySubsequences: true).map(String.init)
        return words.enumerated().map { i, w in i == words.count - 1 ? w : w + " " }
    }

    // MARK: Scripts

    private func perform(_ intent: AssistantIntent, quote: String) async {
        switch intent {
        case .renewRegistration: await runRenewal(quote)
        case .owe(let business): runOwe(quote, business: business)
        case .needsYou: runNeedsYou(quote)
        case .visaStatus: runVisa(quote)
        case .salikTopUp(let amount): await runTopUp(quote, salik: true, amount: amount)
        case .nolTopUp(let amount): await runTopUp(quote, salik: false, amount: amount)
        case .journey(let hint): runJourney(quote, hint: hint)
        case .vehicleFines: runVehicleFines(quote)
        case .energy: runEnergy(quote)
        case .metro: runMetro(quote)
        case .fees: runFees(quote)
        case .remind: runRemind(quote)
        case .scanPassport: runScan(quote)
        case .employerComplaint(let mandarin): await runComplaint(quote, mandarin: mandarin)
        case .continueInEnglish, .unknown: runFallback(quote, mandarin: AssistantIntent.isMandarin(quote))
        }
    }

    // 1 · Renew my car registration (Paper TA7-0 → TQX-0 → U2V-0)
    private func runRenewal(_ quote: String) async {
        guard let store else { return }
        guard let plan = store.assistantRenewalPlan() else {
            show(AssistantCard(
                quote: AssistantText(quote),
                headline: AssistantText("Your vehicles are all registered. Nothing needs renewing right now."),
                controls: .action(AssistantText("See your vehicles"), .push(.vehicles))
            ), face: .awake)
            return
        }
        let v = plan.vehicle
        let finesTotal = plan.fines.reduce(Decimal(0)) { $0 + $1.amount }
        var steps = [AssistantStep(id: 0, title: AssistantText("Insurance and test are valid"))]
        if !plan.fines.isEmpty {
            let t = plan.fines.count == 1 ? "1 fine blocks the renewal" : "\(plan.fines.count) fines block the renewal"
            steps.append(AssistantStep(id: 1, title: AssistantText(t), amount: finesTotal))
        }
        steps.append(AssistantStep(id: 2, title: AssistantText("Preparing the renewal"), amount: plan.fee))
        steps.append(AssistantStep(id: 3, title: AssistantText("Your OK to pay"), amount: plan.total))
        steps.append(AssistantStep(id: 4, title: AssistantText("Pay, renew, save the new registration card")))

        let headline: String
        switch plan.fines.count {
        case 0: headline = "Your \(v.model)'s registration ends on \(DemoDate.long(v.registrationExpiry)). I'll renew it. You'll only need to confirm once."
        case 1: headline = "A \(plan.fines[0].title.lowercased()) is blocking the renewal. I'll clear it first, then renew. You'll only need to confirm once."
        default: headline = "\(plan.fines.count) fines are blocking the renewal. I'll clear them first, then renew. You'll only need to confirm once."
        }
        var task = AssistantTaskBlock(
            symbol: "car.fill", hue: .sky,
            title: AssistantText("Renew registration"),
            subtitle: AssistantText("\(v.shortName) · \(v.plate.code) \(v.plate.number)"),
            steps: steps
        )
        takeOverTarget = .openService("renew-vehicle-registration")
        show(AssistantCard(quote: AssistantText(quote), headline: AssistantText(headline), surface: .task(task), controls: .task), face: .working)

        // Checks and preparation, one step at a time; the ring fills as steps finish.
        let n = Double(steps.count)
        for i in 0..<(steps.count - 2) {
            task.steps[i].state = .active
            update(task)
            try? await Task.sleep(for: .milliseconds(i == steps.count - 3 ? 1400 : 800))
            guard !Task.isCancelled else { return }
            task.steps[i].state = .done
            if task.steps[i].title.en == "Preparing the renewal" { task.steps[i].title = AssistantText("Renewal is ready") }
            update(task)
            withAnimation(DNMotion.resolve(DNMotion.smooth)) { progress = Double(i + 1) / n }
        }
        guard !Task.isCancelled else { return }

        // Your turn: one confirm, under the thumb.
        task.steps[steps.count - 2].state = .yourTurn
        task.status = .yourTurn
        let ready = plan.fines.isEmpty
            ? "Ready. The renewal comes to \(Money.aed(plan.total, trimWhole: true)). Pay once and I'll do the rest."
            : "Ready. The fine and the renewal come to \(Money.aed(plan.total, trimWhole: true)). Pay once and I'll do the rest."
        if var c = card {
            c.headline = AssistantText(ready)
            c.surface = .task(task)
            withAnimation(DNMotion.resolve(DNMotion.smooth)) { card = c }
        }
        let lines = [PaymentLine(title: "Vehicle registration", detail: "RTA · \(v.shortName)", amount: plan.fee, symbol: "car.fill", hue: .sky)]
            + plan.fines.map { PaymentLine(title: $0.title, detail: $0.entity, amount: $0.amount, symbol: $0.symbol, hue: $0.hue) }
        let request = PaymentRequest(
            id: "assistant-renewal-\(v.id)",
            title: plan.fines.isEmpty ? "Renew registration" : (plan.fines.count == 1 ? "Renew registration and pay a fine" : "Renew registration and pay \(plan.fines.count) fines"),
            lines: lines,
            purpose: .vehicleRenewal(vehicleID: v.id, fee: plan.fee, fineIDs: plan.fines.map(\.id)),
            doneByAssistant: true
        )
        let amount = Money.aed(plan.total, trimWhole: true)
        yourTurn(AssistantConfirm(style: .pay(plan.total), title: AssistantText("Pay \(amount)"), reason: "Pay \(amount) to renew your registration")) { [weak self] method in
            await self?.finishRenewal(plan: plan, request: request, task: task, method: method)
        }
    }

    private func finishRenewal(plan: AssistantRenewalPlan, request: PaymentRequest, task: AssistantTaskBlock, method: PaymentMethod) async {
        guard let store else { return }
        var task = task
        let last = task.steps.count - 1
        task.steps[last - 1].state = .done
        task.steps[last].state = .active
        task.status = .working
        update(task)
        set(phase: .card, face: .working)
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { progress = Double(last) / Double(task.steps.count) }
        try? await Task.sleep(for: .milliseconds(900))
        guard !Task.isCancelled else { return }

        let receipt = store.completePayment(request, method: method)
        DNHaptics.success()
        let v = store.state.vehicles.first { $0.id == plan.vehicle.id } ?? plan.vehicle
        let until = AssistantFormat.dayMonthYear(v.registrationExpiry)
        let finesTotal = plan.fines.reduce(Decimal(0)) { $0 + $1.amount }
        var rows: [AssistantStep] = []
        if !plan.fines.isEmpty {
            rows.append(AssistantStep(id: 0, title: AssistantText(plan.fines.count == 1 ? "Fine paid" : "Fines paid"), amount: finesTotal, state: .done))
        }
        rows.append(AssistantStep(id: 1, title: AssistantText("Registration renewed"), amount: plan.fee, state: .done))
        rows.append(AssistantStep(id: 2, title: AssistantText("Valid until \(DemoDate.full(v.registrationExpiry))"), state: .done))
        rows.append(AssistantStep(id: 3, title: AssistantText("New card saved to Documents"), state: .done))
        var done = task
        done.steps = rows
        done.status = .done
        done.receiptID = receipt.id
        done.reference = receipt.reference
        done.paidLine = "Paid \(Money.aed(request.total)) · \(method.label)"
        let headline = plan.fines.isEmpty
            ? "Done. Your \(v.model) is registered until \(until)."
            : "Done. Your \(v.model) is registered until \(until), and the fine is paid."
        finish(
            headline: AssistantText(headline),
            surface: .task(done),
            rating: AssistantRatingPrompt(question: "How was renewing your registration?", detail: "One tap. It goes to RTA and the Happiness Meter.", receiptID: receipt.id, thanks: "Thanks. RTA and the Happiness Meter will see this."),
            note: AssistantNote(text: "\(v.model) renewed", receiptID: receipt.id),
            intent: .renewRegistration
        )
    }

    // 2 · How much do I owe? / 6 · Pay everything for the café
    private func runOwe(_ quote: String, business: Bool) {
        guard let store else { return }
        let items = store.payables
        guard !items.isEmpty else {
            show(AssistantCard(quote: AssistantText(quote), headline: AssistantText("You don't owe anything right now. I'll tell you when a bill or fine comes in.")), face: .awake)
            return
        }
        let total = store.payAllTotal
        let headline: String
        if business {
            headline = "Everything due for the café comes to \(Money.aed(total)): \(items.count == 1 ? "1 payment" : "\(items.count) bills and fines"). Pay once and I'll do the rest."
        } else if items.count == 1 {
            headline = "You owe \(Money.aed(total)): your \(items[0].title.lowercased()) from \(items[0].entity)."
        } else {
            headline = "You owe \(Money.aed(total)) across \(items.count) payments. Pay them together with one confirm."
        }
        var request = items.count == 1 ? store.paymentRequest(for: items[0]) : store.payAllRequest()
        request.doneByAssistant = true
        takeOverTarget = .present(.payment(request))
        show(AssistantCard(quote: AssistantText(quote), headline: AssistantText(headline), surface: .payables(items), controls: .task), face: .yourTurn)
        let title = items.count == 1 ? "Pay \(Money.aed(total))" : "Pay all · \(Money.aed(total))"
        yourTurn(AssistantConfirm(style: .pay(total), title: AssistantText(title), reason: "Pay \(Money.aed(total))")) { [weak self] method in
            await self?.finishPayAll(items: items, request: request, method: method)
        }
    }

    private func finishPayAll(items: [Payable], request: PaymentRequest, method: PaymentMethod, intent: AssistantIntent = .owe(business: false)) async {
        guard let store else { return }
        var task = AssistantTaskBlock(
            symbol: "creditcard.fill", hue: .sky,
            title: AssistantText(items.count == 1 ? items[0].title : "Pay all"),
            subtitle: AssistantText(items.count == 1 ? items[0].entity : "\(items.count) bills and fines"),
            steps: items.enumerated().map { i, p in AssistantStep(id: i, title: AssistantText(p.title), amount: p.amount, state: .active) }
        )
        if var c = card { c.surface = .task(task); c.controls = .none; withAnimation(DNMotion.resolve(DNMotion.smooth)) { card = c } }
        set(phase: .card, face: .working)
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { progress = 0.6 }
        try? await Task.sleep(for: .milliseconds(900))
        guard !Task.isCancelled else { return }

        let receipt = store.completePayment(request, method: method)
        DNHaptics.success()
        for i in task.steps.indices { task.steps[i].state = .done }
        task.status = .done
        task.receiptID = receipt.id
        task.reference = receipt.reference
        task.paidLine = "Paid \(Money.aed(request.total)) · \(method.label)"
        let rest = store.payables.isEmpty ? " Nothing else is due." : ""
        finish(
            headline: AssistantText("Done. You've paid \(Money.aed(request.total)).\(rest)"),
            surface: .task(task),
            rating: AssistantRatingPrompt(question: "How was paying?", detail: "One tap. It goes to the Happiness Meter.", receiptID: receipt.id, thanks: "Thanks. The Happiness Meter will see this."),
            note: AssistantNote(text: "Paid \(Money.aed(request.total, trimWhole: true))", receiptID: receipt.id),
            intent: intent
        )
    }

    // What do I need to do?
    private func runNeedsYou(_ quote: String) {
        guard let store else { return }
        let items = store.state.needsYou
        guard !items.isEmpty else {
            show(AssistantCard(quote: AssistantText(quote), headline: AssistantText("Nothing needs you right now. I'll tell you when something does.")), face: .awake)
            return
        }
        let week = store.dueThisWeekCount
        let headline = "\(items.count == 1 ? "1 thing needs" : "\(items.count) things need") you" + (week > 0 ? ", \(week) this week." : ".")
        let rows = items.prefix(4).map { item in
            AssistantListRow(
                id: item.id, symbol: item.symbol, hue: item.hue, title: item.title,
                detail: item.due.map { "\(item.entity) · \(DemoDate.dueLabel($0))" } ?? item.entity,
                value: item.amount.map { Money.aed($0) }
            )
        }
        show(AssistantCard(quote: AssistantText(quote), headline: AssistantText(headline), surface: .list(Array(rows))), face: .awake)
    }

    // 3 · When will my visa renewal be done?
    private func runVisa(_ quote: String) {
        guard let store else { return }
        let visas = store.state.requests.filter { ($0.title + $0.serviceID).lowercased().contains("visa") }
        guard let r = visas.first(where: { $0.status != .done }) ?? visas.first else {
            if let doc = store.state.documents.first(where: { $0.title.lowercased().contains("visa") }), let expiry = doc.expiry {
                let days = DemoDate.daysFromToday(expiry)
                let headline = days > 90
                    ? "Your \(doc.title.lowercased()) is valid until \(DemoDate.full(expiry)). Nothing to do yet. I'll remind you 90 days before."
                    : "Your \(doc.title.lowercased()) ends on \(DemoDate.full(expiry)), in \(days) days. You can renew it now."
                show(AssistantCard(
                    quote: AssistantText(quote),
                    headline: AssistantText(headline),
                    surface: .list([AssistantListRow(id: doc.id, symbol: doc.symbol, hue: doc.hue, title: doc.title, detail: doc.number, value: DemoDate.short(expiry))]),
                    controls: .action(AssistantText("See the document"), .push(.document(doc.id)))
                ), face: .awake)
                completed.insert(AssistantIntent.visaStatus.key)
                return
            }
            show(AssistantCard(
                quote: AssistantText(quote),
                headline: AssistantText("You don't have a visa renewal in progress."),
                controls: .action(AssistantText("Renew a residence visa"), .openService("renew-residence-visa"))
            ), face: .awake)
            return
        }
        let headline: String
        var action = AssistantText("Open the tracker")
        switch r.status {
        case .yourTurn(let due):
            let ask = store.state.needsYou.first { if case .requestStep(let id) = $0.kind { return id == r.id }; return false }?.title
                ?? r.currentStepIndex.map { r.steps[$0].title } ?? "Finish your step"
            headline = "It's waiting for you: \(AssistantFormat.lowerFirst(ask)) by \(DemoDate.long(due)). Then \(r.entity) usually decides within 2 working days."
            action = AssistantText(r.action ?? "Open the tracker")
        case .inReview(let by):
            headline = "\(r.entity) is reviewing it. You should hear back by \(DemoDate.long(by))."
        case .approved(let note):
            headline = "It's approved: \(note)."
        case .done:
            headline = "It's done. Your new visa is in Documents."
        }
        show(AssistantCard(quote: AssistantText(quote), headline: AssistantText(headline), surface: .request(r), controls: .action(action, .push(.request(r.id)))), face: .awake)
        completed.insert(AssistantIntent.visaStatus.key)
    }

    // 4 · Top up Salik with AED 100 (and nol for Wei)
    private func runTopUp(_ quote: String, salik: Bool, amount: Decimal) async {
        guard let store else { return }
        let amount = max(amount, 1)
        let balance: Decimal
        if salik {
            balance = store.state.salikBalance
        } else if let nol = store.state.nolBalance {
            balance = nol
        } else {
            show(AssistantCard(
                quote: AssistantText(quote),
                headline: AssistantText("I can't find a nol card on your account yet."),
                controls: .action(AssistantText("Top up a nol card"), .openService("top-up-nol-card"))
            ), face: .awake)
            return
        }
        let name = salik ? "Salik" : "nol card"
        let tags = store.state.vehicles.compactMap(\.salikTag).count
        let headline: String
        if salik {
            let trips = NSDecimalNumber(decimal: balance / 4).intValue
            headline = "Your Salik balance is \(Money.aed(balance, trimWhole: true)), about \(trips) trips. I'll top it up with \(Money.aed(amount, trimWhole: true))."
        } else {
            headline = "Your nol card has \(Money.aed(balance)). I'll top it up with \(Money.aed(amount, trimWhole: true))."
        }
        var task = AssistantTaskBlock(
            symbol: salik ? "road.lanes" : "tram.fill", hue: salik ? .sand : .sky,
            title: AssistantText(salik ? "Top up Salik" : "Top up your nol card"),
            subtitle: AssistantText(salik ? "\(tags == 1 ? "1 tag" : "\(tags) tags") · balance \(Money.aed(balance))" : "Balance \(Money.aed(balance))"),
            steps: [
                AssistantStep(id: 0, title: AssistantText(salik ? "Salik account found" : "nol card found")),
                AssistantStep(id: 1, title: AssistantText("Your OK to pay"), amount: amount),
                AssistantStep(id: 2, title: AssistantText("Pay and update your balance")),
            ]
        )
        takeOverTarget = salik ? .present(.topUpSalik) : .openService("top-up-nol-card")
        show(AssistantCard(quote: AssistantText(quote), headline: AssistantText(headline), surface: .task(task), controls: .task), face: .working)
        task.steps[0].state = .active
        update(task)
        try? await Task.sleep(for: .milliseconds(800))
        guard !Task.isCancelled else { return }
        task.steps[0].state = .done
        task.steps[1].state = .yourTurn
        task.status = .yourTurn
        update(task)
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { progress = 1.0 / 3 }
        let request = PaymentRequest(
            id: "assistant-\(salik ? "salik" : "nol")-\(UUID().uuidString.prefix(6))",
            title: salik ? "Top up Salik" : "Top up nol card",
            lines: [PaymentLine(title: "\(name) top-up", detail: salik ? "Salik" : "RTA", amount: amount, symbol: task.symbol, hue: task.hue)],
            purpose: salik ? .salikTopUp(amount) : .nolTopUp(amount),
            doneByAssistant: true
        )
        let label = Money.aed(amount, trimWhole: true)
        yourTurn(AssistantConfirm(style: .pay(amount), title: AssistantText("Pay \(label)"), reason: "Pay \(label) to top up your \(name)")) { [weak self] method in
            await self?.finishTopUp(salik: salik, request: request, task: task, method: method)
        }
    }

    private func finishTopUp(salik: Bool, request: PaymentRequest, task: AssistantTaskBlock, method: PaymentMethod) async {
        guard let store else { return }
        var task = task
        task.steps[1].state = .done
        task.steps[2].state = .active
        task.status = .working
        update(task)
        set(phase: .card, face: .working)
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { progress = 2.0 / 3 }
        try? await Task.sleep(for: .milliseconds(800))
        guard !Task.isCancelled else { return }
        let receipt = store.completePayment(request, method: method)
        DNHaptics.success()
        let balance = salik ? store.state.salikBalance : (store.state.nolBalance ?? 0)
        var done = task
        done.steps = [
            AssistantStep(id: 0, title: AssistantText("Topped up"), amount: request.total, state: .done),
            AssistantStep(id: 1, title: AssistantText("New balance"), amount: balance, state: .done),
        ]
        done.status = .done
        done.receiptID = receipt.id
        done.reference = receipt.reference
        done.paidLine = "Paid \(Money.aed(request.total)) · \(method.label)"
        finish(
            headline: AssistantText("Done. Your \(salik ? "Salik balance" : "nol card balance") is now \(Money.aed(balance))."),
            surface: .task(done),
            rating: AssistantRatingPrompt(question: salik ? "How was topping up Salik?" : "How was topping up your nol card?", detail: "One tap. It goes to \(salik ? "Salik" : "RTA") and the Happiness Meter.", receiptID: receipt.id, thanks: "Thanks. \(salik ? "Salik" : "RTA") and the Happiness Meter will see this."),
            note: AssistantNote(text: salik ? "Salik topped up" : "nol card topped up", receiptID: receipt.id),
            intent: salik ? .salikTopUp(request.total) : .nolTopUp(request.total)
        )
    }

    // 5 · What's next for my house?
    private func runJourney(_ quote: String, hint: String) {
        guard let store else { return }
        let wanted: String? = ["house", "build", "villa", "drawing"].contains(where: hint.contains) ? "build-a-house"
            : (hint.contains("move") ? "move-home" : nil)
        let started = store.state.journeys
        let pick = started.first { $0.journeyID == wanted } ?? (wanted == nil ? started.first : nil)
        guard let pick, let service = Catalogue.shared.service(pick.journeyID), let stages = service.stages, !stages.isEmpty else {
            if let wanted, let s = Catalogue.shared.service(wanted), let stages = s.stages, let first = stages.first, let last = stages.last {
                show(AssistantCard(
                    quote: AssistantText(quote),
                    headline: AssistantText("You haven't started this yet. \(s.shortName) takes \(stages.count) stages, from \(first.name.lowercased()) to \(last.name.lowercased()). I can show you where to begin."),
                    controls: .action(AssistantText("See the journey"), .push(.journey(wanted)))
                ), face: .awake)
            } else {
                show(AssistantCard(
                    quote: AssistantText(quote),
                    headline: AssistantText("You haven't started a journey yet. Journeys take you through something big, start to finish."),
                    controls: .action(AssistantText("See all journeys"), .push(.journeysList))
                ), face: .awake)
            }
            return
        }
        let i = min(max(pick.stageIndex, 0), stages.count - 1)
        let waiting = pick.waitingOn.replacingOccurrences(of: " · ", with: ", ")
        let headline = pick.needsYou
            ? "You're at stage \(i + 1) of \(stages.count): \(AssistantFormat.lowerFirst(stages[i].name)). It's your turn to \(AssistantFormat.lowerFirst(pick.headline)). \(waiting)."
            : "You're at stage \(i + 1) of \(stages.count): \(AssistantFormat.lowerFirst(stages[i].name)). \(waiting)."
        let block = AssistantJourneyBlock(
            symbol: service.symbol, hue: service.hue,
            title: service.shortName,
            subtitle: [Self.started(pick.startedOn), pick.destination].compactMap { $0 }.joined(separator: " · "),
            stage: i, stageCount: stages.count,
            now: pick.headline, nowDetail: pick.waitingOn,
            next: i + 1 < stages.count ? stages[i + 1].name : nil
        )
        show(AssistantCard(quote: AssistantText(quote), headline: AssistantText(headline), surface: .journey(block), controls: .action(AssistantText("Open the journey"), .push(.journey(pick.journeyID)))), face: .awake)
        completed.insert(AssistantIntent.journey("").key)
    }

    /// "Started 4 days ago", "Started yesterday", "Started on 25 Jan"
    private static func started(_ d: Date) -> String {
        let n = DemoDate.daysFromToday(d)
        if n >= -6 { return "Started \(DemoDate.relative(d).lowercased())" }
        return "Started on \(DemoDate.short(d))"
    }

    // Faisal · Which van has a fine?
    private func runVehicleFines(_ quote: String) {
        guard let store else { return }
        let fined = store.state.vehicles.enumerated().compactMap { i, v -> (Int, Vehicle, [Payable])? in
            let fines = store.payables.filter { v.fineIDs.contains($0.id) }
            return fines.isEmpty ? nil : (i, v, fines)
        }
        guard let (index, v, fines) = fined.first else {
            show(AssistantCard(quote: AssistantText(quote), headline: AssistantText("None of your vehicles has a fine right now.")), face: .awake)
            return
        }
        let total = fines.reduce(Decimal(0)) { $0 + $1.amount }
        let isVan = quote.lowercased().contains("van")
        let who = isVan ? "Van \(index + 1), the \(v.shortName) \(v.plate.code) \(v.plate.number)," : "Your \(v.shortName) \(v.plate.code) \(v.plate.number)"
        let what = fines.count == 1 ? "a \(fines[0].title.lowercased()) for \(Money.aed(total, trimWhole: true))" : "\(fines.count) fines, \(Money.aed(total, trimWhole: true)) in all"
        let rows = fines.map { AssistantListRow(id: $0.id, symbol: $0.symbol, hue: $0.hue, title: $0.title, detail: $0.detail, value: Money.aed($0.amount)) }
        var request = fines.count == 1 ? store.paymentRequest(for: fines[0]) : store.payAllRequest()
        if fines.count > 1 {
            request = PaymentRequest(id: "assistant-fines-\(v.id)", title: "Pay \(fines.count) fines", lines: fines.map { PaymentLine(title: $0.title, detail: $0.entity, amount: $0.amount, symbol: $0.symbol, hue: $0.hue) }, purpose: .payables(fines.map(\.id)))
        }
        request.doneByAssistant = true
        takeOverTarget = .push(.vehicle(v.id))
        show(AssistantCard(quote: AssistantText(quote), headline: AssistantText("\(who) has \(what). Pay it now with one confirm?"), surface: .list(rows), controls: .task), face: .yourTurn)
        let label = Money.aed(total, trimWhole: true)
        yourTurn(AssistantConfirm(style: .pay(total), title: AssistantText("Pay \(label)"), reason: "Pay \(label)")) { [weak self] method in
            await self?.finishPayAll(items: fines, request: request, method: method, intent: .vehicleFines)
        }
    }

    // Faisal · How much power did the café use?
    private func runEnergy(_ quote: String) {
        guard let store else { return }
        guard let e = store.state.energy else {
            show(AssistantCard(quote: AssistantText(quote), headline: AssistantText("I can't see an electricity account yet. Link DEWA in your profile and I'll keep track.")), face: .awake)
            return
        }
        let place = quote.lowercased().contains("café") || quote.lowercased().contains("cafe") ? "The café" : "Your home"
        let change = e.changeVsLastMonth == 0 ? "the same as the month before" : "\(abs(e.changeVsLastMonth))% \(e.changeVsLastMonth < 0 ? "less" : "more") than the month before"
        let kwh = e.kWh.formatted(.number.locale(Locale(identifier: "en_GB")))
        show(AssistantCard(
            quote: AssistantText(quote),
            headline: AssistantText("\(place) used \(kwh) kWh in \(e.monthLabel), \(change)."),
            surface: .energy(e)
        ), face: .awake)
        completed.insert(AssistantIntent.energy.key)
    }

    // Wei · When is the next metro?
    private func runMetro(_ quote: String) {
        guard let store else { return }
        guard let m = store.state.metro, let first = m.departures.first else {
            show(AssistantCard(quote: AssistantText(quote), headline: AssistantText("Add the Metro widget and pick your station, and I'll tell you when the next train leaves.")), face: .awake)
            return
        }
        let rows = m.departures.map { d in
            AssistantListRow(id: d.towards, symbol: "tram.fill", hue: .sky, title: "Towards \(d.towards)", detail: "\(m.line) · then in \(d.nextInMinutes) min", value: "\(d.inMinutes) min")
        }
        show(AssistantCard(
            quote: AssistantText(quote),
            headline: AssistantText("The next \(m.line) train towards \(first.towards) leaves \(m.name) in \(first.inMinutes) minutes."),
            surface: .list(rows)
        ), face: .awake)
        completed.insert(AssistantIntent.metro.key)
    }

    // On the visa tracker (Paper UR2-0)
    private func runFees(_ quote: String) {
        guard let store else { return }
        let r = trackerRequest(store)
        let step = r?.steps.firstIndex { $0.title.lowercased().contains("fee") && $0.state != .done }
        guard let r, let step else {
            show(AssistantCard(quote: AssistantText(quote), headline: AssistantText("There are no fees left to pay for this.")), face: .awake)
            return
        }
        let about = r.steps[step].detail.components(separatedBy: " · ").first ?? r.steps[step].detail
        show(AssistantCard(
            quote: AssistantText(quote),
            headline: AssistantText("\(about). You'll see the exact amount before you pay, at step \(step + 1) of \(r.steps.count)."),
            surface: .request(r)
        ), face: .awake)
        completed.insert(AssistantIntent.fees.key)
    }

    private func runRemind(_ quote: String) {
        guard let store else { return }
        let r = trackerRequest(store)
        let task = r.flatMap { r in store.state.needsYou.first { if case .requestStep(let id) = $0.kind { return id == r.id }; return false } }
        // At the fee step the tracker waits on its current step, with no Needs you item of its own.
        let step = r.flatMap { r -> RequestStep? in
            guard case .yourTurn = r.status, let i = r.currentStepIndex else { return nil }
            return r.steps[i]
        }
        let first = store.state.needsYou.first
        let what: String
        if let task {
            what = AssistantFormat.lowerFirst(task.title)
        } else if let r, let step {
            what = "\(AssistantFormat.lowerFirst(step.title)) to \(AssistantFormat.lowerFirst(r.title))"
        } else if let first {
            // Bills and fines are named as things ("Electricity & water bill"), the rest as things to do.
            what = first.payable != nil ? "pay your \(AssistantFormat.lowerFirst(first.title))" : AssistantFormat.lowerFirst(first.title)
        } else {
            what = "come back to this"
        }
        show(AssistantCard(
            quote: AssistantText(quote),
            headline: AssistantText("Done. I'll remind you at 20:00 tonight to \(what).")
        ), face: .done)
        completed.insert(AssistantIntent.remind.key)
    }

    private func runScan(_ quote: String) {
        // The request waiting on a passport photo: the tracker on screen, or the first one waiting.
        let r = store.flatMap { store in
            trackerRequest(store).flatMap { waitsOnPassport($0) ? $0 : nil } ?? store.state.requests.first(where: waitsOnPassport)
        }
        let owner = r?.entity ?? "GDRFA"
        // Start scanning opens the tracker's own fake camera (demo step 7) over the assistant.
        let start: AssistantFollowUp = r.map { .scanPassport(requestID: $0.id) } ?? .present(.comingSoon(Self.scanComingSoon(owner: owner)))
        show(AssistantCard(
            quote: AssistantText(quote),
            headline: AssistantText("Hold your passport flat in good light, photo page up. I'll check it's sharp before it goes to \(owner)."),
            controls: .action(AssistantText("Start scanning"), start)
        ), face: .awake)
    }

    private static func scanComingSoon(owner: String) -> ComingSoon {
        ComingSoon(
            title: "Scan your passport",
            detail: "Scanning with the camera comes with the service flows.",
            owner: owner, symbol: "camera.fill", hue: .lilac, kind: .notInDemo
        )
    }

    private func waitsOnPassport(_ r: RequestItem) -> Bool {
        guard case .yourTurn = r.status, let i = r.currentStepIndex else { return false }
        return r.steps[i].title.lowercased().contains("passport")
    }

    private func trackerRequest(_ store: DemoStore) -> RequestItem? {
        if case .request(let id)? = router?.assistantCurrentPath.last, let r = store.request(id) { return r }
        return store.state.requests.first { if case .yourTurn = $0.status { return true }; return false } ?? store.state.requests.first
    }

    // 7 · Wei asks in Mandarin (Paper UXH-0): report a problem with your employer.
    private func runComplaint(_ quote: String, mandarin: Bool) async {
        let (m1, m2) = AssistantFormat.lastTwoMonths()
        let name = store?.persona?.fullName ?? "Wei Zhang"
        let n1 = AssistantFormat.monthNumber(m1), n2 = AssistantFormat.monthNumber(m2)
        let e1 = AssistantFormat.monthEnglish(m1), e2 = AssistantFormat.monthEnglish(m2)
        let a1 = AssistantFormat.monthArabic(m1), a2 = AssistantFormat.monthArabic(m2)
        let year = AssistantFormat.year(m2)
        let fields = [
            AssistantField(id: 0, label: AssistantText("Name", zh: "姓名"), value: AssistantText("\(name) · from UAE PASS", zh: "\(name)（来自 UAE PASS）")),
            AssistantField(id: 1, label: AssistantText("Employer", zh: "雇主"), value: AssistantText("The company on your work permit", zh: "您工作许可上的公司")),
            AssistantField(id: 2, label: AssistantText("Problem", zh: "问题"), value: AssistantText("Wages for \(e1) and \(e2) not paid", zh: "\(n1)月和\(n2)月的工资未发放")),
            AssistantField(id: 3, label: AssistantText("Amount", zh: "金额"), value: AssistantText("AED 6,400", zh: "6,400 迪拉姆")),
        ]
        var block = AssistantComplaintBlock(
            fields: fields,
            originalEnglish: "My employer has not paid my wages for \(e1) and \(e2) \(year), a total of AED 6,400. I ask the Permanent Committee for Labour Affairs to look into this.",
            originalArabic: "لم يدفع صاحب العمل راتبي عن شهري \(a1) و\(a2) \(year)، بإجمالي 6,400 درهم. أرجو من اللجنة الدائمة لشؤون العمال النظر في ذلك."
        )
        let quoteText = AssistantText(Self.translation(of: quote), zh: mandarin ? quote : nil)
        let working = AssistantText(
            "You can report this to Dubai's labour authority, and it stays confidential. I'm preparing it now with your details from UAE PASS.",
            zh: "你可以向迪拜劳工事务部门投诉，他们会保密处理。我正在帮你准备，你的信息会从 UAE PASS 自动填好。"
        )
        takeOverTarget = .openService("report-employer-problem")
        show(AssistantCard(quote: quoteText, headline: working, surface: .complaint(block), controls: .task), face: .working)
        for i in fields.indices {
            try? await Task.sleep(for: .milliseconds(650))
            guard !Task.isCancelled else { return }
            block.visibleCount = i + 1
            updateComplaint(block)
            withAnimation(DNMotion.resolve(DNMotion.smooth)) { progress = Double(i + 1) / Double(fields.count + 1) }
        }
        try? await Task.sleep(for: .milliseconds(400))
        guard !Task.isCancelled else { return }
        block.status = .yourTurn
        if var c = card {
            c.headline = AssistantText(
                "Your report is ready and stays confidential. I'll send it to Dubai's labour authority in English and Arabic.",
                zh: "投诉已经准备好了，会保密处理。我会用英文和阿拉伯文提交给迪拜劳工事务部门。"
            )
            c.surface = .complaint(block)
            withAnimation(DNMotion.resolve(DNMotion.smooth)) { card = c }
        }
        yourTurn(AssistantConfirm(
            style: .uaePass,
            title: AssistantText("Confirm with UAE PASS", zh: "用 UAE PASS 确认"),
            caption: AssistantText("It's sent in your name", zh: "投诉将以您的名义提交"),
            reason: "Send your report in your name"
        )) { [weak self] _ in
            await self?.finishComplaint(block: block, mandarin: mandarin)
        }
    }

    private func finishComplaint(block: AssistantComplaintBlock, mandarin: Bool) async {
        var block = block
        block.status = .working
        updateComplaint(block)
        set(phase: .card, face: .working)
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { progress = 0.9 }
        try? await Task.sleep(for: .milliseconds(900))
        guard !Task.isCancelled else { return }
        let ref = DemoStore.reference(prefix: "LAB")
        block.status = .done
        block.reference = ref
        store?.addRequest(RequestItem(
            id: "assistant-\(ref)", serviceID: "report-employer-problem",
            title: "Report a problem with your employer", entity: "Labour authority", reference: ref,
            status: .inReview(answerBy: DemoDate.day(5)), notice: nil,
            steps: [
                RequestStep(title: "Tell the labour authority what happened", detail: "Today · sent by your assistant in your name", state: .done),
                RequestStep(title: "The labour authority looks into it", detail: "They'll contact you within 5 working days", state: .current),
                RequestStep(title: "You get an answer", detail: "We'll notify you", state: .upcoming),
            ],
            startedOn: .now, symbol: "exclamationmark.bubble.fill", hue: .sand
        ))
        DNHaptics.success()
        // Quiet where it matters: plain words, no celebration, no rating.
        guard var c = card else { return }
        c.headline = AssistantText(
            "Sent. Your reference is \(ref). The labour authority will contact you within 5 working days.",
            zh: "已提交。参考编号是 \(ref)。劳工事务部门会在 5 个工作日内联系你。"
        )
        c.surface = .complaint(block)
        c.controls = .none
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { card = c; progress = 1 }
        note = AssistantNote(text: "Report sent", receiptID: nil)
        completed.insert(AssistantIntent.employerComplaint(mandarin: true).key)
        lastTopic = "work"
        offersEnglish = mandarin && !showsEnglish
        set(phase: .card, face: .quiet)
        announce(c.headline.resolved(english: showsEnglish))
        autoClose()
    }

    private static func translation(of quote: String) -> String {
        switch quote {
        case weiPrompt: "My wages haven't been paid for two months. What should I do?"
        case "我的公司已经两个月没发工资了，我想投诉。": "My company hasn't paid my wages for two months. I want to complain."
        default: AssistantIntent.isMandarin(quote) ? "My employer hasn't paid my wages." : quote
        }
    }

    // 8 · Anything else: never a dead end.
    private func runFallback(_ quote: String, mandarin: Bool) {
        show(AssistantCard(
            quote: AssistantText(mandarin ? "Asked in Mandarin" : quote, zh: mandarin ? quote : nil),
            headline: AssistantText("I can't do that in this demo yet. Try one of these.", zh: mandarin ? "这个演示里我还做不到。你可以试试下面这些。" : nil)
        ), face: .awake)
    }

    // MARK: Helpers

    private func show(_ c: AssistantCard, face: AssistantFaceState) {
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { card = c }
        set(phase: .card, face: face)
        announce(c.headline.resolved(english: showsEnglish))
    }

    private func update(_ task: AssistantTaskBlock) {
        guard var c = card else { return }
        c.surface = .task(task)
        withAnimation(DNMotion.resolve(DNMotion.snappy)) { card = c }
    }

    private func updateComplaint(_ block: AssistantComplaintBlock) {
        guard var c = card else { return }
        c.surface = .complaint(block)
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { card = c }
    }

    private func yourTurn(_ c: AssistantConfirm, action: @escaping (PaymentMethod) async -> Void) {
        onConfirm = action
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { confirm = c }
        set(phase: .card, face: .yourTurn)
        announce("Your turn. \(c.title.resolved(english: showsEnglish)).")
        if AssistantLaunch.autoConfirm {
            Task { [weak self] in
                try? await Task.sleep(for: .milliseconds(1500))
                self?.confirmTapped()
            }
        }
    }

    private func finish(headline: AssistantText, surface: AssistantSurface, rating: AssistantRatingPrompt?, note: AssistantNote, intent: AssistantIntent) {
        guard var c = card else { return }
        c.headline = headline
        c.surface = surface
        c.controls = .none
        c.rating = rating
        withAnimation(DNMotion.resolve(DNMotion.smooth)) {
            card = c
            progress = 1
        }
        self.note = note
        completed.insert(intent.key)
        lastTopic = intent.topic
        set(phase: .card, face: .done)
        announce(headline.resolved(english: showsEnglish))
        autoClose()
    }

    private func autoClose() {
        guard AssistantLaunch.autoClose else { return }
        Task { [weak self] in
            try? await Task.sleep(for: .milliseconds(2500))
            self?.close()
        }
    }

    private func set(phase: AssistantPhase, face: AssistantFaceState) {
        withAnimation(DNMotion.resolve(DNMotion.smooth)) {
            self.phase = phase
        }
        if self.face != face {
            withAnimation(DNMotion.resolve(face == .yourTurn || face == .done ? DNMotion.bouncy : DNMotion.smooth)) {
                self.face = face
            }
        }
    }

    private func announce(_ text: String) {
        guard !text.isEmpty else { return }
        AccessibilityNotification.Announcement(text).post()
    }

    private func cancelJob() {
        job?.cancel()
        job = nil
        isConfirming = false
    }
}

// MARK: - Store and router helpers (only read existing state)

/// What renewing a registration takes: the vehicle, the fines that block it, the RTA fee.
struct AssistantRenewalPlan {
    let vehicle: Vehicle
    let fines: [Payable]
    /// RTA renewal fee: AED 400 + AED 20 knowledge and innovation fees.
    let fee: Decimal
    var total: Decimal { fee + fines.reduce(0) { $0 + $1.amount } }
}

extension DemoStore {
    /// The vehicle whose registration is due (from Needs you, or expiring within 30 days), and its fines.
    func assistantRenewalPlan() -> AssistantRenewalPlan? {
        let due = state.needsYou.compactMap { item -> String? in
            if case .renewal(let sid, let thing) = item.kind, sid == "renew-vehicle-registration" { return thing }
            return nil
        }
        let vehicle = state.vehicles.first { due.contains($0.id) }
            ?? state.vehicles.filter { DemoDate.daysFromToday($0.registrationExpiry) <= 30 }.min { $0.registrationExpiry < $1.registrationExpiry }
        guard let vehicle else { return nil }
        let fines = payables.filter { vehicle.fineIDs.contains($0.id) }
        return AssistantRenewalPlan(vehicle: vehicle, fines: fines, fee: 420)
    }
}

extension Router {
    /// The current tab's stack (what's on screen under the assistant).
    var assistantCurrentPath: [Route] {
        switch tab {
        case .home: homePath
        case .services: servicesPath
        case .activity: activityPath
        case .search: searchPath
        }
    }
}

// MARK: - Confirming

enum AssistantAuth {
    /// Face ID when the device has it; otherwise (the Simulator) a short 0.8 s confirm.
    static func confirm(reason: String) async -> Bool {
        let context = LAContext()
        var error: NSError?
        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            do {
                return try await context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason)
            } catch {
                return false
            }
        }
        try? await Task.sleep(for: .milliseconds(800))
        return true
    }
}

// MARK: - Launch arguments (temporary, for screenshots)

/// `-assistantRun "Renew my car registration"` runs a script when the overlay opens;
/// `-assistantListen` starts hold-to-talk; `-assistantConfirm` confirms automatically;
/// `-assistantClose` closes 2.5 s after a task is done (shows the note and the updated screen);
/// `-assistantEnglish` shows Mandarin cards in English.
enum AssistantLaunch {
    private static var consumed = false
    private static var args: [String] { ProcessInfo.processInfo.arguments }

    static var autoConfirm: Bool { args.contains("-assistantConfirm") }
    static var autoClose: Bool { args.contains("-assistantClose") }
    /// `-assistantEnglish`: Mandarin cards start in English (for screenshots).
    static var forceEnglish: Bool { args.contains("-assistantEnglish") }

    static func take() -> (run: String?, listen: Bool) {
        guard !consumed else { return (nil, false) }
        consumed = true
        var run: String?
        if let i = args.firstIndex(of: "-assistantRun"), i + 1 < args.count { run = args[i + 1] }
        return (run, args.contains("-assistantListen"))
    }
}
