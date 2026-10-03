import SwiftUI

/// Happiness Meter · Survey sheet (Paper C1 `F49-0`, rules `F97-0`), from the header smiley,
/// Profile, or with a service as context. One tap on a face is enough; reasons and a comment
/// are optional. After a choice the sheet grows. After Unhappy it offers help. Never blocks.
struct HappinessMeterSheet: View {
    var context: String?

    @Environment(\.dismiss) private var dismiss
    @Environment(ToastCenter.self) private var toasts
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private enum Stage { case rating, sent, unhappyHelp }

    @State private var face: HappinessFace?
    @State private var reasons: Set<String> = []
    @State private var comment = ""
    @State private var stage: Stage = .rating
    @State private var detent: PresentationDetent = .medium
    @State private var comingSoon: ComingSoon?
    @State private var thanked = false
    @FocusState private var commentFocused: Bool

    private static let thanks = "Thanks — your feedback goes to the Happiness Meter"

    var body: some View {
        NavigationStack {
            ScrollView {
                Group {
                    switch stage {
                    case .rating: rating
                    case .sent: sent
                    case .unhappyHelp: unhappyHelp
                    }
                }
                .padding(.horizontal, DNSpace.insetPage)
                .padding(.bottom, 24)
            }
            .profileScrollHook()
            .scrollBounceBehavior(.basedOnSize)
            .scrollDismissesKeyboard(.interactively)
            .navigationTitle("Happiness Meter")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(role: .close) { dismiss() }.tint(DNColor.textPrimary)
                }
            }
        }
        .presentationDetents([.medium, .large], selection: $detent)
        .presentationDragIndicator(.visible)
        .profileComingSoon($comingSoon)
        .onAppear(perform: applyLaunchOptions)
        .onChange(of: face) { old, new in
            if old != new { reasons = [] }
            if new != nil, detent != .large {
                withAnimation(DNMotion.resolve(DNMotion.smooth)) { detent = .large }
            }
        }
        .onDisappear {
            // The rating is sent as soon as a face is tapped, so closing never loses it.
            if face != nil, !thanked { toasts.show(Self.thanks) }
        }
    }

    // MARK: Rating

    private var question: String {
        guard let context, !context.isEmpty else { return "How do you feel about DubaiNow today?" }
        if let s = Catalogue.shared.service(context) {
            return s.kind == "service" ? "How was it to \(ProfileText.lowerFirst(s.name))?" : "How do you feel about \(ProfileText.lowerFirst(s.name))?"
        }
        return "How was it to \(ProfileText.lowerFirst(context))?"
    }

    private var rating: some View {
        VStack(spacing: 24) {
            Text(question)
                .dnFont(.title2)
                .fontWeight(.bold)
                .foregroundStyle(DNColor.textPrimary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 8)
                .accessibilityAddTraits(.isHeader)

            HappinessFacesPicker(selection: $face, size: 80)

            if let face {
                followUp(face)
                    .transition(reduceMotion ? .opacity : .opacity.combined(with: .offset(y: 12)))
            } else {
                Text("One tap is enough. Goes to the Dubai Government Happiness Meter. Your name is not shared.")
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .animation(DNMotion.resolve(DNMotion.smooth), value: face)
    }

    private func reasonsTitle(_ face: HappinessFace) -> String {
        switch face {
        case .happy: "What went well?"
        case .neutral: "What could be better?"
        case .unhappy: "What went wrong?"
        }
    }

    private func reasonOptions(_ face: HappinessFace) -> [String] {
        switch face {
        case .happy: ["Easy to find", "Quick", "Clear information", "Easy to pay", "Something else"]
        case .neutral: ["Finding a service", "Speed", "Paying", "Wrong information", "Something else"]
        case .unhappy: ["Something didn't work", "Too slow", "Couldn't find it", "Wrong information", "Paying", "Something else"]
        }
    }

    private func followUp(_ face: HappinessFace) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(reasonsTitle(face))
                .dnFont(.headline)
                .foregroundStyle(DNColor.textPrimary)
                .accessibilityAddTraits(.isHeader)

            ProfileFlowLayout(spacing: 8, lineSpacing: 8) {
                ForEach(reasonOptions(face), id: \.self) { reason in
                    ProfileReasonChip(title: reason, isSelected: reasons.contains(reason)) {
                        DNHaptics.selection()
                        withAnimation(DNMotion.resolve(DNMotion.snappy)) {
                            if reasons.contains(reason) { reasons.remove(reason) } else { reasons.insert(reason) }
                        }
                    }
                }
            }

            TextField("Tell us more (optional)", text: $comment, axis: .vertical)
                .lineLimit(3...6)
                .dnFont(.body)
                .focused($commentFocused)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(DNColor.fieldSurface, in: .rect(cornerRadius: 16, style: .continuous))
                .padding(.top, 4)

            Button("Send", action: send)
                .buttonStyle(.dn(.primary))
                .padding(.top, 6)

            Text("Goes to the Dubai Government Happiness Meter. Your name is not shared.")
                .dnFont(.caption1)
                .fontWeight(.regular)
                .foregroundStyle(DNColor.textSecondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
        }
    }

    private func send() {
        commentFocused = false
        DNHaptics.success()
        if face == .unhappy {
            withAnimation(DNMotion.resolve(DNMotion.smooth)) {
                stage = .unhappyHelp
                detent = .medium
            }
        } else {
            withAnimation(DNMotion.resolve(DNMotion.smooth)) {
                stage = .sent
                detent = .medium
            }
            Task { @MainActor in
                try? await Task.sleep(for: .seconds(1.2))
                closeWithThanks()
            }
        }
    }

    private func closeWithThanks() {
        guard !thanked else { return }
        thanked = true
        dismiss()
        toasts.show(Self.thanks)
    }

    // MARK: Sent (happy / neutral)

    private var sent: some View {
        VStack(spacing: 14) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 56, weight: .semibold))
                .foregroundStyle(DNColor.statusSuccessForeground)
                .symbolEffect(.bounce, options: .nonRepeating, isActive: !reduceMotion)
                .accessibilityHidden(true)
            Text("Thanks")
                .dnFont(.title2)
                .fontWeight(.bold)
                .foregroundStyle(DNColor.textPrimary)
            Text("Your feedback goes to the Happiness Meter.")
                .dnFont(.body)
                .foregroundStyle(DNColor.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 40)
        .accessibilityElement(children: .combine)
    }

    // MARK: Unhappy → help

    private var unhappyHelp: some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack(alignment: .top, spacing: 14) {
                Circle()
                    .fill(HappinessFace.unhappy.fill)
                    .overlay(FaceGlyph(face: .unhappy, size: 30))
                    .frame(width: 52, height: 52)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 4) {
                    Text("Sorry about that")
                        .dnFont(.title3)
                        .foregroundStyle(DNColor.textPrimary)
                        .accessibilityAddTraits(.isHeader)
                    Text("Your rating was sent. Do you want help with it now?")
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .padding(.top, 16)

            VStack(spacing: 10) {
                Button("Get help with this") {
                    comingSoon = ComingSoon(
                        title: "Get help with this",
                        detail: "Help pages, live chat and a call back come in the next phase of DubaiNow. Your rating was still sent.",
                        symbol: "questionmark.circle.fill",
                        hue: .sky,
                        kind: .notInDemo
                    )
                }
                .buttonStyle(.dn(.primary))

                Button("Report a problem") {
                    comingSoon = ComingSoon(
                        title: "Report a problem",
                        detail: "Reporting a problem to the DubaiNow team comes in the next phase. Your rating and comment were still sent.",
                        symbol: "exclamationmark.bubble.fill",
                        hue: .rose,
                        kind: .notInDemo
                    )
                }
                .buttonStyle(.dn(.secondary))

                Button("Not now") { closeWithThanks() }
                    .buttonStyle(.dn(.tertiary))
            }
        }
    }

    // MARK: Demo hooks

    /// `-happinessFace happy|neutral|unhappy` preselects a face; `-happinessSent` shows the
    /// after-send state (with Unhappy: the help offer).
    private func applyLaunchOptions() {
        // Reduce Motion: open at full height so the follow-up fades in instead of the sheet growing.
        if reduceMotion { detent = .large }
        let defaults = UserDefaults.standard
        if let raw = defaults.string(forKey: "happinessFace"), let f = HappinessFace(rawValue: raw) {
            face = f
            detent = .large
            if ProcessInfo.processInfo.arguments.contains("-happinessSent") {
                stage = f == .unhappy ? .unhappyHelp : .sent
                thanked = f != .unhappy
                detent = .medium
            }
        }
    }
}

private struct ProfileReasonChip: View {
    var title: String
    var isSelected: Bool
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 12, weight: .bold))
                        .accessibilityHidden(true)
                }
                Text(title)
                    .dnFont(.subheadline)
                    .fontWeight(.semibold)
                    .multilineTextAlignment(.leading)
            }
            .foregroundStyle(isSelected ? DNColor.chipTextSelected : DNColor.chipText)
            .padding(.horizontal, 14)
            .frame(minHeight: 40)
            .background(isSelected ? DNColor.chipSurfaceSelected : DNColor.chipSurfaceOnCard, in: .capsule)
            .contentShape(.capsule)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }
}
