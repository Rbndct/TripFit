⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Itinerary (MainActivity) critique

Evidence: `itinerary-light.png`, `-dark.png`, `-font.png` (Pixel 3a emulator, all three render), `itinerary-checks.txt`, source. No tablet or TalkBack evidence.

## Design-specificity verdict

This screen is grounded in TripFit, not generic. The navy carrier header, the white "stamped" Day pill with outlined siblings, and the monospace day summary read as legs of one boarding pass, and the Day-tab filter is a real structural choice rather than a template list. A reader could not lift it into an unrelated product unchanged. The stop cards (numbered circle, title, meta, chip, Booked pill) are plain Material and carry no ticket ornament, which DESIGN.md says is intended. The one place the world broke down was the system chrome: a white status strip sat above the navy header, which read as a bug, not a ticket.

## Nielsen heuristics (0-4)

| # | Heuristic | Score | Note |
|---|-----------|-------|------|
| 1 | Visibility of system status | 3 | Selected Day pill is clear visually; TalkBack had no selected-state cue (fixed). Weather banner surfaces risk early. |
| 2 | Match with real world | 3 | Leg, Booked, Day vocabulary is natural. "1 stops" / "1 outdoor stops" broke it (fixed). |
| 3 | User control and freedom | 3 | Back, Cancel on leg dialog and weather dialog, Keep plan. No reorder or delete here (brief: out of scope). |
| 4 | Consistency and standards | 3 | Material components throughout. Status bar differed from Home and other screens (fixed). Leg chip claims monospace in source but renders Roboto. |
| 5 | Error prevention | 3 | Booked stops are excluded from the weather swap. |
| 6 | Recognition over recall | 3 | Toolbar icons have titles (tooltip and TalkBack name). The route glyph is abstract; "Optimize route" is only discoverable by long-press. |
| 7 | Flexibility and efficiency | 2 | One fixed path, no shortcuts. Acceptable for a prototype hub. |
| 8 | Aesthetic and minimal design | 3 | Header is ~30% of the screen (title, subtitle, tabs, summary, banner) and does not collapse on scroll. |
| 9 | Error recovery | 3 | Snackbar on failed swap. No undo on the swap. |
| 10 | Help and documentation | n/a | Not applicable for a hub screen. |

Total 26/36 over the 9 scored heuristics (about 2.9 average).

## Cognitive load

- Visible decisions at once: 2 action icons + overflow, N day tabs, FAB, per-card chip + card tap, optional banner. Seven-plus controls, but grouped by region (header chrome / day / list / FAB). Primary action (FAB) is unambiguous.
- Tapping a day tab did not reset list scroll, so a user scrolled deep in Day 1 landed mid-list in Day 2 (fixed).
- Header stacks five rows of information before the first stop; acceptable at 3 stops, heavier with the banner at font scale 1.3.
- Day number, stop count, and travel minutes are in monospace and scan as a ledger; this is working as designed.

## Strengths

1. The day-tab strip follows the On-Navy Contrast Rule exactly (white fill selected, translucent stroke unselected) and is legible in light, dark, and at font 1.3 with no clipping.
2. Dark mode is first-class: navy shell stays fixed, cards and the Booked pill re-tone correctly, outline strokes stay visible.
3. Behavior-rich hub stays tidy: weather swap, leg mode picker, and the eight-way feature menu all route from the toolbar without extra on-screen clutter.

## Issues

1. **P1 · Status bar** · `itinerary-light.png` top strip is white above the navy header; `itinerary-dark.png` is near-black; `MainActivity.java` padded the root by the top inset so the header could not extend under the status bar, and nothing set light status icons. Fix: header absorbs the top inset, navy status bar, light icons (Home's `applyShellSystemBars()` pattern).
2. **P1 · Touch target** · `itinerary-checks.txt` TOUCH_TARGET 40x48dp on "More options". Fix: toolbar theme overlay that gives the overflow button `minWidth` 48dp, scoped to this screen.
3. **P1 · Accessibility, day tabs** · `MainActivity.java` `bindTabSelectedState`: selection was conveyed only by fill colour. Fix: state description "Selected" on the active tab.
4. **P1 · Copy** · `strings_prototype.xml` `day_summary`, `weather_banner`, `weather_swapped`: "1 stops", "1 outdoor stops", visible on any one-stop day. Fix: plurals.
5. **P2 · Scroll** · switching days kept the previous scroll offset. Fix: `scrollToPosition(0)` on tab tap.
6. **P2 · Mono-Means-Data** · `item_stop.xml` `text_stop_meta` ("Stay 45 min", "At 18:00") are duration/time data but set in Roboto. Fix: monospace (one line).
7. **P3 · Leg chip font** · `item_stop.xml:62` sets `android:fontFamily="monospace"` but Material Chip text appearance wins, so the chip renders Roboto (`itinerary-light.png`). It mixes a mode word with minutes, so not fixed; needs a chip text appearance style if mono is wanted.
8. **P3 · Header weight** · header does not collapse on scroll, so on long days it eats ~30% of the viewport (more at font 1.3). Polish idea only.
9. **P3 · Day tab rebuild** · `renderDayTabs()` recreates every pill on each tap, so the ripple and any motion are lost; the "flip a leg" moment in the brief has no transition. Not changed.
10. **P3 · Weather banner role** · clickable `TextView` with no button role for TalkBack. Not changed.

## Fix log

- Issue 1 (P1) fixed: `MainActivity.java` (`applyShellSystemBars()`, header takes top inset, root keeps side/bottom insets), `activity_main.xml` (`appbar_itinerary` id).
- Issue 2 (P1) fixed: `activity_main.xml` (toolbar `android:theme`), new `values/styles_itinerary.xml` (overflow minWidth 48dp, scoped to this screen; `view_toolbar.xml` untouched).
- Issue 3 (P1) fixed: `MainActivity.java` `ViewCompat.setStateDescription` + `state_selected` string.
- Issue 4 (P1) fixed: `strings_prototype.xml` plurals `day_summary`, `weather_banner`, `weather_swapped`; `MainActivity.java` uses `getQuantityString`.
- Issue 5 (P2) fixed: `MainActivity.java` (scroll to top on tab tap).
- Issue 6 (P2) fixed: `item_stop.xml` (monospace on meta).
- Issues 7-10 (P3) left, logged here only.
- Flagged for user: none.
- Shared file changed: `res/values/strings_prototype.xml`.
- Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` exit 0, 0 lint errors, unit tests green. Not re-verified on device; confirm status bar, overflow target, and scroll reset in the final sweep.
