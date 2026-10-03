package com.salehtl.dubainownext.features.profile

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import com.salehtl.dubainownext.app.AppTab
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.app.LocalInSheet
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.PushedHeader
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.app.Router
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.DocumentItem
import com.salehtl.dubainownext.data.Payable
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.data.Vehicle
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNIcons
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.Inter
import com.salehtl.dubainownext.design.components.ChipTone
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.HueIcon
import com.salehtl.dubainownext.design.components.DNRowDivider
import com.salehtl.dubainownext.design.components.TonalIconButton
import com.salehtl.dubainownext.design.components.dnCanvasBackground
import com.salehtl.dubainownext.design.components.dnCard
import com.salehtl.dubainownext.design.components.dnElevation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Shared building blocks for the Profile feature (profile sheet, things, settings pages,
// Happiness Meter, notifications). Mirrors ios/…/Features/Profile/ProfileShared.swift.
// Everything here is internal or private and prefixed `Profile` so nothing clashes after the merge.

// MARK: - Navigation

/** Pages that only exist inside the profile sheet (pushed on its own stack). */
internal enum class ProfilePage(val key: String, val title: String) {
    Home("home", "Your home"),
    PaymentMethods("paymentMethods", "Payment methods"),
    NotificationSettings("notificationSettings", "Notifications"),
    Accessibility("accessibility", "Accessibility and display"),
    LinkedAccounts("linkedAccounts", "Linked accounts and privacy"),
    Privacy("privacy", "Privacy policy and terms"),
    DeleteAccount("deleteAccount", "Delete your account");

    companion object {
        fun parse(s: String): ProfilePage? = entries.firstOrNull { it.key.equals(s.trim(), ignoreCase = true) }
    }
}

/** One screen on the profile sheet's own back stack. */
internal sealed interface ProfileEntry {
    data class Page(val page: ProfilePage) : ProfileEntry
    data class Thing(val route: Route) : ProfileEntry
}

/** Things (vehicles, documents, family) are this feature's screens, so they can push inside the sheet. */
internal fun Route.isProfileThing(): Boolean =
    this is Route.Vehicle || this == Route.Vehicles || this == Route.Documents || this is Route.Document || this == Route.Family

/**
 * How Profile screens move: inside the profile sheet's own stack (things and settings pages),
 * or on the tab's stack when the screen is pushed in a tab. Other features' screens (service
 * start pages, request tracker, history…) always open on the tab: the sheet closes first.
 */
internal abstract class ProfileNav {
    abstract val inSheet: Boolean
    abstract fun push(route: Route)
    abstract fun open(page: ProfilePage)
    abstract fun back()
    abstract fun comingSoon(item: ComingSoon)
}

private class ProfileTabNav(private val router: Router) : ProfileNav() {
    override val inSheet = false
    override fun push(route: Route) = router.push(route)
    override fun open(page: ProfilePage) = router.present(SheetRoute.Profile)
    override fun back() { router.pop() }
    override fun comingSoon(item: ComingSoon) = router.present(SheetRoute.ComingSoonSheet(item))
}

internal class ProfileSheetNav(
    private val router: Router,
    val stack: SnapshotStateList<ProfileEntry>,
    private val onComingSoon: (ComingSoon) -> Unit,
    /** Called with the popped screen's depth (1 = first pushed) so its saved state can be dropped. */
    private val onPopped: (Int, ProfileEntry) -> Unit = { _, _ -> },
) : ProfileNav() {
    override val inSheet = true
    override fun push(route: Route) {
        if (route.isProfileThing()) stack.add(ProfileEntry.Thing(route))
        else profileOpenOnTab(router, route)
    }
    override fun open(page: ProfilePage) { stack.add(ProfileEntry.Page(page)) }
    override fun back() {
        if (stack.isEmpty()) return
        val depth = stack.size
        onPopped(depth, stack.removeAt(stack.lastIndex))
    }
    override fun comingSoon(item: ComingSoon) = onComingSoon(item)
}

/** Closes whatever sheet is up and pushes [route] on the current tab (Home when on Search). */
internal fun profileOpenOnTab(router: Router, route: Route) {
    router.dismissSheet()
    if (router.tab == AppTab.Search) router.closeSearch()
    router.push(route)
}

internal val LocalProfileNav = compositionLocalOf<ProfileNav?> { null }

@Composable
internal fun profileNav(): ProfileNav {
    val router = LocalRouter.current
    return LocalProfileNav.current ?: remember(router) { ProfileTabNav(router) }
}

/** The route for a catalogue record (start page, bundle or journey). Null when it isn't in the catalogue. */
internal fun profileRouteForService(id: String): Route? {
    val s = Catalogue.service(id) ?: return null
    return when (s.type) {
        ServiceType.Standalone -> Route.Service(id)
        ServiceType.Bundle -> Route.Bundle(id)
        ServiceType.Journey -> Route.Journey(id)
    }
}

internal object ProfileComingSoons {
    val help = ComingSoon(
        title = "Get help",
        detail = "Help pages, live chat and a way to report a problem come in the next phase of DubaiNow.",
        symbol = "questionmark.circle.fill", hue = Hue.Sky, kind = ComingSoon.Kind.NotInDemo,
    )
    val language = ComingSoon(
        title = "Language",
        detail = "This demo is in English. Arabic, with the whole app laid out right to left, comes next.",
        symbol = "globe", hue = Hue.Sky, kind = ComingSoon.Kind.NotInDemo,
    )
}

// MARK: - Debug extras

/** Launch extras for demos and screenshots (see NOTES.md). Read once per process where it matters. */
internal object ProfileDebug {
    var launchApplied = false
    fun extra(name: String): String? = LaunchOptions.extras[name]?.takeIf { it.isNotBlank() }
    fun flag(name: String): Boolean = LaunchOptions.flag(name)
}

// MARK: - Store helpers (public DemoStore API only)

/** Unpaid fines across all the resident's vehicles. */
internal val DemoStore.profileFineCount: Int get() = state.vehicles.sumOf { it.fineIDs.size }

/** The unpaid fines on a vehicle (from Needs you). */
internal fun DemoStore.profileFines(vehicle: Vehicle): List<Payable> = payables.filter { it.id in vehicle.fineIDs }

internal fun DemoStore.profileMarkUnread(id: String) = markRead(id, read = false)

internal fun DemoStore.profileSetLinked(id: String, linked: Boolean) {
    state = state.copy(linkedAccounts = state.linkedAccounts.map { if (it.id == id) it.copy(isLinked = linked) else it })
}

internal fun DemoStore.profileDeleteHistory() { state = state.copy(history = emptyList()) }

/** Requests that are still running with an entity. */
internal val DemoStore.profileActiveRequests: List<RequestItem> get() = state.requests.filter { it.status != RequestItem.Status.Done }

internal object ProfileText {
    /** "Ahmed", "Ahmed and Layla", "Ahmed, Layla and Omar" (UK style, no serial comma). */
    fun join(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        else -> items.dropLast(1).joinToString(", ") + " and " + items.last()
    }

    fun lowerFirst(s: String): String = if (s.isEmpty()) s else s.substring(0, 1).lowercase() + s.substring(1)

    fun firstName(name: String): String = name.substringBefore(" ")

    /** "14 Mar 2027" */
    fun shortYear(d: java.time.LocalDateTime): String = "${DemoDate.short(d)} ${d.year}"
}

// MARK: - Settings kept on this phone (like iOS @AppStorage)

/** Notification settings and "Keep activity for". Shared by all residents, like the iOS app. */
internal object ProfilePrefs {
    private var prefs: SharedPreferences? = null
    private val bools = mutableStateMapOf(
        "due" to true, "turn" to true, "results" to true, "payments" to true,
        "news" to false, "tips" to false, "quiet" to true,
    )
    var keepActivity by mutableStateOf("2 years"); private set
    var quietFrom by mutableIntStateOf(22 * 60); private set
    var quietTo by mutableIntStateOf(7 * 60); private set

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("dn_profile", Context.MODE_PRIVATE)
        prefs = p
        bools.keys.toList().forEach { k -> if (p.contains(k)) bools[k] = p.getBoolean(k, bools[k] ?: false) }
        keepActivity = p.getString("keepActivity", keepActivity) ?: keepActivity
        quietFrom = p.getInt("quietFrom", quietFrom)
        quietTo = p.getInt("quietTo", quietTo)
    }

    fun bool(key: String): Boolean = bools[key] ?: false
    fun setBool(key: String, value: Boolean) { bools[key] = value; prefs?.edit { putBoolean(key, value) } }
    fun updateKeepActivity(value: String) { keepActivity = value; prefs?.edit { putString("keepActivity", value) } }
    fun updateQuiet(from: Int? = null, to: Int? = null) {
        from?.let { quietFrom = it; prefs?.edit { putInt("quietFrom", it) } }
        to?.let { quietTo = it; prefs?.edit { putInt("quietTo", it) } }
    }

    /** "Everything" when an optional kind is on, otherwise "Important only". */
    val summary: String get() = if (bool("news") || bool("tips")) "Everything" else "Important only"

    fun clock(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)
}

