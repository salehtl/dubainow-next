package com.salehtl.dubainownext.features.activity

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.LocalToasts
import com.salehtl.dubainownext.app.PushedHeader
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.data.RequestStep
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.ComingSoon
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNButtonKind
import com.salehtl.dubainownext.design.components.DNButtonSize
import com.salehtl.dubainownext.design.components.DNListGroup
import com.salehtl.dubainownext.design.components.DNListRow
import com.salehtl.dubainownext.design.components.DNRowDivider
import com.salehtl.dubainownext.design.components.DNRowIcon
import com.salehtl.dubainownext.design.components.StatusChip
import com.salehtl.dubainownext.design.components.TonalIconButton
import com.salehtl.dubainownext.design.components.dnCanvasBackground
import com.salehtl.dubainownext.design.components.dnCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Request tracker (Paper EE3-0): status chip, title, copyable reference, what you need to know,
 * the steps (done ✓, current numbered, upcoming outlined, the outcome with a star), and the
 * current step's actions: scan or upload a passport, or pay the fees.
 */
@Composable
fun RequestTrackerScreen(requestId: String) {
    val store = LocalStore.current
    val router = LocalRouter.current
    val toasts = LocalToasts.current
    val context = LocalContext.current
    val request = store.request(requestId)
    val capture = rememberActivityCaptureState()
    var showsCancel by remember { mutableStateOf(false) }
    var showsSent by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    fun copyReference(r: RequestItem) {
        if (ActivityExport.copy(context, "Reference", r.reference)) toasts.show("Reference copied")
    }

    fun comingSoon(r: RequestItem, title: String, detail: String) {
        router.present(SheetRoute.ComingSoonSheet(ComingSoon(title, detail, r.entity, r.symbol, r.hue, ComingSoon.Kind.NotInDemo)))
    }

    LaunchedEffect(requestId) {
        val r = store.request(requestId) ?: return@LaunchedEffect
        when {
            ActivityDemo.take("scan", "shoot", "send") -> { delay(600); capture.scan = requestId }
            ActivityDemo.take("uploaded", "fees", "feesdone", "feessent") -> {
                if (r.activityCurrentStep?.activityKind == ActivityStepKind.Upload) store.advanceRequest(requestId)
                if (ActivityDemo.value != "uploaded") {
                    delay(600)
                    store.request(requestId)?.let { router.present(SheetRoute.Payment(it.activityFeesRequest)) }
                }
                if (ActivityDemo.value == "feessent") {
                    // After the fees are paid and the sheet closes: what GDRFA now has.
                    snapshotFlow { router.sheet }.first { it == null }
                    delay(600); showsSent = true
                }
            }
            ActivityDemo.take("upload") -> { delay(500); capture.upload = requestId }
            ActivityDemo.take("sent") -> { delay(500); showsSent = true }
            ActivityDemo.take("cancel") -> { delay(500); showsCancel = true }
            ActivityDemo.take("menu") -> { delay(500); menu = true }
        }
    }

    Box(Modifier.fillMaxSize().dnCanvasBackground()) {
        if (request != null) {
            Box(Modifier.fillMaxWidth().height(360.dp).background(Brush.verticalGradient(listOf(request.hue.start, DNColor.surfaceSecondary.copy(alpha = 0f)))))
        }
        Column(Modifier.fillMaxSize()) {
            PushedHeader(actions = {
                if (request != null) {
                    Box {
                        TonalIconButton(Icons.Rounded.MoreVert, "More", onClick = { menu = true })
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = DNColor.surfacePrimary) {
                            DropdownMenuItem(
                                text = { Text("Copy reference", style = DNTextStyle.Body.style) },
                                leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                                onClick = { menu = false; copyReference(request) },
                            )
                            DropdownMenuItem(
                                text = { Text("Share status", style = DNTextStyle.Body.style) },
                                leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    ActivityExport.shareText(
                                        context, "${request.title} (${request.entity})\n${request.activityStatus().text}\nReference ${request.reference}",
                                        request.title, "Share status",
                                    )
                                },
                            )
                            if (request.serviceID == "renew-residence-visa") {
                                DropdownMenuItem(
                                    text = { Text("Ask your assistant", style = DNTextStyle.Body.style) },
                                    leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                                    onClick = { menu = false; router.openAssistant("When will my visa renewal be done?") },
                                )
                            }
                            if (request.activityCanCancel) {
                                HorizontalDivider(color = DNColor.borderSubtle)
                                DropdownMenuItem(
                                    text = { Text("Cancel this application", style = DNTextStyle.Body.style, color = DNColor.actionDestructive) },
                                    leadingIcon = { Icon(Icons.Rounded.Cancel, contentDescription = null, tint = DNColor.actionDestructive) },
                                    onClick = { menu = false; showsCancel = true },
                                )
                            }
                        }
                    }
                }
            })
            if (request == null) {
                ActivityEmptyState(Icons.Rounded.FindInPage, "Request not found", "It may belong to another resident, or the demo was reset.")
            } else {
                ActivityTrackerContent(
                    request,
                    onCopy = { copyReference(request) },
                    onScan = { capture.scan = request.id },
                    onUpload = { capture.upload = request.id },
                    onPay = { router.present(SheetRoute.Payment(request.activityFeesRequest)) },
                    onSent = { showsSent = true },
                    onHelp = {
                        comingSoon(request, "Get help with this application", "Chat, call-backs and help from ${request.entity} come in a later phase of the demo.")
                    },
                    onCancel = { showsCancel = true },
                )
            }
        }
    }

    ActivityCaptureHost(capture)

    if (showsCancel && request != null) {
        AlertDialog(
            onDismissRequest = { showsCancel = false },
            containerColor = DNColor.surfacePrimary,
            shape = RoundedCornerShape(DNRadius.dialog),
            title = { Text("Cancel this application?", style = DNTextStyle.Title2.style, color = DNColor.textPrimary) },
            text = { Text("Any fees you've paid may not be refunded.", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary) },
            dismissButton = {
                TextButton(onClick = { showsCancel = false }) {
                    Text("Keep it", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textLink)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showsCancel = false
                    comingSoon(request, "Cancel this application", "Cancelling an application with ${request.entity} isn't part of this demo. Nothing has been cancelled.")
                }) {
                    Text("Cancel application", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.actionDestructive)
                }
            },
        )
    }

    if (showsSent && request != null) ActivitySentSheet(request) { showsSent = false }
}

