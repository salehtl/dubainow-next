package com.salehtl.dubainownext.features.activity

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DraftItem
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.NeedsYouItem
import com.salehtl.dubainownext.data.Receipt
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.ChipTone
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNButtonKind
import com.salehtl.dubainownext.design.components.DNButtonSize
import com.salehtl.dubainownext.design.components.HueIcon
import com.salehtl.dubainownext.design.components.SegmentedProgress
import com.salehtl.dubainownext.design.components.dnCard
import kotlinx.coroutines.flow.first

// MARK: - Filter chips

/** All · Needs you n · In progress n · Drafts n · History — M3 filter chips in our chip colours. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActivityFilterChips(selection: ActivityFilter, counts: Map<ActivityFilter, Int>, onSelect: (ActivityFilter) -> Unit) {
    val view = LocalView.current
    val rowState = rememberLazyListState()
    val inset = with(LocalDensity.current) { DNSpace.insetPage.roundToPx() }
    LaunchedEffect(selection) {
        // Keep the chosen chip in view (e.g. Drafts chosen from a launch extra or "See all").
        snapshotFlow { rowState.layoutInfo.totalItemsCount }.first { it > 0 }
        val index = ActivityFilter.entries.indexOf(selection)
        val info = rowState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        if (info == null || info.offset < 0 || info.offset + info.size > rowState.layoutInfo.viewportEndOffset - inset) {
            rowState.animateScrollToItem(index, -inset)
        }
    }
    LazyRow(
        Modifier.fillMaxWidth(),
        state = rowState,
        contentPadding = PaddingValues(horizontal = DNSpace.insetPage),
        horizontalArrangement = Arrangement.spacedBy(DNSpace.chipGap),
    ) {
        items(ActivityFilter.entries, key = { it.key }) { filter ->
            val selected = selection == filter
            val count = counts[filter]?.takeIf { it > 0 }
            FilterChip(
                selected = selected,
                onClick = { if (!selected) { DNHaptics.selection(view); onSelect(filter) } },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(filter.title, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold))
                        if (count != null) {
                            if (filter == ActivityFilter.NeedsYou) {
                                Box(
                                    Modifier.defaultMinSize(minWidth = 20.dp, minHeight = 20.dp).clip(CircleShape)
                                        .background(DNColor.actionDestructive).padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("$count", style = DNTextStyle.Caption1.style.copy(fontWeight = FontWeight.Bold), color = DNColor.textOnDestructive)
                                }
                            } else {
                                Text(
                                    "$count", style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (selected) DNColor.chipTextSelected else DNColor.chipCount,
                                )
                            }
                        }
                    }
                },
                leadingIcon = if (selected) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                shape = CircleShape,
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = DNColor.chipSurface, labelColor = DNColor.chipText,
                    selectedContainerColor = DNColor.chipSurfaceSelected, selectedLabelColor = DNColor.chipTextSelected,
                    selectedLeadingIconColor = DNColor.chipTextSelected,
                ),
                modifier = Modifier.semantics { if (count != null) contentDescription = "${filter.title}, $count" },
            )
        }
    }
}

// MARK: - Needs you card

/**
 * The big card for one Needs you item: a request step waiting for the resident, a bill or
 * fine to pay, a renewal or a journey stage. Same data as Home's Needs you.
 */
