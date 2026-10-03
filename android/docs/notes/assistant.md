# Assistant (Android) — build notes

Port of `ios/DubaiNowDemo/Features/Assistant/*` (Paper C1 row 11: V83-0, W1D-0, screens RUW-0 … UXH-0).
Scripted for the demo: keyword matching, no network, no AI, no real microphone. Everything goes
through `DemoStore`, so the screen underneath updates while it works.

## Files
- `AssistantOverlay.kt` — **`fun AssistantOverlay()`** (signature unchanged). Layout of the three zones
  (face top centre exactly over the header logo, card hanging from it, your side at the bottom),
  `BackHandler` closes it, `BiometricPrompt` confirm (`assistantAuthenticate`), note → snackbar.
- `AssistantEngine.kt` — state machine (Compose state + coroutines in the overlay's scope) and every
  script, ported 1:1 with iOS copy; `DemoStore.assistantRenewalPlan()`; `AssistantLaunch` debug extras.
- `AssistantModel.kt` — face states, card/surface/confirm models, `AssistantIntent.match` (same keywords),
  `AssistantFormat`, `assistantIcon()` (private icon mappings, see below).
- `AssistantFace.kt` — the logo in a white tonal orb (no glass) with the ring drawn on a `Canvas`:
  ripples (listening), comet (thinking, 1 turn / 1.4 s), progress arc (working), light gathered at the
  bottom + one nod (your turn), conic ring closing + one glow (done), soft still ring (quiet).
- `AssistantBackdrop.kt` — veil (screen stays visible and live behind it), wash from the logo, bottom
  frost, animated edge light (sweep gradient of the brand blues rotating round the screen edge, 9 s/turn,
  breathing; follows the display's corner radius on API 31+). Tap the veil = close.
- `AssistantCardView.kt` — the white card (radius 28) and its surfaces: task checklist (M3
  `CircularProgressIndicator` for the active step), payables, request tracker, journey, employer report
  (with English/Arabic original), list rows, energy bars; inline `HappinessFacesPicker`; Stop / I'll do it myself.
- `AssistantBottomBar.kt` — suggestion chips (M3 `ElevatedAssistChip`), transcript + waveform, ask bar
  (pill `BasicTextField` + mic/send), close `TonalIconButton`, the confirm (accent "Pay AED x" or black
  "Confirm with UAE PASS", fingerprint glyph, "Confirming…"), "With Google Pay · Change". Swipe the bar down = close.

## Scripts (same as iOS)
Renew my car registration (AED 1,020 with Sara's fine / 420 without, from `store.state`; one confirm →
`completePayment(PaymentRequest(…, PaymentPurpose.VehicleRenewal(…), doneByAssistant = true))` → done card
with receipt row + Happiness faces) · How much do I owe? (Pay all) · Pay everything for the café · visa
status (tracker, "Upload now" pushes `Route.Request`) · Salik / nol top-up · house / move journey · which
van has a fine · café energy · next metro · what do I need to do · tracker context (scan passport, fees,
remind me) · Wei in Mandarin (Chinese card, "Show in English" / "显示中文", UAE PASS confirm → reference,
`store.addRequest(…)`, quiet: no rating; "Continue in English" offered after) · fallback "I can't do that in
this demo yet. Try one of these." (never a dead end). Suggestion rules as iOS (≤3 for the screen, 1 after a
task, never Pay all on Home root while the Pay all button shows, done tasks not repeated).
Hold-to-talk (`router.assistantSeed == "__listen__"`) types out a fake transcript, then sends.

## Debug extras
`assistant true` plus:
- `assistantRun <text or key>` — runs a script. Keys (adb can't pass spaces through the helper):
  `renew owe visa salik nol house move cafe van energy metro needs fees remind scan wei unknown`;
  underscores read as spaces (`assistantRun Top_up_Salik_with_AED_50`).
- `assistantConfirm true` — confirms 1.5 s into your turn. `assistantStop true` — taps Stop instead.
- `assistantClose true` — closes 2.5 s after done (shows the note snackbar + updated screen).
- `assistantEnglish true` — Mandarin cards start in English. `assistantListen true` — hold-to-talk.
- `assistantPaidFine true` — pays the fines blocking the renewal first, so `assistantRun renew` takes the
  AED 420 path (no fine step, "The renewal comes to AED 420").
- e.g. `persona sara tab home assistant true assistantRun renew assistantConfirm true` (done ≈ 12–15 s after launch),
  `persona wei tab services assistant true assistantRun wei`, `persona sara tab activity route request:sara-visa assistant true`.

## Android decisions / deviations
- No Liquid Glass: solid white card and chips on a light veil (canvas at 22 %, as iOS, so the screen stays
  readable underneath: Paper V83-0 "the edge light and a soft wash are the only change"); the edge light is layered strokes (no blur,
  works on API 26+). Reduce Motion = "Remove animations" (animator scale 0): no ripples/comet/glow, still
  2 dp edge line, ring still shows progress, spinner becomes a still ring.
- Confirm: `androidx.biometric.BiometricPrompt` (BIOMETRIC_WEAK) when enrolled; otherwise 0.9 s (brief)
  "Confirming…". Cancel = stays at your turn. Pay glyph is the fingerprint (no Face ID glyph on Android).
- After closing a finished task the iOS logo "stretches into a note"; on Android the overlay closes fully
  (tab bar returns) and the note is the M3 snackbar via `LocalToasts`: "Patrol renewed · Receipt" (5 s,
  action opens the receipt sheet); "Report sent" for Wei (no receipt).
- "Upload now" / take-over hand over immediately as the overlay fades (no 350 ms delay).
- TalkBack: like iOS, the engine announces each new headline, "Your turn. Pay AED 1,020." and Stop/Done
  (`View.announceForAccessibility`, deprecation suppressed: each answer is a new card node, so a live region
  on the headline is not read when a card replaces another). The transcript is not a live region (it read
  every word). The face is one node ("DubaiNow assistant", state "Working on it"…); composite buttons
  (Stop, I'll do it myself, Change, Receipt, Show in English, the confirm) are single nodes with one label,
  in Mandarin on Mandarin cards. The overlay's full-screen node (`paneTitle = "DubaiNow assistant"`) makes
  Compose drop the covered screen from the accessibility tree, so TalkBack stays in the assistant
  (checked with a uiautomator dump), the Android equivalent of iOS `.isModal`. Keep that semantics node.
- Suggestions never repeat the question just asked (iOS re-offered "Scan my passport with me" and Wei's
  Mandarin prompt after Stop; Paper rule 6 "Suggestions add, never repeat").
- Large text: the confirm, Stop / I'll do it myself and the "With Google Pay · Change" caption wrap to two
  lines instead of cutting off.

## Requests for the orchestrator (shared code)
1. `DNIcons`: add `camera.fill` → `Icons.Rounded.PhotoCamera` (the "Scan your passport" Coming soon sheet is
   rendered by shared `ComingSoonContent`, so it shows the grid fallback today). Also nice to have centrally:
   `checklist`, `banknote`, `camera`, `character.bubble`, `doc.text`, `stop.fill`, `mic.fill`, `arrow.up`
   (mapped privately in `assistantIcon()` for now).
2. `HappinessFacesPicker`: `border(0.dp, …)` draws a hairline ring round unselected faces; apply the border
   only when selected.
3. ~~Hide MainScaffold from TalkBack~~: not needed (see TalkBack above). Related, in `HappinessFacesPicker`:
   each face reads its label twice ("Happy, Happy"): the clickable column sets `contentDescription` and also
   shows the label text; use `clearAndSetSemantics` or drop the description when `showsLabels`.
4. (From iOS) "Not now goes to Needs you": a store action to park a pending assistant confirm as a Needs you
   item when closed at your turn. Today × just closes.
5. (From iOS) `Vehicle.renewalFee` on the model, replacing the AED 420 constant in `assistantRenewalPlan()`.
6. (From iOS) an Activity record for non-payment assistant tasks ("Done with your assistant").
7. Wei's Mandarin and nol prompts are added by the assistant only if missing from `assistantPrompts`
   (no-op once the personas build lands).

## Review pass (3 Oct)
Every script run end to end on emulator-5556 with the debug extras (Sara renewal with the fine AED 1,020 and
without it AED 420, owe / Pay all, visa tracker, Salik top-up, needs you, move/house, metro, energy, fees,
scan, stop, fallback, hold-to-talk; Wei in Mandarin, English, Stop and sent) plus the close note. Fixed:
TalkBack (announcements, face read as one node, duplicate labels, transcript spam, mixed-language labels),
the veil (0.58 → 0.22 as iOS/Paper), suggestions repeating the question just asked, `close()` running twice
while the overlay fades (back or a tap during the fade showed the note again), 0.9 s confirm fallback,
large-text wrapping, haptic on "Show in English". Not checked on device: typing with the keyboard, 200 %
font, a real fingerprint prompt (the emulator has none enrolled), the receipt sheet over the assistant.

## QA pass (3 Oct)
Checked on emulator-5558 (Sara, Wei, Faisal, Khalid). Fixed:
- **Remind me tonight at 8** at the visa fee step said "…to electricity & water bill." When the tracker's
  request has no Needs you item (the fee step), the reminder names its current step and the request: "…to pay
  the fees to renew your residence visa." The fallback is grammatical too: bills and fines read "to pay your
  electricity & water bill", other items read as their title ("to review your villa drawings").
- **Start scanning** (Scan my passport with me) opened a "Not part of this demo" sheet that pointed to a
  missing "Upload now". It now opens the tracker's own fake camera (`ActivityCaptureHost` from
  features/activity, new `AssistantFollowUp.ScanPassport`) over the assistant. Close goes back to the
  assistant. Use this photo moves the request on ("Sent to GDRFA" toast), and the assistant closes on the
  updated tracker (Pay AED 1,146.50). This works from any screen while a request waits on a passport photo.
  With none waiting, the sheet stays and reads "Scanning with the camera comes with the service flows."
- **Swipe the bar down** rarely closed: the drag counted only after touch slop and missed the lift-off, and
  the bar sits ~64 dp above the screen edge. It now measures from touch-down to lift-off (like iOS's
  translation) and closes at 40 dp, or on a > 1,000 dp/s flick past 20 dp. Taps on ×, the field, mic and
  the confirm still work. A 19 dp drag and an upward swipe don't close it.
- **Caption and transcript over sharp screen text** ("With Google Pay · Change" on "Your widgets", "It's
  sent in your name" on the beach widget, "Listening…" on Services tiles): the backdrop now gets the bottom
  bar's measured height (`bottomBand`). Behind your side (chips, caption, transcript, bar) the frost turns
  nearly solid (≈ 0.96 together with the frost below it, and 64 dp of fade above), standing in for iOS's
  material blur. Text behind measures ~3 % contrast (240 on 247). The rest of the screen keeps the light veil.
