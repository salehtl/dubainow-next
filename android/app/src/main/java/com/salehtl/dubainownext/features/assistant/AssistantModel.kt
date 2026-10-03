package com.salehtl.dubainownext.features.assistant

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.ui.graphics.vector.ImageVector
import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.EnergyReading
import com.salehtl.dubainownext.data.Payable
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.design.DNIcons
import com.salehtl.dubainownext.design.Hue
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

// The assistant's vocabulary (port of ios/…/Features/Assistant/AssistantModel.swift): what the face is
// doing, what the card shows, what you're asked to confirm. Paper C1 row 11 · V83-0 and W1D-0.

/** Eight states, one mark. The mark never changes shape; the ring of light says what it's doing. */
internal enum class AssistantFaceState(val spoken: String) {
    Resting("Resting"), Awake("Ready"), Listening("Listening"), Thinking("Thinking"),
    Working("Working on it"), YourTurn("Your turn"), Done("Done"), Quiet("Done"),
}

/** Where the conversation is. (After a task, Android shows the note as a snackbar once the overlay closes.) */
internal enum class AssistantPhase { Idle, Listening, Thinking, Card }

/** Text that may come in Mandarin with an English version ("Show in English" flips it). */
internal data class AssistantText(val en: String, val zh: String? = null) {
    fun resolved(english: Boolean): String = if (english) en else (zh ?: en)
}

/** One card hangs from the face: what you asked, its answer, and the task it's running. */
internal data class AssistantCard(
    val quote: AssistantText,
    val headline: AssistantText,
    val isThinking: Boolean = false,
    val surface: AssistantSurface? = null,
    val controls: AssistantControls = AssistantControls.None,
    val rating: AssistantRatingPrompt? = null,
    val id: String = UUID.randomUUID().toString(),
) {
    /** Mandarin cards get the "Show in English" toggle. */
    val isMandarin: Boolean get() = headline.zh != null
}

/** The tinted block inside the card. */
internal sealed interface AssistantSurface {
    data class Task(val block: AssistantTaskBlock) : AssistantSurface
    data class Payables(val items: List<Payable>) : AssistantSurface
    data class Request(val item: RequestItem) : AssistantSurface
    data class Journey(val block: AssistantJourneyBlock) : AssistantSurface
    data class Complaint(val block: AssistantComplaintBlock) : AssistantSurface
    data class Rows(val rows: List<AssistantListRow>) : AssistantSurface
    data class Energy(val reading: EnergyReading) : AssistantSurface
}

internal enum class AssistantTaskStatus { Working, YourTurn, Done, Stopped }

/** A service the assistant is running, as a checklist ("Renew registration · 3 of 5"). */
internal data class AssistantTaskBlock(
    val symbol: String,
    val hue: Hue,
    val title: AssistantText,
    val subtitle: AssistantText,
    val steps: List<AssistantStep>,
    val status: AssistantTaskStatus = AssistantTaskStatus.Working,
    val receiptID: String? = null,
    val reference: String? = null,
    val paidLine: String? = null,
) {
    /** "3 of 5": the step it's on. */
    val position: Int
        get() {
            val i = steps.indexOfFirst { it.state == AssistantStepState.Active || it.state == AssistantStepState.YourTurn }
                .takeIf { it >= 0 } ?: (steps.size - 1)
            return minOf(i + 1, steps.size)
        }

    fun withStep(index: Int, state: AssistantStepState, title: AssistantText? = null) =
        copy(steps = steps.mapIndexed { i, s -> if (i == index) s.copy(state = state, title = title ?: s.title) else s })
}

internal enum class AssistantStepState { Pending, Active, YourTurn, Done }

internal data class AssistantStep(
    val id: Int,
    val title: AssistantText,
    val amount: BigDecimal? = null,
    val state: AssistantStepState = AssistantStepState.Pending,
)

internal data class AssistantJourneyBlock(
    val symbol: String,
    val hue: Hue,
    val title: String,
    val subtitle: String,
    val stage: Int,          // 0-based
    val stageCount: Int,
    val now: String,
    val nowDetail: String,
    val next: String?,
)

/** "Report a problem with your employer", filled in from UAE PASS (Wei, in Mandarin). */
internal data class AssistantComplaintBlock(
    val fields: List<AssistantField>,
    val visibleCount: Int = 0,
    val originalEnglish: String,
    val originalArabic: String,
    val status: AssistantTaskStatus = AssistantTaskStatus.Working,
    val reference: String? = null,
)

internal data class AssistantField(val id: Int, val label: AssistantText, val value: AssistantText)

