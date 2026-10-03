import SwiftUI

/// Files the Activity feature shares: the History CSV ("Export") and a receipt PDF
/// ("Download receipt (PDF)"). Written to the temporary folder; nothing leaves the device
/// unless the resident shares it.
enum ActivityExport {
    // MARK: CSV

    static func csv(_ receipts: [Receipt]) -> String {
        var lines = ["Date,What,Paid to,Amount (AED),Paid with,Reference"]
        for r in receipts.sorted(by: { $0.date > $1.date }) {
            let amount = r.amount.map { Money.figure($0).replacingOccurrences(of: ",", with: "") } ?? ""
            lines.append([
                ActivityFormat.csvDate(r.date),
                r.title + (r.count.map { $0 > 1 ? " (×\($0))" : "" } ?? ""),
                r.entity,
                amount,
                r.method?.label ?? "",
                r.reference,
            ].map { escape($0) }.joined(separator: ","))
        }
        lines.append("")
        lines.append(escape("DubaiNow demo export. No real payments were made."))
        return lines.joined(separator: "\n")
    }

    /// Writes the CSV and returns its URL (nil if the folder can't be written).
    static func writeCSV(_ receipts: [Receipt]) -> URL? {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("DubaiNow history.csv")
        do {
            try csv(receipts).write(to: url, atomically: true, encoding: .utf8)
            return url
        } catch {
            return nil
        }
    }

    private static func escape(_ field: String) -> String {
        guard field.contains(",") || field.contains("\"") || field.contains("\n") else { return field }
        return "\"" + field.replacingOccurrences(of: "\"", with: "\"\"") + "\""
    }

    // MARK: PDF

    /// Renders the receipt as a one-page PDF with ImageRenderer.
    static func writePDF(_ receipt: Receipt) -> URL? {
        let safeRef = receipt.reference.replacingOccurrences(of: "/", with: "-")
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("DubaiNow receipt \(safeRef).pdf")
        let renderer = ImageRenderer(content: ActivityReceiptPDFPage(receipt: receipt))
        renderer.proposedSize = ProposedViewSize(width: 595, height: nil)
        var written = false
        renderer.render { size, draw in
            var box = CGRect(x: 0, y: 0, width: 595, height: max(842, size.height))
            guard let pdf = CGContext(url as CFURL, mediaBox: &box, nil) else { return }
            pdf.beginPDFPage(nil)
            pdf.translateBy(x: 0, y: box.height - size.height)
            draw(pdf)
            pdf.endPDFPage()
            pdf.closePDF()
            written = true
        }
        return written ? url : nil
    }

    /// Plain-text summary (the share button when the PDF isn't ready).
    static func summary(_ r: Receipt) -> String {
        var lines = [r.title, r.entity]
        if let amount = r.amount { lines.append(Money.aed(amount)) }
        lines.append(ActivityFormat.whenLong(r.date))
        if let m = r.method { lines.append("Paid with \(m.label)") }
        lines += r.rows.map { "\($0.label): \($0.value)" }
        lines.append("Reference \(r.reference)")
        lines.append("DubaiNow demo · no money moved")
        return lines.joined(separator: "\n")
    }
}

/// The printable receipt (A4 width). Says clearly that it's a demo document.
struct ActivityReceiptPDFPage: View {
    let receipt: Receipt

    var body: some View {
        VStack(alignment: .leading, spacing: 28) {
            HStack(alignment: .center) {
                DNLogo(height: 34)
                Text("DubaiNow")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(DNColor.textPrimary)
                Spacer()
                Text("DEMO · NOT A REAL RECEIPT")
                    .font(.system(size: 10, weight: .bold))
                    .tracking(0.8)
                    .foregroundStyle(DNColor.statusWarningForeground)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(DNColor.statusWarningSurface, in: .capsule)
            }

            VStack(alignment: .leading, spacing: 6) {
                Text(receipt.amount == nil ? "Confirmation" : "Receipt")
                    .font(.system(size: 13, weight: .semibold))
                    .tracking(0.8)
                    .foregroundStyle(DNColor.textSecondary)
                Text(receipt.title)
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(DNColor.textPrimary)
                if let amount = receipt.amount {
                    Text(Money.aed(amount))
                        .font(.system(size: 40, weight: .bold, design: .rounded))
                        .foregroundStyle(DNColor.textPrimary)
                }
            }

            VStack(spacing: 0) {
                pdfRow("Paid to", receipt.entity)
                pdfRow("When", ActivityFormat.whenLong(receipt.date))
                if let m = receipt.method { pdfRow("Paid with", m.label) }
                ForEach(Array(receipt.rows.enumerated()), id: \.offset) { _, row in pdfRow(row.label, row.value) }
                pdfRow("Reference", receipt.reference, mono: true)
            }

            Text("This document was made by the DubaiNow demo app. No money moved and no government system was contacted.")
                .font(.system(size: 11))
                .foregroundStyle(DNColor.textSecondary)
        }
        .padding(48)
        .frame(width: 595, alignment: .leading)
        .background(DNColor.white)
        .environment(\.colorScheme, .light)
    }

    private func pdfRow(_ label: String, _ value: String, mono: Bool = false) -> some View {
        VStack(spacing: 0) {
            HStack(alignment: .firstTextBaseline) {
                Text(label)
                    .font(.system(size: 13))
                    .foregroundStyle(DNColor.textSecondary)
                Spacer(minLength: 24)
                Text(value)
                    .font(.system(size: 13, weight: .semibold, design: mono ? .monospaced : .default))
                    .foregroundStyle(DNColor.textPrimary)
                    .multilineTextAlignment(.trailing)
            }
            .padding(.vertical, 11)
            Rectangle().fill(DNColor.borderSubtle).frame(height: 1)
        }
    }
}
