package com.salehtl.dubainownext.features.services

import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.app.Router
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.CatalogueSubtopic
import com.salehtl.dubainownext.data.CatalogueTopic
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.DraftItem
import com.salehtl.dubainownext.data.JourneyProgress
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.NeedsYouItem
import com.salehtl.dubainownext.data.Payable
import com.salehtl.dubainownext.data.PaymentLine
import com.salehtl.dubainownext.data.PaymentPurpose
import com.salehtl.dubainownext.data.PaymentRequest
import com.salehtl.dubainownext.data.Plate
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.data.ServiceType
import com.salehtl.dubainownext.data.Vehicle
import com.salehtl.dubainownext.data.hue
import com.salehtl.dubainownext.data.symbol
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.ChipTone
import com.salehtl.dubainownext.design.components.ComingSoon
import java.math.BigDecimal
import java.time.LocalDateTime

// Port of ios/…/Features/Services/ServicesData.swift. The lists every Services screen shows;
// screens and the reachability check (ServicesReachability) read the same functions, so
// "every record is reachable" is checked against what's actually on screen.

internal object ServicesData {
    private val catalogue: Catalogue get() = Catalogue.shared

    // MARK: Topics

    /** Sub-topics of a topic that have at least one record, in catalogue order. */
    fun subtopics(topic: CatalogueTopic): List<CatalogueSubtopic> =
        topic.subtopics.filter { services(topic.id, it.id).isNotEmpty() }

    /** Everything in a sub-topic: standalone services (most used first), then its bundles and journeys. */
    fun services(topic: String, subtopic: String): List<CatalogueService> {
        val all = catalogue.services.filter { it.topic == topic && it.subtopic == subtopic }
        return all.filter { it.type == ServiceType.Standalone }.sortedByDescending { it.uniques30d } +
            all.filter { it.type != ServiceType.Standalone }.sortedBy { it.name }
    }

    /** The rows a topic page shows for a sub-topic before "See all": the 3 most used standalone services. */
    fun preview(topic: String, subtopic: String, limit: Int = 3): List<CatalogueService> {
        val all = services(topic, subtopic)
        val standalone = all.filter { it.type == ServiceType.Standalone }
        return (standalone.ifEmpty { all }).take(limit)
    }

    /** Bundles and journeys that belong to a topic (journeys first, as cards). */
    fun bundlesAndJourneys(topic: String): List<CatalogueService> {
        val multi = catalogue.services.filter { it.topic == topic && it.type != ServiceType.Standalone }
        return multi.filter { it.type == ServiceType.Journey } + multi.filter { it.type == ServiceType.Bundle }
    }

    /** Standalone services in a topic whose sub-topic isn't listed (a guard: none today). */
    fun unfiled(topic: CatalogueTopic): List<CatalogueService> {
        val known = topic.subtopics.map { it.id }.toSet()
        return catalogue.services.filter { it.topic == topic.id && it.type == ServiceType.Standalone && (it.subtopic ?: "") !in known }
    }

