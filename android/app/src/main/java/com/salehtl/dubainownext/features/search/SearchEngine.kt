package com.salehtl.dubainownext.features.search

import com.salehtl.dubainownext.data.Catalogue
import com.salehtl.dubainownext.data.CatalogueService
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

// Search ranking for the catalogue (Paper p-W-0 · 04 Search · "How results work"), ported from
// ios/…/Features/Search/SearchEngine.swift. Builds on the catalogue with: words people use
// (Mulkiya, Ejari, Amer…), synonyms (car → vehicle, licence ↔ license), plural/stem matches,
// Arabic names, and one or two typos corrected ("mulkia" → "mulkiya"), with the correction shown.

// MARK: - Text helpers

internal object SearchText {
    private val marks = Regex("\\p{Mn}+")

    /** Lowercased with accents and Arabic marks folded; punctuation untouched. */
    fun fold(s: String): String = marks.replace(Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD), "")

    /** Lowercased, accents folded, punctuation → spaces, apostrophes dropped ("child's" → "childs"). */
    fun normalise(s: String): String {
        val folded = fold(s)
        val out = StringBuilder(folded.length)
        for (ch in folded) {
            if (ch == '\'' || ch == '’') continue
            out.append(if (ch.isLetterOrDigit()) ch else ' ')
        }
        return out.toString()
    }

    fun tokens(s: String): List<String> = normalise(s).split(' ').filter { it.isNotEmpty() }

    /** Words that carry no meaning for matching ("how do I renew my car" → renew, car). */
    val stopWords: Set<String> = setOf(
        "a", "an", "the", "my", "your", "our", "i", "me", "we", "you", "to", "for", "of", "in", "on", "at",
        "and", "or", "is", "are", "am", "be", "do", "does", "did", "how", "what", "whats", "when", "where",
        "which", "who", "can", "could", "should", "will", "would", "please", "with", "from", "about", "it",
        "this", "that", "much", "many", "need", "want", "near", "nearby", "nearest", "closest", "now", "today",
        "there", "any", "some", "get", "im", "by",
    )

    private fun isAscii(w: String) = w.all { it.code < 128 }

    /** Simple English stemming: plurals, -ing, -ed, -al ("renewal" → "renew", "fines" → "fine"). */
    fun stem(w: String): String {
        if (w.length <= 3 || !isAscii(w)) return w
        return when {
            w.endsWith("ies") && w.length > 4 -> w.dropLast(3) + "y"
            w.endsWith("sses") -> w.dropLast(2)
            w.endsWith("ches") || w.endsWith("shes") || w.endsWith("xes") -> w.dropLast(2)
            w.endsWith("ing") && w.length > 5 -> w.dropLast(3)
            w.endsWith("ed") && w.length > 4 && !w.endsWith("eed") -> w.dropLast(2)
            w.endsWith("al") && w.length > 6 -> w.dropLast(2)
            w.endsWith("s") && !w.endsWith("ss") && !w.endsWith("us") && !w.endsWith("is") -> w.dropLast(1)
            else -> w
        }
    }

    /** Edit distance with an early exit once it exceeds [limit]. */
    fun levenshtein(a: String, b: String, limit: Int = 2): Int {
        if (abs(a.length - b.length) > limit) return limit + 1
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = min(min(prev[j] + 1, cur[j - 1] + 1), prev[j - 1] + cost)
                rowMin = min(rowMin, cur[j])
            }
            if (rowMin > limit) return limit + 1
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    /** Typos allowed for a word of this length (none under 5 letters). */
    fun typoAllowance(w: String): Int = when {
        w.length < 5 -> 0
        w.length < 8 -> 1
        else -> 2
    }

    fun isAsciiWord(w: String) = isAscii(w)
}

// MARK: - Words people use

/** A local term people search for, and the services it means (first is the main one). */
internal data class SearchLocalTerm(val phrase: List<String>, val label: String, val serviceIDs: List<String>)

internal object SearchVocabulary {
    private fun term(phrase: String, label: String, ids: List<String>) = SearchLocalTerm(SearchText.tokens(phrase), label, ids)

