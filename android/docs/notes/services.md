# Services feature (Android) — notes

Port of `ios/DubaiNowDemo/Features/Services/*` (same content, logic and copy) with Android chrome from the
Paper Android kit. Built from Paper C1 `DBS-0` (browse), `DJ8-0` (topic), `DT8-0` (start page), `DP3-0`
(journey layout), C2 "Service types and naming", and the Android sheets `LOA-0` (list rows), `MC2-0`
(fields/navigation), `MDV-0` (overlays), `OFF-0` (service flow). All numbers come from `Catalogue.shared`
(219 services, 12 topics, 40 entities and providers), not the older Paper copy (312 / 28).

## Files
| File | What |
|---|---|
| `ServicesScreen` | Tab root: `MainHeader`, "Services" + count line, For you shelf (persona only), Browse by topic grid (1 column at font scale ≥ 1.5), All at once shelf (personal bundles first), Start to finish shelf (started first, with progress), Browse another way (A to Z 219, By entity 40) |
| `TopicScreen` | Hue wash, disc header, "N services from …", sub-topic chips that scroll to their section, personal banner (renewal › your-turn step › bill/fine › journey), up to 3 rows per sub-topic + "See all N", the topic's bundles/journeys as cards. Share button in the header. |
| `SubtopicScreen` | Every record in a sub-topic (lazy grouped rows) |
| `ServiceStartScreen` | Start page; routes bundles/journeys to their pages. Star + ⋮ (Share, Report a problem) in `PushedHeader`, breadcrumb, also-called, chips, description, personal cards (vehicle + "Before you renew", bills/fines + "Pay with your other bills", request in progress, draft), donation (charity radio list, Show all 20, amount `SegmentedButton`), primary action (Pay AED x / Start now / Donate AED x / Continue/View your request / Continue your draft / Open), renew: "Ask DubaiNow to do it" → `router.openAssistant("Renew my car registration")` and "Renew a different vehicle" (M3 `AlertDialog`), curated sections for renew-vehicle-registration, otherwise About / Also done as part of / Related, "Run by …" footer |
| `BundleScreen` | `ServicesBundlePage`: stacked discs, count line, Pay all (payables + total → `SheetRoute.Payment(store.payAllRequest())`, "Nothing to pay", guest → Sign in = Profile sheet), family visas card, What it does for you (second person), Ways to see it, Covers (lazy rows), Who DubaiNow talks to |
| `JourneyScreen` | `ServicesJourneyPage` (`DP3-0`): overline, chips, progress card, Continue / Start this journey, step list (done ✓ / current with halo + tonal part pills / upcoming), parts as links, dashed "not in DubaiNow yet" items. `after-a-death` is quiet: no wash, neutral marks, condolence line, inverse "Start when you're ready". |
| `AllServicesScreen` | Title + in-page search pill (fixed), type filter (`SingleChoiceSegmentedButtonRow`), summary, sticky letter headers (`stickyHeader`), 219 lazy rows, letter rail (tap/drag) |
| `EntitiesScreen` / `EntityScreen` | DubaiNow first, entities A to Z, "Owner to confirm" last (41 rows); search; entity page grouped by topic |
| `BundlesListScreen` / `JourneysListScreen` | Header + full-width cards |
| `ServicesData` | Lists every screen shows + `DemoStore.services…` personal helpers (read-only) + `ServicesText` |
| `ServicesComponents` | Page frame (`ServicesPage`: wash + header + LazyColumn with bottom-bar padding), tiles, cards, banner, `ServicesRow` (row + sibling star), group helpers, chips, links, record actions, search field, `ServicesButton` (icon after text), `ServicesPlate` (fixed-size plate), `ServicesChoiceChip` (M3 filter chip used instead of segments at large text) |
| `ServicesReachability` | Count check |

