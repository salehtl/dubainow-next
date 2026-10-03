import SwiftUI

/// Delete your account — three steps (Paper C1 `WEH-0` → `WJ3-0` → `WLG-0`):
/// 1. what we delete and what stays, 2. a native alert then Face ID,
/// 3. signed out back to Welcome with a toast. App Store guideline 5.1.1(v).
struct ProfileDeleteAccountView: View {
    @Environment(DemoStore.self) private var store
    @Environment(ToastCenter.self) private var toasts
    @State private var confirms = false
    @State private var asksDownload = false
    @State private var isDeleting = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("This removes everything DubaiNow keeps about you, on every device. Your UAE PASS and your records with government stay as they are.")
                    .dnFont(.body)
                    .foregroundStyle(DNColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)

                list(title: "What we delete", symbol: "trash", color: DNColor.actionDestructive, items: [
                    "Your favorites, widgets and Home layout",
                    "Your activity history and unfinished drafts",
                    "The links to RTA, Salik, DEWA and the other entities",
                    "Saved cards, family members and notification settings",
                ])

                list(title: "What stays", symbol: "checkmark.circle", color: DNColor.textSecondary, items: [
                    "Your UAE PASS. Manage or close it in the UAE PASS app.",
                    "Your visas, vehicles, bills, fines and receipts with each entity. The law says they keep them.",
                ])

                if let notice = requestsNotice {
                    HStack(alignment: .top, spacing: 12) {
                        Image(systemName: "exclamationmark")
                            .font(.system(size: 13, weight: .heavy))
                            .foregroundStyle(DNColor.statusWarningForeground)
                            .frame(width: 26, height: 26)
                            .background(DNColor.surfacePrimary, in: .circle)
                            .accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(notice.title)
                                .dnFont(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundStyle(DNColor.textPrimary)
                            Text(notice.body)
                                .dnFont(.subheadline)
                                .foregroundStyle(DNColor.textSecondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(DNColor.statusWarningSurface, in: .rect(cornerRadius: DNRadius.card, style: .continuous))
                    .accessibilityElement(children: .combine)
                }

                VStack(spacing: 8) {
                    Button("Download a copy of your information first") { asksDownload = true }
                        .dnFont(.headline)
                        .foregroundStyle(DNColor.textLink)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity, minHeight: 44)
                        .buttonStyle(.plain)

                    Button {
                        DNHaptics.warning()
                        confirms = true
                    } label: {
                        if isDeleting {
                            ProgressView().tint(DNColor.textOnDestructive)
                        } else {
                            Text("Delete your account")
                        }
                    }
                    .buttonStyle(ProfileDestructiveFillStyle())
                    .disabled(isDeleting)
                }
                .padding(.top, 12)
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.top, 8)
            .padding(.bottom, 32)
        }
        .profileScrollHook()
        .dnCanvasBackground()
        .profileChrome("Delete your account", large: false)
        .profileDownloadCopyAlert(isPresented: $asksDownload)
        .task {
            // Demo hooks: `-profileDeleteAlert` shows step 2's alert; `-profileAutoDelete`
            // runs step 2 → 3 (Face ID, then signed out to Welcome).
            let args = ProcessInfo.processInfo.arguments
            guard args.contains("-profileDeleteAlert") || args.contains("-profileAutoDelete") else { return }
            try? await Task.sleep(for: .seconds(1))
            if args.contains("-profileAutoDelete") { await delete() } else { confirms = true }
        }
        .alert("Delete your account?", isPresented: $confirms) {
            Button("Delete", role: .destructive) {
                Task { await delete() }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This can't be undone. You'll confirm with Face ID, and you'll be signed out on every device.")
        }
    }

    private var requestsNotice: (title: String, body: String)? {
        let active = store.profileActiveRequests
        guard let first = active.first else { return nil }
        let title = active.count == 1 ? "1 request is still in progress" : "\(active.count) requests are still in progress"
        var body = "Your request to \(ProfileText.lowerFirst(first.title)) carries on with \(first.entity)"
        switch active.count {
        case 1: body += "."
        case 2: body += ", and 1 other with its entity."
        default: body += ", and \(active.count - 1) others with their entities."
        }
        return (title, body + " Updates will come by SMS and email instead.")
    }

    private func list(title: String, symbol: String, color: Color, items: [String]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            ProfileOverline(text: title)
            VStack(alignment: .leading, spacing: 14) {
                ForEach(items, id: \.self) { item in
                    HStack(alignment: .firstTextBaseline, spacing: 12) {
                        Image(systemName: symbol)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(color)
                            .frame(width: 22)
                            .accessibilityHidden(true)
                        Text(item)
                            .dnFont(.callout)
                            .foregroundStyle(DNColor.textPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(DNColor.surfacePrimary, in: .rect(cornerRadius: DNRadius.listGroup, style: .continuous))
            .accessibilityElement(children: .combine)
        }
    }

    private func delete() async {
        isDeleting = true
        let confirmed = await ProfileAuth.confirm(reason: "Confirm you want to delete your DubaiNow account")
        guard confirmed else {
            isDeleting = false
            return
        }
        DNHaptics.success()
        store.signOut()
        toasts.show("Your account is deleted. We've emailed you a confirmation.")
    }
}
