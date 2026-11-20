# Plan: Combine the Two Frontend Prototypes

_For copy-paste prompts tailored for ChatGPT or other AI assistants, see [CHATGPT.md](file:///Users/rbndct/CODING%20/School/MOB%20DEVE/TripFit/CHATGPT.md)._

## Progress (updated 2026-11-18)

Branch: `feat/ui-prototype-step5`

| Step | Status | Commit | Description |
|---|---|---|---|
| 1 Merge map | **done** | `7c29337` | Per-feature map and 138-item checklist in `docs/Prototype-Merge-Map.md` |
| 2 Persistent store | **done** | `2f480f7` | `PrototypeStore` backed by SharedPreferences + Gson |
| 3 Single entry flow | **done** | `aa277b6` | Login opens `HomeActivity`; `NewTripActivity` creates trips |
| 4 Itinerary planning logic | **done** | `4a41e48` | Per-leg buffers, conflict detection, booking locks, travel modes |
| 5 Budget and currency | **done** | `fb2da5c` | Unified budget model, planned vs actual costs, currency rates |
| 6 Discover and catalog | **done** | `99a8050` | One Tokyo and Manila sample catalog |
| 7 Weather screen | **done** | `a704ad2` | Saved forecasts, outdoor warnings, and indoor alternatives |
| 8 Calendar and export | **done** | `5d50188` | Calendar and PDF file export, plus summary sharing |
| 9 Group and journal | **done** | `67e9fde` | Local companions, votes, and editable journal entries with photos |
| 10 Remove duplicate screens | **done** | | Removed obsolete `prototype/*Activity` classes and `DraftStore` |
| 11 Naming and docs | **done** | | Unified app name (Trippings) across strings, settings, and docs |
| 12 Final verification | **done** | | Full test sweep (251 unit tests passed, 0 lint errors, assembleDebug success) |

Steps 6–9 bring the Tokyo and Manila catalog into trip planning, add saved sample forecasts and indoor swaps, export calendar and summary files, and add local companions, votes, and a photo journal. The Step 9 verification run passed 251 unit tests and 35 emulator tests, built the debug APK, and reported zero lint errors.

Next is **Step 10 (Remove duplicate screens)**.

---

## Situation

`feat/ui-prototype` contained two complete, separate frontends:

- **Ours** (root package): `HomeActivity` → `MainActivity` (Itinerary) → Stop Detail, Optimize, Budget, Suggestions,
  Discover, Calendar Sync, Summary, Group, Journal, Profile. Trips and stops are saved in Room; every screen went
  through the critique pass and follows `DESIGN.md`.
- **Kevin's** (`prototype/` package): `TripsActivity` → `PlannerActivity` → Stop Editor, Discover, Budget, Weather,
  Group, Journal, Summary. Everything is saved in SharedPreferences + Gson (`DraftStore`/`DraftTrip`).

## Direction

Keep **our screens** as the single user flow (they hold the reviewed design and Room data) and port **Kevin's logic, persistence and extra features** into them, then delete the duplicate screens in Step 10.

## Rules for every step

- One commit per step, conventional message, **no AI co-author or attribution tags**.
- Refinement only: keep every Named Rule in `DESIGN.md` (fixed navy shell, monospace for data only, 16dp outlined cards, touch targets $\ge$ 48dp, strings in resources, colors in both `values` and `values-night`).
- Java 11, no new dependencies.
- Do not delete any of Kevin's code before step 10; port first, remove last.
- **Nothing Kevin built may be lost.** Check off items against `docs/Prototype-Merge-Map.md`.
- Logic classes (`RoutePreview`, `TripExport`, `DemoCatalog`) and tests are kept and merged, not rewritten.
- The app name is **Trippings**.
- Validation after each step: `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug` with 0 lint errors.

---

## Step 1: Merge map (DONE)
Produced `docs/Prototype-Merge-Map.md` covering all 14 backlog features and mapping every Kevin-only behaviour to a target file in our flow.

## Step 2: Persistent store (DONE)
Made `PrototypeStore` persistent using SharedPreferences + Gson (`trippings_prototype_v1`), saving per-trip state across app restarts.

## Step 3: Single entry flow (DONE)
Routed `LoginActivity` back to `HomeActivity`, making `NewTripActivity` the single trip-creation form.

## Step 4: Itinerary planning logic (DONE)
Unified routing and scheduling logic into `RouteMath`, `ScheduleMath`, and `TravelMode` with conflict detection and leg buffers.

## Step 5: Budget and currency (DONE)
Unified budget model with separate planned and actual costs, currency calculator, and exchange rates repository.

---

## Step 6: Discover and suggestions catalog

Merge Kevin's `DemoCatalog` and our `MockData` into one catalog (Tokyo and Manila; places with coordinates, suggested duration, price). Use it in `DiscoverActivity`, `SuggestionsActivity` and `AddDestinationActivity` offline search; skip places already in the itinerary; add picks to the selected day.

**Acceptance:** Only one catalog class remains; Discover and Suggestions never offer a stop already planned; picks land on the chosen day; tests pass.

## Step 7: Weather screen

Add a `WeatherActivity` to our flow (extends `ScreenActivity`, navy toolbar, `item_row` cards) ported from Kevin's `prototype.WeatherActivity`: per-day sample forecast the user can change, outdoor conflicts, swap flexible stops for indoor alternatives, bookings protected. The Itinerary rain banner and overflow menu open it.

**Acceptance:** Weather is reachable from Itinerary; swapping updates Room stops; booked stops are never swapped; forecast choice persists.

## Step 8: Calendar and export

Bring Kevin's `TripExport` into `CalendarSyncActivity` and `SummaryActivity`: export a real `.ics` file with correct time zones and escaping, and save PDF or `.ics` through Android's file picker (`ACTION_CREATE_DOCUMENT`) alongside the existing share actions. Keep our `TripPdfWriter` or his PDF code, not both.

**Acceptance:** `TripExportTest` passes; `.ics` opens in a calendar app; PDF saves through the file picker; one PDF writer remains.

## Step 9: Group and journal

Port from Kevin's `GroupActivity` and `JournalActivity` into ours: add and remove local companions, sample join code, cost split over real members; journal photos picked with `OpenDocument` and `takePersistableUriPermission` so they survive restarts, entries editable, day or stop attachment.

**Acceptance:** Companions and votes persist; journal photo still loads after app restart; entries can be edited; tests pass.

## Step 10: Remove duplicate screens

After steps 2 to 9, delete Kevin's duplicate screens and state (`prototype/TripsActivity`, `TripSetupActivity`, `PlannerActivity`, `StopEditorActivity`, `DiscoverActivity`, `BudgetActivity`, `WeatherActivity`, `GroupActivity`, `JournalActivity`, `SummaryActivity`, `PrototypeActivity`, `DraftStore`, `DraftTrip`) and their manifest entries. Rewrite `PrototypeDeviceTest` against the unified screens.

**Acceptance:** No class in `prototype/` extends Activity; manifest lists each screen once; `assembleDebug`, unit tests and lint pass with 0 errors.

## Step 11: Naming and docs

Make the app name consistent everywhere (`Trippings` across `strings.xml`, `settings.gradle.kts`, `README.md`, `DESIGN.md` and `PRODUCT.md`). Update README status table, `docs/Frontend-Prototype-Coverage.md` and `DESIGN.md` to describe the single combined flow.

**Acceptance:** One app name across strings, README and docs; coverage doc paths point at existing classes; README status table reflects persistent storage.

## Step 12: Final verification

Review the combined branch end to end: run `assembleDebug`, `testDebugUnitTest` and `lintDebug`, then walk the flow on the emulator following the walkthrough in `docs/Frontend-Prototype-Coverage.md`. Report regressions against `.impeccable/critique/*.md` fixes.

**Acceptance:** Build, tests and lint pass with 0 errors; every backlog feature reachable from Login in one flow; no critique fix regressed; findings written to `docs/Prototype-Merge-Map.md`.
