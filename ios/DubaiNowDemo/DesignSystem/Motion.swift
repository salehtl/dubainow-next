import SwiftUI
import UIKit

/// DubaiNow motion (Paper C1 row 08 · "Three springs, one physics").
/// These are our own springs — deliberately named `dn…` so they don't get confused with
/// SwiftUI's built-in `.snappy`, `.smooth` and `.bouncy`, which use different values.
enum DNMotion {
    /// Presses, toggles, chips, the tab selection. response 0.30 · damping 0.86
    static let snappy = Animation.spring(response: 0.30, dampingFraction: 0.86)
    /// Sheets, pushes, the search morph, reflowing grids. response 0.40 · damping 0.90
    static let smooth = Animation.spring(response: 0.40, dampingFraction: 0.90)
    /// Moments of success only (favorite added, widget dropped). response 0.50 · damping 0.70
    static let bouncy = Animation.spring(response: 0.50, dampingFraction: 0.70)

    /// 150 ms – press highlights, results updating as you type.
    static let fast = Animation.easeInOut(duration: 0.15)
    /// 250 ms – cross-fades, tab content, Reduce Motion swaps.
    static let base = Animation.easeInOut(duration: 0.25)
    /// 600 ms – the Home wash changing with the time of day.
    static let wash = Animation.easeInOut(duration: 0.6)

    /// 30 ms between items entering (first 8 only).
    static let stagger: Double = 0.03
    /// Widgets, tiles and cards while pressed.
    static let pressScale: CGFloat = 0.97
    /// Touch and hold: lift after 0.45 s to 1.04.
    static let holdDelay: Double = 0.45
    static let holdLift: CGFloat = 1.04
    /// Toasts: 2.5 s plain, 5 s with Undo (doubled with VoiceOver on).
    static let toastHold: Double = 2.5
    static let toastHoldWithUndo: Double = 5

    /// The animation to use, honouring Reduce Motion (springs become a 250 ms cross-fade).
    static func resolve(_ animation: Animation) -> Animation {
        UIAccessibility.isReduceMotionEnabled ? base : animation
    }

    static func stagger(_ index: Int) -> Double {
        Double(min(index, 8)) * stagger
    }
}

/// Press feedback for tiles, widgets and cards: scales to 0.97 with the snappy spring.
struct DNPressableStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed && !UIAccessibility.isReduceMotionEnabled ? DNMotion.pressScale : 1)
            .animation(DNMotion.snappy, value: configuration.isPressed)
    }
}

extension ButtonStyle where Self == DNPressableStyle {
    /// Tiles, widgets and cards: scale to 0.97 while pressed.
    static var dnPressable: DNPressableStyle { DNPressableStyle() }
}

/// Haptics, used sparingly (Paper C1 · Haptics and fallbacks).
/// Never for scrolling, pushes or anything that can repeat quickly.
enum DNHaptics {
    /// Switching tabs, choosing a chip or a Happiness face.
    static func selection() { UISelectionFeedbackGenerator().selectionChanged() }
    /// Removing a favorite or a widget.
    static func light() { UIImpactFeedbackGenerator(style: .light).impactOccurred() }
    /// Entering edit mode, lifting a widget.
    static func medium() { UIImpactFeedbackGenerator(style: .medium).impactOccurred() }
    /// Crossing the pull threshold at the end of Home.
    static func soft() { UIImpactFeedbackGenerator(style: .soft).impactOccurred() }
    /// Payment done, request sent, favorite added.
    static func success() { UINotificationFeedbackGenerator().notificationOccurred(.success) }
    /// Favorites full (8 of 8), parking about to end.
    static func warning() { UINotificationFeedbackGenerator().notificationOccurred(.warning) }
    /// Payment failed, a form that cannot be sent.
    static func error() { UINotificationFeedbackGenerator().notificationOccurred(.error) }
}
