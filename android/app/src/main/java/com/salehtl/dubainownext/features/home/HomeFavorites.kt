package com.salehtl.dubainownext.features.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.app.Router
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.FavoriteItem
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.HueTile
import com.salehtl.dubainownext.design.components.dnElevation
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime

/**
 * Anchor 2 · Favorites. Four columns of hue tiles (they stretch with the screen), label
 * 13/16 medium under each. Up to eight; Edit opens the edit sheet. Touch and hold for
 * Open / Edit Favorites / Remove. At large font sizes: two columns, label beside the tile.
 */
@Composable
internal fun HomeFavoritesSection(grid: HomeGrid, modifier: Modifier = Modifier, menuIndex: Int? = null) {
    val store = LocalStore.current; val router = LocalRouter.current
    val favorites = store.state.favorites
    val large = LocalDensity.current.fontScale >= 1.5f
    Column(modifier.fillMaxWidth().padding(horizontal = DNSpace.insetPage), verticalArrangement = Arrangement.spacedBy(DNSpace.sm)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Favorites", style = DNTextStyle.Section.style, color = DNColor.textPrimary, modifier = Modifier.weight(1f).semantics { heading() })
            HomeLinkButton("Edit", description = "Edit Favorites") { router.present(SheetRoute.EditFavorites) }
        }
        if (large) {
            favorites.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(grid.gutter)) {
                    row.forEach { item -> HomeFavoriteCell(item, grid, large = true, modifier = Modifier.weight(1f)) }
                    if (row.size < 2) Spacer(Modifier.weight(1f))
                }
            }
            if (favorites.size < 4) {
                HomeLinkButton("Add a favorite", leading = true) { router.present(SheetRoute.EditFavorites) }
            }
        } else {
            val cells: List<FavoriteItem?> = favorites + if (favorites.size < 4) listOf(null) else emptyList()
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                cells.chunked(4).forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(grid.tileGutter)) {
                        row.forEachIndexed { c, item ->
                            if (item == null) HomeFavoriteAddTile(grid)
                            else HomeFavoriteCell(item, grid, large = false, initialMenu = menuIndex == r * 4 + c)
                        }
                    }
                }
            }
        }
    }
}

// MARK: Tile

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeFavoriteCell(item: FavoriteItem, grid: HomeGrid, large: Boolean, modifier: Modifier = Modifier, initialMenu: Boolean = false) {
    val store = LocalStore.current; val router = LocalRouter.current; val toasts = LocalToasts.current; val view = LocalView.current
    var menu by remember { mutableStateOf(false) }
    LaunchedEffect(initialMenu) { if (initialMenu) menu = true }
    val interaction = remember { MutableInteractionSource() }
    val badge = homeFavoriteBadge(item)
    val label = (homeService(item)?.name ?: item.title) + (badge?.spoken?.let { ", $it" } ?: "")
    val open = { HomeFavoriteRouting.open(item, store, router) }
    val remove = {
        val before = store.state.favorites
        DNHaptics.light(view)
        store.removeFavorite(item)
        toasts.show("Removed ${item.title} from Favorites", undo = { store.homeRestoreFavorites(before) })
    }
    Box(modifier) {
        val clickable = Modifier
            .homePressScale(interaction)
            .combinedClickable(
                interactionSource = interaction, indication = null, role = Role.Button,
                onLongClick = { DNHaptics.medium(view); menu = true }, onClick = { open() },
            )
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                onClick { open(); true }
                onLongClick(label = "More options") { menu = true; true }
                customActions = listOf(CustomAccessibilityAction("Remove from Favorites") { remove(); true })
            }
        if (large) {
            Row(
                clickable.fillMaxWidth().heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HomeFavoriteTileFace(item, 52.dp, interaction, badge = null)
                Text(item.title, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textPrimary)
            }
        } else {
            Column(clickable.width(grid.tile), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeFavoriteTileFace(item, grid.tile, interaction, badge)
                Text(
                    item.title, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textPrimary,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.requiredWidth(grid.tile + 8.dp),
                )
            }
        }
        DropdownMenu(
            expanded = menu, onDismissRequest = { menu = false },
            shape = RoundedCornerShape(16.dp), containerColor = DNColor.surfacePrimary,
        ) {
            DropdownMenuItem(
                text = { Text("Open", style = DNTextStyle.Body.style) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
                onClick = { menu = false; open() },
            )
            DropdownMenuItem(
                text = { Text("Edit Favorites", style = DNTextStyle.Body.style) },
                leadingIcon = { Icon(Icons.Rounded.GridView, contentDescription = null) },
                onClick = { menu = false; router.present(SheetRoute.EditFavorites) },
            )
            androidx.compose.material3.HorizontalDivider(color = DNColor.borderSubtle)
            DropdownMenuItem(
                text = { Text("Remove from Favorites", style = DNTextStyle.Body.style) },
                leadingIcon = { Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = null) },
                colors = MenuDefaults.itemColors(textColor = DNColor.actionDestructive, leadingIconColor = DNColor.actionDestructive),
                onClick = { menu = false; remove() },
            )
        }
    }
}

