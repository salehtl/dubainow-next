import SwiftUI

// Simple pages pushed inside the profile sheet: Home, Payment methods, Notifications
// settings, Accessibility and display, Privacy policy and terms.

// MARK: - Home

struct ProfileHomeView: View {
    @Environment(DemoStore.self) private var store

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                if let home = store.state.home {
                    VStack(alignment: .leading, spacing: 10) {
                        HueIcon(symbol: "house.fill", hue: .sand, size: 56)
                        Text(home.address)
                            .dnFont(.title2)
                            .fontWeight(.bold)
                            .foregroundStyle(DNColor.textPrimary)
                        Text("Linked to your DEWA account\(home.ejariExpiry != nil ? " and tenancy contract" : "")")
                            .dnFont(.subheadline)
                            .foregroundStyle(DNColor.textSecondary)
                    }

                    ProfileFactsCard(facts: facts(home))

                    VStack(alignment: .leading, spacing: 12) {
                        Text("Things you can do")
                            .dnFont(.section)
                            .foregroundStyle(DNColor.textPrimary)
                            .accessibilityAddTraits(.isHeader)
                        ProfileGroup {
                            ForEach(services, id: \.id) { s in
                                if let route = ProfileRoutes.route(forService: s.id) {
                                    NavigationLink(value: route) {
                                        ProfileRow(title: s.name, subtitle: subtitle(for: s))
                                    }
                                    .buttonStyle(ProfileRowButtonStyle())
                                }
                            }
                        }
                    }
                } else {
                    ContentUnavailableView("No home linked", systemImage: "house", description: Text("Link your DEWA account or tenancy contract to see your home here."))
                }
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.vertical, 16)
        }
        .dnCanvasBackground()
        .profileChrome("Your home", large: false)
    }

    private func facts(_ home: HomeInfo) -> [(label: String, value: String)] {
        var rows: [(label: String, value: String)] = []
        if let makani = home.makani { rows.append(("Makani number", makani)) }
        rows.append(("DEWA account", home.dewaAccount))
        if let ejari = home.ejariExpiry { rows.append(("Tenancy contract", "Until \(DemoDate.full(ejari))")) }
        rows.append(("Information from", "DEWA and DLD · updated today"))
        return rows
    }

    private var services: [CatalogueService] {
        ["pay-electricity-water-bill", "renew-tenancy-contract", "move-home"].compactMap { Catalogue.shared.service($0) }
    }

    private func subtitle(for s: CatalogueService) -> String {
        if s.type == .journey, let j = store.state.journeys.first(where: { $0.journeyID == s.id }) {
            let total = s.stages?.count ?? 0
            return "Started · stage \(j.stageIndex + 1) of \(total) · \(j.headline)"
        }
        return s.ownerLabel
    }
}

// MARK: - Payment methods

struct ProfilePaymentMethodsView: View {
    @Environment(DemoStore.self) private var store
    @State private var comingSoon: ComingSoon?

    /// "Apple Pay and 1 saved card"
    static func summary(_ methods: [PaymentMethod]) -> String {
        let cards = methods.filter { if case .card = $0 { return true }; return false }.count
        let hasApplePay = methods.contains(.applePay)
        switch (hasApplePay, cards) {
        case (true, 0): return "Apple Pay"
        case (true, 1): return "Apple Pay and 1 saved card"
        case (true, let n): return "Apple Pay and \(n) saved cards"
        case (false, 0): return "Add a way to pay"
        case (false, 1): return "1 saved card"
        case (false, let n): return "\(n) saved cards"
        }
    }

