# Trippings — ChatGPT AI Assistant Guide

Welcome! This guide is designed for developers using **ChatGPT** (ChatGPT Plus, ChatGPT Projects, Canvas, or OpenAI Codex) to continue developing and combining the frontend prototypes for **Trippings** (formerly TripFit).

---

## 1. Project Overview & Tech Stack

Trippings is an Android trip-planning app (itinerary building, route optimization, calendar sync, budget tracking) for DLSU CCS-IT (ITISMOB).

* **Language:** **Java 11** (Note: Gradle build files use Kotlin DSL `.kts`, but the app source code in `app/src/main/java` is strictly Java).
* **UI Architecture:** Classic Android View system — AppCompat, Material Components (M3), ConstraintLayout, RecyclerView. **Do NOT use Jetpack Compose.**
* **Database & Persistence:**
  * **Room DB (`TripFitDatabase` v1):** Stores trips (`TripEntity`), itinerary stops (`ItineraryItemEntity`), and places (`PlaceEntity`).
  * **`PrototypeStore`:** SharedPreferences + Gson (`trippings_prototype_v1`), saving per-stop details (costs, currency, category), weather choices, companion members, and journal entries.
* **Package Name:** `com.itismob.s01.group01.tripfit`
* **Test Suite:** JUnit 4 (unit tests) & AndroidX Espresso (instrumented tests).

