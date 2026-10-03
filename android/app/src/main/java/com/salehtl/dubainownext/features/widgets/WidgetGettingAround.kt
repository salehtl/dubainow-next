package com.salehtl.dubainownext.features.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DirectionsSubway
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalWidgetPreview
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.MetroDeparture
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.ParkingSession
import com.salehtl.dubainownext.data.WidgetSize
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.ComingSoon
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.ceil
import kotlin.math.floor

// Getting around: Parking, Salik, nol card, Metro departures (Paper C1 · 02 Widgets · Launch set, AZ0).

// MARK: - Parking

/** A parking session at a moment in time: live → ending soon (10 min) → expired (counts up). */
internal class WidgetParkingModel(val session: ParkingSession, val now: LocalDateTime) {
    enum class Phase { Live, EndingSoon, Expired }

    val remaining: Double = Duration.between(now, session.endsAt).toMillis() / 1000.0
    val phase: Phase = if (remaining <= 0) Phase.Expired else if (remaining <= 600) Phase.EndingSoon else Phase.Live
    val minutesLeft: Int = maxOf(0, ceil(remaining / 60).toInt())
    val minutesOver: Int = maxOf(0, floor(-remaining / 60).toInt())
    val fraction: Double
        get() {
            val total = Duration.between(session.startedAt, session.endsAt).toMillis() / 1000.0
            return if (total > 0) (remaining / total).coerceIn(0.0, 1.0) else 0.0
        }
    val ends: String get() = DemoDate.time(session.endsAt)
    val started: String get() = DemoDate.time(session.startedAt)
    val paid: BigDecimal
        get() {
            val minutes = Duration.between(session.startedAt, session.endsAt).toMinutes()
            return (session.ratePerHour * BigDecimal(minutes)).divide(BigDecimal(60), 2, RoundingMode.HALF_UP)
        }

    /** "42 min" or "1 h 42 min" as figure parts. */
    val figureParts: List<WidgetFigurePart>
        get() {
            val m = if (phase == Phase.Expired) minutesOver else minutesLeft
            val unit = if (phase == Phase.Expired) "min over" else "min"
            return if (m >= 60) listOf(WidgetFigurePart("${m / 60}", "h"), WidgetFigurePart("${m % 60}", unit))
            else listOf(WidgetFigurePart("$m", unit))
        }

    val meterColor get() = when (phase) {
        Phase.Live -> DNColor.signParking
        Phase.EndingSoon -> DNColor.signSun
        Phase.Expired -> DNColor.red200
    }

    val spoken: String
        get() = when (phase) {
            Phase.Live -> "Parking, Zone ${session.zone}, ${WidgetFormat.spokenDuration(minutesLeft)} left, ends at $ends"
            Phase.EndingSoon -> "Parking ending soon, Zone ${session.zone}, ${WidgetFormat.spokenDuration(minutesLeft)} left, ends at $ends"
            Phase.Expired -> "Parking ended at $ends in Zone ${session.zone}, ${WidgetFormat.spokenDuration(minutesOver)} over. You could be fined."
        }
}

@Composable
internal fun WidgetParkingContent(size: WidgetSize, now: LocalDateTime) {
    val store = LocalStore.current
    val session = store.state.parking
    if (session == null) {
        WidgetCalmState(
            title = "Not parked",
            message = "Start a session when you park",
            quick = { WidgetQuickCapsule(WidgetQuickIntent.StartParking, "Start parking", height = 36.dp) },
        ) { WidgetParkingSign() }
        return
    }
    val m = WidgetParkingModel(session, now)
    if (size == WidgetSize.Small) ParkingSmall(m) else ParkingMedium(m)
}

@Composable
private fun ParkingFigure(m: WidgetParkingModel) {
    val expired = m.phase == WidgetParkingModel.Phase.Expired
    WidgetFigure(
        m.figureParts,
        modifier = Modifier.padding(top = 6.dp),
        color = if (expired) DNColor.statusErrorForeground else DNColor.textPrimary,
        numeric = (if (expired) m.minutesOver else m.minutesLeft).toDouble(),
    )
}

