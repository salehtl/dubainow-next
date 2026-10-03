# DubaiNow Next

A concept for the next DubaiNow: one app for Dubai's government services, built around what each resident
needs next. The designs and design system live in Paper (file `DubaiNow Next`); this repo holds the demo apps
and the material around them. Everything is a **demo**: personas instead of real accounts, fake payments,
a scripted assistant, no real government systems.

| Folder | What's in it |
|---|---|
| [`ios/`](ios/README.md) | Native SwiftUI demo app (iOS 26+). Shipped to TestFlight as 6.0 (1). |
| [`android/`](android/docs/BUILD-BRIEF.md) | Native Kotlin + Jetpack Compose port of the same demo (Android 8+). Shared with Firebase App Distribution. |
| [`catalogue/`](catalogue/README.md) | The service catalogue: 219 services (standalone, bundles, journeys) in 12 topics, from 351 legacy records. Both apps ship `dubainow-services.json` from here. |
| `brand/` | The DubaiNow logo as SVG. |
| `videos/core-vision-demo/` | "Four residents" vision video (HyperFrames source; renders are built locally and not committed). |
| [`liquid-glass/`](liquid-glass/README.md) | Liquid Glass experiments (live backdrop sampling). |
| `figma-to-paper/` | Scripts and runbook from the Figma → Paper migration. |

## Demo apps

Both apps have the same four residents (Sara, Khalid, Faisal, Wei) plus guest mode, and follow the same
5-minute story: [`ios/docs/DEMO-SCRIPT.md`](ios/docs/DEMO-SCRIPT.md).

**iOS** — Xcode 27. Open `ios/DubaiNowDemo.xcodeproj`, or `ios/tools/build.sh run -welcome`.
TestFlight upload: `ios/tools/testflight.sh` (team 8CFG28JHP3, bundle `com.salehtl.DubaiNowNext`).

**Android** — JDK 21 + Android SDK 36 (command-line tools, no Android Studio needed).
`android/tools/run.sh persona sara` builds, installs and launches on a running emulator.
Release: `android/tools/release.sh` → `android/build/dist/DubaiNowNext-<version>-<build>.apk`, then
[`android/docs/FIREBASE.md`](android/docs/FIREBASE.md). The release needs `android/dubainow-upload.jks` and
`android/keystore.properties`, which are kept out of git: keep your own backup of both.

QA helpers (Android): `tools/sweep.py` (crash sweep over every route), `tools/tap.py` (tap by label),
`tools/emu-lock.sh` (share emulators between parallel agents).
