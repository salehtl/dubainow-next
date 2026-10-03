package com.salehtl.dubainownext.features.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.Payable
import com.salehtl.dubainownext.data.Vehicle
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.ChipTone
import com.salehtl.dubainownext.design.components.NumberPlate
import com.salehtl.dubainownext.design.components.StatusChip
import com.salehtl.dubainownext.design.components.dnCard
import java.math.BigDecimal

/** Your vehicles: one card per vehicle (plate, registration, fines) → the vehicle page. */
@Composable
fun VehiclesScreen() {
    val store = LocalStore.current
    val nav = profileNav()
    val vehicles = store.state.vehicles
    ProfileScreen("Your vehicles", largeTitle = true, spacing = 16.dp) {
        if (vehicles.isEmpty()) {
            ProfileEmptyState(Icons.Rounded.DirectionsCar, "No vehicles yet", "Vehicles registered to you with RTA show here on their own.")
        } else {
            Text("Registered to you with RTA. They update on their own.", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
            vehicles.forEach { v ->
                ProfileVehicleCard(v, store.profileFines(v), onClick = { nav.push(Route.Vehicle(v.id)) })
            }
        }
        ProfileGroup(Modifier.padding(top = 8.dp)) {
            profileRouteForService("register-vehicle")?.let { route ->
                ProfileRow("Register a vehicle", onClick = { nav.push(route) }, subtitle = "A new or used car you've bought") {
                    ProfileGreyDisc(icon = Icons.Rounded.Add)
                }
                ProfileDivider(68.dp)
            }
            ProfileRow(
                "All vehicle services", onClick = { nav.push(Route.Subtopic("driving-and-transport", "vehicles")) },
                subtitle = "RTA, Dubai Police and Salik",
            ) { ProfileGreyDisc(icon = Icons.Rounded.GridView) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileVehicleCard(vehicle: Vehicle, fines: List<Payable>, onClick: () -> Unit) {
    val days = DemoDate.daysFromToday(vehicle.registrationExpiry)
    Column(
        Modifier.fillMaxWidth().dnCard(DNRadius.listGroup).clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // No chevron on Android (Paper LOA-0): the whole card is the button.
        NumberPlate(vehicle.plate.code, vehicle.plate.number, emirate = vehicle.plate.emirate)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(vehicle.shortName, style = DNTextStyle.Title3.style, color = DNColor.textPrimary)
            Text("${vehicle.year} · ${vehicle.colour} · ${vehicle.insurer}", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                days < 0 -> StatusChip("Registration expired", tone = ChipTone.Error, onWhite = true)
                days <= 60 -> StatusChip("Registration · $days days left", tone = ChipTone.Warning, onWhite = true)
                else -> StatusChip("Registered to ${DemoDate.monthYear(vehicle.registrationExpiry)}", tone = ChipTone.Success, onWhite = true)
            }
            if (fines.isNotEmpty()) {
                val total = fines.fold(BigDecimal.ZERO) { a, f -> a + f.amount }
                StatusChip("${if (fines.size == 1) "1 fine" else "${fines.size} fines"} · ${Money.aed(total, trimWhole = true)}", tone = ChipTone.Error, onWhite = true)
            }
        }
    }
}
