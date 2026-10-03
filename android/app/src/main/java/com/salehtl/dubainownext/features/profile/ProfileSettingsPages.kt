package com.salehtl.dubainownext.features.profile

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsAccessibility
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.HomeInfo
import com.salehtl.dubainownext.data.PaymentMethod
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.HueIcon

// Simple pages pushed inside the profile sheet: Home, Payment methods, Notifications settings,
// Accessibility and display, Privacy policy and terms (ios/…/ProfileSettingsPages.swift).

/** A titled group with an optional footer, like an inset grouped section. */
@Composable
private fun ProfileSettingsGroup(header: String?, footer: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        header?.let { ProfileOverline(it) }
        ProfileGroup(content = content)
        footer?.let { ProfileFootnote(it) }
    }
}

// MARK: - Home

@Composable
internal fun ProfileHomePage() {
    val store = LocalStore.current
    val nav = profileNav()
    val home = store.state.home
    ProfileScreen(ProfilePage.Home.title) {
        if (home == null) {
            ProfileEmptyState(Icons.Rounded.Home, "No home linked", "Link your DEWA account or tenancy contract to see your home here.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HueIcon("house.fill", Hue.Sand, size = 56.dp)
                Text(
                    home.address, style = DNTextStyle.Title2.style.copy(fontWeight = FontWeight.Bold), color = DNColor.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "Linked to your DEWA account" + if (home.ejariExpiry != null) " and tenancy contract" else "",
                    style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary,
                )
            }
            ProfileFactsCard(profileHomeFacts(home))
            val services = listOf("pay-electricity-water-bill", "renew-tenancy-contract", "move-home").mapNotNull { Catalogue.service(it) }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileSection("Things you can do")
                ProfileGroup {
                    services.forEachIndexed { i, s ->
                        if (i > 0) ProfileDivider()
                        ProfileRow(s.name, onClick = { profileRouteForService(s.id)?.let(nav::push) }, subtitle = profileServiceSubtitle(s))
                    }
                }
            }
        }
    }
}

private fun profileHomeFacts(home: HomeInfo): List<Pair<String, String>> = buildList {
    home.makani?.let { add("Makani number" to it) }
    add("DEWA account" to home.dewaAccount)
    home.ejariExpiry?.let { add("Tenancy contract" to "Until ${DemoDate.full(it)}") }
    add("Information from" to "DEWA and DLD · updated today")
}

@Composable
private fun profileServiceSubtitle(s: CatalogueService): String {
    val store = LocalStore.current
    if (s.type == ServiceType.Journey) {
        store.state.journeys.firstOrNull { it.journeyID == s.id }?.let { j ->
            return "Started · stage ${j.stageIndex + 1} of ${s.stages?.size ?: 0} · ${j.headline}"
        }
    }
    return s.ownerLabel
}

// MARK: - Payment methods

@Composable
internal fun ProfilePaymentMethodsPage() {
    val store = LocalStore.current
    val nav = profileNav()
    ProfileScreen(ProfilePage.PaymentMethods.title) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileGroup {
                store.state.paymentMethods.forEachIndexed { i, m ->
                    if (i > 0) ProfileDivider(64.dp)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp).semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ProfileGreyDisc(icon = if (m == PaymentMethod.GooglePay) Icons.Rounded.Wallet else Icons.Rounded.CreditCard, size = 36.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(m.label, style = DNTextStyle.RowTitle.style, color = DNColor.textPrimary)
                            Text(if (i == 0) "Used first" else "Saved for payments", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
                        }
                        if (i == 0) {
                            // The supporting line already says "Used first"; the tick is decoration (no double announcement).
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = DNColor.actionAccent, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                ProfileDivider(64.dp)
                ProfileRow(
                    "Add a card", titleColor = DNColor.textLink, showsChevron = false,
                    onClick = {
                        nav.comingSoon(
                            ComingSoon(
                                title = "Add a card",
                                detail = "Adding a card comes with the payment flows in the next phase. Google Pay and your saved card work in this demo.",
                                symbol = "creditcard.fill", hue = Hue.Sky, kind = ComingSoon.Kind.NotInDemo,
                            ),
                        )
                    },
                ) { ProfileGreyDisc(icon = Icons.Rounded.Add, size = 36.dp) }
            }
            ProfileFootnote("You confirm every payment with your fingerprint. This is a demo: no money moves.")
        }
    }
}