    /** Paper "Words people use → the service" plus the other local names residents type. */
    val localTerms: List<SearchLocalTerm> = listOf(
        term("mulkiya", "Mulkiya", listOf("renew-vehicle-registration", "register-vehicle", "transfer-vehicle-ownership", "get-vehicle-certificate", "cancel-vehicle-registration")),
        term("ejari", "Ejari", listOf("register-tenancy-contract", "renew-tenancy-contract", "end-tenancy-contract")),
        term("amer", "Amer", listOf("renew-residence-visa", "apply-residence-visa", "sponsor-family-member", "check-visa-status", "get-visit-visa")),
        term("tasjeel", "Tasjeel", listOf("find-testing-centre", "renew-vehicle-registration")),
        term("tasheel", "Tasheel", listOf("sponsor-domestic-worker", "report-employer-problem")),
        term("mohre", "MOHRE", listOf("report-employer-problem", "sponsor-domestic-worker")),
        term("salik", "Salik", listOf("top-up-salik-account", "activate-salik-tag")),
        term("nol", "nol", listOf("top-up-nol-card", "pay-public-transport-fine")),
        term("dewa", "DEWA", listOf("pay-electricity-water-bill", "connect-electricity-water", "move-electricity-water-account", "close-electricity-water-account")),
        term("emirates id", "Emirates ID", listOf("get-or-renew-emirates-id")),
        term("eid", "EID", listOf("get-or-renew-emirates-id")),
        term("hawiya", "Hawiya", listOf("get-or-renew-emirates-id")),
        term("id card", "ID card", listOf("get-or-renew-emirates-id")),
        term("good conduct", "good conduct", listOf("get-good-conduct-certificate")),
        term("police clearance", "police clearance", listOf("get-good-conduct-certificate")),
        term("pcc", "PCC", listOf("get-good-conduct-certificate")),
        term("iqama", "Iqama", listOf("renew-residence-visa", "apply-residence-visa")),
        term("mawaqif", "Mawaqif", listOf("pay-parking-other-emirates")),
        term("mparking", "mParking", listOf("pay-for-parking")),
        term("noc", "NOC", listOf("get-police-letter", "get-building-letter", "get-endowment-noc")),
        term("recharge salik", "Salik recharge", listOf("top-up-salik-account")),
        term("salik recharge", "Salik recharge", listOf("top-up-salik-account")),
    )

    /** Words that mean the same thing. Spelling variants count (licence/license, colour/color). */
    val synonymGroups: List<List<String>> = listOf(
        listOf("car", "vehicle", "auto", "automobile"),
        listOf("licence", "license"),
        listOf("colour", "color"),
        listOf("centre", "center"),
        listOf("enrolment", "enrollment"),
        listOf("programme", "program"),
        listOf("home", "house", "villa", "apartment", "flat"),
        listOf("tenancy", "rent", "rental", "lease", "ejari"),
        listOf("electricity", "power", "dewa"),
        listOf("phone", "mobile", "telecom"),
        listOf("fine", "penalty", "violation", "mukhalafa"),
        listOf("child", "kid", "son", "daughter"),
        listOf("renew", "extend"),
        listOf("marriage", "married", "wedding", "marry"),
        listOf("doctor", "clinic", "hospital"),
        listOf("visa", "residency", "residence", "iqama"),
        listOf("prayer", "salah", "salat", "namaz"),
        listOf("toll", "salik"),
        listOf("metro", "nol", "tram", "bus"),
        listOf("pay", "payment"),
        listOf("certificate", "letter"),
        listOf("identity", "id", "eid", "hawiya"),
        listOf("top", "recharge"),
        listOf("permit", "licence"),
    )

    val synonyms: Map<String, Set<String>> by lazy {
        val map = HashMap<String, MutableSet<String>>()
        for (group in synonymGroups) {
            val stems = group.map(SearchText::stem).toSet()
            for (w in group) {
                val s = SearchText.stem(w)
                map.getOrPut(s) { mutableSetOf() }.addAll(stems - s)
            }
        }
        map
    }
}

// MARK: - Index

internal data class SearchWord(val word: String, val stem: String)

/** One searchable catalogue record with its words pre-normalised. */
internal class SearchEntry(
    val service: CatalogueService,
    /** Normalised full name. */
    val name: String,
    val nameWords: List<SearchWord>,
    val aliasWords: List<SearchWord>,
    /** (normalised, original) */
    val aliasPhrases: List<Pair<String, String>>,
    val ownerWords: List<SearchWord>,
    val descriptionStems: Set<String>,
    val popularity: Double,
)

/** A catalogue record that matched, with what it's also called. */
internal data class SearchMatch(
    val service: CatalogueService,
    val score: Double,
    /** The alias or local term that matched, when the name itself doesn't say it. */
    val alsoCalled: String?,
    /** Every query word matched the name, an alias or a local term exactly. */
    val isExact: Boolean,
) {
    val id: String get() = service.id
}

/** A query broken into words, with any typo corrected. */
internal data class SearchQueryAnalysis(
    val raw: String,
    /** All normalised words, stop words included (for spotting questions). */
    val allWords: List<String>,
    /** Meaningful words, after correction. */
    val words: List<String>,
    /** "mulkiya" when the resident typed "mulkia". null when nothing was corrected. */
    val corrected: String?,
    /** A local term in the query (Mulkiya, Ejari…). */
    val localTerm: SearchLocalTerm?,
) {
    val isEmpty: Boolean get() = words.isEmpty()
}

