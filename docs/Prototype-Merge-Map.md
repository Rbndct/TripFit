# Prototype Merge Map

_Step 1 of `.claude/plans/combine-prototypes.md`. Written 2026-11-16 on branch `feat/ui-prototype`. No code was changed in this step._

**Path convention.** `<pkg>/` means `app/src/main/java/com/itismob/s01/group01/tripfit/`. Kevin's classes are under `<pkg>/prototype/`. Where a simple name exists twice, the full path decides which class is meant: for example `prototype/BudgetActivity.java` is Kevin's and `BudgetActivity.java` (root package) is ours.

**Which 14 backlog features.** `docs/TripFit-Proposal-and-Backlog.md` has 19 rows. The proposal's own groupings do not give 14:
- High plus Medium priority gives 13 rows.
- The five sprints list 18 functions plus "Database & Project Setup".

The only 14-item grouping in the repo is the README "Features" table. It merges rows that share a screen:
- #4, #6 and #7 (Stop Detail)
- #11, #12 and #13 (Budget, Budget Setup)
- #14 and #15 (Suggestions)

This document uses those 14 features. Because each one bundles its backlog rows, **all 19 backlog rows are in scope and none is excluded**. Features are ordered by their lowest backlog number, so F4 (#4/#6/#7) comes before F5 (#5).

---

## 1. Overview

**Base UI.** Our root-package screens are the single user flow: Home → Itinerary (`MainActivity`) → Stop Detail, Optimize, Budget, Budget Setup, Suggestions, Discover, Calendar Sync, Summary, Group, Journal, Profile, New Trip, Add Destination, Map Picker. They hold the reviewed `DESIGN.md` design (navy `ScreenActivity` shell, `item_row` cards, critique fixes in `.impeccable/critique/*.md`) and the Room trip and stop data. Kevin's screens (`prototype/*Activity`) are the source for logic, persistence and extra features. His logic classes (`RoutePreview`, `TripExport`, `DemoCatalog`) and his tests are kept and moved over, not rewritten. His screens and his `DraftStore`/`DraftTrip` state are deleted only in step 10, and only once every checklist item they provide is ticked and reachable from Login. That gate is in section 4.

**Data model.** Kevin keeps each trip as one Gson document, `DraftTrip`, inside a single JSON array. It lives in SharedPreferences file `trippings_prototype_v1`, key `trips`, read and written by `DraftStore`. The document holds:
- Trip fields: city, ISO `startDate`, `days`, budget and category limits, `dailyBudget`.
- Stops: with coordinates, cost, currency, category, buffer, mode and outdoor flag.
- Expenses, members, votes, journal entries, per-day weather, and `calendarPreview`.

Ours splits the same data in two:
- **Room** (`data/local`, `TripFitDatabase` v1): `TripEntity`, `ItineraryItemEntity` (day, position, time, duration, booked), and `PlaceEntity` (coordinates of place-backed stops).
- **`prototype/PrototypeStore`**: session-only in-memory maps for trip meta, budget, expenses, leg modes, journal, votes and sync status, plus a global buffer setting.

Room already has unused columns that fit several of Kevin's fields: `ItineraryItemEntity.bufferMin`, `transportMode`, and `TripEntity.startDate`/`endDate`/`homeCurrency`/`budget`. Everything else must go into a persistent `PrototypeStore` (step 2) or into a Room migration. See section 5.

---

## 2. Per-feature merge map

### F1. Backlog #1 Google Sign-In Authentication (and the sprint plan's "Database & Project Setup")

- **Ours:**
  - `<pkg>/LoginActivity.java`: debug bypass via `DEV_BYPASS_LOGIN`. `goToHome()` currently starts `prototype.TripsActivity`.
  - `<pkg>/HomeActivity.java` and `<pkg>/ProfileActivity.java`: Sign out returns to Login and clears the back stack.
  - `<pkg>/TripFitApp.java`.
  - `<pkg>/data/RemoteDataSource.java`: a blank or invalid `SUPABASE_URL` gives `api = null` and an `IOException` on fetch.
  - `<pkg>/data/TripRepositoryImpl.java#fetchCities/searchPlaces`: falls back to `OfflineData`.
  - `<pkg>/data/TripRepositoryImpl.java#ensureSeedData`: seeds "Tokyo Trip" with 6 stops, Skytree booked 18:00, teamLab 15:30.
- **Kevin's:**
  - `prototype/TripsActivity.java#render`: trip list, the "Frontend preview" notice, and the "Traveler preferences" dialog with "Back to welcome".
  - `prototype/DraftStore.java#all`: seeds "Tokyo, at your own pace" from `DemoCatalog` with members.
  - `prototype/PrototypeActivity.java#onCreate`: loads the trip by id; screens `finish()` when it is null.
- **Kevin-only behaviours:**
  - All trip state survives an app restart (P-02).
  - The seeded sample trip is labelled "Sample trip" and has members "You" and "Alex (sample)" (P-03).
  - The sample trip's stops carry real catalog coordinates, costs, currencies, durations and outdoor flags (P-04).
  - A screen opened with an unknown trip id closes instead of rendering defaults (P-05).
  - A notice says drafts stay on the device and that discovery, prices, rates and weather are samples (P-07).
  - The brand tagline "Less planning. More exploring." (P-116).
- **Already matched:**
  - Startup without Supabase (P-01). The evidence is `RemoteDataSource(OkHttpClient,String)` plus `RemoteDataSourceTest#missingConfiguration_*`/`invalidConfiguration_*`. It must be re-verified once Login opens Home.
  - "Back to welcome" is matched by Profile's Sign out (P-06).
- **Lands in:**
  - P-02: `prototype/PrototypeStore.java`.
  - P-03: `PrototypeStore` plus `ui/TripAdapter.java`.
  - P-04: `data/TripRepositoryImpl.java#ensureSeedData` plus the merged catalog.
  - P-05: `ScreenActivity.java` (shared guard).
  - P-07: `ProfileActivity.java` About dialog (`about_message`).
  - P-116: `LoginActivity`/`HomeActivity` strings.
- **Plan step:** 2 (P-02, P-03), 3 (P-01, P-05, P-06, P-07), 6 (P-04), 11 (P-116).

### F2. Backlog #2 Add Destination

- **Ours:**
  - `<pkg>/AddDestinationActivity.java`: city chips, debounced Supabase or offline search, map pin through `MapPickerActivity`, a duplicate-name warning (ours only), and a booked switch with time.
  - `<pkg>/MapPickerActivity.java` (shared): tap, long-press and drag, coordinates without geocoding, OSM attribution.
  - Map pins are stored as a `PlaceEntity` with lat/lng. `ItineraryItemWithPlace` exposes only `placeName`, so coordinates are never read back.
- **Kevin's:**
  - `prototype/StopEditorActivity.java#render`, which is both his add and edit form:
    - Sample-place search over `DemoCatalog.places(city)`.
    - Map picker centred on the existing pin or `DemoCatalog.lat/lng(city)`.
    - A "Selected pin" label.
    - The whole draft saved to the Bundle as Gson in `onSaveInstanceState`.
- **Kevin-only behaviours:**
  - Choosing a sample place pre-fills name, lat/lng, address, category, duration, cost, price currency and outdoor flag (P-17).
  - When nothing matches, he shows "No sample matches. Add a name or choose a map pin." (P-18).
  - The map picker reopens at the stop's saved pin, or at the city centre (P-19).
  - Coordinates show immediately as "Selected pin: %.5f, %.5f". With no pin it reads "No pin selected · you can still add a stop by name". An empty name defaults to "Pinned destination" (P-20).
  - A saved pin is shown again when the stop is reopened (P-21).
  - The editor draft survives rotation (P-23).
  - The day must be in 1..trip days (P-24).
- **Already matched:** map tap-to-pin (P-22, shared `MapPickerActivity`). His `PrototypeDeviceTest#mapTapReturnsTheNewCoordinatesWithoutGeocoding` already targets our class.
- **Lands in:**
  - P-17, P-18: `AddDestinationActivity.java` plus the merged catalog.
  - P-19, P-20, P-21, P-23: `AddDestinationActivity.java` and `StopDetailActivity.java`, plus an `ItineraryItemDao` projection that adds `p.lat, p.lng` to `ItineraryItemWithPlace`.
  - P-24: `AddDestinationActivity#validateDay` and `StopDetailActivity#pickDay`.
- **Plan step:** 4 (P-19 to P-21, P-23, P-24), 6 (P-17, P-18), 10 (P-22 test move).

### F3. Backlog #3 Route Optimization

- **Ours:**
  - `<pkg>/OptimizeActivity.java`: per-day nearest-neighbour ordering with locked slots, and km saved.
  - `<pkg>/prototype/RouteMath.java`: flat-earth km, `optimize(points, locked)`.
  - Coordinates are fake: `MockData.coordFor(name)` and `PrototypeStore.legKm(itemId)`, a hash of the id.
- **Kevin's:**
  - `prototype/RoutePreview.java#day(trip, day, optimize=true)`: time-based scheduling around booked anchors.
  - `#distance`: haversine.
  - `#travel`: road factor 1.25 and per-mode speed.
  - `prototype/PlannerActivity.java` "Preview a shorter route" dialog.
- **Kevin-only behaviours:**
  - Leg distances use the stops' real coordinates (haversine × 1.25) (P-25).
  - A stop with no pin uses a 20-minute placeholder leg, and the card says so (P-26).
  - The preview shows current vs preview incoming-travel minutes and the timed new order, with ⚠ on conflicting slots. The buttons are "Keep current" and "Apply order" (P-27).
  - Booked stops are anchors by time, not by list slot. A flexible stop that cannot finish (including travel and buffer) before the next booking is scheduled after that booking (P-28).
  - A notice says the estimates are straight-line and that booked times stay fixed (P-29).
- **Lands in:**
  - P-25, P-28: `prototype/RouteMath.java`, `ScheduleMath.java`, `TravelMode.java`. `PrototypeStore.legKm` and `MockData.coordFor` are retired.
  - P-26: `ui/ItineraryAdapter.java`.
  - P-27, P-29: `OptimizeActivity.java` (`text_locked_note`).
- **Plan step:** 4.

### F4. Backlog #4 Set Duration Per Stop, #6 Booked Activity Lock-In, #7 Smart Buffer Time

- **Ours:**
  - `<pkg>/StopDetailActivity.java`:
    - Day picker, max = `EXTRA_MAX_DAY` (maxDay + 1).
    - Duration of 1 or more; blank gives a default of 60.
    - Booked switch and time.
    - A buffer switch that only displays the global `PrototypeStore.getBufferMin()` and is not saved.
  - `<pkg>/ProfileActivity.java#pickBuffer`: a global buffer of 0/10/15/20/30.
  - `<pkg>/prototype/ScheduleMath.java#build`: booked stops pinned, a global buffer after every stop, no travel time, no conflict detection.
  - `ui/ItineraryAdapter#resolveMetaText`: shows "Booked at HH:mm" or the duration.
- **Kevin's:**
  - `prototype/StopEditorActivity.java`: duration, buffer, booked, fixed time and outdoor fields.
  - `prototype/RoutePreview.java#day`: start, end, travel, conflict.
  - `prototype/PlannerActivity.java#render`: per-card times, BOOKED/FLEXIBLE and the conflict text.
- **Kevin-only behaviours:**
  - Duration is validated to 5..720 minutes; blank means 60 (P-30).
  - Catalog places carry a suggested duration (P-31).
  - Each stop has its own buffer before its incoming leg, 0..180 minutes, default 15, applied only when there is a previous stop (P-32).
  - Arrival = previous end + travel + buffer (P-33, `RoutePreviewTest#perLegModeAndBufferAffectArrival`).
  - A booked stop whose earliest arrival is after its fixed time, or that ends after 24:00, is flagged [CHECK CONFLICT] and its time is not moved (P-34, `RoutePreviewTest#overlappingBookingsAreFlaggedWithoutMovingTheirTimes`).
  - A flexible stop ending after 20:00 is flagged (P-35).
  - The card shows the conflict sentence (P-36).
  - The card shows computed start–end and "BOOKED / FIXED" vs "FLEXIBLE" (P-37).
- **Already matched:** a blank duration defaults to 60 (`ScheduleMath.DEFAULT_STAY_MIN`), and booked stops keep their time (`ScheduleMathTest#build_bookedStop_isPinnedToItsFixedTime`).
- **Lands in:**
  - P-30, P-32: `StopDetailActivity.java` and `AddDestinationActivity.java`, with the buffer persisted in `ItineraryItemEntity.bufferMin` (an existing column).
  - P-33 to P-35: `prototype/ScheduleMath.java` (merged with `RoutePreview`).
  - P-36, P-37: `ui/ItineraryAdapter.java` and `item_stop.xml`.
  - P-31: the merged catalog.
- **Plan step:** 4 (P-31 in 6).

### F5. Backlog #5 Day-by-Day Itinerary Builder

- **Ours:**
  - `<pkg>/MainActivity.java`: day tabs built from the distinct days that have stops (`distinctDays`), the stop list filtered by day, `daySummary` (stops + travel), and the empty state.
  - `<pkg>/NewTripActivity.java`: a date-range picker gives `datesLabel` ("MMM d – MMM d", no year) and `tripDays`. Both live only in the session `TripMeta`; `TripEntity.startDate` stays null.
- **Kevin's:**
  - `prototype/TripSetupActivity.java`: ISO start date, days 1..30, edit mode.
  - `prototype/PlannerActivity.java#render`: chips for every day 1..`trip.days`, `date(day)` headings, the empty-day notice, the day total, and "Spread flexible stops across days".
  - `prototype/PrototypeActivity.java#date`.
- **Kevin-only behaviours:**
  - The ISO start date and the trip length (1..30) are persisted (P-08).
  - Trip settings can be edited after creation (P-11).
  - Shortening the trip is refused while stops exist on later days (P-12).
  - The edit-mode note says city changes keep stops and currency changes keep the number (P-13).
  - Day tabs list every trip day, including empty ones; the selected day is clamped to the trip length and survives rotation (P-38).
  - Each day heading shows its calendar date, e.g. "Day 2 · Wed, Dec 2" (P-39).
  - Balance days moves each flexible stop to the least-loaded day, measured by duration + buffer. Booked stops keep their day and time, and a confirmation dialog explains this first (P-40).
  - Empty-day copy: "A day full of possibilities…" (P-41).
  - The day total gives stay minutes, travel minutes, "plus buffers" (P-42).
- **Lands in:**
  - P-08, P-11, P-12, P-13: `NewTripActivity.java` (create plus edit mode), stored in `TripEntity.startDate`/`endDate`. This needs a `TripDao` update query and a repository method. `MainActivity` overflow gets "Trip settings".
  - P-38, P-39, P-41, P-42: `MainActivity.java`.
  - P-40: `OptimizeActivity.java` (second action) plus a repository batch day update.
- **Plan step:** 3 (P-08, P-11 to P-13), 4 (P-38 to P-42).

### F6. Backlog #8 Per-Leg Transport Mode Selection

- **Ours:**
  - `<pkg>/MainActivity.java#onLegClick` and `ui/ItineraryAdapter#bindLeg`: a chip on each stop except the last, for the leg **leaving** that stop.
  - Modes are WALK/CYCLE/DRIVE/TRANSIT (`prototype/TravelMode.java`: 5/15/30/22 km/h, overhead 0/2/5/8; `suggestFor`: under 1.5 km Walk, under 6 km Transit, otherwise Drive).
  - Mode is kept in `PrototypeStore.legModes` (session only).
- **Kevin's:**
  - `DraftTrip.Stop#mode` is the leg **arriving** at the stop: "Suggested", "Walking", "Cycling", "Driving" or "Transit".
  - `prototype/RoutePreview.java#mode`: Suggested = under 1.2 km Walking, under 4 km Cycling, otherwise Transit; Walking when either end has no pin.
  - `#travel`: 4.5/15/18 (+8 min)/25 km/h, `ceil`, minimum 1 minute.
  - Edited in `StopEditorActivity`.
- **Kevin-only behaviours:**
  - The mode is persisted per leg and includes an explicit "Suggested" option (P-43).
  - His distance thresholds for Suggested (P-44).
  - His speeds and minimum-1-minute rule (P-45).
  - The leg line reads "↓ Mode · ~N min travel + M min buffer" (P-46).
- **Lands in:**
  - P-43: `ItineraryItemEntity.transportMode` (existing column), `MainActivity#onLegClick`, `StopDetailActivity.java`.
  - P-44, P-45: `prototype/TravelMode.java`.
  - P-46: `ui/ItineraryAdapter.java` chip label.
- **Plan step:** 4. Blocked by Q5 (constants) and Q10 (incoming vs outgoing).

### F7. Backlog #9 Discover Suggested Itinerary

- **Ours:**
  - `<pkg>/DiscoverActivity.java`: 3 famous and 3 hidden gems from `MockData.famous/hiddenGems(city)`, a swap with the pool, select-to-add, round-robin across days (`1 + i % days`), a Tokyo/Manila switch, and `EXTRA_OPEN_ITINERARY`.
  - `MockPlace` has no coordinates, no duration and no description.
- **Kevin's:**
  - `prototype/DiscoverActivity.java`: `#render` filters, sort and cards; `#suggestedPlan`; `#swap`; `#remaining`.
  - `prototype/DemoCatalog.java`: 17 places with city, name, category, lat, lng, price, rating, reviews, minutes, hidden, outdoor and description.
- **Kevin-only behaviours:**
  - One catalog with all of those fields (P-47).
  - Filters "All places / Famous / Hidden gems / Food / Activities" plus a text search, where Famous excludes Food (P-48).
  - A chosen target day, and picks land on it (P-49).
  - Places sorted by distance from that day's last pinned stop (or the city centre), with a "Nearby X" label (P-50).
  - Each card shows:
    - POPULAR PICK/HIDDEN GEM · category · Outdoors/Indoors.
    - The description.
    - ★ rating · sample reviews · km.
    - A price tier: Free, $ (Tokyo under ¥1,200, Manila under ₱200) or $$.
    - The local price ≈ home price, and the minutes.
    - (P-51)
  - A place already in the itinerary (matched by name) shows "Already in your itinerary" and cannot be added (P-52; also fixes critique `discover.md` #10).
  - "Build a suggested itinerary" (P-53):
    - Offers up to 3 must-see and 2 hidden non-food places that are not already planned, all pre-checked.
    - Adds them from the target day, 3 per day, capped at the last day.
    - Shows a toast when none are left.
  - "Swap with a flexible stop" (P-54):
    - Replaces any flexible stop in the trip and keeps its id, day, mode and buffer.
    - Booked stops are never offered.
    - Shows a toast when nothing is flexible or the place is already planned.
  - The no-match empty state (P-55).
  - Filter, query, day and budget toggle survive rotation (P-56).
  - The sample-data notice (P-57).
  - A catalog pick saves its coordinates, category, cost, currency, duration and outdoor flag (P-58).
- **Lands in:**
  - P-47: one catalog class in `prototype/` replacing the `DemoCatalog` and `MockData` lists.
  - P-48 to P-57: `DiscoverActivity.java`. The rows are `UiRow`/`item_row`, using `withSelected` and `withAccessibility` per `DESIGN.md`.
  - P-58: a `TripRepository` insert-from-catalog method.
- **Plan step:** 6.

### F8. Backlog #10 Calendar Sync

- **Ours:**
  - `<pkg>/CalendarSyncActivity.java`: event list from `ScheduleMath.build` (global buffer, no travel), a simulated 1.5 s sync, "updated" on re-sync, "Open calendar", and `PrototypeStore.isSynced/markSynced` (session only).
- **Kevin's:**
  - `prototype/SummaryActivity.java#render`: the preview dialog, the warning count, an `.ics` save with `CreateDocument("text/calendar")`, and the per-day event preview.
  - `prototype/TripExport.java#calendar/utc/escape/fold`.
- **Kevin-only behaviours:**
  - The preview dialog names the time zone (Asia/Tokyo or Asia/Manila) and says nothing is sent to Google. Its completion is persisted: "✓ Preview reviewed · No events have been synced" (P-62).
  - The event preview has (P-63):
    - Date headings and real start–end times.
    - A Booked tag.
    - A location: address, else pin, else city.
    - ⚠ on conflicts.
  - "N schedule warning(s)" is shown before export (P-64).
  - The `.ics` file is saved through the file picker as "Trippings-itinerary.ics" and refused when there are no stops (P-65).
  - The `.ics` file is valid (P-66, `TripExportTest`):
    - UTC times computed from the destination zone.
    - `UID <stopId>@trippings.local`.
    - Escaped `\ ; , \n`.
    - Lines folded at 75 UTF-8 octets without splitting a code point.
    - CRLF line endings.
  - An invalid date throws `IllegalArgumentException` and is reported (P-67).
  - Save success and failure toasts (P-68).
- **Already matched:** syncing is disabled with no events (P-61, `CalendarSyncActivity#render` sets `syncButton.setEnabled(hasEvents…)`).
- **Lands in:** `CalendarSyncActivity.java`, which calls `prototype/TripExport.java#calendar` with the trip built from Room and `PrototypeStore`. P-64 also goes into `SummaryActivity.java`.
- **Plan step:** 8.

### F9. Backlog #11 Budget Setup, #12 Running Budget Tracker, #13 Currency Conversion

- **Ours:**
  - `<pkg>/BudgetActivity.java`: spent vs total, category progress, the expense list, and the Add Expense FAB.
  - `<pkg>/ui/AddExpenseDialog.java`: the amount is always in `budget.getLocalCurrency()`, with a live preview.
  - `<pkg>/BudgetSetupActivity.java`: overall or daily, home currency, an optional split that must not exceed the trip total. Amounts are shown as `(long)` and lose their decimals.
  - `prototype/BudgetSettings.java`, `BudgetMath.java`, `data/CurrencyRates.java` (live Frankfurter) and `prototype/CurrencyMock.java` (fallback).
  - There are no planned costs, no expense removal and no calculator.
- **Kevin's:**
  - `prototype/BudgetActivity.java#planned/actual/render`.
  - `prototype/DraftTrip.java#costCurrency/convertedCost/totalBudget/categoryBudget`.
  - `prototype/RoutePreview.java#convert/rate`: 1 JPY = 0.38 PHP, 1 USD = 58 PHP, 1 EUR = 63 PHP.
  - `prototype/StopEditorActivity.java`: cost, price currency, category.
  - `prototype/TripSetupActivity.java`: budget at creation and a 30/40/20/10 auto-split.
- **Kevin-only behaviours:**
  - Budget and the daily flag are set at trip creation (P-09).
  - Default category limits are a 30/40/20/10 split of the budget, recomputed when the budget changes (P-10).
  - Each stop has an estimated cost (0..1e8) in its own currency, defaulting to the local currency. Changing the city does not change it (P-69, `TripExportTest#changingCityDoesNotChangeAnExistingStopsPriceCurrency`).
  - Each stop has a category (P-70).
  - Planned costs (converted stop costs) are shown apart from logged expenses, overall and per category, and never added together (P-71).
  - A warning appears when the planned itinerary is over budget (P-72).
  - Each expense has a "Paid in" currency (P-73).
  - The expense amount is 0.01..1e8 (P-74).
  - Removing an expense asks for confirmation (P-75).
  - Category limits are each 0..1e8 and must sum to no more than the budget. Daily mode multiplies the category limits by days too. Editable amounts keep two decimals (P-76).
  - A currency calculator: amount (default 1000) from any currency (default local) to home, labelled "sample rate" (P-77).
  - A rates and estimates notice (P-78).
  - One rates table (P-79).
  - The stop card shows its estimated cost in home currency (P-80).
  - The itinerary shows the trip budget and start date (P-81).
  - "Find budget-friendly food & activities" links to suggestions (P-82).
- **Already matched:** remaining after logged expenses with an over state (`BudgetActivity#renderRemaining`), original amount plus conversion in the expense rows, the overall/daily toggle, and a split that cannot exceed the total.
- **Lands in:**
  - P-09: `NewTripActivity.java`.
  - P-10, P-76: `BudgetSetupActivity.java` and `PrototypeStore#defaultBudget`.
  - P-69, P-70: `StopDetailActivity.java` and `AddDestinationActivity.java`, stored in the store keyed by item id (see Q9).
  - P-71, P-72, P-75, P-77, P-78, P-82: `BudgetActivity.java`.
  - P-73, P-74: `ui/AddExpenseDialog.java`.
  - P-79: `data/CurrencyRates.java` plus `prototype/CurrencyMock.java`.
  - P-80: `ui/ItineraryAdapter.java`.
  - P-81: `MainActivity.java` subtitle.
- **Plan step:** 5 (P-09 in 3). Blocked by Q6 and Q7.

### F10. Backlog #14 Budget-Aware Food Suggestions, #15 Budget-Aware Activity Suggestions

- **Ours:**
  - `<pkg>/SuggestionsActivity.java`: Food/Activities tabs and a "Fits my budget" chip, where remaining = limit − logged.
  - Picks are added to `EXTRA_DAY`. A session-only `added` set prevents double taps.
  - `MockData.food()/activities()` (Tokyo only, fixed distances).
- **Kevin's:** `prototype/DiscoverActivity.java#remaining` and the budget filter in `#render`, with Food and Activities as filter values.
- **Kevin-only behaviours:**
  - Remaining category budget = limit − max(planned, logged) (P-59).
  - Each card says "X remaining in Category" and "Over category budget". Suggestions exist for Manila too, with real distances (P-60).
  - P-50, P-51 and P-52 also apply to this screen.
- **Lands in:** `SuggestionsActivity.java` and the merged catalog.
- **Plan step:** 6.

### F11. Backlog #16 Weather-Aware Rescheduling

- **Ours:**
  - `<pkg>/MainActivity.java#renderWeatherBanner/showWeatherDialog/swapForIndoor`.
  - Day 2 is always rainy (`MockData.isRainy`), outdoor is guessed from name keywords (`MockData.isOutdoor`).
  - It swaps **all** outdoor flexible stops by adding the replacements at the end of the day and deleting the originals, which loses position and duration. Nothing is persisted.
- **Kevin's:** `prototype/WeatherActivity.java#render/alternatives` and `DraftTrip.weather`.
- **Kevin-only behaviours:**
  - A Weather screen with a per-day sample forecast (Clear, Rain, Storm, each with sample temperature and chance text). The user can change it and it is persisted (P-83).
  - The default forecast is Rain on days 2, 5, 8, … and Clear otherwise (P-84).
  - Each stop has an outdoor flag, editable as "Outdoor activity (check weather)" (P-85).
  - On non-clear days every outdoor stop is listed. A booked one says "Booked: review with your provider" and gets no swap (P-86).
  - Per-stop indoor alternatives (P-87):
    - Swap days with an indoor flexible stop from another day, or
    - Replace it with an unplanned catalog indoor activity, keeping id, day, buffer and mode.
    - A toast appears when there are none.
  - The no-conflict and no-outdoor messages and the not-live notice (P-88).
  - The rain banner and the overflow menu open the screen (P-89; required by the plan).
- **Lands in:**
  - P-83, P-84, P-86 to P-88: a new `<pkg>/WeatherActivity.java` (extends `ScreenActivity`, `item_row` cards, strings in resources).
  - P-85: `StopDetailActivity.java`.
  - P-89: `MainActivity.java`.
  - In-place stop replacement needs a new `TripRepository`/`ItineraryItemDao` update. It is a query change only, not a schema change.
- **Plan step:** 7.

### F12. Backlog #17 Trip Summary Export

- **Ours:**
  - `<pkg>/SummaryActivity.java`: per-day sections (`ScheduleMath`, global buffer) and a budget card showing spent only.
  - "Export PDF" calls `ui/TripPdfWriter.write`: a cache file with no line wrapping, then a FileProvider share.
  - "Share summary" shares plain text.
- **Kevin's:**
  - `prototype/SummaryActivity.java#write`: `CreateDocument("application/pdf")` saved as "Trippings-trip-summary.pdf", and the share chooser.
  - `prototype/TripExport.java#summary/pdf`.
- **Kevin-only behaviours:**
  - The PDF is saved through the Android file picker as well as shared (P-90).
  - The PDF content is the full summary (P-91):
    - Times with [BOOKED] and [CHECK CONFLICT].
    - Stay, travel and buffer.
    - "Estimate: CUR x (≈ HOME y)" and pins.
    - Trip limit, planned, logged and remaining.
    - Per-category limit/planned/logged.
    - The rates footnote.
    - Text word-wrapped at 515pt, blocks kept together, 43 lines per page, header "Trippings | Trip summary", footer "Local prototype · Page N".
  - The shared text is the same summary, subject "<name> · Trippings" (P-92).
  - Every trip day is listed, with "No stops planned yet." for empty days (P-93).
  - All exported amounts have exactly 2 decimals (P-94, `TripExportAmountsTest`).
  - A PROTOTYPE disclaimer at the top (P-95).
- **Lands in:** `SummaryActivity.java` plus one PDF writer (Q12 recommends `TripExport#pdf`, because it writes to any `OutputStream` and so works for both the picker and the cache-file share).
- **Plan step:** 8.

### F13. Backlog #18 Group Trip Mode

- **Ours:**
  - `<pkg>/GroupActivity.java`: a fixed invite code `TRIP-7K2Q` with copy and share (ours only), and hard-coded members Ana R., Marco D. and Lia S.
  - Votes are on up to 5 itinerary stops, with a fake base count of 2–4 plus your vote. They are global, not per trip, and session only.
  - The split divides the **budget** total over 4 people, even or custom 40/30/20/10.
- **Kevin's:** `prototype/GroupActivity.java#render` and `DraftTrip.members/votes`.
- **Kevin-only behaviours:**
  - Companions are persisted per trip and listed with "You · Organizer". A new trip starts with "You" (P-96).
  - Adding a companion requires a name and rejects case-insensitive duplicates ("This person is already listed") (P-97).
  - The sample join code TRIP-DEMO (case-insensitive) adds "Sam (sample)" once. A wrong code shows "Use the sample code TRIP-DEMO" (P-98).
  - One honest local vote per place ("1 vote · You" / "0 votes"), toggled and persisted (P-99).
  - Vote candidates are the city's catalog activities (P-100).
  - Equal split of the **planned** total and of logged spending over the real member count, labelled "Equal split preview only…" (P-101).
  - A local-preview notice (P-102).
  - A link to Discover (P-103).
- **Not in Kevin's code:** removing a companion. The plan's step 9 adds it, so it is not a port item. See Q16.
- **Lands in:** `GroupActivity.java` plus `PrototypeStore` (members, per-trip votes).
- **Plan step:** 9 (P-96 data in 2).

### F14. Backlog #19 Photo / Journal Log

- **Ours:**
  - `<pkg>/JournalActivity.java`: the Add entry dialog requires a note. It attaches to a stop or to "General" (day 0).
  - `PickVisualMedia` only records a `hasPhoto` boolean; the URI is thrown away.
  - Entries are newest first, with no edit, no delete and no photo shown. Session only (`prototype/JournalEntry.java`).
  - `StopDetailActivity#bindPhoto` also has an unsaved photo button.
- **Kevin's:** `prototype/JournalActivity.java#onCreate` (OpenDocument plus `takePersistableUriPermission`), `#render`, `#addPhoto`, `#onSaveInstanceState`, and `DraftTrip.Entry`.
- **Kevin-only behaviours:**
  - Photos are picked with `OpenDocument` and `takePersistableUriPermission`, and the URI is stored. A `SecurityException` shows "Could not keep access to this photo…" (P-104).
  - Photos are rendered (P-105):
    - Downsampled to at most 1200 px, 190dp center-crop, with a content description.
    - An undecodable photo shows "This image cannot be displayed…"; a lost one shows "no longer accessible…".
  - An entry can attach to a day ("Whole day") or to a stop, in which case the day follows the stop (P-106).
  - Saving needs a note **or** a photo (P-107).
  - Entries can be edited, with all fields pre-filled (P-108).
  - Removing an entry asks for confirmation: "The original photo will stay in your gallery." (P-109).
  - The photo can be removed from a draft (P-110).
  - The draft keeps the photo URI and the id being edited across rotation (P-111).
- **Already matched:** a stop picker, a note draft that survives rotation, and the empty state.
- **Lands in:** `JournalActivity.java`, `prototype/JournalEntry.java` (add id, stopId, photoUri) and `PrototypeStore`.
- **Plan step:** 9 (persistence in 2).

---

## 3. Port checklist

Paths are relative to `<pkg>/` unless they start with `app/`. "Verify" means the behaviour already exists in our flow; tick it once it is confirmed reachable from Login.

- [x] **P-01** App starts and plans trips with no `local.properties` Supabase values: a blank URL gives a null API and an `IOException`, and the repository falls back to `OfflineData`. `RemoteDataSourceTest` missing/invalid-config tests keep passing (verify) | Kevin: data/RemoteDataSource.java#RemoteDataSource(OkHttpClient,String), app/build.gradle.kts `getProperty(...,"")` | Ours: LoginActivity.java → HomeActivity.java, data/TripRepositoryImpl.java | Step 3
- [x] **P-02** Trip state survives process death: expenses, budget, leg modes, buffers, journal, votes, members, weather, calendar status, and `TripMeta` (city, ISO start date, days, home currency; session-only today, so a restart would reset every trip to Tokyo, 2 days) | Kevin: prototype/DraftStore.java#save/all/get | Ours: prototype/PrototypeStore.java (SharedPreferences + Gson, one document per trip id) | Step 2
- [x] **P-03** Seeded sample trip is marked as a sample ("Sample trip" label) and has members "You", "Alex (sample)" | Kevin: prototype/DraftStore.java#all | Ours: data/TripRepositoryImpl.java#ensureSeedData, PrototypeStore, ui/TripAdapter.java | Step 2
- [x] **P-04** Sample trip stops come from the catalog with coordinates, cost, currency, duration and outdoor flag (3 on day 1, 3 on day 2; Skytree booked 18:00, teamLab 15:30) | Kevin: prototype/DraftStore.java#all, DemoCatalog.Place#stop | Ours: data/TripRepositoryImpl.java#ensureSeedData + merged catalog | Step 6
- [x] **P-05** A screen opened with a missing or unknown trip id closes itself instead of showing defaults | Kevin: prototype/PrototypeActivity.java#onCreate + each screen's `if(trip==null) finish()` | Ours: ScreenActivity.java (shared guard used by trip screens) | Step 3
- [x] **P-06** "Traveler preferences → Back to welcome" is covered by Profile → Sign out (verify) | Kevin: prototype/TripsActivity.java#render | Ours: ProfileActivity.java#confirmSignOut | Step 3
- [x] **P-07** Notice that drafts stay on the device and that discovery, prices, rates and weather are samples | Kevin: prototype/TripsActivity.java#render notice | Ours: ProfileActivity.java About (`about_message`) | Step 3
- [x] **P-08** Trip start date stored as validated ISO `yyyy-MM-dd` (non-lenient, error "Use a valid date, for example 2026-12-01"); trip length 1..30 days persisted | Kevin: prototype/TripSetupActivity.java#onCreate | Ours: NewTripActivity.java → TripEntity.startDate/endDate (new TripDao update) | Step 3
- [x] **P-09** Budget (1..100,000,000) and the "daily budget" flag can be set when creating a trip | Kevin: prototype/TripSetupActivity.java#onCreate | Ours: NewTripActivity.java (or hand-off to BudgetSetupActivity) | Step 3
- [x] **P-10** Default category limits = 30/40/20/10 % (Food/Activities/Transport/Other) of the budget, recomputed when the budget amount changes | Kevin: prototype/TripSetupActivity.java#onCreate | Ours: prototype/PrototypeStore.java#defaultBudget, BudgetSetupActivity.java | Step 5
- [ ] **P-11** Trip settings (name, city, start date, days, home currency, budget, daily) can be edited from the itinerary | Kevin: prototype/PlannerActivity.java "Trip settings" → TripSetupActivity (editing) | Ours: NewTripActivity.java edit mode + MainActivity.java overflow item | Step 3
- [x] **P-12** Shortening a trip is refused while stops exist on later days ("Move stops from later days before shortening this trip") | Kevin: prototype/TripSetupActivity.java#onCreate | Ours: NewTripActivity.java edit mode | Step 3
- [x] **P-13** Edit-mode note: changing city keeps stops; changing currency keeps the numeric budget, so review category limits | Kevin: prototype/TripSetupActivity.java#onCreate | Ours: NewTripActivity.java helper text | Step 3
- [x] **P-14** City choice explains that discovery covers Tokyo and Manila and that a pin can go anywhere | Kevin: prototype/TripSetupActivity.java#onCreate | Ours: NewTripActivity.java city helper | Step 3
- [x] **P-15** Home trip card shows city, trip length, stop count, "Sample trip", start date and total budget (also resolves critique `home.md` #4) | Kevin: prototype/TripsActivity.java#render | Ours: ui/TripAdapter.java, ui/HomeTripRow.java, viewmodel/HomeViewModel.java | Step 3
- [x] **P-16** Trips list shows the newest trip first | Kevin: prototype/DraftStore.java#save (`add(0,…)`) | Ours: data/local/dao/TripDao.java#observeAll (currently `ORDER BY name`) | Step 3
- [x] **P-17** Sample-place search pre-fills name, lat/lng, address, category, duration, cost, price currency and outdoor flag | Kevin: prototype/StopEditorActivity.java#render (search) | Ours: AddDestinationActivity.java + merged catalog | Step 6
- [x] **P-18** No-match search message "No sample matches. Add a name or choose a map pin." | Kevin: prototype/StopEditorActivity.java#render | Ours: AddDestinationActivity.java (`search_empty_state`) | Step 6
- [x] **P-19** Map picker opens at the stop's saved pin, otherwise at the city centre | Kevin: prototype/StopEditorActivity.java#render (map button) | Ours: AddDestinationActivity.java, StopDetailActivity.java (new "Adjust pin" action) | Step 4
- [x] **P-20** Picked coordinates shown immediately as "Selected pin: %.5f, %.5f"; with no pin "No pin selected · you can still add a stop by name"; empty name defaults to "Pinned destination" | Kevin: prototype/StopEditorActivity.java#updateLocation, mapPicker callback | Ours: AddDestinationActivity.java, StopDetailActivity.java | Step 4
- [x] **P-21** A saved pin is read back and shown when the stop is reopened | Kevin: prototype/StopEditorActivity.java + DraftStore | Ours: data/local/ItineraryItemWithPlace.java + ItineraryItemDao projection (`p.lat, p.lng`), StopDetailActivity.java | Step 4
- [x] **P-22** Map tap, long-press and drag place the pin; coordinates return without geocoding; OSM attribution shown; device test kept | Kevin: app/src/androidTest/.../prototype/PrototypeDeviceTest.java#mapTapReturnsTheNewCoordinatesWithoutGeocoding | Ours: MapPickerActivity.java (shared) | Step 10
- [x] **P-23** Stop editor draft (all fields including pin) survives rotation | Kevin: prototype/StopEditorActivity.java#onSaveInstanceState | Ours: StopDetailActivity.java, AddDestinationActivity.java | Step 4
- [ ] **P-24** Stop day validated to 1..trip days (not "max existing day + 1") | Kevin: prototype/StopEditorActivity.java (`number(day,1,trip.days)`) | Ours: AddDestinationActivity.java#validateDay, StopDetailActivity.java#pickDay | Step 4
- [x] **P-25** Leg distance = haversine on real stop coordinates × 1.25 road factor (no name-hash coordinates) | Kevin: prototype/RoutePreview.java#distance/travel | Ours: prototype/RouteMath.java, TravelMode.java, OptimizeActivity.java, ui/ItineraryAdapter.java#legMinutes (retire PrototypeStore#legKm, MockData#coordFor) | Step 4
- [x] **P-26** A leg touching a stop with no pin uses a 20-minute placeholder; card text "No map pin yet · travel uses a 20 min placeholder" | Kevin: prototype/RoutePreview.java#travel, PlannerActivity.java#render | Ours: prototype/TravelMode.java, ui/ItineraryAdapter.java | Step 4
- [x] **P-27** Route preview shows current vs preview incoming-travel minutes, the timed new order with ⚠ on conflicts, and "Keep current" / "Apply order" | Kevin: prototype/PlannerActivity.java "Preview a shorter route" | Ours: OptimizeActivity.java | Step 4
- [x] **P-28** Optimizer schedules flexible stops nearest-neighbour around booked anchors by time; a flexible stop that cannot finish (with travel + buffer) before the next booking goes after it; booked times never change (`RoutePreviewTest#fixedBookingsKeepTheirTimeAndFlexibleStopsFitAroundThem`) | Kevin: prototype/RoutePreview.java#day(optimize=true) | Ours: prototype/RouteMath.java + ScheduleMath.java | Step 4
- [x] **P-29** Notice that travel uses straight-line estimates, booked times stay fixed, and live routing is not connected | Kevin: prototype/PlannerActivity.java notice | Ours: OptimizeActivity.java (`text_locked_note`) | Step 4
- [x] **P-30** Stay duration validated to 5..720 minutes; blank = 60 | Kevin: prototype/StopEditorActivity.java (`number(duration,5,720)`) | Ours: StopDetailActivity.java#save, AddDestinationActivity.java#persist | Step 4
- [x] **P-31** Catalog places carry a suggested duration that is used when added | Kevin: prototype/DemoCatalog.java Place#minutes, #stop | Ours: merged catalog, data/TripRepositoryImpl.java insert | Step 6
- [x] **P-32** Buffer per stop before its incoming leg, 0..180 min, default 15, applied only when a previous stop exists | Kevin: prototype/DraftTrip.java Stop#buffer, StopEditorActivity.java, RoutePreview.java#day | Ours: ItineraryItemEntity.bufferMin (existing column), StopDetailActivity.java, prototype/ScheduleMath.java | Step 4
- [x] **P-33** Arrival = previous end + travel + buffer (`RoutePreviewTest#perLegModeAndBufferAffectArrival`) | Kevin: prototype/RoutePreview.java#day | Ours: prototype/ScheduleMath.java | Step 4
- [x] **P-34** A booked stop that overlaps the previous stop's end (earliest arrival after its fixed time) or ends after 24:00 is flagged [CHECK CONFLICT]; its time is not moved (`RoutePreviewTest#overlappingBookingsAreFlaggedWithoutMovingTheirTimes`) | Kevin: prototype/RoutePreview.java#day | Ours: prototype/ScheduleMath.java | Step 4
- [x] **P-35** A flexible stop ending after 20:00 is flagged as a conflict | Kevin: prototype/RoutePreview.java#day (`>1200`) | Ours: prototype/ScheduleMath.java | Step 4
- [x] **P-36** Conflicting stop card shows "Schedule conflict: check travel time, booked blocks or the 20:00 day limit." (text, not colour alone) | Kevin: prototype/PlannerActivity.java#render | Ours: ui/ItineraryAdapter.java, res/layout/item_stop.xml | Step 4
- [x] **P-37** Stop card shows computed start–end and "BOOKED / FIXED" or "FLEXIBLE" | Kevin: prototype/PlannerActivity.java#render | Ours: ui/ItineraryAdapter.java#resolveMetaText | Step 4
- [x] **P-38** Day tabs list every trip day 1..N, including empty days; selected day clamped to trip length and kept across rotation | Kevin: prototype/PlannerActivity.java#render, #onSaveInstanceState | Ours: MainActivity.java#renderDayTabs | Step 4
- [x] **P-39** Day heading shows the calendar date ("Day 2 · Wed, Dec 2"); falls back to "Day N" on a bad date | Kevin: prototype/PrototypeActivity.java#date, PlannerActivity.java#render | Ours: MainActivity.java (`text_day_summary`) | Step 4
- [x] **P-40** Balance days: flexible stops go to the least-loaded day (duration + buffer); booked stops keep day and time; confirmation dialog explains it | Kevin: prototype/PlannerActivity.java "Spread flexible stops across days" | Ours: OptimizeActivity.java + TripRepository batch day update | Step 4
- [x] **P-41** Empty day copy "A day full of possibilities. Add your first stop or discover a suggested plan." | Kevin: prototype/PlannerActivity.java#render | Ours: MainActivity.java (`empty_state`) | Step 4
- [x] **P-42** Day total line: minutes at stops + minutes of travel, "plus buffers" | Kevin: prototype/PlannerActivity.java#render | Ours: MainActivity.java#renderSelectedDayStops (`R.plurals.day_summary`) | Step 4
- [x] **P-43** Leg mode persisted per leg with options Suggested / Walking / Cycling / Driving / Transit | Kevin: prototype/DraftTrip.java Stop#mode, StopEditorActivity.java | Ours: ItineraryItemEntity.transportMode (existing column), MainActivity.java#onLegClick, StopDetailActivity.java | Step 4
- [x] **P-44** "Suggested" resolves by distance: under 1.2 km Walking, under 4 km Cycling, otherwise Transit; Walking when either end has no pin | Kevin: prototype/RoutePreview.java#mode | Ours: prototype/TravelMode.java#suggestFor | Step 4
- [x] **P-45** Leg minutes = ceil(km × 1.25 / speed × 60), speeds Walking 4.5, Cycling 15, Transit 18 (+8 min), Driving 25 km/h, minimum 1 | Kevin: prototype/RoutePreview.java#travel | Ours: prototype/TravelMode.java#minutesFor | Step 4
- [x] **P-46** Leg label "Mode · ~N min travel + M min buffer" | Kevin: prototype/PlannerActivity.java#render | Ours: ui/ItineraryAdapter.java#bindLeg | Step 4
- [x] **P-47** One catalog (Tokyo, Manila) with lat/lng, category, local price, rating, reviews, minutes, hidden, outdoor and description | Kevin: prototype/DemoCatalog.java | Ours: merged catalog class in prototype/ (replaces MockData lists) | Step 6
- [x] **P-48** Discover filters All / Famous (no Food) / Hidden gems / Food / Activities, plus text search | Kevin: prototype/DiscoverActivity.java#render | Ours: DiscoverActivity.java | Step 6
- [x] **P-49** User picks the target day; picks are added to that day | Kevin: prototype/DiscoverActivity.java#render (`targetDay`) | Ours: DiscoverActivity.java + TripRepositoryImpl.java#addCatalogPlaces | Step 6
- [x] **P-50** Suggestions sorted by distance from the target day's last pinned stop (else city centre), with a "Nearby X" label | Kevin: prototype/DiscoverActivity.java#render | Ours: DiscoverActivity.java, SuggestionsActivity.java | Step 6
- [x] **P-51** Card shows POPULAR PICK / HIDDEN GEM · category · Outdoors/Indoors, description, ★ rating · reviews · km, tier Free/$/$$ (Tokyo < ¥1,200, Manila < ₱200), local ≈ home price, minutes | Kevin: prototype/DiscoverActivity.java#render | Ours: DiscoverActivity.java, SuggestionsActivity.java (UiRow) | Step 6
- [x] **P-52** A place already in the itinerary (matched by name) shows "Already in your itinerary" and cannot be added | Kevin: prototype/DiscoverActivity.java#render | Ours: DiscoverActivity.java, SuggestionsActivity.java | Step 6
- [x] **P-53** "Build a suggested itinerary": up to 3 must-see + 2 hidden non-food, unplanned, pre-checked; added from the target day, 3 per day, capped at the last day; toast if none remain | Kevin: prototype/DiscoverActivity.java#suggestedPlan | Ours: DiscoverActivity.java | Step 6
- [x] **P-54** Swap a catalog place in for any flexible stop (keeps id, day, mode, buffer); booked never offered; toasts for nothing flexible or already planned | Kevin: prototype/DiscoverActivity.java#swap | Ours: DiscoverActivity.java + TripRepository in-place update | Step 6
- [x] **P-55** Empty result "No sample places match these filters…" | Kevin: prototype/DiscoverActivity.java#render | Ours: DiscoverActivity.java, SuggestionsActivity.java (`group_empty`) | Step 6
- [x] **P-56** Discover filter, query, target day and budget toggle survive rotation | Kevin: prototype/DiscoverActivity.java#onSaveInstanceState | Ours: DiscoverActivity.java#onSaveInstanceState | Step 6
- [x] **P-57** Notice that ratings, reviews, names and prices are illustrative and distances straight-line | Kevin: prototype/DiscoverActivity.java#render | Ours: DiscoverActivity.java, SuggestionsActivity.java | Step 6
- [x] **P-58** Adding a catalog place saves coordinates, category, cost + currency, duration and outdoor flag | Kevin: prototype/DemoCatalog.java Place#stop | Ours: data/TripRepositoryImpl.java (new insert-from-catalog), PrototypeStore | Step 6
- [x] **P-59** Remaining category budget = limit − max(planned, logged) | Kevin: prototype/DiscoverActivity.java#remaining | Ours: SuggestionsActivity.java#render | Step 6
- [x] **P-60** Each suggestion says "X remaining in Category" and "Over category budget"; the budget filter works for Manila too | Kevin: prototype/DiscoverActivity.java#render | Ours: SuggestionsActivity.java#toRow | Step 6
- [x] **P-61** Calendar preview needs at least one stop (verify; ours disables Sync with no events) | Kevin: prototype/SummaryActivity.java#render | Ours: CalendarSyncActivity.java#render | Step 8
- [x] **P-62** Preview dialog states event count, destination time zone, and that nothing is sent to Google; completion persisted and shown as "Preview reviewed · No events have been synced" | Kevin: prototype/SummaryActivity.java#render, DraftTrip#calendarPreview | Ours: CalendarSyncActivity.java, PrototypeStore#isSynced/markSynced (persistent) | Step 8
- [x] **P-63** Event preview per day with date, real start–end (travel + buffer), Booked tag, location (address, else pin, else city), ⚠ on conflict | Kevin: prototype/SummaryActivity.java#render | Ours: CalendarSyncActivity.java#showEvents | Step 8
- [x] **P-64** "N schedule warning(s)" shown before import/export | Kevin: prototype/SummaryActivity.java#render | Ours: CalendarSyncActivity.java, SummaryActivity.java | Step 8
- [x] **P-65** Save `.ics` through ACTION_CREATE_DOCUMENT (`text/calendar`, "Trippings-itinerary.ics"); refused with a message when there are no stops | Kevin: prototype/SummaryActivity.java (calendarExport) | Ours: CalendarSyncActivity.java | Step 8
- [x] **P-66** `.ics` content: VCALENDAR 2.0, PRODID, CALSCALE; one VEVENT per scheduled stop; UID `<id>@trippings.local`; DTSTAMP; DTSTART/DTEND in UTC from Asia/Tokyo or Asia/Manila; escaped SUMMARY/LOCATION/DESCRIPTION; 75-octet UTF-8-safe folding; CRLF; booked and conflict notes (`TripExportTest`) | Kevin: prototype/TripExport.java#calendar/utc/escape/fold | Ours: prototype/TripExport.java (kept) called from CalendarSyncActivity.java | Step 8
- [x] **P-67** Invalid trip date → `IllegalArgumentException("Invalid trip date")`, shown to the user, not a crash | Kevin: prototype/TripExport.java#utc, SummaryActivity.java#write | Ours: CalendarSyncActivity.java | Step 8
- [x] **P-68** Save toasts "Calendar file saved. Import it into your calendar when ready." / "Trip summary saved" / "Could not save the file. Choose a different location and try again." | Kevin: prototype/SummaryActivity.java#write | Ours: CalendarSyncActivity.java, SummaryActivity.java | Step 8
- [x] **P-69** Each stop has an estimated cost (0..1e8) in its own price currency (default local); changing city keeps it (`TripExportTest#changingCityDoesNotChangeAnExistingStopsPriceCurrency`) | Kevin: prototype/DraftTrip.java#costCurrency/convertedCost, StopEditorActivity.java | Ours: StopDetailActivity.java, AddDestinationActivity.java, PrototypeStore | Step 5
- [x] **P-70** Each stop has a budget category (Activities/Food/Transport/Other) | Kevin: prototype/StopEditorActivity.java | Ours: StopDetailActivity.java, PrototypeStore | Step 5
- [x] **P-71** Planned (converted stop costs) and logged totals shown separately, overall and per category, never added together (`TripExportTest#summaryKeepsEstimatesAndExpensesSeparate`) | Kevin: prototype/BudgetActivity.java#planned/actual/render | Ours: BudgetActivity.java, prototype/BudgetMath.java | Step 5
- [x] **P-72** Warning when the planned itinerary exceeds the trip budget | Kevin: prototype/BudgetActivity.java#render | Ours: BudgetActivity.java | Step 5
- [x] **P-73** Expense currency chosen per expense ("Paid in", default local) and kept | Kevin: prototype/BudgetActivity.java#render | Ours: ui/AddExpenseDialog.java | Step 5
- [x] **P-74** Expense amount must be 0.01..100,000,000 (title required, already matched) | Kevin: prototype/BudgetActivity.java#render | Ours: ui/AddExpenseDialog.java#parseAmount | Step 5
- [x] **P-75** Remove an expense, with confirmation | Kevin: prototype/BudgetActivity.java#render | Ours: BudgetActivity.java#renderExpenses, PrototypeStore (expense id) | Step 5
- [x] **P-76** Category limits each 0..1e8 and summed against the budget; daily mode scales category limits by days; editable amounts keep two decimals | Kevin: prototype/BudgetActivity.java#render, DraftTrip.java#categoryBudget | Ours: BudgetSetupActivity.java, prototype/BudgetSettings.java#limitFor | Step 5
- [x] **P-77** Currency calculator: amount (default 1000) from a chosen currency (default local) to home, labelled as sample or live rate | Kevin: prototype/BudgetActivity.java#render | Ours: BudgetActivity.java (new card or dialog) | Step 5
- [ ] **P-78** Notice naming the rate source and that estimates exclude unpriced travel legs | Kevin: prototype/BudgetActivity.java#render | Ours: BudgetActivity.java | Step 5
- [x] **P-79** One rates table (Kevin's sample rates merged with CurrencyMock) | Kevin: prototype/RoutePreview.java#convert/rate | Ours: data/CurrencyRates.java, prototype/CurrencyMock.java | Step 5
- [ ] **P-80** Stop card shows category · duration · estimated cost in home currency | Kevin: prototype/PlannerActivity.java#render | Ours: ui/ItineraryAdapter.java | Step 5
- [x] **P-81** Itinerary shows the trip budget and start date | Kevin: prototype/PlannerActivity.java overview card | Ours: MainActivity.java#bindSubtitle | Step 5
- [x] **P-82** Budget links to budget-friendly food and activities | Kevin: prototype/BudgetActivity.java#render | Ours: BudgetActivity.java → SuggestionsActivity.java | Step 5
- [x] **P-83** Weather screen: per-day forecast (Clear / Rain / Storm with sample temperature and chance), user-changeable, persisted per trip | Kevin: prototype/WeatherActivity.java#render | Ours: new WeatherActivity.java (ScreenActivity), PrototypeStore | Step 7
- [x] **P-84** Default forecast Rain on days 2, 5, 8…, Clear otherwise | Kevin: prototype/WeatherActivity.java#render | Ours: PrototypeStore (replaces MockData#isRainy) | Step 7
- [x] **P-85** Per-stop outdoor flag ("Outdoor activity (check weather)"), set from the catalog or by the user | Kevin: prototype/DraftTrip.java Stop#outdoor, StopEditorActivity.java | Ours: StopDetailActivity.java, PrototypeStore (replaces MockData#isOutdoor keywords) | Step 7
- [x] **P-86** On non-clear days every outdoor stop is listed; booked ones say "Booked: review with your provider" and get no swap button | Kevin: prototype/WeatherActivity.java#render | Ours: WeatherActivity.java | Step 7
- [x] **P-87** Indoor alternatives per stop: swap days with an indoor flexible stop on another day, or replace with an unplanned catalog indoor activity (keeps id, day, buffer, mode); toast when none | Kevin: prototype/WeatherActivity.java#alternatives | Ours: WeatherActivity.java + TripRepository in-place update | Step 7
- [x] **P-88** "No sample weather conflicts…", "No outdoor stops to flag on this day." and the not-live notice | Kevin: prototype/WeatherActivity.java#render | Ours: WeatherActivity.java | Step 7
- [x] **P-89** Weather is reachable from the itinerary (rain banner and overflow) | Kevin: prototype/PlannerActivity.java toolkit chip/button | Ours: MainActivity.java#onMenuItem, weatherBanner | Step 7
- [x] **P-90** Save the PDF through ACTION_CREATE_DOCUMENT ("Trippings-trip-summary.pdf") alongside share | Kevin: prototype/SummaryActivity.java (pdfExport, #write) | Ours: SummaryActivity.java | Step 8
- [x] **P-91** PDF: full summary, word-wrapped to width, blocks kept together, 43 lines/page, header and page footer, multi-page renderable | Kevin: prototype/TripExport.java#pdf | Ours: the one kept PDF writer (TripExport#pdf or ui/TripPdfWriter.java) | Step 8
- [x] **P-92** Shared text uses the same summary; subject "<trip name> · Trippings" | Kevin: prototype/SummaryActivity.java#render (share), TripExport.java#summary | Ours: SummaryActivity.java#shareText | Step 8
- [x] **P-93** Summary lists every trip day, with "No stops planned yet." for empty days | Kevin: prototype/TripExport.java#summary | Ours: SummaryActivity.java | Step 8
- [x] **P-94** Every exported amount has exactly two decimals (`TripExportAmountsTest`) | Kevin: prototype/TripExport.java#summary | Ours: prototype/TripExport.java (kept) | Step 8
- [x] **P-95** Export starts with the PROTOTYPE disclaimer and ends with the rates and travel-cost footnotes | Kevin: prototype/TripExport.java#summary | Ours: prototype/TripExport.java (kept) | Step 8
- [x] **P-96** Companions persisted per trip, shown with "You · Organizer"; a new trip starts with "You" | Kevin: prototype/GroupActivity.java#render, TripSetupActivity.java | Ours: GroupActivity.java#memberRows, PrototypeStore | Step 9
- [x] **P-97** Add companion: name required; case-insensitive duplicate rejected ("This person is already listed") | Kevin: prototype/GroupActivity.java#render | Ours: GroupActivity.java | Step 9
- [x] **P-98** Sample join code TRIP-DEMO (case-insensitive) adds "Sam (sample)" once; wrong code shows "Use the sample code TRIP-DEMO" | Kevin: prototype/GroupActivity.java#render | Ours: GroupActivity.java | Step 9
- [x] **P-99** One honest local vote per place, toggle on/off, "1 vote · You" / "0 votes", persisted per trip | Kevin: prototype/GroupActivity.java#render | Ours: GroupActivity.java#voteRow, PrototypeStore#voteCount/toggleVote | Step 9
- [x] **P-100** Vote candidates are the city's catalog activities | Kevin: prototype/GroupActivity.java#render | Ours: GroupActivity.java#destinationNames | Step 9
- [x] **P-101** Equal split of the planned total and of logged spending over the real member count, labelled "Equal split preview only…" | Kevin: prototype/GroupActivity.java#render | Ours: GroupActivity.java#renderSplit | Step 9
- [x] **P-102** Notice that members and votes are simulated locally and no invitations are sent | Kevin: prototype/GroupActivity.java#render | Ours: GroupActivity.java | Step 9
- [x] **P-103** Group links to Discover to choose destinations | Kevin: prototype/GroupActivity.java#render | Ours: GroupActivity.java | Step 9
- [x] **P-104** Journal photo picked with OpenDocument + takePersistableUriPermission and stored as a URI; still loads after restart; SecurityException message | Kevin: prototype/JournalActivity.java#onCreate | Ours: JournalActivity.java (replace PickVisualMedia), prototype/JournalEntry.java | Step 9
- [x] **P-105** Photo shown in the entry (at most 1200 px, 190dp center-crop, content description), with messages for undecodable or inaccessible images | Kevin: prototype/JournalActivity.java#addPhoto | Ours: JournalActivity.java | Step 9
- [x] **P-106** Entry attaches to a day ("Whole day") or to a stop (day follows the stop) | Kevin: prototype/JournalActivity.java#render | Ours: JournalActivity.java#bindStopField | Step 9
- [x] **P-107** Saving needs a note or a photo (photo-only allowed) | Kevin: prototype/JournalActivity.java#render | Ours: JournalActivity.java#submit | Step 9
- [x] **P-108** Edit an entry (note, photo, day, stop pre-filled) | Kevin: prototype/JournalActivity.java#render ("Edit memory") | Ours: JournalActivity.java | Step 9
- [x] **P-109** Remove an entry, with confirmation ("The original photo will stay in your gallery.") | Kevin: prototype/JournalActivity.java#render | Ours: JournalActivity.java | Step 9
- [x] **P-110** Remove the photo from an unsaved draft | Kevin: prototype/JournalActivity.java#render | Ours: JournalActivity.java | Step 9
- [x] **P-111** Journal draft keeps note, photo URI, edited entry id and day across rotation | Kevin: prototype/JournalActivity.java#onSaveInstanceState | Ours: JournalActivity.java#onSaveInstanceState (our draft Bundle; `draftOf` belongs to ui/AddExpenseDialog) | Step 9
- [ ] **P-112** Device tests rewritten against our screens: every toolkit screen opens with status-bar padding (including after resume), pin + expense survive reopening the store, PDF renders 2+ pages | Kevin: app/src/androidTest/.../prototype/PrototypeDeviceTest.java | Ours: same file, rewritten | Step 10
- [x] **P-113** Kevin's unit tests pass against the merged code: `RoutePreviewTest` (4), `TripExportTest` (4), `TripExportAmountsTest` (4) | Kevin: app/src/test/.../prototype/*.java | Ours: same files | Steps 4, 5, 8
- [ ] **P-114** Tapping a form's primary action hides the keyboard | Kevin: prototype/PrototypeActivity.java#button/hideKeyboard | Ours: forms that stay open after submit (BudgetActivity calculator, GroupActivity, WeatherActivity) | Step 12
- [x] **P-115** Fallback toast "This screen is not included in this build." for a missing screen. Not ported: our menu references classes directly, so a missing screen fails at compile time. Tick when Q-confirmed and all menu targets compile | Kevin: prototype/PrototypeActivity.java#openFeature | Ours: MainActivity.java#onMenuItem | Step 10
- [ ] **P-116** Brand tagline "Less planning. More exploring." | Kevin: prototype/TripsActivity.java#render | Ours: res/values/strings.xml (Login/Home) | Step 11
- [x] **P-117** Kevin's screen headlines ("A little budget clarity", "Better together", …). Decision: keep our critiqued titles; his title assertions in `PrototypeDeviceTest` are rewritten. Tick when the team agrees (Q20) | Kevin: prototype/*Activity.java#page titles | Ours: res/values/strings_prototype.xml | Step 10

_Added by the architect review (items found only by scanning every Kevin class):_

- [x] **P-118** A new stop defaults to the itinerary's selected day | Kevin: prototype/PlannerActivity.java#edit (`putExtra("day",selectedDay)`), StopEditorActivity.java#onCreate | Ours: MainActivity.java (FAB passes the day), AddDestinationActivity.java | Step 4
- [ ] **P-119** Typed (non-catalog) stop defaults: category Activities, buffer 15, mode Suggested, cost 0 in the trip's local currency, outdoor = true | Kevin: prototype/DraftTrip.java#Stop initialisers | Ours: prototype/PrototypeStore.java (per-item defaults), AddDestinationActivity.java | Step 5
- [x] **P-120** Destination name is required ("Enter a destination name") (verify) | Kevin: prototype/StopEditorActivity.java#render | Ours: AddDestinationActivity.java, StopDetailActivity.java | Step 4
- [x] **P-121** Removing a stop asks for confirmation and also deletes that stop's store record (cost, currency, category, outdoor) so no orphan data is left | Kevin: prototype/StopEditorActivity.java#render ("Remove this stop?") | Ours: StopDetailActivity.java#confirmDelete (verify), data/TripRepositoryImpl.java#deleteItineraryItem, PrototypeStore | Step 5
- [x] **P-122** A journal entry whose stop was removed still renders as "Day N" | Kevin: prototype/JournalActivity.java#render | Ours: JournalActivity.java | Step 9
- [x] **P-123** The itinerary display (not only the optimizer) keeps flexible stops in list order but moves any that cannot finish (travel + buffer) before the next booking to after it | Kevin: prototype/RoutePreview.java#day(optimize=false) | Ours: merged ScheduleMath/RoutePreview, MainActivity.java, CalendarSyncActivity.java, SummaryActivity.java | Step 4
- [x] **P-124** Each day starts at 09:00 and its first stop has no travel or buffer (verify) | Kevin: prototype/RoutePreview.java#day | Ours: prototype/ScheduleMath.java | Step 4
- [x] **P-125** A pinned stop's card shows its coordinates "%.5f, %.5f" | Kevin: prototype/PlannerActivity.java#render | Ours: ui/ItineraryAdapter.java | Step 4
- [x] **P-126** A booked time uses a 24-hour picker that defaults to 09:00 for a new booking | Kevin: prototype/StopEditorActivity.java#render | Ours: StopDetailActivity.java, AddDestinationActivity.java, ui/TimeFields.java (verify) | Step 4
- [x] **P-127** Balance days covers the whole trip: booked stops count as fixed load (duration + buffer) on their day, flexible stops are handled in itinerary order across all days, and empty days receive stops | Kevin: prototype/PlannerActivity.java#render | Ours: OptimizeActivity.java + TripRepository batch day update | Step 4
- [x] **P-128** Stop-editor sample search is a case-insensitive substring match over the trip city's catalog; a blank query lists every sample place; a pick sets the address to "<name>, <city>" | Kevin: prototype/StopEditorActivity.java#render, DemoCatalog.java#Place#stop | Ours: AddDestinationActivity.java + merged catalog | Step 6
- [ ] **P-129** Confirmations after changes: "Added to your itinerary", "N stops added across your trip days", "Stop swapped", "Itinerary updated. Review the new schedule.", "Budget saved" | Kevin: prototype/DiscoverActivity.java, WeatherActivity.java, BudgetActivity.java | Ours: DiscoverActivity.java, SuggestionsActivity.java, WeatherActivity.java, BudgetSetupActivity.java (Snackbars per DESIGN.md) | Step 5
- [x] **P-130** An out-of-range or non-finite number is rejected with the allowed range in the field error, never silently clamped | Kevin: prototype/PrototypeActivity.java#number | Ours: StopDetailActivity.java, AddDestinationActivity.java, NewTripActivity.java, BudgetSetupActivity.java, ui/AddExpenseDialog.java, BudgetActivity.java | Step 3
- [x] **P-131** Trip name is required ("Give your trip a name") (verify) | Kevin: prototype/TripSetupActivity.java#onCreate | Ours: NewTripActivity.java#attemptCreate | Step 3
- [x] **P-132** New-trip defaults: start date today, 3 days, home currency PHP, budget 15,000 with the 30/40/20/10 split; creating the trip opens the itinerary | Kevin: prototype/TripSetupActivity.java#onCreate, DraftTrip.java | Ours: NewTripActivity.java | Step 3
- [x] **P-133** PHP, JPY, USD and EUR are offered for home currency, stop price, expense "Paid in" and the calculator (verify against the merged rates table) | Kevin: TripSetupActivity.java, StopEditorActivity.java, BudgetActivity.java (`CURRENCIES`) | Ours: NewTripActivity.java, StopDetailActivity.java, ui/AddExpenseDialog.java, BudgetActivity.java | Step 5
- [ ] **P-134** Each category card shows planned and logged, a logged-vs-limit progress bar and "X remaining of Y · Over budget"; empty history reads "No expenses yet…" (verify) | Kevin: prototype/BudgetActivity.java#render | Ours: BudgetActivity.java | Step 5
- [x] **P-135** All rate text comes from the single rates table: no hard-coded "1 JPY = 0.38 PHP…" in the Budget notice or export footnote; it says "sample" or "as of <date>" | Kevin: BudgetActivity.java#render, TripExport.java#summary | Ours: BudgetActivity.java, prototype/TripExport.java | Step 5
- [x] **P-136** Journal form: day picker lists every trip day, stop picker shows "Day N · name", status "Photo attached · visible only in this local journal", form resets after saving | Kevin: prototype/JournalActivity.java | Ours: JournalActivity.java | Step 9
- [x] **P-137** The forecast fills new days with defaults when the trip gets longer; each day heading reads "Day N · EEE, MMM d" | Kevin: prototype/WeatherActivity.java#render | Ours: WeatherActivity.java, PrototypeStore | Step 7
- [x] **P-138** The group always contains "You", so the cost split never divides by zero | Kevin: prototype/GroupActivity.java#render | Ours: GroupActivity.java#renderSplit, PrototypeStore | Step 9

---

## 4. Screen deletion gate (for plan step 10)

**Rule:** a row may be deleted only when **every** ID in it is ticked in section 3 **and** reachable from Login in our flow. Any unticked ID blocks that deletion and is reported to the user. The logic classes `RoutePreview`, `TripExport` and `DemoCatalog` are not deleted, but they depend on `DraftTrip` (see Q4).

| Kevin class (`<pkg>/prototype/`) | Checklist IDs it provides | Target in our UI that must make them reachable |
|---|---|---|
| `TripsActivity` | P-03, P-06, P-07, P-15, P-16, P-116 | P-03, P-15, P-16 → `HomeActivity` (`ui/TripAdapter`). P-06, P-07 → `ProfileActivity`. P-116 → `LoginActivity`/`HomeActivity` strings |
| `TripSetupActivity` | P-08, P-09, P-10, P-11, P-12, P-13, P-14, P-96 | P-08, P-09, P-11 to P-14 → `NewTripActivity` (create and edit). P-10 → `BudgetSetupActivity`. P-96 → `GroupActivity` |
| `PlannerActivity` | P-11, P-26, P-27, P-29, P-36, P-37, P-38, P-39, P-40, P-41, P-42, P-46, P-80, P-81, P-89 | P-26, P-36, P-37, P-38, P-39, P-41, P-42, P-46, P-80, P-81, P-89 → `MainActivity` / `ui/ItineraryAdapter`. P-27, P-29, P-40 → `OptimizeActivity`. P-11 → `MainActivity` overflow → `NewTripActivity` |
| `StopEditorActivity` | P-17, P-18, P-19, P-20, P-21, P-23, P-24, P-30, P-32, P-43, P-69, P-70, P-85 | P-17, P-18 → `AddDestinationActivity`. P-19 to P-21, P-23, P-24, P-30 → `AddDestinationActivity` and `StopDetailActivity`. P-32, P-43, P-69, P-70, P-85 → `StopDetailActivity` (P-43 also `MainActivity` leg chip) |
| `DiscoverActivity` | P-48, P-49, P-50, P-51, P-52, P-53, P-54, P-55, P-56, P-57, P-59, P-60 | P-48, P-49, P-53, P-54, P-56 → `DiscoverActivity` (root). P-50 to P-52, P-55, P-57 → `DiscoverActivity` and `SuggestionsActivity`. P-59, P-60 → `SuggestionsActivity` |
| `BudgetActivity` | P-71, P-72, P-73, P-74, P-75, P-76, P-77, P-78, P-82 | P-71, P-72, P-75, P-77, P-78, P-82 → `BudgetActivity` (root). P-73, P-74 → `ui/AddExpenseDialog`. P-76 → `BudgetSetupActivity` |
| `WeatherActivity` | P-83, P-84, P-86, P-87, P-88 | New root `WeatherActivity`, opened from `MainActivity` (P-89) |
| `GroupActivity` | P-96, P-97, P-98, P-99, P-100, P-101, P-102, P-103 | `GroupActivity` (root) |
| `JournalActivity` | P-104, P-105, P-106, P-107, P-108, P-109, P-110, P-111 | `JournalActivity` (root) |
| `SummaryActivity` | P-61, P-62, P-63, P-64, P-65, P-67, P-68, P-90, P-92 | P-61 to P-65, P-67, P-68 → `CalendarSyncActivity`. P-64, P-68, P-90, P-92 → `SummaryActivity` (root) |
| `PrototypeActivity` | P-05, P-39, P-114, P-115, P-117 | P-05 → `ScreenActivity`. P-39 → `MainActivity`. P-114 → forms listed in P-114. P-115 → `MainActivity#onMenuItem`. P-117 → `strings_prototype.xml` |
| `DraftStore` | P-02, P-03, P-16, P-112 | `prototype/PrototypeStore` (persistent), `TripDao#observeAll`, `PrototypeDeviceTest` |
| `DraftTrip` | P-02, P-10, P-69, P-76, P-113, plus every item served through `RoutePreview` (P-25, P-28, P-33, P-34, P-35, P-44, P-45), `TripExport` (P-66, P-91, P-93, P-94, P-95) and `DemoCatalog` (P-04, P-31, P-47, P-58) | `PrototypeStore` + Room. Deletable only if those logic classes and tests no longer reference `DraftTrip`, or if Q4 decides to keep it as a pure snapshot model |

Shared classes that stay and must keep their items: `MapPickerActivity` (P-22) and `data/RemoteDataSource` (P-01). Manifest entries to remove at step 10: the 10 `.prototype.*` `<activity>` lines in `app/src/main/AndroidManifest.xml`.

---

## 5. Data-model and persistence notes

### Field mapping (DraftTrip → ours)

| `DraftTrip` field | Ours today | Proposed home |
|---|---|---|
| `id` | `TripEntity.id` | same |
| `name` | `TripEntity.name` | same |
| `city` | `TripMeta.city` (session) | persistent store document (Room `trips` has no city column) |
| `startDate` (ISO) | `TripMeta.dates` label, no year (session); `TripEntity.startDate` always null | `TripEntity.startDate` (existing column, no migration) |
| `days` | `TripMeta.days` (session) | `TripEntity.endDate` (derived) or the store. Architect to pick one source |
| `homeCurrency` | `BudgetSettings.homeCurrency` (session); `TripEntity.homeCurrency` hard-coded `"USD"` | one source of truth (Q24) |
| `budget`, `dailyBudget`, `food/activity/transport/otherBudget` | `BudgetSettings` (session); `TripEntity.budget` null | persistent `BudgetSettings` in the store |
| `sample` | implicit (`SEED_TRIP_ID`) | the store, or keep the `SEED_TRIP_ID` check |
| `calendarPreview` | `PrototypeStore.syncedTrips` (session) | persistent store |
| `stops[].id/day/duration/booked/fixedMinute` | `ItineraryItemEntity.id/day/durationMin/isBooked/time ("HH:mm")` | same. Order within a day: Kevin uses list order, ours `position` |
| `stops[].buffer` | not saved (`ItineraryItemEntity.bufferMin` is always null) | `bufferMin` (existing column) via a new DAO update |
| `stops[].mode` (incoming leg) | `PrototypeStore.legModes` keyed by the **outgoing** item (session) | `transportMode` (existing column). Q10 decides incoming vs outgoing |
| `stops[].name/lat/lng/hasLocation/address` | `customName`, or `PlaceEntity` name/lat/lng/address for place-backed stops (coordinates never read back) | `PlaceEntity` via an extended `ItineraryItemWithPlace` projection. Custom stops have no coordinates (P-26 placeholder) |
| `stops[].category/cost/currency/outdoor` | none | persistent store keyed by item id (no migration), or Room v2 (Q9) |
| `expenses[]` (id, title, category, currency, amount) | `prototype/Expense` (no id), session | persistent store; add `id` for P-75. Room `ExpenseEntity` exists but is unused and has no `title` |
| `members[]` | hard-coded `GroupActivity.MEMBERS` | persistent store, per trip |
| `votes[]` | `PrototypeStore.myVotes`, **global**, not per trip | persistent store, per trip |
| `journal[]` (id, day, stopId, note, photo URI) | `JournalEntry(day, stopName, note, hasPhoto)` | extend with id, stopId, photoUri. Persistent store |
| `weather[]` | none (`MockData.isRainy`) | persistent store |

### Room migration status

- `data/local/TripFitDatabase.java`: `version = 1`, `exportSchema = false`. There is no `app/schemas/` folder, no `Migration`, and no `fallbackToDestructiveMigration`.
- Any change to an entity (a new column or table) will crash existing installs on open with an `IllegalStateException` unless the version is bumped to 2 **and** a `Migration` is added. The alternative, destructive fallback, wipes every saved trip and stop.
- **Recommendation:** avoid schema changes in steps 2 to 9.
  - Use the existing unused columns: `bufferMin`, `transportMode`, `startDate`, `endDate`, `homeCurrency`, `budget`.
  - Read stored `PlaceEntity.lat/lng` through a projection change, which is not a schema change.
  - Keep per-stop cost, currency, category and outdoor in the persistent `PrototypeStore`, keyed by item id.
  - If the team wants those fields in Room, do it in a single migration (v1→v2, `ALTER TABLE itinerary_items ADD COLUMN …`), turn on `exportSchema`, and add a migration test.
- In-place replacement (P-54, P-87) and batch day moves (P-40) need new `ItineraryItemDao` update queries only.
- Inserting catalog picks as `PlaceEntity` rows needs a matching `CityEntity`, because of the foreign key. Today those rows exist only after `fetchCities` has run (offline ids `offline-tokyo`/`offline-manila`).

### What Kevin's JSON holds that ours has no field for

- Custom-stop coordinates and address.
- Per-stop cost, price currency, category and outdoor flag.
- An explicit "Suggested" mode.
- Per-stop buffer (the column exists but is never written).
- Expense ids and per-expense currency choice.
- Members, per-trip votes and per-day weather.
- Journal entry ids, `stopId`, and persisted photo URIs (their URI permissions stay granted to the app).
- A persisted calendar-preview flag, the `sample` flag, and the ISO start date.

### Kevin's already-saved drafts

- They live in SharedPreferences file `trippings_prototype_v1`, key `trips`, as one JSON array. They can only exist on devices that ran his flow (team phones and emulators).
- **Recommendation:** drop them rather than import them.
  - The new persistent store **must use a different file name**, for example `trippings_store_v2`. Otherwise Gson would read his `DraftTrip` array as the new format.
  - Step 10 can delete the old file on startup and release any persisted photo URI permissions.
- This is an open question (Q3). It blocks step 10, and the file-name part blocks step 2.

---

## 6. Open questions for the team

1. **BLOCKING (step 8, step 11).** Is the app name **Trippings** or **TripFit**? The plan says Trippings. `strings.xml` `app_name` and `settings.gradle.kts` already say Trippings. These still say TripFit: README, DESIGN.md, PRODUCT.md, `strings_prototype.xml` (`setting_about`, `about_message`, `itinerary_empty_body`, `summary_share_subject`, `invite_message`) and the `tripfit-` file prefix in `ui/TripPdfWriter.java#safeName`. Kevin's `TripExport` writes "TRIPPINGS" and "@trippings.local". Proposal: package and theme names (`Theme.TripFit`) stay; user-facing text says Trippings.
2. **BLOCKING (step 2).** Confirm that **our screens are the base UI**. If the team prefers Kevin's screens, stop after step 1 and re-plan, as the plan's Direction section says.
3. **BLOCKING (step 2: file name; step 10: data).** Kevin's saved drafts in `trippings_prototype_v1`: drop them (recommended) or import them once into Room and the store? Either way the new store uses a different prefs file.
4. **BLOCKING (step 4).** The plan deletes `DraftTrip` at step 10 but also says `RoutePreview`, `TripExport`, `DemoCatalog` and their tests are "kept, not rewritten". All of them take `DraftTrip` as input. Choose one:
   - (a) Keep `DraftTrip` as a pure in-memory snapshot, built from Room and the store by an adapter, and remove it from the step 10 delete list (recommended, because tests stay unchanged).
   - (b) Retarget the classes to a new model and minimally adapt the tests.
5. **BLOCKING (step 4).** Travel constants conflict. `TravelModeTest` asserts walking at 5 km/h (1 km = 12 min) and `suggestFor(3.0) == TRANSIT`. Kevin's model uses 4.5 km/h × 1.25 and Suggested(3 km) = Cycling. Only one model may remain, so one test file must change. Which constants win?
6. **BLOCKING (step 5).** Rates. `RoutePreviewTest` asserts 1000 JPY = 380 PHP. `CurrencyMockTest` asserts 150 JPY = 56 PHP (373.33 per 1000). Live Frankfurter rates make any conversion non-deterministic unless tests call `CurrencyRates.useFallback()`. Which numbers become the single fallback table, and do the UI notices say "sample" or "live as of <date>"?
7. **BLOCKING (step 5).** Daily-budget semantics. Kevin multiplies the category limits by days and checks their sum against the per-day amount. Ours treats category limits as trip totals, checked against the trip total. Pick one.
8. **Non-blocking (step 5).** Default category split: Kevin's 30/40/20/10 % of the budget, or our fixed ₱4,000/5,000/3,000/3,000 scaled to home currency?
9. **BLOCKING (step 2, step 4).** Where do per-stop cost, currency, category, outdoor flag and custom-stop coordinates live: the persistent `PrototypeStore` keyed by item id (no migration, recommended) or Room v2 with a migration? The architect should confirm.
10. **BLOCKING (step 4).** Leg-mode semantics: Kevin stores the mode on the **destination** stop (incoming leg). Ours keys it by the **origin** stop (outgoing chip). Choose one before writing `transportMode`.
11. **Non-blocking (step 4).** Profile's global buffer setting: keep it as the default buffer for new stops (recommended), or remove it now that buffers are per stop?
12. **BLOCKING (step 8).** Keep one PDF writer. Recommendation: `TripExport#pdf`. It writes to any `OutputStream` (works for both the file picker and the FileProvider share), wraps lines, and is covered by `PrototypeDeviceTest`. Then delete `ui/TripPdfWriter.java`.
13. **Non-blocking (step 8).** Calendar Sync currently shows a simulated "N events added" success. Kevin's wording says explicitly that nothing is sent to Google. Keep the simulated sync state, reword it as a preview, or both?
14. **Non-blocking (steps 3 and 6).** New Trip offers 6 cities (`MockData.CITIES`) but catalog, Discover, offline search and the time zones only cover Tokyo and Manila. Limit the picker or keep 6 with a note?
15. **Non-blocking (step 9).** Group: should one code serve both share and join (ours `TRIP-7K2Q` vs Kevin's `TRIP-DEMO`)? Should the fake members (Ana, Marco, Lia), the fake vote base counts and the custom 40/30/20/10 split be removed in favour of real local companions and honest counts (recommended)? Should vote candidates be catalog activities or itinerary stops?
16. **Non-blocking (step 9).** Removing a companion is in the plan but not in Kevin's code. Confirm it is wanted.
17. **Non-blocking (step 9).** Journal targets: keep our trip-wide "General" option alongside Kevin's day-level and stop-level attachment (recommended)? What happens to the unsaved photo button on Stop Detail: remove it, or route it to a journal entry?
18. **Non-blocking (step 3).** Home ordering: newest first (Kevin) or by name (ours)?
19. **Non-blocking (step 6).** Sample trip name and contents: "Tokyo Trip" (ours, includes Nakamise) or "Tokyo, at your own pace" (catalog-based)?
20. **Non-blocking (step 10).** Screen titles: keep our critiqued titles (recommended) and drop Kevin's headline copy? His device test asserts his titles.
21. **BLOCKING (step 2).** Kevin has to be told about the plan before step 2 so he stops building on the screens that step 10 removes. This is a plan rule.
22. **Non-blocking (step 3).** Where does the "drafts stay on this device / sample data" notice go: the About dialog (recommended, keeps Home's design) or a Home notice card?
23. **Non-blocking (step 7).** Weather conditions and default forecast: adopt Kevin's Clear/Rain/Storm and the days 2, 5, 8 rain pattern?
24. **BLOCKING (step 3).** Single source of truth for the home currency and the trip length: `TripEntity` columns, the store, or both kept in sync? (`TripEntity.homeCurrency` is currently hard-coded to `"USD"` while the budget defaults to PHP.)

### Decisions (architect review, applied unless the team objects)

Every question except **Q21** is settled by the plan's own rules or the code. Only Q21 needs a human.

| Q | Decision | Why | Blocks |
|---|---|---|---|
| 1 | User-facing text says **Trippings**. Package, `Theme.TripFit`, `tripfit.db`, class names stay | The user confirmed the name; `strings.xml` and `settings.gradle.kts` already say it | none |
| 2 | Our screens are the base UI | Plan "Direction" | none |
| 3 | New store file `trippings_store_v2`. Never read, import or delete `trippings_prototype_v1`; leave it orphaned | Sample data only; deleting is irreversible and gains nothing | step 2 (file name) |
| 4 | Steps 4-9: adapter `prototype/TripSnapshots#from(TripEntity, items, PrototypeStore)` builds a `DraftTrip` on demand; `RoutePreview`, `TripExport`, `DemoCatalog` keep their bodies; the store never serializes `DraftTrip`. Step 10: `git mv DraftTrip.java TripSnapshot.java`, rename the type in his logic classes and tests (assertions unchanged), delete `DraftStore` | Satisfies both "keep his logic" and "step 10 deletes `DraftTrip`/`DraftStore`" | step 4, step 10 |
| 5 | **Kevin's constants win.** `TravelMode` keeps the enum and owns the numbers: Walk 4.5, Cycle 15, Drive 25, Transit 18 + 8 min, x1.25 road factor, `ceil`, minimum 1; Suggested = Walk under 1.2 km, Cycle under 4 km, else Transit (straight-line km). `transportMode == null` means Suggested. **Only `TravelModeTest` changes** (`suggestFor(3.0)` becomes CYCLE, `suggestFor(12.0)` TRANSIT, `WALK.minutesFor(1.0)` 17, add `suggestFor(5.0)` = TRANSIT) | `RoutePreviewTest` asserts relations, not constants | step 4 |
| 6 | One table: `CurrencyMock.PER_USD` behind `CurrencyRates` (live Frankfurter when available). `RoutePreview.convert` delegates to `CurrencyRates.convert`. `RoutePreviewTest` gets `@Before CurrencyRates.useFallback()` and lines 33-34 become 150 JPY = 56 PHP and 56 PHP = 150 JPY. UI says "sample rate" when not live and "rate as of <date>" when live | Two lines change on his side instead of three assertions on ours | step 5 (+8 footnote) |
| 7 | Kevin's semantics: in daily mode the category limits are per day too; the sum is checked against the per-day amount. Add `BudgetSettings#limitFor(category, days)` | One unit on both sides of the check | step 5 |
| 8 | Kevin's 30/40/20/10 split of the budget, recomputed when the budget changes; remove the fixed 4000/5000/3000/3000 | Works in any currency | none |
| 9 | Cost, price currency, category, outdoor live in the persistent store keyed by item id. Coordinates live in `PlaceEntity` (as map pins already do) via a new `ItineraryItemDao#updatePlace`. Removing a stop removes its store record. **No Room migration** | Avoids v1 to v2 | steps 2, 4, 5 |
| 10 | **Incoming leg (Kevin's).** The chip between stop i and i+1 edits item i+1's `transportMode`/buffer | `RoutePreview.mode(from,to)` reads `to.mode`; `RoutePreviewTest` relies on it | step 4 |
| 11 | Keep Profile's buffer as the default for new stops and for rows whose `bufferMin` is null | Both default to 15 | none |
| 12 | Keep `TripExport#pdf`, delete `ui/TripPdfWriter`; move `safeName` to `ui/ExportNames` with prefix `trippings-` and update both `InputParsingTest#safeName_*` | One writer, works for picker and share | step 8 |
| 13 | Calendar Sync becomes a preview: drop the fake "N events added"; keep a persisted "Preview reviewed · No events synced" state and the `.ics` save | Nothing really syncs | none |
| 14 | Limit the New Trip city picker to Tokyo and Manila, with the P-14 helper text | Catalog, pins, currency and `.ics` zones only support those two | step 3 |
| 15 | One code `TRIP-DEMO` shown, shared and accepted; remove fake members and base vote counts; equal split over real members (drop the 40/30/20/10 custom split); vote candidates are the city's catalog activities | Honest counts, variable member count | none |
| 16 | Yes: remove any companion except "You", with confirmation | Plan step 9 says "add and remove" | none |
| 17 | Keep "General" plus day- and stop-level attachment; Stop Detail's photo button opens Journal with that stop preselected | Removes a control that saved nothing | none |
| 18 | Newest first via `ORDER BY rowid DESC` (no migration) | P-16 | none |
| 19 | Keep "Tokyo Trip"; add Nakamise to the merged catalog; seed stops get catalog coordinates, cost, outdoor flags; label "Sample trip", members "You" and "Alex (sample)" | P-03, P-04 | step 6 |
| 20 | Keep our critiqued titles; rewrite the title assertions in his device test | Our screens are the base | none |
| 21 | **The user tells Kevin before step 2.** | Plan rule; cannot be done by the agent | **step 2; needs the user** |
| 22 | Put the notice in the About dialog | Home's design unchanged | none |
| 23 | Adopt Clear/Rain/Storm, rain on days 2, 5, 8..., replacing `MockData.isRainy` | P-83, P-84 | none |
| 24 | **The persistent store's `TripMeta` is the single source** for city, ISO start date, days, home currency. `BudgetSettings` reads currencies from it. `TripEntity.startDate/endDate/budget` stay unwritten; `createTrip` writes `null` instead of `"USD"`. Rename uses a new `UPDATE` query | Six screens read `store.meta(tripId)` synchronously; city has no Room column; avoids the REPLACE cascade (section 8) | steps 2, 3 |

---

## 7. Risks and sequencing notes

- **Name clashes.** `prototype.BudgetActivity`, `DiscoverActivity`, `GroupActivity`, `JournalActivity` and `SummaryActivity` share simple names with root-package classes. Until step 10:
  - Imports and `Intent` targets must stay fully qualified.
  - Manifest entries use the `.prototype.` prefix; do not de-duplicate them before step 10.
  - A new root `WeatherActivity` (step 7) clashes with `prototype.WeatherActivity` the same way.
- **Name-based tests.** `PrototypeDeviceTest` launches Kevin's classes by name, casts to `PrototypeActivity`, reads `root.getPaddingTop()`, and asserts his exact titles. It will not compile after step 10 until it is rewritten (P-112). `ExampleInstrumentedTest` and `TripRepositoryImplTest` are unaffected.
- **Tests that must keep passing.** There are 75 `@Test` methods across 14 files in `app/src/test`; the plan's "37+" is out of date. These include:
  - Ours: `BudgetMathTest`, `RouteMathTest`, `ScheduleMathTest`, `TravelModeTest`, `CurrencyMockTest`, `CurrencyRatesTest`, `CurrencyRepositoryTest`, `FrankfurterDataSourceTest`, `RemoteDataSourceTest`, `InputParsingTest`.
  - Kevin's: `RoutePreviewTest`, `TripExportTest`, `TripExportAmountsTest`.
  - Q5 and Q6 make at least one of `TravelModeTest`/`RoutePreviewTest` and one of `CurrencyMockTest`/`RoutePreviewTest` contradictory. Settle them before step 4 and step 5, or "no duplicate route logic" and "single rates table" cannot both be met.
- **Live rates in tests.** `CurrencyRates` is a static global swapped by `CurrencyRepository`. Any test touching conversion must reset it with `CurrencyRates.useFallback()`.
- **Room.** v1 with no migrations: any entity edit without a migration crashes existing installs. Prefer the existing columns plus projection changes (section 5).
- **Persistence collision.** Reusing `trippings_prototype_v1` for the new store would make Gson parse Kevin's `DraftTrip` array as the new format. Use a new file name in step 2.
- **Ordering.** Step 3 has to persist the ISO start date and the trip length first: steps 4 (all-day tabs, date headings, day validation), 7 (forecast per day) and 8 (`.ics` UTC times) all depend on them.
- **Data needed for routing.** Step 6 adds catalog picks with coordinates. Step 4's real-distance routing (P-25) only has coordinates for place-backed and map-pinned stops until then. Free-text and current Discover/Suggestions stops fall back to the 20-minute placeholder (P-26), which is expected, not a regression.
- **Weather swap.** Today's add-then-delete swap loses duration and position. Steps 6 and 7 need an in-place update to keep id, day, buffer and mode (P-54, P-87).
- **Merge conflicts.** Steps 4 to 9 all touch `MainActivity`, `ItineraryAdapter`, `StopDetailActivity` and `PrototypeStore`, plus `strings_prototype.xml` and both `values`/`values-night` color files. Do one commit per step and rebase in step order. New strings go in resources and new colors in both themes (`DESIGN.md` rules).
- **Design rules to keep in every port.**
  - Navy `ScreenActivity` shell; do not port `PrototypeActivity`'s light status bar or 18dp cards.
  - 16dp outlined `item_row` cards; monospace only on values.
  - Touch targets of 48dp or more; plurals for counts; two-decimal amounts through `CurrencyRates.format` in the UI (`TripExport` keeps its own `%.2f` with ISO codes for exports, as its tests require).
  - Snackbars on the `CoordinatorLayout` root; dialogs survive rotation; a save button is disabled while it runs.
- **Critique items.** Ports that touch screens with open critique items should note them:
  - `discover.md` #10 (resolved by P-52).
  - `home.md` #4 (resolved by P-15).
  - `add-destination.md` #9 (result hand-off, unchanged).
  - `summary.md` #11 (PDF vs share content, affected by P-92).

---

## 8. Architect review: corrections, gate additions, sequencing

### Corrections to the text above (the corrected fact wins)

| Where | Fact |
|---|---|
| §1, §5 "unused columns" | `TripEntity.homeCurrency` is written on every insert, hard-coded `"USD"`, never read. `startDate`, `endDate`, `budget`, `userId` are always null. `bufferMin` and `transportMode` are always null and no update query writes them (`ItineraryItemDao#updateDetails` omits both) |
| §5 and F5 (**data-loss hazard**) | `TripDao#insert` uses `OnConflictStrategy.REPLACE` and `itinerary_items` has `onDelete = CASCADE`. Saving an edited `TripEntity` through `insert` **deletes all of its stops**. Trip edits must use `UPDATE`/`@Update`, never REPLACE |
| P-16 | `TripEntity` has no creation timestamp and random UUID ids; "newest first" is `ORDER BY rowid DESC` and only holds while trips are never REPLACE-inserted |
| F4 | The Itinerary passes `Math.max(2, maxDay() + 1)` to Stop Detail (`pickDay` defaults to 3). Only `StopDetailActivity` caps the day; `AddDestinationActivity#validateDay` has no upper bound |
| P-67 | Kevin's `SummaryActivity#write` catches `RuntimeException` and shows the generic "Could not save the file…" toast; the "Invalid trip date" text is never shown |
| P-44/P-45 | Mode thresholds use straight-line km; minutes use km x 1.25; for Transit the minimum of 1 applies after adding 8 |
| P-66 | `fold()` wraps before content passes 73 octets (continuation lines at most 74 with the leading space); the test asserts at most 75 |
| F9 rates | `RoutePreview.rate()` treats an unknown code as PHP (rate 1); `CurrencyRates` passes unknown codes through. Q6 picks pass-through |
| §7 tests (Q5) | `RoutePreviewTest` asserts no speeds or thresholds; only `TravelModeTest` must change |
| §7 tests (Q6) | `CurrencyRatesTest` lines 26 and 80 also assert 150 JPY = 56 PHP (three assertions on our side) |
| §7 tests (missed) | `ScheduleMathTest#build_flexibleStops_flowWithBufferFromNineAm` expects B at 10:00 (Kevin's 20-minute placeholder gives 10:20); `RouteMathTest#optimize_lockedStop_keepsItsSlot` contradicts anchoring by time (P-28); `InputParsingTest#safeName_*` assert the `tripfit-` prefix and break when `TripPdfWriter` goes (Q12) |
| P-47 / step 6 | There is a **third catalog**: `data/OfflineData` (10 places with coordinates). It disagrees with `DemoCatalog` ("Nezu Shrine" vs "Nezu Museum"; Fort Santiago coordinates differ). "Only one catalog remains" means folding it in too |
| P-112 | `PrototypeDeviceTest` has 4 tests; two build `DraftTrip`/`DraftStore` directly, so they depend on Q4 as well as screen titles |

### Deletion-gate additions (section 4 plus these rows)

A row also owns every item whose only UI is that screen:
- **PlannerActivity:** P-25, P-28, P-33, P-34, P-35, P-44, P-45, P-118, P-123, P-124, P-125, P-127.
- **StopEditorActivity:** P-31, P-119, P-120, P-121, P-126, P-128.
- **SummaryActivity:** P-66, P-91, P-93, P-94, P-95, P-135.
- **DiscoverActivity:** P-47, P-58, P-129. **BudgetActivity:** P-79, P-129, P-133, P-134, P-135. **WeatherActivity:** P-129, P-137.
- **GroupActivity:** P-138. **JournalActivity:** P-122, P-136.
- **TripSetupActivity:** P-130, P-131, P-132. **PrototypeActivity:** P-130.
- **DraftTrip:** renamed to `TripSnapshot` in step 10 (Q4). The gate passes when nothing references the name `DraftTrip`.
- P-56 and P-59/P-60 disagree on where the budget filter lives; it belongs in Suggestions.

### Sequencing changes to the plan

1. **Step 2:** also persist `TripMeta` with an ISO `startDate` (Q24). Put a `KeyValueStorage` interface (getString/putString) behind the store, with an in-memory fake in tests and a SharedPreferences implementation in the app, initialised by `PrototypeStore.init(Context)` from `TripFitApp.onCreate`; `get()` stays so the public API is kept. JVM tests cannot use SharedPreferences (no Robolectric, no new dependencies); "survives process death" is checked on the emulator. Serialize plain mutable DTOs, not the immutable `BudgetSettings`/`Expense`/`JournalEntry`. Add expense `id` and journal `id`, `stopId`, `photoUri` now so the format does not churn. The test baseline is 75 tests, not "37+".
2. **Step 3:** acceptance is reworded to "no path reachable from Login or the launcher starts `prototype.TripsActivity`, `TripSetupActivity` or `PlannerActivity`" (his own code and device test still start them until step 10). Step 3 must precede step 4. Validate the 30-day limit after the date-range picker.
3. **Step 4** is the largest. Do the engine first (snapshot adapter, `TravelMode`/`RoutePreview` merge, retiring `RouteMath.optimize` slot locks and `ScheduleMath.build`, DAO updates for `bufferMin`, `transportMode`, place), then the UI. Port the intent of the five `RouteMathTest` cases and the `ScheduleMathTest` flow case to `RoutePreview` semantics (B at 10:20) so coverage does not drop. `StopDetailActivity` should observe its item by id instead of taking more intent extras.
4. **Step 6:** fold `OfflineData` into the catalog; upsert the offline cities in `ensureSeedData` before inserting place rows (foreign key); existing installs keep the coordinate-less seed, which is acceptable.
5. **Steps 6/7:** in-place replacement needs `ItineraryItemDao#replaceIdentity`; day swaps need two day/position updates. Query changes only.
6. **Step 8:** the `safeName` move (Q12) is required or `InputParsingTest` stops compiling; generate the export footnote from the rates table (P-135).
7. **Step 9:** our seeded journal entry has `hasPhoto = true` with no URI; seed it without a photo or it will read "no longer accessible".
8. **Step 10:** rename `DraftTrip` (Q4), remove the 10 manifest lines, rewrite `PrototypeDeviceTest#pinnedStopAndExpense…` against `PrototypeStore` (keep the isolated-prefs `ContextWrapper` pattern). `DebugLauncherActivity` resolves only root-package names, so it is unaffected.
