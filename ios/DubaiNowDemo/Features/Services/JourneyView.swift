import SwiftUI

/// A journey page ("Start to finish"), laid out like Paper C1 `DP3-0` (Buy a used car):
/// overline, name, description, scale, progress when started, then the stages as a
/// vertical step list with their services and the steps that aren't in DubaiNow yet.
struct JourneyView: View {
    var serviceID: String

    var body: some View {
        if let service = Catalogue.shared.service(serviceID) {
            switch service.type {
            case .journey: ServicesJourneyPage(service: service)
            case .bundle: ServicesBundlePage(service: service)
            case .standalone: ServiceStartView(serviceID: serviceID)
            }
        } else {
            ServicesMissingRecord(id: serviceID)
        }
    }
}

struct ServicesJourneyPage: View {
    var service: CatalogueService

    @Environment(DemoStore.self) private var store
    @Environment(Router.self) private var router
    @Environment(ToastCenter.self) private var toasts

    /// "After a death" is told quietly: no hue, no accent, no celebratory words.
    private var isSensitive: Bool { service.id == "after-a-death" }
    private var stages: [CatalogueService.Stage] { service.stages ?? [] }
    private var progress: JourneyProgress? { store.servicesJourney(service.id) }
    private var accent: Color { isSensitive ? DNColor.neutral600 : DNColor.actionAccent }

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    header
                    if let progress { progressCard(progress) }
                    primaryButton
                    stepList
                        .padding(.top, 6)
                        .id("steps")
                    if let entities = service.entities, !entities.isEmpty {
                        VStack(alignment: .leading, spacing: 10) {
                            ServicesPageHeading(title: "Who's involved")
                            ServicesFlowLayout {
                                ForEach(entities, id: \.self) { ServicesInfoChip(symbol: "building.columns", text: $0) }
                            }
                        }
                    }
                    VStack(alignment: .leading, spacing: 14) {
                        Rectangle().fill(DNColor.borderSubtle).frame(height: 1)
                        Text("DubaiNow keeps the plan. Each step is run by its own entity.")
                            .dnFont(.footnote)
                            .foregroundStyle(DNColor.textSecondary)
                    }
                }
                .padding(.horizontal, DNSpace.insetPage)
                .padding(.top, 8)
                .padding(.bottom, 32)
            }
            .servicesLaunchScroll(proxy)
        }
        .background {
            if isSensitive {
                DNColor.surfacePrimary.ignoresSafeArea()
            } else {
                ServicesHueWash(color: service.hue.start.opacity(0.7), base: DNColor.surfacePrimary, height: 300)
            }
        }
        .navigationTitle(service.shortName)
        .navigationBarTitleDisplayMode(.inline)
        .dnPushedToolbar()
        .toolbar { ServicesRecordToolbar(service: service, store: store, router: router, toasts: toasts) }
    }

    // MARK: Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                RouteMark(color: accent, size: 16)
                Text("Start to finish".uppercased())
                    .dnFont(.overline)
                    .foregroundStyle(accent)
            }
            .accessibilityElement(children: .combine)
            Text(service.name)
                .dnFont(.title1)
                .foregroundStyle(DNColor.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            if isSensitive {
                Text("We're sorry for your loss. You can take these steps in your own time and come back to them whenever you need to.")
                    .dnFont(.callout)
                    .foregroundStyle(DNColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Text(service.description)
                .dnFont(.callout)
                .foregroundStyle(DNColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            ServicesFlowLayout {
                ServicesInfoChip(symbol: "list.number", text: ServicesText.count(stages.count, "stage"))
                if let e = service.entities, !e.isEmpty {
                    ServicesInfoChip(symbol: "building.columns", text: ServicesText.count(e.count, "entity", "entities"))
                }
                if let lasts = service.lasts {
                    ServicesInfoChip(symbol: "clock", text: lasts)
                }
            }
            .padding(.top, 6)
        }
    }

    // MARK: Progress

    private func progressCard(_ p: JourneyProgress) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline) {
                Text("You're at stage \(p.stageIndex + 1) of \(stages.count)")
                    .dnFont(.rowTitle)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                Spacer(minLength: 8)
                Text("from your activity")
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
            }
            SegmentedProgress(
                total: max(stages.count, 1), done: p.stageIndex,
                doneColor: isSensitive ? DNColor.neutral600 : DNColor.statusSuccessForeground,
                currentColor: accent, trackColor: DNColor.neutral300, height: 6
            )
            Text([p.headline, p.waitingOn].joined(separator: " · "))
                .dnFont(.footnote)
                .foregroundStyle(DNColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .background(DNColor.surfaceSecondary, in: .rect(cornerRadius: DNRadius.card, style: .continuous))
        .accessibilityElement(children: .combine)
    }

    // MARK: Primary action

    @ViewBuilder private var primaryButton: some View {
        if let p = progress, stages.indices.contains(p.stageIndex) {
            let next = stages[p.stageIndex].parts.compactMap { Catalogue.shared.service($0) }.first
            Button {
                if let next { router.open(service: next) } else { startSheet() }
            } label: {
                HStack(spacing: 8) {
                    Text("Continue: \(p.headline)")
                    Image(systemName: "arrow.right").font(.system(size: 15, weight: .bold))
                }
            }
            .buttonStyle(.dn(.primary))
        } else {
            Button {
                startSheet()
            } label: {
                HStack(spacing: 8) {
                    Text(isSensitive ? "Start when you're ready" : "Start this journey")
                    if !isSensitive { Image(systemName: "arrow.right").font(.system(size: 15, weight: .bold)) }
                }
            }
            .buttonStyle(.dn(isSensitive ? .inverse : .primary))
        }
    }

    private func startSheet() {
        router.present(.comingSoon(ServicesData.comingSoon(
            service, kind: .servicesPhase,
            detail: isSensitive
                ? "Soon DubaiNow will keep these steps together for you, in order, and tell each entity once."
                : "Soon DubaiNow will keep this plan for you: what's done, what's next and who it's waiting on."
        )))
    }

    // MARK: Step list

    private var stepList: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(stages.enumerated()), id: \.offset) { index, stage in
                stageRow(stage, index: index)
            }
        }
    }

    private enum StageState { case done, current, upcoming }

    private func state(_ index: Int) -> StageState {
        guard let p = progress else { return .upcoming }
        if index < p.stageIndex { return .done }
        if index == p.stageIndex { return .current }
        return .upcoming
    }

    private func stageRow(_ stage: CatalogueService.Stage, index: Int) -> some View {
        let st = state(index)
        let isLast = index == stages.count - 1
        let parts = stage.parts.compactMap { Catalogue.shared.service($0) }
        return HStack(alignment: .top, spacing: 14) {
            VStack(spacing: 0) {
                marker(st, number: index + 1)
                if !isLast {
                    Rectangle()
                        .fill(st == .done ? (isSensitive ? DNColor.neutral600 : DNColor.statusSuccessForeground) : DNColor.neutral300)
                        .frame(width: 2)
                        .frame(maxHeight: .infinity)
                }
            }
            .frame(width: 32)

            VStack(alignment: .leading, spacing: 8) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(stage.name)
                        .dnFont(.headline)
                        .fontWeight(st == .current ? .bold : .semibold)
                        .foregroundStyle(DNColor.textPrimary)
                    if let detail = stageDetail(stage, state: st) {
                        Text(detail)
                            .dnFont(.subheadline)
                            .foregroundStyle(DNColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .accessibilityElement(children: .combine)
                .accessibilityLabel("Stage \(index + 1) of \(stages.count), \(stage.name)\(st == .done ? ", done" : st == .current ? ", you're here" : ""). \(stageDetail(stage, state: st) ?? "")")
                ForEach(parts) { part in
                    if st == .current {
                        currentPart(part)
                    } else {
                        ServicesLink(title: part.name, weight: .semibold) { router.open(service: part) }
                    }
                }
                ForEach(stage.notInDubaiNowYet, id: \.self) { missing in
                    missingRow(missing)
                }
            }
            .padding(.top, 4)
            .padding(.bottom, isLast ? 8 : 24)
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    private func stageDetail(_ stage: CatalogueService.Stage, state: StageState) -> String? {
        switch state {
        case .done: return "Done"
        case .current: return progress?.waitingOn
        case .upcoming: return stage.takes
        }
    }

    @ViewBuilder private func marker(_ st: StageState, number: Int) -> some View {
        switch st {
        case .done:
            Circle()
                .fill(isSensitive ? DNColor.neutral600 : DNColor.statusSuccessForeground)
                .overlay(Image(systemName: "checkmark").font(.system(size: 14, weight: .heavy)).foregroundStyle(.white))
                .frame(width: 32, height: 32)
                .accessibilityHidden(true)
        case .current:
            Circle()
                .fill(accent)
                .overlay(Text("\(number)").font(.system(size: 16, weight: .bold, design: .rounded)).foregroundStyle(.white))
                .frame(width: 32, height: 32)
                .background(Circle().fill(accent.opacity(0.25)).padding(-5))
                .accessibilityHidden(true)
        case .upcoming:
            Circle()
                .fill(DNColor.surfacePrimary)
                .overlay(Circle().strokeBorder(DNColor.neutral300, lineWidth: 1.5))
                .overlay(Text("\(number)").font(.system(size: 16, weight: .bold, design: .rounded)).foregroundStyle(DNColor.textPrimary))
                .frame(width: 32, height: 32)
                .accessibilityHidden(true)
        }
    }

    private func currentPart(_ part: CatalogueService) -> some View {
        Button {
            router.open(service: part)
        } label: {
            HStack(spacing: 10) {
                Text(part.name)
                    .dnFont(.rowTitle)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textLink)
                    .multilineTextAlignment(.leading)
                Spacer(minLength: 8)
                Image(systemName: "arrow.right")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(DNColor.actionAccent)
                    .accessibilityHidden(true)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(minHeight: 48)
            .background(DNColor.actionAccentSubtle, in: .rect(cornerRadius: 16, style: .continuous))
            .contentShape(.rect(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(.dnPressable)
    }

    private func missingRow(_ missing: CatalogueService.Missing) -> some View {
        Button {
            router.present(.comingSoon(ComingSoon(
                title: missing.name,
                detail: "\(missing.owner ?? "Its owner") handles this outside DubaiNow for now. It joins this journey when it's in DubaiNow.",
                owner: missing.owner, symbol: "arrow.up.forward.square", hue: isSensitive ? .lilac : service.hue, kind: .notInDemo
            )))
        } label: {
            HStack(alignment: .top, spacing: 10) {
                Image(systemName: "arrow.up.forward.square")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(DNColor.textSecondary)
                    .padding(.top, 2)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    Text(missing.name)
                        .dnFont(.callout)
                        .foregroundStyle(DNColor.textPrimary)
                        .multilineTextAlignment(.leading)
                    Text("\(missing.owner ?? "Owner to confirm") · not in DubaiNow yet")
                        .dnFont(.footnote)
                        .foregroundStyle(DNColor.textSecondary)
                }
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(DNColor.neutral300, style: StrokeStyle(lineWidth: 1, dash: [4, 3]))
            )
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityHint("Not in DubaiNow yet")
    }
}
