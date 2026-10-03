# Profile feature — notes

## What's built
| File | What |
|---|---|
| `ProfileSheet.swift` | `ProfileSheet` (Paper `EOJ-0`, guest `X6U-0`): its own `NavigationStack(path:)` with `.dnRouteDestinations()` plus `ProfilePage` destinations. Close (role `.close`) top left, avatar, "Verified with UAE PASS", Documents shelf (all docs, "See all N" → `DocumentsView`), Your things (Vehicles + fine badge, Home, Family, Payment methods), Settings, Sign out (confirmation dialog → `store.signOut()`), Delete your account, Demo controls row (→ `router.present(.demoMenu)`), footer (long press → demo controls). Guest: sign in = persona picker dialog, Create an account → ComingSoon. |
| `ProfileSettingsPages.swift` | Your home, Payment methods, Notifications settings (Important on / Optional off / quiet hours, `@AppStorage`), Accessibility and display (live values + Open iPhone Settings), Privacy policy and terms. |
| `ProfileLinkedAccountsView.swift` | `F0U-0`: UAE PASS, one switch per `store.state.linkedAccounts`, Keep activity for (menu picker), Download a copy (alert → toast), Delete history (dialog → clears `state.history`). |
| `ProfileDeleteAccountView.swift` | `WEH-0` → `WJ3-0` → `WLG-0`: what we delete / what stays / requests still in progress → native alert → Face ID (LocalAuthentication, falls back when not enrolled) → `signOut()` + toast on Welcome. |
| `VehicleView.swift` | `ETT-0`: hero plate, At a glance (Registration → service start, Fines → payment sheet, Salik → top-up sheet, Insurance → ComingSoon), Things you can do (catalogue records), On your Home toggles, Recent activity, Details. |
| `VehiclesView.swift`, `DocumentsView.swift`, `DocumentView.swift`, `FamilyView.swift` | Lists and detail pages; Family has the "Renew your family's visas" bundle callout (`NavigationLink(value: .bundle("renew-family-visas"))`). |
| `HappinessMeterSheet.swift` | `F49-0`/`F97-0`: question (DubaiNow or the context service), 80 pt faces, sheet grows to large after a choice, reasons per face, optional comment, Send → in-sheet "Thanks" → closes after 1.2 s + toast. Unhappy → "Sorry about that" with Get help / Report a problem (ComingSoon) / Not now. Closing after a face still thanks (the rating is never lost). Reduce Motion opens at large. |
| `NotificationsView.swift` | Today / Earlier, unread dots, Mark all read, swipe to read/unread, empty state "You're all caught up". Close button only when it's the bell sheet. |
| `ProfileShared.swift` | `Profile*` helpers: groups/rows, document card, flow layout, Face ID helper, store extension (read-only helpers + `profileMarkUnread`). |

All pushed Profile screens use `.dnPushedToolbar()` in tabs and skip it inside the profile sheet (`\.profileIsInSheet`).

## Launch arguments
- `-persona sara -sheet profile` · `-persona guest -sheet profile`
- `-persona sara -sheet profile -profilePage linkedAccounts|notificationSettings|accessibility|privacy|deleteAccount|home|paymentMethods`
- `-persona sara -sheet profile -profileRoute vehicle:sara-patrol` (any `Route` launch string, comma-separated)
- `-persona sara -route vehicle:sara-patrol` (also `vehicles`, `documents`, `document:sara-visa-doc`, `family`, `notifications`)
- `-persona sara -sheet happiness` (+ `-happinessFace happy|neutral|unhappy`, + `-happinessSent`)
- `-persona sara -sheet notifications` (+ `-profileOpenNotification sara-n-visa` taps a row on launch)
- Delete flow: `-sheet profile -profilePage deleteAccount -profileDeleteAlert` (step 2) or `-profileAutoDelete` (steps 2→3)
- `-profileScroll center|bottom` starts Profile scroll views there (screenshots).

## Requests for shared code (worked around inside this folder)
1. **Logo inside sheets.** `dnPushedToolbar()` adds the assistant logo even inside sheets; other features' screens pushed in the profile or notifications sheet (e.g. the request tracker from a notification) show it, and tapping it opens the assistant *behind* the sheet. Suggest a shared `isInSheet` environment flag (promote `profileIsInSheet`) set in `SheetDestination`, honoured by `PushedToolbar`.
2. **Toasts behind sheets.** The root `ToastHost` is covered by sheets. I added `.toastHost(bottomInset: 24)` on the profile sheet. A shared answer (host in `SheetDestination`) would help Payment/Receipt too.
3. **Widget kinds for a vehicle.** "Registration countdown" and "Fines" toggles on the vehicle page are local state (no `WidgetKind`); only "Salik balance" adds/removes a real widget. Suggest `WidgetKind.vehicleRegistration(String)` and `.fines`.
4. **Model fields** now derived in Profile: `Vehicle.trafficFileNumber/chassis/registeredOn/inspectionCentre`; `DocumentItem.issuer` + related service/request id; `LinkedAccount.fullName/monogram` (RTA → "Roads and Transport Authority").
5. **Store actions**: `markUnread(_:)`, `setLinked(_:_:)`, `deleteHistory()`, and an app-level Happiness Meter record (`rateApp(face:reasons:comment:context:)`) — today only receipts can be rated.
6. **Per-persona settings.** Notification settings and "Keep activity for" use `@AppStorage` (shared by all personas, survive Reset demo). Move to `PersonaState` if they should reset.
7. **Face ID policy.** I use `.deviceOwnerAuthenticationWithBiometrics` with no fallback button: with `.deviceOwnerAuthentication` the Simulator asked for the iPhone passcode, which would stall a demo. When Face ID isn't enrolled the flow continues. Payment confirmations may want the same.
8. **Notification rows** are Buttons + `navigationDestination(item:)` (mark read and push in one tap, on whichever stack shows the list) instead of `NavigationLink(value:)` + `simultaneousGesture`, which is unreliable in `List`.
9. **Copy.** The Happiness Meter toast uses the brief's "Thanks — your feedback goes to the Happiness Meter" (Paper `F97-0` says "Thanks, your rating was sent").
