import SwiftUI

/// A service start page (Paper C1 `DT8-0`). Standalone records get the start page; if a route
/// names a bundle or journey, its own page is shown instead.
struct ServiceStartView: View {
    var serviceID: String

    var body: some View {
        if let service = Catalogue.shared.service(serviceID) {
            switch service.type {
            case .standalone: ServicesStartPage(service: service)
            case .bundle: ServicesBundlePage(service: service)
            case .journey: ServicesJourneyPage(service: service)
            }
        } else {
            ServicesMissingRecord(id: serviceID)
        }
    }
}

// MARK: - Curated content

/// Hand-written start page content (the rest of the catalogue gets the generic page until
/// service content is written in the services phase).
struct ServicesCuratedContent {
    var chips: [(symbol: String, text: String)]
    var needs: [String]
    var costs: [(label: String, amount: Decimal)]
    var takes: String
    var otherWays: [String]
    var related: [String]

    static let all: [String: ServicesCuratedContent] = [
        "renew-vehicle-registration": ServicesCuratedContent(
            chips: [("clock", "5 minutes"), ("banknote", "AED 420"), ("building.columns", "RTA")],
            needs: [
                "UAE PASS to sign in. We fill in your details from it.",
                "Valid insurance. We check it with your insurer.",
                "A passed inspection if the car is over 3 years old.",
            ],
            costs: [("Registration renewal", 400), ("Knowledge and innovation fees", 20)],
            takes: "About 5 minutes. Your digital registration card is ready straight away. A printed card arrives in 2 working days if you ask for one.",
            otherWays: ["Visit an RTA customer happiness centre"],
            related: ["transfer-vehicle-ownership", "find-testing-centre", "pay-traffic-fine"]
        ),
    ]
}

// MARK: - Start page

private struct ServicesStartPage: View {
    var service: CatalogueService

    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(ToastCenter.self) private var toasts

    @State private var vehicleID: String?
    @State private var choosesVehicle = false
    @State private var charityID: String?
    @State private var amount: Decimal = 50
    @State private var showsAllCharities = false

    private var curated: ServicesCuratedContent? { ServicesCuratedContent.all[service.id] }
    private var owed: [Payable] { store.servicesPayables(for: service.id) }
    private var requests: [RequestItem] { store.servicesRequests(for: service) }
    private var draft: DraftItem? { store.servicesDraft(for: service.id) }
    private var isRenewal: Bool { service.id == "renew-vehicle-registration" }

