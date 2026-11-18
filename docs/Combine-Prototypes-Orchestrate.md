# Plan-Orchestrate Result

**Plan**: `docs/Combine-Prototypes-Plan.md` and `docs/Prototype-Merge-Map.md`
**Lang**: `java`
**ECC mode**: `plugin`
**Steps**: 12
**Scope**: all

## Progress (updated 2026-11-18)

| Step | Status | Commit |
|---|---|---|
| 1 Merge map | done | 7c29337 |
| 2 Persistent store | done | 2f480f7 |
| 3 Single entry flow | done | aa277b6 |
| 4 Itinerary planning logic | done | 4a41e48 |
| 5 Budget and currency | done | fb2da5c |
| 6 | done | 99a8050 |
| 7 | done | a704ad2 |
| 8 | done | |
| 9-12 | not started | |

Next is Step 9 (Group and journal). Step 5 notes: one rates table
(`CurrencyMock` behind `CurrencyRates`, `RoutePreview.convert` and the export footer delegate to it); per-stop cost,
currency and category live in the store's `ItemDetails` keyed by item id; planned and logged are never summed;
daily mode applies category limits per day (`BudgetSettings#limitFor(category, days)`); `AmountRules` validates
amounts. Review fixes applied; `ItemDetails.costCurrency` null-backfill skipped (no trip context). Known gaps: stops
added from Discover/Suggestions carry no cost until step 6; planned days use max(meta.days, maxDay) while limits use
meta.days. 234 unit tests, 0 lint errors; androidTest compile-checked only.

## Steps overview

| # | Title | Tags | Chain |
|---|---|---|---|
| 1 | Merge map: decide per feature what to port | design, plan | `ecc:planner,ecc:architect` |
| 2 | Persistent store | impl | `ecc:tdd-guide,ecc:java-reviewer` |
| 3 | Single entry flow | impl | `ecc:tdd-guide,ecc:java-reviewer` |
| 4 | Itinerary planning logic | refactor, impl | `ecc:architect,ecc:tdd-guide,ecc:java-reviewer` |
| 5 | Budget and currency | impl | `ecc:tdd-guide,ecc:java-reviewer` |
| 6 | Discover and suggestions catalog | refactor, impl | `ecc:architect,ecc:tdd-guide,ecc:java-reviewer` |
| 7 | Weather screen | impl | `ecc:tdd-guide,ecc:java-reviewer` |
| 8 | Calendar and export | impl | `ecc:tdd-guide,ecc:java-reviewer` |
| 9 | Group and journal | impl | `ecc:tdd-guide,ecc:java-reviewer` |
| 10 | Remove duplicate screens | refactor | `ecc:architect,ecc:refactor-cleaner,ecc:java-reviewer` |
| 11 | Naming and docs | docs | `ecc:doc-updater` |
| 12 | Final verification | review | `ecc:java-reviewer,ecc:code-reviewer` |

---

## Step 1 — Merge map: decide per feature what to port

**Intent**: Produce the merge map only; no code changes.
**Tags**: design, plan
**Chain rationale**: `ecc:architect` closes the chain; chain follows the default for the primary tag.

```bash
/ecc:orchestrate custom "ecc:planner,ecc:architect" "[Plan: .claude/plans/combine-prototypes.md#step-1] Read both implementations (ours: root-package Activities + prototype/PrototypeStore; Kevin's: prototype/*Activity, DraftStore, DraftTrip, RoutePreview, TripExport, DemoCatalog) and docs/Frontend-Prototype-Coverage.md. Write docs/Prototype-Merge-Map.md listing, per feature, the behaviours of Kevin's version that ours lacks and where each will live. Acceptance: docs/Prototype-Merge-Map.md covers all 14 backlog features; every Kevin-only behaviour has a target file in our flow; open questions for the team are listed"
```

## Step 2 — Persistent store

