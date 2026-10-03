package com.salehtl.dubainownext.features.services

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DraftItem
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.PaymentLine
import com.salehtl.dubainownext.data.PaymentPurpose
import com.salehtl.dubainownext.data.PaymentRequest
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.data.Vehicle
import com.salehtl.dubainownext.data.hue
import com.salehtl.dubainownext.data.icon
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNIcons
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.ChipTone
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.DNButtonKind
import com.salehtl.dubainownext.design.components.DNTextButton
import com.salehtl.dubainownext.design.components.HueIcon
import com.salehtl.dubainownext.design.components.RowTrailing
import com.salehtl.dubainownext.design.components.SegmentedProgress
import java.math.BigDecimal
import java.time.LocalDate

/**
 * A service start page (Paper C1 `DT8-0`). Standalone records get the start page; if a route
 * names a bundle or journey, its own page is shown instead.
 */
@Composable
fun ServiceStartScreen(serviceId: String) {
    val service = Catalogue.shared.service(serviceId)
    when (service?.type) {
        null -> ServicesMissingRecord(serviceId)
        ServiceType.Standalone -> ServicesStartPage(service)
        ServiceType.Bundle -> ServicesBundlePage(service)
        ServiceType.Journey -> ServicesJourneyPage(service)
    }
}

// MARK: - Curated content

/**
 * Hand-written start page content (the rest of the catalogue gets the generic page until
 * service content is written in the services phase).
 */
private data class ServicesCuratedContent(
    val chips: List<Pair<String, String>>,
    val needs: List<String>,
    val costs: List<Pair<String, BigDecimal>>,
    val takes: String,
    val otherWays: List<String>,
    val related: List<String>,
) {
    companion object {
        val all: Map<String, ServicesCuratedContent> = mapOf(
            "renew-vehicle-registration" to ServicesCuratedContent(
                chips = listOf("clock" to "5 minutes", "banknote" to "AED 420", "building.columns" to "RTA"),
                needs = listOf(
                    "UAE PASS to sign in. We fill in your details from it.",
                    "Valid insurance. We check it with your insurer.",
                    "A passed inspection if the car is over 3 years old.",
                ),
                costs = listOf("Registration renewal" to BigDecimal(400), "Knowledge and innovation fees" to BigDecimal(20)),
                takes = "About 5 minutes. Your digital registration card is ready straight away. A printed card arrives in 2 working days if you ask for one.",
                otherWays = listOf("Visit an RTA customer happiness centre"),
                related = listOf("transfer-vehicle-ownership", "find-testing-centre", "pay-traffic-fine"),
            ),
        )
    }
}

// MARK: - Start page

private sealed interface ServicesPrimary {
    data class Pay(val request: PaymentRequest) : ServicesPrimary
    data object Renew : ServicesPrimary
    data class Donate(val option: CatalogueService.Option) : ServicesPrimary
    data class Request(val request: RequestItem) : ServicesPrimary
    data class Draft(val draft: DraftItem) : ServicesPrimary
    data object Open : ServicesPrimary
    data object Start : ServicesPrimary
}

private val donationAmounts = listOf(10, 50, 100, 500)

