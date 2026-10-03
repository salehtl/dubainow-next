import Foundation

/// The demo residents you can sign in as (persona picker behind "Sign in with UAE PASS").
/// Each persona file builds a full, internally consistent `PersonaState`.
enum Personas {
    static var all: [Persona] { [sara, khalid, faisal, wei] }

    static func persona(id: String) -> Persona? { all.first { $0.id == id } }

    /// Guest: no account. Services, Search and public widgets only.
    static func guestState() -> PersonaState {
        var s = PersonaState()
        s.favorites = [
            FavoriteItem(id: "fav-parking", title: "Pay parking", symbol: "p.square.fill", hue: .sky, target: .service("pay-for-parking")),
            FavoriteItem(id: "fav-donate", title: "Donate", symbol: "heart.fill", hue: .lilac, target: .service("donate-to-charity")),
        ]
        s.widgets = [
            HomeWidget(id: "g-prayer", kind: .prayer, size: .small),
            HomeWidget(id: "g-weather", kind: .weather, size: .small),
            HomeWidget(id: "g-metro", kind: .metro, size: .medium),
        ]
        s.metro = MetroStation(name: "Business Bay", line: "Red Line", departures: [
            MetroDeparture(towards: "Centrepoint", inMinutes: 3, nextInMinutes: 9),
            MetroDeparture(towards: "Expo 2020", inMinutes: 6, nextInMinutes: 14),
        ])
        return s
    }
}

extension DemoDate {
    /// A fixed calendar date (for things far away, like an Emirates ID expiry).
    static func fixed(_ year: Int, _ month: Int, _ day: Int, hour: Int = 9) -> Date {
        Calendar.current.date(from: DateComponents(year: year, month: month, day: day, hour: hour)) ?? .now
    }
}

/// Dubai prayer times for early October (used by the prayer widget). Static timetable.
enum PrayerTimes {
    struct Prayer: Hashable { let name: String; let hour: Int; let minute: Int }
    static let today: [Prayer] = [
        Prayer(name: "Fajr", hour: 4, minute: 58),
        Prayer(name: "Sunrise", hour: 6, minute: 12),
        Prayer(name: "Dhuhr", hour: 12, minute: 12),
        Prayer(name: "Asr", hour: 15, minute: 34),
        Prayer(name: "Maghrib", hour: 18, minute: 5),
        Prayer(name: "Isha", hour: 19, minute: 19),
    ]

    /// The next prayer after `now` (wraps to tomorrow's Fajr).
    static func next(after now: Date) -> (prayer: Prayer, date: Date) {
        let cal = Calendar.current
        for p in today where p.name != "Sunrise" {
            if let d = cal.date(bySettingHour: p.hour, minute: p.minute, second: 0, of: now), d > now {
                return (p, d)
            }
        }
        let fajr = today[0]
        let tomorrow = cal.date(byAdding: .day, value: 1, to: now) ?? now
        return (fajr, cal.date(bySettingHour: fajr.hour, minute: fajr.minute, second: 0, of: tomorrow) ?? now)
    }
}
