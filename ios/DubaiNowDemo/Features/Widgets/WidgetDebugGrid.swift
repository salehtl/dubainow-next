import SwiftUI

/// TEMPORARY (Widgets builder): every widget kind at every size it's designed in, laid out on the
/// Home grid, so the widgets can be checked before Home lands. Shown in place of the gallery when
/// the app launches with `-sheet widgetGallery -widgetsDebug`.
///   -widgetsDebugOnly parking,salik   only these kinds (parking salik nol metro prayer weather energy bills family journey request)
///   -widgetsDebugParking ending|expired|none   force a parking state
///   -widgetsDebugPaid                          pay everything first (Bills and fines "all paid")
struct WidgetDebugGrid: View {
    @Environment(DemoStore.self) private var store
    @State private var width: CGFloat = 402

    private var only: Set<String>? {
        UserDefaults.standard.string(forKey: "widgetsDebugOnly").map { Set($0.split(separator: ",").map(String.init)) }
    }

    private var widgets: [HomeWidget] {
        let journeyID = store.state.journeys.first?.journeyID ?? "move-home"
        let requestID = store.state.requests.first?.id ?? "sara-visa"
        let all: [(String, WidgetKind)] = [
            ("parking", .parking), ("salik", .salik), ("prayer", .prayer), ("weather", .weather),
            ("nol", .nol), ("request", .visaTracker(requestID)), ("bills", .billsAndFines), ("family", .familyVisas),
            ("metro", .metro), ("energy", .energy), ("journey", .journey(journeyID)),
        ]
        return all.filter { only?.contains($0.0) ?? true }.flatMap { name, kind in
            kind.widgetSizes.map { HomeWidget(id: "debug-\(name)-\($0.rawValue)", kind: kind, size: $0) }
        }
        .sorted { order($0.size) < order($1.size) }
    }

    private func order(_ s: WidgetSize) -> Int { s == .small ? 0 : (s == .medium ? 1 : 2) }

    var body: some View {
        NavigationStack {
            ScrollView {
                let unit = max(120, (width - DNSpace.insetPage * 2 - DNSpace.widgetGutter) / 2)
                let wide = unit * 2 + DNSpace.widgetGutter
                VStack(spacing: DNSpace.widgetGutter) {
                    ForEach(rows, id: \.self) { row in
                        HStack(spacing: DNSpace.widgetGutter) {
                            ForEach(row) { w in
                                WidgetView(widget: w)
                                    .frame(width: w.size == .small ? unit : wide,
                                           height: w.size == .large ? wide : unit)
                            }
                            if row.count == 1, row[0].size == .small { Spacer(minLength: 0) }
                        }
                    }
                }
                .padding(.horizontal, DNSpace.insetPage)
                .padding(.vertical, 12)
            }
            .onGeometryChange(for: CGFloat.self) { $0.size.width } action: { width = $0 }
            .background(DNColor.surfaceSecondary)
            .navigationTitle("Widgets · debug")
            .navigationBarTitleDisplayMode(.inline)
        }
        .presentationDetents([.large])
        .onAppear(perform: applyOverrides)
    }

    private var rows: [[HomeWidget]] {
        var rows: [[HomeWidget]] = []
        var pending: HomeWidget?
        for w in widgets {
            if w.size == .small {
                if let p = pending { rows.append([p, w]); pending = nil } else { pending = w }
            } else {
                if let p = pending { rows.append([p]); pending = nil }
                rows.append([w])
            }
        }
        if let p = pending { rows.append([p]) }
        return rows
    }

    private func applyOverrides() {
        let defaults = UserDefaults.standard
        if let mode = defaults.string(forKey: "widgetsDebugParking"), var p = store.state.parking {
            switch mode {
            case "ending": p.endsAt = DemoDate.minutes(8)
            case "expired": p.endsAt = DemoDate.minutes(-3)
            case "none": store.state.parking = nil; return
            default: break
            }
            store.state.parking = p
        }
        if ProcessInfo.processInfo.arguments.contains("-widgetsDebugPaid") {
            store.completePayment(store.payAllRequest())
        }
    }
}