// MARK: - Fingerprint / face (BiometricPrompt)

internal tailrec fun Context.profileFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.profileFragmentActivity()
    else -> null
}

/**
 * Asks for the fingerprint or face when the phone has one enrolled. Never blocks the demo: with
 * nothing enrolled it shows "Confirming…" for 0.9 s and carries on. Returns false only when the
 * person cancels.
 */
internal suspend fun profileConfirmIdentity(context: Context, title: String, subtitle: String): Boolean {
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK
    val activity = context.profileFragmentActivity()?.takeIf {
        BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }
    if (activity == null) { delay(900); return true }
    return suspendCancellableCoroutine { cont ->
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (cont.isActive) cont.resume(true)
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_CANCELED
                if (cont.isActive) cont.resume(!cancelled)
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title).setSubtitle(subtitle).setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(authenticators).build()
        prompt.authenticate(info)
        cont.invokeOnCancellation { prompt.cancelAuthentication() }
    }
}

// MARK: - Intents

internal fun profileShare(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    profileStart(context, Intent.createChooser(send, null))
}

internal fun profileStart(context: Context, intent: Intent) {
    try { context.startActivity(intent) } catch (_: ActivityNotFoundException) { } catch (_: SecurityException) { }
}

// MARK: - Screen frame

/** True when the font size setting is very large (layouts stack instead of sitting side by side). */
@Composable
internal fun profileLargeText(): Boolean = LocalDensity.current.fontScale >= 1.5f

