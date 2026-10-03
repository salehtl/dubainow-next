package com.salehtl.dubainownext.features.services

import android.content.Intent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.app.MainHeader
import com.salehtl.dubainownext.app.PushedHeader
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.CatalogueTopic
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.hue
import com.salehtl.dubainownext.data.icon
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNIcons
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.CatalogueRow
import com.salehtl.dubainownext.design.components.ChipTone
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNButtonKind
import com.salehtl.dubainownext.design.components.DNButtonSize
import com.salehtl.dubainownext.design.components.DNTextButton
import com.salehtl.dubainownext.design.components.HueIcon
import com.salehtl.dubainownext.design.components.NumberPlate
import com.salehtl.dubainownext.design.components.RowTrailing
import com.salehtl.dubainownext.design.components.SectionHeader
import com.salehtl.dubainownext.design.components.StatusChip
import com.salehtl.dubainownext.design.components.TonalIconButton
import com.salehtl.dubainownext.design.components.ToastCenter
import com.salehtl.dubainownext.design.components.dnCard
import com.salehtl.dubainownext.design.components.dnElevation

// MARK: - Page frame

/** Bottom padding that clears the floating tab bar and the gesture bar. */
@Composable
internal fun servicesBottomPadding(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp

/**
 * The font scale Services lays out for. The debug extra `servicesFontScale 2` previews a larger
 * system font size inside Services only (the shared emulators' settings must not change).
 */
@Composable
internal fun servicesFontScale(): Float =
    LaunchOptions.extras["servicesFontScale"]?.toFloatOrNull()?.coerceIn(0.85f, 3f) ?: LocalDensity.current.fontScale

/** True at the largest font sizes, where tiles go to one column and cards widen (iOS: accessibility sizes). */
@Composable
internal fun servicesLargeText(): Boolean = servicesFontScale() >= 1.5f

/**
 * A Services screen: optional hue wash behind the top (Paper: hue start → clear over 360),
 * the shared header (main or pushed), and a LazyColumn that scrolls under the floating bar.
 */
@Composable
internal fun ServicesPage(
    modifier: Modifier = Modifier,
    base: Color = DNColor.surfaceSecondary,
    wash: Color? = null,
    washHeight: Dp = 360.dp,
    isRoot: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    state: LazyListState = rememberLazyListState(),
    above: (@Composable ColumnScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val density = LocalDensity.current
    val scale = servicesFontScale()
    if (scale != density.fontScale) {
        CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
            ServicesPageFrame(modifier, base, wash, washHeight, isRoot, actions, state, above, content)
        }
    } else {
        ServicesPageFrame(modifier, base, wash, washHeight, isRoot, actions, state, above, content)
    }
}

@Composable
private fun ServicesPageFrame(
    modifier: Modifier,
    base: Color,
    wash: Color?,
    washHeight: Dp,
    isRoot: Boolean,
    actions: @Composable RowScope.() -> Unit,
    state: LazyListState,
    above: (@Composable ColumnScope.() -> Unit)?,
    content: LazyListScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().background(base)) {
        if (wash != null) {
            Box(Modifier.fillMaxWidth().height(washHeight).background(Brush.verticalGradient(listOf(wash, wash.copy(alpha = 0f)))))
        }
        Column(Modifier.fillMaxSize()) {
            if (isRoot) MainHeader() else PushedHeader(actions = actions)
            above?.invoke(this)
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                state = state,
                contentPadding = PaddingValues(top = 4.dp, bottom = servicesBottomPadding()),
            ) { content() }
        }
    }
}

/** Page padding for lazy items. */
internal val ServicesInset = Modifier.padding(horizontal = DNSpace.insetPage)

/**
 * `servicesScroll <key>` (debug extra) scrolls a Services screen to an item on launch, since
 * the emulator can't be scrolled from the command line. Runs once per process.
 */