internal object SearchIndex {
    private class Built(val entries: List<SearchEntry>, val vocabulary: Map<String, Double>)

    private val built: Built by lazy { build() }
    val entries: List<SearchEntry> get() = built.entries
    /** Word → weight (how often residents meet it). Used to pick typo corrections. */
    private val vocabulary: Map<String, Double> get() = built.vocabulary

    private fun build(): Built {
        val catalogue = Catalogue.shared
        val records = catalogue.services + catalogue.things + catalogue.guides
        fun words(s: String) = SearchText.tokens(s).map { SearchWord(it, SearchText.stem(it)) }

        val vocab = HashMap<String, Double>()
        val entries = records.map { s ->
            val nameWords = words(s.name) + words(s.shortName) + words(s.nameAr)
            val aliasWords = s.aliases.flatMap { words(it) }
            val ownerWords = words(s.owner ?: "")
            val pop = log10(max(s.uniques30d, 0).toDouble() + 1)
            for (w in nameWords + aliasWords + ownerWords) if (w.word.length > 1) vocab[w.word] = (vocab[w.word] ?: 0.0) + 1 + pop
            SearchEntry(
                service = s,
                name = SearchText.tokens(s.name).joinToString(" "),
                nameWords = nameWords,
                aliasWords = aliasWords,
                aliasPhrases = s.aliases.map { SearchText.tokens(it).joinToString(" ") to it },
                ownerWords = ownerWords,
                descriptionStems = SearchText.tokens(s.description).map(SearchText::stem).toSet(),
                popularity = pop,
            )
        }
        for (t in SearchVocabulary.localTerms) for (w in t.phrase) vocab[w] = (vocab[w] ?: 0.0) + 12
        for (group in SearchVocabulary.synonymGroups) for (w in group) vocab[w] = (vocab[w] ?: 0.0) + 2
        // Words the direct answers understand.
        for (w in listOf("owe", "bills", "balance", "status", "prayer", "dhuhr", "asr", "maghrib", "isha", "fajr", "parking", "expire")) {
            vocab[w] = (vocab[w] ?: 0.0) + 8
        }
        return Built(entries, vocab)
    }

    /** Builds the index ahead of the first keystroke (call off the main thread). */
    fun warmUp() { built }

    // MARK: Query analysis

    /**
     * Splits the query into words and corrects one or two typos ("visa stetus" → "visa status").
     * [extraWords] are the resident's own things (plate numbers, car models) so they aren't "corrected".
     */
    fun analyse(raw: String, extraWords: Set<String> = emptySet(), allowCorrection: Boolean = true): SearchQueryAnalysis {
        val all = SearchText.tokens(raw)
        var meaningful = all.filter { it !in SearchText.stopWords }
        if (meaningful.isEmpty()) meaningful = all.filter { it.length > 1 }

        val corrected = meaningful.toMutableList()
        var didCorrect = false
        if (allowCorrection) {
            meaningful.forEachIndexed { i, w ->
                if (!isKnown(w, extraWords)) correction(w)?.let { corrected[i] = it; didCorrect = true }
            }
        }

        val localTerm = findLocalTerm(corrected)
        var correctedRaw: String? = null
        if (didCorrect) {
            // Rebuild the sentence with corrected words in place, keeping the resident's other words.
            val out = mutableListOf<String>()
            var j = 0
            for (w in all) {
                if (j < meaningful.size && w == meaningful[j]) { out += corrected[j]; j++ } else out += w
            }
            correctedRaw = out.joinToString(" ")
        }
        val correctedAll = correctedRaw?.let { SearchText.tokens(it) } ?: all
        return SearchQueryAnalysis(raw, correctedAll, corrected, correctedRaw, localTerm)
    }

    private fun isKnown(w: String, extraWords: Set<String>): Boolean {
        if (w.length < 5 || w.any { it.isDigit() } || !SearchText.isAsciiWord(w)) return true
        if (w in extraWords || vocabulary.containsKey(w) || vocabulary.containsKey(SearchText.stem(w))) return true
        if (SearchVocabulary.synonyms.containsKey(SearchText.stem(w))) return true
        // Still typing: a prefix of a known word isn't a typo.
        return vocabulary.keys.any { it.startsWith(w) }
    }

