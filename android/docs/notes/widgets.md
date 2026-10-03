# Widgets (Android) — notes for the orchestrator

Port of `ios/DubaiNowDemo/Features/Widgets/*` (see `ios/docs/notes/Widgets.md`). Paper: C1 `p-W-0` · AVV anatomy,
AZ0 launch set, BAL states, BFM extend parking, BQU touch and hold, IZX/JA9 bundles and journeys, ABL/K8W gallery;
Android kit `MDV-0` (M5 menu + sheet), `MC2-0` (outlined field).

## Files
- `WidgetView.kt` — public `WidgetView(widget, modifier)`, the M3 touch-and-hold menu, actions, spec dispatch.
- `WidgetParts.kt` — figure (rolling digits), caption, title, chip, meter, stages bar, glyphs (parking sign,
  Salik gantry, nol card, bundle mark), quick actions, calm state, formatting.
- `WidgetCatalog.kt` — `WidgetKind.widgetSizes / widgetLayoutSize / widgetOwner / widgetSearchTerms / widgetKey`,
  bills / family / journey models, store helpers (read-only).
- `WidgetGettingAround.kt` (parking, Salik, nol, metro) · `WidgetCityAndFaith.kt` (prayer, weather) ·
  `WidgetHomeAndBundles.kt` (energy, bills and fines, family visas) · `WidgetJourneys.kt` (journey, request
  tracker, certificates).
- `WidgetGallerySheet.kt` — public `WidgetGallerySheet()`; `WidgetSheetParts.kt` — sheet header, overline, choice chip.
- `ExtendParkingSheet.kt`, `TopUpSalikSheet.kt` — public sheets.
- `WidgetDebugGrid.kt` — debug only (see below).

## What works
- Every kind at every designed size (parking S/M · salik S/M · prayer S/L · weather S · metro M · energy M · nol S ·
  bills S/M · family visas S/M · journey M/L · visa tracker S; certificates calm state). Undesigned sizes fall back
  to the nearest layout. Fills the parent, clips to `DNRadius.widget`, adds the Canvas elevation itself
  (Home should not add a background or shadow).
- Live from `store.state`: LaunchedEffect tickers (parking + metro 1 s, prayer 15 s, others 60 s; no timers when
  `LocalWidgetPreview` is true). Parking live → ending soon (≤10 min, amber, filled +) → expired (red edge, counts
  up, "Extend now"); metro counts down from launch with a one-shot Live pulse; prayer countdown + sun on the arc
  (honours `hour` override). Changed digits roll per character (AnimatedContent), odometer digits roll.
- Calm states when a persona has no data (Not parked, Salik Not connected, Add card, Nothing yet, Sign in…).
- Tap: parking → Extend sheet · salik → Top up · prayer/energy/nol → their service · bills → bundle pay-all ·
  family → bundle renew-family-visas · journey → journey · tracker → request · metro/weather → ComingSoon.
  Quick actions are their own buttons (+ / capsule): ExtendParking, TopUpSalik, nol → Payment(NolTopUp 50),
  Renew family visas, Connect → ComingSoon.
- Touch and hold (M3 DropdownMenu, medium haptic): quick action · Open … · Size (expands to the sizes, check on the
  current one → `store.resizeWidget`) · Edit Home (`router.homeEditRequested = true`, closes a sheet first) ·
  Remove widget (red; light haptic; snackbar "Parking removed · Undo" → `store.restoreWidget(widget, index)`).
- TalkBack: one sentence per widget (the card's drawn text is cleared), click/long-click labels as verb phrases
  ("Double-tap to open Extend parking"), custom actions (quick action, Make small/medium/large, Remove widget). The
  quick action is its own element too, as on iOS: it reports where it is drawn (`WidgetQuickSlots`) and WidgetView
  lays a TalkBack-only button ("Extend parking", "Top up Salik"…) over it; it has no pointer input, so touches still
  reach the drawn button. Checked with a uiautomator dump. Press scale 0.97 + ripple. Font scale capped at 1.3 inside widgets (iOS caps Dynamic Type at
  xxxLarge: glanceable fixed frames).
- Gallery: title + close, pill search field ("Search 18 widgets"), FilterChip shelf row (jumps to a shelf and follows
  the scroll), shelves of live previews (`LocalWidgetPreview provides true`) with + badge; tap → `store.addWidget`,
  success haptic, sheet closes, snackbar "Family visas added to Home · Undo". On Home → green check, "On Home", disabled.
- Extend parking: session card (ticks), 30 min / 1 / 2 / 3 hours, new end time + cost, "Extend for AED 4.00" →
  `SheetRoute.Payment(… PaymentPurpose.ExtendParking(minutes))`; not parked → Start parking (ComingSoon).
- Top up Salik: dark balance card, AED 50/100/200/Other (M3 outlined field, AED 50–2,000 validated), new balance +
  trips, "Pay AED 100" → `SheetRoute.Payment(… PaymentPurpose.SalikTopUp(amount))`; not linked → Link Salik.

## Debug extras
- `sheet widgetGallery widgetsDebug true` → debug grid over Home: the persona's Home widgets (live; menus really
  change Home) + every kind and size. Filters: `widgetsDebugOnly parking,salik,…`, `widgetsDebugSizes medium,large`.
  States: `widgetsDebugParking ending|expired|none`, `widgetsDebugPaid true`. Menus: `widgetsDebugMenu w-parking`
  (widget id or `parking-small`), `widgetsDebugMenuSizes true`. Actions after 1.5 s: `widgetsDebugAction
  remove:w-parking | resize:w-salik:medium | add:family:medium`.
