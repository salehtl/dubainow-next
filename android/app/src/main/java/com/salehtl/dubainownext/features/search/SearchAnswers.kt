package com.salehtl.dubainownext.features.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DocumentItem
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.ParkingSession
import com.salehtl.dubainownext.data.Payable
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.data.RequestStep
import com.salehtl.dubainownext.data.personas.PrayerTimes
import com.salehtl.dubainownext.design.DNColor
import com.salehtl.dubainownext.design.DNTextStyle
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.ChipTone
import com.salehtl.dubainownext.design.components.DNButton
import com.salehtl.dubainownext.design.components.DNButtonKind
import com.salehtl.dubainownext.design.components.DNButtonSize
import com.salehtl.dubainownext.design.components.DNTextButton
import com.salehtl.dubainownext.design.components.HueIcon
import com.salehtl.dubainownext.design.components.MoneyText
import com.salehtl.dubainownext.design.components.SegmentedProgress
import com.salehtl.dubainownext.design.components.StatusChip
import com.salehtl.dubainownext.design.components.dnCard
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.LocalDateTime

// Direct answers (Paper "Search · Direct answer"): worked out from the resident's own linked
// accounts, always saying where the numbers come from and when they were updated.

/** The white answer card: radius 28, 20 dp padding, 18 dp between parts. */
@Composable
internal fun SearchAnswerCard(
    source: String,
    sourceSymbol: String = "lock.fill",
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth().dnCard(28.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        val sourceLine: @Composable (Modifier) -> Unit = { mod ->
            Row(mod, verticalAlignment = Alignment.CenterVertically) {
                Icon(searchIcon(sourceSymbol), contentDescription = null, tint = DNColor.textSecondary, modifier = Modifier.size(13.dp))
                Text(source.uppercase(), style = DNTextStyle.Overline.style, color = DNColor.textSecondary, modifier = Modifier.padding(start = 6.dp))
            }
        }
        if (searchLargeText()) {
            // Large text: the source and "Updated 18:06" stack instead of squeezing each other.
            Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                sourceLine(Modifier)
                if (trailing != null) Text(trailing, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
            }
        } else {
            Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                sourceLine(Modifier.weight(1f))
                if (trailing != null) Text(trailing, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
            }
        }
        content()
    }
}

/** "You owe" · hero amount · a line under it. */
@Composable
private fun SearchHeadline(
    title: String,
    amount: BigDecimal? = null,
    figure: String? = null,
    unit: String? = null,
    detail: String? = null,
    detailTone: ChipTone = ChipTone.Neutral,
) {
    Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = DNTextStyle.Headline.style, color = DNColor.textPrimary)
        if (amount != null) {
            MoneyText(amount, size = DNTextStyle.NumericHero, unit = DNTextStyle.NumericUnit)
        } else if (figure != null) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(figure, style = DNTextStyle.NumericHero.style, color = DNColor.textPrimary)
                if (unit != null) Text(unit, style = DNTextStyle.NumericUnit.style, color = DNColor.textPrimary, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        if (detail != null) {
            Text(
                detail,
                style = DNTextStyle.Subheadline.style.copy(fontWeight = if (detailTone == ChipTone.Neutral) FontWeight.Normal else FontWeight.SemiBold),
                color = searchToneColor(detailTone),
            )
        }
    }
}

@Composable
private fun SearchAnswerDivider() = HorizontalDivider(thickness = 0.5.dp, color = DNColor.listRowSeparator)

// MARK: - How much do I owe?

