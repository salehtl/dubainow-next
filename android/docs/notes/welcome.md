# Welcome (pre-login) — Android build notes

Paper: page C1 `p-W-0`, stories `RSF-0` `S5Z-0` `S96-0` `SCD-0` `SFK-0` `SIR-0`, rules board `SVF-0`; sheet chrome from
the Android kit `MDV-0` (M5 Overlays). iOS reference: `ios/DubaiNowDemo/Features/Welcome/*`, `ios/docs/notes/Welcome.md`.

## Files
- `WelcomeScreen.kt` — `fun WelcomeScreen()` (signature unchanged). The story player and the fixed panel; it presents its
  own sheets (Welcome is outside the main scaffold and the router).
- `WelcomeStories.kt` — the 6 stories (copy identical to iOS), `WelcomeStoryClock`, `WelcomeSheet`, `WelcomeLaunch`
  (debug extras, read once per launch).
- `WelcomeHeroes.kt` — the six heroes drawn from Canvas objects on Paper's 402 × 392 canvas at Paper's positions and
  tilts (same numbers as iOS): orbit of tiles · residency + DEWA brief cards with Pay all · parking / Salik (lifted out
  of its dashed slot) / prayer widgets · Build a house route + journey card · search field + answer card + alias chip ·
  30×30 poster with Free badge and the "See events" link sticker.
- `WelcomeSignInSheet.kt` — "Sign in with UAE PASS" M3 modal bottom sheet: hand-off → persona picker → biometric check.

## What works
- Edge-to-edge player: hue wash (start → end 62% → start) behind the status and gesture bars, cross-fades 300 ms.
  Segmented progress (6 × 3 dp), 6 s per story, filled by a frame loop that pauses and keeps elapsed time. Tap the
  right two-thirds = next, left third = back, horizontal swipe also works, touch-and-hold pauses. Never loops: after
  story 6 every segment is full; next from there starts at story 1. Pauses while a sheet is up or the app isn't resumed.
- Remove animations (animator duration scale 0) or TalkBack (touch exploration): no auto-advance, current segment full.
  The story area is one TalkBack element ("Story 2 of 6, What needs you, first." + state description with
  source/kicker/body) and a polite live region, so the new story is read when it changes. Double-tap = next (on
  story 6 it is labelled "Back to story 1", like a tap). Custom actions behave like VoiceOver's adjustable element:
  Next story (not on story 6), Previous story (not on story 1), See events (story 6). Heroes are hidden.
- Motion: hero objects rise with `DNMotion.smooth()`, 40 ms apart, settling at their tilt; copy fades in last (180 ms
  delay); life while playing (orbit dots, floating tiles and Salik, breathing current stop, blinking caret, route draws
  in, Free badge float). All still with Remove animations (250 ms fades instead).
- Larger text: hero text is part of the picture (drawn at fontScale 1); the hero scales to fit, hides below 50% and at
  font scale ≥ 1.5, where the copy scrolls. Header capped at 1.3. Panel never truncates (labels wrap; checked at 200%).
  Sheets checked at 200% too: the picker, hand-off and check reflow; the UAE PASS badge and avatar initials keep
  their drawn size (they sit in fixed shapes); the Coming Soon sheets open in full and scroll, so OK is reachable.
- Layout direction: Welcome is forced left to right (English-only build). On a phone set to Arabic the app is laid
  out right to left (`supportsRtl`), which would mirror the hero positions but not their tilts, and flip the
  progress bar against its fill. Remove the override in `WelcomeScreen()` when Arabic is added (Paper: in Arabic the
  segments fill right to left and the tap zones mirror).
- Arabic pill hidden (English build); comment left in the header where it goes.
- Panel: black UAE PASS pill (Fingerprint, 56 dp, ripple) → sign-in sheet · white "Continue as guest" →
  `store.continueAsGuest()` · "New to UAE PASS? Create an account" → ComingSoon `.NotInDemo` sheet.
