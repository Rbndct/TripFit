⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Budget Setup critique, 2026-11-16

Evidence: `budget-setup-light/dark/font.png` (Pixel 3a emulator), checks file ("no native-check issues"). All three rendered. The light and font shots predate the shared status-bar fix (status icons look dark on navy); dark is correct.

## Design-specificity verdict
A competent, plain Material form: a segmented mode switch, outlined fields, a switch, four category fields and one primary button. The navy shell, the amber selected segment and the monospace values carry the TripFit identity, but nothing on the screen is specific to a boarding pass, and the screen had two typographic slips against the Named Rules: every floating label ("Budget (PHP)", "Home currency", "Food") and the split-total sentence were monospace. Refinement mode: keep the structure, fix the rule breaks and the logic around the split.

## Nielsen heuristics
| # | Heuristic | Score | Key issue |
|---|-----------|-------|-----------|
| 1 | Visibility of system status | 2 | Split total never reacted to mode or currency changes; over-limit only surfaced as a Snackbar after Save |
| 2 | Match system / real world | 2 | Per-day mode never said "per day" or showed the trip total; split compared against the per-day figure |
| 3 | User control and freedom | 3 | Back arrow, nothing destructive |
| 4 | Consistency and standards | 2 | Labels and a sentence in monospace |
| 5 | Error prevention | 2 | No live over-limit warning; double tap could save twice |
| 6 | Recognition over recall | 2 | Category fields carried no currency or unit |
| 7 | Flexibility and efficiency | 2 | No IME Next/Done chain |
| 8 | Aesthetic and minimalist design | 3 | Clean, one primary action |
| 9 | Error recovery | 2 | Budget error stayed after typing; focus never moved |
| 10 | Help and documentation | 3 | Currency note is useful |
| **Total** | | **23/40** | Acceptable, real gaps |

## Cognitive load
Low: one decision per field, at most six fields. Checklist fails: in per-day mode the user must mentally multiply to know what the split is checked against; the split result is learned only after pressing Save. Rotation also left the field hint and currency note on the stored currency while the dropdown showed the restored one.

## Strengths
1. Single primary action, clear grouping (mode, amount, currency, optional split), switch reveals the split only when wanted.
2. Dark theme is correct: amber selected segment, readable outlines, fixed navy shell.
3. Numeric fields use number keyboards with a 9 digit cap; the field hint names the currency.

## Issues
1. P1 Split total compares against the per-day amount, not the trip total, while Save compares against the trip total. `BudgetSetupActivity.java` (old `updateSplitSum`). Fix: one `tripTotal()` used by both.
2. P1 Split total, field hint and currency note do not update when the currency changes (only on dropdown click) or when the mode toggles; after rotation they show the stored currency. Old `bindCurrent`. Fix: listeners on the currency text, the mode toggle and the amount, one `refreshLabels()`.
3. P1 Over-limit appears only as a Snackbar after Save, no inline state, focus stays on the button. Fix: live "Limits are X over the Y trip budget" in the error color (words carry the meaning), focus the first limit field on a failed save.
4. P1 Budget error stays after the user types; focus does not move to the field. `save()`. Fix: clear on edit, `requestFocus()` on failure.
5. P1 No IME chain: fields end in a default Enter. `activity_budget_setup.xml`. Fix: Next on the amount and first three limits, Done on the last submits.
6. P1 Per-day mode is unlabeled: hint "Budget (PHP)" and no trip total. Fix: "Budget per day (PHP)" and a helper "X × N days = Y trip total".
7. P1 Floating labels and the split sentence are monospace (labels are not data). `activity_budget_setup.xml:78,177`, light/dark shots. Fix: `TextInputLayout.setTypeface(DEFAULT)` for all six layouts, sentence in the body face; typed values stay monospace.
8. P2 Category limit fields have no unit. Fix: prefix with the home currency code.
9. P2 Save is not guarded against a double tap (finish() is synchronous so the risk is small). Fix: `isSaving` flag.
10. P3 (left) Success feedback after Save is the Budget screen re-rendering; a Snackbar would need a result hand-off (changes the contract with BudgetActivity).
11. P3 (left) Status bar icons dark in light shots: already fixed in `ScreenActivity`, needs re-capture.
12. P3 (left) Mode buttons and Save render 40dp tall with a 48dp touch target (checks pass).

## Fix log
- Issue 1: `BudgetSetupActivity.java` (`tripTotal`, `updateSplitSum`, `save`).
- Issue 2: `BudgetSetupActivity.java` (`bindListeners`, `refreshLabels`; text watchers on currency and amount, toggle listener).
- Issue 3: `BudgetSetupActivity.java` (error-colored split line, focus Food on failed save), `strings_prototype.xml` (`budget_split_over`, reworded `budget_split_sum`).
- Issue 4: `BudgetSetupActivity.java` (error cleared on edit, focus the amount).
- Issue 5: `activity_budget_setup.xml` (`imeOptions`), `BudgetSetupActivity.java` (Done submits).
- Issue 6: `strings_prototype.xml` (`field_budget_hint_daily`, plural `budget_daily_total`), `BudgetSetupActivity.java` (`refreshLabels`).
- Issue 7: `activity_budget_setup.xml` (split sum no longer monospace), `BudgetSetupActivity.java` (`useLabelFont`).
- Issue 8: `BudgetSetupActivity.java` (`setPrefixText`).
- Issue 9: `BudgetSetupActivity.java` (`isSaving`).
- Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` passed (0 errors, tests green). Not verified on device: label typeface, prefix, helper text; rotation restore of the derived labels.
