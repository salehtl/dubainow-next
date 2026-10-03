import SwiftUI

// Story heroes (Paper C1 · Welcome, "Story hero · …" frames). Each hero is drawn from real Canvas
// objects on Paper's 402-pt hero canvas, at Paper's positions and tilts, and scaled to fit the phone.
// Heroes are decorative: VoiceOver skips them (the story area is one adjustable element).

// MARK: - Canvas, placement and motion

/// Sara's sample figures used by the tour (rules board: "Its numbers are Sara's sample data").
enum WelcomeSample {
    static let dewaBill = Decimal(41260) / 100
    static let payAll = Decimal(101260) / 100
}

enum WelcomeHeroMetrics {
    /// Paper's hero frame is 402 pt wide; every object fits in the top 392 pt of it.
    static let size = CGSize(width: 402, height: 392)
}

/// The fixed-size canvas a hero is drawn on. Objects are placed at absolute positions.
struct WelcomeHeroCanvas<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        ZStack(alignment: .topLeading) { content }
            .frame(width: WelcomeHeroMetrics.size.width, height: WelcomeHeroMetrics.size.height, alignment: .topLeading)
    }
}

/// Places an object like Paper does (left/top, tilted about its top-left corner) and lets it rise into
/// place with the smooth spring, 40 ms after the one before, settling at its tilt (rules board · Motion).
/// Reduce Motion: no movement, just a 250 ms fade.
private struct WelcomePlaced: ViewModifier {
    var x: CGFloat
    var y: CGFloat
    var tilt: Double
    var order: Int
    var shown: Bool
    var rises: Bool
    var interactive: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        let settled = shown || reduceMotion || !rises
        content
            .fixedSize()
            .scaleEffect(settled ? 1 : 0.96, anchor: .topLeading)
            .rotationEffect(.degrees(settled ? tilt : tilt * 0.3), anchor: .topLeading)
            .offset(x: x, y: y + (settled ? 0 : 28))
            .opacity(shown ? 1 : 0)
            .animation(reduceMotion ? DNMotion.base : DNMotion.smooth.delay(Double(order) * 0.04), value: shown)
            .allowsHitTesting(interactive)
    }
}

extension View {
    /// Paper placement on the hero canvas. `order` sets the 40 ms rise stagger.
    fileprivate func welcomePlaced(x: CGFloat, y: CGFloat, tilt: Double = 0, order: Int, shown: Bool,
                                   rises: Bool = true, interactive: Bool = false) -> some View {
        modifier(WelcomePlaced(x: x, y: y, tilt: tilt, order: order, shown: shown, rises: rises, interactive: interactive))
    }

    /// Two-layer Canvas shadow tinted with the story's hue ink (near + far).
    fileprivate func welcomeShadow(_ tint: Color, near: Double = 0.08, far: Double, farRadius: CGFloat, farY: CGFloat) -> some View {
        self
            .shadow(color: tint.opacity(near), radius: 1, y: 1)
            .shadow(color: tint.opacity(far), radius: farRadius, y: farY)
    }
}

/// A slow, small float so the hero feels alive while the story plays. Off with Reduce Motion.
private struct WelcomeFloat: ViewModifier {
    var amplitude: CGFloat = 3
    var period: Double
    var phase: Double
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        if reduceMotion {
            content
        } else {
            TimelineView(.animation) { context in
                let t = context.date.timeIntervalSinceReferenceDate
                content.offset(y: amplitude * sin((t / period + phase) * 2 * .pi))
            }
        }
    }
}

/// The soft white glow behind every hero (radial, 85% → 0 at 70%).
private struct WelcomeGlow: View {
    var body: some View {
        Circle()
            .fill(RadialGradient(colors: [DNColor.white.opacity(0.85), DNColor.white.opacity(0)],
                                 center: .center, startRadius: 0, endRadius: 158))
            .frame(width: 320, height: 320)
    }
}

/// Shared "rise on appear" state for a hero.
private struct WelcomeAppear: ViewModifier {
    @Binding var shown: Bool
    func body(content: Content) -> some View {
        content.onAppear { shown = true }
    }
}

// MARK: - Shared Canvas objects

/// White chip on a hue (brief card status, "3 days left").
private struct WelcomeChip: View {
    var text: LocalizedStringKey
    var tone: Color = DNColor.textPrimary
    var dot = false

    var body: some View {
        HStack(spacing: 5) {
            if dot { Circle().fill(tone).frame(width: 7, height: 7) }
            Text(text)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(tone)
        }
        .padding(.horizontal, 10)
        .frame(height: 26)
        .background(DNColor.white, in: .capsule)
    }
}

/// The 32 pt glyph disc at the top of a brief card.
private struct WelcomeGlyphDisc: View {
    var symbol: String
    var ink: Color
    var body: some View {
        Circle()
            .fill(DNColor.posterGlyph)
            .frame(width: 32, height: 32)
            .overlay(Image(systemName: symbol).font(.system(size: 15, weight: .semibold)).foregroundStyle(ink))
    }
}

