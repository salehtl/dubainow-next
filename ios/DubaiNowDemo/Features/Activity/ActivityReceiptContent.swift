import SwiftUI

/// A stored receipt (Paper EII-0): paid tick, amount, rows, PDF, and the Happiness Meter faces.
/// Used by the payment sheet (after paying, with the tick drawing in and rows staggering),
/// the receipt sheet and the pushed receipt.
struct ActivityReceiptList: View {
    let receiptID: String
    var animateIn: Bool = false
    /// Pushed screens sit on the grey canvas; sheets keep the native grouped background.
    var onCanvas: Bool = false

    @Environment(DemoStore.self) private var store
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var revealed: Bool
    @State private var pdfURL: URL?

    init(receiptID: String, animateIn: Bool = false, onCanvas: Bool = false) {
        self.receiptID = receiptID
        self.animateIn = animateIn
        self.onCanvas = onCanvas
        _revealed = State(initialValue: !animateIn)
    }

    var body: some View {
        if let receipt = store.receipt(receiptID) {
            list(receipt)
        } else {
            ContentUnavailableView(
                "Receipt not found",
                systemImage: "doc.text.magnifyingglass",
                description: Text("It may belong to another resident, or the demo was reset.")
            )
            .modifier(ActivityCanvas(enabled: onCanvas))
        }
    }

    private func list(_ r: Receipt) -> some View {
        let details = detailRows(r)
        return List {
            Section {
                ActivityReceiptHero(receipt: r, animate: animateIn)
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
            }

            Section {
                ForEach(Array(details.enumerated()), id: \.offset) { index, row in
                    ActivityReceiptDetailRow(label: row.label, value: row.value, style: row.style)
                        .modifier(ActivityStagger(index: index, revealed: revealed))
                }
            }

            Section {
                ActivityReceiptPDFButton(receipt: r, url: pdfURL)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
                    .modifier(ActivityStagger(index: details.count, revealed: revealed))
            }

            Section {
                ActivityRatingCard(receipt: r)
                    .modifier(ActivityStagger(index: details.count + 1, revealed: revealed))
            }
        }
        .listStyle(.insetGrouped)
        .listSectionSpacing(16)
        .contentMargins(.top, 4, for: .scrollContent)
        .modifier(ActivityCanvas(enabled: onCanvas))
        .task(id: r.id) { pdfURL = ActivityExport.cachedPDF(r) }
        .onAppear {
            guard !revealed else { return }
            revealed = true
        }
    }

    private func detailRows(_ r: Receipt) -> [(label: String, value: String, style: ActivityReceiptDetailRow.Style)] {
        var rows: [(String, String, ActivityReceiptDetailRow.Style)] = [("When", ActivityFormat.whenLong(r.date), .plain)]
        if let m = r.method { rows.append(("Paid with", m.label, .plain)) }
        for row in r.rows {
            rows.append((row.label, row.value, row.value.hasPrefix("AED") ? .amount : .plain))
        }
        rows.append(("Reference", r.reference, .reference))
        return rows.map { (label: $0.0, value: $0.1, style: $0.2) }
    }
}

private struct ActivityCanvas: ViewModifier {
    var enabled: Bool
    func body(content: Content) -> some View {
        if enabled {
            content.scrollContentBackground(.hidden).dnCanvasBackground()
        } else {
            content
        }
    }
}

/// Rows arrive one after another (Paper motion: "receipt rows arrive one after another").
struct ActivityStagger: ViewModifier {
    let index: Int
    let revealed: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .opacity(revealed ? 1 : 0)
            .offset(y: revealed || reduceMotion ? 0 : 12)
            .animation(
                reduceMotion ? DNMotion.base : DNMotion.smooth.delay(0.3 + DNMotion.stagger(index) * 2),
                value: revealed
            )
    }
}

// MARK: - Hero

struct ActivityReceiptHero: View {
    let receipt: Receipt
    var animate: Bool

