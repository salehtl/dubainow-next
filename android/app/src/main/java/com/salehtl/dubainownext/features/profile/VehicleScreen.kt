package com.salehtl.dubainownext.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.Payable
import com.salehtl.dubainownext.data.PaymentLine
import com.salehtl.dubainownext.data.PaymentPurpose
import com.salehtl.dubainownext.data.PaymentRequest
import com.salehtl.dubainownext.data.Plate
import com.salehtl.dubainownext.data.Vehicle
import com.salehtl.dubainownext.data.WidgetKind
import com.salehtl.dubainownext.data.WidgetSize
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNIcons
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.Inter
import com.salehtl.dubainownext.design.components.ChipTone
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNButtonKind
import com.salehtl.dubainownext.design.components.DNButtonSize
import com.salehtl.dubainownext.design.components.DNTextButton
import com.salehtl.dubainownext.design.components.HueIcon
import com.salehtl.dubainownext.design.components.TonalIconButton
import com.salehtl.dubainownext.design.components.dnCard
import com.salehtl.dubainownext.design.components.dnElevation
import java.math.BigDecimal

/**
 * Profile · Your vehicle (Paper C1 `ETT-0`): plate, at a glance (registration, fines, Salik,
 * insurance), things you can do, Home widgets for this car, recent activity, details.
 * Pushed in a tab (our header) or inside the profile sheet (its own back row).
 */
@Composable
fun VehicleScreen(vehicleId: String) {
    val store = LocalStore.current
    val vehicle = store.state.vehicles.firstOrNull { it.id == vehicleId }
    if (vehicle == null) {
        ProfileScreen("Vehicle") {
            ProfileEmptyState(Icons.Rounded.DirectionsCar, "Vehicle not found", "It isn't linked to your account any more.")
        }
    } else {
        ProfileVehicleDetail(vehicle)
    }
}

@Composable
private fun ProfileVehicleDetail(vehicle: Vehicle) {
    val store = LocalStore.current
    val context = LocalContext.current
    val nav = profileNav()
    var menu by remember { mutableStateOf(false) }

    ProfileScreen(
        vehicle.shortName, wash = if (nav.inSheet) null else Hue.Rose, spacing = 32.dp,
        actions = {
            Box {
                TonalIconButton(Icons.Rounded.MoreVert, "More", onClick = { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = DNColor.surfaceContainer) {
                    DropdownMenuItem(
                        text = { Text("Share vehicle details", style = DNTextStyle.Callout.style, color = DNColor.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null, tint = DNColor.textPrimary) },
                        onClick = { menu = false; profileShare(context, profileVehicleShareText(vehicle)) },
                    )
                    DropdownMenuItem(
                        text = { Text("Report wrong information", style = DNTextStyle.Callout.style, color = DNColor.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.Feedback, contentDescription = null, tint = DNColor.textPrimary) },
                        onClick = {
                            menu = false
                            nav.comingSoon(
                                ComingSoon(
                                    title = "Report wrong information",
                                    detail = "Vehicle details come from RTA. Reporting a mistake comes in the next phase.",
                                    owner = "RTA", symbol = "exclamationmark.bubble.fill", hue = Hue.Rose, kind = ComingSoon.Kind.NotInDemo,
                                ),
                            )
                        },
                    )
                }
            }
        },
    ) {
        ProfileVehicleHeader(vehicle)
        ProfileVehicleGlance(vehicle, store.profileFines(vehicle))
        ProfileVehicleThings()
        ProfileVehicleOnHome(vehicle)
        ProfileVehicleActivity(vehicle, store.profileFines(vehicle))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileSection("Details")
            ProfileFactsCard(
                listOf(
                    "Traffic file number" to "10458823",
                    "Chassis number" to "Ending ${profileChassis(vehicle)}",
                    "Registered since" to "${DemoDate.short(vehicle.registrationExpiry)} ${vehicle.year}",
                    "Salik tag" to (vehicle.salikTag?.replace("Tag ending", "Ending") ?: "None"),
                    "Information from" to "RTA · updated today",
                ),
            )
        }
    }
}

