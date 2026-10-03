---
workflow: general-video
flow: automation
storyboard: no
message: "The new core app and service library work for every kind of resident: a father, a business owner, a mother, and a resident who uses DubaiNow in Arabic."
destination: presentation / YouTube
aspect: "16:9"
size: 1920x1080
language: en
audience: the CEO, then DubaiNow programme stakeholders
length: "63s (v3)"
angle: "Four residents (v3: i18n beat; v1 was A morning with Sara)"
sound: "music bed + UI sound effects"
---

## Intent

A short demo of the Core app vision (Direction A, Canvas) from the Paper page "C1 Core app · Canvas".
It follows Sara Haddad, a resident of Al Barsha 1, through one morning in the app. Every screen is
the real Paper design, rebuilt as live HTML so it can move.

## Stated (user answers, 2 Oct 2026)

- Story: "A morning with Sara" (recommended option).
- Format: 16:9, 1920×1080.
- Sound: music bed + effects. The music comes from HeyGen's catalogue; the user is signing in with
  `npx hyperframes auth login`.
- Review: build it and review the cut (no storyboard pass).

## Inferred / defaulted

- Length about 40s, from "short".
- Route: `/general-video`, because the source is Paper designs, not a website capture.
- Look: the C1 cover's language. Pastel morning wash (sky, lilac, peach hues from the DS), Instrument
  Serif display over Inter, DubaiNow blue (#365BEB) as the single accent.
- Motion: the Canvas motion tokens (snappy 0.30/0.86, smooth 0.40/0.90, bouncy 0.50/0.70 springs,
  press scale 0.97) drive the in-phone motion.
- Scenario data stays exactly as designed (AED 412.60 DEWA bill, AED 600 fine, Salik AED 18 → 118,
  visa step 3 of 5, plate M 48213).

## Assets

- Screens: Paper JSX for Home · Default, Search · Direct answer, Activity · Receipt + Happiness
  Meter, Activity · Request tracker, Home · Widget gallery (`source/paper/`, converted by
  `tools/jsx2html.mjs`).
- Logo: `brand/dubainow-logo.svg` (traced, three paths: dark, light, overlap).
- SFX: bundled HyperFrames library (Pixabay licence, `assets/sfx/CREDITS.md`).
- Fonts: Inter, Instrument Serif and JetBrains Mono (OFL, via Fontsource); SF Pro and SF Pro Rounded
  from the local Apple install (Apple licence: mock-ups of Apple-platform UI only).

## v2 (2 Oct 2026, user direction)

- Replace the serif with SF Pro.
- Show how the new core and the new service library benefit different residents: a father, a business owner,
  a mother, and a worker whose first language isn't English. Smooth and professional — it goes to the CEO.
- Choices (asked): worker = plain English + forgiving search; new screens = content variants in the video;
  business owner = café owner with two vans; length about 60 s.
- Personas: Khalid (father, Build a house — existing C1 board), Faisal (café owner, Pay all — variant),
  Sara (mother, Renew your family's visas — existing C1 board), Imran (delivery rider, search — variant).
- Music: HeyGen catalogue track fcde7f68 ("Minimalist, premium, corporate tech…"), bars 28–31 removed.
- Delivery: renders/DubaiNow-Next-Four-Residents.mp4 (presentation quality) and -share.mp4 (34 MB).

## v3 (2 Oct 2026, user direction: "showcase i18n rather than low English ability")

- Persona 4 is now Mahmoud, a site technician who uses DubaiNow in Arabic (was Imran, typo search).
- His beat: English Home → toggle to العربية → right-to-left wipe into the Arabic Home → Arabic search
  for "صوت العامل" matches "الإبلاغ عن مشكلة مع جهة العمل" by its Arabic alias.
- Built from the Paper AR masters (Arabic brief card and widget grid) and the DS Arabic rules: Tajawal,
  0–9 digits with "درهم" after the number, numbers and IDs in LTR isolates, gradients flipped to 200°,
  mirrored tab order, inset separators from the reading edge.
- Composed for the video (not in Paper yet): Mahmoud's EN/AR Home content, the Arabic search result
  screen, and the Arabic copy (service names, owners, headings), written for the video and worth a
  native review before any public use.
