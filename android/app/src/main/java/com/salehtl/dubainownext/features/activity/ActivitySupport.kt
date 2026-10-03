package com.salehtl.dubainownext.features.activity

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.fragment.app.FragmentActivity
import com.salehtl.dubainownext.app.LaunchOptions
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.DraftItem
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.NeedsYouItem
import com.salehtl.dubainownext.data.PaymentLine
import com.salehtl.dubainownext.data.PaymentMethod
import com.salehtl.dubainownext.data.PaymentPurpose
import com.salehtl.dubainownext.data.PaymentRequest
import com.salehtl.dubainownext.data.Receipt
import com.salehtl.dubainownext.data.ReceiptRow
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.data.RequestStep
import com.salehtl.dubainownext.data.aed
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.ChipTone
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Shared helpers for the Activity feature (Activity tab, request tracker, receipts, payments).
// Anything here that belongs in data/ is listed in NOTES.md for the orchestrator to promote.

// MARK: - Store helpers (read the public state; small writes replace store.state)

/**
 * Requests with government that aren't waiting on a Needs you card (so Needs you and
 * In progress never show the same request twice).
 */
internal val DemoStore.activityInProgress: List<RequestItem>
    get() {
        val waiting = state.needsYou.mapNotNull { (it.kind as? NeedsYouItem.Kind.RequestStep)?.requestID }.toSet()
        return state.requests.filter { it.status != RequestItem.Status.Done && it.id !in waiting }
    }

/** Puts a deleted draft back where it was (Undo on the "Draft deleted" toast). */
internal fun DemoStore.activityRestoreDraft(draft: DraftItem, index: Int) {
    if (state.drafts.any { it.id == draft.id }) return
    val list = state.drafts.toMutableList()
    list.add(index.coerceIn(0, list.size), draft)
    state = state.copy(drafts = list)
}

/** Adds rows to a receipt the store created without any (e.g. request fees list their parts). */
internal fun DemoStore.activityAttachRows(rows: List<ReceiptRow>, receiptId: String) {
    state = state.copy(history = state.history.map { if (it.id == receiptId && it.rows.isEmpty()) it.copy(rows = rows) else it })
}

// MARK: - Request steps and status

internal enum class ActivityStepKind { Upload, Pay, Waiting }

/** What the resident can do on this step when it's current. */
internal val RequestStep.activityKind: ActivityStepKind
    get() {
        val t = title.lowercase()
        return when {
            "upload" in t || "scan" in t -> ActivityStepKind.Upload
            t.startsWith("pay ") -> ActivityStepKind.Pay
            else -> ActivityStepKind.Waiting
        }
    }

internal val RequestItem.activityCurrentStep: RequestStep? get() = currentStepIndex?.let { steps[it] }

internal data class ActivityStatus(val text: String, val tone: ChipTone)

/**
 * Status line and tone for rows ("Approved · card arrives Thursday") and the tracker chip
 * ("Your turn · 3 days left").
 */
internal fun RequestItem.activityStatus(chip: Boolean = false): ActivityStatus {
    val s = status
    // The fee step reads the same as on iOS, on the row and on the tracker chip.
    if (activityCurrentStep?.activityKind == ActivityStepKind.Pay) return ActivityStatus("Your turn · pay the fees", ChipTone.Warning)
    return when (s) {
        is RequestItem.Status.YourTurn ->
            ActivityStatus(if (chip) "Your turn · ${ActivityFormat.daysLeft(s.due)}" else "Your turn · by ${DemoDate.long(s.due)}", ChipTone.Warning)
        is RequestItem.Status.InReview -> ActivityStatus("In review · answer by ${DemoDate.short(s.answerBy)}", ChipTone.Info)
        is RequestItem.Status.Approved -> ActivityStatus("Approved · ${s.note}", ChipTone.Success)
        RequestItem.Status.Done -> ActivityStatus("Done", ChipTone.Success)
    }
}

/** The outcome row at the end of the tracker (the star). */
internal val RequestItem.activityOutcome: Pair<String, String>
    get() = when (serviceID) {
        "renew-residence-visa" -> "New visa and Emirates ID" to "Added to your Documents straight away"
        "renew-driving-licence" -> "New driving licence" to "Your digital licence updates in Documents"
        "top-up-salik-account" -> "Decision from Salik" to "We'll tell you as soon as it's made"
        else -> "All done" to "We'll keep the record in your History"
    }

