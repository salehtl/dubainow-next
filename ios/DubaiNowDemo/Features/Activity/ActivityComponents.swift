import SwiftUI

// MARK: - Filter chips

/// All · Needs you n · In progress n · Drafts n · History. Chip tokens from Paper (40 pt pills).
struct ActivityFilterChips: View {
    @Binding var selection: ActivityFilter
    var counts: [ActivityFilter: Int]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: DNSpace.chipGap) {
                ForEach(ActivityFilter.allCases) { filter in
                    ActivityFilterChip(filter: filter, count: counts[filter], isSelected: selection == filter) {
                        guard selection != filter else { return }
                        DNHaptics.selection()
                        withAnimation(DNMotion.resolve(DNMotion.snappy)) { selection = filter }
                    }
                }
            }
            .padding(.horizontal, DNSpace.insetPage)
            .padding(.vertical, 2)
        }
        .scrollClipDisabled()
    }
}

private struct ActivityFilterChip: View {
    let filter: ActivityFilter
    let count: Int?
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(filter.title)
                    .dnFont(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(isSelected ? DNColor.chipTextSelected : DNColor.chipText)
                if let count, count > 0 {
                    if filter == .needsYou {
                        Text("\(count)")
                            .dnFont(.caption1)
                            .fontWeight(.bold)
                            .fontDesign(.rounded)
                            .foregroundStyle(DNColor.textOnDestructive)
                            .padding(.horizontal, 6)
                            .frame(minWidth: 20, minHeight: 20)
                            .background(DNColor.actionDestructive, in: .capsule)
                    } else {
                        Text("\(count)")
                            .dnFont(.footnote)
                            .fontWeight(.semibold)
                            .foregroundStyle(isSelected ? DNColor.chipTextSelected : DNColor.chipCount)
                    }
                }
            }
            .padding(.horizontal, count == nil ? DNSpace.chipInset : 14)
            .frame(minHeight: DNSpace.chipHeight)
            .background(isSelected ? DNColor.chipSurfaceSelected : DNColor.chipSurface, in: .capsule)
            .contentShape(.capsule)
        }
        .buttonStyle(.dnPressable)
        .accessibilityLabel(count.map { "\(filter.title), \($0)" } ?? filter.title)
        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
    }
}

// MARK: - Needs you card

/// The big card for one Needs you item: a request step waiting for the resident, a bill or
/// fine to pay, a renewal or a journey stage. Same data as Home's Needs you.
struct ActivityNeedsYouCard: View {
    let item: NeedsYouItem
    var onPrimary: () -> Void
    var onSecondary: () -> Void
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            header
            if let p = item.progress {
                SegmentedProgress(
                    total: p.total,
                    done: max(p.current - 1, 0),
                    doneColor: DNColor.actionAccent,
                    currentColor: DNColor.statusWarningForeground
                )
            }
            buttons
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .dnCard(radius: DNRadius.brief)
    }

    @ViewBuilder private var header: some View {
        let layout = typeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: 10))
            : AnyLayout(HStackLayout(alignment: .top, spacing: 12))
        layout {
            HueIcon(symbol: item.symbol, hue: item.hue, size: 44)
            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .dnFont(.headline)
                    .foregroundStyle(DNColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Text(subtitle)
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
                if let payable = item.payable, let due = payable.due {
                    ActivityDueChip(text: DemoDate.dueLabel(due), urgent: DemoDate.daysFromToday(due) <= 3)
                        .padding(.top, 4)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            trailing
        }
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder private var trailing: some View {
        if let amount = item.amount {
            Text(Money.aed(amount))
                .dnFont(.numericUnit)
                .foregroundStyle(DNColor.textPrimary)
                .lineLimit(1)
                .fixedSize()
        } else if let due = item.due {
            ActivityDueChip(text: ActivityFormat.dueChip(due), urgent: DemoDate.daysFromToday(due) <= 3)
                .accessibilityLabel(DemoDate.dueLabel(due))
        }
    }

    private var subtitle: String {
        item.detail.localizedCaseInsensitiveContains(item.entity) ? item.detail : "\(item.detail) · \(item.entity)"
    }

    @ViewBuilder private var buttons: some View {
        let layout = typeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(spacing: 8))
            : AnyLayout(HStackLayout(spacing: 8))
        layout {
            Button(item.primaryAction, action: onPrimary)
                .buttonStyle(.dn(.primary, size: .medium))
            if let secondary = item.secondaryAction {
                Button(secondary, action: onSecondary)
                    .buttonStyle(.dn(.secondary, size: .medium, fullWidth: typeSize.isAccessibilitySize))
                    .accessibilityHint(secondary == "Later" ? "Moves this to the end of the list" : "")
            }
        }
    }
}

/// "3 days" / "Due tomorrow" — a tinted pill with no dot (Paper's Needs you chip).
struct ActivityDueChip: View {
    var text: String
    var urgent: Bool = true

    var body: some View {
        Text(text)
            .dnFont(.footnote)
            .fontWeight(.semibold)
            .foregroundStyle(urgent ? DNColor.statusWarningForeground : DNColor.textSecondary)
            .lineLimit(1)
            .padding(.horizontal, 10)
            .frame(minHeight: 26)
            .background(urgent ? DNColor.statusWarningSurface : DNColor.surfaceSecondary, in: .capsule)
            .fixedSize()
    }
}

