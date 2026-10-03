package com.salehtl.dubainownext.features.activity

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.data.Receipt
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.DNLogo
import com.salehtl.dubainownext.design.components.HappinessFacesPicker
import com.salehtl.dubainownext.design.components.MoneyText
import com.salehtl.dubainownext.design.components.TonalIconButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A stored receipt (Paper EII-0): paid tick, amount, rows, PDF, and the Happiness Meter faces.
 * Used by the payment sheet (after paying, with the tick drawing in and rows arriving one after
 * another), the receipt sheet and the pushed receipt.
 */
@Composable
internal fun ActivityReceiptContent(
    receiptId: String,
    modifier: Modifier = Modifier,
    animateIn: Boolean = false,
    bottomPadding: Dp = 24.dp,
) {
    val store = LocalStore.current
    val receipt = store.receipt(receiptId)
    if (receipt == null) {
        ActivityEmptyState(Icons.Rounded.FindInPage, "Receipt not found", "It may belong to another resident, or the demo was reset.", modifier)
        return
    }
    val reduceMotion = activityReduceMotion()
    var revealed by remember(receiptId) { mutableStateOf(!animateIn) }
    LaunchedEffect(receiptId) { revealed = true }
    val details = activityReceiptDetails(receipt)

    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = DNSpace.insetPage).padding(bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ActivityReceiptHero(receipt, animate = animateIn)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.listGroup))) {
            details.forEachIndexed { i, row ->
                Column(Modifier.activityStagger(i, revealed, reduceMotion).background(DNColor.surfacePrimary)) {
                    ActivityReceiptDetailRow(row)
                    if (i < details.lastIndex) Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(0.5.dp).background(DNColor.listRowSeparator))
                }
            }
        }
        ActivityReceiptPdfButton(receipt, Modifier.activityStagger(details.size, revealed, reduceMotion))
        ActivityRatingCard(receipt, Modifier.activityStagger(details.size + 1, revealed, reduceMotion))
    }
}

/** Rows arrive one after another (Paper motion: "receipt rows arrive one after another"). */
@Composable
private fun Modifier.activityStagger(index: Int, revealed: Boolean, reduceMotion: Boolean): Modifier {
    val delay = if (reduceMotion) 0 else 300 + index * DNMotion.STAGGER_MS.toInt() * 2
    val alpha by animateFloatAsState(if (revealed) 1f else 0f, tween(if (reduceMotion) 250 else 320, delay), label = "stagger-alpha")
    val shift by animateFloatAsState(if (revealed || reduceMotion) 0f else 12f, tween(380, delay, FastOutSlowInEasing), label = "stagger-shift")
    return this.graphicsLayer { this.alpha = alpha; translationY = shift * density }
}

internal enum class ActivityRowStyle { Plain, Amount, Reference }
internal data class ActivityDetailRow(val label: String, val value: String, val style: ActivityRowStyle)

private fun activityReceiptDetails(r: Receipt): List<ActivityDetailRow> = buildList {
    add(ActivityDetailRow("When", ActivityFormat.whenLong(r.date), ActivityRowStyle.Plain))
    r.method?.let { add(ActivityDetailRow("Paid with", it.label, ActivityRowStyle.Plain)) }
    r.rows.forEach { add(ActivityDetailRow(it.label, it.value, if (it.value.startsWith("AED")) ActivityRowStyle.Amount else ActivityRowStyle.Plain)) }
    add(ActivityDetailRow("Reference", r.reference, ActivityRowStyle.Reference))
}

// MARK: - Hero

