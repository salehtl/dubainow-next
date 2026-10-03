package com.salehtl.dubainownext.features.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.WidgetSize
import com.salehtl.dubainownext.data.personas.PrayerTimes
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.ComingSoon
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Faith and family: Prayer times (small, large). City life: Weather and beach (small).

// MARK: - Prayer times

internal class WidgetPrayerModel(val now: LocalDateTime) {
    data class Slot(val name: String, val date: LocalDateTime)

    val next: Pair<PrayerTimes.Prayer, LocalDateTime> = PrayerTimes.next(now)

    fun date(p: PrayerTimes.Prayer): LocalDateTime = now.withHour(p.hour).withMinute(p.minute).withSecond(0).withNano(0)

    /** The five prayers (no Sunrise) for the row in the large size. */
    val prayers: List<Slot> = PrayerTimes.today.filter { it.name != "Sunrise" }.map { Slot(it.name, date(it)) }

    private val sunrise: LocalDateTime = PrayerTimes.today.firstOrNull { it.name == "Sunrise" }?.let { date(it) } ?: now
    private val sunset: LocalDateTime = PrayerTimes.today.firstOrNull { it.name == "Maghrib" }?.let { date(it) } ?: now

    /** 0 at sunrise, 1 at Maghrib; null at night. */
    fun dayFraction(d: LocalDateTime): Double? {
        val span = Duration.between(sunrise, sunset).seconds.toDouble()
        if (span <= 0) return null
        val t = Duration.between(sunrise, d).seconds / span
        return if (t in 0.0..1.0) t else null
    }

    val countdown: String get() = DemoDate.countdown(next.second, now)
    val minutesToNext: Int get() = maxOf(0, Duration.between(now, next.second).toMinutes().toInt())
    val clock: String get() = WidgetFormat.clock(next.first.hour, next.first.minute)
}

@Composable
internal fun WidgetPrayerContent(size: WidgetSize, now: LocalDateTime) {
    val m = WidgetPrayerModel(now)
    if (size == WidgetSize.Large) PrayerLarge(m) else PrayerSmall(m)
}

@Composable
private fun Moon() = Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = DNColor.widgetSkyGlyph, modifier = Modifier.size(15.dp))

@Composable
private fun PrayerSmall(m: WidgetPrayerModel) {
    Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Moon()
            WidgetTitle(m.next.first.name, modifier = Modifier.weight(1f))
            Text(m.countdown, style = widgetType(13f, 500, lineHeight = 18f, tabular = true), color = DNColor.textOnHueSecondary, maxLines = 1)
        }
        WidgetFigure(m.clock, size = 40f, numeric = m.minutesToNext.toDouble(), modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.heightIn(min = 4.dp).weight(1f))
        WidgetSunArc(m, large = false, modifier = Modifier.fillMaxWidth().height(52.dp))
    }
}

