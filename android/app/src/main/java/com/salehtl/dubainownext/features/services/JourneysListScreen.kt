package com.salehtl.dubainownext.features.services

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.ServiceType

/** Every journey ("Start to finish"); journeys the resident started come first, with progress. */
@Composable
fun JourneysListScreen() {
    val store = LocalStore.current
    val started = store.state.journeys.map { it.journeyID }.toSet()
    val all = Catalogue.shared.journeys
    val journeys = all.filter { it.id in started } + all.filter { it.id !in started }
    ServicesPage {
        item(key = "header") {
            ServicesListHeader(
                ServiceType.Journey, "Start to finish",
                "Big moments that need several entities over weeks or months. DubaiNow keeps the plan, does what it can and shows who handles the rest.",
            )
        }
        journeys.forEach { j -> item(key = j.id) { ServicesMultiCard(j, ServicesInset.padding(top = 16.dp)) } }
    }
}