/// A Canvas tile: hue squircle with a top highlight and a glyph.
private struct WelcomeTile<Fill: ShapeStyle>: View {
    var symbol: String
    var size: CGFloat
    var radius: CGFloat
    var fill: Fill
    var ink: Color
    var glyph: CGFloat
    var shadowTint: Color

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: radius, style: .continuous)
        shape
            .fill(fill)
            .overlay(
                shape.strokeBorder(
                    LinearGradient(colors: [DNColor.white.opacity(0.6), .clear], startPoint: .top, endPoint: .center),
                    lineWidth: 1
                )
            )
            .overlay(Image(systemName: symbol).font(.system(size: glyph, weight: .semibold)).foregroundStyle(ink))
            .frame(width: size, height: size)
            .shadow(color: DNColor.elevationNear, radius: 1, y: 1)
            .shadow(color: shadowTint.opacity(0.12), radius: 12, y: 10)
    }
}

// MARK: - 1 · Welcome (sky): the orbit of service tiles around the app icon

struct WelcomeOrbitHero: View {
    @State private var shown = false

    var body: some View {
        WelcomeHeroCanvas {
            WelcomeGlow().welcomePlaced(x: 41, y: 25, order: 0, shown: shown, rises: false)
            WelcomeOrbitRings()
                .frame(width: 402, height: 371)
                .welcomePlaced(x: 0, y: 0, order: 0, shown: shown, rises: false)
            appIcon.welcomePlaced(x: 141, y: 125, order: 1, shown: shown)

            tile("p.square.fill", size: 68, radius: 18, fill: DNColor.surfacePrimary, ink: DNColor.actionAccent, glyph: 26, period: 4.2, phase: 0.0)
                .welcomePlaced(x: 35, y: 81, tilt: -8, order: 2, shown: shown)
            tile("road.lanes", size: 54, radius: 15, fill: Hue.sand.gradient, ink: Hue.sand.ink, glyph: 20, period: 3.6, phase: 0.3)
                .welcomePlaced(x: 200, y: 10, tilt: 6, order: 3, shown: shown)
            tile("doc.text", size: 62, radius: 17, fill: Hue.mint.gradient, ink: Hue.mint.ink, glyph: 23, period: 4.6, phase: 0.55)
                .welcomePlaced(x: 313, y: 108, tilt: 8, order: 4, shown: shown)
            tile("car.fill", size: 68, radius: 18, fill: Hue.rose.gradient, ink: Hue.rose.ink, glyph: 24, period: 3.9, phase: 0.15)
                .welcomePlaced(x: 282, y: 247, tilt: -5, order: 5, shown: shown)
            tile("bolt.fill", size: 52, radius: 14, fill: Hue.peach.gradient, ink: Hue.peach.ink, glyph: 20, period: 4.4, phase: 0.7)
                .welcomePlaced(x: 116, y: 297, tilt: 9, order: 6, shown: shown)
            tile("heart.fill", size: 58, radius: 16, fill: Hue.lilac.gradient, ink: Hue.lilac.ink, glyph: 22, period: 3.8, phase: 0.45)
                .welcomePlaced(x: 29, y: 202, tilt: -10, order: 7, shown: shown)
        }
        .modifier(WelcomeAppear(shown: $shown))
    }

    private var appIcon: some View {
        let shape = RoundedRectangle(cornerRadius: 34, style: .continuous)
        return shape
            .fill(LinearGradient(colors: [DNColor.white, DNColor.neutral50], startPoint: .top, endPoint: .bottom))
            .overlay(DNLogo(height: 70).accessibilityHidden(true))
            .frame(width: 120, height: 120)
            .welcomeShadow(Hue.sky.ink, near: 0.08, far: 0.16, farRadius: 20, farY: 18)
    }

    private func tile<F: ShapeStyle>(_ symbol: String, size: CGFloat, radius: CGFloat, fill: F, ink: Color,
                                     glyph: CGFloat, period: Double, phase: Double) -> some View {
        WelcomeTile(symbol: symbol, size: size, radius: radius, fill: fill, ink: ink, glyph: glyph, shadowTint: Hue.sky.ink)
            .modifier(WelcomeFloat(period: period, phase: phase))
    }
}

/// Two orbits around the app icon. Their dots drift slowly in opposite directions.
private struct WelcomeOrbitRings: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    private let center = CGPoint(x: 201, y: 185)

    var body: some View {
        TimelineView(.animation(paused: reduceMotion)) { context in
            let t = reduceMotion ? 0 : context.date.timeIntervalSinceReferenceDate
            Canvas { ctx, _ in
                let ring = DNColor.white.opacity(0.9)
                ctx.stroke(Path(ellipseIn: circle(r: 150)), with: .color(ring), lineWidth: 1.5)
                ctx.stroke(Path(ellipseIn: circle(r: 96)), with: .color(ring),
                           style: StrokeStyle(lineWidth: 1.5, lineCap: .round, dash: [2, 6]))

                // Outer orbit: top (ink) and right (white). Inner orbit: left (white) and right (ink).
                let outer = (t / 70).truncatingRemainder(dividingBy: 1) * 2 * .pi
                let inner = -(t / 50).truncatingRemainder(dividingBy: 1) * 2 * .pi
                dot(&ctx, r: 150, angle: -.pi / 2 + outer, size: 3, color: Hue.sky.ink)
                dot(&ctx, r: 150, angle: 0 + outer, size: 3, color: DNColor.white)
                dot(&ctx, r: 96, angle: .pi + inner, size: 3.5, color: DNColor.white)
                dot(&ctx, r: 96, angle: 0 + inner, size: 2.5, color: Hue.sky.ink)
            }
        }
    }

    private func circle(r: CGFloat) -> CGRect {
        CGRect(x: center.x - r, y: center.y - r, width: r * 2, height: r * 2)
    }

    private func dot(_ ctx: inout GraphicsContext, r: CGFloat, angle: Double, size: CGFloat, color: Color) {
        let p = CGPoint(x: center.x + r * cos(angle), y: center.y + r * sin(angle))
        ctx.fill(Path(ellipseIn: CGRect(x: p.x - size, y: p.y - size, width: size * 2, height: size * 2)), with: .color(color))
    }
}

