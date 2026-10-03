package com.salehtl.dubainownext.features.home

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.EaseInOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Crop169
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.Square
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.FavoriteItem
import com.salehtl.dubainownext.data.HomeWidget
import com.salehtl.dubainownext.data.NeedsYouItem
import com.salehtl.dubainownext.data.WidgetKind
import com.salehtl.dubainownext.data.WidgetSize
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import java.time.LocalDateTime

// MARK: - Grid

/**
 * The Home grid (Paper C1 · Favorites spec): 20 dp page insets, 12 dp gutters.
 * Favorites are four columns, widgets two. Everything stretches with the screen width, so tiles,
 * widgets and cards share the same left and right edges. Worked out in whole pixels so the edges
 * really meet at any width: two smalls and the gutter are exactly one wide widget, and the
 * favorite gaps take the odd pixels so the fourth column ends on the right inset.
 */
internal class HomeGrid(widthPx: Int, density: Density) {
    private val scale = density.density
    private val gutterPx = with(density) { DNSpace.widgetGutter.roundToPx() }
    private val contentPx = with(density) {
        maxOf(0, (if (widthPx > 0) widthPx else 402.dp.roundToPx()) - DNSpace.insetPage.roundToPx() * 2)
    }
    private val tilePx = maxOf(with(density) { 56.dp.roundToPx() }, (contentPx - gutterPx * 3) / 4)
    private val smallPx = maxOf(with(density) { 120.dp.roundToPx() }, (contentPx - gutterPx) / 2)
    private fun dp(px: Int): Dp = (px / scale).dp

    val inset: Dp get() = DNSpace.insetPage
    val gutter: Dp = dp(gutterPx)
    /** Width of the content column (screen − 2 × 20). */
    val content: Dp = dp(contentPx)
    /** One favorite column: (width − 40 − 36) / 4 (81.5 on a 402 dp screen). */
    val tile: Dp = dp(tilePx)
    /** Gap between favorite columns: the gutter plus the odd pixels. */
    val tileGutter: Dp = dp(maxOf(gutterPx, (contentPx - tilePx * 4) / 3))
    /** One small widget: (width − 40 − 12) / 2 (175 on a 402 dp screen). */
    val small: Dp = dp(smallPx)
    /** A medium or large widget: two smalls and the gutter. */
    val wide: Dp = dp(smallPx * 2 + gutterPx)

    fun size(of: WidgetSize): DpSize = when (of) {
        WidgetSize.Small -> DpSize(small, small)
        WidgetSize.Medium -> DpSize(wide, small)
        WidgetSize.Large -> DpSize(wide, wide)
    }
}

// MARK: - Widget rows

/** Widgets packed in order: two smalls side by side, mediums and larges on their own row. */
internal fun homePackWidgets(widgets: List<HomeWidget>): List<List<HomeWidget>> {
    val rows = mutableListOf<List<HomeWidget>>()
    var i = 0
    while (i < widgets.size) {
        val w = widgets[i]
        if (w.size == WidgetSize.Small && i + 1 < widgets.size && widgets[i + 1].size == WidgetSize.Small) {
            rows += listOf(w, widgets[i + 1]); i += 2
        } else {
            rows += listOf(w); i += 1
        }
    }
    return rows
}

/**
 * Which sizes each widget offers. Mirrors the Widgets feature's `WidgetKind.widgetSizes` on iOS
 * (bundles Small/Medium, journeys Medium/Large; the Android model has no such property yet).
 */
internal object HomeWidgetSizes {
    fun allowed(kind: WidgetKind): List<WidgetSize> = when (kind) {
        WidgetKind.Parking, WidgetKind.Salik, WidgetKind.BillsAndFines, WidgetKind.FamilyVisas -> listOf(WidgetSize.Small, WidgetSize.Medium)
        WidgetKind.Prayer -> listOf(WidgetSize.Small, WidgetSize.Large)
        WidgetKind.Weather, WidgetKind.Nol, is WidgetKind.VisaTracker -> listOf(WidgetSize.Small)
        WidgetKind.Metro, WidgetKind.Energy -> listOf(WidgetSize.Medium)
        is WidgetKind.Journey -> listOf(WidgetSize.Medium, WidgetSize.Large)
        WidgetKind.Certificates -> emptyList()
    }

