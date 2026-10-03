package com.salehtl.dubainownext.data

import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.design.components.HappinessFace
import com.salehtl.dubainownext.design.Hue
import java.math.BigDecimal
import java.time.LocalDateTime

// Mirrors ios/DubaiNowDemo/Data/Models.swift — same names and meanings, so both apps tell
// the same story with the same numbers. Symbol fields hold the iOS SF Symbol names; render
// them with DNIcons.forSymbol(…).

// MARK: Persona

/** A demo resident you can sign in as. All their data is consistent across every screen. */
data class Persona(
    val id: String,                 // "sara"
    val firstName: String,
    val lastName: String,
    val initials: String,
    val avatarHue: Hue,
    /** One line for the persona picker: who they are and what's going on for them. */
    val tagline: String,
    val area: String,               // "Al Barsha 1"
    val residency: String,          // "Residence visa" / "UAE national"
    val emiratesID: String,         // masked
    val phone: String,              // masked
    /** Things the assistant suggests for this person (first is the showcase). */
    val assistantPrompts: List<String>,
    /** Builds a fresh copy of their data (so "Reset demo" works). */
    val makeState: () -> PersonaState,
) {
    val fullName: String get() = "$firstName $lastName"
    override fun equals(other: Any?) = other is Persona && other.id == id
    override fun hashCode() = id.hashCode()
}

/** Everything about a resident that the demo shows or changes. */
data class PersonaState(
    val needsYou: List<NeedsYouItem> = emptyList(),
    val favorites: List<FavoriteItem> = emptyList(),
    val widgets: List<HomeWidget> = emptyList(),
    /** One suggestion at a time ("Add Family visas to Home?"). null when none. */
    val widgetSuggestion: WidgetKind? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val documents: List<DocumentItem> = emptyList(),
    val family: List<FamilyMember> = emptyList(),
    val home: HomeInfo? = null,
    val paymentMethods: List<PaymentMethod> = listOf(PaymentMethod.GooglePay),
    val requests: List<RequestItem> = emptyList(),
    val drafts: List<DraftItem> = emptyList(),
    val history: List<Receipt> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val linkedAccounts: List<LinkedAccount> = emptyList(),
    val salikBalance: BigDecimal = BigDecimal.ZERO,
    val nolBalance: BigDecimal? = null,
    val parking: ParkingSession? = null,
    val energy: EnergyReading? = null,
    val metro: MetroStation? = null,
    val weatherSpot: String = "Kite Beach",
    val journeys: List<JourneyProgress> = emptyList(),
    val familyVisas: FamilyVisasStatus? = null,
)

// MARK: Payables

/** Something that can be paid: a bill, a fine, a fee or a top-up. */
data class Payable(
    val id: String,
    val kind: Kind,
    val title: String,              // "Electricity & water bill"
    val entity: String,             // "DEWA"
    val detail: String,             // "Al Barsha 1 · Account 2004 1187"
    val amount: BigDecimal,
    val due: LocalDateTime?,
    val reference: String,
    val symbol: String,
    val hue: Hue,
    /** Catalogue service this payment belongs to ("pay-electricity-water-bill"). */
    val serviceID: String?,
    /** e.g. "Blocks your car renewal" */
    val note: String? = null,
) {
    enum class Kind { Bill, Fine, Fee, TopUp }
}

// MARK: Needs you

/** A card in "Needs you" (Home anchor 1) and the Activity "Needs you" filter. */
data class NeedsYouItem(
    val id: String,
    val kind: Kind,
    val entity: String,
    val symbol: String,
    val hue: Hue,
    val title: String,
    val detail: String,
    val due: LocalDateTime?,
    val primaryAction: String,      // "Pay now", "Upload now", "Renew", "Continue"
    val secondaryAction: String?,   // "Later", "Details"
    val progress: StepProgress? = null,
) {
    sealed interface Kind {
        /** Pay it (bill or fine). Pay all covers every payable item. */
        data class Payment(val payable: Payable) : Kind
        /** A step in a request that's waiting for the resident. */
        data class RequestStep(val requestID: String) : Kind
        /** Something to renew (e.g. vehicle registration) — opens the service. */
        data class Renewal(val serviceID: String, val thingID: String?) : Kind
        /** A journey stage waiting for the resident. */
        data class JourneyStep(val journeyID: String) : Kind
    }

    val payable: Payable? get() = (kind as? Kind.Payment)?.payable
    val amount: BigDecimal? get() = payable?.amount
}

