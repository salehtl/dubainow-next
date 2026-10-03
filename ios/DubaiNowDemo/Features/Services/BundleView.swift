import SwiftUI

/// A bundle page ("All at once"): the same job across many accounts, done in one go
/// (Paper C2 blueprints: Pay all, Family visas, Certificates).
struct BundleView: View {
    var serviceID: String

    var body: some View {
        if let service = Catalogue.shared.service(serviceID) {
            switch service.type {
            case .bundle: ServicesBundlePage(service: service)
            case .journey: ServicesJourneyPage(service: service)
            case .standalone: ServiceStartView(serviceID: serviceID)
            }
        } else {
            ServicesMissingRecord(id: serviceID)
        }
    }
}

struct ServicesBundlePage: View {
    var service: CatalogueService

    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(ToastCenter.self) private var toasts

    private var isPayAll: Bool { service.id == "pay-all" }

    /// The catalogue describes bundles for staff ("the resident…"); the page speaks to the resident.
    private static let doesTheWork: [String: String] = [
        "pay-all": "Finds everything due across your accounts and pays it in one checkout. You can untick anything you'd rather pay later.",
        "renew-family-visas": "Lists every visa you sponsor that's due, then books the medical tests, renews the Emirates IDs and checks insurance as part of one application.",
        "get-certificates": "Tick the certificates and letters you need, for a job, a loan or a visa. DubaiNow asks each entity and keeps the results in your Documents.",
        "apply-all-pod-benefits": "One application checks which benefits you're eligible for and applies for every one you qualify for.",
    ]
    private var isFamilyVisas: Bool { service.id == "renew-family-visas" }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    header
                    VStack(alignment: .leading, spacing: 14) {
                        personal
                        actions
                    }
                    .id("personal")
                    details
                }
                .padding(.horizontal, DNSpace.insetPage)
                .padding(.top, 8)
                .padding(.bottom, 32)
            }
            .servicesLaunchScroll(proxy)
        }
        .background(ServicesHueWash(color: service.hue.start))
        .navigationTitle(service.shortName)
        .navigationBarTitleDisplayMode(.inline)
        .dnPushedToolbar()
        .toolbar { ServicesRecordToolbar(service: service, store: store, router: router, toasts: toasts) }
    }

    // MARK: Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 12) {
            ServicesStackedDiscs(parts: service.resolvedParts)
            ServiceTypeBadge(type: .bundle)
            VStack(alignment: .leading, spacing: 6) {
                Text(service.name)
                    .dnFont(.title1)
                    .foregroundStyle(DNColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityAddTraits(.isHeader)
                if let line = countLine {
                    Text(line)
                        .dnFont(.headline)
                        .foregroundStyle(DNColor.textLink)
                }
                Text(service.description)
                    .dnFont(.callout)
                    .foregroundStyle(DNColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    /// "2 bills and fines, one payment" when there's a personal count, else "Covers 13 services".
    private var countLine: String? {
        if let n = store.servicesBundleCount(service), let line = service.countLine(n: n) { return line }
        return service.covers.map { "Covers \($0)" }
    }

    // MARK: Personal

    @ViewBuilder private var personal: some View {
        if isPayAll {
            if store.isGuest || store.persona == nil {
                ServicesNote(symbol: "person.crop.circle", title: "Sign in to see what you owe",
                             detail: "DubaiNow finds every bill, fine and top-up across your accounts.")
            } else if store.payables.isEmpty {
                ServicesNote(symbol: "checkmark.seal.fill", tint: DNColor.statusSuccessForeground,
                             title: "Nothing to pay", detail: "You're all paid up. We'll tell you when a new bill or fine arrives.")
            } else {
                payablesCard
            }
        } else if isFamilyVisas, let fv = store.state.familyVisas, store.persona != nil {
            familyCard(fv)
        }
    }

    private var payablesCard: some View {
        VStack(spacing: 0) {
            ForEach(store.payables) { p in
                HStack(spacing: 12) {
                    HueIcon(symbol: p.symbol, hue: p.hue, size: 40)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(p.title)
                            .dnFont(.rowTitle)
                            .foregroundStyle(DNColor.textPrimary)
                        Text([p.entity, p.due.map { DemoDate.dueLabel($0) } ?? p.note].compactMap { $0 }.joined(separator: " · "))
                            .dnFont(.footnote)
                            .foregroundStyle(DNColor.textSecondary)
                    }
                    Spacer(minLength: 8)
                    Text(Money.aed(p.amount))
                        .dnFont(.subheadline)
                        .fontWeight(.semibold)
                        .fontDesign(.rounded)
                        .foregroundStyle(DNColor.textPrimary)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .accessibilityElement(children: .combine)
                Rectangle().fill(DNColor.listRowSeparator).frame(height: 1).padding(.leading, 68)
            }
            HStack(alignment: .firstTextBaseline) {
                Text("Total")
                    .dnFont(.headline)
                    .foregroundStyle(DNColor.textPrimary)
                Spacer()
                MoneyText(amount: store.payAllTotal, size: .numericMedium)
            }
            .padding(16)
            .accessibilityElement(children: .combine)
        }
        .dnCard(radius: DNRadius.brief)
    }

    private func familyCard(_ fv: FamilyVisasStatus) -> some View {
        let members = store.state.family.filter { fv.memberIDs.contains($0.id) }
        return VStack(spacing: 0) {
            ForEach(members) { m in
                let due = fv.dueMemberIDs.contains(m.id)
                HStack(spacing: 12) {
                    Avatar(initials: m.initials, hue: m.hue, size: 40)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(m.name)
                            .dnFont(.rowTitle)
                            .foregroundStyle(DNColor.textPrimary)
                        Text(m.relation + (m.visaExpiry.map { " · visa expires \(DemoDate.short($0))" } ?? ""))
                            .dnFont(.footnote)
                            .foregroundStyle(DNColor.textSecondary)
                    }
                    Spacer(minLength: 8)
                    if due {
                        StatusChip(text: "Due", tone: .warning, onWhite: true)
                    } else {
                        Text("Not due")
                            .dnFont(.footnote)
                            .foregroundStyle(DNColor.textSecondary)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .accessibilityElement(children: .combine)
                if m.id != members.last?.id {
                    Rectangle().fill(DNColor.listRowSeparator).frame(height: 1).padding(.leading, 68)
                }
            }
        }
        .dnCard(radius: DNRadius.brief)
    }

    // MARK: Actions

    @ViewBuilder private var actions: some View {
        if isPayAll {
            if store.isGuest || store.persona == nil {
                Button("Sign in") { router.present(.profile) }
                    .buttonStyle(.dn(.primary))
            } else if !store.payables.isEmpty {
                Button("Pay \(Money.aed(store.payAllTotal))") {
                    router.present(.payment(store.payAllRequest()))
                }
                .buttonStyle(.dn(.primary))
                Text("No money moves in this demo.")
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                    .frame(maxWidth: .infinity)
            }
        } else {
            Button {
                router.present(.comingSoon(ServicesData.comingSoon(service, kind: .servicesPhase)))
            } label: {
                HStack(spacing: 8) {
                    Text("Start now")
                    Image(systemName: "arrow.right").font(.system(size: 15, weight: .bold))
                }
            }
            .buttonStyle(.dn(.primary))
        }
    }

    // MARK: Details

    @ViewBuilder private var details: some View {
        if let work = Self.doesTheWork[service.id] ?? service.doesTheWork {
            VStack(alignment: .leading, spacing: 8) {
                ServicesPageHeading(title: "What it does for you")
                Text(work)
                    .dnFont(.callout)
                    .foregroundStyle(DNColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        if let views = service.views, !views.isEmpty {
            VStack(alignment: .leading, spacing: 10) {
                ServicesPageHeading(title: "Ways to see it")
                ServicesFlowLayout {
                    ForEach(views, id: \.self) { ServicesInfoChip(symbol: "line.3.horizontal.decrease", text: $0, fill: DNColor.surfacePrimary) }
                }
            }
        }
        let parts = service.resolvedParts
        if !parts.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                ServicesSectionHeader(
                    title: "Covers",
                    subtitle: "\(ServicesText.count(parts.count, "service")). Each one still works on its own."
                )
                ServicesRowGroup(separatorInset: 68) {
                    ForEach(parts) { ServicesRow(service: $0, trailing: .chevron) }
                }
                let missing = (service.parts ?? []).count - parts.count
                if missing > 0 {
                    Text("\(ServicesText.count(missing, "more service")) will join when \(missing == 1 ? "it's" : "they're") in DubaiNow.")
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textSecondary)
                        .padding(.horizontal, 4)
                }
            }
        }
        if let entities = service.entities, !entities.isEmpty {
            VStack(alignment: .leading, spacing: 10) {
                ServicesPageHeading(title: "Who DubaiNow talks to")
                ServicesFlowLayout {
                    ForEach(entities, id: \.self) { ServicesInfoChip(symbol: "building.columns", text: $0, fill: DNColor.surfacePrimary) }
                }
            }
        }
        VStack(alignment: .leading, spacing: 14) {
            Rectangle().fill(DNColor.borderSubtle).frame(height: 1)
            Text("Run by \(ServicesData.fullName(service.owner)), with \(ServicesText.count(service.entities?.count ?? 0, "provider")).")
                .dnFont(.footnote)
                .foregroundStyle(DNColor.textSecondary)
        }
    }
}

/// A quiet message card ("Nothing to pay", "Sign in to see what you owe").
struct ServicesNote: View {
    var symbol: String
    var tint: Color = DNColor.textLink
    var title: String
    var detail: String

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            Image(systemName: symbol)
                .font(.system(size: 26, weight: .semibold))
                .foregroundStyle(tint)
                .frame(width: 32)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .dnFont(.headline)
                    .foregroundStyle(DNColor.textPrimary)
                Text(detail)
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .dnCard(radius: DNRadius.brief)
        .accessibilityElement(children: .combine)
    }
}

/// A bundle's first parts as overlapping discs with white rings (bundle page header).
struct ServicesStackedDiscs: View {
    var parts: [CatalogueService]
    var size: CGFloat = 52

    /// Up to three parts with different glyphs (Pay all has several fines).
    private var distinct: [CatalogueService] {
        var seen = Set<String>()
        var picked = parts.filter { seen.insert($0.symbol).inserted }
        if picked.count < 3 { picked += parts.filter { p in !picked.contains(p) } }
        return Array(picked.prefix(3))
    }

    var body: some View {
        HStack(spacing: -size * 0.36) {
            ForEach(Array(distinct.enumerated()), id: \.offset) { index, part in
                HueIcon(symbol: part.symbol, hue: part.hue, size: size)
                    .padding(3)
                    .background(DNColor.surfacePrimary, in: .circle)
                    .zIndex(Double(3 - index))
            }
        }
        .dnElevation()
        .accessibilityHidden(true)
    }
}