private fun profileVehicleShareText(v: Vehicle): String {
    val emirate = v.plate.emirate.lowercase().replaceFirstChar { it.uppercase() }
    return "${v.name} · $emirate ${v.plate.code} ${v.plate.number} · registration until ${DemoDate.full(v.registrationExpiry)}"
}

private fun profileChassis(v: Vehicle): String {
    var seed = 0
    v.id.forEach { seed = seed * 31 + it.code }
    seed = kotlin.math.abs(seed)
    val letters = "ABCDEFGHJKLMNPRSTUVWXYZ"
    return "${seed % 9 + 1}${letters[seed % letters.length]}${"%04d".format(seed % 10000)}"
}

// MARK: Header

@Composable
private fun ProfileVehicleHeader(vehicle: Vehicle) {
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ProfileHeroPlate(vehicle.plate)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(vehicle.shortName, style = DNTextStyle.Title1.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
            Text("${vehicle.year} · ${vehicle.colour} · Private · Registered to you", style = DNTextStyle.Callout.style, color = DNColor.textSecondary)
        }
    }
}

/**
 * A big Dubai plate for the vehicle header (60 dp tall). Always left to right. The plate is a
 * picture of the real thing with a spoken label, so its lettering keeps its size at any font size
 * (scaled text would be clipped by the fixed plate).
 */
@Composable
internal fun ProfileHeroPlate(plate: Plate) {
    val emirate = plate.emirate.lowercase().replaceFirstChar { it.uppercase() }
    val density = LocalDensity.current
    fun fixed(v: Float) = with(density) { v.dp.toSp() }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier.height(60.dp).dnElevation(10.dp).clip(RoundedCornerShape(10.dp)).background(DNColor.plateSurface)
                .border(2.dp, DNColor.plateEdge, RoundedCornerShape(10.dp)).padding(start = 12.dp, end = 16.dp)
                .clearAndSetSemantics { contentDescription = "Plate $emirate ${plate.code} ${plate.number}" },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(plate.emirate, fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = fixed(11f), lineHeight = fixed(13f), letterSpacing = fixed(0.9f), color = DNColor.plateInk)
                Text(plate.code, fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = fixed(26f), lineHeight = fixed(28f), color = DNColor.plateInk)
            }
            Box(Modifier.width(1.5.dp).height(40.dp).background(DNColor.plateRule))
            Text(
                plate.number, fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = fixed(40f), lineHeight = fixed(44f),
                letterSpacing = fixed(0.8f), color = DNColor.plateInk, maxLines = 1,
                style = DNTextStyle.NumericLarge.style,
            )
        }
    }
}

// MARK: At a glance