    private var vehicle: Vehicle? {
        guard isRenewal, store.persona != nil else { return nil }
        if let vehicleID, let v = store.state.vehicles.first(where: { $0.id == vehicleID }) { return v }
        return store.servicesVehicleForRenewal()
    }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    header
                        .padding(.horizontal, DNSpace.insetPage)
                        .padding(.top, 8)
                    VStack(alignment: .leading, spacing: 14) {
                        personal
                        actions
                    }
                    .padding(.horizontal, DNSpace.insetPage)
                    .padding(.top, 24)
                    .id("personal")
                    VStack(alignment: .leading, spacing: 26) {
                        if let curated { curatedSections(curated) } else { aboutSections }
                        footer
                    }
                    .padding(.horizontal, DNSpace.insetPage)
                    .padding(.top, 28)
                    .id("details")
                }
                .padding(.bottom, 32)
            }
            .servicesLaunchScroll(proxy)
        }
        .background(ServicesHueWash(color: service.hue.start, base: DNColor.surfacePrimary))
        .navigationTitle(service.shortName)
        .navigationBarTitleDisplayMode(.inline)
        .dnPushedToolbar()
        .toolbar { ServicesRecordToolbar(service: service, store: store, router: router, toasts: toasts) }
        .confirmationDialog("Which vehicle?", isPresented: $choosesVehicle, titleVisibility: .visible) {
            ForEach(store.state.vehicles) { v in
                Button("\(v.shortName) · \(v.plate.code) \(v.plate.number)") {
                    withAnimation(DNMotion.resolve(DNMotion.smooth)) { vehicleID = v.id }
                }
            }
        }
    }

    // MARK: Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 12) {
            ServicesHeaderDisc(symbol: service.symbol, hue: service.hue, size: 64)
            breadcrumb
            VStack(alignment: .leading, spacing: 4) {
                Text(service.name)
                    .dnFont(.title1)
                    .foregroundStyle(DNColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityAddTraits(.isHeader)
                if let also = ServicesData.alsoCalled(service) {
                    Text("Also called \(also)")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                }
            }
            ServicesFlowLayout {
                if let curated {
                    ForEach(Array(curated.chips.enumerated()), id: \.offset) { _, chip in
                        ServicesInfoChip(symbol: chip.symbol, text: chip.text)
                    }
                } else {
                    ServicesInfoChip(symbol: "building.columns", text: service.ownerLabel)
                    if let audience = service.audienceLabel {
                        ServicesInfoChip(symbol: "person.2", text: audience)
                    }
                    if let status = service.statusLabel, !hasWorkingFlow {
                        ServicesInfoChip(
                            symbol: service.status == "maintenance" ? "wrench.and.screwdriver" : "clock",
                            text: status,
                            tint: service.status == "maintenance" ? DNColor.statusWarningForeground : DNColor.statusInfoForeground,
                            fill: service.status == "maintenance" ? DNColor.statusWarningSurface : DNColor.statusInfoSurface
                        )
                    }
                }
            }
            .padding(.top, 4)
            if !service.description.isEmpty {
                Text(service.description)
                    .dnFont(.callout)
                    .foregroundStyle(DNColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 4)
            }
        }
    }

    @ViewBuilder private var breadcrumb: some View {
        if let topicID = service.topic, let topic = Catalogue.shared.topic(topicID) {
            HStack(spacing: 6) {
                Button(topic.name) { router.push(.topic(topicID)) }
                if let subID = service.subtopic, let sub = topic.subtopics.first(where: { $0.id == subID }) {
                    Image(systemName: "chevron.forward")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundStyle(DNColor.neutral400)
                        .accessibilityHidden(true)
                    Button(sub.name) { router.push(.subtopic(topic: topicID, sub: subID)) }
                }
            }
            .buttonStyle(.plain)
            .dnFont(.footnote)
            .fontWeight(.medium)
            .foregroundStyle(DNColor.textLink)
            .lineLimit(1)
            .frame(minHeight: 28)
            .padding(.top, 4)
        }
    }

    /// The donate flow and payable services work in the demo even if the record isn't live.
    private var hasWorkingFlow: Bool { !owed.isEmpty || service.options?.isEmpty == false }

    // MARK: Personal

    @ViewBuilder private var personal: some View {
        if let vehicle { vehicleCard(vehicle) }
        if !owed.isEmpty { payablesCard }
        ForEach(requests) { requestCard($0) }
        if let draft, requests.isEmpty { draftCard(draft) }
        if let options = service.options, !options.isEmpty { donation(options) }
    }

    private func vehicleCard(_ v: Vehicle) -> some View {
        let days = DemoDate.daysFromToday(v.registrationExpiry)
        let expiry: (String, Color) = days < 0
            ? ("Expired \(DemoDate.short(v.registrationExpiry))", DNColor.statusErrorForeground)
            : days <= 30 ? ("Expires \(DemoDate.short(v.registrationExpiry)) · \(ServicesText.inDays(v.registrationExpiry))", DNColor.statusWarningForeground)
            : ("Valid until \(DemoDate.full(v.registrationExpiry))", DNColor.textSecondary)
        let fines = store.payables.filter { v.fineIDs.contains($0.id) }
        let fineTotal = fines.reduce(Decimal(0)) { $0 + $1.amount }
        let age = Calendar.current.component(.year, from: .now) - v.year
        let inspected = v.lastInspection.map { DemoDate.daysFromToday($0) >= -60 } ?? false

        return VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 14) {
                NumberPlate(code: v.plate.code, number: v.plate.number, emirate: v.plate.emirate, small: true)
                    .fixedSize()
                VStack(alignment: .leading, spacing: 2) {
                    Text(v.name)
                        .dnFont(.rowTitle)
                        .fontWeight(.semibold)
                        .foregroundStyle(DNColor.textPrimary)
                    Text(expiry.0)
                        .dnFont(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(expiry.1)
                }
                .accessibilityElement(children: .combine)
                Spacer(minLength: 0)
            }
            .padding(16)
            Rectangle().fill(DNColor.borderSubtle).frame(height: 1)
            VStack(alignment: .leading, spacing: 12) {
                Text("Before you renew".uppercased())
                    .dnFont(.overline)
                    .foregroundStyle(DNColor.textSecondary)
                    .accessibilityAddTraits(.isHeader)
                if v.insuranceExpiry > v.registrationExpiry {
                    ServicesCheckRow(ok: true, title: "Insurance is valid until \(DemoDate.full(v.insuranceExpiry))")
                } else {
                    ServicesCheckRow(ok: false, title: "Insurance ends \(DemoDate.full(v.insuranceExpiry))", detail: "Renew it with \(v.insurer) first.")
                }
                if let last = v.lastInspection, inspected {
                    ServicesCheckRow(ok: true, title: "Passed inspection on \(DemoDate.long(last))")
                } else if age >= 3 {
                    ServicesCheckRow(ok: false, title: "Needs an inspection", detail: "Cars over 3 years old need one before renewing.")
                } else {
                    ServicesCheckRow(ok: true, title: "No inspection needed yet")
                }
                if fines.isEmpty {
                    ServicesCheckRow(ok: true, title: "No unpaid fines")
                } else {
                    ServicesCheckRow(
                        ok: false,
                        title: "\(ServicesText.count(fines.count, "unpaid fine")) of \(Money.aed(fineTotal, trimWhole: true))",
                        detail: "You can pay \(fines.count == 1 ? "it" : "them") as part of renewing."
                    )
                }
            }
            .padding(16)
        }
        .background(DNColor.surfaceSecondary, in: .rect(cornerRadius: DNRadius.brief, style: .continuous))
    }

    private var payablesCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(owed.enumerated()), id: \.element.id) { index, p in
                if index > 0 { Rectangle().fill(DNColor.borderSubtle).frame(height: 1).padding(.leading, 70) }
                HStack(alignment: .top, spacing: 14) {
                    HueIcon(symbol: p.symbol, hue: p.hue, size: 40)
                    VStack(alignment: .leading, spacing: 3) {
                        HStack(alignment: .firstTextBaseline, spacing: 8) {
                            Text(p.title)
                                .dnFont(.rowTitle)
                                .fontWeight(.semibold)
                                .foregroundStyle(DNColor.textPrimary)
                            Spacer(minLength: 4)
                            Text(Money.aed(p.amount))
                                .dnFont(.rowTitle)
                                .fontWeight(.semibold)
                                .fontDesign(.rounded)
                                .foregroundStyle(DNColor.textPrimary)
                                .fixedSize()
                        }
                        Text("\(p.entity) · \(p.detail)")
                            .dnFont(.footnote)
                            .foregroundStyle(DNColor.textSecondary)
                        if let due = p.due {
                            Text(DemoDate.dueLabel(due))
                                .dnFont(.footnote)
                                .fontWeight(.semibold)
                                .foregroundStyle(DNColor.statusWarningForeground)
                        } else if let note = p.note {
                            Text(note)
                                .dnFont(.footnote)
                                .fontWeight(.semibold)
                                .foregroundStyle(DNColor.statusErrorForeground)
                        }
                    }
                }
                .padding(16)
                .accessibilityElement(children: .combine)
            }
            if store.showsPayAll, store.payables.count > owed.count {
                Rectangle().fill(DNColor.borderSubtle).frame(height: 1)
                Button {
                    router.push(.bundle("pay-all"))
                } label: {
                    HStack {
                        Text("Pay with your other bills · \(Money.aed(store.payAllTotal))")
                            .dnFont(.callout)
                            .fontWeight(.medium)
                            .foregroundStyle(DNColor.textLink)
                        Spacer()
                        Image(systemName: "chevron.forward")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(DNColor.listRowChevron)
                    }
                    .padding(.horizontal, 16)
                    .frame(minHeight: 50)
                    .contentShape(.rect)
                }
                .buttonStyle(.plain)
            }
        }
        .background(DNColor.surfaceSecondary, in: .rect(cornerRadius: DNRadius.brief, style: .continuous))
    }

    private func requestCard(_ r: RequestItem) -> some View {
        let current = r.currentStepIndex ?? r.steps.count
        return VStack(alignment: .leading, spacing: 10) {
            Text("In progress".uppercased())
                .dnFont(.overline)
                .foregroundStyle(DNColor.textSecondary)
            Text(r.title)
                .dnFont(.rowTitle)
                .fontWeight(.semibold)
                .foregroundStyle(DNColor.textPrimary)
            Text(r.statusLine)
                .dnFont(.subheadline)
                .fontWeight(.semibold)
                .foregroundStyle(ServicesTone.color(r.servicesTone))
            SegmentedProgress(total: r.steps.count, done: current, height: 6)
            if let i = r.currentStepIndex {
                Text("Step \(i + 1) of \(r.steps.count): \(r.steps[i].title)")
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DNColor.surfaceSecondary, in: .rect(cornerRadius: DNRadius.brief, style: .continuous))
        .accessibilityElement(children: .combine)
    }

    private func draftCard(_ d: DraftItem) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("You have a draft".uppercased())
                .dnFont(.overline)
                .foregroundStyle(DNColor.textSecondary)
            Text("\(d.stepsDone) of \(d.stepsTotal) steps done · saved \(ServicesText.relativeInSentence(d.savedOn))")
                .dnFont(.rowTitle)
                .foregroundStyle(DNColor.textPrimary)
            SegmentedProgress(total: d.stepsTotal, done: d.stepsDone, height: 6)
            Text("Drafts are kept for 30 days.")
                .dnFont(.footnote)
                .foregroundStyle(DNColor.textSecondary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DNColor.surfaceSecondary, in: .rect(cornerRadius: DNRadius.brief, style: .continuous))
        .accessibilityElement(children: .combine)
    }

    // MARK: Donation (services with options)

    private static let amounts: [Decimal] = [10, 50, 100, 500]

    private func donation(_ options: [CatalogueService.Option]) -> some View {
        let chosen = charityID ?? options.first?.id
        let visible = showsAllCharities ? options : Array(options.prefix(5))
        return VStack(alignment: .leading, spacing: 12) {
            ServicesPageHeading(title: "Choose a charity")
            ServicesRowGroup {
                ForEach(visible) { option in
                    let selected = option.id == chosen
                    Button {
                        DNHaptics.selection()
                        charityID = option.id
                    } label: {
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(option.name)
                                    .dnFont(.rowTitle)
                                    .foregroundStyle(DNColor.listRowTitle)
                                if let d = option.description {
                                    Text(d)
                                        .dnFont(.footnote)
                                        .foregroundStyle(DNColor.listRowSupporting)
                                        .multilineTextAlignment(.leading)
                                }
                            }
                            Spacer(minLength: 8)
                            Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                                .font(.system(size: 22))
                                .foregroundStyle(selected ? DNColor.actionAccent : DNColor.neutral300)
                                .accessibilityHidden(true)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .frame(minHeight: 60)
                        .contentShape(.rect)
                    }
                    .buttonStyle(ServicesRowButtonStyle())
                    .accessibilityAddTraits(selected ? .isSelected : [])
                }
            }
            .overlay(RoundedRectangle(cornerRadius: DNRadius.listGroup, style: .continuous).strokeBorder(DNColor.borderSubtle))
            if options.count > 5 {
                DNTextButton(title: showsAllCharities ? "Show fewer" : "Show all \(options.count) charities") {
                    withAnimation(DNMotion.resolve(DNMotion.smooth)) { showsAllCharities.toggle() }
                }
            }
            ServicesPageHeading(title: "How much")
                .padding(.top, 6)
            Picker("Amount", selection: $amount) {
                ForEach(Self.amounts, id: \.self) { a in
                    Text(Money.aed(a, trimWhole: true)).tag(a)
                }
            }
            .pickerStyle(.segmented)
            .padding(.bottom, 6)
        }
    }

    // MARK: Actions

    private enum Primary {
        case pay(PaymentRequest, Decimal)
        case renew
        case donate(CatalogueService.Option)
        case request(RequestItem)
        case draft(DraftItem)
        case open
        case start
    }

    private var primary: Primary {
        if let request = store.servicesPaymentRequest(for: owed) { return .pay(request, request.total) }
        if isRenewal { return .renew }
        if let options = service.options, let first = options.first {
            return .donate(options.first { $0.id == charityID } ?? first)
        }
        if let r = requests.first { return .request(r) }
        if let draft { return .draft(draft) }
        if service.kind != "service" { return .open }
        return .start
    }

    @ViewBuilder private var actions: some View {
        let p = primary
        Button {
            perform(p)
        } label: {
            HStack(spacing: 8) {
                Text(primaryTitle(p))
                if case .start = p { Image(systemName: "arrow.right").font(.system(size: 15, weight: .bold)) }
                if case .renew = p { Image(systemName: "arrow.right").font(.system(size: 15, weight: .bold)) }
            }
        }
        .buttonStyle(.dn(.primary))

        if isRenewal, !store.isGuest {
            Button {
                router.openAssistant(seed: "Renew my car registration")
            } label: {
                Label("Ask DubaiNow to do it", systemImage: "sparkles")
            }
            .buttonStyle(.dn(.secondary))
            if store.state.vehicles.count > 1 {
                HStack {
                    Spacer()
                    DNTextButton(title: "Renew a different vehicle") { choosesVehicle = true }
                    Spacer()
                }
            }
        }
        if let note = statusNote {
            Text(note)
                .dnFont(.footnote)
                .foregroundStyle(DNColor.textSecondary)
                .frame(maxWidth: .infinity)
                .multilineTextAlignment(.center)
        }
    }

    private var statusNote: String? {
        guard !hasWorkingFlow else { return nil }
        switch service.status {
        case "maintenance": return "This service is under maintenance. You can still read about it here."
        case "to publish", "proposed", "to confirm": return "Not available yet. It will open from here when it's ready."
        default: return nil
        }
    }

    private func primaryTitle(_ p: Primary) -> String {
        switch p {
        case .pay(_, let total): "Pay \(Money.aed(total))"
        case .renew: "Start now"
        case .donate: "Donate \(Money.aed(amount, trimWhole: true))"
        case .request(let r): if case .yourTurn = r.status { "Continue your request" } else { "View your request" }
        case .draft: "Continue your draft"
        case .open: "Open"
        case .start: "Start now"
        }
    }

    private func perform(_ p: Primary) {
        switch p {
        case .pay(let request, _):
            router.present(.payment(request))
        case .renew:
            router.present(.comingSoon(ServicesData.comingSoon(service, kind: .servicesPhase,
                detail: "Renewing a registration in DubaiNow comes in the next phase. You can already ask DubaiNow to do it for you.")))
        case .donate(let option):
            router.present(.payment(PaymentRequest(
                id: "donate-\(option.id)-\(Money.figure(amount))",
                title: "Donate to \(option.name)",
                lines: [PaymentLine(title: option.name, detail: option.owner ?? "Registered charity", amount: amount, symbol: "heart.fill", hue: .lilac)],
                purpose: .donation(charity: option.name, amount: amount)
            )))
        case .request(let r):
            router.push(.request(r.id))
        case .draft(let d):
            router.present(.comingSoon(ServicesData.comingSoon(service, kind: .servicesPhase,
                detail: "Your draft is saved with \(d.stepsDone) of \(d.stepsTotal) steps done. The steps inside this service are designed next, and your draft will open from here.")))
        case .open:
            router.present(.comingSoon(ServicesData.comingSoon(service, kind: .notInDemo)))
        case .start:
            router.present(.comingSoon(ServicesData.comingSoon(service)))
        }
    }

    // MARK: Details

    @ViewBuilder private func curatedSections(_ c: ServicesCuratedContent) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            ServicesPageHeading(title: "What you need")
            ForEach(c.needs, id: \.self) { need in
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text("•").accessibilityHidden(true)
                    Text(need).fixedSize(horizontal: false, vertical: true)
                }
                .dnFont(.callout)
                .foregroundStyle(DNColor.textPrimary)
            }
        }
        VStack(alignment: .leading, spacing: 10) {
            ServicesPageHeading(title: "How much it costs")
            ServicesFactTable(rows: c.costs.map { ($0.label, Money.aed($0.amount, trimWhole: true)) },
                              total: ("Total", Money.aed(c.costs.reduce(0) { $0 + $1.amount }, trimWhole: true)))
        }
        VStack(alignment: .leading, spacing: 8) {
            ServicesPageHeading(title: "How long it takes")
            Text(c.takes)
                .dnFont(.callout)
                .foregroundStyle(DNColor.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        VStack(alignment: .leading, spacing: 4) {
            ServicesPageHeading(title: "Other ways to do this")
            ForEach(c.otherWays, id: \.self) { way in
                ServicesLink(title: way) {
                    router.present(.comingSoon(ComingSoon(
                        title: way, detail: "Centre locations and opening hours aren't part of this demo.",
                        owner: service.owner, symbol: "mappin.and.ellipse", hue: service.hue, kind: .notInDemo
                    )))
                }
            }
        }
        relatedSection(c.related.compactMap { Catalogue.shared.service($0) })
    }

    @ViewBuilder private var aboutSections: some View {
        VStack(alignment: .leading, spacing: 10) {
            ServicesPageHeading(title: "About this service")
            ServicesFactTable(rows: [
                ("Run by", service.owner ?? "To be confirmed"),
                ("Who it's for", service.audienceLabel.map { $0.replacingOccurrences(of: "For ", with: "").servicesCapitalizedFirst } ?? "Everyone"),
            ] + (service.statusLabel.map { [("Status", $0)] } ?? []))
        }
        let partOf = ServicesData.partOf(service)
        if !partOf.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                ServicesPageHeading(title: "Also done as part of")
                ServicesRowGroup(separatorInset: 68) {
                    ForEach(partOf) { ServicesRow(service: $0, trailing: .chevron, showsContext: false) }
                }
            }
        }
        relatedSection(ServicesData.related(to: service))
    }

    @ViewBuilder private func relatedSection(_ related: [CatalogueService]) -> some View {
        if !related.isEmpty {
            VStack(alignment: .leading, spacing: 4) {
                ServicesPageHeading(title: "Related")
                ForEach(related) { s in
                    ServicesLink(title: s.name) { router.open(service: s) }
                }
            }
        }
    }

    private var footer: some View {
        VStack(alignment: .leading, spacing: 14) {
            Rectangle().fill(DNColor.borderSubtle).frame(height: 1)
            Text(service.owner == nil ? "The owner of this service is being confirmed." : "Run by \(ServicesData.fullName(service.owner)).")
                .dnFont(.footnote)
                .foregroundStyle(DNColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.top, 4)
    }
}

