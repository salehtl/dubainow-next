package com.salehtl.dubainownext.features.search

import com.salehtl.dubainownext.app.Route
import com.salehtl.dubainownext.app.SheetRoute
import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.CatalogueService
import com.salehtl.dubainownext.data.DemoDate
import com.salehtl.dubainownext.data.DemoStore
import com.salehtl.dubainownext.data.DocumentItem
import com.salehtl.dubainownext.data.Money
import com.salehtl.dubainownext.data.NeedsYouItem
import com.salehtl.dubainownext.data.Payable
import com.salehtl.dubainownext.data.Plate
import com.salehtl.dubainownext.data.RequestItem
import com.salehtl.dubainownext.data.WidgetKind
import com.salehtl.dubainownext.data.hue
import com.salehtl.dubainownext.data.symbol
import com.salehtl.dubainownext.design.Hue
import com.salehtl.dubainownext.design.components.ChipTone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDateTime

// What Search shows for a query, in the Paper order (04 · Search · "How results work"):
// 1 Direct answer · 2 Top result · 3 Services (at most 3, then See all) · 4 Your things ·
// 5 Widgets you can add, then help and guides. Ported from SearchModel.swift; all of it reads
// DemoStore's public API only.

// MARK: - The resident's own things

internal sealed interface SearchLeading {
    data class Icon(val symbol: String, val hue: Hue) : SearchLeading
    data class PlateMark(val plate: Plate) : SearchLeading
    data class Initials(val initials: String, val hue: Hue) : SearchLeading
}

internal sealed interface SearchAction {
    data class Push(val route: Route) : SearchAction
    data class Sheet(val sheet: SheetRoute) : SearchAction
    data class Pay(val payable: Payable) : SearchAction
    data class Service(val id: String) : SearchAction
}

/** The compact Top result card: dark disc · name · "Balance AED 18 · low" · one action. */
internal data class SearchCard(
    val symbol: String,
    val title: String,
    val label: String,
    val value: String,
    val note: String? = null,
    val noteTone: ChipTone = ChipTone.Neutral,
    val button: String,
    val buttonAction: SearchAction,
)

/**
 * Something of the resident's that search can find: an account, a vehicle, a document,
 * a request, a bill. Things with a [card] can be the Top result, with one useful action.
 */
internal data class SearchThing(
    val id: String,
    val title: String,
    val subtitle: String,
    val tone: ChipTone = ChipTone.Neutral,
    val leading: SearchLeading,
    val keywords: List<String>,
    val action: SearchAction,
    val card: SearchCard? = null,
    /** The direct answer that already shows this thing (so it isn't listed twice). */
    val coveredBy: SearchIntent? = null,
    val priority: Double = 0.0,
)

// MARK: - Direct answers

/** Questions the demo can answer from the resident's own data. */
internal sealed interface SearchIntent {
    data object Owe : SearchIntent
    data class Request(val id: String) : SearchIntent
    data object SalikBalance : SearchIntent
    data object NolBalance : SearchIntent
    /** A named prayer, or the next one. */
    data class Prayer(val name: String?) : SearchIntent
    data object Parking : SearchIntent

    val isPersonal: Boolean get() = this !is Prayer
}

internal object SearchIntentDetector {
    private val prayerNames = mapOf(
        "fajr" to "Fajr", "dhuhr" to "Dhuhr", "zuhr" to "Dhuhr", "duhr" to "Dhuhr", "dhuhur" to "Dhuhr",
        "asr" to "Asr", "maghrib" to "Maghrib", "magrib" to "Maghrib", "isha" to "Isha", "ishaa" to "Isha",
    )
    private val prayerWords = setOf("prayer", "prayers", "salah", "salat", "namaz", "adhan", "azan", "athan")
    private val statusWords = setOf(
        "status", "when", "done", "ready", "progress", "track", "tracking", "renewal", "renewing", "expire", "expires",
        "expiry", "application", "arrive", "arriving", "update", "happening", "approved", "card",
    )
    private val oweWords = setOf("owe", "owed", "owing", "outstanding", "dues", "unpaid", "debt")
    private val billWords = setOf("bill", "bills", "fine", "fines")
    private val billsOnly = setOf("bill", "bills", "fine", "fines", "my", "pay", "all", "total", "and", "the", "what", "are", "show", "me", "i", "have")
    private val balanceWords = setOf("balance", "much", "left", "credit", "money")
    private val parkingWords = setOf("parking", "park", "parked")
    private val parkingOnly = setOf(
        "parking", "park", "my", "time", "left", "how", "long", "much", "session", "when", "does", "do", "end", "ends",
        "expire", "extend", "is", "the", "i", "have", "remaining", "current", "car", "parked",
    )

