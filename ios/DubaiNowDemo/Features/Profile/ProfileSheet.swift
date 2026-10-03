import SwiftUI

/// Pages that only exist inside the profile sheet (pushed on its own stack).
enum ProfilePage: String, Hashable {
    case home, paymentMethods, notificationSettings, accessibility, linkedAccounts, privacy, deleteAccount
}

/// Profile · Your profile (Paper C1 `EOJ-0`), opened from the avatar. A large sheet with its
/// own NavigationStack: things and documents push inside it. Guests get `X6U-0`.
struct ProfileSheet: View {
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(ToastCenter.self) private var toasts
    @State private var path = NavigationPath()
    @State private var comingSoon: ComingSoon?
    @State private var showsHappiness = false

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if store.isGuest {
                    ProfileGuestContent(comingSoon: $comingSoon, showsHappiness: $showsHappiness)
                } else {
                    ProfileSignedInContent(comingSoon: $comingSoon, showsHappiness: $showsHappiness)
                }
            }
            .dnRouteDestinations()
            .navigationDestination(for: ProfilePage.self) { page in
                ProfilePageView(page: page)
            }
        }
        .environment(\.profileIsInSheet, true)
        .toastHost(bottomInset: 24)
        .presentationDetents([.large])
        .profileComingSoon($comingSoon)
        .sheet(isPresented: $showsHappiness) {
            HappinessMeterSheet(context: nil)
                .environment(store)
                .environment(router)
                .environment(toasts)
        }
        .onAppear(perform: applyLaunchPage)
    }

    /// Demo/screenshot hooks: `-profilePage linkedAccounts` or `-profileRoute vehicle:sara-patrol`.
    private func applyLaunchPage() {
        let defaults = UserDefaults.standard
        if let raw = defaults.string(forKey: "profileRoute") {
            for part in raw.split(separator: ",") {
                if let route = Route(launchString: String(part)) { path.append(route) }
            }
        }
        if let raw = defaults.string(forKey: "profilePage"), let page = ProfilePage(rawValue: raw) {
            path.append(page)
        }
    }
}

/// Maps the sheet's own pages.
struct ProfilePageView: View {
    var page: ProfilePage

    var body: some View {
        switch page {
        case .home: ProfileHomeView()
        case .paymentMethods: ProfilePaymentMethodsView()
        case .notificationSettings: ProfileNotificationSettingsView()
        case .accessibility: ProfileAccessibilityView()
        case .linkedAccounts: ProfileLinkedAccountsView()
        case .privacy: ProfilePrivacyView()
        case .deleteAccount: ProfileDeleteAccountView()
        }
    }
}

// MARK: - Signed in