// MARK: - Pieces

/// A ✓ or ! line in "Before you renew".
struct ServicesCheckRow: View {
    var ok: Bool
    var title: String
    var detail: String? = nil

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            ZStack {
                Circle().fill(ok ? DNColor.statusSuccessSurface : DNColor.statusWarningSurface)
                Image(systemName: ok ? "checkmark" : "exclamationmark")
                    .font(.system(size: 11, weight: .heavy))
                    .foregroundStyle(ok ? DNColor.statusSuccessForeground : DNColor.statusWarningForeground)
            }
            .frame(width: 22, height: 22)
            .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textPrimary)
                if let detail {
                    Text(detail)
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textSecondary)
                }
            }
            .fixedSize(horizontal: false, vertical: true)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(ok ? "Done" : "Needs attention"): \(title). \(detail ?? "")")
    }
}

/// Label/value rows on a grey card ("How much it costs", "About this service").
struct ServicesFactTable: View {
    var rows: [(String, String)]
    var total: (String, String)? = nil

    var body: some View {
        VStack(spacing: 0) {
            ForEach(Array(rows.enumerated()), id: \.offset) { index, row in
                if index > 0 { Rectangle().fill(DNColor.borderSubtle).frame(height: 1) }
                HStack(alignment: .firstTextBaseline, spacing: 12) {
                    Text(row.0)
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                    Spacer(minLength: 8)
                    Text(row.1)
                        .dnFont(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(DNColor.textPrimary)
                        .multilineTextAlignment(.trailing)
                }
                .padding(.vertical, 13)
                .accessibilityElement(children: .combine)
            }
            if let total {
                Rectangle().fill(DNColor.borderSubtle).frame(height: 1)
                HStack {
                    Text(total.0).dnFont(.subheadline).fontWeight(.bold)
                    Spacer()
                    Text(total.1).dnFont(.headline).fontWeight(.bold)
                }
                .foregroundStyle(DNColor.textPrimary)
                .padding(.vertical, 14)
                .accessibilityElement(children: .combine)
            }
        }
        .padding(.horizontal, 16)
        .background(DNColor.surfaceSecondary, in: .rect(cornerRadius: 18, style: .continuous))
    }
}

extension RequestItem {
    var servicesTone: StatusChip.Tone {
        switch status {
        case .yourTurn: .warning
        case .inReview: .info
        case .approved, .done: .success
        }
    }
}

extension String {
    /// "people of determination" → "People of determination"
    var servicesCapitalizedFirst: String { prefix(1).uppercased() + dropFirst() }
}
