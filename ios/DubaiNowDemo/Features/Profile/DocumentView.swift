import SwiftUI

/// One document: the card large, its status and what you can do with it.
struct DocumentView: View {
    var documentID: String

    @Environment(DemoStore.self) private var store

    var body: some View {
        if let doc = store.state.documents.first(where: { $0.id == documentID }) {
            ProfileDocumentDetail(document: doc)
        } else {
            ContentUnavailableView("Document not found", systemImage: "doc", description: Text("It isn't linked to your account any more."))
                .dnCanvasBackground()
                .profileChrome("Document", large: false)
        }
    }
}

private struct ProfileDocumentDetail: View {
    var document: DocumentItem

    @Environment(DemoStore.self) private var store
    @State private var comingSoon: ComingSoon?

    private struct Action: Identifiable {
        var id: String { title }
        var title: String
        var subtitle: String?
        var route: Route
    }

    private var info: (issuer: String, actions: [Action], note: String?) {
        let t = document.title.lowercased()
        func request(_ serviceID: String) -> RequestItem? { store.state.requests.first { $0.serviceID == serviceID } }

        if t.contains("emirates id") {
            return ("ICP", serviceAction("get-or-renew-emirates-id", title: "Renew your Emirates ID"), nil)
        }
        if t.contains("visa") {
            if let r = request("renew-residence-visa") {
                return ("GDRFA", [Action(title: "Track your renewal", subtitle: r.statusLine, route: .request(r.id))], nil)
            }
            return ("GDRFA", serviceAction("renew-residence-visa", title: "Renew your residence visa"), nil)
        }
        if t.contains("driving licence") {
            if let r = request("renew-driving-licence") {
                return ("RTA", [Action(title: "Track your renewal", subtitle: r.statusLine, route: .request(r.id))], nil)
            }
            return ("RTA", serviceAction("renew-driving-licence", title: "Renew your driving licence"), nil)
        }
        if t.contains("vehicle registration") {
            var actions: [Action] = []
            if let v = store.state.vehicles.first(where: { document.number.contains($0.plate.number) }) {
                actions.append(Action(title: "See your \(v.model)", subtitle: "\(v.plate.code) \(v.plate.number)", route: .vehicle(v.id)))
            }
            actions += serviceAction("renew-vehicle-registration", title: "Renew your vehicle registration")
            return ("RTA", actions, nil)
        }
        if t.contains("tenancy") {
            return ("Dubai Land Department", serviceAction("renew-tenancy-contract", title: "Renew your tenancy contract"), nil)
        }
        if t.contains("passport") {
            let country = document.number.split(separator: "·").first.map { $0.trimmingCharacters(in: .whitespaces) } ?? "Your country"
            if country == "United Arab Emirates" {
                return ("ICP", [], "Passport services in DubaiNow are still being confirmed with ICP.")
            }
            return (country, [], "Passports are renewed by your embassy or consulate, not in DubaiNow.")
        }
        if t.contains("title deed") {
            return ("Dubai Land Department", serviceAction("check-title-deed", title: "Check a title deed is genuine"), nil)
        }
        if t.contains("family book") { return ("ICP", [], nil) }
        if t.contains("trade licence") { return ("Department of Economy and Tourism", [], nil) }
        if t.contains("work permit") { return ("MOHRE", [], nil) }
        return ("The issuing entity", [], nil)
    }

    private func serviceAction(_ id: String, title: String) -> [Action] {
        guard let route = ProfileRoutes.route(forService: id) else { return [] }
        return [Action(title: title, subtitle: Catalogue.shared.service(id)?.ownerLabel, route: route)]
    }

    var body: some View {
        let info = info
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                hero

                if document.status != .valid {
                    HStack(spacing: 8) {
                        StatusChip(text: document.profileStatusText, tone: document.profileStatusTone, onWhite: true)
                        Spacer()
                    }
                }

                ProfileFactsCard(facts: facts(issuer: info.issuer))

                if !info.actions.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Things you can do")
                            .dnFont(.section)
                            .foregroundStyle(DNColor.textPrimary)
                            .accessibilityAddTraits(.isHeader)
                        ProfileGroup {
                            ForEach(info.actions) { action in
                                NavigationLink(value: action.route) {
                                    ProfileRow(title: action.title, subtitle: action.subtitle)
                                }
                                .buttonStyle(ProfileRowButtonStyle())
                            }
                        }
                    }
                }

                if let note = info.note {
                    Text(note)
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                }

                ProfileGroup {
                    ShareLink(item: shareText) {
                        ProfileRow(title: "Share a copy", showsChevron: false) {
                            ProfileRowSymbol(symbol: "square.and.arrow.up", color: DNColor.textLink)
                        }
                    }
                    .buttonStyle(ProfileRowButtonStyle())
                    Button {
                        comingSoon = ComingSoon(
                            title: "Show to verify",
                            detail: "A QR code that lets someone check this document is real comes in the next phase.",
                            owner: info.issuer,
                            symbol: "qrcode",
                            hue: document.hue,
                            kind: .notInDemo
                        )
                    } label: {
                        ProfileRow(title: "Show to verify", showsChevron: false) {
                            ProfileRowSymbol(symbol: "qrcode", color: DNColor.textLink)
                        }
                    }
                    .buttonStyle(ProfileRowButtonStyle())
                }
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.top, 8)
            .padding(.bottom, 40)
        }
        .dnCanvasBackground()
        .profileChrome(document.title, large: false)
        .profileComingSoon($comingSoon)
    }

    private var hero: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top) {
                Text(document.title)
                    .dnFont(.title2)
                    .fontWeight(.bold)
                Spacer()
                Image(systemName: document.symbol)
                    .font(.system(size: 26, weight: .medium))
                    .accessibilityHidden(true)
            }
            Spacer(minLength: 28)
            if let p = store.persona {
                Text(p.fullName.uppercased())
                    .dnFont(.footnote)
                    .fontWeight(.semibold)
                    .tracking(0.8)
                    .padding(.bottom, 4)
            }
            Text(document.number)
                .font(.system(size: 17, weight: .regular, design: .monospaced))
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(document.profileExpiryLine)
                .dnFont(.subheadline)
                .fontWeight(.semibold)
                .padding(.top, 6)
        }
        .foregroundStyle(document.hue.ink)
        .padding(20)
        .frame(maxWidth: .infinity, minHeight: 210, alignment: .topLeading)
        .background(document.hue.gradient, in: .rect(cornerRadius: DNRadius.widget, style: .continuous))
        .dnElevation()
        .accessibilityElement(children: .combine)
    }

    private func facts(issuer: String) -> [(label: String, value: String)] {
        var rows: [(label: String, value: String)] = [("Number", document.number)]
        if let expiry = document.expiry {
            let days = DemoDate.daysFromToday(expiry)
            rows.append((days < 0 ? "Expired" : "Expires", "\(DemoDate.full(expiry))"))
            if days >= 0 && days <= 120 { rows.append(("Time left", days == 1 ? "1 day" : "\(days) days")) }
        }
        rows.append(("Issued by", issuer))
        // Passports are issued abroad; DubaiNow reads the copy held with your residency.
        let source = document.title.lowercased().contains("passport") ? "ICP" : issuer
        rows.append(("Information from", "\(source) · updated today"))
        return rows
    }

    private var shareText: String {
        "\(document.title) · \(document.number) · \(document.profileExpiryLine)"
    }
}
