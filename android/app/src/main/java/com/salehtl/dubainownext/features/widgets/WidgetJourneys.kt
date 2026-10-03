package com.salehtl.dubainownext.features.widgets

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.data.RequestStep
import com.salehtl.dubainownext.data.WidgetSize
import com.salehtl.dubainownext.data.hue
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNIcons
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.RouteMark

// Start to finish: one widget per journey you've started (medium, large). Filled with the journey's
// hue, route mark, the stage as the figure, and a caption that always says whose turn it is.
// Plus the request tracker (a request in progress) and the certificates fallback.

// MARK: - Journey

@Composable
internal fun WidgetJourneyContent(journeyID: String, size: WidgetSize) {
    val store = LocalStore.current
    val m = WidgetJourneyModel.from(journeyID, store)
    when {
        m == null -> WidgetCalmState(
            title = Catalogue.service(journeyID)?.short ?: "Journey",
            message = "You haven't started this journey. Start it from Services and it shows here.",
        ) { WidgetGlyph(Icons.Rounded.Route, ink = DNColor.textSecondary, fill = DNColor.neutral100) }
        m.isQuiet -> JourneyQuiet(m)
        size == WidgetSize.Large -> JourneyLarge(m)
        else -> JourneyMedium(m)
    }
}

@Composable
private fun JourneyHeader(m: WidgetJourneyModel, trailing: String? = null) {
    val hue = m.service.hue
    Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(DNColor.white.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
            RouteMark(color = hue.ink, size = 18.dp)
        }
        Column(Modifier.weight(1f)) {
            WidgetTitle(m.service.short)
            Text(m.caption, style = widgetType(12f, 600, lineHeight = 16f), color = hue.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) Text(trailing, style = widgetType(12f, 600, lineHeight = 16f), color = hue.ink)
    }
}

@Composable
private fun JourneyFigure(m: WidgetJourneyModel) {
    WidgetFit(Modifier.padding(top = 2.dp), minScale = 0.75f) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WidgetRollingText(
                "${m.number}", widgetType(44f, 700, lineHeight = 46f, tracking = -0.8f, tabular = true), DNColor.textPrimary,
                m.number.toDouble(), Modifier.alignByBaseline(),
            )
            Text(
                "of ${m.total} · ${m.stageName}", style = widgetType(17f, 600, lineHeight = 22f), color = DNColor.textPrimary,
                maxLines = 1, modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

@Composable
private fun JourneyTurn(m: WidgetJourneyModel) {
    if (m.progress.needsYou) {
        WidgetChip("1 thing needs you", DNColor.textPrimary, DNColor.white, icon = Icons.Rounded.ArrowUpward)
    } else {
        WidgetCaption(m.progress.waitingOn, color = DNColor.textOnHueSecondary)
    }
}

@Composable
private fun JourneyBar(m: WidgetJourneyModel) =
    WidgetStagesBar(m.total, m.progress.stageIndex, m.service.hue.ink, m.service.hue.ink.copy(alpha = 0.2f))

@Composable
private fun JourneyMedium(m: WidgetJourneyModel) {
    Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp)) {
        JourneyHeader(m)
        JourneyFigure(m)
        JourneyTurn(m)
        Spacer(Modifier.heightIn(min = 8.dp).weight(1f))
        JourneyBar(m)
    }
}

@Composable
private fun JourneyLarge(m: WidgetJourneyModel) {
    Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp)) {
        JourneyHeader(m, trailing = m.age)
        JourneyFigure(m)
        JourneyTurn(m)
        Spacer(Modifier.heightIn(min = 10.dp).weight(1f))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DNColor.white.copy(alpha = 0.55f)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            m.stages.forEachIndexed { i, stage -> JourneyStageRow(m, i, stage) }
        }
    }
}

@Composable
private fun JourneyStageRow(m: WidgetJourneyModel, i: Int, stage: CatalogueService.Stage) {
    val hue = m.service.hue
    val current = m.progress.stageIndex
    Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            i < current -> Box(Modifier.size(16.dp).clip(CircleShape).background(hue.ink), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = DNColor.white, modifier = Modifier.size(11.dp))
            }
            i == current -> Box(
                Modifier.size(16.dp).border(3.dp, hue.end, CircleShape).padding(1.dp).clip(CircleShape).background(DNColor.white),
                contentAlignment = Alignment.Center,
            ) { Box(Modifier.size(6.dp).clip(CircleShape).background(hue.ink)) }
            else -> Box(Modifier.size(16.dp).border(1.5.dp, hue.ink.copy(alpha = 0.35f), CircleShape))
        }
        Text(
            "${i + 1} · ${stage.name}", style = widgetType(13f, if (i == current) 700 else 400, lineHeight = 18f),
            color = if (i <= current) DNColor.textPrimary else DNColor.textOnHueSecondary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        Text(
            when {
                i < current -> "Done"
                i == current -> "You are here"
                else -> stage.takes ?: ""
            },
            style = widgetType(12f, if (i == current) 700 else 400, lineHeight = 16f),
            color = if (i == current) hue.ink else DNColor.textOnHueSecondary, maxLines = 1,
        )
    }
}