@Composable
internal fun ActivityNeedsYouCard(item: NeedsYouItem, onPrimary: () -> Unit, onSecondary: () -> Unit, modifier: Modifier = Modifier) {
    val large = activityLargeText()
    Column(
        modifier.fillMaxWidth().dnCard(DNRadius.brief).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val subtitle = if (item.detail.contains(item.entity, ignoreCase = true)) item.detail else "${item.detail} · ${item.entity}"
        val texts: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, style = DNTextStyle.Headline.style, color = DNColor.textPrimary)
                Text(subtitle, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                val due = item.payable?.due
                if (due != null) {
                    ActivityDueChip(DemoDate.dueLabel(due), urgent = DemoDate.daysFromToday(due) <= 3, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        val trailing: @Composable () -> Unit = {
            val amount = item.amount
            val due = item.due
            if (amount != null) {
                Text(Money.aed(amount), style = DNTextStyle.NumericUnit.style, color = DNColor.textPrimary, maxLines = 1)
            } else if (due != null) {
                ActivityDueChip(
                    ActivityFormat.dueChip(due), urgent = DemoDate.daysFromToday(due) <= 3,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = DemoDate.dueLabel(due) },
                )
            }
        }
        if (large) {
            Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HueIcon(item.symbol, item.hue, size = 44.dp)
                texts()
                trailing()
            }
        } else {
            Row(Modifier.semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HueIcon(item.symbol, item.hue, size = 44.dp)
                Box(Modifier.weight(1f)) { texts() }
                trailing()
            }
        }
        item.progress?.let { p ->
            SegmentedProgress(
                total = p.total, done = maxOf(p.current - 1, 0),
                doneColor = DNColor.actionAccent, currentColor = DNColor.statusWarningForeground,
            )
        }
        val secondary = item.secondaryAction
        if (large) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DNButton(item.primaryAction, onPrimary, size = DNButtonSize.Medium)
                if (secondary != null) DNButton(secondary, onSecondary, kind = DNButtonKind.Secondary, size = DNButtonSize.Medium)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DNButton(item.primaryAction, onPrimary, modifier = Modifier.weight(1f), size = DNButtonSize.Medium)
                if (secondary != null) DNButton(secondary, onSecondary, kind = DNButtonKind.Secondary, size = DNButtonSize.Medium, fullWidth = false)
            }
        }
    }
}

/** "3 days" / "Due tomorrow" — a tinted pill with no dot (Paper's Needs you chip). */
@Composable
internal fun ActivityDueChip(text: String, urgent: Boolean = true, modifier: Modifier = Modifier) {
    Box(
        modifier.heightIn(min = 26.dp).clip(CircleShape)
            .background(if (urgent) DNColor.statusWarningSurface else DNColor.surfaceSecondary)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), maxLines = 1,
            color = if (urgent) DNColor.statusWarningForeground else DNColor.textSecondary,
        )
    }
}

// MARK: - Status line (dot + text)

internal val ChipTone.activityColor: Color
    get() = when (this) {
        ChipTone.Warning -> DNColor.statusWarningForeground
        ChipTone.Info -> DNColor.statusInfoForeground
        ChipTone.Success -> DNColor.statusSuccessForeground
        ChipTone.Error -> DNColor.statusErrorForeground
        ChipTone.Neutral -> DNColor.textSecondary
    }

@Composable
internal fun ActivityStatusLine(status: ActivityStatus, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(status.tone.activityColor))
        Text(status.text, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.Medium), color = status.tone.activityColor)
    }
}

// MARK: - Rows

/** In progress: icon · title · status line (→ tracker), no chevron (Paper LOA-0). Sits inside a list group. */
@Composable
internal fun ActivityRequestRow(request: RequestItem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HueIcon(request.symbol, request.hue, size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(request.title, style = DNTextStyle.RowTitle.style, color = DNColor.listRowTitle)
            ActivityStatusLine(request.activityStatus())
        }
    }
}