@Composable
private fun ServicesStartPage(service: CatalogueService) {
    val store = LocalStore.current; val router = LocalRouter.current; val view = LocalView.current
    val extras = LaunchOptions.extras

    var vehicleID by rememberSaveable { mutableStateOf<String?>(null) }
    var choosesVehicle by rememberSaveable { mutableStateOf(false) }
    var charityID by rememberSaveable { mutableStateOf(extras["servicesCharity"]) }
    var amount by rememberSaveable { mutableIntStateOf(extras["servicesAmount"]?.toIntOrNull()?.takeIf { it in donationAmounts } ?: 50) }
    var showsAllCharities by rememberSaveable { mutableStateOf(ServicesDebug.flag("servicesAllCharities")) }

    LaunchedEffect(Unit) {
        if (ServicesDebug.flag("servicesVehicleDialog") && !ServicesDebug.dialogOpened) { ServicesDebug.dialogOpened = true; choosesVehicle = true }
    }

    val curated = ServicesCuratedContent.all[service.id]
    val owed = store.servicesPayables(service.id)
    val requests = store.servicesRequests(service)
    val draft = store.servicesDraft(service.id)
    val isRenewal = service.id == "renew-vehicle-registration"
    val options = service.options.orEmpty()
    /** The donate flow and payable services work in the demo even if the record isn't live. */
    val hasWorkingFlow = owed.isNotEmpty() || options.isNotEmpty()

    val vehicle: Vehicle? = if (!isRenewal || store.persona == null) null else
        vehicleID?.let { id -> store.state.vehicles.firstOrNull { it.id == id } } ?: store.servicesVehicleForRenewal()

    val primary: ServicesPrimary = store.servicesPaymentRequest(owed)?.let { ServicesPrimary.Pay(it) }
        ?: if (isRenewal) ServicesPrimary.Renew
        else options.firstOrNull()?.let { first -> ServicesPrimary.Donate(options.firstOrNull { it.id == charityID } ?: first) }
            ?: requests.firstOrNull()?.let { ServicesPrimary.Request(it) }
            ?: draft?.let { ServicesPrimary.Draft(it) }
            ?: if (service.kind != "service") ServicesPrimary.Open else ServicesPrimary.Start

    fun perform(p: ServicesPrimary) {
        when (p) {
            is ServicesPrimary.Pay -> router.present(SheetRoute.Payment(p.request))
            ServicesPrimary.Renew -> router.present(SheetRoute.ComingSoonSheet(ServicesData.comingSoon(
                service, ComingSoon.Kind.ServicesPhase,
                "Renewing a registration in DubaiNow comes in the next phase. You can already ask DubaiNow to do it for you.",
            )))
            is ServicesPrimary.Donate -> {
                val a = BigDecimal(amount)
                router.present(SheetRoute.Payment(PaymentRequest(
                    id = "donate-${p.option.id}-${Money.figure(a)}",
                    title = "Donate to ${p.option.name}",
                    lines = listOf(PaymentLine(p.option.name, p.option.owner ?: "Registered charity", a, "heart.fill", Hue.Lilac)),
                    purpose = PaymentPurpose.Donation(p.option.name, a),
                )))
            }
            is ServicesPrimary.Request -> router.push(Route.Request(p.request.id))
            is ServicesPrimary.Draft -> router.present(SheetRoute.ComingSoonSheet(ServicesData.comingSoon(
                service, ComingSoon.Kind.ServicesPhase,
                "Your draft is saved with ${p.draft.stepsDone} of ${p.draft.stepsTotal} steps done. The steps inside this service are designed next, and your draft will open from here.",
            )))
            ServicesPrimary.Open -> router.present(SheetRoute.ComingSoonSheet(ServicesData.comingSoon(service, ComingSoon.Kind.NotInDemo)))
            ServicesPrimary.Start -> router.present(SheetRoute.ComingSoonSheet(ServicesData.comingSoon(service)))
        }
    }

    val primaryTitle = when (primary) {
        is ServicesPrimary.Pay -> "Pay ${Money.aed(primary.request.total)}"
        ServicesPrimary.Renew -> "Start now"
        is ServicesPrimary.Donate -> "Donate ${Money.aed(BigDecimal(amount), trimWhole = true)}"
        is ServicesPrimary.Request -> if (primary.request.status is RequestItem.Status.YourTurn) "Continue your request" else "View your request"
        is ServicesPrimary.Draft -> "Continue your draft"
        ServicesPrimary.Open -> "Open"
        ServicesPrimary.Start -> "Start now"
    }

    val statusNote = if (hasWorkingFlow) null else when (service.status) {
        "maintenance" -> "This service is under maintenance. You can still read about it here."
        "to publish", "proposed", "to confirm" -> "Not available yet. It will open from here when it's ready."
        else -> null
    }

    val state = rememberLazyListState()
    // Blocks of the page, in order (each is one lazy item, so `servicesScroll personal|details` can find it).
    val keys = buildList {
        add("header")
        if (vehicle != null) add("personal-vehicle")
        if (owed.isNotEmpty()) add("personal-payables")
        requests.forEach { add("personal-request-${it.id}") }
        if (draft != null && requests.isEmpty()) add("personal-draft")
        if (options.isNotEmpty()) add("personal-donation")
        add("actions")
        add("details")
        add("footer")
    }
    ServicesLaunchScroll(state) { key ->
        when (key) {
            "personal" -> keys.indexOfFirst { it.startsWith("personal") }.takeIf { it >= 0 } ?: keys.indexOf("actions")
            else -> keys.indexOf(key).takeIf { it >= 0 }
        }
    }

    if (choosesVehicle) {
        ServicesVehicleDialog(
            vehicles = store.state.vehicles, selected = vehicle?.id,
            onSelect = { vehicleID = it; choosesVehicle = false; DNHaptics.selection(view) },
            onDismiss = { choosesVehicle = false },
        )
    }

    ServicesPage(base = DNColor.surfacePrimary, wash = service.hue.start, state = state, actions = { ServicesRecordActions(service) }) {
        item(key = "header") { ServicesStartHeader(service, curated, hasWorkingFlow) }
        if (vehicle != null) item(key = "personal-vehicle") { ServicesVehicleCard(vehicle, ServicesInset.padding(top = 24.dp)) }
        if (owed.isNotEmpty()) item(key = "personal-payables") { ServicesPayablesCard(owed, ServicesInset.padding(top = 14.dp)) }
        requests.forEach { r -> item(key = "personal-request-${r.id}") { ServicesRequestCard(r, ServicesInset.padding(top = 14.dp)) } }
        if (draft != null && requests.isEmpty()) item(key = "personal-draft") { ServicesDraftCard(draft, ServicesInset.padding(top = 14.dp)) }
        if (options.isNotEmpty()) item(key = "personal-donation") {
            ServicesDonation(
                options, chosen = charityID ?: options.first().id, onChoose = { DNHaptics.selection(view); charityID = it },
                showsAll = showsAllCharities, onToggleAll = { showsAllCharities = !showsAllCharities },
                amount = amount, onAmount = { DNHaptics.selection(view); amount = it },
                modifier = ServicesInset.padding(top = 24.dp),
            )
        }
        item(key = "actions") {
            Column(ServicesInset.padding(top = if (keys[1] == "actions") 24.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val arrow = primary == ServicesPrimary.Start || primary == ServicesPrimary.Renew
                ServicesButton(primaryTitle, onClick = { perform(primary) }, trailing = if (arrow) Icons.AutoMirrored.Rounded.ArrowForward else null)
                if (isRenewal && !store.isGuest) {
                    ServicesButton(
                        "Ask DubaiNow to do it", onClick = { router.openAssistant("Renew my car registration") },
                        kind = DNButtonKind.Secondary, leading = Icons.Rounded.AutoAwesome,
                    )
                    if (store.state.vehicles.size > 1) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            DNTextButton("Renew a different vehicle", onClick = { choosesVehicle = true })
                        }
                    }
                }
                statusNote?.let {
                    Text(it, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item(key = "details") {
            Column(ServicesInset.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
                if (curated != null) ServicesCuratedSections(service, curated) else ServicesAboutSections(service)
            }
        }
        item(key = "footer") {
            ServicesFooter(
                if (service.owner == null) "The owner of this service is being confirmed." else "Run by ${ServicesData.fullName(service.owner)}.",
                ServicesInset.padding(top = 30.dp),
            )
        }
    }
}

// MARK: Header

@Composable
private fun ServicesStartHeader(service: CatalogueService, curated: ServicesCuratedContent?, hasWorkingFlow: Boolean) {
    Column(ServicesInset.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ServicesHeaderDisc(service.icon, service.hue, size = 64.dp)
        ServicesBreadcrumb(service)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(service.name, style = DNTextStyle.Title1.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
            ServicesData.alsoCalled(service)?.let {
                Text("Also called $it", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
            }
        }
        ServicesFlow(Modifier.padding(top = 4.dp)) {
            if (curated != null) {
                curated.chips.forEach { (symbol, text) -> ServicesInfoChip(servicesIcon(symbol), text) }
            } else {
                ServicesInfoChip(servicesIcon("building.columns"), service.ownerLabel)
                service.audienceLabel?.let { ServicesInfoChip(servicesIcon("person.2"), it) }
                val status = service.statusLabel
                if (status != null && !hasWorkingFlow) {
                    val maintenance = service.status == "maintenance"
                    ServicesInfoChip(
                        servicesIcon(if (maintenance) "wrench.and.screwdriver" else "clock"), status,
                        tint = if (maintenance) DNColor.statusWarningForeground else DNColor.statusInfoForeground,
                        fill = if (maintenance) DNColor.statusWarningSurface else DNColor.statusInfoSurface,
                    )
                }
            }
        }
        if (service.description.isNotEmpty()) {
            Text(service.description, style = DNTextStyle.Callout.style, color = DNColor.textPrimary, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** "Driving and transport › Vehicles" — each part opens its page. */
@Composable
private fun ServicesBreadcrumb(service: CatalogueService) {
    val router = LocalRouter.current
    val topicID = service.topic ?: return
    val topic = Catalogue.shared.topic(topicID) ?: return
    val sub = service.subtopic?.let { id -> topic.subtopics.firstOrNull { it.id == id } }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ServicesCrumb(topic.name, Modifier.weight(1f, fill = false)) { router.push(Route.Topic(topicID)) }
        if (sub != null) {
            Icon(DNIcons.forSymbol("chevron.forward"), contentDescription = null, tint = DNColor.neutral400, modifier = Modifier.size(16.dp))
            ServicesCrumb(sub.name, Modifier.weight(1f, fill = false)) { router.push(Route.Subtopic(topicID, sub.id)) }
        }
    }
}

@Composable
private fun ServicesCrumb(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            title, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textLink,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

// MARK: Personal cards

private val ServicesGreyCard = RoundedCornerShape(DNRadius.brief)

@Composable
private fun ServicesVehicleCard(v: Vehicle, modifier: Modifier = Modifier) {
    val store = LocalStore.current
    val days = DemoDate.daysFromToday(v.registrationExpiry)
    val (expiry, expiryColor) = when {
        days < 0 -> "Expired ${DemoDate.short(v.registrationExpiry)}" to DNColor.statusErrorForeground
        days <= 30 -> "Expires ${DemoDate.short(v.registrationExpiry)} · ${ServicesText.inDays(v.registrationExpiry)}" to DNColor.statusWarningForeground
        else -> "Valid until ${DemoDate.full(v.registrationExpiry)}" to DNColor.textSecondary
    }
    val fines = store.payables.filter { it.id in v.fineIDs }
    val fineTotal = fines.fold(BigDecimal.ZERO) { a, p -> a + p.amount }
    val age = LocalDate.now().year - v.year
    val inspected = v.lastInspection?.let { DemoDate.daysFromToday(it) >= -60 } ?: false

    Column(modifier.fillMaxWidth().clip(ServicesGreyCard).background(DNColor.surfaceSecondary)) {
        Row(Modifier.padding(16.dp).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ServicesPlate(v.plate.code, v.plate.number, v.plate.emirate)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(v.name, style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
                Text(expiry, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = expiryColor)
            }
        }
        HorizontalDivider(thickness = 1.dp, color = DNColor.borderSubtle)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("BEFORE YOU RENEW", style = DNTextStyle.Overline.style, color = DNColor.textSecondary, modifier = Modifier.semantics { heading() })
            if (v.insuranceExpiry.isAfter(v.registrationExpiry)) {
                ServicesCheckRow(true, "Insurance is valid until ${DemoDate.full(v.insuranceExpiry)}")
            } else {
                ServicesCheckRow(false, "Insurance ends ${DemoDate.full(v.insuranceExpiry)}", "Renew it with ${v.insurer} first.")
            }
            val last = v.lastInspection
            when {
                last != null && inspected -> ServicesCheckRow(true, "Passed inspection on ${DemoDate.long(last)}")
                age >= 3 -> ServicesCheckRow(false, "Needs an inspection", "Cars over 3 years old need one before renewing.")
                else -> ServicesCheckRow(true, "No inspection needed yet")
            }
            if (fines.isEmpty()) {
                ServicesCheckRow(true, "No unpaid fines")
            } else {
                ServicesCheckRow(
                    false, "${ServicesText.count(fines.size, "unpaid fine")} of ${Money.aed(fineTotal, trimWhole = true)}",
                    "You can pay ${if (fines.size == 1) "it" else "them"} as part of renewing.",
                )
            }
        }
    }
}

/** A ✓ or ! line in "Before you renew". */
@Composable
private fun ServicesCheckRow(ok: Boolean, title: String, detail: String? = null) {
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "${if (ok) "Done" else "Needs attention"}: $title. ${detail ?: ""}" },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(if (ok) DNColor.statusSuccessSurface else DNColor.statusWarningSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (ok) Icons.Rounded.Check else Icons.Rounded.PriorityHigh, contentDescription = null,
                tint = if (ok) DNColor.statusSuccessForeground else DNColor.statusWarningForeground, modifier = Modifier.size(14.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = DNTextStyle.Subheadline.style, color = DNColor.textPrimary)
            detail?.let { Text(it, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary) }
        }
    }
}

@Composable
private fun ServicesPayablesCard(owed: List<com.salehtl.dubainownext.data.Payable>, modifier: Modifier = Modifier) {
    val store = LocalStore.current; val router = LocalRouter.current
    val large = servicesLargeText()
    Column(modifier.fillMaxWidth().clip(ServicesGreyCard).background(DNColor.surfaceSecondary)) {
        owed.forEachIndexed { i, p ->
            if (i > 0) ServicesDivider(70.dp, DNColor.borderSubtle)
            Row(Modifier.padding(16.dp).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HueIcon(p.symbol, p.hue, size = 40.dp)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val title = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold)
                    if (large) {
                        // At the largest text sizes the amount goes under the title so neither is squeezed.
                        Text(p.title, style = title, color = DNColor.textPrimary)
                        Text(Money.aed(p.amount), style = title.copy(fontFeatureSettings = "tnum"), color = DNColor.textPrimary)
                    } else Row(verticalAlignment = Alignment.Top) {
                        Text(p.title, style = title, color = DNColor.textPrimary, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Text(Money.aed(p.amount), style = title.copy(fontFeatureSettings = "tnum"), color = DNColor.textPrimary)
                    }
                    Text("${p.entity} · ${p.detail}", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
                    val due = p.due
                    if (due != null) {
                        Text(DemoDate.dueLabel(due), style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.statusWarningForeground)
                    } else p.note?.let {
                        Text(it, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.statusErrorForeground)
                    }
                }
            }
        }
        if (store.showsPayAll && store.payables.size > owed.size) {
            HorizontalDivider(thickness = 1.dp, color = DNColor.borderSubtle)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) { router.push(Route.Bundle("pay-all")) }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Pay with your other bills · ${Money.aed(store.payAllTotal)}",
                    style = DNTextStyle.Callout.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textLink, modifier = Modifier.weight(1f),
                )
                Icon(DNIcons.forSymbol("chevron.forward"), contentDescription = null, tint = DNColor.listRowChevron, modifier = Modifier.size(20.dp))
            }
        }
    }
}

private val RequestItem.servicesTone: ChipTone
    get() = when (status) {
        is RequestItem.Status.YourTurn -> ChipTone.Warning
        is RequestItem.Status.InReview -> ChipTone.Info
        else -> ChipTone.Success
    }

@Composable
private fun ServicesRequestCard(r: RequestItem, modifier: Modifier = Modifier) {
    val router = LocalRouter.current
    val current = r.currentStepIndex ?: r.steps.size
    Column(
        modifier.fillMaxWidth().clip(ServicesGreyCard).background(DNColor.surfaceSecondary)
            .clickable(onClickLabel = "Open the request tracker") { router.push(Route.Request(r.id)) }.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("IN PROGRESS", style = DNTextStyle.Overline.style, color = DNColor.textSecondary)
        Text(r.title, style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
        Text(r.statusLine, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = servicesToneColor(r.servicesTone))
        SegmentedProgress(total = r.steps.size, done = current, height = 6.dp)
        r.currentStepIndex?.let { i ->
            Text("Step ${i + 1} of ${r.steps.size}: ${r.steps[i].title}", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        }
    }
}

@Composable
private fun ServicesDraftCard(d: DraftItem, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().clip(ServicesGreyCard).background(DNColor.surfaceSecondary).padding(16.dp).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("YOU HAVE A DRAFT", style = DNTextStyle.Overline.style, color = DNColor.textSecondary)
        Text(
            "${d.stepsDone} of ${d.stepsTotal} steps done · saved ${ServicesText.relativeInSentence(d.savedOn)}",
            style = DNTextStyle.RowTitle.style, color = DNColor.textPrimary,
        )
        SegmentedProgress(total = d.stepsTotal, done = d.stepsDone, height = 6.dp)
        Text("Drafts are kept for 30 days.", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
    }
}

// MARK: Donation (services with options)

@Composable
private fun ServicesDonation(
    options: List<CatalogueService.Option>,
    chosen: String,
    onChoose: (String) -> Unit,
    showsAll: Boolean,
    onToggleAll: () -> Unit,
    amount: Int,
    onAmount: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = if (showsAll) options else options.take(5)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ServicesPageHeading("Choose a charity")
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.listGroup)).background(DNColor.surfacePrimary)
                .then(Modifier.servicesOutline(DNRadius.listGroup)).selectableGroup(),
        ) {
            visible.forEachIndexed { i, option ->
                val selected = option.id == chosen
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp)
                        .selectable(selected = selected, role = Role.RadioButton) { onChoose(option.id) }
                        .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(option.name, style = DNTextStyle.RowTitle.style, color = DNColor.listRowTitle)
                        option.description?.let { Text(it, style = DNTextStyle.Footnote.style, color = DNColor.listRowSupporting) }
                    }
                    RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = DNColor.actionAccent, unselectedColor = DNColor.neutral400))
                    Spacer(Modifier.width(4.dp))
                }
                if (i < visible.lastIndex) ServicesDivider()
            }
        }
        if (options.size > 5) {
            DNTextButton(if (showsAll) "Show fewer" else "Show all ${options.size} charities", onClick = onToggleAll)
        }
        ServicesPageHeading("How much", Modifier.padding(top = 6.dp))
        if (servicesFontScale() > 1.15f) {
            // Four amounts don't fit as segments at large text sizes; wrapping M3 filter chips do.
            ServicesFlow(Modifier.fillMaxWidth()) {
                donationAmounts.forEach { a -> ServicesChoiceChip(Money.aed(BigDecimal(a), trimWhole = true), amount == a) { onAmount(a) } }
            }
        } else SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            donationAmounts.forEachIndexed { i, a ->
                SegmentedButton(
                    selected = amount == a, onClick = { onAmount(a) },
                    shape = SegmentedButtonDefaults.itemShape(i, donationAmounts.size),
                    colors = servicesSegmentedColors(),
                    icon = {},
                ) { Text(Money.aed(BigDecimal(a), trimWhole = true), style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"), maxLines = 1) }
            }
        }
    }
}

