# Activity (Android): notes for the orchestrator

## What's built
| File | What it does |
|---|---|
| `ActivityScreen.kt` | Activity tab (Paper E8D-0). `MainHeader` + large title, M3 `FilterChip`s (All · Needs you n · In progress n · Drafts n · History) that scroll the chosen chip into view. Needs you uses `store.state.needsYou` (same items and numbers as Home) with a "Pay both · AED 1,012.60" link. In progress → tracker. Drafts (kept 30 days). History by month (latest 5 in All, then "See all n"), Export. Empty state for each filter. Guests get an inline signed-out state and the WZQ-0 sign-in sheet on every visit. The filter and scroll position are kept while you're on a pushed screen. |
| `RequestTrackerScreen.kt` | Tracker (EE3-0): hue wash, status chip, reference you can copy (system `ClipboardManager` + toast), notice card, step line (done ✓, current numbered with halo, upcoming outlined, outcome star). Current step: **Scan passport** opens the fake camera, **Upload file** opens an M3 sheet (Files / Photos). Either one calls `advanceRequest`, plays the success haptic and shows the toast "Sent to GDRFA". **Pay the fees** opens `PaymentSheet` (`RequestFees`, AED 1,146.50 for the visa). Also: "What you've sent" sheet, "Get help" → ComingSoon, "Cancel this application" → `AlertDialog` → ComingSoon. The ⋮ `DropdownMenu` has Copy reference, Share status, Ask your assistant (visa only) and Cancel. |
| `ActivityPassportCapture.kt` | Full-screen fake camera in an edge-to-edge `Dialog` with light status-bar icons. It has a drawn passport page that comes into focus, frame corners that turn green, a status pill, a shutter with a flash, then Use this photo / Retake. Back closes it. |
| `PaymentSheet.kt` | Title row with a close button, then payee line, line rows, total in `MoneyText`, "Pay with" (Google Pay / Visa via `DropdownMenu`), a big "Pay AED x" pill with a fingerprint icon, and "Demo · no money moves". Pay uses `BiometricPrompt` (FragmentActivity from `LocalContext`) only when a biometric is enrolled. Otherwise it shows 0.9 s of "Confirming…". If the person cancels, they go back to review. Lockout says "That didn't match". Any other error completes after 0.9 s, so the demo never gets stuck. Then `completePayment` runs and the sheet becomes the receipt: fade and scale, the tick draws in, rows arrive one by one, success haptic. Guard: "Nothing to pay". |
| `ActivityReceiptContent.kt`, `ReceiptSheet.kt`, `ReceiptScreen.kt` | Receipt (EII-0): tick, Paid/Sent, hero amount, title, assistant chip, then When / Paid with / rows / Reference (monospace, touch and hold to copy). Below that: Download receipt (PDF) and the Happiness faces (`store.rate`). The sheet has a title row with share and close. The pushed screen uses `PushedHeader` with share. Tall sheets stay below the status bar (`ActivitySheetFrame`). |
| `ActivityExport.kt` | CSV (same columns as iOS) shared as text through `ACTION_SEND`. Receipt PDF drawn with `PdfDocument` (Inter, logo, "DEMO · NOT A REAL RECEIPT"), saved once per receipt to **Downloads via MediaStore** (Android 10+), then shared as `application/pdf`. Before Android 10 it shares a text summary. The share icon shares the text summary. |
| `DraftsScreen.kt`, `HistoryScreen.kt`, `ActivitySections.kt`, `ActivityComponents.kt`, `ActivitySupport.kt` | Pushed Drafts (EL5-0) and History (Export in the title row). Shared blocks: cards, rows, chips, headers, notices, empty states, the guest sheet, and drafts with `SwipeToDismissBox` (swipe left = delete, which asks first in an `AlertDialog` "Keep it / Delete draft" and then shows an Undo toast; swipe right = pin). Drafts also open a touch-and-hold `DropdownMenu` and offer TalkBack custom actions. The supporting helpers sit in the same files: status/step kinds, fees, formatters, pins, filter memory and demo extras. |

All other top-level declarations are `internal`/`private` and prefixed `Activity…` / `Payment…`.

## Debug extras (`-e key value`)
- `activityFilter needsYou|inProgress|drafts|history`
- `activityScroll needsYou|inProgress|drafts|history|bottom`
- `activityDemo` (each runs once per launch):
  - `scan|shoot|send` with `route request:sara-visa`: open the camera, take the photo, send it.
  - `uploaded`: start the tracker with the upload done.
  - `fees`: upload done, then open the fees payment and pay it automatically.
  - `feesdone`: the same, then close the sheet 2.5 s after the receipt (the tracker after paying).
  - `feessent`: like `feesdone`, then open "What you've sent".
  - `pay` with `sheet payAll`: pay the open payment sheet automatically. `paydone` also closes it afterwards
    (e.g. `tab activity sheet payAll activityDemo paydone activityScroll history`).
  - `upload|sent|cancel|menu` on the tracker: upload sheet, "What you've sent", cancel dialog, ⋮ menu.
  - `delete` on the Activity tab or Drafts: the delete dialog.
  - `paywith` on the payment sheet: the method menu.
  - `pdf` on a receipt: Download PDF.
  - `csv` on the Activity tab: Export.
- Examples: `persona guest tab activity`, `route receipt:sara-r-dewa`, `sheet receipt:sara-r-salik`, `route history`, `route drafts`.

