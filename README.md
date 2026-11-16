# TripFit

Smart itinerary planner for Android: route optimization, day-by-day planning, calendar sync and budget tracking in one app.

TripFit is the ITISMOB (Mobile Development) term project of De La Salle University, College of Computer Studies.

**Team:** Cris Jemuel T. Dela Cruz · Raphael Benedict C. Maagma · Kevin Mathew V. Mariposa · Julia Kirsten G. Palomo

---

## Why TripFit

Travelers plan trips across a map app, a spreadsheet, a currency converter, blog posts and their calendar. TripFit folds those into one pipeline:

```
add stops → optimize the route → split into days → sync to calendar → track the budget
```

## Status

The app is a **clickable UI prototype**. Every backlog feature has a screen and every button leads somewhere. The backend is only partly connected:

| Layer | State |
|---|---|
| Trips, stops, day/position, booked times, durations | **Saved** on-device in Room (SQLite) |
| Place search | Supabase REST, with a bundled offline fallback (Tokyo, Manila) |
| Expenses, budget setup, transport modes, journal, votes, calendar-sync status | **Session-only** (in-memory `PrototypeStore`, reset when the app closes) |
| Prices, distances, weather, exchange rates, Discover suggestions | **Demo data** (`prototype/MockData`) until the APIs are wired |
| Google Sign-In, Calendar push | UI only; debug builds skip the login check |

## Features

| Backlog item | Screen |
|---|---|
| Google Sign-In | Login |
| Add destination (search or map pin) | Add Destination, Map Picker |
| Route optimization | Optimize (booked stops stay locked) |
| Duration per stop, booked lock-in, smart buffer | Stop Detail |
| Day-by-day itinerary | Itinerary (day tabs) |
| Per-leg transport mode | Itinerary (transport chip on each stop) |
| Discover suggested itinerary | Discover (famous spots and hidden gems) |
| Calendar sync | Calendar Sync |
| Budget setup, running tracker, currency conversion | Budget, Budget Setup |
| Budget-aware food and activity suggestions | Suggestions |
| Weather-aware rescheduling | Itinerary (rain banner, swap to indoor stops) |
| Trip summary export | Trip Summary (real PDF and text share) |
| Group trip mode | Group Trip (invite code, votes, cost split) |
| Photo / journal log | Journal |

## Tech stack

- **Android, Java 11**, classic Views (AppCompat, Material 3, ConstraintLayout, RecyclerView); no Jetpack Compose
- `minSdk 24`, `targetSdk 37`, Gradle Kotlin DSL scripts
- **Room** for local persistence, **ViewModel + LiveData**
- **Retrofit + OkHttp** for Supabase REST
- **osmdroid** (OpenStreetMap) for the map picker
- **Supabase** (Postgres, Edge Functions) as the backend
- **Python** ingestion pipeline for place data

## Getting started

### Prerequisites

- Android Studio (recent stable) with the Android SDK and an emulator or device on API 24+
- JDK 17+ for Gradle

### Configure

Create `local.properties` in the repo root. Android Studio adds `sdk.dir` for you; add the Supabase values:

```properties
SUPABASE_URL=https://<your-project>.supabase.co
SUPABASE_ANON_KEY=<anon key>
```

`local.properties` is git-ignored. Leave the values blank and the app still runs: place search falls back to the bundled offline data.

### Build and run

```bash
./gradlew assembleDebug                 # build the debug APK
./gradlew installDebug                  # install on a running emulator/device
./gradlew testDebugUnitTest             # unit tests
./gradlew lintDebug                     # Android lint
```

Or open the project in Android Studio and press **Run**. In debug builds, either sign-in button goes straight to Home; a sample "Tokyo Trip" is created on first launch.

## Project structure

```
app/src/main/java/com/itismob/s01/group01/tripfit/
├── *Activity.java      one Activity per screen; ScreenActivity is the shared base (navy toolbar, insets)
├── data/               repository, Room database/DAOs/entities, Supabase API, offline search data
├── viewmodel/          ViewModels for Home, Itinerary, New Trip, Add Destination
├── ui/                 adapters, list rows, dialogs, custom views (ticket perforation, barcode)
└── prototype/          logic and demo data for features without a backend yet
                        (budget math, route ordering, scheduling, currency, PrototypeStore)
app/src/debug/          debug-only screen launcher used for screenshot capture
app/src/test/           JUnit unit tests
pipeline/               Python place-data ingestion
supabase/               database migrations and the ors-proxy Edge Function
docs/                   proposal, backlog, API integration plan, developer handoff
```

## Data pipeline

`pipeline/` gathers places for each city from free sources (Nominatim, Overpass, OpenTripMap, Wikidata, Wikivoyage), then normalizes, de-duplicates and cleans them before upserting into Supabase:

```bash
pip install -r pipeline/requirements.txt
SUPABASE_URL=... SUPABASE_SERVICE_KEY=... OPENTRIPMAP_API_KEY=... \
  python -m pipeline.main --cities "Tokyo,JP" "Manila,PH"
```

The GitHub Action `.github/workflows/ingest.yml` runs it weekly using repository secrets. The service key and API keys live only there and never ship in the app. Routing goes through the `ors-proxy` Edge Function, so the OpenRouteService key stays server-side as well.

## Design

The visual system is a **boarding pass / ticket stub**: a fixed navy header band that stays the same in dark mode, monospace used only for data (times, amounts, counts), and flat outlined cards. See [`DESIGN.md`](DESIGN.md) for the full rules and [`PRODUCT.md`](PRODUCT.md) for the product brief.

## Documentation

- [`docs/TripFit-Proposal-and-Backlog.md`](docs/TripFit-Proposal-and-Backlog.md): proposal, scope, 19-item backlog, sprint plan
- [`docs/API-Integration-Plan.md`](docs/API-Integration-Plan.md): backend architecture and phases
- [`docs/DEV-HANDOFF.md`](docs/DEV-HANDOFF.md): conventions and constraints for the data layer
- [`docs/api-terms.md`](docs/api-terms.md): usage terms of the free APIs
