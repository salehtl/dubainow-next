# Welcome (pre-login) — build notes

Paper: page C1 `p-W-0`, English stories `RSF-0` `S5Z-0` `S96-0` `SCD-0` `SFK-0` `SIR-0`, rules board `SVF-0`.

## What's built
- `WelcomeView.swift` — the full-screen story player and the fixed sign-in panel. Type name and `init()` unchanged.
  - 6 stories, each in its own hue wash (start → end at 62% → start), kicker + one-line 34/40 headline + body.
  - Segmented progress (6 × 3 pt). 6 s per story, the segment fills linearly (`TimelineView`), driven by
    `WelcomeStoryClock`. Tap the right two-thirds = next, left third = back, swipe sideways also works,
    touch-and-hold pauses (letting go resumes). Never loops: after story 6 it stops with every segment full;
    tapping next from the last story starts again from story 1. Pauses while a sheet is up or the app is inactive.
  - Reduce Motion or VoiceOver: no auto-advance (current segment shows full). The story area is one adjustable
    element ("Story 2 of 6, What needs you, first." + kicker/body as value; swipe up/down changes story;
    story 6 has a "See events" custom action). Heroes are hidden from VoiceOver.
  - Larger text: the hero keeps Paper's composition and scales down; it hides below 50% and at all
    accessibility sizes, where the copy scrolls. Panel is capped at AX3 and never truncates (labels wrap).
  - Motion: hue cross-fades 300 ms; hero objects rise with `DNMotion.smooth`, 40 ms apart, settling at their
    tilt; copy fades in last. A little life while a story plays (orbit dots drift, tiles float, the Salik widget
    floats, the journey's current stop breathes, the search caret blinks, the route draws in). All of it is off
    with Reduce Motion.
  - Haptics: `selection` on story change by tap/swipe/VoiceOver adjust; `success` when sign-in is verified.
  - The عربي language pill is hidden (English-only build) — comment left where it goes in the header.
- `WelcomeHeroes.swift` — the six heroes drawn from Canvas objects at Paper's exact positions/tilts on a
  402 × 392 canvas: orbit of service tiles around the app icon · residency + DEWA brief cards with the Pay all pill ·
  parking / Salik (lifted out of its dashed slot) / prayer widgets · Build a house route across 7 stages with the
  journey card · search field + answer card + "amer → Renew your residence visa" alias chip · Dubai Fitness
  Challenge 30×30 poster with Free badge and the "See events" link sticker (the only tappable thing in a story;
  opens a `.notInDemo` ComingSoon sheet). Amounts via `Money` (Sara's sample data).
- `WelcomeStories.swift` — story model (copy as `LocalizedStringResource`) and `WelcomeStoryClock`.
- `WelcomeSignInSheet.swift` — "Sign in with UAE PASS": large sheet with native close button →
  hand-off ("Signing in with UAE PASS", DubaiNow → UAE PASS marks with travelling dots, 1.6 s) →
  "Choose a demo resident" (inset-grouped List: Avatar, full name, tagline for each `Personas.all`; footer
  "This is a demo. No real accounts are used.") → Face ID-style check (`LAContext.deviceOwnerAuthentication` when
  available; otherwise a short pulse) → "Verified" tick 0.8 s + success haptic → the sheet dismisses and
  `store.signIn(persona)` runs in `onDismiss` (RootView then fades to Home).
- Panel: UAE PASS pill (black, `touchid`, Large 54) → sign-in sheet · white "Continue as guest" →
  `store.continueAsGuest()` · "New to UAE PASS? Create an account" → `ComingSoon` (`.notInDemo`,
  "Create a UAE PASS account"). Sheets use local `.sheet` state (Welcome is outside the TabView/router).

## Launch arguments
- `-welcome` — start at Welcome (app-level).
- `-welcomeStory 1…6` — start on a story.
- `-welcomeSheet signIn|handoff|picker|verifying|verified|createAccount|events` — open a sheet/stage at launch
  (handoff/verifying/verified hold still for screenshots).
- `-welcomeAutoSignIn` (with `-welcomeSheet signIn`) — picks the first resident automatically to run the whole
  flow without taps.

## Shared requests (for the orchestrator)
- None required. Optional: `DemoStore.hasSeenTour` exists but nothing sets it; the rules board says later
  visits show announcements only. The demo always plays the full tour (better for showing the boss). If wanted,
  set `hasSeenTour = true` in `signIn`/`continueAsGuest` and Welcome can then show only story 6 + story 1 as a
  still end card.
- `Hue`'s light RGB values are private, so the wash interpolates in sRGB (3 stops) rather than OKLab — visually
  indistinguishable on these pastels. Exposing `Hue.washGradient` (start → end 62% → start, OKLab) would be tidier.
- Info.plist already has `NSFaceIDUsageDescription`; its text says "payments and changes to your account" —
  consider adding "and to sign in" since Welcome uses it too.