// MARK: - Status line (dot + text)

struct ActivityStatusLine: View {
    var text: String
    var tone: StatusChip.Tone

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            Circle()
                .fill(tone.activityColor)
                .frame(width: 7, height: 7)
                .alignmentGuide(.firstTextBaseline) { d in d[.bottom] - 1 }
            Text(text)
                .dnFont(.footnote)
                .fontWeight(.medium)
                .foregroundStyle(tone.activityColor)
        }
    }
}

extension StatusChip.Tone {
    var activityColor: Color {
        switch self {
        case .warning: DNColor.statusWarningForeground
        case .info: DNColor.statusInfoForeground
        case .success: DNColor.statusSuccessForeground
        case .error: DNColor.statusErrorForeground
        case .neutral: DNColor.textSecondary
        }
    }
}

// MARK: - Rows

/// In progress: icon · title · status line (→ tracker).
struct ActivityRequestRow: View {
    let request: RequestItem

    var body: some View {
        let status = request.activityStatus()
        HStack(spacing: 12) {
            HueIcon(symbol: request.symbol, hue: request.hue, size: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(request.title)
                    .dnFont(.rowTitle)
                    .foregroundStyle(DNColor.listRowTitle)
                ActivityStatusLine(text: status.text, tone: status.tone)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, 6)
        .accessibilityElement(children: .combine)
    }
}

/// Drafts in the All list: icon · title · "2 of 4 steps · saved 18 Sep" · Continue.
struct ActivityDraftRow: View {
    let draft: DraftItem
    var isPinned: Bool

    var body: some View {
        HStack(spacing: 12) {
            HueIcon(symbol: draft.symbol, hue: draft.hue, size: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(draft.title)
                    .dnFont(.rowTitle)
                    .foregroundStyle(DNColor.listRowTitle)
                    .fixedSize(horizontal: false, vertical: true)
                HStack(spacing: 4) {
                    if isPinned {
                        Image(systemName: "pin.fill")
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundStyle(DNColor.textSecondary)
                            .accessibilityLabel("Pinned")
                    }
                    Text("\(draft.stepsDone) of \(draft.stepsTotal) steps · saved \(DemoDate.short(draft.savedOn))")
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.listRowSupporting)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text("Continue")
                .dnFont(.subheadline)
                .fontWeight(.medium)
                .foregroundStyle(DNColor.textLink)
        }
        .padding(.vertical, 6)
        .accessibilityElement(children: .combine)
        .accessibilityHint("Continues the draft")
    }
}

/// Drafts filter and the Drafts screen: the expanded card (progress, what's next, Continue / Start over).
struct ActivityDraftCard: View {
    let draft: DraftItem
    var isPinned: Bool
    var onContinue: () -> Void
    var onStartOver: () -> Void
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                HueIcon(symbol: draft.symbol, hue: draft.hue, size: 40)
                VStack(alignment: .leading, spacing: 2) {
                    Text(draft.title)
                        .dnFont(.rowTitle)
                        .fontWeight(.semibold)
                        .foregroundStyle(DNColor.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                    Text([draft.activityOwner, "saved \(DemoDate.short(draft.savedOn))"].compactMap { $0 }.joined(separator: " · "))
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textSecondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if isPinned {
                    Image(systemName: "pin.fill")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(DNColor.textSecondary)
                        .accessibilityLabel("Pinned")
                }
            }
            .accessibilityElement(children: .combine)
            VStack(alignment: .leading, spacing: 6) {
                SegmentedProgress(total: draft.stepsTotal, done: draft.stepsDone, currentIsActive: false, doneColor: DNColor.actionAccent)
                Text(draft.activityNextStep)
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
            }
            .accessibilityElement(children: .combine)
            let layout = typeSize.isAccessibilitySize
                ? AnyLayout(VStackLayout(spacing: 8))
                : AnyLayout(HStackLayout(spacing: 8))
            layout {
                Button("Continue", action: onContinue)
                    .buttonStyle(.dn(.primary, size: .medium))
                Button("Start over", action: onStartOver)
                    .buttonStyle(.dn(.secondary, size: .medium, fullWidth: typeSize.isAccessibilitySize))
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DNColor.surfacePrimary, in: .rect(cornerRadius: DNRadius.brief, style: .continuous))
    }
}

/// History: icon · title (×6) · date · entity · amount right-aligned.
struct ActivityHistoryRow: View {
    let receipt: Receipt
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        let layout = typeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: 6))
            : AnyLayout(HStackLayout(spacing: 12))
        layout {
            HueIcon(symbol: receipt.symbol, hue: receipt.hue, size: 40)
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(receipt.shortTitle)
                        .dnFont(.rowTitle)
                        .foregroundStyle(DNColor.listRowTitle)
                        .fixedSize(horizontal: false, vertical: true)
                    if let count = receipt.count, count > 1 {
                        Text("×\(count)")
                            .dnFont(.caption1)
                            .fontDesign(.rounded)
                            .foregroundStyle(DNColor.textSecondary)
                            .padding(.horizontal, 6)
                            .frame(minHeight: 20)
                            .background(DNColor.surfaceSecondary, in: .capsule)
                            .accessibilityLabel("\(count) payments")
                    }
                }
                Text(subtitle)
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.listRowSupporting)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text(receipt.amount.map { Money.aed($0) } ?? "Sent")
                .dnFont(.numericUnit)
                .foregroundStyle(receipt.amount == nil ? DNColor.textSecondary : DNColor.textPrimary)
                .lineLimit(1)
                .fixedSize()
        }
        .padding(.vertical, 2)
        .accessibilityElement(children: .combine)
    }

    private var subtitle: String {
        let date = DemoDate.daysFromToday(receipt.date) >= -1 ? ActivityFormat.whenShort(receipt.date) : DemoDate.short(receipt.date)
        return "\(date) · \(receipt.entity)"
    }
}