// MARK: - 2 · Needs you (peach): brief cards and the Pay all pill

struct WelcomeNeedsYouHero: View {
    @State private var shown = false
    private let tint = Hue.peach.ink

    var body: some View {
        WelcomeHeroCanvas {
            WelcomeGlow().welcomePlaced(x: 41, y: 25, order: 0, shown: shown, rises: false)
            residencyCard.welcomePlaced(x: 12, y: 42, tilt: -8, order: 1, shown: shown)
            billCard.welcomePlaced(x: 108, y: 76, tilt: 4, order: 2, shown: shown)
            payAllPill.welcomePlaced(x: 30, y: 322, tilt: -3, order: 3, shown: shown)
        }
        .modifier(WelcomeAppear(shown: $shown))
    }

    private var residencyCard: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    HStack(spacing: 8) {
                        WelcomeGlyphDisc(symbol: "doc.text", ink: Hue.lilac.ink)
                        Text("Residency").font(.system(size: 13, weight: .semibold)).foregroundStyle(DNColor.textPrimary)
                    }
                    Spacer(minLength: 8)
                    WelcomeChip(text: "3 days left")
                }
                Spacer(minLength: 0)
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Text("3").font(.system(size: 34, weight: .bold, design: .rounded)).tracking(-0.5)
                    Text("of 5 steps").font(.system(size: 17, weight: .semibold, design: .rounded))
                }
                .foregroundStyle(DNColor.textPrimary)
            }
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 18)
            .frame(height: 120)
            .background(Hue.lilac.start)
            .overlay(alignment: .bottomLeading) {
                Rectangle().fill(DNColor.progressTrackOnHue).frame(height: 5)
                    .overlay(alignment: .leading) { Rectangle().fill(DNColor.actionAccent).frame(width: 109) }
            }

            VStack(alignment: .leading, spacing: 12) {
                cardTitle("Upload passport copy", subtitle: "Residence renewal · GDRFA")
                HStack(spacing: 6) {
                    Image(systemName: "play.fill").font(.system(size: 13, weight: .bold))
                    Text("Continue").font(.system(size: 17, weight: .semibold)).tracking(-0.15)
                }
                .foregroundStyle(DNColor.textOnAccent)
                .frame(maxWidth: .infinity, minHeight: 44)
                .background(DNColor.actionAccent, in: .capsule)
            }
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 16)
        }
        .frame(width: DNSize.briefCard)
        .background(DNColor.surfacePrimary)
        .clipShape(.rect(cornerRadius: DNRadius.brief, style: .continuous))
        .welcomeShadow(tint, near: 0.06, far: 0.14, farRadius: 18, farY: 16)
    }

    private var billCard: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    HStack(spacing: 8) {
                        WelcomeGlyphDisc(symbol: "bolt.fill", ink: Hue.sand.ink)
                        Text("DEWA").font(.system(size: 13, weight: .semibold)).foregroundStyle(DNColor.textPrimary)
                    }
                    Spacer(minLength: 8)
                    WelcomeChip(text: "Due tomorrow", tone: DNColor.statusWarningForeground, dot: true)
                }
                Spacer(minLength: 0)
                HStack(alignment: .firstTextBaseline, spacing: 5) {
                    Text("AED").font(.system(size: 17, weight: .semibold, design: .rounded))
                    Text(Money.figure(WelcomeSample.dewaBill)).font(.system(size: 34, weight: .bold, design: .rounded)).tracking(-0.5)
                }
                .foregroundStyle(DNColor.textPrimary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .frame(height: 120)
            .background(Hue.sand.start)

            VStack(alignment: .leading, spacing: 12) {
                cardTitle("Electricity & water bill", subtitle: "Al Barsha 1 · Account 2004 1187")
                HStack(spacing: 8) {
                    Text("Pay now")
                        .foregroundStyle(DNColor.textOnAccent)
                        .frame(maxWidth: .infinity, minHeight: 44)
                        .background(DNColor.actionAccent, in: .capsule)
                    Text("Later")
                        .foregroundStyle(DNColor.textLink)
                        .frame(width: 88, height: 44)
                        .background(DNColor.actionAccentSubtle, in: .capsule)
                }
                .font(.system(size: 17, weight: .semibold))
                .tracking(-0.15)
            }
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 16)
        }
        .frame(width: DNSize.briefCard)
        .background(DNColor.surfacePrimary)
        .clipShape(.rect(cornerRadius: DNRadius.brief, style: .continuous))
        .welcomeShadow(tint, near: 0.06, far: 0.18, farRadius: 22, farY: 20)
    }

    private func cardTitle(_ title: LocalizedStringKey, subtitle: LocalizedStringKey) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title).font(.system(size: 17, weight: .semibold)).tracking(-0.15).foregroundStyle(DNColor.textPrimary)
            Text(subtitle).font(.system(size: 15)).foregroundStyle(DNColor.textSecondary)
        }
        .lineLimit(1)
    }

    private var payAllPill: some View {
        HStack(spacing: 8) {
            HStack(spacing: -2.7) {
                Circle().fill(DNColor.white.opacity(0.5)).frame(width: 8, height: 8)
                Circle().fill(DNColor.white.opacity(0.78)).frame(width: 8, height: 8)
                Circle().fill(DNColor.white).frame(width: 8, height: 8)
            }
            Text("Pay all · \(Money.aed(WelcomeSample.payAll))")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(DNColor.textOnAccent)
        }
        .padding(.leading, 14)
        .padding(.trailing, 16)
        .frame(height: 40)
        .background(DNColor.actionAccent, in: .capsule)
        .welcomeShadow(DNColor.actionAccent, near: 0.18, far: 0.3, farRadius: 15, farY: 14)
    }
}

