⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Journal (+ Add Entry dialog) critique

Evidence: `journal-light/dark/font.png` all rendered; `journal-checks.txt` = no native-check issues. The Add Entry dialog is NOT in any screenshot, so it was reviewed from source only. The light and font shots predate the shared status-bar fix (status icons look dark on navy); `setScreen` now handles that, so not counted as an issue.

## Design-specificity verdict
The screen is a competent but generic Material list: navy shell, one outlined 16dp card, a tonal extended FAB. It inherits the boarding-pass world correctly (fixed navy header, no monospace leaking into prose, flat cards) but adds nothing of its own, which is right for a Refinement pass on a secondary screen. The weaknesses are behavioral, not visual: the mocked add flow loses state on rotation, mislabels trip-wide entries as "Day 1", and its confirmation and empty state do not point at the next step.

## Nielsen heuristics (0-4)
1. Visibility of system status: 2 (save feedback was a Snackbar that sat on the FAB; photo state was text only and the new entry's position was not shown)
2. Match with real world: 2 ("General" entries rendered as "Day 1 · General")
3. User control and freedom: 2 (rotation destroyed the half-written entry; photo cannot be removed, only replaced)
4. Consistency and standards: 3 (matches Budget's dialog pattern once draft restore added)
5. Error prevention: 2 (no counter against the 500-char limit; double FAB tap could stack dialogs)
6. Recognition over recall: 3 ("Stop" label hid that General is an option)
7. Flexibility and efficiency: 2 (keyboard did not open with the dialog; focus landed on the dropdown)
8. Aesthetic and minimal design: 3
9. Error recovery: 2 (note error never cleared while typing)
10. Help and documentation: 2 (empty state did not name the Add entry action)
Total about 23/40.

## Cognitive load
Low: one list, one action. Dialog has three fields (attach to, note, photo), well inside the 4-item working-memory limit. The only extra load was in the dialog: the first focused field was a read-only dropdown, so the user had to tap the note before typing.

## Strengths
1. Clean single-purpose screen with one primary action; list scales with font 1.3 without clipping (journal-font.png).
2. Monospace correctly absent: entry text and meta are prose in Roboto.
3. Dark mode is first-class; the navy shell holds and the FAB uses theme roles.

## Issues
1. **P1** Add Entry dialog and typed text vanish on rotation (JournalActivity.java old :73-115). Fix: draft Bundle in onSaveInstanceState (note, stop, photo), dismiss in onDestroy, reopen after the stop list loads. DONE.
2. **P1** Trip-wide entries are saved as day 1 and show "Day 1 · General" (old :118-125). Fix: store day 0 for General and render the stop name only. DONE.
3. **P1** Save confirmation Snackbar covers the FAB and says only "Entry saved" (old :126). Fix: anchor above the FAB, say where it went ("Entry saved to Day 1 · Senso-ji Temple"), and scroll the list to the new top entry. DONE.
4. **P1** FAB can stack two dialogs on a fast double tap (old :54). Fix: one-dialog guard (`entryDialog != null`). DONE.
5. **P1** Keyboard does not open with the dialog and focus starts on the read-only dropdown (dialog_add_journal.xml). Fix: SOFT_INPUT_STATE_VISIBLE and focus the note field. DONE.
6. **P1** Note error never clears when the user types; invalid submit does not refocus the field. Fix: ErrorClearingWatcher and requestFocus. DONE.
7. **P1** Empty state is left-aligned small text that does not name the action (activity_journal.xml:15-23). Fix: centered, reworded to name "Add entry". DONE.
8. **P1** Photo button 40dp tall (Material default) under the 48dp target, and "attached" was text only with a stale view reference after recreate. Fix: minHeight 48dp, check icon plus "Photo attached · tap to change", state kept in the activity. DONE.
9. **P2** "Stop" dropdown label hides that General is selectable. Fix: reworded "Attach to" (string only). DONE.
10. **P2** No character counter for the 500-char note. Fix: counterEnabled. DONE.
11. **P3** Done IME action not applied to the note: a journal note is multiline, Enter must insert a newline. Intentional, no change.
12. **P3** Entries have no timestamp and cannot be edited, deleted or have the photo removed; mock store only. Not changed (product).

## Fix log
- JournalActivity.java: dialog draft save/restore across rotation (note, stop, photo) with dismiss in onDestroy and reopen after stops load; one-dialog guard; General stored as day 0 and rendered as stop name only; Snackbar anchored on the FAB naming the destination; list scrolls to top after save; keyboard opens with focus on the note; note error clears on edit and refocuses; photo state in activity with check icon and tap-to-change label; double-submit guard.
- dialog_add_journal.xml: photo button minHeight 48dp; note counter (500).
- activity_journal.xml: empty state centered with larger padding.
- strings_prototype.xml (shared): `journal_empty` reworded; `entry_stop_label` -> "Attach to"; `entry_saved` replaced by `journal_saved_to`; added `photo_attached_change`.
- Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` passed (0 lint errors, tests green).
