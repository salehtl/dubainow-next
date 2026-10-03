import SwiftUI
import LocalAuthentication

// Shared building blocks for the Profile feature (profile sheet, things, settings pages,
// Happiness Meter, notifications). Everything here is prefixed `Profile` to avoid clashes.

// MARK: - Environment

extension EnvironmentValues {
    /// True inside the profile sheet's own NavigationStack. Sheets have no logo, so pushed
    /// screens there skip the assistant logo (it would open the assistant behind the sheet).
    @Entry var profileIsInSheet: Bool = false
}

/// Pushed-screen chrome for Profile screens: title, and the assistant logo only when the
/// screen is pushed in a tab (never inside a sheet).
struct ProfilePushedChrome: ViewModifier {
    var title: String
    var large: Bool
    var forceSheet: Bool = false
    @Environment(\.profileIsInSheet) private var inSheet

    func body(content: Content) -> some View {
        let titled = content
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(large ? .large : .inline)
        if inSheet || forceSheet {
            titled
        } else {
            titled.dnPushedToolbar()
        }
    }
}

extension View {
    func profileChrome(_ title: String, large: Bool = true, forceSheet: Bool = false) -> some View {
        modifier(ProfilePushedChrome(title: title, large: large, forceSheet: forceSheet))
    }

    /// Presents a "coming next" sheet locally (keeps the current sheet or stack where it is).
    func profileComingSoon(_ item: Binding<ComingSoon?>) -> some View {
        sheet(item: item) { ComingSoonSheet(item: $0) }
    }
}

// MARK: - Routing helpers

enum ProfileRoutes {
    /// The route for a catalogue record (start page, bundle or journey). Nil when the
    /// record isn't in the catalogue yet.
    static func route(forService id: String) -> Route? {
        guard let s = Catalogue.shared.service(id) else { return nil }
        switch s.type {
        case .standalone: return .service(id)
        case .bundle: return .bundle(id)
        case .journey: return .journey(id)
        }
    }

    static let notInDemoHelp = ComingSoon(
        title: "Get help",
        detail: "Help pages, live chat and a way to report a problem come in the next phase of DubaiNow.",
        symbol: "questionmark.circle.fill",
        hue: .sky,
        kind: .notInDemo
    )

    static let language = ComingSoon(
        title: "Language",
        detail: "This demo is in English. Arabic, with the whole app laid out right to left, comes next.",
        symbol: "globe",
        hue: .sky,
        kind: .notInDemo
    )
}

// MARK: - Store helpers (read existing state only)

extension DemoStore {
    /// Unpaid fines across all the resident's vehicles.
    var profileFineCount: Int { state.vehicles.reduce(0) { $0 + $1.fineIDs.count } }

    /// The unpaid fines on a vehicle (from Needs you).
    func profileFines(for vehicle: Vehicle) -> [Payable] {
        payables.filter { vehicle.fineIDs.contains($0.id) }
    }

    func profileMarkUnread(_ id: String) {
        if let i = state.notifications.firstIndex(where: { $0.id == id }) { state.notifications[i].isRead = false }
    }

    /// Requests that are still running with an entity.
    var profileActiveRequests: [RequestItem] {
        state.requests.filter { if case .done = $0.status { return false }; return true }
    }
}

enum ProfileText {
    /// "Ahmed", "Ahmed and Layla", "Ahmed, Layla and Omar" (UK style, no serial comma).
    static func join(_ items: [String]) -> String {
        switch items.count {
        case 0: return ""
        case 1: return items[0]
        default: return items.dropLast().joined(separator: ", ") + " and " + (items.last ?? "")
        }
    }

    static func lowerFirst(_ s: String) -> String {
        guard let first = s.first else { return s }
        return first.lowercased() + s.dropFirst()
    }
}

// MARK: - Face ID (LocalAuthentication)

enum ProfileAuth {
    /// Asks for Face ID. Falls back gracefully: when the device has no Face ID enrolled
    /// (e.g. a Simulator — enrol it in Features › Face ID to see the prompt) the demo carries on.
    /// Returns false only when the person cancels.
    static func confirm(reason: String) async -> Bool {
        let context = LAContext()
        context.localizedFallbackTitle = ""
        var error: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else { return true }
        do {
            return try await context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason)
        } catch let e as LAError {
            switch e.code {
            case .userCancel, .appCancel, .systemCancel: return false
            default: return true
            }
        } catch {
            return true
        }
    }
}

// MARK: - Grouped rows

/// A white rounded group (radius 24) with separators inset to the text column.
struct ProfileGroup<Content: View>: View {
    var separatorInset: CGFloat = 16
    var elevated: Bool = false
    @ViewBuilder var content: Content
    @Environment(\.displayScale) private var displayScale

