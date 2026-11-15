⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Map Picker critique

Evidence: `map-picker-dark.png` rendered (map tiles loaded from cache, pin and confirm button visible). `map-picker-light.png` and `map-picker-font.png` are BLANK (launcher icon, launch timing), so light and 1.3x were judged from source only; the font-scale view of the bottom panel is unverified. `map-picker-checks.txt`: NO_LABEL ImageButton at [0,77][154,231] = the toolbar back arrow. Tile gaps (grey squares) in the dark shot are tile-load timing, not a defect.

## Design-specificity verdict
Before the fix this was a bare default: a `colorSurface` toolbar (near-black in dark, pale lavender status bar above it), osmdroid's stock green pin with a hand icon, and a full-width button. It had no connection to the boarding-pass world and nothing told the user how to place a pin. After the fix it takes the same navy carrier shell as every other ScreenActivity screen, uses a brand-tinted pin, and has a small "ticket stub" panel under the map (hint, monospace coordinates as data, one primary button). A map is a utility surface, so the world is carried by the shell and data type only; a translucent toolbar over the map was rejected because it would break the fixed-shell rule and hide map tiles behind chrome.

Shell decision: moved onto `ScreenActivity` (same as Add Destination). The screen is a toolbar plus a map in a column, not a true full-bleed map, so the shared shell, insets, status-bar treatment and labelled back arrow apply unchanged.

## Nielsen heuristics (0-4)
1. Visibility of system status: 1 before / 3 after. Confirm ran up to ~20s of reverse-geocoding with no feedback and a re-tappable button; no readout of where the pin was.
2. Match with real world: 3. "Drop a pin", "Use this location".
3. User control and freedom: 3. Back works, result contract is OK/cancel; pin reset on rotation before fix.
4. Consistency and standards: 1 before / 3 after. Only screen with a non-shell toolbar.
5. Error prevention: 2. Double-tap on confirm fired duplicate requests (fixed). Geocode failure falls back to "Pinned location" in the caller.
6. Recognition over recall: 1 before / 3 after. Drag requires a long-press in osmdroid and was explained only in an info bubble nobody would open.
7. Flexibility and efficiency: 3. Pinch/zoom, drag, and now tap-to-place.
8. Aesthetic and minimal design: 3. Minimal; stock pin was off-brand.
9. Error recovery: 2. Silent fallback name; acceptable.
10. Help and documentation: 1 before / 3 after (persistent one-line hint).

## Cognitive load
Single task, one decision, one action. The only load problem was discoverability of the gesture (hold-then-drag). Panel is now three items (hint, coordinates, button), well inside working-memory limits.

## Strengths
1. Minimal: one map, one primary action, matches the one-primary-action rule.
2. Result contract (lat, lng, name via Nominatim) is simple and respects the one-request-per-confirm usage policy.
3. Map starts centered on the chosen city at street-level zoom 15.

## Issues
1. P0 · Back arrow has no content description (checks.txt NO_LABEL; old `activity_map_picker.xml` toolbar). Fix: use shared `view_toolbar` include (`cd_back`).
2. P1 · Not on the navy shell: toolbar on `colorSurface`, status bar lavender with dark-theme toolbar (dark shot). Fix: extend `ScreenActivity`, `setScreen(...)`.
3. P1 · No edge-to-edge insets: confirm button sat on the gesture-bar edge (dark shot bottom), and on API 35+ content would run under the status bar. Fix: ScreenActivity insets.
4. P1 · Pin placement not discoverable: osmdroid drag needs long-press; the only hint was an info bubble shown on marker tap (`MapPickerActivity` `setTitle`). Fix: persistent hint text in the bottom panel, tap-to-place via `MapEventsOverlay`, info window removed.
5. P1 · Confirm gives no feedback while geocoding and can be tapped repeatedly (`confirmLocation`). Fix: disable button and show "Finding place…".
6. P1 · No readout of the chosen position. Fix: monospace coordinates line updated on drag and tap.
7. P1 · Pin position lost on rotation (reset to city center). Fix: save/restore in `onSaveInstanceState`.
8. P1 · OSM tile licence requires attribution; none shown. Fix: `CopyrightOverlay`.
9. P2 · Stock green hand-pin off-brand. Fix: `ic_map_pin_40.xml`, `colorPrimary` fill with white outline.
10. P2 · Map has no content description for TalkBack. Fix: `map_picker_map_cd`.
11. P2 · Confirm button default min height 40dp. Fix: `minHeight=56dp` (matches Login).
12. P3 · OSM tiles are bright in dark mode (glare against the dark shell). Not fixed: needs an inverted tile filter, a visual decision. Flagged.
13. P3 · Not verified: light and 1.3x font renders were blank; hint wraps to 2-3 lines at 1.3x by design (wrap_content panel).

## Fix log
- #1 back label: `activity_map_picker.xml` now includes `@layout/view_toolbar` (carries `cd_back`).
- #2, #3 shell + insets: `MapPickerActivity` extends `ScreenActivity`, calls `setScreen`; removed local toolbar wiring.
- #4 hint: new bottom panel hint (`map_picker_marker_hint` reworded), tap-to-place `MapEventsOverlay`, marker info window removed.
- #5 feedback: button disabled and relabelled `map_picker_finding` during reverse-geocode.
- #6 coordinates: `text_coordinates` (monospace) updated on tap, drag, drag end.
- #7 rotation: pin lat/lng saved and restored.
- #8 attribution: `CopyrightOverlay` added.
- #9 pin: new `res/drawable/ic_map_pin_40.xml`, set as marker icon.
- #10 map content description: `map_picker_map_cd`.
- #11 button min height 56dp.
- #12 flagged; #13 left (unverifiable).
- Result contract with AddDestinationActivity unchanged (extras, RESULT_OK, finish).
- Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` exit 0, 0 lint errors.
