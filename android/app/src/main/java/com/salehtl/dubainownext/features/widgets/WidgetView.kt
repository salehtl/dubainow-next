package com.salehtl.dubainownext.features.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.app.LocalWidgetPreview
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.app.Router
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.HomeWidget
import com.salehtl.dubainownext.data.PaymentLine
import com.salehtl.dubainownext.data.PaymentPurpose
import com.salehtl.dubainownext.data.PaymentRequest
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.data.WidgetKind
import com.salehtl.dubainownext.data.WidgetSize
import com.salehtl.dubainownext.data.aed
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNIcons
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.ToastCenter
import com.salehtl.dubainownext.design.components.dnElevation
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.roundToInt

/**
 * One Home widget (an in-app card, not an Android app widget). Paper C1 · 02 Widgets.
 *
 * It fills whatever size the parent gives it (Home: small = half width square, medium = full width
 * × small height, large = full width square), clips to [DNRadius.widget] and adds the Canvas
 * elevation itself, and draws every [WidgetKind] from the persona's live state. The whole card opens
 * the right place; the quick action (+ Extend, + Top up…) is its own button on top. Touch and hold
 * opens the M3 menu: quick action, Open, Size, Edit Home, Remove (with Undo).
 * Inside `LocalWidgetPreview provides true` (gallery, Edit Home) it takes no taps, menus or timers.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetView(widget: HomeWidget, modifier: Modifier = Modifier) {
    val store = LocalStore.current
    val router = LocalRouter.current
    val toasts = LocalToasts.current
    val isPreview = LocalWidgetPreview.current
    val view = LocalView.current
    val layoutSize = widget.kind.widgetLayoutSize(widget.size)

    // Countdowns tick every second; slower facts refresh every 15 s or every minute. No timers in previews.
    var real by remember { mutableStateOf(LocalDateTime.now()) }
    if (!isPreview) {
        val tick = widget.kind.widgetTickMillis
        LaunchedEffect(tick) {
            while (true) {
                delay(tick - System.currentTimeMillis() % tick)
                real = LocalDateTime.now()
            }
        }
    }
    // Like iOS `store.now`: the demo hour at :41:00 (seconds zeroed, so "in 2h 31m" reads the same).
    val shown = store.hourOverride?.let { real.withHour(it).withMinute(41).withSecond(0).withNano(0) } ?: real

    val spec = widgetSpec(widget.kind, layoutSize, store, real, shown)
    val shape = RoundedCornerShape(DNRadius.widget)
    var menuOpen by remember { mutableStateOf(false) }
    var sizesOpen by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && !isPreview) DNMotion.PRESS_SCALE else 1f, DNMotion.snappy(), label = "widget-press")

    val actions = remember(store, router, toasts) { WidgetActions(store, router, toasts) }
    val openMenu = {
        DNHaptics.medium(view)
        sizesOpen = false
        menuOpen = true
    }

    // Debug: `widgetsDebugMenu <widget id | key-size>` opens this widget's menu at launch (screenshots).
    if (!isPreview) {
        LaunchedEffect(widget.id) {
            val target = LaunchOptions.extras["widgetsDebugMenu"] ?: return@LaunchedEffect
            if (target == widget.id || target == "${widget.kind.widgetKey}-${widget.size.name.lowercase()}") {
                delay(1200)
                menuOpen = true
                sizesOpen = LaunchOptions.flag("widgetsDebugMenuSizes")
            }
        }
    }

    // Widgets are glanceable fixed frames: like iOS (Dynamic Type capped at xxxLarge) the font scale stops at 1.3.
    val density = LocalDensity.current
    val capped = remember(density) { Density(density.density, density.fontScale.coerceAtMost(1.3f)) }

    // The quick action reports where it's drawn; a TalkBack-only button sits over it (iOS draws its quick
    // button over the card the same way), so it is its own element while the card reads one sentence.
    val quickSlots = remember { WidgetQuickSlots() }

    Box(modifier.graphicsLayer { scaleX = scale; scaleY = scale }.onPlaced { quickSlots.card = it }) {
        Box(
            Modifier
                .fillMaxSize()
                .dnElevation(DNRadius.widget)
                .clip(shape)
                .widgetSurface(spec.surface)
                .let { m -> spec.edge?.let { m.border(1.5.dp, it, shape) } ?: m }
                .let { m ->
                    if (isPreview) m else m.combinedClickable(
                        interactionSource = interaction,
                        indication = ripple(color = if (spec.surface.isDark) Color.White else Color.Unspecified),
                        role = Role.Button,
                        onClickLabel = widgetClickLabel(spec.hint),
                        onLongClickLabel = "show options",
                        onLongClick = openMenu,
                        hapticFeedbackEnabled = false,
                        onClick = { actions.open(spec.open) },
                    )
                }
                .clearAndSetSemantics {
                    contentDescription = spec.summary
                    if (!isPreview) {
                        role = Role.Button
                        onClick(label = widgetClickLabel(spec.hint)) { actions.open(spec.open); true }
                        onLongClick(label = "show options") { openMenu(); true }
                        customActions = buildList {
                            spec.quick?.let { q -> add(CustomAccessibilityAction(q.label) { actions.perform(q); true }) }
                            widget.kind.widgetSizes.filter { it != widget.size }.forEach { s ->
                                add(CustomAccessibilityAction("Make ${s.widgetLabel.lowercase()}") { store.resizeWidget(widget, s); true })
                            }
                            add(CustomAccessibilityAction("Remove widget") { actions.remove(widget, view); true })
                        }
                    }
                },
        ) {
            CompositionLocalProvider(
                LocalWidgetQuick provides (if (isPreview) null else { intent -> actions.perform(intent) }),
                LocalWidgetQuickSlots provides (if (isPreview) null else quickSlots),
                LocalDensity provides capped,
            ) {
                WidgetContent(widget.kind, layoutSize, real, shown)
            }
        }
        val slot = quickSlots.slot
        if (!isPreview && slot != null) {
            // No pointer input: touches go through to the drawn quick action underneath.
            Box(
                Modifier
                    .offset { IntOffset(slot.bounds.left.roundToInt(), slot.bounds.top.roundToInt()) }
                    .size(with(density) { slot.bounds.width.toDp() }, with(density) { slot.bounds.height.toDp() })
                    .semantics {
                        contentDescription = slot.intent.label
                        role = Role.Button
                        onClick { actions.perform(slot.intent); true }
                    },
            )
        }
        if (!isPreview) {
            WidgetMenu(
                widget = widget,
                spec = spec,
                expanded = menuOpen,
                sizesOpen = sizesOpen,
                onToggleSizes = { sizesOpen = !sizesOpen },
                onDismiss = { menuOpen = false },
                actions = actions,
            )
        }
    }
}

// MARK: - Menu (touch and hold)

@Composable
private fun WidgetMenu(
    widget: HomeWidget,
    spec: WidgetSpec,
    expanded: Boolean,
    sizesOpen: Boolean,
    onToggleSizes: () -> Unit,
    onDismiss: () -> Unit,
    actions: WidgetActions,
) {
    val view = LocalView.current
    val textStyle = DNTextStyle.Callout.style
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        spec.quick?.let { q ->
            DropdownMenuItem(
                text = { Text(q.label, style = textStyle) },
                leadingIcon = { Icon(Icons.Rounded.AddCircleOutline, contentDescription = null) },
                onClick = { onDismiss(); actions.perform(q) },
            )
        }
        DropdownMenuItem(
            text = { Text("Open ${widget.kind.title}", style = textStyle) },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
            onClick = { onDismiss(); actions.open(spec.open) },
        )
        val sizes = widget.kind.widgetSizes
        if (sizes.size > 1) {
            DropdownMenuItem(
                text = { Text("Size", style = textStyle) },
                leadingIcon = { Icon(Icons.Rounded.FitScreen, contentDescription = null) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(widget.size.widgetLabel, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                        Icon(if (sizesOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
                    }
                },
                onClick = onToggleSizes,
            )
            if (sizesOpen) {
                sizes.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s.widgetLabel, style = textStyle) },
                        leadingIcon = {
                            if (s == widget.size) Icon(Icons.Rounded.Check, contentDescription = "Current size", tint = DNColor.actionAccent)
                            else Spacer(Modifier.size(24.dp))
                        },
                        onClick = {
                            onDismiss()
                            if (s != widget.size) {
                                DNHaptics.selection(view)
                                actions.store.resizeWidget(widget, s)
                            }
                        },
                        contentPadding = MenuDefaults.DropdownMenuItemContentPadding,
                        modifier = Modifier,
                    )
                }
            }
        }
        DropdownMenuItem(
            text = { Text("Edit Home", style = textStyle) },
            leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
            onClick = { onDismiss(); actions.editHome(view) },
        )
        HorizontalDivider(color = DNColor.borderSubtle)
        DropdownMenuItem(
            text = { Text("Remove widget", style = textStyle, color = DNColor.actionDestructive) },
            leadingIcon = { Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = null, tint = DNColor.actionDestructive) },
            onClick = { onDismiss(); actions.remove(widget, view) },
        )
    }
}

// MARK: - Actions

/** What tapping a widget opens. */
internal sealed interface WidgetOpen {
    data class Sheet(val sheet: SheetRoute) : WidgetOpen
    data class Push(val route: Route) : WidgetOpen
    data class Service(val id: String) : WidgetOpen
    data class Soon(val item: ComingSoon) : WidgetOpen
}

