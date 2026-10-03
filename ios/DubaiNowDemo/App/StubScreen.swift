import SwiftUI

/// Temporary placeholder used until a feature's real view lands.
struct StubScreen: View {
    var title: String
    var detail: String = "Being built"
    var pushed: Bool = true

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                Text(title).dnFont(.title2).foregroundStyle(DNColor.textPrimary)
                Text(detail).dnFont(.body).foregroundStyle(DNColor.textSecondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 80)
        }
        .dnCanvasBackground()
        .modifier(StubToolbar(pushed: pushed))
    }
}

private struct StubToolbar: ViewModifier {
    var pushed: Bool
    func body(content: Content) -> some View {
        if pushed { content.dnPushedToolbar() } else { content.dnMainToolbar() }
    }
}