    /** [words] are all the normalised words, stop words included. */
    fun detect(words: List<String>, requests: List<RequestItem>): SearchIntent? {
        val set = words.toSet()
        if (set.isEmpty()) return null

        words.firstNotNullOfOrNull { prayerNames[it] }?.let { return SearchIntent.Prayer(it) }
        if (set.any { it in prayerWords }) return SearchIntent.Prayer(null)

        if (set.any { it in oweWords }) return SearchIntent.Owe
        if (billsOnly.containsAll(set) && set.any { it in billWords }) return SearchIntent.Owe

        if ("salik" in set && set.any { it in balanceWords }) return SearchIntent.SalikBalance
        if ("nol" in set && set.any { it in balanceWords }) return SearchIntent.NolBalance
        if (set == setOf("balance") || set == setOf("my", "balance")) return SearchIntent.SalikBalance

        if (set.any { it in parkingWords } && parkingOnly.containsAll(set)) return SearchIntent.Parking

        if (set.any { it in statusWords }) {
            for (r in requests) if (set.any { it in requestKeywords(r) }) return SearchIntent.Request(r.id)
        }
        return null
    }

    fun requestKeywords(r: RequestItem): Set<String> {
        val keys = SearchText.tokens(r.title + " " + r.entity).toMutableSet()
        keys.removeAll(SearchText.stopWords)
        keys.removeAll(setOf("renew", "apply", "get", "check"))
        if ("visa" in keys || "residence" in keys) keys.addAll(listOf("visa", "residency", "iqama", "amer"))
        if ("licence" in keys) keys.addAll(listOf("license", "licence"))
        return keys
    }
}

// MARK: - Results

internal data class SearchResults(
    val analysis: SearchQueryAnalysis,
    val intent: SearchIntent? = null,
    /** True when a guest asked something personal: show "Sign in to see answers…". */
    val needsSignIn: Boolean = false,
    val topThing: SearchThing? = null,
    val topService: SearchMatch? = null,
    val services: List<SearchMatch> = emptyList(),
    val servicesTitle: String = "Services",
    val things: List<SearchThing> = emptyList(),
    val widgets: List<WidgetKind> = emptyList(),
    val guides: List<SearchMatch> = emptyList(),
) {
    val hasAnswer: Boolean get() = intent != null || needsSignIn
    val isEmpty: Boolean
        get() = !hasAnswer && topThing == null && topService == null && services.isEmpty() && things.isEmpty() && widgets.isEmpty() && guides.isEmpty()

    /** For TalkBack: "6 results". */
    val count: Int
        get() = (if (hasAnswer) 1 else 0) + (if (topThing == null) 0 else 1) + (if (topService == null) 0 else 1) +
            services.size + minOf(things.size, 6) + widgets.size + guides.size
}