@Composable
private fun ParkingCaption(m: WidgetParkingModel, medium: Boolean) {
    when (m.phase) {
        WidgetParkingModel.Phase.Live -> WidgetCaption(if (medium) "left · ends ${m.ends}" else "left in Zone ${m.session.zone}")
        WidgetParkingModel.Phase.EndingSoon -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Rounded.WatchLater, contentDescription = null, tint = DNColor.statusWarningForeground, modifier = Modifier.size(14.dp))
            WidgetCaption("Ends at ${m.ends}", color = DNColor.statusWarningForeground, weight = 600)
        }
        WidgetParkingModel.Phase.Expired -> WidgetCaption("Ended ${m.ends} · fine risk", color = DNColor.statusErrorForeground, weight = 600)
    }
}

@Composable
private fun ParkingSmall(m: WidgetParkingModel) {
    Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            WidgetParkingSign()
            Spacer(Modifier.weight(1f))
            when (m.phase) {
                WidgetParkingModel.Phase.Live -> WidgetQuickPlus(WidgetQuickIntent.ExtendParking, WidgetQuickTone.Subtle)
                WidgetParkingModel.Phase.EndingSoon -> WidgetQuickPlus(WidgetQuickIntent.ExtendParking, WidgetQuickTone.Filled)
                WidgetParkingModel.Phase.Expired -> Unit
            }
        }
        ParkingFigure(m)
        ParkingCaption(m, medium = false)
        Spacer(Modifier.heightIn(min = 6.dp).weight(1f))
        if (m.phase == WidgetParkingModel.Phase.Expired) {
            WidgetQuickCapsule(WidgetQuickIntent.ExtendParking, "Extend now", tone = WidgetQuickTone.Destructive, height = 30.dp, fullWidth = false)
        } else {
            WidgetMeter(m.fraction, m.meterColor)
        }
    }
}

@Composable
private fun ParkingMedium(m: WidgetParkingModel) {
    Row(Modifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(Modifier.height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WidgetParkingSign()
                WidgetTitle("Zone ${m.session.zone}")
            }
            ParkingFigure(m)
            ParkingCaption(m, medium = true)
            Spacer(Modifier.heightIn(min = 6.dp).weight(1f))
            WidgetMeter(m.fraction, m.meterColor)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(DNColor.neutral200))
        Column(Modifier.weight(1f).fillMaxHeight().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetFactRow("Plate", "${m.session.plate.code} ${m.session.plate.number}")
            WidgetFactRow("Started", m.started)
            WidgetFactRow("Paid", Money.aed(m.paid, trimWhole = true))
            Spacer(Modifier.weight(1f))
            when (m.phase) {
                WidgetParkingModel.Phase.Live -> WidgetQuickCapsule(WidgetQuickIntent.ExtendParking, "Extend", icon = Icons.Rounded.Add)
                WidgetParkingModel.Phase.EndingSoon -> WidgetQuickCapsule(WidgetQuickIntent.ExtendParking, "Extend", icon = Icons.Rounded.Add, tone = WidgetQuickTone.Filled)
                WidgetParkingModel.Phase.Expired -> WidgetQuickCapsule(WidgetQuickIntent.ExtendParking, "Extend now", tone = WidgetQuickTone.Destructive)
            }
        }
    }
}

internal fun widgetParkingSpec(store: DemoStore, now: LocalDateTime): WidgetSpec {
    val session = store.state.parking
        ?: return WidgetSpec(
            summary = "Parking. Not parked right now.", hint = "Opens parking",
            quick = WidgetQuickIntent.StartParking, open = WidgetOpen.Sheet(SheetRoute.ExtendParking),
        )
    val m = WidgetParkingModel(session, now)
    return WidgetSpec(
        edge = if (m.phase == WidgetParkingModel.Phase.Expired) DNColor.statusErrorForeground else null,
        summary = m.spoken,
        hint = "Opens Extend parking",
        quick = WidgetQuickIntent.ExtendParking,
        open = WidgetOpen.Sheet(SheetRoute.ExtendParking),
    )
}

// MARK: - Salik