    var body: some View {
        VStack(spacing: 0) {
            Group(subviews: content) { subviews in
                ForEach(subviews) { subview in
                    subview
                    if subview.id != subviews.last?.id {
                        Rectangle()
                            .fill(DNColor.listRowSeparator)
                            .frame(height: 1 / displayScale)
                            .padding(.leading, separatorInset)
                    }
                }
            }
        }
        .background(DNColor.surfacePrimary, in: .rect(cornerRadius: DNRadius.listGroup, style: .continuous))
        .clipShape(.rect(cornerRadius: DNRadius.listGroup, style: .continuous))
        .dnElevation(elevated)
    }
}

/// "SIGNED IN WITH" — the small grouped header above a group.
struct ProfileOverline: View {
    var text: String
    var body: some View {
        Text(text.uppercased())
            .dnFont(.overline)
            .foregroundStyle(DNColor.textGroupedHeader)
            .padding(.horizontal, 4)
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityAddTraits(.isHeader)
    }
}

/// "Documents", "Your things", "Settings" — 20/26 bold.
struct ProfileSectionTitle: View {
    var text: String
    var body: some View {
        Text(text)
            .dnFont(.title3)
            .fontWeight(.bold)
            .foregroundStyle(DNColor.textPrimary)
            .accessibilityAddTraits(.isHeader)
    }
}

/// A list row: leading view, title (+ supporting line), optional value, count badge and chevron.
struct ProfileRow<Leading: View>: View {
    var title: String
    var subtitle: String? = nil
    var value: String? = nil
    var badge: Int? = nil
    var emphasised: Bool = false
    var titleColor: Color = DNColor.textPrimary
    var showsChevron: Bool = true
    @ViewBuilder var leading: Leading

    var body: some View {
        HStack(spacing: 12) {
            leading
            VStack(alignment: .leading, spacing: 1) {
                Text(title)
                    .dnFont(.rowTitle)
                    .fontWeight(emphasised ? .semibold : .medium)
                    .foregroundStyle(titleColor)
                    .multilineTextAlignment(.leading)
                if let subtitle {
                    Text(subtitle)
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textSecondary)
                        .multilineTextAlignment(.leading)
                }
            }
            Spacer(minLength: 8)
            if let value {
                Text(value)
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textSecondary)
                    .multilineTextAlignment(.trailing)
            }
            if let badge, badge > 0 {
                Text("\(badge)")
                    .font(.system(size: 12, weight: .bold, design: .rounded))
                    .foregroundStyle(DNColor.textOnDestructive)
                    .padding(.horizontal, 6)
                    .frame(minWidth: 22, minHeight: 22)
                    .background(DNColor.actionDestructive, in: .capsule)
            }
            if showsChevron {
                Image(systemName: "chevron.forward")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(DNColor.listRowChevron)
                    .accessibilityHidden(true)
            }
        }
        .padding(.horizontal, DNSpace.listRowInsetInline)
        .padding(.vertical, 10)
        .frame(minHeight: subtitle == nil ? DNSpace.listRowMinHeight : DNSpace.listRowMinHeightTwoLine)
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }
}

extension ProfileRow where Leading == EmptyView {
    init(title: String, subtitle: String? = nil, value: String? = nil, badge: Int? = nil,
         emphasised: Bool = false, titleColor: Color = DNColor.textPrimary, showsChevron: Bool = true) {
        self.init(title: title, subtitle: subtitle, value: value, badge: badge, emphasised: emphasised,
                  titleColor: titleColor, showsChevron: showsChevron) { EmptyView() }
    }
}

/// A plain 24 pt SF Symbol slot for settings rows.
struct ProfileRowSymbol: View {
    var symbol: String
    var color: Color = DNColor.textPrimary
    var body: some View {
        Image(systemName: symbol)
            .font(.system(size: 19, weight: .regular))
            .foregroundStyle(color)
            .frame(width: 24, height: 24)
            .accessibilityHidden(true)
    }
}

/// A grey disc with a glyph (Payment methods) or a monogram (entities).
struct ProfileGreyDisc: View {
    var symbol: String? = nil
    var monogram: String? = nil
    var size: CGFloat = 40

    var body: some View {
        Circle()
            .fill(DNColor.surfaceSecondary)
            .overlay {
                if let symbol {
                    Image(systemName: symbol)
                        .font(.system(size: size * 0.42, weight: .medium))
                        .foregroundStyle(DNColor.textPrimary)
                } else if let monogram {
                    Text(monogram)
                        .font(.system(size: monogram.count > 3 ? 9 : 11, weight: .heavy))
                        .foregroundStyle(DNColor.textPrimary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                        .padding(4)
                }
            }
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }
}

/// Row press feedback inside custom groups (a grey wash while pressed).
struct ProfileRowButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? DNColor.listRowSurfacePressed : Color.clear)
            .animation(DNMotion.fast, value: configuration.isPressed)
    }
}

