package com.salehtl.dubainownext.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.AppTab
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.NeedsYouItem
import com.salehtl.dubainownext.data.Payable
import com.salehtl.dubainownext.data.Vehicle
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNIcons
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSize
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNButtonKind
import com.salehtl.dubainownext.design.components.DNButtonSize
import com.salehtl.dubainownext.design.components.MoneyText
import com.salehtl.dubainownext.design.components.NumberPlate
import com.salehtl.dubainownext.design.components.RouteMark
import com.salehtl.dubainownext.design.components.dnCard

/** The staggered entrance runs once per process (not again after coming back from a pushed screen). */
private object HomeRailEntrance { var played = false }

/** One card's fade and rise in the rail's entrance. */
private const val HOME_CARD_ENTRANCE_MS = 320

/** The rail keeps 14 dp under the cards for their shadow; Favorites starts 10 dp into it (the iOS spacing). */
private val HomeRailShadowOverlap = 10.dp

/** Progress (0…1) of card [index] when the rail's entrance clock is at [clock] (0…1 over [totalMs]). */
private fun homeCardEntrance(clock: Float, index: Int, totalMs: Int): Float {
    if (clock >= 1f) return 1f
    val local = ((clock * totalMs - DNMotion.STAGGER_MS * index) / HOME_CARD_ENTRANCE_MS).coerceIn(0f, 1f)
    return FastOutSlowInEasing.transform(local)
}

/**
 * Anchor 1 · Needs you. Heading, count, fold chevron, summary, See all, Pay all and the
 * rail of brief cards. Folded (C1 · Needs you, folded): one line that keeps the count,
 * the next thing due and Pay all; the whole header is the button that unfolds it.
 */
@Composable
internal fun HomeNeedsYouSection(modifier: Modifier = Modifier) {
    val store = LocalStore.current
    val items = store.state.needsYou
    val animationsOff = homeAnimationsOff()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DNSpace.sm)) {
        if (items.isEmpty()) {
            HomeCaughtUp()
        } else {
            HomeNeedsYouHeader(items)
            if (store.showsPayAll) HomePayAllPill(Modifier.padding(horizontal = DNSpace.insetPage))
            AnimatedVisibility(
                visible = !store.isNeedsYouFolded,
                modifier = Modifier.layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    val trim = HomeRailShadowOverlap.roundToPx().coerceAtMost(p.height)
                    layout(p.width, p.height - trim) { p.place(0, 0) }
                },
                enter = if (animationsOff) fadeIn(DNMotion.base()) else expandVertically(DNMotion.smooth()) + fadeIn(DNMotion.base()),
                exit = if (animationsOff) fadeOut(DNMotion.base()) else shrinkVertically(DNMotion.smooth()) + fadeOut(DNMotion.fast()),
            ) {
                HomeNeedsYouRail(items)
            }
        }
    }
}

// MARK: Header

