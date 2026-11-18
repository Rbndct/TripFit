# Plan: Combine the two frontend prototypes

## Progress (updated 2026-11-17)

| Step | Status | Commit |
|---|---|---|
| 1 Merge map | done | 7c29337 |
| 2 Persistent store | done | 2f480f7 |
| 3 Single entry flow | done | aa277b6 |
| 4 Itinerary planning logic | done | 4a41e48 |
| 5 Budget and currency | done | fb2da5c |
| 6-12 | not started | |

Next: step 6 (Discover and suggestions catalog; fold `data/OfflineData` in too). Step 5 notes: one rates table
(`CurrencyMock` behind `CurrencyRates`, `RoutePreview.convert` and the export footer delegate to it); per-stop cost,
currency and category live in the store's `ItemDetails` keyed by item id; planned and logged are never summed;
daily mode applies category limits per day (`BudgetSettings#limitFor(category, days)`); `AmountRules` validates
amounts. Review fixes applied; `ItemDetails.costCurrency` null-backfill skipped (no trip context). Known gaps: stops
added from Discover/Suggestions carry no cost until step 6; planned days use max(meta.days, maxDay) while limits use
meta.days. 234 unit tests, 0 lint errors; androidTest compile-checked only.

## Situation (2026-11-16)

`feat/ui-prototype` contains two complete, separate frontends:

- **Ours** (root package): `HomeActivity` → `MainActivity` (Itinerary) → Stop Detail, Optimize, Budget, Suggestions,
  Discover, Calendar Sync, Summary, Group, Journal, Profile. Trips and stops are saved in Room; every screen went
  through the impeccable critique pass and follows `DESIGN.md`. Other state is session-only (`PrototypeStore`).
- **Kevin's** (`prototype/` package): `TripsActivity` → `PlannerActivity` → Stop Editor, Discover, Budget, Weather,
  Group, Journal, Summary. Everything is saved in SharedPreferences + Gson (`DraftStore`/`DraftTrip`). Stronger logic:
  per-leg buffers, booking-conflict warnings, day balancing, per-stop cost and currency, `.ics` export, file-picker
  saves, persistent journal photos, a Weather screen.

`LoginActivity` now opens Kevin's `TripsActivity`, so **our screens are unreachable from the app** even though they
are still in the code. The two flows do not share data.

## Direction

