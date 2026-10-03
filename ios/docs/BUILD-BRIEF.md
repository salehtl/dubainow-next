# DubaiNow demo app — build brief (read fully before writing code)

**What we're building:** a native SwiftUI iPhone app that demos the *core* of the new DubaiNow
(Direction A, "Canvas"), designed in Paper. It is a **demo**, not production: no real accounts,
payments or government systems. It must feel real and polished in the iOS Simulator (and later
TestFlight) when the product owner's boss taps through it. Service flows themselves come later;
every one of the 219 catalogue records must still be *listed* (including 4 bundles and 5 journeys),
and tapping into anything outside the core lands on a designed "coming next" state — never a dead end.

## Decisions (from the product owner — do not revisit)
- **Native iOS first.** Use iOS's own components wherever they exist: `TabView` (+ search tab), `NavigationStack`,
  toolbars, `.sheet` with detents, `.alert`, `.confirmationDialog`, `Menu`, `.contextMenu`, `.swipeActions`,
  `Toggle`, `Picker(.segmented)`, `DatePicker`, `ProgressView`, `ShareLink`, `.searchable`. Liquid Glass comes from the
  system (toolbars, tab bar, sheets) or `.glassEffect()` / `.buttonStyle(.glass)` — **glass only on the navigation layer,
  never on content**. Do NOT recreate Paper's four-layer glass (frost/lens/sheen/rim) — that was a Paper approximation.
  Don't set `.presentationBackground` on sheets (it breaks native sheet glass).
- **SF Pro** (system font) everywhere, via our text styles: `.dnFont(.title2)` etc. SF Pro Rounded for numbers
  (`.numericLarge` …). Never Inter.
- **SF Symbols** for all icons (iOS only for now). Paper's SVG icons are just references for which symbol to pick.
- **Light mode only** for this build (Info.plist forces Light). Still use DNColor tokens, never raw hex in views.
- **English only** for this build. Write strings as plain literals in views (String Catalog-ready); don't build Arabic.
- **Demo personas**: Sara (main), Khalid, Faisal, Wei, plus Guest. Signing in = choosing a persona.
- **Fake payments**: a native-feeling confirmation (Face ID style via LocalAuthentication, which works in the
  Simulator) → receipt. Never imitate the Apple Pay sheet itself. No money moves; say so subtly.
- **Assistant is scripted** for the demo.
- **Home grid stretches** to any phone width: 20 pt page insets, 12 pt gutters; favorites 4 columns;
  widgets 2 columns (small = half width square, medium = full width × small height, large = full × full width).