// MARK: Vehicle chooser

/** "Which vehicle?" (M3 dialog with a single-choice list). */
@Composable
private fun ServicesVehicleDialog(vehicles: List<Vehicle>, selected: String?, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DNColor.surfacePrimary,
        title = { Text("Which vehicle?", style = DNTextStyle.Title2.style, color = DNColor.textPrimary) },
        text = {
            Column(Modifier.selectableGroup()) {
                vehicles.forEach { v ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp))
                            .selectable(selected = v.id == selected, role = Role.RadioButton) { onSelect(v.id) }.padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = v.id == selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = DNColor.actionAccent))
                        Column(Modifier.weight(1f)) {
                            Text(v.name, style = DNTextStyle.RowTitle.style, color = DNColor.textPrimary)
                            Text("${v.plate.code} ${v.plate.number} · registration ends ${DemoDate.short(v.registrationExpiry)}", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", style = DNTextStyle.Callout.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textLink) } },
    )
}

// MARK: Details

@Composable
private fun ServicesCuratedSections(service: CatalogueService, c: ServicesCuratedContent) {
    val router = LocalRouter.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ServicesPageHeading("What you need")
        c.needs.forEach { need ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•", style = DNTextStyle.Callout.style, color = DNColor.textPrimary, modifier = Modifier.clearAndSetSemantics {})
                Text(need, style = DNTextStyle.Callout.style, color = DNColor.textPrimary, modifier = Modifier.weight(1f))
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ServicesPageHeading("How much it costs")
        ServicesFactTable(
            c.costs.map { it.first to Money.aed(it.second, trimWhole = true) },
            total = "Total" to Money.aed(c.costs.fold(BigDecimal.ZERO) { a, x -> a + x.second }, trimWhole = true),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ServicesPageHeading("How long it takes")
        Text(c.takes, style = DNTextStyle.Callout.style, color = DNColor.textPrimary)
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ServicesPageHeading("Other ways to do this")
        c.otherWays.forEach { way ->
            ServicesLink(way) {
                router.present(SheetRoute.ComingSoonSheet(ComingSoon(
                    title = way, detail = "Centre locations and opening hours aren't part of this demo.",
                    owner = service.owner, symbol = "map.fill", hue = service.hue, kind = ComingSoon.Kind.NotInDemo,
                )))
            }
        }
    }
    ServicesRelated(c.related.mapNotNull { Catalogue.service(it) })
}

@Composable
private fun ServicesAboutSections(service: CatalogueService) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ServicesPageHeading("About this service")
        ServicesFactTable(buildList {
            add("Run by" to (service.owner ?: "To be confirmed"))
            add("Who it's for" to (service.audienceLabel?.removePrefix("For ")?.replaceFirstChar { it.uppercase() } ?: "Everyone"))
            service.statusLabel?.let { add("Status" to it) }
        })
    }
    val partOf = ServicesData.partOf(service)
    if (partOf.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ServicesPageHeading("Also done as part of")
            ServicesRowGroup(partOf, separatorInset = 68.dp) { ServicesRow(it, trailing = RowTrailing.Chevron, showsContext = false) }
        }
    }
    ServicesRelated(ServicesData.related(service))
}

