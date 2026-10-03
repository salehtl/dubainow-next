package com.salehtl.dubainownext.features.activity

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.PaymentLine
import com.salehtl.dubainownext.data.PaymentMethod
import com.salehtl.dubainownext.data.PaymentPurpose
import com.salehtl.dubainownext.data.PaymentRequest
import com.salehtl.dubainownext.data.ReceiptRow
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNHaptics
import com.salehtl.dubainownext.design.DNMotion
import com.salehtl.dubainownext.design.DNRadius
import com.salehtl.dubainownext.design.DNSpace
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNButtonKind
import com.salehtl.dubainownext.design.components.DNButtonSize
import com.salehtl.dubainownext.design.components.DNRowIcon
import com.salehtl.dubainownext.design.components.HueIcon
import com.salehtl.dubainownext.design.components.MoneyText
import java.math.BigDecimal
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private sealed interface PaymentPhase {
    data object Review : PaymentPhase
    data object Confirming : PaymentPhase
    data class Paid(val receiptId: String) : PaymentPhase
}

/** What the sheet body shows. Decided once per phase, so the review never re-renders as "Nothing to pay" while it fades out. */
private sealed interface PaymentBody {
    data object Review : PaymentBody
    data class NothingToPay(val alreadyPaid: Boolean) : PaymentBody
    data class Receipt(val receiptId: String) : PaymentBody
}

/**
 * The fake payment confirmation used everywhere (`router.present(SheetRoute.Payment(request))`).
 * Check what you're paying → choose how → "Pay AED x" with the fingerprint or face (BiometricPrompt,
 * only when one is enrolled; otherwise a short "Confirming…") → the sheet becomes the receipt: the
 * tick draws in, rows arrive one after another, success haptic. No money moves; the sheet says so.
 */
@Composable
fun PaymentSheet(request: PaymentRequest) {
    val store = LocalStore.current
    val router = LocalRouter.current
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var phase by remember(request.id) { mutableStateOf<PaymentPhase>(PaymentPhase.Review) }
    var method by remember { mutableStateOf(store.state.paymentMethods.firstOrNull() ?: PaymentMethod.GooglePay) }
    var notice by remember { mutableStateOf<String?>(null) }
    // After a biometric lockout the next tries skip the prompt (it would fail straight away again).
    var lockedOut by remember { mutableStateOf(false) }

    val isAlreadyPaid = (request.purpose as? PaymentPurpose.Payables)?.ids
        ?.let { ids -> ids.isNotEmpty() && ids.none { id -> store.payables.any { it.id == id } } } ?: false
    val isNothingToPay = request.lines.isEmpty() || request.total.signum() <= 0 || isAlreadyPaid

    fun complete() {
        if (phase != PaymentPhase.Confirming) return
        val receipt = store.completePayment(request, method)
        if (request.purpose is PaymentPurpose.RequestFees || request.lines.size > 1) {
            store.activityAttachRows(request.lines.map { ReceiptRow(it.title, Money.aed(it.amount)) }, receipt.id)
        }
        DNHaptics.success(view)
        phase = PaymentPhase.Paid(receipt.id)
    }

    fun confirmShortly() {
        scope.launch { delay(900); complete() }
    }

    fun pay() {
        if (phase != PaymentPhase.Review) return
        notice = null
        phase = PaymentPhase.Confirming
        val activity = context.activityFindFragmentActivity()
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK
        val enrolled = BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        // Never block the demo: no biometric enrolled (or locked out) → a short "Confirming…" instead.
        if (activity == null || !enrolled || lockedOut) { confirmShortly(); return }
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(context), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = complete()

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON, BiometricPrompt.ERROR_CANCELED -> {
                        phase = PaymentPhase.Review
                    }
                    BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        DNHaptics.error(view)
                        lockedOut = true
                        phase = PaymentPhase.Review
                        notice = "That didn't match. Try again."
                    }
                    else -> confirmShortly()
                }
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm your payment")
                .setSubtitle("${Money.aed(request.total)} to ${request.activityPayees}")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(authenticators)
                .setConfirmationRequired(false)
                .build(),
        )
    }

    LaunchedEffect(request.id) {
        // `activityDemo pay|fees` pays automatically; `paydone|feesdone|feessent` then close the sheet to show what changed.
        val demo = ActivityDemo.value
        if (demo in setOf("pay", "fees", "paydone", "feesdone", "feessent") && !isNothingToPay && ActivityDemo.once("autopay")) {
            delay(1000); pay()
            if (demo == "paydone" || demo == "feesdone" || demo == "feessent") {
                snapshotFlow { phase }.first { it is PaymentPhase.Paid }
                delay(2500); router.dismissSheet()
            }
        }
    }

    val paid = phase as? PaymentPhase.Paid
    // Paying removes the payables from the store, so "already paid" must only be decided before paying.
    val body: PaymentBody = when {
        paid != null -> PaymentBody.Receipt(paid.receiptId)
        phase == PaymentPhase.Review && isNothingToPay -> PaymentBody.NothingToPay(isAlreadyPaid)
        else -> PaymentBody.Review
    }
    ActivitySheetFrame {
        ActivitySheetTitleRow(
            title = when (body) {
                is PaymentBody.Receipt -> "Receipt"
                is PaymentBody.NothingToPay -> ""
                PaymentBody.Review -> "Check and pay"
            },
            onClose = { router.dismissSheet() },
            closeEnabled = phase != PaymentPhase.Confirming,
            actions = { if (paid != null) ActivityReceiptShareButton(paid.receiptId) },
        )
        AnimatedContent(
            targetState = body,
            contentKey = { it::class },
            transitionSpec = {
                (fadeIn(DNMotion.smooth()) + scaleIn(DNMotion.smooth(), initialScale = 0.96f)) togetherWith fadeOut(DNMotion.base())
            },
            label = "payment-phase",
        ) { shown ->
            when (shown) {
                is PaymentBody.Receipt -> Column(Modifier.fillMaxWidth()) {
                    ActivityReceiptContent(shown.receiptId, Modifier.weight(1f, fill = false), animateIn = true, bottomPadding = 8.dp)
                    Box(Modifier.padding(horizontal = DNSpace.sheetPadding).padding(top = 8.dp, bottom = 8.dp)) {
                        DNButton("Done", onClick = { router.dismissSheet() })
                    }
                }
                is PaymentBody.NothingToPay -> PaymentNothingToPay(shown.alreadyPaid) { router.dismissSheet() }
                PaymentBody.Review -> PaymentReview(
                    request = request,
                    method = method,
                    onMethod = { method = it },
                    // The review keeps its busy look while it fades out into the receipt.
                    confirming = phase != PaymentPhase.Review,
                    notice = notice,
                    onPay = ::pay,
                )
            }
        }
    }
}

