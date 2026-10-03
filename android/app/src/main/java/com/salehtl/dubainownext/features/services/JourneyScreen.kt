package com.salehtl.dubainownext.features.services

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.JourneyProgress
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.data.hue
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.Inter
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.DNButtonKind
import com.salehtl.dubainownext.design.components.RouteMark
import com.salehtl.dubainownext.design.components.SegmentedProgress

/**
 * A journey page ("Start to finish"), laid out like Paper C1 `DP3-0` (Buy a used car):
 * overline, name, description, scale, progress when started, then the stages as a vertical
 * step list with their services and the steps that aren't in DubaiNow yet.
 */
@Composable
fun JourneyScreen(serviceId: String) {
    val service = Catalogue.shared.service(serviceId)
    when (service?.type) {
        null -> ServicesMissingRecord(serviceId)
        ServiceType.Journey -> ServicesJourneyPage(service)
        ServiceType.Bundle -> ServicesBundlePage(service)
        ServiceType.Standalone -> ServiceStartScreen(serviceId)
    }
}

private enum class ServicesStageState { Done, Current, Upcoming }

@Composable
internal fun ServicesJourneyPage(service: CatalogueService) {
    val store = LocalStore.current; val router = LocalRouter.current
    /** "After a death" is told quietly: no hue, no accent, no celebratory words. */
    val isSensitive = service.id == "after-a-death"
    val stages = service.stages.orEmpty()
    val progress = store.servicesJourney(service.id)
    val accent = if (isSensitive) DNColor.neutral600 else DNColor.actionAccent
    val doneColor = if (isSensitive) DNColor.neutral600 else DNColor.statusSuccessForeground

    fun stateOf(index: Int): ServicesStageState {
        val p = progress ?: return ServicesStageState.Upcoming
        return when {
            index < p.stageIndex -> ServicesStageState.Done
            index == p.stageIndex -> ServicesStageState.Current
            else -> ServicesStageState.Upcoming
        }
    }

    fun startSheet() {
        router.present(SheetRoute.ComingSoonSheet(ServicesData.comingSoon(
            service, ComingSoon.Kind.ServicesPhase,
            if (isSensitive) "Soon DubaiNow will keep these steps together for you, in order, and tell each entity once."
            else "Soon DubaiNow will keep this plan for you: what's done, what's next and who it's waiting on.",
        )))
    }

    val state = rememberLazyListState()
    val keys = buildList {
        add("header"); if (progress != null) add("progress"); add("primary")
        stages.indices.forEach { add("stage-$it") }
        if (!service.entities.isNullOrEmpty()) add("entities")
        add("footer")
    }
    ServicesLaunchScroll(state) { key -> (if (key == "steps") keys.indexOf("stage-0") else keys.indexOf(key)).takeIf { it >= 0 } }

    ServicesPage(
        base = DNColor.surfacePrimary,
        wash = if (isSensitive) null else service.hue.start.copy(alpha = 0.7f), washHeight = 300.dp,
        state = state, actions = { ServicesRecordActions(service) },
    ) {
        item(key = "header") {
            Column(ServicesInset.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RouteMark(color = accent, size = 16.dp)
                    Text("START TO FINISH", style = DNTextStyle.Overline.style, color = accent)
                }
                Text(service.name, style = DNTextStyle.Title1.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
                if (isSensitive) {
                    Text(
                        "We're sorry for your loss. You can take these steps in your own time and come back to them whenever you need to.",
                        style = DNTextStyle.Callout.style, color = DNColor.textPrimary,
                    )
                }
                Text(service.description, style = DNTextStyle.Callout.style, color = DNColor.textSecondary)
                ServicesFlow(Modifier.padding(top = 6.dp)) {
                    ServicesInfoChip(servicesIcon("list.number"), ServicesText.count(stages.size, "stage"))
                    service.entities?.takeIf { it.isNotEmpty() }?.let {
                        ServicesInfoChip(servicesIcon("building.columns"), ServicesText.count(it.size, "entity", "entities"))
                    }
                    service.lasts?.let { ServicesInfoChip(servicesIcon("clock"), it) }
                }
            }
        }
        if (progress != null) item(key = "progress") {
            ServicesJourneyProgressCard(progress, stages.size, accent, doneColor, ServicesInset.padding(top = 22.dp))
        }
        item(key = "primary") {
            Box(ServicesInset.padding(top = 22.dp)) {
                val p = progress
                if (p != null && p.stageIndex in stages.indices) {
                    val next = stages[p.stageIndex].parts.firstNotNullOfOrNull { Catalogue.service(it) }
                    ServicesButton(
                        "Continue: ${p.headline}", trailing = Icons.AutoMirrored.Rounded.ArrowForward,
                        onClick = { if (next != null) router.open(next) else startSheet() },
                    )
                } else {
                    ServicesButton(
                        if (isSensitive) "Start when you're ready" else "Start this journey",
                        kind = if (isSensitive) DNButtonKind.Inverse else DNButtonKind.Primary,
                        trailing = if (isSensitive) null else Icons.AutoMirrored.Rounded.ArrowForward,
                        onClick = { startSheet() },
                    )
                }
            }
        }
        stages.forEachIndexed { index, stage ->
            item(key = "stage-$index") {
                ServicesStageRow(
                    service, stage, index, stages.size, stateOf(index), progress, accent, doneColor, isSensitive,
                    ServicesInset.padding(top = if (index == 0) 28.dp else 0.dp),
                )
            }
        }
        service.entities?.takeIf { it.isNotEmpty() }?.let { entities ->
            item(key = "entities") {
                Column(ServicesInset.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ServicesPageHeading("Who's involved")
                    ServicesFlow { entities.forEach { ServicesInfoChip(servicesIcon("building.columns"), it) } }
                }
            }
        }
        item(key = "footer") {
            ServicesFooter("DubaiNow keeps the plan. Each step is run by its own entity.", ServicesInset.padding(top = 22.dp))
        }
    }
}

