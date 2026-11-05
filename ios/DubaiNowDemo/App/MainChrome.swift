import SwiftUI

/// The header on every main screen (Home, Services, Activity tab roots):
/// Happiness Meter smiley top left (never a bell), the DubaiNow logo top centre (tap → the
/// assistant), bell (Home only) and avatar top right. Native toolbar items, so iOS gives
/// them Liquid Glass. The logo has no glass behind it.
struct MainToolbar: ViewModifier {
    var showsBell: Bool = false
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router

    func body(content: Content) -> some View {
        content.toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    router.present(.happinessMeter(context: nil))
                } label: {
                    Image(systemName: "face.smiling")
                        .font(.system(size: 20, weight: .medium))
                }
                .tint(DNColor.textPrimary)
                .accessibilityLabel("Happiness Meter")
            }
            ToolbarItem(placement: .principal) {
                AssistantLogoButton()
            }
            .sharedBackgroundVisibility(.hidden)
            if showsBell && !store.isGuest {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        router.present(.notifications)
                    } label: {
                        Image(systemName: "bell")
                            .font(.system(size: 18, weight: .medium))
                            .overlay(alignment: .topTrailing) {
                                if store.unreadCount > 0 {
                                    Text("\(store.unreadCount)")
                                        .font(.system(size: 11, weight: .bold, design: .rounded))
                                        .foregroundStyle(.white)
                                        .frame(minWidth: 17, minHeight: 17)
                                        .background(DNColor.red700, in: .circle)
                                        .offset(x: 9, y: -9)
                                }
                            }
                    }
                    .tint(DNColor.textPrimary)
                    .accessibilityLabel(store.unreadCount > 0 ? "Notifications, \(store.unreadCount) new" : "Notifications")
                }
                ToolbarSpacer(.fixed, placement: .topBarTrailing)
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    router.present(.profile)
                } label: {
                    if let p = store.persona {
                        Avatar(initials: p.initials, hue: p.avatarHue, size: 32)
                    } else {
                        Image(systemName: "person")
                            .font(.system(size: 18, weight: .medium))
                    }
                }
                .tint(DNColor.textPrimary)
                .accessibilityLabel(store.isGuest ? "Sign in or profile" : "Your profile")
            }
        }
    }
}

/// Pushed screens: system back button, the logo top centre (the assistant is reachable from
/// every full screen), optional trailing actions supplied by the screen itself.
struct PushedToolbar: ViewModifier {
    func body(content: Content) -> some View {
        content.toolbar {
            ToolbarItem(placement: .principal) {
                AssistantLogoButton()
            }
            .sharedBackgroundVisibility(.hidden)
        }
    }
}

extension View {
    /// Tab-root header: smiley · logo · (bell) · avatar.
    func dnMainToolbar(showsBell: Bool = false) -> some View { modifier(MainToolbar(showsBell: showsBell)) }
    /// Pushed-screen header: back · logo · (your own trailing items).
    func dnPushedToolbar() -> some View { modifier(PushedToolbar()) }
}

/// The logo is the assistant's face. Tap to ask; hold to talk.
struct AssistantLogoButton: View {
    @Environment(Router.self) private var router
    @Environment(DemoStore.self) private var store
    @Environment(\.dnIsInSheet) private var isInSheet

    private func open(seed: String?) {
        guard store.session != nil, !store.isGuest else { return }
        DNHaptics.medium()
        if isInSheet || router.sheet != nil {
            // The assistant covers the app, not a sheet: close the sheet first.
            router.dismissSheet()
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(450))
                router.openAssistant(seed: seed)
            }
        } else {
            router.openAssistant(seed: seed)
        }
    }

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var isPressed = false

    private var isLive: Bool { store.session != nil && !store.isGuest }

    var body: some View {
        DNLogo(height: 28)
            .overlay { if isLive && !reduceMotion { AssistantLogoSheen(height: 28) } }
            .scaleEffect(isPressed && isLive && !reduceMotion ? 0.9 : 1)
            .animation(DNMotion.snappy, value: isPressed)
            .padding(.horizontal, 10)
            .frame(minWidth: 44, minHeight: 44)
            .contentShape(.rect)
            .onTapGesture { open(seed: nil) }
            .onLongPressGesture(minimumDuration: DNMotion.holdDelay) {
                open(seed: "__listen__")
            } onPressingChanged: { isPressed = $0 }
            .accessibilityElement()
            .accessibilityLabel("DubaiNow assistant")
            .accessibilityHint("Ask DubaiNow to do something for you")
            .accessibilityAddTraits(.isButton)
    }
}

