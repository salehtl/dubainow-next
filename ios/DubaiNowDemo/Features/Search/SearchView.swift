import SwiftUI

/// The search tab's page (Paper p-W-0 · 04 Search). The field itself is iOS's own: the
/// search tab morphs the tab bar into it (`.searchable` on the TabView), and its text is
/// `router.searchQuery`. This page shows what fits the query:
/// Ready (zero state) → Results as you type → Direct answer → Did you mean.
struct SearchView: View {
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(ToastCenter.self) private var toasts

    /// The query after a 150 ms pause in typing.
    @State private var query = ""
    /// Set by "Search instead for …": search this exact text without correcting it.
    @State private var exactQuery: String?
    @State private var showsAllServices = false
    @State private var choosingOneToPay = false
    @State private var recents = SearchRecents.shared

    private var trimmed: String { query.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var isReady: Bool { trimmed.count < 2 }
    private var resident: String { store.persona?.id ?? "guest" }

    var body: some View {
        ScrollViewReader { proxy in
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                if isReady {
                    readyState
                        .transition(.opacity)
                } else {
                    results(store.searchResults(for: trimmed, exact: exactQuery == trimmed))
                }
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.top, isReady ? 4 : 14)
            .padding(.bottom, 32)
            .frame(maxWidth: .infinity, alignment: .leading)
            Color.clear.frame(height: 1).id(SearchLaunch.endID)
        }
        .task { await SearchLaunch.screenshotHelpers(proxy) }
        }
        .scrollDismissesKeyboard(.immediately)
        .dnCanvasBackground()
        .navigationTitle("Search")
        .navigationBarTitleDisplayMode(.inline)
        .dnPushedToolbar()
        .onAppear(perform: applyLaunchQuery)
        .task(id: router.searchQuery) { await debounce(router.searchQuery) }
        .onSubmit(of: .search) { recents.add(router.searchQuery, for: resident) }
        .confirmationDialog("Pay one at a time", isPresented: $choosingOneToPay, titleVisibility: .visible) {
            ForEach(store.payables) { p in
                Button("\(p.title) · \(Money.aed(p.amount))") { pay(p) }
            }
        } message: {
            Text("Choose what to pay first.")
        }
    }

    // MARK: Typing

    private func debounce(_ text: String) async {
        if text.trimmingCharacters(in: .whitespacesAndNewlines).count < 2 {
            withAnimation(DNMotion.resolve(DNMotion.fast)) { query = text; exactQuery = nil }
            return
        }
        try? await Task.sleep(for: .milliseconds(150))
        guard !Task.isCancelled else { return }
        withAnimation(DNMotion.resolve(DNMotion.fast)) {
            query = text
            showsAllServices = false
            if exactQuery != text.trimmingCharacters(in: .whitespacesAndNewlines) { exactQuery = nil }
        }
        // VoiceOver hears the count after a pause in typing: "6 results".
        try? await Task.sleep(for: .milliseconds(700))
        guard !Task.isCancelled, UIAccessibility.isVoiceOverRunning else { return }
        let r = store.searchResults(for: trimmed, exact: exactQuery == trimmed)
        let message = r.isEmpty ? "No results" : (r.count == 1 ? "1 result" : "\(r.count) results")
        AccessibilityNotification.Announcement(message).post()
    }

    /// Demo and screenshots: `-searchQuery "how much do I owe"` fills the field once at launch.
    private func applyLaunchQuery() {
        guard !SearchLaunch.applied else { return }
        SearchLaunch.applied = true
        if let q = UserDefaults.standard.string(forKey: "searchQuery"), !q.isEmpty {
            router.searchQuery = q
        }
    }

    private func search(_ text: String) {
        DNHaptics.selection()
        router.searchQuery = text
    }

    // MARK: Ready (zero state)

    @ViewBuilder private var readyState: some View {
        // The page title lives in the content: iOS hides the navigation bar while the field is active.
        Text("Search")
            .dnFont(.largeTitle)
            .foregroundStyle(DNColor.textPrimary)
            .accessibilityAddTraits(.isHeader)
            .padding(.leading, 0)
            .padding(.bottom, -4)

        let recentItems = recents.items(for: resident)
        if !recentItems.isEmpty {
            SearchSection(title: "Recent", actionTitle: "Clear", action: {
                withAnimation(DNMotion.resolve(DNMotion.fast)) { recents.clear(for: resident) }
            }) {
                SearchFlowLayout(spacing: 8) {
                    ForEach(recentItems, id: \.self) { item in
                        SearchRecentChip(text: item) { search(item) }
                    }
                }
            }
        }

        SearchSection(title: store.isGuest ? "Try asking" : "Ask about your things") {
            SearchGroup(separatorInset: 50) {
                ForEach(suggestions, id: \.text) { s in
                    Button { search(s.text) } label: { SearchSuggestionRow(symbol: s.symbol, text: s.text) }
                        .buttonStyle(.plain)
                }
                if store.isGuest {
                    Button { store.signOut() } label: {
                        SearchSuggestionRow(symbol: "person.crop.circle", text: "Sign in to ask about your bills, fines and visas")
                    }
                    .buttonStyle(.plain)
                } else {
                    Button { router.openAssistant() } label: { SearchAskRow(query: nil) }
                        .buttonStyle(.plain)
                }
            }
        }

        SearchSection(title: "Popular in Dubai this week") {
            SearchGroup(separatorInset: 50) {
                ForEach(Array(popular.enumerated()), id: \.element.id) { i, s in
                    Button { open(service: s) } label: { SearchPopularRow(rank: i + 1, service: s) }
                        .buttonStyle(.plain)
                }
            }
        }
    }

    private struct Suggestion { let symbol: String; let text: String }

    /// Questions this resident can ask, from their own data (at most three).
    private var suggestions: [Suggestion] {
        if store.isGuest {
            return [
                Suggestion(symbol: "moon.stars", text: "When is the next prayer?"),
                Suggestion(symbol: "p.square", text: "Pay for parking"),
                Suggestion(symbol: "person.text.rectangle", text: "Check my visa status"),
            ]
        }
        let s = store.state
        var out: [Suggestion] = []
        if !store.payables.isEmpty { out.append(Suggestion(symbol: "list.bullet", text: "How much do I owe?")) }
        if s.requests.contains(where: { $0.serviceID.contains("visa") }) {
            out.append(Suggestion(symbol: "person.text.rectangle", text: "When does my visa expire?"))
        }
        if s.vehicles.contains(where: { DemoDate.daysFromToday($0.registrationExpiry) <= 30 }) {
            out.append(Suggestion(symbol: "car", text: "Renew my car registration"))
        }
        if s.salikBalance > 0 { out.append(Suggestion(symbol: "road.lanes", text: "What's my Salik balance?")) }
        if s.parking != nil { out.append(Suggestion(symbol: "p.square", text: "How long is left on my parking?")) }
        out.append(Suggestion(symbol: "moon.stars", text: "When is the next prayer?"))
        return Array(out.prefix(3))
    }

    /// The most used live services in the last 30 days.
    private var popular: [CatalogueService] {
        Array(Catalogue.shared.services.filter(\.isLive).sorted { $0.uniques30d > $1.uniques30d }.prefix(5))
    }

    // MARK: Results

    @ViewBuilder private func results(_ r: SearchResults) -> some View {
        let words = r.analysis.words

        if let corrected = r.analysis.corrected {
            SearchCorrectionHeader(corrected: corrected, original: trimmed) {
                exactQuery = trimmed
            }
            .transition(.opacity)
        }

        // 1 · Direct answer
        if r.needsSignIn {
            SearchSignInCard { store.signOut() }
                .transition(.opacity)
        } else if let intent = r.intent {
            VStack(alignment: .leading, spacing: 14) {
                answer(intent)
                SearchFootnote(text: store.searchAnswerSources(for: intent))
            }
            .transition(.opacity)
        }

        // 2 · Top result
        if let thing = r.topThing, let card = thing.card {
            SearchSection(title: "Top result") {
                SearchTopThingCard(card: card, words: words, open: { perform(thing.action) }, act: { perform(card.buttonAction) })
                    .id(thing.id)
                    .transition(.opacity)
            }
            .transition(.opacity)
        } else if let top = r.topService {
            let strip = strip(for: top.service)
            SearchTopServiceCard(match: top, strip: strip) {
                if let route = strip?.route { record(); router.push(route) } else { open(service: top.service) }
            }
                .id(top.id)
                .transition(.opacity)
        }

        // 3 · Services: at most 3, then See all
        if !r.services.isEmpty {
            let limit = 3
            let shown = showsAllServices ? r.services : Array(r.services.prefix(limit))
            SearchSection(
                title: r.servicesTitle,
                actionTitle: r.services.count > limit && !showsAllServices ? "See all \(r.services.count)" : nil,
                action: { withAnimation(DNMotion.resolve(DNMotion.fast)) { showsAllServices = true } }
            ) {
                SearchGroup(separatorInset: r.hasAnswer ? 16 : 64) {
                    ForEach(Array(shown.enumerated()), id: \.offset) { _, m in
                        Button { open(service: m.service) } label: {
                            serviceRow(m, words: words, showsIcon: !r.hasAnswer)
                        }
                        .buttonStyle(.plain)
                        .id(m.id)
                        .transition(.opacity)
                    }
                }
            }
            .transition(.opacity)
        }

        // 4 · Your things
        if !r.things.isEmpty {
            SearchSection(title: "Your things") {
                SearchGroup(separatorInset: 64) {
                    ForEach(Array(r.things.prefix(6).enumerated()), id: \.offset) { _, t in
                        Button { perform(t.action) } label: { SearchThingRow(thing: t, words: words) }
                            .buttonStyle(.plain)
                            .id(t.id)
                            .transition(.opacity)
                    }
                }
            }
            .transition(.opacity)
        }

        // 5 · Widgets you can add, then help and guides
        if !r.widgets.isEmpty {
            SearchSection(title: r.widgets.allSatisfy(store.hasWidget) ? "Widgets" : "Widgets you can add") {
                SearchGroup(separatorInset: 64) {
                    ForEach(r.widgets, id: \.self) { kind in
                        let onHome = store.hasWidget(kind)
                        Button { onHome ? router.select(.home) : addWidget(kind) } label: {
                            SearchWidgetRow(kind: kind, isOnHome: onHome) { addWidget(kind) }
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .transition(.opacity)
        }
        if !r.guides.isEmpty {
            SearchSection(title: "Help and guides") {
                SearchGroup(separatorInset: 64) {
                    ForEach(r.guides) { m in
                        Button { open(service: m.service) } label: { serviceRow(m, words: words, showsIcon: true) }
                            .buttonStyle(.plain)
                    }
                }
            }
            .transition(.opacity)
        }

        if r.isEmpty {
            VStack(alignment: .leading, spacing: 6) {
                Text("No results for “\(trimmed)”")
                    .dnFont(.title3)
                    .foregroundStyle(DNColor.textPrimary)
                Text("Check the spelling or try another word. You can also browse every service, or ask DubaiNow.")
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.horizontal, 4)
            .padding(.top, 8)
            .transition(.opacity)
        }

        notWhatYouNeed
    }

    private func serviceRow(_ m: SearchMatch, words: [String], showsIcon: Bool) -> some View {
        let personal = store.searchContext(for: m.service)
        let context = m.alsoCalled.map { "also called \($0)" } ?? personal?.text
        let tone: StatusChip.Tone = m.alsoCalled == nil ? (personal?.tone ?? .neutral) : .neutral
        return SearchServiceRow(service: m.service, words: words, showsIcon: showsIcon, context: context, contextTone: tone)
    }

    @ViewBuilder private var notWhatYouNeed: some View {
        SearchSection(title: "Not what you need?") {
            VStack(spacing: 10) {
                if !store.isGuest {
                    SearchGroup {
                        Button {
                            recents.add(trimmed, for: resident)
                            router.openAssistant(seed: trimmed)
                        } label: { SearchAskRow(query: trimmed) }
                            .buttonStyle(.plain)
                    }
                }
                HStack(spacing: 8) {
                    SearchCapsuleButton(title: "Browse by topic") { router.select(.services) }
                    SearchCapsuleButton(title: "A to Z") { router.push(.allServices) }
                }
            }
        }
        .padding(.top, 4)
    }

    // MARK: Answers

    @ViewBuilder private func answer(_ intent: SearchIntent) -> some View {
        let updated = DemoDate.time(store.now)
        switch intent {
        case .owe:
            SearchOweAnswer(
                payables: store.payables, updated: updated,
                payAll: { record(); router.present(.payment(store.payAllRequest())) },
                pay: { pay($0) },
                payOneAtATime: { record(); choosingOneToPay = true }
            )
        case .request(let id):
            if let r = store.request(id) {
                SearchRequestAnswer(
                    request: r,
                    document: r.serviceID.contains("visa") ? store.state.documents.first { $0.title.localizedCaseInsensitiveContains("visa") } : nil,
                    updated: updated,
                    open: { record(); router.push(.request(r.id)) }
                )
            }
        case .salikBalance:
            let balance = store.state.salikBalance
            let low = balance <= 20
            let trips = NSDecimalNumber(decimal: balance / 4).intValue
            SearchBalanceAnswer(
                title: "Your Salik balance", balance: balance,
                detail: "About \(trips) trips left\(low ? " · low" : "")", low: low,
                lastTopUp: lastTopUp(serviceID: "top-up-salik-account"), updated: updated,
                topUp: { record(); router.present(.topUpSalik) }
            )
        case .nolBalance:
            SearchBalanceAnswer(
                title: "Your nol card balance", balance: store.state.nolBalance ?? 0,
                detail: nil, low: false, lastTopUp: lastTopUp(serviceID: "top-up-nol-card"), updated: updated,
                topUp: { record(); router.open(serviceID: "top-up-nol-card") }
            )
        case .prayer(let name):
            SearchPrayerAnswer(named: name, now: store.now, widgetOnHome: store.hasWidget(.prayer)) {
                addWidget(.prayer)
            }
        case .parking:
            SearchParkingAnswer(
                session: store.state.parking, updated: updated,
                extend: { record(); router.present(.extendParking) },
                payForParking: { record(); router.open(serviceID: "pay-for-parking") }
            )
        }
    }

    private func lastTopUp(serviceID: String) -> String? {
        guard let r = store.state.history.first(where: { $0.serviceID == serviceID }), let amount = r.amount else { return nil }
        return "Last topped up \(Money.aed(amount, trimWhole: true)) on \(DemoDate.long(r.date))"
    }

    /// The resident's own context for a service card ("Your Patrol is due in 12 days").
    private func strip(for service: CatalogueService) -> SearchContextStrip? {
        guard !store.isGuest else { return nil }
        for item in store.state.needsYou where item.searchServiceID == service.id {
            if case .renewal(_, let thingID) = item.kind, let v = store.state.vehicles.first(where: { $0.id == thingID }) {
                let days = DemoDate.daysFromToday(v.registrationExpiry)
                return SearchContextStrip(plate: v.plate, text: "Your \(v.model) is due in \(days) days")
            }
            if let p = item.payable {
                let due = p.due.map { DemoDate.dueLabel($0).lowercased() } ?? "to pay"
                return SearchContextStrip(symbol: p.symbol, text: "You have \(Money.aed(p.amount)) \(due)")
            }
        }
        if let r = store.state.requests.first(where: { $0.serviceID == service.id && $0.title == service.name }) {
            let status = r.statusLine.prefix(1).lowercased() + r.statusLine.dropFirst()
            return SearchContextStrip(symbol: r.symbol, text: "You have a request · \(status)", buttonTitle: "Track your request", route: .request(r.id))
        }
        if let d = store.state.drafts.first(where: { $0.serviceID == service.id }) {
            return SearchContextStrip(symbol: "doc.badge.clock", text: "You started this · \(d.stepsDone) of \(d.stepsTotal) steps done", buttonTitle: "Continue")
        }
        return nil
    }

    // MARK: Actions

    private func record() { recents.add(trimmed, for: resident) }

    private func open(service: CatalogueService) {
        record()
        router.open(service: service)
    }

    private func pay(_ p: Payable) {
        record()
        router.present(.payment(store.paymentRequest(for: p)))
    }

    private func perform(_ action: SearchThing.Action) {
        record()
        switch action {
        case .push(let route): router.push(route)
        case .sheet(let sheet): router.present(sheet)
        case .pay(let p): router.present(.payment(store.paymentRequest(for: p)))
        case .service(let id): router.open(serviceID: id)
        }
    }

    private func addWidget(_ kind: WidgetKind) {
        guard !store.hasWidget(kind) else { return }
        store.addWidget(kind, size: SearchWidgetRow.size(kind))
        DNHaptics.success()
        toasts.show("Added \(kind.title) to Home", symbol: "checkmark.circle.fill")
    }
}

/// Launch arguments for demos and screenshots (applied once per run):
/// `-searchQuery "sal"` fills the field, `-searchHideKeyboard` lowers the keyboard,
/// `-searchScrollToEnd` scrolls to the bottom of the results.
private enum SearchLaunch {
    static var applied = false
    static let endID = "search-end"

    static func screenshotHelpers(_ proxy: ScrollViewProxy) async {
        let args = ProcessInfo.processInfo.arguments
        guard args.contains("-searchHideKeyboard") || args.contains("-searchScrollToEnd") else { return }
        try? await Task.sleep(for: .seconds(1.2))
        if args.contains("-searchHideKeyboard") {
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        }
        if args.contains("-searchScrollToEnd") {
            try? await Task.sleep(for: .seconds(0.5))
            withAnimation { proxy.scrollTo(endID, anchor: .bottom) }
        }
    }
}
