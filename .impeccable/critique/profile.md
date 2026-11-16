⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Profile critique

Evidence: `profile-light/dark/font.png` all rendered; `profile-checks.txt` = no native-check issues. Status bar reads navy in all three shots. Picker dialogs (currency, buffer), About and Sign out confirm are NOT in the screenshots, so they were reviewed from source only. `item_setting.xml` is shared with Stop Detail's "Move to day" row and was NOT edited; all changes went through `activity_profile.xml` / `ProfileActivity.java`, so Stop Detail is unaffected.

## Design-specificity verdict
A competent, plain Material settings list: one outlined 16dp account card, a "Preferences" heading, rows with monospace values (correct: PHP and "15 min" are data), two switches and an outlined red Sign out. It sits correctly inside the navy shell and uses no ticket motif, which is right for a utility screen in Refinement mode. The weaknesses are affordance and honesty, not styling: the picker rows and About look like static text, the Dark mode switch contradicts the screen when the system drives dark mode, and changing a setting gives no confirmation.

## Nielsen heuristics (0-4)
1. Visibility of system status: 1 (a changed currency or buffer only silently rewrites a trailing value; dark switch shows OFF on a dark screen, profile-dark.png)
2. Match with real world: 3 ("Handled natively by Google Calendar" sits under a switch that toasts; prototype mock)
3. User control and freedom: 3 (Cancel on pickers, confirm before sign out)
4. Consistency and standards: 2 (rows use 16sp BodyLarge, switches used default label size; Stop Detail's picker row has a chevron, Profile's did not)
5. Error prevention: 3 (sign-out confirmation; no free-text input)
6. Recognition over recall: 2 (Home currency, buffer and About give no cue they are tappable)
7. Flexibility and efficiency: 3
8. Aesthetic and minimal design: 3
9. Error recovery: n/a
10. Help and documentation: 3 (helper line under reminders)
Total about 23/36 scored, normalised about 25/40.

## Cognitive load
Low: seven controls in two groups, nothing nested. The only load is recall: the user must guess which rows open pickers. The About row has no value and no icon, so it reads as a stray label.

## Strengths
1. Monospace used only for data values (PHP, 15 min); labels and prose stay in Roboto.
2. Sign out is outlined, red-toned via theme-safe `status_over` (defined in both modes), with a text label and a confirm dialog; one clear destructive action, not the primary one.
3. Layout holds at font 1.3 with no clipping (profile-font.png); rows are 154px (56dp) tall, above 48dp.

## Issues
1. **P1** Dark mode switch reads OFF while the screen is dark whenever the system theme drives it (profile-dark.png: dark screen, switch off; ProfileActivity old :40 compared only to `MODE_NIGHT_YES`). Fix: derive the state from the actual `uiMode`, and `saveEnabled=false` so a restored view state cannot disagree. DONE.
2. **P1** Currency, buffer and About rows have no tap affordance (profile-light.png; item_setting has no icon). Fix: trailing expand-more icon on the two picker rows (same as Stop Detail's day row) and a new chevron-right on About, set in code on the value TextView. DONE.
3. **P1** TalkBack reads title and value as separate stops with no hint of the action (item_setting.xml, ProfileActivity old :53). Fix: row content description "Title, value" (buffer spoken as "15 minutes" via a plural) and a replaced click action label "Choose" / "Open". DONE.
4. **P1** A changed setting gets no confirmation (old :67-71, :88-93). Fix: Snackbar "Home currency set to USD", "Default buffer set to 15 minutes" / "Buffer removed". DONE.
5. **P1** Picker, About and Sign-out dialogs vanish on rotation (no state kept). Fix: remember the open dialog kind in the saved state, reopen after recreate, dismiss in onDestroy to avoid a leaked window. Pickers already show the current choice selected. DONE.
6. **P1** "Preferences" is not marked as a heading for TalkBack (activity_profile.xml:73). Fix: `accessibilityHeading`. DONE.
7. **P2** Switch labels use the default 14sp-ish text while rows use BodyLarge, so the list looks uneven (profile-light.png). Fix: `textAppearanceBodyLarge` + onSurface on both switches (same file already edited). DONE.
8. **P2** Reminders helper text touches the About row with no gap (profile-light.png). Fix: `paddingBottom` space_sm. DONE.
9. **P2** Sign out relies on Material's 40dp button height; fixed `minHeight=48dp`. DONE.
10. **P3** Avatar is decorative (`contentDescription=@null`), correct; the account card reads as three TalkBack stops. Left as is.
11. **P3** The Event reminders switch only toasts; the helper says Google Calendar handles it. Mock behaviour, not changed.
12. **P3** Name "Traveler" and email `traveler@gmail.com` are placeholders; real values belong to Google Sign-In wiring (already flagged under Calendar Sync).

## Fix log
- ProfileActivity.java: dark switch reads the live uiMode; trailing icons on rows (expand-more for pickers, chevron-right for About) set in code; row content descriptions and "Choose"/"Open" action labels; Snackbar confirmations for currency and buffer; open dialog remembered across rotation and dismissed in onDestroy; row rendering extracted to `renderCurrency`/`renderBuffer`.
- activity_profile.xml: heading on "Preferences"; BodyLarge/onSurface on both switches; `saveEnabled=false` on the dark switch; helper bottom padding; Sign out minHeight 48dp.
- drawable/ic_chevron_right_24.xml: new, self-tinted with `colorOnSurfaceVariant` (mirrors ic_expand_more_24).
- strings_prototype.xml (shared): added `profile_row_cd`, plural `buffer_minutes_spoken`, `profile_action_choose`, `profile_action_open`, `profile_currency_set`, `profile_buffer_none_set`, plural `profile_buffer_set`.
- `item_setting.xml` unchanged; Stop Detail's day row unaffected.
- Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` passed (0 lint errors, tests green).
