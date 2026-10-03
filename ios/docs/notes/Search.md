# Search — build notes

Paper p-W-0 · 04 Search: `D8G-0` rules, `CQB-0` Ready, `CVP-0` Results as you type, `D2W-0` Direct answer, `D5U-0` Did you mean.

## What's built
- **SearchView.swift** — the search tab's page. Uses the native search field (`router.searchQuery`, bound on the TabView); no extra field. 150 ms debounce, results from 2 characters, results cross-fade in place (`DNMotion.fast`, slot-keyed rows with opacity transitions), `.scrollDismissesKeyboard(.immediately)`, VoiceOver announces "N results" after a pause in typing. Inline nav title + logo (`.dnPushedToolbar()`); the large "Search" title is drawn in the content because iOS hides the bar while the field is active.
  - **Ready**: Recent (session-local, per resident; Sara starts with the 4 Paper chips; Clear), "Ask about your things" (up to 3 questions from the resident's own data) + "Ask DubaiNow" row (`router.openAssistant()`), "Popular in Dubai this week" (top 5 live by `uniques30d`). Guest: "Try asking" + a sign-in row.
  - **Results** in Paper order: correction header → direct answer + footnote → Top result (account card, or a service card with "Also called" chip and the resident's context strip) → Services (3, then "See all N" expands in place; "Also matching" / "Related services") → Your things → Widgets you can add → Help and guides → "Not what you need?" (Ask DubaiNow with the query, Browse by topic, A to Z). Empty state for no results.
- **SearchEngine.swift** — ranking on top of the catalogue: local terms (Mulkiya, Ejari, Amer, Tasjeel, Tasheel, MOHRE, Salik, nol, DEWA, Emirates ID/EID/Hawiya, good conduct/police clearance/PCC, Iqama, Mawaqif, NOC…), aliases incl. Arabic (سالك finds Salik), synonyms and UK/US spellings (car→vehicle, licence/license…), stems/plurals, prefix while typing, typo tolerance (Levenshtein 1 for 5–7 letters, 2 for 8+) with "Showing results for X · Search instead for Y".
- **SearchModel.swift** — the resident's things (accounts, bills/fines, vehicles + Salik tags, requests, documents, drafts, family, journeys), intent detection, related services, answer sources. All in `extension DemoStore` using existing state only.
- **SearchAnswers.swift** — answer cards: owe (Pay both → `payAllRequest()`, Pay one at a time → confirmationDialog), request status (visa: steps, notice, expiry → push `.request`), Salik/nol balance (→ `.topUpSalik` / top-up-nol-card), prayer (named or next, from `PrayerTimes`, add widget), parking (live minutes left → `.extendParking`), guest sign-in card.
- **SearchComponents.swift** — section header/group, chips, rows, top cards, flow layout, recents store.

## Launch arguments (screenshots)
`-persona sara -tab search` plus:
- `-searchQuery "sal"` · `"how much do I owe"` · `"mulkia"` · `"visa stetus"` · `"salek"` · `"when is dhuhr"` · `"parking"` · `"salik balance"` · `"amer"` · `"renew my car registration"`
- `-searchHideKeyboard` lowers the keyboard; `-searchScrollToEnd` scrolls to the bottom.
These are read inside `SearchView` (`SearchLaunch`), applied once per run. Remove or promote to `LaunchOptions` as you like.

## Requests for shared code
1. **CatalogueRow `highlight` doesn't render bold.** `inlinePresentationIntent = .stronglyEmphasized` is lost under `.dnFont(.rowTitle)` (explicit `.medium` weight). Verified on device: highlight "sal" → no bold. Search uses its own `SearchServiceRow` (Paper's search row: 36 pt disc, 58 pt, regular title with bold match) and `SearchHighlight` (bold works when followed by `.fontWeight(...)`). Suggested fix in CatalogueRow: add `.fontWeight(.medium)` after `.dnFont(.rowTitle)` on the title, or set `attributed[range].font = DNTextStyle.rowTitle.font.bold()`; it also mixes indices from `lowercased()` with the original string.
2. Promote `-searchQuery` into `LaunchOptions` if you want it permanent.
3. `RequestItem` for the Salik dispute uses `serviceID: "top-up-salik-account"`; search only links requests to a service when the titles match, so that row doesn't say "in review". A `dispute-salik-charge` record (Paper shows "Dispute a Salik charge", "Check your Salik trips") would help.
4. Catalogue has no time estimates ("about 2 minutes", "5 min" in Paper), so rows show owner + personal context instead.
5. Guest "Sign in" calls `store.signOut()` to reach Welcome (UAE PASS). Swap for a direct sign-in sheet if one appears.