@Composable
private fun ActivityTrackerContent(
    r: RequestItem,
    onCopy: () -> Unit,
    onScan: () -> Unit,
    onUpload: () -> Unit,
    onPay: () -> Unit,
    onSent: () -> Unit,
    onHelp: () -> Unit,
    onCancel: () -> Unit,
) {
    val store = LocalStore.current
    Column(
        // animateContentSize clips, so it goes outside the page inset: the current step's halo reaches past it.
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).animateContentSize()
            .padding(horizontal = DNSpace.insetPage).padding(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        // Header: status, title, reference
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val status = r.activityStatus(chip = true)
            StatusChip(status.text, tone = status.tone, onWhite = true)
            Text(r.title, style = DNTextStyle.Title1.style, color = DNColor.textPrimary, modifier = Modifier.semantics { heading() })
            Row(
                Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClickLabel = "Copy the reference", onClick = onCopy)
                    .clearAndSetSemantics { contentDescription = "Reference ${ActivityFormat.spelled(r.reference)}" },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Reference", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                Text(r.reference, style = DNTextStyle.Footnote.style.copy(fontFamily = FontFamily.Monospace), color = DNColor.textPrimary)
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = DNColor.textLink, modifier = Modifier.size(16.dp))
            }
        }

        r.notice?.let { (title, body) ->
            Column(
                Modifier.fillMaxWidth().dnCard().padding(16.dp).semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(title, style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
                Text(body, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
            }
        }

        // Steps
        Column(Modifier.padding(top = 4.dp)) {
            r.steps.forEachIndexed { index, step ->
                ActivityStepRow(index + 1, step, store.activityStepDetail(r, step), currentDetail(r, step)) {
                    if (step.state == RequestStep.State.Current) ActivityStepActions(r, step, onScan, onUpload, onPay)
                }
            }
            val (title, detail) = r.activityOutcome
            ActivityOutcomeRow(title, detail, achieved = r.status == RequestItem.Status.Done)
        }

        // Links
        DNListGroup {
            DNListRow("What you've sent", onClick = onSent)
            DNRowDivider()
            DNListRow("Get help with this application", onClick = onHelp)
            if (r.activityCanCancel) {
                DNRowDivider()
                DNListRow("Cancel this application", showsChevron = false, titleColor = DNColor.actionDestructive, onClick = onCancel)
            }
        }
    }
}