// MARK: - Review

@Composable
private fun PaymentReview(
    request: PaymentRequest,
    method: PaymentMethod,
    onMethod: (PaymentMethod) -> Unit,
    confirming: Boolean,
    notice: String?,
    onPay: () -> Unit,
) {
    val payees = request.activityPayees
    val payeeLine = if (request.lines.size == 1) "You're paying $payees" else "${request.lines.size} payments to $payees"
    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = DNSpace.sheetPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(payeeLine, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary, modifier = Modifier.padding(horizontal = 4.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.listGroup)).background(DNColor.surfacePrimary)) {
                    request.lines.forEachIndexed { i, line ->
                        PaymentLineRow(line)
                        if (i < request.lines.lastIndex) Box(Modifier.padding(start = 68.dp).fillMaxWidth().height(0.5.dp).background(DNColor.listRowSeparator))
                    }
                }
            }
            PaymentTotalRow(request.total)
            PaymentPayWith(method, onMethod, enabled = !confirming)
        }
        // The pay bar stays at the bottom of the sheet.
        Column(
            Modifier.fillMaxWidth().padding(horizontal = DNSpace.sheetPadding).padding(top = 12.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedVisibility(notice != null) {
                Text(notice ?: "", style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.Medium), color = DNColor.statusErrorForeground)
            }
            PaymentPayButton(request.total, confirming, onPay)
            Row(
                Modifier.semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = DNColor.textSecondary, modifier = Modifier.size(16.dp))
                Text("Demo · no money moves", style = DNTextStyle.Caption1.style, color = DNColor.textSecondary)
            }
        }
    }
}

/**
 * Who the money goes to ("You're paying Salik"). A bill's line names its entity in `detail`, but a
 * Salik top-up shows the new balance there and a parking extension the new end time, so those name
 * the operator instead (as their receipts do).
 */
private val PaymentRequest.activityPayees: String
    get() = when (purpose) {
        is PaymentPurpose.SalikTopUp -> "Salik"
        is PaymentPurpose.ExtendParking -> "Parkin"
        else -> ActivityFormat.list(lines.map { it.detail })
    }