/** Router and store calls shared by the card, its quick action, its menu and TalkBack actions. */
internal class WidgetActions(val store: DemoStore, val router: Router, private val toasts: ToastCenter) {
    fun open(target: WidgetOpen) {
        when (target) {
            is WidgetOpen.Sheet -> router.present(target.sheet)
            is WidgetOpen.Push -> navigate(target.route)
            is WidgetOpen.Service -> {
                val service = Catalogue.service(target.id) ?: return
                navigate(
                    when (service.type) {
                        ServiceType.Standalone -> Route.Service(target.id)
                        ServiceType.Bundle -> Route.Bundle(target.id)
                        ServiceType.Journey -> Route.Journey(target.id)
                    }
                )
            }
            is WidgetOpen.Soon -> router.present(SheetRoute.ComingSoonSheet(target.item))
        }
    }

    /** Pushes on the current tab. From inside a sheet (the gallery's debug grid), closes the sheet first. */
    fun navigate(route: Route) {
        if (router.sheet != null) router.dismissSheet()
        router.push(route)
    }

    fun perform(intent: WidgetQuickIntent) {
        when (intent) {
            WidgetQuickIntent.ExtendParking -> router.present(SheetRoute.ExtendParking)
            WidgetQuickIntent.StartParking -> router.present(
                SheetRoute.ComingSoonSheet(
                    ComingSoon(title = "Pay for parking", owner = "Parkin", symbol = "p.square.fill", hue = Hue.Sky, kind = ComingSoon.Kind.ServicesPhase)
                )
            )
            WidgetQuickIntent.TopUpSalik -> router.present(SheetRoute.TopUpSalik)
            WidgetQuickIntent.TopUpNol -> router.present(
                SheetRoute.Payment(
                    PaymentRequest(
                        id = "widget-nol-" + UUID.randomUUID().toString().take(6),
                        title = "Top up your nol card",
                        lines = listOf(PaymentLine("nol card top-up", "RTA", 50.aed, "tram.fill", Hue.Sky)),
                        purpose = PaymentPurpose.NolTopUp(50.aed),
                    )
                )
            )
            WidgetQuickIntent.RenewFamilyVisas -> navigate(Route.Bundle("renew-family-visas"))
            is WidgetQuickIntent.Connect -> router.present(
                SheetRoute.ComingSoonSheet(
                    ComingSoon(
                        title = "Link your ${intent.account} account",
                        detail = "In the full app you confirm the link once with UAE PASS, and this widget fills in by itself.",
                        owner = intent.account,
                        symbol = widgetSymbol("link", widgetAccountSymbol(intent.account)),
                        hue = Hue.Sky,
                        kind = ComingSoon.Kind.NotInDemo,
                    )
                )
            )
        }
    }

