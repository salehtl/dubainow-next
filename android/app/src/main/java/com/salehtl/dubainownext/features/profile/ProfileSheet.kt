package com.salehtl.dubainownext.features.profile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalInSheet
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.HomeInfo
import com.salehtl.dubainownext.data.Persona
import com.salehtl.dubainownext.data.PaymentMethod
import com.salehtl.dubainownext.data.personas.Personas
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.Avatar
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNButtonKind
import com.salehtl.dubainownext.design.components.DNTextButton
import com.salehtl.dubainownext.design.components.HueIcon
import com.salehtl.dubainownext.design.components.ToastHost
import com.salehtl.dubainownext.design.components.dnElevation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Profile · Your profile (Paper C1 `EOJ-0`, guest `X6U-0`), opened from the avatar. Renders inside
 * the shell's M3 modal bottom sheet and keeps its own back stack: things (vehicles, documents,
 * family) and the settings pages push inside the sheet; system back pops them first.
 */
@Composable
fun ProfileSheet() {
    val store = LocalStore.current
    val router = LocalRouter.current
    val toasts = LocalToasts.current
    val context = LocalContext.current
    remember { ProfilePrefs.init(context); true }

    val launch = remember { ProfileLaunch.consume() }
    val stack = remember { mutableStateListOf<ProfileEntry>().apply { addAll(launch.entries) } }
    var comingSoon by remember { mutableStateOf(launch.comingSoon) }
    var showsHappiness by remember { mutableStateOf(launch.happiness) }
    // Each screen on the sheet's stack keeps its saved state (scroll position, shelf position)
    // while something is pushed on top of it, like a NavigationStack; popped screens drop theirs.
    val saved = rememberSaveableStateHolder()
    val nav = remember(router, stack) {
        ProfileSheetNav(router, stack, onComingSoon = { comingSoon = it }, onPopped = { depth, entry -> saved.removeState(profileStackKey(depth, entry)) })
    }

    BackHandler(enabled = stack.isNotEmpty()) { nav.back() }
    ProfileBackTestHook()

    CompositionLocalProvider(LocalProfileNav provides nav) {
        Box(Modifier.fillMaxWidth().profileLargeSheetHeight()) {
            AnimatedContent(
                targetState = stack.size to stack.lastOrNull(),
                transitionSpec = {
                    val forward = targetState.first > initialState.first
                    (slideInHorizontally(DNMotion.smooth()) { if (forward) it / 3 else -it / 3 } + fadeIn(DNMotion.base())) togetherWith
                        (slideOutHorizontally(DNMotion.smooth()) { if (forward) -it / 3 else it / 3 } + fadeOut(DNMotion.base()))
                },
                contentAlignment = Alignment.TopStart,
                modifier = Modifier.fillMaxSize(),
                label = "profile-stack",
            ) { (depth, entry) ->
                saved.SaveableStateProvider(profileStackKey(depth, entry)) {
                    when (entry) {
                        null -> if (store.isGuest) ProfileGuestContent(onHappiness = { showsHappiness = true })
                            else ProfileSignedInContent(onHappiness = { showsHappiness = true })
                        is ProfileEntry.Page -> ProfilePageContent(entry.page)
                        is ProfileEntry.Thing -> ProfileThingContent(entry.route)
                    }
                }
            }
            ToastHost(toasts, bottomInset = 16.dp)
        }
    }

    ProfileComingSoonSheet(comingSoon) { comingSoon = null }
    if (showsHappiness) ProfileHappinessSheet(onDismiss = { showsHappiness = false })
}

/** Saved-state key for the screen at [depth] on the sheet's stack (0 = the profile itself). */
private fun profileStackKey(depth: Int, entry: ProfileEntry?): String = "profile-$depth-${entry ?: "root"}"

/**
 * The profile is a large sheet: as tall as the window allows (the shell's sheet already stops
 * short of the status bar, so the app shows above it).
 */
@Composable
private fun Modifier.profileLargeSheetHeight(): Modifier {
    return this.layout { measurable, constraints ->
        if (!constraints.hasBoundedHeight) {
            val p = measurable.measure(constraints)
            return@layout layout(p.width, p.height) { p.place(0, 0) }
        }
        val h = constraints.maxHeight
        val p = measurable.measure(constraints.copy(minHeight = h, maxHeight = h))
        layout(p.width, p.height) { p.place(0, 0) }
    }
}

