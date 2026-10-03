package com.salehtl.dubainownext.features.assistant

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.EnergyReading
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.Payable
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.data.RequestStep
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNButtonSize
import com.salehtl.dubainownext.design.components.HappinessFace
import com.salehtl.dubainownext.design.components.HappinessFacesPicker
import com.salehtl.dubainownext.design.components.HueIcon
import com.salehtl.dubainownext.design.components.SegmentedProgress
import com.salehtl.dubainownext.design.components.dnElevation
import java.math.BigDecimal

private val CardShape = RoundedCornerShape(28.dp)
private val SemiBold = FontWeight.SemiBold

/**
 * One white card (radius 28) hangs from the face: what you asked, its answer, and the task it's
 * running (Paper TA7-0 / TQX-0 / U2V-0 / UXH-0). Scrolls inside when it's taller than the space.
 */
@Composable
internal fun AssistantCardView(
    card: AssistantCard,
    english: Boolean,
    maxHeight: Dp,
    reduceMotion: Boolean,
    onToggleEnglish: () -> Unit,
    onStop: () -> Unit,
    onTakeOver: () -> Unit,
    onAction: (AssistantFollowUp) -> Unit,
    onReceipt: (String) -> Unit,
    onRate: (HappinessFace, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .dnElevation(28.dp)
            .clip(CardShape)
            .background(DNColor.surfacePrimary)
            .animateContentSize(DNMotion.smooth())
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState()),
    ) {
        Said(card, english, reduceMotion, onToggleEnglish)
        card.surface?.let { surface ->
            AssistantSurfaceView(surface, english, reduceMotion, onReceipt, Modifier.padding(horizontal = 8.dp))
        }
        card.rating?.let { AssistantRating(it, card.id, onRate) }
        when (val controls = card.controls) {
            AssistantControls.None -> if (card.surface != null && card.rating == null) Spacer(Modifier.height(8.dp))
            AssistantControls.Task -> Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val zh = card.isMandarin && !english
                // TalkBack reads them in the card's language (the English hints only in English).
                AssistantSoftButton(if (zh) "停止" else "Stop", DNColor.textPrimary, if (zh) "停止" else "Stop. Nothing is paid or sent",
                    Modifier.weight(1f), showsStop = true, onClick = onStop)
                AssistantSoftButton(if (zh) "我自己来" else "I'll do it myself", DNColor.textLink, if (zh) "我自己来" else "I'll do it myself. Opens the service at the same step",
                    Modifier.weight(1f), onClick = onTakeOver)
            }
            is AssistantControls.Action -> DNButton(
                controls.title.resolved(english), onClick = { onAction(controls.followUp) },
                modifier = Modifier.padding(8.dp), size = DNButtonSize.Medium,
            )
        }
    }
}

@Composable
private fun Said(card: AssistantCard, english: Boolean, reduceMotion: Boolean, onToggleEnglish: () -> Unit) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val quote = card.quote.resolved(english)
            Text(
                "“$quote”", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Medium), color = DNColor.textSecondary,
                modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = "You asked: $quote" },
            )
            if (card.isMandarin) {
                Box(
                    Modifier.heightIn(min = 48.dp).clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onToggleEnglish)
                        .padding(horizontal = 6.dp)
                        .clearAndSetSemantics { contentDescription = if (english) "Show in Chinese" else "Show in English" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (english) "显示中文" else "Show in English", style = DNTextStyle.Footnote.style.copy(fontWeight = SemiBold), color = DNColor.textLink)
                }
            }
        }
        if (card.isThinking) {
            AssistantThinkingDots(reduceMotion, Modifier.height(26.dp).semantics { contentDescription = "Thinking" })
        } else {
            val headline = card.headline.resolved(english)
            AnimatedContent(headline, transitionSpec = { fadeIn(DNMotion.base()) togetherWith fadeOut(DNMotion.fast()) }, label = "headline") { text ->
                Text(
                    text, style = DNTextStyle.Title3.style, color = DNColor.textPrimary,
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                )
            }
        }
    }
}

