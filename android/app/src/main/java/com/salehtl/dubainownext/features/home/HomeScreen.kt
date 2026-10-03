package com.salehtl.dubainownext.features.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.AppTab
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.MainHeader
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.design.AmbientWash
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNSpace
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.salehtl.dubainownext.app.ScrollToTopOnReselect

/** Where Home was scrolled to, kept across the shell recreating the tab root (pushes, tab switches). */
internal object HomeMemory {
    var persona: String? = null
    var scroll = 0
    var railIndex = 0
    var railOffset = 0
}

/** Pull past the end this far (after resistance) to open the widget gallery. */
private val HomePullThreshold = 100.dp
/** Finger travel → pull distance (rubber band). */
private const val HomePullResistance = 0.5f

/**
 * Home — the Canvas (Paper C1 · Home · Default).
 * Two fixed anchors (Needs you, then Favorites), then the resident's own widgets on a
 * two-column canvas. A time-of-day wash sits behind the top and scrolls with the content.
 * Pull past the end to open the widget gallery. Edit Home rearranges the widgets.
 */
@Composable
fun HomeScreen() {
    HomeFontScaleOverride { HomeScreenContent() }
}

@Composable
private fun HomeScreenContent() {
    val store = LocalStore.current; val router = LocalRouter.current; val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val personaKey = store.persona?.id ?: if (store.isGuest) "guest" else "none"
    // The shell recreates the tab root after a push; keep the place on Home like iOS does.
    val scroll = remember(personaKey) {
        if (HomeMemory.persona != personaKey) { HomeMemory.scroll = 0; HomeMemory.railIndex = 0; HomeMemory.railOffset = 0 }
        HomeMemory.persona = personaKey
        ScrollState(HomeMemory.scroll)
    }
    DisposableEffect(personaKey) {
        onDispose { if (HomeMemory.persona == personaKey) HomeMemory.scroll = scroll.value }
    }
    ScrollToTopOnReselect(AppTab.Home, scroll)
    var isEditing by remember(personaKey) { mutableStateOf(false) }
    var selectedID by remember(personaKey) { mutableStateOf<String?>(null) }
    val selected = selectedID?.let { id -> store.state.widgets.firstOrNull { it.id == id } }
    var widgetsY by remember { mutableFloatStateOf(0f) }
    var favoriteMenuIndex by remember { mutableStateOf<Int?>(null) }
    var sizeMenu by remember { mutableStateOf(false) }

    // Pull past the end (A7N-0).
    var pull by remember { mutableFloatStateOf(0f) }
    var armed by remember { mutableStateOf(false) }
    var settle by remember { mutableStateOf<Job?>(null) }
    val thresholdPx = with(density) { HomePullThreshold.toPx() }
    val showsFooter = store.state.widgets.isNotEmpty()
    val pullEnabled by rememberUpdatedState(!isEditing && showsFooter && router.sheet == null)

    fun enterEdit() {
        if (isEditing) return
        DNHaptics.medium(view)
        isEditing = true
        selectedID = null
        scope.launch { scroll.animateScrollTo(0, DNMotion.smooth()) }
        @Suppress("DEPRECATION")
        view.announceForAccessibility("Editing Home")
    }

    fun exitEdit() {
        DNHaptics.selection(view)
        isEditing = false
        selectedID = null
        scope.launch { scroll.animateScrollTo(0, DNMotion.smooth()) }
    }

    val pullConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Finger moving back down while pulled: shrink the pull before the content scrolls.
                if (pull > 0f && available.y > 0f && source == NestedScrollSource.UserInput) {
                    val reduce = minOf(available.y * HomePullResistance, pull)
                    pull -= reduce
                    homeUpdateArmed(thresholdPx, pull, armed) { armed = it }
                    return Offset(0f, reduce / HomePullResistance)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!pullEnabled || source != NestedScrollSource.UserInput || available.y >= 0f) return Offset.Zero
                settle?.cancel()
                pull += -available.y * HomePullResistance
                homeUpdateArmed(thresholdPx, pull, armed) {
                    armed = it
                    if (it) DNHaptics.soft(view)
                }
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pull <= 0f) return Velocity.Zero
                if (armed) {
                    armed = false
                    router.present(SheetRoute.WidgetGallery)
                }
                val from = pull
                settle = scope.launch { animate(from, 0f, animationSpec = DNMotion.smooth()) { v, _ -> pull = v } }
                return available
            }
        }
    }

    // A removed widget can't stay selected (Undo brings it back unselected, as on iOS).
    LaunchedEffect(store.state.widgets) {
        if (selectedID != null && store.state.widgets.none { it.id == selectedID }) selectedID = null
    }

    // A widget's "Edit Home" menu item asks Home to enter edit mode.
    LaunchedEffect(router.homeEditRequested) {
        if (router.homeEditRequested) {
            if (router.tab == AppTab.Home) enterEdit()
            router.homeEditRequested = false
        }
    }

    val statusTopPx = WindowInsets.statusBars.getTop(density).toFloat()

    // Debug extras (demo and screenshots), applied once per process.
    LaunchedEffect(Unit) {
        if (HomeLaunch.appliedHome) return@LaunchedEffect
        HomeLaunch.appliedHome = true
        if (HomeLaunch.folded) store.isNeedsYouFolded = true
        if (HomeLaunch.emptyNeedsYou) store.state = store.state.copy(needsYou = emptyList())
        HomeLaunch.fillFavoritesIfAsked(store)
        HomeLaunch.addJourneyCardIfAsked(store)
        favoriteMenuIndex = HomeLaunch.favoriteMenuIndex
        if (HomeLaunch.editing) {
            isEditing = true
            HomeLaunch.selectedWidgetIndex?.let { i -> selectedID = store.state.widgets.getOrNull(i)?.id }
            sizeMenu = HomeLaunch.sizeMenu
        }
        val target = HomeLaunch.scrollTarget
        val pullDp = HomeLaunch.pull
        if (target != null || pullDp != null) {
            delay(700)
            val headerPx = statusTopPx + with(density) { 72.dp.toPx() }
            when (target) {
                "widgets" -> scroll.scrollTo((widgetsY - headerPx - with(density) { 24.dp.toPx() }).toInt().coerceAtLeast(0))
                "favorites", "top" -> scroll.scrollTo(0)
                else -> scroll.scrollTo(scroll.maxValue)
            }
            if (pullDp != null) {
                scroll.scrollTo(scroll.maxValue)
                pull = with(density) { pullDp.dp.toPx() }
                armed = pull >= thresholdPx
            }
        }
    }

    BackHandler(enabled = isEditing) { exitEdit() }
    LaunchedEffect(isEditing) { router.hidesTabBar = isEditing }
    DisposableEffect(Unit) { onDispose { router.hidesTabBar = false } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val grid = HomeGrid(constraints.maxWidth, density)
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val headerHeight = topInset + 72.dp
        val tabBarSpace = bottomInset + 12.dp + 64.dp
        val editBarSpace = bottomInset + 12.dp // no tab bar while editing
        val washHeight = 420.dp + topInset
        val timeOfDay = store.timeOfDay

        // Content (scrolls; the wash scrolls with it, starting behind the header).
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { translationY = -pull }
                .nestedScroll(pullConnection)
                .verticalScroll(scroll),
        ) {
            Crossfade(timeOfDay, animationSpec = DNMotion.wash(), label = "wash") { tod ->
                AmbientWash(tod, height = washHeight)
            }
            AnimatedContent(
                targetState = isEditing,
                transitionSpec = { fadeIn(DNMotion.base()) togetherWith fadeOut(DNMotion.fast()) using SizeTransform(clip = false) },
                label = "home-mode",
            ) { editing ->
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(headerHeight))
                    if (editing) {
                        HomeEditPinnedGroup(Modifier.padding(top = 18.dp))
                        HomeWidgetsSection(
                            grid, isEditing = true, selectedID = selectedID,
                            onSelect = { w ->
                                DNHaptics.selection(view)
                                selectedID = if (selectedID == w.id) null else w.id
                            },
                            onEdit = {}, modifier = Modifier.padding(top = 22.dp),
                        )
                        Spacer(Modifier.height(editBarSpace + 64.dp + 48.dp))
                    } else {
                        if (store.isGuest) HomeGuestSignInCard(Modifier.padding(top = 12.dp))
                        else HomeNeedsYouSection(Modifier.padding(top = 12.dp))
                        HomeFavoritesSection(grid, Modifier.padding(top = DNSpace.gapScreenSection), menuIndex = favoriteMenuIndex)
                        HomeWidgetsSection(
                            grid, isEditing = false, selectedID = null, onSelect = {}, onEdit = { enterEdit() },
                            modifier = Modifier.padding(top = DNSpace.gapScreenSection)
                                .onGloballyPositioned { widgetsY = it.positionInParent().y },
                        )
                        if (showsFooter) {
                            val hintFade = with(density) { 40.dp.toPx() }
                            HomeWidgetsFooter(hintAlpha = (1f - pull / hintFade).coerceIn(0f, 1f), modifier = Modifier.padding(top = DNSpace.sm))
                        }
                        Spacer(Modifier.height(tabBarSpace + 44.dp))
                    }
                }
            }
        }

        // Scroll edge behind the header: the wash colour at the current offset, fading out at the bottom.
        Box(
            Modifier.fillMaxWidth().height(headerHeight + 18.dp).drawBehind {
                val s = scroll.value.toFloat()
                if (s <= 0f) return@drawBehind
                val show = (s / 16.dp.toPx()).coerceIn(0f, 1f)
                val t = ((s + headerHeight.toPx() / 2) / washHeight.toPx()).coerceIn(0f, 1f)
                val a = if (t < 0.5f) 1f - 0.45f * (t * 2) else 0.55f * (1f - (t - 0.5f) * 2)
                val edge = lerp(DNColor.surfaceSecondary, timeOfDay.wash, a.coerceIn(0f, 1f))
                drawRect(
                    Brush.verticalGradient(
                        0f to edge.copy(alpha = 0.97f * show),
                        0.72f to edge.copy(alpha = 0.92f * show),
                        1f to edge.copy(alpha = 0f),
                    ),
                )
            },
        )

        // Header: the main header (smiley · logo · bell · avatar), or Add · Edit Home · Done.
        AnimatedContent(
            targetState = isEditing,
            transitionSpec = { fadeIn(DNMotion.base()) togetherWith fadeOut(DNMotion.fast()) },
            label = "home-header",
        ) { editing ->
            if (editing) HomeEditTopBar(onAdd = { router.present(SheetRoute.WidgetGallery) }, onDone = { exitEdit() })
            else MainHeader(showsBell = true)
        }

        // Pull indicator, above the tab bar.
        val indicatorAlpha = with(density) { ((pull - 40.dp.toPx()) / 40.dp.toPx()).coerceIn(0f, 1f) }
        if (pull > 0f) {
            HomePullIndicator(
                progress = pull / thresholdPx, armed = armed,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = tabBarSpace + 20.dp)
                    .graphicsLayer { alpha = indicatorAlpha },
            )
        }

        // Edit Home bar (the shell hides its tab bar while editing, so this takes its place).
        AnimatedVisibility(
            visible = isEditing,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = editBarSpace),
            enter = slideInVertically(DNMotion.smooth()) { it } + fadeIn(DNMotion.base()),
            exit = slideOutVertically(DNMotion.smooth()) { it } + fadeOut(DNMotion.fast()),
        ) {
            HomeEditBottomBar(selected, sizeMenuInitially = sizeMenu)
        }
    }
}

private inline fun homeUpdateArmed(threshold: Float, pull: Float, armed: Boolean, set: (Boolean) -> Unit) {
    if (pull >= threshold && !armed) set(true) else if (pull < threshold && armed) set(false)
}