@Composable
private fun ServicesJourneyProgressCard(p: JourneyProgress, total: Int, accent: Color, doneColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.card)).background(DNColor.surfaceSecondary).padding(16.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "You're at stage ${p.stageIndex + 1} of $total", style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold),
                color = DNColor.textPrimary, modifier = Modifier.weight(1f),
            )
            Text("from your activity", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        }
        SegmentedProgress(
            total = maxOf(total, 1), done = p.stageIndex, doneColor = doneColor, currentColor = accent,
            trackColor = DNColor.neutral300, height = 6.dp,
        )
        Text(listOf(p.headline, p.waitingOn).joinToString(" · "), style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
    }
}

@Composable
private fun ServicesStageRow(
    service: CatalogueService,
    stage: CatalogueService.Stage,
    index: Int,
    count: Int,
    st: ServicesStageState,
    progress: JourneyProgress?,
    accent: Color,
    doneColor: Color,
    isSensitive: Boolean,
    modifier: Modifier = Modifier,
) {
    val router = LocalRouter.current
    val isLast = index == count - 1
    val parts = stage.parts.mapNotNull { Catalogue.service(it) }
    val detail = when (st) {
        ServicesStageState.Done -> "Done"
        ServicesStageState.Current -> progress?.waitingOn
        ServicesStageState.Upcoming -> stage.takes
    }
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        // The marker and line are decoration; the stage heading below says the same for TalkBack.
        Column(Modifier.width(32.dp).fillMaxHeight().clearAndSetSemantics {}, horizontalAlignment = Alignment.CenterHorizontally) {
            ServicesStageMarker(st, index + 1, accent, doneColor)
            if (!isLast) {
                Box(Modifier.width(2.dp).weight(1f).background(if (st == ServicesStageState.Done) doneColor else DNColor.neutral300))
            }
        }
        Column(Modifier.weight(1f).padding(top = 4.dp, bottom = if (isLast) 8.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(
                Modifier.clearAndSetSemantics {
                    contentDescription = "Stage ${index + 1} of $count, ${stage.name}" +
                        (if (st == ServicesStageState.Done) ", done" else if (st == ServicesStageState.Current) ", you're here" else "") + ". ${detail ?: ""}"
                    heading()
                },
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stage.name, color = DNColor.textPrimary,
                    style = DNTextStyle.Headline.style.copy(fontWeight = if (st == ServicesStageState.Current) FontWeight.Bold else FontWeight.SemiBold),
                )
                detail?.let { Text(it, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary) }
            }
            parts.forEach { part ->
                if (st == ServicesStageState.Current) ServicesCurrentPart(part) { router.open(part) }
                else ServicesLink(part.name, weight = FontWeight.SemiBold) { router.open(part) }
            }
            stage.notInDubaiNowYet.forEach { missing ->
                ServicesMissingStep(missing) {
                    router.present(SheetRoute.ComingSoonSheet(ComingSoon(
                        title = missing.name,
                        detail = "${missing.owner ?: "Its owner"} handles this outside DubaiNow for now. It joins this journey when it's in DubaiNow.",
                        owner = missing.owner, symbol = "building.columns.fill",
                        hue = if (isSensitive) Hue.Lilac else service.hue, kind = ComingSoon.Kind.NotInDemo,
                    )))
                }
            }
        }
    }
}

@Composable
private fun ServicesStageMarker(st: ServicesStageState, number: Int, accent: Color, doneColor: Color) {
    // Fixed type inside the 32 dp disc (iOS uses a fixed 16 pt); the stage heading carries the text for TalkBack.
    val fixed = with(LocalDensity.current) { 16.dp.toSp() }
    val numberStyle = DNTextStyle.Headline.style.copy(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = fixed, lineHeight = fixed, fontFeatureSettings = "tnum")
    when (st) {
        ServicesStageState.Done -> Box(Modifier.size(32.dp).clip(CircleShape).background(doneColor), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        ServicesStageState.Current -> Box(
            Modifier.size(32.dp).drawBehind { drawCircle(accent.copy(alpha = 0.25f), radius = size.minDimension / 2 + 5.dp.toPx()) }
                .clip(CircleShape).background(accent),
            contentAlignment = Alignment.Center,
        ) { Text("$number", style = numberStyle, color = Color.White) }
        ServicesStageState.Upcoming -> Box(
            Modifier.size(32.dp).clip(CircleShape).background(DNColor.surfacePrimary).border(1.5.dp, DNColor.neutral300, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("$number", style = numberStyle, color = DNColor.textPrimary) }
    }
}

/** The current stage's services as tonal pills with an arrow. */
@Composable
private fun ServicesCurrentPart(part: CatalogueService, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(DNColor.actionAccentSubtle)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(part.name, style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textLink, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = DNColor.actionAccent, modifier = Modifier.size(18.dp))
    }
}

/** A step that isn't in DubaiNow yet (dashed outline, opens a "not in DubaiNow" sheet). */
@Composable
private fun ServicesMissingStep(missing: CatalogueService.Missing, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .drawBehind {
                drawRoundRect(
                    DNColor.neutral300, cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                )
            }
            .clickable(onClickLabel = "Not in DubaiNow yet", onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = DNColor.textSecondary, modifier = Modifier.padding(top = 2.dp).size(18.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(missing.name, style = DNTextStyle.Callout.style, color = DNColor.textPrimary)
            Text("${missing.owner ?: "Owner to confirm"} · not in DubaiNow yet", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        }
    }
}
