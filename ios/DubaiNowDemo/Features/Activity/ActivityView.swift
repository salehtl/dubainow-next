import SwiftUI

/// The Activity tab (Paper E8D-0): filter chips, then Needs you (the same items and numbers as
/// Home), In progress with government, Drafts (kept 30 days) and History by month.
/// Guests get the sign-in sheet (Paper WZQ-0).
struct ActivityView: View {
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @State private var filter: ActivityFilter = ActivityFilter.launchValue ?? .all
    @State private var scan: ActivityCaptureTarget?
    @State private var upload: ActivityCaptureTarget?
    @State private var draftToDelete: DraftItem?
    @State private var showsGuestSheet = false

    var body: some View {
        Group {
            if store.isGuest {
                guestState
            } else {
                activityList
            }
        }
        .navigationTitle("Activity")
        .navigationBarTitleDisplayMode(.large)
        .dnMainToolbar()
        .activityPassportCapture(scan: $scan, upload: $upload)
        .activityDraftDeletion($draftToDelete)
    }

    // MARK: List

    private var counts: [ActivityFilter: Int] {
        [
            .needsYou: store.state.needsYou.count,
            .inProgress: store.activityInProgress.count,
            .drafts: store.state.drafts.count,
        ]
    }

    private var activityList: some View {
        ScrollViewReader { proxy in
        List {
            Section {
                ActivityFilterChips(selection: $filter, counts: counts)
                    .padding(.horizontal, -DNSpace.insetPage)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
            }

            switch filter {
            case .all:
                needsYouSection
                inProgressSection
                ActivityDraftsSections(expanded: false, onDelete: { draftToDelete = $0 })
                ActivityHistorySections(receipts: store.state.history, showsTitle: true, limit: 5) {
                    withAnimation(DNMotion.resolve(DNMotion.snappy)) { filter = .history }
                }
                if isEverythingEmpty {
                    emptySection(symbol: "clock.arrow.circlepath", title: "Nothing here yet", message: "Things you need to do, requests with government, drafts and receipts all show up here.")
                }
            case .needsYou:
                if store.state.needsYou.isEmpty {
                    emptySection(symbol: "checkmark.circle", title: "You're all caught up", message: "When a bill, fine, renewal or request needs you, it shows up here and on Home.")
                } else {
                    needsYouSection
                }
            case .inProgress:
                if store.activityInProgress.isEmpty {
                    emptySection(symbol: "building.columns", title: "Nothing in progress", message: "Requests you send to government show here until they're done.")
                } else {
                    inProgressSection
                }
            case .drafts:
                if store.state.drafts.isEmpty {
                    emptySection(symbol: "doc.badge.clock", title: "No drafts", message: "If you leave a service before the end, we keep your answers here for 30 days.")
                } else {
                    ActivityDraftsSections(expanded: true, onDelete: { draftToDelete = $0 })
                }
            case .history:
                if store.state.history.isEmpty {
                    emptySection(symbol: "clock.arrow.circlepath", title: "Nothing here yet", message: "When you pay or send something, the receipt is kept here.")
                } else {
                    ActivityHistorySections(receipts: store.state.history, showsTitle: true)
                }
            }
        }
        .listStyle(.insetGrouped)
        .listSectionSpacing(20)
        .scrollContentBackground(.hidden)
        .dnCanvasBackground()
        .animation(DNMotion.resolve(DNMotion.smooth), value: filter)
        .task {
            // `-activityScroll needsYou|bottom` (screenshots and demos).
            guard let target = UserDefaults.standard.string(forKey: "activityScroll") else { return }
            try? await Task.sleep(for: .milliseconds(600))
            switch target {
            case "needsYou":
                proxy.scrollTo("needsYou", anchor: .bottom)
            default:
                if let last = store.state.history.sorted(by: { $0.date > $1.date }).prefix(5).last {
                    proxy.scrollTo("rec-\(last.id)", anchor: .bottom)
                }
            }
        }
        }
    }

    private var isEverythingEmpty: Bool {
        store.state.needsYou.isEmpty && store.activityInProgress.isEmpty && store.state.drafts.isEmpty && store.state.history.isEmpty
    }