    var body: some View {
        VStack(spacing: 8) {
            ActivityPaidTick(animate: animate)
            Text(receipt.amount == nil ? "Sent" : "Paid")
                .dnFont(.subheadline)
                .fontWeight(.semibold)
                .foregroundStyle(DNColor.statusSuccessForeground)
            if let amount = receipt.amount {
                MoneyText(amount: amount, size: .numericHero)
                    .padding(.top, 2)
            }
            Text(receipt.title)
                .dnFont(.headline)
                .foregroundStyle(DNColor.textPrimary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            if receipt.doneByAssistant {
                HStack(spacing: 6) {
                    DNLogo(height: 16)
                        .accessibilityHidden(true)
                    Text("Done with your assistant")
                        .dnFont(.footnote)
                        .fontWeight(.semibold)
                        .foregroundStyle(DNColor.textPrimary)
                }
                .padding(.horizontal, 12)
                .frame(minHeight: 30)
                .background(DNColor.surfacePrimary, in: .capsule)
                .padding(.top, 4)
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 8)
        .padding(.bottom, 4)
        .accessibilityElement(children: .combine)
    }
}

/// The success disc; the tick draws in over 400 ms (instant with Reduce Motion).
struct ActivityPaidTick: View {
    var animate: Bool
    var size: CGFloat = 56
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var progress: CGFloat
    @State private var popped: Bool

    init(animate: Bool, size: CGFloat = 56) {
        self.animate = animate
        self.size = size
        _progress = State(initialValue: animate ? 0 : 1)
        _popped = State(initialValue: !animate)
    }

    var body: some View {
        Circle()
            .fill(DNColor.statusSuccessSurface)
            .overlay(
                ActivityCheckmark()
                    .trim(from: 0, to: progress)
                    .stroke(DNColor.statusSuccessForeground, style: StrokeStyle(lineWidth: size * 0.055, lineCap: .round, lineJoin: .round))
                    .frame(width: size * 0.5, height: size * 0.5)
            )
            .frame(width: size, height: size)
            .scaleEffect(popped ? 1 : 0.6)
            .onAppear {
                guard animate, progress == 0 else { return }
                if reduceMotion {
                    withAnimation(DNMotion.base) { progress = 1; popped = true }
                } else {
                    withAnimation(DNMotion.bouncy) { popped = true }
                    withAnimation(.easeOut(duration: 0.4).delay(0.08)) { progress = 1 }
                }
            }
            .accessibilityHidden(true)
    }
}

/// Paper's tick: M5 12.6 l4.2 4.2 9-9.5 on a 24 grid.
struct ActivityCheckmark: Shape {
    func path(in rect: CGRect) -> Path {
        let sx = rect.width / 24, sy = rect.height / 24
        var p = Path()
        p.move(to: CGPoint(x: 5 * sx, y: 12.6 * sy))
        p.addLine(to: CGPoint(x: 9.2 * sx, y: 16.8 * sy))
        p.addLine(to: CGPoint(x: 18.2 * sx, y: 7.3 * sy))
        return p
    }
}

// MARK: - Rows

struct ActivityReceiptDetailRow: View {
    enum Style { case plain, amount, reference }
    var label: String
    var value: String
    var style: Style = .plain

    var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .firstTextBaseline, spacing: 16) {
                labelText
                Spacer(minLength: 8)
                valueText.multilineTextAlignment(.trailing)
            }
            VStack(alignment: .leading, spacing: 2) {
                labelText
                valueText
            }
        }
        .frame(minHeight: 32)
        .accessibilityElement(children: .combine)
    }

    private var labelText: some View {
        Text(label)
            .dnFont(.subheadline)
            .foregroundStyle(DNColor.textSecondary)
            .lineLimit(2)
    }

    @ViewBuilder private var valueText: some View {
        switch style {
        case .plain:
            Text(value)
                .dnFont(.subheadline)
                .fontWeight(.semibold)
                .foregroundStyle(DNColor.textPrimary)
        case .amount:
            Text(value)
                .dnFont(.subheadline)
                .fontWeight(.bold)
                .fontDesign(.rounded)
                .foregroundStyle(DNColor.textPrimary)
        case .reference:
            Text(value)
                .dnFont(.footnote)
                .monospaced()
                .foregroundStyle(DNColor.textPrimary)
                .accessibilityLabel(value.map { String($0) }.joined(separator: " "))
                .contextMenu {
                    Button {
                        UIPasteboard.general.string = value
                    } label: {
                        Label("Copy reference", systemImage: "doc.on.doc")
                    }
                }
        }
    }
}

