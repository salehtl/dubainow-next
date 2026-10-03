import SwiftUI

// Start to finish: one widget per journey you've started (medium, large). Filled with the journey's
// hue, route mark, the stage as the figure, and a caption that always says whose turn it is.
// Plus the request tracker (a request in progress) and the certificates fallback.

// MARK: - Journey

struct WidgetJourneyContent: View {
    var journeyID: String
    var size: WidgetSize
    @Environment(DemoStore.self) private var store

    var body: some View {
        if let m = WidgetJourneyModel(id: journeyID, store: store) {
            if m.isQuiet {
                quiet(m)
            } else if size == .large {
                large(m)
            } else {
                medium(m)
            }
        } else {
            let service = Catalogue.shared.service(journeyID)
            WidgetCalmState(
                title: service?.shortName ?? "Journey",
                message: "You haven't started this journey. Start it from Services and it shows here."
            ) {
                WidgetGlyph(symbol: "point.topleft.down.to.point.bottomright.curvepath", ink: DNColor.textSecondary, fill: DNColor.neutral100)
            }
        }
    }

    private func header(_ m: WidgetJourneyModel, trailing: String? = nil) -> some View {
        let hue = m.service.hue
        return HStack(spacing: 10) {
            Circle()
                .fill(DNColor.white.opacity(0.72))
                .overlay(RouteMark(color: hue.ink, size: 18))
                .frame(width: 34, height: 34)
            VStack(alignment: .leading, spacing: 0) {
                WidgetTitle(m.service.shortName)
                Text(m.caption)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(hue.ink)
                    .lineLimit(1)
                    .minimumScaleFactor(0.85)
            }
            Spacer(minLength: 6)
            if let trailing {
                Text(trailing)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(hue.ink)
            }
        }
        .frame(height: 44)
    }

    private func figure(_ m: WidgetJourneyModel) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 4) {
            Text("\(m.number)")
                .font(.system(size: 44, weight: .bold, design: .rounded))
                .tracking(-0.8)
                .contentTransition(.numericText(value: Double(m.number)))
            Text("of \(m.total) · \(m.stageName)")
                .font(.system(size: 17, weight: .semibold, design: .rounded))
                .lineLimit(1)
                .minimumScaleFactor(0.75)
        }
        .foregroundStyle(DNColor.textPrimary)
        .animation(DNMotion.resolve(DNMotion.smooth), value: m.number)
    }

    @ViewBuilder
    private func turn(_ m: WidgetJourneyModel) -> some View {
        if m.progress.needsYou {
            WidgetChip(text: "1 thing needs you", symbol: "arrow.up", foreground: DNColor.textPrimary, background: DNColor.white)
        } else {
            WidgetCaption(m.progress.waitingOn, color: DNColor.textOnHueSecondary)
        }
    }

    private func bar(_ m: WidgetJourneyModel) -> some View {
        WidgetStagesBar(total: m.total, current: m.progress.stageIndex, ink: m.service.hue.ink, track: m.service.hue.ink.opacity(0.2))
    }

    private func medium(_ m: WidgetJourneyModel) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            header(m)
            figure(m).padding(.top, 2)
            turn(m)
            Spacer(minLength: 8)
            bar(m)
        }
        .padding(EdgeInsets(top: 12, leading: 16, bottom: 16, trailing: 16))
    }

    private func large(_ m: WidgetJourneyModel) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            header(m, trailing: m.age)
            figure(m).padding(.top, 2)
            turn(m)
            Spacer(minLength: 10)
            VStack(spacing: 3) {
                ForEach(Array(m.stages.enumerated()), id: \.offset) { i, stage in
                    stageRow(m, index: i, stage: stage)
                }
            }
            .padding(12)
            .background(DNColor.white.opacity(0.55), in: .rect(cornerRadius: 18, style: .continuous))
        }
        .padding(EdgeInsets(top: 12, leading: 16, bottom: 16, trailing: 16))
    }

    private func stageRow(_ m: WidgetJourneyModel, index i: Int, stage: CatalogueService.Stage) -> some View {
        let hue = m.service.hue
        let current = m.progress.stageIndex
        return HStack(spacing: 10) {
            Group {
                if i < current {
                    Circle().fill(hue.ink)
                        .overlay(Image(systemName: "checkmark").font(.system(size: 8, weight: .heavy)).foregroundStyle(DNColor.white))
                } else if i == current {
                    Circle().fill(DNColor.white)
                        .overlay(Circle().fill(hue.ink).frame(width: 8, height: 8))
                        .background(Circle().fill(hue.end).padding(-3))
                } else {
                    Circle().strokeBorder(hue.ink.opacity(0.35), lineWidth: 1.5)
                }
            }
            .frame(width: 16, height: 16)
            Text("\(i + 1) · \(stage.name)")
                .font(.system(size: 13, weight: i == current ? .bold : .regular))
                .foregroundStyle(i <= current ? DNColor.textPrimary : DNColor.textOnHueSecondary)
                .lineLimit(1)
            Spacer(minLength: 6)
            Text(i < current ? "Done" : (i == current ? "You are here" : (stage.takes ?? "")))
                .font(.system(size: 12, weight: i == current ? .bold : .regular))
                .foregroundStyle(i == current ? hue.ink : DNColor.textOnHueSecondary)
                .lineLimit(1)
        }
        .frame(height: 24)
    }

    /// Quiet tone for sensitive journeys: white, no hue, no big figure, no countdown.
    private func quiet(_ m: WidgetJourneyModel) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 10) {
                Circle().fill(DNColor.neutral100)
                    .overlay(RouteMark(color: DNColor.textSecondary, size: 18))
                    .frame(width: 34, height: 34)
                WidgetTitle(m.service.shortName)
            }
            .frame(height: 44)
            Spacer(minLength: 6)
            Text("Next: \(WidgetFormat.lowerFirst(m.progress.headline))")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(DNColor.textPrimary)
                .lineLimit(2)
            WidgetCaption("\(m.progress.stageIndex) of \(m.total) stages done · whenever you're ready")
            Spacer(minLength: 8)
            WidgetStagesBar(total: m.total, current: m.progress.stageIndex, ink: DNColor.neutral400, track: DNColor.neutral200)
        }
        .padding(EdgeInsets(top: 12, leading: 16, bottom: 16, trailing: 16))
    }
}