    private fun correction(w: String): String? {
        val allowance = SearchText.typoAllowance(w)
        if (allowance <= 0) return null
        var bestWord: String? = null; var bestDistance = Int.MAX_VALUE; var bestWeight = 0.0
        for ((candidate, weight) in vocabulary) {
            if (abs(candidate.length - w.length) > allowance || candidate.length < 3) continue
            val d = SearchText.levenshtein(w, candidate, allowance)
            if (d > allowance) continue
            if (bestWord == null || d < bestDistance || (d == bestDistance && weight > bestWeight)) {
                bestWord = candidate; bestDistance = d; bestWeight = weight
            }
        }
        return bestWord
    }

    private fun findLocalTerm(words: List<String>): SearchLocalTerm? {
        // Longest phrase first ("emirates id" before "eid").
        for (term in SearchVocabulary.localTerms.sortedByDescending { it.phrase.size }) {
            val n = term.phrase.size
            if (n > words.size) continue
            for (start in 0..(words.size - n)) if (words.subList(start, start + n) == term.phrase) return term
        }
        return null
    }

    // MARK: Ranking

    private enum class Field { Name, Alias, Owner, Description }

    /**
     * Catalogue records for the query, best first. [personal] adds a small boost for services
     * tied to the resident's own things (a bill due, a request in progress).
     */
    fun services(q: SearchQueryAnalysis, personal: Set<String> = emptySet()): List<SearchMatch> {
        if (q.words.isEmpty()) return emptyList()
        val phrase = q.words.joinToString(" ")
        val termWords = q.localTerm?.phrase?.toSet() ?: emptySet()

        val matches = mutableListOf<SearchMatch>()
        for (e in entries) {
            val termRank = q.localTerm?.serviceIDs?.indexOf(e.service.id)?.takeIf { it >= 0 }
            var total = 0.0
            var exact = true
            var aliasHit: String? = null
            var failed = false

            for ((i, w) in q.words.withIndex()) {
                val isLast = i == q.words.lastIndex
                if (termRank != null && w in termWords) { total += 1; continue }
                val (s, field) = score(w, isLast, e)
                if (s <= 0) { failed = true; break }
                total += s
                if (s < 0.95) exact = false
                if (field == Field.Alias && aliasHit == null) {
                    aliasHit = e.aliasPhrases.firstOrNull { p -> p.first.split(' ').any { it.startsWith(w) } }?.second
                }
            }
            if (failed) continue

            var score = total / q.words.size * 100
            if (e.name.startsWith(phrase)) score += 15
            else if (e.name.contains(" $phrase")) score += 8
            e.aliasPhrases.firstOrNull { it.first == phrase }?.let { score += 20; aliasHit = it.second }
            if (termRank != null) {
                score += if (termRank == 0) 40 else 18
                q.localTerm?.let { term -> if (!e.name.contains(term.phrase.joinToString(" "))) aliasHit = term.label }
            }
            score += e.popularity * 1.6
            if (e.service.id in personal) score += 8
            if (!e.service.isLive) score -= 6
            if (score < 45) continue

            // Only say "also called" when the name doesn't already contain the word.
            val hit = aliasHit?.takeUnless { SearchText.fold(e.service.name).contains(SearchText.fold(it)) }
            val isExact = exact && (q.words.size >= 2 || termRank != null || e.aliasPhrases.any { it.first == phrase } || e.name == phrase)
            matches += SearchMatch(e.service, score, hit, isExact)
        }
        return matches.sortedWith(compareByDescending<SearchMatch> { it.score }.thenByDescending { it.service.uniques30d })
    }

    /**
     * How well one query word matches a record: exact or same stem 1, prefix 0.9 (last word) /
     * 0.75, synonym 0.95, a typo 0.55; aliases ×0.95, owner ×0.8, description words 0.35.
     */
    private fun score(w: String, isLast: Boolean, e: SearchEntry): Pair<Double, Field> {
        val ws = SearchText.stem(w)
        val syn = SearchVocabulary.synonyms[ws] ?: emptySet()
        var best = 0.0
        var bestField = Field.Name

        fun consider(words: List<SearchWord>, weight: Double, field: Field) {
            for (sw in words) {
                val s = when {
                    sw.word == w || sw.stem == ws -> 1.0
                    w.length >= 2 && sw.word.startsWith(w) -> if (isLast) 0.9 else 0.75
                    sw.stem in syn -> 0.95
                    w.length >= 5 && SearchText.levenshtein(sw.word, w, 1) <= 1 -> 0.55
                    else -> 0.0
                }
                if (s * weight > best) { best = s * weight; bestField = field }
                if (best >= 1) return
            }
        }
        consider(e.nameWords, 1.0, Field.Name)
        if (best < 1) consider(e.aliasWords, 0.95, Field.Alias)
        if (best < 0.8) consider(e.ownerWords, 0.8, Field.Owner)
        if (best < 0.35 && ws in e.descriptionStems) { best = 0.35; bestField = Field.Description }
        return best to bestField
    }
}
