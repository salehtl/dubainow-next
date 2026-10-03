package com.salehtl.dubainownext.features.search

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.AppTab
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.app.Router
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.Payable
import com.salehtl.dubainownext.data.WidgetKind
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.ChipTone
import com.salehtl.dubainownext.design.components.dnCanvasBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * The search tab (Paper p-W-0 · 04 Search), as Android's full-screen M3 search view: the shell hides
 * the bottom bar while `router.tab == AppTab.Search`, and this draws the search field at the top
 * (back arrow → Home, clear, keyboard search action, focused on open) bound to `router.searchQuery`.
 * Below it: Ready (zero state) → Results as you type → Direct answer → Did you mean, as on iOS.
 *
 * Debug extras: `searchQuery how_much_do_I_owe` (underscores → spaces) fills the field once per run, `searchHideKeyboard true`
 * keeps the keyboard down, `searchScrollToEnd true` scrolls to the bottom, `searchScrollBy 900` scrolls by px,
 * `searchFontScale 2` previews 200 % text, `searchType when_is_dhuhr` types key by key, `searchRoundTrip true` pushes a
 * result and comes back, `searchPayOne true` opens the "Pay one at a time" dialog.
 */
@Composable
fun SearchScreen() {
    // Debug: `searchFontScale 2` previews the screen at 200 % text without changing the device setting.
    val scale = LaunchOptions.extras["searchFontScale"]?.toFloatOrNull()
    if (scale != null) {
        val d = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) { SearchView() }
    } else {
        SearchView()
    }
}