/**
 * What a step says once it's done: an upload that was "Your turn" is now being checked, and a fee
 * step that only had an estimate shows what was paid (from the receipt).
 */
internal fun DemoStore.activityStepDetail(r: RequestItem, step: RequestStep): String {
    if (step.state != RequestStep.State.Done) return step.detail
    if (step.detail.startsWith("Your turn")) return if (step.activityKind == ActivityStepKind.Upload) "Sent · being checked" else "Done"
    if (step.activityKind == ActivityStepKind.Pay && step.detail.startsWith("About")) {
        val paid = state.history.firstOrNull { it.serviceID == r.serviceID && it.title.startsWith("Paid the fees") }
        val amount = paid?.amount ?: return "Paid"
        return "${DemoDate.short(paid.date)} · ${Money.aed(amount)}"
    }
    return step.detail
}

private fun currentDetail(r: RequestItem, step: RequestStep): String? {
    if (step.state != RequestStep.State.Current) return null
    return when (step.activityKind) {
        ActivityStepKind.Upload -> (r.status as? RequestItem.Status.YourTurn)?.let { "Your turn · by ${DemoDate.long(it.due)}" } ?: "Your turn"
        ActivityStepKind.Pay -> "Your turn · the exact amount is ${Money.aed(r.activityFeesRequest.total)}"
        ActivityStepKind.Waiting -> null
    }
}

@Composable
private fun ActivityStepActions(r: RequestItem, step: RequestStep, onScan: () -> Unit, onUpload: () -> Unit, onPay: () -> Unit) {
    when (step.activityKind) {
        ActivityStepKind.Upload -> if (activityLargeText()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DNButton("Scan passport", onScan, size = DNButtonSize.Medium, icon = Icons.Rounded.PhotoCamera)
                DNButton("Upload file", onUpload, kind = DNButtonKind.Secondary, size = DNButtonSize.Medium)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DNButton("Scan passport", onScan, modifier = Modifier.weight(1f), size = DNButtonSize.Medium, icon = Icons.Rounded.PhotoCamera)
                DNButton("Upload file", onUpload, kind = DNButtonKind.Secondary, size = DNButtonSize.Medium, fullWidth = false)
            }
        }
        ActivityStepKind.Pay -> DNButton(
            "Pay ${Money.aed(r.activityFeesRequest.total)}", onPay, size = DNButtonSize.Medium, fullWidth = false, icon = Icons.Rounded.CreditCard,
        )
        ActivityStepKind.Waiting -> Unit
    }
}

/** One step on the vertical line: a 28 dp marker, the connector below it, title, detail, actions. */
@Composable
private fun ActivityStepRow(number: Int, step: RequestStep, display: String, currentDetail: String?, actions: @Composable () -> Unit) {
    val stateLabel = when (step.state) {
        RequestStep.State.Done -> "done"; RequestStep.State.Current -> "current"; RequestStep.State.Upcoming -> "to do"
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.width(28.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            ActivityStepMarker(number, step.state)
            Box(
                Modifier.width(2.dp).weight(1f)
                    .background(if (step.state == RequestStep.State.Done) DNColor.statusSuccessForeground else DNColor.neutral300),
            )
        }
        Column(Modifier.weight(1f).padding(top = 3.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(
                Modifier.clearAndSetSemantics { contentDescription = "Step $number, $stateLabel. ${step.title}. ${currentDetail ?: display}" },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    step.title, color = DNColor.textPrimary,
                    style = DNTextStyle.RowTitle.style.copy(fontWeight = if (step.state == RequestStep.State.Current) FontWeight.Bold else FontWeight.SemiBold),
                )
                if (currentDetail != null) {
                    Text(currentDetail, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.statusWarningForeground)
                } else {
                    Text(display, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                }
            }
            actions()
        }
    }
}

@Composable
private fun ActivityStepMarker(number: Int, state: RequestStep.State) {
    when (state) {
        RequestStep.State.Done -> Box(
            Modifier.size(28.dp).clip(CircleShape).background(DNColor.statusSuccessForeground),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Check, contentDescription = null, tint = DNColor.white, modifier = Modifier.size(16.dp)) }
        RequestStep.State.Current -> Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            // The halo draws 5 dp outside the 28 dp disc without changing the layout (requiredSize: a plain size would be capped at 28 dp).
            Box(
                Modifier.requiredSize(38.dp).clip(CircleShape).background(DNColor.signSun.copy(alpha = 0.3f)),
            )
            Box(Modifier.size(28.dp).clip(CircleShape).background(DNColor.statusWarningForeground), contentAlignment = Alignment.Center) {
                Text("$number", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Bold), color = DNColor.white)
            }
        }
        RequestStep.State.Upcoming -> Box(
            Modifier.size(28.dp).clip(CircleShape).background(DNColor.surfacePrimary).border(1.5.dp, DNColor.neutral300, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("$number", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Bold), color = DNColor.textPrimary) }
    }
}

