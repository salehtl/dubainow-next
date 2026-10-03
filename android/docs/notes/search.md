# Search (Android) — build notes

Paper p-W-0 · 04 Search: `D8G-0` rules, `CQB-0` Ready, `CVP-0` Results as you type, `D2W-0` Direct answer,
`D5U-0` Did you mean. Android kit `MC2-0` (search field). Ported from `ios/DubaiNowDemo/Features/Search/*.swift`.

## What's built
- **SearchScreen.kt**: `fun SearchScreen()`, the search tab as a **full-screen M3 search view**. The shell hides the
  bottom bar on `AppTab.Search`. The screen draws a 72 dp white header (back arrow → `router.tab = AppTab.Home`,
  `BasicTextField` bound to `router.searchQuery` with the iOS prompt `Services, fines, "how much do I owe"`, clear ✕ when
  there's text, mic otherwise, which opens the assistant listening (`openAssistant("__listen__")`, signed-in only), IME
  action Search adds to recents and lowers the keyboard; keyboard autocorrect is off because search corrects typos itself
  and the keyboard would rewrite Mulkiya, Ejari, Salik). There's a hairline divider under the header, and the content sits
  on the grey canvas below.
  - The field is focused and the keyboard raised on open. Neither happens when you come back from a result pushed in the search tab.
  - Tapping a recent search or a suggested question searches and lowers the keyboard (it's a submitted search).
  - A new query always starts at the top of the list (`requestScrollToItem(0)`; keyed items would otherwise keep the old
    scroll anchor and hide a new Top result or answer above it).
  - Coming back from a pushed result keeps "Search instead for …", "See all" and the scroll position (`SearchSession`
    holds them, because the shell disposes the screen on push). Leaving the view always lowers the keyboard.
  - Results update after a 150 ms debounce once the query has 2 or more characters. Sections are `LazyColumn` items with `animateItem` fades
    (skipped when "Remove animations" is on). Dragging the list lowers the keyboard, and `imePadding` keeps the end reachable.
  - The query is kept for 10 minutes after leaving the tab (Paper rule), then cleared.
  - TalkBack: a polite live region says "N results" after a pause in typing. Section headers are headings, and every row/card
    merges its semantics.
  - **Ready**: Recent chips (per resident, session-only; Sara starts with the 4 Paper chips; Clear). Then "Ask about your things"
    (up to 3 questions from the resident's own data) and an **Ask DubaiNow** row (`router.openAssistant()`). Then "Popular in Dubai
    this week" (top 5 live by `uniques30d`). Guest: "Try asking" plus a sign-in row.
  - **Results**, in Paper order: the correction header ("Showing results for **mulkiya** · Search instead for mulkia"), the direct
    answer with its footnote, then the Top result (an account card, or a service card with "Also called" and owner chips and the
    resident's context strip). Then Services (3, then "See all N" expands in place; "Also matching" / "Related services"), Your
    things (6), Widgets you can add, Help and guides, and "Not what you need?" (Ask DubaiNow with the query → `openAssistant(q)`,
    Browse by topic → Services tab, A to Z → pushes `Route.AllServices`). There's also an empty state.
  - Tapping a result pushes in the search tab (`router.push` / `router.open`). Sheets: Payment, Top up Salik, Extend parking.
  - "Pay one at a time" uses an M3 `AlertDialog` that lists the payables (iOS has a confirmation dialog here). Rows show
    "DEWA · AED 412.60" under the title so nothing wraps in the narrow dialog.
- **SearchEngine.kt**: ranking ported 1:1. It covers local terms (Mulkiya, Ejari, Amer, Tasjeel, Tasheel, MOHRE, Salik, nol, DEWA,
  Emirates ID/EID/Hawiya, good conduct/PCC, Iqama, Mawaqif, mParking, NOC, Salik recharge), Arabic names and aliases (سالك
  finds Salik), synonyms and UK/US spellings, stems and plurals, prefix while typing, and typo tolerance (1 edit for 5–7 letters,
  2 edits for 8+). The index builds lazily (warmed up on `Dispatchers.Default` when the screen opens).
- **SearchModel.kt**: the resident's things, intent detection, results assembly, related services, answer sources and
  context lines. These are extension functions on `DemoStore` that use only its public API.
- **SearchAnswers.kt** has the answer cards:
  - **owe**: "Pay both · AED 1,012.60" → `SheetRoute.Payment(store.payAllRequest())`. With one payable it pays that one. There's also "Pay one at a time".
  - **request status**: steps, notice and visa expiry → pushes `Route.Request`.
  - **Salik / nol balance**: → `TopUpSalik` / opens `top-up-nol-card`.
  - **prayer**: named or next, from `PrayerTimes`, with "Add prayer times to Home".
  - **parking**: live minutes left → `ExtendParking`.
  - **guest**: a "Sign in with UAE PASS" card.
- **SearchComponents.kt** has the rows, section/group, recents, highlight (bold where a query word starts a word, accent-folded),
  top cards, chips and the correction/footnote/capsules.

Large text (checked at 200 % with `searchFontScale 2`): the top-thing card, the owe rows, the prayer row, the answer header and
the context strip stack. The pay-all button drops the amount instead of truncating it. Number plates keep their lettering at
100 %, like a logo.

## Debug extras
`persona sara tab search` plus:
- `searchQuery sal`. Use underscores for spaces, because adb splits extras on spaces: `how_much_do_I_owe`, `mulkia`, `visa_stetus`,
  `salek`, `when_is_dhuhr`, `parking`, `salik_balance`, `amer`, `renew_my_car_registration`, `ejari`, `driving_license`,
  `سالك`. This is applied once per run.
- `searchHideKeyboard true`: don't focus or raise the keyboard (for screenshots).
- `searchScrollToEnd true`, `searchScrollBy 900` (px).
- `searchFontScale 2`: preview the screen at 200 % text without touching the device setting.
- `searchType renew_my_car_registration`: types the text one key every 320 ms (debounce, list updates, scroll reset).
- `searchRoundTrip true`: opens "See all", scrolls, pushes A to Z and pops back after 1.5 s (state must survive; wait ~11 s).
- `searchPayOne true`: opens the "Pay one at a time" dialog (use with `searchQuery how_much_do_I_owe`).

## Requests for shared code
1. **NumberPlate** has a fixed height (26/34 dp) but sp lettering, so it clips at large text sizes. Search wraps it to 100 %
   (`SearchPlate`). Suggest fixing that in the component, or keeping plates at a fixed scale everywhere.
2. **ServiceTypeBadge** has a fixed 24 dp height, so "All at once" clips at 200 %. Suggest `heightIn(min = 24.dp)`.
3. **DNIcons** has no mapping for `list.bullet`, `car`, `p.square`, `moon.stars`, `person.crop.circle`, `lock.fill`, `calendar`,
   `info.circle`, `doc.badge.clock` or `sun.max.fill`. Search maps these privately in `searchIcon()`. Worth adding them to DNIcons.
4. **CatalogueRow `highlight`** bolds only the first substring and ignores word starts and accents. Search uses its own row,
   `SearchServiceRow` (36 dp disc, regular title with bold matches, like the iOS search row).
5. **Shell**: `AnimatedContent` disposes the search screen when a result is pushed. Search now keeps its own state across a push
   (`SearchSession`), but a `SaveableStateHolder` per tab route would fix this for every feature. Also, `MainScaffold`'s back
   handler has a no-op ternary (`if (tab == Search) Home else Home`).
6. Promote `searchQuery` to `LaunchOptions` if you want it permanent (and decode `_` → space there).
7. Same as iOS: the Salik dispute `RequestItem` uses `serviceID: "top-up-salik-account"`, the catalogue has no time
   estimates ("about 2 minutes"), and guest "Sign in" calls `store.signOut()` to reach Welcome.
8. **Back from search goes to Home, not to where search was opened.** M3's search view closes back to the screen it opened
   from; the shell (system back) and search's back arrow both go to Home because nothing records the tab search was opened
   from. Suggest `Router.searchOrigin` (set when the search circle is tapped) and use it in both places.
9. **iOS copy bug** (fixed here, worth fixing on iOS): with nothing to pay and no car, the owe footnote reads "your linked  accounts"
   (empty list, double space). Android says "your linked accounts".

## Review pass (independent reviewer)
Checked ranking for `sal`, `mulkia`, `ejari`, `visa stetus`, `how much do I owe`, `dhuhr` and guest against the iOS logic
(port is 1:1), plus Khalid, Faisal and Wei. No crashes. Fixed: the field placeholder (now the iOS prompt), 48 dp targets
(recent chips, "Clear"/"See all", "Search instead for", without changing the section rhythm), plates read twice by TalkBack in
Your things, "Clear" now says "Clear recent searches", the scroll anchor on a new query, state lost on return from a pushed
result, the keyboard staying up after tapping a suggestion, keyboard autocorrect, the narrow pay-one dialog rows and the empty
owe footnote. Not changed: back still goes to Home (shared request 8).

