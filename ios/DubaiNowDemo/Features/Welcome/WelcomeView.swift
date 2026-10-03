import SwiftUI

/// Pre-login Welcome (Paper C1 · "10 · Welcome"): the whole screen is a story player that tours the app
/// and carries announcements; the sign-in panel underneath never moves, so nobody has to watch to get in.
///
/// Rules (Paper board "Stories first, then one tap to sign in"):
/// - 6 s per story, the segment fills in a straight line. Tap the right two-thirds for next, the left third
///   for back, or swipe sideways. Touch and hold pauses. It never loops on its own.
/// - Reduce Motion or VoiceOver: no auto-advance; the story area is one adjustable element.
/// - Larger text: the hero shrinks first, then hides at the accessibility sizes. Copy and panel show in full.
///
/// Debug launch arguments: `-welcomeStory 1…6` starts on a story; `-welcomeSheet signIn|handoff|picker|verifying|verified|createAccount|events`
/// opens a sheet at launch (for screenshots).
struct WelcomeView: View {
    @Environment(DemoStore.self) private var store
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.accessibilityVoiceOverEnabled) private var voiceOverOn
    @Environment(\.dynamicTypeSize) private var typeSize
    @Environment(\.scenePhase) private var scenePhase

    private let stories = WelcomeStory.all
    @State private var clock: WelcomeStoryClock
    @State private var isHolding = false
    @State private var touchStart: Date?
    @State private var storyWidth: CGFloat = 402
    @State private var bottomInset: CGFloat = 34
    @State private var sheet: WelcomeSheet?
    /// Chosen in the sign-in sheet; signed in once the sheet has gone.
    @State private var pendingPersona: Persona?

    init() {
        let launch = WelcomeLaunch.consume()
        _clock = State(initialValue: WelcomeStoryClock(count: WelcomeStory.all.count, startAt: launch.story))
        _sheet = State(initialValue: launch.sheet)
    }

    var body: some View {
        let story = stories[clock.index]
        VStack(spacing: 0) {
            storyArea(story)
            panel
        }
        .background { wash(story) }
        .onGeometryChange(for: CGFloat.self) { $0.safeAreaInsets.bottom } action: { bottomInset = $0 }
        .sheet(item: $sheet, onDismiss: finishSignIn) { sheet in
            sheetContent(sheet)
        }
        .task(id: WelcomeRunKey(index: clock.index, running: shouldRun)) {
            guard shouldRun else {
                clock.pause()
                return
            }
            clock.run()
            try? await Task.sleep(for: .seconds(clock.remaining))
            guard !Task.isCancelled else { return }
            withAnimation(storyAnimation) { clock.timerEnded() }
        }
    }

    // MARK: Playback

    /// Reduce Motion and VoiceOver turn auto-advance off.
    private var autoAdvance: Bool { !reduceMotion && !voiceOverOn }

    private var shouldRun: Bool {
        autoAdvance && !isHolding && sheet == nil && scenePhase == .active && !clock.isFinished
    }

    /// The hue cross-fades in 300 ms (250 ms with Reduce Motion).
    private var storyAnimation: Animation { DNMotion.resolve(.easeInOut(duration: 0.3)) }

    private func goNext() {
        DNHaptics.selection()
        withAnimation(storyAnimation) { clock.next() }
    }

    private func goPrevious() {
        DNHaptics.selection()
        withAnimation(storyAnimation) { clock.previous() }
    }

    /// Tap zones (left third back, right two-thirds next), sideways swipes, and touch-and-hold to pause.
    private var storyGesture: some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { _ in
                guard touchStart == nil else { return }
                touchStart = .now
                isHolding = true
            }
            .onEnded { value in
                let held = Date.now.timeIntervalSince(touchStart ?? .now)
                touchStart = nil
                isHolding = false
                let dx = value.translation.width, dy = value.translation.height
                if abs(dx) > 44, abs(dx) > abs(dy) {
                    if dx < 0 { goNext() } else { goPrevious() }
                } else if held < 0.35, hypot(dx, dy) < 12 {
                    if value.startLocation.x < storyWidth / 3 { goPrevious() } else { goNext() }
                }
                // Anything longer was a hold: letting go resumes the story.
            }
    }

    // MARK: Story area

    private func storyArea(_ story: WelcomeStory) -> some View {
        ZStack {
            // Only the story area takes the story gestures; the panel never does.
            Color.clear
                .contentShape(.rect)
                .gesture(storyGesture)

            VStack(spacing: 0) {
                progressBar
                    .padding(.horizontal, 16)
                    .padding(.top, 4)
                    .allowsHitTesting(false)

                HStack(spacing: 10) {
                    ZStack(alignment: .leading) {
                        WelcomeStoryHeader(story: story)
                            .id(story.sourceKey)
                            .transition(.opacity)
                    }
                    Spacer(minLength: 0)
                    // English only for this build: the عربي language pill (top right in Paper; it switches the
                    // whole app at once and keeps the current story) is hidden. Put it back here with Arabic.
                }
                .frame(minHeight: 36)
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .dynamicTypeSize(...DynamicTypeSize.accessibility1)
                .allowsHitTesting(false)

                if typeSize.isAccessibilitySize {
                    // The hero hides at the accessibility sizes; the copy scrolls if it has to.
                    ScrollView {
                        copy(story).padding(.top, 24)
                    }
                    .scrollBounceBehavior(.basedOnSize)
                    .scrollIndicatorsFlash(onAppear: true)
                } else {
                    heroArea(story)
                    copy(story).allowsHitTesting(false)
                }
            }
        }
        .onGeometryChange(for: CGFloat.self) { $0.size.width } action: { storyWidth = $0 }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("Story \(clock.index + 1) of \(stories.count), \(String(localized: story.headline))"))
        .accessibilityValue(Text(story.accessibilityValue))
        .accessibilityHint(Text("Swipe up or down to change the story."))
        .accessibilityAdjustableAction { direction in
            switch direction {
            case .increment: if clock.index < stories.count - 1 { goNext() }
            case .decrement: if clock.index > 0 { goPrevious() }
            @unknown default: break
            }
        }
        .accessibilityActions {
            if story.hero == .poster {
                Button("See events") { sheet = .events }
            }
        }
    }

    private var progressBar: some View {
        TimelineView(.animation(paused: !clock.isRunning)) { context in
            WelcomeProgressBar(
                count: stories.count,
                index: clock.index,
                // Without auto-advance the current segment just shows where you are.
                fraction: autoAdvance ? clock.fraction(at: context.date) : 1,
                finished: clock.isFinished
            )
        }
        .accessibilityHidden(true)
    }

    /// The hero keeps Paper's composition and scales down to fit; it hides when it would get too small.
    private func heroArea(_ story: WelcomeStory) -> some View {
        GeometryReader { geo in
            let size = WelcomeHeroMetrics.size
            let scale = min(1, geo.size.width / size.width, geo.size.height / size.height)
            let scaledHeight = size.height * scale
            // Paper's hero frame is 419 pt with the objects in its top 392 pt: sit at the top like Paper,
            // and centre only the space beyond that on taller phones.
            let top = max(0, (geo.size.height - scaledHeight - 27) / 2)
            if scale >= 0.5 {
                ZStack {
                    hero(for: story)
                        .id(story.id)
                        .transition(.asymmetric(insertion: .identity, removal: .opacity))
                }
                .frame(width: size.width, height: size.height)
                .scaleEffect(scale, anchor: .top)
                .frame(width: geo.size.width, height: scaledHeight, alignment: .top)
                .offset(y: top)
            }
        }
        .accessibilityHidden(true)
    }

    @ViewBuilder
    private func hero(for story: WelcomeStory) -> some View {
        switch story.hero {
        case .orbit: WelcomeOrbitHero()
        case .needsYou: WelcomeNeedsYouHero()
        case .widgets: WelcomeWidgetsHero()
        case .journey: WelcomeJourneyHero()
        case .search: WelcomeSearchHero()
        case .poster: WelcomePosterHero { sheet = .events }
        }
    }

    /// Kicker, one-line headline (34/40 bold) and body. Copy fades in last.
    private func copy(_ story: WelcomeStory) -> some View {
        ZStack(alignment: .bottomLeading) {
            WelcomeStoryCopy(story: story)
                .id(story.id)
                .transition(.asymmetric(
                    insertion: .opacity.animation(DNMotion.resolve(.easeOut(duration: 0.25).delay(0.18))),
                    removal: .opacity.animation(.easeIn(duration: 0.12))
                ))
        }
    }

    private func wash(_ story: WelcomeStory) -> some View {
        ZStack {
            WelcomeWash(hue: story.hue)
                .id(story.hue)
                .transition(.opacity)
        }
        .animation(storyAnimation, value: story.hue)
        .ignoresSafeArea()
    }

    // MARK: Sign-in panel (fixed: it doesn't advance with the stories)

    private var panel: some View {
        VStack(spacing: 12) {
            Button {
                sheet = .signIn
            } label: {
                HStack(spacing: DNSpace.xs) {
                    Image(systemName: "touchid")
                        .font(.system(size: 24, weight: .regular))
                        .accessibilityHidden(true)
                    Text("Sign in with UAE PASS")
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(WelcomePillStyle(kind: .uaePass))

            Button("Continue as guest") {
                store.continueAsGuest()
            }
            .buttonStyle(WelcomePillStyle(kind: .guest))

            Button {
                sheet = .createAccount
            } label: {
                Text("New to UAE PASS? \(Text("Create an account").fontWeight(.semibold).foregroundStyle(DNColor.textOnWashLink))")
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textOnWashSecondary)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity, minHeight: DNSpace.targetMinimum)
                    .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .padding(.vertical, -6)
        }
        .padding(.horizontal, DNSpace.insetPage)
        // Paper: the link row ends 30 pt above the screen edge (just inside the home-indicator area).
        .padding(.bottom, bottomInset > 0 ? 30 - bottomInset : 16)
        .dynamicTypeSize(...DynamicTypeSize.accessibility3)
    }

    // MARK: Sheets (local: Welcome sits outside the main TabView and its router)

    @ViewBuilder
    private func sheetContent(_ sheet: WelcomeSheet) -> some View {
        switch sheet {
        case .signIn:
            WelcomeSignInSheet { persona in
                pendingPersona = persona
                self.sheet = nil
            }
        case .createAccount:
            ComingSoonSheet(item: ComingSoon(
                title: "Create a UAE PASS account",
                detail: "In the real app, this opens UAE PASS so you can create your account. When it’s done, you come back to DubaiNow signed in.",
                owner: "UAE PASS",
                symbol: "person.crop.circle.badge.plus",
                hue: .sky,
                kind: .notInDemo
            ))
        case .events:
            ComingSoonSheet(item: ComingSoon(
                title: "Dubai Fitness Challenge events",
                detail: "In the real app, this link opens the events, and you don’t need to sign in to see them.",
                owner: "Dubai Fitness Challenge",
                symbol: "figure.run",
                hue: .rose,
                kind: .notInDemo
            ))
        }
    }

    private func finishSignIn() {
        guard let persona = pendingPersona else { return }
        pendingPersona = nil
        store.signIn(persona)
    }
}