@Composable
internal fun ServicesLaunchScroll(state: LazyListState, indexOf: (String) -> Int?) {
    LaunchedEffect(Unit) {
        val target = LaunchOptions.extras["servicesScroll"] ?: return@LaunchedEffect
        if (ServicesDebug.scrolled) return@LaunchedEffect
        val index = indexOf(target) ?: return@LaunchedEffect
        ServicesDebug.scrolled = true
        kotlinx.coroutines.delay(500)
        state.scrollToItem(index)
    }
}

/** True when the system "Remove animations" setting is on (jumps instead of animated scrolls). */
internal fun servicesReduceMotion(context: android.content.Context): Boolean =
    android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** One-shot debug state for launch extras (each fires once per process). */
internal object ServicesDebug {
    var scrolled = false
    var menuOpened = false
    var starred = false
    var dialogOpened = false
    fun flag(name: String) = LaunchOptions.extras[name] == "true"
}

// MARK: - Discs and tiles

/** A hue disc with a white ring and the soft elevation (page headers: 56–64 dp). */
@Composable
internal fun ServicesHeaderDisc(icon: ImageVector, hue: Hue, size: Dp = 64.dp) {
    Box(Modifier.size(size + 6.dp).dnElevation(size).clip(CircleShape).background(DNColor.surfacePrimary).padding(3.dp)) {
        HueIcon(icon, hue, size = size)
    }
}

/** Browse by topic tile: white, r24, hue disc, name, real service count (Paper DBS-0). */
@Composable
internal fun ServicesTopicTile(topic: CatalogueTopic, modifier: Modifier = Modifier, compact: Boolean = true, onClick: () -> Unit) {
    Column(
        modifier.fillMaxWidth().heightIn(min = if (compact) 148.dp else 0.dp)
            .clip(RoundedCornerShape(DNRadius.brief)).background(DNColor.surfacePrimary)
            .clickable(onClickLabel = "Open the topic", onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HueIcon(topic.icon, topic.hue, size = 44.dp)
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(topic.name, style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
            Text(ServicesText.count(topic.services, "service"), style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        }
    }
}

/** "For you" card: hue disc + status chip, title, detail (Paper DBS-0, 248 wide). */
@Composable
internal fun ServicesForYouCard(item: ServicesForYouItem, width: Dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.width(width).dnCard(DNRadius.brief).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HueIcon(item.symbol, item.hue, size = 40.dp)
            Spacer(Modifier.weight(1f).width(8.dp))
            StatusChip(item.chip, tone = item.chipTone, onWhite = true)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, style = DNTextStyle.Headline.style, color = DNColor.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(item.detail, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The personal banner on a topic page (Driving: plate · Registration due · in 12 days · Renew). */
@Composable
internal fun ServicesBannerCard(banner: ServicesBanner, modifier: Modifier = Modifier) {
    val router = LocalRouter.current; val view = LocalView.current
    val stacked = LocalDensity.current.fontScale >= 1.3f
    val lead: @Composable () -> Unit = {
        Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val plate = banner.plate
            if (plate != null) ServicesPlate(plate.code, plate.number, plate.emirate)
            else HueIcon(banner.symbol, banner.hue, size = 44.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(banner.title, style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
                Text(banner.detail, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = servicesToneColor(banner.detailTone))
            }
        }
    }
    val button: @Composable () -> Unit = {
        DNButton(banner.actionTitle, onClick = { DNHaptics.selection(view); router.servicesPerform(banner.action) }, size = DNButtonSize.Small, fullWidth = false)
    }
    Box(modifier.fillMaxWidth().dnCard().padding(16.dp)) {
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { lead(); button() }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { lead() }
                button()
            }
        }
    }
}

/**
 * The small number plate. It's a picture of a real plate with fixed type (iOS uses fixed point
 * sizes, and TalkBack reads its description), so it keeps its size when the system font grows;
 * the shared plate uses sp in a fixed-height box and clips at large text sizes.
 */
@Composable
internal fun ServicesPlate(code: String, number: String, emirate: String) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
        NumberPlate(code, number, emirate = emirate, small = true)
    }
}

