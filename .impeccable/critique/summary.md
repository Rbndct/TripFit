⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Trip Summary critique, 2026-11-16

Evidence: `summary-light/dark/font.png` (Pixel 3a emulator), checks file ("no native-check issues"). All three rendered. Light and font shots predate the shared status-bar fix (status icons look dark on navy); dark is correct.

## Design-specificity verdict
A faithful but flat printout: trip name, three outlined cards, two buttons. It is inside the TripFit world (navy shell, 16dp outlined cards, one navy primary action) but it reads as a text dump rather than a ticket stub. Times and amounts were buried in body-face sentences ("Food: ₱4,293", "09:00-09:45  Senso-ji Temple") instead of the monospace data slot the boarding-pass system reserves for them, long stop names would wrap under their times, and a budget that had been overspent looked identical to one that had not. The export action gave no feedback while writing and could be tapped repeatedly. Refinement: keep the structure, put data in data slots, add text status, make export honest.

## Nielsen heuristics
| # | Heuristic | Score | Key issue |
|---|-----------|-------|-----------|
| 1 | Visibility of system status | 1 | Export had no busy state; no over-budget state |
| 2 | Match system / real world | 3 | Day / Budget language fine |
| 3 | User control and freedom | 3 | Read-only; back works |
| 4 | Consistency and standards | 2 | Data not in monospace slots (unlike Calendar Sync, Budget) |
| 5 | Error prevention | 1 | Double tap opened two choosers; export allowed before data loaded |
| 6 | Recognition over recall | 3 | Everything visible |
| 7 | Flexibility and efficiency | 3 | Two clear export routes |
| 8 | Aesthetic and minimalist design | 3 | Clean; empty-state copy dead-ends |
| 9 | Error recovery | 2 | Failure message had no next step |
| 10 | Help and documentation | n/a | Read-only summary |
| **Total** | | **21/36** | Acceptable, real state gaps |

## Cognitive load
Low. One scroll, one primary action. Violation: a zero or missing budget printed "₱0 of ₱0 spent", and an overspent budget gave no cue, so the reader had to compare two numbers.

## Strengths
1. Export and Share pinned below the scroll area, always reachable (font 1.3 shot).
2. Flat 16dp outlined cards; dark theme correct, shell stays navy.
3. Export failure path exists and the PDF work runs off the main thread.

## Issues
1. P1 Export has no busy or disabled state and no double-tap guard; each tap spawns a PDF write and a chooser. `SummaryActivity.java` (old `exportPdf`). Fix: `isExporting` flag, disabled button with "Preparing PDF…" live-region label, buttons disabled until trip and items have loaded.
2. P1 Export result is delivered to a destroyed activity if rotated mid-write. Fix: `finishExport` guards `isFinishing()/isDestroyed()`.
3. P1 Export failure said only "Could not create the PDF". Fix: "...Try again."
4. P1 Times and amounts sit in body-face sentences; names wrap under times. `item_row` subtitle. Fix: own section card with label left and monospace data value right (time range per stop, amount per category).
5. P1 Over-budget has no status; zero budget prints "₱0 of ₱0 spent". Fix: "N over budget" note in error color (text carries the meaning), "No budget set" note and "X spent" when total is 0; same note added to the PDF/share text.
6. P1 Headings not marked: trip name and card titles are plain TextViews for TalkBack. Fix: `setAccessibilityHeading` on the trip name and each section title; each label/value line is one focus stop.
7. P1 Empty state "No stops to summarize yet." is a dead end. Fix: names the next step (add places on the itinerary).
8. P2 No stop count on day cards. Fix: plural `summary_stop_count` in the header (monospace).
9. P2 Buttons lacked explicit 48dp min height. One-line `minHeight`.
10. P3 PDF text export: long lines are not wrapped (clip past ~80 chars), day headings not bold; not touched (do not redesign the PDF).
11. P3 Export PDF and Share summary carry the same content; product call. Flagged.
12. P3 Export on an empty itinerary still creates a budget-only PDF.

## Fix log
- Issue 1 (P1) fixed: `SummaryActivity.java` `isExporting`/`updateActions()`; `activity_summary.xml` buttons start disabled, export has a polite live region; new string `summary_exporting`.
- Issue 2 (P1) fixed: `finishExport()` guards finishing/destroyed activity.
- Issue 3 (P1) fixed: `pdf_failed` reworded.
- Issue 4 (P1) fixed: new `layout/item_summary_section.xml` and `layout/item_summary_line.xml`; the RecyclerView became a `container_summary` LinearLayout filled in `render()` (shared `RowAdapter`/`UiRow` untouched).
- Issue 5 (P1) fixed: `budgetNote()`, strings `summary_spent_value`, `summary_spent_only`, existing `budget_over` / `budget_no_total`; note also appended to the export text.
- Issue 6 (P1) fixed: `ViewCompat.setAccessibilityHeading` on trip name and section titles; line rows focusable.
- Issue 7 (P1) fixed: `summary_empty` reworded.
- Issue 8 (P2) fixed: plural `summary_stop_count` via `getQuantityString` (single call site).
- Issue 9 (P2) fixed: `minHeight=48dp` on both buttons.
- Issues 10-12 not fixed (flagged / P3; `TripPdfWriter.java` untouched).
- Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` passed (0 lint errors, tests green).