// MARK: - Pieces

enum WelcomeSheet: String, Identifiable {
    case signIn, createAccount, events
    var id: String { rawValue }
}

private struct WelcomeRunKey: Equatable {
    var index: Int
    var running: Bool
}

/// Launch arguments for screenshots, applied once per launch.
enum WelcomeLaunch {
    private static var consumed = false

    static func consume() -> (story: Int, sheet: WelcomeSheet?) {
        guard !consumed else { return (0, nil) }
        consumed = true
        let defaults = UserDefaults.standard
        let story = max(defaults.integer(forKey: "welcomeStory") - 1, 0)
        let sheet: WelcomeSheet? = switch defaults.string(forKey: "welcomeSheet") {
        case "signIn", "handoff", "picker", "verifying", "verified": .signIn
        case "createAccount": .createAccount
        case "events": .events
        default: nil
        }
        return (story, sheet)
    }

    /// The sign-in stage to open on (`-welcomeSheet picker|verifying|verified`).
    static var signInStage: String? { UserDefaults.standard.string(forKey: "welcomeSheet") }
}

extension WelcomeStory {
    var sourceKey: String {
        switch source {
        case .dubaiNow: "dubaiNow"
        case .entity(let name, _, _): String(localized: name)
        }
    }

    /// What VoiceOver reads after the label: who it's from, the kicker and the body.
    var accessibilityValue: String {
        let text = "\(String(localized: kicker)). \(String(localized: body))"
        if case .entity(let name, _, _) = source {
            return "From \(String(localized: name)). \(text)"
        }
        return text
    }
}

