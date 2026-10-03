# Home (Android) — build notes

Port of `ios/DubaiNowDemo/Features/Home/*` (the Canvas). Paper C1 (`p-W-0`): 9U5 default · KH6/KRB folded ·
JM5 Pay all + suggestion · JX1 journey · A3J all caught up · A7N pull past the end · AF5/I9L Edit Home ·
C56/CBA/CHX Favorites + edit sheet + 8 of 8 · WOV guest.

## Files
- `HomeScreen.kt` — `HomeScreen()`: wash (Crossfade by `store.timeOfDay`, starts behind the header and scrolls),
  fixed header (`MainHeader(showsBell = true)`, or Add · Edit Home · Done) with a scroll-edge fade in the wash
  colour, anchors, widgets, footer; pull past the end (nestedScroll on the bottom overscroll, 100 dp after 0.5
  resistance, soft haptic when armed, gallery on release, content rubber-bands up); Edit Home (BackHandler exits;
  starts when `router.homeEditRequested` turns true, then clears it); scroll position kept in `HomeMemory`.
- `HomeNeedsYou.kt` — heading + count + fold chevron (28 dp in a 48 dp target), summary (amber clock line when
  something is due today/overdue), See all → Activity, Pay all pill → `SheetRoute.Payment(store.payAllRequest())`,
  LazyRow of 272 dp brief cards (snap to start, equal heights via a measuring SubcomposeLayout, staggered entrance
  once per process, jumps back to the start when a new item lands in front), folded line, All caught up, guest
  "See what needs you" card (UAE PASS → `store.signOut()`), `HomeLinkButton`.
- `HomeFavorites.kt` — 4-column grid ((w − 40 − 36) / 4), badges (parking "41m" green with dot, Vehicles red
  count of fines + renewals), Add tile under 4, long-press `DropdownMenu` (Open · Edit Favorites · Remove + Undo
  toast), iOS tap routing (`HomeFavoriteRouting`), 2-column label-beside layout at font scale ≥ 1.5,
  three-disc mark for a "pay-all" favorite (Paper).
- `HomeWidgets.kt` — packing (two smalls per row; small (w − 40 − 12)/2 square, medium full × small, large full
  square) rendering `WidgetView(widget, Modifier.size(...))`; suggestion (JM5: medium preview + "+" badge,
  "Suggested · …", Not now, adds at the top at Medium, Undo); empty "Make this space yours"; Add widget slot; pull
  hint and indicator; Edit Home pieces (pinned group, − badges, ±1.5° wobble off when animations are removed,
  focus ring, tonal bottom bar Earlier · Later · size — tap = next size ("Wide"), long-press = DropdownMenu).
  `LocalWidgetPreview provides true` around edit-mode widgets and the suggestion.
- `HomeSupport.kt` — `HomeGrid` (worked out in whole pixels: two smalls + gutter = one wide widget exactly, and the
  favorite gaps take the odd pixels so the fourth column ends on the right inset), packing, `HomeWidgetSizes` (mirror of iOS `WidgetKind.widgetSizes`), store
  extensions (public API only), copy helpers, debug extras, small pieces (dashed border, focus ring, press scale,
  wobble, chips, Pay all glyph).
- `EditFavoritesSheet.kt` — `EditFavoritesSheet()`: × (reverts this session) · Favorites · ✓; "On your Home"
  grid with − badges and dashed slots to 8, "4 of 8"; reorder = tap a favorite to select it, then Move earlier /
  Move later (plus TalkBack custom actions); 8 of 8 notice (warning haptic, scrolls to it, nudges, announces);
  search field; Suggested for you · Your things · main topic · Results / no results; Browse all 219 services.
  Large font: list rows with up/down/remove buttons.

## Debug extras (`-e key value`)
`homeFolded true` · `homeEmptyNeedsYou true` · `homeEditing true` · `homeSelectWidget <i>` · `homeSizeMenu true` ·
`homeScrollTo widgets|bottom` · `homePull <dp>` (pull indicator; ≥ 100 shows "Release") · `homeFavoriteMenu <i>` ·
`homeJourneyCard true` (Build a house step first in Needs you, JX1) · `homeFontScale <scale>` (lays Home and the edit
sheet out as if the system font size were e.g. 2 = 200%, without touching the shared emulator setting) · with
`sheet editFavorites`: `homeLimit true` (fill to 8 of 8) · `homeFavSelect <i>` · `homeFavQuery <text>`.

## Decisions
- Android reorders by select-and-move (Edit Home bar, and Move buttons in the sheet) instead of drag; header copy
  "Select one to move it" / "Tap a favorite to move it. This is the order on Home."