/** Everything Search needs for one query. Pure: reads the catalogue and the resident's state. */
internal fun DemoStore.searchResults(raw: String, exact: Boolean): SearchResults {
    val allThings = if (isGuest) emptyList() else searchThings()
    val ownWords = allThings.flatMap { it.keywords }.toSet()
    val analysis = SearchIndex.analyse(raw, ownWords, allowCorrection = !exact)
    if (analysis.isEmpty) return SearchResults(analysis)

    // 1 · Direct answer
    var intent: SearchIntent? = null
    var needsSignIn = false
    SearchIntentDetector.detect(analysis.allWords, state.requests)?.let { found ->
        if (isGuest && found.isPersonal) needsSignIn = true
        else if (searchCanAnswer(found)) intent = found
    }

    // Services, ranked; the resident's own services get a small boost.
    val personal = (state.needsYou.mapNotNull { it.searchServiceID } + state.requests.map { it.serviceID } + state.drafts.map { it.serviceID }).toSet()
    val ranked = SearchIndex.services(analysis, personal)
    val guides = ranked.filter { it.service.kind == "info" }
    val catalogueThings = ranked.filter { it.service.kind == "thing" }
    val matches = ranked.filter { it.service.kind == "service" }.toMutableList()

    // 4 · Your things (and catalogue things like "Your vehicles")
    val ownMatches = searchMatch(allThings, analysis.words).toMutableList()
    intent?.let { i -> ownMatches.removeAll { it.coveredBy == i } }
    if (!isGuest) {
        ownMatches += catalogueThings.take(2).map { m ->
            SearchThing(
                id = "cat-${m.service.id}", title = m.service.name, subtitle = m.service.ownerLabel,
                leading = SearchLeading.Icon(m.service.symbol, m.service.hue), keywords = emptyList(), action = SearchAction.Service(m.service.id),
            )
        }
    }

    // 2 · Top result: a local term's service, else one of the resident's accounts, else a clear service match.
    var topThing: SearchThing? = null
    var topService: SearchMatch? = null
    if (intent == null && !needsSignIn) {
        val cardIndex = ownMatches.indexOfFirst { it.card != null && it.priority >= 80 }.takeIf { it >= 0 }
        // A local term names a service ("Mulkiya") unless it names one of your accounts ("Salik").
        val termNamesThing = analysis.localTerm?.let { term ->
            cardIndex?.let { i -> ownMatches[i].card?.let { SearchText.normalise(it.title).contains(term.phrase.joinToString(" ")) } }
        } ?: false
        val clearWinner = matches.firstOrNull()?.takeIf { it.isExact }?.takeIf { first ->
            matches.size == 1 || first.score - matches[1].score >= 8 || !matches[1].isExact
        }
        val first = matches.firstOrNull()
        when {
            analysis.localTerm != null && !termNamesThing && first != null && first.isExact -> topService = first
            // "Renew my car registration": the service, with the resident's Patrol on it.
            clearWinner != null && searchContext(clearWinner.service) != null -> topService = clearWinner
            cardIndex != null -> topThing = ownMatches.removeAt(cardIndex)
            clearWinner != null -> topService = clearWinner
        }
    }
    topService?.let { top -> matches.removeAll { it.id == top.id } }

    // 3 · Services (Related services after an answer; Also matching under a top service)
    val services: List<SearchMatch>
    val servicesTitle: String
    val answered = intent
    if (answered != null) {
        val related = searchRelatedServiceIDs(answered).mapNotNull { id ->
            Catalogue.service(id)?.let { SearchMatch(it, 0.0, null, false) }
        }
        val seen = HashSet<String>()
        services = (related + matches).filter { seen.add(it.id) }
        servicesTitle = "Related services"
    } else {
        services = matches
        servicesTitle = if (topService == null) "Services" else "Also matching"
    }

    return SearchResults(
        analysis = analysis, intent = intent, needsSignIn = needsSignIn, topThing = topThing, topService = topService,
        services = services, servicesTitle = servicesTitle, things = ownMatches, widgets = searchWidgets(analysis.words),
        guides = guides.take(2),
    )
}

private fun DemoStore.searchCanAnswer(intent: SearchIntent): Boolean = when (intent) {
    SearchIntent.Owe -> true
    is SearchIntent.Request -> request(intent.id) != null
    SearchIntent.SalikBalance -> state.linkedAccounts.any { it.id == "salik" && it.isLinked } || state.salikBalance.signum() > 0
    SearchIntent.NolBalance -> state.nolBalance != null
    is SearchIntent.Prayer -> true
    SearchIntent.Parking -> true
}

/** Minutes left on a parking session (never negative). */
internal fun searchMinutesLeft(endsAt: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): Int =
    maxOf(0L, Duration.between(now, endsAt).toMinutes()).toInt()

/** Salik trips left at AED 4 a trip. */
internal fun searchTrips(balance: BigDecimal): Int = balance.divide(BigDecimal(4), 0, RoundingMode.DOWN).toInt()

