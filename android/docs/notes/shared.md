# Shared (app/, design/, data/) — Android notes

The shell (`app/RootScreen.kt`, `app/Router.kt`, `app/RouteDestinations.kt`, `app/Chrome.kt`, `app/DemoMenu.kt`),
the design system (`design/`) and the store (`data/`, personas excluded).

## QA pass (3 Oct)
- **Back inside the Profile sheet** (shared-1): on API 33+ the M3 sheet's own window back callback beat every
  `BackHandler` in the sheet, so Back closed the whole sheet from any depth. `SheetHost` now turns
  `shouldDismissOnBackPress` off for sheets with pages (`SheetRoute.hasPages`: Profile) and adds a base `BackHandler`
  that hides the sheet (slide down) and dismisses it. ProfileSheet's own handler registers later, so it pops pages
  first. Other sheets keep the default (predictive back). Checked with keyevent 4 and the edge swipe, signed in and
  guest, `profileRoute document:…`, and Profile → Demo controls → Back.
- **Pop animation and saved state** (shared-2, shared-3): the tab content's `AnimatedContent` targeted `(tab, depth)`
  and re-read the stack, so after a pop the leaving pane redrew as a second tab root under a new state key (two roots
  sliding past each other; a ghost Search root consumed `SearchSession`). The target is now a `TabScreen(tab, depth,
  route)` with one stable key for the pane and its `SaveableStateProvider`. A popped screen's state is dropped when
  its pane leaves (on dispose), or at once if it isn't on screen (reset, cleared stacks). A to Z keeps its filter and
  query, Entities its query, the Services root and topic pages their scroll on the first trip, and Search its query,
  "See all" and scroll after Back.
- **Toasts on Welcome** (shared-4): `ToastHost(topInset =)` puts the toast under Welcome's story header (status bar +
  64 dp) while signed out, so it never covers the UAE PASS pill at any text size. iOS has the same overlap
  (`RootView` uses `.toastHost()` with the 96 pt default over Welcome); not changed here.
- **Pushed header logo** (shared-5): `PushedHeader` has the same 104 dp sides as `MainHeader`, so the logo (and the
  assistant face over it) is centred with 0, 1 or 2 actions.
- **Card text** (shared-6): ellipsis on the journey card's stage and scale lines, the bundle card description and the
  catalogue row's supporting lines.
- **Demo controls** (shared-7): title row with a Close button; the current resident has a check icon
  ("Current resident") and its row is marked selected.
- **Sign-out flash** (shared-8): `DemoStore.signOut()` no longer clears `state` (Welcome never reads it; signIn and
  continueAsGuest replace it). While the app fades out, `MainScaffold` keeps the tab it was on and `MainHeader` keeps
  the avatar (router.reset() and the null persona would otherwise switch them for the last 0.3 s).
- **Salik icon** (shared-9): `road.lanes` maps to a gantry `ImageVector` (`DNIcons.SalikGantry`), the same rounded
  rects as the Salik widget's glyph. Material has no gantry glyph.
- **Tab reselect** (shared-10): tapping the selected tab pops to its root; tapping it again at the root bumps
  `Router.reselectCount(tab)`. Tab roots opt in with `ScrollToTopOnReselect(tab, scrollState | lazyListState)`, which
  only reacts to taps made while the root is showing. Needs one call each in HomeScreen, ServicesScreen and
  ActivityScreen (requested from those owners; verified in a scratch build with the calls added).