private struct ProfileSignedInContent: View {
    @Binding var comingSoon: ComingSoon?
    @Binding var showsHappiness: Bool

    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(ToastCenter.self) private var toasts
    @Environment(\.dismiss) private var dismiss
    @State private var confirmsSignOut = false
    @ScaledMetric(relativeTo: .subheadline) private var documentCardWidth: CGFloat = 210

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                identity
                    .frame(maxWidth: .infinity)
                if !store.state.documents.isEmpty { documents }
                yourThings
                ProfileSettingsSection(comingSoon: $comingSoon, showsHappiness: $showsHappiness, showsLinkedAccounts: true)
                    .padding(.horizontal, DNSpace.insetPage)
                accountActions
            }
            .padding(.top, 8)
            .padding(.bottom, 40)
        }
        .profileScrollHook()
        .dnCanvasBackground()
        .navigationTitle("Your profile")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button(role: .close) { dismiss() }.tint(DNColor.textPrimary)
            }
        }
        .confirmationDialog("Sign out of DubaiNow?", isPresented: $confirmsSignOut, titleVisibility: .visible) {
            Button("Sign out", role: .destructive) {
                store.signOut()
                toasts.show("You're signed out")
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("You can sign in again with UAE PASS at any time.")
        }
    }

    // MARK: Identity

    private var identity: some View {
        VStack(spacing: 10) {
            if let p = store.persona {
                Avatar(initials: p.initials, hue: p.avatarHue, size: 88)
                    .overlay(Circle().strokeBorder(.white, lineWidth: 4).padding(-4))
                    .shadow(color: DNColor.elevationFar, radius: 12, x: 0, y: 8)
                    .padding(.top, 4)
                Text(p.fullName)
                    .dnFont(.title2)
                    .fontWeight(.bold)
                    .foregroundStyle(DNColor.textPrimary)
                    .multilineTextAlignment(.center)
                    .accessibilityAddTraits(.isHeader)
            }
            HStack(spacing: 6) {
                Image(systemName: "checkmark.shield.fill")
                    .font(.system(size: 14, weight: .semibold))
                Text("Verified with UAE PASS")
                    .dnFont(.footnote)
                    .fontWeight(.semibold)
            }
            .foregroundStyle(DNColor.statusSuccessForeground)
            .padding(.leading, 8)
            .padding(.trailing, 12)
            .frame(minHeight: 28)
            .background(DNColor.statusSuccessSurface, in: .capsule)
            .accessibilityElement(children: .combine)
        }
        .padding(.horizontal, DNSpace.insetPage)
    }

    // MARK: Documents

    private var documents: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                ProfileSectionTitle(text: "Documents")
                Spacer(minLength: 8)
                NavigationLink(value: Route.documents) {
                    Text("See all \(store.state.documents.count)")
                        .dnFont(.subheadline)
                        .fontWeight(.medium)
                        .foregroundStyle(DNColor.textLink)
                        .frame(minHeight: 44)
                        .contentShape(.rect)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("See all \(store.state.documents.count) documents")
            }
            .padding(.horizontal, DNSpace.insetPage)

            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 12) {
                    ForEach(store.state.documents) { doc in
                        NavigationLink(value: Route.document(doc.id)) {
                            ProfileDocumentCard(document: doc, width: min(documentCardWidth, 300))
                        }
                        .buttonStyle(.dnPressable)
                    }
                }
                .scrollTargetLayout()
                .padding(.vertical, 6)
            }
            .contentMargins(.horizontal, DNSpace.insetPage, for: .scrollContent)
            .scrollTargetBehavior(.viewAligned)
            .scrollClipDisabled()
            .padding(.top, -6)
        }
    }

    // MARK: Your things

    private var vehiclesRoute: Route {
        if store.state.vehicles.count == 1, let v = store.state.vehicles.first { return .vehicle(v.id) }
        return .vehicles
    }

    private var yourThings: some View {
        VStack(alignment: .leading, spacing: 12) {
            ProfileSectionTitle(text: "Your things")
            ProfileGroup(separatorInset: 68) {
                NavigationLink(value: vehiclesRoute) {
                    ProfileRow(
                        title: "Vehicles",
                        subtitle: store.state.vehicles.isEmpty ? "Add a vehicle you own" : ProfileText.join(store.state.vehicles.map(\.shortName)),
                        badge: store.profileFineCount,
                        emphasised: true
                    ) { HueIcon(symbol: "car.fill", hue: .rose, size: 40) }
                    .accessibilityHint(store.profileFineCount > 0 ? "\(store.profileFineCount) unpaid fine" : "")
                }
                .buttonStyle(ProfileRowButtonStyle())

                if let home = store.state.home {
                    NavigationLink(value: ProfilePage.home) {
                        ProfileRow(
                            title: "Home",
                            subtitle: homeLine(home),
                            emphasised: true
                        ) { HueIcon(symbol: "house.fill", hue: .sand, size: 40) }
                    }
                    .buttonStyle(ProfileRowButtonStyle())
                }

                if !store.state.family.isEmpty {
                    NavigationLink(value: Route.family) {
                        ProfileRow(
                            title: "Family",
                            subtitle: ProfileText.join(store.state.family.map { String($0.name.split(separator: " ").first ?? "") }),
                            emphasised: true
                        ) { HueIcon(symbol: "person.2.fill", hue: .peach, size: 40) }
                    }
                    .buttonStyle(ProfileRowButtonStyle())
                }

                NavigationLink(value: ProfilePage.paymentMethods) {
                    ProfileRow(
                        title: "Payment methods",
                        subtitle: ProfilePaymentMethodsView.summary(store.state.paymentMethods),
                        emphasised: true
                    ) { ProfileGreyDisc(symbol: "creditcard") }
                }
                .buttonStyle(ProfileRowButtonStyle())
            }
        }
        .padding(.horizontal, DNSpace.insetPage)
    }

    private func homeLine(_ home: HomeInfo) -> String {
        let area = store.persona?.area ?? "Your home"
        return home.ejariExpiry != nil ? "\(area) · DEWA and Ejari linked" : "\(area) · DEWA linked"
    }

    // MARK: Sign out, delete, demo controls, footer

    private var accountActions: some View {
        VStack(spacing: 10) {
            Button {
                confirmsSignOut = true
            } label: {
                Text("Sign out")
                    .dnFont(.rowTitle)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.actionDestructive)
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .background(DNColor.surfacePrimary, in: .rect(cornerRadius: DNRadius.listGroup, style: .continuous))
                    .contentShape(.rect)
            }
            .buttonStyle(.dnPressable)

            NavigationLink(value: ProfilePage.deleteAccount) {
                Text("Delete your account")
                    .dnFont(.subheadline)
                    .fontWeight(.medium)
                    .foregroundStyle(DNColor.actionDestructive)
                    .frame(minHeight: 44)
                    .contentShape(.rect)
            }
            .buttonStyle(.plain)

            ProfileDemoControlsRow()
                .padding(.top, 6)

            ProfileFooter(text: "DubaiNow 6.0 · Signed in with UAE PASS on this iPhone")
        }
        .padding(.horizontal, DNSpace.insetPage)
    }
}