@Composable
private fun AssistantThinkingDots(reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "dots").animateFloat(0f, 2f * Math.PI.toFloat(), infiniteRepeatable(tween(1257, easing = LinearEasing)), label = "t")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val a = if (reduceMotion) 0.6f else 0.3f + 0.7f * maxOf(0f, kotlin.math.sin(t - i * 0.7f))
            Box(Modifier.size(8.dp).alpha(a).clip(CircleShape).background(DNColor.blue600))
        }
    }
}

/** Inline Happiness faces after a task. */
@Composable
private fun AssistantRating(prompt: AssistantRatingPrompt, cardID: String, onRate: (HappinessFace, String) -> Unit) {
    var rating by remember(cardID) { mutableStateOf<HappinessFace?>(null) }
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(prompt.question, style = DNTextStyle.RowTitle.style.copy(fontWeight = SemiBold), color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
            Text(if (rating == null) prompt.detail else prompt.thanks, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        HappinessFacesPicker(rating, onSelect = { rating = it; onRate(it, prompt.receiptID) }, size = 56.dp)
    }
}

/** Stop · I'll do it myself: 48 dp, a soft tint on the card. */
@Composable
private fun AssistantSoftButton(
    title: String, color: Color, description: String, modifier: Modifier = Modifier, showsStop: Boolean = false, onClick: () -> Unit,
) {
    Row(
        modifier.heightIn(min = 48.dp).clip(CircleShape).background(DNColor.blue950.copy(alpha = 0.06f))
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showsStop) Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        // Two lines rather than "I'll do it…" with large text.
        Text(title, style = DNTextStyle.RowTitle.style.copy(fontWeight = SemiBold, textAlign = TextAlign.Center), color = color, maxLines = 2,
            modifier = Modifier.weight(1f, fill = false))
    }
}

// MARK: - The tinted block inside the card

@Composable
internal fun AssistantSurfaceView(
    surface: AssistantSurface,
    english: Boolean,
    reduceMotion: Boolean,
    onReceipt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(DNColor.blue50.copy(alpha = 0.92f))
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp),
    ) {
        when (surface) {
            is AssistantSurface.Task -> TaskSurface(surface.block, english, reduceMotion, onReceipt)
            is AssistantSurface.Payables -> PayablesSurface(surface.items)
            is AssistantSurface.Request -> RequestSurface(surface.item, reduceMotion)
            is AssistantSurface.Journey -> JourneySurface(surface.block)
            is AssistantSurface.Complaint -> ComplaintSurface(surface.block, english)
            is AssistantSurface.Rows -> ListSurface(surface.rows)
            is AssistantSurface.Energy -> EnergySurface(surface.reading)
        }
    }
}

@Composable
private fun TaskSurface(t: AssistantTaskBlock, english: Boolean, reduceMotion: Boolean, onReceipt: (String) -> Unit) {
    SurfaceHeader(t.symbol, t.hue, t.title.resolved(english), t.subtitle.resolved(english)) {
        when (t.status) {
            AssistantTaskStatus.Done -> Text("Done", color = DNColor.statusSuccessForeground, style = trailingStyle)
            AssistantTaskStatus.Stopped -> Text("Stopped", color = DNColor.textSecondary, style = trailingStyle)
            else -> Text("${t.position} of ${t.steps.size}", color = DNColor.textLink, style = trailingStyle)
        }
    }
    t.steps.forEach { step -> AssistantStepRow(step, english, reduceMotion) }
    val receiptID = t.receiptID
    if (t.status == AssistantTaskStatus.Done && receiptID != null) {
        AssistantRule()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                Icon(assistantIcon("doc.text"), contentDescription = null, tint = DNColor.iconSecondary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                t.paidLine?.let { Text(it, style = DNTextStyle.Footnote.style.copy(fontWeight = SemiBold), color = DNColor.textPrimary) }
                t.reference?.let { Text("Reference $it", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, maxLines = 1) }
            }
            Box(
                Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).clip(CircleShape)
                    .clickable(role = Role.Button) { onReceipt(receiptID) }
                    .clearAndSetSemantics { contentDescription = "View the receipt" }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Receipt", style = DNTextStyle.Subheadline.style.copy(fontWeight = SemiBold), color = DNColor.textLink) }
        }
    }
}

