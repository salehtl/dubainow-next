import SwiftUI

// Faith and family: Prayer times (small, large). City life: Weather and beach (small).

// MARK: - Prayer times

struct WidgetPrayerModel {
    struct Slot: Identifiable {
        var id: String { name }
        var name: String
        var date: Date
    }

    let now: Date
    let next: (prayer: PrayerTimes.Prayer, date: Date)

    init(now: Date) {
        self.now = now
        self.next = PrayerTimes.next(after: now)
    }

    func date(_ p: PrayerTimes.Prayer) -> Date {
        Calendar.current.date(bySettingHour: p.hour, minute: p.minute, second: 0, of: now) ?? now
    }

    /// The five prayers (no Sunrise) for the row in the large size.
    var prayers: [Slot] {
        PrayerTimes.today.filter { $0.name != "Sunrise" }.map { Slot(name: $0.name, date: date($0)) }
    }

    var sunrise: Date { PrayerTimes.today.first { $0.name == "Sunrise" }.map(date) ?? now }
    var sunset: Date { PrayerTimes.today.first { $0.name == "Maghrib" }.map(date) ?? now }

    /// 0 at sunrise, 1 at Maghrib; nil at night.
    func dayFraction(_ d: Date) -> Double? {
        let span = sunset.timeIntervalSince(sunrise)
        guard span > 0 else { return nil }
        let t = d.timeIntervalSince(sunrise) / span
        return (0...1).contains(t) ? t : nil
    }

    var countdown: String { DemoDate.countdown(to: next.date, from: now) }
    var minutesToNext: Int { max(0, Int(next.date.timeIntervalSince(now) / 60)) }
    var clock: String { WidgetFormat.clock(hour: next.prayer.hour, minute: next.prayer.minute) }
}

struct WidgetPrayerContent: View {
    var size: WidgetSize
    var now: Date
    @Environment(DemoStore.self) private var store

    var body: some View {
        let m = WidgetPrayerModel(now: now)
        if size == .large { large(m) } else { small(m) }
    }

    private var moon: some View {
        Image(systemName: "moon.fill")
            .font(.system(size: 13, weight: .semibold))
            .foregroundStyle(DNColor.widgetSkyGlyph)
    }

    private func small(_ m: WidgetPrayerModel) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 6) {
                moon
                WidgetTitle(m.next.prayer.name)
                Spacer(minLength: 4)
                Text(m.countdown)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(DNColor.textOnHueSecondary)
                    .lineLimit(1)
                    .contentTransition(.numericText())
            }
            WidgetFigure(m.clock, size: 40).padding(.top, 2)
            Spacer(minLength: 4)
            WidgetSunArc(model: m, large: false).frame(height: 52)
        }
        .padding(EdgeInsets(top: 16, leading: 16, bottom: 14, trailing: 16))
    }

    private func large(_ m: WidgetPrayerModel) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 6) {
                moon
                WidgetTitle("Prayer times")
                Spacer(minLength: 4)
                Image(systemName: "mappin")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(DNColor.iconSecondary)
                Text(store.persona?.area ?? "Dubai")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(DNColor.textOnHueSecondary)
                    .lineLimit(1)
            }
            .frame(height: 24)
            Spacer(minLength: 8)
            VStack(alignment: .leading, spacing: 2) {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(m.next.prayer.name)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(DNColor.textPrimary)
                    Text(m.countdown)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(DNColor.textOnHueSecondary)
                        .contentTransition(.numericText())
                }
                WidgetFigure(m.clock)
            }
            Spacer(minLength: 8)
            WidgetSunArc(model: m, large: true).frame(height: 116)
            Spacer(minLength: 8)
            HStack(spacing: 0) {
                ForEach(m.prayers) { slot in
                    let isNext = slot.name == m.next.prayer.name
                    VStack(spacing: 2) {
                        Text(slot.name)
                            .font(.system(size: 12, weight: isNext ? .semibold : .medium))
                            .foregroundStyle(isNext ? DNColor.textPrimary : DNColor.textOnHueSecondary)
                        Text(WidgetFormat.clock(slot.date))
                            .font(.system(size: 17, weight: isNext ? .bold : .semibold, design: .rounded))
                            .monospacedDigit()
                            .foregroundStyle(DNColor.textPrimary)
                    }
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                    .padding(.vertical, 6)
                    .frame(maxWidth: .infinity)
                    .background {
                        if isNext {
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .fill(DNColor.widgetSurface)
                                .shadow(color: DNColor.elevationNear, radius: 1, y: 1)
                        }
                    }
                }
            }
        }
        .padding(18)
    }
}

/// The sun's path across the sky: a dotted arc from sunrise to Maghrib, the travelled part in
/// sun orange, the sun where it is now, an open marker at the next prayer and dots at the others.
struct WidgetSunArc: View {
    var model: WidgetPrayerModel
    var large: Bool