/**
 * Demo/screenshot hooks, applied the first time the sheet opens after launch:
 * `profileRoute vehicle:sara-patrol` and/or `profilePage linkedAccounts` push inside the sheet;
 * `profileComingSoon language|help` opens a local "coming soon"; `profileHappiness true` opens the meter.
 */
private class ProfileLaunch(val entries: List<ProfileEntry>, val comingSoon: ComingSoon?, val happiness: Boolean) {
    companion object {
        fun consume(): ProfileLaunch {
            if (ProfileDebug.launchApplied) return ProfileLaunch(emptyList(), null, false)
            ProfileDebug.launchApplied = true
            val out = mutableListOf<ProfileEntry>()
            ProfileDebug.extra("profileRoute")?.split(",")
                ?.mapNotNull { Route.parse(it.trim()) }?.filter { it.isProfileThing() }
                ?.forEach { out += ProfileEntry.Thing(it) }
            ProfileDebug.extra("profilePage")?.let { ProfilePage.parse(it) }?.let { out += ProfileEntry.Page(it) }
            val soon = when (ProfileDebug.extra("profileComingSoon")) {
                "language" -> ProfileComingSoons.language
                "help" -> ProfileComingSoons.help
                else -> null
            }
            return ProfileLaunch(out, soon, ProfileDebug.flag("profileHappiness"))
        }
    }
}

/** `profileTestBack <n>` presses back n times (1.5 s apart) through the sheet's dispatcher, like the system gesture. */
@Composable
private fun ProfileBackTestHook() {
    val owner = LocalOnBackPressedDispatcherOwner.current ?: return
    LaunchedEffect(owner) {
        val n = ProfileDebug.extra("profileTestBack")?.toIntOrNull() ?: return@LaunchedEffect
        repeat(n) { delay(1500); owner.onBackPressedDispatcher.onBackPressed() }
    }
}

@Composable
private fun ProfilePageContent(page: ProfilePage) = when (page) {
    ProfilePage.Home -> ProfileHomePage()
    ProfilePage.PaymentMethods -> ProfilePaymentMethodsPage()
    ProfilePage.NotificationSettings -> ProfileNotificationSettingsPage()
    ProfilePage.Accessibility -> ProfileAccessibilityPage()
    ProfilePage.LinkedAccounts -> ProfileLinkedAccountsPage()
    ProfilePage.Privacy -> ProfilePrivacyPage()
    ProfilePage.DeleteAccount -> ProfileDeleteAccountPage()
}

@Composable
private fun ProfileThingContent(route: Route) {
    when (route) {
        is Route.Vehicle -> VehicleScreen(route.id)
        Route.Vehicles -> VehiclesScreen()
        Route.Documents -> DocumentsScreen()
        is Route.Document -> DocumentScreen(route.id)
        Route.Family -> FamilyScreen()
        else -> Unit
    }
}

// MARK: - Signed in (EOJ-0)

@Composable
private fun ProfileSignedInContent(onHappiness: () -> Unit) {
    val store = LocalStore.current
    val router = LocalRouter.current
    val toasts = LocalToasts.current
    var confirmsSignOut by remember { mutableStateOf(ProfileDebug.extra("profileDialog") == "signOut") }
    val scroll = rememberScrollState()

    Column(Modifier.fillMaxSize().background(DNColor.surfaceSecondary)) {
        ProfileSheetTitleRow("Your profile", onClose = { router.dismissSheet() })
        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ProfileIdentity()
            if (store.state.documents.isNotEmpty()) ProfileDocumentsShelf()
            ProfileYourThings()
            ProfileSettingsSection(showsLinkedAccounts = true, onHappiness = onHappiness, modifier = Modifier.padding(horizontal = DNSpace.insetPage))
            ProfileAccountActions(onSignOut = { confirmsSignOut = true })
        }
    }
    ProfileScrollHook(scroll)

    if (confirmsSignOut) {
        ProfileAlert(
            title = "Sign out of DubaiNow?",
            text = "You can sign in again with UAE PASS at any time.",
            confirm = "Sign out", destructive = true,
            onConfirm = {
                confirmsSignOut = false
                toasts.show("You're signed out")
                router.reset()
                store.signOut()
            },
            onDismiss = { confirmsSignOut = false },
        )
    }
}

