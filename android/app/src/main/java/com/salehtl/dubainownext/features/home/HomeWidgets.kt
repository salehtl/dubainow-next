package com.salehtl.dubainownext.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.app.LocalWidgetPreview
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.HomeWidget
import com.salehtl.dubainownext.data.WidgetKind
import com.salehtl.dubainownext.data.WidgetSize
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNButtonSize
import com.salehtl.dubainownext.design.components.DNRowDivider
import com.salehtl.dubainownext.design.components.dnElevation
import com.salehtl.dubainownext.features.widgets.WidgetView

/**
 * "Your widgets": a two-column canvas (small = half width square, medium = full width ×
 * small height, large = full width square), packed in order. In Edit Home the widgets
 * wobble and carry remove badges; select one to move it earlier or later, or resize it.
 */
@Composable
internal fun HomeWidgetsSection(
    grid: HomeGrid,
    isEditing: Boolean,
    selectedID: String?,
    onSelect: (HomeWidget) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val store = LocalStore.current; val toasts = LocalToasts.current; val view = LocalView.current
    val widgets = store.state.widgets
    val suggestion: WidgetKind? = store.state.widgetSuggestion?.takeIf { !store.isGuest && !isEditing && !store.hasWidget(it) }
    var lastSuggestion by remember { mutableStateOf(suggestion) }
    if (suggestion != null) lastSuggestion = suggestion

    val remove = { w: HomeWidget ->
        val index = store.state.widgets.indexOfFirst { it.id == w.id }
        DNHaptics.light(view)
        store.removeWidget(w)
        toasts.show("Removed ${w.kind.title}", undo = { store.restoreWidget(w, index) })
    }
    val add = { kind: WidgetKind ->
        DNHaptics.success(view)
        val w = store.homeAddSuggestedWidget(kind, WidgetSize.Medium)
        toasts.show("Added ${kind.title} to Home", undo = {
            store.removeWidget(w)
            store.state = store.state.copy(widgetSuggestion = kind)
        })
    }
    val dismiss = {
        store.dismissSuggestion()
        toasts.show("We won't suggest it again for 30 days", icon = Icons.Rounded.Schedule)
    }

    Column(
        modifier.fillMaxWidth().padding(horizontal = DNSpace.insetPage),
        verticalArrangement = Arrangement.spacedBy(if (isEditing) 14.dp else DNSpace.sm),
    ) {
        HomeWidgetsHeader(isEditing, widgets.isEmpty(), onEdit)
        AnimatedVisibility(
            visible = suggestion != null,
            enter = fadeIn(DNMotion.base()) + scaleIn(DNMotion.smooth(), initialScale = 0.96f),
            exit = fadeOut(DNMotion.base()) + scaleOut(DNMotion.smooth(), targetScale = 0.96f),
        ) {
            lastSuggestion?.let { kind ->
                HomeWidgetSuggestion(kind, grid, onAdd = { add(kind) }, onDismiss = dismiss, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        if (widgets.isEmpty() && suggestion == null && !isEditing) {
            HomeWidgetsEmpty()
        } else if (widgets.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(grid.gutter)) {
                homePackWidgets(widgets).forEach { row ->
                    key(row.joinToString("+") { it.id }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(grid.gutter)) {
                            row.forEach { widget ->
                                val size = grid.size(widget.size)
                                if (isEditing) {
                                    HomeEditableWidget(
                                        widget = widget,
                                        seed = widgets.indexOfFirst { it.id == widget.id },
                                        isSelected = selectedID == widget.id,
                                        onSelect = { onSelect(widget) },
                                        onRemove = { remove(widget) },
                                        width = size.width, height = size.height,
                                    )
                                } else {
                                    // The widget owns its touch-and-hold menu (Open, Size, Edit Home, Remove);
                                    // "Edit Home" there sets router.homeEditRequested, which HomeScreen watches.
                                    WidgetView(widget, Modifier.size(size.width, size.height))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeWidgetsHeader(isEditing: Boolean, empty: Boolean, onEdit: () -> Unit) {
    if (isEditing) {
        Row(Modifier.fillMaxWidth().heightIn(min = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("YOUR WIDGETS", style = DNTextStyle.Overline.style, color = DNColor.textSecondary, modifier = Modifier.weight(1f).semantics { heading() })
            Text(
                if (empty) "Add one to start" else "Select one to move it",
                style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textSecondary,
            )
        }
    } else {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Your widgets", style = DNTextStyle.Section.style, color = DNColor.textPrimary, modifier = Modifier.weight(1f).semantics { heading() })
            if (!empty) HomeLinkButton("Edit", description = "Edit Home", onClick = onEdit)
        }
    }
}

// MARK: Editable widget (Edit Home)

@Composable
private fun HomeEditableWidget(
    widget: HomeWidget,
    seed: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
) {
    val store = LocalStore.current
    val label = "${widget.kind.title} widget, ${HomeWidgetSizes.spoken(widget.size)}"
    Box(
        Modifier.size(width, height)
            .zIndex(if (isSelected) 1f else 0f)
            .homeWobble(active = !isSelected, seed = seed)
            .homeFocusRing(isSelected, DNRadius.widget),
    ) {
        // A preview: no taps, menus or timers inside; a tap anywhere selects it.
        CompositionLocalProvider(LocalWidgetPreview provides true) {
            WidgetView(widget, Modifier.size(width, height))
        }
        Box(
            Modifier.matchParentSize().clip(RoundedCornerShape(DNRadius.widget))
                .clickable(role = Role.Button, onClickLabel = "Select to move or resize") { onSelect() }
                .clearAndSetSemantics {
                    contentDescription = label
                    role = Role.Button
                    selected = isSelected
                    onClick(label = "Select to move or resize") { onSelect(); true }
                    customActions = listOf(
                        CustomAccessibilityAction("Remove") { onRemove(); true },
                        CustomAccessibilityAction("Move earlier") { store.homeMoveWidget(widget.id, -1); true },
                        CustomAccessibilityAction("Move later") { store.homeMoveWidget(widget.id, 1); true },
                    )
                },
        )
        HomeRemoveBadge(
            "Remove ${widget.kind.title}", onRemove,
            Modifier.align(Alignment.TopStart).offset(x = (-18).dp, y = (-18).dp),
        )
    }
}

/** The grey "−" badge (28 dp in a 48 dp target) at the top-left corner of a widget or favorite. */
@Composable
internal fun HomeRemoveBadge(description: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 28.dp) {
    Box(
        modifier.size(48.dp).clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(size + 6.dp).clip(CircleShape).background(DNColor.surfaceSecondary).padding(3.dp)
                .clip(CircleShape).background(DNColor.neutral300),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Remove, contentDescription = null, tint = DNColor.textPrimary, modifier = Modifier.size(size * 0.6f)) }
    }
}

// MARK: Suggested widget (JM5-0)

/**
 * One suggestion at a time: the widget itself (a preview) with a + badge, the reason underneath
 * and "Not now". One tap adds it where it stands.
 */
@Composable
private fun HomeWidgetSuggestion(kind: WidgetKind, grid: HomeGrid, onAdd: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val store = LocalStore.current
    val reason = when (kind) {
        WidgetKind.FamilyVisas -> store.state.familyVisas?.let { fv ->
            val days = maxOf(0, DemoDate.daysFromToday(fv.expiresOn))
            val n = fv.dueMemberIDs.size
            "$n ${if (n == 1) "visa expires" else "visas expire"} in $days days"
        } ?: "Your family's visas in one place"
        WidgetKind.BillsAndFines -> "3 bill accounts are linked"
        is WidgetKind.Journey -> "You started this journey"
        is WidgetKind.VisaTracker -> "Follow your request"
        else -> "Picked for you"
    }
    val interaction = remember { MutableInteractionSource() }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.clearAndSetSemantics {
                    contentDescription = "Suggested widget: ${kind.title}. $reason"
                    role = Role.Button
                    onClick(label = "Add to Home") { onAdd(); true }
                    customActions = listOf(CustomAccessibilityAction("Not now") { onDismiss(); true })
                },
        ) {
            Box(Modifier.size(grid.wide, grid.small).homePressScale(interaction)) {
                CompositionLocalProvider(LocalWidgetPreview provides true) {
                    WidgetView(HomeWidget("suggested", kind, WidgetSize.Medium), Modifier.size(grid.wide, grid.small))
                }
                Box(
                    Modifier.matchParentSize().clip(RoundedCornerShape(DNRadius.widget))
                        .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onAdd),
                )
            }
            Box(
                Modifier.align(Alignment.TopStart).offset(x = (-17).dp, y = (-17).dp).size(48.dp).clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(DNColor.surfaceSecondary).padding(3.dp)
                        .clip(CircleShape).background(DNColor.actionAccent),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Add, contentDescription = null, tint = DNColor.textOnAccent, modifier = Modifier.size(18.dp)) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Suggested · $reason", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary,
                modifier = Modifier.weight(1f).clearAndSetSemantics { },
            )
            Box(
                Modifier.offset(x = 8.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onDismiss).padding(horizontal = 8.dp)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                Text("Not now", style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textLink)
            }
        }
    }
}

// MARK: Empty canvas (A3J-0)

@Composable
private fun HomeWidgetsEmpty() {
    val router = LocalRouter.current
    Column(
        Modifier.fillMaxWidth().homeDashedBorder(DNRadius.widget).padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Make this space yours", style = DNTextStyle.Title3.style.copy(fontWeight = FontWeight.Bold),
            color = DNColor.textPrimary, textAlign = TextAlign.Center,
        )
        Text(
            "Add live widgets for parking, Salik, the Metro, prayer times and more.",
            style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary, textAlign = TextAlign.Center,
        )
        DNButton(
            "Add widget", onClick = { router.present(SheetRoute.WidgetGallery) }, size = DNButtonSize.Medium,
            fullWidth = false, icon = Icons.Rounded.Add, modifier = Modifier.padding(top = 8.dp),
        )
    }
}

// MARK: Footer: Add widget slot, pull hint, pull indicator

@Composable
internal fun HomeWidgetsFooter(hintAlpha: Float, modifier: Modifier = Modifier) {
    val router = LocalRouter.current
    Column(
        modifier.fillMaxWidth().padding(horizontal = DNSpace.insetPage),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        val interaction = remember { MutableInteractionSource() }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 96.dp).homePressScale(interaction)
                .homeDashedBorder(DNRadius.widget).clip(RoundedCornerShape(DNRadius.widget))
                .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button) { router.present(SheetRoute.WidgetGallery) }
                .semantics(mergeDescendants = true) { contentDescription = "Add widget. Opens the widget gallery" },
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).dnElevation(22.dp).clip(CircleShape).background(DNColor.surfacePrimary),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Add, contentDescription = null, tint = DNColor.textLink, modifier = Modifier.size(22.dp)) }
            Text("Add widget", style = DNTextStyle.Headline.style, color = DNColor.textLink)
        }
        Column(
            Modifier.alpha(hintAlpha).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { router.present(SheetRoute.WidgetGallery) }
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .clearAndSetSemantics { contentDescription = "Browse widgets"; role = Role.Button },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null, tint = DNColor.textSecondary, modifier = Modifier.size(20.dp))
            Text("Pull past the end to browse widgets", style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textSecondary)
        }
    }
}

