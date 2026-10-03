# Activity feature — notes for the orchestrator

## What's built
| File | What |
|---|---|
| `ActivityView.swift` | Activity tab (Paper E8D-0). Large title, `.dnMainToolbar()`, chips (All · Needs you n · In progress n · Drafts n · History) in chip tokens. Needs you = `store.state.needsYou` as big cards (same items/numbers as Home) + "Pay both · AED 1,012.60" header link. In progress (with government) → tracker. Drafts (kept 30 days). History by month (latest 5 in All, "See all n"). Guest: sign-in sheet (WZQ-0) on every visit + an inline signed-out state. |
| `RequestTrackerView.swift` | Tracker (EE3-0): hue wash, status chip, title, copy-to-clipboard reference (toast), notice card, vertical steps (done ✓ green, current numbered brown with halo, upcoming outlined, outcome row with star), current-step actions: **Scan passport** (fake camera) / **Upload file** (dialog) → `advanceRequest` + "Sent to GDRFA" toast + success haptic; **Pay the fees** step → `PaymentSheet` `.requestFees` (AED 1,146.50 for the visa). "What you've sent" sheet, "Get help" → ComingSoon, "Cancel this application" → confirmationDialog → ComingSoon `.notInDemo`. ⋯ menu: copy reference, share status, ask assistant (visa), cancel. |
| `PaymentSheet.swift` | The fake payment (`router.present(.payment(...))`): medium/large sheet, lines with hue icons, total in `MoneyText`, "Pay with" Menu over `store.state.paymentMethods`, "Demo · no money moves", "Pay AED x" pill with `faceid` in an iOS 26 `safeAreaBar`. Face ID via LocalAuthentication (`.deviceOwnerAuthentication`, "Confirm your payment") **only when biometrics are enrolled**; otherwise 0.9 s "Confirming…". Then `completePayment` → morphs (blurReplace) into the receipt: tick draws in 0.4 s, rows stagger, success haptic, faces, PDF, Done. Guards: no lines / zero total / already-paid payables → "Nothing to pay". |
| `ReceiptView.swift`, `ReceiptSheet.swift`, `ActivityReceiptContent.swift` | Receipt (EII-0) pushed + sheet: share (PDF), paid tick, hero amount, When / Paid with / extra rows / Reference (monospaced, long-press to copy), "Done with your assistant" chip, "Download receipt (PDF)", Happiness faces → `store.rate`. |
| `DraftsView.swift`, `HistoryView.swift` | Full lists (pushed). Drafts: info notice, expanded cards (progress, "Next: …", Continue / Start over → ComingSoon `.servicesPhase`), swipe Delete (confirm → `deleteDraft` → toast with Undo) and Pin, same in context menu / VoiceOver actions. History: months, Export (CSV via ShareLink). |
| `ActivityComponents.swift`, `ActivitySupport.swift`, `ActivityPassportCapture.swift`, `ActivityExport.swift` | Shared rows/cards/chips, helpers, fake capture screen, CSV + PDF (ImageRenderer; PDF says "DEMO · NOT A REAL RECEIPT"). |

## Launch arguments
- `-persona sara -tab activity` · `-activityFilter needsYou|inProgress|drafts|history` · `-activityScroll needsYou|bottom`
- `-route request:sara-visa` · `-route receipt:sara-r-dewa` · `-route history` · `-route drafts`
- `-sheet payAll` · `-sheet receipt:sara-r-salik` · `-persona guest -tab activity`
- Rehearsal driver (my folder only): `-activityDemo scan|shoot|send|uploaded|fees|pay`
  (open the scan, take the photo, send it, start after the upload, pay the visa fees, auto-pay the open payment sheet).

## Shared requests (worked around inside my folder)
1. **`advanceRequest`** sets `.inReview` even when the next step is "Pay the fees" (needs the resident). Suggest `.yourTurn` + a Needs you item for the fee step, and rewrite the finished step's detail ("Your turn · by …" → "Sent"). I derive this in Activity (`activityStatus`, `displayDetail`).
2. **`RequestStep.kind`** (upload / pay / wait), **`RequestItem.fees: [PaymentLine]`** and **`outcome`** in the model instead of my title-matching and per-service tables (`ActivitySupport.swift`).
3. **`completePayment`** should fill receipt rows from the request lines for multi-line and `.requestFees` payments (I call `activityAttachRows`).
4. **Drafts:** `DraftItem.isPinned`, `nextStep`, `DemoStore.restoreDraft(_:at:)`, `pinDraft(_:)`. Pins are a session singleton now (`ActivityDraftPins`, not cleared by Reset demo).
5. Promote `DemoStore.activityInProgress` (requests not done and not already a Needs you card) so Home/Search use the same count.
6. The app-wide `.tint(DNColor.actionAccent)` turns toolbar glyphs (close ✕, smiley) blue; Paper shows them in the label colour.
7. Demo tip: the Simulator has a passcode but no Face ID, so payments use the "Confirming…" path. To show Face ID: Features › Face ID › Enrolled, then Matching Face when prompted.

## Not verified by gesture
Swipe actions and the delete dialog (the simulator-control tool wasn't authorised); they're native `.swipeActions` / `.confirmationDialog`, mirrored in the context menu.

## Fixes for 6.0 (2) (3 Oct)
- Payment sheet payee line: a Salik top-up reads "You're paying Salik" and a parking extension "You're paying Parkin"
  (their line detail is the new balance / end time, which used to be read as the payee). `PaymentRequest.activityPayees`.
