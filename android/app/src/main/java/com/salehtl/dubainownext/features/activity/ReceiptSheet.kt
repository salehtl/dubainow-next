package com.salehtl.dubainownext.features.activity

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.salehtl.dubainownext.app.LocalRouter

/** A stored receipt as a sheet (Paper EII-0): "Receipt" · share · close, then the receipt. */
@Composable
fun ReceiptSheet(receiptId: String) {
    val router = LocalRouter.current
    ActivitySheetFrame {
        ActivitySheetTitleRow(
            title = "Receipt",
            onClose = { router.dismissSheet() },
            actions = { ActivityReceiptShareButton(receiptId) },
        )
        ActivityReceiptContent(receiptId, Modifier.weight(1f, fill = false))
    }
}