    var body: some View {
        List {
            Section {
                ForEach(Array(store.state.paymentMethods.enumerated()), id: \.offset) { index, method in
                    HStack(spacing: 12) {
                        ProfileGreyDisc(symbol: symbol(method), size: 36)
                        VStack(alignment: .leading, spacing: 1) {
                            Text(method.label)
                                .dnFont(.rowTitle)
                                .foregroundStyle(DNColor.textPrimary)
                            Text(index == 0 ? "Used first" : "Saved for payments")
                                .dnFont(.footnote)
                                .foregroundStyle(DNColor.textSecondary)
                        }
                        Spacer()
                        if index == 0 {
                            Image(systemName: "checkmark")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(DNColor.actionAccent)
                                .accessibilityLabel("Used first")
                        }
                    }
                    .accessibilityElement(children: .combine)
                }
                Button {
                    comingSoon = ComingSoon(
                        title: "Add a card",
                        detail: "Adding a card comes with the payment flows in the next phase. Apple Pay and your saved card work in this demo.",
                        symbol: "creditcard.fill",
                        hue: .sky,
                        kind: .notInDemo
                    )
                } label: {
                    Label("Add a card", systemImage: "plus")
                        .dnFont(.rowTitle)
                        .foregroundStyle(DNColor.textLink)
                }
            } footer: {
                Text("You confirm every payment with Face ID. This is a demo: no money moves.")
            }
        }
        .scrollContentBackground(.hidden)
        .dnCanvasBackground()
        .profileComingSoon($comingSoon)
        .profileChrome("Payment methods", large: false)
    }

    private func symbol(_ m: PaymentMethod) -> String {
        switch m {
        case .applePay: "apple.logo"
        case .card: "creditcard.fill"
        }
    }
}

// MARK: - Notifications settings

struct ProfileNotificationSettingsView: View {
    enum Keys {
        static let due = "profile.notify.due"
        static let turn = "profile.notify.turn"
        static let results = "profile.notify.results"
        static let payments = "profile.notify.payments"
        static let news = "profile.notify.news"
        static let tips = "profile.notify.tips"
        static let quiet = "profile.notify.quiet"
    }

    @AppStorage(Keys.due) private var due = true
    @AppStorage(Keys.turn) private var turn = true
    @AppStorage(Keys.results) private var results = true
    @AppStorage(Keys.payments) private var payments = true
    @AppStorage(Keys.news) private var news = false
    @AppStorage(Keys.tips) private var tips = false
    @AppStorage(Keys.quiet) private var quiet = true
    @State private var quietFrom = Calendar.current.date(bySettingHour: 22, minute: 0, second: 0, of: .now) ?? .now
    @State private var quietTo = Calendar.current.date(bySettingHour: 7, minute: 0, second: 0, of: .now) ?? .now
    @Environment(\.openURL) private var openURL

    var body: some View {
        List {
            Section {
                Toggle("Due dates and renewals", isOn: $due)
                Toggle("When it's your turn", isOn: $turn)
                Toggle("Results and approvals", isOn: $results)
                Toggle("Payments and receipts", isOn: $payments)
            } header: {
                Text("Important")
            } footer: {
                Text("We tell you when something is due, when a request needs you, and when there's a result.")
            }

            Section {
                Toggle("City news and announcements", isOn: $news)
                Toggle("Tips for using DubaiNow", isOn: $tips)
            } header: {
                Text("Optional")
            } footer: {
                Text("Off until you turn them on. We never send advertising.")
            }

            Section {
                Toggle("Quiet hours", isOn: $quiet.animation(DNMotion.resolve(DNMotion.smooth)))
                if quiet {
                    DatePicker("From", selection: $quietFrom, displayedComponents: .hourAndMinute)
                    DatePicker("To", selection: $quietTo, displayedComponents: .hourAndMinute)
                }
            } footer: {
                Text("Nothing makes a sound in quiet hours, except a payment you're making.")
            }

            Section {
                Button("Open iPhone Settings") {
                    if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                }
                .foregroundStyle(DNColor.textLink)
            } footer: {
                Text("To turn all DubaiNow notifications off, use your iPhone's Settings.")
            }
        }
        .tint(DNColor.controlSwitchTrackOn)
        .scrollContentBackground(.hidden)
        .dnCanvasBackground()
        .profileChrome("Notifications", large: false)
    }
}

// MARK: - Accessibility and display