/**
 * A Profile screen: our pushed header in a tab (back · logo · actions), or the sheet's own back
 * row inside the profile sheet. Content scrolls; the bottom leaves room for the floating tab bar.
 */
@Composable
internal fun ProfileScreen(
    title: String,
    modifier: Modifier = Modifier,
    largeTitle: Boolean = false,
    wash: Hue? = null,
    spacing: Dp = 24.dp,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val nav = profileNav()
    val otherSheet = !nav.inSheet && LocalInSheet.current
    val scroll = rememberScrollState()
    Box(modifier.fillMaxSize().dnCanvasBackground()) {
        if (wash != null) {
            Box(Modifier.fillMaxWidth().height(380.dp).background(Brush.verticalGradient(listOf(wash.start, wash.start.copy(alpha = 0f)))))
        }
        Column(Modifier.fillMaxSize()) {
            when {
                nav.inSheet -> ProfileBackRow(title, onBack = nav::back, actions = actions)
                otherSheet -> ProfileSheetTitleRow(title, onClose = null, actions = actions)
                else -> PushedHeader(actions = actions)
            }
            Column(
                Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = DNSpace.insetPage).padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                if (largeTitle && !nav.inSheet && !otherSheet) {
                    Text(title, style = DNTextStyle.LargeTitle.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
                }
                content()
                if (nav.inSheet || otherSheet) Spacer(Modifier.height(16.dp))
                else Spacer(Modifier.height(profileBottomBarClearance()))
            }
        }
    }
    ProfileScrollHook(scroll)
}