- Edit Home's bottom bar floats just above the shell's tab bar (the shell can't hide it yet — request 1).
- Swiping the edit sheet away (or back) keeps the changes, like ✓; only × reverts.

## Requests for the orchestrator (shared code)
1. **Hide the tab bar in Edit Home (high).** iOS hides it. e.g. `Router.hidesTabBar` (Home sets it while editing)
   checked by `MainScaffold`'s BottomBar visibility; then in `HomeScreen` change the edit bar's bottom padding from
   `tabBarSpace + 12.dp` to `navigation bar + 12.dp` and the edit spacer to match.
2. `app/Chrome.kt` MainHeader: for guests the avatar button is an empty white circle (the `content` lambda is
   always passed, so `PersonOutline` never draws). Pass `content = null` when `persona == null`.
3. Shell recreates tab roots after a push (AnimatedContent keyed by depth), so screen state is lost; Home keeps its
   scroll and rail position in `HomeMemory`. A `SaveableStateHolder` in `MainScaffold` would fix it for everyone.
4. Model: add `WidgetKind.widgetSizes` (Widgets feature); then point `HomeWidgetSizes.allowed` at it.
5. Promote to `DemoStore`: `homeRestoreFavorites/NeedsYou` (Undo / ×), `homeMoveWidget/Favorite(id, step)`,
   `homeAddSuggestedWidget`. `later()` should hide the card until tomorrow (KRB rule 5).
6. Icons: `road.lanes` → `Toll` reads as "(O" on the Salik tile; `your-documents` has no record override in
   `ServiceVisuals` (falls back to the grid glyph when added from the sheet).
7. Optional: `SheetHost` could block swipe-to-dismiss for EditFavorites while there are unsaved changes (iOS).
8. `app/Chrome.kt` MainHeader: the logo sits about 28 dp left of centre on Home (one button on the left, bell + avatar on
   the right, equal-weight spacers). iOS and Paper 9U5 centre it; e.g. give both side groups the same fixed width.
9. `app/Chrome.kt` MainHeader: the bell's count badge is a fixed 18 dp circle with 11 sp text, so the number is clipped
   at large font sizes (seen at 200%). Use `heightIn`/`widthIn(min = 18.dp)` and a dp-capped text size.

## Not verified by touch
No input injection on the shared emulators, so taps, long-press menus, the pull gesture and the nested-scroll
maths were checked by code and via the debug extras above (menus, selected widget, pull indicator), not by dragging.
Large-font layouts were checked at 200% with `homeFontScale 2` (Home, folded, Edit Home, guest, edit sheet).

## Independent review (3 Oct)
Checked against iOS `Features/Home/*` and Paper 9U5 for Sara, guest, and (in a throwaway build with the ported
Khalid/Faisal/Wei personas and the real Widgets feature) Khalid, Faisal and Wei. Sara: Needs you 4, "2 due this week",
Pay all AED 1,012.60, folded "Next: electricity and water bill, tomorrow", all caught up, suggestion "2 visas expire in
73 days", packing and Edit Home all match. Khalid: journey card 4 of 7 · Design, no Pay all, Pay all favorite. Faisal:
Pay all AED 2,035.20, 5 favorites (second row), Vehicles badge 1. Wei: 1 item, no Pay all. No crashes. Fixed:
- Favorites' fourth column ended ~3.4 dp short of the right inset at 411 dp (tile floored to whole dp), and
  wide widgets were 1.4 dp wider than two smalls + gutter. `HomeGrid` now works in pixels (see Files).
- The rail's staggered entrance replayed whenever a card scrolled out and back in (LazyRow drops item state); it now
  runs off one rail-level clock.
- Rail-to-Favorites gap was ~12 dp larger than iOS/Paper (14 dp shadow padding under the cards); Favorites now starts
  10 dp into it (the shadow still shows).
- Edit Home top bar: at large text "Edit Home" ran under Add and Done; the title now takes the space between them.
- Edit sheet at large text said "Tap a favorite to move it" over the arrow-button list; now "Move a favorite up or
  down. This is the order on Home."
- A removed widget stayed selected (Undo brought it back selected); selection now clears, as on iOS.
- Pay all wraps instead of clipping at the largest sizes; link buttons are at least 48 dp wide; "Add a favorite"
  (large text) lines up with the left inset; Edit Home bar segments report disabled state properly and offer the size
  list as a TalkBack long-press action ("Choose a size").

## QA pass (3 Oct)
- home-1 · Needs you cards at Large/Largest text (115/130%): "Upload now" read "Upload …" and the DEWA badge "DE…".
  The actions are now a small `Layout` (`HomeBriefActions`) that stacks the two buttons full width when their labels
  don't fit side by side at natural width (secondary ≥ 88 dp, 8 dp gap), and the rail makes that call once for every
  card (as iOS switches all cards at once), so the buttons still line up from card to card. The badge row
  (`HomeBriefTopRow`) puts the chip under the badge when the two don't fit (6 dp above the figure, 10 dp when stacked,
  as iOS); the poster (`HomeBriefPoster`) is measured per card and every poster takes the tallest one's height (120 dp
  at default text, so nothing changes there). Chip text ellipsises instead of clipping. Checked on the emulator at
  100% (unchanged), 115%, 130% and 200% for Sara, 130% for Khalid (journey card) and Faisal; buttons still work.
- home-2 · Edit Favorites search: focusing the field (and the results replacing the suggestions) scrolls the sheet so
  the field sits 12 dp under the title row, with the results above the keyboard. Checked with "visa" at 100%, and at
  200% with 8 of 8, clear (×) and no results.