/// Six 3-pt segments: played stories full, the current one filling, the rest empty.
private struct WelcomeProgressBar: View {
    var count: Int
    var index: Int
    var fraction: Double
    var finished: Bool

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<count, id: \.self) { i in
                let fill = finished || i < index ? 1 : (i == index ? fraction : 0)
                Capsule()
                    .fill(DNColor.progressTrackOnHue)
                    .overlay(alignment: .leading) {
                        GeometryReader { geo in
                            Capsule()
                                .fill(DNColor.textPrimary)
                                .frame(width: max(0, geo.size.width * fill))
                        }
                    }
                    .frame(height: 3)
            }
        }
    }
}

/// DubaiNow · Welcome, or the entity that published the announcement.
private struct WelcomeStoryHeader: View {
    let story: WelcomeStory

    var body: some View {
        HStack(spacing: 10) {
            switch story.source {
            case .dubaiNow:
                Circle()
                    .fill(DNColor.surfacePrimary)
                    .frame(width: 36, height: 36)
                    .overlay(DNLogo(height: 19))
                    .shadow(color: DNColor.elevationNear, radius: 1, y: 1)
                Text("DubaiNow")
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                Text("Welcome")
                    .foregroundStyle(DNColor.textOnWashSecondary)
            case .entity(let name, let symbol, let age):
                Circle()
                    .fill(story.hue.ink)
                    .frame(width: 36, height: 36)
                    .overlay(
                        Image(systemName: symbol)
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(DNColor.white)
                    )
                    .shadow(color: DNColor.elevationNear, radius: 1, y: 1)
                Text(name)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                Text(age)
                    .foregroundStyle(DNColor.textOnWashSecondary)
            }
        }
        .dnFont(.subheadline)
        .lineLimit(1)
    }
}

