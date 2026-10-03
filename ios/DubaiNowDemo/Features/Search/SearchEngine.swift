import Foundation

// Search ranking for the catalogue (Paper p-W-0 · 04 Search · "How results work").
// Builds on `Catalogue.shared.search` with: words people use (Mulkiya, Ejari, Amer…),
// synonyms (car → vehicle, licence ↔ license), plural/stem matches, Arabic names,
// and one or two typos corrected ("mulkia" → "mulkiya"), with the correction shown.

// MARK: - Text helpers

nonisolated enum SearchText {
    /// Lowercased, accents folded, punctuation → spaces, apostrophes dropped ("child's" → "childs").
    static func normalise(_ s: String) -> String {
        let folded = s.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: Locale(identifier: "en"))
        var out = ""
        out.reserveCapacity(folded.count)
        for ch in folded {
            if ch == "'" || ch == "’" { continue }
            if ch.isLetter || ch.isNumber { out.append(ch) } else { out.append(" ") }
        }
        return out
    }

    static func tokens(_ s: String) -> [String] {
        normalise(s).split(separator: " ").map(String.init)
    }

    /// Words that carry no meaning for matching ("how do I renew my car" → renew, car).
    static let stopWords: Set<String> = [
        "a", "an", "the", "my", "your", "our", "i", "me", "we", "you", "to", "for", "of", "in", "on", "at",
        "and", "or", "is", "are", "am", "be", "do", "does", "did", "how", "what", "whats", "when", "where",
        "which", "who", "can", "could", "should", "will", "would", "please", "with", "from", "about", "it",
        "this", "that", "much", "many", "need", "want", "near", "nearby", "nearest", "closest", "now", "today",
        "there", "any", "some", "get", "im", "by",
    ]

    /// Simple English stemming: plurals, -ing, -ed, -al ("renewal" → "renew", "fines" → "fine").
    static func stem(_ w: String) -> String {
        guard w.count > 3, w.allSatisfy({ $0.isASCII }) else { return w }
        if w.hasSuffix("ies"), w.count > 4 { return String(w.dropLast(3)) + "y" }
        if w.hasSuffix("sses") { return String(w.dropLast(2)) }
        if w.hasSuffix("ches") || w.hasSuffix("shes") || w.hasSuffix("xes") { return String(w.dropLast(2)) }
        if w.hasSuffix("ing"), w.count > 5 { return String(w.dropLast(3)) }
        if w.hasSuffix("ed"), w.count > 4, !w.hasSuffix("eed") { return String(w.dropLast(2)) }
        if w.hasSuffix("al"), w.count > 6 { return String(w.dropLast(2)) }
        if w.hasSuffix("s"), !w.hasSuffix("ss"), !w.hasSuffix("us"), !w.hasSuffix("is") { return String(w.dropLast()) }
        return w
    }

    /// Edit distance with an early exit once it exceeds `limit`.
    static func levenshtein(_ a: String, _ b: String, limit: Int = 2) -> Int {
        let a = Array(a), b = Array(b)
        if abs(a.count - b.count) > limit { return limit + 1 }
        if a.isEmpty { return b.count }
        if b.isEmpty { return a.count }
        var prev = Array(0...b.count)
        var cur = Array(repeating: 0, count: b.count + 1)
        for i in 1...a.count {
            cur[0] = i
            var rowMin = cur[0]
            for j in 1...b.count {
                let cost = a[i - 1] == b[j - 1] ? 0 : 1
                cur[j] = min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                rowMin = min(rowMin, cur[j])
            }
            if rowMin > limit { return limit + 1 }
            swap(&prev, &cur)
        }
        return prev[b.count]
    }

    /// Typos allowed for a word of this length (none under 5 letters).
    static func typoAllowance(_ w: String) -> Int {
        switch w.count {
        case ..<5: 0
        case 5..<8: 1
        default: 2
        }
    }
}

// MARK: - Words people use

/// A local term people search for, and the services it means (first is the main one).
struct SearchLocalTerm {
    let phrase: [String]
    let label: String
    let serviceIDs: [String]
}