/** One thing being paid: hue icon · title · detail (who it goes to, or the new balance or end time) · amount. */
@Composable
private fun PaymentLineRow(line: PaymentLine) {
    val large = activityLargeText()
    val texts: @Composable (Modifier) -> Unit = { m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(line.title, style = DNTextStyle.RowTitle.style, color = DNColor.textPrimary)
            Text(line.detail, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        }
    }
    val amount: @Composable () -> Unit = {
        Text(Money.aed(line.amount), style = DNTextStyle.NumericUnit.style, color = DNColor.textPrimary, maxLines = 1)
    }
    val base = Modifier.fillMaxWidth().heightIn(min = 72.dp).semantics(mergeDescendants = true) {}.padding(horizontal = 16.dp, vertical = 12.dp)
    if (large) {
        Column(base, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HueIcon(line.symbol, line.hue, size = 40.dp); texts(Modifier); amount()
        }
    } else {
        Row(base, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HueIcon(line.symbol, line.hue, size = 40.dp); texts(Modifier.weight(1f)); amount()
        }
    }
}

@Composable
private fun PaymentTotalRow(total: BigDecimal) {
    val modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.listGroup)).background(DNColor.surfacePrimary)
        .semantics(mergeDescendants = true) { contentDescription = "Total, ${Money.spoken(total)}" }
        .padding(horizontal = 16.dp, vertical = 14.dp)
    if (activityLargeText()) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Total", style = DNTextStyle.Headline.style, color = DNColor.textPrimary)
            MoneyText(total, size = DNTextStyle.NumericMedium)
        }
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Text("Total", style = DNTextStyle.Headline.style, color = DNColor.textPrimary, modifier = Modifier.weight(1f))
            MoneyText(total, size = DNTextStyle.NumericMedium)
        }
    }
}

/** "Pay with Google Pay · Change" — the methods open in an M3 dropdown menu. */
@Composable
private fun PaymentPayWith(method: PaymentMethod, onMethod: (PaymentMethod) -> Unit, enabled: Boolean) {
    val store = LocalStore.current
    val view = LocalView.current
    val methods = store.state.paymentMethods.ifEmpty { listOf(PaymentMethod.GooglePay) }
    val canChange = methods.size > 1 && enabled
    var open by remember { mutableStateOf(ActivityDemo.value == "paywith" && ActivityDemo.once("paywith")) }
    Box {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(DNRadius.listGroup)).background(DNColor.surfacePrimary)
                .clickable(enabled = canChange, role = Role.Button, onClickLabel = "Choose another way to pay") { open = true }
                .semantics(mergeDescendants = true) { contentDescription = "Pay with ${method.label}" }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DNRowIcon(method.activityIcon, tint = DNColor.textPrimary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Pay with", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
                Text(method.label, style = DNTextStyle.RowTitle.style, color = DNColor.textPrimary)
            }
            if (methods.size > 1) {
                Text(
                    "Change", style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.Medium),
                    color = if (enabled) DNColor.textLink else DNColor.textDisabled,
                )
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = DNColor.surfacePrimary) {
            methods.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m.label, style = DNTextStyle.Body.style) },
                    leadingIcon = { Icon(m.activityIcon, contentDescription = null) },
                    trailingIcon = if (m == method) ({ Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = DNColor.actionAccent) }) else null,
                    onClick = { open = false; if (m != method) { DNHaptics.selection(view); onMethod(m) } },
                )
            }
        }
    }
}

/** The big "Pay AED x" pill with the fingerprint; "Confirming…" with a spinner while it confirms. */
@Composable
private fun PaymentPayButton(total: BigDecimal, confirming: Boolean, onPay: () -> Unit) {
    val label = activityPayLabel(total)
    Box(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(CircleShape).background(DNColor.actionAccent)
            .clickable(enabled = !confirming, role = Role.Button, onClick = onPay)
            .semantics(mergeDescendants = true) {
                contentDescription = if (confirming) "Confirming payment" else "Pay ${Money.spoken(total)}"
                onClick(label = "Confirm with your fingerprint or face") { if (!confirming) onPay(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(confirming, transitionSpec = { fadeIn(DNMotion.fast()) togetherWith fadeOut(DNMotion.fast()) }, label = "pay-label") { busy ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = DNColor.textOnAccent, strokeWidth = 2.5.dp)
                    Text("Confirming…", style = DNTextStyle.Button.style, color = DNColor.textOnAccent)
                } else {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = DNColor.textOnAccent, modifier = Modifier.size(24.dp))
                    Text(label, style = DNTextStyle.Button.style, color = DNColor.textOnAccent)
                }
            }
        }
    }
}

// MARK: - Nothing to pay

@Composable
private fun PaymentNothingToPay(alreadyPaid: Boolean, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = DNSpace.sheetPadding).padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ActivityEmptyState(
            Icons.Rounded.CheckCircle, "Nothing to pay",
            if (alreadyPaid) "This has already been paid. The receipt is in Activity, under History." else "There's nothing due here right now.",
        )
        DNButton("Done", onClick = onDone, kind = DNButtonKind.Secondary, size = DNButtonSize.Medium, fullWidth = false)
    }
}
