package com.salehtl.dubainownext.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.AppTab
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.CatalogueTopic
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.FavoriteItem
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.NeedsYouItem
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.data.hue
import com.salehtl.dubainownext.data.symbol
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNButtonKind
import com.salehtl.dubainownext.design.components.DNButtonSize
import com.salehtl.dubainownext.design.components.DNListGroup
import com.salehtl.dubainownext.design.components.DNRowDivider
import com.salehtl.dubainownext.design.components.HueIcon
import com.salehtl.dubainownext.design.components.HueTile
import com.salehtl.dubainownext.design.components.TonalIconButton
import kotlinx.coroutines.launch

/** A row you can add to (or remove from) Favorites. */
private data class HomeFavoriteCandidate(val service: CatalogueService, val title: String, val subtitle: String)

/**
 * Favorites edit sheet (C1 · Favorites edit sheet, Limit reached 8 of 8).
 * Mirrors the Home grid: your favorites in order (remove; select one and move it earlier or
 * later), empty slots up to eight, then things to add — suggested for you, your things (hubs),
 * the services of your main topic, or search results. Changes show on Home straight away;
 * × puts them back, ✓ keeps them (so does swiping the sheet away).
 */
@Composable
fun EditFavoritesSheet() {
    HomeFontScaleOverride { HomeEditFavoritesContent() }
}