@Composable
private fun ServicesRelated(related: List<CatalogueService>) {
    val router = LocalRouter.current
    if (related.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ServicesPageHeading("Related")
        related.forEach { s -> ServicesLink(s.name) { router.open(s) } }
    }
}

/** Label/value rows on a grey card ("How much it costs", "About this service"). */
@Composable
private fun ServicesFactTable(rows: List<Pair<String, String>>, total: Pair<String, String>? = null) {
    BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DNColor.surfaceSecondary).padding(horizontal = 16.dp)) {
        // The value is measured first and takes up to two thirds of the width; the label wraps in
        // the rest. At the largest text sizes each value goes under its label instead.
        val valueMax = maxWidth * 0.65f
        val stacked = servicesLargeText()
        Column(Modifier.fillMaxWidth()) {
            rows.forEachIndexed { i, (label, value) ->
                if (i > 0) HorizontalDivider(thickness = 1.dp, color = DNColor.borderSubtle)
                if (stacked) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(label, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                        Text(value, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(label, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary, modifier = Modifier.weight(1f))
                        Text(
                            value, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary,
                            textAlign = TextAlign.End, modifier = Modifier.widthIn(max = valueMax),
                        )
                    }
                }
            }
            if (total != null) {
                HorizontalDivider(thickness = 1.dp, color = DNColor.borderSubtle)
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                    Text(total.first, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Bold), color = DNColor.textPrimary, modifier = Modifier.weight(1f))
                    Text(total.second, style = DNTextStyle.Headline.style.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), color = DNColor.textPrimary)
                }
            }
        }
    }
}

/** A 1 dp subtle outline (the charity list sits on a white page). */
private fun Modifier.servicesOutline(radius: Dp): Modifier = this.border(1.dp, DNColor.borderSubtle, RoundedCornerShape(radius))
