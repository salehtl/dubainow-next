package com.salehtl.dubainownext.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.PushedHeader
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.dnCanvasBackground

/** Everything paid or sent, grouped by month (newest first), with Export (CSV). */
@Composable
fun HistoryScreen() {
    val store = LocalStore.current
    val context = LocalContext.current
    val history = store.state.history
    val months = ActivityFormat.months(history)
    Column(Modifier.fillMaxSize().dnCanvasBackground()) {
        PushedHeader()
        val pad = Modifier.padding(horizontal = DNSpace.insetPage)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
            item(key = "title") {
                Row(pad.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "History", style = DNTextStyle.LargeTitle.style, color = DNColor.textPrimary,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    if (history.isNotEmpty()) ActivityHeaderAction("Export", { ActivityExport.shareCsv(context, history) })
                }
            }
            if (months.isEmpty()) {
                item(key = "empty") {
                    ActivityEmptyState(Icons.Rounded.History, "Nothing here yet", "When you pay or send something, the receipt is kept here.", pad)
                }
            }
            items(months, key = { it.id }) { month ->
                Column(Modifier.animateItem().then(pad).padding(top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActivityMonthHeader(month.title)
                    ActivityHistoryMonthGroup(month)
                }
            }
        }
    }
}