/// A grey pill (e.g. "View policy") — the neutral sibling of the Canvas buttons.
struct ProfileNeutralPillStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .dnFont(.subheadline)
            .fontWeight(.semibold)
            .foregroundStyle(DNColor.textPrimary)
            .lineLimit(1)
            .minimumScaleFactor(0.8)
            .padding(.horizontal, DNSpace.buttonInsetSmall)
            .frame(maxWidth: .infinity)
            .frame(minHeight: DNSpace.buttonHeightSmall)
            .background(configuration.isPressed ? DNColor.neutral200 : DNColor.surfaceSecondary, in: .capsule)
            .contentShape(.capsule)
            .animation(DNMotion.fast, value: configuration.isPressed)
    }
}

/// Filled destructive pill ("Delete your account").
struct ProfileDestructiveFillStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .dnFont(.button)
            .foregroundStyle(DNColor.textOnDestructive)
            .lineLimit(1)
            .minimumScaleFactor(0.8)
            .frame(maxWidth: .infinity)
            .frame(minHeight: DNSpace.buttonHeightLarge)
            .background(configuration.isPressed ? DNColor.actionDestructivePressed : DNColor.actionDestructive, in: .capsule)
            .contentShape(.capsule)
            .animation(DNMotion.fast, value: configuration.isPressed)
    }
}

// MARK: - Toggle row

struct ProfileToggleRow: View {
    var title: String
    var subtitle: String? = nil
    @Binding var isOn: Bool

    var body: some View {
        Toggle(isOn: $isOn) {
            VStack(alignment: .leading, spacing: 1) {
                Text(title)
                    .dnFont(.rowTitle)
                    .foregroundStyle(DNColor.textPrimary)
                if let subtitle {
                    Text(subtitle)
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textSecondary)
                }
            }
        }
        .tint(DNColor.controlSwitchTrackOn)
        .padding(.horizontal, DNSpace.listRowInsetInline)
        .padding(.vertical, 10)
        .frame(minHeight: 60)
    }
}

// MARK: - Key–value card

struct ProfileFactsCard: View {
    var facts: [(label: String, value: String)]
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            ForEach(Array(facts.enumerated()), id: \.offset) { _, fact in
                let layout = typeSize.isAccessibilitySize
                    ? AnyLayout(VStackLayout(alignment: .leading, spacing: 2))
                    : AnyLayout(HStackLayout(alignment: .firstTextBaseline, spacing: 12))
                layout {
                    Text(fact.label)
                        .dnFont(.subheadline)
                        .foregroundStyle(DNColor.textSecondary)
                    if !typeSize.isAccessibilitySize { Spacer(minLength: 8) }
                    Text(fact.value)
                        .dnFont(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(DNColor.textPrimary)
                        .multilineTextAlignment(typeSize.isAccessibilitySize ? .leading : .trailing)
                }
                .accessibilityElement(children: .combine)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .dnCard(radius: DNRadius.listGroup)
    }
}

// MARK: - Flow layout (chips)

/// Lays chips out left to right and wraps onto new lines.
struct ProfileFlowLayout: Layout {
    var spacing: CGFloat = 8
    var lineSpacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, lineHeight: CGFloat = 0, widest: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(ProposedViewSize(width: maxWidth, height: nil))
            if x > 0, x + size.width > maxWidth {
                y += lineHeight + lineSpacing
                x = 0
                lineHeight = 0
            }
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
            widest = max(widest, x - spacing)
        }
        return CGSize(width: proposal.width ?? widest, height: y + lineHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, lineHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(ProposedViewSize(width: bounds.width, height: nil))
            if x > bounds.minX, x + size.width > bounds.maxX {
                y += lineHeight + lineSpacing
                x = bounds.minX
                lineHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(width: min(size.width, bounds.width), height: size.height))
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }
    }
}

// MARK: - Documents

extension DocumentItem {
    /// "Valid to March 2028" / "Expires 18 November 2026" / "Expired on 2 May 2026".
    var profileExpiryLine: String {
        guard let expiry else { return "No expiry date" }
        let days = DemoDate.daysFromToday(expiry)
        let date = "\(DemoDate.short(expiry)) \(Calendar.current.component(.year, from: expiry))"
        if days < 0 { return "Expired on \(date)" }
        // A renewal that's already approved carries the new, far-off date.
        if days > 365 { return "Valid to \(DemoDate.monthYear(expiry))" }
        switch status {
        case .valid: return "Valid to \(DemoDate.monthYear(expiry))"
        case .renewing, .expiringSoon, .expired: return "Expires \(date)"
        }
    }