/** The exact fees when the "Pay the fees" step comes up. */
internal val RequestItem.activityFeeLines: List<PaymentLine>
    get() = when (serviceID) {
        "renew-residence-visa" -> listOf(
            PaymentLine("Residence visa, 2 years", "GDRFA", 650.aed, "person.text.rectangle.fill", Hue.Lilac),
            PaymentLine("Emirates ID, 2 years", "ICP", 370.aed, "creditcard.fill", Hue.Mint),
            PaymentLine("Service and knowledge fees", "GDRFA", 126.50.aed, "doc.text.fill", Hue.Sky),
        )
        else -> {
            val amount = ActivityFormat.firstAmount(activityCurrentStep?.detail ?: "") ?: 100.aed
            listOf(PaymentLine("Fees for your request", entity, amount, symbol, hue))
        }
    }

/** The payment request for the "Pay the fees" step. */
internal val RequestItem.activityFeesRequest: PaymentRequest
    get() {
        val lines = activityFeeLines
        val total = lines.fold(BigDecimal.ZERO) { a, l -> a + l.amount }
        return PaymentRequest(id = "fees-$id", title = "Pay the fees", lines = lines, purpose = PaymentPurpose.RequestFees(id, total))
    }

internal val RequestItem.activityCanCancel: Boolean
    get() = status is RequestItem.Status.YourTurn || status is RequestItem.Status.InReview

// MARK: - Formatting

internal object ActivityFormat {
    private val monthName = DateTimeFormatter.ofPattern("MMMM", Locale.UK)
    private val monthKey = DateTimeFormatter.ofPattern("yyyy-MM", Locale.UK)
    private val isoish = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.UK)

    // Built on DemoDate so every screen writes months the same way (see NOTES.md, shared request 2).
    /** "1 Oct 2026, 08:14" */
    fun whenLong(d: LocalDateTime): String = "${DemoDate.short(d)} ${d.year}, ${DemoDate.time(d)}"
    /** "1 Oct, 08:14" */
    fun whenShort(d: LocalDateTime): String = "${DemoDate.short(d)}, ${DemoDate.time(d)}"
    /** "2026-10-01 08:14" (CSV) */
    fun csvDate(d: LocalDateTime): String = d.format(isoish)

    /** "3 days left", "1 day left", "due today", "overdue" */
    fun daysLeft(due: LocalDateTime): String = when (val n = DemoDate.daysFromToday(due)) {
        in Int.MIN_VALUE..-1 -> "overdue"
        0 -> "due today"
        1 -> "1 day left"
        else -> "$n days left"
    }

    /** Compact chip on a Needs you card: "Today", "Tomorrow", "3 days", "15 Oct". */
    fun dueChip(due: LocalDateTime): String = when (val n = DemoDate.daysFromToday(due)) {
        in Int.MIN_VALUE..-1 -> "Overdue"
        0 -> "Today"
        1 -> "Tomorrow"
        in 2..13 -> "$n days"
        else -> DemoDate.short(due)
    }

    data class Month(val id: String, val title: String, val receipts: List<Receipt>)

    /** Receipts grouped by month, newest first ("October", "September", "August 2025"). */
    fun months(receipts: List<Receipt>): List<Month> {
        val thisYear = LocalDate.now().year
        return receipts.sortedByDescending { it.date }
            .groupBy { it.date.format(monthKey) }
            .map { (key, list) ->
                val d = list.first().date
                Month(key, if (d.year == thisYear) d.format(monthName) else DemoDate.monthYear(d), list)
            }
    }

    private val amountPattern = Regex("""AED\s?[0-9][0-9,]*(\.[0-9]+)?""")

    /** The first "AED 1,150" style amount in a string. */
    fun firstAmount(text: String): BigDecimal? {
        val match = amountPattern.find(text) ?: return null
        return match.value.replace("AED", "").replace(",", "").trim().toBigDecimalOrNull()
    }

    /** "DEWA and Dubai Police", "DEWA, RTA and Salik" */
    fun list(items: List<String>): String {
        val unique = items.distinct()
        return when (unique.size) {
            0 -> ""
            1 -> unique[0]
            else -> unique.dropLast(1).joinToString(", ") + " and " + unique.last()
        }
    }

    /** "Reference G D R F A dash …" — read character by character. */
    fun spelled(reference: String): String = reference.toList().joinToString(" ")
}

// MARK: - Receipt copy

/** "How was topping up Salik?" */
internal val Receipt.activityRatingQuestion: String
    get() = when (serviceID) {
        "top-up-salik-account" -> "How was topping up Salik?"
        "top-up-nol-card" -> "How was topping up your nol card?"
        "pay-for-parking" -> "How was paying for parking?"
        "pay-electricity-water-bill" -> "How was paying your DEWA bill?"
        "pay-traffic-fine" -> "How was paying your fine?"
        "pay-all" -> "How was paying your bills and fines?"
        "renew-vehicle-registration" -> "How was renewing your registration?"
        "renew-driving-licence" -> "How was renewing your licence?"
        "renew-residence-visa" -> "How was paying your visa fees?"
        "donate-to-charity" -> "How was donating?"
        else -> if (amount == null) "How was it?" else "How was paying?"
    }