// MARK: Things

/** The resident's own things as search items, with the words that find them. */
internal fun DemoStore.searchThings(): List<SearchThing> {
    val out = mutableListOf<SearchThing>()
    val s = state

    // Accounts with balances
    if (s.linkedAccounts.any { it.id == "salik" && it.isLinked } || s.salikBalance.signum() > 0) {
        val low = s.salikBalance <= BigDecimal(20)
        val trips = searchTrips(s.salikBalance)
        val balance = Money.aed(s.salikBalance, trimWhole = true)
        out += SearchThing(
            id = "acct-salik", title = "Salik account",
            subtitle = "Balance $balance" + if (low) " · low, about $trips trips" else "",
            tone = if (low) ChipTone.Warning else ChipTone.Neutral, leading = SearchLeading.Icon("road.lanes", Hue.Sand),
            keywords = listOf("salik", "toll", "tolls", "gate", "account", "balance", "recharge", "top", "up", "darb"),
            action = SearchAction.Sheet(SheetRoute.TopUpSalik),
            card = SearchCard("road.lanes", "Salik", "Balance", balance, note = if (low) "low" else null, noteTone = ChipTone.Warning,
                button = "Top up", buttonAction = SearchAction.Sheet(SheetRoute.TopUpSalik)),
            coveredBy = SearchIntent.SalikBalance, priority = 6.0,
        )
    }
    s.nolBalance?.let { nol ->
        out += SearchThing(
            id = "acct-nol", title = "nol card", subtitle = "Balance ${Money.aed(nol)}",
            leading = SearchLeading.Icon("tram.fill", Hue.Sky),
            keywords = listOf("nol", "card", "metro", "bus", "tram", "balance", "top", "up", "recharge"),
            action = SearchAction.Service("top-up-nol-card"),
            card = SearchCard("tram.fill", "nol card", "Balance", Money.aed(nol), button = "Top up", buttonAction = SearchAction.Service("top-up-nol-card")),
            coveredBy = SearchIntent.NolBalance, priority = 4.0,
        )
    }
    s.parking?.let { p ->
        val left = searchMinutesLeft(p.endsAt)
        out += SearchThing(
            id = "acct-parking", title = "Parking in zone ${p.zone}",
            subtitle = "$left min left · ${p.area}", tone = if (left <= 15) ChipTone.Warning else ChipTone.Neutral,
            leading = SearchLeading.Icon("p.square.fill", Hue.Sky),
            keywords = listOf("parking", "park", "parkin", "zone", SearchText.normalise(p.zone), "session", "extend") + SearchText.tokens(p.area),
            action = SearchAction.Sheet(SheetRoute.ExtendParking),
            card = SearchCard("p.square.fill", "Parking", "Zone ${p.zone} ·", "$left min left", button = "Extend", buttonAction = SearchAction.Sheet(SheetRoute.ExtendParking)),
            coveredBy = SearchIntent.Parking, priority = 5.0,
        )
    }

    // Bills and fines
    for (p in payables) {
        val due = p.due?.let { DemoDate.dueLabel(it).lowercase() }
        val note = p.note?.lowercase()
        var keys = SearchText.tokens("${p.title} ${p.entity} ${p.detail}")
        keys = keys + if (p.kind == Payable.Kind.Bill) listOf("bill", "bills", "pay") else listOf("fine", "fines", "pay", "traffic")
        if (p.entity == "DEWA") keys = keys + listOf("dewa", "electricity", "water", "utility", "utilities")
        out += SearchThing(
            id = "pay-${p.id}", title = p.title,
            subtitle = listOfNotNull(p.entity, Money.aed(p.amount), due ?: note).joinToString(" · "),
            tone = if (note != null) ChipTone.Error else if (due != null) ChipTone.Warning else ChipTone.Neutral,
            leading = SearchLeading.Icon(p.symbol, p.hue), keywords = keys, action = SearchAction.Pay(p),
            card = SearchCard(
                p.symbol, if (p.entity == "DEWA") "DEWA bill" else p.title, if (due == null) "To pay" else "Due",
                Money.aed(p.amount), note = due ?: note, noteTone = if (note != null) ChipTone.Error else ChipTone.Warning,
                button = "Pay", buttonAction = SearchAction.Pay(p),
            ),
            coveredBy = SearchIntent.Owe, priority = 7.0,
        )
    }

    // Vehicles, and their Salik tags
    for (v in s.vehicles) {
        val days = DemoDate.daysFromToday(v.registrationExpiry)
        val due = days <= 30
        val reg = if (due) "registration due in $days days" else "registered to ${DemoDate.short(v.registrationExpiry)}"
        val keys = SearchText.tokens("${v.make} ${v.model} ${v.year} ${v.colour} ${v.plate.code} ${v.plate.number} ${v.insurer}") +
            listOf("car", "cars", "vehicle", "vehicles", "registration", "mulkiya", "insurance", "plate") +
            (if (due) listOf("renew", "renewal", "due") else emptyList())
        out += SearchThing(
            id = "veh-${v.id}", title = v.name, subtitle = "${v.plate.code} ${v.plate.number} · $reg",
            tone = if (due) ChipTone.Warning else ChipTone.Neutral, leading = SearchLeading.PlateMark(v.plate), keywords = keys,
            action = SearchAction.Push(Route.Vehicle(v.id)),
            card = SearchCard(
                "car.fill", v.shortName, "Registration", if (due) "due in $days days" else "valid", noteTone = ChipTone.Warning,
                button = if (due) "Renew" else "Open",
                buttonAction = if (due) SearchAction.Service("renew-vehicle-registration") else SearchAction.Push(Route.Vehicle(v.id)),
            ),
            priority = if (due) 3.0 else 1.0,
        )
        v.salikTag?.let { tag ->
            out += SearchThing(
                id = "tag-${v.id}", title = "Salik tag on your ${v.model}", subtitle = "$tag · active",
                leading = SearchLeading.PlateMark(v.plate),
                keywords = listOf("salik", "tag", "tags", "toll") + SearchText.tokens("${v.make} ${v.model} ${v.plate.number}"),
                action = SearchAction.Push(Route.Vehicle(v.id)),
            )
        }
    }

    // Requests in progress
    for (r in s.requests) {
        val tone = searchTone(r.status)
        val keys = SearchIntentDetector.requestKeywords(r).toList() + listOf("request", "requests", "application", "status", "track")
        out += SearchThing(
            id = "req-${r.id}", title = r.title, subtitle = r.statusLine, tone = tone, leading = SearchLeading.Icon(r.symbol, r.hue),
            keywords = keys, action = SearchAction.Push(Route.Request(r.id)),
            card = SearchCard(r.symbol, r.title, "", r.statusLine, noteTone = tone, button = r.action ?: "Track", buttonAction = SearchAction.Push(Route.Request(r.id))),
            coveredBy = SearchIntent.Request(r.id), priority = if (tone == ChipTone.Warning) 4.0 else 2.0,
        )
    }

    // Documents
    for (d in s.documents) {
        val keys = SearchText.tokens("${d.title} ${d.number}").toMutableList()
        val t = d.title.lowercase()
        if ("emirates" in t) keys += listOf("eid", "id", "hawiya", "identity", "card")
        if ("visa" in t) keys += listOf("visa", "residency", "residence", "iqama")
        if ("licence" in t) keys += listOf("license", "licence", "driving")
        if ("registration" in t) keys += listOf("mulkiya", "registration", "car", "vehicle")
        if ("tenancy" in t) keys += listOf("ejari", "tenancy", "rent", "lease")
        keys += listOf("document", "documents")
        val subtitle = d.statusNote ?: d.expiry?.let { "Valid to ${DemoDate.monthYear(it)}" } ?: d.number
        out += SearchThing(
            id = "doc-${d.id}", title = d.title, subtitle = subtitle,
            tone = if (d.status == DocumentItem.Status.Valid) ChipTone.Neutral else ChipTone.Warning,
            leading = SearchLeading.Icon(d.symbol, d.hue), keywords = keys, action = SearchAction.Push(Route.Document(d.id)),
        )
    }

    // Drafts
    for (d in s.drafts) {
        val name = Catalogue.service(d.serviceID)?.name ?: ""
        out += SearchThing(
            id = "draft-${d.id}", title = d.title, subtitle = "Draft · ${d.stepsDone} of ${d.stepsTotal} steps done",
            leading = SearchLeading.Icon(d.symbol, d.hue), keywords = SearchText.tokens("${d.title} $name") + listOf("draft", "drafts"),
            action = SearchAction.Service(d.serviceID),
        )
    }

    // Family
    for (m in s.family) {
        val visa = m.visaExpiry?.let { " · visa to ${DemoDate.short(it)}" } ?: ""
        out += SearchThing(
            id = "fam-${m.id}", title = m.name, subtitle = "${m.relation}$visa", leading = SearchLeading.Initials(m.initials, m.hue),
            keywords = SearchText.tokens("${m.name} ${m.relation}") + listOf("family"), action = SearchAction.Push(Route.Family),
        )
    }

    // Journeys started
    for (j in s.journeys) {
        val journey = Catalogue.service(j.journeyID) ?: continue
        val stages = journey.stages?.size ?: 0
        out += SearchThing(
            id = "jrn-${j.journeyID}", title = journey.name + (j.destination?.let { " $it" } ?: ""),
            subtitle = "Stage ${j.stageIndex + 1} of $stages · ${j.headline}", leading = SearchLeading.Icon(journey.symbol, journey.hue),
            keywords = SearchText.tokens("${journey.name} ${j.destination ?: ""} ${j.headline}") + journey.aliases.flatMap { SearchText.tokens(it) } + listOf("journey"),
            action = SearchAction.Push(Route.Journey(j.journeyID)),
        )
    }
    return out
}

