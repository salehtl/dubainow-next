package com.salehtl.dubainownext.features.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Drafts
import androidx.compose.material.icons.rounded.Markunread
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.PushedHeader
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.NotificationItem
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.DNRowDivider
import com.salehtl.dubainownext.design.components.HueIcon
import com.salehtl.dubainownext.design.components.dnCanvasBackground
import kotlinx.coroutines.delay

/**
 * Notifications, grouped Today / Earlier with unread dots. Shown as the bell sheet ([inSheet]) and
 * pushed (Route.Notifications). Tapping one marks it read and opens its link: pushed on the tab, or —
 * from the bell sheet — the sheet closes and the link opens on the tab underneath.
 */
@Composable
fun NotificationsScreen(inSheet: Boolean) {
    val store = LocalStore.current
    val router = LocalRouter.current
    val view = LocalView.current
    val all = store.state.notifications.sortedByDescending { it.date }
    val today = all.filter { DemoDate.daysFromToday(it.date) == 0 }
    val earlier = all.filter { DemoDate.daysFromToday(it.date) != 0 }
    val unread = store.unreadCount

    fun open(n: NotificationItem) {
        store.markRead(n.id)
        val link = n.link ?: return
        if (inSheet) profileOpenOnTab(router, link) else router.push(link)
    }
    val markAll: () -> Unit = { DNHaptics.selection(view); store.markAllRead() }

    // Demo hook: `profileOpenNotification <id>` taps a notification on launch.
    LaunchedEffect(Unit) {
        val id = ProfileDebug.extra("profileOpenNotification") ?: return@LaunchedEffect
        val n = store.state.notifications.firstOrNull { it.id == id } ?: return@LaunchedEffect
        delay(600)
        open(n)
    }

    Column(if (inSheet) Modifier.fillMaxWidth() else Modifier.fillMaxSize().dnCanvasBackground()) {
        if (inSheet) {
            ProfileSheetTitleRow("Notifications", onClose = { router.dismissSheet() }) {
                ProfileMarkAllRead(enabled = unread > 0, onClick = markAll)
            }
        } else {
            PushedHeader()
        }
        LazyColumn(
            if (inSheet) Modifier.fillMaxWidth() else Modifier.weight(1f),
            contentPadding = PaddingValues(start = DNSpace.insetPage, end = DNSpace.insetPage, top = 4.dp, bottom = if (inSheet) 24.dp else profileBottomBarClearance()),
        ) {
            if (!inSheet) {
                item(key = "title") {
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Notifications", style = DNTextStyle.LargeTitle.style, color = DNColor.textPrimary, modifier = Modifier.weight(1f).semantics { heading() })
                        ProfileMarkAllRead(enabled = unread > 0, onClick = markAll)
                    }
                }
            }
            if (all.isEmpty()) {
                item(key = "empty") {
                    ProfileEmptyState(
                        Icons.Rounded.NotificationsNone, "You're all caught up",
                        "We'll tell you when something is due, when it's your turn, and when there's a result.",
                        Modifier.padding(bottom = 40.dp),
                    )
                }
            } else if (unread == 0) {
                item(key = "caught-up") {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = 4.dp).semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = DNColor.statusSuccessForeground, modifier = Modifier.size(18.dp))
                        Text("You're all caught up", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Medium), color = DNColor.statusSuccessForeground)
                    }
                }
            }
            listOf("Today" to today, "Earlier" to earlier).forEach { (title, items) ->
                if (items.isEmpty()) return@forEach
                item(key = "h-$title") {
                    ProfileOverline(title, Modifier.padding(top = 12.dp, bottom = 8.dp))
                }
                itemsIndexed(items, key = { _, n -> n.id }) { i, n ->
                    val shape = profileGroupItemShape(i, items.size)
                    Column(Modifier.clip(shape).background(DNColor.surfacePrimary, shape)) {
                        if (i > 0) DNRowDivider(inset = 76.dp)
                        ProfileNotificationRow(n, onOpen = { open(n) })
                    }
                }
            }
        }
    }
}

