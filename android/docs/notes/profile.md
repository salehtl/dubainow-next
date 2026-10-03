# Profile feature (Android) — notes

Port of `ios/DubaiNowDemo/Features/Profile/*` to Compose/M3. Same copy, data and flows; Android chrome
(tonal circles, M3 sheets/dialogs/switches/menus/chips, BiometricPrompt).

## Files
| File | What |
|---|---|
| `ProfileSheet.kt` | `ProfileSheet()` (Paper `EOJ-0`, guest `X6U-0`). Own back stack (`mutableStateListOf<ProfileEntry>` + `AnimatedContent` + `BackHandler`; each stack entry sits in a `SaveableStateProvider`, so the profile keeps its scroll position when you come back from a page, like iOS). Title row "Your profile" + close; avatar, Verified chip, documents carousel (`LazyRow` + snap), Your things, Settings, Sign out (`AlertDialog` → `router.reset()` + `store.signOut()` + toast), Delete your account, Demo controls row (→ `SheetRoute.DemoMenu`), footer (long press / a11y action → demo controls). Guest: sign in = persona `AlertDialog`, Create an account → coming soon. Happiness Meter opens as a nested sheet on top (profile keeps its place). Own `ToastHost` (the shell skips it for Profile). Sheet height stops below the status bar. |
| `ProfileShared.kt` | Navigation (`ProfilePage`, `ProfileEntry`, `ProfileNav` + `LocalProfileNav`), `ProfileScreen` frame (PushedHeader in a tab / back row in the sheet), rows/groups/switch/facts card/pills/alerts, document card, nested coming-soon sheet, store extensions, `ProfilePrefs` (SharedPreferences, like iOS `@AppStorage`), `profileConfirmIdentity` (BiometricPrompt or 0.9 s fallback). |
| `ProfileSettingsPages.kt` | Your home, Payment methods, Notifications (M3 `Switch`es, quiet hours with M3 `TimePicker` dialog, Open Android settings), Accessibility and display (live values, re-read on resume; opens `ACTION_ACCESSIBILITY_SETTINGS`), Privacy policy and terms. |
| `ProfileAccountPages.kt` | Linked accounts and privacy (`F0U-0`: UAE PASS, switch per entity, Keep activity for `DropdownMenu`, Download a copy, Delete history) and Delete your account (`WEH-0`→`WJ3-0`→`WLG-0`). |
| `VehicleScreen.kt` | `ETT-0`: hero plate, At a glance 2×2 (1 column at large text), Things you can do, On your Home switches, Recent activity, Details, ⋮ menu (Share / Report). |
| `VehiclesScreen.kt`, `DocumentsScreen.kt`, `DocumentScreen.kt`, `FamilyScreen.kt` | Lists and detail pages (family visas bundle callout). |
| `HappinessMeterSheet.kt` | `F49-0`/`F97-0`: question (DubaiNow or context service), 80 dp faces, reasons as M3 `FilterChip`s, `OutlinedTextField`, Send → "Thanks" → closes after 1.2 s + toast; Unhappy → help offer. Closing after a face still thanks. |
| `NotificationsScreen.kt` | Today / Earlier (`LazyColumn`), unread dots, Mark all read (announced as disabled when nothing is unread), tap = mark read + open link, swipe right (`SwipeToDismissBox`, springs back) / long press menu / TalkBack action = mark read/unread, empty and "all caught up" states. |

## Navigation rules
- Inside the profile sheet, **things** (`Route.Vehicle/Vehicles/Documents/Document/Family`) and the settings pages push on the sheet's own stack (back row + system back pops).
- Other features' routes (service start pages, bundles, request tracker, History, subtopic) **close the sheet and push on the current tab** (Home if on Search) — their screens call `router.push` themselves, which would land behind a sheet.
- Bell sheet (`NotificationsScreen(inSheet = true)`): tapping a linked notification closes the sheet and pushes the link on the tab.
- Pay fines / Top up Salik use `router.present(SheetRoute.Payment/TopUpSalik)` (replaces the profile sheet, as on iOS).

## Debug extras (with `agent-shot.sh … key value`)
- `sheet profile` + `profilePage linkedAccounts|notificationSettings|accessibility|privacy|deleteAccount|home|paymentMethods`
- `sheet profile` + `profileRoute vehicle:sara-patrol` (comma-separated thing routes, e.g. `vehicles,vehicle:sara-corolla`)
- `profileDialog signOut|persona` (persona = guest sign-in picker), `profileComingSoon language|help`, `profileHappiness true`
- `profileDelete alert|auto|confirming` (on the delete page: shows the dialog / runs the whole flow / shows the busy state)
- `profileScroll bottom|center|<px>` starts Profile scroll views there
- `profileTestBack <n>` presses back n times through the sheet's back dispatcher (verifies the in-sheet stack)
- `sheet happiness` + `happinessFace happy|neutral|unhappy` (+ `happinessSent true`)
- `sheet notifications` + `profileOpenNotification sara-n-visa` taps a row on launch; `profileSwipeNotification sara-n-visa` swipes it (toggles read)
- `profilePage notificationSettings` + `profileDialog quietFrom` opens the quiet-hours time picker
- Pushed in a tab: `route vehicle:sara-patrol|vehicles|documents|document:sara-visa-doc|family|notifications`
- Profile launch hooks apply once per process (the first time the sheet opens).

