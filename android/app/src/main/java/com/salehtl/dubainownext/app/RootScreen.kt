package com.salehtl.dubainownext.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.ToastHost
import com.salehtl.dubainownext.design.components.dnCanvasBackground
import com.salehtl.dubainownext.design.components.dnElevation
import com.salehtl.dubainownext.features.assistant.AssistantOverlay
import com.salehtl.dubainownext.features.welcome.WelcomeScreen

/** Welcome (signed out) or the main app. The assistant overlay and toasts sit above both. */
@Composable
fun RootScreen() {
    val store = LocalStore.current; val router = LocalRouter.current; val toasts = LocalToasts.current
    Box(Modifier.fillMaxSize().dnCanvasBackground()) {
        AnimatedContent(store.session == null, transitionSpec = { fadeIn(DNMotion.base()) togetherWith fadeOut(DNMotion.base()) }, label = "root") { signedOut ->
            if (signedOut) WelcomeScreen() else MainScaffold()
        }
        AnimatedVisibility(router.isAssistantPresented && store.session != null, enter = fadeIn(DNMotion.smooth()), exit = fadeOut(DNMotion.smooth())) {
            AssistantOverlay()
        }
        // Welcome has no tab bar but a tall sign-in panel at the bottom: toasts drop in under its header instead.
        ToastHost(toasts, topInset = if (store.session == null) WelcomeToastTop else null)
    }
}

private val WelcomeToastTop = 64.dp // below Welcome's story progress and header row

/**
 * The three tabs plus the search circle. Each tab keeps its own back stack (Router); the bottom
 * bar is our floating bar on a tonal surface (Paper Android kit), hidden on search, under the
 * assistant and while Home is being edited.
 */
@Composable
fun MainScaffold() {
    val router = LocalRouter.current; val store = LocalStore.current
    // Signing out fades the app out as it was: router.reset() has already gone back to Home by then.
    val lastTab = remember { arrayOf(router.tab) }
    val tab = if (store.session != null) router.tab.also { lastTab[0] = it } else lastTab[0]
    val stack = router.stack(tab)
    BackHandler(enabled = router.sheet == null && !router.isAssistantPresented && (stack.isNotEmpty() || router.tab != AppTab.Home)) {
        if (!router.pop()) { if (router.tab == AppTab.Search) router.closeSearch() else router.tab = AppTab.Home }
    }
    // Each screen (tab root or pushed route) keeps its saveable state (scroll, fields) while it's
    // covered by a push or another tab. A popped screen drops its state once it has left the screen.
    val saved = rememberSaveableStateHolder()
    val onScreen = remember { mutableSetOf<String>() }
    val seenKeys = remember { mutableSetOf<String>() }
    val liveKeys = router.screenKeys()
    SideEffect {
        (seenKeys - liveKeys - onScreen).forEach { saved.removeState(it) }
        seenKeys.retainAll(liveKeys); seenKeys.addAll(liveKeys)
    }
    Box(Modifier.fillMaxSize()) {
        // The target carries its route, so a screen that's sliding out keeps drawing itself (and its
        // state key) after the pop, instead of re-reading the shorter stack.
        AnimatedContent(
            targetState = TabScreen(tab, stack.size, stack.lastOrNull()),
            transitionSpec = {
                if (initialState.tab == targetState.tab) {
                    val forward = targetState.depth >= initialState.depth
                    (slideInHorizontally(DNMotion.smooth()) { if (forward) it / 3 else -it / 3 } + fadeIn(DNMotion.base())) togetherWith
                        (slideOutHorizontally(DNMotion.smooth()) { if (forward) -it / 3 else it / 3 } + fadeOut(DNMotion.base()))
                } else fadeIn(DNMotion.base()) togetherWith fadeOut(DNMotion.base())
            },
            label = "tab-content",
        ) { screen ->
            DisposableEffect(screen.key) {
                onScreen += screen.key
                onDispose { onScreen -= screen.key; if (screen.key !in router.screenKeys()) saved.removeState(screen.key) }
            }
            saved.SaveableStateProvider(screen.key) {
                Box(Modifier.fillMaxSize()) {
                    if (screen.route == null) TabRoot(screen.tab) else RouteDestination(screen.route)
                }
            }
        }
        AnimatedVisibility(
            visible = tab != AppTab.Search && !router.isAssistantPresented && !(router.hidesTabBar && tab == AppTab.Home),
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(DNMotion.smooth()) { it } + fadeIn(), exit = slideOutVertically(DNMotion.smooth()) { it } + fadeOut(),
        ) { BottomBar(tab) }
        SheetHost()
    }
}