// MARK: - Rows

/**
 * A catalogue row that opens its record, with the Favorites star as a separate button beside
 * it (never nested inside the row's click target), so both are tappable and TalkBack reads
 * them as two elements.
 */
@Composable
internal fun ServicesRow(
    service: CatalogueService,
    modifier: Modifier = Modifier,
    trailing: RowTrailing = RowTrailing.Favorite,
    showsIcon: Boolean = true,
    showsContext: Boolean = true,
) {
    val store = LocalStore.current; val router = LocalRouter.current
    val context = if (showsContext) store.servicesContext(service) else null
    Row(modifier.fillMaxWidth().background(DNColor.surfacePrimary), verticalAlignment = Alignment.CenterVertically) {
        CatalogueRow(
            service, Modifier.weight(1f),
            trailing = if (trailing == RowTrailing.Favorite) RowTrailing.None else trailing,
            showsIcon = showsIcon, context = context?.text, contextTone = context?.tone ?: ChipTone.Neutral,
            onClick = { router.open(service) },
        )
        if (trailing == RowTrailing.Favorite) {
            ServicesStarButton(service)
            Spacer(Modifier.width(4.dp))
        }
    }
}

/** The Favorites star for a row (toast + haptics, "Favorites is full" at 8). */
@Composable
internal fun ServicesStarButton(service: CatalogueService) {
    val store = LocalStore.current; val toasts = LocalToasts.current; val view = LocalView.current
    val isFavorite = store.isFavorite(service.id)
    IconButton(
        onClick = { servicesToggleFavorite(service, store, toasts, view) },
        modifier = Modifier.semantics {
            contentDescription = if (isFavorite) "Remove ${service.short} from Favorites" else "Add ${service.short} to Favorites"
        },
    ) {
        Icon(
            if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline, contentDescription = null,
            tint = if (isFavorite) DNColor.actionAccent else DNColor.iconSecondary,
        )
    }
}

/** Favorites toggle with the toast and haptics from the motion spec. */
internal fun servicesToggleFavorite(service: CatalogueService, store: DemoStore, toasts: ToastCenter, view: View) {
    val wasFavorite = store.isFavorite(service.id)
    if (store.toggleFavorite(service)) {
        if (wasFavorite) {
            DNHaptics.light(view)
            toasts.show("Removed from Favorites", Icons.Rounded.StarOutline)
        } else {
            DNHaptics.success(view)
            toasts.show("Added to Favorites", Icons.Rounded.Star, undo = { store.toggleFavorite(service) })
        }
    } else {
        DNHaptics.warning(view)
        toasts.show("Favorites is full (8 of 8). Remove one first.", Icons.Rounded.Error)
    }
}

/** The shape of row [index] of [count] in a rounded list group. */
internal fun servicesGroupShape(index: Int, count: Int, radius: Dp = DNRadius.listGroup): Shape = when {
    count == 1 -> RoundedCornerShape(radius)
    index == 0 -> RoundedCornerShape(topStart = radius, topEnd = radius)
    index == count - 1 -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
    else -> RoundedCornerShape(0.dp)
}

/** A white rounded group of rows with inset separators (small, non-lazy groups). */
@Composable
internal fun <T> ServicesRowGroup(items: List<T>, modifier: Modifier = Modifier, separatorInset: Dp = 16.dp, row: @Composable (T) -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.listGroup)).background(DNColor.surfacePrimary)) {
        items.forEachIndexed { i, item ->
            row(item)
            if (i < items.lastIndex) ServicesDivider(separatorInset)
        }
    }
}

@Composable
internal fun ServicesDivider(inset: Dp = 16.dp, color: Color = DNColor.listRowSeparator) {
    HorizontalDivider(Modifier.padding(start = inset), thickness = 0.5.dp, color = color)
}