internal data class AssistantListRow(
    val id: String,
    val symbol: String,
    val hue: Hue,
    val title: String,
    val detail: String,
    val value: String? = null,
)

/** Buttons at the foot of the card. */
internal sealed interface AssistantControls {
    data object None : AssistantControls
    /** Stop and "I'll do it myself" sit on every task card. */
    data object Task : AssistantControls
    /** One plain next step ("Upload now", "Open the journey"). */
    data class Action(val title: AssistantText, val followUp: AssistantFollowUp) : AssistantControls
}

/** Where the assistant hands over after it closes. */
internal sealed interface AssistantFollowUp {
    data class Push(val route: Route) : AssistantFollowUp
    data class Present(val sheet: SheetRoute) : AssistantFollowUp
    data class OpenService(val id: String) : AssistantFollowUp
    /** The fake passport camera, over the assistant (the tracker's own scan). */
    data class ScanPassport(val requestID: String) : AssistantFollowUp
}

/** Inline Happiness faces after a task. */
internal data class AssistantRatingPrompt(val question: String, val detail: String, val receiptID: String, val thanks: String)

/** The one thing for you to do, under your thumb. */
internal data class AssistantConfirm(
    val style: Style,
    val title: AssistantText,
    val caption: AssistantText? = null,
    val reason: String,
) {
    sealed interface Style {
        /** Fingerprint payment ("Pay AED 1,020"). */
        data class Pay(val amount: BigDecimal) : Style
        /** Sent to an authority in your name. */
        data object UaePass : Style
    }

    val isUaePass: Boolean get() = style == Style.UaePass
}

/** After closing a finished task: a short note (Android: snackbar, "Receipt" action). */
internal data class AssistantNote(val text: String, val receiptID: String?)

/** A suggestion chip. */
internal data class AssistantSuggestion(val text: String, val symbol: String, val hue: Hue)

// MARK: - What you asked for

/** Keyword matching for the scripted demo (no network, no AI). */
internal sealed interface AssistantIntent {
    data object RenewRegistration : AssistantIntent
    data class Owe(val business: Boolean) : AssistantIntent
    data object NeedsYou : AssistantIntent
    data object VisaStatus : AssistantIntent
    data class SalikTopUp(val amount: BigDecimal) : AssistantIntent
    data class NolTopUp(val amount: BigDecimal) : AssistantIntent
    data class Journey(val hint: String) : AssistantIntent
    data object VehicleFines : AssistantIntent
    data object Energy : AssistantIntent
    data object Metro : AssistantIntent
    data object Fees : AssistantIntent
    data object Remind : AssistantIntent
    data object ScanPassport : AssistantIntent
    data class EmployerComplaint(val mandarin: Boolean) : AssistantIntent
    data object ContinueInEnglish : AssistantIntent
    data class Unknown(val mandarin: Boolean) : AssistantIntent

    /** Done-once key (so a finished task isn't suggested again). */
    val key: String
        get() = when (this) {
            RenewRegistration -> "renew"; is Owe -> "owe"; NeedsYou -> "needsYou"; VisaStatus -> "visa"
            is SalikTopUp -> "salik"; is NolTopUp -> "nol"; is Journey -> "journey"; VehicleFines -> "vehicleFines"
            Energy -> "energy"; Metro -> "metro"; Fees -> "fees"; Remind -> "remind"; ScanPassport -> "scan"
            is EmployerComplaint -> "complaint"; ContinueInEnglish -> "english"; is Unknown -> "unknown"
        }

    /** Suggestions after a task prefer the same part of life ("one after a task"). */
    val topic: String
        get() = when (this) {
            RenewRegistration, is SalikTopUp, is NolTopUp, VehicleFines, Metro -> "driving"
            Energy, is Journey -> "home"
            is Owe, NeedsYou -> "money"
            VisaStatus, Fees, ScanPassport, Remind -> "visa"
            is EmployerComplaint -> "work"
            ContinueInEnglish, is Unknown -> "other"
        }

    val chipSymbol: String
        get() = when (this) {
            RenewRegistration -> "car.fill"; is Owe -> "creditcard.fill"; NeedsYou -> "checklist"
            VisaStatus -> "person.text.rectangle"; is SalikTopUp -> "tag.fill"; is NolTopUp -> "tram.fill"
            is Journey -> "house.fill"; VehicleFines -> "exclamationmark.triangle.fill"; Energy -> "bolt.fill"
            Metro -> "tram.fill"; Fees -> "banknote"; Remind -> "bell"; ScanPassport -> "camera"
            is EmployerComplaint -> "briefcase.fill"; ContinueInEnglish -> "character.bubble"; is Unknown -> "sparkles"
        }

