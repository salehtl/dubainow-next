import SwiftUI

/// A stored receipt, pushed (from History or a notification).
struct ReceiptView: View {
    var receiptID: String

    var body: some View {
        ActivityReceiptList(receiptID: receiptID, onCanvas: true)
            .navigationTitle("Receipt")
            .navigationBarTitleDisplayMode(.inline)
            .dnPushedToolbar()
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    ActivityReceiptShareButton(receiptID: receiptID)
                }
            }
    }
}