@Composable
private fun HomeEditFavoritesContent() {
    val store = LocalStore.current; val router = LocalRouter.current; val view = LocalView.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val favorites = store.state.favorites
    val limit = DemoStore.FAVORITES_LIMIT
    val isFull = favorites.size >= limit
    var snapshot by remember { mutableStateOf<List<FavoriteItem>?>(null) }
    val hasChanges = snapshot?.let { it != favorites } ?: false
    var query by remember { mutableStateOf("") }
    var selectedID by remember { mutableStateOf<String?>(null) }
    val selected = selectedID?.let { id -> favorites.firstOrNull { it.id == id } }
    val nudge = remember { Animatable(1f) }
    val limitRequester = remember { BringIntoViewRequester() }
    val large = LocalDensity.current.fontScale >= 1.5f
    val trimmed = query.trim()
    val scroll = rememberScrollState()
    var searchY by remember { mutableFloatStateOf(0f) }
    var searchFocused by remember { mutableStateOf(false) }

    // Searching: bring the field to the top of the sheet (on focus, and when the results replace the
    // suggestions) so the results show above the keyboard instead of under it.
    LaunchedEffect(searchFocused, trimmed.isEmpty()) {
        if (searchFocused) scroll.animateScrollTo(searchY.toInt(), DNMotion.smooth())
    }

    LaunchedEffect(Unit) {
        if (!HomeLaunch.appliedSheet) {
            HomeLaunch.appliedSheet = true
            HomeLaunch.fillFavoritesIfAsked(store)
            HomeLaunch.favoriteSelectIndex?.let { i -> selectedID = store.state.favorites.getOrNull(i)?.id }
            HomeLaunch.favoriteQuery?.let { query = it }
        }
        if (snapshot == null) snapshot = store.state.favorites
    }

    fun remove(item: FavoriteItem) {
        DNHaptics.light(view)
        if (selectedID == item.id) selectedID = null
        store.removeFavorite(item)
    }

    fun move(item: FavoriteItem, step: Int) {
        DNHaptics.selection(view)
        store.homeMoveFavorite(item.id, step)
    }

    fun toggle(service: CatalogueService) {
        when {
            store.isFavorite(service.id) -> { DNHaptics.light(view); store.toggleFavorite(service) }
            isFull -> {
                DNHaptics.warning(view)
                scope.launch { limitRequester.bringIntoView() }
                scope.launch { nudge.animateTo(1.03f, DNMotion.snappy()); nudge.animateTo(1f, DNMotion.snappy()) }
                @Suppress("DEPRECATION")
                view.announceForAccessibility("Your Home has $limit favorites. Remove one to add another.")
            }
            store.toggleFavorite(service) -> DNHaptics.success(view)
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val grid = HomeGrid(constraints.maxWidth, LocalDensity.current)
        Column(Modifier.fillMaxWidth()) {
            // Title row: × reverts this session's changes, ✓ keeps them.
            Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp)) {
                TonalIconButton(
                    Icons.Rounded.Close, if (hasChanges) "Cancel changes" else "Close",
                    onClick = {
                        snapshot?.let { if (hasChanges) store.homeRestoreFavorites(it) }
                        router.dismissSheet()
                    },
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                Text(
                    "Favorites", style = DNTextStyle.Headline.style, color = DNColor.textPrimary,
                    modifier = Modifier.align(Alignment.Center).semantics { heading() },
                )
                TonalIconButton(
                    Icons.Rounded.Check, "Done",
                    onClick = {
                        if (hasChanges) DNHaptics.success(view)
                        router.dismissSheet()
                    },
                    modifier = Modifier.align(Alignment.CenterEnd),
                    tint = DNColor.textOnAccent, container = DNColor.actionAccent,
                )
            }

            Column(
                Modifier.fillMaxWidth().verticalScroll(scroll)
                    .padding(start = DNSpace.insetPage, end = DNSpace.insetPage, top = 12.dp, bottom = 24.dp),
            ) {
                // On your Home
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        HomeOverline("On your Home", Modifier.weight(1f))
                        Text(
                            "${favorites.size} of $limit",
                            style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                            color = if (isFull) DNColor.statusWarningForeground else DNColor.textSecondary,
                            modifier = Modifier.semantics { contentDescription = "${favorites.size} of $limit favorites" },
                        )
                    }
                    if (large) {
                        favorites.forEachIndexed { i, item ->
                            HomeFavoriteListRow(item, canEarlier = i > 0, canLater = i < favorites.lastIndex, onRemove = { remove(item) }, onMove = { move(item, it) })
                        }
                    } else {
                        val cells: List<FavoriteItem?> = favorites + List(maxOf(0, limit - favorites.size)) { null }
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            cells.chunked(4).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(grid.tileGutter)) {
                                    row.forEach { item ->
                                        if (item == null) {
                                            Box(Modifier.size(grid.tile).homeDashedBorder(DNRadius.tile).clearAndSetSemantics { })
                                        } else {
                                            val index = favorites.indexOf(item)
                                            HomeEditFavoriteTile(
                                                item, grid,
                                                position = "${index + 1} of ${favorites.size}",
                                                isSelected = selectedID == item.id,
                                                onSelect = {
                                                    DNHaptics.selection(view)
                                                    selectedID = if (selectedID == item.id) null else item.id
                                                },
                                                onRemove = { remove(item) },
                                                onMove = { move(item, it) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Move the selected favorite (Android: select and move, instead of dragging).
                    AnimatedVisibility(
                        visible = selected != null && !large,
                        enter = expandVertically(DNMotion.smooth()) + fadeIn(DNMotion.base()),
                        exit = shrinkVertically(DNMotion.smooth()) + fadeOut(DNMotion.fast()),
                    ) {
                        val item = selected ?: favorites.firstOrNull()
                        val index = item?.let { favorites.indexOf(it) } ?: 0
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DNButton(
                                "Move earlier", onClick = { item?.let { move(it, -1) } }, kind = DNButtonKind.Secondary, size = DNButtonSize.Small,
                                enabled = index > 0, icon = Icons.Rounded.ChevronLeft, modifier = Modifier.weight(1f),
                            )
                            DNButton(
                                "Move later", onClick = { item?.let { move(it, 1) } }, kind = DNButtonKind.Secondary, size = DNButtonSize.Small,
                                enabled = index in 0 until favorites.lastIndex, icon = Icons.Rounded.ChevronRight, modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Text(
                        when {
                            large -> "Move a favorite up or down. This is the order on Home."
                            selected != null -> "${selected.title} is selected. Move it, or tap it again when it's in place."
                            else -> "Tap a favorite to move it. This is the order on Home."
                        },
                        style = DNTextStyle.Footnote.style, color = DNColor.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }

                // 8 of 8
                AnimatedVisibility(
                    visible = isFull,
                    enter = expandVertically(DNMotion.smooth()) + fadeIn(DNMotion.base()),
                    exit = shrinkVertically(DNMotion.smooth()) + fadeOut(DNMotion.fast()),
                ) {
                    Row(
                        Modifier.padding(top = 16.dp).fillMaxWidth().bringIntoViewRequester(limitRequester)
                            .graphicsLayer { scaleX = nudge.value; scaleY = nudge.value }
                            .clip(RoundedCornerShape(DNRadius.card)).background(DNColor.statusWarningSurface).padding(16.dp)
                            .semantics(mergeDescendants = true) { },
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Rounded.Error, contentDescription = null, tint = DNColor.statusWarningForeground, modifier = Modifier.size(22.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Your Home has $limit favorites", style = DNTextStyle.Headline.style, color = DNColor.statusWarningForeground)
                            Text("Remove one to add another. Eight keeps Home quick to scan.", style = DNTextStyle.Subheadline.style, color = DNColor.textPrimary)
                        }
                    }
                }

                // Search. Its y inside the column's 12 dp top padding is the scroll that puts it at the top of the sheet.
                Row(
                    Modifier.padding(top = 24.dp).onGloballyPositioned { searchY = it.positionInParent().y }
                        .fillMaxWidth().heightIn(min = 52.dp).clip(CircleShape).background(DNColor.surfacePrimary)
                        .padding(start = 16.dp, end = if (query.isEmpty()) 16.dp else 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = DNColor.iconSecondary, modifier = Modifier.size(22.dp))
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = DNTextStyle.Body.style.copy(color = DNColor.textPrimary),
                        cursorBrush = SolidColor(DNColor.actionAccent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                        modifier = Modifier.weight(1f).onFocusChanged { searchFocused = it.isFocused }
                            .semantics { contentDescription = "Search services to add" },
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) Text("Search services to add", style = DNTextStyle.Body.style, color = DNColor.fieldHint)
                                inner()
                            }
                        },
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Cancel, contentDescription = "Clear search", tint = DNColor.iconSecondary)
                        }
                    }
                }

                if (trimmed.isEmpty()) {
                    val suggested = homeSuggestedCandidates(store)
                    val things = homeThingCandidates(store)
                    val topic = homeMainTopic(favorites)
                    if (suggested.isNotEmpty()) HomeCandidateGroup("Suggested for you", null, suggested, isFull, ::toggle)
                    if (things.isNotEmpty()) HomeCandidateGroup("Your things", "hubs from your profile", things, isFull, ::toggle)
                    topic?.let { HomeCandidateGroup(it.name, null, homeTopicCandidates(it, favorites), isFull, ::toggle) }
                } else {
                    val found = homeSearchCandidates(trimmed)
                    if (found.isEmpty()) {
                        Text(
                            "No services match “$trimmed”. Try a different word, like “parking” or “visa”.",
                            style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary,
                            modifier = Modifier.padding(top = 24.dp, start = 4.dp, end = 4.dp),
                        )
                    } else {
                        HomeCandidateGroup("Results", null, found, isFull, ::toggle)
                    }
                }

                // Browse all
                Box(
                    Modifier.padding(top = 20.dp).fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) { router.dismissSheet(); router.select(AppTab.Services) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Browse all ${Catalogue.shared.services.size} services",
                        style = DNTextStyle.Body.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textLink, textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// MARK: Grid tile (with − badge)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeEditFavoriteTile(
    item: FavoriteItem,
    grid: HomeGrid,
    position: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val name = homeService(item)?.name ?: item.title
    Box {
        Column(
            Modifier.width(grid.tile).homePressScale(interaction)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onSelect)
                .clearAndSetSemantics {
                    contentDescription = name
                    stateDescription = position
                    selected = isSelected
                    role = Role.Button
                    onClick(label = if (isSelected) "Deselect" else "Select to move") { onSelect(); true }
                    customActions = listOf(
                        CustomAccessibilityAction("Remove from Favorites") { onRemove(); true },
                        CustomAccessibilityAction("Move earlier") { onMove(-1); true },
                        CustomAccessibilityAction("Move later") { onMove(1); true },
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.homeFocusRing(isSelected, DNRadius.tile)) {
                HomeFavoriteTileFace(item, grid.tile, interaction, badge = null)
            }
            Text(
                item.title, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textPrimary,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.requiredWidth(grid.tile + 8.dp),
            )
        }
        HomeRemoveBadge("Remove ${item.title}", onRemove, Modifier.align(Alignment.TopStart).offset(x = (-20).dp, y = (-20).dp), size = 26.dp)
    }
}

/** Large font sizes: a plain list with Remove and Move buttons instead of the grid. */
@Composable
private fun HomeFavoriteListRow(item: FavoriteItem, canEarlier: Boolean, canLater: Boolean, onRemove: () -> Unit, onMove: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.card)).background(DNColor.surfacePrimary).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HueTile(item.symbol, item.hue, size = 48.dp)
        Text(item.title, style = DNTextStyle.RowTitle.style, color = DNColor.textPrimary, modifier = Modifier.weight(1f).padding(start = 4.dp))
        IconButton(onClick = { onMove(-1) }, enabled = canEarlier) { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move ${item.title} earlier") }
        IconButton(onClick = { onMove(1) }, enabled = canLater) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move ${item.title} later") }
        IconButton(onClick = onRemove) { Icon(Icons.Rounded.RemoveCircle, contentDescription = "Remove ${item.title}", tint = DNColor.actionDestructive) }
    }
}

// MARK: Groups and rows

@Composable
private fun HomeOverline(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = DNTextStyle.Overline.style, color = DNColor.textGroupedHeader, modifier = modifier.semantics { heading() })
}

@Composable
private fun HomeCandidateGroup(title: String, trailing: String?, rows: List<HomeFavoriteCandidate>, isFull: Boolean, onToggle: (CatalogueService) -> Unit) {
    Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            HomeOverline(title, Modifier.weight(1f))
            if (trailing != null) Text(trailing, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        }
        DNListGroup {
            rows.forEachIndexed { i, row ->
                HomeCandidateRow(row, isFull, onToggle)
                if (i < rows.lastIndex) DNRowDivider(inset = 68.dp)
            }
        }
    }
}

@Composable
private fun HomeCandidateRow(row: HomeFavoriteCandidate, isFull: Boolean, onToggle: (CatalogueService) -> Unit) {
    val store = LocalStore.current
    val added = store.isFavorite(row.service.id)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClickLabel = if (added) "Remove from Favorites" else "Add to Favorites") { onToggle(row.service) }
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
            .semantics(mergeDescendants = true) {
                stateDescription = if (added) "On Home" else if (isFull) "Favorites are full" else "Not on Home"
                selected = added
            },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HueIcon(row.service.symbol, row.service.hue, size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.title, style = DNTextStyle.Headline.style, color = DNColor.listRowTitle)
            Text(row.subtitle, style = DNTextStyle.Footnote.style, color = DNColor.listRowSupporting)
        }
        Box(Modifier.size(48.dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(
                    when {
                        added -> DNColor.actionAccent
                        isFull -> DNColor.neutral100
                        else -> DNColor.actionAccentSubtle
                    },
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (added) Icons.Rounded.Check else Icons.Rounded.Add, contentDescription = null,
                    tint = when {
                        added -> DNColor.textOnAccent
                        isFull -> DNColor.neutral400
                        else -> DNColor.actionAccent
                    },
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// MARK: Candidates

/** Personal suggestions first (renewals coming up, a nol balance), then popular services. */
private fun homeSuggestedCandidates(store: DemoStore): List<HomeFavoriteCandidate> {
    if (store.isGuest) return homePopular(3)
    val out = mutableListOf<HomeFavoriteCandidate>()
    fun add(id: String, subtitle: String) {
        if (out.size >= 3 || out.any { it.service.id == id }) return
        val s = Catalogue.service(id) ?: return
        out += HomeFavoriteCandidate(s, s.name, subtitle)
    }
    val st = store.state
    for (item in st.needsYou) {
        val k = item.kind
        if (k is NeedsYouItem.Kind.Renewal) {
            val v = st.vehicles.firstOrNull { it.id == k.thingID }
            val whenText = item.due?.let { DemoDate.dueLabel(it).lowercase() } ?: "due soon"
            add(k.serviceID, v?.let { "Your ${it.model} is $whenText" } ?: "Yours is $whenText")
        }
    }
    st.nolBalance?.let { add("top-up-nol-card", "Balance ${Money.aed(it)}") }
    if (st.journeys.isNotEmpty() || st.vehicles.isNotEmpty()) add("check-prayer-times", "Popular in Dubai this week")
    for (item in st.needsYou) {
        val k = item.kind
        if (k is NeedsYouItem.Kind.Payment) {
            val sid = k.payable.serviceID ?: continue
            add(sid, item.due?.let { "Your bill is ${DemoDate.dueLabel(it).lowercase()}" } ?: "${k.payable.entity} · ${Money.aed(k.payable.amount)}")
        }
    }
    homePopular(6).forEach { add(it.service.id, it.subtitle) }
    return out
}

private fun homePopular(n: Int): List<HomeFavoriteCandidate> =
    Catalogue.shared.services
        .filter { it.type == ServiceType.Standalone && it.isLive }
        .sortedByDescending { it.uniques30d }
        .take(n)
        .map { HomeFavoriteCandidate(it, it.name, "${it.ownerLabel} · popular in Dubai") }

/** Hubs from the resident's profile. */
private fun homeThingCandidates(store: DemoStore): List<HomeFavoriteCandidate> {
    val s = store.state
    val out = mutableListOf<HomeFavoriteCandidate>()
    fun add(id: String, title: String, subtitle: String) {
        val service = Catalogue.service(id) ?: return
        out += HomeFavoriteCandidate(service, title, subtitle)
    }
    if (s.vehicles.isNotEmpty()) {
        add("your-vehicles", "Vehicles", s.vehicles.joinToString(", ") { it.shortName })
        add("top-up-salik-account", "Salik", "Balance, trips and top-ups")
    }
    if (s.documents.isNotEmpty()) add("your-documents", "Documents", s.documents.take(4).joinToString(", ") { it.title })
    if (s.family.isNotEmpty()) add("your-family-residency", "Family residency", s.family.joinToString(", ") { it.name.split(" ").firstOrNull() ?: it.name })
    s.home?.let { add("your-electricity-water-account", "DEWA account", "Electricity and water · ${it.dewaAccount}") }
    return out
}

/** The topic most of the resident's favorites come from (Driving and transport for Sara). */
private fun homeMainTopic(favorites: List<FavoriteItem>): CatalogueTopic? {
    val counts = favorites.mapNotNull { homeService(it)?.topic }.groupingBy { it }.eachCount()
    val id = counts.maxByOrNull { it.value }?.key ?: "driving-and-transport"
    return Catalogue.shared.topic(id)
}

private fun homeTopicCandidates(topic: CatalogueTopic, favorites: List<FavoriteItem>): List<HomeFavoriteCandidate> =
    Catalogue.shared.standalone(topic.id).filter { it.isLive }.take(4).map { s ->
        var subtitle = s.ownerLabel
        val fav = favorites.firstOrNull { homeTargetID(it) == s.id }
        if (fav != null && fav.title != s.name) subtitle += " · on Home as “${fav.title}”"
        HomeFavoriteCandidate(s, s.name, subtitle)
    }

private fun homeSearchCandidates(query: String): List<HomeFavoriteCandidate> =
    Catalogue.shared.search(query)
        .filter { it.type == ServiceType.Standalone || it.kind == "thing" }
        .take(20)
        .map { HomeFavoriteCandidate(it, it.name, it.ownerLabel) }