/// A soft band of light that crosses the mark now and then, so the logo reads as something you
/// can tap (it's the assistant). Clipped to the mark's shape; never runs under Reduce Motion.
private struct AssistantLogoSheen: View {
    var height: CGFloat
    /// First sweep shortly after the header appears, then one every few seconds.
    private let firstDelay: Duration = .milliseconds(1200)
    private let interval: Duration = .seconds(6)
    private let sweep: Double = 1.1

    @State private var phase: CGFloat = 0

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            LinearGradient(
                colors: [.white.opacity(0), .white.opacity(0.65), .white.opacity(0)],
                startPoint: .leading, endPoint: .trailing
            )
            .frame(width: w * 0.55, height: geo.size.height * 1.6)
            .rotationEffect(.degrees(18))
            // From fully left of the mark to fully right of it.
            .offset(x: -w * 0.6 + phase * w * 1.6, y: -geo.size.height * 0.3)
        }
        .mask(DNLogo(height: height))
        .allowsHitTesting(false)
        .accessibilityHidden(true)
        .task {
            try? await Task.sleep(for: firstDelay)
            while !Task.isCancelled {
                withAnimation(.easeInOut(duration: sweep)) { phase = 1 }
                try? await Task.sleep(for: .seconds(sweep))
                phase = 0
                try? await Task.sleep(for: interval)
            }
        }
    }
}

/// Hidden demo controls (from Profile → "Demo controls", or a long press on the version line):
/// switch resident, reset the demo, change the time of day.
struct DemoMenuSheet: View {
    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(ToastCenter.self) private var toasts
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section("Sign in as") {
                    ForEach(Personas.all) { p in
                        Button {
                            store.signIn(p)
                            router.reset()
                            dismiss()
                            toasts.show("Signed in as \(p.firstName)")
                        } label: {
                            HStack(spacing: 12) {
                                Avatar(initials: p.initials, hue: p.avatarHue, size: 40)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(p.fullName).dnFont(.rowTitle).foregroundStyle(DNColor.textPrimary)
                                    Text(p.tagline).dnFont(.footnote).foregroundStyle(DNColor.textSecondary)
                                }
                                Spacer()
                                if store.persona == p { Image(systemName: "checkmark").foregroundStyle(DNColor.actionAccent) }
                            }
                        }
                    }
                    Button("Continue as guest") {
                        store.continueAsGuest(); router.reset(); dismiss()
                    }
                }
                Section("Time of day") {
                    Picker("Home wash", selection: Binding(get: { store.hourOverride ?? -1 }, set: { store.hourOverride = $0 < 0 ? nil : $0 })) {
                        Text("Now").tag(-1)
                        Text("Morning").tag(9)
                        Text("Midday").tag(13)
                        Text("Evening").tag(18)
                        Text("Night").tag(22)
                    }
                    .pickerStyle(.segmented)
                }
                Section {
                    Button("Reset this resident's data") {
                        store.resetDemo(); router.reset(); dismiss()
                        toasts.show("Demo reset")
                    }
                    Button("Show Welcome again", role: .destructive) {
                        store.signOut(); dismiss()
                    }
                } footer: {
                    Text("This is a demo. No real accounts, payments or government systems are connected.")
                }
            }
            .navigationTitle("Demo controls")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

extension EnvironmentValues {
    /// True inside any sheet presented by `SheetDestination`.
    @Entry var dnIsInSheet: Bool = false
}
