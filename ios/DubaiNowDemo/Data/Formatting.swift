import Foundation

/// Money in AED. Western digits always (Arabic too: `ar-AE@numbers=latn`).
enum Money {
    private static let figureFormatter: NumberFormatter = {
        let f = NumberFormatter()
        f.locale = Locale(identifier: "en_AE")
        f.numberStyle = .decimal
        f.minimumFractionDigits = 2
        f.maximumFractionDigits = 2
        return f
    }()

    private static let wholeFormatter: NumberFormatter = {
        let f = NumberFormatter()
        f.locale = Locale(identifier: "en_AE")
        f.numberStyle = .decimal
        f.maximumFractionDigits = 0
        return f
    }()

    /// "1,012.60" (or "600" when whole and `trimWhole` is true).
    static func figure(_ amount: Decimal, trimWhole: Bool = false) -> String {
        let n = NSDecimalNumber(decimal: amount)
        if trimWhole, amount == amount.rounded() { return wholeFormatter.string(from: n) ?? "\(amount)" }
        return figureFormatter.string(from: n) ?? "\(amount)"
    }

    /// "AED 1,012.60"
    static func aed(_ amount: Decimal, trimWhole: Bool = false) -> String { "AED " + figure(amount, trimWhole: trimWhole) }

    static func spoken(_ amount: Decimal) -> String { "\(figure(amount)) dirhams" }
}

extension Decimal {
    func rounded(scale: Int = 0) -> Decimal {
        var source = self, result = Decimal()
        NSDecimalRound(&result, &source, scale, .plain)
        return result
    }
}

/// Dates in the demo are relative to "now", so due dates stay true whenever the demo is opened.
enum DemoDate {
    static var calendar: Calendar { Calendar.current }

    /// A date `days` from today at a given time.
    static func day(_ days: Int, hour: Int = 9, minute: Int = 0) -> Date {
        let start = calendar.startOfDay(for: .now)
        let d = calendar.date(byAdding: .day, value: days, to: start) ?? start
        return calendar.date(bySettingHour: hour, minute: minute, second: 0, of: d) ?? d
    }

    /// Minutes from now.
    static func minutes(_ m: Int) -> Date { Date.now.addingTimeInterval(TimeInterval(m * 60)) }

    private static let dayMonth: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_GB"); f.dateFormat = "d MMM"; return f
    }()
    private static let dayMonthLong: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_GB"); f.dateFormat = "d MMMM"; return f
    }()
    private static let dayMonthYear: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_GB"); f.dateFormat = "d MMMM yyyy"; return f
    }()
    private static let monthYear: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_GB"); f.dateFormat = "MMMM yyyy"; return f
    }()
    private static let time: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_GB"); f.dateFormat = "HH:mm"; return f
    }()
    private static let weekday: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_GB"); f.dateFormat = "EEEE"; return f
    }()

    /// "24 Sep"
    static func short(_ d: Date) -> String { dayMonth.string(from: d) }
    /// "4 October"
    static func long(_ d: Date) -> String { dayMonthLong.string(from: d) }
    /// "14 March 2027"
    static func full(_ d: Date) -> String { dayMonthYear.string(from: d) }
    /// "March 2028"
    static func monthYear(_ d: Date) -> String { monthYear.string(from: d) }
    /// "08:14"
    static func time(_ d: Date) -> String { time.string(from: d) }
    /// "Thursday"
    static func weekday(_ d: Date) -> String { weekday.string(from: d) }

    /// Whole days from today to the date (negative in the past).
    static func daysFromToday(_ d: Date) -> Int {
        calendar.dateComponents([.day], from: calendar.startOfDay(for: .now), to: calendar.startOfDay(for: d)).day ?? 0
    }

    /// "Due today", "Due tomorrow", "Due in 3 days", "Due 13 Oct", "Overdue".
    static func dueLabel(_ d: Date) -> String {
        let n = daysFromToday(d)
        switch n {
        case ..<0: return "Overdue"
        case 0: return "Due today"
        case 1: return "Due tomorrow"
        case 2...13: return "Due in \(n) days"
        default: return "Due \(short(d))"
        }
    }

    /// "today", "yesterday", "3 days ago", "24 Sep"
    static func relative(_ d: Date) -> String {
        let n = daysFromToday(d)
        switch n {
        case 0: return "Today"
        case -1: return "Yesterday"
        case -6 ... -2: return "\(-n) days ago"
        default: return short(d)
        }
    }

    /// "in 2h 31m"
    static func countdown(to d: Date, from now: Date = .now) -> String {
        let mins = max(0, Int(d.timeIntervalSince(now) / 60))
        let h = mins / 60, m = mins % 60
        return h > 0 ? "in \(h)h \(m)m" : "in \(m) min"
    }
}