/** Who the rating goes to ("It goes to Salik and the Happiness Meter."). */
internal val Receipt.activityRatingRecipient: String get() = entity

internal val PaymentMethod.activityIcon: ImageVector
    get() = when (this) {
        PaymentMethod.GooglePay -> Icons.Rounded.AccountBalanceWallet
        is PaymentMethod.Card -> Icons.Rounded.CreditCard
    }

// MARK: - Filters

internal enum class ActivityFilter(val key: String, val title: String) {
    All("all", "All"),
    NeedsYou("needsYou", "Needs you"),
    InProgress("inProgress", "In progress"),
    Drafts("drafts", "Drafts"),
    History("history", "History");

    companion object {
        /** `activityFilter drafts` (screenshots and demos). */
        val launchValue: ActivityFilter? get() = LaunchOptions.extras["activityFilter"]?.let { v -> entries.firstOrNull { it.key.equals(v, ignoreCase = true) } }
    }
}

/**
 * What the Activity tab remembers while you visit a tracker or a receipt (the tab root leaves
 * composition on a push): the chosen filter and the scroll position. Reset for a new persona.
 */
internal object ActivityTabMemory {
    var personaId: String? = null
    var filter by mutableStateOf(ActivityFilter.launchValue ?: ActivityFilter.All)
    var firstIndex = 0
    var firstOffset = 0
    var didApplyLaunchScroll = false

    fun sync(persona: String?) {
        if (persona != personaId) {
            personaId = persona
            filter = ActivityFilter.launchValue ?: ActivityFilter.All
            firstIndex = 0; firstOffset = 0
        }
    }
}

// MARK: - Draft pins (demo-only; see NOTES.md → DraftItem.isPinned)

internal object ActivityDraftPins {
    private val pinned = mutableStateListOf<String>()

    fun toggle(id: String) { if (id in pinned) pinned.remove(id) else pinned.add(id) }
    fun isPinned(id: String) = id in pinned
    fun sorted(drafts: List<DraftItem>): List<DraftItem> = drafts.filter { isPinned(it.id) } + drafts.filterNot { isPinned(it.id) }
}

/** The next thing to do when they continue (demo copy; falls back to the step count). */
internal val DraftItem.activityNextStep: String
    get() = when (serviceID) {
        "register-tenancy-contract" -> "Next: add your landlord's details"
        "get-good-conduct-certificate" -> "Next: choose who the certificate is for"
        else -> "Next: step ${minOf(stepsDone + 1, stepsTotal)} of $stepsTotal"
    }

/** "Dubai Police" (the record's owner, when the catalogue knows it). */
internal val DraftItem.activityOwner: String?
    get() = Catalogue.service(serviceID)?.ownerLabel?.takeIf { it.isNotEmpty() && it != "Owner to confirm" }

// MARK: - Demo extras

/**
 * `activityDemo scan|shoot|send|uploaded|fees|feesdone|pay|paydone|upload|sent|cancel|menu|delete|paywith|pdf|csv` drives the flows
 * for screenshots and rehearsals. Each value fires once per launch.
 */
internal object ActivityDemo {
    val value: String? get() = LaunchOptions.extras["activityDemo"]
    private val used = mutableSetOf<String>()

    /** True the first time it's asked for one of [values] (so a demo step runs once). */
    fun take(vararg values: String): Boolean {
        val v = value ?: return false
        if (v !in values) return false
        return once(v)
    }

    /** True the first time [key] is asked for in this launch. */
    fun once(key: String): Boolean {
        if (key in used) return false
        used += key
        return true
    }
}

// MARK: - Platform helpers

/** The system "Remove animations" setting (animator duration scale 0). */
@Composable
internal fun activityReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
}

/** True at the largest font sizes, where rows stack instead of sitting side by side. */
@Composable
internal fun activityLargeText(): Boolean {
    LocalConfiguration.current
    return LocalDensity.current.fontScale >= 1.5f
}

internal tailrec fun Context.activityFindActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activityFindActivity()
    else -> null
}

internal fun Context.activityFindFragmentActivity(): FragmentActivity? = activityFindActivity() as? FragmentActivity

/** "Pay AED 1,146.50" */
internal fun activityPayLabel(total: BigDecimal) = "Pay ${Money.aed(total)}"