internal fun searchTone(status: RequestItem.Status): ChipTone = when (status) {
    is RequestItem.Status.YourTurn -> ChipTone.Warning
    is RequestItem.Status.InReview -> ChipTone.Info
    is RequestItem.Status.Approved, RequestItem.Status.Done -> ChipTone.Success
}

/** Things whose words match every query word, best first. `priority` becomes the match score. */
private fun searchMatch(things: List<SearchThing>, words: List<String>): List<SearchThing> {
    if (words.isEmpty()) return emptyList()
    val scored = mutableListOf<SearchThing>()
    for (t in things) {
        val keys = t.keywords.map { it to SearchText.stem(it) }
        var total = 0.0
        var ok = true
        for ((i, w) in words.withIndex()) {
            val ws = SearchText.stem(w)
            val syn = SearchVocabulary.synonyms[ws] ?: emptySet()
            var best = 0.0
            for ((k, ks) in keys) {
                if (k == w || ks == ws) { best = 1.0; break }
                if (w.length >= 2 && k.startsWith(w)) best = maxOf(best, if (i == words.lastIndex) 0.9 else 0.75)
                else if (ks in syn) best = maxOf(best, 0.8)
                else if (w.length >= 5 && SearchText.levenshtein(k, w, 1) <= 1) best = maxOf(best, 0.55)
            }
            if (best == 0.0) { ok = false; break }
            total += best
        }
        if (!ok) continue
        val priority = total / words.size * 100 + t.priority
        if (priority >= 50) scored += t.copy(priority = priority)
    }
    return scored.sortedByDescending { it.priority }
}