struct ProfileAccessibilityView: View {
    @Environment(\.dynamicTypeSize) private var typeSize
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.legibilityWeight) private var legibility
    @Environment(\.colorSchemeContrast) private var contrast
    @Environment(\.openURL) private var openURL

    var body: some View {
        List {
            Section {
                Text("DubaiNow follows your iPhone's settings. Change them in Settings and the app updates straight away.")
                    .dnFont(.body)
                    .foregroundStyle(DNColor.textPrimary)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 4, leading: 4, bottom: 4, trailing: 4))
            }

            Section("On this iPhone now") {
                LabeledContent("Text size", value: textSizeLabel)
                LabeledContent("Bold text", value: legibility == .bold ? "On" : "Off")
                LabeledContent("Reduce Motion", value: reduceMotion ? "On" : "Off")
                LabeledContent("Increase contrast", value: contrast == .increased ? "On" : "Off")
                LabeledContent("VoiceOver", value: UIAccessibility.isVoiceOverRunning ? "On" : "Off")
            }

            Section {
                LabeledContent("Appearance", value: "Light")
            } footer: {
                Text("Dark mode comes in a later version of the demo.")
            }

            Section {
                Button {
                    if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                } label: {
                    Label("Open iPhone Settings", systemImage: "gear")
                        .foregroundStyle(DNColor.textLink)
                }
            } footer: {
                Text("Text size is in Settings › Accessibility › Display & Text Size.")
            }
        }
        .scrollContentBackground(.hidden)
        .dnCanvasBackground()
        .profileChrome("Accessibility and display", large: false)
    }

    private var textSizeLabel: String {
        switch typeSize {
        case .xSmall, .small, .medium: "Smaller"
        case .large: "Default"
        case .xLarge, .xxLarge, .xxxLarge: "Larger"
        default: "Largest (accessibility)"
        }
    }
}

// MARK: - Privacy policy and terms

struct ProfilePrivacyView: View {
    private struct Part: Identifiable {
        let id = UUID()
        let heading: String
        let body: String
    }

    private let privacy: [Part] = [
        Part(heading: "What DubaiNow keeps",
             body: "Your favorites, widgets and Home layout, your activity and unfinished drafts, and the accounts you link. You choose how long we keep your activity."),
        Part(heading: "What we ask government for",
             body: "When you open something personal, DubaiNow asks the entity that holds it. For example, RTA for your vehicles and DEWA for your bills. You choose who it can ask in Linked accounts and privacy."),
        Part(heading: "Who we share it with",
             body: "Only the entity that runs the service you're using. We never sell your information or use it for marketing."),
        Part(heading: "Your choices",
             body: "You can download a copy of your information, delete your history or delete your account at any time from your profile."),
    ]

    private let terms: [Part] = [
        Part(heading: "Using DubaiNow",
             body: "Each service is provided by the entity named beside it, under that entity's own terms. DubaiNow brings them together in one place."),
        Part(heading: "Payments",
             body: "Fees, bills and fines are paid to the entity that charges them. Your receipts are kept in Activity."),
        Part(heading: "Changes",
             body: "If these terms change, we'll tell you in the app before the change applies."),
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 28) {
                Text("Last updated 2 October 2026")
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                section("Privacy policy", privacy)
                section("Terms of use", terms)
                Text("This is a demo. No real accounts, payments or government systems are connected.")
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(DNColor.surfacePrimary, in: .rect(cornerRadius: DNRadius.card, style: .continuous))
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.vertical, 16)
        }
        .dnCanvasBackground()
        .profileChrome("Privacy policy and terms", large: false)
    }

    private func section(_ title: String, _ parts: [Part]) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(title)
                .dnFont(.title2)
                .fontWeight(.bold)
                .foregroundStyle(DNColor.textPrimary)
                .accessibilityAddTraits(.isHeader)
            ForEach(parts) { part in
                VStack(alignment: .leading, spacing: 4) {
                    Text(part.heading)
                        .dnFont(.headline)
                        .foregroundStyle(DNColor.textPrimary)
                        .accessibilityAddTraits(.isHeader)
                    Text(part.body)
                        .dnFont(.body)
                        .foregroundStyle(DNColor.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
        }
    }
}