// MARK: - Settings (shared by signed in and guest)

private struct ProfileSettingsSection: View {
    @Binding var comingSoon: ComingSoon?
    @Binding var showsHappiness: Bool
    var showsLinkedAccounts: Bool

    @AppStorage(ProfileNotificationSettingsView.Keys.news) private var news = false
    @AppStorage(ProfileNotificationSettingsView.Keys.tips) private var tips = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ProfileSectionTitle(text: "Settings")
            ProfileGroup(separatorInset: 52) {
                Button {
                    comingSoon = ProfileRoutes.language
                } label: {
                    ProfileRow(title: "Language", value: "English") { ProfileRowSymbol(symbol: "globe") }
                }
                .buttonStyle(ProfileRowButtonStyle())

                NavigationLink(value: ProfilePage.notificationSettings) {
                    ProfileRow(title: "Notifications", value: news || tips ? "Everything" : "Important only") {
                        ProfileRowSymbol(symbol: "bell")
                    }
                }
                .buttonStyle(ProfileRowButtonStyle())

                NavigationLink(value: ProfilePage.accessibility) {
                    ProfileRow(title: "Accessibility and display") { ProfileRowSymbol(symbol: "accessibility") }
                }
                .buttonStyle(ProfileRowButtonStyle())

                if showsLinkedAccounts {
                    NavigationLink(value: ProfilePage.linkedAccounts) {
                        ProfileRow(title: "Linked accounts and privacy") { ProfileRowSymbol(symbol: "lock.shield") }
                    }
                    .buttonStyle(ProfileRowButtonStyle())
                }

                Button {
                    showsHappiness = true
                } label: {
                    ProfileRow(title: "Happiness Meter") { ProfileRowSymbol(symbol: "face.smiling") }
                }
                .buttonStyle(ProfileRowButtonStyle())

                Button {
                    comingSoon = ProfileRoutes.notInDemoHelp
                } label: {
                    ProfileRow(title: "Get help") { ProfileRowSymbol(symbol: "questionmark.circle") }
                }
                .buttonStyle(ProfileRowButtonStyle())

                NavigationLink(value: ProfilePage.privacy) {
                    ProfileRow(title: "Privacy policy and terms") { ProfileRowSymbol(symbol: "doc.text") }
                }
                .buttonStyle(ProfileRowButtonStyle())
            }
        }
    }
}

/// "Demo controls" — opens the demo menu (switch resident, reset, time of day).
private struct ProfileDemoControlsRow: View {
    @Environment(Router.self) private var router

