⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Critique: Suggestions (`SuggestionsActivity`, `activity_suggestions.xml`)

Evidence: `suggestions-light/dark/font.png` (Pixel 3a emulator, 1080x2220). All three show the same state: Food tab, "₱0 left in your Food budget", and the empty message. The light and font shots predate the shared navy status-bar fix (status icons look dark on navy); judged from source, `ScreenActivity.applyShellSystemBars()` now covers it. Native checks: no issues.

## Design-specificity verdict

The screen is a generic Material list wearing the shared navy toolbar and the shared outlined row card. It does not need its own ticket motif (DESIGN.md reserves that for Home and Itinerary), so staying plain is correct. What it lacked was the product's own point: it is the "budget-aware" screen, yet on the seed trip it opened to an empty list with a "₱0 left" line and no explanation, no sense of which day it feeds, and no way to see what was over budget. The fix keeps the world as is and makes the budget logic legible.

## Nielsen heuristics (0-4)

| # | Heuristic | Score | Note |
|---|-----------|-------|------|
| 1 | Visibility of system status | 2 | Target day never shown; no pending or "added" state on rows; "₱0 left" does not say the budget is overspent |
| 2 | Match with real world | 3 | Plain language, local and home prices both shown |
| 3 | User control and freedom | 2 | Filter chip is reversible, but the empty state makes the user find it themselves |
| 4 | Consistency and standards | 3 | Shared rows, Material tabs and filter chip; hint sentence set wholly in monospace broke the Mono-Means-Data rule |
| 5 | Error prevention | 1 | Add could be tapped repeatedly, creating duplicate stops; rotation reset any guard |
| 6 | Recognition over recall | 2 | Over-budget items are invisible when the filter is off |
| 7 | Flexibility and efficiency | 3 | One tap adds; tabs and chip are quick |
| 8 | Aesthetic and minimal design | 3 | Clean, low density |
| 9 | Error recovery | 1 | Failure reused "Could not save destination", the wrong copy for this action |
| 10 | Help and documentation | n/a | |

Total 20/36 across 9 scored heuristics.

## Cognitive load

Low item count (6 per tab) and one filter, so intrinsic load is small. Extraneous load came from the empty default (the user must infer the budget is spent, then find the chip) and from three pieces of hidden context: the target day, which items are over budget, and which items are already added. Working memory was needed to avoid double-adding.

## Strengths

1. Budget-aware filter chip with a remaining-budget line is the right core idea and sits where the eye lands after the tabs.
2. Reuses the shared outlined row card, so it matches Discover, Budget and Optimize.
3. Confirmation snackbar after adding (stays on screen, names the stop and day).

## Issues

1. **P1** Default state is a dead end. On the seed trip the Food budget is overspent, so the list is empty with only text pointing at a chip. Evidence: all three screenshots; `SuggestionsActivity.java` render. Fix: add a "Show all places" button in the empty state that clears the filter; shorten the copy.
2. **P1** "₱0 left in your Food budget" hides that the budget is overspent (`Math.max(0, remaining)`). Fix: when remaining is 0 or less, say "Your Food budget is used up".
3. **P1** Add button has no spoken label naming what it adds, and no in-flight or added state; repeated taps created duplicate stops. Evidence: `SuggestionsActivity.toRow`. Fix: `withAccessibility("Add X to Day N")`, an "Adding…" label while saving (taps ignored), then "Added" with the selected outline and no action.
4. **P1** Target day is invisible; from the itinerary menu it silently defaults to Day 1. Fix: "Adding to Day N" line above the budget hint.
5. **P1** With the filter off, over-budget picks look identical to affordable ones. Fix: subtitle ends with "Over budget" (text, not colour alone).
6. **P1** Failure toast said "Could not save destination". Fix: new copy "Could not add X. Try again."
7. **P1** Rotation lost the selected tab and the added state. Fix: save and restore the tab index and added names.
8. **P2** The budget hint was entirely monospace and 12sp. Fix: Roboto body-medium; only the amount is monospace via a span.
9. **P2** Itinerary overflow menu opens this screen without a day, so the target is always Day 1 (`MainActivity.open`). Not fixed: it changes another screen's launch (flagged).
10. **P3** Picks are not checked against stops already in the itinerary, and the "Added" state is lost if the activity is recreated while a save is in flight.
11. **P3** The "Fits my budget" chip does nothing when the category has no limit set.

## Fix log

- Issue 1: `activity_suggestions.xml` (empty group with tonal "Show all places" button), `SuggestionsActivity.java` (button unchecks the chip), `strings_prototype.xml` (`suggestions_show_all`, shorter `suggestions_empty`).
- Issue 2: `SuggestionsActivity.budgetHint`, string `suggestions_budget_used_up`.
- Issue 3: `SuggestionsActivity.toRow` / `addToItinerary` using `UiRow.withAccessibility` and `withSelected` (no change to shared classes); strings `suggestion_add_cd`; reuses `action_added`, `discover_adding`.
- Issue 4: `activity_suggestions.xml` (`text_target_day`), string `suggestions_target_day`.
- Issue 5: string `suggestion_subtitle_over`, used in `toRow` when the price does not fit.
- Issue 6: string `suggestion_add_failed`.
- Issue 7: `onSaveInstanceState` / `restoreState` in `SuggestionsActivity.java`.
- Issue 8: hint TextView drops monospace and moves to body-medium; `TypefaceSpan("monospace")` on the amount only.
- Not fixed: 9 (flagged), 10, 11 (P3).
- Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` completed with no errors and 0 lint errors.