@Composable
private fun SearchView() {
    val store = LocalStore.current; val router = LocalRouter.current; val toasts = LocalToasts.current
    val view = LocalView.current; val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    /** True when coming back from a result pushed in the search tab (keyboard stays down, list stays put). */
    val returning = remember { SearchSession.open(router) }
    val listState = rememberLazyListState(SearchSession.scrollIndex, SearchSession.scrollOffset)
    val initial = router.searchQuery
    var field by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    /** The query after a 150 ms pause in typing. */
    var query by remember { mutableStateOf(initial) }
    // "Search instead for …" and "See all" live in SearchSession so they survive a pushed result.
    val exactQuery = SearchSession.exactQuery
    val showsAllServices = SearchSession.showsAllServices
    var choosingOneToPay by remember { mutableStateOf(LaunchOptions.flag("searchPayOne")) }
    var fieldFocused by remember { mutableStateOf(false) }
    var announcement by remember { mutableStateOf("") }
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    val trimmed = query.trim()
    val isReady = trimmed.length < 2
    val resident = store.persona?.id ?: "guest"

    /** A new query starts at the top of the list (keyed items would otherwise hold the old scroll anchor). */
    fun applyQuery(text: String) {
        if (query.trim() != text.trim()) {
            SearchSession.showsAllServices = false
            listState.requestScrollToItem(0)
        }
        query = text
    }

    // Typing: the field follows router.searchQuery; results follow after 150 ms.
    LaunchedEffect(router.searchQuery) {
        val text = router.searchQuery
        if (field.text != text) field = TextFieldValue(text, TextRange(text.length))
        if (text.trim().length < 2) { applyQuery(text); SearchSession.exactQuery = null; announcement = ""; return@LaunchedEffect }
        delay(150)
        applyQuery(text)
        if (SearchSession.exactQuery != text.trim()) SearchSession.exactQuery = null
        // TalkBack hears the count after a pause in typing: "6 results".
        delay(700)
        val r = store.searchResults(text.trim(), SearchSession.exactQuery == text.trim())
        announcement = if (r.isEmpty) "No results" else if (r.count == 1) "1 result" else "${r.count} results"
    }
    LaunchedEffect(Unit) { withContext(Dispatchers.Default) { SearchIndex.warmUp() } }
    // Focus on open (not when coming back from a result, and not for screenshots).
    LaunchedEffect(Unit) {
        if (returning || LaunchOptions.flag("searchHideKeyboard")) return@LaunchedEffect
        delay(120)
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }
    LaunchedEffect(Unit) {
        // Debug: `searchType renew_my_car` types the text one key at a time (exercises debounce and list updates).
        val typed = LaunchOptions.extras["searchType"]?.replace('_', ' ') ?: return@LaunchedEffect
        delay(1200)
        for (i in 1..typed.length) { router.searchQuery = typed.take(i); delay(320) }
    }
    LaunchedEffect(Unit) {
        // Debug: `searchRoundTrip true` opens "See all", scrolls, pushes A to Z and comes back; nothing should reset.
        if (returning || !LaunchOptions.flag("searchRoundTrip")) return@LaunchedEffect
        delay(1200); SearchSession.showsAllServices = true
        delay(400); listState.scrollBy(500f)
        delay(400); keyboard?.hide(); focusManager.clearFocus(); router.push(Route.AllServices)
        MainScope().launch { delay(1500); router.pop() }
    }
    LaunchedEffect(Unit) {
        val by = LaunchOptions.extras["searchScrollBy"]?.toFloatOrNull()
        if (LaunchOptions.flag("searchScrollToEnd")) {
            delay(1200); listState.scrollToItem(maxOf(0, listState.layoutInfo.totalItemsCount - 1))
        } else if (by != null) {
            delay(1200); listState.scrollBy(by)
        }
    }
    // Leaving (a pushed result, another tab, sign-out) never leaves an orphaned keyboard behind.
    DisposableEffect(Unit) { onDispose { keyboard?.hide(); SearchSession.close(router, listState) } }

    // Scrolling the results lowers the keyboard.
    val focusedNow by rememberUpdatedState(fieldFocused)
    val hideOnDrag = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (focusedNow && source == NestedScrollSource.UserInput && available.y != 0f) {
                    keyboard?.hide(); focusManager.clearFocus()
                }
                return Offset.Zero
            }
        }
    }

    // MARK: Actions
    fun hideKeyboard() { keyboard?.hide(); focusManager.clearFocus() }
    fun record() = SearchRecents.add(trimmed, resident)
    fun search(text: String) { DNHaptics.selection(view); hideKeyboard(); router.searchQuery = text }
    fun push(route: Route) { hideKeyboard(); router.push(route) }
    fun present(sheet: SheetRoute) { hideKeyboard(); router.present(sheet) }
    fun openService(s: CatalogueService) { record(); hideKeyboard(); router.open(s) }
    fun openService(id: String) { Catalogue.service(id)?.let { openService(it) } }
    fun pay(p: Payable) { record(); present(SheetRoute.Payment(store.paymentRequest(p))) }
    fun perform(action: SearchAction) {
        record()
        when (action) {
            is SearchAction.Push -> push(action.route)
            is SearchAction.Sheet -> present(action.sheet)
            is SearchAction.Pay -> present(SheetRoute.Payment(store.paymentRequest(action.payable)))
            is SearchAction.Service -> openService(action.id)
        }
    }
    fun addWidget(kind: WidgetKind) {
        if (store.hasWidget(kind)) return
        store.addWidget(kind, SearchWidgets.size(kind))
        DNHaptics.success(view)
        toasts.show("Added ${kind.title} to Home")
    }
    val actions = SearchActions(
        search = ::search, record = ::record, hideKeyboard = ::hideKeyboard, push = ::push, present = ::present,
        openService = { openService(it) }, openServiceID = { openService(it) }, pay = ::pay, perform = ::perform, addWidget = ::addWidget,
        payOneAtATime = { record(); hideKeyboard(); choosingOneToPay = true },
    )

    val results = if (isReady) null else remember(trimmed, exactQuery, store.state, store.session) { store.searchResults(trimmed, exactQuery == trimmed) }

    Column(Modifier.fillMaxSize().dnCanvasBackground()) {
        SearchViewHeader(
            value = field,
            onValueChange = { field = it; if (router.searchQuery != it.text) router.searchQuery = it.text },
            focusRequester = focusRequester,
            onBack = { hideKeyboard(); router.closeSearch() },
            onClear = {
                router.searchQuery = ""; SearchSession.exactQuery = null
                runCatching { focusRequester.requestFocus() }; keyboard?.show()
            },
            onSubmit = { SearchRecents.add(router.searchQuery, resident); hideKeyboard() },
            onMic = if (store.isGuest) null else ({ hideKeyboard(); router.openAssistant("__listen__") }),
            onFocusChange = { fieldFocused = it },
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().nestedScroll(hideOnDrag).imePadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp + bottom),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                val animated: Modifier.(LazyItemScope) -> Modifier = { scope ->
                    if (reduceMotion) this else with(scope) {
                        animateItem(fadeInSpec = DNMotion.fast<Float>(), placementSpec = DNMotion.smooth<IntOffset>(), fadeOutSpec = DNMotion.fast<Float>())
                    }
                }
                if (results == null) {
                    searchReadyItems(store, router, resident, actions, animated)
                } else {
                    searchResultItems(
                        store = store, router = router, r = results, trimmed = trimmed, actions = actions, animated = animated,
                        showsAllServices = showsAllServices, onShowAll = { SearchSession.showsAllServices = true },
                        onSearchOriginal = { SearchSession.exactQuery = trimmed; listState.requestScrollToItem(0) },
                    )
                }
            }
            // TalkBack: "6 results" after a pause in typing.
            Box(Modifier.size(1.dp).semantics { liveRegion = LiveRegionMode.Polite; contentDescription = announcement })
        }
    }

    if (choosingOneToPay) {
        AlertDialog(
            onDismissRequest = { choosingOneToPay = false },
            confirmButton = { TextButton(onClick = { choosingOneToPay = false }) { Text("Cancel", style = DNTextStyle.Button.style) } },
            title = { Text("Pay one at a time", style = DNTextStyle.Title2.style, color = DNColor.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Choose what to pay first.", style = DNTextStyle.Body.style, color = DNColor.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    store.payables.forEach { p -> SearchPayOneRow(p) { choosingOneToPay = false; actions.pay(p) } }
                }
            },
            containerColor = DNColor.surfacePrimary,
            shape = RoundedCornerShape(DNRadius.dialog),
        )
    }
}

