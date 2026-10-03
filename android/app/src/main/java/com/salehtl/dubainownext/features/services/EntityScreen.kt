package com.salehtl.dubainownext.features.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.RowTrailing

/** One owner's services, grouped by topic. */
@Composable
fun EntityScreen(name: String) {
    val label = if (name == "To confirm") "Owner to confirm" else name
    val services = ServicesData.services(ownedBy = name)
    val topicOrder = Catalogue.shared.topics.map { it.id }
    val groups = services.groupBy { it.topic ?: "" }.entries
        .sortedBy { (key, _) -> topicOrder.indexOf(key).let { if (it < 0) Int.MAX_VALUE else it } }
        .map { (key, list) -> (Catalogue.shared.topic(key)?.name ?: "Across topics") to list.sortedBy { it.name } }
    val subtitle = run {
        val full = ServicesData.fullName(name)
        val count = ServicesText.count(services.size, "service")
        when (name) {
            "To confirm" -> "$count whose owner is being checked with government"
            "DubaiNow" -> "$count: bundles, journeys and DubaiNow's own"
            else -> if (full == name) count else "$full · $count"
        }
    }
    val state = rememberLazyListState()

    ServicesPage(state = state) {
        item(key = "header") {
            Row(
                ServicesInset.padding(top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ServicesEntityMark(label, size = 52.dp, fill = DNColor.surfacePrimary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(label, style = DNTextStyle.Title2.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
                    Text(subtitle, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                }
            }
        }
        if (services.isEmpty()) item(key = "empty") {
            Text(
                "$label doesn't run any services in DubaiNow yet.", style = DNTextStyle.Body.style, color = DNColor.textSecondary,
                modifier = ServicesInset.padding(top = 40.dp),
            )
        }
        groups.forEach { (title, list) ->
            item(key = "group-$title") {
                Text(
                    title.uppercase(), style = DNTextStyle.Overline.style, color = DNColor.textGroupedHeader,
                    modifier = ServicesInset.padding(start = 4.dp, top = 20.dp, bottom = 8.dp).semantics { heading() },
                )
            }
            servicesGroupItems(list, key = { "$title-${it.id}" }, separatorInset = 68.dp) { ServicesRow(it, trailing = RowTrailing.Favorite) }
        }
    }
}
