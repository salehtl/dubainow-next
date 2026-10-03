# Widgets — notes for the orchestrator

Paper: page C1 (`p-W-0`) · AVV anatomy, AZ0 launch set, BAL states, BFM quick action, BQU touch and hold,
BXR motion, IZX bundles and journeys, JA9 their states, ABL / K8W gallery.

## What's built
- `WidgetView(widget:)` — every `WidgetKind` at every size it's designed in; fills the frame it's given, clips
  to `DNRadius.widget`, adds the Canvas elevation itself (Home should not add a background or shadow).
  - Sizes: parking S/M · salik S/M · prayer S/L · weather S · metro M · energy M · nol S · bills S/M ·
    family visas S/M · journey M/L · visaTracker S. An undesigned size falls back to the nearest layout
    (`WidgetKind.widgetLayoutSize(for:)`); `widgetSizes` lists the designed ones (used by Resize).
  - Live: parking countdown + draining meter (live → ending soon ≤10 min, amber → expired, red edge,
    counts up), metro departures tick down from launch with a one-shot Live pulse, prayer countdown and
    sun position on the arc, numbers roll (`.numericText`). All from `store.state`; calm "Not parked /
    Not connected / Add card / Sign in" states when a persona has no data (other personas are empty today).
  - Tap: parking → Extend sheet · salik → Top up · prayer/energy/nol → their service · bills → bundle pay-all ·
    family visas → bundle renew-family-visas · journey → journey page · visa tracker → request tracker ·
    metro/weather → ComingSoon `.notInDemo`. Quick action is its own button (and its own VoiceOver element)
    drawn on top of the card via an anchor preference. Press scales 0.97 and dims 3 %.
  - Touch and hold: quick action · Open … · Size (palette picker, `store.resizeWidget`) · Edit Home ·
    Remove widget (`store.removeWidget`, light haptic, toast "Parking removed · Undo" restores position).
  - VoiceOver: one sentence per widget, quick action as a separate button, plus custom actions
    (quick action, Make small/medium/large, Remove widget).
- `WidgetGallerySheet` — "Add widgets": native search ("Search N widgets"), shelf chips (For you, All at once,
  Your journeys, Getting around, Your home, Requests, Faith, City life) that jump to shelves and follow the
  scroll, live non-interactive previews with a + badge. + → `addWidget`, success haptic, sheet lowers,
  toast "Family visas added to Home · Undo". Kinds already on Home show a green check and "On Home".
  Certificates isn't offered (footnote under All at once).
- `ExtendParkingSheet` — session card, add-time chips (30 min, 1, 2, 3 hours) at `ratePerHour`, new end time,
  cost, "Extend for AED 4.00" → `.payment(... .extendParking(minutes:))`. Not parked → "You're not parked
  right now" + Start parking (ComingSoon .servicesPhase).
- `TopUpSalikSheet` — balance card, AED 50 / 100 / 200 / Other (validated, AED 50–2,000), new balance and
  trips, "Pay AED 100" → `.payment(... .salikTopUp(amount))`. Not linked → Link Salik (ComingSoon).

## Launch arguments
- `-persona sara -sheet widgetGallery` · add `-widgetsGalleryShelf journeys` (scroll to a shelf),
  `-widgetsGalleryQuery car` (search typed in), `-widgetsGalleryAutoAdd 1` (adds the first suggestion → toast).
- `-persona sara -sheet extendParking` · `-persona khalid -sheet extendParking` (not parked)
- `-persona sara -sheet topUpSalik` · `-persona guest -sheet topUpSalik` (not linked)
- TEMPORARY debug grid (delete `WidgetDebugGrid.swift` and the `-widgetsDebug` branch in the gallery when Home lands):
  `-persona sara -hour 9 -sheet widgetGallery -widgetsDebug` with optional
  `-widgetsDebugOnly parking,salik,nol,metro,prayer,weather,energy,bills,family,journey,request`,
  `-widgetsDebugParking ending|expired|none`, `-widgetsDebugPaid` (Bills and fines "all paid").

## For Home
- Frames: small = unit × unit, medium = full × unit, large = full × full (unit = (width − 40 − 12) / 2).
- Edit mode: set `.environment(\.widgetIsPreview, true)` on widgets so they stop taking taps/menus while jiggling.
- "Edit Home" in a widget menu calls `.environment(\.widgetEditHomeAction, WidgetEditHomeAction { isEditing = true })`
  if Home sets it, otherwise posts `Notification.Name.widgetEditHomeRequested`. Please wire one of them.
- Widgets cap Dynamic Type at xxxLarge (glanceable, fixed frames); VoiceOver carries the full sentence.

## Shared requests
1. `DemoStore.addWidget` could return the new `HomeWidget` (I wrap it as `widgetAdd` to Undo exactly that one),
   and a `restoreWidget(_:at:)` for Undo after Remove (I added `widgetRestore` in an extension).
2. `WidgetKind` metadata (`widgetSizes`, owner, symbol) could move next to the model in `Models.swift`.
3. nol quick action pays a fixed AED 50 straight to the payment sheet; a `SheetRoute.topUpNol` amount sheet
   would match Salik.
4. Data gaps: nol card type/number (Paper "Silver card ··· 4417"), parking area "Al Barsha 1" (data says
   "Al Barsha"), per-stage completion dates for journeys ("Done in January" — shows "Done").
5. Paper's Extend sheet has a "Pay with Apple Pay" row; omitted here because the payment sheet chooses the method.
6. Loading / Stale / Error states (BAL) aren't drawn: the demo has no network.
