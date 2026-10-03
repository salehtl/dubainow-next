package com.salehtl.dubainownext.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.data.DocumentItem
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNTextStyle

/** Your documents: every document as a wallet card (2 columns; 1 at very large text sizes). */
@Composable
fun DocumentsScreen() {
    val store = LocalStore.current
    val docs = store.state.documents
    val needsAttention = docs.filter { it.status != DocumentItem.Status.Valid }
    val valid = docs.filter { it.status == DocumentItem.Status.Valid }
    ProfileScreen("Your documents", largeTitle = true) {
        if (docs.isEmpty()) {
            ProfileEmptyState(Icons.Rounded.Badge, "No documents yet", "Your Emirates ID, visa and licences show here when you sign in with UAE PASS.")
        } else {
            Text("From the entities that issue them. They update on their own.", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
            if (needsAttention.isNotEmpty()) ProfileDocumentGrid("Needs a look", needsAttention)
            if (valid.isNotEmpty()) ProfileDocumentGrid("Valid", valid)
        }
    }
}

@Composable
private fun ProfileDocumentGrid(title: String, docs: List<DocumentItem>) {
    val nav = profileNav()
    val columns = if (profileLargeText()) 1 else 2
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileSectionTitle(title)
        docs.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { d ->
                    ProfileDocumentCard(d, onClick = { nav.push(Route.Document(d.id)) }, modifier = Modifier.weight(1f).fillMaxHeight())
                }
                if (row.size < columns) Spacer(Modifier.weight(1f))
            }
        }
    }
}