Android adaptations: tonal circle buttons (shared `TonalIconButton`), `more_vert` overflow `DropdownMenu`,
system share sheet, `AlertDialog` vehicle chooser, M3 `RadioButton`s, `SegmentedButton`s (filter and amount),
ripple everywhere, Android navigation rows without chevrons on "Browse another way" (count under the title,
Paper `LOA-0`). Service rows/cards stay Shared (same as iOS). Reduce motion: section jumps don't animate when
"Remove animations" is on. Every long screen is a `LazyColumn` with keys.

## Launch extras
```
persona sara tab services                                          # Browse
persona sara tab services route topic:driving-and-transport
persona sara tab services route subtopic:driving-and-transport/vehicles
persona sara tab services route service:renew-vehicle-registration
persona sara tab services route service:pay-traffic-fine | service:donate-to-charity | service:check-visa-status
persona sara tab services route bundle:pay-all | bundle:renew-family-visas
persona sara tab services route journey:move-home | journey:after-a-death
persona khalid tab services route journey:build-a-house
persona sara tab services route allServices | entities | entity:RTA | bundlesList | journeysList
persona guest tab services
```
Debug extras (this folder only, each fires once per process):
- `servicesCheck true` — runs the count check (logcat tag `ServicesCheck`, also `files/services-check.txt`)
- `servicesScroll <id>` — scrolls on launch. Browse: `topics` `bundles` `journeys` `browse`; topic: a sub-topic
  id (`fines`) or `section-multi`; start/bundle: `personal` `actions` `details`; journey: `steps`; A to Z: a letter; entities: `list`
- `servicesFilter all|standalone|bundle|journey`, `servicesQuery <text>` — A to Z (query also on entities)
- `servicesMenu true` (opens ⋮), `servicesStar true` (toggles the star → toast), `servicesVehicleDialog true`
- `servicesCharity <option id>`, `servicesAmount 10|50|100|500`, `servicesAllCharities true` — donate page
- `servicesFontScale 2` — lays every Services screen out at that font scale (header included) without touching
  the shared emulators' settings. Linear scaling, so it's harsher than Android 14's non-linear 200%.

## Count check — every record reachable (same as iOS)
```
[ServicesCheck] catalogue: 219 services (210 standalone, 4 bundles, 5 journeys)
[ServicesCheck] reachable: 219 of 219 · unreachable: none
[ServicesCheck] via topics: 216 (topic counts add up to 216) · via shelves: 9 · via A to Z: 219 · via entities: 219 (41 rows)
[ServicesCheck] not under a topic (reached from shelves, A to Z or entities instead): edge-of-life-review
```

## Requests for shared code
1. **`DemoDate.short` prints "15 Sept"** on Android (`Locale.UK` + CLDR); iOS prints "15 Sep". Use `Locale.US`
   for `MMM`, or a fixed month table, in `data/Formatting.kt`.
2. **`DNIcons` missing mappings** used by Services: `clock`, `banknote`, `building.columns`, `person.2`,
   `list.number`, `line.3.horizontal.decrease`, `wrench.and.screwdriver`, `mappin.and.ellipse`,
   `arrow.up.forward.square`, `square.and.arrow.up`, `exclamationmark.bubble`. Mapped locally in
   `servicesIcon()`. Because `ComingSoonContent` renders `HueIcon(symbol)`, the "Other ways" sheet uses
   `map.fill` and the "not in DubaiNow yet" sheet uses `building.columns.fill` instead of the iOS symbols.
   (`Icons.Rounded.Accessible` in `design/Icons.kt` is deprecated → `Icons.AutoMirrored.Rounded.Accessible`.)