data class StepProgress(val current: Int, val total: Int)

// MARK: Favorites and widgets

/** A favorite tile on Home (up to 8). Opens a catalogue record or a thing. */
data class FavoriteItem(
    val id: String,
    val title: String,              // "Pay parking"
    val symbol: String,
    val hue: Hue,
    val target: Target,
) {
    sealed interface Target {
        data class Service(val id: String) : Target
        data class Thing(val id: String) : Target
    }
}

enum class WidgetSize {
    /** half width square */ Small,
    /** full width × small height */ Medium,
    /** full width square */ Large,
}

/** Kinds of Home widget. Content widgets read from the persona's state. */
sealed interface WidgetKind {
    data object Parking : WidgetKind
    data object Salik : WidgetKind
    data object Prayer : WidgetKind
    data object Weather : WidgetKind
    data object Metro : WidgetKind
    data object Energy : WidgetKind
    data object Nol : WidgetKind
    data object BillsAndFines : WidgetKind
    data object FamilyVisas : WidgetKind
    data object Certificates : WidgetKind
    data class Journey(val id: String) : WidgetKind
    data class VisaTracker(val requestID: String) : WidgetKind

    val title: String
        get() = when (this) {
            Parking -> "Parking"; Salik -> "Salik"; Prayer -> "Prayer times"; Weather -> "Weather and beaches"
            Metro -> "Metro"; Energy -> "Energy use"; Nol -> "nol card"; BillsAndFines -> "Bills and fines"
            FamilyVisas -> "Family visas"; Certificates -> "Certificates and letters"
            is Journey -> Catalogue.service(id)?.short ?: "Journey"
            is VisaTracker -> "Request tracker"
        }
}

data class HomeWidget(val id: String, val kind: WidgetKind, val size: WidgetSize)

// MARK: Things

data class Plate(val emirate: String = "DUBAI", val code: String, val number: String)

data class Vehicle(
    val id: String,
    val make: String,
    val model: String,
    val year: Int,
    val colour: String,
    val plate: Plate,
    val registrationExpiry: LocalDateTime,
    val insurer: String,
    val insuranceExpiry: LocalDateTime,
    val lastInspection: LocalDateTime?,
    val salikTag: String?,
    /** Payable ids of fines on this vehicle. */
    val fineIDs: List<String> = emptyList(),
) {
    val name: String get() = "$make $model $year"
    val shortName: String get() = "$make $model"
}

data class DocumentItem(
    val id: String,
    val title: String,
    val number: String,
    val expiry: LocalDateTime?,
    val status: Status = Status.Valid,
    val statusNote: String? = null,
    val symbol: String,
    val hue: Hue,
) {
    enum class Status { Valid, Renewing, ExpiringSoon, Expired }
}

data class FamilyMember(
    val id: String,
    val name: String,
    val relation: String,
    val age: Int?,
    val visaExpiry: LocalDateTime?,
    val sponsoredByYou: Boolean,
    val initials: String,
    val hue: Hue,
)

data class HomeInfo(val address: String, val dewaAccount: String, val ejariExpiry: LocalDateTime?, val makani: String?)

/** On Android the wallet is Google Pay (the iOS app shows Apple Pay). */
sealed interface PaymentMethod {
    data object GooglePay : PaymentMethod
    data class Card(val brand: String, val last4: String) : PaymentMethod

    val label: String get() = when (this) { GooglePay -> "Google Pay"; is Card -> "$brand ending $last4" }
}

// MARK: Activity

data class RequestStep(val title: String, val detail: String, val state: State) {
    enum class State { Done, Current, Upcoming }
}

