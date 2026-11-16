⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Budget (+ Add Expense dialog) critique, 2026-11-16

Evidence: `budget-light/dark/font.png` (Pixel 3a emulator), checks file ("no native-check issues"). The Add Expense dialog is NOT in the screenshots; it was reviewed from source only. The light shot predates the shared status-bar fix (status icons look dark on navy); dark is correct.

## Design-specificity verdict
This is the most data-honest screen so far: a single outlined hero card (spent, of total, bar, left), per-category progress rows, then the expense ledger in the shared outlined rows. Monospace amounts and the navy shell tie it to the boarding-pass world, and "Over" is a real, product-specific state (a red bar plus a pill). It is still close to a generic Material budget screen: there is no ticket cue on the hero, and the progress tracks are a pale yellow with M3 stop dots, which is the theme default rather than a choice. Refinement mode: that is acceptable and not worth a visual-world change.

## Nielsen heuristics
| # | Heuristic | Score | Key issue |
|---|-----------|-------|-----------|
| 1 | Visibility of system status | 3 | Over-budget is red bar + text + pill (not color-only). Add confirmation was a Snackbar that covered the FAB |
| 2 | Match system / real world | 3 | "left" / "over budget" read well; "₱-X left" possible when no budget is set |
| 3 | User control and freedom | 3 | Dialog is cancellable; rotation discarded typed input |
| 4 | Consistency and standards | 3 | "Over" pill used monospace though it is a label |
| 5 | Error prevention | 3 | Amount parse is solid; no double-submit risk (synchronous save) |
| 6 | Recognition over recall | 3 | Currency shown in field hint; live home-currency preview |
| 7 | Flexibility and efficiency | 2 | No IME Next/Done in the dialog; keyboard not opened on show |
| 8 | Aesthetic and minimalist design | 3 | Clean; hero button is outlined so FAB stays the one primary |
| 9 | Error recovery | 2 | Errors stayed after fixing the field; no focus on the invalid field |
| 10 | Help and documentation | 3 | Empty state described the problem but not the action |
| **Total** | | **28/40** | Good |

## Cognitive load
Low: four categories, one hero number, one action. Checklist fails: the empty-expense message gave no next step; TalkBack had to traverse three stops (name, badge, amount) plus a silent progress bar per category. Decision points are within 4.

## Strengths
1. Over-budget has redundant cues (red bar, "Over" pill, "over budget" copy) and works in dark (tokens defined in both themes).
2. Hero hierarchy is clear: large mono spent amount, quiet "of total", one remaining line.
3. The dialog shows a live converted amount, which delivers the currency-conversion promise in context.

## Issues
1. P1 Dialog: no IME actions; keyboard is not raised on open. `dialog_add_expense.xml` fields, `AddExpenseDialog.java`. Fix: Next on the title, Done submits from the amount, soft input visible.
2. P1 Dialog: validation errors persist after the user edits, and focus stays on the Add button. `AddExpenseDialog.java:~70`. Fix: clear each error on edit, focus the first invalid field.
3. P1 Rotation drops the open dialog and everything typed. `BudgetActivity.java`. Fix: additive `show(..., draft, ...)` + `draftOf()`, saved in `onSaveInstanceState`, dialog dismissed in `onDestroy`, one-dialog guard.
4. P1 Category rows are three TalkBack stops with silent progress bars ("Food", "Over", "₱4,293 of ₱4,000"). `item_category.xml`, `BudgetActivity.renderCategories`. Fix: one focusable row with a content description naming over state; bars hidden from accessibility.
5. P1 With total 0 (no budget set) the hero reads "of ₱0" and "₱-X left". `BudgetActivity.render`. Fix: "No budget set", remaining line hidden.
6. P2 "Added" confirmation Snackbar covers the FAB. Fix: anchor to the FAB (done, in the same file).
7. P2 "Over" pill uses monospace (it is a label, not data). `item_category.xml:34` fixed.
8. P2 Empty expenses text gave no next action. String reworded to name the Add expense button.
9. P2 Total progress bar duplicated the text for TalkBack. Hidden from accessibility (`activity_budget.xml`).
10. P3 (left) Conversion preview is a sentence set in monospace; fine as an amount-led line.
11. P3 (left) Category row (name + pill + "₱4,293 of ₱4,000") could crowd at font scale >= 1.5; fine at 1.3 per screenshot.
12. P3 (left) Progress track is a pale yellow with M3 stop dots; theme default, not changed.
13. P3 (left) Expenses cannot be edited or deleted: product decision, flagged.

## Fix log
- Issue 1: `AddExpenseDialog.java` (Form class), `dialog_add_expense.xml` (actionNext / actionDone, soft input visible).
- Issue 2: `AddExpenseDialog.java` (SimpleWatcher clears errors, requestFocus on first invalid).
- Issue 3: `AddExpenseDialog.java` (additive overload `show(.., Bundle draft, ..)` returning the dialog, `draftOf`), `BudgetActivity.java` (save/restore/dismiss).
- Issue 4: `item_category.xml`, `BudgetActivity.java` (screen-reader focusable row, content description), `strings_prototype.xml` (`category_row_cd`, `category_row_cd_over`).
- Issue 5: `BudgetActivity.java` (`renderRemaining`), `strings_prototype.xml` (`budget_no_total`).
- Issues 6-9: `BudgetActivity.java`, `item_category.xml`, `activity_budget.xml`, `strings_prototype.xml`.
- Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` passed (0 errors, tests green).
