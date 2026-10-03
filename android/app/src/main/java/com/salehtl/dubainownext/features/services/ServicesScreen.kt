package com.salehtl.dubainownext.features.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.BundleCard
import com.salehtl.dubainownext.design.components.DNListGroup
import com.salehtl.dubainownext.design.components.DNListRow
import com.salehtl.dubainownext.design.components.DNRowDivider
import com.salehtl.dubainownext.design.components.JourneyCard
import com.salehtl.dubainownext.app.AppTab
import com.salehtl.dubainownext.app.ScrollToTopOnReselect

/**
 * Services tab root (Paper C1 `DBS-0`): For you, Browse by topic, All at once (bundles),
 * Start to finish (journeys) and Browse another way (A to Z, by entity).
 * Numbers come from the refactored catalogue, not the older Paper copy.
 */
@Composable
fun ServicesScreen() {
    val store = LocalStore.current; val router = LocalRouter.current; val context = LocalContext.current
    val catalogue = Catalogue.shared
    val large = servicesLargeText()
    val forYou = if (store.isGuest) emptyList() else store.servicesForYou()
    val topicRows = catalogue.topics.chunked(if (large) 1 else 2)
    val state = rememberLazyListState()
    ScrollToTopOnReselect(AppTab.Services, state)

    LaunchedEffect(Unit) { ServicesReachability.runIfRequested(context) }

    // Bundles with something personal to say (Pay all: 2 bills; Family visas: 2 due) come first.
    val bundles = catalogue.bundles.let { all -> all.filter { store.servicesBundleCount(it) != null } + all.filter { store.servicesBundleCount(it) == null } }
    // Journeys the resident started come first.
    val started = store.state.journeys.map { it.journeyID }.toSet()
    val journeys = catalogue.journeys.let { all -> all.filter { it.id in started } + all.filter { it.id !in started } }

    val keys = buildList {
        add("header"); if (forYou.isNotEmpty()) add("forYou"); add("topics")
        topicRows.indices.forEach { add("topics-$it") }
        add("bundles"); add("journeys"); add("browse")
    }
    ServicesLaunchScroll(state) { keys.indexOf(it).takeIf { i -> i >= 0 } }

    ServicesPage(isRoot = true, state = state) {
        item(key = "header") {
            Column(ServicesInset.padding(top = 4.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Services", style = DNTextStyle.LargeTitle.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
                Text(
                    "${catalogue.services.size} services from ${ServicesData.entityCount} government entities and providers, organised by what you need to do.",
                    style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary,
                )
            }
        }
        if (forYou.isNotEmpty()) item(key = "forYou") {
            Column(Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                Row(ServicesInset.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("For you", style = DNTextStyle.Section.style, color = DNColor.textPrimary, modifier = Modifier.weight(1f).semantics { heading() })
                    Text("from your profile", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                }
                ServicesShelf {
                    forYou.forEach { item ->
                        ServicesForYouCard(item, width = if (large) 300.dp else 248.dp, modifier = ServicesShelfItem) { router.servicesPerform(item.action) }
                    }
                }
            }
        }
        item(key = "topics") {
            Text(
                "Browse by topic", style = DNTextStyle.Section.style, color = DNColor.textPrimary,
                modifier = ServicesInset.padding(top = 16.dp, bottom = 12.dp).semantics { heading() },
            )
        }
        topicRows.forEachIndexed { i, row ->
            item(key = "topics-$i") {
                Row(
                    ServicesInset.fillMaxWidth().height(IntrinsicSize.Max).padding(bottom = DNSpace.widgetGutter),
                    horizontalArrangement = Arrangement.spacedBy(DNSpace.widgetGutter),
                ) {
                    row.forEach { topic ->
                        ServicesTopicTile(topic, Modifier.weight(1f).fillMaxHeight(), compact = !large) { router.push(Route.Topic(topic.id)) }
                    }
                    if (row.size == 1 && !large) Spacer(Modifier.weight(1f))
                }
            }
        }
        item(key = "bundles") {
            Column(Modifier.padding(top = 16.dp)) {
                ServicesSectionHeader(
                    "All at once", ServicesInset, subtitle = "One job across all your accounts, done in one go",
                    linkTitle = "See all", onLink = { router.push(Route.BundlesList) },
                )
                ServicesShelf {
                    bundles.forEach { b ->
                        BundleCard(b, ServicesShelfItem, count = store.servicesBundleCount(b), width = if (large) 300.dp else 272.dp) { router.open(b) }
                    }
                }
            }
        }
        item(key = "journeys") {
            Column(Modifier.padding(top = 16.dp)) {
                ServicesSectionHeader(
                    "Start to finish", ServicesInset, subtitle = "Life events that need several services, in order",
                    linkTitle = "See all", onLink = { router.push(Route.JourneysList) },
                )
                ServicesShelf {
                    journeys.forEach { j ->
                        JourneyCard(j, ServicesShelfItem, progress = store.servicesJourney(j.id), width = if (large) 300.dp else 272.dp) { router.open(j) }
                    }
                }
            }
        }
        item(key = "browse") {
            // Android list rows: no chevron on navigation rows; the count sits under the title (Paper LOA-0).
            DNListGroup(ServicesInset.padding(top = 24.dp), header = "Browse another way") {
                DNListRow(
                    "All services, A to Z", subtitle = ServicesText.count(catalogue.services.size, "service"),
                    showsChevron = false, onClick = { router.push(Route.AllServices) },
                )
                DNRowDivider()
                DNListRow(
                    "By government entity", subtitle = "${ServicesData.entityCount} entities and providers",
                    showsChevron = false, onClick = { router.push(Route.Entities) },
                )
            }
        }
    }
}