- Sign-in sheet (M3, radius 28, drag handle, stops 12 dp under the status bar, close at the end of the title row):
  hand-off 1.6 s ("Signing in with UAE PASS", DubaiNow → UAE PASS marks with travelling dots; spinner when reduced) →
  "Choose a demo resident" (Avatar 48, full name, tagline, no chevron per Paper LOA-0; footer with lock "This is a
  demo. No real accounts are used."; a persona without a tagline shows just the name, centred) → check:
  `BiometricPrompt` on the `FragmentActivity` when `BIOMETRIC_STRONG | DEVICE_CREDENTIAL` (WEAK on API 28–29) can
  authenticate; otherwise 0.7 s "Confirming it's you" pulse → "Verified" tick 0.8 s + success haptic → sheet slides
  away → `store.signIn(persona)`. Swipe/back/scrim are blocked while the check runs (Back is a `BackHandler` in the
  sheet: material3 1.3 reads `shouldDismissOnBackPress` only when the sheet opens). A cancelled prompt still carries
  on (the demo never blocks), like iOS. TalkBack: each stage has a pane title ("Signing in with UAE PASS", "Choose a
  demo resident", "Confirming it’s you") so it is announced when it replaces the last one; the check is one view
  whose live region then reads "Verified, Signing in as …" (iOS's announcement).
- "See events" sticker (story 6) → ComingSoon `.NotInDemo` sheet; its taps never also change the story. Its touch
  area reaches past the 44 dp sticker so it stays at least 48 dp on screen when the hero is scaled down.
- Story and open sheet survive rotation.

## Review pass (3 Oct)
Fixed: TalkBack didn't hear the story change after next/previous (live region added) and its next/previous actions
wrapped round (now like VoiceOver's adjustable); sheet stages and the "Verified" moment weren't announced (pane titles,
one live-region view for the check); avatar initials were read before each name; empty taglines left a blank line;
"See events" fell under 48 dp when the hero scales down; a cut-short touch could leave the story paused; at 200%
the UAE PASS badge and small avatars clipped and the Coming Soon sheets hid OK below the screen; Welcome mirrored
on Arabic-locale phones; the hero fade ignored Remove animations; `welcomeFontScale` didn't reach the sheets.

## QA pass (3 Oct)
- Back during the check (`Confirming it’s you` or the `Verified` tick) dismissed the sheet and the sign-in never
  finished. material3 1.3.2 copies `shouldDismissOnBackPress` once, when the sheet's dialog is created (before any
  check), and its own back callback then hides the sheet without asking `confirmValueChange`. The sheet now always
  passes `shouldDismissOnBackPress = false` and handles Back with a `BackHandler`: it closes like Cancel, and is
  swallowed while the check runs. The Verified step signs in even if the slide-away is cut short (`try`/`finally`).
  Checked on the emulator: Back and the edge-swipe gesture during Confirming and Verified leave the sheet up and the
  flow ends on Home (Sara, Khalid); Back on the hand-off and the picker still closes the sheet.
- The resident rows ended in a chevron; removed (Paper LOA-0: Android navigation rows have none). The whole 72 dp row
  is still the target.

## Debug extras
`welcomeStory 1…6` · `welcomeSheet signIn|handoff|picker|verifying|verified|createAccount|events`
(handoff/verifying/verified hold still) · `welcomeAutoSignIn true` (with a sign-in sheet: picks Sara after 1 s; runs
the whole flow to Home) · `welcomePaused true` (no auto-advance, for screenshots) · `welcomeReduceMotion true` ·
`welcomeTalkBack true` · `welcomeFontScale 2` (simulates the font size setting, in the player and in its sheets).

## Not done / differences
- Not checked by touch on the emulator (shared emulators, no taps): tap zones, hold and swipe are reasoned, not
  tested; the auto sign-in path was run end to end.
- The emulator has no screen lock, so the real `BiometricPrompt` path wasn't exercised (fallback path was).
- `hasSeenTour` isn't used (as iOS): the demo always plays the full tour.
- Shadows are single tinted M3 shadows (Android can't draw the iOS near + far pair); SF Rounded numerals are Inter
  tabular per the brief. The Salik tile/glyph uses Material `Toll` (same as `DNIcons` "road.lanes").

## Shared requests (for the orchestrator)
- `DNIcons`: add `"person.crop.circle.badge.plus" to Icons.Rounded.PersonAdd` and
  `"figure.run" to Icons.AutoMirrored.Rounded.DirectionsRun`. Until then Welcome renders its two ComingSoon sheets
  with a private copy of `ComingSoonContent` that takes an `ImageVector` (`WelcomeComingSoonContent`); with the
  mappings it could use the shared `ComingSoonContent` directly.
- Optional: a shared `rememberReduceMotion()` (animator duration scale 0) and `rememberTalkBackOn()` in `design/` —
  Welcome has private ones; other features adding big motion will need the same.
- Note for `DNHaptics.success`: `HapticFeedbackConstants.CONFIRM` is API 30+ (minSdk 26); it's a no-op/lint issue on
  older devices.
- App-wide layout direction: the manifest has `android:supportsRtl="true"` and nothing forces LTR, so on a phone set
  to Arabic every screen mirrors around English text. While the build is English-only, provide
  `LocalLayoutDirection provides LayoutDirection.Ltr` at the root (or set `supportsRtl` false). Welcome already
  forces LTR for itself.
- `Avatar` (design/components/Marks.kt): the initials' size is in sp, so they grow with the font size inside a fixed
  circle; at 200% a 26 dp avatar clips its letter. Suggest drawing the initials at a fixed size (fontScale 1).
  Welcome wraps its avatars to do that.
- Shared ComingSoon in `SheetHost`: with larger text the sheet opens half-height (`skipPartiallyExpanded = false`)
  with OK below the screen, and `ComingSoonContent` doesn't scroll. Welcome's copy opens in full and scrolls; the
  shared one could do the same.
- Persona stubs: Khalid, Faisal and Wei have empty taglines in this copy (being ported); the picker handles it.
