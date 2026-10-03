# Assistant — build notes

DubaiNow's own assistant (Paper C1 row 11: V83-0, W1D-0, screens RUW-0 … UXH-0). Scripted for the
demo: keyword matching, no network, no AI, no real microphone. Everything it does goes through
`DemoStore`, so the screen underneath updates while it works.

## Files
- `AssistantOverlay.swift` — the overlay (`AssistantOverlay()`, same type name and init as the placeholder):
  layout of the three zones, the 5 s note after a task, VoiceOver modal + escape.
- `AssistantEngine.swift` — state machine and all scripts; `AssistantAuth` (LocalAuthentication, 0.8 s
  fallback in the Simulator); `AssistantLaunch` (temporary launch args); read-only helpers
  `DemoStore.assistantRenewalPlan()` and `Router.assistantCurrentPath`.
- `AssistantModel.swift` — face states, card/surface/confirm models, `AssistantIntent` keyword matching.
- `AssistantFace.swift` — the logo in a glass orb with the ring of light (8 states), comet, ripples.
- `AssistantBackdrop.swift` — veil, wash from the logo, bottom frost, animated edge light.
- `AssistantCardView.swift` — the glass card and its surfaces (task checklist, payables, request tracker,
  journey, employer report, list, energy bars), inline Happiness faces.
- `AssistantBottomBar.swift` — suggestion chips, ask bar (text field + mic), listening transcript and
  waveform, the confirm pill ("Pay AED 1,020" with Face ID glyph / black "UAE PASS" with Touch ID glyph).

## Scripts
| Ask | What happens |
|---|---|
| "Renew my car registration" | Checklist animates (insurance ✓, fine ✓, preparing → ✓), ring fills, your turn → Pay AED 1,020 → `completePayment(.vehicleRenewal…, doneByAssistant: true)` → done card (rows, valid until, reference, Receipt) + Happiness faces → close → "Patrol renewed" note; Home already updated (Needs you −2, Pay all gone). Vehicle, fines and total come from `store.state`; fee 420 is a constant. |
| "How much do I owe?" | Payables + total, confirm "Pay all · AED x" (`payAllRequest()`, any persona). |
| "Pay everything for the café" | Same Pay all flow, café wording. |
| "When will my visa renewal be done?" / "visa" | Request tracker card (from `state.requests`), "Upload now" closes and pushes `.request(id)` (skipped if already there). No visa request → residence visa document instead. |
| "Top up Salik with AED 100" / "Top up my nol card with AED 50" | Task → confirm → `.salikTopUp` / `.nolTopUp`. Amount parsed from the text. |
| "What's next for my house?" / "…my move?" / "villa drawings" | Journey status from `state.journeys` (stage x of n, now/next), "Open the journey". |
| "Which van has a fine?" | Vehicle with fines → confirm pay. |
| "How much power did the café use?" | Energy answer with 6-month bars. |
| "When is the next metro?" | Departures from `state.metro`. |
| "What do I need to do?" | Needs you summary. |
| On a request tracker: "Scan my passport with me", "What will the fees be?", "Remind me tonight at 8" | Context suggestions (Paper UR2-0) with scripted answers. |
| Wei: "我的工资两个月没发了，怎么办？" | Mandarin card (system font = PingFang SC): fields fill in from UAE PASS, your turn with the black UAE PASS pill ("投诉将以您的名义提交") → sent with a reference. Quiet: no celebration, no rating. "Show in English" on the card; "Continue in English" offered after. Independent of Wei's data. |
| Anything else | "I can't do that in this demo yet. Try one of these." + suggestions (Mandarin too). |

Stop and "I'll do it myself" are on every work card (take over opens the service / Pay sheet / Top up sheet).
Suggestions: up to 3 for the screen (persona prompts; never Pay all on Home when the Pay all button is
showing; never a task already done), one after a task (same part of life first).
Ways out: ×, swipe the ask bar down, tap the screen, VoiceOver escape.
Reduce Motion: no ripples/comet/glow/breathing; still 2 pt edge line; ring still shows progress.
Large text: card scrolls; amounts move under the step title.

## Launch arguments
- `-persona sara -assistant` — idle (tap the logo).
- `-persona sara -assistant -assistantListen` — hold to talk (fake transcript, then sends).
- `-persona sara -assistant -assistantRun "Renew my car registration"` — runs a script (≈7 s to your turn).
- add `-assistantConfirm` — confirms automatically at your turn; add `-assistantClose` — closes 2.5 s after done (shows the note + updated screen).
- `-persona wei -tab services -assistant -assistantRun "我的工资两个月没发了，怎么办？"` (add `-assistantEnglish` for English).
- `-persona sara -route request:sara-visa -assistant` — tracker suggestions.

## Deviations / decisions
- Ask bar placeholder follows Paper: "Ask or tell me what to do" when idle, "Ask or add something" once a card is up.
- Last renewal step says "Pay, renew, save the new registration card" (Paper says "licence"); done row "New card saved to Documents".
- After closing a finished task the overlay stays presented for 5 s as a non-modal, click-through note at the logo (tap = receipt), then calls `closeAssistant()`.
- The employer report adds nothing to history (no payment), as instructed.

## Requests for the orchestrator (shared code)
1. **Activity record for non-payment tasks** — e.g. `DemoStore.recordAssistantTask(title:entity:reference:symbol:hue:)` so the employer report also appears in Activity "Done with your assistant" (spec rule 5).
2. **"Not now goes to Needs you"** (rule 7) — a store action to park a pending assistant confirm (a `PaymentRequest`) as a Needs you item when the overlay is closed at "your turn". Today × just closes.
3. **Hide the tab bar while the assistant is up** (`.toolbarVisibility(router.isAssistantPresented ? .hidden : .automatic, for: .tabBar)` inside each tab) so it truly morphs into the ask bar; today the frost covers it.
4. **Renewal fee on the model** (`Vehicle.renewalFee`), replacing the AED 420 constant in `AssistantRenewalPlan`.
5. Optional: a `Router.assistantNote` so the header logo itself stretches into the note (now drawn by the overlay).
6. Wei's Mandarin and nol prompts are added by the assistant only if missing from `assistantPrompts` (the personas helper's data already has them, so this is a no-op after merge).
7. Not built: Dynamic Island / Live Activity for a task still running after close; Siri / Action button entry.

## Fixes for 6.0 (2) (3 Oct, found by the Android QA pass)
- **Start scanning** now opens the tracker's own fake passport camera over the assistant (`AssistantFollowUp.scanPassport`,
  presented by `AssistantOverlay` with `activityPassportCapture`). Use this photo sends it ("Sent to GDRFA") and the assistant
  closes once the request leaves the step it started on (`ScanWatch`); closing the camera keeps the assistant open. With no
  passport step waiting, it falls back to Coming soon ("Scanning with the camera comes with the service flows.").
- **Remind me** at the visa fee step (no Needs you item of its own) says "…to pay the fees to renew your residence visa";
  bills read "…to pay your electricity & water bill".