@Composable
private fun ProfileVehicleGlance(vehicle: Vehicle, fines: List<Payable>) {
    val store = LocalStore.current
    val router = LocalRouter.current
    val nav = profileNav()
    val regDays = DemoDate.daysFromToday(vehicle.registrationExpiry)
    val cards = buildList<@Composable (Modifier) -> Unit> {
        add { m ->
            ProfileGlanceCard(
                m, symbol = "calendar", hue = Hue.Rose, label = "Registration",
                value = {
                    when {
                        regDays < 0 -> Text("Expired", style = DNTextStyle.NumericMedium.style, color = DNColor.textPrimary)
                        regDays <= 60 -> Row {
                            Text("$regDays", style = DNTextStyle.NumericLarge.style, color = DNColor.textPrimary, modifier = Modifier.alignByBaseline())
                            Spacer(Modifier.width(5.dp))
                            Text(if (regDays == 1) "day left" else "days left", style = DNTextStyle.NumericUnit.style, color = DNColor.textPrimary, modifier = Modifier.alignByBaseline())
                        }
                        else -> Text("Valid", style = DNTextStyle.NumericMedium.style, color = DNColor.textPrimary)
                    }
                },
                status = {
                    when {
                        regDays < 0 -> ProfileStatusLine("Expired ${DemoDate.short(vehicle.registrationExpiry)}", ChipTone.Error)
                        regDays <= 60 -> ProfileStatusLine("Expires ${DemoDate.short(vehicle.registrationExpiry)}", ChipTone.Warning)
                        else -> ProfileStatusLine("Until ${ProfileText.shortYear(vehicle.registrationExpiry)}", ChipTone.Success)
                    }
                },
                action = {
                    DNButton(
                        if (regDays <= 60) "Renew" else "Renew early", onClick = { nav.push(Route.Service("renew-vehicle-registration")) },
                        kind = if (regDays <= 60) DNButtonKind.Primary else DNButtonKind.Secondary, size = DNButtonSize.Small,
                    )
                },
            )
        }
        add { m ->
            val total = fines.fold(BigDecimal.ZERO) { a, f -> a + f.amount }
            ProfileGlanceCard(
                m, symbol = "doc.text.fill", hue = null, label = "Fines",
                value = {
                    if (fines.isEmpty()) Text("None", style = DNTextStyle.NumericMedium.style, color = DNColor.textPrimary)
                    else ProfileAmount(total)
                },
                status = {
                    if (fines.isEmpty()) ProfileStatusLine("Nothing to pay", ChipTone.Success)
                    else ProfileStatusLine(if (fines.size == 1) "1 ${fines.first().title.lowercase()}" else "${fines.size} fines", ChipTone.Error)
                },
                action = {
                    if (fines.isEmpty()) ProfileNeutralPill("History", onClick = { nav.push(Route.History) })
                    else DNButton("Pay", onClick = { router.present(SheetRoute.Payment(profileFinesRequest(store, vehicle, fines))) }, size = DNButtonSize.Small)
                },
            )
        }
        if (vehicle.salikTag != null) add { m ->
            val balance = store.state.salikBalance
            val low = balance < BigDecimal(50)
            ProfileGlanceCard(
                m, symbol = "road.lanes", hue = Hue.Sand, label = "Salik",
                value = { ProfileAmount(balance) },
                status = {
                    ProfileStatusLine(
                        if (low) "Low balance" else "About ${balance.divide(BigDecimal(4)).toInt()} trips",
                        if (low) ChipTone.Warning else ChipTone.Neutral,
                    )
                },
                action = { DNButton("Top up", onClick = { router.present(SheetRoute.TopUpSalik) }, kind = DNButtonKind.Secondary, size = DNButtonSize.Small) },
            )
        }
        add { m ->
            val days = DemoDate.daysFromToday(vehicle.insuranceExpiry)
            ProfileGlanceCard(
                m, symbol = "checkmark.shield.fill", hue = Hue.Mint, label = "Insurance",
                value = { Text(if (days < 0) "Expired" else "Valid", style = DNTextStyle.NumericMedium.style, color = DNColor.textPrimary) },
                status = {
                    ProfileStatusLine(
                        if (days < 0) "Ended ${DemoDate.short(vehicle.insuranceExpiry)}" else "Until ${ProfileText.shortYear(vehicle.insuranceExpiry)}",
                        if (days < 0) ChipTone.Error else if (days < 30) ChipTone.Warning else ChipTone.Success,
                    )
                },
                action = {
                    ProfileNeutralPill("View policy", onClick = {
                        nav.comingSoon(
                            ComingSoon(
                                title = "Your insurance policy",
                                detail = "Your policy comes from ${vehicle.insurer}. Opening insurers' documents isn't part of this demo.",
                                owner = vehicle.insurer, symbol = "checkmark.shield.fill", hue = Hue.Mint, kind = ComingSoon.Kind.NotInDemo,
                            ),
                        )
                    })
                },
            )
        }
    }
    val columns = if (profileLargeText()) 1 else 2
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileSection("At a glance")
        cards.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(DNSpace.widgetGutter)) {
                row.forEach { card -> card(Modifier.weight(1f).fillMaxHeight()) }
                if (row.size < columns) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun profileFinesRequest(store: com.salehtl.dubainownext.data.DemoStore, vehicle: Vehicle, fines: List<Payable>): PaymentRequest =
    if (fines.size == 1) store.paymentRequest(fines.first())
    else PaymentRequest(
        id = "pay-fines-${vehicle.id}",
        title = "Pay ${fines.size} fines",
        lines = fines.map { PaymentLine(it.title, it.entity, it.amount, it.symbol, it.hue) },
        purpose = PaymentPurpose.Payables(fines.map { it.id }),
    )

/** "AED 600" — small unit, large figure (Inter, tabular figures). */
@Composable
private fun ProfileAmount(amount: BigDecimal) {
    Row(Modifier.clearAndSetSemantics { contentDescription = Money.spoken(amount) }) {
        Text("AED", style = DNTextStyle.NumericUnit.style, color = DNColor.textPrimary, modifier = Modifier.alignByBaseline())
        Spacer(Modifier.width(5.dp))
        Text(Money.figure(amount, trimWhole = true), style = DNTextStyle.NumericLarge.style, color = DNColor.textPrimary, modifier = Modifier.alignByBaseline())
    }
}

/** One "At a glance" card: header (disc + label), value, status line, a small pill button. */
@Composable
private fun ProfileGlanceCard(
    modifier: Modifier,
    symbol: String,
    hue: Hue?,
    label: String,
    value: @Composable () -> Unit,
    status: @Composable () -> Unit,
    action: @Composable () -> Unit,
) {
    Column(
        modifier.heightIn(min = 176.dp).dnCard(DNRadius.widget, DNColor.surfaceWidget).padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hue != null) HueIcon(profileSymbolIcon(symbol), hue, size = 28.dp)
            else ProfileErrorDisc(symbol, 28.dp)
            Text(
                label, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() },
            )
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            value()
            status()
        }
        Spacer(Modifier.height(10.dp))
        action()
    }
}