@Composable
private fun ProfileIdentity() {
    val store = LocalStore.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = DNSpace.insetPage),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        store.persona?.let { p ->
            Box(
                Modifier.padding(top = 4.dp).size(96.dp).dnElevation(48.dp).clip(CircleShape).background(DNColor.white).padding(4.dp),
                contentAlignment = Alignment.Center,
            ) { Avatar(p.initials, p.avatarHue, size = 88.dp) }
            Text(
                p.fullName, style = DNTextStyle.Title2.style.copy(fontWeight = FontWeight.Bold), color = DNColor.textPrimary,
                textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() },
            )
        }
        Row(
            Modifier.heightIn(min = 28.dp).clip(CircleShape).background(DNColor.statusSuccessSurface)
                .padding(start = 8.dp, end = 12.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = DNColor.statusSuccessForeground, modifier = Modifier.size(16.dp))
            Text("Verified with UAE PASS", style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.statusSuccessForeground)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileDocumentsShelf() {
    val store = LocalStore.current
    val nav = profileNav()
    val docs = store.state.documents
    val fontScale = LocalDensity.current.fontScale
    val cardWidth = (210f * fontScale.coerceIn(1f, 1.4f)).dp
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = DNSpace.insetPage, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileSectionTitle("Documents", Modifier.weight(1f))
            DNTextButton(
                "See all ${docs.size}", onClick = { nav.push(Route.Documents) },
                modifier = Modifier.semantics { contentDescription = "See all ${docs.size} documents" },
            )
        }
        val listState = rememberLazyListState()
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState, ProfileShelfSnapPosition),
            contentPadding = PaddingValues(horizontal = DNSpace.insetPage, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(docs, key = { it.id }) { d ->
                // A shared minimum height keeps the row even; cards still grow at large text sizes instead of clipping.
                ProfileDocumentCard(d, onClick = { nav.push(Route.Document(d.id)) }, modifier = Modifier.width(cardWidth).heightIn(min = profileShelfCardHeight(fontScale)))
            }
        }
    }
}

/**
 * Cards rest with their leading edge on the page inset, so the shelf starts with a whole card (iOS .viewAligned).
 * The last card rests on the trailing inset instead: its leading edge can't reach the start, and a target past the
 * end made a fling stop between two cards.
 */
private val ProfileShelfSnapPosition = object : SnapPosition {
    override fun position(layoutSize: Int, itemSize: Int, beforeContentPadding: Int, afterContentPadding: Int, itemIndex: Int, itemCount: Int): Int =
        (if (itemIndex == itemCount - 1) SnapPosition.End else SnapPosition.Start)
            .position(layoutSize, itemSize, beforeContentPadding, afterContentPadding, itemIndex, itemCount)
}

/** Cards in the shelf share one height so the row reads as a wallet. */
private fun profileShelfCardHeight(fontScale: Float) = (132f * fontScale.coerceIn(1f, 1.6f)).dp

@Composable
private fun ProfileYourThings() {
    val store = LocalStore.current
    val nav = profileNav()
    val s = store.state
    val vehiclesRoute: Route = if (s.vehicles.size == 1) Route.Vehicle(s.vehicles.first().id) else Route.Vehicles
    val fines = store.profileFineCount
    Column(Modifier.padding(horizontal = DNSpace.insetPage), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileSectionTitle("Your things")
        ProfileGroup {
            ProfileRow(
                "Vehicles", onClick = { nav.push(vehiclesRoute) },
                subtitle = if (s.vehicles.isEmpty()) "Add a vehicle you own" else ProfileText.join(s.vehicles.map { it.shortName }),
                badge = fines, badgeDescription = "$fines unpaid ${if (fines == 1) "fine" else "fines"}", emphasised = true,
            ) { HueIcon("car.fill", Hue.Rose, size = 40.dp) }
            s.home?.let { home ->
                ProfileDivider(68.dp)
                ProfileRow("Home", onClick = { nav.open(ProfilePage.Home) }, subtitle = profileHomeLine(store.persona, home), emphasised = true) {
                    HueIcon("house.fill", Hue.Sand, size = 40.dp)
                }
            }
            if (s.family.isNotEmpty()) {
                ProfileDivider(68.dp)
                ProfileRow("Family", onClick = { nav.push(Route.Family) }, subtitle = ProfileText.join(s.family.map { ProfileText.firstName(it.name) }), emphasised = true) {
                    HueIcon("person.2.fill", Hue.Peach, size = 40.dp)
                }
            }
            ProfileDivider(68.dp)
            ProfileRow("Payment methods", onClick = { nav.open(ProfilePage.PaymentMethods) }, subtitle = profilePaymentSummary(s.paymentMethods), emphasised = true) {
                ProfileGreyDisc(icon = Icons.Rounded.CreditCard)
            }
        }
    }
}