    /** "38 services from RTA, Dubai Police, Parkin and 6 others" */
    fun topicLine(topic: CatalogueTopic): String {
        val counts = mutableMapOf<String, Int>()
        catalogue.services.filter { it.topic == topic.id && it.type == ServiceType.Standalone }.forEach { s ->
            val o = s.owner
            if (o != null && o != "DubaiNow") counts[o] = (counts[o] ?: 0) + 1
        }
        val owners = counts.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).map { it.key }
        val n = "${topic.services} ${if (topic.services == 1) "service" else "services"}"
        return when (owners.size) {
            0 -> n
            1 -> "$n from ${owners[0]}"
            in 2..4 -> "$n from ${owners.dropLast(1).joinToString(", ")} and ${owners.last()}"
            else -> "$n from ${owners.take(3).joinToString(", ")} and ${owners.size - 3} others"
        }
    }

    // MARK: A to Z

    enum class Filter(val label: String) {
        All("All"), Standalone("Standalone"), Bundle("All at once"), Journey("Start to finish");

        companion object {
            fun parse(s: String?): Filter? = entries.firstOrNull { it.name.equals(s, ignoreCase = true) }
        }
    }

    fun aToZ(filter: Filter): List<CatalogueService> {
        val all = catalogue.aToZ
        return when (filter) {
            Filter.All -> all
            Filter.Standalone -> all.filter { it.type == ServiceType.Standalone }
            Filter.Bundle -> all.filter { it.type == ServiceType.Bundle }
            Filter.Journey -> all.filter { it.type == ServiceType.Journey }
        }
    }

    data class LetterSection(val letter: String, val services: List<CatalogueService>)

    fun letterSections(services: List<CatalogueService>): List<LetterSection> {
        val order = mutableListOf<String>()
        val groups = mutableMapOf<String, MutableList<CatalogueService>>()
        for (s in services) {
            val first = s.name.firstOrNull()?.uppercaseChar()
            val letter = if (first == null || !first.isLetter()) "#" else first.toString()
            if (letter !in groups) order += letter
            groups.getOrPut(letter) { mutableListOf() } += s
        }
        return order.map { LetterSection(it, groups[it].orEmpty()) }
    }

    /** Name, alias or owner contains the query (A to Z's own search). */
    fun matches(s: CatalogueService, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return s.name.contains(q, ignoreCase = true) || s.aliases.any { it.contains(q, ignoreCase = true) } ||
            (s.owner ?: "").contains(q, ignoreCase = true)
    }

    // MARK: Entities

    data class Owner(
        /** The owner value in the catalogue ("RTA", "DubaiNow", "To confirm"). */
        val key: String,
        val label: String,
        val count: Int,
        val detail: String?,
    )

    /**
     * Owners as listed on "By government entity": DubaiNow (bundles, journeys and its own services)
     * first, then every entity A to Z, then services whose owner is still to confirm.
     */
    val owners: List<Owner> by lazy {
        val named = catalogue.owners.filter { it.first != "To confirm" }
            .map { (name, count) -> Owner(name, name, count, fullName(name).takeIf { it != name }) }
        val dubaiNow = catalogue.services(ownedBy = "DubaiNow").size
        val unconfirmed = catalogue.services(ownedBy = "To confirm").size
        buildList {
            if (dubaiNow > 0) add(Owner("DubaiNow", "DubaiNow", dubaiNow, "Bundles, journeys and DubaiNow's own services"))
            addAll(named)
            if (unconfirmed > 0) add(Owner("To confirm", "Owner to confirm", unconfirmed, "Being checked with government"))
        }
    }

    /** Government entities and providers that run services (DubaiNow included, "to confirm" not). */
    val entityCount: Int get() = owners.count { it.key != "To confirm" }

    fun services(ownedBy: String): List<CatalogueService> = catalogue.services(ownedBy = ownedBy)

    /** The owner's full name for footers ("Roads and Transport Authority (RTA)"). */
    fun fullName(owner: String?): String {
        if (owner == null) return "an owner still to be confirmed"
        val names = mapOf(
            "RTA" to "Roads and Transport Authority (RTA)",
            "DEWA" to "Dubai Electricity and Water Authority (DEWA)",
            "GDRFA" to "General Directorate of Residency and Foreign Affairs (GDRFA)",
            "DHA" to "Dubai Health Authority (DHA)",
            "DLD" to "Dubai Land Department (DLD)",
            "KHDA" to "Knowledge and Human Development Authority (KHDA)",
            "CDA" to "Community Development Authority (CDA)",
            "IACAD" to "Islamic Affairs and Charitable Activities Department (IACAD)",
            "MBRHE" to "Mohammed Bin Rashid Housing Establishment (MBRHE)",
            "ICP" to "Federal Authority for Identity, Citizenship, Customs and Port Security (ICP)",
            "DET" to "Department of Economy and Tourism (DET)",
        )
        return names[owner] ?: owner
    }

    // MARK: Service pages

    /** Other services in the same sub-topic, most used first. */
    fun related(service: CatalogueService, limit: Int = 4): List<CatalogueService> {
        val t = service.topic ?: return emptyList()
        val sub = service.subtopic ?: return emptyList()
        return services(t, sub).filter { it.id != service.id && it.type == ServiceType.Standalone }.take(limit)
    }

    /** Bundles and journeys that include this service as a part. */
    fun partOf(service: CatalogueService): List<CatalogueService> =
        catalogue.services.filter { (it.parts ?: emptyList()).contains(service.id) }

    /** Aliases worth showing in English ("Mulkiya, vehicle renewal"). */
    fun alsoCalled(service: CatalogueService): String? {
        val latin = service.aliases.filter { alias ->
            alias.none { it.code in 0x0600..0x06FF } &&
                !alias.equals(service.name, ignoreCase = true) && !alias.equals(service.short, ignoreCase = true)
        }
        return latin.takeIf { it.isNotEmpty() }?.take(3)?.joinToString(", ")
    }

    /** The right "coming next" sheet for a record, by its status. */
    fun comingSoon(service: CatalogueService, kind: ComingSoon.Kind? = null, detail: String? = null): ComingSoon {
        val k = kind ?: when {
            service.status == "maintenance" -> ComingSoon.Kind.Maintenance
            service.isLive -> ComingSoon.Kind.ServicesPhase
            else -> ComingSoon.Kind.NotLiveYet
        }
        return ComingSoon(
            title = service.name,
            detail = detail,
            owner = if (service.type == ServiceType.Standalone) service.ownerLabel else null,
            symbol = if (service.type == ServiceType.Journey) "point.topleft.down.to.point.bottomright.curvepath" else service.symbol,
            hue = service.hue,
            kind = k,
        )
    }
}

