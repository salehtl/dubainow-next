package com.salehtl.dubainownext.features.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.ServiceTypeBadge

/** Every bundle ("All at once"), with the resident's own count line where we know it. */
@Composable
fun BundlesListScreen() {
    val store = LocalStore.current
    val all = Catalogue.shared.bundles
    val bundles = all.filter { store.servicesBundleCount(it) != null } + all.filter { store.servicesBundleCount(it) == null }
    ServicesPage {
        item(key = "header") {
            ServicesListHeader(
                ServiceType.Bundle, "All at once",
                "The same job across many accounts, people or entities, done in one go: one set of questions, one payment, one receipt. Each part still works on its own.",
            )
        }
        bundles.forEach { b -> item(key = b.id) { ServicesMultiCard(b, ServicesInset.padding(top = 16.dp)) } }
    }
}

/** Badge, title and explanation at the top of the bundles and journeys lists. */
@Composable
internal fun ServicesListHeader(type: ServiceType, title: String, text: String) {
    Column(ServicesInset.padding(top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ServiceTypeBadge(type)
        Text(title, style = DNTextStyle.Title1.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
        Text(text, style = DNTextStyle.Callout.style, color = DNColor.textSecondary)
    }
}
