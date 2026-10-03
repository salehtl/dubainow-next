package com.salehtl.dubainownext.features.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.ParkingSession
import com.salehtl.dubainownext.data.PaymentLine
import com.salehtl.dubainownext.data.PaymentPurpose
import com.salehtl.dubainownext.data.PaymentRequest
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.DNButton
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * Extend parking — the Parking widget's quick action (Paper C1 · Widget · Quick action, BFM; Android kit
 * M5). Current session, add-time chips priced at the zone's hourly rate, the new end time, then the
 * fake payment confirmation (built by Activity) applies it.
 */
@Composable
fun ExtendParkingSheet() {
    val store = LocalStore.current
    val router = LocalRouter.current
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        WidgetSheetHeader("Extend parking") { router.dismissSheet() }
        val session = store.state.parking
        if (session == null) ExtendParkingNotParked() else ExtendParkingForm(session)
    }
}

private val EXTEND_OPTIONS = listOf(30, 60, 120, 180)

private fun extendLabel(m: Int): String = when {
    m < 60 -> "$m min"
    m == 60 -> "1 hour"
    m % 60 == 0 -> "${m / 60} hours"
    else -> "${m / 60} h ${m % 60} min"
}

private fun minutesText(m: Int): String = if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"

@Composable
private fun ExtendParkingForm(session: ParkingSession) {
    val router = LocalRouter.current
    val view = LocalView.current
    // `widgetsParkingMinutes 120` preselects a chip (screenshots).
    var minutes by rememberSaveable {
        mutableIntStateOf(LaunchOptions.extras["widgetsParkingMinutes"]?.toIntOrNull()?.takeIf { it in EXTEND_OPTIONS } ?: 60)
    }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000 - System.currentTimeMillis() % 1000)
            now = LocalDateTime.now()
        }
    }
    val m = WidgetParkingModel(session, now)
    val price = session.ratePerHour.multiply(BigDecimal(minutes)).divide(BigDecimal(60), 2, RoundingMode.HALF_UP)
    val base = if (session.endsAt.isAfter(now)) session.endsAt else now
    val end = base.plusMinutes(minutes.toLong())
    val twoRows = LocalDensity.current.fontScale > 1.3f

    Column(
        Modifier.fillMaxWidth().padding(start = DNSpace.insetPage, end = DNSpace.insetPage, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        // Session card
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.card)).background(DNColor.surfacePrimary).padding(14.dp)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WidgetParkingSign(size = 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Zone ${session.zone} · ${session.area}", style = DNTextStyle.Headline.style, color = DNColor.textPrimary)
                val plate = "${session.plate.code} ${session.plate.number}"
                val (line, color) = when (m.phase) {
                    WidgetParkingModel.Phase.Expired -> "$plate · ended ${m.ends} (${m.minutesOver} min ago)" to DNColor.statusErrorForeground
                    WidgetParkingModel.Phase.EndingSoon -> "$plate · ends ${m.ends} (${m.minutesLeft} min)" to DNColor.statusWarningForeground
                    WidgetParkingModel.Phase.Live -> "$plate · ends ${m.ends} (${minutesText(m.minutesLeft)})" to DNColor.textSecondary
                }
                Text(line, style = DNTextStyle.Subheadline.style, color = color)
            }
        }

        // Add time
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WidgetOverline("Add time")
            val rows = if (twoRows) EXTEND_OPTIONS.chunked(2) else listOf(EXTEND_OPTIONS)
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DNSpace.chipGap)) {
                    row.forEach { value ->
                        WidgetChoiceChip(
                            label = extendLabel(value),
                            selected = minutes == value,
                            onClick = { DNHaptics.selection(view); minutes = value },
                            modifier = Modifier.weight(1f).semantics {
                                contentDescription = "Add ${extendLabel(value)}"
                                selected = minutes == value
                            },
                        )
                    }
                }
            }
        }

        // New end time and cost
        val endBlock: @Composable () -> Unit = {
            Column(
                Modifier.clearAndSetSemantics { contentDescription = "New end time ${DemoDate.time(end)}" },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("New end time", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                WidgetRollingText(DemoDate.time(end), DNTextStyle.NumericLarge.style, DNColor.textPrimary, minutes.toDouble())
            }
        }
        val costBlock: @Composable (Alignment.Horizontal) -> Unit = { align ->
            Column(
                Modifier.clearAndSetSemantics { contentDescription = "Cost ${Money.spoken(price)}" },
                horizontalAlignment = align, verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("Cost", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("AED", style = DNTextStyle.NumericUnit.style, color = DNColor.textPrimary, modifier = Modifier.alignByBaseline())
                    WidgetRollingText(Money.figure(price), DNTextStyle.NumericLarge.style, DNColor.textPrimary, price.toDouble(), Modifier.alignByBaseline())
                }
            }
        }
        if (twoRows) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { endBlock(); costBlock(Alignment.Start) }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                endBlock()
                Spacer(Modifier.weight(1f))
                costBlock(Alignment.End)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DNButton("Extend for ${Money.aed(price)}", onClick = {
                val request = PaymentRequest(
                    id = "extend-parking-$minutes-${System.currentTimeMillis() / 1000}",
                    title = "Extend parking",
                    lines = listOf(
                        PaymentLine(
                            title = "Parking · Zone ${session.zone}",
                            detail = "${extendLabel(minutes)} more · until ${DemoDate.time(end)}",
                            amount = price,
                            symbol = "p.square.fill",
                            hue = Hue.Sky,
                        )
                    ),
                    purpose = PaymentPurpose.ExtendParking(minutes),
                )
                router.present(SheetRoute.Payment(request))
            })
            Text(
                "Run by Parkin · we'll remind you 10 min before it ends", style = DNTextStyle.Footnote.style,
                color = DNColor.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ExtendParkingNotParked() {
    val router = LocalRouter.current
    Column(
        Modifier.fillMaxWidth().padding(start = DNSpace.insetPage, end = DNSpace.insetPage, top = 12.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        WidgetParkingSign(size = 56.dp, dimmed = true)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("You're not parked right now", style = DNTextStyle.Title3.style, color = DNColor.textPrimary, textAlign = TextAlign.Center)
            Text(
                "Start a session when you park. You can extend it from here or from the Parking widget on Home.",
                style = DNTextStyle.Body.style, color = DNColor.textSecondary, textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        DNButton("Start parking", onClick = {
            router.present(
                SheetRoute.ComingSoonSheet(
                    ComingSoon(title = "Pay for parking", owner = "Parkin", symbol = "p.square.fill", hue = Hue.Sky, kind = ComingSoon.Kind.ServicesPhase)
                )
            )
        })
    }
}
