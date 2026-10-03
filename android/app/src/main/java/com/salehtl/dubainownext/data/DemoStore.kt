package com.salehtl.dubainownext.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.salehtl.dubainownext.data.personas.Personas
import com.salehtl.dubainownext.design.components.HappinessFace
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.TimeOfDay
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

/** Who is using the app. */
sealed interface Session {
    data class SignedIn(val persona: Persona) : Session
    data object Guest : Session
}

/**
 * The demo's single source of truth: the signed-in persona and their data. Every screen reads
 * from here, so numbers agree everywhere. Mirrors ios/…/Data/DemoStore.swift.
 * State is an immutable [PersonaState] replaced with `copy(...)`; Compose observes it.
 */
class DemoStore {
    var session by mutableStateOf<Session?>(null)
    var state by mutableStateOf(PersonaState())

    /** Needs you is folded (stays folded until unfolded; new items don't unfold it). */
    var isNeedsYouFolded by mutableStateOf(false)
    /** Time-of-day override for the demo menu (null = real time). */
    var hourOverride by mutableStateOf<Int?>(null)

    val persona: Persona? get() = (session as? Session.SignedIn)?.persona
    val isGuest: Boolean get() = session == Session.Guest

    val now: LocalDateTime get() = hourOverride?.let { LocalDateTime.now().withHour(it).withMinute(41).withSecond(0).withNano(0) } ?: LocalDateTime.now()
    val timeOfDay: TimeOfDay get() = TimeOfDay.of(now)

    // MARK: Session

    fun signIn(persona: Persona) {
        state = persona.makeState()
        isNeedsYouFolded = false
        session = Session.SignedIn(persona)
    }

    fun continueAsGuest() {
        state = Personas.guestState()
        session = Session.Guest
    }

    /**
     * Keeps [state] (Welcome never reads it; signIn and continueAsGuest replace it), so the app
     * fades out to Welcome as it was instead of flashing an empty Home.
     */
    fun signOut() {
        session = null
    }

    /** Restores the current persona's data to how it started. */
    fun resetDemo() {
        persona?.let { signIn(it) } ?: run { if (isGuest) continueAsGuest() }
    }

    // MARK: Needs you

    val payables: List<Payable> get() = state.needsYou.mapNotNull { it.payable }

    /** Total of everything payable in Needs you. */
    val payAllTotal: BigDecimal get() = payables.fold(BigDecimal.ZERO) { a, p -> a + p.amount }

    /** Pay all shows when 2 or more payments are due. */
    val showsPayAll: Boolean get() = payables.size >= 2

    /** "2 due this week" */
    val dueThisWeekCount: Int
        get() = state.needsYou.count { item -> item.due?.let { DemoDate.daysFromToday(it) in 0..6 } ?: false }

    /** The next thing due (for the folded Needs you line). */
    val nextDue: NeedsYouItem? get() = state.needsYou.filter { it.due != null }.minByOrNull { it.due!! }

    fun payAllRequest(): PaymentRequest {
        val items = payables
        return PaymentRequest(
            id = "pay-all-" + items.joinToString("-") { it.id },
            title = if (items.size == 2) "Pay both" else "Pay all ${items.size}",
            lines = items.map { PaymentLine(it.title, it.entity, it.amount, it.symbol, it.hue) },
            purpose = PaymentPurpose.Payables(items.map { it.id }),
        )
    }

    fun paymentRequest(payable: Payable) = PaymentRequest(
        id = "pay-${payable.id}",
        title = payable.title,
        lines = listOf(PaymentLine(payable.title, payable.entity, payable.amount, payable.symbol, payable.hue)),
        purpose = PaymentPurpose.Payables(listOf(payable.id)),
    )

    /** "Later" on a Needs you card: moves it to the end (it stays in Activity). */
    fun later(item: NeedsYouItem) {
        val rest = state.needsYou.filter { it.id != item.id }
        state = state.copy(needsYou = rest + item)
    }

    // MARK: Payments (fake — no money moves)

