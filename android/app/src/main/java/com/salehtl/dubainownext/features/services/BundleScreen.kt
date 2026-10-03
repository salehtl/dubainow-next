package com.salehtl.dubainownext.features.services

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.FamilyVisasStatus
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.data.hue
import com.salehtl.dubainownext.data.icon
import com.salehtl.dubainownext.data.symbol
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.Avatar
import com.salehtl.dubainownext.design.components.ChipTone
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.HueIcon
import com.salehtl.dubainownext.design.components.MoneyText
import com.salehtl.dubainownext.design.components.RowTrailing
import com.salehtl.dubainownext.design.components.ServiceTypeBadge
import com.salehtl.dubainownext.design.components.StatusChip
import com.salehtl.dubainownext.design.components.dnCard
import com.salehtl.dubainownext.design.components.dnElevation
import com.salehtl.dubainownext.design.DNTextStyle.NumericMedium

/**
 * A bundle page ("All at once"): the same job across many accounts, done in one go
 * (Paper C2 blueprints: Pay all, Family visas, Certificates).
 */
@Composable
fun BundleScreen(serviceId: String) {
    val service = Catalogue.shared.service(serviceId)
    when (service?.type) {
        null -> ServicesMissingRecord(serviceId)
        ServiceType.Bundle -> ServicesBundlePage(service)
        ServiceType.Journey -> ServicesJourneyPage(service)
        ServiceType.Standalone -> ServiceStartScreen(serviceId)
    }
}

/** The catalogue describes bundles for staff ("the resident…"); the page speaks to the resident. */
private val servicesBundleDoesTheWork = mapOf(
    "pay-all" to "Finds everything due across your accounts and pays it in one checkout. You can untick anything you'd rather pay later.",
    "renew-family-visas" to "Lists every visa you sponsor that's due, then books the medical tests, renews the Emirates IDs and checks insurance as part of one application.",
    "get-certificates" to "Tick the certificates and letters you need, for a job, a loan or a visa. DubaiNow asks each entity and keeps the results in your Documents.",
    "apply-all-pod-benefits" to "One application checks which benefits you're eligible for and applies for every one you qualify for.",
)

