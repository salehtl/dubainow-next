package com.salehtl.dubainownext.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.data.LinkedAccount
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.DNTextButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Linked accounts and privacy (Paper C1 `F0U-0`) and Delete your account (`WEH-0` → `WJ3-0` → `WLG-0`).

// MARK: - Linked accounts and privacy

private val profileKeepOptions = listOf("3 months", "1 year", "2 years", "5 years")

@Composable
internal fun ProfileLinkedAccountsPage() {
    val store = LocalStore.current
    val toasts = LocalToasts.current
    val view = LocalView.current
    val nav = profileNav()
    var asksDownload by remember { mutableStateOf(false) }
    var confirmsDeleteHistory by remember { mutableStateOf(false) }
    var keepMenu by remember { mutableStateOf(false) }

    ProfileScreen(ProfilePage.LinkedAccounts.title, spacing = 20.dp) {
        Text(
            "DubaiNow shows your things by asking government for them. You choose who it can ask. We never share your information for marketing.",
            style = DNTextStyle.Body.style, color = DNColor.textPrimary, modifier = Modifier.padding(horizontal = 4.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileOverline("Signed in with")
            ProfileGroup {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(DNColor.surfaceInverse),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = DNColor.textOnInverse, modifier = Modifier.size(24.dp)) }
                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("UAE PASS", style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
                        Text("Verified · fingerprint on this phone", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
                    }
                    DNTextButton("Manage", onClick = {
                        nav.comingSoon(
                            ComingSoon(
                                title = "Manage UAE PASS",
                                detail = "Your UAE PASS account is managed in the UAE PASS app. It isn't connected in this demo.",
                                symbol = "touchid", hue = Hue.Sky, kind = ComingSoon.Kind.NotInDemo,
                            ),
                        )
                    })
                }
            }
        }

        val accounts = store.state.linkedAccounts
        if (accounts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileOverline("Who DubaiNow can ask")
                ProfileGroup {
                    accounts.forEachIndexed { i, a ->
                        if (i > 0) ProfileDivider(68.dp)
                        ProfileToggleRow(
                            profileEntityName(a), checked = a.isLinked, emphasised = true,
                            subtitle = if (a.isLinked) a.detail else "Not linked",
                            onCheckedChange = { on -> DNHaptics.selection(view); store.profileSetLinked(a.id, on) },
                        ) { ProfileGreyDisc(monogram = profileMonogram(a)) }
                    }
                }
                ProfileFootnote("Turning one off hides its widgets and reminders. It does not cancel anything you have with them.")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileOverline("Your information")
            ProfileGroup {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.DropdownList) { keepMenu = true }
                        .padding(start = 16.dp, end = 10.dp).semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Keep activity for", style = DNTextStyle.RowTitle.style, color = DNColor.textPrimary, modifier = Modifier.weight(1f))
                    Box {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ProfilePrefs.keepActivity, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = DNColor.textSecondary)
                        }
                        DropdownMenu(expanded = keepMenu, onDismissRequest = { keepMenu = false }, containerColor = DNColor.surfaceContainer) {
                            profileKeepOptions.forEach { o ->
                                DropdownMenuItem(
                                    text = { Text(o, style = DNTextStyle.Callout.style, color = DNColor.textPrimary) },
                                    onClick = { DNHaptics.selection(view); ProfilePrefs.updateKeepActivity(o); keepMenu = false },
                                    trailingIcon = if (o == ProfilePrefs.keepActivity) {
                                        { Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = DNColor.actionAccent) }
                                    } else null,
                                )
                            }
                        }
                    }
                }
                ProfileDivider()
                ProfileRow("Download a copy of your information", onClick = { asksDownload = true })
                ProfileDivider()
                ProfileRow("Delete your DubaiNow history", onClick = { confirmsDeleteHistory = true }, titleColor = DNColor.actionDestructive, showsChevron = false)
            }
        }
    }

    if (asksDownload) {
        ProfileDownloadCopyAlert(onDone = { asksDownload = false }, onSend = {
            asksDownload = false
            DNHaptics.success(view)
            toasts.show("We'll email you a link within 24 hours", icon = Icons.Rounded.Email)
        })
    }
    if (confirmsDeleteHistory) {
        ProfileAlert(
            title = "Delete your DubaiNow history?",
            text = "This removes your past payments and receipts from DubaiNow. Each entity keeps its own records.",
            confirm = "Delete history", destructive = true,
            onConfirm = {
                confirmsDeleteHistory = false
                store.profileDeleteHistory()
                DNHaptics.success(view)
                toasts.show("Your DubaiNow history is deleted")
            },
            onDismiss = { confirmsDeleteHistory = false },
        )
    }
}

/** Entities by their full names, as on the Paper screen. */
private fun profileEntityName(a: LinkedAccount): String = when (a.id) {
    "rta" -> "Roads and Transport Authority"
    "dewa" -> "Dubai Electricity and Water"
    "police" -> "Dubai Police"
    "gdrfa" -> "Residency and Foreign Affairs"
    "dha" -> "Dubai Health Authority"
    else -> a.name
}

