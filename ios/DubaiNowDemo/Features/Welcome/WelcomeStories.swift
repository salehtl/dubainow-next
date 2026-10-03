import SwiftUI

/// One story in the pre-login player (Paper C1 · "10 · Welcome", English stories 1–6).
/// Each story takes one Canvas hue for its wash, kicker and shadows.
struct WelcomeStory: Identifiable {
    enum Hero { case orbit, needsYou, widgets, journey, search, poster }

    /// Who the story is from: DubaiNow's own tour, or an entity's announcement.
    enum Source {
        case dubaiNow
        case entity(name: LocalizedStringResource, symbol: String, age: LocalizedStringResource)
    }

    let id: Int
    let hero: Hero
    let hue: Hue
    var source: Source = .dubaiNow
    let kicker: LocalizedStringResource
    /// One line at the default text size (34/40 bold).
    let headline: LocalizedStringResource
    /// Up to two lines.
    let body: LocalizedStringResource

    /// The tour (stories 1–5) plus one sample announcement (story 6).
    /// The rules board says first launch shows the tour and up to 2 announcements, and later visits
    /// show announcements only; the demo always plays all six so the boss sees the whole tour.
    static let all: [WelcomeStory] = [
        WelcomeStory(
            id: 0, hero: .orbit, hue: .sky,
            kicker: "Welcome to DubaiNow",
            headline: "Dubai, in one app.",
            body: "Pay, renew, apply and track government services. One sign-in, with UAE PASS."
        ),
        WelcomeStory(
            id: 1, hero: .needsYou, hue: .peach,
            kicker: "Needs you",
            headline: "What needs you, first.",
            body: "Bills, fines and renewals show up before they’re late. Pay them all in one go."
        ),
        WelcomeStory(
            id: 2, hero: .widgets, hue: .lilac,
            kicker: "Your widgets",
            headline: "Your city, live.",
            body: "Parking time, Salik balance, prayer times and the metro, on a Home you arrange."
        ),
        WelcomeStory(
            id: 3, hero: .journey, hue: .sand,
            kicker: "Start to finish",
            headline: "One step at a time.",
            body: "Building a house or moving home? We line up every service you need, in order."
        ),
        WelcomeStory(
            id: 4, hero: .search, hue: .mint,
            kicker: "Search",
            headline: "Just ask.",
            body: "Local names, typos and questions like “how much do I owe” all get a straight answer."
        ),
        WelcomeStory(
            id: 5, hero: .poster, hue: .rose,
            source: .entity(name: "Dubai Fitness Challenge", symbol: "figure.run", age: "2h"),
            kicker: "Announcement",
            headline: "Dubai gets moving.",
            body: "Free runs, rides and classes across the city, from 1 to 30 November."
        ),
    ]
}

/// The story clock: 6 s per story, the segment fills in a straight line, pauses keep the elapsed time.
/// It never loops on its own — after the last story it stops with every segment full.
@Observable
final class WelcomeStoryClock {
    static let storyDuration: TimeInterval = 6

    let count: Int
    private(set) var index: Int
    /// The last story has played to the end; every segment is full.
    private(set) var isFinished = false
    /// Time played on the current story before the last pause.
    private var elapsed: TimeInterval = 0
    /// When the current run started (nil while paused).
    private var startedAt: Date?

    init(count: Int, startAt index: Int = 0) {
        self.count = count
        self.index = min(max(index, 0), max(count - 1, 0))
    }

    var isRunning: Bool { startedAt != nil }

    /// Seconds left on the current story (valid when a run starts).
    var remaining: TimeInterval { max(0, Self.storyDuration - elapsed) }

    /// How much of the current story has played, 0…1.
    func fraction(at date: Date) -> Double {
        if isFinished { return 1 }
        let running = startedAt.map { date.timeIntervalSince($0) } ?? 0
        return min(1, max(0, (elapsed + running) / Self.storyDuration))
    }

    func run() {
        guard startedAt == nil, !isFinished else { return }
        startedAt = .now
    }

    func pause() {
        guard let startedAt else { return }
        elapsed += Date.now.timeIntervalSince(startedAt)
        self.startedAt = nil
    }

    /// Jump to a story and start it from the beginning.
    func go(to newIndex: Int) {
        index = min(max(newIndex, 0), count - 1)
        elapsed = 0
        startedAt = nil
        isFinished = false
    }

    /// Next story. From the last story (finished or not) it starts again from story 1.
    func next() { go(to: index < count - 1 ? index + 1 : 0) }

    /// Previous story. On story 1 it restarts story 1.
    func previous() { go(to: index - 1) }

    /// The timer ran out: move on, or stop on the last story with every segment full.
    func timerEnded() {
        if index < count - 1 {
            go(to: index + 1)
        } else {
            elapsed = Self.storyDuration
            startedAt = nil
            isFinished = true
        }
    }
}