## Shared requests (worked around inside this folder)
1. **Type.kt crash risk:** `trackingEm()` returns `0.sp` for zero tracking but em otherwise. M3 `OutlinedTextField` lerps the label between `bodyLarge` (em) and `bodySmall` (sp) and throws `IllegalArgumentException: Cannot perform operation for Em and Sp`. Fix: return `0.em`. I override the two styles locally around my text field; **every other M3 text field (search!) will crash until fixed.**
2. **SheetHost:** full-height sheets slide under the status bar. I cap the profile sheet below it (`profileLargeSheetHeight`). Suggest a shared top gap / `sheetMaxHeight` for all large sheets.
3. **DNIcons:** missing `globe`, `questionmark.circle.fill`, `qrcode`, `calendar` (Profile maps them privately in `profileSymbolIcon`; the shared `ComingSoonContent` still falls back to the grid icon for them).
4. **DemoDate.short** uses `Locale.UK` → "15 Sept" on current Android (Paper/iOS say "15 Sep"). Suggest a fixed short-month table.
5. **Store actions** derived here via the public `state`: `profileMarkUnread` (= `markRead(id, false)`), `profileSetLinked`, `profileDeleteHistory`; no app-level Happiness record (`rateApp`) yet — only receipts can be rated.
6. **Per-persona settings:** notification settings and "Keep activity for" are phone-wide (SharedPreferences), like iOS `@AppStorage`; they don't reset with Reset demo.
7. **Widget kinds for a vehicle:** "Registration countdown" and "Fines" switches are local state; only "Salik balance" adds/removes a real widget (with Undo).
8. **Biometrics:** `BIOMETRIC_WEAK`, negative button "Cancel"; not enrolled → 0.9 s "Confirming…" then carries on. Payment confirmations may want the same helper (`profileConfirmIdentity`).
9. `LaunchOptions.extras` uses deprecated `Bundle.get` (warning in app/, not ours).
10. **List rows vs Paper `LOA-0` (List rows · Android, Adapted):** Paper says Android navigation rows have *no chevron* and a value row shows its value *under the title*. Profile now puts values under the title (Language · English, Notifications · Important only, quiet hours From/To), which also keeps the titles readable at 200 % text. Chevrons stay for now because the shared `DNListRow` and the other features draw them; if the orchestrator adopts Paper's rule, drop the chevron in `ProfileRow` (one line) along with `DNListRow`.
11. **Icons:** `Icons.Rounded` has no outlined shield/document, so "Linked accounts and privacy" (`PrivacyTip`) and "Privacy policy and terms" (`Article`) are filled while the other settings icons are outlines (Paper uses outlines). Fine to keep unless the icon set gains outlined Rounded glyphs.

## Not done / limits
- No "Share" sheet preview of vehicle/document beyond the Android chooser; "Show to verify", "Report wrong information", "Add a card", "Manage UAE PASS", "Get help" are coming-soon sheets (as on iOS).
- Not verified on device with biometrics enrolled (emulator has none → fallback path), nor at 200 % font size.

## Review pass (independent reviewer, 3 Oct)
Checked every screen on emulator-5556 (signed in, guest, Khalid's empty states, all seven pages, things in the sheet and in a tab, delete alert → confirming → Welcome + toast, sign out dialog, persona picker, Happiness Meter states, bell sheet, pushed notifications, in-sheet back), against iOS and Paper (`EOJ-0`, M5 overlays, `LOA-0` list rows). No crashes; zero errors/warnings; public signatures unchanged; every non-private top-level name is `Profile`/`profile`-prefixed. Fixed:
- **Profile lost its scroll position** after visiting a page in the sheet (each `AnimatedContent` entry rebuilt its `ScrollState`) → per-entry `SaveableStateProvider`, state dropped when an entry is popped.
- **Content under the floating bottom bar** on pushed screens/notifications: the bottom room ignored the navigation bar (fine with gesture nav, ~20 dp hidden with three-button nav) → `profileBottomBarClearance()` = navigation inset + 96 dp.
- **Large text:** documents-shelf cards had a fixed height (text clipped at 200 %) → minimum height, cards grow; the vehicle hero plate's lettering scaled inside a fixed 60 dp plate (clipped) → fixed-size lettering (the plate is a picture with a spoken label); settings value rows squeezed their titles → value under the title (Paper `LOA-0`).
- **Notifications:** added swipe right to mark read/unread (iOS leading swipe); "Mark all read" now disabled for TalkBack and touch when nothing is unread (was a silent no-op button).
- **TalkBack:** Payment methods' first card announced "Used first" twice → tick is decorative.

## QA pass (3 Oct)
Fixed the four QA findings owned by Profile (checked on emulator-5558):
- **profile-1 · Guest persona picker:** the "Sign in with UAE PASS" dialog cut every tagline to two lines. It now shows the avatar and full name only, as the iOS confirmation dialog does.
- **profile-2 · Nested sheets under the status bar:** with the keyboard up, the Happiness Meter opened from Profile grew to the top of the window, so the drag handle sat on the clock. `ProfileHappinessSheet` and `ProfileComingSoonSheet` now use SheetHost's inset (`windowInsetsPadding(WindowInsets.statusBars).padding(top = 8.dp)`).
- **profile-3 · Chevrons:** removed from notification rows with a link and from the "Your vehicles" cards (Paper `LOA-0`). The whole row or card is still the button. Shared-requests item 10 is out of date: no Profile list row draws a chevron now.
- **profile-4 · Documents shelf snapping:** cards snapped to the centre, which left half cards on both edges. They now rest with their leading edge on the page inset, like iOS `.viewAligned`. The last card snaps to the trailing inset (`ProfileShelfSnapPosition`), because a plain `SnapPosition.Start` let a fling stop between two cards at the end of the shelf.