/**
 * Room under the last item of a screen pushed in a tab, so it scrolls clear of the floating bottom
 * bar (64 dp bar + 12 dp gap above the gesture/navigation bar) with some air: works with gesture and
 * three-button navigation alike.
 */
@Composable
internal fun profileBottomBarClearance(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 96.dp

/** `profileScroll bottom|center|<px>` starts Profile scroll views there (screenshots only). */
@Composable
internal fun ProfileScrollHook(scroll: ScrollState) {
    LaunchedEffect(scroll) {
        val v = ProfileDebug.extra("profileScroll") ?: return@LaunchedEffect
        delay(450)
        when (v) {
            "bottom" -> scroll.scrollTo(scroll.maxValue)
            "center" -> scroll.scrollTo(scroll.maxValue / 2)
            else -> v.toIntOrNull()?.let { scroll.scrollTo(it.coerceAtMost(scroll.maxValue)) }
        }
    }
}

/** Inside the profile sheet: back arrow and the page title (the M3 small top app bar, our tonal circle). */
@Composable
internal fun ProfileBackRow(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TonalIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = onBack)
        Text(
            title, style = DNTextStyle.Title3.style, color = DNColor.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp).semantics { heading() },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** A sheet's own title row (Android: title at the start, close at the end — Paper M5). */
@Composable
internal fun ProfileSheetTitleRow(title: String, onClose: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = DNSpace.insetPage, end = 16.dp, bottom = 8.dp).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title, style = DNTextStyle.Title2.style.copy(fontWeight = FontWeight.Bold), color = DNColor.textPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).semantics { heading() },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
        if (onClose != null) {
            Spacer(Modifier.width(8.dp))
            TonalIconButton(Icons.Rounded.Close, "Close", onClick = onClose)
        }
    }
}

// MARK: - Titles

/** "Documents", "Your things", "Settings" — 20/26 bold. */
@Composable
internal fun ProfileSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = DNTextStyle.Title3.style.copy(fontWeight = FontWeight.Bold), color = DNColor.textPrimary, modifier = modifier.semantics { heading() })
}

/** "At a glance", "Things you can do" — 22/28 bold (Section). */
@Composable
internal fun ProfileSection(text: String, modifier: Modifier = Modifier) {
    Text(text, style = DNTextStyle.Section.style, color = DNColor.textPrimary, modifier = modifier.semantics { heading() })
}

/** "SIGNED IN WITH" — the small grouped header above a group. */
@Composable
internal fun ProfileOverline(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), style = DNTextStyle.Overline.style, color = DNColor.textGroupedHeader,
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp).semantics { heading() },
    )
}

@Composable
internal fun ProfileFootnote(text: String, modifier: Modifier = Modifier) {
    Text(text, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, modifier = modifier.padding(horizontal = 4.dp))
}

// MARK: - Groups and rows

/** A white rounded group (radius 24). Put [ProfileDivider] between rows. */
@Composable
internal fun ProfileGroup(modifier: Modifier = Modifier, elevated: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().dnElevation(DNRadius.listGroup, enabled = elevated)
            .clip(RoundedCornerShape(DNRadius.listGroup)).background(DNColor.surfacePrimary),
        content = content,
    )
}

@Composable
internal fun ProfileDivider(inset: Dp = 16.dp) = DNRowDivider(inset)