/** What rows and cards can do (so the list builders stay plain functions). */
private class SearchActions(
    val search: (String) -> Unit,
    val record: () -> Unit,
    val hideKeyboard: () -> Unit,
    val push: (Route) -> Unit,
    val present: (SheetRoute) -> Unit,
    val openService: (CatalogueService) -> Unit,
    val openServiceID: (String) -> Unit,
    val pay: (Payable) -> Unit,
    val perform: (SearchAction) -> Unit,
    val addWidget: (WidgetKind) -> Unit,
    val payOneAtATime: () -> Unit,
)

/**
 * State that outlives the search view's composition: the launch query (applied once per run), the
 * "query kept for 10 minutes" rule, and what a pushed result must not reset ("Search instead for",
 * "See all", the scroll position). The shell disposes this screen when a result is pushed.
 */
private object SearchSession {
    private const val KEEP_MS = 10 * 60 * 1000L
    private var launchApplied = false
    private var leftAt: Long? = null
    /** A result was pushed in the search tab when the view went away. */
    private var pushed = false

    /** Set by "Search instead for …": search this exact text without correcting it. */
    var exactQuery by mutableStateOf<String?>(null)
    var showsAllServices by mutableStateOf(false)
    var scrollIndex = 0
        private set
    var scrollOffset = 0
        private set

    /** Called when the view appears. True when coming back from a pushed result. */
    fun open(router: Router): Boolean {
        if (!launchApplied) {
            launchApplied = true
            // adb splits extras on spaces, so "how_much_do_I_owe" works too.
            LaunchOptions.extras["searchQuery"]?.takeIf { it.isNotBlank() }?.let { router.searchQuery = it.replace('_', ' ') }
        }
        // A reset (persona switch, demo reset) clears the query, so a blank query is always a fresh open.
        val back = pushed && router.searchQuery.isNotBlank()
        pushed = false
        if (!back) {
            val expired = leftAt?.let { System.currentTimeMillis() - it > KEEP_MS } ?: false
            if (expired) router.searchQuery = ""
            if (expired || router.searchQuery.isBlank()) { exactQuery = null; showsAllServices = false }
            scrollIndex = 0; scrollOffset = 0
        }
        leftAt = null
        return back
    }

    /** Called when the view goes away: to a pushed result (keep everything) or to another tab. */
    fun close(router: Router, list: LazyListState) {
        pushed = router.tab == AppTab.Search && router.stack(AppTab.Search).isNotEmpty()
        scrollIndex = if (pushed) list.firstVisibleItemIndex else 0
        scrollOffset = if (pushed) list.firstVisibleItemScrollOffset else 0
        leftAt = System.currentTimeMillis()
    }
}

