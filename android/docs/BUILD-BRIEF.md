# DubaiNow demo — Android build brief (read fully before writing code)

**What we're building:** the Android version of the DubaiNow core demo. The iOS app (SwiftUI) is finished and is
the **reference for behaviour, copy, data and flows**: `/Users/saleh/Developer/Claude Workspace/DubaiNow Next/ios/DubaiNowDemo/`
(features in `Features/<X>/`, notes with launch args and decisions in `ios/docs/notes/<X>.md`, demo script in
`ios/docs/DEMO-SCRIPT.md`). Port your feature to **Kotlin + Jetpack Compose (Material 3)** so it behaves and reads the
same, but **looks and feels native on Android**, following the Android side of the Paper design system. It's a demo:
personas instead of accounts, fake payments, a scripted assistant. It will be shared through Firebase App Distribution
with the product owner's boss, on an Android phone.

## Android decisions (from the design system — do not revisit)
- **Same content, Android chrome.** Paper inventory tiers: *Shared* components (widgets, brief cards, favorite tiles,
  status cards, topic tiles, request tracker, receipts, number plate, Happiness faces, service rows) look the same on
  both platforms. *Adapted* ones use Material 3 behaviour: **tonal surfaces instead of Liquid Glass**, ripple and state
  layers, **M3 modal bottom sheets** (radius 28, drag handle, back dismisses), the **floating tonal tab bar** above the
  gesture bar, **search opens a full-screen M3 search view**, snackbar placement for toasts. *Native* ones are the real
  M3 components: `Switch`, `SegmentedButton`, `CircularProgressIndicator`/`LinearProgressIndicator`, `AlertDialog`,
  `DropdownMenu`, `DatePicker`, `FilterChip`/`AssistChip`.
- **Type: Inter** everywhere (via `DNTextStyle.X.style`), numbers Inter with tabular figures (`Numeric*` styles).
  Never SF fonts.
- **Icons: Material Symbols Rounded** — `Icons.Rounded.*` / `Icons.AutoMirrored.Rounded.*` (material-icons-extended).
  The shared data stores iOS SF Symbol names; render those with `DNIcons.forSymbol(name)` (add any missing mapping in
  your own folder with a private helper and note it). Never draw SF Symbols.
- **Targets 48 dp** minimum. Edge-to-edge: pad for status/navigation bars (`statusBarsPadding()`,
  `navigationBarsPadding()`), the app draws behind them.
