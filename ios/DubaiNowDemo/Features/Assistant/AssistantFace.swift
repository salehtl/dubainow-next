import SwiftUI

/// The light the assistant is made of: the brand blues (Paper's conic edge/ring gradient).
enum AssistantLight {
    static let ring: [Color] = [DNColor.blue300, DNColor.blue600, DNColor.blue200, DNColor.blue800, DNColor.blue400, DNColor.blue700, DNColor.blue300]
    static let glow = DNColor.actionAccent
    static let line = DNColor.blue200
    static let halo = DNColor.blue300
}

/// Its face: the DubaiNow mark inside a glass orb, with a ring of light that says what it's
/// doing (Paper W1D-0 · eight states, one mark). The mark never changes shape.
struct AssistantFace: View {
    var state: AssistantFaceState
    var progress: Double
    /// False while the orb is still forming (wakes 0.6 → 1, bouncy).
    var isAwake: Bool

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var nod = false
    @State private var glow = false

    private let orb: CGFloat = 68
    private let ring: CGFloat = 74

    var body: some View {
        ZStack {
            // Halo: a soft light from the logo.
            Circle()
                .fill(RadialGradient(
                    colors: [DNColor.white.opacity(0.95), DNColor.blue50.opacity(0.78), AssistantLight.halo.opacity(0.32), AssistantLight.halo.opacity(0)],
                    center: .center, startRadius: 0, endRadius: 80
                ))
                .frame(width: 160, height: 160)
                .opacity(state == .quiet ? 0.6 : 1)

            if state == .listening && !reduceMotion {
                AssistantRipples()
            }

            ringView
                .frame(width: ring, height: ring)
                .scaleEffect(isAwake ? 1 : 0.6)
                .opacity(isAwake ? 1 : 0)

            Circle()
                .fill(DNColor.white.opacity(0.35))
                .frame(width: orb, height: orb)
                .glassEffect(.regular.tint(DNColor.white.opacity(0.55)), in: .circle)
                .shadow(color: DNColor.blue800.opacity(0.18), radius: 10, y: 8)
                .scaleEffect(isAwake ? 1 : 0.6)
                .opacity(isAwake ? 1 : 0)

            DNLogo(height: 38)
                .scaleEffect(isAwake ? 1 : 0.74)
                .rotation3DEffect(.degrees(nod ? 6 : 0), axis: (x: 1, y: 0, z: 0), anchor: .center)
                .accessibilityHidden(true)
        }
        .frame(width: 160, height: 160)
        .onChange(of: state) { _, new in
            if new == .yourTurn && !reduceMotion {
                // One nod towards the button under your thumb.
                withAnimation(DNMotion.bouncy) { nod = true }
                Task { @MainActor in
                    try? await Task.sleep(for: .milliseconds(380))
                    withAnimation(DNMotion.bouncy) { nod = false }
                }
            }
            if new == .done && !reduceMotion {
                // The ring closes and glows once (0.6 s).
                withAnimation(.easeOut(duration: 0.3)) { glow = true }
                Task { @MainActor in
                    try? await Task.sleep(for: .milliseconds(320))
                    withAnimation(.easeIn(duration: 0.3)) { glow = false }
                }
            }
        }
        .accessibilityElement()
        .accessibilityLabel("DubaiNow assistant")
        .accessibilityValue(state.spoken)
    }

    @ViewBuilder private var ringView: some View {
        switch state {
        case .resting:
            EmptyView()
        case .awake, .listening:
            Circle()
                .fill(AngularGradient(colors: AssistantLight.ring, center: .center, angle: .degrees(200)))
                .shadow(color: AssistantLight.glow.opacity(state == .listening ? 0.38 : 0.3), radius: state == .listening ? 13 : 9)
                .shadow(color: DNColor.blue300.opacity(0.8), radius: 2)
        case .thinking:
            ZStack {
                Circle().fill(DNColor.blue100)
                if reduceMotion {
                    Circle().strokeBorder(DNColor.blue400, lineWidth: 3)
                } else {
                    AssistantComet()
                }
            }
            .shadow(color: AssistantLight.glow.opacity(0.22), radius: 8)
        case .working:
            // The ring fills as steps finish, matching the steps on the card.
            ZStack {
                Circle().fill(DNColor.blue100)
                Circle()
                    .trim(from: 0, to: max(0.02, progress))
                    .stroke(
                        AngularGradient(colors: [DNColor.blue400, DNColor.blue600, DNColor.blue700], center: .center),
                        style: StrokeStyle(lineWidth: 3.5, lineCap: .round)
                    )
                    .padding(1.75)
                    .rotationEffect(.degrees(-90))
            }
            .shadow(color: AssistantLight.glow.opacity(0.25), radius: 8)
        case .yourTurn:
            // The light gathers at the bottom, towards the button under your thumb.
            Circle()
                .fill(LinearGradient(colors: [DNColor.white.opacity(0.85), DNColor.blue200.opacity(0.85), DNColor.blue600], startPoint: .top, endPoint: .bottom))
                .shadow(color: AssistantLight.glow.opacity(0.36), radius: 9, y: 8)
        case .done:
            Circle()
                .fill(AngularGradient(colors: AssistantLight.ring, center: .center, angle: .degrees(200)))
                .shadow(color: AssistantLight.glow.opacity(glow ? 0.6 : 0.3), radius: glow ? 20 : 9)
        case .quiet:
            // Still, soft. No ripples, no glow.
            Circle().fill(DNColor.blue200.opacity(0.8))
        }
    }
}

/// Thinking: a single comet of light circles the ring (1 turn / 1.4 s, linear).
private struct AssistantComet: View {
    @State private var spin = false
    var body: some View {
        Circle()
            .trim(from: 0, to: 0.3)
            .stroke(
                AngularGradient(colors: [DNColor.blue600.opacity(0), DNColor.blue600], center: .center, startAngle: .degrees(0), endAngle: .degrees(108)),
                style: StrokeStyle(lineWidth: 3.5, lineCap: .round)
            )
            .padding(1.75)
            .rotationEffect(.degrees(spin ? 360 : 0))
            .onAppear {
                withAnimation(.linear(duration: 1.4).repeatForever(autoreverses: false)) { spin = true }
            }
    }
}

/// Listening: two ripples follow the voice level.
private struct AssistantRipples: View {
    var body: some View {
        TimelineView(.animation) { context in
            let t = context.date.timeIntervalSinceReferenceDate
            let level = 0.5 + 0.5 * abs(sin(t * 5.3) * sin(t * 2.1 + 0.6))
            let level2 = 0.5 + 0.5 * abs(sin(t * 3.7 + 1.1) * sin(t * 1.7))
            ZStack {
                Circle()
                    .fill(DNColor.blue300.opacity(0.06))
                    .overlay(Circle().strokeBorder(DNColor.blue600.opacity(0.16), lineWidth: 1))
                    .frame(width: 132, height: 132)
                    .scaleEffect(0.86 + 0.16 * level2)
                Circle()
                    .fill(DNColor.blue300.opacity(0.1))
                    .overlay(Circle().strokeBorder(DNColor.blue600.opacity(0.34), lineWidth: 1.5))
                    .frame(width: 98, height: 98)
                    .scaleEffect(0.9 + 0.14 * level)
            }
        }
        .accessibilityHidden(true)
    }
}