    private func emptySection(symbol: String, title: String, message: String) -> some View {
        Section {
            ActivityEmptyState(symbol: symbol, title: title, message: message)
                .listRowBackground(Color.clear)
        }
    }

    // MARK: Needs you

    @ViewBuilder private var needsYouSection: some View {
        if !store.state.needsYou.isEmpty {
            Section {
                VStack(spacing: 12) {
                    ForEach(store.state.needsYou) { item in
                        ActivityNeedsYouCard(
                            item: item,
                            onPrimary: { primary(item) },
                            onSecondary: { secondary(item) }
                        )
                        .transition(.opacity.combined(with: .scale(scale: 0.96)))
                    }
                }
                .padding(.bottom, 8)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
                .id("needsYou")
            } header: {
                ActivitySectionHeader(title: "Needs you") {
                    if store.showsPayAll {
                        let request = store.payAllRequest()
                        DNTextButton(title: "\(request.title) · \(Money.aed(store.payAllTotal))") {
                            router.present(.payment(request))
                        }
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                    }
                }
                .activitySectionHeader()
            }
        }
    }

    private func primary(_ item: NeedsYouItem) {
        switch item.kind {
        case .payment(let payable):
            router.present(.payment(store.paymentRequest(for: payable)))
        case .requestStep(let requestID):
            if store.request(requestID)?.activityCurrentStep?.activityKind == .pay, let r = store.request(requestID) {
                router.present(.payment(r.activityFeesRequest))
            } else {
                scan = ActivityCaptureTarget(id: requestID)
            }
        case .renewal(let serviceID, _):
            router.open(serviceID: serviceID)
        case .journeyStep(let journeyID):
            router.push(.journey(journeyID))
        }
    }

    private func secondary(_ item: NeedsYouItem) {
        if item.secondaryAction == "Later" {
            store.later(item)
            return
        }
        switch item.kind {
        case .payment(let payable):
            if let vehicle = store.state.vehicles.first(where: { $0.fineIDs.contains(payable.id) }) {
                router.push(.vehicle(vehicle.id))
            } else if let serviceID = payable.serviceID {
                router.open(serviceID: serviceID)
            }
        case .requestStep(let requestID):
            router.push(.request(requestID))
        case .renewal(let serviceID, let thingID):
            if let thingID, store.state.vehicles.contains(where: { $0.id == thingID }) {
                router.push(.vehicle(thingID))
            } else {
                router.open(serviceID: serviceID)
            }
        case .journeyStep(let journeyID):
            router.push(.journey(journeyID))
        }
    }

    // MARK: In progress

    @ViewBuilder private var inProgressSection: some View {
        let requests = store.activityInProgress
        if !requests.isEmpty {
            Section {
                ForEach(requests) { request in
                    NavigationLink(value: Route.request(request.id)) {
                        ActivityRequestRow(request: request)
                    }
                }
            } header: {
                ActivitySectionHeader(title: "In progress") {
                    Text("with government")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                }
                .activitySectionHeader()
            }
        }
    }

    // MARK: Guest

    private var guestState: some View {
        ScrollView {
            VStack(spacing: 20) {
                ActivityGuestTile()
                VStack(spacing: 6) {
                    Text("Your activity is private")
                        .dnFont(.title2)
                        .foregroundStyle(DNColor.textPrimary)
                        .multilineTextAlignment(.center)
                    Text("Sign in to track your requests with government, pay what's due and find every receipt.")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)
                }
                ActivityUAEPassButton { store.signOut() }
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.top, 48)
        }
        .dnCanvasBackground()
        .onAppear { showsGuestSheet = true }
        .sheet(isPresented: $showsGuestSheet) {
            ActivityGuestSheet(
                onSignIn: {
                    showsGuestSheet = false
                    Task { @MainActor in
                        try? await Task.sleep(for: .milliseconds(350))
                        store.signOut()
                    }
                },
                onNotNow: {
                    showsGuestSheet = false
                    router.select(.home)
                }
            )
        }
    }
}