// MARK: Widgets

/** Widgets matching the query that the resident can add to Home. */
private fun DemoStore.searchWidgets(words: List<String>): List<WidgetKind> {
    if (words.isEmpty()) return emptyList()
    val candidates = mutableListOf<Pair<WidgetKind, Set<String>>>(
        WidgetKind.Prayer to setOf("prayer", "prayers", "fajr", "dhuhr", "asr", "maghrib", "isha", "salah"),
        WidgetKind.Weather to setOf("weather", "beach", "beaches", "temperature", "forecast", "flag"),
        WidgetKind.Metro to setOf("metro", "train", "departures"),
    )
    if (!isGuest) {
        if (state.parking != null) candidates += WidgetKind.Parking to setOf("parking")
        if (state.salikBalance.signum() > 0) candidates += WidgetKind.Salik to setOf("salik")
        if (state.energy != null) candidates += WidgetKind.Energy to setOf("energy", "consumption", "electricity", "usage")
        if (state.nolBalance != null) candidates += WidgetKind.Nol to setOf("nol")
    }
    return candidates.mapNotNull { (kind, keys) ->
        kind.takeIf {
            words.all { w -> keys.any { (it.startsWith(w) && w.length >= 4) || SearchText.stem(it) == SearchText.stem(w) } }
        }
    }
}