/** One screen of a tab: its root ([route] null) or the route pushed at [depth]. */
private data class TabScreen(val tab: AppTab, val depth: Int, val route: Route?) {
    val key: String get() = "$tab/$depth/${route ?: "root"}"
}

/** The saved-state keys of every screen still on a tab's stack (roots included). */
private fun Router.screenKeys(): Set<String> = AppTab.entries.flatMap { t ->
    listOf(TabScreen(t, 0, null).key) + stack(t).mapIndexed { i, r -> TabScreen(t, i + 1, r).key }
}.toSet()

@Composable
private fun TabRoot(tab: AppTab) = when (tab) {
    AppTab.Home -> com.salehtl.dubainownext.features.home.HomeScreen()
    AppTab.Services -> com.salehtl.dubainownext.features.services.ServicesScreen()
    AppTab.Activity -> com.salehtl.dubainownext.features.activity.ActivityScreen()
    AppTab.Search -> com.salehtl.dubainownext.features.search.SearchScreen()
}

/**
 * For tab roots: scrolls back to the top when the selected tab is tapped again at its root, like
 * iOS's TabView and M3 navigation bars. Taps from before the screen appeared don't count.
 */
@Composable
fun ScrollToTopOnReselect(tab: AppTab, state: ScrollState) {
    val count = LocalRouter.current.reselectCount(tab)
    val start = remember { count }
    LaunchedEffect(count) { if (count != start) state.animateScrollTo(0) }
}

@Composable
fun ScrollToTopOnReselect(tab: AppTab, state: LazyListState) {
    val count = LocalRouter.current.reselectCount(tab)
    val start = remember { count }
    LaunchedEffect(count) { if (count != start) state.animateScrollToItem(0) }
}

/** Floating tonal bar (Home, Services, Activity) + separate search circle, above the gesture bar. */
@Composable
private fun BottomBar(shownTab: AppTab) {
    val router = LocalRouter.current; val view = LocalView.current
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.weight(1f).height(64.dp).dnElevation(32.dp).clip(CircleShape).background(DNColor.materialTonal).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(Triple(AppTab.Home, "Home", Icons.Rounded.Home), Triple(AppTab.Services, "Services", Icons.Rounded.GridView), Triple(AppTab.Activity, "Activity", Icons.Rounded.History))
                .forEach { (tab, label, icon) ->
                    TabItem(label, icon, selected = shownTab == tab, modifier = Modifier.weight(1f)) {
                        // The selected tab again: back to its root, or (already there) back to the top.
                        if (router.tab != tab) { DNHaptics.selection(view); router.tab = tab }
                        else if (router.stack(tab).isNotEmpty()) router.popToRoot() else router.reselect(tab)
                    }
                }
        }
        Box(
            Modifier.size(64.dp).dnElevation(32.dp).clip(CircleShape).background(DNColor.materialTonal)
                .clickable(role = Role.Button) { DNHaptics.selection(view); router.openSearch() }
                .semantics { contentDescription = "Search" },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Search, contentDescription = null, tint = DNColor.textPrimary, modifier = Modifier.size(26.dp)) }
    }
}

@Composable
private fun TabItem(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.fillMaxSize().clip(CircleShape).background(if (selected) DNColor.actionAccentSubtle else DNColor.materialTonal)
            .clickable(role = Role.Tab, onClick = onClick).semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        val c = if (selected) DNColor.actionAccent else DNColor.textPrimary
        Icon(icon, contentDescription = null, tint = c, modifier = Modifier.size(24.dp))
        Text(label, style = DNTextStyle.Caption1.style.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium), color = c)
    }
}