/**
 * A list row: leading view, title (+ supporting line), count badge and chevron. A [value]
 * ("English", "Important only", "22:00") shows under the title, as Paper's Android list rows do —
 * which also keeps the title readable at large font sizes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ProfileRow(
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    badge: Int? = null,
    badgeDescription: String? = null,
    emphasised: Boolean = false,
    titleColor: Color = DNColor.textPrimary,
    showsChevron: Boolean = false, // Paper LOA-0: Android navigation rows have no chevron
    onLongClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle == null && value == null) DNSpace.listRowMinHeight else DNSpace.listRowMinHeightTwoLine)
            .let {
                when {
                    onClick != null && onLongClick != null -> it.combinedClickable(role = Role.Button, onLongClick = onLongClick, onClick = onClick)
                    onClick != null -> it.clickable(role = Role.Button, onClick = onClick)
                    else -> it
                }
            }
            .padding(horizontal = DNSpace.listRowInsetInline, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                title, style = DNTextStyle.RowTitle.style.copy(fontWeight = if (emphasised) FontWeight.SemiBold else FontWeight.Medium),
                color = titleColor,
            )
            (subtitle ?: value)?.let { Text(it, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary) }
        }
        if (badge != null && badge > 0) {
            Box(
                Modifier.heightIn(min = 22.dp).widthIn(min = 22.dp).clip(CircleShape).background(DNColor.actionDestructive).padding(horizontal = 6.dp)
                    .clearAndSetSemantics { contentDescription = badgeDescription ?: "$badge" },
                contentAlignment = Alignment.Center,
            ) { Text("$badge", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DNColor.textOnDestructive) }
        }
        if (showsChevron) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = DNColor.listRowChevron, modifier = Modifier.size(22.dp))
        }
    }
}

/** A plain 24 dp icon slot for settings rows. */
@Composable
internal fun ProfileRowIcon(icon: ImageVector, tint: Color = DNColor.textPrimary) {
    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** A grey disc with a glyph (Payment methods) or a monogram (entities). */
@Composable
internal fun ProfileGreyDisc(icon: ImageVector? = null, monogram: String? = null, size: Dp = 40.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(DNColor.surfaceSecondary).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        if (icon != null) Icon(icon, contentDescription = null, tint = DNColor.textPrimary, modifier = Modifier.size(size * 0.5f))
        else if (monogram != null) {
            Text(monogram, fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = if (monogram.length > 3) 9.sp else 11.sp, color = DNColor.textPrimary, maxLines = 1)
        }
    }
}

/** M3 switch in our colours, with the check icon when on (Paper M2: state isn't carried by colour alone). */
@Composable
internal fun ProfileSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) {
    Switch(
        checked = checked, onCheckedChange = onCheckedChange,
        thumbContent = if (checked) { { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) } } else null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = DNColor.controlSwitchThumb, checkedTrackColor = DNColor.controlSwitchTrackOn,
            checkedIconColor = DNColor.controlSwitchTrackOn, checkedBorderColor = DNColor.controlSwitchTrackOn,
            uncheckedThumbColor = DNColor.controlSwitchOutline, uncheckedTrackColor = DNColor.controlSwitchTrackOff,
            uncheckedBorderColor = DNColor.controlSwitchOutline,
        ),
    )
}

/** A row with a title (+ supporting line) and an M3 switch; the whole row toggles. */
@Composable
internal fun ProfileToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    emphasised: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = DNSpace.listRowInsetInline, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = DNTextStyle.RowTitle.style.copy(fontWeight = if (emphasised) FontWeight.SemiBold else FontWeight.Medium), color = DNColor.textPrimary)
            if (subtitle != null) Text(subtitle, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        }
        ProfileSwitch(checked, onCheckedChange = null)
    }
}

/** A label and value on one line (stacked at very large text sizes). */
@Composable
internal fun ProfileValueRow(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = DNColor.textSecondary) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = DNSpace.listRowInsetInline, vertical = 10.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = DNTextStyle.RowTitle.style, color = DNColor.textPrimary, modifier = Modifier.weight(1f))
        Text(value, style = DNTextStyle.Subheadline.style, color = valueColor, textAlign = TextAlign.End)
    }
}

// MARK: - Key–value card