// MARK: - Header (M3 full-screen search view)

private const val SEARCH_PROMPT = "Services, fines, \"how much do I owe\""

@Composable
private fun SearchViewHeader(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    onMic: (() -> Unit)?,
    onFocusChange: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(DNColor.surfacePrimary).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = DNColor.textPrimary)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp).focusRequester(focusRequester)
                    .onFocusChanged { onFocusChange(it.isFocused) }
                    .semantics { contentDescription = "Search" },
                textStyle = DNTextStyle.Body.style.copy(color = DNColor.textPrimary),
                singleLine = true,
                cursorBrush = SolidColor(DNColor.actionAccent),
                // No keyboard autocorrect: search fixes typos itself and shows it ("Showing results for mulkiya"),
                // while the keyboard would rewrite local words like Mulkiya, Ejari or Salik.
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.text.isEmpty()) {
                            // Same prompt as the iOS search field.
                            Text(SEARCH_PROMPT, style = DNTextStyle.Body.style, color = DNColor.fieldHint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        inner()
                    }
                },
            )
            if (value.text.isNotEmpty()) {
                IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = DNColor.iconSecondary) }
            } else if (onMic != null) {
                IconButton(onClick = onMic) { Icon(Icons.Rounded.Mic, contentDescription = "Ask DubaiNow by voice", tint = DNColor.iconSecondary) }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = DNColor.borderSubtle)
    }
}

// MARK: - Ready (zero state)

private data class SearchSuggestion(val symbol: String, val text: String)

/** Questions this resident can ask, from their own data (at most three). */
private fun DemoStore.searchSuggestions(): List<SearchSuggestion> {
    if (isGuest) {
        return listOf(
            SearchSuggestion("moon.stars", "When is the next prayer?"),
            SearchSuggestion("p.square", "Pay for parking"),
            SearchSuggestion("person.text.rectangle", "Check my visa status"),
        )
    }
    val s = state
    val out = mutableListOf<SearchSuggestion>()
    if (payables.isNotEmpty()) out += SearchSuggestion("list.bullet", "How much do I owe?")
    if (s.requests.any { "visa" in it.serviceID }) out += SearchSuggestion("person.text.rectangle", "When does my visa expire?")
    if (s.vehicles.any { DemoDate.daysFromToday(it.registrationExpiry) <= 30 }) out += SearchSuggestion("car", "Renew my car registration")
    if (s.salikBalance.signum() > 0) out += SearchSuggestion("road.lanes", "What's my Salik balance?")
    if (s.parking != null) out += SearchSuggestion("p.square", "How long is left on my parking?")
    out += SearchSuggestion("moon.stars", "When is the next prayer?")
    return out.take(3)
}

/** The most used live services in the last 30 days. */
private val searchPopular: List<CatalogueService> by lazy {
    Catalogue.shared.services.filter { it.isLive }.sortedByDescending { it.uniques30d }.take(5)
}