// MARK: - PDF

/// "Download receipt (PDF)" — a white pill that opens the share sheet with the generated PDF.
struct ActivityReceiptPDFButton: View {
    let receipt: Receipt
    let url: URL?

    var body: some View {
        Group {
            if let url {
                ShareLink(item: url, preview: SharePreview(receipt.title, image: Image(systemName: "doc.richtext"))) {
                    label
                }
            } else {
                label.opacity(0.5)
            }
        }
        .buttonStyle(.dnPressable)
    }

    private var label: some View {
        HStack(spacing: 8) {
            Image(systemName: "arrow.down.to.line")
                .font(.system(size: 16, weight: .semibold))
            Text("Download receipt (PDF)")
                .dnFont(.rowTitle)
                .fontWeight(.semibold)
        }
        .foregroundStyle(DNColor.textLink)
        .frame(maxWidth: .infinity, minHeight: 50)
        .background(DNColor.surfacePrimary, in: .capsule)
        .contentShape(.capsule)
    }
}

/// Toolbar share button: the PDF once it's rendered, plain text until then.
struct ActivityReceiptShareButton: View {
    let receiptID: String
    @Environment(DemoStore.self) private var store
    @State private var url: URL?

    var body: some View {
        if let r = store.receipt(receiptID) {
            Group {
                if let url {
                    ShareLink(item: url, preview: SharePreview(r.title, image: Image(systemName: "doc.richtext"))) {
                        Image(systemName: "square.and.arrow.up")
                    }
                } else {
                    ShareLink(item: ActivityExport.summary(r)) {
                        Image(systemName: "square.and.arrow.up")
                    }
                }
            }
            .accessibilityLabel("Share receipt")
            .task(id: r.id) { url = ActivityExport.cachedPDF(r) }
        }
    }
}

// MARK: - Happiness Meter

/// "How was topping up Salik?" — one tap rates; it goes to the entity and the Happiness Meter.
struct ActivityRatingCard: View {
    let receipt: Receipt
    @Environment(DemoStore.self) private var store

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 2) {
                Text(receipt.activityRatingQuestion)
                    .dnFont(.rowTitle)
                    .fontWeight(.semibold)
                    .foregroundStyle(DNColor.textPrimary)
                    .accessibilityAddTraits(.isHeader)
                Text(receipt.rating == nil
                     ? "One tap. It goes to \(receipt.activityRatingRecipient) and the Happiness Meter."
                     : "Thanks. Sent to \(receipt.activityRatingRecipient) and the Happiness Meter.")
                    .dnFont(.footnote)
                    .foregroundStyle(DNColor.textSecondary)
                    .contentTransition(.opacity)
            }
            HappinessFacesPicker(selection: Binding(
                get: { store.receipt(receipt.id)?.rating },
                set: { face in
                    guard let face else { return }
                    withAnimation(DNMotion.resolve(DNMotion.snappy)) { store.rate(receiptID: receipt.id, face) }
                }
            ))
        }
        .padding(.vertical, 8)
    }
}

extension ActivityExport {
    private static var pdfCache: [String: URL] = [:]

    /// Renders once per receipt and keeps the file for the session.
    static func cachedPDF(_ receipt: Receipt) -> URL? {
        if let url = pdfCache[receipt.id], FileManager.default.fileExists(atPath: url.path) { return url }
        let url = writePDF(receipt)
        if let url { pdfCache[receipt.id] = url }
        return url
    }
}