// MARK: Related services and context

internal fun DemoStore.searchRelatedServiceIDs(intent: SearchIntent): List<String> = when (intent) {
    SearchIntent.Owe -> {
        val ids = payables.mapNotNull { it.serviceID }.toMutableList()
        if (state.vehicles.isNotEmpty()) ids += "get-traffic-clearance"
        ids.ifEmpty { listOf("pay-all", "pay-traffic-fine", "pay-electricity-water-bill") }
    }
    is SearchIntent.Request -> request(intent.id)?.let { r ->
        val ids = mutableListOf(r.serviceID)
        if ("visa" in r.serviceID) ids += listOf("check-visa-status", "sponsor-family-member")
        if ("licence" in r.serviceID) ids += listOf("replace-driving-licence", "change-driving-licence-details")
        ids
    } ?: emptyList()
    SearchIntent.SalikBalance -> listOf("top-up-salik-account", "activate-salik-tag")
    SearchIntent.NolBalance -> listOf("top-up-nol-card", "pay-public-transport-fine")
    is SearchIntent.Prayer -> listOf("check-prayer-times", "find-mosque", "find-qibla")
    SearchIntent.Parking -> listOf("pay-for-parking", "buy-parking-subscription", "get-parking-permit")
}

/** The accounts an answer was worked out from ("DEWA, RTA and Dubai Police"). */
internal fun DemoStore.searchAnswerSources(intent: SearchIntent): String = when (intent) {
    SearchIntent.Owe -> {
        val names = payables.map { it.entity }.toMutableSet()
        if (state.vehicles.isNotEmpty()) names += "RTA"
        val ordered = state.linkedAccounts.map { it.name }.filter { it in names }
        val list = ordered.ifEmpty { names.sorted() }
        // Nothing to pay and no car: no names to list (iOS prints "your linked  accounts" here).
        val linked = if (list.isEmpty()) "linked" else "linked ${SearchFormat.list(list)}"
        "Answers are worked out from your $linked accounts. Only you can see them."
    }
    is SearchIntent.Request -> "Answers are worked out from your ${request(intent.id)?.entity ?: "government"} application. Only you can see them."
    SearchIntent.SalikBalance -> "Answers are worked out from your linked Salik account. Only you can see them."
    SearchIntent.NolBalance -> "Answers are worked out from your linked nol card. Only you can see them."
    SearchIntent.Parking -> "Answers are worked out from your linked Parkin account. Only you can see them."
    is SearchIntent.Prayer -> "Prayer times are for Dubai, from IACAD."
}

/** Personal context beside a service row ("due tomorrow", "your Patrol, due in 12 days"). */
internal fun DemoStore.searchContext(service: CatalogueService): Pair<String, ChipTone>? {
    if (isGuest) return null
    for (item in state.needsYou) {
        if (item.searchServiceID != service.id) continue
        item.payable?.let { p ->
            val due = p.due?.let { DemoDate.dueLabel(it).lowercase() } ?: "to pay"
            return "${Money.aed(p.amount, trimWhole = true)} $due" to (if (p.due == null) ChipTone.Error else ChipTone.Warning)
        }
        val due = item.due?.let { DemoDate.dueLabel(it).lowercase() } ?: "needs you"
        val kind = item.kind
        if (kind is NeedsYouItem.Kind.Renewal) {
            state.vehicles.firstOrNull { it.id == kind.thingID }?.let { v -> return "your ${v.model}, $due" to ChipTone.Warning }
        }
        return due to ChipTone.Warning
    }
    if (service.id == "top-up-salik-account" && state.salikBalance.signum() > 0) {
        return "balance ${Money.aed(state.salikBalance, trimWhole = true)}" to (if (state.salikBalance <= BigDecimal(20)) ChipTone.Warning else ChipTone.Neutral)
    }
    state.requests.firstOrNull { it.serviceID == service.id && it.title == service.name }?.let { r ->
        return when (r.status) {
            is RequestItem.Status.YourTurn -> "your turn" to ChipTone.Warning
            is RequestItem.Status.InReview -> "in review" to ChipTone.Info
            is RequestItem.Status.Approved -> "approved" to ChipTone.Success
            RequestItem.Status.Done -> null
        }
    }
    state.drafts.firstOrNull { it.serviceID == service.id }?.let { d ->
        return "draft, ${d.stepsDone} of ${d.stepsTotal} done" to ChipTone.Info
    }
    return null
}