@Composable
private fun PrayerLarge(m: WidgetPrayerModel) {
    val store = LocalStore.current
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Moon()
            WidgetTitle("Prayer times", modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.Place, contentDescription = null, tint = DNColor.iconSecondary, modifier = Modifier.size(13.dp))
            Text(
                store.persona?.area?.ifEmpty { null } ?: "Dubai", style = widgetType(13f, 500, lineHeight = 18f),
                color = DNColor.textOnHueSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.heightIn(min = 8.dp).weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(m.next.first.name, style = widgetType(17f, 600, lineHeight = 22f), color = DNColor.textPrimary, modifier = Modifier.alignByBaseline())
                Text(m.countdown, style = widgetType(15f, 500, lineHeight = 20f, tabular = true), color = DNColor.textOnHueSecondary, modifier = Modifier.alignByBaseline())
            }
            WidgetFigure(m.clock, numeric = m.minutesToNext.toDouble())
        }
        Spacer(Modifier.heightIn(min = 8.dp).weight(1f))
        WidgetSunArc(m, large = true, modifier = Modifier.fillMaxWidth().height(116.dp))
        Spacer(Modifier.heightIn(min = 8.dp).weight(1f))
        Row(Modifier.fillMaxWidth()) {
            m.prayers.forEach { slot ->
                val isNext = slot.name == m.next.first.name
                Column(
                    Modifier.weight(1f)
                        .let {
                            if (isNext) it.shadow(1.dp, RoundedCornerShape(14.dp), ambientColor = DNColor.elevationNear, spotColor = DNColor.elevationFar)
                                .clip(RoundedCornerShape(14.dp)).background(DNColor.widgetSurface)
                            else it
                        }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        slot.name, style = widgetType(12f, if (isNext) 600 else 500, lineHeight = 16f),
                        color = if (isNext) DNColor.textPrimary else DNColor.textOnHueSecondary, maxLines = 1,
                    )
                    Text(
                        WidgetFormat.clock(slot.date), style = widgetType(17f, if (isNext) 700 else 600, lineHeight = 22f, tabular = true),
                        color = DNColor.textPrimary, maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * The sun's path across the sky: a dotted arc from sunrise to Maghrib, the travelled part in sun
 * orange, the sun where it is now, an open marker at the next prayer and dots at the others.
 */
@Composable
private fun WidgetSunArc(model: WidgetPrayerModel, large: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val inset = (if (large) 10 else 5.5f).toFloat().dp.toPx()
        val base = size.height - (if (large) 8 else 6).dp.toPx()
        val cx = size.width / 2
        val rx = (size.width - inset * 2) / 2
        val ry = base - (if (large) 8 else 6).dp.toPx()
        fun point(t: Double): Offset {
            val a = PI * (1 - t)
            return Offset((cx + rx * cos(a)).toFloat(), (base - ry * sin(a)).toFloat())
        }
        fun arc(to: Double): Path = Path().apply {
            val steps = 64
            for (i in 0..steps) {
                val p = point(to * i / steps)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
        }

        drawLine(DNColor.widgetSkyHorizon, Offset(0f, base + 0.5f), Offset(size.width, base + 0.5f), strokeWidth = 1.dp.toPx())
        drawPath(
            arc(1.0), DNColor.widgetSkyPath,
            style = Stroke(
                1.6.dp.toPx(), cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), (if (large) 5 else 4).dp.toPx())),
            ),
        )
        val sunT = model.dayFraction(model.now)
        if (sunT != null) {
            drawPath(arc(sunT), DNColor.signSun, style = Stroke((if (large) 2.6f else 2.4f).dp.toPx(), cap = StrokeCap.Round))
        }

        val markerR = (if (large) 3.2f else 2.6f).dp.toPx()
        if (large) drawCircle(DNColor.widgetSkyMarker, markerR, point(0.0))
        model.prayers.forEach { slot ->
            val t = model.dayFraction(slot.date) ?: return@forEach
            if (sunT != null && t < sunT) return@forEach
            val p = point(t)
            if (slot.name == model.next.first.name) {
                val r = (if (large) 4.6f else 3.6f).dp.toPx()
                drawCircle(DNColor.widgetMoon, r, p)
                drawCircle(DNColor.widgetSkyGlyph, r, p, style = Stroke((if (large) 2f else 1.8f).dp.toPx()))
            } else {
                drawCircle(DNColor.widgetSkyMarker, markerR, p)
            }
        }
        if (sunT != null) drawSun(point(sunT), large)
    }
}

private fun DrawScope.drawSun(at: Offset, large: Boolean) {
    drawCircle(DNColor.widgetSunGlow, (if (large) 15 else 11).dp.toPx(), at)
    drawCircle(DNColor.widgetSun, (if (large) 8f else 6.5f).dp.toPx(), at)
}

internal fun widgetPrayerSpec(now: LocalDateTime): WidgetSpec {
    val m = WidgetPrayerModel(now)
    return WidgetSpec(
        surface = WidgetSurface.Sky,
        summary = "Next prayer ${m.next.first.name} at ${m.clock}, in ${WidgetFormat.spokenDuration(m.minutesToNext)}.",
        hint = "Opens Check prayer times",
        open = WidgetOpen.Service("check-prayer-times"),
    )
}

// MARK: - Weather and beach

/** The demo's weather (NCM and Dubai Municipality aren't connected). */
private const val WEATHER_TEMPERATURE = "37°"
private const val WEATHER_DETAIL = "Feels 42° · UV 9"

@Composable
internal fun WidgetWeatherContent() {
    val store = LocalStore.current
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = DNColor.signSun, modifier = Modifier.size(16.dp))
            WidgetTitle(store.state.weatherSpot)
        }
        WidgetFigure(WEATHER_TEMPERATURE, modifier = Modifier.padding(top = 2.dp))
        WidgetCaption(WEATHER_DETAIL)
        Spacer(Modifier.heightIn(min = 8.dp).weight(1f))
        WidgetChip(
            "Safe to swim", DNColor.statusSuccessForeground, DNColor.statusSuccessSurface,
            icon = Icons.Rounded.Flag, height = 28.dp,
        )
    }
}

internal fun widgetWeatherSpec(store: DemoStore): WidgetSpec = WidgetSpec(
    summary = "${store.state.weatherSpot}, 37 degrees, feels like 42, UV index 9. Green flag: safe to swim.",
    open = WidgetOpen.Soon(
        ComingSoon(
            title = "Weather and beaches",
            detail = "Forecasts and beach flags come from NCM and Dubai Municipality. They aren't part of this demo yet.",
            owner = "NCM and Dubai Municipality", symbol = widgetSymbol("sun.max.fill", "leaf.fill"), hue = Hue.Sand, kind = ComingSoon.Kind.NotInDemo,
        )
    ),
)
