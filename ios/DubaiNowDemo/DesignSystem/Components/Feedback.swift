import SwiftUI

// MARK: - Toasts

/// A short confirmation that rises above the tab bar ("Added to Favorites · Undo").
/// 2.5 s plain, 5 s with Undo; doubled with VoiceOver on.
struct Toast: Identifiable, Equatable {
    let id = UUID()
    var message: String
    var symbol: String? = "checkmark.circle.fill"
    var undoTitle: String? = nil
    var undo: (() -> Void)? = nil

    static func == (lhs: Toast, rhs: Toast) -> Bool { lhs.id == rhs.id }
}

@Observable
final class ToastCenter {
    private(set) var current: Toast?
    private var task: Task<Void, Never>?

    func show(_ message: String, symbol: String? = "checkmark.circle.fill", undoTitle: String? = nil, undo: (() -> Void)? = nil) {
        show(Toast(message: message, symbol: symbol, undoTitle: undoTitle ?? (undo == nil ? nil : "Undo"), undo: undo))
    }

    func show(_ toast: Toast) {
        task?.cancel()
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { current = toast }
        var hold = toast.undo == nil ? DNMotion.toastHold : DNMotion.toastHoldWithUndo
        if UIAccessibility.isVoiceOverRunning { hold *= 2 }
        UIAccessibility.post(notification: .announcement, argument: toast.message)
        task = Task { [weak self] in
            try? await Task.sleep(for: .seconds(hold))
            guard !Task.isCancelled else { return }
            self?.dismiss()
        }
    }

    func dismiss() {
        withAnimation(DNMotion.resolve(DNMotion.smooth)) { current = nil }
    }
}

/// Place once at the root: shows the current toast above the tab bar on glass.
struct ToastHost: ViewModifier {
    @Environment(ToastCenter.self) private var toasts
    var bottomInset: CGFloat = 96

    func body(content: Content) -> some View {
        content.overlay(alignment: .bottom) {
            if let toast = toasts.current {
                HStack(spacing: 10) {
                    if let symbol = toast.symbol {
                        Image(systemName: symbol)
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(DNColor.statusSuccessForeground)
                    }
                    Text(toast.message)
                        .dnFont(.subheadline)
                        .fontWeight(.medium)
                        .foregroundStyle(DNColor.textPrimary)
                        .lineLimit(2)
                    Spacer(minLength: 4)
                    if let undo = toast.undo {
                        Button(toast.undoTitle ?? "Undo") {
                            undo()
                            toasts.dismiss()
                        }
                        .dnFont(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(DNColor.textLink)
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 18)
                .frame(minHeight: DNSpace.toastHeight)
                .glassEffect(.regular, in: .capsule)
                .padding(.horizontal, 20)
                .padding(.bottom, bottomInset)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .id(toast.id)
            }
        }
    }
}

extension View {
    func toastHost(bottomInset: CGFloat = 96) -> some View { modifier(ToastHost(bottomInset: bottomInset)) }
}

// MARK: - Coming soon

/// What a demo dead end says. Used for anything outside the core: service flows,
/// providers we don't connect to in the demo, and records that aren't live yet.
struct ComingSoon: Identifiable, Hashable {
    var id: String { title + (detail ?? "") }
    var title: String
    var detail: String? = nil
    var owner: String? = nil
    var symbol: String = "sparkles"
    var hue: Hue = .sky
    var kind: Kind = .servicesPhase

    enum Kind: Hashable {
        /// The service flow itself comes in the services phase.
        case servicesPhase
        /// The record isn't live yet ("to publish", "proposed", "to confirm").
        case notLiveYet
        /// The service is under maintenance.
        case maintenance
        /// Anything else the demo doesn't cover.
        case notInDemo
    }

    var headline: String {
        switch kind {
        case .servicesPhase: "This service comes in the next phase"
        case .notLiveYet: "Not available yet"
        case .maintenance: "Under maintenance"
        case .notInDemo: "Not part of this demo"
        }
    }

    var message: String {
        switch kind {
        case .servicesPhase:
            "This demo shows the core of the new DubaiNow. The steps inside each service are designed next, and they'll open from here."
        case .notLiveYet:
            "This service is planned. When it's ready it will appear here with everything you need to start."
        case .maintenance:
            "The service is being updated. You can still read about it and come back soon."
        case .notInDemo:
            "This part isn't built in the demo yet."
        }
    }
}

struct ComingSoonSheet: View {
    var item: ComingSoon
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 20) {
            HueIcon(symbol: item.symbol, hue: item.hue, size: 64)
                .padding(.top, 32)
            VStack(spacing: 8) {
                Text(item.title)
                    .dnFont(.title3)
                    .foregroundStyle(DNColor.textPrimary)
                    .multilineTextAlignment(.center)
                if let owner = item.owner {
                    Text(owner)
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                }
            }
            VStack(spacing: 8) {
                Text(item.headline)
                    .dnFont(.headline)
                    .foregroundStyle(DNColor.textPrimary)
                Text(item.detail ?? item.message)
                    .dnFont(.body)
                    .foregroundStyle(DNColor.textSecondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.horizontal, 8)
            Spacer(minLength: 0)
            Button("OK") { dismiss() }
                .buttonStyle(.dn(.primary))
        }
        .padding(.horizontal, DNSpace.insetPage)
        .padding(.bottom, 16)
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
    }
}