/**
 * Emits each row of a rounded group as its own lazy item (long lists: sub-topics, entities),
 * with the group's corners on the first and last rows and inset separators between.
 */
internal fun <T> LazyListScope.servicesGroupItems(
    items: List<T>,
    key: (T) -> Any,
    separatorInset: Dp = 16.dp,
    row: @Composable (T) -> Unit,
) {
    val count = items.size
    items.forEachIndexed { i, item ->
        item(key = key(item)) {
            Column(ServicesInset.clip(servicesGroupShape(i, count)).background(DNColor.surfacePrimary)) {
                row(item)
                if (i < count - 1) ServicesDivider(separatorInset)
            }
        }
    }
}

// MARK: - Headings, chips and links

/** Section heading inside service pages: 20/26 bold ("What you need"). */
@Composable
internal fun ServicesPageHeading(title: String, modifier: Modifier = Modifier) {
    Text(
        title, style = DNTextStyle.Title3.style.copy(fontWeight = FontWeight.Bold), color = DNColor.textPrimary,
        modifier = modifier.semantics { heading() },
    )
}

/** A section header with an optional "See all N" link (topic pages, shelves). */
@Composable
internal fun ServicesSectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, linkTitle: String? = null, onLink: (() -> Unit)? = null) {
    if (linkTitle != null && onLink != null) {
        SectionHeader(title, modifier, subtitle) { DNTextButton(linkTitle, onClick = onLink) }
    } else {
        SectionHeader(title, modifier, subtitle)
    }
}

