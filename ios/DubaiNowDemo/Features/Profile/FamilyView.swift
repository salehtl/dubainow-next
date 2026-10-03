import SwiftUI

/// Your family: the people you sponsor and their visa expiry, with the family visas bundle
/// ("All at once") when two or more visas are due.
struct FamilyView: View {
    @Environment(DemoStore.self) private var store

    private var sponsored: [FamilyMember] { store.state.family.filter(\.sponsoredByYou) }
    private var others: [FamilyMember] { store.state.family.filter { !$0.sponsoredByYou } }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                if store.state.family.isEmpty {
                    ContentUnavailableView(
                        "No family members yet",
                        systemImage: "person.2",
                        description: Text("People you sponsor show here with their visas.")
                    )
                    .padding(.top, 40)
                } else {
                    Text(store.state.family.contains { $0.visaExpiry != nil }
                         ? "Their visas and Emirates IDs renew through you, so we keep an eye on the dates."
                         : "The people in your family book. Their documents renew through you.")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)

                    if let status = store.state.familyVisas, !status.dueMemberIDs.isEmpty {
                        ProfileFamilyVisasCallout(status: status, family: store.state.family)
                    }

                    if !sponsored.isEmpty { group("Sponsored by you", sponsored) }
                    if !others.isEmpty { group("Your household", others) }
                }

                if store.state.family.isEmpty || !sponsored.isEmpty,
                   let route = ProfileRoutes.route(forService: "sponsor-family-member") {
                    ProfileGroup {
                        NavigationLink(value: route) {
                            ProfileRow(title: "Sponsor a family member", subtitle: "GDRFA") {
                                ProfileGreyDisc(symbol: "plus")
                            }
                        }
                        .buttonStyle(ProfileRowButtonStyle())
                    }
                }
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.bottom, 40)
        }
        .dnCanvasBackground()
        .profileChrome("Your family")
    }

    private func group(_ title: String, _ members: [FamilyMember]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            ProfileOverline(text: title)
            ProfileGroup(separatorInset: 72) {
                ForEach(members) { member in
                    ProfileFamilyRow(member: member)
                }
            }
        }
    }
}

private struct ProfileFamilyRow: View {
    var member: FamilyMember

    var body: some View {
        HStack(spacing: 12) {
            Avatar(initials: member.initials, hue: member.hue, size: 44)
            VStack(alignment: .leading, spacing: 2) {
                Text(member.name)
                    .dnFont(.rowTitle)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                Text(member.age.map { "\(member.relation) · \($0)" } ?? member.relation)
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                if let expiry = member.visaExpiry {
                    visaLine(expiry)
                        .padding(.top, 2)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(minHeight: 72)
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder private func visaLine(_ expiry: Date) -> some View {
        let days = DemoDate.daysFromToday(expiry)
        HStack(spacing: 5) {
            Image(systemName: days <= 90 ? "clock.fill" : "checkmark.circle.fill")
                .font(.system(size: 11, weight: .semibold))
                .accessibilityHidden(true)
            if days < 0 {
                Text("Visa expired \(DemoDate.short(expiry))")
            } else if days <= 90 {
                Text("Visa expires in \(days) days · \(DemoDate.short(expiry))")
            } else {
                Text("Visa valid to \(DemoDate.monthYear(expiry))")
            }
        }
        .dnFont(.footnote)
        .fontWeight(.semibold)
        .foregroundStyle(days < 0 ? DNColor.statusErrorForeground : days <= 90 ? DNColor.statusWarningForeground : DNColor.statusSuccessForeground)
    }
}

/// "2 of 3 visas expire in 73 days" → the Renew your family's visas bundle.
private struct ProfileFamilyVisasCallout: View {
    var status: FamilyVisasStatus
    var family: [FamilyMember]

    private var dueNames: String {
        ProfileText.join(family.filter { status.dueMemberIDs.contains($0.id) }.map { String($0.name.split(separator: " ").first ?? "") })
    }

    var body: some View {
        let days = DemoDate.daysFromToday(status.expiresOn)
        let bundle = Catalogue.shared.service("renew-family-visas")
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                ServiceTypeBadge(type: .bundle)
                Spacer()
                if let bundle { BundleDiscs(service: bundle, size: 36) }
            }
            Text("\(status.dueMemberIDs.count) of \(status.memberIDs.count) visas expire in \(days) days")
                .dnFont(.title3)
                .foregroundStyle(DNColor.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
            Text("\(dueNames)'s visas expire on \(DemoDate.long(status.expiresOn)). Renew them in one application, with the medical tests, Emirates IDs and insurance done in one go.")
                .dnFont(.subheadline)
                .foregroundStyle(DNColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            NavigationLink(value: Route.bundle("renew-family-visas")) {
                Text("Renew together")
            }
            .buttonStyle(.dn(.primary, size: .medium))
        }
        .padding(16)
        .dnCard(radius: DNRadius.listGroup)
    }
}
