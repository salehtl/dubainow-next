# Home (the Canvas) — build notes

Paper page C1 (`p-W-0`): 9U5 Default · KH6/KRB Needs you folded · JM5 Pay all + suggested widget ·
JX1 journey on Home · A3J all caught up · A7N pull past the end · AF5/I9L Edit and arrange ·
C56/CBA/CHX Favorites spec, edit sheet, 8 of 8 · IUI bundle/journey rules · WOV Guest.

## Files
- `HomeView.swift` — the screen: wash (starts behind the header, scrolls with content), anchors,
  widgets, footer, pull past the end (72 pt, soft haptic, opens the gallery on release), Edit Home
  (toolbar swaps to Add · Edit Home · Done, tab bar hidden, bottom glass bar).
- `HomeNeedsYou.swift` — heading + count + fold chevron (44 pt target), summary (amber when
  something's due today), See all → Activity, Pay all pill (≥ 2 payments), rail of 272 pt brief cards
  (`viewAligned`, equal heights, staggered entrance), folded line, All caught up. Cards: payment /
  request step / renewal (shows the number plate) / journey step (hue gradient, route mark, ink bar).
- `HomeFavorites.swift` — 4-column grid that stretches ((w − 40 − 36) / 4), badges (parking "41m"
  green, Vehicles red count of fines + renewals), Add tile when fewer than 4, context menu (Open,
  Edit Favorites, Remove + Undo), tap routing, AX layout (2 columns, label beside tile).
- `HomeWidgets.swift` — canvas packing (two smalls side by side), suggestion (JM5: widget + "+" badge
  + "Suggested · …" + Not now), empty "Make this space yours", Add widget slot, pull hint/indicator,
  Edit Home pieces (pinned group, remove badges, wobble ±1.5°, focus ring, drag-reorder via
  onDrag/onDrop, Earlier · Later · Size bar).
- `HomeGuest.swift` — "See what needs you" + Sign in with UAE PASS (→ `store.signOut()` → Welcome).
- `HomeSupport.swift` — `HomeGrid`, packing, sizes per kind, `DemoStore` helpers (restore for Undo,
  move by one, add suggestion at top, vehicle attention count), copy helpers, launch args, wobble.
- `EditFavoritesSheet.swift` — medium/large sheet: On your Home grid (− badges, drag, dashed slots
  to 8, "4 of 8"), 8-of-8 notice (disabled + scrolls to it, warning haptic, VoiceOver announcement),
  search, Suggested for you, Your things, the main topic's services, Browse all 219 services.
  × reverts this session's changes, ✓ keeps them (swipe-to-dismiss is off while there are changes).
  Glass at medium; grey canvas at large.

## Launch arguments (all optional, for demos/screenshots)
- `-persona sara -hour 9` default · `-persona khalid|faisal|wei|guest`
- `-homeFolded` Needs you folded · `-homeEmptyNeedsYou` All caught up
- `-homeEditing` Edit Home · `-homeEditing -homeSelectWidget 1` without dragging (selected + bar)
- `-homeScrollTo favorites|widgets` · `-homeScrollBottom`
- `-homeStandInWidgets` simple stand-ins while `WidgetView` is a placeholder (off = real WidgetView)
- `-sheet editFavorites` (+ `-homeSheetLarge` to open full height, + `-homeLimit` to fill to 8 of 8)

## Decisions (where Paper and the task differed)
- Edit Home follows Paper AF5/I9L: Needs you and Favorites become a "Pinned · always first" group;
  only widgets wobble/move. Favorites are arranged in the edit sheet.
- The suggested widget is added at **medium** (what the preview shows, JM5) and takes the
  suggestion's place at the top, not `.small` at the end.
- Widgets keep their own touch-and-hold menu (Widgets feature). Its "Edit Home" posts
  `DNWidgetEditHomeRequested`; HomeView listens for it (no Home-level menu, so no double menus).
- The size control in the Edit Home bar says "Wide" (Paper I9L); the widget menu says "Medium".

## Requests for the orchestrator (shared code)
1. **Search field on every tab (high):** `.searchable` on the TabView in `App/RootView.swift` puts a
   search bar under the header on Home, Services and Activity (iOS 27 runtime). Move `.searchable`
   onto the Search tab's NavigationStack/SearchView. Home spacing was checked without it.
2. After merging Widgets: in `HomeWidgetContent` (HomeSupport.swift) add
   `.environment(\.widgetIsPreview, isPreview)` for the edit-mode and suggestion previews (they're
   already inert via `allowsHitTesting(false)`), and replace `HomeWidgetSizes.allowed(for:)` with
   `kind.widgetSizes`. Optionally set `\.widgetEditHomeAction` instead of the notification.
3. Promote to `DemoStore`: `restoreFavorites/Widgets/NeedsYou` (Undo), `moveWidget(id, by:)`,
   `addWidget(_:size:at:)`.
4. `later(_:)` should hide the card until tomorrow (KRB rule 5); it only moves it to the back now.
5. Folded "New: …" dot (KRB) needs a seen-marker in the store (e.g. `needsYouSeenIDs`); not built.
6. Data: Khalid's "Pay all" favorite uses `creditcard.fill`; Paper uses the three-disc mark.

## Not verified by touch
No touch injection was available on my simulator, so drag-to-reorder, context menus and the pull
gesture were checked by code and by on-screen debug readouts only (scroll-geometry maths confirmed
0 at rest top and bottom), not by actually dragging.
