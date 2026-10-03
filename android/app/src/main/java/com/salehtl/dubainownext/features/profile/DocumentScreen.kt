package com.salehtl.dubainownext.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.DocumentItem
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNIcons
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Inter
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.StatusChip
import com.salehtl.dubainownext.design.components.dnElevation

/** One document: the card large, its status and what you can do with it. */
@Composable
fun DocumentScreen(documentId: String) {
    val store = LocalStore.current
    val doc = store.state.documents.firstOrNull { it.id == documentId }
    if (doc == null) {
        ProfileScreen("Document") {
            ProfileEmptyState(Icons.Rounded.Description, "Document not found", "It isn't linked to your account any more.")
        }
    } else {
        ProfileDocumentDetail(doc)
    }
}

private data class ProfileDocAction(val title: String, val subtitle: String?, val route: Route)
private data class ProfileDocInfo(val issuer: String, val actions: List<ProfileDocAction>, val note: String?)

private fun profileServiceAction(id: String, title: String): List<ProfileDocAction> {
    val route = profileRouteForService(id) ?: return emptyList()
    return listOf(ProfileDocAction(title, Catalogue.service(id)?.ownerLabel, route))
}

private fun profileDocInfo(store: DemoStore, document: DocumentItem): ProfileDocInfo {
    val t = document.title.lowercase()
    fun request(serviceID: String) = store.state.requests.firstOrNull { it.serviceID == serviceID }
    return when {
        "emirates id" in t -> ProfileDocInfo("ICP", profileServiceAction("get-or-renew-emirates-id", "Renew your Emirates ID"), null)
        "visa" in t -> request("renew-residence-visa")?.let { r ->
            ProfileDocInfo("GDRFA", listOf(ProfileDocAction("Track your renewal", r.statusLine, Route.Request(r.id))), null)
        } ?: ProfileDocInfo("GDRFA", profileServiceAction("renew-residence-visa", "Renew your residence visa"), null)
        "driving licence" in t -> request("renew-driving-licence")?.let { r ->
            ProfileDocInfo("RTA", listOf(ProfileDocAction("Track your renewal", r.statusLine, Route.Request(r.id))), null)
        } ?: ProfileDocInfo("RTA", profileServiceAction("renew-driving-licence", "Renew your driving licence"), null)
        "vehicle registration" in t -> {
            val actions = mutableListOf<ProfileDocAction>()
            store.state.vehicles.firstOrNull { document.number.contains(it.plate.number) }?.let { v ->
                actions += ProfileDocAction("See your ${v.model}", "${v.plate.code} ${v.plate.number}", Route.Vehicle(v.id))
            }
            actions += profileServiceAction("renew-vehicle-registration", "Renew your vehicle registration")
            ProfileDocInfo("RTA", actions, null)
        }
        "tenancy" in t -> ProfileDocInfo("Dubai Land Department", profileServiceAction("renew-tenancy-contract", "Renew your tenancy contract"), null)
        "passport" in t -> {
            val country = document.number.split("·").firstOrNull()?.trim() ?: "Your country"
            if (country == "United Arab Emirates") ProfileDocInfo("ICP", emptyList(), "Passport services in DubaiNow are still being confirmed with ICP.")
            else ProfileDocInfo(country, emptyList(), "Passports are renewed by your embassy or consulate, not in DubaiNow.")
        }
        "title deed" in t -> ProfileDocInfo("Dubai Land Department", profileServiceAction("check-title-deed", "Check a title deed is genuine"), null)
        "family book" in t -> ProfileDocInfo("ICP", emptyList(), null)
        "trade licence" in t -> ProfileDocInfo("Department of Economy and Tourism", emptyList(), null)
        "work permit" in t -> ProfileDocInfo("MOHRE", emptyList(), null)
        else -> ProfileDocInfo("The issuing entity", emptyList(), null)
    }
}

@Composable
private fun ProfileDocumentDetail(document: DocumentItem) {
    val store = LocalStore.current
    val nav = profileNav()
    val context = LocalContext.current
    val info = profileDocInfo(store, document)

    ProfileScreen(document.title) {
        ProfileDocumentHero(document, store.persona?.fullName)
        if (document.status != DocumentItem.Status.Valid) {
            StatusChip(document.profileStatusText, tone = document.profileStatusTone, onWhite = true)
        }
        ProfileFactsCard(profileDocFacts(document, info.issuer))
        if (info.actions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileSection("Things you can do")
                ProfileGroup {
                    info.actions.forEachIndexed { i, a ->
                        if (i > 0) ProfileDivider()
                        ProfileRow(a.title, onClick = { nav.push(a.route) }, subtitle = a.subtitle)
                    }
                }
            }
        }
        info.note?.let { Text(it, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary) }
        ProfileGroup {
            ProfileRow(
                "Share a copy", titleColor = DNColor.textLink, showsChevron = false,
                onClick = { profileShare(context, "${document.title} · ${document.number} · ${document.profileExpiryLine}") },
            ) { ProfileRowIcon(Icons.Rounded.Share, DNColor.textLink) }
            ProfileDivider(52.dp)
            ProfileRow(
                "Show to verify", titleColor = DNColor.textLink, showsChevron = false,
                onClick = {
                    nav.comingSoon(
                        ComingSoon(
                            title = "Show to verify",
                            detail = "A QR code that lets someone check this document is real comes in the next phase.",
                            owner = info.issuer, symbol = if (nav.inSheet) "qrcode" else "checkmark.seal.fill", hue = document.hue, kind = ComingSoon.Kind.NotInDemo,
                        ),
                    )
                },
            ) { ProfileRowIcon(Icons.Rounded.QrCode, DNColor.textLink) }
        }
    }
}

@Composable
private fun ProfileDocumentHero(document: DocumentItem, holder: String?) {
    val shape = RoundedCornerShape(DNRadius.widget)
    Column(
        Modifier.fillMaxWidth().heightIn(min = 210.dp).dnElevation(DNRadius.widget).clip(shape).background(document.hue.gradient)
            .border(1.dp, Brush.verticalGradient(listOf(DNColor.tileHighlight, Color.Transparent)), shape)
            .semantics(mergeDescendants = true) {}.padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                document.title, style = DNTextStyle.Title2.style.copy(fontWeight = FontWeight.Bold), color = document.hue.ink,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Icon(DNIcons.forSymbol(document.symbol), contentDescription = null, tint = document.hue.ink, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(28.dp))
        Column {
            holder?.let {
                Text(
                    it.uppercase(), style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
                    color = document.hue.ink, modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Text(
                document.number, fontFamily = Inter, fontSize = 17.sp, color = document.hue.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = DNTextStyle.Body.style.copy(fontFeatureSettings = "tnum", letterSpacing = 0.6.sp),
            )
            Text(
                document.profileExpiryLine, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold),
                color = document.hue.ink, modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private fun profileDocFacts(document: DocumentItem, issuer: String): List<Pair<String, String>> = buildList {
    add("Number" to document.number)
    document.expiry?.let { e ->
        val days = DemoDate.daysFromToday(e)
        add((if (days < 0) "Expired" else "Expires") to DemoDate.full(e))
        if (days in 0..120) add("Time left" to if (days == 1) "1 day" else "$days days")
    }
    add("Issued by" to issuer)
    // Passports are issued abroad; DubaiNow reads the copy held with your residency.
    val source = if (document.title.lowercase().contains("passport")) "ICP" else issuer
    add("Information from" to "$source · updated today")
}