@Composable
private fun ProfileErrorDisc(symbol: String, size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(DNColor.statusErrorSurface), contentAlignment = Alignment.Center) {
        Icon(DNIcons.forSymbol(symbol), contentDescription = null, tint = DNColor.statusErrorForeground, modifier = Modifier.size(size * 0.5f))
    }
}

// MARK: Things you can do

private val profileVehicleServiceIDs = listOf(
    "renew-vehicle-registration", "pay-traffic-fine", "find-testing-centre", "transfer-vehicle-ownership", "get-vehicle-certificate",
)

@Composable
private fun ProfileVehicleThings() {
    val nav = profileNav()
    val all = Catalogue.shared.standalone("driving-and-transport", "vehicles").size
    val services = profileVehicleServiceIDs.mapNotNull { id -> Catalogue.service(id)?.let { s -> profileRouteForService(id)?.let { s to it } } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileSection("Things you can do")
        ProfileGroup(elevated = true) {
            services.forEach { (s, route) ->
                ProfileRow(s.name, onClick = { nav.push(route) })
                ProfileDivider()
            }
            ProfileRow(
                "All $all vehicle services", onClick = { nav.push(Route.Subtopic("driving-and-transport", "vehicles")) },
                emphasised = true, titleColor = DNColor.textLink, showsChevron = false,
            )
        }
    }
}

// MARK: On your Home