private fun profileGroupItemShape(index: Int, count: Int): Shape {
    val r = DNRadius.listGroup
    val top = if (index == 0) r else 0.dp
    val bottom = if (index == count - 1) r else 0.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/** "Mark all read" — greyed out and announced as disabled when everything is read. */
@Composable
private fun ProfileMarkAllRead(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 48.dp).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Mark all read", style = DNTextStyle.Callout.style, color = if (enabled) DNColor.textLink else DNColor.textDisabled)
    }
}

/**
 * One notification. Tap opens it (and marks it read); swipe right toggles read / unread, like the
 * iOS leading swipe; long press shows the same action in a menu, and TalkBack lists it in Actions.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ProfileNotificationRow(item: NotificationItem, onOpen: () -> Unit) {
    val store = LocalStore.current
    val view = LocalView.current
    var menu by remember { mutableStateOf(false) }
    val time = if (DemoDate.daysFromToday(item.date) == 0) DemoDate.time(item.date) else DemoDate.relative(item.date)
    val dot by animateColorAsState(if (item.isRead) Color.Transparent else DNColor.actionAccent, DNMotion.base(), label = "dot")
    val toggleRead = { if (item.isRead) store.profileMarkUnread(item.id) else store.markRead(item.id) }
    val currentToggle by rememberUpdatedState(toggleRead)
    val label = "${if (item.isRead) "" else "Unread. "}${item.title}. ${item.body}. $time"

    // A full swipe to the end toggles read state, then the row springs back.
    val swipe = rememberSwipeToDismissBoxState(positionalThreshold = { it * 0.35f })
    LaunchedEffect(swipe.currentValue) {
        if (swipe.currentValue == SwipeToDismissBoxValue.StartToEnd) {
            DNHaptics.selection(view)
            currentToggle()
            swipe.reset()
        }
    }
    // Demo hook: `profileSwipeNotification <id>` swipes that row once on launch.
    LaunchedEffect(Unit) {
        if (ProfileDebug.extra("profileSwipeNotification") != item.id) return@LaunchedEffect
        delay(900)
        swipe.dismiss(SwipeToDismissBoxValue.StartToEnd)
    }

    Box {
        SwipeToDismissBox(
            state = swipe,
            enableDismissFromEndToStart = false,
            backgroundContent = {
                val swiping = swipe.dismissDirection == SwipeToDismissBoxValue.StartToEnd
                Row(
                    Modifier.fillMaxSize().background(if (swiping) DNColor.actionAccent else Color.Transparent).padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (swiping) {
                        Icon(if (item.isRead) Icons.Rounded.Markunread else Icons.Rounded.Drafts, contentDescription = null, tint = DNColor.textOnAccent)
                        Text(if (item.isRead) "Unread" else "Read", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textOnAccent)
                    }
                }
            },
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 72.dp).background(DNColor.surfacePrimary)
                    .combinedClickable(onClick = onOpen, onLongClick = { DNHaptics.medium(view); menu = true })
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
                    .clearAndSetSemantics {
                        contentDescription = label
                        role = Role.Button
                        onClick(label = if (item.link == null) "Mark as read" else "Open") { onOpen(); true }
                        customActions = listOf(CustomAccessibilityAction(if (item.isRead) "Mark as unread" else "Mark as read") { toggleRead(); true })
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.padding(top = 16.dp).size(8.dp).clip(CircleShape).background(dot))
                HueIcon(item.symbol, item.hue, size = 40.dp, modifier = Modifier.padding(end = 4.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            item.title, style = DNTextStyle.RowTitle.style.copy(fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.SemiBold),
                            color = DNColor.textPrimary, modifier = Modifier.weight(1f).alignByBaseline(),
                        )
                        Text(time, style = DNTextStyle.Caption1.style, color = if (item.isRead) DNColor.textSecondary else DNColor.textLink, modifier = Modifier.alignByBaseline())
                    }
                    Text(item.body, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = DNColor.surfaceContainer) {
            DropdownMenuItem(
                text = { Text(if (item.isRead) "Mark as unread" else "Mark as read", style = DNTextStyle.Callout.style, color = DNColor.textPrimary) },
                leadingIcon = { Icon(if (item.isRead) Icons.Rounded.Markunread else Icons.Rounded.Drafts, contentDescription = null, tint = DNColor.textPrimary) },
                onClick = { menu = false; DNHaptics.selection(view); toggleRead() },
            )
        }
    }
}
