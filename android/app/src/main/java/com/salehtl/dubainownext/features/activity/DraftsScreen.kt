package com.salehtl.dubainownext.features.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.PushedHeader
import com.salehtl.dubainownext.data.DraftItem
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.dnCanvasBackground
import kotlinx.coroutines.delay

/** All drafts (Paper EL5-0): kept for 30 days; swipe to pin or delete. */
@Composable
fun DraftsScreen() {
    val store = LocalStore.current
    var draftToDelete by remember { mutableStateOf<DraftItem?>(null) }
    val drafts = ActivityDraftPins.sorted(store.state.drafts)
    LaunchedEffect(Unit) {
        if (ActivityDemo.take("delete")) { delay(500); draftToDelete = store.state.drafts.firstOrNull() }
    }
    Column(Modifier.fillMaxSize().dnCanvasBackground()) {
        PushedHeader()
        val pad = Modifier.padding(horizontal = DNSpace.insetPage)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
            item(key = "title") {
                Text(
                    "Drafts", style = DNTextStyle.LargeTitle.style, color = DNColor.textPrimary,
                    modifier = pad.padding(top = 4.dp, bottom = 12.dp).semantics { heading() },
                )
            }
            if (drafts.isEmpty()) {
                item(key = "empty") {
                    ActivityEmptyState(Icons.Rounded.PendingActions, "No drafts", "If you leave a service before the end, we keep your answers here for 30 days.", pad)
                }
            } else {
                item(key = "notice") { ActivityInfoNotice(ACTIVITY_DRAFTS_NOTICE, modifier = Modifier.animateItem().then(pad).padding(bottom = 12.dp)) }
                items(drafts, key = { it.id }) { d ->
                    ActivityDraftCardItem(d, { draftToDelete = it }, Modifier.animateItem().then(pad).padding(bottom = 12.dp))
                }
                item(key = "footer") {
                    Text(
                        ACTIVITY_DRAFTS_FOOTER, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary,
                        modifier = Modifier.animateItem().then(pad).padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
    ActivityDraftDeleteDialog(draftToDelete) { draftToDelete = null }
}