    fun remove(widget: HomeWidget, view: android.view.View) {
        val index = store.state.widgets.indexOfFirst { it.id == widget.id }.let { if (it < 0) store.state.widgets.size else it }
        DNHaptics.light(view)
        store.removeWidget(widget)
        toasts.show("${widget.kind.title} removed", icon = Icons.Rounded.RemoveCircle, undo = { store.restoreWidget(widget, index) })
    }

    fun editHome(view: android.view.View) {
        DNHaptics.medium(view)
        if (router.sheet != null) router.dismissSheet()
        router.homeEditRequested = true
    }
}

/**
 * TalkBack says "Double-tap to <label>", so the iOS hint ("Opens Extend parking") becomes a verb phrase
 * ("open Extend parking"). No hint: TalkBack's own "activate".
 */
internal fun widgetClickLabel(hint: String): String? =
    if (hint.isEmpty()) null else if (hint.startsWith("Opens ")) "open " + hint.removePrefix("Opens ") else hint

/** The shared symbol name if the shared icon map has it, else a mapped fallback (until it's added). */
internal fun widgetSymbol(preferred: String, fallback: String): String = if (DNIcons.has(preferred)) preferred else fallback

private fun widgetAccountSymbol(account: String): String = when (account) {
    "Salik" -> "road.lanes"
    "DEWA" -> "bolt.fill"
    "nol" -> "tram.fill"
    else -> "sparkles"
}