## Shared requests (worked around in my folder)
1. **FileProvider**: add `androidx.core.content.FileProvider` with a cache path. The CSV could then go as a real `.csv` attachment, and the PDF could be shared from cache instead of being saved to Downloads.
2. **`DemoDate` uses `Locale.UK`**, which gives "29 Sept" on current Android/ICU, while iOS and Paper show "29 Sep". My formatters follow `DemoDate` so dates look the same everywhere. Fix it centrally, for example with explicit short month names.
3. **`advanceRequest`**: when the next step is "Pay the fees" it sets `YourTurn` but adds no Needs you item, so the fee step only shows in In progress. Consider adding a Needs you card (primary action "Pay the fees"). Activity already routes a pay step on a `RequestStep` card to the fees payment. Also consider rewriting the finished step's "Your turn · by …" detail. I show "Sent · being checked".
4. Promote to `data/`:
   - `RequestStep.kind` (upload/pay/wait), `RequestItem.fees: List<PaymentLine>` and `outcome`. For now I match on titles and use a per-service table (`ActivitySupport.kt`).
   - `DemoStore.activityInProgress`, so Home and Search use the same count.
   - `restoreDraft(draft, at)`, plus `DraftItem.isPinned` / `nextStep`. Pins are a session object now.
5. **`completePayment`** should fill receipt rows from the request lines for `RequestFees` (and for multi-line requests that aren't payables). I call `activityAttachRows`.
6. **`MainHeader`**: the guest avatar button draws an empty circle, because the content lambda draws nothing when `persona == null`.
7. **`SheetHost`**: full-height sheets put the drag handle under the status bar. I cap my sheets' height (`ActivitySheetFrame`). The same fix inside `SheetHost` would cover every sheet.
8. Icon mappings used locally (not in `DNIcons`): `eye.fill` → Visibility, `text.bubble.fill` → AutoMirrored Chat, `doc.fill` → Description.

## Not verified by gesture
I didn't inject taps on the shared emulator. The only input I sent was two Back presses to close the share sheets after the `pdf` and `csv` checks. So I checked the swipe-to-dismiss motion, long-press menus and the real BiometricPrompt sheet in code only. The emulator has no biometric enrolled, so payments took the "Confirming…" path. Every state they lead to was checked on screen through the extras above.

## Review pass (fixes after the first build)
- **Payment sheet:** after paying bills, the review faded out as "Nothing to pay" (the store had already
  removed the payables and the leaving content re-read them). The body is now decided once per phase
  (`PaymentBody`), and the leaving review keeps its "Confirming…" look. "Change" greys out while confirming.
- **Biometric lockout:** a lockout showed "That didn't match" on every retry (the prompt fails straight away
  for 30 s), so the demo could get stuck. The first lockout shows the message; the next try uses the short
  "Confirming…" path.
- **Tracker after paying the fees:** the done "Pay the fees" step still said "About AED 1,150 · we'll show the
  exact amount". It now shows the receipt's date and amount ("3 Oct · AED 1,146.50"), and "What you've sent"
  lists the payment with that amount (`DemoStore.activityStepDetail`). iOS has the same gap; worth porting.
- **Copy:** the fee step's tracker chip reads "Your turn · pay the fees" like iOS (it said "3 days left");
  the draft dialog's cancel button is "Keep it" like iOS and the tracker's cancel dialog.
- **Dates:** receipt and history times are built from `DemoDate.short`/`time`, so the central "Sept" → "Sep"
  fix (shared request 2) reaches them too.
- **TalkBack:** the draft card is one labelled element again (its inner rows were separate merge groups, which
  left the clickable card with no label); the Needs you day chip reads "Due in 2 days" like iOS; an empty
  sheet title is no longer a heading.
- The upload chooser slides away before the request moves on (and ignores a second tap); unused helpers removed.

## QA pass (3 Oct)
- **Payee line (activity-1):** "You're paying …" and the fingerprint prompt's subtitle named the line's
  detail, which for a Salik top-up is the new balance and for a parking extension the new end time. They now
  say "You're paying Salik" / "You're paying Parkin" (`PaymentRequest.activityPayees`, keyed on the purpose;
  bills still use the entity in `detail`). The line rows keep the new balance and end time. iOS has the same
  copy bug (`PaymentSheet.swift` `payeeLine`).
- **Tracker halo (activity-2):** the current step's signSun halo (30%, 5 dp around the disc) was capped to
  28 dp by its parent and hidden under the disc. It uses `requiredSize(38.dp)` now, and the tracker's
  `animateContentSize` (which clips) moved outside the page inset so the halo's left edge isn't cut off.
- **In progress rows (activity-3):** no trailing chevron (Paper LOA-0), like `DNListRow`.
- **Copy reference (activity-4):** `ActivityExport.copy` returns whether to confirm. The "Reference copied"
  toast only shows below Android 13; 13+ shows the system clipboard confirmation instead.
- **Draft menu (activity-5):** the touch-and-hold menu is anchored to a point at the finger (the down
  position is watched in the initial pass, without consuming), so it opens on the pressed card instead of
  above the card's top edge over the previous draft. With no room below the finger it still opens upward
  (its corner at the finger), so while the menu is open the pressed draft keeps the pressed row colour
  (`listRowSurfacePressed`, multiplied) to show which one it acts on, as iOS lifts the card. Opened from
  TalkBack, it uses the card's last touch point (its top-left if never touched).
- Verified on emulator-5554 (API 36). The below-13 toast path for copying is code-only (no older emulator).
