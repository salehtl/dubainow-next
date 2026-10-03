import SwiftUI

/// The six Canvas hues. A service's hue belongs to its record and defaults to its topic.
/// Each hue has a start and end (for gradients) and an ink (for glyphs and text on the hue).
enum Hue: String, CaseIterable, Codable, Hashable {
    case sky, sand, rose, lilac, mint, peach

    var start: Color {
        switch self {
        case .sky: DNColor.hueSkyStart
        case .sand: DNColor.hueSandStart
        case .rose: DNColor.hueRoseStart
        case .lilac: DNColor.hueLilacStart
        case .mint: DNColor.hueMintStart
        case .peach: DNColor.huePeachStart
        }
    }

    var end: Color {
        switch self {
        case .sky: DNColor.hueSkyEnd
        case .sand: DNColor.hueSandEnd
        case .rose: DNColor.hueRoseEnd
        case .lilac: DNColor.hueLilacEnd
        case .mint: DNColor.hueMintEnd
        case .peach: DNColor.huePeachEnd
        }
    }

    var ink: Color {
        switch self {
        case .sky: DNColor.hueSkyInk
        case .sand: DNColor.hueSandInk
        case .rose: DNColor.hueRoseInk
        case .lilac: DNColor.hueLilacInk
        case .mint: DNColor.hueMintInk
        case .peach: DNColor.huePeachInk
        }
    }

    /// Light-mode RGB of start/end, used to build the OKLab gradient stops.
    private var lightRGB: (start: (Double, Double, Double), end: (Double, Double, Double)) {
        switch self {
        case .sky: ((0xE4, 0xEE, 0xFF), (0xCF, 0xDD, 0xFB))
        case .sand: ((0xFF, 0xF0, 0xDC), (0xFB, 0xDD, 0xB8))
        case .rose: ((0xFB, 0xE6, 0xE4), (0xF4, 0xCB, 0xC6))
        case .lilac: ((0xEF, 0xE7, 0xFA), (0xDC, 0xCF, 0xF3))
        case .mint: ((0xE1, 0xF5, 0xEA), (0xC2, 0xE7, 0xD6))
        case .peach: ((0xFF, 0xE9, 0xD9), (0xFF, 0xCE, 0xB5))
        }
    }

    /// Paper: `linear-gradient(in oklab 160deg, start, end)` (200° in Arabic).
    /// SwiftUI interpolates in sRGB, so intermediate stops are computed in OKLab.
    var gradient: LinearGradient {
        LinearGradient(
            stops: OKLab.stops(from: lightRGB.start, to: lightRGB.end, count: 6),
            startPoint: CSSAngle.start(160),
            endPoint: CSSAngle.end(160)
        )
    }

    /// A flat wash of the hue, for large surfaces (journey cards, hero areas).
    var wash: LinearGradient {
        LinearGradient(colors: [start, start.opacity(0.6)], startPoint: .top, endPoint: .bottom)
    }
}

/// Converts a CSS gradient angle (0° = to top, clockwise) into SwiftUI unit points,
/// using the CSS rule that the gradient line reaches the corners of a square box.
enum CSSAngle {
    static func start(_ degrees: Double) -> UnitPoint {
        let (dx, dy, half) = vector(degrees)
        return UnitPoint(x: 0.5 - dx * half, y: 0.5 - dy * half)
    }

    static func end(_ degrees: Double) -> UnitPoint {
        let (dx, dy, half) = vector(degrees)
        return UnitPoint(x: 0.5 + dx * half, y: 0.5 + dy * half)
    }

    private static func vector(_ degrees: Double) -> (Double, Double, Double) {
        let r = degrees * .pi / 180
        let dx = sin(r), dy = -cos(r)
        let half = (abs(sin(r)) + abs(cos(r))) / 2
        return (dx, dy, half)
    }
}

/// Minimal OKLab maths (Björn Ottosson) for perceptual gradient stops.
enum OKLab {
    static func stops(from a: (Double, Double, Double), to b: (Double, Double, Double), count: Int) -> [Gradient.Stop] {
        let la = toLab(a), lb = toLab(b)
        return (0..<count).map { i in
            let t = Double(i) / Double(count - 1)
            let l = (la.0 + (lb.0 - la.0) * t, la.1 + (lb.1 - la.1) * t, la.2 + (lb.2 - la.2) * t)
            let rgb = toRGB(l)
            return Gradient.Stop(color: Color(.sRGB, red: rgb.0, green: rgb.1, blue: rgb.2, opacity: 1), location: t)
        }
    }

    private static func lin(_ c: Double) -> Double {
        let v = c / 255
        return v <= 0.04045 ? v / 12.92 : pow((v + 0.055) / 1.055, 2.4)
    }

    private static func gam(_ v: Double) -> Double {
        let c = v <= 0.0031308 ? 12.92 * v : 1.055 * pow(v, 1 / 2.4) - 0.055
        return min(max(c, 0), 1)
    }

    private static func toLab(_ c: (Double, Double, Double)) -> (Double, Double, Double) {
        let r = lin(c.0), g = lin(c.1), b = lin(c.2)
        let l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        let m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        let s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        return (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
                1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
                0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s)
    }

    private static func toRGB(_ lab: (Double, Double, Double)) -> (Double, Double, Double) {
        let l_ = lab.0 + 0.3963377774 * lab.1 + 0.2158037573 * lab.2
        let m_ = lab.0 - 0.1055613458 * lab.1 - 0.0638541728 * lab.2
        let s_ = lab.0 - 0.0894841775 * lab.1 - 1.2914855480 * lab.2
        let l = l_ * l_ * l_, m = m_ * m_ * m_, s = s_ * s_ * s_
        return (gam(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s),
                gam(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
                gam(-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s))
    }
}

/// The Home wash changes with the time of day (C1 · Design system changes).
/// morning 05–11 · midday 11–16 · evening 16–20 · night 20–05. It fades into the canvas over 420 pt.
enum TimeOfDay: String, CaseIterable {
    case morning, midday, evening, night

    init(date: Date) {
        let hour = Calendar.current.component(.hour, from: date)
        switch hour {
        case 5..<11: self = .morning
        case 11..<16: self = .midday
        case 16..<20: self = .evening
        default: self = .night
        }
    }

    var wash: Color {
        switch self {
        case .morning: DNColor.washMorning
        case .midday: DNColor.washMidday
        case .evening: DNColor.washEvening
        case .night: DNColor.washNight
        }
    }

    var greeting: String {
        switch self {
        case .morning: "Good morning"
        case .midday: "Good afternoon"
        case .evening: "Good evening"
        case .night: "Good evening"
        }
    }
}

/// The ambient wash behind the top of Home (and other tab roots). 420 pt tall.
struct AmbientWash: View {
    var timeOfDay: TimeOfDay
    var height: CGFloat = 420

    var body: some View {
        LinearGradient(
            colors: [timeOfDay.wash, timeOfDay.wash.opacity(0.55), DNColor.surfaceSecondary.opacity(0)],
            startPoint: .top,
            endPoint: .bottom
        )
        .frame(height: height)
        .frame(maxWidth: .infinity)
        .ignoresSafeArea(edges: .top)
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}