    var profileStatusTone: StatusChip.Tone {
        switch status {
        case .valid: return .success
        case .expired: return .error
        case .expiringSoon: return .warning
        case .renewing: return (statusNote ?? "").localizedCaseInsensitiveContains("your turn") ? .warning : .info
        }
    }

    var profileStatusText: String {
        if let statusNote { return statusNote }
        switch status {
        case .valid: return "Valid"
        case .renewing: return "Renewing"
        case .expiringSoon: return "Expires soon"
        case .expired: return "Expired"
        }
    }
}

/// The wallet-style document card (Profile shelf, Documents grid).
struct ProfileDocumentCard: View {
    var document: DocumentItem
    var width: CGFloat? = 210

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 8) {
                Text(document.title)
                    .dnFont(.subheadline)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.leading)
                Spacer(minLength: 4)
                Image(systemName: document.symbol)
                    .font(.system(size: 17, weight: .medium))
                    .accessibilityHidden(true)
            }
            Spacer(minLength: 14)
            VStack(alignment: .leading, spacing: 6) {
                if document.status == .valid {
                    Text(document.number)
                        .font(.system(size: 13, weight: .regular, design: .monospaced))
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                } else {
                    ProfileOnHueChip(text: document.profileStatusText, tone: document.profileStatusTone)
                }
                Text(document.profileExpiryLine)
                    .dnFont(.footnote)
                    .fontWeight(.semibold)
            }
        }
        .foregroundStyle(document.hue.ink)
        .padding(16)
        .frame(width: width, alignment: .leading)
        .frame(maxWidth: width == nil ? .infinity : nil, minHeight: 132, alignment: .leading)
        .background(document.hue.gradient, in: .rect(cornerRadius: 22, style: .continuous))
        .dnElevation()
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(document.title). \(document.status == .valid ? "" : document.profileStatusText + ". ")\(document.profileExpiryLine)")
        .accessibilityAddTraits(.isButton)
    }
}

/// A status chip that sits on a hue (white-ish surface, coloured dot and text).
struct ProfileOnHueChip: View {
    var text: String
    var tone: StatusChip.Tone

    var body: some View {
        HStack(spacing: 5) {
            Circle().fill(dot).frame(width: 6, height: 6)
            Text(text)
                .dnFont(.caption1)
                .fontWeight(.bold)
                .foregroundStyle(foreground)
                .lineLimit(2)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.leading, 8)
        .padding(.trailing, 10)
        .padding(.vertical, 4)
        .frame(minHeight: 24)
        .background(DNColor.chipOnHue, in: .rect(cornerRadius: 12, style: .continuous))
    }

    private var dot: Color {
        switch tone {
        case .warning: DNColor.signSun
        case .info: DNColor.statusInfoForeground
        case .success: DNColor.statusSuccessForeground
        case .error: DNColor.statusErrorForeground
        case .neutral: DNColor.textSecondary
        }
    }

    private var foreground: Color {
        switch tone {
        case .warning: DNColor.statusWarningForeground
        case .info: DNColor.statusInfoForeground
        case .success: DNColor.statusSuccessForeground
        case .error: DNColor.statusErrorForeground
        case .neutral: DNColor.textSecondary
        }
    }
}

// MARK: - "Download a copy" (shared by Linked accounts and Delete account)

struct ProfileDownloadCopyAlert: ViewModifier {
    @Binding var isPresented: Bool
    @Environment(ToastCenter.self) private var toasts

    func body(content: Content) -> some View {
        content.alert("Download a copy of your information?", isPresented: $isPresented) {
            Button("Send me a copy") {
                DNHaptics.success()
                toasts.show("We'll email you a link within 24 hours", symbol: "envelope.fill")
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("We'll put together everything DubaiNow keeps about you and email you a link to download it.")
        }
    }
}

extension View {
    func profileDownloadCopyAlert(isPresented: Binding<Bool>) -> some View {
        modifier(ProfileDownloadCopyAlert(isPresented: isPresented))
    }
}

// MARK: - Screenshot hook

extension View {
    /// `-profileScroll center|bottom` starts Profile scroll views there (demo screenshots only).
    func profileScrollHook() -> some View {
        let anchor: UnitPoint? = switch UserDefaults.standard.string(forKey: "profileScroll") {
        case "center": .center
        case "bottom": .bottom
        default: nil
        }
        return defaultScrollAnchor(anchor)
    }
}
