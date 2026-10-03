import SwiftUI

/// Everything paid or sent, grouped by month (newest first), with Export (CSV).
struct HistoryView: View {
    @Environment(DemoStore.self) private var store

    var body: some View {
        List {
            ActivityHistorySections(receipts: store.state.history, showsTitle: false)
        }
        .listStyle(.insetGrouped)
        .listSectionSpacing(20)
        .scrollContentBackground(.hidden)
        .dnCanvasBackground()
        .overlay {
            if store.state.history.isEmpty {
                ActivityEmptyState(
                    symbol: "clock.arrow.circlepath",
                    title: "Nothing here yet",
                    message: "When you pay or send something, the receipt is kept here."
                )
            }
        }
        .navigationTitle("History")
        .navigationBarTitleDisplayMode(.large)
        .dnPushedToolbar()
        .toolbar {
            if !store.state.history.isEmpty {
                ToolbarItem(placement: .topBarTrailing) {
                    ActivityExportButton(receipts: store.state.history, style: .toolbar)
                }
            }
        }
    }
}

// MARK: - Sections (used by the Activity tab and the History screen)

struct ActivityHistorySections: View {
    var receipts: [Receipt]
    /// Show the "History · Export" header above the first month (Activity tab).
    var showsTitle: Bool
    var limit: Int? = nil
    var onSeeAll: (() -> Void)? = nil

    @Environment(Router.self) private var router

    var body: some View {
        let sorted = receipts.sorted { $0.date > $1.date }
        let shown = limit.map { Array(sorted.prefix($0)) } ?? sorted
        let months = ActivityFormat.months(shown)
        ForEach(Array(months.enumerated()), id: \.element.id) { index, month in
            Section {
                ForEach(month.receipts) { receipt in
                    Button {
                        router.push(.receipt(receipt.id))
                    } label: {
                        ActivityHistoryRow(receipt: receipt)
                    }
                    .accessibilityHint("Opens the receipt")
                    .id("rec-\(receipt.id)")
                }
            } header: {
                VStack(alignment: .leading, spacing: 10) {
                    if index == 0 && showsTitle {
                        ActivitySectionHeader(title: "History") {
                            ActivityExportButton(receipts: receipts, style: .inline)
                        }
                    }
                    ActivityMonthHeader(title: month.title)
                        .padding(.horizontal, 4)
                }
                .activitySectionHeader()
            } footer: {
                if index == months.count - 1, let onSeeAll, shown.count < sorted.count {
                    DNTextButton(title: "See all \(sorted.count)", action: onSeeAll)
                }
            }
        }
    }
}

/// "Export" → share a CSV of everything in History.
struct ActivityExportButton: View {
    enum Style { case inline, toolbar }
    var receipts: [Receipt]
    var style: Style
    @State private var url: URL?

    var body: some View {
        Group {
            if let url {
                ShareLink(item: url, preview: SharePreview("DubaiNow history", image: Image(systemName: "tablecells"))) {
                    label
                }
            } else {
                label.opacity(0.4)
            }
        }
        .accessibilityLabel("Export history")
        .accessibilityHint("Shares a spreadsheet of your payments")
        .task(id: receipts.map(\.id)) { url = ActivityExport.writeCSV(receipts) }
    }

    @ViewBuilder private var label: some View {
        switch style {
        case .inline:
            Text("Export")
                .dnFont(.subheadline)
                .foregroundStyle(DNColor.textLink)
                .frame(minHeight: 44)
                .contentShape(.rect)
        case .toolbar:
            Text("Export")
        }
    }
}