    /** Applies a confirmed payment and returns its receipt. */
    fun completePayment(request: PaymentRequest, method: PaymentMethod = PaymentMethod.GooglePay): Receipt {
        var receipt = Receipt(
            id = "r-" + UUID.randomUUID().toString().take(8),
            title = request.title, shortTitle = request.title, entity = "DubaiNow",
            amount = request.total, date = LocalDateTime.now(), method = method,
            reference = reference("DN"), symbol = "checkmark.circle.fill", hue = Hue.Mint,
            serviceID = null, doneByAssistant = request.doneByAssistant,
        )
        when (val p = request.purpose) {
            is PaymentPurpose.Payables -> {
                val paid = payables.filter { it.id in p.ids }
                state = state.copy(
                    needsYou = state.needsYou.filter { it.payable?.id !in p.ids },
                    vehicles = state.vehicles.map { v -> v.copy(fineIDs = v.fineIDs.filter { it !in p.ids }) },
                )
                receipt = if (paid.size == 1) {
                    val one = paid.first()
                    receipt.copy(
                        title = paidTitle(one), shortTitle = paidTitle(one), entity = one.entity, symbol = one.symbol,
                        hue = one.hue, serviceID = one.serviceID, reference = one.reference,
                        rows = listOf(ReceiptRow("For", one.detail)),
                    )
                } else {
                    receipt.copy(
                        title = "Paid ${paid.size} bills and fines", shortTitle = "Paid ${paid.size} bills and fines",
                        symbol = "creditcard.fill", hue = Hue.Sky, serviceID = "pay-all",
                        rows = paid.map { ReceiptRow("${it.entity} · ${it.title}", Money.aed(it.amount)) },
                    )
                }
            }
            is PaymentPurpose.SalikTopUp -> {
                state = state.copy(salikBalance = state.salikBalance + p.amount)
                receipt = receipt.copy(
                    title = "Topped up your Salik account", shortTitle = "Topped up Salik", entity = "Salik",
                    symbol = "road.lanes", hue = Hue.Sand, serviceID = "top-up-salik-account",
                    rows = listOf(ReceiptRow("New balance", Money.aed(state.salikBalance))),
                )
            }
            is PaymentPurpose.NolTopUp -> {
                val nb = (state.nolBalance ?: BigDecimal.ZERO) + p.amount
                state = state.copy(nolBalance = nb)
                receipt = receipt.copy(
                    title = "Topped up your nol card", shortTitle = "Topped up nol", entity = "RTA",
                    symbol = "tram.fill", hue = Hue.Sky, serviceID = "top-up-nol-card",
                    rows = listOf(ReceiptRow("New balance", Money.aed(nb))),
                )
            }
            is PaymentPurpose.ExtendParking -> {
                val rows = mutableListOf<ReceiptRow>()
                state.parking?.let { s ->
                    val base = if (s.endsAt.isAfter(LocalDateTime.now())) s.endsAt else LocalDateTime.now()
                    val updated = s.copy(endsAt = base.plusMinutes(p.minutes.toLong()))
                    state = state.copy(parking = updated)
                    rows += ReceiptRow("Zone", updated.zone)
                    rows += ReceiptRow("Now ends", DemoDate.time(updated.endsAt))
                }
                receipt = receipt.copy(
                    title = "Extended your parking", shortTitle = "Extended parking", entity = "Parkin",
                    symbol = "p.square.fill", hue = Hue.Sky, serviceID = "pay-for-parking", rows = rows,
                )
            }
            is PaymentPurpose.RequestFees -> {
                advanceRequest(p.requestID)
                val r = request(p.requestID)
                receipt = receipt.copy(
                    title = r?.let { "Paid the fees · ${it.title}" } ?: "Paid the fees for your request",
                    shortTitle = r?.let { "Paid the fees · ${it.title}" } ?: "Paid request fees",
                    entity = r?.entity ?: receipt.entity, symbol = r?.symbol ?: receipt.symbol,
                    hue = r?.hue ?: receipt.hue, serviceID = r?.serviceID,
                )
            }
            is PaymentPurpose.VehicleRenewal -> {
                state = state.copy(
                    needsYou = state.needsYou.filter { item ->
                        if (item.payable?.id in p.fineIDs) return@filter false
                        val k = item.kind
                        !(k is NeedsYouItem.Kind.Renewal && k.serviceID == "renew-vehicle-registration" && k.thingID == p.vehicleID)
                    },
                    vehicles = state.vehicles.map { v ->
                        if (v.id == p.vehicleID) v.copy(fineIDs = emptyList(), registrationExpiry = v.registrationExpiry.plusYears(1)) else v
                    },
                )
                val v = state.vehicles.firstOrNull { it.id == p.vehicleID }
                val rows = mutableListOf(
                    ReceiptRow("Vehicle", v?.let { "${it.shortName} · ${it.plate.code} ${it.plate.number}" } ?: ""),
                    ReceiptRow("Registration fee", Money.aed(p.fee)),
                )
                if (p.fineIDs.isNotEmpty()) rows += ReceiptRow("Fines cleared", p.fineIDs.size.toString())
                v?.let { rows += ReceiptRow("Valid until", DemoDate.full(it.registrationExpiry)) }
                receipt = receipt.copy(
                    title = "Renewed your vehicle registration", shortTitle = "Renewed registration", entity = "RTA",
                    symbol = "car.fill", hue = Hue.Sky, serviceID = "renew-vehicle-registration", rows = rows,
                )
            }
            is PaymentPurpose.Donation -> {
                receipt = receipt.copy(
                    title = "Donated to ${p.charity}", shortTitle = "Donated to ${p.charity}", entity = p.charity,
                    symbol = "heart.fill", hue = Hue.Lilac, serviceID = "donate-to-charity",
                )
            }
        }
        state = state.copy(history = listOf(receipt) + state.history)
        return receipt
    }