// MARK: - 3 · Your widgets (lilac): Salik lifted out of its slot

struct WelcomeWidgetsHero: View {
    @State private var shown = false

    var body: some View {
        WelcomeHeroCanvas {
            WelcomeGlow().welcomePlaced(x: 41, y: 25, order: 0, shown: shown, rises: false)
            emptySlot.welcomePlaced(x: 208, y: 28, order: 0, shown: shown, rises: false)
            WelcomePrayerWidget().welcomePlaced(x: 118, y: 204, tilt: -3, order: 3, shown: shown)
            WelcomeParkingWidget().welcomePlaced(x: 22, y: 20, tilt: -6, order: 1, shown: shown)
            WelcomeSalikWidget()
                .modifier(WelcomeFloat(amplitude: 2.5, period: 3.4, phase: 0.2))
                .welcomePlaced(x: 200, y: 4, tilt: 8, order: 2, shown: shown)
        }
        .modifier(WelcomeAppear(shown: $shown))
    }

    /// The dashed slot Salik was lifted out of.
    private var emptySlot: some View {
        RoundedRectangle(cornerRadius: DNRadius.widget, style: .continuous)
            .fill(DNColor.white.opacity(0.28))
            .overlay(
                RoundedRectangle(cornerRadius: DNRadius.widget, style: .continuous)
                    .strokeBorder(Hue.lilac.ink.opacity(0.22), style: StrokeStyle(lineWidth: 2, dash: [6, 4]))
            )
            .frame(width: DNSize.widgetUnit, height: DNSize.widgetUnit)
    }
}

private struct WelcomeParkingWidget: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                // Parking sign: blue plate, white inner rule, blue outer ring.
                RoundedRectangle(cornerRadius: 9, style: .continuous)
                    .fill(DNColor.signParking)
                    .overlay(RoundedRectangle(cornerRadius: 9, style: .continuous).strokeBorder(DNColor.white, lineWidth: 2))
                    .overlay(Text("P").font(.system(size: 22, weight: .heavy, design: .rounded)).foregroundStyle(DNColor.white))
                    .frame(width: 32, height: 32)
                    .padding(1)
                    .background(DNColor.signParking, in: .rect(cornerRadius: 10, style: .continuous))
                Spacer()
                Circle()
                    .fill(DNColor.actionAccentSubtle)
                    .overlay(Image(systemName: "plus").font(.system(size: 14, weight: .bold)).foregroundStyle(DNColor.actionAccent))
                    .frame(width: 34, height: 34)
                    .frame(width: 44, height: 44)
            }
            .frame(height: 44)

            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text("42").font(.system(size: 44, weight: .bold, design: .rounded)).tracking(-0.8)
                Text("min").font(.system(size: 17, weight: .semibold, design: .rounded))
            }
            .foregroundStyle(DNColor.textPrimary)
            .padding(.top, 6)
            .frame(height: 52, alignment: .bottom)

            Text("left in Zone 365C")
                .font(.system(size: 13))
                .foregroundStyle(DNColor.textSecondary)
                .padding(.bottom, 12)

            Capsule().fill(DNColor.neutral200)
                .frame(width: 143, height: 6)
                .overlay(alignment: .leading) { Capsule().fill(DNColor.signParking).frame(width: 50, height: 6) }
        }
        .padding(.top, 12)
        .padding(.leading, 16)
        .padding(.trailing, 12)
        .padding(.bottom, 16)
        .frame(width: DNSize.widgetUnit, height: DNSize.widgetUnit, alignment: .top)
        .background(DNColor.widgetSurface, in: .rect(cornerRadius: DNRadius.widget, style: .continuous))
        .shadow(color: DNColor.elevationNear, radius: 1, y: 1)
        .shadow(color: DNColor.elevationFar, radius: 12, y: 8)
    }
}