extension WidgetSpec {
    static func journey(id: String, store: DemoStore) -> WidgetSpec {
        guard let m = WidgetJourneyModel(id: id, store: store) else {
            let name = Catalogue.shared.service(id)?.shortName ?? "Journey"
            return WidgetSpec(summary: "\(name). Not started.", open: .route(.journey(id)))
        }
        if m.isQuiet {
            return WidgetSpec(summary: "\(m.service.shortName). Next: \(m.progress.headline). \(m.progress.stageIndex) of \(m.total) stages done.",
                              open: .route(.journey(id)))
        }
        return WidgetSpec(surface: .hue(m.service.hue), summary: m.spoken, hint: "Opens the journey", open: .route(.journey(id)))
    }
}

// MARK: - Request tracker

/// A request in progress (e.g. the visa renewal): days left on your turn, steps done.
struct WidgetRequestContent: View {
    var requestID: String
    @Environment(DemoStore.self) private var store

    var body: some View {
        if let r = store.widgetRequest(requestID) {
            let done = r.steps.filter { $0.state == .done }.count
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    WidgetGlyph(symbol: r.symbol, ink: r.hue.ink, fill: r.hue.gradient)
                    Spacer(minLength: 4)
                    WidgetChip(text: "\(done) of \(r.steps.count) done", foreground: r.hue.ink, background: r.hue.start)
                }
                .frame(height: 44)
                figure(r).padding(.top, 6)
                WidgetCaption(caption(r), lines: 2)
                Spacer(minLength: 6)
                WidgetStagesBar(total: r.steps.count, current: r.currentStepIndex ?? done, ink: r.hue.ink, track: DNColor.neutral200)
            }
            .padding(EdgeInsets(top: 12, leading: 16, bottom: 16, trailing: 12))
        } else {
            WidgetCalmState(title: "Request tracker", message: "Nothing in progress. Requests you start show here") {
                WidgetGlyph(symbol: "doc.text.fill", ink: Hue.lilac.ink, fill: Hue.lilac.gradient)
            }
        }
    }

    @ViewBuilder
    private func figure(_ r: RequestItem) -> some View {
        switch r.status {
        case .yourTurn(let due):
            let d = max(0, DemoDate.daysFromToday(due))
            WidgetFigure("\(d)", unit: d == 1 ? "day" : "days", numeric: Double(d))
        case .inReview(let by):
            let d = max(0, DemoDate.daysFromToday(by))
            WidgetFigure("\(d)", unit: d == 1 ? "day" : "days", numeric: Double(d))
        case .approved:
            WidgetFigure("Approved", size: 30)
        case .done:
            WidgetFigure("Done", size: 34)
        }
    }

    private func caption(_ r: RequestItem) -> String {
        switch r.status {
        case .yourTurn:
            let step = r.currentStepIndex.map { r.steps[$0].title } ?? r.title
            return "to " + WidgetFormat.lowerFirst(step)
        case .inReview: return "until \(r.entity) answers"
        case .approved(let note): return note.prefix(1).uppercased() + note.dropFirst()
        case .done: return r.title
        }
    }
}

extension WidgetSpec {
    static func request(id: String, store: DemoStore) -> WidgetSpec {
        guard let r = store.widgetRequest(id) else {
            return WidgetSpec(summary: "Request tracker. Nothing in progress.", open: .route(.history))
        }
        let done = r.steps.filter { $0.state == .done }.count
        return WidgetSpec(summary: "\(r.title). \(r.statusLine). \(done) of \(r.steps.count) steps done.",
                          hint: "Opens the request", open: .route(.request(id)))
    }
}

// MARK: - Certificates (bundle with nothing live)

/// Certificates and letters has no live state, so it isn't offered as a widget. If one is ever on
/// Home (old data), it says so calmly instead of disappearing.
struct WidgetCertificatesContent: View {
    var body: some View {
        WidgetCalmState(title: "Certificates and letters", message: "Nothing in progress. Ask for a certificate and it shows here") {
            WidgetBundleMark()
        }
    }
}