    fun receipt(id: String): Receipt? = state.history.firstOrNull { it.id == id }

    fun rate(receiptID: String, face: HappinessFace) {
        state = state.copy(history = state.history.map { if (it.id == receiptID) it.copy(rating = face) else it })
    }

    // MARK: Requests

    fun request(id: String): RequestItem? = state.requests.firstOrNull { it.id == id }

    /** Completes the current step (e.g. passport uploaded) and moves to the next. */
    fun advanceRequest(id: String) {
        val r = request(id) ?: return
        val steps = r.steps.toMutableList()
        r.currentStepIndex?.let { c ->
            steps[c] = steps[c].copy(state = RequestStep.State.Done)
            if (c + 1 < steps.size) steps[c + 1] = steps[c + 1].copy(state = RequestStep.State.Current)
        }
        val next = steps.indexOfFirst { it.state == RequestStep.State.Current }
        val needsPayment = next >= 0 && steps[next].title.lowercase().startsWith("pay")
        val updated = r.copy(
            steps = steps, notice = null,
            status = if (needsPayment) RequestItem.Status.YourTurn(DemoDate.day(3)) else RequestItem.Status.InReview(DemoDate.day(2)),
            action = if (needsPayment) "Pay the fees" else null,
        )
        state = state.copy(
            requests = state.requests.map { if (it.id == id) updated else it },
            needsYou = state.needsYou.filterNot { (it.kind as? NeedsYouItem.Kind.RequestStep)?.requestID == id },
        )
    }

    /** Records a request sent without a payment (e.g. a report the assistant submitted). */
    fun addRequest(request: RequestItem) {
        state = state.copy(requests = listOf(request) + state.requests.filter { it.id != request.id })
    }

    // MARK: Favorites (up to 8)

    fun isFavorite(serviceID: String): Boolean = state.favorites.any {
        (it.target as? FavoriteItem.Target.Service)?.id == serviceID || (it.target as? FavoriteItem.Target.Thing)?.id == serviceID
    }