- **Motion**: our own springs `DNMotion.snappy/.smooth/.bouncy` (not SwiftUI's built-in `.snappy` etc.),
  `DNMotion.resolve(_:)` honours Reduce Motion. Haptics via `DNHaptics` exactly as the Paper motion spec says.
- **Copy**: GOV.UK style — plain English, verb first, sentence case, no entity names in service names (owner shown
  beside), UK spelling ("colour", "catalogue") except the feature name "Favorites" (US). Western digits always.
  Amounts as "AED 1,012.60" (`Money.aed`). Dates via `DemoDate` (relative to today, so due dates stay true).

## Where things are
- Project: `/Users/saleh/Developer/Claude Workspace/DubaiNow Next/ios/` — `DubaiNowDemo.xcodeproj`, scheme `DubaiNowDemo`,
  bundle id `com.salehtl.DubaiNowNext`, iOS 26.0+, Swift 5 mode with default MainActor isolation.
  Folders are *synchronised*: any `.swift` file you add under `DubaiNowDemo/` is compiled automatically. Never edit
  `project.pbxproj`.
- Paper file (the design, **read-only** for you): fileId `01M3VK547PT2KNWSB1WPYWFJHJ`. Page "C1 Core app · Canvas" is
  `p-W-0` (screens are 402 pt wide artboards). Design-system pages: 10–16 components, 20–25 service parts, 02–05 foundations.
  Load Paper tools with ToolSearch `select:mcp__Paper__get_screenshot,mcp__Paper__get_jsx,mcp__Paper__get_tree_summary,mcp__Paper__get_node_info,mcp__Paper__find_nodes,mcp__Paper__get_children`
  and read `mcp__Paper__get_guide({topic:"paper-mcp-instructions"})` once. Use `get_jsx` (format `inline-styles`) for exact
  values — large outputs are saved to a file you can parse with python; use `get_screenshot` (scale 0.6–1) to see a screen.
  **Never call Paper write tools** (write_html, update_styles, set_text_content, delete/move/rename/duplicate, create_*,
  set_tokens, finish_working_on_nodes). Another session is editing that file. Budget: ~40 Paper calls.
- Apple skills (read, don't install): `/private/tmp/claude-501/-Users-saleh-Developer-Claude-Workspace-DubaiNow-Next/a423a7e6-7f55-4328-8d48-cc1e30571fe1/scratchpad/apple-skills/skills/`
  — especially `ios/run-simulator/SKILL.md`, `design/liquid-glass/SKILL.md`, `ios/navigation-patterns/*`,
  `swiftui/toolbars/SKILL.md`, `design/sf-symbols/SKILL.md`, `design/animation-patterns/*`, `ios/ui-review/hig-checklist.md`,
  `ios/accessibility-audit/SKILL.md`, `swiftui/layout/SKILL.md`.
- Catalogue data: `DubaiNowDemo/Resources/dubainow-services.json` (bundled; read through `Catalogue.shared`).
- Paper tokens snapshot: `ios/design/tokens.json` (already generated into `DesignSystem/Tokens.swift`).

## How you work (important — 8 builders run at the same time)
1. Make your own working copy and work ONLY there:
   `rsync -a --exclude 'docs' "/Users/saleh/Developer/Claude Workspace/DubaiNow Next/ios/" "$WORK/"` where
   `WORK=/private/tmp/claude-501/-Users-saleh-Developer-Claude-Workspace-DubaiNow-Next/a423a7e6-7f55-4328-8d48-cc1e30571fe1/scratchpad/work/<your-feature>`.
2. Only create/modify files inside **your own folder(s)** listed in your task (e.g. `DubaiNowDemo/Features/Home/`).
   Do not change `App/`, `DesignSystem/`, `Data/`, `Resources/` or other features — the orchestrator merges only your folder.
   If you need something shared changed or added (a store action, a model field, a component fix), **work around it
   inside your folder** (e.g. a `private` helper view, an `extension DemoStore` with computed properties or methods
   that only use existing stored state), and write the request in `Features/<YourFeature>/NOTES.md` so the orchestrator
   can promote it later. Keep the **type names and initialisers** of your placeholder views exactly as they are
   (they're referenced from `App/RouteDestinations.swift`).
   Prefix private helper types with your feature name (e.g. `HomeNeedsYouCard`) to avoid name clashes after the merge.
3. Build in your copy with your own derived data:
   `cd "$WORK" && xcodebuild build -project DubaiNowDemo.xcodeproj -scheme DubaiNowDemo -destination 'platform=iOS Simulator,name=DN-<your-feature>' -derivedDataPath "$WORK/dd" 2>&1 | grep -E "error:|warning:|BUILD"`
   Zero errors and zero warnings in your files.
4. Verify on **your own simulator** (create once, don't touch other simulators):
   `xcrun simctl create "DN-<your-feature>" "iPhone 18 Pro"` (the runtime is iOS 27.0), boot it, install
   `$WORK/dd/Build/Products/Debug-iphonesimulator/DubaiNowDemo.app`, launch with launch arguments, screenshot with
   `xcrun simctl io <udid> screenshot --type=jpeg file.jpg` and LOOK at it (Read the image; `sips -Z 900` first to keep it small).
   Compare against the Paper screen. Iterate until it looks right. Shut your simulator down when you finish.
   Launch arguments: `-persona sara|khalid|faisal|wei|guest`, `-tab home|services|activity|search`,
   `-route topic:driving-and-transport` (comma-separate several: `-route topic:health,service:pay-traffic-fine`),
   `-sheet profile|happiness|notifications|widgetGallery|editFavorites|extendParking|topUpSalik|demoMenu|payAll|receipt:<id>|comingSoon`,
   `-assistant`, `-welcome`, `-hour 9|13|18|22` (time of day for the Home wash).
   Routes: topic:<id>, subtopic:<topic>/<sub>, service:<id>, bundle:<id>, journey:<id>, allServices, entities, entity:<name>,
   bundlesList, journeysList, vehicle:<id>, vehicles, documents, document:<id>, family, request:<id>, receipt:<id>, drafts, history, notifications.
   To drive taps you can't reach by launch arguments, it's fine to temporarily add a launch argument handled inside your
   own view (e.g. `-homeEditing`) — read it with `ProcessInfo.processInfo.arguments.contains(...)`.
5. Don't use git, don't install anything, no network calls in the app.
6. When done, write `Features/<YourFeature>/NOTES.md` (what you built, launch args to see each screen, shared requests),
   and report back briefly (≤ 300 words): files, what works, what's left, shared requests, 2–3 screenshot paths.

## Foundation you can use (read the source when in doubt)
**DesignSystem/**
- Tokens: `DNColor.<camelCaseToken>` (e.g. `surfacePrimary`, `surfaceSecondary` (grey canvas), `textPrimary`,
  `textSecondary`, `textLink`, `actionAccent`, `actionAccentSubtle`, `statusWarningForeground`, `hueSkyStart`…, `washMorning`…,
  `plateInk`, `listRowSeparator`, `elevationNear/Far`, `widgetSurfaceInverse`, `signParking`, `signMetroRed`, `signLed`…),
  `DNSpace.*` (`insetPage` 20, `widgetGutter` 12, `gapSection`, `buttonHeightLarge`…), `DNRadius.*` (`card` 20, `brief` 24,
  `widget` 26, `tile` 20, `listGroup` 24, `pill`, `sheet` 40), `DNSize.*` (`favoriteTile` 81.5, `widgetUnit` 175,
  `widgetWide` 362, `briefCard` 272).
- Type: `.dnFont(.largeTitle/.title1/.title2/.section/.title3/.headline/.button/.body/.callout/.rowTitle/.subheadline/.footnote/.overline/.caption1/.caption2/.numericUnit/.numericMedium/.numericLarge/.numericHero)`;
  `DNTextStyle.x.font` for `Text` interpolation.
- Motion/haptics: `DNMotion.snappy/.smooth/.bouncy/.fast/.base/.wash`, `DNMotion.resolve(_:)`, `.buttonStyle(.dnPressable)`,
  `DNHaptics.selection/light/medium/soft/success/warning/error()`.
- `Hue` (.sky .sand .rose .lilac .mint .peach) with `.start .end .ink .gradient` (OKLab 160° like Paper); `TimeOfDay`, `AmbientWash`.
- Surfaces: `.dnCard(radius:)`, `.dnElevation()`, `.dnCanvasBackground()`, `DNListGroup(header:footer:) { rows }`,
  `DNListRow(title:subtitle:value:showsChevron:) { leading }`, `DNRowIcon(symbol:)`.
- Buttons: `.buttonStyle(.dn(.primary|.secondary|.tertiary|.destructive|.inverse, size: .large|.medium|.small, fullWidth:))`,
  `GlassIconButton(symbol:label:action:)`, `DNTextButton(title:action:)`.
- Marks: `HueIcon(symbol:hue:size:)`, `HueTile(symbol:hue:)`, `SectionHeader(title:subtitle:) { accessory }`,
  `StatusChip(text:tone:onWhite:)`, `NumberPlate(code:number:small:)`, `MoneyText(amount:size:)`,
  `SegmentedProgress(total:done:)`, `StackedCirclesMark`, `RouteMark`, `ServiceTypeBadge(type:)`, `HappinessFace`,
  `FaceGlyph`, `HappinessFacesPicker(selection:)`, `DNLogo(height:)`, `Avatar(initials:hue:size:)`.
- Feedback: `ToastCenter` (`@Environment(ToastCenter.self)`, `.show("Added to Favorites", undo: {…})`),
  `ComingSoon(title:detail:owner:symbol:hue:kind:)` + `ComingSoonSheet` (present with `router.present(.comingSoon(...))`).
- Catalogue rows: `CatalogueRow(service:trailing: .chevron|.favorite|.none, context:highlight:)` (handles standalone,
  bundle "All at once" and journey "Start to finish"), `BundleDiscs`, `JourneyCard(service:progress:)`, `BundleCard(service:count:)`.

**Data/**
- `Catalogue.shared`: `.topics`, `.services` (219), `.service(id)`, `.topic(id)`, `.standalone(inTopic:subtopic:)`,
  `.all(inTopic:)`, `.bundles`, `.journeys`, `.aToZ`, `.owners`, `.services(ownedBy:)`, `.search(_:)`, `.things`.
  `CatalogueService`: id, type (.standalone/.bundle/.journey), kind ("service"/"thing"/"info"), name, shortName, description,
  topic, subtopic, owner, ownerLabel, aliases, status/statusLabel/isLive, audienceLabel, parts/resolvedParts, stages,
  scaleLine, countLine(n:), covers, doesTheWork, entities, options, `.hue`, `.symbol`.
- `ServiceVisuals` (topic hues/symbols, sub-topic symbols, overrides). `Money.aed/figure`, `DemoDate.*`, `PrayerTimes`.
- `Models.swift`: Persona, PersonaState, Payable, NeedsYouItem, FavoriteItem, HomeWidget/WidgetKind/WidgetSize, Vehicle,
  Plate, DocumentItem, FamilyMember, HomeInfo, PaymentMethod, RequestItem/RequestStep, DraftItem, Receipt/ReceiptRow,
  NotificationItem, LinkedAccount, ParkingSession, EnergyReading, MetroStation, JourneyProgress, FamilyVisasStatus,
  PaymentPurpose, PaymentRequest, PaymentLine.
- `DemoStore` (`@Environment(DemoStore.self)`): `session`, `persona`, `isGuest`, `state` (PersonaState), `now`, `timeOfDay`,
  `isNeedsYouFolded`, `signIn(_:)`, `continueAsGuest()`, `signOut()`, `resetDemo()`, `payables`, `payAllTotal`, `showsPayAll`,
  `dueThisWeekCount`, `nextDue`, `payAllRequest()`, `paymentRequest(for:)`, `later(_:)`, `completePayment(_:method:) -> Receipt`,
  `receipt(_:)`, `rate(receiptID:_:)`, `request(_:)`, `advanceRequest(_:)`, `isFavorite(serviceID:)`, `toggleFavorite(_:) -> Bool`,
  `removeFavorite`, `moveFavorites`, `addWidget/removeWidget/resizeWidget/moveWidgets/hasWidget`, `dismissSuggestion()`,
  `unreadCount`, `markAllRead()`, `markRead(_:)`, `deleteDraft(_:)`, `favoritesLimit` (8).
- `Personas.all`, `Personas.persona(id:)`, `Personas.guestState()`.

**App/**
- `Router` (`@Environment(Router.self)`): `tab`, per-tab paths, `searchQuery` (bound to the native search field on the TabView),
  `push(_ Route)`, `select(_ tab, push:)`, `popToRoot()`, `present(_ SheetRoute)`, `dismissSheet()`, `openAssistant(seed:)`,
  `closeAssistant()`, `open(service:)`, `open(serviceID:)`.
- `Route` and `SheetRoute` enums (see `Router.swift`); destinations in `RouteDestinations.swift`.
- Headers: tab roots use `.dnMainToolbar(showsBell:)` (smiley · logo · bell · avatar); pushed screens use `.dnPushedToolbar()`
  (logo top centre — the logo opens the assistant) and add their own trailing toolbar items. Sheets have no logo.
- `DemoMenuSheet` (switch persona, reset, time of day).

## Scenario facts — Sara Haddad (keep consistent)
Al Barsha 1 · residence visa (renewing) · husband Ahmed, children Layla (9) and Omar (6), all sponsored by Sara.
Needs you = 4 (2 due this week): DEWA bill AED 412.60 due tomorrow · "Upload a clearer passport copy" (visa renewal,
step 3 of 5, due in 2 days) · speeding fine AED 600 (Dubai Police, blocks the car renewal) · Patrol registration due in 12 days.
Pay all = AED 1,012.60 (DEWA + fine). Renewal via the assistant: fee AED 420 (400 + 20 knowledge & innovation fees) + fine 600 = AED 1,020.
Nissan Patrol 2022 · plate Dubai M 48213 · Toyota Corolla 2019 · K 30577. Salik AED 18 (low, about 4 trips; topped up
AED 100 13 days ago → AED 118 then). Parking: Zone 365C, 42 min left. Notifications: 2 unread.
In progress: visa renewal (your turn), driving licence (approved, card arrives in 2 days), Salik dispute (in review).
Drafts: Ejari (2 of 4), good conduct certificate (1 of 3). Journey started: Move home (to Jumeirah Village Circle, stage 2 of 5).
Suggested widget: Family visas (2 of 3 visas expire in 73 days).
Read `Data/Personas/Sara.swift` for every value; read the other persona files when they're filled in.

## Quality bar
- Match the Paper screens closely in hierarchy, spacing and colour — but where a native component exists, use it and
  accept its native metrics.
- Every tappable thing does something sensible. Inside the core: real navigation. Outside: `ComingSoon` sheet with the
  right `kind` (.servicesPhase for service flows, .notLiveYet for "to publish/proposed/to confirm", .maintenance, .notInDemo).
- Accessibility: VoiceOver labels on icon-only buttons, combine rows into one element, 44 pt targets, Dynamic Type works
  up to AX sizes (layouts reflow — e.g. side-by-side → stacked), Reduce Motion respected, colour never carries meaning alone.
- Performance: use `LazyVStack`/`List` for long lists (the A–Z has 219 rows).
- No force unwraps on catalogue lookups (some referenced parts don't exist yet).