**Intent**: Session-only state becomes saved state.
**Tags**: impl
**Chain rationale**: `ecc:java-reviewer` closes the chain; chain follows the default for the primary tag.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-2] Make prototype/PrototypeStore persistent using Kevin's DraftStore approach (SharedPreferences + Gson, one JSON document per trip id) so expenses, budget, leg modes, journal, votes and sync status survive app restarts. Keep its public API; add what the merge map needs (per-stop cost and currency, members, weather). Acceptance: Unit tests cover save and reload round trip; data survives process death; existing 37+ unit tests still pass"
```

## Step 3 — Single entry flow

**Intent**: One app flow: Login, Home, Itinerary.
**Tags**: impl
**Chain rationale**: `ecc:java-reviewer` closes the chain; chain follows the default for the primary tag.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-3] LoginActivity currently opens prototype.TripsActivity, which orphans HomeActivity and every critiqued screen. Route Login back to HomeActivity and make NewTripActivity the only trip-creation form, porting any field TripSetupActivity has that it lacks. Acceptance: Login opens HomeActivity; no code path starts prototype.TripsActivity, TripSetupActivity or PlannerActivity; compile and tests pass"
```

## Step 4 — Itinerary planning logic

**Intent**: One routing and scheduling implementation.
**Tags**: refactor, impl
**Chain rationale**: `ecc:java-reviewer` closes the chain; chain follows the default for the primary tag.

```bash
/ecc:orchestrate custom "ecc:architect,ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-4] Merge Kevin's RoutePreview into our RouteMath, ScheduleMath and TravelMode: buffer before each incoming leg, overlapping-booking warnings, balancing flexible stops across days, current vs preview travel totals. Surface them in MainActivity, StopDetailActivity and OptimizeActivity; persist buffer in Room or the store. Acceptance: RoutePreviewTest cases pass against the merged classes; booked stops never move; conflicts are shown in the itinerary; no duplicate route logic remains"
```

## Step 5 — Budget and currency

**Intent**: One budget model.
**Tags**: impl
**Chain rationale**: `ecc:java-reviewer` closes the chain; chain follows the default for the primary tag.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-5] Port from Kevin's BudgetActivity and DraftTrip into our BudgetActivity, BudgetSetupActivity and StopDetailActivity: planned cost with original currency per stop, planned costs kept separate from logged expenses, a currency calculator. Merge his sample rates and our CurrencyMock into one rates table. Acceptance: Tracker shows planned and actual separately; each expense and stop keeps its original currency; a single rates table remains; unit tests cover conversion and totals"
```

## Step 6 — Discover and suggestions catalog

**Intent**: One sample catalog.
**Tags**: refactor, impl
**Chain rationale**: `ecc:java-reviewer` closes the chain; chain follows the default for the primary tag.

```bash
/ecc:orchestrate custom "ecc:architect,ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-6] Merge Kevin's DemoCatalog and our MockData into one catalog (Tokyo and Manila; places with coordinates, suggested duration, price). Use it in DiscoverActivity, SuggestionsActivity and AddDestinationActivity offline search; skip places already in the itinerary; add picks to the selected day. Acceptance: Only one catalog class remains; Discover and Suggestions never offer a stop already planned; picks land on the chosen day; tests pass"
```

## Step 7 — Weather screen

**Intent**: Replaces the banner dialog.
**Tags**: impl
**Chain rationale**: `ecc:java-reviewer` closes the chain; chain follows the default for the primary tag.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-7] Add a WeatherActivity to our flow (extends ScreenActivity, navy toolbar, item_row cards) ported from Kevin's prototype.WeatherActivity: per-day sample forecast the user can change, outdoor conflicts, swap flexible stops for indoor alternatives, bookings protected. The Itinerary rain banner and overflow menu open it. Acceptance: Weather is reachable from Itinerary; swapping updates Room stops; booked stops are never swapped; forecast choice persists"
```

## Step 8 — Calendar and export