@Composable
private fun HomeNeedsYouHeader(items: List<NeedsYouItem>) {
    val store = LocalStore.current; val router = LocalRouter.current; val view = LocalView.current
    val folded = store.isNeedsYouFolded
    val countPhrase = if (items.size == 1) "1 item" else "${items.size} items"
    val toggle = {
        DNHaptics.selection(view)
        store.isNeedsYouFolded = !store.isNeedsYouFolded
        @Suppress("DEPRECATION")
        view.announceForAccessibility(if (store.isNeedsYouFolded) "Needs you folded" else "Needs you unfolded")
    }
    if (folded) {
        val line = homeFoldedLine()
        Row(
            Modifier.fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = "Unfold") { toggle() }
                .padding(horizontal = DNSpace.insetPage, vertical = 2.dp)
                .clearAndSetSemantics {
                    contentDescription = "Needs you, $countPhrase, folded. $line"
                    role = Role.Button
                    onClick(label = "Unfold") { toggle(); true }
                },
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                HomeNeedsYouHeadingRow(items.size, folded = true, onToggle = null)
                if (store.homeDueToday != null) {
                    HomeClockLine(line, maxLines = 2)
                } else {
                    Text(line, style = DNTextStyle.Subheadline.style, color = DNColor.textOnWashSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    } else {
        Row(Modifier.fillMaxWidth().padding(horizontal = DNSpace.insetPage), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                HomeNeedsYouHeadingRow(items.size, folded = false, onToggle = toggle)
                val today = store.homeDueToday
                val due = today?.due
                if (due != null) {
                    HomeClockLine(if (DemoDate.daysFromToday(due) < 0) "Something's overdue" else "Something's due today", maxLines = 1)
                } else {
                    Text(
                        if (store.dueThisWeekCount > 0) "${store.dueThisWeekCount} due this week" else "Nothing due this week",
                        style = DNTextStyle.Subheadline.style, color = DNColor.textOnWashSecondary,
                    )
                }
            }
            HomeLinkButton("See all", color = DNColor.textOnWashLink, description = "See all in Activity") { router.select(AppTab.Activity) }
        }
    }
}

@Composable
private fun HomeNeedsYouHeadingRow(count: Int, folded: Boolean, onToggle: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Needs you", style = DNTextStyle.Section.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
        HomeCountBadge(count, Modifier.semantics { contentDescription = if (count == 1) "1 item" else "$count items" })
        // 28 dp chevron in a 48 dp target that doesn't push the heading row taller.
        Box(Modifier.size(28.dp).wrapContentSize(unbounded = true)) {
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .let { m ->
                        if (onToggle != null) m.clickable(role = Role.Button, onClickLabel = "Fold") { onToggle() }
                            .semantics { contentDescription = "Fold Needs you" }
                        else m.clearAndSetSemantics { }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(DNColor.neutral200), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.KeyboardArrowUp, contentDescription = null, tint = DNColor.textPrimary,
                        modifier = Modifier.size(20.dp).rotate(if (folded) 180f else 0f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeClockLine(text: String, maxLines: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = DNColor.statusWarningForeground, modifier = Modifier.size(15.dp))
        Text(
            text, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold),
            color = DNColor.statusWarningForeground, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** "Due today: …" or "Next: electricity and water bill, tomorrow". */
@Composable
private fun homeFoldedLine(): String {
    val store = LocalStore.current
    store.homeDueToday?.let { return "Due today: ${HomeCopy.inline(it.title)}" }
    val next = store.nextDue
    val due = next?.due
    if (next != null && due != null) return "Next: ${HomeCopy.inline(next.title)}, ${HomeCopy.whenText(due)}"
    return store.state.needsYou.firstOrNull()?.let { "Next: ${HomeCopy.inline(it.title)}" } ?: ""
}

// MARK: Pay all

@Composable
private fun HomePayAllPill(modifier: Modifier = Modifier) {
    val store = LocalStore.current; val router = LocalRouter.current
    val interaction = remember { MutableInteractionSource() }
    val total = store.payAllTotal
    Box(
        modifier
            .heightIn(min = 48.dp)
            .homePressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                router.present(SheetRoute.Payment(store.payAllRequest()))
            }
            .semantics(mergeDescendants = true) { contentDescription = "Pay all, ${Money.spoken(total)}, ${store.payables.size} payments" },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier.heightIn(min = 40.dp).clip(CircleShape).background(DNColor.actionAccent)
                .indication(interaction, ripple(color = DNColor.white))
                .padding(start = 14.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomePayAllGlyph()
            // One line; wraps rather than clips at the largest font sizes.
            Text(
                "Pay all · ${Money.aed(total)}", style = DNTextStyle.Callout.style.copy(fontWeight = FontWeight.SemiBold),
                color = DNColor.textOnAccent, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
    }
}

// MARK: Rail

/**
 * The rail of brief cards: a LazyRow that snaps card by card. Every card takes the height of the
 * tallest one (measured first), so the actions line up along the bottom.
 */
@Composable
private fun HomeNeedsYouRail(items: List<NeedsYouItem>) {
    val store = LocalStore.current; val toasts = LocalToasts.current
    val large = LocalDensity.current.fontScale >= 1.5f
    val cardWidth = if (large) 320.dp else DNSize.briefCard
    val listState = rememberLazyListState(HomeMemory.railIndex, HomeMemory.railOffset)
    DisposableEffect(Unit) {
        onDispose { HomeMemory.railIndex = listState.firstVisibleItemIndex; HomeMemory.railOffset = listState.firstVisibleItemScrollOffset }
    }
    val fling = rememberSnapFlingBehavior(listState, SnapPosition.Start)
    // Something new at the front (or Undo after Later): bring the rail back to its start.
    val firstID = items.firstOrNull()?.id
    var lastFirst by remember { mutableStateOf(firstID) }
    LaunchedEffect(firstID) {
        if (firstID != lastFirst) { lastFirst = firstID; listState.animateScrollToItem(0) }
    }
    // Staggered entrance, driven by one clock for the whole rail: a card that scrolls out and back
    // in (LazyRow drops its state) reads the finished clock instead of fading in again.
    val animationsOff = homeAnimationsOff()
    val entranceCards = remember { items.size.coerceIn(1, 4) }
    val entranceMs = HOME_CARD_ENTRANCE_MS + (DNMotion.STAGGER_MS * (entranceCards - 1)).toInt()
    val entrance = remember { Animatable(if (!HomeRailEntrance.played && !animationsOff) 0f else 1f) }
    LaunchedEffect(Unit) {
        HomeRailEntrance.played = true
        if (entrance.value < 1f) entrance.animateTo(1f, tween(entranceMs, easing = LinearEasing))
    }

    val later: (NeedsYouItem) -> Unit = { item ->
        val before = store.state.needsYou
        store.later(item)
        toasts.show("Moved to the back. It's still in Activity.", undo = { store.homeRestoreNeedsYou(before) })
    }

    SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
        val cardPx = cardWidth.roundToPx()
        val card = Constraints(minWidth = cardPx, maxWidth = cardPx)
        // One layout for every card, as iOS switches them all at once: if one card's actions don't
        // fit side by side, every card stacks them; every poster takes the tallest poster's height.
        val actionsPx = cardPx - (HomeBriefInset * 2).roundToPx()
        val stackActions = large || items.any { item ->
            !homeBriefActionsFit(subcompose("actions-${item.id}") { HomeBriefButtons(item, {}, {}) }, actionsPx)
        }
        val posterHeight = (items.maxOfOrNull { item ->
            subcompose("poster-${item.id}") { HomeBriefPoster(item, height = null, large = large) }.first().measure(card).height
        } ?: 0).toDp()
        val tallest = items.mapIndexed { i, item ->
            subcompose("measure-${item.id}-$i") { HomeBriefCard(item, onLater = {}, height = null, posterHeight = posterHeight, stackActions = stackActions, large = large) }
                .firstOrNull()?.measure(card)?.height ?: 0
        }.maxOrNull() ?: 0
        val cardHeight = tallest.toDp()
        val rail = subcompose("rail") {
            LazyRow(
                state = listState,
                flingBehavior = fling,
                contentPadding = PaddingValues(start = DNSpace.insetPage, end = DNSpace.insetPage, top = 2.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(DNSpace.widgetGutter),
            ) {
                itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
                    HomeBriefCard(
                        item, onLater = { later(item) }, height = cardHeight, posterHeight = posterHeight, stackActions = stackActions, large = large,
                        modifier = Modifier
                            .width(cardWidth)
                            .animateItem()
                            .graphicsLayer {
                                val t = homeCardEntrance(entrance.value, minOf(index, entranceCards - 1), entranceMs)
                                alpha = t
                                translationY = (1f - t) * 12.dp.toPx()
                            },
                    )
                }
            }
        }.first().measure(constraints.copy(minHeight = 0))
        layout(rail.width, rail.height) { rail.place(0, 0) }
    }
}

// MARK: All caught up (A3J-0)

@Composable
private fun HomeCaughtUp() {
    Column(Modifier.fillMaxWidth().padding(horizontal = DNSpace.insetPage), verticalArrangement = Arrangement.spacedBy(DNSpace.sm)) {
        Box(Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.BottomStart) {
            Text("Needs you", style = DNTextStyle.Section.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
        }
        Row(
            Modifier.fillMaxWidth().dnCard(DNRadius.brief).padding(18.dp).semantics(mergeDescendants = true) { },
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(DNColor.statusSuccessSurface), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = DNColor.statusSuccessForeground, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("You're all caught up", style = DNTextStyle.Headline.style, color = DNColor.textPrimary)
                Text("Bills, fines and renewals show up here when they need you.", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
            }
        }
    }
}

// MARK: Guest (WOV-0)

/** Guest · Home: in place of Needs you, an invitation to sign in (back to Welcome, which picks a resident). */
@Composable
internal fun HomeGuestSignInCard(modifier: Modifier = Modifier) {
    val store = LocalStore.current; val view = LocalView.current
    Column(
        modifier.fillMaxWidth().padding(horizontal = DNSpace.insetPage).dnCard(DNRadius.brief).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("See what needs you", style = DNTextStyle.Section.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
        Text(
            "Sign in to see your bills, fines and renewals here, and pay them in one go.",
            style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(CircleShape).background(DNColor.surfaceInverse)
                .clickable(role = Role.Button) { DNHaptics.light(view); store.signOut() }
                .semantics(mergeDescendants = true) { },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Fingerprint, contentDescription = null, tint = DNColor.textOnInverse,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 20.dp).size(26.dp),
            )
            Text(
                "Sign in with UAE PASS", style = DNTextStyle.Button.style, color = DNColor.textOnInverse, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 56.dp),
            )
        }
    }
}

// MARK: Brief card

/** Side padding of a brief card's poster and body. */
private val HomeBriefInset = 16.dp

/**
 * A 272 dp brief card (radius 24): a 120 dp poster in the item's hue with the entity badge,
 * a status chip and the figure (amount, steps or stage), then title, detail and actions.
 * [height] null = natural height (used to measure); otherwise the card fills it and the
 * actions sit on the bottom edge. The rail passes [posterHeight] and [stackActions] so every
 * card is laid out the same way.
 */
@Composable
private fun HomeBriefCard(
    item: NeedsYouItem,
    onLater: () -> Unit,
    height: Dp?,
    posterHeight: Dp,
    stackActions: Boolean,
    large: Boolean,
    modifier: Modifier = Modifier,
) {
    val store = LocalStore.current; val router = LocalRouter.current
    val chip = homeBriefChip(item)
    val summary = buildList {
        add(item.title); add(item.detail)
        item.amount?.let { add(Money.spoken(it)) }
        chip?.let { add(it.first) }
    }.joinToString(", ")

    val primary = {
        when (val k = item.kind) {
            is NeedsYouItem.Kind.Payment -> router.present(SheetRoute.Payment(store.paymentRequest(k.payable)))
            is NeedsYouItem.Kind.RequestStep -> router.push(Route.Request(k.requestID))
            is NeedsYouItem.Kind.Renewal -> router.open(k.serviceID)
            is NeedsYouItem.Kind.JourneyStep -> router.push(Route.Journey(k.journeyID))
        }
    }
    val secondary = { title: String ->
        if (title == "Later") onLater() else when (val k = item.kind) {
            is NeedsYouItem.Kind.Payment -> k.payable.serviceID?.let { router.open(it) } ?: onLater()
            is NeedsYouItem.Kind.RequestStep -> router.push(Route.Request(k.requestID))
            is NeedsYouItem.Kind.Renewal -> homeVehicle(store, k.thingID)?.let { router.push(Route.Vehicle(it.id)) } ?: router.open(k.serviceID)
            is NeedsYouItem.Kind.JourneyStep -> router.push(Route.Journey(k.journeyID))
        }
    }

    Column(
        modifier
            .let { if (height != null) it.height(height) else it }
            .dnCard(DNRadius.brief)
            .semantics { customActions = item.secondaryAction?.let { listOf(CustomAccessibilityAction(it) { secondary(it); true }) } ?: emptyList() },
    ) {
        HomeBriefPoster(item, posterHeight, large)
        // Body
        Column(
            Modifier.fillMaxWidth().let { if (height != null) it.weight(1f) else it }
                .padding(start = HomeBriefInset, end = HomeBriefInset, top = 14.dp, bottom = 16.dp),
        ) {
            Column(Modifier.clearAndSetSemantics { contentDescription = summary }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, style = DNTextStyle.Headline.style, color = DNColor.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.detail, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(12.dp))
            if (height != null) Spacer(Modifier.weight(1f))
            HomeBriefActions(stacked = stackActions) { HomeBriefButtons(item, primary, secondary) }
        }
    }
}

/**
 * The poster: entity badge and status chip on top, the figure at the bottom, a progress bar along
 * the bottom edge for steps. [height] null = natural height, at least 120 dp (used to measure);
 * the rail then gives every poster the tallest one's height.
 */
@Composable
private fun HomeBriefPoster(item: NeedsYouItem, height: Dp?, large: Boolean) {
    val isJourney = item.kind is NeedsYouItem.Kind.JourneyStep
    val chip = homeBriefChip(item)
    Box(
        Modifier.fillMaxWidth()
            .let { if (height != null) it.height(height) else it.heightIn(min = 120.dp) }
            .background(if (isJourney) item.hue.gradient else androidx.compose.ui.graphics.SolidColor(item.hue.start))
            .clearAndSetSemantics { },
    ) {
        val fraction = homeProgressFraction(item)
        Column(
            Modifier.fillMaxWidth().let { if (height != null) it.fillMaxHeight() else it }
                .padding(start = HomeBriefInset, end = HomeBriefInset, top = 14.dp, bottom = if (fraction == null) 14.dp else 18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            HomeBriefTopRow(item, chip, stacked = large)
            HomeBriefFigure(item)
        }
        if (fraction != null) {
            val track = if (isJourney) item.hue.ink.copy(alpha = 0.2f) else DNColor.progressTrackOnHue
            val fill = if (isJourney) item.hue.ink else DNColor.actionAccent
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(5.dp).background(track)) {
                Box(Modifier.fillMaxWidth(fraction.toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(fill))
            }
        }
    }
}

/**
 * Entity badge with the status chip on the right, or the chip under the badge when [stacked] or
 * when the two don't fit on one line (iOS stacks them at accessibility sizes; Inter runs wider than
 * SF, so Android can need it at Large and Largest text too). Keeps at least 6 dp under it for the
 * figure, 10 dp when stacked (as iOS).
 */
@Composable
private fun HomeBriefTopRow(item: NeedsYouItem, chip: Pair<String, HomeChipTone>?, stacked: Boolean, modifier: Modifier = Modifier) {
    Layout(
        content = {
            HomeBriefBadge(item)
            chip?.let { HomeBriefChip(it.first, it.second) }
        },
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val any = Constraints(maxWidth = width)
        val badge = measurables[0]
        val chipM = measurables.getOrNull(1)
        val gap = 4.dp.roundToPx()
        val below = (if (stacked) 10.dp else 6.dp).roundToPx()
        if (chipM == null) {
            val b = badge.measure(any)
            layout(width, b.height + below) { b.place(0, 0) }
        } else if (!stacked && badge.maxIntrinsicWidth(Constraints.Infinity) + gap + chipM.maxIntrinsicWidth(Constraints.Infinity) <= width) {
            val c = chipM.measure(any)
            val b = badge.measure(Constraints(maxWidth = (width - gap - c.width).coerceAtLeast(0)))
            val h = maxOf(b.height, c.height)
            layout(width, h + below) { b.place(0, (h - b.height) / 2); c.place(width - c.width, (h - c.height) / 2) }
        } else {
            val b = badge.measure(any)
            val c = chipM.measure(any)
            val spacing = 8.dp.roundToPx()
            layout(width, b.height + spacing + c.height + 10.dp.roundToPx()) { b.place(0, 0); c.place(0, b.height + spacing) }
        }
    }
}

/** The primary action, then the secondary one if the item has it. */
@Composable
private fun HomeBriefButtons(item: NeedsYouItem, onPrimary: () -> Unit, onSecondary: (String) -> Unit) {
    DNButton(item.primaryAction, onClick = onPrimary, size = DNButtonSize.Medium)
    item.secondaryAction?.let { DNButton(it, onClick = { onSecondary(it) }, kind = DNButtonKind.Secondary, size = DNButtonSize.Medium) }
}

/** The secondary action's width side by side: its label's width, at least 88 dp. */
private fun Density.homeBriefSecondWidth(second: IntrinsicMeasurable): Int =
    maxOf(second.maxIntrinsicWidth(Constraints.Infinity), 88.dp.roundToPx())

/** One action, or two whose labels fit side by side in [width] px (8 dp apart) without truncating. */
private fun Density.homeBriefActionsFit(buttons: List<IntrinsicMeasurable>, width: Int): Boolean {
    val second = buttons.getOrNull(1) ?: return true
    return buttons[0].maxIntrinsicWidth(Constraints.Infinity) + 8.dp.roundToPx() + homeBriefSecondWidth(second) <= width
}

/**
 * The card's actions side by side (the secondary at its natural width, the primary takes the rest),
 * or stacked full width when [stacked] or when they don't fit. iOS swaps HStack for VStack at
 * accessibility sizes; with Inter, Android needs it from about 115% text.
 */
@Composable
private fun HomeBriefActions(stacked: Boolean, content: @Composable () -> Unit) {
    Layout(content, Modifier.fillMaxWidth()) { measurables, constraints ->
        val width = constraints.maxWidth
        val gap = 8.dp.roundToPx()
        val primary = measurables[0]
        val second = measurables.getOrNull(1)
        if (second == null) {
            val p = primary.measure(Constraints.fixedWidth(width))
            layout(width, p.height) { p.place(0, 0) }
        } else if (!stacked && homeBriefActionsFit(measurables, width)) {
            val secondWidth = homeBriefSecondWidth(second)
            val s = second.measure(Constraints.fixedWidth(secondWidth))
            val p = primary.measure(Constraints.fixedWidth(width - gap - secondWidth))
            val h = maxOf(p.height, s.height)
            layout(width, h) { p.place(0, (h - p.height) / 2); s.place(width - secondWidth, (h - s.height) / 2) }
        } else {
            val p = primary.measure(Constraints.fixedWidth(width))
            val s = second.measure(Constraints.fixedWidth(width))
            layout(width, p.height + gap + s.height) { p.place(0, 0); s.place(0, p.height + gap) }
        }
    }
}

@Composable
private fun HomeBriefBadge(item: NeedsYouItem, modifier: Modifier = Modifier) {
    val title = (item.kind as? NeedsYouItem.Kind.JourneyStep)?.let { Catalogue.service(it.journeyID)?.short } ?: item.entity
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(DNColor.posterGlyph.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
            if (item.kind is NeedsYouItem.Kind.JourneyStep) RouteMark(color = item.hue.ink, size = 16.dp)
            else Icon(DNIcons.forSymbol(item.symbol), contentDescription = null, tint = item.hue.ink, modifier = Modifier.size(18.dp))
        }
        Text(title, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HomeBriefFigure(item: NeedsYouItem) {
    when (val k = item.kind) {
        is NeedsYouItem.Kind.Payment -> MoneyText(k.payable.amount)
        is NeedsYouItem.Kind.RequestStep -> {
            val (current, total) = homeRequestProgress(item, k.requestID)
            HomeStepsFigure("$current", "of $total steps")
        }
        is NeedsYouItem.Kind.JourneyStep -> {
            val j = homeJourneyInfo(k.journeyID)
            HomeStepsFigure("${j.first}", j.third?.let { "of ${j.second} · $it" } ?: "of ${j.second}")
        }
        is NeedsYouItem.Kind.Renewal -> {
            val v = homeVehicle(LocalStore.current, k.thingID)
            val due = item.due
            if (v != null) NumberPlate(v.plate.code, v.plate.number, emirate = v.plate.emirate)
            else if (due != null) Text(DemoDate.short(due), style = DNTextStyle.NumericMedium.style, color = DNColor.textPrimary)
        }
    }
}

@Composable
private fun HomeStepsFigure(big: String, rest: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(big, style = DNTextStyle.NumericLarge.style, color = DNColor.textPrimary, modifier = Modifier.alignByBaseline())
        Text(rest, style = DNTextStyle.NumericUnit.style, color = DNColor.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.alignByBaseline())
    }
}

private fun homeBriefChip(item: NeedsYouItem): Pair<String, HomeChipTone>? {
    if (item.kind is NeedsYouItem.Kind.JourneyStep) return "Your turn" to HomeChipTone.Neutral
    item.due?.let { due ->
        val days = DemoDate.daysFromToday(due)
        val tone = if (days < 0) HomeChipTone.Error else if (days <= 1) HomeChipTone.Warning else HomeChipTone.Neutral
        return DemoDate.dueLabel(due) to tone
    }
    if (item.payable?.kind == Payable.Kind.Fine) return "Unpaid" to HomeChipTone.Error
    return null
}

@Composable
private fun homeProgressFraction(item: NeedsYouItem): Double? = when (val k = item.kind) {
    is NeedsYouItem.Kind.RequestStep -> {
        val (current, total) = homeRequestProgress(item, k.requestID)
        maxOf(current - 1, 0).toDouble() / maxOf(total, 1)
    }
    is NeedsYouItem.Kind.JourneyStep -> {
        val j = homeJourneyInfo(k.journeyID)
        (j.first - 0.5) / maxOf(j.second, 1)
    }
    else -> null
}

@Composable
private fun homeRequestProgress(item: NeedsYouItem, id: String): Pair<Int, Int> {
    item.progress?.let { return it.current to it.total }
    val r = LocalStore.current.request(id) ?: return 1 to 1
    return ((r.currentStepIndex ?: 0) + 1) to r.steps.size
}

/** (current stage number, total stages, stage name). */
@Composable
private fun homeJourneyInfo(id: String): Triple<Int, Int, String?> {
    val stages = Catalogue.service(id)?.stages ?: emptyList()
    val index = LocalStore.current.state.journeys.firstOrNull { it.journeyID == id }?.stageIndex ?: 0
    val total = maxOf(stages.size, index + 1)
    return Triple(index + 1, total, stages.getOrNull(index)?.name)
}

private fun homeVehicle(store: com.salehtl.dubainownext.data.DemoStore, thingID: String?): Vehicle? {
    thingID ?: return null
    return store.state.vehicles.firstOrNull { it.id == thingID }
}

// MARK: Link button

/**
 * "See all", "Edit": 17 medium link in a 48 dp target, text aligned with the page inset
 * (the right one; the left one when [leading]).
 */
@Composable
internal fun HomeLinkButton(
    title: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = DNColor.textLink,
    description: String? = null,
    leading: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier.offset(x = if (leading) (-8).dp else 8.dp).heightIn(min = 48.dp).widthIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp)
            .let { if (description != null) it.semantics { contentDescription = description } else it },
        contentAlignment = Alignment.Center,
    ) {
        Text(title, style = DNTextStyle.Body.style.copy(fontWeight = FontWeight.Medium), color = color)
    }
}