private fun profileHomeLine(persona: Persona?, home: HomeInfo): String {
    val area = persona?.area ?: "Your home"
    return if (home.ejariExpiry != null) "$area · DEWA and Ejari linked" else "$area · DEWA linked"
}

/** "Google Pay and 1 saved card" */
internal fun profilePaymentSummary(methods: List<PaymentMethod>): String {
    val cards = methods.count { it is PaymentMethod.Card }
    val wallet = methods.any { it == PaymentMethod.GooglePay }
    return when {
        wallet && cards == 0 -> "Google Pay"
        wallet && cards == 1 -> "Google Pay and 1 saved card"
        wallet -> "Google Pay and $cards saved cards"
        cards == 0 -> "Add a way to pay"
        cards == 1 -> "1 saved card"
        else -> "$cards saved cards"
    }
}

// MARK: - Settings (shared by signed in and guest)

@Composable
private fun ProfileSettingsSection(showsLinkedAccounts: Boolean, onHappiness: () -> Unit, modifier: Modifier = Modifier) {
    val nav = profileNav()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileSectionTitle("Settings")
        ProfileGroup {
            ProfileRow("Language", onClick = { nav.comingSoon(ProfileComingSoons.language) }, value = "English") { ProfileRowIcon(Icons.Rounded.Language) }
            ProfileDivider(52.dp)
            ProfileRow("Notifications", onClick = { nav.open(ProfilePage.NotificationSettings) }, value = ProfilePrefs.summary) {
                ProfileRowIcon(Icons.Rounded.NotificationsNone)
            }
            ProfileDivider(52.dp)
            ProfileRow("Accessibility and display", onClick = { nav.open(ProfilePage.Accessibility) }) { ProfileRowIcon(Icons.Rounded.AccessibilityNew) }
            if (showsLinkedAccounts) {
                ProfileDivider(52.dp)
                ProfileRow("Linked accounts and privacy", onClick = { nav.open(ProfilePage.LinkedAccounts) }) { ProfileRowIcon(Icons.Rounded.PrivacyTip) }
            }
            ProfileDivider(52.dp)
            ProfileRow("Happiness Meter", onClick = onHappiness) { ProfileRowIcon(Icons.Rounded.SentimentSatisfied) }
            ProfileDivider(52.dp)
            ProfileRow("Get help", onClick = { nav.comingSoon(ProfileComingSoons.help) }) { ProfileRowIcon(Icons.AutoMirrored.Rounded.HelpOutline) }
            ProfileDivider(52.dp)
            ProfileRow("Privacy policy and terms", onClick = { nav.open(ProfilePage.Privacy) }) { ProfileRowIcon(Icons.AutoMirrored.Rounded.Article) }
        }
    }
}

// MARK: - Sign out, delete, demo controls, footer

@Composable
private fun ProfileAccountActions(onSignOut: () -> Unit) {
    val nav = profileNav()
    Column(
        Modifier.fillMaxWidth().padding(horizontal = DNSpace.insetPage),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(DNRadius.listGroup)).background(DNColor.surfacePrimary)
                .clickable(role = Role.Button, onClick = onSignOut),
            contentAlignment = Alignment.Center,
        ) { Text("Sign out", style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.actionDestructive) }
        DNTextButton("Delete your account", onClick = { nav.open(ProfilePage.DeleteAccount) }, color = DNColor.actionDestructive)
        Spacer(Modifier.height(4.dp))
        ProfileDemoControlsRow()
        ProfileFooter("DubaiNow 6.0 · Signed in with UAE PASS on this phone")
    }
}

/** "Demo controls" — opens the demo menu (switch resident, reset, time of day). */
@Composable
private fun ProfileDemoControlsRow() {
    val router = LocalRouter.current
    val view = LocalView.current
    ProfileGroup {
        ProfileRow(
            "Demo controls", onClick = { DNHaptics.medium(view); router.present(SheetRoute.DemoMenu) },
            subtitle = "Switch resident, reset, time of day",
        ) { ProfileRowIcon(Icons.Rounded.Tune, DNColor.textSecondary) }
    }
}

/** The version line. A long press also opens the demo controls. */
@Composable
private fun ProfileFooter(text: String) {
    val router = LocalRouter.current
    val view = LocalView.current
    Text(
        text, style = DNTextStyle.Caption1.style.copy(fontWeight = FontWeight.Normal), color = DNColor.textSecondary, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 14.dp)
            .pointerInput(Unit) { detectTapGestures(onLongPress = { DNHaptics.medium(view); router.present(SheetRoute.DemoMenu) }) }
            .semantics {
                customActions = listOf(CustomAccessibilityAction("Demo controls") { router.present(SheetRoute.DemoMenu); true })
            },
    )
}