/**
 * Revealed under the end of Home while pulling past it (A7N-0). The ring fills to the
 * threshold; past it, a soft haptic and "Release to add widgets".
 */
@Composable
internal fun HomePullIndicator(progress: Float, armed: Boolean, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(if (armed) 1.06f else 1f, DNMotion.snappy(), label = "pull-scale")
    Column(
        modifier.clearAndSetSemantics { },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(64.dp).scale(scale), contentAlignment = Alignment.Center) {
            Box(
                Modifier.requiredSize(128.dp).background(
                    Brush.radialGradient(listOf(DNColor.actionAccent.copy(alpha = if (armed) 0.16f else 0.08f), Color.Transparent)),
                ),
            )
            Canvas(Modifier.fillMaxSize()) {
                val w = 4.dp.toPx()
                val inset = w / 2
                drawCircle(DNColor.neutral200, radius = size.minDimension / 2 - inset, style = Stroke(w))
                drawArc(
                    DNColor.actionAccent, startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0.001f, 1f), useCenter = false,
                    topLeft = Offset(inset, inset), size = Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round),
                )
            }
            Box(
                Modifier.padding(8.dp).fillMaxSize().shadow(8.dp, CircleShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.12f))
                    .clip(CircleShape).background(DNColor.surfacePrimary),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.DashboardCustomize, contentDescription = null, tint = DNColor.textPrimary, modifier = Modifier.size(22.dp)) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (armed) "Release to add widgets" else "Pull to add widgets", style = DNTextStyle.Headline.style, color = DNColor.textPrimary)
            Text("Opens the widget gallery", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        }
    }
}

