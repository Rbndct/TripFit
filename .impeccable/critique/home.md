⚠️ DEGRADED: single-context (lean pipeline: one agent critiques and fixes)

# Critique: Home (`activity_home.xml`, `item_trip_card_hero.xml`, `item_trip_card_stub.xml`)

Evidence: `home-light.png`, `home-dark.png`, `home-font.png` (all render; single Tokyo Trip hero card), `home-checks.txt` (no native-check issues). Stub card reviewed from source only. Emulator evidence only (Pixel 3a, no edge-to-edge enforcement visible).

## Design-specificity verdict
Specific, not generic. The navy carrier band, perforation dots, barcode strip and monospace "2 DAYS · 6 STOPS" ledger read as a boarding pass rather than a stock Material list, and the fixed-shell and mono-means-data rules are honored. The weak spots are around the ticket, not in it: the system chrome (status bar) does not carry the shell in dark mode, the hero's lower body has an inconsistent strip, and the subtitle copy is a placeholder repeated on every card.

## Nielsen heuristics (0-4)
| # | Heuristic | Score | Key issue |
|---|-----------|-------|-----------|
| 1 | Visibility of system status | 3 | DRAFT/PLANNED derived from stop count; counts visible. No loading state (fine, local Room). |
| 2 | Match real world | 3 | Ticket metaphor is apt; "Hi, Traveler" is generic. |
| 3 | User control and freedom | 3 | System Back works; no way to delete/rename a trip from Home (out of scope). |
| 4 | Consistency and standards | 3 | Material FAB, cards, AppBar. Status bar breaks consistency in dark (#1). |
| 5 | Error prevention | n/a | No destructive actions on this screen. |
| 6 | Recognition over recall | 3 | Status + counts on-card; "Day-by-day itinerary" subtitle carries no information (#4). |
| 7 | Flexibility and efficiency | 2 | Single tap opens trip; no shortcuts needed at this scale. |
| 8 | Aesthetic and minimalist design | 3 | Strong hero; mismatched strip and cramped pill row dent it (#2, #3). |
| 9 | Error recovery | n/a | No error states reachable. |
| 10 | Help and documentation | n/a | Not applicable for a dashboard. |

Total 20/28 applicable (good, with fixable polish).

## Cognitive load
Low. One primary action (New Trip FAB), one hero, one list. Each card has 3 data points (name, status, counts). Stub card packs three pills in a row (status, days, stops) which is the densest spot and will wrap/clip at large font scales. Header greeting + subtitle + "Your Trips" is two heading levels on a very sparse screen, acceptable.

## Strengths
1. Hero ticket is a memorable, on-brand signature and fully token-driven (fixed navy, themed barcode and perforation).
2. Mono-for-data discipline is clean; counts and status read as ledger fields while titles stay Roboto.
3. Font scale 1.3 holds up: no clipping, FAB and header reflow cleanly; profile button is a proper 48dp target with a label.

## Issues
1. **P1 System bars / insets** (`home-dark.png` top strip; `activity_home.xml` header, `HomeActivity.java:27-31`). Status bar is painted with `colorPrimary` (dark: lavender `#AEB9D1`) with white icons: poor contrast and the fixed navy shell visibly breaks in dark mode. Separately, the root applies top padding 0 and the header relies on 24dp of padding, so on Android 15+ (edge-to-edge enforced, targetSdk 37) the greeting can collide with a taller status bar or cutout. Fix: set status bar to `ticket_carrier_navy` with light icons; apply the top inset to the AppBarLayout and reduce its own top padding to 16dp.
2. **P1 Hero card strip** (`home-light.png` y~655-695, `home-dark.png` same; `item_trip_card_hero.xml:50`). The perforation view is filled with flat `colorSurface` while the card body below carries the 2dp tonal-elevation tint, so a lighter (light) or darker (dark) band cuts across the ticket. Fix: drop the view's background so the stub shares the card's surface.
3. **P2 Status row cramped** (`home-light.png` y~695; `item_trip_card_hero.xml:60`). PLANNED pill touches the perforation row (no top padding). Fix: 4dp top padding (one line).
4. **P2 Placeholder subtitle** (`item_trip_card_hero.xml:78`, `item_trip_card_stub.xml:30`). "Day-by-day itinerary" repeats on every card and tells the user nothing. Needs real data (date range or destination) and a product choice; flagged, not changed.
5. **P2 AppBar lift** (`activity_home.xml:13`). M3 `AppBarLayout` defaults to `liftOnScroll`, which can swap the navy for a surface tint once the list scrolls (multiple trips). Fix: `app:liftOnScroll="false"` (one line). Unverified on device with several trips.
6. **P2 Stub chip row at large font** (`item_trip_card_stub.xml:36-76`). Three non-wrapping pills in a horizontal LinearLayout will overflow on narrow screens at font scale >= 1.5. Not fixed (needs a wrapping Flow, more than one line); revisit in final sweep.
7. **P3 Preview ref** (`activity_home.xml` `tools:listitem="@layout/item_trip_card"` pointed to a layout that no longer exists). Fixed (one line).
8. **P3 Greeting** "Hi, Traveler" is generic although PRODUCT.md commits to Google Sign-In (name available). Flagged for user.
9. **P3** RecyclerView inside NestedScrollView disables recycling; fine for a handful of trips, revisit if lists grow. No empty state added: trips cannot be deleted and Tokyo is seeded on first run, so an empty message would only flash during seeding.

## Fix log
- #1 fixed: `HomeActivity.java` (new `applyShellSystemBars()`: navy status bar, light icons; top inset applied to new `appbar_home` padding), `activity_home.xml` (id on AppBarLayout, header top padding `space_lg` to `space_md`).
- #2 fixed: `item_trip_card_hero.xml` (removed perforation `android:background`).
- #3 fixed: `item_trip_card_hero.xml` (`paddingTop` `space_xs` on the status row).
- #5 fixed: `activity_home.xml` (`app:liftOnScroll="false"`).
- #7 fixed: `activity_home.xml` (tools:listitem).
- #4, #6, #8, #9 not changed (flagged / deferred, see above).
- Shared files changed: none.
- Validation: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug -q` exit 0.
