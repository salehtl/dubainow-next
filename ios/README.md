# DubaiNow Next — demo app (iOS)

A native SwiftUI demo of the new DubaiNow core (Direction A, "Canvas"), built from the Paper file
`DubaiNow Next` (page C1 Core app · Canvas). It's a **demo**: personas instead of real accounts, fake
payments, a scripted assistant. No network, no real government systems.

## Run it
Open `DubaiNowDemo.xcodeproj` in Xcode 27 and run the `DubaiNowDemo` scheme on an iPhone simulator, or:

```bash
tools/build.sh run -welcome
```

Useful launch arguments (also settable in the scheme):

| Argument | What it does |
|---|---|
| `-welcome` | Start on the Welcome stories (sign-in) |
| `-persona sara\|khalid\|faisal\|wei\|guest` | Skip Welcome and sign in as a demo resident |
| `-tab home\|services\|activity\|search` | Start on a tab |
| `-route topic:driving-and-transport,service:pay-traffic-fine` | Push screens on the current tab |
| `-sheet profile\|happiness\|notifications\|widgetGallery\|payAll\|demoMenu` | Open a sheet |
| `-assistant` | Open the assistant |
| `-hour 9\|13\|18\|22` | Time of day for the Home wash |

In the app, **Profile → Demo controls** switches resident, resets the demo and changes the time of day.

## Personas
- **Sara Haddad** — mum of two in Al Barsha: car registration due, a speeding fine, DEWA bill, visa renewal (your turn), moving home.
- **Khalid Al Mansoori** — Emirati father building a house (Build a house journey).
- **Faisal Rahman** — café owner in Al Quoz with two vans (Pay all).
- **Wei Zhang** — site engineer; asks the assistant in Mandarin.
- **Guest** — public parts only.

## How it's built
- `App/` — app entry, launch options, router (`Route`, `SheetRoute`), tab bar, header chrome.
- `DesignSystem/` — `Tokens.swift` is generated from Paper tokens (`python3 tools/gen_tokens.py`, source `design/tokens.json`);
  SF Pro type styles at Paper sizes (`.dnFont`), our springs (`DNMotion`), haptics, hues (OKLab 160° gradients), shared components.
- `Data/` — the service catalogue (`Resources/dubainow-services.json`, 219 services), the demo store and personas.
- `Features/` — Welcome, Home, Widgets, Services, Search, Activity (and payments), Profile, Assistant.

Decisions: native iOS components and Liquid Glass wherever they exist; SF Pro and SF Symbols; light mode only for now;
English only for now (strings are String Catalog-ready).

## TestFlight (later)
Needs a paid Apple Developer Program team in Xcode → set `DEVELOPMENT_TEAM`, pick a unique App Store Connect name
(the live app owns "DubaiNow"), archive, upload, and add testers as internal testers.