/** Grey info chip with a glyph ("5 minutes", "AED 420", "RTA"). */
@Composable
internal fun ServicesInfoChip(icon: ImageVector, text: String, tint: Color = DNColor.textPrimary, fill: Color = DNColor.surfaceSecondary) {
    Row(
        Modifier.heightIn(min = 32.dp).clip(CircleShape).background(fill).padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(
            text, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = tint,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Wraps chips onto new lines (entities, info chips). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ServicesFlow(modifier: Modifier = Modifier, content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

/** Inline link (accent text, 48 dp target) used in Related lists and journey stages. */
@Composable
internal fun ServicesLink(title: String, modifier: Modifier = Modifier, weight: FontWeight = FontWeight.Normal, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(title, style = DNTextStyle.Callout.style.copy(fontWeight = weight), color = DNColor.textLink)
    }
}

internal fun servicesToneColor(tone: ChipTone): Color = when (tone) {
    ChipTone.Warning -> DNColor.statusWarningForeground
    ChipTone.Info -> DNColor.statusInfoForeground
    ChipTone.Success -> DNColor.statusSuccessForeground
    ChipTone.Error -> DNColor.statusErrorForeground
    ChipTone.Neutral -> DNColor.textSecondary
}

/** Material Symbols for the SF names this feature uses that the shared map doesn't have. */
internal fun servicesIcon(symbol: String): ImageVector = when (symbol) {
    "clock" -> Icons.Rounded.Schedule
    "banknote" -> Icons.Rounded.Payments
    "building.columns" -> Icons.Rounded.AccountBalance
    "person.2" -> Icons.Rounded.People
    "wrench.and.screwdriver" -> Icons.Rounded.Build
    "list.number" -> Icons.Rounded.FormatListNumbered
    "line.3.horizontal.decrease" -> Icons.Rounded.FilterList
    "mappin.and.ellipse" -> Icons.Rounded.LocationOn
    "arrow.up.forward.square" -> Icons.AutoMirrored.Rounded.OpenInNew
    "exclamationmark.bubble", "exclamationmark.bubble.fill" -> Icons.Rounded.Feedback
    "square.and.arrow.up" -> Icons.Rounded.Share
    else -> DNIcons.forSymbol(symbol)
}

// MARK: - Layout

/**
 * A horizontal shelf of cards that share one height. Shelves hold a handful of cards, so a
 * scrolling Row (not a LazyRow) lets them measure together.
 */
@Composable
internal fun ServicesShelf(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(IntrinsicSize.Max)
            .padding(horizontal = DNSpace.insetPage, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** Full height inside a [ServicesShelf]. */
internal val ServicesShelfItem = Modifier.fillMaxHeight()

// MARK: - Missing record

/** Shown if a route names a record that isn't in the catalogue (some parts aren't records yet). */
@Composable
internal fun ServicesMissingRecord(id: String) {
    ServicesPage {
        item(key = "missing") {
            Column(
                ServicesInset.fillMaxWidth().padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = DNColor.iconSecondary, modifier = Modifier.size(48.dp))
                Text("Not in DubaiNow yet", style = DNTextStyle.Title3.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
                Text(
                    "This service isn't in the catalogue yet. It will appear here when its owner publishes it.",
                    style = DNTextStyle.Body.style, color = DNColor.textSecondary, textAlign = TextAlign.Center,
                )
                Text(id, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
            }
        }
    }
}

// MARK: - Record actions (star + overflow) for the pushed header

/** Star (Favorites, with a toast) and ⋮ (Share, Report a problem) for start, bundle and journey pages. */
@Composable
internal fun ServicesRecordActions(service: CatalogueService) {
    val store = LocalStore.current; val router = LocalRouter.current; val toasts = LocalToasts.current
    val view = LocalView.current; val context = LocalContext.current
    val isFavorite = store.isFavorite(service.id)
    var menu by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (ServicesDebug.flag("servicesStar") && !ServicesDebug.starred) {
            ServicesDebug.starred = true; kotlinx.coroutines.delay(600); servicesToggleFavorite(service, store, toasts, view)
        }
        if (ServicesDebug.flag("servicesMenu") && !ServicesDebug.menuOpened) {
            ServicesDebug.menuOpened = true; kotlinx.coroutines.delay(600); menu = true
        }
    }

    TonalIconButton(
        if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
        if (isFavorite) "Remove from Favorites" else "Add to Favorites",
        onClick = { servicesToggleFavorite(service, store, toasts, view) },
        tint = if (isFavorite) DNColor.actionAccent else DNColor.textPrimary,
    )
    Box {
        TonalIconButton(Icons.Rounded.MoreVert, "More options", onClick = { menu = true })
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = DNColor.surfacePrimary) {
            DropdownMenuItem(
                text = { Text("Share", style = DNTextStyle.Callout.style) },
                leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                onClick = {
                    menu = false
                    servicesShare(context, "${service.name} on DubaiNow")
                },
            )
            DropdownMenuItem(
                text = { Text("Report a problem", style = DNTextStyle.Callout.style) },
                leadingIcon = { Icon(Icons.Rounded.Feedback, contentDescription = null) },
                onClick = {
                    menu = false
                    router.present(SheetRoute.ComingSoonSheet(ComingSoon(
                        title = "Report a problem",
                        detail = "Soon you'll be able to tell us what's wrong with \"${service.name}\" and we'll pass it to whoever runs it.",
                        symbol = "exclamationmark.bubble.fill", hue = service.hue, kind = ComingSoon.Kind.NotInDemo,
                    )))
                },
            )
        }
    }
}

/** The system share sheet with a line of text. */
internal fun servicesShare(context: android.content.Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    runCatching { context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

// MARK: - Cards and pieces shared by record pages

/** A quiet message card ("Nothing to pay", "Sign in to see what you owe"). */
@Composable
internal fun ServicesNote(icon: ImageVector, title: String, detail: String, tint: Color = DNColor.textLink) {
    Row(
        Modifier.fillMaxWidth().dnCard(DNRadius.brief).padding(16.dp).semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = DNTextStyle.Headline.style, color = DNColor.textPrimary)
            Text(detail, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
        }
    }
}

/** The rule and the "Run by …" line at the end of record pages. */
@Composable
internal fun ServicesFooter(text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HorizontalDivider(thickness = 1.dp, color = DNColor.borderSubtle)
        Text(text, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
    }
}

/**
 * A Canvas pill button (same look as the shared DNButton) whose icon can sit after the text
 * ("Start now →"); the shared button only puts icons first.
 */
@Composable
internal fun ServicesButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: DNButtonKind = DNButtonKind.Primary,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
) {
    val (fg, bg) = when (kind) {
        DNButtonKind.Primary -> DNColor.textOnAccent to DNColor.actionAccent
        DNButtonKind.Secondary -> DNColor.textLink to DNColor.actionAccentSubtle
        DNButtonKind.Tertiary -> DNColor.textLink to Color.Transparent
        DNButtonKind.Destructive -> DNColor.actionDestructive to DNColor.surfacePrimary
        DNButtonKind.Inverse -> DNColor.textOnInverse to DNColor.surfaceInverse
    }
    Row(
        modifier.fillMaxWidth().heightIn(min = DNButtonSize.Large.height).clip(CircleShape).background(bg)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 24.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) Icon(leading, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Text(text, style = DNTextStyle.Button.style, color = fg, textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false))
        if (trailing != null) Icon(trailing, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
    }
}

/** SegmentedButton colours from our tokens (accent-subtle selected, white otherwise). */
@Composable
internal fun servicesSegmentedColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = DNColor.actionAccentSubtle,
    activeContentColor = DNColor.textLink,
    activeBorderColor = DNColor.borderControl,
    inactiveContainerColor = DNColor.surfacePrimary,
    inactiveContentColor = DNColor.textPrimary,
    inactiveBorderColor = DNColor.borderControl,
)

/** One option of a single choice as an M3 filter chip (what segmented buttons become at large text sizes). */
@Composable
internal fun ServicesChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick, modifier = modifier,
        label = { Text(label, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(vertical = 6.dp)) },
        leadingIcon = if (selected) ({ Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }) else null,
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = DNColor.surfacePrimary, labelColor = DNColor.textPrimary,
            selectedContainerColor = DNColor.actionAccentSubtle, selectedLabelColor = DNColor.textLink, selectedLeadingIconColor = DNColor.textLink,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = selected, borderColor = DNColor.borderControl, selectedBorderColor = DNColor.borderControl,
        ),
    )
}