@Composable
private fun ProfileVehicleOnHome(vehicle: Vehicle) {
    val store = LocalStore.current
    val toasts = LocalToasts.current
    val view = LocalView.current
    var registration by remember(vehicle.id) { mutableStateOf(true) }
    var finesWidget by remember(vehicle.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ProfileSection("On your Home")
            Text("Widgets for this car. They update on their own.", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
        }
        ProfileGroup(elevated = true) {
            ProfileToggleRow("Registration countdown", registration, { DNHaptics.selection(view); registration = it }, subtitle = "Small widget")
            if (vehicle.salikTag != null) {
                ProfileDivider()
                ProfileToggleRow(
                    "Salik balance", store.hasWidget(WidgetKind.Salik),
                    onCheckedChange = { on ->
                        if (on) {
                            store.addWidget(WidgetKind.Salik, WidgetSize.Small)
                            DNHaptics.success(view)
                            toasts.show("Salik added to Home")
                        } else {
                            store.state.widgets.firstOrNull { it.kind == WidgetKind.Salik }?.let { w ->
                                val at = store.state.widgets.indexOf(w)
                                store.removeWidget(w)
                                DNHaptics.light(view)
                                toasts.show("Salik removed from Home", undo = { store.restoreWidget(w, at) })
                            }
                        }
                    },
                    subtitle = "Small widget · shared by both cars",
                )
            }
            ProfileDivider()
            ProfileToggleRow("Fines", finesWidget, { DNHaptics.selection(view); finesWidget = it }, subtitle = "Only shows when you have one")
        }
    }
}

// MARK: Recent activity

private data class ProfileActivityItem(
    val id: String,
    val title: String,
    val detail: String,
    val amount: String?,
    val amountIsDue: Boolean = false,
    val leading: @Composable () -> Unit,
)

@Composable
private fun ProfileVehicleActivity(vehicle: Vehicle, fines: List<Payable>) {
    val store = LocalStore.current
    val nav = profileNav()
    val items = buildList {
        store.state.parking?.takeIf { it.plate == vehicle.plate }?.let { p ->
            add(ProfileActivityItem("parking", "Parking now", "Zone ${p.zone} · ${p.area} · ends ${DemoDate.time(p.endsAt)}", null) { ProfileLetterDisc("P", Hue.Sky) })
        }
        fines.forEach { f ->
            val parts = f.detail.split("·").map { it.trim() }
            val place = parts.firstOrNull() ?: f.detail
            val whenText = parts.getOrNull(1)
            add(
                ProfileActivityItem(f.id, "${f.title} issued", listOfNotNull(whenText, place).joinToString(" · "), Money.aed(f.amount), amountIsDue = true) {
                    ProfileErrorDisc("doc.text.fill", 36.dp)
                },
            )
        }
        vehicle.lastInspection?.let { d ->
            val centre = if (store.persona?.id == "sara") "Al Barsha inspection centre" else "Vehicle testing centre"
            add(ProfileActivityItem("inspection", "Passed vehicle inspection", "${DemoDate.short(d)} · $centre", Money.aed(BigDecimal(170))) {
                HueIcon("checkmark", Hue.Mint, size = 36.dp)
            })
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileSection("Recent activity", Modifier.weight(1f))
            DNTextButton("See all", onClick = { nav.push(Route.History) }, modifier = Modifier.semantics { contentDescription = "See all activity" })
        }
        if (items.isEmpty()) {
            Text("Nothing yet. Payments and inspections for this car will show here.", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
        } else {
            ProfileGroup(elevated = true) {
                items.forEachIndexed { i, item ->
                    if (i > 0) ProfileDivider(64.dp)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp).semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item.leading()
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(item.title, style = DNTextStyle.RowTitle.style, color = DNColor.textPrimary)
                            Text(item.detail, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
                        }
                        item.amount?.let {
                            Text(
                                it, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                                color = if (item.amountIsDue) DNColor.statusErrorForeground else DNColor.textPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A hue disc with a letter (the "P" for parking). */
@Composable
private fun ProfileLetterDisc(letter: String, hue: Hue) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(hue.gradient), contentAlignment = Alignment.Center) {
        Text(letter, fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = hue.ink)
    }
}

