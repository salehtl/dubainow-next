import SwiftUI

/// Profile · Your vehicle (Paper C1 `ETT-0`): plate, at a glance (registration, fines,
/// Salik, insurance), things you can do, Home widgets for this car, recent activity, details.
struct VehicleView: View {
    var vehicleID: String

    @Environment(DemoStore.self) private var store

    var body: some View {
        if let vehicle = store.state.vehicles.first(where: { $0.id == vehicleID }) {
            ProfileVehicleDetail(vehicle: vehicle)
        } else {
            ContentUnavailableView("Vehicle not found", systemImage: "car", description: Text("It isn't linked to your account any more."))
                .dnCanvasBackground()
                .profileChrome("Vehicle", large: false)
        }
    }
}

private struct ProfileVehicleDetail: View {
    var vehicle: Vehicle

    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(ToastCenter.self) private var toasts
    @Environment(\.dynamicTypeSize) private var typeSize
    @State private var comingSoon: ComingSoon?
    @State private var showsRegistrationWidget = true
    @State private var showsFinesWidget = false

    private var fines: [Payable] { store.profileFines(for: vehicle) }
    private var registrationDays: Int { DemoDate.daysFromToday(vehicle.registrationExpiry) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 32) {
                header
                atAGlance
                thingsYouCanDo
                onYourHome
                recentActivity
                details
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.top, 8)
            .padding(.bottom, 40)
        }
        .profileScrollHook()
        .background {
            ZStack(alignment: .top) {
                DNColor.surfaceCanvas
                LinearGradient(colors: [Hue.rose.start, Hue.rose.start.opacity(0)], startPoint: .top, endPoint: .bottom)
                    .frame(height: 380)
            }
            .ignoresSafeArea()
        }
        .profileChrome(vehicle.shortName, large: false)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    ShareLink(item: shareText) {
                        Label("Share vehicle details", systemImage: "square.and.arrow.up")
                    }
                    Button {
                        comingSoon = ComingSoon(
                            title: "Report wrong information",
                            detail: "Vehicle details come from RTA. Reporting a mistake comes in the next phase.",
                            owner: "RTA",
                            symbol: "exclamationmark.bubble.fill",
                            hue: .rose,
                            kind: .notInDemo
                        )
                    } label: {
                        Label("Report wrong information", systemImage: "exclamationmark.bubble")
                    }
                } label: {
                    Image(systemName: "ellipsis")
                }
                .tint(DNColor.textPrimary)
                .accessibilityLabel("More")
            }
        }
        .profileComingSoon($comingSoon)
    }

    // MARK: Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 14) {
            ProfileHeroPlate(plate: vehicle.plate)
            VStack(alignment: .leading, spacing: 4) {
                Text(vehicle.shortName)
                    .dnFont(.title1)
                    .foregroundStyle(DNColor.textPrimary)
                    .accessibilityAddTraits(.isHeader)
                Text("\(String(vehicle.year)) · \(vehicle.colour) · Private · Registered to you")
                    .dnFont(.callout)
                    .foregroundStyle(DNColor.textSecondary)
            }
        }
        .padding(.top, 8)
    }

    // MARK: At a glance

    private var atAGlance: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionTitle("At a glance")
            let columns = typeSize.isAccessibilitySize
                ? [GridItem(.flexible())]
                : [GridItem(.flexible(), spacing: DNSpace.widgetGutter), GridItem(.flexible())]
            LazyVGrid(columns: columns, spacing: DNSpace.widgetGutter) {
                registrationCard
                finesCard
                if vehicle.salikTag != nil { salikCard }
                insuranceCard
            }
        }
    }

    private var registrationCard: some View {
        ProfileGlanceCard(symbol: "calendar", hue: .rose, label: "Registration") {
            if registrationDays < 0 {
                Text("Expired").dnFont(.numericMedium).foregroundStyle(DNColor.textPrimary)
            } else if registrationDays <= 60 {
                HStack(alignment: .firstTextBaseline, spacing: 5) {
                    Text("\(registrationDays)").dnFont(.numericLarge)
                    Text(registrationDays == 1 ? "day left" : "days left").dnFont(.numericUnit)
                }
                .foregroundStyle(DNColor.textPrimary)
            } else {
                Text("Valid").dnFont(.numericMedium).foregroundStyle(DNColor.textPrimary)
            }
        } status: {
            if registrationDays < 0 {
                ProfileStatusLine(text: "Expired \(DemoDate.short(vehicle.registrationExpiry))", tone: .error)
            } else if registrationDays <= 60 {
                ProfileStatusLine(text: "Expires \(DemoDate.short(vehicle.registrationExpiry))", tone: .warning)
            } else {
                ProfileStatusLine(text: "Until \(DemoDate.short(vehicle.registrationExpiry)) \(Calendar.current.component(.year, from: vehicle.registrationExpiry))", tone: .success)
            }
        } action: {
            NavigationLink(value: Route.service("renew-vehicle-registration")) {
                Text(registrationDays <= 60 ? "Renew" : "Renew early")
            }
            .buttonStyle(.dn(registrationDays <= 60 ? .primary : .secondary, size: .small))
        }
    }

    private var finesCard: some View {
        let total = fines.reduce(Decimal(0)) { $0 + $1.amount }
        return ProfileGlanceCard(symbol: "doc.text.fill", hue: nil, label: "Fines") {
            if fines.isEmpty {
                Text("None").dnFont(.numericMedium).foregroundStyle(DNColor.textPrimary)
            } else {
                HStack(alignment: .firstTextBaseline, spacing: 5) {
                    Text("AED").dnFont(.numericUnit)
                    Text(Money.figure(total, trimWhole: true)).dnFont(.numericLarge)
                }
                .foregroundStyle(DNColor.textPrimary)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Money.spoken(total))
            }
        } status: {
            if fines.isEmpty {
                ProfileStatusLine(text: "Nothing to pay", tone: .success)
            } else {
                ProfileStatusLine(text: finesLine, tone: .error)
            }
        } action: {
            if fines.isEmpty {
                NavigationLink(value: Route.history) { Text("History") }
                    .buttonStyle(ProfileNeutralPillStyle())
            } else {
                Button("Pay") { payFines() }
                    .buttonStyle(.dn(.primary, size: .small))
            }
        }
    }

    private var finesLine: String {
        if fines.count == 1, let f = fines.first { return "1 \(f.title.lowercased())" }
        return "\(fines.count) fines"
    }

    private var salikCard: some View {
        let balance = store.state.salikBalance
        let low = balance < 50
        return ProfileGlanceCard(symbol: "road.lanes", hue: .sand, label: "Salik") {
            HStack(alignment: .firstTextBaseline, spacing: 5) {
                Text("AED").dnFont(.numericUnit)
                Text(Money.figure(balance, trimWhole: true)).dnFont(.numericLarge)
            }
            .foregroundStyle(DNColor.textPrimary)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Money.spoken(balance))
        } status: {
            ProfileStatusLine(text: low ? "Low balance" : "About \(NSDecimalNumber(decimal: balance / 4).intValue) trips", tone: low ? .warning : .neutral)
        } action: {
            Button("Top up") { router.present(.topUpSalik) }
                .buttonStyle(.dn(.secondary, size: .small))
        }
    }

    private var insuranceCard: some View {
        let days = DemoDate.daysFromToday(vehicle.insuranceExpiry)
        return ProfileGlanceCard(symbol: "checkmark.shield.fill", hue: .mint, label: "Insurance") {
            Text(days < 0 ? "Expired" : "Valid")
                .dnFont(.numericMedium)
                .foregroundStyle(DNColor.textPrimary)
        } status: {
            ProfileStatusLine(
                text: days < 0 ? "Ended \(DemoDate.short(vehicle.insuranceExpiry))" : "Until \(DemoDate.short(vehicle.insuranceExpiry)) \(Calendar.current.component(.year, from: vehicle.insuranceExpiry))",
                tone: days < 0 ? .error : (days < 30 ? .warning : .success)
            )
        } action: {
            Button("View policy") {
                comingSoon = ComingSoon(
                    title: "Your insurance policy",
                    detail: "Your policy comes from \(vehicle.insurer). Opening insurers' documents isn't part of this demo.",
                    owner: vehicle.insurer,
                    symbol: "checkmark.shield.fill",
                    hue: .mint,
                    kind: .notInDemo
                )
            }
            .buttonStyle(ProfileNeutralPillStyle())
        }
    }

    private func payFines() {
        guard !fines.isEmpty else { return }
        if fines.count == 1, let f = fines.first {
            router.present(.payment(store.paymentRequest(for: f)))
        } else {
            router.present(.payment(PaymentRequest(
                id: "pay-fines-\(vehicle.id)",
                title: "Pay \(fines.count) fines",
                lines: fines.map { PaymentLine(title: $0.title, detail: $0.entity, amount: $0.amount, symbol: $0.symbol, hue: $0.hue) },
                purpose: .payables(fines.map(\.id))
            )))
        }
    }

    // MARK: Things you can do

    private var serviceIDs: [String] {
        ["renew-vehicle-registration", "pay-traffic-fine", "find-testing-centre", "transfer-vehicle-ownership", "get-vehicle-certificate"]
    }

    private var thingsYouCanDo: some View {
        let all = Catalogue.shared.standalone(inTopic: "driving-and-transport", subtopic: "vehicles").count
        return VStack(alignment: .leading, spacing: 12) {
            sectionTitle("Things you can do")
            ProfileGroup {
                ForEach(serviceIDs, id: \.self) { id in
                    if let s = Catalogue.shared.service(id), let route = ProfileRoutes.route(forService: id) {
                        NavigationLink(value: route) {
                            ProfileRow(title: s.name)
                        }
                        .buttonStyle(ProfileRowButtonStyle())
                    }
                }
                NavigationLink(value: Route.subtopic(topic: "driving-and-transport", sub: "vehicles")) {
                    ProfileRow(title: "All \(all) vehicle services", emphasised: true, titleColor: DNColor.textLink, showsChevron: false)
                }
                .buttonStyle(ProfileRowButtonStyle())
            }
            .dnElevation()
        }
    }

    // MARK: On your Home

    private var salikWidgetBinding: Binding<Bool> {
        Binding(
            get: { store.hasWidget(.salik) },
            set: { on in
                if on {
                    store.addWidget(.salik, size: .small)
                    DNHaptics.success()
                    toasts.show("Salik added to Home")
                } else if let w = store.state.widgets.first(where: { $0.kind == .salik }) {
                    store.removeWidget(w)
                    DNHaptics.light()
                    toasts.show("Salik removed from Home", undo: { store.addWidget(.salik, size: .small) })
                }
            }
        )
    }

    private var onYourHome: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                sectionTitle("On your Home")
                Text("Widgets for this car. They update on their own.")
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textSecondary)
            }
            ProfileGroup {
                ProfileToggleRow(title: "Registration countdown", subtitle: "Small widget", isOn: $showsRegistrationWidget)
                if vehicle.salikTag != nil {
                    ProfileToggleRow(title: "Salik balance", subtitle: "Small widget · shared by both cars", isOn: salikWidgetBinding)
                }
                ProfileToggleRow(title: "Fines", subtitle: "Only shows when you have one", isOn: $showsFinesWidget)
            }
            .dnElevation()
        }
    }

    // MARK: Recent activity

    private struct ActivityItem: Identifiable {
        let id: String
        var title: String
        var detail: String
        var amount: String?
        var amountIsDue: Bool = false
        var leading: AnyView
    }

    private var activity: [ActivityItem] {
        var items: [ActivityItem] = []
        if let p = store.state.parking, p.plate == vehicle.plate {
            items.append(ActivityItem(
                id: "parking",
                title: "Parking now",
                detail: "Zone \(p.zone) · \(p.area) · ends \(DemoDate.time(p.endsAt))",
                amount: nil,
                leading: AnyView(ProfileLetterDisc(letter: "P", hue: .sky))
            ))
        }
        for f in fines {
            let place = f.detail.split(separator: "·").first.map { $0.trimmingCharacters(in: .whitespaces) } ?? f.detail
            let when = f.detail.split(separator: "·").dropFirst().first.map { $0.trimmingCharacters(in: .whitespaces) }
            items.append(ActivityItem(
                id: f.id,
                title: "\(f.title) issued",
                detail: [when, place].compactMap { $0 }.joined(separator: " · "),
                amount: Money.aed(f.amount),
                amountIsDue: true,
                leading: AnyView(
                    Circle().fill(DNColor.statusErrorSurface)
                        .overlay(Image(systemName: "doc.text.fill").font(.system(size: 14, weight: .semibold)).foregroundStyle(DNColor.statusErrorForeground))
                        .frame(width: 36, height: 36)
                )
            ))
        }
        if let inspected = vehicle.lastInspection {
            items.append(ActivityItem(
                id: "inspection",
                title: "Passed vehicle inspection",
                detail: "\(DemoDate.short(inspected)) · \(store.persona?.id == "sara" ? "Al Barsha inspection centre" : "Vehicle testing centre")",
                amount: Money.aed(170),
                leading: AnyView(HueIcon(symbol: "checkmark", hue: .mint, size: 36))
            ))
        }
        return items
    }

    private var recentActivity: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                sectionTitle("Recent activity")
                Spacer(minLength: 8)
                NavigationLink(value: Route.history) {
                    Text("See all")
                        .dnFont(.subheadline)
                        .fontWeight(.medium)
                        .foregroundStyle(DNColor.textLink)
                        .frame(minHeight: 44)
                        .contentShape(.rect)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("See all activity")
            }
            if activity.isEmpty {
                Text("Nothing yet. Payments and inspections for this car will show here.")
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textSecondary)
            } else {
                ProfileGroup(separatorInset: 64) {
                    ForEach(activity) { item in
                        HStack(spacing: 12) {
                            item.leading.accessibilityHidden(true)
                            VStack(alignment: .leading, spacing: 1) {
                                Text(item.title)
                                    .dnFont(.rowTitle)
                                    .foregroundStyle(DNColor.textPrimary)
                                Text(item.detail)
                                    .dnFont(.footnote)
                                    .foregroundStyle(DNColor.textSecondary)
                            }
                            Spacer(minLength: 8)
                            if let amount = item.amount {
                                Text(amount)
                                    .font(.system(size: 15, weight: .semibold, design: .rounded))
                                    .foregroundStyle(item.amountIsDue ? DNColor.statusErrorForeground : DNColor.textPrimary)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .frame(minHeight: 64)
                        .accessibilityElement(children: .combine)
                    }
                }
                .dnElevation()
            }
        }
    }

    // MARK: Details

    private var details: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionTitle("Details")
            ProfileFactsCard(facts: [
                ("Traffic file number", "10458823"),
                ("Chassis number", "Ending \(chassis)"),
                ("Registered since", "\(DemoDate.short(vehicle.registrationExpiry)) \(vehicle.year)"),
                ("Salik tag", vehicle.salikTag?.replacingOccurrences(of: "Tag ending", with: "Ending") ?? "None"),
                ("Information from", "RTA · updated today"),
            ])
        }
    }

    private var chassis: String {
        let seed = abs(vehicle.id.unicodeScalars.reduce(0) { $0 &* 31 &+ Int($1.value) })
        let letters = Array("ABCDEFGHJKLMNPRSTUVWXYZ")
        return "\(seed % 9 + 1)\(letters[seed % letters.count])\(String(format: "%04d", seed % 10000))"
    }

    private var shareText: String {
        "\(vehicle.name) · \(vehicle.plate.emirate.capitalized) \(vehicle.plate.code) \(vehicle.plate.number) · registration until \(DemoDate.full(vehicle.registrationExpiry))"
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(text)
            .dnFont(.section)
            .foregroundStyle(DNColor.textPrimary)
            .accessibilityAddTraits(.isHeader)
    }
}