internal class WidgetSalikModel(val balance: BigDecimal) {
    /** Salik charges AED 4 a gate in the demo. */
    val trips: Int = maxOf(0, (balance.toDouble() / 4).toInt())
    val isLow: Boolean = trips < 6
    val tripsLine: String
        get() {
            val t = if (trips == 1) "1 trip" else "$trips trips"
            return if (isLow) "Low · about $t" else "About $t"
        }
}

@Composable
internal fun WidgetSalikContent(size: WidgetSize) {
    val store = LocalStore.current
    if (!store.widgetSalikLinked) {
        WidgetCalmState(
            title = "Salik",
            message = "Link your account to see your balance",
            onDark = true,
            quick = { WidgetQuickCapsule(WidgetQuickIntent.Connect("Salik"), "Connect", tone = WidgetQuickTone.White, height = 36.dp) },
        ) { SalikGlyph() }
        return
    }
    val m = WidgetSalikModel(store.state.salikBalance)
    Box(Modifier.fillMaxSize()) {
        WidgetRoadMarking()
        if (size == WidgetSize.Small) SalikSmall(m) else SalikMedium(m, store)
    }
}

@Composable
private fun SalikGlyph() = WidgetGantryGlyph(ink = DNColor.widgetOnInverse, fill = DNColor.widgetOnInverseRule)

@Composable
private fun SalikFigure(m: WidgetSalikModel) {
    WidgetFigure(
        Money.figure(m.balance, trimWhole = true),
        modifier = Modifier.padding(top = 6.dp),
        leadingUnit = "AED",
        color = DNColor.widgetOnInverse,
        unitColor = DNColor.widgetOnInverseSecondary,
        numeric = m.balance.toDouble(),
    )
}

@Composable
private fun SalikTripsLine(m: WidgetSalikModel) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(if (m.isLow) DNColor.signLow else DNColor.signLive))
        // No room for a second line under the figure at the 1.3 font cap: shrink like iOS rather than cut the count.
        WidgetCaption(m.tripsLine, color = if (m.isLow) DNColor.signLow else DNColor.widgetOnInverseSecondary, weight = 600, minScale = 0.85f)
    }
}

@Composable
private fun SalikPips(m: WidgetSalikModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(10) { i ->
            val lit = i < minOf(m.trips, 10)
            Box(
                Modifier.weight(1f).height(6.dp).clip(CircleShape)
                    .background(if (lit) (if (m.isLow) DNColor.signLow else DNColor.widgetOnInverse) else DNColor.widgetOnInverseTrack)
            )
        }
    }
}

@Composable
private fun SalikSmall(m: WidgetSalikModel) {
    Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            SalikGlyph()
            Spacer(Modifier.weight(1f))
            WidgetQuickPlus(WidgetQuickIntent.TopUpSalik, WidgetQuickTone.OnInverse)
        }
        SalikFigure(m)
        SalikTripsLine(m)
        Spacer(Modifier.heightIn(min = 8.dp).weight(1f))
        SalikPips(m)
    }
}

@Composable
private fun SalikMedium(m: WidgetSalikModel, store: DemoStore) {
    val lastTopUp = store.state.history.firstOrNull { it.serviceID == "top-up-salik-account" && it.amount != null }
    Row(Modifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(Modifier.height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SalikGlyph()
                WidgetTitle("Salik", color = DNColor.widgetOnInverse)
            }
            SalikFigure(m)
            SalikTripsLine(m)
            Spacer(Modifier.heightIn(min = 8.dp).weight(1f))
            SalikPips(m)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(DNColor.widgetOnInverseRule))
        Column(Modifier.weight(1f).fillMaxHeight().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetFactRow("Last top-up", lastTopUp?.amount?.let { Money.aed(it, trimWhole = true) } ?: "None", onDark = true)
            WidgetFactRow("On", lastTopUp?.let { WidgetFormat.shortDate(it.date) } ?: "–", onDark = true)
            WidgetFactRow("Each gate", "AED 4", onDark = true)
            Spacer(Modifier.weight(1f))
            WidgetQuickCapsule(WidgetQuickIntent.TopUpSalik, "Top up", icon = Icons.Rounded.Add, tone = WidgetQuickTone.White)
        }
    }
}