Keep **our screens** as the single user flow (they hold the reviewed design and Room data) and port **Kevin's
logic, persistence and extra features** into them, then delete the duplicate screens. If the team prefers the
opposite (Kevin's screens as the base), stop after step 1 and re-plan.

## Rules for every step

- One commit per step, conventional message, **no `Co-Authored-By` or other Claude attribution**.
- Refinement only: keep every Named Rule in `DESIGN.md` (fixed navy shell, monospace for data only, 16dp outlined
  cards, touch targets >= 48dp, strings in resources, colors in both `values` and `values-night`).
- Java 11, no new dependencies (Gson is already present through Retrofit's converter).
- Do not delete any of Kevin's code before step 10; port first, remove last.
- **Nothing Kevin built may be lost.** Step 1's merge map must end with a checklist of every behaviour in his
  screens and in `docs/Frontend-Prototype-Coverage.md` (including buffers, booking-conflict warnings, day balancing,
  per-stop cost and currency, currency calculator, editable weather forecast, `.ics` export, file-picker saves,
  persistent journal photos, companions and join code, startup without Supabase, map tap-to-pin). Each later step
  ticks the items it ports. Step 10 may only delete a screen once every checklist item that screen provided is
  ticked and reachable in our UI; any unticked item blocks the deletion and is reported to the user.
- His logic classes (`RoutePreview`, `TripExport`, `DemoCatalog`) and his tests are kept and merged, not rewritten.
- The app name is **Trippings**.
- Validation after each step: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug` with 0 lint errors.
- Before step 2, tell Kevin the plan so he does not keep building on the screens that step 10 removes.

## Step 1: Merge map: decide per feature what to port

Read both implementations (ours: root-package Activities + prototype/PrototypeStore; Kevin's: prototype/*Activity, DraftStore, DraftTrip, RoutePreview, TripExport, DemoCatalog) and docs/Frontend-Prototype-Coverage.md. Write docs/Prototype-Merge-Map.md listing, per feature, the behaviours of Kevin's version that ours lacks and where each will live.

**Acceptance:** docs/Prototype-Merge-Map.md covers all 14 backlog features; every Kevin-only behaviour has a target file in our flow; open questions for the team are listed.

## Step 2: Persistent store

Make prototype/PrototypeStore persistent using Kevin's DraftStore approach (SharedPreferences + Gson, one JSON document per trip id) so expenses, budget, leg modes, journal, votes and sync status survive app restarts. Keep its public API; add what the merge map needs (per-stop cost and currency, members, weather).

**Acceptance:** Unit tests cover save and reload round trip; data survives process death; existing 37+ unit tests still pass.

## Step 3: Single entry flow

LoginActivity currently opens prototype.TripsActivity, which orphans HomeActivity and every critiqued screen. Route Login back to HomeActivity and make NewTripActivity the only trip-creation form, porting any field TripSetupActivity has that it lacks.

**Acceptance:** Login opens HomeActivity; no code path starts prototype.TripsActivity, TripSetupActivity or PlannerActivity; compile and tests pass.

## Step 4: Itinerary planning logic

Merge Kevin's RoutePreview into our RouteMath, ScheduleMath and TravelMode: buffer before each incoming leg, overlapping-booking warnings, balancing flexible stops across days, current vs preview travel totals. Surface them in MainActivity, StopDetailActivity and OptimizeActivity; persist buffer in Room or the store.

**Acceptance:** RoutePreviewTest cases pass against the merged classes; booked stops never move; conflicts are shown in the itinerary; no duplicate route logic remains.

## Step 5: Budget and currency

Port from Kevin's BudgetActivity and DraftTrip into our BudgetActivity, BudgetSetupActivity and StopDetailActivity: planned cost with original currency per stop, planned costs kept separate from logged expenses, a currency calculator. Merge his sample rates and our CurrencyMock into one rates table.

**Acceptance:** Tracker shows planned and actual separately; each expense and stop keeps its original currency; a single rates table remains; unit tests cover conversion and totals.

## Step 6: Discover and suggestions catalog

Merge Kevin's DemoCatalog and our MockData into one catalog (Tokyo and Manila; places with coordinates, suggested duration, price). Use it in DiscoverActivity, SuggestionsActivity and AddDestinationActivity offline search; skip places already in the itinerary; add picks to the selected day.

**Acceptance:** Only one catalog class remains; Discover and Suggestions never offer a stop already planned; picks land on the chosen day; tests pass.

## Step 7: Weather screen

Add a WeatherActivity to our flow (extends ScreenActivity, navy toolbar, item_row cards) ported from Kevin's prototype.WeatherActivity: per-day sample forecast the user can change, outdoor conflicts, swap flexible stops for indoor alternatives, bookings protected. The Itinerary rain banner and overflow menu open it.

**Acceptance:** Weather is reachable from Itinerary; swapping updates Room stops; booked stops are never swapped; forecast choice persists.

## Step 8: Calendar and export

Bring Kevin's TripExport into CalendarSyncActivity and SummaryActivity: export a real .ics file with correct time zones and escaping, and save PDF or .ics through Android's file picker (ACTION_CREATE_DOCUMENT) alongside the existing share actions. Keep our TripPdfWriter or his PDF code, not both.

**Acceptance:** TripExportTest passes; .ics opens in a calendar app; PDF saves through the file picker; one PDF writer remains.

## Step 9: Group and journal

Port from Kevin's GroupActivity and JournalActivity into ours: add and remove local companions, sample join code, cost split over real members; journal photos picked with OpenDocument and takePersistableUriPermission so they survive restarts, entries editable, day or stop attachment.

**Acceptance:** Companions and votes persist; journal photo still loads after app restart; entries can be edited; tests pass.

## Step 10: Remove duplicate screens

After steps 2 to 9, delete Kevin's duplicate screens and state (prototype/TripsActivity, TripSetupActivity, PlannerActivity, StopEditorActivity, DiscoverActivity, BudgetActivity, WeatherActivity, GroupActivity, JournalActivity, SummaryActivity, PrototypeActivity, DraftStore, DraftTrip) and their manifest entries. Rewrite PrototypeDeviceTest against the unified screens.

**Acceptance:** No class in prototype/ extends Activity; manifest lists each screen once; assembleDebug, unit tests and lint pass with 0 errors.

## Step 11: Naming and docs

Make the app name consistent everywhere (Kevin renamed it to Trippings in strings.xml and settings.gradle.kts; README, DESIGN.md and PRODUCT.md still say TripFit; use the name the team confirms in the merge map). Update README status table, docs/Frontend-Prototype-Coverage.md and DESIGN.md to describe the single combined flow.

**Acceptance:** One app name across strings, README and docs; coverage doc paths point at existing classes; README status table reflects persistent storage.

## Step 12: Final verification

Review the combined branch end to end: run assembleDebug, testDebugUnitTest and lintDebug, then walk the flow on the TripFit_Lite emulator (python3 .claude/plans/tools/capture.py for screenshots) following the walkthrough in docs/Frontend-Prototype-Coverage.md. Report regressions against .impeccable/critique/*.md fixes.

**Acceptance:** Build, tests and lint pass with 0 errors; every backlog feature reachable from Login in one flow; no critique fix regressed; findings written to docs/Prototype-Merge-Map.md.