@Composable
internal fun ProfileFactsCard(facts: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val stacked = profileLargeText()
    Column(
        modifier.fillMaxWidth().dnCard(DNRadius.listGroup).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        facts.forEach { (label, value) ->
            if (stacked) {
                Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                    Text(value, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
                }
            } else {
                Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(label, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                    Text(
                        value, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                        color = DNColor.textPrimary, textAlign = TextAlign.End, modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// MARK: - Buttons

/** A grey pill ("View policy", "History") — the neutral sibling of the Canvas buttons. 36 tall in a 48 target. */
@Composable
internal fun ProfileNeutralPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth().height(DNSpace.buttonHeightSmall).clip(CircleShape).background(DNColor.surfaceSecondary)
                .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Filled destructive pill ("Delete your account"), with a busy state. */
@Composable
internal fun ProfileDestructiveButton(text: String, busy: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(CircleShape)
            .background(if (busy) DNColor.actionDestructivePressed else DNColor.actionDestructive)
            .clickable(enabled = !busy, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), color = DNColor.textOnDestructive, strokeWidth = 2.5.dp, trackColor = Color.Transparent)
                Text("Confirming…", style = DNTextStyle.Button.style, color = DNColor.textOnDestructive)
            }
        } else {
            Text(text, style = DNTextStyle.Button.style, color = DNColor.textOnDestructive)
        }
    }
}

/** M3 alert dialog in our colours. The confirm action is named for what it does; destructive is red and last. */
@Composable
internal fun ProfileAlert(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismiss: String = "Cancel",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DNColor.surfaceContainer,
        shape = RoundedCornerShape(DNRadius.dialog),
        title = { Text(title, style = DNTextStyle.Title2.style, color = DNColor.textPrimary) },
        text = { Text(text, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary) },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismiss, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textLink) }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = if (destructive) DNColor.actionDestructive else DNColor.textLink)
            }
        },
    )
}

/** "Download a copy of your information?" — shared by Linked accounts and Delete account. */
@Composable
internal fun ProfileDownloadCopyAlert(onDone: () -> Unit, onSend: () -> Unit) {
    ProfileAlert(
        title = "Download a copy of your information?",
        text = "We'll put together everything DubaiNow keeps about you and email you a link to download it.",
        confirm = "Send me a copy", onConfirm = onSend, onDismiss = onDone,
    )
}

// MARK: - Coming soon, kept local (the profile sheet stays where it is)

/** SF Symbol names used by this feature that the shared DNIcons map doesn't have yet (see NOTES.md). */
internal fun profileSymbolIcon(name: String): ImageVector = when (name) {
    "globe" -> Icons.Rounded.Language
    "questionmark.circle.fill" -> Icons.AutoMirrored.Rounded.Help
    "qrcode" -> Icons.Rounded.QrCode
    "calendar" -> Icons.Rounded.CalendarToday
    else -> DNIcons.forSymbol(name)
}

