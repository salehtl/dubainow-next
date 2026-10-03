import SwiftUI

/// Guest · Home (WOV-0): in place of Needs you, an invitation to sign in. Signing in goes
/// back to Welcome, where choosing UAE PASS picks a demo resident.
struct HomeGuestSignInCard: View {
    @Environment(DemoStore.self) private var store

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("See what needs you")
                .dnFont(.section)
                .foregroundStyle(DNColor.textPrimary)
                .accessibilityAddTraits(.isHeader)
            Text("Sign in to see your bills, fines and renewals here, and pay them in one go.")
                .dnFont(.subheadline)
                .foregroundStyle(DNColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            Button {
                DNHaptics.light()
                store.signOut()
            } label: {
                ZStack {
                    Text("Sign in with UAE PASS")
                    HStack {
                        Image(systemName: "touchid")
                            .font(.system(size: 22, weight: .regular))
                            .accessibilityHidden(true)
                        Spacer()
                    }
                }
            }
            .buttonStyle(.dn(.inverse, size: .large))
            .padding(.top, 14)
        }
        .padding(20)
        .dnCard(radius: DNRadius.brief)
        .padding(.horizontal, DNSpace.insetPage)
    }
}
