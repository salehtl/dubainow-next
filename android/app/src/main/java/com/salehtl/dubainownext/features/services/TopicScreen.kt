package com.salehtl.dubainownext.features.services

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.CatalogueTopic
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.data.hue
import com.salehtl.dubainownext.data.icon
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.BundleCard
import com.salehtl.dubainownext.design.components.JourneyCard
import com.salehtl.dubainownext.design.components.TonalIconButton
import kotlinx.coroutines.launch

private const val MULTI_SECTION = "section-multi"

/**
 * A topic page (Paper C1 `DJ8-0`): hue disc header, sub-topic chips that scroll to their
 * section, a personal banner, up to 3 rows per sub-topic with "See all N", then the topic's
 * bundles and journeys.
 */
@Composable
fun TopicScreen(topicId: String) {
    val topic = Catalogue.shared.topic(topicId)
    if (topic == null) ServicesMissingRecord(topicId) else ServicesTopicPage(topic)
}

@Composable
private fun ServicesTopicPage(topic: CatalogueTopic) {
    val store = LocalStore.current; val router = LocalRouter.current
    val context = LocalContext.current; val view = LocalView.current
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState()

    val subtopics = ServicesData.subtopics(topic)
    val multi = ServicesData.bundlesAndJourneys(topic.id)
    val unfiled = ServicesData.unfiled(topic)
    val banner = store.servicesBanner(topic.id)

    val keys = buildList {
        add("header"); add("chips"); if (banner != null) add("banner")
        subtopics.forEach { add(it.id) }
        if (unfiled.isNotEmpty()) add("unfiled")
        if (multi.isNotEmpty()) { add(MULTI_SECTION); multi.forEach { add("multi-${it.id}") } }
    }
    fun indexOf(key: String) = keys.indexOf(key).takeIf { it >= 0 }
    ServicesLaunchScroll(state, ::indexOf)

    fun jump(key: String) {
        DNHaptics.selection(view)
        indexOf(key)?.let { i ->
            scope.launch { if (servicesReduceMotion(context)) state.scrollToItem(i) else state.animateScrollToItem(i) }
        }
    }

    ServicesPage(
        wash = topic.hue.start.copy(alpha = 0.9f), state = state,
        actions = {
            TonalIconButton(Icons.Rounded.Share, "Share", onClick = {
                servicesShare(context, "${topic.name} on DubaiNow: ${ServicesData.topicLine(topic)}.")
            })
        },
    ) {
        item(key = "header") {
            Column(ServicesInset.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ServicesHeaderDisc(topic.icon, topic.hue, size = 56.dp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(topic.name, style = DNTextStyle.Title1.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
                    Text(ServicesData.topicLine(topic), style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                }
            }
        }
        item(key = "chips") {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = DNSpace.insetPage).padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                subtopics.forEach { sub -> ServicesJumpChip(sub.name, sub.services) { jump(sub.id) } }
                if (multi.isNotEmpty()) ServicesJumpChip("All at once and start to finish", null) { jump(MULTI_SECTION) }
            }
        }
        if (banner != null) item(key = "banner") {
            ServicesBannerCard(banner, ServicesInset.padding(top = 12.dp))
        }
        subtopics.forEach { sub ->
            item(key = sub.id) {
                val all = ServicesData.services(topic.id, sub.id)
                val rows = ServicesData.preview(topic.id, sub.id)
                val more = all.size > rows.size
                Column(ServicesInset.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ServicesSectionHeader(
                        sub.name,
                        linkTitle = if (more) "See all ${all.size}" else null,
                        onLink = if (more) ({ router.push(Route.Subtopic(topic.id, sub.id)) }) else null,
                    )
                    ServicesRowGroup(rows) { ServicesRow(it, showsIcon = false) }
                }
            }
        }
        if (unfiled.isNotEmpty()) item(key = "unfiled") {
            Column(ServicesInset.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ServicesSectionHeader("Other services")
                ServicesRowGroup(unfiled) { ServicesRow(it, showsIcon = false) }
            }
        }
        if (multi.isNotEmpty()) {
            item(key = MULTI_SECTION) {
                val title = when {
                    multi.all { it.type == ServiceType.Journey } -> "Start to finish"
                    multi.all { it.type == ServiceType.Bundle } -> "All at once"
                    else -> "All at once and start to finish"
                }
                ServicesSectionHeader(title, ServicesInset.padding(top = 24.dp, bottom = 4.dp), subtitle = "Several services, handled together")
            }
            multi.forEach { s -> item(key = "multi-${s.id}") { ServicesMultiCard(s, ServicesInset.padding(top = 8.dp)) } }
        }
    }
}

/** A bundle or journey as a full-width card (topic pages, lists). */
@Composable
internal fun ServicesMultiCard(service: CatalogueService, modifier: Modifier = Modifier) {
    val store = LocalStore.current; val router = LocalRouter.current
    Box(modifier) {
        if (service.type == ServiceType.Journey) {
            JourneyCard(service, progress = store.servicesJourney(service.id), width = null) { router.open(service) }
        } else {
            BundleCard(service, count = store.servicesBundleCount(service), width = null) { router.open(service) }
        }
    }
}

/** A white pill that scrolls the page to a section ("Vehicles 11"). */
@Composable
private fun ServicesJumpChip(title: String, count: Int?, onClick: () -> Unit) {
    Box(Modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.heightIn(min = 40.dp).clip(CircleShape).background(DNColor.surfacePrimary)
                .clickable(onClickLabel = "Scroll to the section", onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = count?.let { "$title, $it services" } ?: title }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary, maxLines = 1)
            if (count != null) Text("$count", style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textSecondary)
        }
    }
}