    var body: some View {
        Canvas { ctx, size in
            let inset: CGFloat = large ? 10 : 5.5
            let base = size.height - (large ? 8 : 6)
            let cx = size.width / 2
            let rx = (size.width - inset * 2) / 2
            let ry = base - (large ? 8 : 6)
            func point(_ t: Double) -> CGPoint {
                let a = Double.pi * (1 - t)
                return CGPoint(x: cx + rx * cos(a), y: base - ry * sin(a))
            }
            func arc(to t: Double) -> Path {
                var p = Path()
                let steps = 64
                for i in 0...steps {
                    let pt = point(t * Double(i) / Double(steps))
                    if i == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
                }
                return p
            }

            var horizon = Path()
            horizon.move(to: CGPoint(x: 0, y: base + 0.5))
            horizon.addLine(to: CGPoint(x: size.width, y: base + 0.5))
            ctx.stroke(horizon, with: .color(DNColor.widgetSkyHorizon), lineWidth: 1)

            ctx.stroke(arc(to: 1), with: .color(DNColor.widgetSkyPath),
                       style: StrokeStyle(lineWidth: 1.6, lineCap: .round, dash: [2, large ? 5 : 4]))

            let sunT = model.dayFraction(model.now)
            if let sunT {
                ctx.stroke(arc(to: sunT), with: .color(DNColor.signSun),
                           style: StrokeStyle(lineWidth: large ? 2.6 : 2.4, lineCap: .round))
            }

            // Markers: sunrise (large only), then each prayer in daylight. The next prayer is open.
            let markerR: CGFloat = large ? 3.2 : 2.6
            if large {
                let p = point(0)
                ctx.fill(Path(ellipseIn: CGRect(x: p.x - markerR, y: p.y - markerR, width: markerR * 2, height: markerR * 2)),
                         with: .color(DNColor.widgetSkyMarker))
            }
            for slot in model.prayers {
                guard let t = model.dayFraction(slot.date) else { continue }
                if let sunT, t < sunT { continue }
                let p = point(t)
                if slot.name == model.next.prayer.name {
                    let r: CGFloat = large ? 4.6 : 3.6
                    let rect = CGRect(x: p.x - r, y: p.y - r, width: r * 2, height: r * 2)
                    ctx.fill(Path(ellipseIn: rect), with: .color(DNColor.widgetMoon))
                    ctx.stroke(Path(ellipseIn: rect), with: .color(DNColor.widgetSkyGlyph), lineWidth: large ? 2 : 1.8)
                } else {
                    ctx.fill(Path(ellipseIn: CGRect(x: p.x - markerR, y: p.y - markerR, width: markerR * 2, height: markerR * 2)),
                             with: .color(DNColor.widgetSkyMarker))
                }
            }

            if let sunT {
                let s = point(sunT)
                let glow: CGFloat = large ? 15 : 11
                let sun: CGFloat = large ? 8 : 6.5
                ctx.fill(Path(ellipseIn: CGRect(x: s.x - glow, y: s.y - glow, width: glow * 2, height: glow * 2)),
                         with: .color(DNColor.widgetSunGlow))
                ctx.fill(Path(ellipseIn: CGRect(x: s.x - sun, y: s.y - sun, width: sun * 2, height: sun * 2)),
                         with: .color(DNColor.widgetSun))
            }
        }
        .accessibilityHidden(true)
    }
}

extension WidgetSpec {
    static func prayer(now: Date) -> WidgetSpec {
        let m = WidgetPrayerModel(now: now)
        return WidgetSpec(
            surface: .sky,
            summary: "Next prayer \(m.next.prayer.name) at \(m.clock), in \(WidgetFormat.spokenDuration(minutes: m.minutesToNext)).",
            hint: "Opens Check prayer times",
            open: .service("check-prayer-times")
        )
    }
}

// MARK: - Weather and beach

struct WidgetWeatherContent: View {
    @Environment(DemoStore.self) private var store

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 6) {
                Image(systemName: "sun.max.fill")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(DNColor.signSun)
                WidgetTitle(store.state.weatherSpot)
            }
            WidgetFigure(WidgetWeather.temperature).padding(.top, 2)
            WidgetCaption(WidgetWeather.detail)
            Spacer(minLength: 8)
            WidgetChip(text: "Safe to swim", symbol: "flag.fill", foreground: DNColor.statusSuccessForeground,
                       background: DNColor.statusSuccessSurface, height: 28)
        }
        .padding(16)
    }
}

/// The demo's weather (NCM and Dubai Municipality aren't connected).
enum WidgetWeather {
    static let temperature = "37°"
    static let detail = "Feels 42° · UV 9"
}

extension WidgetSpec {
    static func weather(store: DemoStore) -> WidgetSpec {
        WidgetSpec(
            summary: "\(store.state.weatherSpot), 37 degrees, feels like 42, UV index 9. Green flag: safe to swim.",
            open: .comingSoon(ComingSoon(
                title: "Weather and beaches",
                detail: "Forecasts and beach flags come from NCM and Dubai Municipality. They aren't part of this demo yet.",
                owner: "NCM and Dubai Municipality", symbol: "sun.max.fill", hue: .sand, kind: .notInDemo
            ))
        )
    }
}
