import SwiftUI

/// Everything between the face and your screen: a soft veil, a wash of light from the logo,
/// the frost on your side (the bottom), and the edge light around the screen. Your screen
/// stays live underneath; this is the only change (Paper V83-0 · "Three zones, one light").
struct AssistantBackdrop: View {
    /// 1 awake · 0.55 your turn (the edge light calms) · 0 quiet.
    var edge: Double
    var showsFrost: Bool

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width, h = geo.size.height
            ZStack {
                // Veil, so the screen reads as "behind".
                DNColor.surfaceCanvas.opacity(0.22)
                DNColor.blue600.opacity(0.05)

                // A soft wash from the logo (ellipse 85% × 42% at the top).
                EllipticalGradient(
                    colors: [AssistantLight.halo.opacity(0.42), AssistantLight.halo.opacity(0)],
                    center: .center, startRadiusFraction: 0, endRadiusFraction: 0.5
                )
                .frame(width: w * 1.7, height: h * 0.84)
                .position(x: w / 2, y: 0)

                // Your side: frost behind the suggestions and the ask bar (the tab bar sits under it).
                if showsFrost {
                    ZStack {
                        Rectangle().fill(.ultraThinMaterial)
                        LinearGradient(
                            stops: [
                                .init(color: DNColor.surfaceCanvas.opacity(0.86), location: 0),
                                .init(color: DNColor.surfaceCanvas.opacity(0.66), location: 0.52),
                                .init(color: DNColor.surfaceCanvas.opacity(0), location: 1),
                            ],
                            startPoint: .bottom, endPoint: .top
                        )
                    }
                    .frame(height: 330)
                    .mask(LinearGradient(
                        stops: [.init(color: .black, location: 0), .init(color: .black, location: 0.56), .init(color: .clear, location: 1)],
                        startPoint: .bottom, endPoint: .top
                    ))
                    .frame(maxHeight: .infinity, alignment: .bottom)
                    .transition(.opacity)
                }

                AssistantEdgeLight(intensity: edge, size: geo.size)
            }
            .frame(width: w, height: h)
        }
        .ignoresSafeArea()
        .accessibilityHidden(true)
    }
}

/// The edge light: an angular blue gradient travelling round the screen edge, breathing ±8%
/// every 2.4 s, plus a fine 2 pt line. Reduce Motion: a still 2 pt line.
struct AssistantEdgeLight: View {
    var intensity: Double
    var size: CGSize

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var turn = false
    @State private var breathe = false

    /// iPhone display corners (Paper uses 56).
    private let corner: CGFloat = 56

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: corner, style: .continuous)
        let diagonal = (size.width * size.width + size.height * size.height).squareRoot()
        ZStack {
            if !reduceMotion {
                AngularGradient(colors: AssistantLight.ring, center: .center)
                    .frame(width: diagonal, height: diagonal)
                    .rotationEffect(.degrees(turn ? 360 : 0))
                    .frame(width: size.width, height: size.height)
                    .mask {
                        shape.strokeBorder(.black, lineWidth: 16).blur(radius: 9)
                    }
                    .opacity((breathe ? 1 : 0.84) * 0.95 * intensity)

                shape
                    .strokeBorder(AssistantLight.glow.opacity(0.3 * intensity), lineWidth: 8)
                    .blur(radius: 11)
            }
            if intensity > 0 || reduceMotion {
                shape.strokeBorder(AssistantLight.line.opacity(0.9), lineWidth: 2)
            }
        }
        .frame(width: size.width, height: size.height)
        .allowsHitTesting(false)
        .onAppear {
            guard !reduceMotion else { return }
            withAnimation(.linear(duration: 9).repeatForever(autoreverses: false)) { turn = true }
            withAnimation(.easeInOut(duration: 1.2).repeatForever(autoreverses: true)) { breathe = true }
        }
    }
}
