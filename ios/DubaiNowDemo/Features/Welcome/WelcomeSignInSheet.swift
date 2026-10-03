import SwiftUI
import LocalAuthentication

/// "Sign in with UAE PASS" in the demo: a calm hand-off, then "Choose a demo resident", then a brief
/// Face ID-style check. Neutral DubaiNow styling — it doesn't imitate the UAE PASS app.
/// `onSignedIn` runs after the check; the presenter dismisses the sheet and signs the persona in.
struct WelcomeSignInSheet: View {
    var onSignedIn: (Persona) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var stage: Stage = .handoff
    /// Debug stages (`-welcomeSheet verifying|verified`) stay put for screenshots.
    @State private var holdsStage = false

    enum Stage: Equatable {
        case handoff
        case picker
        case verifying(Persona)
        case verified(Persona)
    }

    init(onSignedIn: @escaping (Persona) -> Void) {
        self.onSignedIn = onSignedIn
        let sample = Personas.all.first
        switch WelcomeLaunch.signInStage {
        case "picker":
            _stage = State(initialValue: .picker)
        case "verifying":
            if let sample { _stage = State(initialValue: .verifying(sample)) }
            _holdsStage = State(initialValue: true)
        case "verified":
            if let sample { _stage = State(initialValue: .verified(sample)) }
            _holdsStage = State(initialValue: true)
        default:
            break
        }
    }

    private var isConfirming: Bool {
        switch stage {
        case .verifying, .verified: true
        default: false
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                switch stage {
                case .handoff:
                    WelcomeHandoffView()
                        .transition(.opacity)
                case .picker:
                    WelcomePersonaPicker(choose: choose)
                        .transition(.opacity)
                case .verifying(let persona):
                    WelcomeVerifyView(persona: persona, verified: false)
                        .transition(.opacity)
                case .verified(let persona):
                    WelcomeVerifyView(persona: persona, verified: true)
                }
            }
            .animation(DNMotion.resolve(DNMotion.smooth), value: stage)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if !isConfirming {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(role: .close) { dismiss() }.tint(DNColor.textPrimary)
                            .accessibilityLabel("Cancel")
                    }
                }
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .interactiveDismissDisabled(isConfirming)
        .task {
            // A short, calm hand-off moment, then the persona picker.
            guard stage == .handoff, WelcomeLaunch.signInStage != "handoff" else { return }
            UIAccessibility.post(notification: .announcement, argument: "Signing in with UAE PASS")
            try? await Task.sleep(for: .seconds(1.6))
            guard !Task.isCancelled else { return }
            stage = .picker
            UIAccessibility.post(notification: .screenChanged, argument: nil)
            // Debug (`-welcomeAutoSignIn`): pick the first resident, to check the whole flow without taps.
            if ProcessInfo.processInfo.arguments.contains("-welcomeAutoSignIn"), let first = Personas.all.first {
                try? await Task.sleep(for: .seconds(1))
                choose(first)
            }
        }
    }

    private func choose(_ persona: Persona) {
        guard stage == .picker else { return }
        stage = .verifying(persona)
        Task { await confirm(persona) }
    }

    /// Face ID (or the device passcode) when it's set up; otherwise a short Face ID-style moment.
    /// Either way the demo carries on: no real account is checked.
    private func confirm(_ persona: Persona) async {
        let context = LAContext()
        var error: NSError?
        var passed = false
        if context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) {
            passed = (try? await context.evaluatePolicy(
                .deviceOwnerAuthentication,
                localizedReason: "Sign in to DubaiNow as \(persona.fullName)"
            )) ?? false
        }
        if !passed {
            try? await Task.sleep(for: .seconds(0.7))
        }
        stage = .verified(persona)
        DNHaptics.success()
        UIAccessibility.post(notification: .announcement, argument: "Verified. Signing in as \(persona.fullName).")
        try? await Task.sleep(for: .seconds(0.8))
        guard !holdsStage else { return }
        onSignedIn(persona)
    }
}

// MARK: - Hand-off

/// DubaiNow → UAE PASS, with dots travelling between the two marks.
private struct WelcomeHandoffView: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(spacing: 28) {
            Spacer(minLength: 0)
            HStack(spacing: 16) {
                Circle()
                    .fill(DNColor.surfacePrimary)
                    .frame(width: 72, height: 72)
                    .overlay(DNLogo(height: 34))
                    .shadow(color: DNColor.elevationNear, radius: 1, y: 1)
                    .shadow(color: DNColor.elevationFar, radius: 12, y: 8)
                WelcomeHandoffDots()
                WelcomeUAEPassMark(size: 72)
            }
            .accessibilityHidden(true)

            VStack(spacing: 8) {
                Text("Signing in with UAE PASS")
                    .dnFont(.title2)
                    .foregroundStyle(DNColor.textPrimary)
                Text("We’re handing you over securely. You’ll come straight back here.")
                    .dnFont(.body)
                    .foregroundStyle(DNColor.textSecondary)
            }
            .multilineTextAlignment(.center)
            .fixedSize(horizontal: false, vertical: true)