// MARK: - Headers and notices

/// "Needs you · Pay both", "In progress · with government": the section title with a trailing
/// note or action, which moves under the title at accessibility text sizes.
struct ActivitySectionHeader<Accessory: View>: View {
    var title: String
    @ViewBuilder var accessory: Accessory
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        let layout = typeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: 2))
            : AnyLayout(HStackLayout(alignment: .firstTextBaseline, spacing: 8))
        layout {
            Text(title)
                .dnFont(.section)
                .foregroundStyle(DNColor.textPrimary)
                .accessibilityAddTraits(.isHeader)
            if !typeSize.isAccessibilitySize { Spacer(minLength: 8) }
            accessory
        }
        .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
    }
}

/// "OCTOBER" — month headers in History.
struct ActivityMonthHeader: View {
    var title: String
    var body: some View {
        Text(title.uppercased())
            .dnFont(.overline)
            .foregroundStyle(DNColor.textGroupedHeader)
            .accessibilityAddTraits(.isHeader)
    }
}

/// The blue notice at the top of Drafts.
struct ActivityInfoNotice: View {
    var symbol: String = "clock"
    var text: String

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: symbol)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(DNColor.statusInfoForeground)
                .padding(.top, 1)
                .accessibilityHidden(true)
            Text(text)
                .dnFont(.subheadline)
                .foregroundStyle(DNColor.blue800)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DNColor.statusInfoSurface, in: .rect(cornerRadius: 18, style: .continuous))
    }
}

/// A quiet empty state for a filter with nothing in it.
struct ActivityEmptyState: View {
    var symbol: String
    var title: String
    var message: String

    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: symbol)
                .font(.system(size: 34, weight: .regular))
                .foregroundStyle(DNColor.iconTertiary)
                .accessibilityHidden(true)
            Text(title)
                .dnFont(.headline)
                .foregroundStyle(DNColor.textPrimary)
            Text(message)
                .dnFont(.subheadline)
                .foregroundStyle(DNColor.textSecondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
        .padding(.horizontal, 16)
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Guest

/// Black UAE PASS pill with the fingerprint mark.
struct ActivityUAEPassButton: View {
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Text("Sign in with UAE PASS")
                .frame(maxWidth: .infinity)
                .overlay(alignment: .leading) {
                    Image(systemName: "touchid")
                        .font(.system(size: 22, weight: .regular))
                        .accessibilityHidden(true)
                }
        }
        .buttonStyle(.dn(.inverse))
    }
}

/// Guest opened Activity (Paper WZQ-0): requests, receipts and drafts belong to an account.
struct ActivityGuestSheet: View {
    var onSignIn: () -> Void
    var onNotNow: () -> Void
    @State private var height: CGFloat = 380

    var body: some View {
        VStack(spacing: 20) {
            ActivityGuestTile()
            VStack(spacing: 6) {
                Text("Sign in to see your activity")
                    .dnFont(.title2)
                    .foregroundStyle(DNColor.textPrimary)
                    .multilineTextAlignment(.center)
                Text("Your requests, receipts and drafts belong to your account. After you sign in, you come straight back here.")
                    .dnFont(.subheadline)
                    .foregroundStyle(DNColor.textSecondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            VStack(spacing: 4) {
                ActivityUAEPassButton(action: onSignIn)
                Button("Not now", action: onNotNow)
                    .dnFont(.body)
                    .foregroundStyle(DNColor.textLink)
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .contentShape(.rect)
                    .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, DNSpace.sheetPadding)
        .padding(.top, 28)
        .padding(.bottom, 8)
        .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { height = $0 }
        .presentationDetents([.height(max(height, 300)), .large])
        .presentationDragIndicator(.visible)
    }
}

struct ActivityGuestTile: View {
    var body: some View {
        RoundedRectangle(cornerRadius: DNRadius.tile, style: .continuous)
            .fill(Hue.mint.gradient)
            .overlay(
                Image(systemName: "clock.arrow.circlepath")
                    .font(.system(size: 26, weight: .semibold))
                    .foregroundStyle(Hue.mint.ink)
            )
            .frame(width: 64, height: 64)
            .accessibilityHidden(true)
    }
}