@Composable
internal fun SearchOweAnswer(
    payables: List<Payable>,
    updated: String,
    onPayAll: () -> Unit,
    onPay: (Payable) -> Unit,
    onPayOneAtATime: () -> Unit,
) {
    val total = payables.fold(BigDecimal.ZERO) { a, p -> a + p.amount }
    SearchAnswerCard(source = "From your accounts", trailing = "Updated $updated") {
        if (payables.isEmpty()) {
            SearchHeadline("You owe", amount = BigDecimal.ZERO, detail = "Nothing to pay right now. We'll tell you when a bill or fine arrives.")
        } else {
            SearchHeadline("You owe", amount = total, detail = "across ${SearchFormat.payablesSummary(payables)}")
            Column {
                payables.forEach { p ->
                    SearchAnswerDivider()
                    SearchOweRow(p) { onPay(p) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Large text: the amount is already the hero figure, so the button drops it rather than truncating.
                val amount = if (searchLargeText()) "" else " · ${Money.aed(total)}"
                val title = when (payables.size) {
                    1 -> if (amount.isEmpty()) "Pay now" else "Pay ${Money.aed(total)}"
                    2 -> "Pay both$amount"
                    else -> "Pay all ${payables.size}$amount"
                }
                DNButton(title, onClick = onPayAll)
                if (payables.size > 1) {
                    DNTextButton("Pay one at a time", onClick = onPayOneAtATime, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun SearchOweRow(p: Payable, onClick: () -> Unit) {
    val note = p.note?.lowercase()?.replace("your ", "")
    val due = p.due?.let { DemoDate.dueLabel(it).lowercase() }
    val tone = when {
        note != null -> ChipTone.Error
        p.due?.let { DemoDate.daysFromToday(it) <= 2 } == true -> ChipTone.Warning
        else -> ChipTone.Neutral
    }
    val stacked = searchLargeText()
    val amount: @Composable () -> Unit = {
        Text(Money.figure(p.amount), style = DNTextStyle.Callout.style.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), color = DNColor.textPrimary)
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = "Pay this one", onClick = onClick)
            .clearAndSetSemantics { contentDescription = "${p.title}, ${p.entity}, ${Money.spoken(p.amount)}" }
            .padding(vertical = if (stacked) 8.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HueIcon(p.symbol, p.hue, size = 36.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(p.title.replace(" & ", " and "), style = DNTextStyle.RowTitle.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.textPrimary)
            Text(
                listOfNotNull(p.entity, note ?: due).joinToString(" · "),
                style = DNTextStyle.Footnote.style.copy(fontWeight = FontWeight.SemiBold), color = searchToneColor(tone),
            )
            if (stacked) amount()
        }
        if (!stacked) amount()
    }
}

// MARK: - Visa status (any request)

@Composable
internal fun SearchRequestAnswer(request: RequestItem, document: DocumentItem?, updated: String, onOpen: () -> Unit) {
    val tone = searchTone(request.status)
    SearchAnswerCard(source = "From your application", trailing = "Updated $updated") {
        Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(request.title, style = DNTextStyle.Title3.style, color = DNColor.textPrimary)
            StatusChip(request.statusLine, tone = tone, onWhite = true)
        }

        val allDone = request.steps.isNotEmpty() && request.steps.all { it.state == RequestStep.State.Done }
        val index = request.currentStepIndex ?: if (allDone) request.steps.lastIndex else null
        if (index != null) {
            Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentedProgress(
                    total = request.steps.size, done = request.steps.count { it.state == RequestStep.State.Done },
                    currentColor = if (tone == ChipTone.Warning) DNColor.statusWarningForeground else DNColor.actionAccent,
                )
                Text("Step ${index + 1} of ${request.steps.size} · ${request.steps[index].title}", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
            }
        }

        request.notice?.let { (title, body) ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DNColor.statusWarningSurface).padding(12.dp)
                    .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = DNTextStyle.Subheadline.style.copy(fontWeight = FontWeight.SemiBold), color = DNColor.statusWarningForeground)
                Text(body, style = DNTextStyle.Footnote.style, color = DNColor.textPrimary)
            }
        }

        val expiry = document?.expiry
        if (document != null && expiry != null) {
            val days = DemoDate.daysFromToday(expiry)
            Row(Modifier.semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(searchIcon("calendar"), contentDescription = null, tint = DNColor.textSecondary, modifier = Modifier.padding(top = 2.dp).size(16.dp))
                Text(
                    "Your current ${document.title.lowercase()} expires on ${DemoDate.long(expiry)}, in $days days.",
                    style = DNTextStyle.Subheadline.style, color = DNColor.textPrimary, modifier = Modifier.weight(1f),
                )
            }
        }

        DNButton(request.action ?: "Track your request", onClick = onOpen)
    }
}

// MARK: - Salik / nol balance

@Composable
internal fun SearchBalanceAnswer(
    title: String,
    balance: BigDecimal,
    detail: String?,
    low: Boolean,
    lastTopUp: String?,
    updated: String,
    onTopUp: () -> Unit,
) {
    SearchAnswerCard(source = "From your accounts", trailing = "Updated $updated") {
        SearchHeadline(title, amount = balance, detail = detail, detailTone = if (low) ChipTone.Warning else ChipTone.Neutral)
        if (lastTopUp != null) Text(lastTopUp, style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
        DNButton("Top up", onClick = onTopUp)
    }
}

// MARK: - Prayer times

@Composable
internal fun SearchPrayerAnswer(named: String?, now: LocalDateTime, widgetOnHome: Boolean, onAddWidget: () -> Unit) {
    val (prayer, date) = searchPrayerTarget(named, now)
    val isToday = date.toLocalDate() == now.toLocalDate()
    val prayers = PrayerTimes.today.filter { it.name != "Sunrise" }
    SearchAnswerCard(source = "Prayer times · Dubai", sourceSymbol = "moon.stars.fill", trailing = if (isToday) "Today" else "Tomorrow") {
        Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (named == null) "Next prayer · ${prayer.name}" else "${prayer.name} is at", style = DNTextStyle.Headline.style, color = DNColor.textPrimary)
            Text(searchClock(prayer), style = DNTextStyle.NumericHero.style, color = DNColor.textPrimary)
            Text(
                "${if (isToday) "Today" else "Tomorrow"} · ${DemoDate.countdown(date, now)}",
                style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary,
            )
        }

        SearchAnswerDivider()

        if (searchLargeText()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                prayers.forEach { p ->
                    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                        SearchPrayerName(p, p.name == prayer.name, Modifier.weight(1f), TextAlign.Start)
                        SearchPrayerTime(p, p.name == prayer.name)
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth()) {
                prayers.forEach { p ->
                    Column(
                        Modifier.weight(1f).semantics(mergeDescendants = true) {},
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        SearchPrayerName(p, p.name == prayer.name)
                        SearchPrayerTime(p, p.name == prayer.name)
                    }
                }
            }
        }

        if (!widgetOnHome) {
            DNButton("Add prayer times to Home", onClick = onAddWidget, kind = DNButtonKind.Secondary, size = DNButtonSize.Medium)
        }
    }
}

@Composable
private fun SearchPrayerName(p: PrayerTimes.Prayer, isTarget: Boolean, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Center) {
    Text(p.name, style = DNTextStyle.Caption1.style, color = if (isTarget) DNColor.textLink else DNColor.textSecondary, modifier = modifier, textAlign = align)
}

@Composable
private fun SearchPrayerTime(p: PrayerTimes.Prayer, isTarget: Boolean) {
    Text(
        searchClock(p),
        style = DNTextStyle.Subheadline.style.copy(fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Medium, fontFeatureSettings = "tnum"),
        color = if (isTarget) DNColor.textLink else DNColor.textPrimary,
    )
}

private fun searchClock(p: PrayerTimes.Prayer) = "%02d:%02d".format(p.hour, p.minute)

/** The named prayer today (or tomorrow when it has passed), else the next one. */
internal fun searchPrayerTarget(named: String?, now: LocalDateTime): Pair<PrayerTimes.Prayer, LocalDateTime> {
    val p = named?.let { n -> PrayerTimes.today.firstOrNull { it.name == n } } ?: return PrayerTimes.next(now)
    val today = now.withHour(p.hour).withMinute(p.minute).withSecond(0).withNano(0)
    return if (today.isAfter(now)) p to today else p to today.plusDays(1)
}

// MARK: - Parking

@Composable
internal fun SearchParkingAnswer(session: ParkingSession?, updated: String, onExtend: () -> Unit, onPayForParking: () -> Unit) {
    SearchAnswerCard(source = "From your accounts", trailing = "Updated $updated") {
        if (session != null) {
            var now by remember { mutableStateOf(LocalDateTime.now()) }
            LaunchedEffect(session.endsAt) { while (true) { now = LocalDateTime.now(); delay(30_000) } }
            val left = searchMinutesLeft(session.endsAt, now)
            SearchHeadline(
                "Your parking",
                figure = if (left >= 60) "${left / 60}h ${left % 60}" else "$left",
                unit = "min left",
                detail = "Zone ${session.zone} · ${session.area} · ends ${DemoDate.time(session.endsAt)}",
                detailTone = if (left <= 15) ChipTone.Warning else ChipTone.Neutral,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SearchPlate(session.plate)
                Text("${Money.aed(session.ratePerHour, trimWhole = true)} an hour", style = DNTextStyle.Footnote.style, color = DNColor.textSecondary)
            }
            DNButton("Extend", onClick = onExtend)
        } else {
            SearchHeadline("You're not parked anywhere right now", detail = "Start a session when you park and we'll remind you before it ends.")
            DNButton("Pay for parking", onClick = onPayForParking, kind = DNButtonKind.Secondary)
        }
    }
}

// MARK: - Guest

@Composable
internal fun SearchSignInCard(onSignIn: () -> Unit) {
    Column(Modifier.fillMaxWidth().dnCard(28.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            HueIcon(searchIcon("lock.fill"), Hue.Sky, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sign in to see answers about your bills and fines", style = DNTextStyle.Headline.style, color = DNColor.textPrimary)
                Text(
                    "With UAE PASS, DubaiNow works out what you owe, your visa status and your balances from your linked accounts.",
                    style = DNTextStyle.Subheadline.style, color = DNColor.textSecondary,
                )
            }
        }
        DNButton("Sign in with UAE PASS", onClick = onSignIn)
    }
}

@Composable
internal fun SearchPayOneRow(p: Payable, onClick: () -> Unit) {
    // Dialogs are narrow: the amount goes on the second line so the title never wraps word by word.
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HueIcon(p.symbol, p.hue, size = 36.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(p.title.replace(" & ", " and "), style = DNTextStyle.RowTitle.style, color = DNColor.textPrimary)
            Text(
                buildAnnotatedString {
                    append("${p.entity} · ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = DNColor.textPrimary, fontFeatureSettings = "tnum")) { append(Money.aed(p.amount)) }
                },
                style = DNTextStyle.Footnote.style, color = DNColor.textSecondary,
            )
        }
    }
}