**Intent**: One export implementation.
**Tags**: impl
**Chain rationale**: `ecc:java-reviewer` closes the chain; chain follows the default for the primary tag.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-8] Bring Kevin's TripExport into CalendarSyncActivity and SummaryActivity: export a real .ics file with correct time zones and escaping, and save PDF or .ics through Android's file picker (ACTION_CREATE_DOCUMENT) alongside the existing share actions. Keep our TripPdfWriter or his PDF code, not both. Acceptance: TripExportTest passes; .ics opens in a calendar app; PDF saves through the file picker; one PDF writer remains"
```

## Step 9 — Group and journal

**Intent**: One Group and one Journal screen.
**Tags**: impl
**Chain rationale**: `ecc:java-reviewer` closes the chain; chain follows the default for the primary tag.

```bash
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-9] Port from Kevin's GroupActivity and JournalActivity into ours: add and remove local companions, sample join code, cost split over real members; journal photos picked with OpenDocument and takePersistableUriPermission so they survive restarts, entries editable, day or stop attachment. Acceptance: Companions and votes persist; journal photo still loads after app restart; entries can be edited; tests pass"
```

## Step 10 — Remove duplicate screens

**Intent**: No parallel app left.
**Tags**: refactor
**Chain rationale**: `ecc:java-reviewer` closes the chain; chain follows the default for the primary tag.

```bash
/ecc:orchestrate custom "ecc:architect,ecc:refactor-cleaner,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-10] After steps 2 to 9, delete Kevin's duplicate screens and state (prototype/TripsActivity, TripSetupActivity, PlannerActivity, StopEditorActivity, DiscoverActivity, BudgetActivity, WeatherActivity, GroupActivity, JournalActivity, SummaryActivity, PrototypeActivity, DraftStore, DraftTrip) and their manifest entries. Rewrite PrototypeDeviceTest against the unified screens. Acceptance: No class in prototype/ extends Activity; manifest lists each screen once; assembleDebug, unit tests and lint pass with 0 errors"
```

## Step 11 — Naming and docs

**Intent**: Docs match the app.
**Tags**: docs
**Chain rationale**: `ecc:doc-updater` closes the chain; chain follows the default for the primary tag.

```bash
/ecc:orchestrate custom "ecc:doc-updater" "[Plan: .claude/plans/combine-prototypes.md#step-11] Make the app name consistent everywhere (Kevin renamed it to Trippings in strings.xml and settings.gradle.kts; README, DESIGN.md and PRODUCT.md still say TripFit; use the name the team confirms in the merge map). Update README status table, docs/Frontend-Prototype-Coverage.md and DESIGN.md to describe the single combined flow. Acceptance: One app name across strings, README and docs; coverage doc paths point at existing classes; README status table reflects persistent storage"
```

## Step 12 — Final verification

**Intent**: Proof the merge works.
**Tags**: review
**Chain rationale**: `ecc:code-reviewer` closes the chain; chain follows the default for the primary tag.

```bash
/ecc:orchestrate custom "ecc:java-reviewer,ecc:code-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-12] Review the combined branch end to end: run assembleDebug, testDebugUnitTest and lintDebug, then walk the flow on the TripFit_Lite emulator (python3 .claude/plans/tools/capture.py for screenshots) following the walkthrough in docs/Frontend-Prototype-Coverage.md. Report regressions against .impeccable/critique/*.md fixes. Acceptance: Build, tests and lint pass with 0 errors; every backlog feature reachable from Login in one flow; no critique fix regressed; findings written to docs/Prototype-Merge-Map.md"
```

## Batch execution

```bash
/ecc:orchestrate custom "ecc:planner,ecc:architect" "[Plan: .claude/plans/combine-prototypes.md#step-1] Read both implementations (ours: root-package Activities + prototype/PrototypeStore; Kevin's: prototype/*Activity, DraftStore, DraftTrip, RoutePreview, TripExport, DemoCatalog) and docs/Frontend-Prototype-Coverage.md. Write docs/Prototype-Merge-Map.md listing, per feature, the behaviours of Kevin's version that ours lacks and where each will live. Acceptance: docs/Prototype-Merge-Map.md covers all 14 backlog features; every Kevin-only behaviour has a target file in our flow; open questions for the team are listed"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-2] Make prototype/PrototypeStore persistent using Kevin's DraftStore approach (SharedPreferences + Gson, one JSON document per trip id) so expenses, budget, leg modes, journal, votes and sync status survive app restarts. Keep its public API; add what the merge map needs (per-stop cost and currency, members, weather). Acceptance: Unit tests cover save and reload round trip; data survives process death; existing 37+ unit tests still pass"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-3] LoginActivity currently opens prototype.TripsActivity, which orphans HomeActivity and every critiqued screen. Route Login back to HomeActivity and make NewTripActivity the only trip-creation form, porting any field TripSetupActivity has that it lacks. Acceptance: Login opens HomeActivity; no code path starts prototype.TripsActivity, TripSetupActivity or PlannerActivity; compile and tests pass"
/ecc:orchestrate custom "ecc:architect,ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-4] Merge Kevin's RoutePreview into our RouteMath, ScheduleMath and TravelMode: buffer before each incoming leg, overlapping-booking warnings, balancing flexible stops across days, current vs preview travel totals. Surface them in MainActivity, StopDetailActivity and OptimizeActivity; persist buffer in Room or the store. Acceptance: RoutePreviewTest cases pass against the merged classes; booked stops never move; conflicts are shown in the itinerary; no duplicate route logic remains"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-5] Port from Kevin's BudgetActivity and DraftTrip into our BudgetActivity, BudgetSetupActivity and StopDetailActivity: planned cost with original currency per stop, planned costs kept separate from logged expenses, a currency calculator. Merge his sample rates and our CurrencyMock into one rates table. Acceptance: Tracker shows planned and actual separately; each expense and stop keeps its original currency; a single rates table remains; unit tests cover conversion and totals"
/ecc:orchestrate custom "ecc:architect,ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-6] Merge Kevin's DemoCatalog and our MockData into one catalog (Tokyo and Manila; places with coordinates, suggested duration, price). Use it in DiscoverActivity, SuggestionsActivity and AddDestinationActivity offline search; skip places already in the itinerary; add picks to the selected day. Acceptance: Only one catalog class remains; Discover and Suggestions never offer a stop already planned; picks land on the chosen day; tests pass"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-7] Add a WeatherActivity to our flow (extends ScreenActivity, navy toolbar, item_row cards) ported from Kevin's prototype.WeatherActivity: per-day sample forecast the user can change, outdoor conflicts, swap flexible stops for indoor alternatives, bookings protected. The Itinerary rain banner and overflow menu open it. Acceptance: Weather is reachable from Itinerary; swapping updates Room stops; booked stops are never swapped; forecast choice persists"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-8] Bring Kevin's TripExport into CalendarSyncActivity and SummaryActivity: export a real .ics file with correct time zones and escaping, and save PDF or .ics through Android's file picker (ACTION_CREATE_DOCUMENT) alongside the existing share actions. Keep our TripPdfWriter or his PDF code, not both. Acceptance: TripExportTest passes; .ics opens in a calendar app; PDF saves through the file picker; one PDF writer remains"
/ecc:orchestrate custom "ecc:tdd-guide,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-9] Port from Kevin's GroupActivity and JournalActivity into ours: add and remove local companions, sample join code, cost split over real members; journal photos picked with OpenDocument and takePersistableUriPermission so they survive restarts, entries editable, day or stop attachment. Acceptance: Companions and votes persist; journal photo still loads after app restart; entries can be edited; tests pass"
/ecc:orchestrate custom "ecc:architect,ecc:refactor-cleaner,ecc:java-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-10] After steps 2 to 9, delete Kevin's duplicate screens and state (prototype/TripsActivity, TripSetupActivity, PlannerActivity, StopEditorActivity, DiscoverActivity, BudgetActivity, WeatherActivity, GroupActivity, JournalActivity, SummaryActivity, PrototypeActivity, DraftStore, DraftTrip) and their manifest entries. Rewrite PrototypeDeviceTest against the unified screens. Acceptance: No class in prototype/ extends Activity; manifest lists each screen once; assembleDebug, unit tests and lint pass with 0 errors"
/ecc:orchestrate custom "ecc:doc-updater" "[Plan: .claude/plans/combine-prototypes.md#step-11] Make the app name consistent everywhere (Kevin renamed it to Trippings in strings.xml and settings.gradle.kts; README, DESIGN.md and PRODUCT.md still say TripFit; use the name the team confirms in the merge map). Update README status table, docs/Frontend-Prototype-Coverage.md and DESIGN.md to describe the single combined flow. Acceptance: One app name across strings, README and docs; coverage doc paths point at existing classes; README status table reflects persistent storage"
/ecc:orchestrate custom "ecc:java-reviewer,ecc:code-reviewer" "[Plan: .claude/plans/combine-prototypes.md#step-12] Review the combined branch end to end: run assembleDebug, testDebugUnitTest and lintDebug, then walk the flow on the TripFit_Lite emulator (python3 .claude/plans/tools/capture.py for screenshots) following the walkthrough in docs/Frontend-Prototype-Coverage.md. Report regressions against .impeccable/critique/*.md fixes. Acceptance: Build, tests and lint pass with 0 errors; every backlog feature reachable from Login in one flow; no critique fix regressed; findings written to docs/Prototype-Merge-Map.md"
```
