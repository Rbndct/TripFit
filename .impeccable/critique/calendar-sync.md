⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Calendar Sync critique, 2026-11-16

Evidence: `calendar-sync-light/dark/font.png` (Pixel 3a emulator), checks file ("no native-check issues"). All three rendered. The light and font shots predate the shared status-bar fix (status icons look dark on navy); dark is correct.

## Design-specificity verdict
A tidy but generic list-and-button screen: account card, count heading, six outlined cards, one navy primary button. The shell and flat 16dp outlined cards keep it inside the TripFit world, and the single bottom action is right. But nothing about it says "itinerary becoming calendar events": the account card wears a clock icon (wrong metaphor, it is an account), times sit in a body-face sentence instead of the monospace data slot a boarding pass uses, and the screen forgets everything it did the moment it is rotated. Refinement mode: keep the structure, fix the semantics, state and copy.

## Nielsen heuristics
| # | Heuristic | Score | Key issue |
|---|-----------|-------|-----------|
| 1 | Visibility of system status | 1 | Sync result and in-flight state vanish on rotation; heading still said "will be created" after a sync; screen re-entry gave no sign it was already synced |
| 2 | Match system / real world | 2 | Clock icon for a Google account; "1 events" |
| 3 | User control and freedom | 3 | Back arrow; nothing destructive |
| 4 | Consistency and standards | 2 | Times not in the monospace data slot |
| 5 | Error prevention | 3 | Button disabled while syncing (guard present) |
| 6 | Recognition over recall | 3 | Event name, day and time visible per card |
| 7 | Flexibility and efficiency | 3 | One tap sync |
| 8 | Aesthetic and minimalist design | 3 | Clean; "0 events will be created" shown beside the empty state |
| 9 | Error recovery | 2 | No failure path (simulated); open-calendar failure uses a Snackbar, fine |
| 10 | Help and documentation | 3 | Re-sync label explains it updates, not duplicates |
| **Total** | | **25/40** | Acceptable, real state gaps |

## Cognitive load
Low: one decision, one button. Checklist fails: the heading wording ("will be created") contradicts the screen once events exist; with no events the heading, an empty message and a disabled "Sync 0 events" button all compete.

## Strengths
1. One primary action pinned at the bottom, outside the scroll area, so it is always reachable (font 1.3 shot).
2. Re-sync copy tells the user it updates rather than duplicates.
3. Dark theme correct; shell stays navy; cards flat 16dp outlined.

## Issues
1. P1 Sync state does not survive rotation: the result text and Open Calendar button are plain view state, and an in-flight simulated sync is cancelled in `onDestroy` and never resumed, leaving the button enabled. `CalendarSyncActivity.java` (old `startSync/finishSync`). Fix: keep `isSyncing` and `resultKind` fields, save them, re-run the delay on restore, draw everything from one `render()`.
2. P1 Counts are not plural ("Sync 1 events", "1 events will be created", "1 events added"). Strings `calendar_events_header`, `action_sync_calendar`, `calendar_success`. Fix: plurals, every call site via `getQuantityString`.
3. P1 Status wording is stale after sync: heading still says "will be created", and re-entering an already-synced trip shows no synced state or Open Calendar button. Fix: heading switches to "N events on your calendar" when synced; Open Calendar visible whenever synced.
4. P1 Account card uses the clock icon (`ic_schedule_24`), and its text block is `match_parent` beside a 40dp icon (overflow risk at long emails/font scale). `activity_calendar_sync.xml`. Fix: person icon, weighted text block, card focusable so TalkBack reads it as one item.
5. P1 Empty state shows "0 events will be created" plus a disabled "Sync 0 events" button; heading also flashed before data loaded. Fix: heading hidden when empty or not yet loaded; empty button reads "Sync to Google Calendar".
6. P1 Result is color/position only and silent to TalkBack. Fix: check icon on the result line, `accessibilityLiveRegion=polite`, spoken "Syncing…" when it starts, heading marked as a heading.
7. P2 Times are in the body face. Fix: day as subtitle, time range in the row's monospace trailing slot (data only).
8. P2 Sync and Open Calendar buttons lacked an explicit 48dp minimum. One-line `minHeight`.
9. P3 Account card shows the placeholder `profile_email`, not a real signed-in account (no auth backend). Flagged.
10. P3 No failure/retry state exists because the sync is simulated; add when the real Calendar API is wired.
11. P3 Events are not grouped under day headers; Day N is on every card. Acceptable at six rows.
12. P3 Stacked result + two buttons in the pinned bar take height in landscape / font 1.3 with many rows; not fixed.

## Fix log
- Issue 1 (P1) fixed: `CalendarSyncActivity.java` now holds `isSyncing`/`resultKind`, saves them in `onSaveInstanceState`, resumes an in-flight sync after rotation, and draws all state from `render()`; repeat taps ignored while syncing.
- Issue 2 (P1) fixed: `strings_prototype.xml` plurals for `calendar_events_header`, `action_sync_calendar`, `calendar_success` (all three call sites use `getQuantityString`; no other callers).
- Issue 3 (P1) fixed: new plural `calendar_events_synced`; heading and Open Calendar button follow `store.isSynced`.
- Issue 4 (P1) fixed: `activity_calendar_sync.xml` person icon, weighted text block, focusable card.
- Issue 5 (P1) fixed: heading/empty visibility gated on loaded data; empty button label uses existing `action_calendar`.
- Issue 6 (P1) fixed: new `drawable/ic_check_24.xml` on the result line, polite live region, `announceForAccessibility`, `setAccessibilityHeading`.
- Issue 7 (P2) fixed: rows use `UiRow.withTrailing` (day subtitle, monospace time); `calendar_event_sub` replaced by `calendar_event_day` and `calendar_event_time`.
- Issue 8 (P2) fixed: `minHeight=48dp` on both buttons.
- Issues 9-12 not fixed (9 flagged to the queue; 10-12 P3 notes).
- Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` passed (0 lint errors, tests green).
