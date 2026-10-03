package com.salehtl.dubainownext.features.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.PushedHeader
import com.salehtl.dubainownext.design.components.dnCanvasBackground

/** A stored receipt, pushed (from History or a notification). */
@Composable
fun ReceiptScreen(receiptId: String) {
    Column(Modifier.fillMaxSize().dnCanvasBackground()) {
        PushedHeader(actions = { ActivityReceiptShareButton(receiptId) })
        ActivityReceiptContent(receiptId, Modifier.weight(1f), bottomPadding = 120.dp)
    }
}