            if reduceMotion {
                ProgressView()
            }
            Spacer(minLength: 0)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct WelcomeHandoffDots: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    private let count = 5

    var body: some View {
        TimelineView(.animation(paused: reduceMotion)) { context in
            let t = reduceMotion ? 0 : context.date.timeIntervalSinceReferenceDate
            let head = (t / 1.1).truncatingRemainder(dividingBy: 1) * Double(count + 2) - 1
            HStack(spacing: 7) {
                ForEach(0..<count, id: \.self) { i in
                    let near = max(0, 1 - abs(Double(i) - head) / 1.6)
                    Circle()
                        .fill(DNColor.textPrimary.opacity(reduceMotion ? 0.35 : 0.18 + 0.72 * near))
                        .frame(width: 6, height: 6)
                        .scaleEffect(1 + 0.35 * near)
                }
            }
        }
        .frame(width: 70)
    }
}

/// The UAE PASS mark as used on the sign-in pill: black disc, fingerprint.
private struct WelcomeUAEPassMark: View {
    var size: CGFloat

    var body: some View {
        Circle()
            .fill(.black)
            .frame(width: size, height: size)
            .overlay(
                Image(systemName: "touchid")
                    .font(.system(size: size * 0.46, weight: .regular))
                    .foregroundStyle(DNColor.white)
            )
            .shadow(color: DNColor.elevationFar, radius: 12, y: 8)
    }
}

// MARK: - Persona picker

private struct WelcomePersonaPicker: View {
    var choose: (Persona) -> Void

    var body: some View {
        List {
            Section {
                ForEach(Personas.all) { persona in
                    Button {
                        choose(persona)
                    } label: {
                        WelcomePersonaRow(persona: persona)
                    }
                }
            } header: {
                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 6) {
                        Image(systemName: "touchid")
                            .font(.system(size: 13, weight: .semibold))
                        Text("UAE PASS")
                            .font(.system(size: 13, weight: .semibold))
                    }
                    .foregroundStyle(DNColor.white)
                    .padding(.horizontal, 10)
                    .frame(height: 26)
                    .background(.black, in: .capsule)
                    .accessibilityHidden(true)

                    Text("Choose a demo resident")
                        .dnFont(.title1)
                        .foregroundStyle(DNColor.textPrimary)
                        .accessibilityAddTraits(.isHeader)
                    Text("Each one has their own bills, cars, family and requests.")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                }
                .textCase(nil)
                .padding(.bottom, 12)
            } footer: {
                Label("This is a demo. No real accounts are used.", systemImage: "lock.fill")
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                    .padding(.top, 4)
            }
        }
        .listStyle(.insetGrouped)
        .contentMargins(.top, 8, for: .scrollContent)
    }
}

private struct WelcomePersonaRow: View {
    let persona: Persona
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        let layout = typeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: 10))
            : AnyLayout(HStackLayout(spacing: 14))
        layout {
            Avatar(initials: persona.initials, hue: persona.avatarHue, size: 48)
            VStack(alignment: .leading, spacing: 2) {
                Text(persona.fullName)
                    .dnFont(.rowTitle)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.listRowTitle)
                Text(persona.tagline)
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.listRowSupporting)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if !typeSize.isAccessibilitySize {
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(DNColor.listRowChevron)
                    .accessibilityHidden(true)
            }
        }
        .padding(.vertical, 6)
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Face ID-style confirmation

private struct WelcomeVerifyView: View {
    let persona: Persona
    let verified: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(spacing: 24) {
            Spacer(minLength: 0)
            RoundedRectangle(cornerRadius: 30, style: .continuous)
                .fill(verified ? DNColor.statusSuccessSurface : DNColor.actionAccentSubtle)
                .frame(width: 104, height: 104)
                .overlay(
                    Image(systemName: verified ? "checkmark" : "faceid")
                        .font(.system(size: verified ? 46 : 58, weight: verified ? .bold : .regular))
                        .foregroundStyle(verified ? DNColor.statusSuccessForeground : DNColor.actionAccent)
                        .contentTransition(.symbolEffect(.replace))
                        .symbolEffect(.pulse, options: .repeating, isActive: !verified && !reduceMotion)
                )
                .accessibilityHidden(true)

            VStack(spacing: 10) {
                Text(verified ? "Verified" : "Confirming it’s you")
                    .dnFont(.title2)
                    .foregroundStyle(DNColor.textPrimary)
                    .contentTransition(.opacity)
                HStack(spacing: 8) {
                    Avatar(initials: persona.initials, hue: persona.avatarHue, size: 26)
                    Text("Signing in as \(persona.fullName)")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                }
            }
            .multilineTextAlignment(.center)
            .accessibilityElement(children: .combine)
            Spacer(minLength: 0)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
