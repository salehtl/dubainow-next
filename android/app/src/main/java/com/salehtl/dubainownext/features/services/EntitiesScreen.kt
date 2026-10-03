package com.salehtl.dubainownext.features.services

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNTextStyle

/**
 * By government entity: every owner with its service count (entities are a filter, not a
 * structure — this list is for people who think that way).
 */
@Composable
fun EntitiesScreen() {
    val router = LocalRouter.current
    var query by rememberSaveable { mutableStateOf(LaunchOptions.extras["servicesQuery"] ?: "") }
    val owners = ServicesData.owners.filter {
        val q = query.trim()
        q.isEmpty() || it.label.contains(q, ignoreCase = true) || (it.detail ?: "").contains(q, ignoreCase = true)
    }
    val state = rememberLazyListState()
    ServicesLaunchScroll(state) { key -> if (key == "list") 3 else null }

    ServicesPage(state = state) {
        item(key = "title") {
            Text(
                "By government entity", style = DNTextStyle.Title1.style, color = DNColor.textPrimary,
                modifier = ServicesInset.padding(top = 4.dp, bottom = 12.dp).semantics { heading() },
            )
        }
        item(key = "search") { ServicesSearchField(query, { query = it }, "Search entities", ServicesInset.padding(bottom = 16.dp), container = DNColor.fieldSurfaceOnGrouped) }
        item(key = "header") {
            Text(
                "${ServicesData.entityCount} government entities and providers".uppercase(), style = DNTextStyle.Overline.style,
                color = DNColor.textGroupedHeader, modifier = ServicesInset.padding(start = 4.dp, bottom = 8.dp).semantics { heading() },
            )
        }
        if (owners.isEmpty()) item(key = "empty") { ServicesNoResults(query) }
        servicesGroupItems(owners, key = { "owner-${it.key}" }, separatorInset = 64.dp) { owner ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = if (owner.detail == null) 56.dp else 72.dp)
                    .clickable { router.push(Route.Entity(owner.key)) }
                    .semantics(mergeDescendants = true) { contentDescription = "${owner.label}, ${ServicesText.count(owner.count, "service")}" }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ServicesEntityMark(owner.label)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(owner.label, style = DNTextStyle.RowTitle.style, color = DNColor.listRowTitle)
                    owner.detail?.let { Text(it, style = DNTextStyle.Footnote.style, color = DNColor.listRowSupporting) }
                }
                Text("${owner.count}", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"), color = DNColor.textSecondary)
            }
        }
        if (owners.isNotEmpty()) item(key = "footer") {
            Text(
                "Each service is listed under the entity that runs it. Bundles and journeys are run by DubaiNow with several entities.",
                style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, modifier = ServicesInset.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            )
        }
    }
}

/** A monogram disc for an entity (no logos in the demo). */
@Composable
internal fun ServicesEntityMark(name: String, size: Dp = 36.dp, fill: Color = DNColor.surfaceSecondary) {
    val initials = servicesEntityInitials(name).ifEmpty { name.take(1).uppercase() }
    // A logo stand-in hidden from TalkBack: its type is sized from the disc (like iOS), not the font setting.
    val density = LocalDensity.current
    val type = with(density) { (size * (if (initials.length > 3) 0.28f else 0.36f)).toSp() }
    val line = with(density) { (size * 0.4f).toSp() }
    Box(Modifier.size(size).clip(CircleShape).background(fill).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Text(
            initials, color = DNColor.textSecondary, maxLines = 1, overflow = TextOverflow.Clip,
            style = DNTextStyle.Caption1.style.copy(fontWeight = FontWeight.Bold, fontSize = type, lineHeight = line),
        )
    }
}

/** "RTA" stays "RTA"; "Awqaf and Minors Affairs Foundation" → "AM"; "Owner to confirm" → "?". */
internal fun servicesEntityInitials(name: String): String {
    if (name == "Owner to confirm") return "?"
    if (name.length <= 5 && !name.contains(" ")) return name
    if (!name.contains(" ")) {
        val capitals = name.filter { it.isUpperCase() }
        return if (capitals.length >= 2) capitals.take(2) else name.take(1).uppercase()
    }
    val words = name.split(" ").filter { it.isNotEmpty() && it.lowercase() !in setOf("and", "of", "in", "the", "or") }
    return words.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }.joinToString("")
}