@Composable
private fun PayablesSurface(items: List<Payable>) {
    val total = items.fold(BigDecimal.ZERO) { a, p -> a + p.amount }
    SurfaceHeader("creditcard.fill", Hue.Sky, if (items.size == 1) "1 payment due" else "${items.size} payments due", "From your linked accounts") {}
    items.forEach { p ->
        AssistantRule()
        Row(Modifier.padding(vertical = 7.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) { HueIcon(assistantIcon(p.symbol), p.hue, size = 28.dp) }
            Column(Modifier.weight(1f)) {
                Text(p.title, style = DNTextStyle.Subheadline.style, color = DNColor.textPrimary)
                Text(listOfNotNull(p.entity, p.due?.let { DemoDate.dueLabel(it) } ?: p.note).joinToString(" · "), style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
            }
            Text(Money.aed(p.amount), style = numericStyle.copy(fontWeight = FontWeight.Medium), color = DNColor.textSecondary)
        }
    }
    AssistantRule()
    Row(Modifier.fillMaxWidth().heightIn(min = 38.dp).padding(start = 48.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Text("Total", style = DNTextStyle.Subheadline.style.copy(fontWeight = SemiBold), color = DNColor.textPrimary, modifier = Modifier.weight(1f))
        Text(Money.aed(total), style = numericStyle.copy(fontWeight = SemiBold), color = DNColor.textPrimary)
    }
}

@Composable
private fun RequestSurface(r: RequestItem, reduceMotion: Boolean) {
    val current = r.currentStepIndex ?: (r.steps.size - 1)
    SurfaceHeader(r.symbol, r.hue, r.title, "${r.entity} · ${r.reference}") {
        Text("${minOf(current + 1, r.steps.size)} of ${r.steps.size}", color = DNColor.textLink, style = trailingStyle)
    }
    r.steps.forEachIndexed { i, s ->
        val state = when (s.state) {
            RequestStep.State.Done -> AssistantStepState.Done
            RequestStep.State.Current -> AssistantStepState.YourTurn
            RequestStep.State.Upcoming -> AssistantStepState.Pending
        }
        AssistantStepRow(AssistantStep(i, AssistantText(s.title), state = state), english = true, reduceMotion = reduceMotion,
            detail = if (s.state == RequestStep.State.Current) s.detail else null)
    }
}

@Composable
private fun JourneySurface(j: AssistantJourneyBlock) {
    SurfaceHeader(j.symbol, j.hue, j.title, j.subtitle) {
        Text("${j.stage + 1} of ${j.stageCount}", color = DNColor.textLink, style = trailingStyle)
    }
    SegmentedProgress(total = j.stageCount, done = j.stage, modifier = Modifier.padding(bottom = 10.dp))
    AssistantRule()
    Labelled("Now", j.now, j.nowDetail)
    j.next?.let {
        AssistantRule()
        Labelled("Next", it, null)
    }
}

@Composable
private fun Labelled(label: String, value: String, detail: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = DNTextStyle.Footnote.style.copy(fontWeight = SemiBold), color = DNColor.textSecondary, modifier = Modifier.width(36.dp).padding(top = 1.dp))
        Column(Modifier.weight(1f)) {
            Text(value, style = DNTextStyle.Subheadline.style, color = DNColor.textPrimary)
            detail?.let { Text(it, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary) }
        }
    }
}