/** Drafts in the All list: icon · title · "2 of 4 steps · saved 18 Sep" · Continue. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ActivityDraftRow(draft: DraftItem, isPinned: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, actions: List<CustomAccessibilityAction>) {
    Row(
        Modifier.fillMaxWidth().background(DNColor.surfacePrimary).heightIn(min = 72.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "More actions", onClickLabel = "Continue the draft")
            .semantics { customActions = actions }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HueIcon(draft.symbol, draft.hue, size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(draft.title, style = DNTextStyle.RowTitle.style, color = DNColor.listRowTitle)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isPinned) Icon(Icons.Rounded.PushPin, contentDescription = "Pinned", tint = DNColor.textSecondary, modifier = Modifier.size(13.dp))
                Text(
                    "${draft.stepsDone} of ${draft.stepsTotal} steps · saved ${DemoDate.short(draft.savedOn)}",
                    style = DNTextStyle.Footnote.style, color = DNColor.listRowSupporting,
                )
            }
        }
        Text("Continue", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textLink)
    }
}

/** Drafts filter and the Drafts screen: the expanded card (progress, what's next, Continue / Start over). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ActivityDraftCard(
    draft: DraftItem,
    isPinned: Boolean,
    onContinue: () -> Unit,
    onStartOver: () -> Unit,
    onLongClick: () -> Unit,
    actions: List<CustomAccessibilityAction>,
) {
    val large = activityLargeText()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.brief)).background(DNColor.surfacePrimary)
            .combinedClickable(onClick = onContinue, onClickLabel = "Continue the draft", onLongClick = onLongClick, onLongClickLabel = "More actions")
            .semantics { customActions = actions }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HueIcon(draft.symbol, draft.hue, size = 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(draft.title, style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
                Text(
                    listOfNotNull(draft.activityOwner, "saved ${DemoDate.short(draft.savedOn)}").joinToString(" · "),
                    style = DNTextStyle.Footnote.style, color = DNColor.textSecondary,
                )
            }
            if (isPinned) Icon(Icons.Rounded.PushPin, contentDescription = "Pinned", tint = DNColor.textSecondary, modifier = Modifier.size(18.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SegmentedProgress(total = draft.stepsTotal, done = draft.stepsDone, currentIsActive = false, doneColor = DNColor.actionAccent)
            Text(draft.activityNextStep, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        }
        if (large) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DNButton("Continue", onContinue, size = DNButtonSize.Medium)
                DNButton("Start over", onStartOver, kind = DNButtonKind.Secondary, size = DNButtonSize.Medium)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DNButton("Continue", onContinue, modifier = Modifier.weight(1f), size = DNButtonSize.Medium)
                DNButton("Start over", onStartOver, kind = DNButtonKind.Secondary, size = DNButtonSize.Medium, fullWidth = false)
            }
        }
    }
}

/** History: icon · title (×6) · date · entity · amount right-aligned. */
@Composable
internal fun ActivityHistoryRow(receipt: Receipt, onClick: () -> Unit) {
    val large = activityLargeText()
    val date = if (DemoDate.daysFromToday(receipt.date) >= -1) ActivityFormat.whenShort(receipt.date) else DemoDate.short(receipt.date)
    val subtitle = "$date · ${receipt.entity}"
    val amountText: @Composable () -> Unit = {
        Text(
            receipt.amount?.let { Money.aed(it) } ?: "Sent", style = DNTextStyle.NumericUnit.style, maxLines = 1,
            color = if (receipt.amount == null) DNColor.textSecondary else DNColor.textPrimary,
        )
    }
    val texts: @Composable (Modifier) -> Unit = { m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(receipt.shortTitle, style = DNTextStyle.RowTitle.style, color = DNColor.listRowTitle, modifier = Modifier.weight(1f, fill = false))
                val count = receipt.count
                if (count != null && count > 1) {
                    Box(
                        Modifier.heightIn(min = 20.dp).clip(CircleShape).background(DNColor.surfaceSecondary).padding(horizontal = 6.dp)
                            .semantics { contentDescription = "$count payments" },
                        contentAlignment = Alignment.Center,
                    ) { Text("×$count", style = DNTextStyle.Caption1.style, color = DNColor.textSecondary) }
                }
            }
            Text(subtitle, style = DNTextStyle.Footnote.style, color = DNColor.listRowSupporting)
        }
    }
    val rowModifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable(onClickLabel = "Open the receipt", onClick = onClick)
        .semantics(mergeDescendants = true) {}.padding(horizontal = 16.dp, vertical = 12.dp)
    if (large) {
        Column(rowModifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HueIcon(receipt.symbol, receipt.hue, size = 40.dp)
            texts(Modifier)
            amountText()
        }
    } else {
        Row(rowModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HueIcon(receipt.symbol, receipt.hue, size = 40.dp)
            texts(Modifier.weight(1f))
            amountText()
        }
    }
}

// MARK: - Headers and notices

