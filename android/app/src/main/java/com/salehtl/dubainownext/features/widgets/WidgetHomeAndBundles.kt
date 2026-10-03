package com.salehtl.dubainownext.features.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.EnergyReading
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.Payable
import com.salehtl.dubainownext.data.WidgetSize
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.Hue
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.abs

// Home and bills: Energy meter. All at once: Bills and fines (Pay all), Family visas (IZX, JA9).

// MARK: - Energy meter

@Composable
internal fun WidgetEnergyContent() {
    val store = LocalStore.current
    val e = store.state.energy
    if (e == null) {
        WidgetCalmState(
            title = "Energy use",
            message = "Link your DEWA account to see this month's use",
            quick = { WidgetQuickCapsule(WidgetQuickIntent.Connect("DEWA"), "Connect", height = 36.dp) },
        ) { WidgetGlyph(Icons.Rounded.Bolt, ink = DNColor.signAmber, fill = DNColor.amber50) }
        return
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetGlyph(Icons.Rounded.Bolt, ink = DNColor.signAmber, fill = DNColor.amber50, size = 28.dp)
            WidgetTitle("Energy · ${e.monthLabel}", modifier = Modifier.weight(1f))
            EnergyTrend(e)
        }
        Spacer(Modifier.heightIn(min = 10.dp).weight(1f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WidgetFit { WidgetOdometer(e.kWh) }
                WidgetCaption(e.comparison, lines = 2)
            }
            WidgetMonthBars(e.lastSixMonths)
        }
    }
}

@Composable
private fun EnergyTrend(e: EnergyReading) {
    val down = e.changeVsLastMonth <= 0
    WidgetChip(
        text = "${abs(e.changeVsLastMonth)}% vs ${WidgetFormat.previousMonthShort(e.monthLabel)}",
        foreground = if (down) DNColor.statusSuccessForeground else DNColor.statusWarningForeground,
        background = if (down) DNColor.statusSuccessSurface else DNColor.statusWarningSurface,
        icon = if (down) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
        height = 26.dp,
    )
}

/**
 * Odometer digits on dark meter tiles; leading zeros dimmed, the last digit lit in sun orange.
 * Each digit rolls on its own when the reading changes.
 */
@Composable
private fun WidgetOdometer(value: Int, digits: Int = 5) {
    var cap = 1; repeat(digits) { cap *= 10 }
    val s = String.format(Locale.US, "%0${digits}d", minOf(value, cap - 1))
    val firstSignificant = s.indexOfFirst { it != '0' }.let { if (it < 0) s.length - 1 else it }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        s.forEachIndexed { i, c ->
            val isLast = i == s.length - 1
            Box(
                Modifier.size(30.dp, 42.dp).clip(RoundedCornerShape(7.dp))
                    .background(if (isLast) DNColor.signSun else DNColor.widgetMeterTile).clipToBounds(),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = c,
                    transitionSpec = {
                        (slideInVertically(DNMotion.smooth()) { it } + fadeIn()) togetherWith (slideOutVertically(DNMotion.smooth()) { -it } + fadeOut())
                    },
                    label = "odometer",
                ) { ch ->
                    Text(
                        ch.toString(), style = widgetType(26f, 700, lineHeight = 30f, tabular = true),
                        color = when {
                            isLast -> DNColor.textPrimary
                            i < firstSignificant -> DNColor.widgetOnInverseTertiary
                            else -> DNColor.widgetOnInverse
                        },
                    )
                }
            }
        }
        Text(
            "kWh", style = widgetType(15f, 600, lineHeight = 20f), color = DNColor.textSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
        )
    }
}