enum SearchVocabulary {
    /// Paper "Words people use → the service" plus the other local names residents type.
    static let localTerms: [SearchLocalTerm] = [
        term("mulkiya", "Mulkiya", ["renew-vehicle-registration", "register-vehicle", "transfer-vehicle-ownership", "get-vehicle-certificate", "cancel-vehicle-registration"]),
        term("ejari", "Ejari", ["register-tenancy-contract", "renew-tenancy-contract", "end-tenancy-contract"]),
        term("amer", "Amer", ["renew-residence-visa", "apply-residence-visa", "sponsor-family-member", "check-visa-status", "get-visit-visa"]),
        term("tasjeel", "Tasjeel", ["find-testing-centre", "renew-vehicle-registration"]),
        term("tasheel", "Tasheel", ["sponsor-domestic-worker", "report-employer-problem"]),
        term("mohre", "MOHRE", ["report-employer-problem", "sponsor-domestic-worker"]),
        term("salik", "Salik", ["top-up-salik-account", "activate-salik-tag"]),
        term("nol", "nol", ["top-up-nol-card", "pay-public-transport-fine"]),
        term("dewa", "DEWA", ["pay-electricity-water-bill", "connect-electricity-water", "move-electricity-water-account", "close-electricity-water-account"]),
        term("emirates id", "Emirates ID", ["get-or-renew-emirates-id"]),
        term("eid", "EID", ["get-or-renew-emirates-id"]),
        term("hawiya", "Hawiya", ["get-or-renew-emirates-id"]),
        term("id card", "ID card", ["get-or-renew-emirates-id"]),
        term("good conduct", "good conduct", ["get-good-conduct-certificate"]),
        term("police clearance", "police clearance", ["get-good-conduct-certificate"]),
        term("pcc", "PCC", ["get-good-conduct-certificate"]),
        term("iqama", "Iqama", ["renew-residence-visa", "apply-residence-visa"]),
        term("mawaqif", "Mawaqif", ["pay-parking-other-emirates"]),
        term("mparking", "mParking", ["pay-for-parking"]),
        term("noc", "NOC", ["get-police-letter", "get-building-letter", "get-endowment-noc"]),
        term("recharge salik", "Salik recharge", ["top-up-salik-account"]),
        term("salik recharge", "Salik recharge", ["top-up-salik-account"]),
    ]

    private static func term(_ phrase: String, _ label: String, _ ids: [String]) -> SearchLocalTerm {
        SearchLocalTerm(phrase: SearchText.tokens(phrase), label: label, serviceIDs: ids)
    }

    /// Words that mean the same thing. Spelling variants count (licence/license, colour/color).
    static let synonymGroups: [[String]] = [
        ["car", "vehicle", "auto", "automobile"],
        ["licence", "license"],
        ["colour", "color"],
        ["centre", "center"],
        ["enrolment", "enrollment"],
        ["programme", "program"],
        ["home", "house", "villa", "apartment", "flat"],
        ["tenancy", "rent", "rental", "lease", "ejari"],
        ["electricity", "power", "dewa"],
        ["phone", "mobile", "telecom"],
        ["fine", "penalty", "violation", "mukhalafa"],
        ["child", "kid", "son", "daughter"],
        ["renew", "extend"],
        ["marriage", "married", "wedding", "marry"],
        ["doctor", "clinic", "hospital"],
        ["visa", "residency", "residence", "iqama"],
        ["prayer", "salah", "salat", "namaz"],
        ["toll", "salik"],
        ["metro", "nol", "tram", "bus"],
        ["pay", "payment"],
        ["certificate", "letter"],
        ["identity", "id", "eid", "hawiya"],
        ["top", "recharge"],
        ["permit", "licence"],
    ]

    static let synonyms: [String: Set<String>] = {
        var map: [String: Set<String>] = [:]
        for group in synonymGroups {
            let stems = Set(group.map(SearchText.stem))
            for w in group { map[SearchText.stem(w), default: []].formUnion(stems.subtracting([SearchText.stem(w)])) }
        }
        return map
    }()
}

// MARK: - Index

/// One searchable catalogue record with its words pre-normalised.
struct SearchEntry {
    let service: CatalogueService
    let name: String                 // normalised full name
    let nameWords: [(word: String, stem: String)]
    let aliasWords: [(word: String, stem: String)]
    let aliasPhrases: [(normalised: String, original: String)]
    let ownerWords: [(word: String, stem: String)]
    let descriptionStems: Set<String>
    let popularity: Double
}

/// A catalogue record that matched, with what to bold and what it's also called.
struct SearchMatch: Identifiable, Hashable {
    var id: String { service.id }
    let service: CatalogueService
    let score: Double
    /// Part of the name to bold (a query word that appears in the name).
    let highlight: String?
    /// The alias or local term that matched, when the name itself doesn't say it.
    let alsoCalled: String?
    /// Every query word matched the name, an alias or a local term exactly.
    let isExact: Bool
}

/// A query broken into words, with any typo corrected.
struct SearchQueryAnalysis {
    let raw: String
    /// All normalised words, stop words included (for spotting questions).
    let allWords: [String]
    /// Meaningful words, after correction.
    let words: [String]
    /// "mulkiya" when the resident typed "mulkia". nil when nothing was corrected.
    let corrected: String?
    /// A local term in the query (Mulkiya, Ejari…).
    let localTerm: SearchLocalTerm?

    var isEmpty: Bool { words.isEmpty }
}