/**
 * "Needs you · Pay both", "In progress · with government": the section title with a trailing
 * note or action, which moves under the title at the largest text sizes.
 */
@Composable
internal fun ActivitySectionHeader(title: String, modifier: Modifier = Modifier, accessory: (@Composable () -> Unit)? = null) {
    if (activityLargeText()) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = DNTextStyle.Section.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
            accessory?.invoke()
        }
    } else {
        Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = DNTextStyle.Section.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.weight(1f).widthIn(min = 8.dp))
            accessory?.invoke()
        }
    }
}

/** A quiet trailing note in a section header ("with government"). */
@Composable
internal fun ActivityHeaderNote(text: String) {
    Text(text, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
}

/** A trailing text action in a section header ("Export", "Pay both · AED 1,012.60"). */
@Composable
internal fun ActivityHeaderAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.heightIn(min = 48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textLink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** "OCTOBER" — month headers in History. */
@Composable
internal fun ActivityMonthHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title.uppercase(), style = DNTextStyle.Overline.style, color = DNColor.textGroupedHeader,
        modifier = modifier.padding(horizontal = 4.dp).semantics { heading() },
    )
}

/** The blue notice at the top of Drafts. */
@Composable
internal fun ActivityInfoNotice(text: String, icon: ImageVector = Icons.Rounded.Schedule, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DNColor.statusInfoSurface).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = DNColor.statusInfoForeground, modifier = Modifier.padding(top = 1.dp).size(18.dp))
        Text(text, style = DNTextStyle.Subheadline.style, color = DNColor.blue800)
    }
}

/** A quiet empty state for a filter with nothing in it. */
@Composable
internal fun ActivityEmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 16.dp).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = DNColor.iconTertiary, modifier = Modifier.size(36.dp))
        Text(title, style = DNTextStyle.Headline.style, color = DNColor.textPrimary, textAlign = TextAlign.Center)
        Text(message, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary, textAlign = TextAlign.Center)
    }
}

// MARK: - Drafts: swipe, menu, delete