// MARK: Edit Home: pinned anchors, top bar and bottom bar

/** "Pinned · always first": Needs you and Favorites can't move. */
@Composable
internal fun HomeEditPinnedGroup(modifier: Modifier = Modifier) {
    val store = LocalStore.current
    val names = store.state.favorites.map { it.title }
    Column(modifier.fillMaxWidth().padding(horizontal = DNSpace.insetPage), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PINNED · ALWAYS FIRST", style = DNTextStyle.Overline.style, color = DNColor.textSecondary, modifier = Modifier.semantics { heading() })
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.card)).background(DNColor.surfaceOnWash).padding(horizontal = 16.dp)) {
            HomePinnedRow(Icons.Rounded.NotificationsNone, Hue.Sand, "Needs you", "Appears when something needs you")
            DNRowDivider(inset = 44.dp)
            HomePinnedRow(Icons.Rounded.FavoriteBorder, Hue.Lilac, "Favorites", if (names.isEmpty()) "Your shortcuts" else names.joinToString(", "))
        }
    }
}

@Composable
private fun HomePinnedRow(icon: ImageVector, hue: Hue, title: String, detail: String) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clearAndSetSemantics { contentDescription = "$title, pinned. $detail" },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(hue.start), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = hue.ink, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = DNTextStyle.Headline.style, color = DNColor.textPrimary)
            Text(detail, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = DNColor.iconSecondary, modifier = Modifier.size(16.dp))
    }
}