    val chipHue: Hue
        get() = when (this) {
            VisaStatus, ScanPassport, Fees, Remind -> Hue.Lilac
            is Journey, is EmployerComplaint, Energy -> Hue.Sand
            else -> Hue.Sky
        }

    companion object {
        fun match(raw: String): AssistantIntent {
            val t = raw.lowercase()
            if (isMandarin(raw)) {
                val workWords = listOf("工资", "薪", "投诉", "雇主", "老板", "公司")
                if (workWords.any { raw.contains(it) }) return EmployerComplaint(mandarin = true)
                if (t.contains("nol") || raw.contains("交通卡")) return NolTopUp(amount(t) ?: BigDecimal(50))
                return Unknown(mandarin = true)
            }
            fun has(vararg words: String) = words.any { t.contains(it) }
            if (has("english")) return ContinueInEnglish
            if (has("salik")) return SalikTopUp(amount(t) ?: BigDecimal(100))
            if (has("nol card", "nol ") || t.endsWith("nol")) return NolTopUp(amount(t) ?: BigDecimal(50))
            if (has("employer", "wages", "salary", "not been paid", "haven't been paid", "haven’t been paid")) return EmployerComplaint(mandarin = false)
            if (has("registration", "mulkiya") || (has("renew") && has("car", "vehicle", "patrol", "corolla"))) return RenewRegistration
            if (has("metro", "train")) return Metro
            if (has("power", "energy", "kwh", "electricity use")) return Energy
            if (has("fine") && !has("pay", "owe") && has("which", "van", "car", "vehicle", "truck")) return VehicleFines
            if (has("remind")) return Remind
            if (has("fee")) return Fees
            if (has("scan")) return ScanPassport
            if (has("visa")) return VisaStatus
            if (has("owe", "pay all", "pay everything", "bills", "fines", "pay my")) {
                return Owe(business = has("café", "cafe", "business", "company", "shop", "van"))
            }
            if (has("what do i need", "needs me", "need to do", "anything due", "what's due", "what’s due")) return NeedsYou
            if (has("house", "home", "journey", "what's next", "what’s next", "next for", "move", "build", "villa", "drawing")) return Journey(t)
            return Unknown(mandarin = false)
        }

        fun isMandarin(s: String): Boolean = s.codePoints().anyMatch { it in 0x4E00..0x9FFF }

        /** "AED 100" → 100 */
        fun amount(s: String): BigDecimal? =
            Regex("""\d[\d,]*(\.\d+)?""").find(s)?.value?.replace(",", "")?.toBigDecimalOrNull()
    }
}

// MARK: - Formatting

internal object AssistantFormat {
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)
    private val monthEN = DateTimeFormatter.ofPattern("MMMM", Locale.UK)
    private val monthAR = DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("ar-AE-u-nu-latn"))

    /** "13 Oct 2027" */
    fun dayMonthYear(d: LocalDateTime): String = d.format(dayMonthYear)

    /** The two months before this one (unpaid wages). */
    fun lastTwoMonths(now: LocalDateTime = LocalDateTime.now()): Pair<LocalDateTime, LocalDateTime> =
        now.minusMonths(2) to now.minusMonths(1)

    fun monthEnglish(d: LocalDateTime): String = d.format(monthEN)
    fun monthArabic(d: LocalDateTime): String = d.format(monthAR)

    /** "Upload a clearer passport copy" → "upload a clearer passport copy" */
    fun lowerFirst(s: String): String = if (s.isEmpty()) s else s.substring(0, 1).lowercase() + s.substring(1)
}

/**
 * Icons the assistant uses that the shared [DNIcons] map doesn't have yet (Material Symbols
 * Rounded); everything else goes through [DNIcons.forSymbol].
 */
internal fun assistantIcon(symbol: String): ImageVector = when (symbol) {
    "checklist" -> Icons.Rounded.Checklist
    "banknote" -> Icons.Rounded.Payments
    "camera", "camera.fill" -> Icons.Rounded.PhotoCamera
    "character.bubble" -> Icons.Rounded.Translate
    "doc.text" -> Icons.Rounded.Description
    "stop.fill" -> Icons.Rounded.Stop
    "mic.fill" -> Icons.Rounded.Mic
    "arrow.up" -> Icons.Rounded.ArrowUpward
    "faceid", "touchid" -> Icons.Rounded.Fingerprint
    "chevron.right" -> Icons.AutoMirrored.Rounded.KeyboardArrowRight
    else -> DNIcons.forSymbol(symbol)
}