/** Six months of use, oldest first; this month in sun orange, last month a shade darker. */
@Composable
private fun WidgetMonthBars(values: List<Int>) {
    val shown = values.takeLast(6)
    val top = (shown.maxOrNull() ?: 1).toFloat(); val bottom = (shown.minOrNull() ?: 0).toFloat()
    Row(Modifier.height(64.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        shown.forEachIndexed { i, v ->
            // Scaled between the lowest and highest month so the change reads at a glance.
            val t = if (top > bottom) (v - bottom) / (top - bottom) else 1f
            val h by animateFloatAsState(26f + 38f * t, DNMotion.smooth(), label = "bar")
            Box(
                Modifier.width(11.dp).height(h.dp).clip(RoundedCornerShape(4.dp)).background(
                    when (i) {
                        shown.size - 1 -> DNColor.signSun
                        shown.size - 2 -> DNColor.neutral300
                        else -> DNColor.neutral200
                    }
                )
            )
        }
    }
}

internal fun widgetEnergySpec(store: DemoStore): WidgetSpec {
    val e = store.state.energy
        ?: return WidgetSpec(
            summary = "Energy use. Not connected. Link your DEWA account to see it.",
            quick = WidgetQuickIntent.Connect("DEWA"), open = WidgetOpen.Service("check-utility-consumption"),
        )
    val change = if (e.changeVsLastMonth <= 0) "down ${abs(e.changeVsLastMonth)}%" else "up ${e.changeVsLastMonth}%"
    val last = WidgetFormat.previousMonthShort(e.monthLabel)
    return WidgetSpec(
        summary = "Energy use in ${e.monthLabel}, ${String.format(Locale.US, "%,d", e.kWh)} kilowatt hours, $change on $last. ${e.comparison}.",
        hint = "Opens your electricity and water use",
        open = WidgetOpen.Service("check-utility-consumption"),
    )
}

// MARK: - Bills and fines (bundle: Pay all)

@Composable
internal fun WidgetBillsContent(size: WidgetSize, now: LocalDateTime) {
    val store = LocalStore.current
    val m = WidgetBillsModel(store, now)
    when {
        store.isGuest -> WidgetCalmState("Bills and fines", "Sign in to see what's due across your accounts") { WidgetBundleMark() }
        m.nothingYet -> WidgetCalmState("Nothing yet this month", "New bills and fines show up here as they arrive") { WidgetBundleMark() }
        size == WidgetSize.Small -> BillsSmall(m)
        else -> BillsMedium(m)
    }
}

@Composable
private fun CheckDisc() = WidgetGlyph(Icons.Rounded.Check, ink = DNColor.statusSuccessForeground, fill = DNColor.statusSuccessSurface)

private fun billsPaidLine(m: WidgetBillsModel): String = when {
    m.allPaid -> "${Money.aed(m.paidThisMonth, trimWhole = true)} paid"
    m.paidThisMonth.signum() > 0 -> "${Money.aed(m.paidThisMonth, trimWhole = true)} paid so far"
    else -> "${Money.aed(m.total, trimWhole = true)} in all"
}

@Composable
private fun BillsSmall(m: WidgetBillsModel) {
    Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 16.dp)) {
        Box(Modifier.height(44.dp), contentAlignment = Alignment.CenterStart) {
            if (m.allPaid) CheckDisc() else WidgetBundleMark()
        }
        WidgetFigure("${m.toCome.size}", unit = "to come", numeric = m.toCome.size.toDouble(), modifier = Modifier.padding(top = 6.dp))
        WidgetCaption(billsPaidLine(m))
        Spacer(Modifier.heightIn(min = 8.dp).weight(1f))
        val chip = m.nextChip
        if (m.allPaid) {
            WidgetChip("Receipts in Activity", DNColor.statusSuccessForeground, DNColor.statusSuccessSurface)
        } else if (chip != null) {
            val overdue = m.overdueCount > 0
            WidgetChip(
                chip,
                foreground = if (overdue) DNColor.statusErrorForeground else DNColor.statusWarningForeground,
                background = if (overdue) DNColor.statusErrorSurface else DNColor.statusWarningSurface,
            )
        }
    }
}

@Composable
private fun BillsMedium(m: WidgetBillsModel) {
    Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (m.allPaid) CheckDisc() else WidgetBundleMark()
            WidgetTitle("Bills and fines", modifier = Modifier.weight(1f))
            if (m.overdueCount > 0) {
                WidgetChip(
                    if (m.overdueCount == 1) "1 thing needs you" else "${m.overdueCount} things need you",
                    DNColor.statusWarningForeground, DNColor.statusWarningSurface, icon = Icons.Rounded.ArrowUpward,
                )
            } else {
                Text(m.monthName, style = widgetType(13f, 600, lineHeight = 18f), color = DNColor.textSecondary)
            }
        }
        if (m.allPaid) {
            WidgetFigure("0", unit = "to come", numeric = 0.0, modifier = Modifier.padding(top = 2.dp))
        } else {
            WidgetFigure(Money.figure(m.total), leadingUnit = "AED", numeric = m.total.toDouble(), modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.heightIn(min = 6.dp).weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WidgetMeter(if (m.allPaid) 1.0 else m.paidFraction, DNColor.statusSuccessForeground)
            Column {
                if (m.allPaid) {
                    WidgetCaption("${Money.aed(m.paidThisMonth, trimWhole = true)} paid in ${m.monthName} · receipts in Activity")
                } else {
                    WidgetCaption(
                        if (m.paidThisMonth.signum() > 0) "Still to come this month · ${Money.aed(m.paidThisMonth, trimWhole = true)} paid so far"
                        else "Still to come this month · nothing paid yet"
                    )
                    m.next?.let { WidgetCaption(billsNextLine(it, m.nextWhen), color = DNColor.statusWarningForeground, weight = 600) }
                }
            }
        }
    }
}

private fun billsNextLine(p: Payable, whenText: String?): String {
    val what = WidgetFormat.lowerFirst(p.title).replace(" bill", "")
    return if (whenText == null) "Next: $what" else "Next: $what, $whenText"
}