final class SearchIndex {
    static let shared = SearchIndex()

    let entries: [SearchEntry]
    /// Word → weight (how often residents meet it). Used to pick typo corrections.
    private(set) var vocabulary: [String: Double]

    private init() {
        let catalogue = Catalogue.shared
        let records = catalogue.services + catalogue.things + catalogue.guides
        func words(_ s: String) -> [(String, String)] { SearchText.tokens(s).map { ($0, SearchText.stem($0)) } }

        var vocab: [String: Double] = [:]
        entries = records.map { s in
            let nameWords = words(s.name) + words(s.shortName) + words(s.nameAr)
            let aliasWords = s.aliases.flatMap { words($0) }
            let ownerWords = words(s.owner ?? "")
            let pop = log10(Double(max(s.uniques30d, 0)) + 1)
            for w in nameWords + aliasWords + ownerWords where w.0.count > 1 {
                vocab[w.0, default: 0] += 1 + pop
            }
            return SearchEntry(
                service: s,
                name: SearchText.normalise(s.name).split(separator: " ").joined(separator: " "),
                nameWords: nameWords,
                aliasWords: aliasWords,
                aliasPhrases: s.aliases.map { (SearchText.tokens($0).joined(separator: " "), $0) },
                ownerWords: ownerWords,
                descriptionStems: Set(SearchText.tokens(s.description).map(SearchText.stem)),
                popularity: pop
            )
        }
        for t in SearchVocabulary.localTerms { for w in t.phrase { vocab[w, default: 0] += 12 } }
        for group in SearchVocabulary.synonymGroups { for w in group { vocab[w, default: 0] += 2 } }
        // Words the direct answers understand.
        for w in ["owe", "bills", "balance", "status", "prayer", "dhuhr", "asr", "maghrib", "isha", "fajr", "parking", "expire"] {
            vocab[w, default: 0] += 8
        }
        vocabulary = vocab
    }

    // MARK: Query analysis

    /// Splits the query into words and corrects one or two typos ("visa stetus" → "visa status").
    /// `extraWords` are the resident's own things (plate numbers, car models) so they aren't "corrected".
    func analyse(_ raw: String, extraWords: Set<String> = [], allowCorrection: Bool = true) -> SearchQueryAnalysis {
        let all = SearchText.tokens(raw)
        var meaningful = all.filter { !SearchText.stopWords.contains($0) }
        if meaningful.isEmpty { meaningful = all.filter { $0.count > 1 } }

        var corrected = meaningful
        var didCorrect = false
        if allowCorrection {
            for (i, w) in meaningful.enumerated() where !isKnown(w, extraWords: extraWords) {
                if let fix = correction(for: w) {
                    corrected[i] = fix
                    didCorrect = true
                }
            }
        }

        let localTerm = Self.findLocalTerm(in: corrected)
        var correctedRaw: String? = nil
        if didCorrect {
            // Rebuild the sentence with corrected words in place, keeping the resident's other words.
            var out: [String] = []
            var j = 0
            for w in all {
                if j < meaningful.count, w == meaningful[j] { out.append(corrected[j]); j += 1 } else { out.append(w) }
            }
            correctedRaw = out.joined(separator: " ")
        }
        let correctedAll = correctedRaw.map { SearchText.tokens($0) } ?? all
        return SearchQueryAnalysis(raw: raw, allWords: correctedAll, words: corrected, corrected: correctedRaw, localTerm: localTerm)
    }

    private func isKnown(_ w: String, extraWords: Set<String>) -> Bool {
        if w.count < 5 || w.contains(where: \.isNumber) || !w.allSatisfy(\.isASCII) { return true }
        if extraWords.contains(w) || vocabulary[w] != nil || vocabulary[SearchText.stem(w)] != nil { return true }
        if SearchVocabulary.synonyms[SearchText.stem(w)] != nil { return true }
        // Still typing: a prefix of a known word isn't a typo.
        return vocabulary.keys.contains { $0.hasPrefix(w) }
    }

    private func correction(for w: String) -> String? {
        let allowance = SearchText.typoAllowance(w)
        guard allowance > 0 else { return nil }
        var best: (word: String, distance: Int, weight: Double)?
        for (candidate, weight) in vocabulary where abs(candidate.count - w.count) <= allowance && candidate.count >= 3 {
            let d = SearchText.levenshtein(w, candidate, limit: allowance)
            guard d <= allowance else { continue }
            if best == nil || d < best!.distance || (d == best!.distance && weight > best!.weight) {
                best = (candidate, d, weight)
            }
        }
        return best?.word
    }