private struct WelcomeStoryCopy: View {
    let story: WelcomeStory

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(story.kicker)
                .dnFont(.overline)
                .textCase(.uppercase)
                .foregroundStyle(story.hue.ink)
            Text(story.headline)
                .dnFont(.largeTitle)
                .foregroundStyle(DNColor.textPrimary)
            Text(story.body)
                .dnFont(.body)
                .foregroundStyle(DNColor.textOnWashSecondary)
        }
        .fixedSize(horizontal: false, vertical: true)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 24)
        .padding(.bottom, 24)
    }
}

/// The story's hue wash: start → end (62%) → start, top to bottom.
private struct WelcomeWash: View {
    var hue: Hue

    var body: some View {
        LinearGradient(
            stops: [
                .init(color: hue.start, location: 0),
                .init(color: hue.end, location: 0.62),
                .init(color: hue.start, location: 1),
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}

/// The two panel pills. UAE PASS is a brand button (Paper exception X-07): black, its mark and label,
/// our pill and the Large height. "Continue as guest" is a white pill on the wash.
private struct WelcomePillStyle: ButtonStyle {
    enum Kind { case uaePass, guest }
    var kind: Kind
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        let pressed = configuration.isPressed
        configuration.label
            .dnFont(.button)
            .multilineTextAlignment(.center)
            .foregroundStyle(kind == .uaePass ? DNColor.white : DNColor.textPrimary)
            .padding(.horizontal, DNSpace.md)
            .padding(.vertical, DNSpace.sm)
            .frame(maxWidth: .infinity, minHeight: DNSpace.buttonHeightLarge)
            .background(fill(pressed: pressed), in: .capsule)
            .contentShape(.capsule)
            .scaleEffect(pressed && !reduceMotion ? 0.98 : 1)
            .animation(DNMotion.snappy, value: pressed)
    }

    private func fill(pressed: Bool) -> Color {
        switch kind {
        // UAE PASS brand black (not a Canvas token on purpose).
        case .uaePass: pressed ? DNColor.ink850 : .black
        case .guest: pressed ? DNColor.neutral100 : DNColor.surfacePrimary
        }
    }
}