private fun profileMonogram(a: LinkedAccount): String = when (a.id) {
    "rta" -> "RTA"; "dewa" -> "DEWA"; "police" -> "DP"; "salik" -> "SLK"; "parkin" -> "PKN"
    "gdrfa" -> "GDRFA"; "dha" -> "DHA"
    else -> a.name.take(4)
}

// MARK: - Delete your account

@Composable
internal fun ProfileDeleteAccountPage() {
    val store = LocalStore.current
    val router = LocalRouter.current
    val toasts = LocalToasts.current
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var confirms by remember { mutableStateOf(false) }
    var asksDownload by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    fun delete() {
        if (deleting) return
        deleting = true
        scope.launch {
            val ok = profileConfirmIdentity(context, "Delete your account", "Confirm you want to delete your DubaiNow account")
            if (!ok) { deleting = false; return@launch }
            DNHaptics.success(view)
            toasts.show("Your account is deleted. We've emailed you a confirmation.")
            router.reset()
            store.signOut()
        }
    }

    // Demo hooks: `profileDelete alert` shows step 2's dialog; `auto` runs 2 → 3; `confirming` shows the busy state.
    LaunchedEffect(Unit) {
        when (ProfileDebug.extra("profileDelete")) {
            "alert" -> { delay(800); confirms = true }
            "auto" -> { delay(800); delete() }
            "confirming" -> deleting = true
        }
    }

    ProfileScreen(ProfilePage.DeleteAccount.title, spacing = 20.dp) {
        Text(
            "This removes everything DubaiNow keeps about you, on every device. Your UAE PASS and your records with government stay as they are.",
            style = DNTextStyle.Body.style, color = DNColor.textPrimary, modifier = Modifier.padding(horizontal = 4.dp),
        )
        ProfileBulletCard(
            "What we delete", Icons.Rounded.DeleteOutline, DNColor.actionDestructive,
            listOf(
                "Your favorites, widgets and Home layout",
                "Your activity history and unfinished drafts",
                "The links to RTA, Salik, DEWA and the other entities",
                "Saved cards, family members and notification settings",
            ),
        )
        ProfileBulletCard(
            "What stays", Icons.Rounded.CheckCircleOutline, DNColor.textSecondary,
            listOf(
                "Your UAE PASS. Manage or close it in the UAE PASS app.",
                "Your visas, vehicles, bills, fines and receipts with each entity. The law says they keep them.",
            ),
        )
        profileRequestsNotice(store.profileActiveRequests)?.let { (title, body) ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.card)).background(DNColor.statusWarningSurface).padding(16.dp)
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(DNColor.surfacePrimary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PriorityHigh, contentDescription = null, tint = DNColor.statusWarningForeground, modifier = Modifier.size(16.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
                    Text(body, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                }
            }
        }
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(CircleShape).clickable(role = Role.Button) { asksDownload = true },
                contentAlignment = Alignment.Center,
            ) {
                Text("Download a copy of your information first", style = DNTextStyle.Headline.style, color = DNColor.textLink, textAlign = TextAlign.Center)
            }
            ProfileDestructiveButton("Delete your account", busy = deleting) { DNHaptics.warning(view); confirms = true }
        }
    }

    if (asksDownload) {
        ProfileDownloadCopyAlert(onDone = { asksDownload = false }, onSend = {
            asksDownload = false
            DNHaptics.success(view)
            toasts.show("We'll email you a link within 24 hours", icon = Icons.Rounded.Email)
        })
    }
    if (confirms) {
        ProfileAlert(
            title = "Delete your account?",
            text = "This can't be undone. You'll confirm with your fingerprint, and you'll be signed out on every device.",
            confirm = "Delete", destructive = true,
            onConfirm = { confirms = false; delete() },
            onDismiss = { confirms = false },
        )
    }
}

private fun profileRequestsNotice(active: List<com.salehtl.dubainownext.data.RequestItem>): Pair<String, String>? {
    val first = active.firstOrNull() ?: return null
    val title = if (active.size == 1) "1 request is still in progress" else "${active.size} requests are still in progress"
    var body = "Your request to ${ProfileText.lowerFirst(first.title)} carries on with ${first.entity}"
    body += when (active.size) {
        1 -> "."
        2 -> ", and 1 other with its entity."
        else -> ", and ${active.size - 1} others with their entities."
    }
    return title to "$body Updates will come by SMS and email instead."
}

@Composable
private fun ProfileBulletCard(title: String, icon: ImageVector, tint: Color, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileOverline(title)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.listGroup)).background(DNColor.surfacePrimary)
                .padding(DNSpace.md).semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(top = 1.dp).size(20.dp))
                    Text(item, style = DNTextStyle.Callout.style, color = DNColor.textPrimary, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