// MARK: - Personal context (from the persona's state)

/** A short personal note on a catalogue row ("you have 1 unpaid", "due in 12 days"). */
internal data class ServicesRowContext(val text: String, val tone: ChipTone)

/** Something the resident can do next, shown on Services (For you) and topic banners. */
internal sealed interface ServicesAction {
    data class Push(val route: Route) : ServicesAction
    data class Pay(val request: PaymentRequest) : ServicesAction
    data class Soon(val item: ComingSoon) : ServicesAction
}

internal data class ServicesForYouItem(
    val id: String,
    val symbol: String,
    val hue: Hue,
    val chip: String,
    val chipTone: ChipTone,
    val title: String,
    val detail: String,
    val action: ServicesAction,
)

/** The personal banner at the top of a topic page (Driving: the Patrol's plate + Renew). */
internal data class ServicesBanner(
    val plate: Plate?,
    val symbol: String,
    val hue: Hue,
    val title: String,
    val detail: String,
    val detailTone: ChipTone,
    val actionTitle: String,
    val action: ServicesAction,
)

/** Performs a Services action (push, pay or a "coming next" sheet). */
internal fun Router.servicesPerform(action: ServicesAction) {
    when (action) {
        is ServicesAction.Push -> push(action.route)
        is ServicesAction.Pay -> present(SheetRoute.Payment(action.request))
        is ServicesAction.Soon -> present(SheetRoute.ComingSoonSheet(action.item))
    }
}

// Everything below only reads existing store state, so it can be promoted to DemoStore later.

internal fun DemoStore.servicesPayables(serviceID: String): List<Payable> = payables.filter { it.serviceID == serviceID }

/** Requests that are this service (the Salik dispute is filed under top-ups, so names must match). */
internal fun DemoStore.servicesRequests(service: CatalogueService): List<RequestItem> {
    if (service.id == "check-visa-status") {
        return state.requests.filter { r ->
            Catalogue.service(r.serviceID)?.topic == "visas-and-residency" && r.status != RequestItem.Status.Done
        }
    }
    return state.requests.filter { it.serviceID == service.id && it.title == service.name }
}