// MARK: - Pieces

/// One "At a glance" card: header (disc + label), value, status line, a small pill button.
private struct ProfileGlanceCard<Value: View, Status: View, Action: View>: View {
    var symbol: String
    var hue: Hue?
    var label: String
    @ViewBuilder var value: Value
    @ViewBuilder var status: Status
    @ViewBuilder var action: Action

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Group {
                    if let hue {
                        HueIcon(symbol: symbol, hue: hue, size: 28)
                    } else {
                        Circle().fill(DNColor.statusErrorSurface)
                            .overlay(Image(systemName: symbol).font(.system(size: 12, weight: .semibold)).foregroundStyle(DNColor.statusErrorForeground))
                            .frame(width: 28, height: 28)
                            .accessibilityHidden(true)
                    }
                }
                Text(label)
                    .dnFont(.footnote)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                    .accessibilityAddTraits(.isHeader)
            }
            Spacer(minLength: 0)
            VStack(alignment: .leading, spacing: 2) {
                value
                status
            }
            .accessibilityElement(children: .combine)
            Spacer(minLength: 0)
            action
        }
        .padding(14)
        .frame(maxWidth: .infinity, minHeight: 176, alignment: .topLeading)
        .background(DNColor.surfaceWidget, in: .rect(cornerRadius: DNRadius.widget, style: .continuous))
        .dnElevation()
    }
}

