import SwiftUI

/// Linked accounts and privacy (Paper C1 `F0U-0`): who DubaiNow can ask, one switch per
/// entity, and "Your information" (keep activity, download a copy, delete history).
struct ProfileLinkedAccountsView: View {
    @Environment(DemoStore.self) private var store
    @Environment(ToastCenter.self) private var toasts
    @AppStorage("profile.keepActivity") private var keepActivity = "2 years"
    @State private var comingSoon: ComingSoon?
    @State private var asksDownload = false
    @State private var confirmsDeleteHistory = false

    private let keepOptions = ["3 months", "1 year", "2 years", "5 years"]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("DubaiNow shows your things by asking government for them. You choose who it can ask. We never share your information for marketing.")
                    .dnFont(.body)
                    .foregroundStyle(DNColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)

                VStack(alignment: .leading, spacing: 8) {
                    ProfileOverline(text: "Signed in with")
                    ProfileGroup {
                        HStack(spacing: 12) {
                            RoundedRectangle(cornerRadius: 10, style: .continuous)
                                .fill(DNColor.surfaceInverse)
                                .overlay(
                                    Image(systemName: "touchid")
                                        .font(.system(size: 20, weight: .regular))
                                        .foregroundStyle(DNColor.textOnInverse)
                                )
                                .frame(width: 40, height: 40)
                                .accessibilityHidden(true)
                            VStack(alignment: .leading, spacing: 1) {
                                Text("UAE PASS")
                                    .dnFont(.rowTitle)
                                    .fontWeight(.semibold)
                                    .foregroundStyle(DNColor.textPrimary)
                                Text("Verified · Face ID on this iPhone")
                                    .dnFont(.footnote)
                                    .foregroundStyle(DNColor.textSecondary)
                            }
                            .accessibilityElement(children: .combine)
                            Spacer(minLength: 8)
                            Button("Manage") {
                                comingSoon = ComingSoon(
                                    title: "Manage UAE PASS",
                                    detail: "Your UAE PASS account is managed in the UAE PASS app. It isn't connected in this demo.",
                                    symbol: "touchid",
                                    hue: .sky,
                                    kind: .notInDemo
                                )
                            }
                            .dnFont(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundStyle(DNColor.textLink)
                            .frame(minWidth: 44, minHeight: 44)
                            .buttonStyle(.plain)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .frame(minHeight: 68)
                    }
                }

                if !store.state.linkedAccounts.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        ProfileOverline(text: "Who DubaiNow can ask")
                        ProfileGroup(separatorInset: 68) {
                            ForEach(store.state.linkedAccounts) { account in
                                ProfileLinkedAccountRow(account: account, isOn: binding(for: account))
                            }
                        }
                        Text("Turning one off hides its widgets and reminders. It does not cancel anything you have with them.")
                            .dnFont(.footnote)
                            .foregroundStyle(DNColor.textSecondary)
                            .padding(.horizontal, 4)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }

                VStack(alignment: .leading, spacing: 8) {
                    ProfileOverline(text: "Your information")
                    ProfileGroup {
                        HStack(spacing: 12) {
                            Text("Keep activity for")
                                .dnFont(.rowTitle)
                                .foregroundStyle(DNColor.textPrimary)
                            Spacer(minLength: 8)
                            Picker("Keep activity for", selection: $keepActivity) {
                                ForEach(keepOptions, id: \.self) { Text($0).tag($0) }
                            }
                            .pickerStyle(.menu)
                            .labelsHidden()
                            .tint(DNColor.textSecondary)
                        }
                        .padding(.leading, 16)
                        .padding(.trailing, 6)
                        .frame(minHeight: 52)

                        Button {
                            asksDownload = true
                        } label: {
                            ProfileRow(title: "Download a copy of your information")
                        }
                        .buttonStyle(ProfileRowButtonStyle())

                        Button {
                            confirmsDeleteHistory = true
                        } label: {
                            ProfileRow(title: "Delete your DubaiNow history", titleColor: DNColor.actionDestructive, showsChevron: false)
                        }
                        .buttonStyle(ProfileRowButtonStyle())
                    }
                }
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.top, 8)
            .padding(.bottom, 40)
        }
        .profileScrollHook()
        .dnCanvasBackground()
        .profileChrome("Linked accounts and privacy", large: false)
        .profileComingSoon($comingSoon)
        .profileDownloadCopyAlert(isPresented: $asksDownload)
        .confirmationDialog("Delete your DubaiNow history?", isPresented: $confirmsDeleteHistory, titleVisibility: .visible) {
            Button("Delete history", role: .destructive) {
                withAnimation(DNMotion.resolve(DNMotion.smooth)) { store.state.history.removeAll() }
                DNHaptics.success()
                toasts.show("Your DubaiNow history is deleted")
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This removes your past payments and receipts from DubaiNow. Each entity keeps its own records.")
        }
    }

    private func binding(for account: LinkedAccount) -> Binding<Bool> {
        Binding(
            get: { store.state.linkedAccounts.first { $0.id == account.id }?.isLinked ?? false },
            set: { newValue in
                guard let i = store.state.linkedAccounts.firstIndex(where: { $0.id == account.id }) else { return }
                DNHaptics.selection()
                withAnimation(DNMotion.resolve(DNMotion.snappy)) { store.state.linkedAccounts[i].isLinked = newValue }
            }
        )
    }
}

private struct ProfileLinkedAccountRow: View {
    var account: LinkedAccount
    @Binding var isOn: Bool

    var body: some View {
        Toggle(isOn: $isOn) {
            HStack(spacing: 12) {
                ProfileGreyDisc(monogram: Self.monogram(account))
                VStack(alignment: .leading, spacing: 1) {
                    Text(Self.fullName(account))
                        .dnFont(.rowTitle)
                        .fontWeight(.semibold)
                        .foregroundStyle(DNColor.textPrimary)
                    Text(account.isLinked ? account.detail : "Not linked")
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textSecondary)
                }
            }
        }
        .tint(DNColor.controlSwitchTrackOn)
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .frame(minHeight: 68)
        .accessibilityHint(isOn ? "DubaiNow can ask \(account.name) for your information" : "DubaiNow won't ask \(account.name)")
    }

    /// Entities by their full names, as on the Paper screen.
    static func fullName(_ a: LinkedAccount) -> String {
        switch a.id {
        case "rta": "Roads and Transport Authority"
        case "dewa": "Dubai Electricity and Water"
        case "police": "Dubai Police"
        case "gdrfa": "Residency and Foreign Affairs"
        case "dha": "Dubai Health Authority"
        default: a.name
        }
    }

    static func monogram(_ a: LinkedAccount) -> String {
        switch a.id {
        case "rta": "RTA"
        case "dewa": "DEWA"
        case "police": "DP"
        case "salik": "SLK"
        case "parkin": "PKN"
        case "gdrfa": "GDRFA"
        case "dha": "DHA"
        default: String(a.name.prefix(4))
        }
    }
}