internal fun widgetBillsSpec(store: DemoStore, now: LocalDateTime): WidgetSpec {
    val m = WidgetBillsModel(store, now)
    val summary = when {
        store.isGuest -> "Bills and fines. Sign in to see what's due."
        m.nothingYet -> "Bills and fines. Nothing yet this month."
        m.allPaid -> "Bills and fines. All paid: ${Money.spoken(m.paidThisMonth)} in ${m.monthName}. Receipts are in Activity."
        else -> buildString {
            append("Bills and fines, ${m.toCome.size} to come this month, ${Money.spoken(m.total)}.")
            m.next?.let { append(" Next: ${WidgetFormat.lowerFirst(it.title)}" + (m.nextWhen?.let { w -> ", due $w." } ?: ".")) }
        }
    }
    return WidgetSpec(summary = summary, hint = "Opens Pay all your bills and fines", open = WidgetOpen.Push(Route.Bundle("pay-all")))
}

// MARK: - Family visas (bundle: Renew your family's visas)

@Composable
internal fun WidgetFamilyContent(size: WidgetSize) {
    val store = LocalStore.current
    val m = WidgetFamilyModel.from(store)
    if (m == null) {
        WidgetCalmState("Family visas", "No family visas are due. We'll show them here 90 days before") { FamilyGlyph() }
        return
    }
    if (size == WidgetSize.Small) FamilySmall(m) else FamilyMedium(m)
}

@Composable
private fun FamilyGlyph() = WidgetGlyph(Icons.Rounded.Badge, ink = Hue.Lilac.ink, fill = Hue.Lilac.gradient)

@Composable
private fun FamilySmall(m: WidgetFamilyModel) {
    Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            FamilyGlyph()
            Spacer(Modifier.weight(1f))
            WidgetBundleMark(badge = true)
        }
        WidgetFigure("${m.days}", unit = if (m.days == 1) "day" else "days", numeric = m.days.toDouble(), modifier = Modifier.padding(top = 6.dp))
        WidgetCaption(if (m.dueCount == 1) "until 1 visa expires" else "until ${m.dueCount} visas expire")
        Spacer(Modifier.heightIn(min = 8.dp).weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            m.members.take(4).forEach { member ->
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(Hue.Lilac.start)
                        .let { if (member.isDue) it.border(2.dp, DNColor.signSun, CircleShape) else it },
                    contentAlignment = Alignment.Center,
                ) { Text(member.initial, style = widgetType(11f, 700, lineHeight = 14f), color = Hue.Lilac.ink) }
            }
            Text(
                "${m.dueCount} of ${m.members.size} due", style = widgetType(12f, 600, lineHeight = 16f),
                color = DNColor.statusWarningForeground, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun FamilyMedium(m: WidgetFamilyModel) {
    Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FamilyGlyph()
            Column(Modifier.weight(1f)) {
                WidgetTitle("Family visas")
                Text(
                    "All at once · ${m.dueCount} due", style = widgetType(12f, 600, lineHeight = 16f),
                    color = DNColor.actionAccent, maxLines = 1,
                )
            }
            if (m.dueCount > 0) {
                WidgetQuickCapsule(WidgetQuickIntent.RenewFamilyVisas, "Renew ${m.dueCount}", height = 34.dp, fullWidth = false)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.Top) {
            m.members.take(4).forEach { member ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    WidgetVisaRing(member.initial, member.days, member.isDue)
                    Text(member.name, style = widgetType(13f, 600, lineHeight = 18f), color = DNColor.textPrimary, maxLines = 1)
                    Text(
                        member.days?.let { if (it == 1) "1 day" else "$it days" } ?: "–",
                        style = widgetType(12f, if (member.isDue) 600 else 400, lineHeight = 16f),
                        color = if (member.isDue) DNColor.statusWarningForeground else DNColor.textSecondary, maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

/** A member's initial in a ring; for visas that are due the ring is the time left (out of 90 days). */
@Composable
private fun WidgetVisaRing(initial: String, days: Int?, isDue: Boolean) {
    val fraction = if (isDue) ((days ?: 0) / 90f).coerceIn(0.04f, 1f) else 1f
    Box(Modifier.padding(2.dp).size(44.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(44.dp)) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                if (isDue) DNColor.neutral200 else Hue.Lilac.end, 0f, 360f, false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke),
            )
            if (isDue) {
                drawArc(
                    DNColor.signSun, -90f, 360f * fraction, false,
                    topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(initial, style = widgetType(15f, 700, lineHeight = 20f), color = Hue.Lilac.ink)
    }
}

internal fun widgetFamilySpec(store: DemoStore, size: WidgetSize): WidgetSpec {
    val m = WidgetFamilyModel.from(store)
        ?: return WidgetSpec(summary = "Family visas. None due.", open = WidgetOpen.Push(Route.Bundle("renew-family-visas")))
    val names = m.members.filter { it.isDue }.joinToString(" and ") { it.name }
    return WidgetSpec(
        summary = "Family visas, ${m.dueCount} of ${m.members.size} due. $names expire in ${m.days} days.",
        hint = "Opens Renew your family's visas",
        quick = if (m.dueCount > 0 && size != WidgetSize.Small) WidgetQuickIntent.RenewFamilyVisas else null,
        open = WidgetOpen.Push(Route.Bundle("renew-family-visas")),
    )
}