private fun LazyListScope.searchReadyItems(
    store: DemoStore,
    router: Router,
    resident: String,
    actions: SearchActions,
    animated: Modifier.(LazyItemScope) -> Modifier,
) {
    val recents = SearchRecents.items(resident)
    if (recents.isNotEmpty()) {
        item(key = "recent") {
            Box(Modifier.animated(this)) {
                SearchSection("Recent", actionTitle = "Clear", actionLabel = "Clear recent searches", action = { SearchRecents.clear(resident) }) {
                    // Chips are 40 dp pills in 48 dp targets: rows sit 8 dp apart, and the outer 4 dp overhang the gaps.
                    FlowRow(Modifier.searchOverhang(4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        recents.forEach { text -> SearchRecentChip(text) { actions.search(text) } }
                    }
                }
            }
        }
    }
    item(key = "ask") {
        val rows = store.searchSuggestions().map { s -> @Composable { SearchSuggestionRow(s.symbol, s.text) { actions.search(s.text) } } } +
            if (store.isGuest) {
                listOf(@Composable {
                    SearchSuggestionRow("person.crop.circle", "Sign in to ask about your bills, fines and visas") { actions.hideKeyboard(); store.signOut() }
                })
            } else {
                listOf(@Composable { SearchAskRow(null) { actions.hideKeyboard(); router.openAssistant() } })
            }
        Box(Modifier.animated(this)) {
            SearchSection(if (store.isGuest) "Try asking" else "Ask about your things") {
                SearchGroup(rows, separatorInset = 50.dp) { it() }
            }
        }
    }
    item(key = "popular") {
        Box(Modifier.animated(this)) {
            SearchSection("Popular in Dubai this week") {
                SearchGroup(searchPopular.withIndex().toList(), separatorInset = 50.dp) { (i, s) ->
                    SearchPopularRow(i + 1, s) { actions.openService(s) }
                }
            }
        }
    }
}

// MARK: - Results

private fun LazyListScope.searchResultItems(
    store: DemoStore,
    router: Router,
    r: SearchResults,
    trimmed: String,
    actions: SearchActions,
    animated: Modifier.(LazyItemScope) -> Modifier,
    showsAllServices: Boolean,
    onShowAll: () -> Unit,
    onSearchOriginal: () -> Unit,
) {
    val words = r.analysis.words

    r.analysis.corrected?.let { corrected ->
        item(key = "correction") {
            Box(Modifier.animated(this)) { SearchCorrectionHeader(corrected, trimmed, onSearchOriginal) }
        }
    }

    // 1 · Direct answer
    if (r.needsSignIn) {
        item(key = "answer-sign-in") {
            Box(Modifier.animated(this)) { SearchSignInCard { actions.hideKeyboard(); store.signOut() } }
        }
    } else {
        r.intent?.let { intent ->
            item(key = "answer-$intent") {
                Column(Modifier.animated(this), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SearchAnswer(store, intent, actions)
                    SearchFootnote(store.searchAnswerSources(intent))
                }
            }
        }
    }

    // 2 · Top result
    val thing = r.topThing
    val thingCard = thing?.card
    val topService = r.topService
    if (thing != null && thingCard != null) {
        item(key = "top-${thing.id}") {
            Box(Modifier.animated(this)) {
                SearchSection("Top result") {
                    SearchTopThingCard(thingCard, words, onOpen = { actions.perform(thing.action) }, onAct = { actions.perform(thingCard.buttonAction) })
                }
            }
        }
    } else if (topService != null) {
        item(key = "top-${topService.id}") {
            val strip = store.searchStrip(topService.service)
            Box(Modifier.animated(this)) {
                SearchTopServiceCard(topService, strip) {
                    val route = strip?.route
                    if (route != null) { actions.record(); actions.push(route) } else actions.openService(topService.service)
                }
            }
        }
    }

    // 3 · Services: at most 3, then See all
    if (r.services.isNotEmpty()) {
        item(key = "services") {
            val limit = 3
            val shown = if (showsAllServices) r.services else r.services.take(limit)
            Box(Modifier.animated(this)) {
                SearchSection(
                    r.servicesTitle,
                    actionTitle = if (r.services.size > limit && !showsAllServices) "See all ${r.services.size}" else null,
                    action = onShowAll,
                ) {
                    SearchGroup(shown, separatorInset = if (r.hasAnswer) 16.dp else 64.dp) { m ->
                        val personal = store.searchContext(m.service)
                        val context = m.alsoCalled?.let { "also called $it" } ?: personal?.first
                        val tone = if (m.alsoCalled == null) personal?.second ?: ChipTone.Neutral else ChipTone.Neutral
                        SearchServiceRow(m.service, words, showsIcon = !r.hasAnswer, context = context, contextTone = tone) { actions.openService(m.service) }
                    }
                }
            }
        }
    }

    // 4 · Your things
    if (r.things.isNotEmpty()) {
        item(key = "things") {
            Box(Modifier.animated(this)) {
                SearchSection("Your things") {
                    SearchGroup(r.things.take(6)) { t -> SearchThingRow(t, words) { actions.perform(t.action) } }
                }
            }
        }
    }

    // 5 · Widgets you can add, then help and guides
    if (r.widgets.isNotEmpty()) {
        item(key = "widgets") {
            Box(Modifier.animated(this)) {
                SearchSection(if (r.widgets.all { store.hasWidget(it) }) "Widgets" else "Widgets you can add") {
                    SearchGroup(r.widgets) { kind ->
                        val onHome = store.hasWidget(kind)
                        SearchWidgetRow(
                            kind, onHome,
                            onClick = { if (onHome) { actions.hideKeyboard(); router.select(AppTab.Home) } else actions.addWidget(kind) },
                            onAdd = { actions.addWidget(kind) },
                        )
                    }
                }
            }
        }
    }
    if (r.guides.isNotEmpty()) {
        item(key = "guides") {
            Box(Modifier.animated(this)) {
                SearchSection("Help and guides") {
                    SearchGroup(r.guides) { m ->
                        SearchServiceRow(m.service, words, showsIcon = true, context = null, contextTone = ChipTone.Neutral) { actions.openService(m.service) }
                    }
                }
            }
        }
    }

    if (r.isEmpty) {
        item(key = "empty") {
            Column(Modifier.animated(this).padding(start = 4.dp, end = 4.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("No results for “$trimmed”", style = DNTextStyle.Title3.style, color = DNColor.textPrimary)
                Text(
                    "Check the spelling or try another word. You can also browse every service, or ask DubaiNow.",
                    style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary,
                )
            }
        }
    }

    item(key = "not-what-you-need") {
        Box(Modifier.animated(this).padding(top = 4.dp)) {
            SearchSection("Not what you need?") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!store.isGuest) {
                        SearchGroup(listOf(trimmed)) { q ->
                            SearchAskRow(q) { actions.record(); actions.hideKeyboard(); router.openAssistant(q) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SearchCapsuleButton("Browse by topic", Modifier.weight(1f)) { actions.hideKeyboard(); router.select(AppTab.Services) }
                        SearchCapsuleButton("A to Z", Modifier.weight(1f)) { actions.push(Route.AllServices) }
                    }
                }
            }
        }
    }
}

// MARK: - Answers

@Composable
private fun SearchAnswer(store: DemoStore, intent: SearchIntent, actions: SearchActions) {
    val updated = DemoDate.time(store.now)
    when (intent) {
        SearchIntent.Owe -> SearchOweAnswer(
            payables = store.payables, updated = updated,
            onPayAll = {
                val items = store.payables
                if (items.size == 1) actions.pay(items[0]) else { actions.record(); actions.present(SheetRoute.Payment(store.payAllRequest())) }
            },
            onPay = { actions.pay(it) },
            onPayOneAtATime = actions.payOneAtATime,
        )
        is SearchIntent.Request -> store.request(intent.id)?.let { req ->
            val document = if ("visa" in req.serviceID) store.state.documents.firstOrNull { "visa" in it.title.lowercase() } else null
            SearchRequestAnswer(req, document, updated) { actions.record(); actions.push(Route.Request(req.id)) }
        }
        SearchIntent.SalikBalance -> {
            val balance = store.state.salikBalance
            val low = balance <= BigDecimal(20)
            SearchBalanceAnswer(
                title = "Your Salik balance", balance = balance,
                detail = "About ${searchTrips(balance)} trips left" + if (low) " · low" else "", low = low,
                lastTopUp = store.searchLastTopUp("top-up-salik-account"), updated = updated,
                onTopUp = { actions.record(); actions.present(SheetRoute.TopUpSalik) },
            )
        }
        SearchIntent.NolBalance -> SearchBalanceAnswer(
            title = "Your nol card balance", balance = store.state.nolBalance ?: BigDecimal.ZERO,
            detail = null, low = false, lastTopUp = store.searchLastTopUp("top-up-nol-card"), updated = updated,
            onTopUp = { actions.openServiceID("top-up-nol-card") },
        )
        is SearchIntent.Prayer -> SearchPrayerAnswer(intent.name, store.now, store.hasWidget(WidgetKind.Prayer)) { actions.addWidget(WidgetKind.Prayer) }
        SearchIntent.Parking -> SearchParkingAnswer(
            session = store.state.parking, updated = updated,
            onExtend = { actions.record(); actions.present(SheetRoute.ExtendParking) },
            onPayForParking = { actions.openServiceID("pay-for-parking") },
        )
    }
}