/** A request in progress with government (the request tracker). */
data class RequestItem(
    val id: String,
    val serviceID: String,
    val title: String,
    val entity: String,
    val reference: String,
    val status: Status,
    /** What the resident needs to know right now (top card on the tracker): title to body. */
    val notice: Pair<String, String>?,
    val steps: List<RequestStep>,
    val startedOn: LocalDateTime,
    val symbol: String,
    val hue: Hue,
    /** Label for the action when it's your turn ("Upload now"). */
    val action: String? = null,
) {
    sealed interface Status {
        data class YourTurn(val due: LocalDateTime) : Status
        data class InReview(val answerBy: LocalDateTime) : Status
        data class Approved(val note: String) : Status
        data object Done : Status
    }

    val statusLine: String
        get() = when (val s = status) {
            is Status.YourTurn -> "Your turn · by ${DemoDate.long(s.due)}"
            is Status.InReview -> "In review · answer by ${DemoDate.short(s.answerBy)}"
            is Status.Approved -> "Approved · ${s.note}"
            Status.Done -> "Done"
        }

    val currentStepIndex: Int? get() = steps.indexOfFirst { it.state == RequestStep.State.Current }.takeIf { it >= 0 }
}

data class DraftItem(
    val id: String,
    val serviceID: String,
    val title: String,
    val stepsDone: Int,
    val stepsTotal: Int,
    val savedOn: LocalDateTime,
    val symbol: String,
    val hue: Hue,
)

/** A completed payment or submission (History, receipts). */
data class Receipt(
    val id: String,
    val title: String,
    val shortTitle: String,
    val entity: String,
    val amount: BigDecimal?,
    val date: LocalDateTime,
    val method: PaymentMethod?,
    val reference: String,
    val rows: List<ReceiptRow> = emptyList(),
    val symbol: String,
    val hue: Hue,
    val serviceID: String?,
    val count: Int? = null,
    val rating: HappinessFace? = null,
    val doneByAssistant: Boolean = false,
)

data class ReceiptRow(val label: String, val value: String)

data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val date: LocalDateTime,
    val symbol: String,
    val hue: Hue,
    val isRead: Boolean,
    val link: Route?,
)

data class LinkedAccount(
    val id: String,
    val name: String,
    val detail: String,
    val symbol: String,
    val hue: Hue,
    val isLinked: Boolean,
)

// MARK: Live things for widgets

data class ParkingSession(
    val zone: String,
    val area: String,
    val plate: Plate,
    val startedAt: LocalDateTime,
    val endsAt: LocalDateTime,
    val ratePerHour: BigDecimal,
)

data class EnergyReading(
    val monthLabel: String,
    val kWh: Int,
    val changeVsLastMonth: Int,
    val comparison: String,
    val lastSixMonths: List<Int>,
)

data class MetroStation(val name: String, val line: String, val departures: List<MetroDeparture>)
data class MetroDeparture(val towards: String, val inMinutes: Int, val nextInMinutes: Int)

/** A journey the resident has started (Build a house, Move home…). */
data class JourneyProgress(
    val journeyID: String,
    val stageIndex: Int,
    val headline: String,
    val waitingOn: String,
    val startedOn: LocalDateTime,
    val destination: String? = null,
    /** True when the current step needs the resident (then the button lives in Needs you). */
    val needsYou: Boolean = false,
)

data class FamilyVisasStatus(val memberIDs: List<String>, val dueMemberIDs: List<String>, val expiresOn: LocalDateTime)

// MARK: Payment requests (fake payment confirmations)

/** What a payment is for; the store applies its effects when it's confirmed. */
sealed interface PaymentPurpose {
    data class Payables(val ids: List<String>) : PaymentPurpose
    data class SalikTopUp(val amount: BigDecimal) : PaymentPurpose
    data class NolTopUp(val amount: BigDecimal) : PaymentPurpose
    data class ExtendParking(val minutes: Int) : PaymentPurpose
    data class RequestFees(val requestID: String, val amount: BigDecimal) : PaymentPurpose
    data class VehicleRenewal(val vehicleID: String, val fee: BigDecimal, val fineIDs: List<String>) : PaymentPurpose
    data class Donation(val charity: String, val amount: BigDecimal) : PaymentPurpose
}

data class PaymentRequest(
    val id: String,
    val title: String,
    val lines: List<PaymentLine>,
    val purpose: PaymentPurpose,
    val doneByAssistant: Boolean = false,
) {
    val total: BigDecimal get() = lines.fold(BigDecimal.ZERO) { acc, l -> acc + l.amount }
}

data class PaymentLine(val title: String, val detail: String, val amount: BigDecimal, val symbol: String, val hue: Hue)
