package com.salehtl.dubainownext.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.data.personas.Personas
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.Avatar
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNButtonKind
import com.salehtl.dubainownext.design.components.DNListGroup
import com.salehtl.dubainownext.design.components.DNRowDivider
import com.salehtl.dubainownext.design.components.TonalIconButton

/** Hidden demo controls: switch resident, reset the demo, change the time of day. */
@Composable
fun DemoMenuSheet() {
    val store = LocalStore.current; val router = LocalRouter.current; val toasts = LocalToasts.current
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Demo controls", style = DNTextStyle.Title2.style, color = DNColor.textPrimary, modifier = Modifier.weight(1f).semantics { heading() })
            TonalIconButton(Icons.Rounded.Close, "Close", onClick = { router.dismissSheet() })
        }
        DNListGroup(header = "Sign in as") {
            Personas.all.forEachIndexed { i, p ->
                val current = store.persona == p
                Row(
                    Modifier.fillMaxWidth().semantics { selected = current }
                        .clickable { store.signIn(p); router.reset(); toasts.show("Signed in as ${p.firstName}") }.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(p.initials, p.avatarHue, size = 40.dp)
                    Column(Modifier.weight(1f)) {
                        Text(p.fullName, style = DNTextStyle.RowTitle.style, color = DNColor.textPrimary)
                        Text(p.tagline, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
                    }
                    if (current) Icon(Icons.Rounded.Check, contentDescription = "Current resident", tint = DNColor.actionAccent)
                }
                if (i < Personas.all.lastIndex) DNRowDivider()
            }
        }
        DNButton("Continue as guest", onClick = { store.continueAsGuest(); router.reset() }, kind = DNButtonKind.Secondary)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("TIME OF DAY", style = DNTextStyle.Overline.style, color = DNColor.textGroupedHeader)
            val options = listOf("Now" to null, "Morning" to 9, "Midday" to 13, "Evening" to 18, "Night" to 22)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, (label, hour) ->
                    SegmentedButton(selected = store.hourOverride == hour, onClick = { store.hourOverride = hour }, shape = SegmentedButtonDefaults.itemShape(i, options.size)) {
                        Text(label, style = DNTextStyle.Caption1.style)
                    }
                }
            }
        }
        DNButton("Reset this resident's data", onClick = { store.resetDemo(); router.reset(); toasts.show("Demo reset") }, kind = DNButtonKind.Secondary)
        DNButton("Show Welcome again", onClick = { store.signOut(); router.reset() }, kind = DNButtonKind.Destructive)
        Text("This is a demo. No real accounts, payments or government systems are connected.", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
    }
}