private struct ProfileStatusLine: View {
    var text: String
    var tone: StatusChip.Tone

    var body: some View {
        Text(text)
            .dnFont(.footnote)
            .fontWeight(.semibold)
            .foregroundStyle(color)
            .lineLimit(2)
    }

    private var color: Color {
        switch tone {
        case .warning: DNColor.statusWarningForeground
        case .info: DNColor.statusInfoForeground
        case .success: DNColor.statusSuccessForeground
        case .error: DNColor.statusErrorForeground
        case .neutral: DNColor.textSecondary
        }
    }
}

/// A big Dubai plate for the vehicle header (60 pt tall). Always left to right.
struct ProfileHeroPlate: View {
    var plate: Plate

    var body: some View {
        HStack(spacing: 12) {
            VStack(spacing: 2) {
                Text(plate.emirate)
                    .font(.system(size: 11, weight: .heavy).width(.expanded))
                    .tracking(0.9)
                Text(plate.code)
                    .font(.system(size: 26, weight: .heavy).width(.expanded))
            }
            Rectangle()
                .fill(DNColor.plateRule)
                .frame(width: 1.5, height: 40)
            Text(plate.number)
                .font(.system(size: 40, weight: .heavy).width(.expanded).monospacedDigit())
                .tracking(0.8)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
        }
        .foregroundStyle(DNColor.plateInk)
        .padding(.leading, 12)
        .padding(.trailing, 16)
        .frame(height: 60)
        .background(DNColor.plateSurface, in: .rect(cornerRadius: 10, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).strokeBorder(DNColor.plateEdge, lineWidth: 2))
        .dnElevation()
        .environment(\.layoutDirection, .leftToRight)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Plate \(plate.emirate.capitalized) \(plate.code) \(plate.number)")
    }
}

/// A hue disc with a letter (the "P" for parking).
private struct ProfileLetterDisc: View {
    var letter: String
    var hue: Hue
    var body: some View {
        Circle()
            .fill(hue.gradient)
            .overlay(Text(letter).font(.system(size: 17, weight: .heavy, design: .rounded)).foregroundStyle(hue.ink))
            .frame(width: 36, height: 36)
    }
}