// MARK: - Content dispatch

/** The drawing for each kind at its layout size. */
@Composable
private fun WidgetContent(kind: WidgetKind, size: WidgetSize, real: LocalDateTime, shown: LocalDateTime) {
    Box(Modifier.fillMaxSize()) {
        when (kind) {
            WidgetKind.Parking -> WidgetParkingContent(size, real)
            WidgetKind.Salik -> WidgetSalikContent(size)
            WidgetKind.Nol -> WidgetNolContent()
            WidgetKind.Metro -> WidgetMetroContent(real)
            WidgetKind.Prayer -> WidgetPrayerContent(size, shown)
            WidgetKind.Weather -> WidgetWeatherContent()
            WidgetKind.Energy -> WidgetEnergyContent()
            WidgetKind.BillsAndFines -> WidgetBillsContent(size, real)
            WidgetKind.FamilyVisas -> WidgetFamilyContent(size)
            is WidgetKind.Journey -> WidgetJourneyContent(kind.id, size)
            is WidgetKind.VisaTracker -> WidgetRequestContent(kind.requestID)
            WidgetKind.Certificates -> WidgetCertificatesContent()
        }
    }
}

// MARK: - Spec (surface, summary, actions)

/**
 * Everything about a widget that isn't drawing: its surface, the one sentence TalkBack reads,
 * its quick action and where it opens.
 */
internal data class WidgetSpec(
    val surface: WidgetSurface = WidgetSurface.Plain,
    val edge: Color? = null,
    val summary: String,
    val hint: String = "",
    val quick: WidgetQuickIntent? = null,
    val open: WidgetOpen,
)

internal fun widgetSpec(kind: WidgetKind, size: WidgetSize, store: DemoStore, real: LocalDateTime, shown: LocalDateTime): WidgetSpec =
    when (kind) {
        WidgetKind.Parking -> widgetParkingSpec(store, real)
        WidgetKind.Salik -> widgetSalikSpec(store)
        WidgetKind.Nol -> widgetNolSpec(store)
        WidgetKind.Metro -> widgetMetroSpec(store, real)
        WidgetKind.Prayer -> widgetPrayerSpec(shown)
        WidgetKind.Weather -> widgetWeatherSpec(store)
        WidgetKind.Energy -> widgetEnergySpec(store)
        WidgetKind.BillsAndFines -> widgetBillsSpec(store, real)
        WidgetKind.FamilyVisas -> widgetFamilySpec(store, size)
        is WidgetKind.Journey -> widgetJourneySpec(kind.id, store)
        is WidgetKind.VisaTracker -> widgetRequestSpec(kind.requestID, store)
        WidgetKind.Certificates -> WidgetSpec(summary = "Certificates and letters. Nothing in progress.", open = WidgetOpen.Service("get-certificates"))
    }