    /** Adds or removes a catalogue record as a favorite. Returns false when full (8 of 8). */
    fun toggleFavorite(service: CatalogueService): Boolean {
        if (isFavorite(service.id)) {
            state = state.copy(favorites = state.favorites.filterNot {
                (it.target as? FavoriteItem.Target.Service)?.id == service.id || (it.target as? FavoriteItem.Target.Thing)?.id == service.id
            })
            return true
        }
        if (state.favorites.size >= FAVORITES_LIMIT) return false
        val item = FavoriteItem(
            id = "fav-${service.id}", title = service.short, symbol = service.symbol, hue = service.hue,
            target = if (service.kind == "thing") FavoriteItem.Target.Thing(service.id) else FavoriteItem.Target.Service(service.id),
        )
        state = state.copy(favorites = state.favorites + item)
        return true
    }

    fun removeFavorite(item: FavoriteItem) { state = state.copy(favorites = state.favorites.filter { it.id != item.id }) }

    fun moveFavorite(from: Int, to: Int) {
        val l = state.favorites.toMutableList(); if (from !in l.indices) return
        val x = l.removeAt(from); l.add(to.coerceIn(0, l.size), x); state = state.copy(favorites = l)
    }

    fun setFavorites(items: List<FavoriteItem>) { state = state.copy(favorites = items) }

    // MARK: Widgets

    fun addWidget(kind: WidgetKind, size: WidgetSize, at: Int? = null): HomeWidget {
        val w = HomeWidget("w-" + UUID.randomUUID().toString().take(6), kind, size)
        val l = state.widgets.toMutableList()
        if (at != null) l.add(at.coerceIn(0, l.size), w) else l.add(w)
        state = state.copy(widgets = l, widgetSuggestion = if (state.widgetSuggestion == kind) null else state.widgetSuggestion)
        return w
    }

    fun removeWidget(widget: HomeWidget) { state = state.copy(widgets = state.widgets.filter { it.id != widget.id }) }

    fun restoreWidget(widget: HomeWidget, at: Int) {
        val l = state.widgets.filter { it.id != widget.id }.toMutableList(); l.add(at.coerceIn(0, l.size), widget)
        state = state.copy(widgets = l)
    }

    fun resizeWidget(widget: HomeWidget, size: WidgetSize) {
        state = state.copy(widgets = state.widgets.map { if (it.id == widget.id) it.copy(size = size) else it })
    }

    fun moveWidget(from: Int, to: Int) {
        val l = state.widgets.toMutableList(); if (from !in l.indices) return
        val x = l.removeAt(from); l.add(to.coerceIn(0, l.size), x); state = state.copy(widgets = l)
    }

    fun hasWidget(kind: WidgetKind) = state.widgets.any { it.kind == kind }

    /** "Not now" hides a suggestion for 30 days. */
    fun dismissSuggestion() { state = state.copy(widgetSuggestion = null) }

    // MARK: Notifications

    val unreadCount: Int get() = state.notifications.count { !it.isRead }
    fun markAllRead() { state = state.copy(notifications = state.notifications.map { it.copy(isRead = true) }) }
    fun markRead(id: String, read: Boolean = true) {
        state = state.copy(notifications = state.notifications.map { if (it.id == id) it.copy(isRead = read) else it })
    }

    // MARK: Drafts

    fun deleteDraft(draft: DraftItem) { state = state.copy(drafts = state.drafts.filter { it.id != draft.id }) }

    companion object {
        const val FAVORITES_LIMIT = 8

        fun reference(prefix: String): String {
            val d = DemoDate.short(LocalDateTime.now()).replace(" ", "").uppercase()
            return "$prefix-$d-${Random.nextInt(1_000_000, 9_999_999)}"
        }

        private fun paidTitle(p: Payable) = when (p.kind) {
            Payable.Kind.Bill -> "Paid your ${p.title.lowercase()}"
            Payable.Kind.Fine -> "Paid a ${p.title.lowercase()}"
            Payable.Kind.Fee -> "Paid ${p.title.lowercase()}"
            Payable.Kind.TopUp -> "Topped up ${p.title}"
        }
    }
}
