# UI Critique Pass — Harness Queue

Persistent state for the screen-by-screen impeccable critique + fix pass. Every sub-agent reads
this file first; the orchestrator updates it after every stage. Survives session boundaries —
to resume, open a session in this repo and say "continue the UI critique pass".

## Status: DONE — all 17 screens committed (16d74c7..f9f0b7b); step 18 sweep + DESIGN.md committed. Only the on-device items under 'Not yet verified' remain

## Rules (all agents)

- Mode is **Refinement** (impeccable): keep the boarding-pass identity and every Named Rule in
  `DESIGN.md` (fixed navy shell never inverts, monospace only for data, one hero card, 16dp cards).
  A fix that changes the visual world is FLAGGED, not applied.
- Material 3 governs structure; touch targets >= 48dp; edge-to-edge insets; dark theme first-class;
  no clipping at font scale 1.3.
- Fix agents touch only their screen's files. Any change to a shared file goes in the
  Shared-Changes Log below, with the reason, before the agent finishes.
- Shared files: `res/layout/view_toolbar.xml`, `res/layout/item_row.xml`, `ui/RowAdapter.java`,
  `ui/UiRow.java`, `ScreenActivity.java`, `res/values/colors.xml`, `res/values-night/colors.xml`,
  `res/values/strings*.xml`, `res/values/dimens.xml`, `res/values/themes.xml`.
