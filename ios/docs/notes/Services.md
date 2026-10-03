# Services feature — notes

Built from Paper C1 `DBS-0` (Browse), `DJ8-0` (Topic), `DT8-0` (Service start), `DP3-0` (journey layout),
C2 "Service types and naming" + blueprints. All numbers and sub-topics come from `Catalogue.shared`
(219 services, 12 topics, 40 entities and providers), not the older Paper copy (312 / 28).

## What's here
| File | Screen |
|---|---|
| `ServicesView` | Tab root: large "Services" header, For you shelf (persona only), Browse by topic grid (1 column at AX sizes), All at once shelf (personal bundles first), Start to finish shelf (started first, with progress), Browse another way (A to Z 219, By entity 40) |
| `TopicView` | Hue disc header, "N services from …" (top owners), sub-topic chips that scroll to their section, personal banner (renewal › your-turn step › bill/fine › journey), up to 3 rows per sub-topic + "See all N", bundles/journeys of the topic as cards |
| `SubtopicView` | Every record in a sub-topic (standalone by use, then bundles/journeys) |
| `ServiceStartView` | Start page (`DT8-0`): star (Favorites + toast) and ⋯ (Share, Report a problem), breadcrumb, also-called, chips, description, personal cards (vehicle + "Before you renew" checks, bills/fines, request in progress, draft), primary action (Pay AED x / Start now / Donate / Continue your request / Continue your draft), curated sections for renew-vehicle-registration, "About this service" + "Also done as part of" + Related for the rest, "Run by …" footer. Routes to a bundle/journey page if given one. |
| `BundleView` | "All at once" page: stacked discs, personal count line, Pay all (payables + total → payment sheet; "Nothing to pay"; guest → Sign in), family visas (members, due chips), what it does (second person), views, Covers (parts as rows), entities |
| `JourneyView` | `DP3-0` layout: overline, scale chips, progress card, Continue / Start this journey, vertical step list (done ✓ / current with halo / upcoming), parts as links (current stage as pills), "not in DubaiNow yet" items with owner (dashed). `after-a-death` is quiet: no hue, neutral marks, condolence line, "Start when you're ready". |
| `AllServicesView` | Native plain `List`, sticky letter sections + section index, segmented filter (All · Standalone · All at once · Start to finish), its own `.searchable` |
| `EntitiesView` / `EntityView` | DubaiNow first, entities A to Z, "Owner to confirm" last; entity page grouped by topic |
| `BundlesListView` / `JourneysListView` | Full-width cards |
| `ServicesData` | Lists every screen shows + `extension DemoStore` personal helpers (`services…`) |
| `ServicesComponents` | Tiles, cards, banner, `ServicesRow` (row + sibling star button), flow layout, shelf, toolbar |
| `ServicesReachability` | Count check (see below) |

## Launch arguments
```
-persona sara -tab services                                   # Browse
-persona sara -tab services -route topic:driving-and-transport
-persona sara -tab services -route subtopic:driving-and-transport/vehicles
-persona sara -tab services -route service:renew-vehicle-registration
-persona sara -tab services -route service:pay-traffic-fine | service:donate-to-charity | service:check-visa-status
-persona sara -tab services -route bundle:pay-all | bundle:renew-family-visas
-persona sara -tab services -route journey:move-home | journey:after-a-death
-persona khalid -tab services -route journey:build-a-house
-persona sara -tab services -route allServices | entities | entity:RTA | bundlesList | journeysList
-persona guest -tab services
```
Extra (inside this folder only): `-servicesScroll <id>` scrolls a Services screen on launch (ids: `topics`,
`bundles`, `journeys`, `browse`; a sub-topic id such as `fines` or `section-multi` on a topic; `personal`,
`details` on start/bundle pages; `steps` on a journey; a letter on A to Z). `-servicesCheck` runs the count check.

## Count check — every record reachable
`-servicesCheck` prints and writes `tmp/services-check.txt` in the app's data container:
```
catalogue: 219 services (210 standalone, 4 bundles, 5 journeys)
reachable: 219 of 219 · unreachable: none
via topics: 216 (topic counts add up to 216) · via shelves: 9 · via A to Z: 219 · via entities: 219 (41 rows)
not under a topic (reached from shelves, A to Z or entities instead): edge-of-life-review
```
(`pay-all` and `get-certificates` have no topic; they're on the All at once shelf and list.)

## Requests for shared code
1. **Stray search field on Services (App/RootView).** `.searchable` on the `TabView` also puts a search field
   in the nav bar of tab roots whose content scrolls (Services shows "Services, fines, …" under the header).
   Tested in my copy: moving `.searchable(text: $router.searchQuery, prompt: …)` onto the search tab's
   `NavigationStack` removes it from Services and the search tab still gets its bottom field. (Reverted.)
2. **NOTES.md files are bundled.** Synchronised folders copy `*.md` into the app; two features with NOTES.md
   fail the build ("Multiple commands produce …/NOTES.md"). Exclude `.md` from the target.
3. **`BundleCard` / `JourneyCard`** have a fixed 176 pt height, so text clips at AX sizes. Suggest `minHeight`.
   `BundleCard` shows `doesTheWork`, which the catalogue writes for staff ("the resident's accounts"); the bundle
   page uses second-person copy (`ServicesBundlePage.doesTheWork`), worth moving into the catalogue or the card.
4. **`CatalogueRow`'s star is inside the row**, so wrapping the row in a Button nests buttons. `ServicesRow` puts
   its own star (`ServicesStarButton`) beside a `CatalogueRow(trailing: .none)` instead. Search could reuse it.
   `CatalogueRow` also ignores `context` for bundles/journeys.
5. **Store helpers to promote** (all read-only over existing state, in `ServicesData.swift`): `servicesContext(for:)`,
   `servicesForYou`, `servicesBanner(forTopic:)`, `servicesBundleCount(_:)`, `servicesRequests(for:)`,
   `servicesRenewal(for:)`, `servicesVehicleForRenewal()`, `servicesPaymentRequest(for:)`.
6. **Request → service link:** Sara's Salik dispute uses `serviceID: top-up-salik-account`. Requests count as
   "this service" only when the title matches the service name, so the top-up page doesn't show the dispute.
   A `dispute-salik-charge` record (or a request kind) would be cleaner.
7. **Catalogue gaps:** `edge-of-life-review` has no topic and no owner ("Edge of Life (to confirm)"), and is
   reachable only via A to Z and "Owner to confirm". `donate-to-charity` is "proposed", but the donate flow works
   in the demo, so its status chip is hidden on its page.