private struct WelcomeSalikWidget: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Circle()
                    .fill(DNColor.white.opacity(0.12))
                    .overlay(Image(systemName: "road.lanes").font(.system(size: 16, weight: .semibold)).foregroundStyle(DNColor.widgetOnInverse))
                    .frame(width: 34, height: 34)
                Spacer()
                Circle()
                    .fill(DNColor.widgetOnInverseTrack)
                    .overlay(Image(systemName: "plus").font(.system(size: 14, weight: .bold)).foregroundStyle(DNColor.widgetOnInverse))
                    .frame(width: 34, height: 34)
                    .frame(width: 44, height: 44)
            }
            .frame(height: 44)

            HStack(alignment: .firstTextBaseline, spacing: 5) {
                Text("AED").font(.system(size: 17, weight: .semibold, design: .rounded)).foregroundStyle(DNColor.white.opacity(0.85))
                Text("18").font(.system(size: 44, weight: .bold, design: .rounded)).tracking(-0.8).foregroundStyle(DNColor.widgetOnInverse)
            }
            .padding(.top, 6)
            .frame(height: 52, alignment: .bottom)

            HStack(spacing: 6) {
                Circle().fill(DNColor.signLow).frame(width: 7, height: 7)
                Text("Low · about 4 trips").font(.system(size: 13, weight: .semibold)).foregroundStyle(DNColor.signLow)
            }
            .padding(.bottom, 12)

            HStack(spacing: 3) {
                ForEach(0..<10, id: \.self) { i in
                    Capsule().fill(i < 4 ? DNColor.signLow : DNColor.widgetOnInverseTrack).frame(height: 6)
                }
            }
            .frame(width: 143)
        }
        .padding(.top, 12)
        .padding(.leading, 16)
        .padding(.trailing, 12)
        .padding(.bottom, 16)
        .frame(width: DNSize.widgetUnit, height: DNSize.widgetUnit, alignment: .top)
        .background {
            ZStack {
                DNColor.widgetSurfaceInverse
                // A faint road running under the balance.
                Path { p in
                    p.move(to: CGPoint(x: 118, y: 175))
                    p.addLine(to: CGPoint(x: 150, y: 92))
                }
                .stroke(DNColor.white.opacity(0.07), style: StrokeStyle(lineWidth: 10, dash: [16, 14]))
            }
        }
        .clipShape(.rect(cornerRadius: DNRadius.widget, style: .continuous))
        .shadow(color: DNColor.shadowLight, radius: 2, y: 2)
        .shadow(color: Hue.lilac.ink.opacity(0.32), radius: 24, y: 24)
    }
}

private struct WelcomePrayerWidget: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    HStack(spacing: 6) {
                        Image(systemName: "moon.fill").font(.system(size: 13, weight: .semibold)).foregroundStyle(DNColor.widgetSkyGlyph)
                        Text("Dhuhr").font(.system(size: 15, weight: .semibold)).foregroundStyle(DNColor.textPrimary)
                    }
                    Spacer()
                    Text("in 2h 31m").font(.system(size: 13, weight: .medium)).foregroundStyle(DNColor.textOnHueSecondary)
                }
                Text("12:12").font(.system(size: 40, weight: .bold, design: .rounded)).tracking(-0.8).foregroundStyle(DNColor.textPrimary)
            }
            Spacer(minLength: 0)
            WelcomeSunArc().frame(width: 143, height: 52)
        }
        .padding(.top, 16)
        .padding(.horizontal, 16)
        .padding(.bottom, 14)
        .frame(width: DNSize.widgetUnit, height: DNSize.widgetUnit)
        .background(
            LinearGradient(colors: [DNColor.widgetSkyTop, DNColor.widgetSkyBottom], startPoint: .top, endPoint: .bottom),
            in: .rect(cornerRadius: DNRadius.widget, style: .continuous)
        )
        .shadow(color: DNColor.elevationNear, radius: 1, y: 1)
        .shadow(color: DNColor.elevationFar, radius: 12, y: 8)
    }
}

/// The sun's path across the sky in the prayer widget (143 × 52).
private struct WelcomeSunArc: View {
    var body: some View {
        Canvas { ctx, _ in
            func point(_ degrees: Double) -> CGPoint {
                let a = degrees * .pi / 180
                return CGPoint(x: 71.5 + 66 * cos(a), y: 46 - 40 * sin(a))
            }
            func arc(from: Double, to: Double) -> Path {
                var p = Path()
                p.move(to: point(from))
                for d in stride(from: from, through: to, by: -2) { p.addLine(to: point(d)) }
                p.addLine(to: point(to))
                return p
            }
            var horizon = Path()
            horizon.move(to: CGPoint(x: 0, y: 46.5))
            horizon.addLine(to: CGPoint(x: 143, y: 46.5))
            ctx.stroke(horizon, with: .color(DNColor.widgetSkyHorizon), lineWidth: 1)
            ctx.stroke(arc(from: 180, to: 0), with: .color(DNColor.widgetSkyPath),
                       style: StrokeStyle(lineWidth: 1.6, lineCap: .round, dash: [2, 4]))
            ctx.stroke(arc(from: 180, to: 127.8), with: .color(DNColor.signSun),
                       style: StrokeStyle(lineWidth: 2.4, lineCap: .round))

            let marker = CGRect(x: 72.2 - 3.6, y: 6 - 3.6, width: 7.2, height: 7.2)
            ctx.fill(Path(ellipseIn: marker), with: .color(DNColor.white))
            ctx.stroke(Path(ellipseIn: marker), with: .color(DNColor.widgetSkyGlyph), lineWidth: 1.8)
            for c in [CGPoint(x: 123.4, y: 21.3), CGPoint(x: 137.5, y: 46)] {
                ctx.fill(Path(ellipseIn: CGRect(x: c.x - 2.6, y: c.y - 2.6, width: 5.2, height: 5.2)), with: .color(DNColor.widgetSkyMarker))
            }
            let sun = CGPoint(x: 31.05, y: 14.4)
            ctx.fill(Path(ellipseIn: CGRect(x: sun.x - 11, y: sun.y - 11, width: 22, height: 22)), with: .color(DNColor.widgetSunGlow))
            ctx.fill(Path(ellipseIn: CGRect(x: sun.x - 6.5, y: sun.y - 6.5, width: 13, height: 13)), with: .color(DNColor.widgetSun))
        }
    }
}

