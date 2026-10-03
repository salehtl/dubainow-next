package com.salehtl.dubainownext.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.AppTab
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.MainHeader
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.DraftItem
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.NeedsYouItem
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.dnCanvasBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.salehtl.dubainownext.app.ScrollToTopOnReselect

/**
 * The Activity tab (Paper E8D-0): filter chips, then Needs you (the same items and numbers as
 * Home), In progress with government, Drafts (kept 30 days) and History by month.
 * Guests get the sign-in sheet (Paper WZQ-0).
 */
@Composable
fun ActivityScreen() {
    val store = LocalStore.current
    Column(Modifier.fillMaxSize().dnCanvasBackground()) {
        MainHeader()
        if (store.isGuest) ActivityGuestState() else ActivityList()
    }
}

// MARK: - The list

/** Everything the list shows, in order (so launch extras can scroll to a section by key). */
private sealed interface ActivityEntry {
    val key: String

    data object Title : ActivityEntry { override val key = "title" }
    data object Chips : ActivityEntry { override val key = "chips" }
    data class Header(override val key: String, val title: String, val accessory: Accessory) : ActivityEntry
    data class NeedsYou(val item: NeedsYouItem) : ActivityEntry { override val key = "ny-${item.id}" }
    data class InProgress(val requests: List<RequestItem>) : ActivityEntry { override val key = "ip-group" }
    data class DraftRows(val drafts: List<DraftItem>) : ActivityEntry { override val key = "drafts-rows" }
    data object DraftNotice : ActivityEntry { override val key = "drafts-notice" }
    data class DraftCard(val draft: DraftItem) : ActivityEntry { override val key = "draft-${draft.id}" }
    data object DraftFooter : ActivityEntry { override val key = "drafts-footer" }
    data class Month(val month: ActivityFormat.Month) : ActivityEntry { override val key = "month-${month.id}" }
    data class SeeAll(val count: Int) : ActivityEntry { override val key = "history-see-all" }
    data class Empty(val icon: ImageVector, val title: String, val message: String) : ActivityEntry { override val key = "empty" }

    enum class Accessory { None, PayAll, WithGovernment, Kept30Days, Export }
}

private fun activityEntries(store: DemoStore, filter: ActivityFilter): List<ActivityEntry> = buildList {
    add(ActivityEntry.Title)
    add(ActivityEntry.Chips)
    val needsYou = store.state.needsYou
    val inProgress = store.activityInProgress
    val drafts = ActivityDraftPins.sorted(store.state.drafts)
    val history = store.state.history.sortedByDescending { it.date }

    fun needsYouSection() {
        if (needsYou.isEmpty()) return
        add(ActivityEntry.Header("ny-header", "Needs you", if (store.showsPayAll) ActivityEntry.Accessory.PayAll else ActivityEntry.Accessory.None))
        needsYou.forEach { add(ActivityEntry.NeedsYou(it)) }
    }
    fun inProgressSection() {
        if (inProgress.isEmpty()) return
        add(ActivityEntry.Header("ip-header", "In progress", ActivityEntry.Accessory.WithGovernment))
        add(ActivityEntry.InProgress(inProgress))
    }
    fun historySection(limit: Int?) {
        if (history.isEmpty()) return
        add(ActivityEntry.Header("history-header", "History", ActivityEntry.Accessory.Export))
        val shown = limit?.let { history.take(it) } ?: history
        ActivityFormat.months(shown).forEach { add(ActivityEntry.Month(it)) }
        if (shown.size < history.size) add(ActivityEntry.SeeAll(history.size))
    }

    when (filter) {
        ActivityFilter.All -> {
            needsYouSection()
            inProgressSection()
            if (drafts.isNotEmpty()) {
                add(ActivityEntry.Header("drafts-header", "Drafts", ActivityEntry.Accessory.Kept30Days))
                add(ActivityEntry.DraftRows(drafts))
            }
            historySection(limit = 5)
            if (needsYou.isEmpty() && inProgress.isEmpty() && drafts.isEmpty() && history.isEmpty()) {
                add(ActivityEntry.Empty(Icons.Rounded.History, "Nothing here yet", "Things you need to do, requests with government, drafts and receipts all show up here."))
            }
        }
        ActivityFilter.NeedsYou ->
            if (needsYou.isEmpty()) add(ActivityEntry.Empty(Icons.Rounded.CheckCircleOutline, "You're all caught up", "When a bill, fine, renewal or request needs you, it shows up here and on Home."))
            else needsYouSection()
        ActivityFilter.InProgress ->
            if (inProgress.isEmpty()) add(ActivityEntry.Empty(Icons.Rounded.AccountBalance, "Nothing in progress", "Requests you send to government show here until they're done."))
            else inProgressSection()
        ActivityFilter.Drafts ->
            if (drafts.isEmpty()) add(ActivityEntry.Empty(Icons.Rounded.PendingActions, "No drafts", "If you leave a service before the end, we keep your answers here for 30 days."))
            else {
                add(ActivityEntry.DraftNotice)
                drafts.forEach { add(ActivityEntry.DraftCard(it)) }
                add(ActivityEntry.DraftFooter)
            }
        ActivityFilter.History ->
            if (history.isEmpty()) add(ActivityEntry.Empty(Icons.Rounded.History, "Nothing here yet", "When you pay or send something, the receipt is kept here."))
            else historySection(limit = null)
    }
}