// MARK: - Notifications settings

@Composable
internal fun ProfileNotificationSettingsPage() {
    val context = LocalContext.current
    val view = LocalView.current
    // 0 none · 1 from · 2 to. Demo hook: `profileDialog quietFrom` opens the time picker.
    var editing by remember { mutableIntStateOf(if (ProfileDebug.extra("profileDialog") == "quietFrom") 1 else 0) }
    ProfileScreen(ProfilePage.NotificationSettings.title) {
        ProfileSettingsGroup("Important", "We tell you when something is due, when a request needs you, and when there's a result.") {
            ProfilePrefToggle("due", "Due dates and renewals")
            ProfileDivider()
            ProfilePrefToggle("turn", "When it's your turn")
            ProfileDivider()
            ProfilePrefToggle("results", "Results and approvals")
            ProfileDivider()
            ProfilePrefToggle("payments", "Payments and receipts")
        }
        ProfileSettingsGroup("Optional", "Off until you turn them on. We never send advertising.") {
            ProfilePrefToggle("news", "City news and announcements")
            ProfileDivider()
            ProfilePrefToggle("tips", "Tips for using DubaiNow")
        }
        ProfileSettingsGroup(null, "Nothing makes a sound in quiet hours, except a payment you're making.") {
            ProfilePrefToggle("quiet", "Quiet hours")
            AnimatedVisibility(
                visible = ProfilePrefs.bool("quiet"),
                enter = expandVertically(DNMotion.smooth()) + fadeIn(DNMotion.base()),
                exit = shrinkVertically(DNMotion.smooth()) + fadeOut(DNMotion.base()),
            ) {
                Column {
                    ProfileDivider()
                    ProfileRow("From", onClick = { DNHaptics.selection(view); editing = 1 }, value = ProfilePrefs.clock(ProfilePrefs.quietFrom), showsChevron = false)
                    ProfileDivider()
                    ProfileRow("To", onClick = { DNHaptics.selection(view); editing = 2 }, value = ProfilePrefs.clock(ProfilePrefs.quietTo), showsChevron = false)
                }
            }
        }
        ProfileSettingsGroup(null, "To turn all DubaiNow notifications off, use your phone's Settings.") {
            ProfileRow(
                "Open Android settings", titleColor = DNColor.textLink, showsChevron = false,
                onClick = {
                    profileStart(
                        context,
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                },
            ) { ProfileRowIcon(Icons.Rounded.Settings, DNColor.textLink) }
        }
    }
    if (editing != 0) {
        ProfileTimeDialog(
            title = if (editing == 1) "Quiet hours from" else "Quiet hours to",
            minutes = if (editing == 1) ProfilePrefs.quietFrom else ProfilePrefs.quietTo,
            onDismiss = { editing = 0 },
            onConfirm = { m -> if (editing == 1) ProfilePrefs.updateQuiet(from = m) else ProfilePrefs.updateQuiet(to = m); editing = 0 },
        )
    }
}

@Composable
private fun ProfilePrefToggle(key: String, title: String) {
    val view = LocalView.current
    ProfileToggleRow(title, checked = ProfilePrefs.bool(key), onCheckedChange = { DNHaptics.selection(view); ProfilePrefs.setBool(key, it) })
}

/** The M3 time picker (clock dial) in a dialog, 24-hour like the rest of the app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTimeDialog(title: String, minutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(DNRadius.dialog), color = DNColor.surfaceContainer) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    title, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textSecondary,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                )
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = DNColor.surfaceSecondary, selectorColor = DNColor.actionAccent,
                        timeSelectorSelectedContainerColor = DNColor.actionAccentSubtle, timeSelectorSelectedContentColor = DNColor.textLink,
                        timeSelectorUnselectedContainerColor = DNColor.surfaceSecondary, timeSelectorUnselectedContentColor = DNColor.textPrimary,
                    ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textLink) }
                    TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                        Text("OK", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textLink)
                    }
                }
            }
        }
    }
}

// MARK: - Accessibility and display

@Composable
internal fun ProfileAccessibilityPage() {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    var tick by remember { mutableIntStateOf(0) }
    // Values are re-read when you come back from Settings.
    LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
    val values = remember(tick, config) { profileAccessibilityValues(context, config.fontScale, config) }

    ProfileScreen(ProfilePage.Accessibility.title) {
        Text(
            "DubaiNow follows your phone's settings. Change them in Settings and the app updates straight away.",
            style = DNTextStyle.Body.style, color = DNColor.textPrimary, modifier = Modifier.padding(horizontal = 4.dp),
        )
        ProfileSettingsGroup("On this phone now", null) {
            values.forEachIndexed { i, (label, value) ->
                if (i > 0) ProfileDivider()
                ProfileValueRow(label, value)
            }
        }
        ProfileSettingsGroup(null, "Dark mode comes in a later version of the demo.") {
            ProfileValueRow("Appearance", "Light")
        }
        ProfileSettingsGroup(null, "Font size is in Settings › Display › Display size and text.") {
            ProfileRow(
                "Open accessibility settings", titleColor = DNColor.textLink, showsChevron = false,
                onClick = { profileStart(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            ) { ProfileRowIcon(Icons.Rounded.SettingsAccessibility, DNColor.textLink) }
        }
    }
}

private fun profileAccessibilityValues(context: Context, fontScale: Float, config: android.content.res.Configuration): List<Pair<String, String>> {
    val size = when {
        fontScale < 0.95f -> "Smaller"
        fontScale <= 1.05f -> "Default"
        fontScale < 1.5f -> "Larger"
        else -> "Largest"
    }
    val bold = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) config.fontWeightAdjustment > 0 else false
    val animationsOff = try {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    } catch (_: Exception) { false }
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    val talkBack = am?.isTouchExplorationEnabled == true
    fun onOff(b: Boolean) = if (b) "On" else "Off"
    return listOf(
        "Font size" to size,
        "Bold text" to onOff(bold),
        "Remove animations" to onOff(animationsOff),
        "TalkBack" to onOff(talkBack),
    )
}

// MARK: - Privacy policy and terms

private val profilePrivacyParts = listOf(
    "What DubaiNow keeps" to "Your favorites, widgets and Home layout, your activity and unfinished drafts, and the accounts you link. You choose how long we keep your activity.",
    "What we ask government for" to "When you open something personal, DubaiNow asks the entity that holds it. For example, RTA for your vehicles and DEWA for your bills. You choose who it can ask in Linked accounts and privacy.",
    "Who we share it with" to "Only the entity that runs the service you're using. We never sell your information or use it for marketing.",
    "Your choices" to "You can download a copy of your information, delete your history or delete your account at any time from your profile.",
)

private val profileTermsParts = listOf(
    "Using DubaiNow" to "Each service is provided by the entity named beside it, under that entity's own terms. DubaiNow brings them together in one place.",
    "Payments" to "Fees, bills and fines are paid to the entity that charges them. Your receipts are kept in Activity.",
    "Changes" to "If these terms change, we'll tell you in the app before the change applies.",
)

@Composable
internal fun ProfilePrivacyPage() {
    ProfileScreen(ProfilePage.Privacy.title, spacing = 28.dp) {
        Text("Last updated 2 October 2026", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        ProfilePolicySection("Privacy policy", profilePrivacyParts)
        ProfilePolicySection("Terms of use", profileTermsParts)
        Box(Modifier.fillMaxWidth().background(DNColor.surfacePrimary, RoundedCornerShape(DNRadius.card)).padding(14.dp)) {
            Text("This is a demo. No real accounts, payments or government systems are connected.", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        }
    }
}

@Composable
private fun ProfilePolicySection(title: String, parts: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = DNTextStyle.Title2.style.copy(fontWeight = FontWeight.Bold), color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
        parts.forEach { (part, body) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(part, style = DNTextStyle.Headline.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
                Text(body, style = DNTextStyle.Body.style, color = DNColor.textSecondary)
            }
        }
    }
}