/** The faint dashed lane marking behind the Salik balance. */
@Composable
private fun WidgetRoadMarking() {
    Canvas(Modifier.fillMaxSize()) {
        drawLine(
            DNColor.white.copy(alpha = 0.07f),
            start = Offset(size.width * 0.674f, size.height),
            end = Offset(size.width * 0.857f, size.height * 0.526f),
            strokeWidth = 10.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16.dp.toPx(), 14.dp.toPx())),
            cap = StrokeCap.Butt,
        )
    }
}

internal fun widgetSalikSpec(store: DemoStore): WidgetSpec {
    if (!store.widgetSalikLinked) {
        return WidgetSpec(
            surface = WidgetSurface.Inverse, summary = "Salik. Not connected. Link your account to see your balance.",
            quick = WidgetQuickIntent.Connect("Salik"), open = WidgetOpen.Sheet(SheetRoute.TopUpSalik),
        )
    }
    val m = WidgetSalikModel(store.state.salikBalance)
    return WidgetSpec(
        surface = WidgetSurface.Inverse,
        summary = "Salik balance, ${Money.spoken(m.balance)}. ${m.tripsLine.replace(" · ", ", ")}.",
        hint = "Opens Top up Salik",
        quick = WidgetQuickIntent.TopUpSalik,
        open = WidgetOpen.Sheet(SheetRoute.TopUpSalik),
    )
}

// MARK: - nol card

@Composable
internal fun WidgetNolContent() {
    val store = LocalStore.current
    val balance = store.state.nolBalance
    if (balance == null) {
        WidgetCalmState(
            title = "nol card",
            message = "Add your card to see its balance",
            quick = { WidgetQuickCapsule(WidgetQuickIntent.Connect("nol"), "Add card", height = 36.dp) },
        ) { WidgetNolCard() }
        return
    }
    Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            WidgetNolCard()
            Spacer(Modifier.weight(1f))
            WidgetQuickPlus(WidgetQuickIntent.TopUpNol, WidgetQuickTone.Subtle)
        }
        WidgetFigure(Money.figure(balance, trimWhole = true), leadingUnit = "AED", numeric = balance.toDouble(), modifier = Modifier.padding(top = 6.dp))
        WidgetCaption("On your nol card")
        Spacer(Modifier.heightIn(min = 8.dp).weight(1f))
        if (balance < BigDecimal(15)) {
            WidgetChip(
                "Low for a return", DNColor.statusWarningForeground, DNColor.statusWarningSurface,
                icon = Icons.Rounded.Warning, height = 26.dp,
            )
        } else {
            WidgetChip(
                "Enough for today", DNColor.statusSuccessForeground, DNColor.statusSuccessSurface,
                dot = DNColor.statusSuccessForeground, height = 26.dp,
            )
        }
    }
}

internal fun widgetNolSpec(store: DemoStore): WidgetSpec {
    val balance = store.state.nolBalance
        ?: return WidgetSpec(summary = "nol card. Not added yet.", quick = WidgetQuickIntent.Connect("nol"), open = WidgetOpen.Service("top-up-nol-card"))
    val low = if (balance < BigDecimal(15)) " Low for a return trip." else ""
    return WidgetSpec(
        summary = "nol card balance, ${Money.spoken(balance)}.$low", hint = "Opens Top up your nol card",
        quick = WidgetQuickIntent.TopUpNol, open = WidgetOpen.Service("top-up-nol-card"),
    )
}

// MARK: - Metro departures

internal object WidgetMetroTick {
    /**
     * Minutes until this departure and the one after, counting down from launch and wrapping by the
     * line's headway, so the board keeps moving in a long demo.
     */
    fun minutes(d: MetroDeparture, now: LocalDateTime): Pair<Int, Int> {
        val elapsed = elapsed(now)
        val headway = maxOf(1, d.nextInMinutes - d.inMinutes)
        var m = d.inMinutes - elapsed
        if (m < 0) m = ((m % headway) + headway) % headway
        return m to (m + headway)
    }