@Composable
private fun ActivityReceiptHero(receipt: Receipt, animate: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActivityPaidTick(animate)
        Text(
            if (receipt.amount == null) "Sent" else "Paid",
            style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.statusSuccessForeground,
        )
        receipt.amount?.let { MoneyText(it, size = DNTextStyle.NumericHero, modifier = Modifier.padding(top = 2.dp)) }
        Text(
            receipt.title, style = DNTextStyle.Headline.style, color = DNColor.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (receipt.doneByAssistant) {
            Row(
                Modifier.padding(top = 4.dp).heightIn(min = 30.dp).clip(CircleShape).background(DNColor.surfacePrimary).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DNLogo(height = 16.dp, modifier = Modifier.clearAndSetSemantics { })
                Text("Done with your assistant", style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
            }
        }
    }
}

/** The success disc; the tick draws in over 400 ms (instant when animations are off). */
@Composable
internal fun ActivityPaidTick(animate: Boolean, size: Dp = 56.dp) {
    val reduceMotion = activityReduceMotion()
    val progress = remember { Animatable(if (animate && !reduceMotion) 0f else 1f) }
    val pop = remember { Animatable(if (animate && !reduceMotion) 0.6f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            launch { pop.animateTo(1f, DNMotion.bouncy()) }
            delay(80)
            progress.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        }
    }
    val color = DNColor.statusSuccessForeground
    Canvas(Modifier.size(size).scale(pop.value).clearAndSetSemantics { }) {
        drawCircle(DNColor.statusSuccessSurface)
        // Paper's tick: M5 12.6 l4.2 4.2 9-9.5 on a 24 grid, in the middle half of the disc.
        val box = this.size.width * 0.5f
        val o = (this.size.width - box) / 2f
        val s = box / 24f
        val tick = Path().apply {
            moveTo(o + 5f * s, o + 12.6f * s); lineTo(o + 9.2f * s, o + 16.8f * s); lineTo(o + 18.2f * s, o + 7.3f * s)
        }
        val measure = PathMeasure().apply { setPath(tick, false) }
        val part = Path()
        measure.getSegment(0f, measure.length * progress.value, part, true)
        drawPath(part, color, style = Stroke(width = this.size.width * 0.055f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// MARK: - Rows

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActivityReceiptDetailRow(row: ActivityDetailRow) {
    val context = LocalContext.current
    val toasts = LocalToasts.current
    val valueStyle = when (row.style) {
        ActivityRowStyle.Plain -> DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold)
        ActivityRowStyle.Amount -> DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
        ActivityRowStyle.Reference -> DNTextStyle.Footnote.style.copy(fontFamily = FontFamily.Monospace)
    }
    val base = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        .let {
            if (row.style == ActivityRowStyle.Reference) it.combinedClickable(
                onClick = {}, onLongClickLabel = "Copy reference",
                onLongClick = { if (ActivityExport.copy(context, "Reference", row.value)) toasts.show("Reference copied") },
            ) else it
        }
        .semantics(mergeDescendants = true) {
            if (row.style == ActivityRowStyle.Reference) contentDescription = "${row.label}, ${ActivityFormat.spelled(row.value)}"
        }
        .padding(horizontal = 16.dp, vertical = 12.dp)
    if (activityLargeText()) {
        Column(base, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.label, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
            Text(row.value, style = valueStyle, color = DNColor.textPrimary)
        }
    } else {
        Row(base, verticalAlignment = Alignment.CenterVertically) {
            Text(row.label, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary, modifier = Modifier.weight(1f).padding(end = 16.dp))
            Text(row.value, style = valueStyle, color = DNColor.textPrimary, textAlign = TextAlign.End, modifier = Modifier.widthIn(max = 230.dp))
        }
    }
}

// MARK: - PDF and share

/** "Download receipt (PDF)" — a white pill. Saves the PDF to Downloads, then offers to share it. */
@Composable
private fun ActivityReceiptPdfButton(receipt: Receipt, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val toasts = LocalToasts.current
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    fun download() {
        if (working) return
        if (!ActivityExport.canSavePdf) {
            ActivityExport.shareText(context, ActivityExport.summary(receipt), receipt.title, "Share receipt")
            return
        }
        working = true
        scope.launch {
            val uri = withContext(Dispatchers.IO) { ActivityExport.receiptPdf(context, receipt) }
            working = false
            if (uri != null) {
                toasts.show("Receipt saved to Downloads")
                ActivityExport.sharePdf(context, uri, receipt.title)
            } else {
                ActivityExport.shareText(context, ActivityExport.summary(receipt), receipt.title, "Share receipt")
            }
        }
    }
    LaunchedEffect(receipt.id) {
        if (ActivityDemo.take("pdf")) { delay(1200); download() }
    }
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).clip(CircleShape).background(DNColor.surfacePrimary)
            .clickable(enabled = !working, role = Role.Button, onClick = ::download)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(working, transitionSpec = { fadeIn(DNMotion.fast()) togetherWith fadeOut(DNMotion.fast()) }, label = "pdf") { busy ->
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = DNColor.textLink, strokeWidth = 2.dp)
            else Icon(Icons.Rounded.Download, contentDescription = null, tint = DNColor.textLink, modifier = Modifier.size(20.dp))
        }
        Text("Download receipt (PDF)", style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textLink)
    }
}

/** The share button in a receipt's title row or header: the receipt as text. */
@Composable
internal fun ActivityReceiptShareButton(receiptId: String) {
    val store = LocalStore.current
    val context = LocalContext.current
    val receipt = store.receipt(receiptId) ?: return
    TonalIconButton(Icons.Rounded.Share, "Share receipt", onClick = {
        ActivityExport.shareText(context, ActivityExport.summary(receipt), receipt.title, "Share receipt")
    })
}

// MARK: - Happiness Meter

/** "How was topping up Salik?" — one tap rates; it goes to the entity and the Happiness Meter. */
@Composable
private fun ActivityRatingCard(receipt: Receipt, modifier: Modifier = Modifier) {
    val store = LocalStore.current
    val rating = store.receipt(receipt.id)?.rating
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.listGroup)).background(DNColor.surfacePrimary)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                receipt.activityRatingQuestion, style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold),
                color = DNColor.textPrimary, modifier = Modifier.semantics { heading() },
            )
            AnimatedContent(rating == null, transitionSpec = { fadeIn(DNMotion.base()) togetherWith fadeOut(DNMotion.base()) }, label = "rating-note") { unrated ->
                Text(
                    if (unrated) "One tap. It goes to ${receipt.activityRatingRecipient} and the Happiness Meter."
                    else "Thanks. Sent to ${receipt.activityRatingRecipient} and the Happiness Meter.",
                    style = DNTextStyle.Footnote.style, color = DNColor.textSecondary,
                )
            }
        }
        HappinessFacesPicker(selection = rating, onSelect = { face -> store.rate(receipt.id, face) })
    }
}

// MARK: - Sheet title row

/**
 * Sheets have no logo or back: a title at the start and the close button at the end (M3 sheet),
 * with any extra actions (share) before it.
 */
@Composable
internal fun ActivitySheetTitleRow(
    title: String,
    onClose: () -> Unit,
    closeEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(start = DNSpace.sheetPadding, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title, style = DNTextStyle.Title3.style, color = DNColor.textPrimary,
            modifier = Modifier.weight(1f).then(if (title.isNotEmpty()) Modifier.semantics { heading() } else Modifier),
        )
        actions()
        TonalIconButton(
            Icons.Rounded.Close, "Close", onClick = { if (closeEnabled) onClose() },
            tint = if (closeEnabled) DNColor.textPrimary else DNColor.textDisabled,
        )
    }
}

/**
 * Keeps a tall sheet's top (and its drag handle) below the status bar, so a full-height sheet
 * still reads as a sheet.
 */
@Composable
internal fun ActivitySheetFrame(content: @Composable ColumnScope.() -> Unit) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cap = if (maxHeight == Dp.Infinity) Dp.Unspecified else maxHeight - topInset - 16.dp
        Column(Modifier.fillMaxWidth().heightIn(max = cap), content = content)
    }
}
