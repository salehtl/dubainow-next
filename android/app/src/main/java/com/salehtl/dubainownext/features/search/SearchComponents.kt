package com.salehtl.dubainownext.features.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.Plate
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.data.WidgetKind
import com.salehtl.dubainownext.data.WidgetSize
import com.salehtl.dubainownext.data.hue
import com.salehtl.dubainownext.data.icon
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNIcons
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.Avatar
import com.salehtl.dubainownext.design.components.BundleDiscs
import com.salehtl.dubainownext.design.components.ChipTone
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNButtonKind
import com.salehtl.dubainownext.design.components.DNButtonSize
import com.salehtl.dubainownext.design.components.DNLogo
import com.salehtl.dubainownext.design.components.HueIcon
import com.salehtl.dubainownext.design.components.NumberPlate
import com.salehtl.dubainownext.design.components.ServiceTypeBadge
import com.salehtl.dubainownext.design.components.dnCard

// Building blocks for the Search screens (Paper p-W-0: Ready, Results as you type, Direct answer,
// Did you mean). Shared tier: rows and cards look the same as iOS on solid white surfaces;
// Android adds ripple and 48 dp targets.

// MARK: - Recents (session-local)

/** Recent searches for this session, per resident. Not saved to disk. */
internal object SearchRecents {
    private const val LIMIT = 6

    /** Sara starts with the searches drawn on the Paper "Ready" screen. */
    private val byResident = mutableStateMapOf("sara" to listOf("Salik top up", "Mulkiya", "good conduct", "Ejari"))

    fun items(resident: String): List<String> = byResident[resident] ?: emptyList()

    fun add(query: String, resident: String) {
        val q = query.trim()
        if (q.length < 2) return
        val list = (byResident[resident] ?: emptyList()).filterNot { SearchText.fold(it) == SearchText.fold(q) }
        byResident[resident] = (listOf(q) + list).take(LIMIT)
    }

    fun clear(resident: String) { byResident[resident] = emptyList() }
}

// MARK: - Helpers

internal fun searchToneColor(tone: ChipTone): Color = when (tone) {
    ChipTone.Warning -> DNColor.statusWarningForeground
    ChipTone.Error -> DNColor.statusErrorForeground
    ChipTone.Success -> DNColor.statusSuccessForeground
    ChipTone.Info -> DNColor.statusInfoForeground
    ChipTone.Neutral -> DNColor.textSecondary
}

/** Icons for the SF Symbol names only Search uses; everything else goes through DNIcons. */
internal fun searchIcon(symbol: String): ImageVector = when (symbol) {
    "list.bullet" -> Icons.AutoMirrored.Rounded.FormatListBulleted
    "car" -> Icons.Rounded.DirectionsCar
    "p.square" -> Icons.Rounded.LocalParking
    "moon.stars" -> Icons.Rounded.NightsStay
    "person.crop.circle" -> Icons.Rounded.AccountCircle
    "lock.fill" -> Icons.Rounded.Lock
    "calendar" -> Icons.Rounded.CalendarToday
    "info.circle" -> Icons.Outlined.Info
    "doc.badge.clock" -> Icons.Rounded.PendingActions
    "sun.max.fill" -> Icons.Rounded.WbSunny
    else -> DNIcons.forSymbol(symbol)
}

/** True when the text is set large (≥ 150 %): stacked layouts instead of side by side. */
@Composable
internal fun searchLargeText(): Boolean = LocalDensity.current.fontScale >= 1.5f

/** Bolds every place a query word starts a word in [text] ("Top up your **Sal**ik account"). */
internal fun searchHighlight(text: String, words: List<String>): AnnotatedString {
    val folded = StringBuilder()
    val origin = ArrayList<Int>()
    text.forEachIndexed { i, c -> for (f in SearchText.fold(c.toString())) { folded.append(f); origin.add(i) } }
    val lower = folded.toString()
    val bold = BooleanArray(text.length)
    for (w in words) {
        if (w.length < 2) continue
        var from = 0
        while (true) {
            val at = lower.indexOf(w, from)
            if (at < 0) break
            if (at == 0 || !lower[at - 1].isLetter()) for (k in at until at + w.length) bold[origin[k]] = true
            from = at + w.length
        }
    }
    if (bold.none { it }) return AnnotatedString(text)
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val b = bold[i]
            var j = i
            while (j < text.length && bold[j] == b) j++
            if (b) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i, j)) } else append(text.substring(i, j))
            i = j
        }
    }
}

