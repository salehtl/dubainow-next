import SwiftUI

/// One row for any catalogue record, used by Services and Search.
/// - Standalone: hue disc · name · owner (no badge).
/// - Bundle: stacked discs · name · "All at once" · count line.
/// - Journey: route disc · name · "Start to finish" · scale line.
struct CatalogueRow: View {
    enum Trailing { case chevron, favorite, none }

    var service: CatalogueService
    var trailing: Trailing = .chevron
    var showsIcon: Bool = true
    /// Extra personal context ("due in 12 days", "you have 1 unpaid"), shown after the owner.
    var context: String? = nil
    var contextTone: StatusChip.Tone = .neutral
    /// Bold the matching part of the name (search).
    var highlight: String? = nil

    @Environment(DemoStore.self) private var store
    @Environment(ToastCenter.self) private var toasts

    var body: some View {
        HStack(spacing: 12) {
            if showsIcon { leading }
            VStack(alignment: .leading, spacing: 3) {
                title
                    .dnFont(.rowTitle)
                    .foregroundStyle(DNColor.listRowTitle)
                    .multilineTextAlignment(.leading)
                if service.type != .standalone {
                    ServiceTypeBadge(type: service.type)
                    if let line = secondaryLine {
                        Text(line).dnFont(.footnote).foregroundStyle(DNColor.listRowSupporting).lineLimit(2)
                    }
                } else {
                    meta
                }
            }
            Spacer(minLength: 8)
            trailingView
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(minHeight: 64)
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
        .accessibilityHint(service.type == .bundle ? "All at once" : service.type == .journey ? "Start to finish" : "")
    }

    @ViewBuilder private var leading: some View {
        switch service.type {
        case .standalone:
            HueIcon(symbol: service.symbol, hue: service.hue, size: 40)
        case .bundle:
            BundleDiscs(service: service, size: 40)
        case .journey:
            HueIcon(symbol: "point.topleft.down.to.point.bottomright.curvepath", hue: service.hue, size: 40)
        }
    }

    private var title: Text {
        guard let h = highlight, !h.isEmpty,
              let range = service.name.range(of: h, options: [.caseInsensitive, .diacriticInsensitive]) else {
            return Text(service.name)
        }
        var attributed = AttributedString(service.name)
        if let r = Range(range, in: attributed) {
            // An explicit font on the run survives the row's .dnFont (an intent alone doesn't).
            attributed[r].font = DNTextStyle.rowTitle.font.weight(.bold)
        }
        return Text(attributed)
    }

    private var meta: some View {
        HStack(spacing: 0) {
            Text(service.ownerLabel)
                .foregroundStyle(DNColor.listRowSupporting)
            if let status = service.statusLabel {
                Text(" · ").foregroundStyle(DNColor.listRowSupporting)
                Text(status).foregroundStyle(service.status == "maintenance" ? DNColor.statusWarningForeground : DNColor.statusInfoForeground)
            }
            if let context {
                Text(" · ").foregroundStyle(DNColor.listRowSupporting)
                Text(context).foregroundStyle(contextColor)
            }
        }
        .dnFont(.footnote)
        .lineLimit(2)
    }

    private var contextColor: Color {
        switch contextTone {
        case .warning: DNColor.statusWarningForeground
        case .error: DNColor.statusErrorForeground
        case .success: DNColor.statusSuccessForeground
        case .info: DNColor.statusInfoForeground
        case .neutral: DNColor.listRowSupporting
        }
    }

    private var secondaryLine: String? {
        switch service.type {
        case .bundle: service.covers.map { "Covers \($0)" }
        case .journey: service.scaleLine
        case .standalone: nil
        }
    }

    @ViewBuilder private var trailingView: some View {
        switch trailing {
        case .chevron:
            Image(systemName: "chevron.forward")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(DNColor.listRowChevron)
                .accessibilityHidden(true)
        case .favorite:
            let isFav = store.isFavorite(serviceID: service.id)
            Button {
                if store.toggleFavorite(service) {
                    if !isFav {
                        DNHaptics.success()
                        toasts.show("Added to Favorites", symbol: "star.fill", undo: { store.toggleFavorite(service) })
                    } else {
                        DNHaptics.light()
                    }
                } else {
                    DNHaptics.warning()
                    toasts.show("Favorites is full (8 of 8). Remove one first.", symbol: "exclamationmark.circle.fill")
                }
            } label: {
                Image(systemName: isFav ? "star.fill" : "star")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(isFav ? DNColor.actionAccent : DNColor.iconSecondary)
                    .frame(width: 44, height: 44)
                    .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(isFav ? "Remove from Favorites" : "Add to Favorites")
        case .none:
            EmptyView()
        }
    }
}

/// A cluster of a bundle's first three parts (the "All at once" visual): three small hue
/// discs in a triangle, each with a white ring, inside a square of `size`.
struct BundleDiscs: View {
    var service: CatalogueService
    var size: CGFloat = 40

    var body: some View {
        let parts = Array(service.resolvedParts.prefix(3))
        let disc = size * 0.56
        let positions: [CGPoint] = [
            CGPoint(x: disc / 2, y: disc / 2),
            CGPoint(x: size - disc / 2, y: disc / 2 + size * 0.06),
            CGPoint(x: size * 0.42, y: size - disc / 2),
        ]
        ZStack {
            ForEach(Array(parts.enumerated()), id: \.offset) { i, part in
                HueIcon(symbol: part.symbol, hue: part.hue, size: disc)
                    .overlay(Circle().strokeBorder(DNColor.surfacePrimary, lineWidth: 1.5))
                    .position(positions[i])
            }
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

/// A journey as a card (Browse shelf, topic pages): hue wash, "Start to finish",
/// the scale line and its stages as a bar. Pass `progress` when the resident has started it.
struct JourneyCard: View {
    var service: CatalogueService
    var progress: JourneyProgress? = nil
    var width: CGFloat? = 272

    var body: some View {
        let stages = service.stages ?? []
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                ServiceTypeBadge(type: .journey, onHue: true)
                Spacer()
                Image(systemName: service.symbol)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(service.hue.ink)
            }
            Text(service.name)
                .dnFont(.title3)
                .foregroundStyle(DNColor.textPrimary)
            Text(progress.map { "Stage \($0.stageIndex + 1) of \(stages.count) · \($0.headline)" } ?? (service.scaleLine ?? ""))
                .dnFont(.footnote)
                .foregroundStyle(DNColor.textOnHueSecondary)
                .lineLimit(2)
            Spacer(minLength: 0)
            SegmentedProgress(
                total: max(stages.count, 1),
                done: progress?.stageIndex ?? 0,
                currentIsActive: progress != nil,
                doneColor: service.hue.ink,
                currentColor: service.hue.ink.opacity(0.55),
                trackColor: DNColor.progressTrackOnHue
            )
            Text(stages.map(\.name).joined(separator: " · "))
                .dnFont(.caption1)
                .foregroundStyle(DNColor.textOnHueSecondary)
                .lineLimit(1)
        }
        .padding(16)
        .frame(width: width, alignment: .topLeading)
        .frame(minHeight: 176, alignment: .topLeading)
        .background(service.hue.gradient, in: .rect(cornerRadius: DNRadius.card, style: .continuous))
        .dnElevation()
        .accessibilityElement(children: .combine)
    }
}

/// A bundle as a card: white, stacked discs, "All at once", the count line.
struct BundleCard: View {
    var service: CatalogueService
    /// Personal count for the count line ("3 bills and fines, one payment"); nil shows "Covers N services".
    var count: Int? = nil
    var width: CGFloat? = 272

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                ServiceTypeBadge(type: .bundle)
                Spacer()
                BundleDiscs(service: service, size: 36)
            }
            Text(service.name)
                .dnFont(.title3)
                .foregroundStyle(DNColor.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
            Text(count.flatMap { service.countLine(n: $0) } ?? service.covers.map { "Covers \($0)" } ?? "")
                .dnFont(.footnote)
                .foregroundStyle(DNColor.textSecondary)
            Spacer(minLength: 0)
            Text(service.description)
                .dnFont(.caption1)
                .foregroundStyle(DNColor.textSecondary)
                .lineLimit(3)
        }
        .padding(16)
        .frame(width: width, alignment: .topLeading)
        .frame(minHeight: 176, alignment: .topLeading)
        .dnCard()
        .accessibilityElement(children: .combine)
    }
}