- Validation after every fix: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug`
  (0 lint errors, all tests green).
- One screen at a time. Never two fix agents in flight.

## Pipeline (LEAN — chosen 2026-11-12)

Batch stage (orchestrator, once):
1. Boot Pixel_3a headless, memory capped. Install debug APK.
2. For every screen: launch via `DebugLauncherActivity` → screenshots light / dark / font 1.3x
   + uiautomator dump → `.impeccable/review/<slug>-{light,dark,font}.png`, `<slug>.xml`.
3. Run `native_checks.py` over the dumps → `.impeccable/review/<slug>-checks.txt`
   (clickables < 48dp, image controls with no label, views clipped by the screen edge).
4. Shut the emulator down.

Per screen (sequential, one Sonnet agent each):
5. Agent reads this file + DESIGN.md + PRODUCT.md + the screen's source + its 3 screenshots +
   checks file → writes critique to `.impeccable/critique/<slug>.md`
   (first line: `⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)`)
   → applies P0/P1 fixes → logs shared-file changes → runs validation.
6. Orchestrator: re-run validation, commit `fix(ui): <screen> critique pass`, update the queue.

Final stage:
7. Batch re-capture of all screens (one emulator session) → regression review → DESIGN.md update.

## Queue

| # | Screen | Key files | Status | Critique | Commit |
|---|--------|-----------|--------|----------|--------|
| 0 | Phase 0: capture harness (debug launcher + capture script) | `app/src/debug/` | done (re-capture home + itinerary before steps 2 and 4) | — | launcher committed |
| 1 | Login | `LoginActivity`, `activity_login.xml` | done (3 flagged) | `.impeccable/critique/login.md` | 16d74c7 |
| 2 | Home | `HomeActivity`, `activity_home.xml`, `item_trip_card_*.xml`, `TripAdapter` | done (3 flagged) | `.impeccable/critique/home.md` | 6bbabd0 |
| 3 | New Trip | `NewTripActivity`, `activity_new_trip.xml` | done (2 flagged) | `.impeccable/critique/new-trip.md` | f8b9929 |
| 4 | Itinerary | `MainActivity`, `activity_main.xml`, `item_stop.xml`, `ItineraryAdapter`, `menu_itinerary.xml` | done | `.impeccable/critique/itinerary.md` | dbff4aa |
| 5 | Add Destination | `AddDestinationActivity`, `activity_add_destination.xml`, `item_place_result.xml` | done (2 flagged) | `.impeccable/critique/add-destination.md` | 51d76db |
| 6 | Map Picker | `MapPickerActivity`, `activity_map_picker.xml` | done (1 flagged) | `.impeccable/critique/map-picker.md` | 09301a9 |
| 7 | Stop Detail | `StopDetailActivity`, `activity_stop_detail.xml` | done (2 flagged) | `.impeccable/critique/stop-detail.md` | 60ffddc |
| 8 | Optimize | `OptimizeActivity`, `activity_optimize.xml` | done (1 flagged) | `.impeccable/critique/optimize.md` | c34cf15 |
| 9 | Discover | `DiscoverActivity`, `activity_discover.xml` | done (3 flagged) | `.impeccable/critique/discover.md` | 0320b53 |
| 10 | Suggestions | `SuggestionsActivity`, `activity_suggestions.xml` | done (2 flagged) | `.impeccable/critique/suggestions.md` | ff5a538 |
| 11 | Budget (+ Add Expense dialog) | `BudgetActivity`, `activity_budget.xml`, `item_category.xml`, `AddExpenseDialog`, `dialog_add_expense.xml` | done (1 flagged) | `.impeccable/critique/budget.md` | 8e87d7a |
| 12 | Budget Setup | `BudgetSetupActivity`, `activity_budget_setup.xml` | done (1 flagged) | `.impeccable/critique/budget-setup.md` | 2f296a4 |
| 13 | Calendar Sync | `CalendarSyncActivity`, `activity_calendar_sync.xml` | done (2 flagged) | `.impeccable/critique/calendar-sync.md` | 3fa3814 |
| 14 | Trip Summary | `SummaryActivity`, `activity_summary.xml` | done (3 flagged) | `.impeccable/critique/summary.md` | 9eeb85d |
| 15 | Group Trip | `GroupActivity`, `activity_group.xml` | done (2 flagged) | `.impeccable/critique/group.md` | 64c3b34 |
| 16 | Journal | `JournalActivity`, `activity_journal.xml`, `dialog_add_journal.xml` | done (1 flagged) | `.impeccable/critique/journal.md` | fb1130a |
| 17 | Profile | `ProfileActivity`, `activity_profile.xml`, `item_setting.xml` | done (1 flagged) | `.impeccable/critique/profile.md` | f9f0b7b |
| 18 | Final regression sweep + DESIGN.md update | all | done (dialogs/rotation/PDF unverified on device) | see Run Log | docs commit |

Status values: pending → capturing → critiquing → fixing → verifying → done | flagged | blocked

## Shared-Changes Log

_(append: date · screen · file · change · why)_

2026-11-15 · new-trip · `ScreenActivity.java` · added `applyShellSystemBars()` (navy status bar, light icons) called from `setScreen` · light-mode status icons were dark on navy; every navy-toolbar screen now inherits Home's fix, so later screens need not add their own
2026-11-15 · new-trip · `res/values/strings_prototype.xml` · added `cd_pick_dates`, `field_dates_helper` · date-field affordance and empty-state helper text
2026-11-15 · itinerary · `res/values/strings_prototype.xml` · replaced strings `day_summary`, `weather_banner`, `weather_swapped` with plurals of the same names; added `state_selected` · "1 stops" copy bug and day-tab selected-state description for TalkBack

2026-11-15 · add-destination · `res/values/strings.xml` · added `error_day_invalid`, `label_search_city` · day >= 1 validation and the city-chip caption
2026-11-15 · map-picker · `res/values/strings.xml` · reworded `map_picker_marker_hint`; added `map_picker_map_cd`, `map_picker_coords`, `map_picker_finding` · persistent hint, TalkBack map label, coordinate readout, geocode progress label
2026-11-16 · stop-detail · `res/values/strings.xml` · added `error_duration_invalid` · duration must be 1 minute or more (0 was accepted)
2026-11-16 · optimize · `res/values/strings_prototype.xml` · removed `optimize_day_detail`; `optimize_order` now takes (names, beforeKm); added `optimize_day_distance`, `optimize_order_same`, `optimize_summary_small`, `optimize_applied` · single new-distance trailing value, honest no-change copy, apply confirmation

2026-11-16 · discover · `ui/UiRow.java`, `ui/RowAdapter.java` · additive `withSelected()` (2dp primary outline + "Selected" state) and `withAccessibility()` (action content description, card click label); existing constructors and callers unchanged · selection state and swap action were invisible to sighted-state and TalkBack users
2026-11-16 · discover · `res/values/strings_prototype.xml` · `discover_added` is now a plural; added `discover_add_cd`, `discover_remove_cd`, `discover_swap_cd`, `discover_adding` · "Added 1 stops" copy, spoken labels, saving state

2026-11-16 · suggestions · `res/values/strings_prototype.xml` · `suggestions_empty` reworded; added `suggestions_show_all`, `suggestions_budget_used_up`, `suggestions_target_day`, `suggestion_subtitle_over`, `suggestion_add_cd`, `suggestion_add_failed` · empty-state action, overspent hint, target day, over-budget marker, spoken label, correct failure copy

2026-11-16 · budget · `res/values/strings_prototype.xml` · added `budget_no_total`, `category_row_cd`, `category_row_cd_over`; reworded `budget_empty_expenses` (names the Add expense action) · no-budget hero state, one TalkBack stop per category, actionable empty state

2026-11-16 · budget-setup · `res/values/strings_prototype.xml` · added `field_budget_hint_daily`, plural `budget_daily_total`, `budget_split_over`; reworded `budget_split_sum` ("trip budget") · per-day labeling, trip total, inline over-limit state
2026-11-16 · calendar-sync · `res/values/strings_prototype.xml` · `calendar_events_header`, `action_sync_calendar`, `calendar_success` are now plurals; removed `calendar_event_sub`; added plural `calendar_events_synced`, `calendar_event_day`, `calendar_event_time` · "1 events" copy, synced heading, day/time split into subtitle and monospace trailing

2026-11-16 · summary · `res/values/strings_prototype.xml` · added `summary_exporting`, `summary_spent_label`, `summary_spent_value`, `summary_spent_only`, plural `summary_stop_count`; reworded `pdf_failed` ("Try again") and `summary_empty` (names the next step) · busy label, data-in-slot budget lines, stop count, actionable failure/empty copy

2026-11-16 · group · `res/values/strings_prototype.xml` · `vote_count` is now a plural; added plural `vote_count_yours`, `vote_cd`, `vote_remove_cd`, `cd_copy_code`, `split_you`, `split_you_share`; `split_total` is now the label only (no amount) · "1 votes" copy, spoken vote labels, total split into sans label + monospace amount
2026-11-16 · journal · `res/values/strings_prototype.xml` · `journal_empty` reworded (names Add entry); `entry_stop_label` is now "Attach to"; removed `entry_saved`; added `journal_saved_to`, `photo_attached_change` · actionable empty state, destination-naming confirmation, photo replace affordance
2026-11-16 · profile · `res/values/strings_prototype.xml` · added `profile_row_cd`, plural `buffer_minutes_spoken`, `profile_action_choose`, `profile_action_open`, `profile_currency_set`, `profile_buffer_none_set`, plural `profile_buffer_set` · spoken row labels and setting-change confirmations

## Flagged for user

_(fixes that would change the visual world or need a product decision)_

- 2026-11-15 · login · Identity: the login screen has no boarding-pass cues (no navy carrier band, no ticket motif). Adding one changes the visual world; decide whether login should carry the shell.
- 2026-11-15 · login · Product: PRODUCT.md commits to Google Sign-In with no separate account system, but login leads with email/password and has no forgot-password or sign-up. Decide if Google becomes the primary action and the form is removed or demoted.
- 2026-11-15 · login · Recapture: `login-light.png` and `login-font.png` were blank; re-capture in the final sweep. Also verify status-bar icon contrast in dark.
- 2026-11-15 · home · Product: every trip card shows the placeholder subtitle "Day-by-day itinerary" (string `itinerary_subtitle`). Replace with real data (date range / destination) or drop it; needs HomeTripRow + HomeViewModel data and a copy decision.
- 2026-11-15 · home · Identity: greeting "Hi, Traveler" is generic; use the Google account's first name once sign-in is wired.
- 2026-11-15 · home · Final sweep: stub card (multi-trip) was reviewed from source only; its three-pill row (status, days, stops) may overflow at font scale >= 1.5. Verify and consider a wrapping Flow. Also verify the dark status bar on an API 35+ device.

- 2026-11-15 · new-trip · Product: city is prefilled with the first mock city (Tokyo), so "Pick a city" can never fire and the destination is an easy-to-miss default. Decide whether to start blank.
- 2026-11-15 · new-trip · Layout (P2, not applied): Create button floats mid-screen; consider anchoring it at the bottom outside the scroll view. Also unverified on device: helper text wrap at font 1.3.

- 2026-11-15 · add-destination · Product/Layout (P2): save success snackbar is shown then the activity finishes, so it is never seen; needs a result hand-off so Itinerary shows it. Also `add-destination-dark.png` was blank (launcher icon): re-capture in the final sweep, and confirm the chip row and navy shell in dark.
- 2026-11-15 · map-picker · Visual (P3): OSM tiles are bright in dark mode; an inverted/dimmed tile filter would suit the dark shell but changes the map's look, so decide. Also re-capture `map-picker-light` and `-font` (both blank) in the final sweep.
- 2026-11-16 · stop-detail · Product (P1): notes, buffer toggle and attached photo are session-only; "Save changes" silently discards them (no Room columns). Decide: persist them, or remove/relabel the controls. Also P2: Save sits below the fold at font 1.3 (consider a pinned bottom bar) and back with unsaved edits has no guard. Final sweep: re-capture stop-detail (shots predate the shared navy status-bar fix; status icons look dark in light).

- 2026-11-16 · optimize · Product (P2): after Apply the screen closes with only a platform Toast; Itinerary could show a Snackbar via a result extra (changes the launch contract with MainActivity). Final sweep: `optimize-font.png` was blank; re-capture and check the row at font 1.3.

- 2026-11-16 · discover · Product/Layout (P2): swap is only discoverable via a small caption and a tap on the whole card; consider a trailing swap icon button (changes the shared row design). Also P3: picks are not checked against stops already in the itinerary, and non-Tokyo/Manila trips silently fall back to Tokyo. The 4dp CardView check is a scroll-clip artifact (third gem row at the viewport edge), not a defect. Final sweep: re-capture light (predates status-bar fix) and check the selected outline.

- 2026-11-16 · suggestions · Product (P2): the Itinerary overflow menu opens Suggestions without a day, so picks always land on Day 1 (only Stop Detail passes `EXTRA_DAY`). Pass the selected day tab from `MainActivity.open`. Also P3: picks are not checked against stops already in the itinerary. Final sweep: re-capture (shots show the budget-spent empty state on the seed trip and predate the status-bar fix).

- 2026-11-16 · budget-setup · Product (P3): after Save the screen just closes (no confirmation; Budget re-renders). A Snackbar on Budget would need a result extra from BudgetSetupActivity (changes the launch contract). Final sweep: re-capture (shots predate the status-bar fix) and verify floating labels in sans-serif, currency prefix on limit fields, per-day helper, and rotation.

- 2026-11-16 · budget · Product (P3): expenses cannot be edited or deleted after logging. Final sweep: dialog was reviewed from source only (not in screenshots); re-capture budget light (predates status-bar fix) and check the dialog IME flow and rotation draft restore on device.
- 2026-11-16 · calendar-sync · Product (P3): the account card shows the placeholder `profile_email`, not a signed-in Google account, and the sync has no failure/retry state; both belong to the real Calendar API / Google Sign-In wiring. Final sweep: re-capture (shots predate the status-bar fix) and check the synced state, rotation mid-sync, and 1-event copy.

- 2026-11-16 · summary · Product (P3): PDF text export is plain lines (no wrapping, day headings not bold, names over ~80 chars would clip at the page edge) and Share summary and Export PDF carry the same content; decide if both actions are needed. Export on an empty itinerary still produces a budget-only PDF. Final sweep: re-capture (shots predate the status-bar fix and the new section cards), check font 1.3 wrapping of long stop names, and the over-budget note with a real overspent trip.

- 2026-11-16 · group · Product (P3): custom split shares are fixed mock values (40/30/20/10) and cannot be edited; decide if the prototype needs editable shares. Also P3: Share invite can stack two chooser dialogs on a fast double tap; member cards could be compacted. Final sweep: re-capture (shots predate the status-bar fix, split section never seen) and check the voted outline and font 1.3 vote row.

- 2026-11-16 · journal · Product (P3): entries are session-only with no timestamp, edit, delete or photo removal; the photo is a flag, not a stored image. Final sweep: re-capture (shots predate the status-bar fix), review the Add Entry dialog on device (not in screenshots), check keyboard open, rotation draft restore and the Snackbar above the FAB.
- 2026-11-16 · profile · Product (P3): account name/email are placeholders until Google Sign-In is wired; the Event reminders switch only shows a Snackbar (Google Calendar owns reminders); Dark mode and reminder state are not persisted across process death. Final sweep: re-capture and review the pickers, About and Sign-out dialogs on device (not in screenshots), and check that Snackbars clear the system gesture bar.

## Run Log

- 2026-11-12 · queue created (dry run). Environment: no adb device attached; host 8 GB RAM,
  ~58 MB free → Pixel_6 emulator thrashed last session (system_server + SystemUI ANRs).

- 2026-11-15 · batch capture finished on capped Pixel_3a (booted in 18s, no ANRs). 17 dumps + 51
  screenshots in `.impeccable/review/`; native checks written (`*-checks.txt`, 6 issues total).
  BLANK captures: `home-{light,dark,font}.png`, `itinerary-light.png` — either launch timing or a
  real render bug on Home; unverified. Re-capture: boot the emulator, then
  `python3 .claude/plans/tools/capture.py home itinerary` and
  `python3 .claude/plans/tools/native_checks.py 440 .impeccable/review/home.xml .impeccable/review/itinerary.xml`.
- Handoff: orchestrate commands in `.claude/plans/ui-critique-pass.orchestrate.md`.
- 2026-11-15 · step 1 Login done (16d74c7): 0 P0, 4 P1 fixed, P2 heading fixed, 3 flagged. Validation
  green (exit 0). Login light/font shots were blank, so critique used source + dark shot only.
  Next: step 2 Home, which needs the emulator re-capture of `home` + `itinerary` first.
- 2026-11-15 · re-captured `home` + `itinerary` (all 6 shots now render; the blanks were launch timing,
  not a Home render bug). Checks: home 0 issues, itinerary 1 (More options 40x48dp). Emulator shut down.
  Note for step 4: in itinerary-light the status bar strip is white above the navy header (Home's is navy).
- 2026-11-15 · step 2 Home done (6bbabd0): 0 P0, 2 P1 fixed (navy status bar + header takes top inset;
  hero perforation fill band), 2 P2 + 1 P3 fixed, 3 flagged. Validation green (exit 0). Home's status-bar
  fix (`applyShellSystemBars()` in HomeActivity) is the pattern to reuse for Itinerary in step 4.
  Not visually re-verified on device; confirm the perforation and status bar in the final sweep.

- 2026-11-15 · step 3 New Trip done (f8b9929): 0 P0, 5 P1 fixed, 2 P2 fixed, 2 flagged. Validation green (exit 0).
  SHARED CHANGE: `ScreenActivity.setScreen()` now forces the navy status bar + light icons for all 12 ScreenActivity
  screens (everything except Login, Home, Itinerary/MainActivity). Later agents: do NOT re-add per-screen status-bar fixes
  for those; judge status bar from a fresh capture. Itinerary (MainActivity) is not a ScreenActivity, so step 4 still needs
  Home's `applyShellSystemBars()` pattern.

- 2026-11-15 · step 4 Itinerary done (dbff4aa): 0 P0, 4 P1 + 2 P2 fixed, 4 P3 left in critique only, 0 flagged.
  Validation green (exit 0). Plural conversion of day_summary / weather_banner / weather_swapped verified at all 3 call sites.
  Final sweep must visually confirm: Itinerary status bar (no white strip), 48dp overflow button, scroll-to-top on day tab.

- 2026-11-15 · step 5 Add Destination done (51d76db): 0 P0, 7 P1 fixed, 1 P2 fixed + 1 flagged, 1 P3 skipped. Validation green (exit 0).
  STRUCTURAL: AddDestinationActivity now extends ScreenActivity and includes view_toolbar (was AppCompatActivity with its own toolbar).
  Reviewed: tripId comes from ScreenActivity (same EXTRA_TRIP_ID), chip selectionRequired is compatible with the programmatic check.
  Final sweep: re-capture add-destination-dark (was blank) and light (chips not loaded yet). Snackbar-before-finish flagged.

- 2026-11-15 · step 6 Map Picker done (09301a9): 1 P0 + 7 P1 + 3 P2 fixed, 1 flagged (OSM tiles bright in dark). Validation green (exit 0).
  STRUCTURAL: MapPickerActivity now extends ScreenActivity. Reviewed: EXTRA_RESULT_* contract with AddDestination unchanged;
  confirm button cannot stick disabled (geocoder returns null on failure, callback always finishes).
  Final sweep: re-capture map-picker-light + -font (blank); confirm tap-to-place + drag coexist on a device.
  Minor (not fixed): map_picker_coords uses default-locale decimal formatting.

- 2026-11-16 · step 7 Stop Detail done (60ffddc): 0 P0, 4 P1 + 3 P2 fixed, 1 P1 + 1 P2 flagged/not applied. Validation green (exit 0).
  Agent was interrupted by the weekly rate limit mid-read (no partial edits) and resumed via SendMessage after the reset.
  Final sweep: re-capture stop-detail (shots predate the shared status-bar fix); check Save below the fold at font 1.3.

- 2026-11-16 · step 8 Optimize done (c34cf15): 0 P0, 5 P1 + 4 P2 fixed, 1 flagged (Toast only, no Snackbar on Itinerary). Validation green (exit 0).
  BEHAVIOR CHANGE: days are queued only when the new order is shorter by > 0.05 km; Apply disabled when nothing queued.
  Known minor edge (not fixed): after a failed write + immediate retry, a late callback from the first attempt can decrement
  the old counter and finish() early. Needs failure + fast retry; cosmetic. optimize-font.png was blank, re-capture in sweep.

- 2026-11-16 · step 9 Discover done (0320b53): 0 P0, 6 P1 + 1 P2 fixed, 3 flagged. Validation green (exit 0).
  The 'NO_LABEL / 361x4dp CardView' check was a scroll-clip artifact (third row cut off at the viewport edge), not a defect.
  SHARED: UiRow gained additive withSelected()/withAccessibility(); RowAdapter binds them for MaterialCardView rows. Orchestrator
  review fixed a truncating (int) cast in RowAdapter.bindSelected to Math.round so unselected rows keep the XML's 1dp (3px at
  2.75x) outline on ALL shared-row screens (Suggestions, Calendar Sync, Journal, Budget, Summary, Group, Optimize).
  Later agents on those screens: selected-row support exists (UiRow.withSelected); reuse it, do not re-add card stroke code.
  Final sweep: check shared rows still look identical to before on Optimize/Budget/Summary/Group/Journal/Calendar Sync.
- 2026-11-16 · step 10 Suggestions done (pending commit): 0 P0, 7 P1 + 1 P2 fixed, 1 flagged. Validation green.
  Behavior change: Add now shows Adding... then Added (selected outline), ignores repeat taps; tab and added names survive rotation.
  Final sweep: confirm the empty-state "Show all places" button and the over-budget subtitle on device.

- 2026-11-16 · step 10 Suggestions done (ff5a538): 0 P0, 7 P1 + 1 P2 fixed, 2 flagged. Validation green (exit 0).
  Reused UiRow.withSelected/withAccessibility with no change to the shared classes. Orchestrator check: RowAdapter sets no click
  listener when onAction is null, so the 'Added'/'Adding...' buttons cannot crash. Screenshots showed only the empty Food state;
  re-capture after the 'Show all places' fix in the final sweep. Flagged: Itinerary menu opens Suggestions without EXTRA_DAY (picks land on Day 1).

- 2026-11-16 · step 11 Budget done (8e87d7a): 0 P0, 5 P1 + 4 P2 fixed, 1 flagged (no edit/delete of logged expenses). Validation green (exit 0).
  AddExpenseDialog gained an additive show(activity, tripId, draft, onAdded) overload + draftOf(); its only caller is BudgetActivity.
  Dialog reviewed from source only (not in screenshots). Final sweep: open Add Expense on a device, rotate with text typed, check Done submits.

- 2026-11-16 · step 12 Budget Setup done (2f296a4): 0 P0, 7 P1 + 2 P2 fixed, 1 flagged (no confirmation after Save). Validation green (exit 0).
  LOGIC FIX: split total vs per-day amount mismatch (Save used trip total, live sum used the per-day amount); both use tripTotal() now.
  Orchestrator check: all amount fields are inputType=number (digits only), so per-keystroke Double.parseDouble cannot throw.
  Final sweep: label typeface, currency prefix, per-day helper and rotation restore need an on-device look.

- 2026-11-16 · step 13 Calendar Sync done (3fa3814): 0 P0, 6 P1 + 2 P2 fixed, 2 flagged (placeholder account email, no failure/retry state). Validation green (exit 0).
  Orchestrator fix: gated the result line on eventsLoaded so a restored result cannot flash '0 events added' before data loads.
  Note: user committed README + design artifacts/captures (37ee93e, 512a785) between steps 12 and 13; history is linear, nothing of ours disturbed.

- 2026-11-16 · step 14 Trip Summary done (9eeb85d): 0 P0, 7 P1 + 2 P2 fixed, 3 flagged (PDF text clipping/unbolded headings, Export+Share both needed?, budget-only PDF on empty trip). Validation green (exit 0).
  STRUCTURAL: RecyclerView+RowAdapter replaced by a LinearLayout inside the existing NestedScrollView with item_summary_section/item_summary_line;
  orchestrator verified the container scrolls and the two buttons stay pinned. Async export drops results after finish/destroy.
  TripPdfWriter untouched. Final sweep: export a PDF on a device, check over-budget note colour, rotate mid-export.
  Note: user committed another README change (de4748c); history still linear.

- 2026-11-16 · step 15 Group Trip done (pending commit): 0 P0, 8 P1 + 2 P2 fixed, 2 P3 flagged. Validation green (exit 0).
  Scripted check TOUCH_TARGET 88x30dp on Vote was a false positive (first Vote is 88x48dp in group.xml; the 30dp node is a row clipped at the viewport). No shared layout change.
  Votes: tally now in the row subtitle (plural), voted rows get UiRow.withSelected + spoken labels. Total line is sans label + monospace amount.

- 2026-11-16 · step 15 Group Trip done (64c3b34): 0 P0, 8 P1 + 2 P2 fixed, 2 flagged (fixed mock custom shares; fast double-tap on Share invite stacks choosers). Validation green (exit 0).
  The scripted 'Vote 88x30dp' check was a FALSE POSITIVE: group.xml shows the first Vote button at 88x48dp and the 30dp node is the second row clipped by the
  bottom nav region (verified by the orchestrator from the dump). Same artifact as Discover's 361x4dp card. Shared item_row.xml untouched.
  Trade-off to note: vote counts moved from the monospace trailing slot into the sans subtitle. Final sweep: group split section was never captured.

- 2026-11-16 · step 16 Journal done (fb1130a): 0 P0, 8 P1 + 2 P2 fixed, 1 flagged (mock entries: session-only, no edit/delete/timestamp). Validation green (exit 0).
  Dialog + typed note/stop/photo survive rotation via the Budget draft pattern; orchestrator verified reopenPendingDraft() fires once only.
  Final sweep: open Add entry on a device, rotate with text typed, check Snackbar sits above the FAB; journal shots predate the status-bar fix.
- 2026-11-16 · step 17 Profile done (pending commit): 0 P0, 6 P1 + 3 P2 fixed, 1 P3 product flag. Validation green (exit 0).

- 2026-11-16 · step 17 Profile done (f9f0b7b): 0 P0, 6 P1 + 3 P2 fixed, 1 flagged (placeholder account, reminders switch only toasts). Validation green (exit 0).
  item_setting.xml untouched (shared with Stop Detail's day row); orchestrator confirmed no diff. Dark-mode switch now reads real uiMode.
  ALL 17 SCREENS COMMITTED (16d74c7 .. f9f0b7b). Remaining: step 18 final sweep = re-capture all screens, regression review, DESIGN.md update.

- 2026-11-16 · step 18 final sweep. FIRST ATTEMPT INVALID: two emulators were attached (the user's own TripFit_Lite on emulator-5554 and mine), so adb refused
  every command, then captures ran against the USER's emulator by mistake (APK reinstalled, animation scales set to 0, night/font toggled and reset, app force-stopped
  repeatedly; originals unknown) and later against my emulator while System UI was in an ANR dialog (all 17 dumps were the same ANR, 0 app nodes, so the
  first 'all clean' read was meaningless and was retracted). FIXES: capture.py now detects the ANR dialog and fails the capture; every adb call pinned with
  ANDROID_SERIAL after matching the AVD name; my emulator rebooted alone. VALID RESULT: all 17 captures True, dumps contain 16-68 app nodes each, scripted
  checks 0 issues on 15 screens + 2 clipped-row artifacts (Discover 361x4dp, Group Vote 88x30dp). Verified visually: Itinerary + Home status bars navy, Home hero
  perforation band gone, shared-row outline unchanged (Discover), Discover bottom clip, Suggestions empty state + Show all places. NOT viewed: the other screens'
  fresh screenshots were not individually reviewed, only their dumps/checks.
  NOT VERIFIED ON A DEVICE: Add Expense / Add journal entry / Profile dialogs, rotation behaviour, PDF export+share, Map Picker tap-to-place+drag.

  NOTE: the user rewrote history (reflog: 'strip Claude co-author trailers'), so every commit hash recorded above this line was replaced; the table and
  entries now use the CURRENT hashes. Trees were verified identical (old profile commit vs its rewritten twin).

## Decisions (answered 2026-11-12)

1. Screenshot source: **Pixel_3a emulator, headless, memory capped**
2. Autonomy: **self-paced in-session loop, auto-apply P0/P1**, stop only for DESIGN.md conflicts / product decisions
3. Agent models: **all Sonnet**
4. Commits: **baseline prototype commit first, then one commit per screen**
5. Pacing: in-session loop (no cron)
6. Pipeline shape: **Lean** (batch capture once + final re-capture, 1 agent per screen; reports carry the DEGRADED banner)