// MARK: - Section and group

/** "TOP RESULT", "SERVICES · See all 6": overline header, 10 dp above its group. */
@Composable
internal fun SearchSectionHeader(title: String, actionTitle: String? = null, actionLabel: String? = null, action: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 20.dp).padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title.uppercase(), style = DNTextStyle.Overline.style, color = DNColor.textGroupedHeader,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (actionTitle != null && action != null) {
            // A 48 dp target that overhangs the 20 dp header (18 dp into the space above, 10 dp down to the group),
            // so sections keep the same rhythm with or without an action. The label sits 4 dp low to stay level with the title.
            Box(
                Modifier.searchOverhang(top = 18.dp, bottom = 10.dp).heightIn(min = 48.dp).clip(CircleShape)
                    .clickable(role = Role.Button, onClick = action)
                    .then(if (actionLabel != null) Modifier.semantics { contentDescription = actionLabel } else Modifier)
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(actionTitle, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textLink)
            }
        }
    }
}

/** Reports less height than the content has ([top] and [bottom] hang outside), letting a touch target overhang its row. */
internal fun Modifier.searchOverhang(top: Dp, bottom: Dp = top): Modifier = layout { measurable, constraints ->
    val above = top.roundToPx()
    val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    val height = (placeable.height - above - bottom.roundToPx()).coerceAtLeast(0)
    layout(placeable.width, height) { placeable.place(0, -above) }
}

@Composable
internal fun SearchSection(
    title: String,
    actionTitle: String? = null,
    actionLabel: String? = null,
    action: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SearchSectionHeader(title, actionTitle, actionLabel, action)
        content()
    }
}

/** A white group (radius 24) with hairline separators inset past the leading icon. */
@Composable
internal fun <T> SearchGroup(items: List<T>, separatorInset: Dp = 64.dp, row: @Composable (T) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.listGroup)).background(DNColor.surfacePrimary)) {
        items.forEachIndexed { i, item ->
            row(item)
            if (i < items.lastIndex) {
                HorizontalDivider(Modifier.padding(start = separatorInset), thickness = 0.5.dp, color = DNColor.listRowSeparator)
            }
        }
    }
}

@Composable
private fun SearchChevron() {
    Icon(DNIcons.forSymbol("chevron.forward"), contentDescription = null, tint = DNColor.listRowChevron, modifier = Modifier.size(20.dp))
}

// MARK: - Ready state rows

