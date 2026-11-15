⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# New Trip critique

Evidence: `new-trip-light.png`, `new-trip-dark.png`, `new-trip-font.png` (all three rendered, none blank; Pixel 3a emulator), `new-trip-checks.txt` (no native-check issues), source `NewTripActivity.java` / `activity_new_trip.xml`.

## Design-specificity verdict
Functional and correct, but generic: it is a stock Material 3 outlined form under the navy shell toolbar. It stays inside the boarding-pass world only through the header band and the monospace values; there is no ticket cue in the body. For a REFINEMENT pass on a utility form that is acceptable (the world supplies palette, type rules and the shell, not a new layout), so the verdict is "competent default, shell-consistent", not "distinctive". Adding a ticket motif here would change the visual world and is not proposed.

## Nielsen heuristics (0-4)
| # | Heuristic | Score | Note |
|---|-----------|-------|------|
| 1 | Visibility of system status | 2 | Create button stayed enabled while saving (double-submit); dates field gave no hint that blank means 3 days. |
| 2 | Match real world | 3 | Plain trip vocabulary; "I'll add stops / Suggest for me" is clear. |
| 3 | User control and freedom | 3 | Back arrow, system back; no discard confirm (acceptable on a 4-field form). |
| 4 | Consistency and standards | 2 | Light status bar had dark icons on navy; monospace leaked onto the "Trip dates" and "Home currency" labels, against the Mono-Means-Data Rule. |
| 5 | Error prevention | 2 | Dates silently defaulted to 3 days; rotation lost the picked dates and trip length (state held only in fields). |
| 6 | Recognition over recall | 3 | Visible labels, prefilled city and currency. |
| 7 | Flexibility and efficiency | 2 | No IME action on the name field. Create button floats mid-screen, not thumb-anchored. |
| 8 | Aesthetic and minimal design | 3 | Clean, one primary action. Large empty lower half on tall phones. |
| 9 | Error recovery | 2 | Field errors stayed after the user fixed the field; no focus moved to the invalid field. |
| 10 | Help and documentation | 3 | Not needed beyond the dates helper added. |
Total 25/40 before fixes.

## Cognitive load
Low. Four fields plus one binary choice, one primary action, no competing CTAs. Remaining load: the dates field looked like a plain text input (focusable=false, no trailing affordance) so users may try to type; the "How do you want to start?" choice does not say it picks the next screen, but the labels carry enough meaning. Prefilled "Tokyo" removes the empty-city error path but also makes the destination an easy-to-miss default (flagged P3).

## Strengths
1. One primary filled action, correct hierarchy; segmented control is a Material toggle group with required single selection.
2. Dark theme is first-class: navy shell held fixed, amber selected segment and tonal primary button resolve correctly (`new-trip-dark.png`).
3. No clipping at font scale 1.3 and no sub-48dp targets (`new-trip-font.png`, `new-trip-checks.txt`).

## Issues
1. **P1** Light-mode status bar has dark clock/icons on the navy band, nearly unreadable (`new-trip-light.png`, top strip). Cause: `ScreenActivity.setScreen` never forced light status icons. Fix: `applyShellSystemBars()` in `ScreenActivity` (same pattern as Home).
2. **P1** Picked dates and trip length are lost on rotation or process recreation: `datesLabel` / `tripDays` are plain fields, the restored field text then disagrees with the saved trip (`NewTripActivity.java:36-37`, `onCreateResult`). Fix: persist both in `onSaveInstanceState`.
3. **P1** Create can be tapped repeatedly before the async result, creating duplicate trips (`NewTripActivity.attemptCreate`). Fix: disable the button on submit, re-enable on failure.
4. **P1** Dates field has no affordance and no explanation of the empty state (`new-trip-light.png`, "Trip dates"); an empty value silently becomes 3 days. Fix: trailing calendar end icon (opens the picker, with a content description) and helper text "Optional. Without dates your trip starts with 3 days.", cleared once dates are chosen.
5. **P1** Monospace on field labels: "Trip dates" and "Home currency" hints render in monospace (`new-trip-light.png`), violating Mono-Means-Data. Fix: keep monospace on the values, set the layout typeface to default for hint/helper/error.
6. **P2** Errors persist after the user corrects the field, and focus does not move to the invalid field. Fix: `ErrorClearer` text watcher on name, clear city error on selection, `requestFocus()` on the first invalid field.
7. **P2** Name field has no IME action. Fix: `imeOptions="actionDone"` (one line).
8. **P2** Create button floats mid-screen with ~half the screen empty beneath (`new-trip-light.png`); a bottom-anchored action would sit in the thumb zone. Not fixed: layout restructuring beyond a one-liner.
9. **P3** City is prefilled with the first mock city (Tokyo); the "Pick a city" error can never trigger. Needs a product decision (flagged).
10. **P3** Outline stroke `#8B93A6` on paper is about 3:1; Material default, passes the non-text threshold narrowly. Left.

## Fix log
- Issue 1 (P1) fixed: `ScreenActivity.java`, `applyShellSystemBars()` (shared; benefits every navy-toolbar screen).
- Issue 2 (P1) fixed: `NewTripActivity.java`, save/restore `datesLabel` and `tripDays`.
- Issue 3 (P1) fixed: `NewTripActivity.java`, button disabled on submit, re-enabled on failure.
- Issue 4 (P1) fixed: `activity_new_trip.xml` (end icon), `drawable/ic_calendar_24.xml` (new), `strings_prototype.xml` (`cd_pick_dates`, `field_dates_helper`), `NewTripActivity.java` (helper, end-icon click).
- Issue 5 (P1) fixed: `NewTripActivity.applyLabelTypefaces()`; `layout_currency` id added in the layout.
- Issue 6 (P2) fixed (bundled with the edits above): `ErrorClearer`, city selection clears error, focus to invalid field.
- Issue 7 (P2) fixed: `imeOptions="actionDone"`.
- Issue 8 (P2) not fixed (restructure), listed here only.
- Issue 9 flagged for user. Issue 10 left.
- Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` exit 0, 0 lint errors. Not re-verified on device; confirm in the final sweep (helper-text wrap at font 1.3, end icon tint, label typeface).