// MARK: - 4 · Start to finish (sand): Build a house, 7 stages

struct WelcomeJourneyHero: View {
    @State private var shown = false
    private let ink = Hue.sand.ink

    var body: some View {
        WelcomeHeroCanvas {
            WelcomeGlow().welcomePlaced(x: 41, y: 60, order: 0, shown: shown, rises: false)
            WelcomeJourneyRoute(drawn: shown)
                .frame(width: 402, height: 371)
                .welcomePlaced(x: 0, y: 18, order: 0, shown: shown, rises: false)
            moveInTile.welcomePlaced(x: 326, y: 222, order: 4, shown: shown)
            card.welcomePlaced(x: 74, y: 72, order: 1, shown: shown)
        }
        .modifier(WelcomeAppear(shown: $shown))
    }

    private var moveInTile: some View {
        let shape = RoundedRectangle(cornerRadius: 17, style: .continuous)
        return shape
            .fill(LinearGradient(colors: [DNColor.white, Hue.sand.start], startPoint: CSSAngle.start(160), endPoint: CSSAngle.end(160)))
            .overlay(shape.strokeBorder(LinearGradient(colors: [DNColor.white.opacity(0.6), .clear], startPoint: .top, endPoint: .center), lineWidth: 1))
            .overlay(Image(systemName: "house.fill").font(.system(size: 24, weight: .semibold)).foregroundStyle(ink))
            .frame(width: 60, height: 60)
            .welcomeShadow(ink, near: 0.08, far: 0.18, farRadius: 13, farY: 12)
    }

    private var card: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Circle()
                    .fill(Hue.sand.start)
                    .overlay(RouteMark(color: ink, size: 16))
                    .frame(width: 28, height: 28)
                Text("Build a house")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(DNColor.textPrimary)
                    .lineLimit(1)
                    .fixedSize()
                Spacer(minLength: 4)
                Text("Your turn")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(ink)
                    .lineLimit(1)
                    .fixedSize()
                    .padding(.horizontal, 10)
                    .frame(height: 24)
                    .background(Hue.sand.start, in: .capsule)
            }
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text("4").font(.system(size: 34, weight: .bold, design: .rounded))
                Text("of 7 · Design").font(.system(size: 17, weight: .semibold))
            }
            .foregroundStyle(DNColor.textPrimary)
        }
        .padding(.horizontal, 16)
        .padding(.top, 14)
        .padding(.bottom, 16)
        .frame(minWidth: 244)
        .background(DNColor.surfacePrimary, in: .rect(cornerRadius: 22, style: .continuous))
        .welcomeShadow(ink, near: 0.08, far: 0.16, farRadius: 18, farY: 16)
    }
}

/// The Build a house route (Paper path, 402 × 371): 3 stages done, "Design" now, 3 ahead.
private struct WelcomeJourneyRoute: View {
    var drawn: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    private let ink = Hue.sand.ink

    private let done: [CGPoint] = [CGPoint(x: 34, y: 300), CGPoint(x: 86, y: 262), CGPoint(x: 140, y: 286)]
    private let current = CGPoint(x: 196, y: 240)
    private let ahead: [CGPoint] = [CGPoint(x: 252, y: 262), CGPoint(x: 304, y: 222)]

    var body: some View {
        ZStack(alignment: .topLeading) {
            WelcomeRouteShape(part: .whole)
                .stroke(DNColor.white.opacity(0.7), style: StrokeStyle(lineWidth: 18, lineCap: .round))
            WelcomeRouteShape(part: .ahead)
                .stroke(ink, style: StrokeStyle(lineWidth: 3, lineCap: .round, dash: [0.1, 8]))
            WelcomeRouteShape(part: .done)
                .trim(from: 0, to: drawn || reduceMotion ? 1 : 0)
                .stroke(ink, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                .animation(reduceMotion ? nil : .easeOut(duration: 0.7).delay(0.12), value: drawn)
            Path { p in
                p.move(to: CGPoint(x: 196, y: 186))
                p.addLine(to: CGPoint(x: 196, y: 222))
            }
            .stroke(ink, style: StrokeStyle(lineWidth: 2, lineCap: .round, dash: [0.1, 5]))

            ForEach(done.indices, id: \.self) { i in
                Circle().fill(ink)
                    .overlay(Image(systemName: "checkmark").font(.system(size: 10, weight: .heavy)).foregroundStyle(DNColor.white))
                    .frame(width: 20, height: 20)
                    .position(done[i])
            }
            WelcomeCurrentStop(ink: ink).position(current)
            ForEach(ahead.indices, id: \.self) { i in
                Circle().fill(DNColor.white)
                    .overlay(Circle().stroke(ink, lineWidth: 2))
                    .frame(width: 14, height: 14)
                    .position(ahead[i])
            }

            label("Plan", x: 2, y: 318, done: true)
            label("Land", x: 54, y: 230, done: true)
            label("Money", x: 108, y: 304, done: true)
            label("Permits", x: 220, y: 278, done: false)
            label("Build", x: 272, y: 192, done: false)
            label("Move in", x: 324, y: 272, done: false)
        }
        .frame(width: 402, height: 371, alignment: .topLeading)
    }

    private func label(_ text: LocalizedStringKey, x: CGFloat, y: CGFloat, done: Bool) -> some View {
        Text(text)
            .font(.system(size: 13, weight: done ? .semibold : .medium))
            .foregroundStyle(done ? ink : DNColor.textOnWashSecondary)
            .lineLimit(1)
            .fixedSize()
            .frame(width: 64)
            .offset(x: x, y: y)
    }
}

/// The stage you're on: a white halo that breathes gently, ringed in the hue ink.
private struct WelcomeCurrentStop: View {
    var ink: Color
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(paused: reduceMotion)) { context in
            let t = reduceMotion ? 0 : context.date.timeIntervalSinceReferenceDate
            let breathe = 1 + 0.12 * (0.5 + 0.5 * sin(t * 2 * .pi / 2.4))
            ZStack {
                Circle().fill(DNColor.white.opacity(0.55)).frame(width: 44, height: 44).scaleEffect(breathe)
                Circle().fill(DNColor.white).frame(width: 24, height: 24)
                Circle().stroke(ink, lineWidth: 3.5).frame(width: 24, height: 24)
                Circle().fill(ink).frame(width: 9, height: 9)
            }
        }
        .frame(width: 52, height: 52)
    }
}

