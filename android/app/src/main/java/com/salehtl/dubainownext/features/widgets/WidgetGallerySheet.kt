package com.salehtl.dubainownext.features.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.app.LocalWidgetPreview
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.HomeWidget
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.data.WidgetKind
import com.salehtl.dubainownext.data.WidgetSize
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Add widgets" (Paper C1 · Widget gallery sheet ABL / K8W, Android chrome). Search, shelf chips
 * (M3 filter chips that jump to a shelf and follow the scroll) and shelves of live previews.
 * Tapping a preview (or its + badge) adds it to Home, the sheet closes and a snackbar offers Undo.
 *
 * Debug: launched with `widgetsDebug true` it shows every kind at every size instead (WidgetsDebugGrid).
 */
@Composable
fun WidgetGallerySheet() {
    if (LaunchOptions.flag("widgetsDebug")) WidgetsDebugGrid() else WidgetGallery()
}

@Composable
private fun WidgetGallery() {
    val store = LocalStore.current
    val router = LocalRouter.current
    val toasts = LocalToasts.current
    val view = LocalView.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // `widgetsGalleryQuery car` opens with a search typed in (screenshots).
    var query by rememberSaveable { mutableStateOf(LaunchOptions.extras["widgetsGalleryQuery"] ?: "") }
    val shelves = widgetGalleryShelves(store)
    val catalogue = remember(shelves) {
        val seen = mutableSetOf<String>()
        shelves.filter { it.id != "for-you" }.flatMap { it.entries }.filter { seen.add(it.id) }
    }
    val searching = query.isNotBlank()

    fun add(entry: WidgetGalleryEntry) {
        if (store.hasWidget(entry.kind)) return
        val added = store.addWidget(entry.kind, entry.size)
        DNHaptics.success(view)
        router.dismissSheet()
        toasts.show("${entry.title} added to Home", undo = { store.removeWidget(added) })
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val unit = maxOf(120.dp, (maxWidth - DNSpace.insetPage * 2 - DNSpace.widgetGutter) / 2)
        val wide = unit * 2 + DNSpace.widgetGutter
        val listState = rememberLazyListState()
        val chipsState = rememberLazyListState()

        // Flat list keys: the index of each shelf's header in the LazyColumn.
        val headerIndex = remember(shelves) {
            var i = 0
            shelves.associate { s ->
                val at = i
                i += 1 + WidgetGalleryEntry.rows(s.entries).size.coerceAtLeast(1) + (if (s.footnote != null) 1 else 0)
                s.id to at
            }
        }
        var jumpingTo by remember { mutableStateOf<String?>(null) }
        val followed by remember(headerIndex) {
            derivedStateOf {
                val first = listState.firstVisibleItemIndex + (if (listState.firstVisibleItemScrollOffset > 0) 1 else 0)
                headerIndex.entries.filter { it.value <= first }.maxByOrNull { it.value }?.key ?: shelves.firstOrNull()?.id
            }
        }
        val selected = jumpingTo ?: followed

        LaunchedEffect(selected) {
            val i = shelves.indexOfFirst { it.id == selected }
            if (i >= 0) chipsState.animateScrollToItem(maxOf(0, i - 1))
        }
        // `widgetsGalleryShelf journeys` opens scrolled to a shelf; `widgetsGalleryAutoAdd true` adds the first suggestion.
        LaunchedEffect(Unit) {
            LaunchOptions.extras["widgetsGalleryShelf"]?.let { id ->
                headerIndex[id]?.let { delay(600); listState.animateScrollToItem(it) }
            }
            if (LaunchOptions.flag("widgetsGalleryAutoAdd")) {
                shelves.firstOrNull()?.entries?.firstOrNull()?.let { delay(1500); add(it) }
            }
        }

        Column(Modifier.fillMaxSize()) {
            WidgetSheetHeader("Add widgets") { router.dismissSheet() }
            GallerySearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Search ${catalogue.size} widgets",
                modifier = Modifier.padding(horizontal = DNSpace.insetPage, vertical = 8.dp),
            )
            if (!searching) {
                LazyRow(
                    state = chipsState,
                    contentPadding = PaddingValues(horizontal = DNSpace.insetPage),
                    horizontalArrangement = Arrangement.spacedBy(DNSpace.chipGap),
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    items(shelves, key = { it.id }) { shelf ->
                        val isSelected = selected == shelf.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                DNHaptics.selection(view)
                                jumpingTo = shelf.id
                                scope.launch {
                                    headerIndex[shelf.id]?.let { listState.animateScrollToItem(it) }
                                    delay(150)
                                    jumpingTo = null
                                }
                            },
                            label = {
                                Text(
                                    shelf.chip,
                                    style = DNTextStyle.Subheadline.style.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                                )
                            },
                            shape = CircleShape,
                            modifier = Modifier.height(DNSpace.chipHeight + 4.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DNColor.chipSurface, labelColor = DNColor.chipText,
                                selectedContainerColor = DNColor.chipSurfaceSelected, selectedLabelColor = DNColor.chipTextSelected,
                            ),
                            border = BorderStroke(1.dp, if (isSelected) DNColor.chipSurfaceSelected else DNColor.borderSubtle),
                        )
                    }
                }
            }
            CompositionLocalProvider(LocalWidgetPreview provides true) {
                if (searching) {
                    GalleryResults(query, catalogue, unit, wide, store, onAdd = { focus.clearFocus(); add(it) })
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                    ) {
                        shelves.forEach { shelf ->
                            item(key = "h-${shelf.id}") { ShelfHeader(shelf) }
                            val rows = WidgetGalleryEntry.rows(shelf.entries)
                            val endGap = if (shelf.footnote != null) 14.dp else 36.dp
                            if (rows.isEmpty()) {
                                item(key = "e-${shelf.id}") { ShelfEmpty(shelf.emptyMessage ?: "", endGap) }
                            } else {
                                rows.forEachIndexed { r, row ->
                                    item(key = "r-${shelf.id}-$r") {
                                        GalleryRow(row, unit, wide, store, onAdd = ::add, bottom = if (r == rows.lastIndex) endGap else 22.dp)
                                    }
                                }
                            }
                            shelf.footnote?.let { note -> item(key = "f-${shelf.id}") { ShelfFootnote(note) } }
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Pieces

@Composable
private fun GallerySearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String, modifier: Modifier) {
    val focus = LocalFocusManager.current
    Row(
        modifier.fillMaxWidth().height(56.dp).clip(CircleShape).background(DNColor.surfacePrimary).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = DNColor.iconSecondary, modifier = Modifier.size(24.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text(placeholder, style = DNTextStyle.Body.style, color = DNColor.textSecondary, maxLines = 1)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = DNTextStyle.Body.style.copy(color = DNColor.fieldText),
                cursorBrush = SolidColor(DNColor.actionAccent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Rounded.Cancel, contentDescription = "Clear search", tint = DNColor.iconSecondary)
            }
        } else {
            Box(Modifier.size(12.dp))
        }
    }
}

@Composable
private fun ShelfHeader(shelf: WidgetGalleryShelf) {
    Column(
        Modifier.fillMaxWidth().padding(start = DNSpace.insetPage, end = DNSpace.insetPage, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(shelf.title, style = DNTextStyle.Section.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
        Text(shelf.subtitle, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
    }
}

@Composable
private fun ShelfEmpty(message: String, bottom: Dp) {
    Text(
        message, style = DNTextStyle.Callout.style, color = DNColor.textSecondary,
        modifier = Modifier.padding(start = DNSpace.insetPage, end = DNSpace.insetPage, bottom = bottom)
            .fillMaxWidth().clip(RoundedCornerShape(DNRadius.card)).background(DNColor.surfacePrimary).padding(16.dp),
    )
}

@Composable
private fun ShelfFootnote(note: String) {
    Row(
        Modifier.fillMaxWidth().padding(start = DNSpace.insetPage, end = DNSpace.insetPage, bottom = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = DNColor.iconSecondary, modifier = Modifier.padding(top = 1.dp).size(16.dp))
        Text(note, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
    }
}

@Composable
private fun GalleryRow(
    row: List<WidgetGalleryEntry>,
    unit: Dp,
    wide: Dp,
    store: DemoStore,
    onAdd: (WidgetGalleryEntry) -> Unit,
    bottom: Dp,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = DNSpace.insetPage, end = DNSpace.insetPage, bottom = bottom),
        horizontalArrangement = Arrangement.spacedBy(DNSpace.widgetGutter),
        verticalAlignment = Alignment.Top,
    ) {
        row.forEach { entry -> GalleryItem(entry, unit, wide, store.hasWidget(entry.kind), onAdd) }
    }
}

/** A live, non-interactive preview with its + badge, title and "Small · Parkin". */
@Composable
private fun GalleryItem(entry: WidgetGalleryEntry, unit: Dp, wide: Dp, onHome: Boolean, onAdd: (WidgetGalleryEntry) -> Unit) {
    val w = if (entry.size == WidgetSize.Small) unit else wide
    val h = if (entry.size == WidgetSize.Large) wide else unit
    val shape = RoundedCornerShape(DNRadius.widget)
    val sizeLabel = entry.size.widgetLabel.lowercase()
    Column(Modifier.width(w), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(w, h).clearAndSetSemantics {
                role = Role.Button
                contentDescription = if (onHome) "${entry.title}, $sizeLabel, on Home" else "Add ${entry.title}, $sizeLabel"
                stateDescription = entry.caption
                if (onHome) disabled() else onClick(label = "Add to Home") { onAdd(entry); true }
            },
        ) {
            WidgetView(HomeWidget("gallery-${entry.id}", entry.kind, entry.size), Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize().clip(shape)
                    .clickable(enabled = !onHome, indication = ripple(), interactionSource = null) { onAdd(entry) }
            )
            Box(
                Modifier.offset((-8).dp, (-8).dp).size(28.dp)
                    .border(2.dp, DNColor.surfaceSecondary, CircleShape).padding(1.dp)
                    .clip(CircleShape).background(if (onHome) DNColor.statusSuccessForeground else DNColor.actionAccent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (onHome) Icons.Rounded.Check else Icons.Rounded.Add, contentDescription = null, tint = DNColor.white, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.padding(horizontal = 4.dp).clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.title, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                if (onHome) "On Home" else entry.caption, style = DNTextStyle.Footnote.style,
                color = if (onHome) DNColor.statusSuccessForeground else DNColor.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GalleryResults(
    query: String,
    catalogue: List<WidgetGalleryEntry>,
    unit: Dp,
    wide: Dp,
    store: DemoStore,
    onAdd: (WidgetGalleryEntry) -> Unit,
) {
    val q = query.trim().lowercase()
    val found = catalogue.filter { e ->
        e.title.lowercase().contains(q) || e.kind.widgetOwner.lowercase().contains(q) ||
            e.kind.widgetSearchTerms.any { it.contains(q) || q.contains(it) }
    }
    if (found.isEmpty()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Rounded.SearchOff, contentDescription = null, tint = DNColor.iconSecondary, modifier = Modifier.size(40.dp))
            Text("No results for “${query.trim()}”", style = DNTextStyle.Title3.style, color = DNColor.textPrimary, textAlign = TextAlign.Center)
            Text("Check the spelling or try a new search.", style = DNTextStyle.Body.style, color = DNColor.textSecondary, textAlign = TextAlign.Center)
        }
        return
    }
    val rows = WidgetGalleryEntry.rows(found)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)) {
        item(key = "count") {
            Text(
                if (found.size == 1) "1 widget" else "${found.size} widgets", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary,
                modifier = Modifier.padding(start = DNSpace.insetPage, end = DNSpace.insetPage, bottom = 16.dp),
            )
        }
        rows.forEachIndexed { r, row ->
            item(key = "s-$r-${row.first().id}") { GalleryRow(row, unit, wide, store, onAdd, bottom = 22.dp) }
        }
    }
}

// MARK: - Shelves and entries

internal data class WidgetGalleryEntry(
    val kind: WidgetKind,
    val size: WidgetSize,
    val title: String = kind.title,
    val caption: String = "${size.widgetLabel} · ${kind.widgetOwner}",
) {
    val id: String get() = "${kind.widgetKey}-${size.name.lowercase()}"

    companion object {
        /** Smalls pair left to right; medium and large take a row each. */
        fun rows(entries: List<WidgetGalleryEntry>): List<List<WidgetGalleryEntry>> {
            val rows = mutableListOf<List<WidgetGalleryEntry>>()
            var pending: WidgetGalleryEntry? = null
            for (e in entries) {
                if (e.size == WidgetSize.Small) {
                    val p = pending
                    if (p != null) { rows += listOf(p, e); pending = null } else pending = e
                } else {
                    pending?.let { rows += listOf(it); pending = null }
                    rows += listOf(e)
                }
            }
            pending?.let { rows += listOf(it) }
            return rows
        }
    }
}

internal data class WidgetGalleryShelf(
    val id: String,
    val chip: String,
    val title: String,
    val subtitle: String,
    val entries: List<WidgetGalleryEntry>,
    val footnote: String? = null,
    val emptyMessage: String? = null,
)

internal fun widgetGalleryShelves(store: DemoStore): List<WidgetGalleryShelf> {
    val s = store.state
    val shelves = mutableListOf<WidgetGalleryShelf>()

    // For you: what's live on the profile and not on Home yet.
    val picks = mutableListOf<WidgetKind>()
    s.widgetSuggestion?.let { picks += it }
    if (!store.isGuest && store.payables.isNotEmpty()) picks += WidgetKind.BillsAndFines
    s.requests.firstOrNull { it.status is RequestItem.Status.YourTurn }?.let { picks += WidgetKind.VisaTracker(it.id) }
    if (s.nolBalance != null) picks += WidgetKind.Nol
    if (s.parking != null) picks += WidgetKind.Parking
    if (store.widgetSalikLinked) picks += WidgetKind.Salik
    picks += listOf(WidgetKind.Prayer, WidgetKind.Weather)
    val forYou = picks.distinct().filter { !store.hasWidget(it) }.take(4)
        .map { WidgetGalleryEntry(it, it.widgetSizes.firstOrNull() ?: WidgetSize.Small) }
    if (forYou.isNotEmpty()) {
        shelves += WidgetGalleryShelf(
            id = "for-you", chip = "For you", title = "Suggested for you",
            subtitle = if (store.isGuest) "Useful without an account" else "Based on what's live on your profile",
            entries = forYou,
        )
    }

    // All at once: bundles with something live for you.
    val bundles = mutableListOf<WidgetGalleryEntry>()
    if (!store.isGuest && (store.payables.isNotEmpty() || s.history.isNotEmpty())) {
        bundles += listOf(WidgetGalleryEntry(WidgetKind.BillsAndFines, WidgetSize.Small), WidgetGalleryEntry(WidgetKind.BillsAndFines, WidgetSize.Medium))
    }
    if (s.familyVisas != null) {
        bundles += listOf(WidgetGalleryEntry(WidgetKind.FamilyVisas, WidgetSize.Small), WidgetGalleryEntry(WidgetKind.FamilyVisas, WidgetSize.Medium))
    }
    shelves += WidgetGalleryShelf(
        id = "all-at-once", chip = "All at once", title = "All at once",
        subtitle = "Bundles with something live for you",
        entries = bundles,
        footnote = "Certificates and letters isn't offered: it has nothing live to show until you ask for a certificate.",
        emptyMessage = if (store.isGuest) "Sign in to see bundles with something live for you, like Pay all."
        else "None of your bundles has anything live right now.",
    )

    // Your journeys: only the ones you've started.
    val journeys = s.journeys.flatMap { j ->
        val kind = WidgetKind.Journey(j.journeyID)
        listOf(
            WidgetGalleryEntry(kind, WidgetSize.Medium, caption = "Medium · the journey you started on ${WidgetFormat.shortDate(j.startedOn)}"),
            WidgetGalleryEntry(kind, WidgetSize.Large, caption = "Large · adds every stage"),
        )
    }
    shelves += WidgetGalleryShelf(
        id = "journeys", chip = "Your journeys", title = "Your journeys",
        subtitle = "Only the ones you've started",
        entries = journeys,
        emptyMessage = "Start a journey from Services, like Move home, and it shows up here.",
    )

    shelves += WidgetGalleryShelf(
        id = "getting-around", chip = "Getting around", title = "Getting around",
        subtitle = "Parking, tolls and the metro",
        entries = listOf(
            WidgetGalleryEntry(WidgetKind.Parking, WidgetSize.Small), WidgetGalleryEntry(WidgetKind.Salik, WidgetSize.Small),
            WidgetGalleryEntry(WidgetKind.Parking, WidgetSize.Medium), WidgetGalleryEntry(WidgetKind.Salik, WidgetSize.Medium),
            WidgetGalleryEntry(WidgetKind.Nol, WidgetSize.Small), WidgetGalleryEntry(WidgetKind.Metro, WidgetSize.Medium),
        ),
    )

    shelves += WidgetGalleryShelf(
        id = "your-home", chip = "Your home", title = "Your home",
        subtitle = "From your DEWA account",
        entries = listOf(WidgetGalleryEntry(WidgetKind.Energy, WidgetSize.Medium)),
    )

    val tracked = s.requests.filter { it.status is RequestItem.Status.YourTurn || it.status is RequestItem.Status.InReview }
    if (tracked.isNotEmpty()) {
        shelves += WidgetGalleryShelf(
            id = "requests", chip = "Requests", title = "Your requests",
            subtitle = "Follow a request without opening it",
            entries = tracked.map {
                WidgetGalleryEntry(
                    WidgetKind.VisaTracker(it.id), WidgetSize.Small,
                    title = WidgetFormat.capitalizedFirst(it.title.replace("Renew your ", "")),
                    caption = "Small · ${it.entity}",
                )
            },
        )
    }

    shelves += WidgetGalleryShelf(
        id = "faith", chip = "Faith", title = "Faith and family",
        subtitle = "Where you are, no account needed",
        entries = listOf(WidgetGalleryEntry(WidgetKind.Prayer, WidgetSize.Small), WidgetGalleryEntry(WidgetKind.Prayer, WidgetSize.Large)),
    )

    shelves += WidgetGalleryShelf(
        id = "city-life", chip = "City life", title = "City life",
        subtitle = "Weather and beach flags",
        entries = listOf(WidgetGalleryEntry(WidgetKind.Weather, WidgetSize.Small)),
    )
    return shelves
}