    fun elapsed(now: LocalDateTime): Int = maxOf(0, Duration.between(WidgetClock.launch, now).toMinutes().toInt())
}

@Composable
internal fun WidgetMetroContent(now: LocalDateTime) {
    val store = LocalStore.current
    val station = store.state.metro
    if (station == null) {
        WidgetCalmState(title = "Metro", message = "Choose a station to see its next trains", onDark = true) {
            Icon(Icons.Rounded.DirectionsSubway, contentDescription = null, tint = DNColor.widgetOnInverse, modifier = Modifier.size(22.dp))
        }
        return
    }
    val isPreview = LocalWidgetPreview.current
    Column(Modifier.fillMaxSize().padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.DirectionsSubway, contentDescription = null, tint = DNColor.widgetOnInverse, modifier = Modifier.size(18.dp))
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WidgetTitle(station.name, color = DNColor.widgetOnInverse, modifier = Modifier.weight(1f, fill = false))
                val green = station.line.lowercase().contains("green")
                Box(
                    Modifier.height(20.dp).clip(RoundedCornerShape(6.dp)).background(if (green) DNColor.signMetroGreen else DNColor.signMetroRed)
                        .padding(horizontal = 7.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(station.line, style = widgetType(12f, 700, lineHeight = 16f), color = DNColor.white, maxLines = 1) }
            }
            WidgetLivePulse(WidgetMetroTick.elapsed(now), animate = !isPreview)
            Text("Live", style = widgetType(13f, 500, lineHeight = 18f), color = DNColor.widgetOnInverseSecondary)
        }
        station.departures.take(2).forEachIndexed { i, d ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(DNColor.widgetOnInverseRule))
            MetroRow(d, towardsEnd = i > 0, now = now, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetroRow(d: MetroDeparture, towardsEnd: Boolean, now: LocalDateTime, modifier: Modifier) {
    val (inMin, nextMin) = WidgetMetroTick.minutes(d, now)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            if (towardsEnd) Icons.AutoMirrored.Rounded.ArrowBack else Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null, tint = DNColor.widgetOnInverseSecondary, modifier = Modifier.size(18.dp),
        )
        Text(
            d.towards, style = widgetType(17f, 600, lineHeight = 22f), color = DNColor.widgetOnInverse,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (inMin == 0) {
                Text("Now", style = widgetType(24f, 700, lineHeight = 28f), color = DNColor.signLed, modifier = Modifier.alignByBaseline())
            } else {
                WidgetRollingText("$inMin", widgetType(28f, 700, lineHeight = 32f, tracking = -0.3f, tabular = true), DNColor.signLed, inMin.toDouble(), Modifier.alignByBaseline())
                Text("min", style = widgetType(15f, 600, lineHeight = 20f), color = DNColor.signLed, modifier = Modifier.alignByBaseline())
            }
        }
        Text(
            "$nextMin min", style = widgetType(15f, 500, lineHeight = 20f, tabular = true), color = DNColor.widgetOnInverseSecondary,
            textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.widthIn(min = 56.dp),
        )
    }
}

internal fun widgetMetroSpec(store: DemoStore, now: LocalDateTime): WidgetSpec {
    val soon = ComingSoon(
        title = "Metro departures", detail = "Live departures come from RTA. They aren't part of this demo yet.",
        owner = "RTA", symbol = "tram.fill", hue = Hue.Sky, kind = ComingSoon.Kind.NotInDemo,
    )
    val station = store.state.metro
        ?: return WidgetSpec(surface = WidgetSurface.Board, summary = "Metro. No station chosen.", open = WidgetOpen.Soon(soon))
    val lines = station.departures.take(2).map { d ->
        val (inMin, next) = WidgetMetroTick.minutes(d, now)
        if (inMin == 0) "Towards ${d.towards} now, then in $next minutes" else "Towards ${d.towards} in $inMin minutes, then $next"
    }
    return WidgetSpec(
        surface = WidgetSurface.Board,
        summary = "${station.name} metro, ${station.line}, live. " + lines.joinToString(". "),
        open = WidgetOpen.Soon(soon),
    )
}