/** The last row: what you get at the end, marked with a star. */
@Composable
private fun ActivityOutcomeRow(title: String, detail: String, achieved: Boolean) {
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "At the end: $title. $detail" },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(if (achieved) DNColor.statusSuccessForeground else DNColor.surfacePrimary)
                .border(1.5.dp, if (achieved) Color.Transparent else DNColor.neutral300, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (achieved) Icons.Rounded.Star else Icons.Rounded.StarOutline, contentDescription = null,
                tint = if (achieved) DNColor.white else DNColor.textSecondary, modifier = Modifier.size(16.dp),
            )
        }
        Column(Modifier.weight(1f).padding(top = 3.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
            Text(detail, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
        }
    }
}

// MARK: - What you've sent

/** What the resident has already sent for this request (from the steps that are done). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivitySentSheet(request: RequestItem, onDismiss: () -> Unit) {
    val store = LocalStore.current
    val items = request.steps.filter { it.state == RequestStep.State.Done }.map { step ->
        val t = step.title.lowercase()
        val full = store.activityStepDetail(request, step)
        val detail = full.substringBefore(" · ")
        when {
            "medical" in t -> Triple("Medical fitness result", "Shared by DHA · $detail", "cross.case.fill")
            "insurance" in t -> Triple("Health insurance policy", "Found automatically · $detail", "shield.fill")
            "passport" in t -> Triple("Passport copy", "Photo page · sent ${DemoDate.relative(java.time.LocalDateTime.now()).lowercase()}", "person.text.rectangle.fill")
            "eye" in t -> Triple("Eye test result", "Shared by the optician · $detail", "eye.fill")
            t.startsWith("pay") -> Triple("Payment", full, "creditcard.fill")
            "tell" in t -> Triple("What happened", "Your description · $detail", "text.bubble.fill")
            else -> Triple(step.title, full, "doc.fill")
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DNColor.surfaceSecondary,
        shape = RoundedCornerShape(topStart = DNRadius.sheet, topEnd = DNRadius.sheet),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            ActivitySheetTitleRow("What you've sent", onClose = onDismiss)
            Column(Modifier.padding(horizontal = DNSpace.sheetPadding)) {
                if (items.isEmpty()) {
                    ActivityEmptyState(Icons.Rounded.Inbox, "Nothing sent yet", "What you send for this application shows here.")
                } else {
                    DNListGroup(footer = "${request.entity} has these. Reference ${request.reference}.") {
                        items.forEachIndexed { i, (title, detail, symbol) ->
                            DNListRow(title, subtitle = detail, showsChevron = false, leading = { DNRowIcon(activitySentIcon(symbol), tint = DNColor.textPrimary) })
                            if (i < items.lastIndex) DNRowDivider(inset = 56.dp)
                        }
                    }
                }
            }
        }
    }
}

/** Symbols the shared icon map doesn't cover yet (see NOTES.md). */
private fun activitySentIcon(symbol: String) = when (symbol) {
    "eye.fill" -> Icons.Rounded.Visibility
    "text.bubble.fill" -> Icons.AutoMirrored.Rounded.Chat
    "doc.fill" -> Icons.Rounded.Description
    else -> com.salehtl.dubainownext.design.DNIcons.forSymbol(symbol)
}