    /** The size control says "Wide" (Paper I9L); the widget menu says "Medium". */
    fun label(size: WidgetSize) = when (size) { WidgetSize.Small -> "Small"; WidgetSize.Medium -> "Wide"; WidgetSize.Large -> "Large" }
    fun spoken(size: WidgetSize) = when (size) { WidgetSize.Small -> "small"; WidgetSize.Medium -> "wide"; WidgetSize.Large -> "large" }
    fun icon(size: WidgetSize): ImageVector = when (size) {
        WidgetSize.Small -> Icons.Rounded.CropSquare
        WidgetSize.Medium -> Icons.Rounded.Crop169
        WidgetSize.Large -> Icons.Rounded.Square
    }

    fun next(widget: HomeWidget): WidgetSize {
        val sizes = allowed(widget.kind)
        val i = sizes.indexOf(widget.size)
        if (i < 0) return sizes.firstOrNull() ?: widget.size
        return sizes[(i + 1) % sizes.size]
    }
}

// MARK: - Store helpers (only the store's public API)

/** Restores favorites exactly (Undo, or × in the edit sheet). */
internal fun DemoStore.homeRestoreFavorites(favorites: List<FavoriteItem>) { state = state.copy(favorites = favorites) }

/** Restores the Needs you order (Undo after Later). */
internal fun DemoStore.homeRestoreNeedsYou(items: List<NeedsYouItem>) { state = state.copy(needsYou = items) }

/** Moves one widget a step earlier (-1) or later (+1) — "without dragging". */
internal fun DemoStore.homeMoveWidget(id: String, step: Int) {
    val from = state.widgets.indexOfFirst { it.id == id }
    val to = from + step
    if (from < 0 || to !in state.widgets.indices) return
    moveWidget(from, to)
}

/** Moves one favorite a step earlier (-1) or later (+1). */
internal fun DemoStore.homeMoveFavorite(id: String, step: Int) {
    val from = state.favorites.indexOfFirst { it.id == id }
    val to = from + step
    if (from < 0 || to !in state.favorites.indices) return
    moveFavorite(from, to)
}

/** Adds a suggested widget in place of the suggestion (top of the canvas) and returns it. */
internal fun DemoStore.homeAddSuggestedWidget(kind: WidgetKind, size: WidgetSize): HomeWidget {
    val w = addWidget(kind, size, at = 0)
    state = state.copy(widgetSuggestion = null)
    return w
}

/** Things in Needs you that belong to the resident's vehicles (fines and renewals). */
internal val DemoStore.homeVehicleAttentionCount: Int
    get() {
        val fineIDs = state.vehicles.flatMap { it.fineIDs }.toSet()
        val vehicleIDs = state.vehicles.map { it.id }.toSet()
        return state.needsYou.count { item ->
            when (val k = item.kind) {
                is NeedsYouItem.Kind.Payment -> k.payable.id in fineIDs
                is NeedsYouItem.Kind.Renewal -> k.thingID?.let { it in vehicleIDs } ?: false
                else -> false
            }
        }
    }

/** Something due today (or overdue) — turns the Needs you line amber. */
internal val DemoStore.homeDueToday: NeedsYouItem?
    get() = state.needsYou.firstOrNull { item -> item.due?.let { DemoDate.daysFromToday(it) <= 0 } ?: false }

// MARK: - Copy helpers

internal object HomeCopy {
    /** "today", "tomorrow", "in 3 days", "on 14 Oct". */
    fun whenText(date: LocalDateTime): String = when (val n = DemoDate.daysFromToday(date)) {
        in Int.MIN_VALUE..-1 -> "overdue"
        0 -> "today"
        1 -> "tomorrow"
        in 2..6 -> "in $n days"
        else -> "on ${DemoDate.short(date)}"
    }

    /** "Electricity & water bill" → "electricity and water bill" (for a running sentence). */
    fun inline(title: String): String {
        val t = title.replace(" & ", " and ")
        if (t.isEmpty()) return t
        // Keep acronyms ("DEWA", "RTA") as they are.
        val firstWord = t.split(" ").firstOrNull() ?: ""
        if (firstWord.length > 1 && firstWord == firstWord.uppercase()) return t
        return t.first().lowercase() + t.drop(1)
    }
}

