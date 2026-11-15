⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Discover critique

Evidence: `discover-light/dark/font.png` all rendered (light predates the shared status-bar fix: its status icons are dark on navy; dark and font show light icons), `discover-checks.txt`, `discover.xml`, source.

## Design-specificity verdict
This is the generic row-list screen in the TripFit shell: navy toolbar, 16dp outlined cards, amber tonal action, Roboto prose. That is correct for a provisional prototype screen and it stays within the Named Rules (no monospace leakage, shell fixed in dark). It carries no ticket motif of its own, which is right (one hero per world). The weak spot was not look but state: a selected spot looked identical to an unselected one except for button text, and the swap gesture was invisible to assistive tech.

## About the 4dp CardView check
`discover.xml` shows `CardView [44,1922][1036,1934]` (12px = ~4dp) sitting at the bottom edge of the scroll viewport (ends at 1934). It is the third hidden-gem row, clipped by the scroll view, not a divider or a broken card. So the "TOUCH_TARGET 361x4dp" is a scroll-clip artifact (the full card is 80dp and reachable by scrolling) and is harmless; the sliver doubles as a scroll cue. The real defect behind `NO_LABEL` is genuine though: each row card is clickable (swap) but exposed no action label, so TalkBack users never learn a tap swaps the spot.

## Nielsen heuristics (0-4)
1. Visibility of system status: 2 -> 3. Selection was text-only; no feedback while saving. Fixed (outline + state, "Adding...").
2. Match real world: 3. Famous vs hidden gems is clear.
3. User control and freedom: 3. Toggle add/remove, cancel dialogs. Swap silently deselects the replaced spot (acceptable).
4. Consistency and standards: 3. Shared row card.
5. Error prevention: 1 -> 3. Double tap on Add duplicated stops; partial failure + retry duplicated saved ones. Fixed.
6. Recognition over recall: 2. Swap is only hinted by a small caption far from the rows.
7. Flexibility and efficiency: 2. n/a-ish; no "select all".
8. Aesthetic and minimal design: 3. Clean; heavy repeated "Add" buttons are acceptable.
9. Error recovery: 2 -> 3. Failure now re-enables the button and keeps only unsaved picks.
10. Help and documentation: 2. One caption line.
Total ~24/40 -> ~28/40.

## Cognitive load
Low: two sections of three rows, one action per row, one primary button. Choices per screen: 6 adds + 6 swaps + change city. Swap-by-tapping-card vs Add button inside the same card is a mild double-affordance; the caption mitigates it.

## Strengths
- Disabled primary button with a count ("Add 3 to itinerary") makes consequences explicit.
- Dark mode reads well; tonal amber button inverts properly.
- At font 1.3 rows wrap cleanly and nothing clips; 48dp targets hold.

## Issues
1. P1 Double tap on "Add N to itinerary" queues the stops twice, and nothing shows work in progress. `DiscoverActivity.java` addSelected. Fix: `isSaving` guard, button disabled with "Adding...", rows/city picker inert while saving.
2. P1 After a partial save failure the selection stays whole, so retry duplicates stops that did save. Fix: remove each saved spot from the selection; retry re-sends only failures; one error toast.
3. P1 Confirmation says "Added 1 stops". `strings_prototype.xml` `discover_added`. Fix: plural, call site converted with `getQuantityString` (only call site).
4. P1 Selected state is conveyed only by the button label; card looks identical. Fix: selected card gets a 2dp primary outline and TalkBack state "Selected" (`RowAdapter`/`UiRow`, additive).
5. P1 Row-tap swap has no accessible action label (the NO_LABEL finding); action button says just "Add" for every row. Fix: card click action labelled "Swap for another spot"; button content description "Add/Remove <name> ...".
6. P1 Rotation loses city, selection and any swaps. Fix: `onSaveInstanceState` for city, selected names and shown order per section.
7. P2 Section headings and the city line not marked as headings for TalkBack. Fix: `setAccessibilityHeading` on three views (ids added to layout).
8. P2 Swap affordance is a small caption two sections above the rows; consider a trailing swap icon button. Flagged (changes row design).
9. P3 Trips whose city is not Tokyo/Manila silently fall back to Tokyo. Not changed.
10. P3 Picks are not checked against stops already in the itinerary (possible duplicates). Flagged.
11. P3 Light screenshot status icons dark on navy; shared fix already applied, re-verify in sweep.

## Fix log
- 1: `DiscoverActivity.java` isSaving guard + "Adding..." state; `strings_prototype.xml` `discover_adding`.
- 2: `DiscoverActivity.java` `onSaveResult` removes saved spots, one toast, re-enables.
- 3: `strings_prototype.xml` `discover_added` -> plurals; `DiscoverActivity.finishAdding` uses `getQuantityString`.
- 4: `ui/UiRow.java` `withSelected`; `ui/RowAdapter.java` `bindSelected` (stroke + state description).
- 5: `ui/UiRow.java` `withAccessibility`; `ui/RowAdapter.java` `bindAccessibility`; strings `discover_add_cd`, `discover_remove_cd`, `discover_swap_cd`.
- 6: `DiscoverActivity.java` save/restore state, `Section.shownNames/restore`.
- 7: `activity_discover.xml` ids `heading_famous`, `heading_hidden`; headings set in code.
- 8, 10: flagged in queue. 9: left.
- Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` passed (0 lint errors, tests green). Not verified on device.