/**
 * Swipe a draft left to delete it (asks first) or right to pin it, Material style. Touch and hold
 * opens the same actions in a menu, and TalkBack offers them as custom actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActivityDraftSwipe(
    draft: DraftItem,
    shape: Shape,
    onContinue: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable (onLongClick: () -> Unit, actions: List<CustomAccessibilityAction>) -> Unit,
) {
    val view = LocalView.current
    val pinned = ActivityDraftPins.isPinned(draft.id)
    var menu by remember { mutableStateOf(false) }
    // Where the finger last went down on this draft. The menu opens there, on the pressed card.
    var press by remember { mutableStateOf(Offset.Zero) }
    var lastAction by remember { mutableStateOf(0L) }
    val pin = {
        DNHaptics.selection(view)
        ActivityDraftPins.toggle(draft.id)
    }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            val now = System.currentTimeMillis()
            if (value != SwipeToDismissBoxValue.Settled && now - lastAction > 600) {
                lastAction = now
                if (value == SwipeToDismissBoxValue.EndToStart) onDelete() else pin()
            }
            false
        },
    )
    val actions = listOf(
        CustomAccessibilityAction(if (pinned) "Unpin" else "Pin") { pin(); true },
        CustomAccessibilityAction("Delete draft") { onDelete(); true },
    )
    Box(
        Modifier.pointerInput(Unit) {
            // Only watches: the card's own click, long press and swipe still get every event.
            awaitEachGesture { press = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).position }
        },
    ) {
        SwipeToDismissBox(
            state = state,
            // While its menu is open the draft keeps the pressed row colour, so it's clear which one the menu acts on.
            modifier = Modifier.clip(shape).drawWithContent {
                drawContent()
                if (menu) drawRect(DNColor.listRowSurfacePressed, blendMode = BlendMode.Multiply)
            },
            backgroundContent = {
                val direction = state.dismissDirection
                val deleting = direction == SwipeToDismissBoxValue.EndToStart
                if (direction != SwipeToDismissBoxValue.Settled) {
                    Row(
                        Modifier.fillMaxSize().background(if (deleting) DNColor.actionDestructive else DNColor.neutral500).padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (deleting) Arrangement.End else Arrangement.Start,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(if (deleting) Icons.Rounded.Delete else Icons.Rounded.PushPin, contentDescription = null, tint = DNColor.white, modifier = Modifier.size(22.dp))
                            Text(
                                if (deleting) "Delete" else if (pinned) "Unpin" else "Pin",
                                style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.white,
                            )
                        }
                    }
                }
            },
        ) {
            content({ menu = true }, actions)
        }
        // Anchored to a point at the finger, not the whole card: with no room below a card, M3 put the
        // menu above its top edge, over the previous draft.
        Box(Modifier.offset { press.round() }) {
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = DNColor.surfacePrimary) {
                DropdownMenuItem(
                    text = { Text("Continue", style = DNTextStyle.Body.style) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null) },
                    onClick = { menu = false; onContinue() },
                )
                DropdownMenuItem(
                    text = { Text(if (pinned) "Unpin" else "Pin", style = DNTextStyle.Body.style) },
                    leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                    onClick = { menu = false; pin() },
                )
                HorizontalDivider(color = DNColor.borderSubtle)
                DropdownMenuItem(
                    text = { Text("Delete draft", style = DNTextStyle.Body.style, color = DNColor.actionDestructive) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = DNColor.actionDestructive) },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

/** "Delete this draft?" — the M3 dialog; the destructive action is red and comes last. Then Undo. */
@Composable
internal fun ActivityDraftDeleteDialog(draft: DraftItem?, onDismiss: () -> Unit) {
    val d = draft ?: return
    val store = LocalStore.current
    val toasts = LocalToasts.current
    val view = LocalView.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DNColor.surfacePrimary,
        shape = RoundedCornerShape(DNRadius.dialog),
        title = { Text("Delete this draft?", style = DNTextStyle.Title2.style, color = DNColor.textPrimary) },
        text = {
            Text(
                "You'll lose the ${d.stepsDone} of ${d.stepsTotal} steps you've done for ${d.title}.",
                style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep it", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textLink) }
        },
        confirmButton = {
            TextButton(onClick = {
                val index = store.state.drafts.indexOfFirst { it.id == d.id }.coerceAtLeast(0)
                store.deleteDraft(d)
                DNHaptics.light(view)
                toasts.show("Draft deleted", undo = { store.activityRestoreDraft(d, index) })
                onDismiss()
            }) { Text("Delete draft", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.actionDestructive) }
        },
    )
}

// MARK: - Guest

/** Black UAE PASS pill with the fingerprint mark at its leading edge. */
@Composable
internal fun ActivityUAEPassButton(onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(56.dp).clip(CircleShape).background(DNColor.surfaceInverse)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Fingerprint, contentDescription = null, tint = DNColor.textOnInverse,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 20.dp).size(26.dp),
        )
        Text("Sign in with UAE PASS", style = DNTextStyle.Button.style, color = DNColor.textOnInverse)
    }
}

/** The mint History tile on the guest state and sheet. */
@Composable
internal fun ActivityGuestTile() {
    Box(
        Modifier.size(64.dp).clip(RoundedCornerShape(DNRadius.tile)).background(Hue.Mint.gradient),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.History, contentDescription = null, tint = Hue.Mint.ink, modifier = Modifier.size(30.dp))
    }
}

/** Guest opened Activity (Paper WZQ-0): requests, receipts and drafts belong to an account. */
@Composable
internal fun ActivityGuestSheetContent(onSignIn: () -> Unit, onNotNow: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = DNSpace.sheetPadding).padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ActivityGuestTile()
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Sign in to see your activity", style = DNTextStyle.Title2.style, color = DNColor.textPrimary, textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Your requests, receipts and drafts belong to your account. After you sign in, you come straight back here.",
                style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary, textAlign = TextAlign.Center,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ActivityUAEPassButton(onSignIn)
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onNotNow),
                contentAlignment = Alignment.Center,
            ) { Text("Not now", style = DNTextStyle.Body.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textLink) }
        }
    }
}