/** Edit Home's top bar: Add · Edit Home · Done (in place of the main header). */
@Composable
internal fun HomeEditTopBar(onAdd: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    // The title takes the space between the two buttons, so large text wraps instead of running under them.
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.heightIn(min = 48.dp).dnElevation(24.dp).clip(CircleShape)
                .background(DNColor.surfacePrimary.copy(alpha = 0.92f))
                .clickable(role = Role.Button, onClick = onAdd)
                .padding(start = 14.dp, end = 18.dp)
                .semantics(mergeDescendants = true) { contentDescription = "Add widget" },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = DNColor.textPrimary, modifier = Modifier.size(22.dp))
            Text("Add", style = DNTextStyle.Button.style, color = DNColor.textPrimary)
        }
        Text(
            "Edit Home", style = DNTextStyle.Headline.style, color = DNColor.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() },
        )
        Box(
            Modifier.heightIn(min = 48.dp).dnElevation(24.dp).clip(CircleShape).background(DNColor.actionAccent)
                .clickable(role = Role.Button, onClick = onDone).padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Done", style = DNTextStyle.Button.style, color = DNColor.textOnAccent) }
    }
}

/**
 * The tonal bar at the bottom of Edit Home: "Add widget" when nothing is selected;
 * Earlier · Later · Size for the selected widget (moving without dragging, I9L-0).
 */
