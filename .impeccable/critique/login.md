⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Login critique

Evidence: `login-dark.png` is a good render. `login-light.png` and `login-font.png` are BLANK (they show only the default launcher splash icon, so the capture raced the launch). Light mode and font scale 1.3 were judged from source only. `login-checks.txt`: no native-check issues.

## Design-specificity verdict
Could an unrelated product use this screen unchanged? Yes. It is a stock Material 3 sign-in: centred wordmark in `colorPrimary`, a tagline, two outlined fields, filled button, "or", outlined Google button. Nothing carries the boarding-pass world (no carrier-navy band, no ticket motif, no mono data). Correct for Refinement mode and for a gateway screen. The identity gap is flagged, not fixed. Colour roles are used correctly (no raw hex), so dark mode is coherent.

## Nielsen heuristics (0-4)
1. Visibility of system status: 2. No loading or progress state; both buttons navigate instantly (auth is UI only).
2. Match with real world: 3. Plain, familiar copy.
3. User control and freedom: 3. Nothing to undo; no way to recover a forgotten password.
4. Consistency and standards: 3. Material components. The primary button drew shorter than the secondary one (issue 1).
5. Error prevention: 2. Email format is not validated; the keyboard did not advance or submit (issues 2, 3).
6. Recognition over recall: 3. Persistent outlined-box labels, autofill hints set.
7. Flexibility and efficiency: 2. No IME Next/Done wiring (issue 2). Autofill is present.
8. Aesthetic and minimalist design: 3. Clean; lower 45% of the screen is empty.
9. Error recovery: 2. Errors were stale until the next submit (issue 4); no focus jump to the bad field.
10. Help and documentation: n/a for a login form.
Total: 23/36 (scored 9 of 10).

## Cognitive load
Low. Two fields, one primary action, one alternative; 3 decision points (well under 4). Single column, clear grouping, "or" divider separates the two paths cleanly. Extraneous load: the keyboard "Done" key did nothing and errors persisted after the user fixed the field. Product-level tension: two parallel auth paths when PRODUCT.md commits to Google Sign-In only (flagged).

## Strengths
1. Material 3 theming via role tokens, readable in dark (login-dark.png), good contrast for subtitle and outline.
2. Correct input types and autofill hints on both fields; password visibility toggle included.
3. Edge-to-edge handled; screen is scrollable, so large font or landscape cannot clip it.

## Issues
1. **P1** Hierarchy inversion: Sign In drew about 40dp tall (default insets) while the Google button drew 48dp, so the secondary action looked heavier than the one primary action. Evidence: `login-dark.png` (Sign In about 100px vs Google about 118px), `activity_login.xml:78-83,116-124`. Fix: Sign In `minHeight=56dp`, insets 0.
2. **P1** Keyboard flow broken: no `imeOptions`, so the Next key did not advance and Done did not submit. Evidence: `activity_login.xml` inputs, `LoginActivity.java` (no editor-action listener). Fix: `actionNext` on email, `actionDone` on password, Done calls `attemptSignIn()`.
3. **P1** IME not inset-aware: with edge-to-edge (targetSdk 37) `adjustResize` no longer resizes the window, and only `systemBars` insets were applied, so the keyboard could cover the Sign In button with no scroll room. Evidence: `LoginActivity.java:46-49`. Fix: include `Type.ime()` in the padding insets.
4. **P1** Stale validation errors persisted after the user typed; the failed submit did not focus the offending field. Evidence: `LoginActivity.java` (`attemptSignIn`). Fix: clear error on text change, `requestFocus()` on the first invalid field.
5. **P2** Brand "TripFit" is not marked as a heading for TalkBack. Fix: `ViewCompat.setAccessibilityHeading` (done, one line).
6. **P2** No email-format validation (needs a new string in a shared strings file and real auth is not wired yet). Not fixed.
7. **P2** Light-mode and font-scale 1.3 captures are blank. Needs re-capture in the final sweep.
8. **P3** Composition is top-weighted with about 45% empty space below; acceptable for thumb reach. Status-bar icons look dark-on-dark in login-dark.png (clock and battery barely visible); this may be a capture artifact because the blank shots show the inverse, so it needs verification on the re-capture.
9. **P3** `DEV_BYPASS_LOGIN` skips all validation in debug builds, so the validation path above cannot be exercised on debug builds.
10. **Flagged (product)** Visual identity: login carries none of the boarding-pass world; adding a navy carrier band or ticket motif would change the visual world.
11. **Flagged (product)** PRODUCT.md says Google Sign-In with no separate account system, yet the screen leads with an email/password form (and has no forgot-password or sign-up). Decide whether the Google button should be the primary action.

## Fix log
- Issue 1 fixed: `app/src/main/res/layout/activity_login.xml` (Sign In `minHeight=56dp`, insets 0).
- Issue 2 fixed: `activity_login.xml` (imeOptions) and `LoginActivity.java` (Done submits).
- Issue 3 fixed: `LoginActivity.java` (ime insets in root padding).
- Issue 4 fixed: `LoginActivity.java` (`clearErrorOnEdit`, focus first invalid field).
- Issue 5 fixed: `LoginActivity.java` (accessibility heading).
- Not fixed: issues 6-9 (P2/P3, not one-line or need verification); 10-11 flagged in the queue file.
- Shared files changed: none.
- Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` exit 0, 0 lint errors, tests green.