    var body: some View {
        ProfileGroup {
            Button {
                DNHaptics.medium()
                router.present(.demoMenu)
            } label: {
                ProfileRow(title: "Demo controls", subtitle: "Switch resident, reset, time of day") {
                    ProfileRowSymbol(symbol: "slider.horizontal.3", color: DNColor.textSecondary)
                }
            }
            .buttonStyle(ProfileRowButtonStyle())
        }
    }
}

/// The version line. A long press also opens the demo controls.
private struct ProfileFooter: View {
    var text: String
    @Environment(Router.self) private var router

    var body: some View {
        Text(text)
            .dnFont(.caption1)
            .fontWeight(.regular)
            .foregroundStyle(DNColor.textSecondary)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity, minHeight: 44)
            .contentShape(.rect)
            .onLongPressGesture(minimumDuration: DNMotion.holdDelay) {
                DNHaptics.medium()
                router.present(.demoMenu)
            }
            .accessibilityAction(named: "Demo controls") { router.present(.demoMenu) }
    }
}

// MARK: - Guest (X6U-0)

private struct ProfileGuestContent: View {
    @Binding var comingSoon: ComingSoon?
    @Binding var showsHappiness: Bool

    @Environment(DemoStore.self) private var store
    @Environment(ToastCenter.self) private var toasts
    @Environment(\.dismiss) private var dismiss
    @State private var choosesPersona = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                VStack(spacing: 10) {
                    Circle()
                        .fill(DNColor.surfacePrimary)
                        .overlay(
                            Image(systemName: "person")
                                .font(.system(size: 34, weight: .regular))
                                .foregroundStyle(DNColor.textSecondary)
                        )
                        .frame(width: 88, height: 88)
                        .shadow(color: DNColor.elevationFar, radius: 12, x: 0, y: 8)
                        .accessibilityHidden(true)
                        .padding(.top, 4)
                    Text("You're a guest")
                        .dnFont(.title2)
                        .fontWeight(.bold)
                        .foregroundStyle(DNColor.textPrimary)
                        .accessibilityAddTraits(.isHeader)
                    Text("Sign in to see your documents, vehicles, bills and requests.")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity)

                VStack(spacing: 12) {
                    Button {
                        choosesPersona = true
                    } label: {
                        Label("Sign in with UAE PASS", systemImage: "touchid")
                    }
                    .buttonStyle(.dn(.inverse))

                    HStack(spacing: 4) {
                        Text("New to UAE PASS?")
                            .foregroundStyle(DNColor.textPrimary)
                        Button("Create an account") {
                            comingSoon = ComingSoon(
                                title: "Create a UAE PASS account",
                                detail: "UAE PASS accounts are made in the UAE PASS app. In this demo, sign in as one of the demo residents instead.",
                                symbol: "touchid",
                                hue: .sky,
                                kind: .notInDemo
                            )
                        }
                        .fontWeight(.semibold)
                        .foregroundStyle(DNColor.textLink)
                        .buttonStyle(.plain)
                    }
                    .dnFont(.subheadline)
                    .frame(minHeight: 44)
                }

                ProfileSettingsSection(comingSoon: $comingSoon, showsHappiness: $showsHappiness, showsLinkedAccounts: false)

                VStack(spacing: 10) {
                    ProfileDemoControlsRow()
                    ProfileFooter(text: "DubaiNow 6.0 · Browsing as a guest on this iPhone")
                }
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.top, 8)
            .padding(.bottom, 40)
        }
        .profileScrollHook()
        .dnCanvasBackground()
        .navigationTitle("Profile")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button(role: .close) { dismiss() }.tint(DNColor.textPrimary)
            }
        }
        .confirmationDialog("Sign in with UAE PASS", isPresented: $choosesPersona, titleVisibility: .visible) {
            ForEach(Personas.all) { p in
                Button(p.fullName) {
                    DNHaptics.success()
                    withAnimation(DNMotion.resolve(DNMotion.smooth)) { store.signIn(p) }
                    toasts.show("Signed in as \(p.firstName)")
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This is a demo. Choose a resident to sign in as.")
        }
    }
}