/** "Coming soon" on top of a sheet (a nested M3 sheet), so the sheet underneath keeps its place. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileComingSoonSheet(item: ComingSoon?, onDismiss: () -> Unit) {
    if (item == null) return
    val state = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = state, containerColor = DNColor.surfacePrimary,
        // Same inset as SheetHost, so a tall sheet never runs under the status bar.
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 8.dp),
        shape = RoundedCornerShape(topStart = DNRadius.sheet, topEnd = DNRadius.sheet),
    ) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding()) {
            ProfileComingSoonContent(item) { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }
        }
    }
}

/** Same as the shared ComingSoonContent, with this feature's extra icons. */
@Composable
private fun ProfileComingSoonContent(item: ComingSoon, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HueIcon(profileSymbolIcon(item.symbol), item.hue, size = 64.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.title, style = DNTextStyle.Title3.style, color = DNColor.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            item.owner?.let { Text(it, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.headline, style = DNTextStyle.Headline.style, color = DNColor.textPrimary, textAlign = TextAlign.Center)
            Text(item.detail ?: item.message, style = DNTextStyle.Body.style, color = DNColor.textSecondary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.size(4.dp))
        DNButton("OK", onClick = onDone)
    }
}

// MARK: - Status text

internal fun profileToneColor(tone: ChipTone): Color = when (tone) {
    ChipTone.Warning -> DNColor.statusWarningForeground
    ChipTone.Info -> DNColor.statusInfoForeground
    ChipTone.Success -> DNColor.statusSuccessForeground
    ChipTone.Error -> DNColor.statusErrorForeground
    ChipTone.Neutral -> DNColor.textSecondary
}

@Composable
internal fun ProfileStatusLine(text: String, tone: ChipTone) {
    Text(text, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = profileToneColor(tone), maxLines = 2)
}

/** A status chip that sits on a hue (white-ish surface, coloured dot and text). */
@Composable
internal fun ProfileOnHueChip(text: String, tone: ChipTone) {
    val dot = when (tone) {
        ChipTone.Warning -> DNColor.signSun
        else -> profileToneColor(tone)
    }
    Row(
        Modifier.heightIn(min = 24.dp).clip(RoundedCornerShape(12.dp)).background(DNColor.chipOnHue)
            .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        Text(text, style = DNTextStyle.Caption1.style.copy(fontWeight = FontWeight.Bold), color = profileToneColor(tone), maxLines = 2)
    }
}

// MARK: - Documents

/** "Valid to March 2028" / "Expires 18 Nov 2026" / "Expired on 2 May 2026". */
internal val DocumentItem.profileExpiryLine: String
    get() {
        val e = expiry ?: return "No expiry date"
        val days = DemoDate.daysFromToday(e)
        val date = ProfileText.shortYear(e)
        if (days < 0) return "Expired on $date"
        // A renewal that's already approved carries the new, far-off date.
        if (days > 365) return "Valid to ${DemoDate.monthYear(e)}"
        return when (status) {
            DocumentItem.Status.Valid -> "Valid to ${DemoDate.monthYear(e)}"
            else -> "Expires $date"
        }
    }

internal val DocumentItem.profileStatusTone: ChipTone
    get() = when (status) {
        DocumentItem.Status.Valid -> ChipTone.Success
        DocumentItem.Status.Expired -> ChipTone.Error
        DocumentItem.Status.ExpiringSoon -> ChipTone.Warning
        DocumentItem.Status.Renewing -> if ((statusNote ?: "").contains("your turn", ignoreCase = true)) ChipTone.Warning else ChipTone.Info
    }

internal val DocumentItem.profileStatusText: String
    get() = statusNote ?: when (status) {
        DocumentItem.Status.Valid -> "Valid"
        DocumentItem.Status.Renewing -> "Renewing"
        DocumentItem.Status.ExpiringSoon -> "Expires soon"
        DocumentItem.Status.Expired -> "Expired"
    }

/** The wallet-style document card (Profile shelf, Documents grid). */
@Composable
internal fun ProfileDocumentCard(document: DocumentItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    val label = buildString {
        append(document.title).append(". ")
        if (document.status != DocumentItem.Status.Valid) append(document.profileStatusText).append(". ")
        append(document.profileExpiryLine)
    }
    Column(
        modifier.heightIn(min = 132.dp).dnElevation(22.dp).clip(shape).background(document.hue.gradient)
            .border(1.dp, Brush.verticalGradient(listOf(DNColor.tileHighlight, Color.Transparent)), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = label }
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(document.title, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Bold), color = document.hue.ink, modifier = Modifier.weight(1f))
            Icon(DNIcons.forSymbol(document.symbol), contentDescription = null, tint = document.hue.ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (document.status == DocumentItem.Status.Valid) {
                Text(
                    document.number, fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight.Normal, color = document.hue.ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, style = DNTextStyle.Footnote.style.copy(fontFeatureSettings = "tnum", letterSpacing = 0.2.sp),
                )
            } else {
                ProfileOnHueChip(document.profileStatusText, document.profileStatusTone)
            }
            Text(document.profileExpiryLine, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = document.hue.ink)
        }
    }
}

// MARK: - Empty state

@Composable
internal fun ProfileEmptyState(icon: ImageVector, title: String, detail: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(top = 40.dp, start = 16.dp, end = 16.dp).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = DNColor.textSecondary, modifier = Modifier.size(44.dp))
        Text(title, style = DNTextStyle.Title3.style, color = DNColor.textPrimary, textAlign = TextAlign.Center)
        Text(detail, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary, textAlign = TextAlign.Center)
    }
}