- Gallery: `widgetsGalleryQuery car`, `widgetsGalleryShelf journeys|all-at-once|…`, `widgetsGalleryAutoAdd true`.
- Sheets: `widgetsParkingMinutes 120`, `widgetsSalikOther 30`; `persona khalid sheet extendParking` (not parked),
  `persona guest sheet topUpSalik` (not linked).

## For Home
- Frames: small = unit × unit, medium = full × unit, large = full × full, unit = (width − 40 − 12) / 2.
- Edit mode: wrap widgets in `CompositionLocalProvider(LocalWidgetPreview provides true)` (no taps, menus, timers).
- Watch `router.homeEditRequested` (set by "Edit Home") and clear it.

## Shared requests
1. **Crash (shared typography):** `design/Type.kt` `trackingEm` returns `0.sp` for zero tracking and `em` otherwise.
   M3 text fields lerp bodyLarge (em) → bodySmall (sp) for the floating label and throw
   "Cannot perform operation for Em and Sp". Return `0.em` instead. Worked around here with
   `WidgetFieldSafeTypography` (local MaterialTheme with em letter spacing); every other OutlinedTextField with a
   label in the app will crash until this is fixed.
2. `DNIcons` mappings: `"link"` → `Icons.Rounded.Link`, `"sun.max.fill"` → `Icons.Rounded.WbSunny` (used in
   ComingSoon sheets; I fall back to a mapped symbol via `widgetSymbol(...)` until they exist).
3. `DNButton` disabled (`actionDisabled` #EFEFF1) is invisible on the grouped sheet surface (#EFEFF1) — only the label
   shows. Worked around in Top up Salik with a private `SalikPayButton` (M3 disabled look: on-surface 10 %); other
   sheets with a disabled DNButton still need the shared fix.
4. `DemoDate.short` / `dueLabel` use `Locale.UK`, which Android's CLDR formats as "20 Sept" (iOS en_GB: "20 Sep",
   checked with Foundation). Worked around here with `WidgetFormat.shortDate` / `iosMonth`; other features still show
   "Sept" until `DemoDate` is fixed.
5. Same data gaps as iOS: nol card type/number, parking area "Al Barsha 1", per-stage completion dates.
6. A `SheetRoute.TopUpNol` amount sheet would match Salik (nol + pays a fixed AED 50 today, like iOS).
7. `DemoStore.now` keeps the seconds when `hour` is set (iOS sets :41:00), so countdowns read a minute short
   ("in 2h 30m" vs iOS "in 2h 31m"). WidgetView zeroes them itself.

## Review pass (fixes)
- Top up Salik: the Pay button disappeared when the Other amount was invalid (see shared request 3) — now visible.
- Prayer countdown with the `hour` extra read a minute short ("in 2h 30m"); now "in 2h 31m" like iOS.
- "20 Sept" → "20 Sep" (Salik medium "On", gallery journey caption, Bills "Next … 13 Sep").
- Parking, Salik and nol figures had lost the iOS `.padding(.top, 6)` under the glyph row (Paper AZ0 spacing).
- TalkBack: the quick action wasn't reachable as its own element and its + button had no label; click labels read
  "Double-tap to Opens …". Ending-soon clock is the filled clock, like Paper BAL.
- `widgetType` letter spacing is always em (no sp/em mix, same crash class as shared request 1).

## QA pass (3 Oct)
- widgets-1: the Salik trips line read "Low · about 4 t…" at the widgets' 1.3 font cap (`homeFontScale 1.3`), small
  and medium. There's no room for a second line under the figure, so `WidgetCaption` gained `minScale`: below 1 it
  shrinks to fit before it truncates, like iOS `minimumScaleFactor` (`BasicText` autoSize; full 13 sp whenever it
  fits, so 100 % is pixel-identical; line height stays 18). The trips line uses 0.85 (the iOS caption's factor) and
  now reads in full at 1.3; the spoken summary is unchanged.
- Seen, not changed: at 1.3 the medium Salik fact label reads "Last t…" (iOS fact rows shrink to 0.8 too).