- **Light mode only.** **English only** (keep strings in code, ready for resources later). Western digits always.
- **Wallet = Google Pay** on Android (the model's `PaymentMethod.GooglePay`); never imitate the real Google Pay sheet —
  the fake confirmation is our own sheet with a fingerprint/face prompt (androidx.biometric `BiometricPrompt` if the
  device has biometrics enrolled, otherwise a 0.9 s "Confirming…" state — never block the demo).
- **Back**: system back / predictive back must work everywhere (`BackHandler` for in-screen states; the shell handles
  tab stacks and sheets).
- **Motion**: `DNMotion.snappy()/smooth()/bouncy()` (our springs), `DNMotion.fast()/base()`; respect the system
  "Remove animations" setting where you add big motion. Haptics: `DNHaptics.selection/light/medium/success/warning(view)`
  with `val view = LocalView.current`, exactly as the Paper motion spec says.
- **Copy**: identical to iOS (GOV.UK style, sentence case, UK spelling except "Favorites"). Money `Money.aed(...)`,
  dates `DemoDate.*` (relative to today).

## Where things are
- Android project: `/Users/saleh/Developer/Claude Workspace/DubaiNow Next/android/` (Gradle, Kotlin 2.2, AGP 8.11,
  Compose BOM 2025.07, minSdk 26, compileSdk 36). Package `com.salehtl.dubainownext`.
  `JAVA_HOME=/opt/homebrew/opt/openjdk@21`, `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools`.
- Paper (design; **read-only**, fileId `01M3VK547PT2KNWSB1WPYWFJHJ`): C1 screens on page `p-W-0` (same node ids as the
  iOS brief, see `ios/docs/BUILD-BRIEF.md` and your iOS feature notes), the **Android kit** on page `p-Y-0`
  ("M1–M7 Android kit": artboards `M9X-0` M1 Buttons · M2 Controls, `MC2-0` M3 Fields · M4 Navigation, `MDV-0`
  M5 Overlays · M6 Pickers, `OFF-0` M7 Service flow · Android), Android sheets on component pages (e.g. page 20
  `NHI-0` "Waves 2–3 · Masters · Android Light / EN", page 13 `LOA-0` "List rows · Android", `M6O-0` "Navigation
  chrome · Reduced, Android, Arabic", page 10 `OX4-0` Button v0.5 incl. Android). Load Paper tools with ToolSearch
  `select:mcp__Paper__get_screenshot,mcp__Paper__get_jsx,mcp__Paper__get_tree_summary,mcp__Paper__get_node_info,mcp__Paper__get_children`.
  Never call Paper write tools. Budget ~25 Paper calls (the iOS code already encodes most of the design).
- Apple/Android skills are not needed; standard Compose/M3 knowledge applies.

## How you work (8 builders run at the same time)
1. Make your own working copy and work ONLY there:
   `rsync -a --exclude build --exclude .gradle --exclude docs "/Users/saleh/Developer/Claude Workspace/DubaiNow Next/android/" "$WORK/"`
   with `WORK=/private/tmp/claude-501/-Users-saleh-Developer-Claude-Workspace-DubaiNow-Next/a423a7e6-7f55-4328-8d48-cc1e30571fe1/scratchpad/work-android/<feature>`.
   Never re-sync from the main project over your copy.
2. Only create/modify files in **your own package folder** `app/src/main/java/com/salehtl/dubainownext/features/<feature>/`.
   Do not change `app/`, `design/`, `data/`, resources or Gradle files — the orchestrator merges only your folder.
   Need something shared? Work around it inside your folder (private composables, extension functions on `DemoStore`
   that only use its public API, local `remember` state) and list the request in `features/<feature>/NOTES.md`.
   Keep the **public function signatures of your placeholder screens** exactly (they're called from
   `app/RouteDestinations.kt` / `app/RootScreen.kt`). Make every other top-level declaration `private` or `internal`,
   or prefix it with your feature name (e.g. `HomeNeedsYouCard`) so names never clash after the merge.
3. Build in your copy (with your own package suffix so builders can share emulators):
   `cd "$WORK" && JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew :app:assembleDebug -PappSuffix=.<feature> --console=plain -q 2>&1 | grep -E "^e: |^w: .*features/<feature>|error:"`
   Zero errors; no warnings in your files.
4. Look at what you built — two shared emulators are running (`emulator-5554`, `emulator-5556`; use
   **5554 if your feature is welcome/home/widgets/services, 5556 for search/activity/profile/assistant**). Always use the
   locked helper (it installs, launches with extras, waits, screenshots, and reports crashes):
   `/Users/saleh/Developer/Claude Workspace/DubaiNow Next/android/tools/agent-shot.sh <serial> "$WORK/app/build/outputs/apk/debug/app-debug.apk" com.salehtl.dubainownext.<feature> <out.jpg> <wait-seconds> persona sara tab home ...`
   then Read the JPEG. Launch extras: `persona sara|khalid|faisal|wei|guest`, `tab home|services|activity|search`,
   `route topic:driving-and-transport,service:pay-traffic-fine` (same route names as iOS), `sheet profile|happiness|notifications|widgetGallery|editFavorites|extendParking|topUpSalik|demoMenu|payAll|receipt:<id>|comingSoon`,
   `assistant true`, `hour 9|13|18|22`, `welcome true`. For states you can't reach by extras, read your own debug extras
   via `LaunchOptions.extras["yourFlag"]` (e.g. `homeEditing true`). You can't tap on the emulator — design debug extras
   for the states you need to see. Compare against the iOS screenshots/behaviour and the Paper Android kit; iterate.
   Never touch other builders' packages; never wipe or restart the emulators.
5. No git, no network installs, no new Gradle dependencies (everything you need is in the BOM: material3,
   material-icons-extended, animation, foundation, biometric, kotlinx-serialization).
6. When done, write `features/<feature>/NOTES.md` (what you built, debug extras, shared requests) and report back briefly
   (≤ 250 words): files, what works, what's left, shared requests, 2–3 screenshot paths.

## Foundation you can use (read the source when in doubt)
**design/** — `DNColor.*`, `DNSpace.*`, `DNRadius.*` (Android values: sheet 28, dialog 28, snackbar 4, card 20, brief 24,
widget 26, tile 20, listGroup 24), `DNSize.*` (favoriteTile 81.5, widgetUnit 175, briefCard 272), `DNTextStyle.X.style`
(LargeTitle, Title1, Title2, Section, Title3, Headline, Button, Body, Callout, RowTitle, Subheadline, Footnote, Overline,
Caption1, Caption2, NumericUnit/Medium/Large/Hero), `Inter`, `DNMotion`, `DNHaptics`, `Hue` (`.start .end .ink .gradient`),
`TimeOfDay`, `AmbientWash`, `cssGradient`, `DNIcons.forSymbol(name)`.
**design/components/** — `Modifier.dnCard()`, `Modifier.dnElevation()`, `Modifier.dnCanvasBackground()`, `DNListGroup`,
`DNListGroupItems`, `DNRowDivider`, `DNListRow`, `DNRowIcon`, `DNButton(text, onClick, kind, size, fullWidth, enabled, icon)`,
`TonalIconButton` (Android stand-in for iOS glass circles), `DNTextButton`, `HueIcon`, `HueTile`, `SectionHeader`,
`StatusChip(text, tone=ChipTone.*)`, `NumberPlate`, `MoneyText`, `SegmentedProgress`, `StackedCirclesMark`, `RouteMark`,
`ServiceTypeBadge`, `HappinessFace`, `FaceGlyph`, `HappinessFacesPicker`, `DNLogo`, `Avatar`, `ToastCenter`
(`LocalToasts.current.show("…", undo = { … })`), `ComingSoon` + `ComingSoonContent`, `CatalogueRow`, `FavoriteStar`,
`BundleDiscs`, `JourneyCard`, `BundleCard`.
**data/** — `Catalogue.shared` (same API as iOS: topics, services, service(id), standalone(...), all(...), bundles,
journeys, aToZ, owners, services(ownedBy), search(q)); `CatalogueService.hue/.symbol/.icon/.ownerLabel/.statusLabel/...`;
`ServiceVisuals`; `Money`, `DemoDate`, `Int.aed`/`Double.aed`; `Models.kt` (same model names as iOS, Kotlin data
classes/sealed interfaces); `DemoStore` (`LocalStore.current`): same actions as iOS (`signIn`, `continueAsGuest`,
`signOut`, `resetDemo`, `payables`, `payAllTotal`, `showsPayAll`, `dueThisWeekCount`, `nextDue`, `payAllRequest()`,
`paymentRequest(p)`, `later`, `completePayment(req, method)`, `receipt(id)`, `rate`, `request(id)`, `advanceRequest`,
`addRequest`, `isFavorite`, `toggleFavorite`, `removeFavorite`, `moveFavorite`, `setFavorites`, `addWidget(kind,size,at)`,
`removeWidget`, `restoreWidget`, `resizeWidget`, `moveWidget`, `hasWidget`, `dismissSuggestion`, `unreadCount`,
`markAllRead`, `markRead(id, read)`, `deleteDraft`, `FAVORITES_LIMIT`). State is `store.state` (immutable; the store
replaces it — always read through `store.state` inside composables so Compose recomposes). `Personas.all`, `PrayerTimes`.
**app/** — `LocalRouter.current`: `tab`, `stack(tab)`, `push(route)`, `pop()`, `popToRoot()`, `select(tab, push)`,
`present(SheetRoute.X)`, `dismissSheet()`, `searchQuery`, `openAssistant(seed)`, `closeAssistant()`, `open(service)`,
`open(serviceID)`. `Route` / `SheetRoute` (same cases as iOS). `MainHeader(showsBell)` for tab roots,
`PushedHeader(onBack, actions)` for pushed screens (sheets have neither — `LocalInSheet.current` is true inside sheets;
add your own sheet title row with a close `TonalIconButton`). Sheets are hosted by `SheetHost` (ModalBottomSheet) —
your sheet composable renders only its content. Screens scroll under the floating bottom bar: give scrolling content
`contentPadding`/bottom spacer of ~120 dp. `LaunchOptions.extras` for debug extras. `DemoMenuSheet`.

## Scenario facts
Same as iOS — read `ios/docs/BUILD-BRIEF.md` (Scenario facts) and `android/app/src/main/java/com/salehtl/dubainownext/data/personas/Sara.kt`.
Khalid, Faisal and Wei are being ported in parallel; code against `store.state` generically.

## Quality bar
- Match the iOS app's content and flows screen for screen; match Paper's Android adaptations for chrome and controls.
- Every tap does something sensible; outside the core use `ComingSoon` (`router.present(SheetRoute.ComingSoonSheet(...))`).
- Accessibility: content descriptions on icon-only buttons, `semantics(mergeDescendants = true)` on rows/cards, 48 dp
  targets, text scales with the font size setting (layouts reflow at 200%), headings marked with `semantics { heading() }`.
- Performance: `LazyColumn`/`LazyRow` for long lists (A–Z has 219 rows); `key`s on list items.