@Composable
internal fun ServicesBundlePage(service: CatalogueService) {
    val store = LocalStore.current; val router = LocalRouter.current
    val isPayAll = service.id == "pay-all"
    val isFamilyVisas = service.id == "renew-family-visas"
    val signedOut = store.isGuest || store.persona == null
    val parts = service.resolvedParts
    val missing = (service.parts ?: emptyList()).size - parts.size
    val work = servicesBundleDoesTheWork[service.id] ?: service.doesTheWork
    val familyVisas = store.state.familyVisas.takeIf { isFamilyVisas && store.persona != null }
    val countLine = store.servicesBundleCount(service)?.let { service.countLine(it) } ?: service.covers?.let { "Covers $it" }
    // Only Pay all and Family visas (signed in) have a personal card; the rest go straight to the action.
    val hasPersonal = isPayAll || familyVisas != null

    val state = rememberLazyListState()
    val keys = buildList {
        add("header"); if (hasPersonal) add("personal"); add("actions")
        if (work != null) add("work")
        if (!service.views.isNullOrEmpty()) add("views")
        if (parts.isNotEmpty()) { add("covers"); parts.forEach { add("part-${it.id}") }; if (missing > 0) add("missing") }
        if (!service.entities.isNullOrEmpty()) add("entities")
        add("footer")
    }
    ServicesLaunchScroll(state) { key -> (if (key == "details") keys.indexOf("work") else keys.indexOf(key)).takeIf { it >= 0 } }

    ServicesPage(wash = service.hue.start, state = state, actions = { ServicesRecordActions(service) }) {
        item(key = "header") {
            Column(ServicesInset.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ServicesStackedDiscs(parts)
                ServiceTypeBadge(ServiceType.Bundle)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(service.name, style = DNTextStyle.Title1.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
                    countLine?.let { Text(it, style = DNTextStyle.Headline.style, color = DNColor.textLink) }
                    Text(service.description, style = DNTextStyle.Callout.style, color = DNColor.textSecondary)
                }
            }
        }
        if (hasPersonal) item(key = "personal") {
            Box(ServicesInset.padding(top = 24.dp)) {
                when {
                    isPayAll && signedOut -> ServicesNote(
                        Icons.Rounded.AccountCircle, "Sign in to see what you owe",
                        "DubaiNow finds every bill, fine and top-up across your accounts.",
                    )
                    isPayAll && store.payables.isEmpty() -> ServicesNote(
                        Icons.Rounded.Verified, "Nothing to pay",
                        "You're all paid up. We'll tell you when a new bill or fine arrives.", tint = DNColor.statusSuccessForeground,
                    )
                    isPayAll -> ServicesBundlePayables()
                    familyVisas != null -> ServicesFamilyVisasCard(familyVisas)
                }
            }
        }
        item(key = "actions") {
            Column(ServicesInset.padding(top = if (hasPersonal) 14.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isPayAll) {
                    if (signedOut) {
                        DNButton("Sign in", onClick = { router.present(SheetRoute.Profile) })
                    } else if (store.payables.isNotEmpty()) {
                        DNButton("Pay ${Money.aed(store.payAllTotal)}", onClick = { router.present(SheetRoute.Payment(store.payAllRequest())) })
                        Text(
                            "No money moves in this demo.", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    ServicesButton(
                        "Start now", trailing = Icons.AutoMirrored.Rounded.ArrowForward,
                        onClick = { router.present(SheetRoute.ComingSoonSheet(ServicesData.comingSoon(service, ComingSoon.Kind.ServicesPhase))) },
                    )
                }
            }
        }
        if (work != null) item(key = "work") {
            Column(ServicesInset.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ServicesPageHeading("What it does for you")
                Text(work, style = DNTextStyle.Callout.style, color = DNColor.textPrimary)
            }
        }
        service.views?.takeIf { it.isNotEmpty() }?.let { views ->
            item(key = "views") {
                Column(ServicesInset.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ServicesPageHeading("Ways to see it")
                    ServicesFlow { views.forEach { ServicesInfoChip(servicesIcon("line.3.horizontal.decrease"), it, fill = DNColor.surfacePrimary) } }
                }
            }
        }
        if (parts.isNotEmpty()) {
            item(key = "covers") {
                ServicesSectionHeader(
                    "Covers", ServicesInset.padding(top = 20.dp, bottom = 4.dp),
                    subtitle = "${ServicesText.count(parts.size, "service")}. Each one still works on its own.",
                )
            }
            servicesGroupItems(parts, key = { "part-${it.id}" }, separatorInset = 68.dp) { ServicesRow(it, trailing = RowTrailing.Chevron) }
            if (missing > 0) item(key = "missing") {
                Text(
                    "${ServicesText.count(missing, "more service")} will join when ${if (missing == 1) "it's" else "they're"} in DubaiNow.",
                    style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, modifier = ServicesInset.padding(start = 4.dp, top = 8.dp),
                )
            }
        }
        service.entities?.takeIf { it.isNotEmpty() }?.let { entities ->
            item(key = "entities") {
                Column(ServicesInset.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ServicesPageHeading("Who DubaiNow talks to")
                    ServicesFlow { entities.forEach { ServicesInfoChip(servicesIcon("building.columns"), it, fill = DNColor.surfacePrimary) } }
                }
            }
        }
        item(key = "footer") {
            ServicesFooter(
                "Run by ${ServicesData.fullName(service.owner)}, with ${ServicesText.count(service.entities?.size ?: 0, "provider")}.",
                ServicesInset.padding(top = 28.dp),
            )
        }
    }
}

/** Everything payable, with the total (Pay all). */
@Composable
private fun ServicesBundlePayables() {
    val store = LocalStore.current
    // At the largest text sizes the amount moves under the title so the title isn't squeezed.
    val large = servicesLargeText()
    Column(Modifier.fillMaxWidth().dnCard(DNRadius.brief)) {
        store.payables.forEach { p ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val amount: @Composable () -> Unit = {
                    Text(Money.aed(p.amount), style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"), color = DNColor.textPrimary)
                }
                HueIcon(p.symbol, p.hue, size = 40.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(p.title, style = DNTextStyle.RowTitle.style, color = DNColor.textPrimary)
                    Text(
                        listOfNotNull(p.entity, p.due?.let { DemoDate.dueLabel(it) } ?: p.note).joinToString(" · "),
                        style = DNTextStyle.Footnote.style, color = DNColor.textSecondary,
                    )
                    if (large) amount()
                }
                if (!large) amount()
            }
            ServicesDivider(68.dp)
        }
        Row(Modifier.fillMaxWidth().padding(16.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.Bottom) {
            Text("Total", style = DNTextStyle.Headline.style, color = DNColor.textPrimary, modifier = Modifier.weight(1f).padding(bottom = 4.dp))
            MoneyText(store.payAllTotal, size = NumericMedium)
        }
    }
}

/** The family members whose visas you sponsor, with the ones due (Family visas). */
@Composable
private fun ServicesFamilyVisasCard(fv: FamilyVisasStatus) {
    val store = LocalStore.current
    val members = store.state.family.filter { it.id in fv.memberIDs }
    Column(Modifier.fillMaxWidth().dnCard(DNRadius.brief)) {
        members.forEachIndexed { i, m ->
            val due = m.id in fv.dueMemberIDs
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Avatar(m.initials, m.hue, size = 40.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(m.name, style = DNTextStyle.RowTitle.style, color = DNColor.textPrimary)
                    Text(m.relation + (m.visaExpiry?.let { " · visa expires ${DemoDate.short(it)}" } ?: ""), style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
                }
                if (due) StatusChip("Due", tone = ChipTone.Warning, onWhite = true)
                else Text("Not due", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
            }
            if (i < members.lastIndex) ServicesDivider(68.dp)
        }
    }
}

/** A bundle's first parts as overlapping discs with white rings (bundle page header). */
@Composable
private fun ServicesStackedDiscs(parts: List<CatalogueService>, size: Dp = 52.dp) {
    // Up to three parts with different glyphs (Pay all has several fines).
    val seen = mutableSetOf<String>()
    val picked = parts.filter { seen.add(it.symbol) }.toMutableList()
    if (picked.size < 3) picked += parts.filter { it !in picked }
    val distinct = picked.take(3)
    Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(-(size * 0.36f))) {
        distinct.forEachIndexed { index, part ->
            Box(
                Modifier.zIndex((3 - index).toFloat()).dnElevation(size).clip(CircleShape).background(DNColor.surfacePrimary).padding(3.dp),
            ) { HueIcon(part.icon, part.hue, size = size) }
        }
    }
}
