⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Add Destination critique

Evidence: `add-destination-light.png` (captured before the city chips loaded, so the chip row is empty), `add-destination-font.png` (1.3x, chips loaded), `add-destination-dark.png` is BLANK (launcher icon on dark, launch timing) so dark was judged from source only (all colours are theme attrs; the shell is now the fixed navy include). `add-destination-checks.txt`: NO_LABEL ImageButton at bounds [0,77][154,231] = the toolbar back arrow.

## Design-specificity verdict
Before the fix this was the one screen outside the boarding-pass world: a white `MaterialToolbar` on surface, no navy carrier band, no edge-to-edge handling (the status bar was navy only by theme luck and the toolbar did not take the inset), and a back arrow with no label. It was a generic Material form. After the fix it carries the same navy shell as New Trip and Itinerary. The form body stays ordinary outlined Material fields, which is right for an Operate-mode input screen; the world contributes the shell and monospace for numeric data only.

## Nielsen heuristics (0-4)
1. Visibility of system status: 2. Search failure shows the same "No places found" text as a true empty result; save success snackbar is shown on an activity that finishes immediately (never seen).
2. Match with real world: 3. "Day", "Mark as booked (fixed time)" are clear.
3. User control and freedom: 3. Back works; duplicate-stop dialog offers cancel; city chip could be deselected, leaving search city-less.
4. Consistency and standards: 2. Toolbar differed from every other ScreenActivity screen.
5. Error prevention: 2. Day "0" or "00" was accepted.
6. Recognition over recall: 2. Chips had no caption, so "Manila / Tokyo" read as unexplained tags.
7. Flexibility and efficiency: 3. Search, map pick and manual entry all coexist.
8. Aesthetic and minimal design: 3. Clean, but long single column.
9. Error recovery: 2. Errors appear inline but nothing moved focus to the offending field, which may be off-screen at 1.3x.
10. Help and documentation: 3. Name helper text and empty-state copy are good.

## Cognitive load
Three entry routes (search, map, manual) are stacked in one column with an "or add manually" divider, which is a sound progressive structure. The unlabeled chip row at the top adds a decision with no stated meaning. Choosing a place fills the name field far below the search box, with no scroll or focus move; at 1.3x the filled name can be off-screen. Fields: 7 maximum (time is conditional), within working-memory limits.

## Strengths
1. Search-first with graceful manual fallback and explicit helper text.
2. Booked switch reveals the time field only when needed (progressive disclosure).
3. Duplicate-stop warning as a decision dialog rather than a hard block.

## Issues
1. P1 · Back button has no content description (checks.txt NO_LABEL; `activity_add_destination.xml` toolbar). Fix: use shared `view_toolbar` include (carries `cd_back`).
2. P1 · Screen is outside the shell: white toolbar, no top-inset handling, no IME/nav-bar inset (font shot: Save button hugs the gesture bar; on API 35+ content would sit under the status bar). Fix: extend `ScreenActivity`, include `view_toolbar`, call `setScreen`.
3. P1 · City chips have no caption (font shot). Fix: "Search in" label above the chip group; `selectionRequired` so a city is always chosen.
4. P1 · Time field's clock end icon has no content description (`activity_add_destination.xml` layout_time). Fix: `endIconContentDescription`.
5. P1 · Day accepts 0 (`AddDestinationActivity.attemptSave`). Fix: `validateDay()` rejects < 1 with a new string.
6. P1 · Search failure is displayed as "No places found" (`AddDestinationActivity` search observer). Fix: show `search_error` text on failure.
7. P1 · Failed validation does not move focus to the first bad field (hidden off-screen at 1.3x). Fix: request focus on the first invalid input.
8. P2 · Numeric inputs (day, duration, time) are data and should be monospace per DESIGN.md. Fix: `fontFamily="monospace"` on those three inputs (one line each).
9. P2 · Save success snackbar is created then `finish()` runs, so it never shows. Not fixed: needs a result hand-off to Itinerary (other screen's file). Flagged.
10. P3 · "Pick on map" text button text is inset ~12dp from the field edge, a slight misalignment with the fields above. Not fixed.

## Fix log
- 1, 2: `AddDestinationActivity` now extends `ScreenActivity` (`setScreen`), layout includes `view_toolbar` (navy shell, back label, insets, light status icons). Removed local tripId/toolbar wiring.
- 3: `activity_add_destination.xml`: "Search in" caption + `selectionRequired`; string `label_search_city`.
- 4: `endIconContentDescription` = `select_time`.
- 5: `validateDay()` + string `error_day_invalid`.
- 6: empty-state TextView text switches between `search_empty_state` and `search_error`.
- 7: `attemptSave` focuses the first invalid field.
- 8: monospace on day, duration, time inputs.
- 9, 10: not applied (see Flagged in queue).
- Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` exit 0.
