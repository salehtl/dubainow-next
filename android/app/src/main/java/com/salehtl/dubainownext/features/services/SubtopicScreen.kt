package com.salehtl.dubainownext.features.services

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.data.ServiceVisuals
import com.salehtl.dubainownext.data.hue
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNIcons
import com.salehtl.dubainownext.design.DNTextStyle

/**
 * Every record in one sub-topic ("See all N" from a topic page): standalone services most
 * used first, then any bundle or journey that lives here.
 */
@Composable
fun SubtopicScreen(topicId: String, subtopicId: String) {
    val topic = Catalogue.shared.topic(topicId)
    val sub = topic?.subtopics?.firstOrNull { it.id == subtopicId }
    if (topic == null || sub == null) { ServicesMissingRecord("$topicId/$subtopicId"); return }

    val router = LocalRouter.current
    val services = ServicesData.services(topicId, subtopicId)
    val state = rememberLazyListState()
    ServicesLaunchScroll(state) { key -> if (key == "rows") 1 else null }

    ServicesPage(wash = topic.hue.start.copy(alpha = 0.9f), state = state) {
        item(key = "header") {
            Column(ServicesInset.padding(top = 4.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ServicesHeaderDisc(DNIcons.forSymbol(ServiceVisuals.symbol(topicId, subtopicId)), topic.hue, size = 56.dp)
                Box(
                    Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
                        .clickable(onClickLabel = "Open the topic") { router.push(Route.Topic(topicId)) },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(topic.name, style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textLink)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(sub.name, style = DNTextStyle.Title1.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
                    Text(ServicesText.count(services.size, "service"), style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                }
            }
        }
        // Rows in one sub-topic share a glyph, so only bundles and journeys show theirs.
        servicesGroupItems(services, key = { it.id }, separatorInset = 16.dp) {
            ServicesRow(it, showsIcon = it.type != ServiceType.Standalone)
        }
        item(key = "end") { Spacer(Modifier.height(8.dp)) }
    }
}