/** Quiet tone for sensitive journeys: white, no hue, no big figure, no countdown. */
@Composable
private fun JourneyQuiet(m: WidgetJourneyModel) {
    Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp)) {
        Row(Modifier.height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(DNColor.neutral100), contentAlignment = Alignment.Center) {
                RouteMark(color = DNColor.textSecondary, size = 18.dp)
            }
            WidgetTitle(m.service.short)
        }
        Spacer(Modifier.heightIn(min = 6.dp).weight(1f))
        Text(
            "Next: ${WidgetFormat.lowerFirst(m.progress.headline)}", style = widgetType(17f, 600, lineHeight = 22f),
            color = DNColor.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        WidgetCaption("${m.progress.stageIndex} of ${m.total} stages done · whenever you're ready")
        Spacer(Modifier.heightIn(min = 8.dp).weight(1f))
        WidgetStagesBar(m.total, m.progress.stageIndex, DNColor.neutral400, DNColor.neutral200)
    }
}

internal fun widgetJourneySpec(id: String, store: DemoStore): WidgetSpec {
    val m = WidgetJourneyModel.from(id, store)
        ?: return WidgetSpec(summary = "${Catalogue.service(id)?.short ?: "Journey"}. Not started.", open = WidgetOpen.Push(Route.Journey(id)))
    if (m.isQuiet) {
        return WidgetSpec(
            summary = "${m.service.short}. Next: ${m.progress.headline}. ${m.progress.stageIndex} of ${m.total} stages done.",
            open = WidgetOpen.Push(Route.Journey(id)),
        )
    }
    return WidgetSpec(surface = WidgetSurface.OnHue(m.service.hue), summary = m.spoken, hint = "Opens the journey", open = WidgetOpen.Push(Route.Journey(id)))
}

// MARK: - Request tracker

/** A request in progress (e.g. the visa renewal): days left on your turn, steps done. */
@Composable
internal fun WidgetRequestContent(requestID: String) {
    val store = LocalStore.current
    val r = store.widgetRequest(requestID)
    if (r == null) {
        WidgetCalmState("Request tracker", "Nothing in progress. Requests you start show here") {
            WidgetGlyph(Icons.Rounded.Description, ink = Hue.Lilac.ink, fill = Hue.Lilac.gradient)
        }
        return
    }
    val done = r.steps.count { it.state == RequestStep.State.Done }
    Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            WidgetGlyph(DNIcons.forSymbol(r.symbol), ink = r.hue.ink, fill = r.hue.gradient)
            Spacer(Modifier.weight(1f))
            WidgetChip("$done of ${r.steps.size} done", r.hue.ink, r.hue.start)
        }
        RequestFigure(r, Modifier.padding(top = 6.dp))
        WidgetCaption(requestCaption(r), lines = 2)
        Spacer(Modifier.heightIn(min = 6.dp).weight(1f))
        WidgetStagesBar(r.steps.size, r.currentStepIndex ?: done, r.hue.ink, DNColor.neutral200)
    }
}

@Composable
private fun RequestFigure(r: RequestItem, modifier: Modifier) {
    when (val s = r.status) {
        is RequestItem.Status.YourTurn -> {
            val d = maxOf(0, DemoDate.daysFromToday(s.due))
            WidgetFigure("$d", unit = if (d == 1) "day" else "days", numeric = d.toDouble(), modifier = modifier)
        }
        is RequestItem.Status.InReview -> {
            val d = maxOf(0, DemoDate.daysFromToday(s.answerBy))
            WidgetFigure("$d", unit = if (d == 1) "day" else "days", numeric = d.toDouble(), modifier = modifier)
        }
        is RequestItem.Status.Approved -> WidgetFigure("Approved", size = 30f, modifier = modifier)
        RequestItem.Status.Done -> WidgetFigure("Done", size = 34f, modifier = modifier)
    }
}

private fun requestCaption(r: RequestItem): String = when (val s = r.status) {
    is RequestItem.Status.YourTurn -> "to " + WidgetFormat.lowerFirst(r.currentStepIndex?.let { r.steps[it].title } ?: r.title)
    is RequestItem.Status.InReview -> "until ${r.entity} answers"
    is RequestItem.Status.Approved -> WidgetFormat.capitalizedFirst(s.note)
    RequestItem.Status.Done -> r.title
}

internal fun widgetRequestSpec(id: String, store: DemoStore): WidgetSpec {
    val r = store.widgetRequest(id)
        ?: return WidgetSpec(summary = "Request tracker. Nothing in progress.", open = WidgetOpen.Push(Route.History))
    val done = r.steps.count { it.state == RequestStep.State.Done }
    return WidgetSpec(
        summary = "${r.title}. ${r.statusLine}. $done of ${r.steps.size} steps done.",
        hint = "Opens the request", open = WidgetOpen.Push(Route.Request(id)),
    )
}

// MARK: - Certificates (bundle with nothing live)

/**
 * Certificates and letters has no live state, so it isn't offered as a widget. If one is ever on
 * Home (old data), it says so calmly instead of disappearing.
 */
@Composable
internal fun WidgetCertificatesContent() {
    WidgetCalmState("Certificates and letters", "Nothing in progress. Ask for a certificate and it shows here") { WidgetBundleMark() }
}