/** A recent search: white capsule with a history glyph. */
@Composable
internal fun SearchRecentChip(text: String, onClick: () -> Unit) {
    Row(
        // 40 dp pill inside a 48 dp target (like an M3 chip).
        Modifier.minimumInteractiveComponentSize().heightIn(min = 40.dp).clip(CircleShape).background(DNColor.surfacePrimary)
            .clickable(role = Role.Button, onClickLabel = "Search again", onClick = onClick)
            .padding(start = 12.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.History, contentDescription = null, tint = DNColor.textSecondary, modifier = Modifier.size(16.dp))
        Text(text, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A suggested question ("How much do I owe?") with an accent glyph. */
@Composable
internal fun SearchSuggestionRow(symbol: String, text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(searchIcon(symbol), contentDescription = null, tint = DNColor.actionAccent, modifier = Modifier.size(22.dp))
        Text(text, style = DNTextStyle.Callout.style, color = DNColor.textPrimary, modifier = Modifier.weight(1f))
    }
}

/** "Ask DubaiNow": the assistant's face (the logo) and what it can do. */
@Composable
internal fun SearchAskRow(query: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClickLabel = "Open the DubaiNow assistant", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(DNColor.surfaceSecondary).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            DNLogo(height = 18.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Ask DubaiNow", style = DNTextStyle.RowTitle.style, color = DNColor.textPrimary)
            Text(
                query?.let { "“$it”" } ?: "Say what you need and it does the rest",
                style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        SearchChevron()
    }
}

/** "Popular in Dubai this week": rank · name · owner. */
@Composable
internal fun SearchPopularRow(rank: Int, service: CatalogueService, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$rank. ${service.name}, ${service.ownerLabel}" }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "$rank", style = DNTextStyle.Headline.style.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
            color = DNColor.textSecondary, modifier = Modifier.widthIn(min = 22.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(service.name, style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (service.type != ServiceType.Standalone) ServiceTypeBadge(service.type)
                Text(service.ownerLabel, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        SearchChevron()
    }
}

// MARK: - Result rows

@Composable
private fun SearchServiceDisc(service: CatalogueService, size: Dp) {
    when (service.type) {
        ServiceType.Standalone -> HueIcon(service.icon, service.hue, size = size)
        ServiceType.Bundle -> BundleDiscs(service, size = size)
        ServiceType.Journey -> HueIcon("point.topleft.down.to.point.bottomright.curvepath", service.hue, size = size)
    }
}

/**
 * A catalogue record in results (Paper "Results as you type"): 36 dp disc · name with the match in
 * bold · owner and the resident's context. Bundles and journeys show their badge.
 */
@Composable
internal fun SearchServiceRow(
    service: CatalogueService,
    words: List<String>,
    showsIcon: Boolean,
    context: String?,
    contextTone: ChipTone,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showsIcon) SearchServiceDisc(service, 36.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(searchHighlight(service.name, words), style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.Normal), color = DNColor.textPrimary)
            if (service.type != ServiceType.Standalone) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ServiceTypeBadge(service.type)
                    Text(service.ownerLabel, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(searchServiceMeta(service, context, contextTone), style = DNTextStyle.Footnote.style, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        SearchChevron()
    }
}

private fun searchServiceMeta(s: CatalogueService, context: String?, tone: ChipTone): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = DNColor.textSecondary)) { append(s.ownerLabel) }
    s.statusLabel?.let {
        withStyle(SpanStyle(color = DNColor.textSecondary)) { append(" · ") }
        withStyle(SpanStyle(color = if (s.status == "maintenance") DNColor.statusWarningForeground else DNColor.statusInfoForeground)) { append(it) }
    }
    context?.let {
        withStyle(SpanStyle(color = DNColor.textSecondary)) { append(" · ") }
        withStyle(SpanStyle(color = searchToneColor(tone))) { append(it) }
    }
}

@Composable
internal fun SearchThingLeading(leading: SearchLeading) {
    when (leading) {
        is SearchLeading.Icon -> HueIcon(leading.symbol, leading.hue, size = 36.dp)
        // The row already says the plate in its subtitle (iOS hides the plate here too).
        is SearchLeading.PlateMark -> Box(Modifier.clearAndSetSemantics {}) { SearchPlate(leading.plate) }
        is SearchLeading.Initials -> Avatar(leading.initials, leading.hue, size = 36.dp)
    }
}

/** One of the resident's things: plate or hue disc · title (match in bold) · status. */
@Composable
internal fun SearchThingRow(thing: SearchThing, words: List<String>, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SearchThingLeading(thing.leading)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(searchHighlight(thing.title, words), style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.Normal), color = DNColor.textPrimary)
            Text(
                thing.subtitle,
                style = DNTextStyle.Footnote.style.copy(fontWeight = if (thing.tone == ChipTone.Neutral) FontWeight.Normal else FontWeight.SemiBold),
                color = searchToneColor(thing.tone),
            )
        }
        SearchChevron()
    }
}

