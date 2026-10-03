package com.salehtl.dubainownext.features.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.app.LocalRouter
import com.salehtl.dubainownext.app.LocalStore
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.Money
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
import com.salehtl.dubainownext.design.components.DNButtonSize
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Top up Salik — the Salik widget's quick action. Balance, amount chips (AED 50, 100, 200, Other),
 * the new balance, then the fake payment confirmation applies it.
 */
@Composable
fun TopUpSalikSheet() {
    val store = LocalStore.current
    val router = LocalRouter.current
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()) {
        WidgetSheetHeader("Top up Salik") { router.dismissSheet() }
        if (store.widgetSalikLinked) TopUpSalikForm() else TopUpSalikNotLinked()
    }
}

private val SALIK_PRESETS = listOf(50, 100, 200)
private val SALIK_MIN = BigDecimal(50)
private val SALIK_MAX = BigDecimal(2_000)

/** null = Other */
private fun salikParse(text: String): BigDecimal? = text.replace(",", "").trim().toBigDecimalOrNull()

@Composable
private fun TopUpSalikForm() {
    val store = LocalStore.current
    val router = LocalRouter.current
    val view = LocalView.current
    val focus = LocalFocusManager.current
    // `widgetsSalikOther 30` opens with Other chosen and that amount typed in (screenshots).
    val debugOther = LaunchOptions.extras["widgetsSalikOther"]
    var preset by rememberSaveable { mutableStateOf<Int?>(if (debugOther != null) null else 100) }
    var otherText by rememberSaveable { mutableStateOf(debugOther ?: "") }
    val otherRequester = remember { FocusRequester() }

    val amount: BigDecimal? = preset?.let { BigDecimal(it) } ?: salikParse(otherText)?.takeIf { it in SALIK_MIN..SALIK_MAX }?.setScale(2, RoundingMode.HALF_UP)
    val otherHint: String? = if (preset != null) null else {
        val d = salikParse(otherText)
        when {
            d == null || d < SALIK_MIN -> "Top-ups start at AED 50"
            d > SALIK_MAX -> "You can top up up to AED 2,000 at a time"
            else -> null
        }
    }
    val otherError = preset == null && otherText.isNotBlank() && otherHint != null
    val balance = store.state.salikBalance
    val nowModel = WidgetSalikModel(balance)
    val after = amount?.let { WidgetSalikModel(balance + it) }
    val stacked = LocalDensity.current.fontScale > 1.3f

    Column(
        Modifier.fillMaxWidth().padding(start = DNSpace.insetPage, end = DNSpace.insetPage, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        // Balance, drawn like the gantry widget.
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(DNRadius.card)).background(DNColor.widgetSurfaceInverse).padding(16.dp)
                .clearAndSetSemantics { contentDescription = "Your Salik balance, ${Money.spoken(balance)}. ${nowModel.tripsLine.replace(" · ", ", ")}." },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WidgetGantryGlyph(ink = DNColor.widgetOnInverse, fill = DNColor.widgetOnInverseRule)
                Text("Your Salik balance", style = DNTextStyle.Headline.style, color = DNColor.widgetOnInverse)
            }
            val figure: @Composable () -> Unit = {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("AED", style = DNTextStyle.NumericUnit.style, color = DNColor.widgetOnInverseSecondary, modifier = Modifier.alignByBaseline())
                    WidgetRollingText(Money.figure(balance, trimWhole = true), DNTextStyle.NumericLarge.style, DNColor.widgetOnInverse, balance.toDouble(), Modifier.alignByBaseline())
                }
            }
            val trips: @Composable () -> Unit = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (nowModel.isLow) DNColor.signLow else DNColor.signLive))
                    Text(
                        nowModel.tripsLine, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold),
                        color = if (nowModel.isLow) DNColor.signLow else DNColor.widgetOnInverseSecondary,
                    )
                }
            }
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { figure(); trips() }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    figure(); Spacer(Modifier.weight(1f)); trips()
                }
            }
        }

        // Amount
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WidgetOverline("Amount")
            val choices: List<Pair<String, Int?>> = SALIK_PRESETS.map { "AED $it" to it } + ("Other" to null)
            val rows = if (stacked) choices.chunked(2) else listOf(choices)
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DNSpace.chipGap)) {
                    row.forEach { (label, value) ->
                        val isSelected = preset == value
                        WidgetChoiceChip(
                            label = label,
                            selected = isSelected,
                            onClick = {
                                DNHaptics.selection(view)
                                preset = value
                                if (value != null) focus.clearFocus()
                            },
                            modifier = Modifier.weight(1f).semantics {
                                contentDescription = if (value == null) "Other amount" else label
                                selected = isSelected
                            },
                        )
                    }
                }
            }
            AnimatedVisibility(preset == null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                LaunchedEffect(Unit) { if (debugOther == null) otherRequester.requestFocus() }
                WidgetFieldSafeTypography {
                OutlinedTextField(
                    value = otherText,
                    onValueChange = { t -> otherText = t.filter { it.isDigit() || it == '.' || it == ',' }.take(9) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).focusRequester(otherRequester),
                    label = { Text("Amount") },
                    prefix = { Text("AED ", style = DNTextStyle.NumericUnit.style, color = DNColor.textSecondary) },
                    textStyle = DNTextStyle.NumericUnit.style.copy(color = DNColor.fieldText),
                    singleLine = true,
                    isError = otherError,
                    supportingText = otherHint?.let { h -> { Text(h) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DNColor.surfacePrimary, unfocusedContainerColor = DNColor.surfacePrimary,
                        errorContainerColor = DNColor.surfacePrimary,
                        focusedBorderColor = DNColor.fieldFocus, unfocusedBorderColor = DNColor.borderControl,
                        errorBorderColor = DNColor.fieldError, errorSupportingTextColor = DNColor.fieldError,
                        unfocusedSupportingTextColor = DNColor.fieldHint, focusedSupportingTextColor = DNColor.fieldHint,
                    ),
                )
                }
            }
        }

        // New balance
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("New balance", style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary)
                if (after != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("AED", style = DNTextStyle.NumericUnit.style, color = DNColor.textPrimary, modifier = Modifier.alignByBaseline())
                        WidgetRollingText(Money.figure(after.balance), DNTextStyle.NumericLarge.style, DNColor.textPrimary, after.balance.toDouble(), Modifier.alignByBaseline())
                    }
                } else {
                    Text("–", style = DNTextStyle.NumericLarge.style, color = DNColor.textSecondary)
                }
            }
            Spacer(Modifier.weight(1f))
            if (after != null) {
                Text(after.tripsLine, style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SalikPayButton(
                text = amount?.let { "Pay ${Money.aed(it, trimWhole = true)}" } ?: "Pay",
                enabled = amount != null,
                onClick = {
                    val a = amount ?: return@SalikPayButton
                    focus.clearFocus()
                    val request = PaymentRequest(
                        id = "salik-topup-${System.currentTimeMillis() / 1000}",
                        title = "Top up Salik",
                        lines = listOf(
                            PaymentLine(
                                title = "Salik top-up",
                                detail = "New balance ${Money.aed(balance + a, trimWhole = true)}",
                                amount = a,
                                symbol = "road.lanes",
                                hue = Hue.Sand,
                            )
                        ),
                        purpose = PaymentPurpose.SalikTopUp(a),
                    )
                    router.present(SheetRoute.Payment(request))
                },
            )
            Text(
                "Run by Salik · your new balance shows straight away", style = DNTextStyle.Footnote.style,
                color = DNColor.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * The primary DNButton, except that disabled it stays visible: the shared disabled fill
 * (`actionDisabled`) is the same grey as the sheet, so only its label showed (shared request in NOTES.md).
 * Disabled here uses the M3 look: on-surface at 10 % behind a disabled label.
 */
@Composable
private fun SalikPayButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    if (enabled) {
        DNButton(text, onClick = onClick)
    } else {
        Box(
            Modifier.fillMaxWidth().heightIn(min = DNButtonSize.Large.height).clip(CircleShape)
                .background(DNColor.textPrimary.copy(alpha = 0.10f))
                .semantics(mergeDescendants = true) { role = Role.Button; disabled() }
                .padding(horizontal = DNButtonSize.Large.inset),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text, style = DNTextStyle.Button.style.copy(fontWeight = FontWeight.SemiBold),
                color = DNColor.textDisabled, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TopUpSalikNotLinked() {
    val router = LocalRouter.current
    Column(
        Modifier.fillMaxWidth().padding(start = DNSpace.insetPage, end = DNSpace.insetPage, top = 12.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        WidgetGantryGlyph(ink = DNColor.widgetOnInverse, fill = DNColor.widgetSurfaceInverse, size = 56.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Link your Salik account", style = DNTextStyle.Title3.style, color = DNColor.textPrimary, textAlign = TextAlign.Center)
            Text(
                "Once it's linked, your balance shows on Home and you can top up in two taps.",
                style = DNTextStyle.Body.style, color = DNColor.textSecondary, textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        DNButton("Link Salik", onClick = {
            router.present(
                SheetRoute.ComingSoonSheet(
                    ComingSoon(
                        title = "Link your Salik account",
                        detail = "In the full app you confirm the link once with UAE PASS.",
                        owner = "Salik", symbol = widgetSymbol("link", "road.lanes"), hue = Hue.Sand, kind = ComingSoon.Kind.NotInDemo,
                    )
                )
            )
        })
    }
}
