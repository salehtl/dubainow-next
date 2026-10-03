# Personas (Android port)

Ported Khalid, Faisal and Wei from `ios/DubaiNowDemo/Data/Personas/*.swift` to Kotlin, following `Sara.kt`
(top-level `val khalid: Persona get() = Persona(...)` + `fun khalidState(): PersonaState`).

Files changed (only these three): `Khalid.kt`, `Faisal.kt`, `Wei.kt`. `Sara.kt` and `Personas.kt` are untouched.

## Mapping
- `.applePay` → `PaymentMethod.GooglePay`; `.card(brand:last4:)` → `PaymentMethod.Card(brand, last4)`.
- Decimal literals → `.aed` (`386.20.aed`, `96.aed`, Wei's Salik `0.aed`).
- Dates → `DemoDate.day/fixed` with the same arguments (`fixed(2026, 5, 11, hour = 10)` for Khalid's loan receipt).
- Notice tuples → `Pair` (all three have `notice = null`). Routes → `Route.Journey/Request/Receipt/Vehicle`.
- Swift's explicit `nil`/empty assignments are kept explicit (`nolBalance = null`, `metro = null`, `journeys = emptyList()`…).
- Wei's Mandarin assistant prompt is kept verbatim (UTF-8).

## Checks
- A script compared every string literal and number in the Swift sources with the Kotlin: all match. Hue order,
  widget sizes, step states and booleans also match.
- Build `-PappSuffix=.personas`: no errors or warnings.
- Launched `persona khalid|faisal|wei` on emulator-5556: no crashes, avatars K/F/W in sand/sky/mint.
  `persona faisal sheet payAll` gives "Pay all 3"; the demo menu lists all four taglines.

## Key numbers
- **Khalid**: Needs you 2 (1 due this week): villa drawings (journey step 4 of 7, no due date) and DEWA AED 386.20
  due in 5 days. 1 payable, so no Pay all. Unread 2 of 4. Salik AED 96. No nol, parking or metro.
  Energy 2,180 kWh (−16%). Journey build-a-house stageIndex 3, needsYou. Widgets: journey (large), prayer, weather, energy.
- **Faisal**: Needs you 3 (2 due this week): DEWA 1,486.20 (tomorrow), du 399.00 (in 3 days), fine 150.00 on van 2.
  Pay all = AED 2,035.20. Unread 2 of 5. Salik AED 245. Energy 3,412 kWh (−7%). Family visas: 2 members, none due.
- **Wei**: Needs you 1 (1 due this week): du mobile AED 125.00 in 5 days. No Pay all. Unread 2 of 3. nol AED 14.50,
  Salik 0, no vehicles or family. Metro Al Nahda (Green Line), weather spot Al Mamzar Beach. Energy 612 kWh (−11%).

## Shared requests
None.

## Independent review (3 Oct)
- Re-checked Khalid, Faisal and Wei against the Swift sources. Every literal matches, in the same order: strings
  (including the `•` masks, `·` separators and Wei's Mandarin prompt), numbers, nulls and booleans. All enum cases match
  too (hues, payable kinds, widget kinds and sizes, step states, request statuses, payment methods, ratings, favourite
  targets, route links). Apple Pay maps to Google Pay. Only difference: Khalid's `cruiser` plate is declared before
  Needs you instead of after it, which changes nothing.
- The Kotlin model constructors take their fields in the same order as the Swift structs, so the positional
  arguments land in the right fields.
- Every service id the three personas use exists in `assets/dubainow-services.json`, and every SF Symbol name they
  use has a `DNIcons` mapping, so nothing falls back to the grid icon.
- The derived numbers from `DemoStore` agree with the Swift header comments. Khalid: payables 1, no Pay all, 1 due
  this week, 2 unread. Faisal: payables 3, Pay all AED 2,035.20, 2 due this week, 2 unread. Wei: payables 1, no Pay
  all, 1 due this week, 2 unread.
- Recompiled with `-PappSuffix=.personas`: no errors, and no warnings in this folder. Each persona launched on
  emulator-5556 with no crash (home for all three, Khalid's `receipt:khalid-r-loan`, Faisal's `payAll`, Wei's
  `request:wei-bins`).
- No defects found and no data changes made. Public `val khalid/faisal/wei` are unchanged. `khalidState()`,
  `faisalState()` and `weiState()` follow `saraState()` and live in the `data.personas` package, so they can't clash
  with names in feature packages. There is no UI here, so the TalkBack, 48 dp and bottom-bar checks don't apply.