3. **`FavoriteStar`** (shared) shows no "Removed from Favorites" toast and has a generic label; Services uses its
   own `ServicesStarButton` / `servicesToggleFavorite` (iOS behaviour: removed toast, "Add Renew registration to
   Favorites" labels). Search could reuse it.
4. **`CatalogueRow` ignores `context` for bundles/journeys** (same as iOS note). **`BundleCard`** shows the
   catalogue `description`; iOS cards show it too, but the bundle page uses second-person `doesTheWork` copy
   (in `BundleScreen.kt`) — worth moving into the catalogue.
5. **`PushedHeader` has no title.** Pushed screens put their title in the content (A to Z keeps it fixed with the
   search field). If the shell adds an M3 top-app-bar title later, pass it from here.
6. **Store helpers to promote** (read-only, in `ServicesData.kt`): `servicesContext`, `servicesForYou`,
   `servicesBanner`, `servicesBundleCount`, `servicesRequests`, `servicesRenewal`, `servicesVehicleForRenewal`,
   `servicesPaymentRequest`.
7. **Request → service link / catalogue gaps** (same as iOS): Sara's Salik dispute is filed under
   `top-up-salik-account`, so requests count as "this service" only when the title matches;
   `edge-of-life-review` has no topic and no owner (reachable via A to Z and "Owner to confirm");
   `donate-to-charity` is "proposed" but the donate flow works, so its status chip is hidden.
8. Khalid's `build-a-house` progress shows once his persona port lands (code reads `store.state.journeys`).
9. **`StatusChip` has a fixed 26 dp height**, so its text clips at large font sizes ("Due in 12 days" on the For you
   cards, "Due" on family visas). Suggest `heightIn(min = 26.dp)` with vertical padding.
10. **`NumberPlate` uses sp inside a fixed-height box**, so the plate number is cut off at large font sizes. iOS uses
    fixed point sizes for the plate; Services wraps it in `ServicesPlate` (font scale 1). Suggest `dp`-based type in
    the shared component, then `ServicesPlate` can go.

## Large text (200%)
Checked with `servicesFontScale 2` on Browse, topic, start (renew, fine, donate), Pay all, journey, A to Z and
entities. Reflows: one-column topic tiles, stacked banner, A to Z type filter and donation amounts become wrapping
M3 filter chips (above 1.15), fact tables put each value under its label, payable amounts go under the title,
entity monograms, stage numbers and the plate keep fixed type (decorative, as on iOS). Remaining clipping is in
shared components (requests 9 and 10).

## Review pass (3 Oct)
Fixed: inline links (Related, journey parts) were 44 dp → 48 dp with a button role; the breadcrumb clipped long
sub-topic names mid-word → ellipsis like iOS; bundle pages without a personal card (Certificates, POD benefits,
Family visas as a guest) had an empty 24 dp block before Start now; journey stage markers were read by TalkBack
("1", "2") before the stage heading → hidden, as on iOS; the "What you need" bullets left an empty focusable node
→ cleared; charity list and vehicle chooser are now radio groups (`selectableGroup`); For you card text ends with
an ellipsis instead of clipping; the 200% issues above.

## QA pass (3 Oct)
- **A to Z kept its place only until you came back (services-1).** The "back to the top on a new filter" effect was
  `LaunchedEffect(filter)`, which also ran each time the screen re-entered composition (Back from a pushed page, or a
  tab switch), so a restored scroll position jumped to letter A. It now reacts only to a real change
  (`snapshotFlow { filter }.drop(1)`). Checked: R on the rail → Activity tab → Services keeps R (old build: A); with
  the shared Back-state fix (shared-3) in place, R → open "Register a vehicle" → Back also returns at R.
- **"Start to finish" wrapped inside its segment (services-2).** Four equal M3 segments leave about 69 dp per label on
  a 411 dp phone and "Start to finish" needs 77 dp at Caption1 SemiBold (Caption2 would fit with no margin to spare).
  The filter now measures its labels (`BoxWithConstraints` + `rememberTextMeasurer`): segments, one line each, when
  all four fit; otherwise the wrapping M3 filter chips (Paper: filter chips narrow a list that's on screen). That
  replaces the font-scale > 1.15 switch in "Large text" above, so most phones now show chips at the default size and
  wide screens or smaller text show the segments. Copy unchanged. Checked at `servicesFontScale` 0.85 (segments),
  1 (chips) and 2 (chips).