/** The tile itself (hue squircle, or the three-disc mark for Pay all), with ripple and badge. */
@Composable
internal fun HomeFavoriteTileFace(item: FavoriteItem, size: Dp, interaction: MutableInteractionSource?, badge: HomeFavoriteBadgeInfo?) {
    Box {
        if (homeTargetID(item) == "pay-all") {
            Box(
                Modifier.size(size).dnElevation(DNRadius.tile).clip(RoundedCornerShape(DNRadius.tile)).background(item.hue.gradient)
                    .border(1.dp, Brush.verticalGradient(listOf(DNColor.tileHighlight, Color.Transparent)), RoundedCornerShape(DNRadius.tile)),
                contentAlignment = Alignment.Center,
            ) { HomePayAllGlyph(color = item.hue.ink, width = size * 0.46f, height = size * 0.24f) }
        } else {
            HueTile(item.symbol, item.hue, size = size)
        }
        if (interaction != null) {
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(DNRadius.tile)).indication(interaction, ripple()))
        }
        if (badge != null) {
            Row(
                Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp)
                    .heightIn(min = 22.dp).widthIn(min = 22.dp).clip(CircleShape)
                    .background(if (badge.live) DNColor.statusSuccessForeground else DNColor.actionDestructive)
                    .border(2.dp, DNColor.surfaceSecondary, CircleShape)
                    .padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            ) {
                if (badge.live) Box(Modifier.size(5.dp).clip(CircleShape).background(DNColor.textOnAccent))
                Text(badge.text, style = DNTextStyle.Caption1.style.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), color = DNColor.textOnAccent)
            }
        }
    }
}

@Composable
private fun HomeFavoriteAddTile(grid: HomeGrid) {
    val router = LocalRouter.current
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier.width(grid.tile).homePressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) { router.present(SheetRoute.EditFavorites) }
            .clearAndSetSemantics { contentDescription = "Add a favorite"; role = Role.Button },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(grid.tile).homeDashedBorder(DNRadius.tile).clip(RoundedCornerShape(DNRadius.tile)).indication(interaction, ripple()),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, contentDescription = null, tint = DNColor.textLink, modifier = Modifier.size(26.dp)) }
        Text("Add", style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textLink)
    }
}

// MARK: Badge

/** Green with a dot = something running (a parking session). Red with a count = Needs you items for that hub. */
internal data class HomeFavoriteBadgeInfo(val text: String, val live: Boolean, val spoken: String)

@Composable
internal fun homeFavoriteBadge(item: FavoriteItem): HomeFavoriteBadgeInfo? {
    val store = LocalStore.current
    val now by produceState(LocalDateTime.now()) {
        while (true) { delay(20_000); value = LocalDateTime.now() }
    }
    val target = item.target
    return when {
        target is FavoriteItem.Target.Service && target.id == "pay-for-parking" -> {
            val p = store.state.parking ?: return null
            val m = Duration.between(now, p.endsAt).toMinutes().toInt()
            if (m <= 0) return null
            HomeFavoriteBadgeInfo(if (m >= 60) "${m / 60}h" else "${m}m", true, "parking running, $m minutes left")
        }
        target is FavoriteItem.Target.Thing && target.id == "your-vehicles" -> {
            val n = store.homeVehicleAttentionCount
            if (n <= 0) return null
            HomeFavoriteBadgeInfo("$n", false, if (n == 1) "1 needs attention" else "$n need attention")
        }
        else -> null
    }
}

// MARK: Routing

/**
 * Where a favorite goes. Pay parking opens Extend when a session is running; Salik opens
 * Top up; hubs (things) open their own screens; everything else opens its catalogue record.
 */
internal object HomeFavoriteRouting {
    fun open(item: FavoriteItem, store: DemoStore, router: Router) {
        when (val t = item.target) {
            is FavoriteItem.Target.Service -> when (t.id) {
                "pay-for-parking" -> if (store.state.parking != null) router.present(SheetRoute.ExtendParking) else router.open(t.id)
                "top-up-salik-account" -> router.present(SheetRoute.TopUpSalik)
                else -> router.open(t.id)
            }
            is FavoriteItem.Target.Thing -> when (t.id) {
                "your-vehicles" -> router.push(Route.Vehicles)
                "your-documents" -> router.push(Route.Documents)
                "your-family-residency" -> router.push(Route.Family)
                else -> router.open(t.id)
            }
        }
    }
}
