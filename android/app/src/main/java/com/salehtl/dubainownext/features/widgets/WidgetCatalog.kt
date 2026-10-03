package com.salehtl.dubainownext.features.widgets

import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.JourneyProgress
import com.salehtl.dubainownext.data.Payable
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.data.WidgetKind
import com.salehtl.dubainownext.data.WidgetSize
import java.math.BigDecimal
import java.time.LocalDateTime

// What each widget kind is: its sizes, who owns the data, how it reads in menus and the gallery.
// Members are prefixed `widget…` so they can't clash with other features' extensions.

/** Sizes this kind is designed in (Paper launch set + bundles and journeys). First is the default. */
internal val WidgetKind.widgetSizes: List<WidgetSize>
    get() = when (this) {
        WidgetKind.Parking, WidgetKind.Salik, WidgetKind.BillsAndFines, WidgetKind.FamilyVisas -> listOf(WidgetSize.Small, WidgetSize.Medium)
        WidgetKind.Prayer -> listOf(WidgetSize.Small, WidgetSize.Large)
        WidgetKind.Weather, WidgetKind.Nol, is WidgetKind.VisaTracker -> listOf(WidgetSize.Small)
        WidgetKind.Metro, WidgetKind.Energy -> listOf(WidgetSize.Medium)
        is WidgetKind.Journey -> listOf(WidgetSize.Medium, WidgetSize.Large)
        WidgetKind.Certificates -> emptyList()
    }

/** The size to draw when Home asks for one this kind isn't designed in. */
internal fun WidgetKind.widgetLayoutSize(requested: WidgetSize): WidgetSize {
    val sizes = widgetSizes
    if (requested in sizes || sizes.isEmpty()) return requested
    return when (requested) {
        WidgetSize.Small -> sizes.first()
        WidgetSize.Medium -> if (WidgetSize.Large in sizes) WidgetSize.Large else sizes.first()
        WidgetSize.Large -> if (WidgetSize.Medium in sizes) WidgetSize.Medium else sizes.first()
    }
}

/** Who owns the data ("Small · Parkin"). */
internal val WidgetKind.widgetOwner: String
    get() = when (this) {
        WidgetKind.Parking -> "Parkin"
        WidgetKind.Salik -> "Salik"
        WidgetKind.Prayer -> "IACAD"
        WidgetKind.Weather -> "NCM and Dubai Municipality"
        WidgetKind.Metro, WidgetKind.Nol -> "RTA"
        WidgetKind.Energy -> "DEWA"
        WidgetKind.BillsAndFines -> "Pay all"
        WidgetKind.FamilyVisas -> "GDRFA"
        WidgetKind.Certificates -> "DubaiNow"
        is WidgetKind.Journey -> Catalogue.service(id)?.ownerLabel ?: "DubaiNow"
        is WidgetKind.VisaTracker -> "Your request"
    }

/** Words people might search the gallery with. */
internal val WidgetKind.widgetSearchTerms: List<String>
    get() = when (this) {
        WidgetKind.Parking -> listOf("parking", "parkin", "zone", "car", "extend")
        WidgetKind.Salik -> listOf("salik", "toll", "gate", "balance", "top up", "car")
        WidgetKind.Prayer -> listOf("prayer", "salah", "namaz", "adhan", "fajr", "dhuhr", "asr", "maghrib", "isha", "faith", "mosque")
        WidgetKind.Weather -> listOf("weather", "beach", "swim", "temperature", "uv", "flag", "sea")
        WidgetKind.Metro -> listOf("metro", "train", "rta", "station", "departures", "red line", "transport")
        WidgetKind.Energy -> listOf("energy", "electricity", "dewa", "kwh", "meter", "bill", "home")
        WidgetKind.Nol -> listOf("nol", "card", "metro", "bus", "balance", "top up")
        WidgetKind.BillsAndFines -> listOf("bills", "fines", "pay all", "dewa", "money", "due")
        WidgetKind.FamilyVisas -> listOf("family", "visa", "visas", "residency", "renew", "children", "spouse")
        WidgetKind.Certificates -> listOf("certificates", "letters")
        is WidgetKind.Journey -> listOf("journey", "start to finish") + listOfNotNull(Catalogue.service(id)?.name?.lowercase())
        is WidgetKind.VisaTracker -> listOf("request", "tracker", "visa", "application", "status")
    }

/** A stable key for ids, debug extras and the gallery ("parking", "journey-move-home"). */
internal val WidgetKind.widgetKey: String
    get() = when (this) {
        WidgetKind.Parking -> "parking"; WidgetKind.Salik -> "salik"; WidgetKind.Prayer -> "prayer"
        WidgetKind.Weather -> "weather"; WidgetKind.Metro -> "metro"; WidgetKind.Energy -> "energy"
        WidgetKind.Nol -> "nol"; WidgetKind.BillsAndFines -> "bills"; WidgetKind.FamilyVisas -> "family"
        WidgetKind.Certificates -> "certificates"
        is WidgetKind.Journey -> "journey-$id"
        is WidgetKind.VisaTracker -> "request-$requestID"
    }

/** How often a kind's live facts refresh: countdowns every second, slower facts every minute. */
internal val WidgetKind.widgetTickMillis: Long
    get() = when (this) {
        WidgetKind.Parking, WidgetKind.Metro -> 1_000L
        WidgetKind.Prayer -> 15_000L
        else -> 60_000L
    }