internal fun DemoStore.servicesDraft(serviceID: String): DraftItem? = state.drafts.firstOrNull { it.serviceID == serviceID }

internal fun DemoStore.servicesJourney(journeyID: String): JourneyProgress? = state.journeys.firstOrNull { it.journeyID == journeyID }

/** A renewal waiting in Needs you for this service (e.g. the Patrol's registration). */
internal fun DemoStore.servicesRenewal(serviceID: String): NeedsYouItem? =
    state.needsYou.firstOrNull { (it.kind as? NeedsYouItem.Kind.Renewal)?.serviceID == serviceID }

/** The vehicle a renewal refers to, else the one whose registration ends first. */
internal fun DemoStore.servicesVehicleForRenewal(): Vehicle? {
    val thing = (servicesRenewal("renew-vehicle-registration")?.kind as? NeedsYouItem.Kind.Renewal)?.thingID
    thing?.let { id -> state.vehicles.firstOrNull { it.id == id }?.let { return it } }
    return state.vehicles.minByOrNull { it.registrationExpiry }
}

private fun sum(items: List<Payable>): BigDecimal = items.fold(BigDecimal.ZERO) { acc, p -> acc + p.amount }

/** Personal note for a row, from the persona's bills, renewals, requests and drafts. */
internal fun DemoStore.servicesContext(service: CatalogueService): ServicesRowContext? {
    if (persona == null) return null
    val owed = servicesPayables(service.id)
    if (owed.isNotEmpty()) {
        if (owed.all { it.kind == Payable.Kind.Fine }) return ServicesRowContext("you have ${owed.size} unpaid", ChipTone.Error)
        val total = sum(owed)
        owed.mapNotNull { it.due }.minOrNull()?.let { due ->
            return ServicesRowContext("${Money.aed(total)} ${DemoDate.dueLabel(due).lowercase()}", ChipTone.Warning)
        }
        return ServicesRowContext("${Money.aed(total)} to pay", ChipTone.Warning)
    }
    servicesRenewal(service.id)?.due?.let { return ServicesRowContext(DemoDate.dueLabel(it).lowercase(), ChipTone.Warning) }
    servicesRequests(service).firstOrNull()?.let { r ->
        when (r.status) {
            is RequestItem.Status.YourTurn -> return ServicesRowContext("your turn", ChipTone.Warning)
            is RequestItem.Status.InReview -> return ServicesRowContext("in review", ChipTone.Info)
            is RequestItem.Status.Approved -> return ServicesRowContext("approved", ChipTone.Success)
            RequestItem.Status.Done -> Unit
        }
    }
    servicesDraft(service.id)?.let { return ServicesRowContext("draft, ${it.stepsDone} of ${it.stepsTotal} done", ChipTone.Info) }
    when (service.id) {
        "pay-for-parking" -> state.parking?.let { p ->
            if (p.endsAt.isAfter(LocalDateTime.now())) return ServicesRowContext("parked, ends ${DemoDate.countdown(p.endsAt)}", ChipTone.Info)
        }
        "top-up-salik-account" -> if (state.vehicles.isNotEmpty() && state.salikBalance < BigDecimal(50)) {
            return ServicesRowContext("${Money.aed(state.salikBalance, trimWhole = true)} left", ChipTone.Warning)
        }
    }
    return null
}

/** The personal count for a bundle's count line (null when there's nothing personal to say). */
internal fun DemoStore.servicesBundleCount(service: CatalogueService): Int? {
    if (persona == null) return null
    return when (service.id) {
        "pay-all" -> payables.size.takeIf { it > 0 }
        "renew-family-visas" -> state.familyVisas?.dueMemberIDs?.size?.takeIf { it > 0 }
        else -> null
    }
}