@Composable
private fun ActivityList() {
    val store = LocalStore.current
    val router = LocalRouter.current
    val context = LocalContext.current
    val personaId = store.persona?.id
    remember(personaId) { ActivityTabMemory.sync(personaId); personaId }
    val filter = ActivityTabMemory.filter
    val capture = rememberActivityCaptureState()
    var draftToDelete by remember { mutableStateOf<DraftItem?>(null) }
    val listState = rememberLazyListState(ActivityTabMemory.firstIndex, ActivityTabMemory.firstOffset)
    ScrollToTopOnReselect(AppTab.Activity, listState)
    DisposableEffect(listState) {
        onDispose {
            ActivityTabMemory.firstIndex = listState.firstVisibleItemIndex
            ActivityTabMemory.firstOffset = listState.firstVisibleItemScrollOffset
        }
    }

    val entries = activityEntries(store, filter)
    val counts = mapOf(
        ActivityFilter.NeedsYou to store.state.needsYou.size,
        ActivityFilter.InProgress to store.activityInProgress.size,
        ActivityFilter.Drafts to store.state.drafts.size,
    )

    LaunchedEffect(Unit) {
        // `activityScroll needsYou|inProgress|drafts|history|bottom` (screenshots and demos).
        val target = LaunchOptions.extras["activityScroll"] ?: return@LaunchedEffect
        if (ActivityTabMemory.didApplyLaunchScroll) return@LaunchedEffect
        ActivityTabMemory.didApplyLaunchScroll = true
        delay(600)
        val key = when (target) {
            "needsYou" -> "ny-header"; "inProgress" -> "ip-header"; "drafts" -> "drafts-header"; "history" -> "history-header"
            else -> null
        }
        val index = key?.let { k -> entries.indexOfFirst { it.key == k } } ?: entries.lastIndex
        if (index >= 0) listState.animateScrollToItem(index)
    }
    LaunchedEffect(Unit) {
        if (ActivityDemo.take("delete")) { delay(500); draftToDelete = store.state.drafts.firstOrNull() }
        if (ActivityDemo.take("csv")) { delay(800); ActivityExport.shareCsv(context, store.state.history) }
    }

    fun primary(item: NeedsYouItem) {
        when (val k = item.kind) {
            is NeedsYouItem.Kind.Payment -> router.present(SheetRoute.Payment(store.paymentRequest(k.payable)))
            is NeedsYouItem.Kind.RequestStep -> {
                val r = store.request(k.requestID)
                if (r != null && r.activityCurrentStep?.activityKind == ActivityStepKind.Pay) router.present(SheetRoute.Payment(r.activityFeesRequest))
                else capture.scan = k.requestID
            }
            is NeedsYouItem.Kind.Renewal -> router.open(k.serviceID)
            is NeedsYouItem.Kind.JourneyStep -> router.push(Route.Journey(k.journeyID))
        }
    }

    fun secondary(item: NeedsYouItem) {
        if (item.secondaryAction == "Later") { store.later(item); return }
        when (val k = item.kind) {
            is NeedsYouItem.Kind.Payment -> {
                val vehicle = store.state.vehicles.firstOrNull { k.payable.id in it.fineIDs }
                if (vehicle != null) router.push(Route.Vehicle(vehicle.id)) else k.payable.serviceID?.let { router.open(it) }
            }
            is NeedsYouItem.Kind.RequestStep -> router.push(Route.Request(k.requestID))
            is NeedsYouItem.Kind.Renewal -> {
                val thing = k.thingID
                if (thing != null && store.state.vehicles.any { it.id == thing }) router.push(Route.Vehicle(thing)) else router.open(k.serviceID)
            }
            is NeedsYouItem.Kind.JourneyStep -> router.push(Route.Journey(k.journeyID))
        }
    }

    val pad = Modifier.padding(horizontal = DNSpace.insetPage)
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 120.dp)) {
        items(entries, key = { it.key }, contentType = { it::class }) { entry ->
            val anim = Modifier.animateItem()
            when (entry) {
                ActivityEntry.Title -> Text(
                    "Activity", style = DNTextStyle.LargeTitle.style, color = DNColor.textPrimary,
                    modifier = pad.padding(top = 4.dp, bottom = 12.dp).semantics { heading() },
                )
                ActivityEntry.Chips -> Column(Modifier.padding(bottom = 4.dp)) {
                    ActivityFilterChips(filter, counts) { ActivityTabMemory.filter = it }
                }
                is ActivityEntry.Header -> ActivitySectionHeader(entry.title, anim.then(pad).padding(top = 12.dp, bottom = 4.dp)) {
                    when (entry.accessory) {
                        ActivityEntry.Accessory.PayAll -> {
                            val request = store.payAllRequest()
                            ActivityHeaderAction("${request.title} · ${Money.aed(store.payAllTotal)}", { router.present(SheetRoute.Payment(request)) })
                        }
                        ActivityEntry.Accessory.WithGovernment -> ActivityHeaderNote("with government")
                        ActivityEntry.Accessory.Kept30Days -> ActivityHeaderNote("kept for 30 days")
                        ActivityEntry.Accessory.Export -> ActivityHeaderAction("Export", { ActivityExport.shareCsv(context, store.state.history) })
                        ActivityEntry.Accessory.None -> Unit
                    }
                }
                is ActivityEntry.NeedsYou -> ActivityNeedsYouCard(
                    entry.item, onPrimary = { primary(entry.item) }, onSecondary = { secondary(entry.item) },
                    modifier = anim.then(pad).padding(bottom = 12.dp),
                )
                is ActivityEntry.InProgress -> ActivityInProgressGroup(entry.requests, anim.then(pad).padding(bottom = 8.dp))
                is ActivityEntry.DraftRows -> ActivityDraftRowsGroup(entry.drafts, { draftToDelete = it }, anim.then(pad).padding(bottom = 8.dp))
                ActivityEntry.DraftNotice -> ActivityInfoNotice(ACTIVITY_DRAFTS_NOTICE, modifier = anim.then(pad).padding(top = 8.dp, bottom = 12.dp))
                is ActivityEntry.DraftCard -> ActivityDraftCardItem(entry.draft, { draftToDelete = it }, anim.then(pad).padding(bottom = 12.dp))
                ActivityEntry.DraftFooter -> Text(
                    ACTIVITY_DRAFTS_FOOTER, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary,
                    modifier = anim.then(pad).padding(horizontal = 4.dp, vertical = 4.dp),
                )
                is ActivityEntry.Month -> Column(anim.then(pad).padding(top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActivityMonthHeader(entry.month.title)
                    ActivityHistoryMonthGroup(entry.month)
                }
                is ActivityEntry.SeeAll -> Column(anim.then(pad)) {
                    ActivityHeaderAction("See all ${entry.count}", { ActivityTabMemory.filter = ActivityFilter.History })
                }
                is ActivityEntry.Empty -> ActivityEmptyState(entry.icon, entry.title, entry.message, anim.then(pad))
            }
        }
    }

    ActivityCaptureHost(capture)
    ActivityDraftDeleteDialog(draftToDelete) { draftToDelete = null }
}