/** A widget the resident can add to Home from search. */
@Composable
internal fun SearchWidgetRow(kind: WidgetKind, isOnHome: Boolean, onClick: () -> Unit, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HueIcon(searchIcon(SearchWidgets.symbol(kind)), SearchWidgets.hue(kind), size = 36.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("${kind.title} widget", style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.Normal), color = DNColor.textPrimary)
            Text(if (isOnHome) "On your Home" else "Add it to Home", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        }
        if (isOnHome) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = DNColor.statusSuccessForeground, modifier = Modifier.size(20.dp))
        } else {
            DNButton(
                "Add", onClick = onAdd, kind = DNButtonKind.Secondary, size = DNButtonSize.Small, fullWidth = false,
                modifier = Modifier.semantics { contentDescription = "Add the ${kind.title} widget to Home" },
            )
        }
    }
}

internal object SearchWidgets {
    fun symbol(kind: WidgetKind): String = when (kind) {
        WidgetKind.Parking -> "p.square.fill"
        WidgetKind.Salik -> "road.lanes"
        WidgetKind.Prayer -> "moon.stars.fill"
        WidgetKind.Weather -> "sun.max.fill"
        WidgetKind.Metro, WidgetKind.Nol -> "tram.fill"
        WidgetKind.Energy -> "bolt.fill"
        else -> "square.grid.2x2.fill"
    }

    fun hue(kind: WidgetKind): Hue = when (kind) {
        WidgetKind.Parking, WidgetKind.Metro, WidgetKind.Nol -> Hue.Sky
        WidgetKind.Salik, WidgetKind.Weather, WidgetKind.Energy -> Hue.Sand
        WidgetKind.Prayer -> Hue.Lilac
        else -> Hue.Mint
    }

    fun size(kind: WidgetKind): WidgetSize = when (kind) {
        WidgetKind.Metro, WidgetKind.Energy -> WidgetSize.Medium
        else -> WidgetSize.Small
    }
}

// MARK: - Top result cards

/**
 * Top result for one of the resident's accounts (Paper "Results as you type"):
 * dark disc · "Salik" · "Balance AED 18 · low" · one action.
 */
@Composable
internal fun SearchTopThingCard(card: SearchCard, words: List<String>, onOpen: () -> Unit, onAct: () -> Unit) {
    val stacked = searchLargeText()
    val summary: @Composable (Modifier) -> Unit = { mod ->
        Row(
            mod.clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(DNColor.surfaceWidgetInverse), contentAlignment = Alignment.Center) {
                Icon(DNIcons.forSymbol(card.symbol), contentDescription = null, tint = DNColor.widgetOnInverse, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(searchHighlight(card.title, words), style = DNTextStyle.Headline.style.copy(fontWeight = FontWeight.Bold), color = DNColor.textPrimary)
                Text(searchCardValueLine(card), style = DNTextStyle.Subheadline.style)
            }
        }
    }
    if (stacked) {
        Column(Modifier.fillMaxWidth().dnCard(DNRadius.brief).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            summary(Modifier.fillMaxWidth())
            DNButton(card.button, onClick = onAct, kind = DNButtonKind.Secondary, size = DNButtonSize.Small, fullWidth = false)
        }
    } else {
        Row(
            Modifier.fillMaxWidth().dnCard(DNRadius.brief).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            summary(Modifier.weight(1f))
            DNButton(card.button, onClick = onAct, kind = DNButtonKind.Secondary, size = DNButtonSize.Small, fullWidth = false)
        }
    }
}

private fun searchCardValueLine(card: SearchCard): AnnotatedString = buildAnnotatedString {
    if (card.label.isNotEmpty()) withStyle(SpanStyle(color = DNColor.textSecondary)) { append(card.label + " ") }
    withStyle(
        SpanStyle(
            fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum",
            color = if (card.label.isEmpty()) searchToneColor(card.noteTone) else DNColor.textPrimary,
        ),
    ) { append(card.value) }
    card.note?.let { withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = searchToneColor(card.noteTone))) { append(" · $it") } }
}