@Composable
private fun ComplaintSurface(b: AssistantComplaintBlock, english: Boolean) {
    var showsOriginal by remember { mutableStateOf(false) }
    SurfaceHeader(
        "briefcase.fill", Hue.Sand,
        if (english) "Report a problem with your employer" else "投诉雇主",
        if (english) "Permanent Committee for Labour Affairs" else "Report a problem with your employer",
    ) {
        when (b.status) {
            AssistantTaskStatus.Done -> Text(if (english) "Sent" else "已提交", color = DNColor.textSecondary, style = trailingStyle)
            AssistantTaskStatus.Stopped -> Text(if (english) "Stopped" else "已停止", color = DNColor.textSecondary, style = trailingStyle)
            else -> Unit
        }
    }
    val labelWidth = if (english) 64.dp else 40.dp
    b.fields.take(b.visibleCount).forEach { f ->
        AssistantFadeIn(f.id) {
            Column {
                AssistantRule()
                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(f.label.resolved(english), style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, modifier = Modifier.width(labelWidth).padding(top = 1.dp))
                    Text(f.value.resolved(english), style = DNTextStyle.Subheadline.style, color = DNColor.textPrimary, modifier = Modifier.weight(1f))
                }
            }
        }
    }
    b.reference?.let { ref ->
        AssistantRule()
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (english) "Ref" else "编号", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, modifier = Modifier.width(labelWidth).padding(top = 1.dp))
            Text(ref, style = numericStyle, color = DNColor.textPrimary, modifier = Modifier.weight(1f))
        }
    }
    if (b.visibleCount == b.fields.size) {
        AssistantRule()
        val chevron by animateFloatAsState(if (showsOriginal) 90f else 0f, DNMotion.smooth(), label = "chevron")
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { showsOriginal = !showsOriginal }
                .semantics(mergeDescendants = true) { stateDescription = if (showsOriginal) "Shown" else "Hidden" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (english) "See the English and Arabic text" else "查看英文和阿拉伯文原文", style = DNTextStyle.Subheadline.style.copy(fontWeight = SemiBold),
                color = DNColor.textLink, modifier = Modifier.weight(1f))
            Icon(assistantIcon("chevron.right"), contentDescription = null, tint = DNColor.textLink, modifier = Modifier.size(20.dp).rotate(chevron))
        }
        AnimatedVisibility(showsOriginal, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(b.originalEnglish, style = DNTextStyle.Footnote.style, color = DNColor.textPrimary)
                Text(
                    b.originalArabic, color = DNColor.textPrimary, modifier = Modifier.fillMaxWidth(),
                    style = DNTextStyle.Footnote.style.copy(textAlign = TextAlign.Right, textDirection = TextDirection.Rtl),
                )
            }
        }
    }
}

@Composable
private fun EnergySurface(e: EnergyReading) {
    val peak = maxOf(1, e.lastSixMonths.maxOrNull() ?: 1)
    SurfaceHeader("bolt.fill", Hue.Sand, "Energy use", e.comparison) {}
    Canvas(
        Modifier.fillMaxWidth().padding(vertical = 8.dp).height(64.dp)
            .semantics { contentDescription = "Last six months, in kilowatt hours: ${e.lastSixMonths.joinToString(", ")}" },
    ) {
        val n = e.lastSixMonths.size
        if (n == 0) return@Canvas
        val gap = 8.dp.toPx()
        val bw = (size.width - gap * (n - 1)) / n
        e.lastSixMonths.forEachIndexed { i, kwh ->
            val h = maxOf(6.dp.toPx(), size.height * kwh / peak)
            drawRoundRect(
                if (i == n - 1) DNColor.actionAccent else DNColor.blue200,
                topLeft = Offset(i * (bw + gap), size.height - h), size = Size(bw, h), cornerRadius = CornerRadius(4.dp.toPx()),
            )
        }
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text("6 months ago", style = DNTextStyle.Caption1.style, color = DNColor.textSecondary, modifier = Modifier.weight(1f))
        Text(e.monthLabel, style = DNTextStyle.Caption1.style, color = DNColor.textSecondary)
    }
}