    private static func findLocalTerm(in words: [String]) -> SearchLocalTerm? {
        // Longest phrase first ("emirates id" before "eid").
        for term in SearchVocabulary.localTerms.sorted(by: { $0.phrase.count > $1.phrase.count }) {
            let n = term.phrase.count
            guard n <= words.count else { continue }
            for start in 0...(words.count - n) where Array(words[start..<start + n]) == term.phrase {
                return term
            }
        }
        return nil
    }

    // MARK: Ranking

    /// Catalogue records for the query, best first. `personal` adds a small boost for services
    /// tied to the resident's own things (a bill due, a request in progress).
    func services(for q: SearchQueryAnalysis, personal: Set<String> = []) -> [SearchMatch] {
        guard !q.words.isEmpty else { return [] }
        let phrase = q.words.joined(separator: " ")
        let termWords = Set(q.localTerm?.phrase ?? [])

        var matches: [SearchMatch] = []
        for e in entries {
            let termRank = q.localTerm?.serviceIDs.firstIndex(of: e.service.id)
            var total = 0.0
            var exact = true
            var highlight: String? = nil
            var aliasHit: String? = nil
            var failed = false

            for (i, w) in q.words.enumerated() {
                let isLast = i == q.words.count - 1
                if termRank != nil, termWords.contains(w) {
                    total += 1
                    if highlight == nil, e.service.name.range(of: w, options: [.caseInsensitive, .diacriticInsensitive]) != nil { highlight = w }
                    continue
                }
                let (s, field) = Self.score(word: w, isLast: isLast, in: e)
                if s <= 0 { failed = true; break }
                total += s
                if s < 0.95 { exact = false }
                if field == .name, highlight == nil || w.count > (highlight?.count ?? 0) {
                    if e.service.name.range(of: w, options: [.caseInsensitive, .diacriticInsensitive]) != nil { highlight = w }
                }
                if field == .alias, aliasHit == nil {
                    aliasHit = e.aliasPhrases.first { $0.normalised.split(separator: " ").contains { $0.hasPrefix(w) } }?.original
                }
            }
            if failed { continue }

            var score = total / Double(q.words.count) * 100
            if e.name.hasPrefix(phrase) { score += 15 }
            else if e.name.contains(" " + phrase) { score += 8 }
            if let alias = e.aliasPhrases.first(where: { $0.normalised == phrase }) {
                score += 20
                aliasHit = alias.original
            }
            if let rank = termRank {
                score += rank == 0 ? 40 : 18
                if let term = q.localTerm, !e.name.contains(term.phrase.joined(separator: " ")) { aliasHit = term.label }
            }
            score += e.popularity * 1.6
            if personal.contains(e.service.id) { score += 8 }
            if !e.service.isLive { score -= 6 }
            guard score >= 45 else { continue }

            // Only say "also called" when the name doesn't already contain the word.
            if let a = aliasHit, e.service.name.range(of: a, options: [.caseInsensitive, .diacriticInsensitive]) != nil { aliasHit = nil }
            let isExact = exact && (q.words.count >= 2 || termRank != nil || e.aliasPhrases.contains { $0.normalised == phrase } || e.name == phrase)
            matches.append(SearchMatch(service: e.service, score: score, highlight: highlight, alsoCalled: aliasHit, isExact: isExact))
        }
        return matches.sorted { $0.score != $1.score ? $0.score > $1.score : $0.service.uniques30d > $1.service.uniques30d }
    }

    private enum Field { case name, alias, owner, description }

    /// How well one query word matches a record: exact or same stem 1, prefix 0.9 (last word) /
    /// 0.75, synonym 0.95, a typo 0.55; aliases ×0.95, owner ×0.8, description words 0.35.
    private static func score(word w: String, isLast: Bool, in e: SearchEntry) -> (Double, Field) {
        let ws = SearchText.stem(w)
        let syn = SearchVocabulary.synonyms[ws] ?? []
        var best: (Double, Field) = (0, .name)

        func consider(_ words: [(word: String, stem: String)], weight: Double, field: Field) {
            for (word, stem) in words {
                var s = 0.0
                if word == w || stem == ws { s = 1 }
                else if w.count >= 2, word.hasPrefix(w) { s = isLast ? 0.9 : 0.75 }
                else if syn.contains(stem) { s = 0.95 }
                else if w.count >= 5, SearchText.levenshtein(word, w, limit: 1) <= 1 { s = 0.55 }
                if s * weight > best.0 { best = (s * weight, field) }
                if best.0 >= 1 { return }
            }
        }
        consider(e.nameWords, weight: 1, field: .name)
        if best.0 < 1 { consider(e.aliasWords, weight: 0.95, field: .alias) }
        if best.0 < 0.8 { consider(e.ownerWords, weight: 0.8, field: .owner) }
        if best.0 < 0.35, e.descriptionStems.contains(ws) { best = (0.35, .description) }
        return best
    }
}