internal val WidgetSize.widgetLabel: String
    get() = when (this) {
        WidgetSize.Small -> "Small"
        WidgetSize.Medium -> "Medium"
        WidgetSize.Large -> "Large"
    }

// MARK: - Store helpers (read-only views of existing state)

/** Salik is linked when the persona has a linked Salik account (or any balance). */
internal val DemoStore.widgetSalikLinked: Boolean
    get() = state.linkedAccounts.any { it.id == "salik" && it.isLinked } || state.salikBalance > BigDecimal.ZERO

internal fun DemoStore.widgetRequest(id: String): RequestItem? = state.requests.firstOrNull { it.id == id }

// MARK: - Bills and fines (Pay all)

/** The month at a glance for the Bills and fines widget: what's still to come, what's paid. */
internal class WidgetBillsModel(store: DemoStore, now: LocalDateTime) {
    val toCome: List<Payable> = store.payables
    val total: BigDecimal = store.payAllTotal
    val paidThisMonth: BigDecimal
    val monthName: String = WidgetFormat.month(now)
    val next: Payable?
    val overdueCount: Int

    init {
        val parts = (Catalogue.service("pay-all")?.parts ?: emptyList()).toSet() + "pay-all"
        paidThisMonth = store.state.history
            .filter { it.date.year == now.year && it.date.month == now.month && (it.serviceID ?: "") in parts }
            .fold(BigDecimal.ZERO) { a, r -> a + (r.amount ?: BigDecimal.ZERO) }
        next = toCome.filter { it.due != null }.minByOrNull { it.due!! } ?: toCome.firstOrNull()
        overdueCount = toCome.count { p -> (p.due?.let { DemoDate.daysFromToday(it) } ?: 0) < 0 }
    }

    val allPaid: Boolean get() = toCome.isEmpty() && paidThisMonth > BigDecimal.ZERO
    val nothingYet: Boolean get() = toCome.isEmpty() && paidThisMonth.signum() == 0

    /** "tomorrow", "in 3 days", "13 Oct", "overdue", or null when it has no due date. */
    val nextWhen: String?
        get() {
            val due = next?.due ?: return null
            val label = WidgetFormat.iosMonth(DemoDate.dueLabel(due))
            return if (label == "Overdue") "overdue" else label.replace("Due ", "")
        }

    /** "Next due tomorrow" */
    val nextChip: String?
        get() {
            val w = nextWhen ?: return if (toCome.isEmpty()) null else "Pay when ready"
            return if (w == "overdue") "Overdue" else "Next due $w"
        }

    val paidFraction: Double
        get() {
            val paid = paidThisMonth.toDouble(); val left = total.toDouble()
            return if (paid + left > 0) paid / (paid + left) else 0.0
        }
}

// MARK: - Family visas

internal class WidgetFamilyModel private constructor(
    val members: List<Member>,
    val dueCount: Int,
    val days: Int,
) {
    data class Member(val id: String, val name: String, val initial: String, val days: Int?, val isDue: Boolean)

    companion object {
        fun from(store: DemoStore): WidgetFamilyModel? {
            val status = store.state.familyVisas ?: return null
            val family = store.state.family
            val all = status.memberIDs.mapNotNull { id ->
                val m = family.firstOrNull { it.id == id } ?: return@mapNotNull null
                Member(
                    id = id,
                    name = m.name.split(" ").firstOrNull() ?: m.name,
                    initial = m.initials,
                    days = m.visaExpiry?.let { DemoDate.daysFromToday(it) },
                    isDue = id in status.dueMemberIDs,
                )
            }
            return WidgetFamilyModel(
                members = all.filter { it.isDue } + all.filterNot { it.isDue },
                dueCount = status.dueMemberIDs.size,
                days = DemoDate.daysFromToday(status.expiresOn),
            )
        }
    }
}

// MARK: - Journeys

internal class WidgetJourneyModel private constructor(
    val service: CatalogueService,
    val progress: JourneyProgress,
) {
    val stages: List<CatalogueService.Stage> = service.stages ?: emptyList()

    val total: Int get() = maxOf(stages.size, 1)
    val number: Int get() = minOf(progress.stageIndex + 1, total)
    val stageName: String get() = if (progress.stageIndex < stages.size) stages[progress.stageIndex].name else progress.headline

    /** Sensitive journeys use the quiet tone: white, no hue, no big figure, no countdown. */
    val isQuiet: Boolean get() = service.id == "after-a-death"

    val caption: String get() = progress.destination?.let { "Start to finish · $it" } ?: "Start to finish"

    /** "Day 4", "Week 2", "Month 9" since it started. */
    val age: String
        get() {
            val d = maxOf(0, -DemoDate.daysFromToday(progress.startedOn))
            return when {
                d < 7 -> "Day ${d + 1}"
                d < 60 -> "Week ${d / 7 + 1}"
                else -> "Month ${d / 30 + 1}"
            }
        }

    val spoken: String
        get() {
            val turn = if (progress.needsYou) "1 thing needs you" else progress.waitingOn
            return "${service.short}, stage $number of $total, $stageName, $turn"
        }

    companion object {
        fun from(id: String, store: DemoStore): WidgetJourneyModel? {
            val service = Catalogue.service(id) ?: return null
            val progress = store.state.journeys.firstOrNull { it.journeyID == id } ?: return null
            return WidgetJourneyModel(service, progress)
        }
    }
}
