⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Stop Detail critique

Evidence: `stop-detail-{light,dark,font}.png` all render (Pixel 3a emulator); checks file: "no native-check issues". Captures predate the shared navy status-bar fix (`ScreenActivity.applyShellSystemBars`), so the dark-on-navy status icons in light/font shots are stale; source confirms navy + light icons now.

## Design-specificity verdict
This is a competent stock Material form wearing the navy shell. Nothing on it says "boarding pass" below the toolbar: a plain headline, a settings row, two outlined fields and two switches. That is acceptable for an edit sheet (Refinement mode: the shell identity is carried by the toolbar and navy Save button), but the screen reads as a generic settings page rather than a ticket being amended. It trusts Material structure correctly and keeps monospace for data. The real weaknesses are affordance and honesty, not style.

## Nielsen heuristics (0-4)
1. Visibility of system status: 2 (Save gives no in-progress state; buffer/notes/photo state silently not saved)
2. Match with real world: 3
3. User control and freedom: 2 (back discards edits with no guard; attached photo cannot be removed)
4. Consistency and standards: 2 (Day row looked like static text; time field lacked its clock icon unlike Add Destination)
5. Error prevention: 2 (duration 0 accepted; double-tap Save)
6. Recognition over recall: 3
7. Flexibility and efficiency: 3
8. Aesthetic and minimalist design: 3
9. Error recovery: 2 (time error not scrolled to, not cleared when booked is toggled off)
10. Help and documentation: 3 (helper texts are good)

## Cognitive load
Eight controls plus two actions on one scroll; manageable. Intrinsic load low. Extraneous: "Day / Day 1" repeated, and a Day row with no sign it is tappable. Germane: helper under duration ("smart estimate (60 min)") is a strength. Primary action (Save) is below the fold at font 1.3.

## Strengths
- Helper text on duration explains the empty-field behaviour.
- Single primary (navy Save) with a quiet red text Delete behind a confirm dialog; day move uses a clear single-choice dialog.
- Dark theme is first-class: navy shell constant, tonal controls resolve correctly.

## Issues
1. **P1** Day row looks like static text, repeats "Day / Day 1", and its text sits 16dp inside the field gutter (`stop-detail-light.png` y~408; `item_setting.xml` padding). Fix: title "Move to day", trailing chevron, zero horizontal padding.
2. **P1** Fixed-time field has no clock icon although a listener is wired (`activity_stop_detail.xml` layout_time; `setEndIconOnClickListener` is a no-op without endIconMode). Fix: custom end icon with `select_time` description, as Add Destination.
3. **P1** Validation gaps: duration 0 accepted (`StopDetailActivity.save`); failed save for missing time shows no scroll/focus; time error persists after toggling booked off; no error clearing on duration edit. Fix: min-1 check with error, scroll to time field, clear on edit/toggle.
4. **P1** Save can be double-submitted (button stays enabled during write). Fix: disable until result.
5. **P1 (flagged, product)** Notes, buffer toggle and photo are session-only while the button says "Save changes". Cannot be fixed without schema/product decision.
6. **P2** Buffer at 0 min shows a checked, disabled switch with "0 min for parking..." Fix: unchecked, helper "No buffer".
7. **P2** Duration input is data but not monospace, and has no IME Done. Fix both (one-line).
8. **P2** Stop title not marked as a heading for TalkBack. Fix: `setAccessibilityHeading`.
9. **P2 (not applied)** Save below the fold at font 1.3 (`stop-detail-font.png`); consider a pinned bottom bar. Back with unsaved edits has no guard.
10. **P3** Photo button text flips to "Photo attached" with no preview or remove.
11. **P3** Stale status-bar evidence; re-capture in final sweep.

## Fix log
- 1: `StopDetailActivity.bindDayRow` (title string, chevron, padding), new `res/drawable/ic_expand_more_24.xml`.
- 2: `activity_stop_detail.xml` end icon attrs on layout_time (+ cursorVisible false).
- 3: `StopDetailActivity.save/bindDuration/bindBooked`; new string `error_duration_invalid` in `values/strings.xml` (logged in queue).
- 4: `StopDetailActivity.save` disables Save until failure.
- 5: flagged in queue.
- 6: `StopDetailActivity.bindBuffer`.
- 7: `activity_stop_detail.xml` duration input monospace + `imeOptions=actionDone`.
- 8: `StopDetailActivity.onCreate`.
- 9-11: not applied (P2 not one-line / P3).
- Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` exit 0 (compiles, tests green, no lint errors).