/** A small rounded label ("Also called Mulkiya", "RTA"). */
@Composable
internal fun SearchChip(text: String, fill: Color, ink: Color) {
    Box(Modifier.heightIn(min = 24.dp).clip(CircleShape).background(fill).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        Text(text, style = DNTextStyle.Caption1.style.copy(fontWeight = FontWeight.SemiBold), color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Top result for a service (Paper "Did you mean"): disc · name · "Also called…" and owner chips ·
 * the resident's own context · Start now.
 */
@Composable
internal fun SearchTopServiceCard(match: SearchMatch, strip: SearchContextStrip?, onStart: () -> Unit) {
    val s = match.service
    Column(Modifier.fillMaxWidth().dnCard(DNRadius.brief).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SearchServiceDisc(s, 48.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.name, style = DNTextStyle.Headline.style.copy(fontWeight = FontWeight.Bold), color = DNColor.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    match.alsoCalled?.let { SearchChip("Also called $it", DNColor.hueLilacStart, DNColor.hueLilacInk) }
                    if (s.type != ServiceType.Standalone) ServiceTypeBadge(s.type)
                    SearchChip(s.ownerLabel, DNColor.surfaceSecondary, DNColor.textSecondary)
                    s.statusLabel?.let { SearchChip(it, DNColor.statusInfoSurface, DNColor.statusInfoForeground) }
                }
            }
        }
        if (strip != null) {
            val mark: @Composable () -> Unit = {
                if (strip.plate != null) {
                    SearchPlate(strip.plate)
                } else if (strip.symbol != null) {
                    Icon(searchIcon(strip.symbol), contentDescription = null, tint = DNColor.statusWarningForeground, modifier = Modifier.size(18.dp))
                }
            }
            val stripModifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DNColor.statusWarningSurface).padding(12.dp)
                .semantics(mergeDescendants = true) {}
            val stripStyle = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold)
            if (searchLargeText()) {
                Column(stripModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mark()
                    Text(strip.text, style = stripStyle, color = DNColor.statusWarningForeground)
                }
            } else {
                Row(stripModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    mark()
                    Text(strip.text, style = stripStyle, color = DNColor.statusWarningForeground, modifier = Modifier.weight(1f))
                }
            }
        }
        DNButton(strip?.buttonTitle ?: searchStartTitle(s), onClick = onStart)
    }
}

internal fun searchStartTitle(s: CatalogueService): String {
    if (!s.isLive) return "Find out more"
    return when (s.type) {
        ServiceType.Standalone -> if (s.kind == "thing") "Open" else "Start now"
        ServiceType.Bundle -> "See what it covers"
        ServiceType.Journey -> "See the stages"
    }
}

// MARK: - Correction, footnote, browse

/** "Showing results for **mulkiya** · Search instead for mulkia". */
@Composable
internal fun SearchCorrectionHeader(corrected: String, original: String, onSearchOriginal: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 4.dp)) {
        Text(
            buildAnnotatedString {
                append("Showing results for ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(corrected) }
            },
            style = DNTextStyle.Body.style, color = DNColor.textPrimary,
        )
        Row(
            // 48 dp target; its top 10 dp overhang the line above so the two lines sit 4 dp apart, as on iOS.
            Modifier.searchOverhang(top = 10.dp, bottom = 4.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onSearchOriginal)
                .semantics(mergeDescendants = true) { contentDescription = "Search instead for $original" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Search instead for ", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
            Text(original, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textLink)
        }
    }
}

/** "ⓘ Answers are worked out from your linked … accounts. Only you can see them." */
@Composable
internal fun SearchFootnote(text: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(searchIcon("info.circle"), contentDescription = null, tint = DNColor.textSecondary, modifier = Modifier.padding(top = 1.dp).size(16.dp))
        Text(text, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, modifier = Modifier.weight(1f))
    }
}

/** White capsule for "Browse by topic" and "A to Z" (48 dp). */
@Composable
internal fun SearchCapsuleButton(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp).clip(CircleShape).background(DNColor.surfacePrimary)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textLink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The number plate is a mark, like a logo: its lettering stays at 100 % so it never clips when the
 * resident sets large text (the shared NumberPlate has a fixed height; see NOTES.md).
 */
@Composable
internal fun SearchPlate(plate: Plate) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, 1f)) {
        NumberPlate(plate.code, plate.number, emirate = plate.emirate, small = true)
    }
}
