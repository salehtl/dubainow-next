import SwiftUI

/// A stored receipt as a sheet (Paper EII-0): close · "Receipt" · share.
struct ReceiptSheet: View {
    var receiptID: String
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ActivityReceiptList(receiptID: receiptID)
                .navigationTitle("Receipt")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button(role: .close) { dismiss() }.tint(DNColor.textPrimary)
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        ActivityReceiptShareButton(receiptID: receiptID)
                    }
                }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }
}
