package com.salehtl.dubainownext.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.FamilyMember
import com.salehtl.dubainownext.data.FamilyVisasStatus
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.Avatar
import com.salehtl.dubainownext.design.components.BundleDiscs
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNButtonSize
import com.salehtl.dubainownext.design.components.ServiceTypeBadge
import com.salehtl.dubainownext.design.components.dnCard
import java.time.LocalDateTime

/**
 * Your family: the people you sponsor and their visa expiry, with the family visas bundle
 * ("All at once") when two or more visas are due.
 */
@Composable
fun FamilyScreen() {
    val store = LocalStore.current
    val nav = profileNav()
    val family = store.state.family
    val sponsored = family.filter { it.sponsoredByYou }
    val others = family.filter { !it.sponsoredByYou }
    ProfileScreen("Your family", largeTitle = true) {
        if (family.isEmpty()) {
            ProfileEmptyState(Icons.Rounded.People, "No family members yet", "People you sponsor show here with their visas.")
        } else {
            Text(
                if (family.any { it.visaExpiry != null }) "Their visas and Emirates IDs renew through you, so we keep an eye on the dates."
                else "The people in your family book. Their documents renew through you.",
                style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary,
            )
            store.state.familyVisas?.takeIf { it.dueMemberIDs.isNotEmpty() }?.let { ProfileFamilyVisasCallout(it, family) }
            if (sponsored.isNotEmpty()) ProfileFamilyGroup("Sponsored by you", sponsored)
            if (others.isNotEmpty()) ProfileFamilyGroup("Your household", others)
        }
        if (family.isEmpty() || sponsored.isNotEmpty()) {
            profileRouteForService("sponsor-family-member")?.let { route ->
                ProfileGroup {
                    ProfileRow("Sponsor a family member", onClick = { nav.push(route) }, subtitle = "GDRFA") {
                        ProfileGreyDisc(icon = Icons.Rounded.PersonAdd)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileFamilyGroup(title: String, members: List<FamilyMember>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileOverline(title)
        ProfileGroup {
            members.forEachIndexed { i, m ->
                if (i > 0) ProfileDivider(72.dp)
                ProfileFamilyRow(m)
            }
        }
    }
}

@Composable
private fun ProfileFamilyRow(member: FamilyMember) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(member.initials, member.hue, size = 44.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(member.name, style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
            Text(member.age?.let { "${member.relation} · $it" } ?: member.relation, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
            member.visaExpiry?.let { ProfileVisaLine(it) }
        }
    }
}

@Composable
private fun ProfileVisaLine(expiry: LocalDateTime) {
    val days = DemoDate.daysFromToday(expiry)
    val color = when {
        days < 0 -> DNColor.statusErrorForeground
        days <= 90 -> DNColor.statusWarningForeground
        else -> DNColor.statusSuccessForeground
    }
    val text = when {
        days < 0 -> "Visa expired ${DemoDate.short(expiry)}"
        days <= 90 -> "Visa expires in $days days · ${DemoDate.short(expiry)}"
        else -> "Visa valid to ${DemoDate.monthYear(expiry)}"
    }
    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(if (days <= 90) Icons.Rounded.Schedule else Icons.Rounded.CheckCircle, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = color)
    }
}

/** "2 of 3 visas expire in 73 days" → the Renew your family's visas bundle. */
@Composable
private fun ProfileFamilyVisasCallout(status: FamilyVisasStatus, family: List<FamilyMember>) {
    val nav = profileNav()
    val days = DemoDate.daysFromToday(status.expiresOn)
    val bundle = Catalogue.service("renew-family-visas")
    val dueNames = ProfileText.join(family.filter { it.id in status.dueMemberIDs }.map { ProfileText.firstName(it.name) })
    Column(
        Modifier.fillMaxWidth().dnCard(DNRadius.listGroup).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ServiceTypeBadge(ServiceType.Bundle)
            Spacer(Modifier.weight(1f))
            bundle?.let { BundleDiscs(it, size = 36.dp) }
        }
        Text(
            "${status.dueMemberIDs.size} of ${status.memberIDs.size} visas expire in $days days",
            style = DNTextStyle.Title3.style, color = DNColor.textPrimary,
        )
        Text(
            "$dueNames's visas expire on ${DemoDate.long(status.expiresOn)}. Renew them in one application, with the medical tests, Emirates IDs and insurance done in one go.",
            style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary,
        )
        DNButton("Renew together", onClick = { nav.push(Route.Bundle("renew-family-visas")) }, size = DNButtonSize.Medium, fullWidth = false)
    }
}