### Core Plan Documents (Must Read)
All core plan documents are accessible in the visible `docs/` folder (no need to open hidden dot-folders):
* **[docs/Prototype-Merge-Map.md](file:///Users/rbndct/CODING%20/School/MOB%20DEVE/TripFit/docs/Prototype-Merge-Map.md)**: The authoritative 138-item feature mapping and checklist.
* **[docs/Combine-Prototypes-Plan.md](file:///Users/rbndct/CODING%20/School/MOB%20DEVE/TripFit/docs/Combine-Prototypes-Plan.md)** *(also in `.claude/plans/combine-prototypes.md`)*: Master 12-step prototype unification plan.
* **[docs/Combine-Prototypes-Orchestrate.md](file:///Users/rbndct/CODING%20/School/MOB%20DEVE/TripFit/docs/Combine-Prototypes-Orchestrate.md)** *(also in `.claude/plans/combine-prototypes.orchestrate.md`)*: Detailed step-by-step execution directives, scope, intent, and acceptance criteria.

---

## 2. Core Rules & Guidelines for ChatGPT

When generating or modifying code in this repository:

1. **Pure Java 11**: Do not add Kotlin files to `app/src/main/java`.
2. **No New Dependencies**: Rely only on existing dependencies (Room, Gson, Retrofit, OkHttp, Material).
3. **Impeccable Design Rules (`DESIGN.md`)**:
   * All screens extend `ScreenActivity` (consistent navy toolbar shell).
   * Monospace font used only for data/codes/timestamps.
   * 16dp corner radius outlined cards (`item_row`).
   * Touch targets must be at least 48x48dp.
   * Strings must be defined in `res/values/strings.xml` (no raw user-facing hardcoded strings).
   * Colors must be defined for both light and dark modes (`values/colors.xml` and `values-night/colors.xml`).
4. **Clean Commits**:
   * One atomic commit per step with conventional commit message (e.g., `feat: ...`, `refactor: ...`, `docs: ...`).
   * **Do not include AI co-author or attribution tags** in commit messages.
5. **Verification**:
   Always verify code changes with Gradle commands before committing:
   ```bash
   ./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug
   ```

---

## 3. Current Progress

Branch: **`feat/ui-prototype-step5`**

| Step | Feature | Status | Details |
|---|---|---|---|
| 1 | Merge Map | **DONE** | Map written in `docs/Prototype-Merge-Map.md` |
| 2 | Persistent Store | **DONE** | `PrototypeStore` backed by SharedPreferences + Gson |
| 3 | Single Entry Flow | **DONE** | `LoginActivity` -> `HomeActivity` -> `MainActivity` |
| 4 | Planning & Scheduling Logic | **DONE** | Unified `RouteMath`, `ScheduleMath`, buffer & conflict detection |
| 5 | Budget & Currency | **DONE** | Unified budget model, separate planned/actual costs, live exchange rates |
| 6 | Discover & Suggestions Catalog | **DONE** | One Tokyo and Manila sample catalog |
| 7 | Weather Screen | **DONE** | Saved forecasts and indoor alternatives |
| 8 | Calendar & Export | **DONE** | Calendar and PDF file export, plus summary sharing |
| **9** | **Group & Journal** | **NEXT** | Companions, voting, and persistable image URIs |
| 10 | Remove Duplicate Screens | *Pending* | Delete obsolete classes in `prototype/*Activity` |
| 11 | Naming & Docs | *Pending* | Finalize Trippings naming across strings and docs |
| 12 | Final Verification | *Pending* | Full emulator walkthrough & lint sweep |

---

## 4. Copy-Paste Prompts for ChatGPT (Steps 6 through 12)

You can copy and paste the prompts below directly into ChatGPT when working on each step.

---

### Step 6: Discover and Suggestions Catalog

```text
I am continuing the Trippings Android prototype merge plan on branch `feat/ui-prototype-step5`. We have completed Steps 1 through 5. Now we are implementing Step 6: Discover and Suggestions Catalog.

Context & Reference Documents (Please read these in the repo):
- Plan files:
  * `.claude/plans/combine-prototypes.md` (Step 6)
  * `.claude/plans/combine-prototypes.orchestrate.md` (Step 6 details & acceptance criteria)
  * `docs/Prototype-Merge-Map.md` section F2, F5 (checklist items P-47 through P-60)
- Existing catalogs: `app/src/main/java/com/itismob/s01/group01/tripfit/prototype/DemoCatalog.java`, `com/itismob/s01/group01/tripfit/data/MockData.java`, and `com/itismob/s01/group01/tripfit/data/OfflineData.java`.
- Consumer activities: `DiscoverActivity.java`, `SuggestionsActivity.java`, and `AddDestinationActivity.java`.

Goal:
1. Merge `DemoCatalog` and `MockData` into a single canonical catalog class (covering Tokyo and Manila; places with lat/lng, suggested duration, cost, currency, category, rating, reviews, indoor/outdoor flags).
2. Fold `OfflineData` into the unified catalog or delegate to it.
3. Update `DiscoverActivity`, `SuggestionsActivity`, and `AddDestinationActivity` to query this single catalog.
4. When adding or suggesting places:
   - Skip places already in the itinerary for that trip.
   - Allow user to pick the target day (1..N).
   - Persist place cost, currency, category, and outdoor flag into `PrototypeStore` or Room when added.
5. Ensure category remaining budget = limit - max(planned, logged).

Constraints:
- Java 11 only.
- All unit tests must pass: `./gradlew testDebugUnitTest`.
- 0 lint errors: `./gradlew lintDebug`.
- Follow DESIGN.md conventions.

Please inspect the existing files and provide the complete Java code changes needed to complete Step 6.
```

---

### Step 7: Weather Screen

```text
I am implementing Step 7: Weather Screen for the Trippings Android app.

Context & Reference:
- Reference implementation: `app/src/main/java/com/itismob/s01/group01/tripfit/prototype/WeatherActivity.java`.
- Target integration: Add a proper `WeatherActivity` in `com.itismob.s01.group01.tripfit` extending `ScreenActivity` with the navy toolbar and `item_row` cards.
- Reference items: `docs/Prototype-Merge-Map.md` items P-83 to P-89.

Goal:
1. Create/refactor `WeatherActivity` so it extends our standard `ScreenActivity`.
2. Connect it to `MainActivity` via the itinerary rain banner and toolbar overflow menu.
3. Allow user to view and change the per-day forecast (Clear / Rain / Storm), persisting selections in `PrototypeStore`.
4. Detect outdoor conflicts on non-clear days using the stop's outdoor flag:
   - Fixed/booked stops cannot be swapped (marked "Booked: review with provider").
   - Flexible stops offer indoor swap alternatives (either swap days with an indoor flexible stop or swap with an unplanned catalog indoor activity).
5. Swapping updates stops in Room via `TripRepository`.

Constraints:
- Java 11, Material 3, ScreenActivity base class.
- All unit tests pass: `./gradlew testDebugUnitTest`.
- Provide the code updates and XML layout adjustments.
```

---

### Step 8: Calendar and Export

```text
I am implementing Step 8: Calendar and Export for the Trippings Android app.

Context & Reference:
- Reference implementation: `app/src/main/java/com/itismob/s01/group01/tripfit/prototype/TripExport.java`.
- Target screens: `CalendarSyncActivity.java` and `SummaryActivity.java`.
- Reference items: `docs/Prototype-Merge-Map.md` items P-61 to P-68 and P-90 to P-95.
- Tests to satisfy: `TripExportTest.java` and `TripExportAmountsTest.java`.

Goal:
1. Bring Kevin's `TripExport` calendar generator into `CalendarSyncActivity`:
   - Generate valid RFC 5545 `.ics` content (UTC timestamps from Asia/Tokyo or Asia/Manila, proper line folding at 75 octets, escaping).
   - Export `.ics` using Android's file picker (`Intent.ACTION_CREATE_DOCUMENT`).
2. Bring PDF summary export to `SummaryActivity` using `ACTION_CREATE_DOCUMENT` alongside existing sharing:
   - Unify on a single PDF generation mechanism (keep either `TripExport#pdf` or `TripPdfWriter`, not both).
   - Ensure exported amounts have exactly two decimals.
   - Include prototype disclaimers and rates footnotes.

Constraints:
- Java 11.
- Unit tests `TripExportTest` and `TripExportAmountsTest` must pass.
- `./gradlew testDebugUnitTest lintDebug` must succeed with 0 errors.
```

---

### Step 9: Group and Journal

```text
I am implementing Step 9: Group and Journal for the Trippings Android app.

Context & Reference:
- Kevin's prototype screens: `prototype/GroupActivity.java` and `prototype/JournalActivity.java`.
- Our screens: `GroupActivity.java` and `JournalActivity.java`.
- Items P-96 to P-111 in `docs/Prototype-Merge-Map.md`.

Goal:
1. Group Screen:
   - Manage companions per trip (stored in `PrototypeStore`). Trip starts with "You · Organizer".
   - Prevent case-insensitive duplicates ("This person is already listed").
   - Demo join code `TRIP-DEMO` adds sample companion.
   - Voting on candidate catalog activities (persisted per trip).
   - Cost split preview: equal split of planned and logged spending over members.
2. Journal Screen:
   - Pick photos using `ActivityResultContracts.OpenDocument` and call `takePersistableUriPermission` so images survive restarts.
   - Attach entry to a day or a specific stop.
   - Support editing existing entries and removing entries (with confirmation).
   - Handle draft state across device rotation.

Constraints:
- Java 11, Room + PrototypeStore persistence.
- `./gradlew testDebugUnitTest lintDebug` must succeed with 0 errors.
```

---

### Step 10: Remove Duplicate Screens & Gate Cleanup

```text
I am implementing Step 10: Remove Duplicate Screens for the Trippings Android app.

Context & Reference:
- All features from Steps 2 through 9 are now ported to our root-package activities.
- Read section 4 of `docs/Prototype-Merge-Map.md` (Screen-deletion gate).

Goal:
1. Verify that all 138 items in the `docs/Prototype-Merge-Map.md` checklist are satisfied.
2. Safely delete the obsolete prototype Activities in `app/src/main/java/com/itismob/s01/group01/tripfit/prototype/`:
   - `TripsActivity.java`
   - `TripSetupActivity.java`
   - `PlannerActivity.java`
   - `StopEditorActivity.java`
   - `DiscoverActivity.java`
   - `BudgetActivity.java`
   - `WeatherActivity.java`
   - `GroupActivity.java`
   - `JournalActivity.java`
   - `SummaryActivity.java`
   - `PrototypeActivity.java`
   - `DraftStore.java` and `DraftTrip.java`
3. Clean up `AndroidManifest.xml` to remove the deleted activity declarations.
4. Keep the shared logic and utility classes in `prototype/` (such as `PrototypeStore`, `RouteMath`, `ScheduleMath`, `TripExport`, `DemoCatalog`, etc.).
5. Update or rewrite `PrototypeDeviceTest` and any unit tests that referenced the deleted screens.

Constraints:
- `./gradlew assembleDebug testDebugUnitTest lintDebug` must compile and pass with 0 errors.
```

---

### Step 11: Naming & Docs Consistency

```text
I am implementing Step 11: Naming & Docs Consistency for the Trippings Android app.

Goal:
1. Ensure the app name is unified as **Trippings** across `strings.xml`, `settings.gradle.kts`, `README.md`, `DESIGN.md`, and `PRODUCT.md`.
2. Update `README.md` features table to reflect all persistent storage and completed unified features.
3. Update `docs/Frontend-Prototype-Coverage.md` to point to the unified classes.
4. Verify all documentation links and references are valid.
```

---

### Step 12: Final Verification

```text
I am performing Step 12: Final Verification for the Trippings Android app.

Goal:
1. Execute `./gradlew assembleDebug testDebugUnitTest lintDebug`.
2. Verify all 19 backlog features are reachable from `LoginActivity` -> `HomeActivity`.
3. Verify that no visual or functional regressions occurred against `DESIGN.md` rules.
4. Document the final verification results in `docs/Prototype-Merge-Map.md`.
```

---

## 5. Helpful Commands Quick Reference

```bash
# Check branch status
git status

# Compile Java code
./gradlew compileDebugJavaWithJavac

# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "com.itismob.s01.group01.tripfit.prototype.RoutePreviewTest"

# Run Android Lint
./gradlew lintDebug

# Build Debug APK
./gradlew assembleDebug
```