@Composable
internal fun HomeEditBottomBar(selected: HomeWidget?, modifier: Modifier = Modifier, sizeMenuInitially: Boolean = false) {
    val router = LocalRouter.current
    Box(modifier.fillMaxWidth().padding(horizontal = DNSpace.insetPage), contentAlignment = Alignment.Center) {
        if (selected == null) {
            Row(
                Modifier.heightIn(min = 56.dp).dnElevation(28.dp).clip(CircleShape).background(DNColor.materialTonal)
                    .clickable(role = Role.Button) { router.present(SheetRoute.WidgetGallery) }
                    .padding(horizontal = 24.dp)
                    .semantics(mergeDescendants = true) { },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = DNColor.textLink, modifier = Modifier.size(22.dp))
                Text("Add widget", style = DNTextStyle.Headline.style, color = DNColor.textLink)
            }
        } else {
            HomeMoveBar(selected, sizeMenuInitially)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeMoveBar(widget: HomeWidget, sizeMenuInitially: Boolean) {
    val store = LocalStore.current; val view = LocalView.current
    val widgets = store.state.widgets
    val index = widgets.indexOfFirst { it.id == widget.id }
    val sizes = HomeWidgetSizes.allowed(widget.kind)
    val next = HomeWidgetSizes.next(widget)
    var sizeMenu by remember(widget.id) { mutableStateOf(false) }
    LaunchedEffect(widget.id, sizeMenuInitially) { if (sizeMenuInitially) sizeMenu = true }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).dnElevation(32.dp).clip(CircleShape).background(DNColor.materialTonal)
            .padding(horizontal = 8.dp)
            .semantics { contentDescription = "Move or resize ${widget.kind.title}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeBarSegment(Modifier.weight(1f), enabled = index > 0, onClick = { DNHaptics.selection(view); store.homeMoveWidget(widget.id, -1) }, description = "Move earlier") {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = null, modifier = Modifier.size(20.dp), tint = LocalSegmentColor.current)
            Text("Earlier", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = LocalSegmentColor.current)
        }
        HomeBarDivider()
        HomeBarSegment(Modifier.weight(1f), enabled = index in 0 until widgets.lastIndex, onClick = { DNHaptics.selection(view); store.homeMoveWidget(widget.id, 1) }, description = "Move later") {
            Text("Later", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = LocalSegmentColor.current)
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = LocalSegmentColor.current)
        }
        HomeBarDivider()
        // Tap: the next size (as in Paper, "Wide"). Touch and hold: every size.
        Box(Modifier.weight(1f)) {
            HomeBarSegment(
                Modifier.fillMaxWidth(),
                enabled = sizes.size >= 2,
                onClick = { DNHaptics.selection(view); store.resizeWidget(widget, next) },
                onLongClick = { DNHaptics.medium(view); sizeMenu = true },
                description = "Make it ${HomeWidgetSizes.spoken(next)}",
            ) {
                Icon(HomeWidgetSizes.icon(next), contentDescription = null, modifier = Modifier.size(18.dp), tint = LocalSegmentColor.current)
                Text(HomeWidgetSizes.label(next), style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = LocalSegmentColor.current)
            }
            DropdownMenu(expanded = sizeMenu, onDismissRequest = { sizeMenu = false }, shape = RoundedCornerShape(16.dp), containerColor = DNColor.surfacePrimary) {
                sizes.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(HomeWidgetSizes.label(s), style = DNTextStyle.Body.style) },
                        leadingIcon = { Icon(HomeWidgetSizes.icon(s), contentDescription = null) },
                        trailingIcon = { if (s == widget.size) Icon(Icons.Rounded.Check, contentDescription = "Current size", tint = DNColor.actionAccent) },
                        onClick = { sizeMenu = false; DNHaptics.selection(view); store.resizeWidget(widget, s) },
                    )
                }
            }
        }
    }
}

private val LocalSegmentColor = androidx.compose.runtime.compositionLocalOf { DNColor.textPrimary }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeBarSegment(
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
    description: String,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val color = if (enabled) DNColor.textPrimary else DNColor.textDisabled.copy(alpha = 0.6f)
    Row(
        modifier.heightIn(min = 48.dp).clip(CircleShape)
            .combinedClickable(enabled = enabled, role = Role.Button, onLongClick = onLongClick, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                if (enabled) {
                    onClick { onClick(); true }
                    if (onLongClick != null) onLongClick(label = "Choose a size") { onLongClick(); true }
                } else {
                    disabled()
                }
            },
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalSegmentColor provides color) { content() }
    }
}

@Composable
private fun HomeBarDivider() {
    Box(Modifier.width(1.dp).height(24.dp).background(DNColor.borderSubtle))
}