private struct WelcomeRouteShape: Shape {
    enum Part { case whole, done, ahead }
    var part: Part

    private typealias Segment = (end: CGPoint, c1: CGPoint, c2: CGPoint)
    private static let start = CGPoint(x: 34, y: 300)
    private static let segments: [Segment] = [
        (CGPoint(x: 86, y: 262), CGPoint(x: 42.7, y: 293.7), CGPoint(x: 68.3, y: 264.3)),
        (CGPoint(x: 140, y: 286), CGPoint(x: 103.7, y: 259.7), CGPoint(x: 121.7, y: 289.7)),
        (CGPoint(x: 196, y: 240), CGPoint(x: 158.3, y: 282.3), CGPoint(x: 177.3, y: 244.0)),
        (CGPoint(x: 252, y: 262), CGPoint(x: 214.7, y: 236.0), CGPoint(x: 234.0, y: 265.0)),
        (CGPoint(x: 304, y: 222), CGPoint(x: 270.0, y: 259.0), CGPoint(x: 286.7, y: 226.3)),
        (CGPoint(x: 356, y: 236), CGPoint(x: 321.3, y: 217.7), CGPoint(x: 347.3, y: 233.7)),
    ]

    func path(in rect: CGRect) -> Path {
        let range: Range<Int> = switch part {
        case .whole: 0..<6
        case .done: 0..<3
        case .ahead: 3..<6
        }
        var p = Path()
        p.move(to: range.lowerBound == 0 ? Self.start : Self.segments[range.lowerBound - 1].end)
        for i in range {
            let s = Self.segments[i]
            p.addCurve(to: s.end, control1: s.c1, control2: s.c2)
        }
        return p
    }
}

// MARK: - 5 · Search (mint): the answer card and an alias chip

struct WelcomeSearchHero: View {
    @State private var shown = false
    private let tint = Hue.mint.ink

    var body: some View {
        WelcomeHeroCanvas {
            WelcomeGlow().welcomePlaced(x: 41, y: 25, order: 0, shown: shown, rises: false)
            answerCard.welcomePlaced(x: 40, y: 102, tilt: 2, order: 2, shown: shown)
            searchField.welcomePlaced(x: 28, y: 30, tilt: -2, order: 1, shown: shown)
            aliasChip.welcomePlaced(x: 52, y: 342, tilt: -4, order: 3, shown: shown)
        }
        .modifier(WelcomeAppear(shown: $shown))
    }

    /// A picture of the search field (not glass: heroes are content).
    private var searchField: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass").font(.system(size: 17, weight: .semibold)).foregroundStyle(DNColor.textSecondary)
            HStack(spacing: 1) {
                Text("how much do I owe").font(.system(size: 17)).foregroundStyle(DNColor.textPrimary)
                WelcomeCaret()
            }
            Spacer(minLength: 0)
            Image(systemName: "xmark.circle.fill").font(.system(size: 18)).foregroundStyle(DNColor.neutral400).frame(width: 36, height: 36)
            Image(systemName: "mic.fill").font(.system(size: 17)).foregroundStyle(DNColor.textSecondary).frame(width: 36, height: 36)
        }
        .padding(.leading, 18)
        .padding(.trailing, 10)
        .frame(width: 346, height: 56)
        .background(DNColor.surfacePrimary.opacity(0.78), in: .capsule)
        .overlay(Capsule().strokeBorder(DNColor.white, lineWidth: 1))
        .shadow(color: DNColor.elevationNear, radius: 1, y: 1)
        .shadow(color: tint.opacity(0.1), radius: 12, y: 8)
    }

    private var answerCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                HStack(spacing: 6) {
                    Image(systemName: "lock").font(.system(size: 11, weight: .bold))
                    Text("From your accounts").font(.system(size: 13, weight: .semibold)).tracking(0.8).textCase(.uppercase)
                }
                Spacer(minLength: 8)
                Text("Updated 9:41").font(.system(size: 13))
            }
            .foregroundStyle(DNColor.textSecondary)

            VStack(alignment: .leading, spacing: 2) {
                Text("You owe").font(.system(size: 17, weight: .semibold)).foregroundStyle(DNColor.textPrimary)
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Text("AED").font(.system(size: 17, weight: .semibold, design: .rounded))
                    Text(Money.figure(WelcomeSample.payAll)).font(.system(size: 44, weight: .bold, design: .rounded)).tracking(-0.8)
                }
                .foregroundStyle(DNColor.textPrimary)
                Text("across 1 bill and 1 fine").font(.system(size: 15)).foregroundStyle(DNColor.textSecondary)
            }

            Text("Pay both · \(Money.aed(WelcomeSample.payAll))")
                .font(.system(size: 17, weight: .semibold))
                .tracking(-0.15)
                .foregroundStyle(DNColor.textOnAccent)
                .frame(maxWidth: .infinity, minHeight: DNSpace.buttonHeightLarge)
                .background(DNColor.actionAccent, in: .capsule)
        }
        .padding(18)
        .frame(width: 322)
        .background(DNColor.surfacePrimary, in: .rect(cornerRadius: DNRadius.widget, style: .continuous))
        .welcomeShadow(tint, near: 0.08, far: 0.16, farRadius: 20, farY: 18)
    }

    private var aliasChip: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass").font(.system(size: 13, weight: .semibold)).foregroundStyle(DNColor.textSecondary)
            Text("amer").font(.system(size: 15)).foregroundStyle(DNColor.textSecondary)
            Image(systemName: "arrow.right").font(.system(size: 13, weight: .bold)).foregroundStyle(tint)
            Text("Renew your residence visa").font(.system(size: 15, weight: .semibold)).foregroundStyle(DNColor.textPrimary)
        }
        .padding(.leading, 12)
        .padding(.trailing, 16)
        .frame(height: 40)
        .background(DNColor.surfacePrimary, in: .capsule)
        .welcomeShadow(tint, near: 0.08, far: 0.18, farRadius: 14, farY: 12)
    }
}