/** "For you" on Services: renewals, your-turn requests, journeys, family visas, drafts, other requests. */
internal fun DemoStore.servicesForYou(): List<ServicesForYouItem> {
    if (persona == null) return emptyList()
    val items = mutableListOf<ServicesForYouItem>()
    val cat = Catalogue.shared

    for (item in state.needsYou) {
        val k = item.kind as? NeedsYouItem.Kind.Renewal ?: continue
        val s = cat.service(k.serviceID) ?: continue
        val vehicle = state.vehicles.firstOrNull { it.id == k.thingID }
        items += ServicesForYouItem(
            "fy-${item.id}", s.symbol, s.hue,
            item.due?.let { DemoDate.dueLabel(it) } ?: "Due soon", ChipTone.Warning,
            s.name, vehicle?.let { "${it.shortName} · ${it.plate.code} ${it.plate.number}" } ?: item.detail,
            ServicesAction.Push(Route.Service(k.serviceID)),
        )
    }
    val yourTurn = state.requests.filter { it.status is RequestItem.Status.YourTurn }
    for (r in yourTurn) {
        val step = (r.currentStepIndex ?: 0) + 1
        items += ServicesForYouItem(
            "fy-${r.id}", r.symbol, r.hue, "$step of ${r.steps.size} steps", ChipTone.Info,
            r.title, "Your turn · continue where you left off", ServicesAction.Push(Route.Request(r.id)),
        )
    }
    for (j in state.journeys) {
        val s = cat.service(j.journeyID) ?: continue
        val total = s.stages?.size ?: 0
        items += ServicesForYouItem(
            "fy-${j.journeyID}", s.symbol, s.hue, "Stage ${j.stageIndex + 1} of $total", ChipTone.Info,
            s.name, listOfNotNull(j.headline, j.destination).joinToString(" · "), ServicesAction.Push(Route.Journey(j.journeyID)),
        )
    }
    val fv = state.familyVisas
    val fvService = cat.service("renew-family-visas")
    if (fv != null && fv.dueMemberIDs.isNotEmpty() && fvService != null) {
        val names = state.family.filter { it.id in fv.dueMemberIDs }.map { it.name.split(" ").first() }
        items += ServicesForYouItem(
            "fy-family-visas", fvService.symbol, fvService.hue, "${fv.dueMemberIDs.size} of ${fv.memberIDs.size} due", ChipTone.Warning,
            fvService.name, "${ServicesText.list(names)} · expire ${DemoDate.short(fv.expiresOn)}", ServicesAction.Push(Route.Bundle(fvService.id)),
        )
    }
    for (d in state.drafts) {
        items += ServicesForYouItem(
            "fy-${d.id}", d.symbol, d.hue, "Draft · ${d.stepsDone} of ${d.stepsTotal}", ChipTone.Neutral,
            cat.service(d.serviceID)?.name ?: d.title, "Saved ${ServicesText.relativeInSentence(d.savedOn)}",
            ServicesAction.Push(Route.Service(d.serviceID)),
        )
    }
    for (r in state.requests) {
        if (r in yourTurn) continue
        val (chip, detail) = when (val st = r.status) {
            is RequestItem.Status.InReview -> ("In review" to ChipTone.Info) to "Answer by ${DemoDate.short(st.answerBy)}"
            is RequestItem.Status.Approved -> ("Approved" to ChipTone.Success) to st.note.replaceFirstChar { it.uppercase() }
            else -> continue
        }
        items += ServicesForYouItem("fy-${r.id}", r.symbol, r.hue, chip.first, chip.second, r.title, detail, ServicesAction.Push(Route.Request(r.id)))
    }
    return items.take(6)
}

/**
 * The personal banner on a topic page: a renewal first, then a step that's your turn,
 * then a bill or fine (soonest first), then a journey in progress.
 */
