# Storyboard — "Four residents, one DubaiNow" (v3, i18n)

63 s, 1920×1080 at 60 fps. One composition (`index.html`, built from `src/` by
`node tools/build.mjs`). Persona switches land on the music's 8-bar markers
(125 BPM; markers at 15.80, 31.15, 46.51 and 54.19 s).

Shape: title card → `device-surface-showcase` (one phone; each resident's screens swipe in
vertically, like changing whose phone it is) → library board → centred end card.
Rules used: `spring-pop-entrance`, `press-release-spring`, `cursor-click-ripple`,
`multi-phase-camera` (pan + push on the phone), `vertical-spring-ticker` (count-ups).

## 0 — Title (0.0–4.3 s)
Logo assembles; "DubaiNow Next" / "Four residents, one DubaiNow." / "How the new core app and
service library work for the people who use them." The logo docks top left; the phone rises.

## 1 — Khalid, father (4.3–15.8 s) · Start to finish
Paper: Home · A journey on Home (Khalid). Needs you shows "Build a house · Your turn · 4 of 7".
The camera pushes on the card, then Home scrolls to the Build a house widget: stages 1–3 check
off on the beat drop (10.0 s), stage 4 pulses.

## 2 — Faisal, café owner (15.8–27.3 s) · All at once
Content variant of Home · Default: Needs you 3, Pay all · AED 2,035.20 (DEWA 1,486.20 + du
399.00 + traffic fine 150.00). Tap Pay all → receipt sheet (variant of the Salik receipt):
"Paid 2 bills and 1 fine", rows per item, Happiness Meter "How was paying everything at once?"
→ Happy.

## 3 — Sara, mother (27.3–36.9 s) · All at once, suggested
Paper: Home · Pay all and a suggested widget (Sara). The Family visas suggestion arrives on the
marker (31.15 s), opening space in the canvas; rings fill (Layla, Omar 73 days). Tap + → toast
"Family visas added to Home · Undo" (C1 toast component); tap Renew 2.

## 4 — Mahmoud, site technician (36.9–48.4 s) · Arabic and RTL
Replaces Imran (v2, typo search) after the user asked to showcase i18n rather than low English.
Content variant of Home · Default in English: Needs you 2, DEWA AED 186.40 (Al Qusais 1).
On the beat (38.83 s) the copy's "English | العربية" toggle flips and the screen wipes right to
left (glow line) into the same Home in Arabic, built from the Paper AR masters and the DS RTL rules:
mirrored status bar, header and rail; "يحتاج انتباهك"; brief card "ديوا · 186.40 درهم" with the
number in an LTR isolate; Tajawal at AR sizes; hue gradients flipped to 200°; tab bar mirrored
(الرئيسية on the right, search on the left). Camera pushes on the brief card. Tap search → types
"صوت العامل" → "الإبلاغ عن مشكلة مع جهة العمل" (Report a problem with your employer), also called
«صوت العامل», with similar results and the "Not what you're looking for?" actions, all RTL with
inset separators from the reading edge. The alias pulses near the marker (46.5 s).

## 5 — Library (48.4–54.2 s)
"One library, three kinds of service." Standalone (Check your visa status card), All at once
(Bills and fines widget), Start to finish (Build a house widget) — C1 widget boards.

## 6 — End card (54.2–63.0 s)
Logo, "The new DubaiNow." / "Built around what residents need to do." /
"Core app and service library · Design preview · October 2026". Music outro.