/// The text caret in the pictured search field, blinking like the real one.
private struct WelcomeCaret: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.periodic(from: .now, by: 0.53)) { context in
            let on = reduceMotion || Int(context.date.timeIntervalSinceReferenceDate / 0.53).isMultiple(of: 2)
            Capsule().fill(DNColor.actionAccent).frame(width: 2, height: 22).opacity(on ? 1 : 0)
        }
    }
}

// MARK: - 6 · Announcement (rose): Dubai Fitness Challenge 30×30 poster

struct WelcomePosterHero: View {
    /// The link sticker ("See events") — the one tappable thing in a story.
    var onLink: () -> Void
    @State private var shown = false
    private let ink = Hue.rose.ink

    var body: some View {
        WelcomeHeroCanvas {
            WelcomeGlow().welcomePlaced(x: 41, y: 25, order: 0, shown: shown, rises: false)
            track.welcomePlaced(x: 22, y: 66, order: 0, shown: shown, rises: false)
            numbers.frame(width: 402).welcomePlaced(x: 0, y: 108, order: 1, shown: shown)
            freeBadge
                .modifier(WelcomeFloat(amplitude: 2.5, period: 3.2, phase: 0))
                .welcomePlaced(x: 298, y: 34, tilt: 12, order: 2, shown: shown)
            linkSticker.welcomePlaced(x: 124, y: 306, tilt: -3, order: 3, shown: shown, interactive: true)
        }
        .modifier(WelcomeAppear(shown: $shown))
    }

    /// A running track, three lanes, tilted −7°.
    private var track: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 114, style: .circular)
                .stroke(DNColor.white.opacity(0.85), lineWidth: 2)
                .frame(width: 358, height: 228)
            RoundedRectangle(cornerRadius: 94, style: .circular)
                .stroke(DNColor.white.opacity(0.85), style: StrokeStyle(lineWidth: 1.5, lineCap: .round, dash: [1, 7]))
                .frame(width: 318, height: 188)
            RoundedRectangle(cornerRadius: 74, style: .circular)
                .stroke(DNColor.white.opacity(0.6), lineWidth: 1.5)
                .frame(width: 278, height: 148)
        }
        .frame(width: 358, height: 228)
        .rotationEffect(.degrees(-7))
    }

    private var numbers: some View {
        HStack(alignment: .top, spacing: 4) {
            figure(label: "Minutes")
            Text(verbatim: "×")
                .font(.system(size: 64, weight: .regular, design: .rounded))
                .frame(height: 112)
            figure(label: "Days")
        }
        .foregroundStyle(ink)
    }

    private func figure(label: LocalizedStringKey) -> some View {
        VStack(spacing: 2) {
            Text(verbatim: "30")
                .font(.system(size: 112, weight: .heavy, design: .rounded))
                .tracking(-4.5)
                .frame(height: 112)
            Text(label)
                .font(.system(size: 13, weight: .bold))
                .tracking(0.8)
                .textCase(.uppercase)
        }
    }

    private var freeBadge: some View {
        Circle()
            .fill(ink)
            .overlay(Text("Free").font(.system(size: 17, weight: .bold)).foregroundStyle(DNColor.white))
            .frame(width: 68, height: 68)
            .shadow(color: ink.opacity(0.24), radius: 12, y: 10)
    }

    private var linkSticker: some View {
        Button(action: onLink) {
            HStack(spacing: 8) {
                Image(systemName: "link").font(.system(size: 15, weight: .semibold))
                Text("See events").font(.system(size: 15, weight: .semibold))
            }
            .foregroundStyle(DNColor.actionAccent)
            .padding(.leading, 14)
            .padding(.trailing, 18)
            .frame(height: 44)
            .background(DNColor.surfacePrimary, in: .capsule)
            .contentShape(.capsule)
            .welcomeShadow(ink, near: 0.08, far: 0.18, farRadius: 14, farY: 12)
        }
        .buttonStyle(.dnPressable)
    }
}