/** The resident's own context on a service card ("M 48213 · Your Patrol is due in 12 days"). */
internal data class SearchContextStrip(
    val plate: Plate? = null,
    val symbol: String? = null,
    val text: String,
    /** "Continue" when the resident has already started it. */
    val buttonTitle: String? = null,
    /** Where the button goes instead of the service ("Track your request"). */
    val route: Route? = null,
)

internal fun DemoStore.searchStrip(service: CatalogueService): SearchContextStrip? {
    if (isGuest) return null
    for (item in state.needsYou) {
        if (item.searchServiceID != service.id) continue
        val kind = item.kind
        if (kind is NeedsYouItem.Kind.Renewal) {
            state.vehicles.firstOrNull { it.id == kind.thingID }?.let { v ->
                val days = DemoDate.daysFromToday(v.registrationExpiry)
                return SearchContextStrip(plate = v.plate, text = "Your ${v.model} is due in $days days")
            }
        }
        item.payable?.let { p ->
            val due = p.due?.let { DemoDate.dueLabel(it).lowercase() } ?: "to pay"
            return SearchContextStrip(symbol = p.symbol, text = "You have ${Money.aed(p.amount)} $due")
        }
    }
    state.requests.firstOrNull { it.serviceID == service.id && it.title == service.name }?.let { r ->
        val status = r.statusLine.take(1).lowercase() + r.statusLine.drop(1)
        return SearchContextStrip(symbol = r.symbol, text = "You have a request · $status", buttonTitle = "Track your request", route = Route.Request(r.id))
    }
    state.drafts.firstOrNull { it.serviceID == service.id }?.let { d ->
        return SearchContextStrip(symbol = "doc.badge.clock", text = "You started this · ${d.stepsDone} of ${d.stepsTotal} steps done", buttonTitle = "Continue")
    }
    return null
}

/** The catalogue service a Needs you item belongs to. */
internal val NeedsYouItem.searchServiceID: String?
    get() = when (val k = kind) {
        is NeedsYouItem.Kind.Payment -> k.payable.serviceID
        is NeedsYouItem.Kind.Renewal -> k.serviceID
        is NeedsYouItem.Kind.RequestStep -> null
        is NeedsYouItem.Kind.JourneyStep -> k.journeyID
    }

internal object SearchFormat {
    /** "A", "A and B", "A, B and C". */
    fun list(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        else -> items.dropLast(1).joinToString(", ") + " and " + items.last()
    }

    /** "1 bill and 1 fine", "2 bills". */
    fun payablesSummary(items: List<Payable>): String {
        val groups = listOf(
            Triple(Payable.Kind.Bill, "bill", "bills"), Triple(Payable.Kind.Fine, "fine", "fines"),
            Triple(Payable.Kind.Fee, "fee", "fees"), Triple(Payable.Kind.TopUp, "top-up", "top-ups"),
        )
        val parts = groups.mapNotNull { (kind, one, many) ->
            val n = items.count { it.kind == kind }
            if (n == 0) null else "$n ${if (n == 1) one else many}"
        }
        return list(parts)
    }
}

/** "Last topped up AED 100 on 20 September" from the resident's history. */
internal fun DemoStore.searchLastTopUp(serviceID: String): String? {
    val r = state.history.firstOrNull { it.serviceID == serviceID } ?: return null
    val amount = r.amount ?: return null
    return "Last topped up ${Money.aed(amount, trimWhole = true)} on ${DemoDate.long(r.date)}"
}