@Composable
private fun ListSurface(rows: List<AssistantListRow>) {
    rows.forEachIndexed { i, row ->
        if (i > 0) AssistantRule()
        Row(Modifier.padding(vertical = 7.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) { HueIcon(assistantIcon(row.symbol), row.hue, size = 28.dp) }
            Column(Modifier.weight(1f)) {
                Text(row.title, style = DNTextStyle.Subheadline.style, color = DNColor.textPrimary)
                Text(row.detail, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
            }
            row.value?.let { Text(it, style = numericStyle.copy(fontWeight = FontWeight.Medium), color = DNColor.textSecondary) }
        }
    }
}

// MARK: Pieces

private val trailingStyle get() = DNTextStyle.Footnote.style.copy(fontWeight = SemiBold, fontFeatureSettings = "tnum")
private val numericStyle get() = DNTextStyle.Subheadline.style.copy(fontFeatureSettings = "tnum")

@Composable
private fun SurfaceHeader(symbol: String, hue: Hue, title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HueIcon(assistantIcon(symbol), hue, size = 36.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = DNTextStyle.RowTitle.style.copy(fontWeight = SemiBold), color = DNColor.textPrimary)
            Text(subtitle, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        }
        trailing()
    }
}

/** A checklist row: done ✓ · working (spinner) · your OK (radio) · to do. */
@Composable
private fun AssistantStepRow(step: AssistantStep, english: Boolean, reduceMotion: Boolean, detail: String? = null) {
    val large = LocalDensity.current.fontScale >= 1.5f
    val emphasised = step.state == AssistantStepState.Active || step.state == AssistantStepState.YourTurn
    val spoken = when (step.state) {
        AssistantStepState.Done -> "Done"; AssistantStepState.Active -> "In progress"
        AssistantStepState.YourTurn -> "Waiting for you"; AssistantStepState.Pending -> "To do"
    }
    Column(Modifier.semantics(mergeDescendants = true) { stateDescription = spoken }) {
        AssistantRule()
        Row(
            Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(vertical = if (detail == null) 0.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) { AssistantStepMark(step.state, reduceMotion) }
            Column(Modifier.weight(1f)) {
                Text(
                    step.title.resolved(english),
                    style = DNTextStyle.Subheadline.style.copy(fontWeight = if (emphasised) SemiBold else FontWeight.Normal),
                    color = if (step.state == AssistantStepState.Pending) DNColor.textSecondary else DNColor.textPrimary,
                )
                detail?.let { Text(it, style = DNTextStyle.Footnote.style, color = DNColor.statusWarningForeground) }
                // Large text: the amount moves under the title.
                if (large) step.amount?.let { Text(Money.aed(it, trimWhole = true), style = numericStyle.copy(fontWeight = SemiBold), color = DNColor.textSecondary) }
            }
            if (!large) step.amount?.let {
                Text(
                    Money.aed(it, trimWhole = true),
                    style = numericStyle.copy(fontWeight = if (step.state == AssistantStepState.YourTurn) SemiBold else FontWeight.Medium),
                    color = if (step.state == AssistantStepState.YourTurn) DNColor.textPrimary else DNColor.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AssistantStepMark(state: AssistantStepState, reduceMotion: Boolean) {
    Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
        when (state) {
            AssistantStepState.Done -> Box(Modifier.size(22.dp).clip(CircleShape).background(DNColor.actionAccent), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            AssistantStepState.Active -> if (reduceMotion) {
                Box(Modifier.size(22.dp).border(2.2.dp, DNColor.actionAccent, CircleShape))
            } else {
                CircularProgressIndicator(Modifier.size(20.dp), color = DNColor.actionAccent, strokeWidth = 2.2.dp, trackColor = DNColor.blue100)
            }
            AssistantStepState.YourTurn -> Box(
                Modifier.size(22.dp).clip(CircleShape).background(DNColor.actionAccent.copy(alpha = 0.12f)).border(2.2.dp, DNColor.actionAccent, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Box(Modifier.size(8.dp).clip(CircleShape).background(DNColor.actionAccent)) }
            AssistantStepState.Pending -> Box(Modifier.size(22.dp).border(1.5.dp, DNColor.neutral300, CircleShape))
        }
    }
}

/** 0.5 dp hairline between rows. */
@Composable
private fun AssistantRule() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(DNColor.blue950.copy(alpha = 0.09f)))
}

/** Fades a newly added row in once (fields filling in from UAE PASS). */
@Composable
private fun AssistantFadeIn(key: Any, content: @Composable () -> Unit) {
    val a = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { a.animateTo(1f, tween(280)) }
    Box(Modifier.alpha(a.value)) { content() }
}