// MARK: - Guest (X6U-0)

@Composable
private fun ProfileGuestContent(onHappiness: () -> Unit) {
    val store = LocalStore.current
    val router = LocalRouter.current
    val toasts = LocalToasts.current
    val view = LocalView.current
    val nav = profileNav()
    var choosesPersona by remember { mutableStateOf(ProfileDebug.extra("profileDialog") == "persona") }
    val scroll = rememberScrollState()

    Column(Modifier.fillMaxSize().background(DNColor.surfaceSecondary)) {
        ProfileSheetTitleRow("Profile", onClose = { router.dismissSheet() })
        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = DNSpace.insetPage).padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.padding(top = 4.dp).size(88.dp).dnElevation(44.dp).clip(CircleShape).background(DNColor.surfacePrimary),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PersonOutline, contentDescription = null, tint = DNColor.textSecondary, modifier = Modifier.size(40.dp)) }
                Text(
                    "You're a guest", style = DNTextStyle.Title2.style.copy(fontWeight = FontWeight.Bold), color = DNColor.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "Sign in to see your documents, vehicles, bills and requests.", style = DNTextStyle.Subheadline.style,
                    color = DNColor.textSecondary, textAlign = TextAlign.Center,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DNButton("Sign in with UAE PASS", onClick = { choosesPersona = true }, kind = DNButtonKind.Inverse, icon = Icons.Rounded.Fingerprint)
                Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("New to UAE PASS?", style = DNTextStyle.Subheadline.style, color = DNColor.textPrimary)
                    Box(
                        Modifier.heightIn(min = 48.dp).clip(CircleShape).clickable(role = Role.Button) {
                            nav.comingSoon(
                                ComingSoon(
                                    title = "Create a UAE PASS account",
                                    detail = "UAE PASS accounts are made in the UAE PASS app. In this demo, sign in as one of the demo residents instead.",
                                    symbol = "touchid", hue = Hue.Sky, kind = ComingSoon.Kind.NotInDemo,
                                ),
                            )
                        }.padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Create an account", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textLink) }
                }
            }
            ProfileSettingsSection(showsLinkedAccounts = false, onHappiness = onHappiness)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileDemoControlsRow()
                ProfileFooter("DubaiNow 6.0 · Browsing as a guest on this phone")
            }
        }
    }
    ProfileScrollHook(scroll)

    if (choosesPersona) {
        ProfilePersonaDialog(
            onDismiss = { choosesPersona = false },
            onChoose = { p ->
                choosesPersona = false
                DNHaptics.success(view)
                store.signIn(p)
                toasts.show("Signed in as ${p.firstName}")
            },
        )
    }
}

/** "Sign in with UAE PASS" in the demo: choose a resident (iOS uses a confirmation dialog). */
@Composable
private fun ProfilePersonaDialog(onDismiss: () -> Unit, onChoose: (Persona) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DNColor.surfaceContainer,
        shape = RoundedCornerShape(DNRadius.dialog),
        title = { Text("Sign in with UAE PASS", style = DNTextStyle.Title2.style, color = DNColor.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("This is a demo. Choose a resident to sign in as.", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                Spacer(Modifier.height(8.dp))
                Personas.all.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp))
                            .clickable(role = Role.Button) { onChoose(p) }.padding(horizontal = 8.dp, vertical = 8.dp)
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Avatar(p.initials, p.avatarHue, size = 40.dp)
                        // Full name only, like iOS's confirmation dialog (two-line taglines were cut off).
                        Text(p.fullName, style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textLink)
            }
        },
    )
}

// MARK: - Happiness Meter on top of the profile (the profile keeps its place)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileHappinessSheet(onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = state, containerColor = DNColor.surfaceSecondary,
        // Same inset as SheetHost: with the keyboard up the sheet stops below the status bar, not under it.
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 8.dp),
        shape = RoundedCornerShape(topStart = DNRadius.sheet, topEnd = DNRadius.sheet),
    ) {
        CompositionLocalProvider(LocalInSheet provides true, LocalProfileNav provides null) {
            Box(Modifier.fillMaxWidth().navigationBarsPadding()) {
                ProfileHappinessContent(context = null, onClose = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } })
            }
        }
    }
}
