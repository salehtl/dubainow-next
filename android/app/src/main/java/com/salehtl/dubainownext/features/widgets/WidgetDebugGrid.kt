package com.salehtl.dubainownext.features.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.HomeWidget
import com.salehtl.dubainownext.data.WidgetKind
import com.salehtl.dubainownext.data.WidgetSize
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import kotlinx.coroutines.delay

/**
 * DEBUG (Widgets builder): every widget kind at every size it's designed in, laid out on the Home grid
 * (live: ticking, tappable, touch and hold menus), plus the persona's own Home widgets at the top so
 * Resize / Remove / Undo can be tried for real. Shown in place of the gallery with
 * `sheet widgetGallery widgetsDebug true`, with optional extras:
 *   widgetsDebugOnly parking,salik      only these kinds (parking salik nol metro prayer weather energy bills family journey request certificates)
 *   widgetsDebugSizes medium,large      only these sizes
 *   widgetsDebugParking ending|expired|none   force a parking state
 *   widgetsDebugPaid true                pay everything first (Bills and fines "all paid")
 *   widgetsDebugMenu <widget id | key-size>   open that widget's menu (e.g. w-parking, parking-small); widgetsDebugMenuSizes true opens Size
 *   widgetsDebugAction remove:w-parking | resize:w-salik:medium | add:family:medium   run an action after 1.5 s
 */
@Composable
internal fun WidgetsDebugGrid() {
    val store = LocalStore.current
    val router = LocalRouter.current
    val toasts = LocalToasts.current
    val view = LocalView.current

    LaunchedEffect(Unit) {
        val parking = store.state.parking
        when (LaunchOptions.extras["widgetsDebugParking"]) {
            "ending" -> parking?.let { store.state = store.state.copy(parking = it.copy(endsAt = DemoDate.minutes(8))) }
            "expired" -> parking?.let { store.state = store.state.copy(parking = it.copy(endsAt = DemoDate.minutes(-3))) }
            "none" -> store.state = store.state.copy(parking = null)
        }
        if (LaunchOptions.flag("widgetsDebugPaid") && store.payables.isNotEmpty()) store.completePayment(store.payAllRequest())
        val action = LaunchOptions.extras["widgetsDebugAction"]?.split(":") ?: return@LaunchedEffect
        delay(1500)
        val actions = WidgetActions(store, router, toasts)
        when (action.firstOrNull()) {
            "remove" -> store.state.widgets.firstOrNull { it.id == action.getOrNull(1) }?.let { actions.remove(it, view) }
            "resize" -> store.state.widgets.firstOrNull { it.id == action.getOrNull(1) }?.let { w ->
                WidgetSize.entries.firstOrNull { it.name.equals(action.getOrNull(2), true) }?.let { store.resizeWidget(w, it) }
            }
            "add" -> debugKinds(store.state.journeys.firstOrNull()?.journeyID, store.state.requests.firstOrNull()?.id)
                .firstOrNull { it.first == action.getOrNull(1) }?.second?.let { kind ->
                    val size = WidgetSize.entries.firstOrNull { it.name.equals(action.getOrNull(2), true) } ?: kind.widgetSizes.firstOrNull() ?: WidgetSize.Small
                    val added = store.addWidget(kind, size)
                    toasts.show("${kind.title} added to Home", undo = { store.removeWidget(added) })
                }
        }
    }

    val only = LaunchOptions.extras["widgetsDebugOnly"]?.split(",")?.map { it.trim() }?.toSet()
    val onlySizes = LaunchOptions.extras["widgetsDebugSizes"]?.split(",")?.map { it.trim().lowercase() }?.toSet()
    val all = debugKinds(store.state.journeys.firstOrNull()?.journeyID, store.state.requests.firstOrNull()?.id)
        .filter { only == null || it.first in only }
        .flatMap { (name, kind) ->
            val sizes = kind.widgetSizes.ifEmpty { listOf(WidgetSize.Small) }
            sizes.map { HomeWidget("debug-$name-${it.name.lowercase()}", kind, it) }
        }
        .filter { onlySizes == null || it.size.name.lowercase() in onlySizes }
        .sortedBy { it.size.ordinal }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val unit = maxOf(120.dp, (maxWidth - DNSpace.insetPage * 2 - DNSpace.widgetGutter) / 2)
        val wide = unit * 2 + DNSpace.widgetGutter
        Column(Modifier.fillMaxSize()) {
            WidgetSheetHeader("Widgets · debug") { router.dismissSheet() }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)) {
                if (only == null && onlySizes == null) {
                    item(key = "home-label") { DebugLabel("On Home · ${store.state.widgets.size} (live, menus change Home)") }
                    debugRows(store.state.widgets).forEachIndexed { i, row ->
                        item(key = "home-$i-${row.joinToString { it.id + it.size }}") { DebugRow(row, unit, wide) }
                    }
                }
                item(key = "all-label") { DebugLabel("Every kind and size · ${all.size}") }
                debugRows(all).forEachIndexed { i, row ->
                    item(key = "all-$i") { DebugRow(row, unit, wide) }
                }
            }
        }
    }
}

private fun debugKinds(journeyID: String?, requestID: String?): List<Pair<String, WidgetKind>> = listOf(
    "parking" to WidgetKind.Parking, "salik" to WidgetKind.Salik, "prayer" to WidgetKind.Prayer,
    "weather" to WidgetKind.Weather, "nol" to WidgetKind.Nol, "request" to WidgetKind.VisaTracker(requestID ?: "sara-visa"),
    "bills" to WidgetKind.BillsAndFines, "family" to WidgetKind.FamilyVisas, "metro" to WidgetKind.Metro,
    "energy" to WidgetKind.Energy, "journey" to WidgetKind.Journey(journeyID ?: "move-home"),
    "certificates" to WidgetKind.Certificates,
)

/** Smalls pair left to right; medium and large take a row each. */
private fun debugRows(widgets: List<HomeWidget>): List<List<HomeWidget>> {
    val rows = mutableListOf<List<HomeWidget>>()
    var pending: HomeWidget? = null
    for (w in widgets) {
        if (w.size == WidgetSize.Small) {
            val p = pending
            if (p != null) { rows += listOf(p, w); pending = null } else pending = w
        } else {
            pending?.let { rows += listOf(it); pending = null }
            rows += listOf(w)
        }
    }
    pending?.let { rows += listOf(it) }
    return rows
}

@Composable
private fun DebugLabel(text: String) {
    Text(
        text.uppercase(), style = DNTextStyle.Overline.style, color = DNColor.textGroupedHeader,
        modifier = Modifier.fillMaxWidth().padding(start = DNSpace.insetPage, end = DNSpace.insetPage, top = 8.dp, bottom = 12.dp),
    )
}

@Composable
private fun DebugRow(row: List<HomeWidget>, unit: Dp, wide: Dp) {
    Row(
        Modifier.fillMaxWidth().padding(start = DNSpace.insetPage, end = DNSpace.insetPage, bottom = DNSpace.widgetGutter),
        horizontalArrangement = Arrangement.spacedBy(DNSpace.widgetGutter),
    ) {
        row.forEach { w ->
            WidgetView(
                w,
                Modifier.size(
                    width = if (w.size == WidgetSize.Small) unit else wide,
                    height = if (w.size == WidgetSize.Large) wide else unit,
                ),
            )
        }
    }
}