// MARK: - Search

/** An in-page search field (M3 search bar input: pill, leading search icon, clear button). */
@Composable
internal fun ServicesSearchField(
    query: String,
    onQuery: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    container: Color = DNColor.surfaceContainer,
) {
    val focus = LocalFocusManager.current
    BasicTextField(
        value = query,
        onValueChange = onQuery,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = placeholder },
        singleLine = true,
        textStyle = DNTextStyle.Body.style.copy(color = DNColor.fieldText),
        cursorBrush = SolidColor(DNColor.actionAccent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(CircleShape).background(container).padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = DNColor.fieldIcon, modifier = Modifier.size(24.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(placeholder, style = DNTextStyle.Body.style, color = DNColor.fieldHint, maxLines = 1)
                    inner()
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQuery("") }, modifier = Modifier.semantics { contentDescription = "Clear search" }) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = DNColor.fieldIcon)
                    }
                } else Spacer(Modifier.width(12.dp))
            }
        },
    )
}

/** "No results for …" (search on A to Z and entities). */
@Composable
internal fun ServicesNoResults(query: String) {
    Column(
        ServicesInset.fillMaxWidth().padding(top = 48.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.SearchOff, contentDescription = null, tint = DNColor.iconSecondary, modifier = Modifier.size(40.dp))
        Text("No results for \u201C${query.trim()}\u201D", style = DNTextStyle.Headline.style, color = DNColor.textPrimary, textAlign = TextAlign.Center)
        Text("Check the spelling or try a different word.", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary, textAlign = TextAlign.Center)
    }
}