// MARK: - Launch extras (demo and screenshots only)

/**
 * Debug extras handled inside Home (adb … -e homeEditing true):
 * homeFolded · homeEmptyNeedsYou · homeEditing · homeSelectWidget <index> · homeScrollTo widgets|bottom ·
 * homePull <dp> · homeFavoriteMenu <index> · homeSizeMenu · homeLimit · homeFavSelect <index> · homeFavQuery <text> ·
 * homeJourneyCard · homeFontScale <scale>.
 */
internal object HomeLaunch {
    private fun extra(name: String): String? = com.salehtl.dubainownext.app.LaunchOptions.extras[name]
    private fun flag(name: String): Boolean = com.salehtl.dubainownext.app.LaunchOptions.flag(name)

    val folded get() = flag("homeFolded")
    val emptyNeedsYou get() = flag("homeEmptyNeedsYou")
    val editing get() = flag("homeEditing")
    val selectedWidgetIndex: Int? get() = extra("homeSelectWidget")?.toIntOrNull()
    val scrollTarget: String? get() = extra("homeScrollTo")
    val pull: Float? get() = extra("homePull")?.toFloatOrNull()
    val favoriteMenuIndex: Int? get() = extra("homeFavoriteMenu")?.toIntOrNull()
    val sizeMenu get() = flag("homeSizeMenu")
    val fillFavorites get() = flag("homeLimit")
    val favoriteSelectIndex: Int? get() = extra("homeFavSelect")?.toIntOrNull()
    val favoriteQuery: String? get() = extra("homeFavQuery")
    /** Draws Home and the edit sheet at this font scale (e.g. 2 = 200%) without touching the system setting. */
    val fontScale: Float? get() = extra("homeFontScale")?.toFloatOrNull()
    /** Puts a journey step (Build a house, stage 4 of 7) first in Needs you, to check the JX1 card. */
    val journeyCard get() = flag("homeJourneyCard")

    /** Applied once per process, the first time Home (or the sheet) appears. */
    var appliedHome = false
    var appliedSheet = false

    fun addJourneyCardIfAsked(store: DemoStore) {
        if (!journeyCard) return
        val item = NeedsYouItem(
            "ny-home-journey", NeedsYouItem.Kind.JourneyStep("build-a-house"), "Build a house", "house.lodge.fill",
            com.salehtl.dubainownext.design.Hue.Sand, "Review your villa drawings", "Sent by your consultant today",
            null, "Review drawings", null,
        )
        val progress = com.salehtl.dubainownext.data.JourneyProgress(
            "build-a-house", 3, "Design", "Waiting for you", DemoDate.day(-200), needsYou = true,
        )
        store.state = store.state.copy(
            needsYou = listOf(item) + store.state.needsYou.filter { it.id != item.id },
            journeys = store.state.journeys.filter { it.journeyID != "build-a-house" } + progress,
        )
    }

    fun fillFavoritesIfAsked(store: DemoStore) {
        if (!fillFavorites) return
        val extra = listOf("pay-electricity-water-bill", "top-up-nol-card", "your-family-residency", "your-documents")
        for (id in extra) {
            if (store.state.favorites.size >= DemoStore.FAVORITES_LIMIT) break
            val s = Catalogue.service(id) ?: continue
            if (!store.isFavorite(id)) store.toggleFavorite(s)
        }
    }
}

/** Debug: `homeFontScale 2` lays Home (and the edit sheet) out as if the system font size were 200%. */
@Composable
internal fun HomeFontScaleOverride(content: @Composable () -> Unit) {
    val scale = HomeLaunch.fontScale
    if (scale == null) {
        content()
    } else {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, scale), content = content)
    }
}

/** True when the system "Remove animations" setting is on (animator duration scale 0). */
@Composable
internal fun homeAnimationsOff(): Boolean {
    val context = LocalContext.current
    return remember { homeAnimatorScale(context) == 0f }
}

private fun homeAnimatorScale(context: Context): Float =
    runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f)