internal fun DemoStore.servicesBanner(topicID: String): ServicesBanner? {
    if (persona == null) return null
    val cat = Catalogue.shared
    fun inTopic(id: String?): Boolean = id?.let { cat.service(it)?.topic } == topicID

    for (item in state.needsYou) {
        val k = item.kind as? NeedsYouItem.Kind.Renewal ?: continue
        if (!inTopic(k.serviceID)) continue
        val vehicle = state.vehicles.firstOrNull { it.id == k.thingID }
        return ServicesBanner(
            vehicle?.plate, item.symbol, item.hue,
            if (vehicle == null) item.title else "Registration due",
            item.due?.let { ServicesText.inDays(it) } ?: item.detail, ChipTone.Warning,
            item.primaryAction, ServicesAction.Push(Route.Service(k.serviceID)),
        )
    }
    for (item in state.needsYou) {
        val k = item.kind as? NeedsYouItem.Kind.RequestStep ?: continue
        val r = request(k.requestID) ?: continue
        if (!inTopic(r.serviceID)) continue
        return ServicesBanner(
            null, item.symbol, item.hue, item.title,
            item.due?.let { "${r.title} · ${DemoDate.dueLabel(it).lowercase()}" } ?: r.title,
            ChipTone.Warning, item.primaryAction, ServicesAction.Push(Route.Request(k.requestID)),
        )
    }
    val owed = payables.filter { inTopic(it.serviceID) }.sortedBy { it.due ?: LocalDateTime.MAX }
    owed.firstOrNull()?.let { p ->
        return ServicesBanner(
            null, p.symbol, p.hue, p.title,
            p.due?.let { "${Money.aed(p.amount)} · ${DemoDate.dueLabel(it).lowercase()}" } ?: "${Money.aed(p.amount)} · ${p.note ?: "unpaid"}",
            if (p.kind == Payable.Kind.Fine) ChipTone.Error else ChipTone.Warning,
            "Pay", ServicesAction.Pay(paymentRequest(p)),
        )
    }
    state.journeys.firstOrNull { cat.service(it.journeyID)?.topic == topicID }?.let { j ->
        val s = cat.service(j.journeyID) ?: return null
        return ServicesBanner(
            null, s.symbol, s.hue, s.name, "Stage ${j.stageIndex + 1} of ${s.stages?.size ?: 0} · ${j.headline}", ChipTone.Info,
            "Continue", ServicesAction.Push(Route.Journey(s.id)),
        )
    }
    return null
}

/** One payment for every payable that belongs to a service. */
internal fun DemoStore.servicesPaymentRequest(owed: List<Payable>): PaymentRequest? {
    val first = owed.firstOrNull() ?: return null
    if (owed.size == 1) return paymentRequest(first)
    return PaymentRequest(
        id = "pay-" + owed.joinToString("-") { it.id },
        title = "Pay ${owed.size} ${if (first.kind == Payable.Kind.Fine) "fines" else "bills"}",
        lines = owed.map { PaymentLine(it.title, it.entity, it.amount, it.symbol, it.hue) },
        purpose = PaymentPurpose.Payables(owed.map { it.id }),
    )
}

// MARK: - Text helpers

internal object ServicesText {
    /** "Layla and Omar", "Ahmed, Layla and Omar" */
    fun list(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        else -> items.dropLast(1).joinToString(", ") + " and " + items.last()
    }

    /** "in 12 days", "tomorrow", "today", "3 days ago" */
    fun inDays(date: LocalDateTime): String = when (val n = DemoDate.daysFromToday(date)) {
        in Int.MIN_VALUE..-2 -> "${-n} days ago"
        -1 -> "yesterday"
        0 -> "today"
        1 -> "tomorrow"
        else -> "in $n days"
    }

    /** "today", "yesterday", "3 days ago", "on 25 Sep" — for use mid-sentence. */
    fun relativeInSentence(date: LocalDateTime): String {
        val r = DemoDate.relative(date)
        return if (DemoDate.daysFromToday(date) in -6..0) r.lowercase() else "on $r"
    }

    fun count(n: Int, singular: String, plural: String? = null): String = "$n ${if (n == 1) singular else (plural ?: singular + "s")}"
}