// MARK: - Guest

/** Guests see a signed-out state, and the sign-in sheet on every visit (Paper WZQ-0). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityGuestState() {
    val store = LocalStore.current
    val router = LocalRouter.current
    val scope = rememberCoroutineScope()
    var showsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(Unit) { showsSheet = true }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = DNSpace.insetPage).padding(bottom = 120.dp),
    ) {
        Text(
            "Activity", style = DNTextStyle.LargeTitle.style, color = DNColor.textPrimary,
            modifier = Modifier.padding(top = 4.dp, bottom = 36.dp).semantics { heading() },
        )
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ActivityGuestTile()
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your activity is private", style = DNTextStyle.Title2.style, color = DNColor.textPrimary, textAlign = TextAlign.Center)
                Text(
                    "Sign in to track your requests with government, pay what's due and find every receipt.",
                    style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary, textAlign = TextAlign.Center,
                )
            }
            ActivityUAEPassButton { store.signOut() }
        }
    }

    if (showsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showsSheet = false },
            sheetState = sheetState,
            containerColor = DNColor.surfacePrimary,
            shape = RoundedCornerShape(topStart = DNRadius.sheet, topEnd = DNRadius.sheet),
        ) {
            Column(Modifier.padding(bottom = 8.dp)) {
                ActivityGuestSheetContent(
                    onSignIn = {
                        scope.launch {
                            sheetState.hide(); showsSheet = false
                            delay(350); store.signOut()
                        }
                    },
                    onNotNow = {
                        scope.launch {
                            sheetState.hide(); showsSheet = false
                            router.select(AppTab.Home)
                        }
                    },
                )
            }
        }
    }
}