// MARK: - Small shared pieces

/** A dashed rounded outline (empty slots, Add widget). */
internal fun Modifier.homeDashedBorder(radius: Dp, color: Color = DNColor.neutral400, width: Dp = 1.5.dp): Modifier =
    this.drawBehind {
        val w = width.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(w / 2, w / 2),
            size = Size(size.width - w, size.height - w),
            cornerRadius = CornerRadius(radius.toPx() - w / 2),
            style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
        )
    }

/** A focus ring drawn 6 dp outside the content (the selected widget or favorite). */
internal fun Modifier.homeFocusRing(visible: Boolean, radius: Dp, color: Color = DNColor.borderFocus, outset: Dp = 6.dp, width: Dp = 3.dp): Modifier =
    if (!visible) this else this.drawBehind {
        val o = outset.toPx(); val w = width.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(-o + w / 2, -o + w / 2),
            size = Size(size.width + 2 * o - w, size.height + 2 * o - w),
            cornerRadius = CornerRadius((radius + outset).toPx() - w / 2),
            style = Stroke(w),
        )
    }

/** Press feedback for tiles and cards: scales to 0.97 while pressed. */
internal fun Modifier.homePressScale(interaction: MutableInteractionSource): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) DNMotion.PRESS_SCALE else 1f, DNMotion.snappy(), label = "press")
    this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Gentle ±1.5° wobble for widgets in Edit Home. Off when animations are removed. */
internal fun Modifier.homeWobble(active: Boolean, seed: Int): Modifier = composed {
    if (!active || homeAnimationsOff()) return@composed this
    val transition = rememberInfiniteTransition(label = "wobble")
    val angle by transition.animateFloat(
        initialValue = -1.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(130 + (seed % 3) * 12, easing = EaseInOut), RepeatMode.Reverse),
        label = "wobble-angle",
    )
    this.graphicsLayer { rotationZ = if (seed % 2 == 0) angle else -angle }
}

/** The Pay all mark: three overlapping discs (the bundle's stacked circles). */
@Composable
internal fun HomePayAllGlyph(modifier: Modifier = Modifier, color: Color = Color.White, width: Dp = 20.dp, height: Dp = 11.dp) {
    Canvas(modifier.size(width, height).clearAndSetSemantics { }) {
        val r = size.height * 0.36f
        val xs = listOf(0.25f, 0.5f, 0.75f)
        val alphas = listOf(0.5f, 0.78f, 1f)
        xs.zip(alphas).forEach { (x, a) ->
            drawCircle(color.copy(alpha = a), radius = r, center = Offset(size.width * x, size.height / 2))
        }
    }
}

/** Small text chip on a hue poster ("Due tomorrow", "Your turn"). Neutral chips have no dot. */
internal enum class HomeChipTone { Warning, Error, Neutral }

@Composable
internal fun HomeBriefChip(text: String, tone: HomeChipTone, modifier: Modifier = Modifier) {
    val color = when (tone) {
        HomeChipTone.Warning -> DNColor.statusWarningForeground
        HomeChipTone.Error -> DNColor.statusErrorForeground
        HomeChipTone.Neutral -> DNColor.textPrimary
    }
    Row(
        modifier.heightIn(min = 26.dp).clip(CircleShape).background(DNColor.surfacePrimary).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (tone != HomeChipTone.Neutral) Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(text, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A count capsule next to a heading ("Needs you 4"). */
@Composable
internal fun HomeCountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.heightIn(min = 24.dp).widthIn(min = 24.dp).clip(CircleShape).background(DNColor.textPrimary).padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("$count", style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), color = DNColor.textOnInverse)
    }
}

/** The service behind a favorite (for names, topics and labels). */
internal fun homeService(item: FavoriteItem): CatalogueService? = when (val t = item.target) {
    is FavoriteItem.Target.Service -> Catalogue.service(t.id)
    is FavoriteItem.Target.Thing -> Catalogue.service(t.id)
}

internal fun homeTargetID(item: FavoriteItem): String = when (val t = item.target) {
    is FavoriteItem.Target.Service -> t.id
    is FavoriteItem.Target.Thing -> t.id
}
