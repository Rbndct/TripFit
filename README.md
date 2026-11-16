<div align="center">

# TripFit

**Smart itinerary planner for Android: route optimization, day-by-day planning, calendar sync and budget tracking in one app.**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/language-Java%2011-ED8B00?logo=openjdk&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-24-blue)
![Target SDK](https://img.shields.io/badge/targetSdk-37-blue)
![Backend](https://img.shields.io/badge/backend-Supabase-3FCF8E?logo=supabase&logoColor=white)
![Status](https://img.shields.io/badge/status-UI%20prototype-orange)

ITISMOB (Mobile Development) term project · De La Salle University, College of Computer Studies

</div>

> Click any section below to expand it.

## Table of Contents

- [About the Project](#about)
- [Project Status](#status)
- [Features](#features)
- [Built With](#built-with)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Data Pipeline](#data-pipeline)
- [Testing](#testing)
- [Documentation](#documentation)
- [Team](#team)

---

<a name="about"></a>
<details open>
<summary><strong>About the Project</strong></summary>

<br>

Travelers plan trips across a map app, a spreadsheet, a currency converter, blog posts and their calendar. TripFit folds those into one pipeline:

```
add stops → optimize the route → split into days → sync to calendar → track the budget
```

Existing apps solve only pieces of this: Google Trips is discontinued, TripIt only organizes bookings already made, and Wanderlog and Google My Maps list places without sequencing them or accounting for budget.

</details>

<a name="status"></a>
<details>
<summary><strong>Project Status</strong></summary>

<br>

The app is a **clickable UI prototype**. Every backlog feature has a screen and every button leads somewhere. The backend is only partly connected:

| Layer | State |
|---|---|
| Trips, stops, day/position, booked times, durations | **Saved** on-device in Room (SQLite) |
| Place search | Supabase REST, with a bundled offline fallback (Tokyo, Manila) |
| Expenses, budget setup, transport modes, journal, votes, calendar-sync status | **Session-only** (in-memory `PrototypeStore`, reset when the app closes) |
| Prices, distances, weather, exchange rates, Discover suggestions | **Demo data** (`prototype/MockData`) until the APIs are wired |
| Google Sign-In, Calendar push | UI only; debug builds skip the login check |

</details>

<a name="features"></a>
<details>
<summary><strong>Features</strong></summary>

<br>

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

</details>

<a name="built-with"></a>
<details>
<summary><strong>Built With</strong></summary>

<br>

| Area | Technology |
|---|---|
| Language | Java 11 (Gradle Kotlin DSL build scripts) |
| UI | Android Views: AppCompat, Material 3, ConstraintLayout, RecyclerView (no Jetpack Compose) |
| Architecture | ViewModel + LiveData, repository pattern |
| Local storage | Room (SQLite) |
| Networking | Retrofit + OkHttp |
| Maps | osmdroid (OpenStreetMap) |
| Backend | Supabase (Postgres, REST, Edge Functions) |
| Data ingestion | Python 3.11 |
| Testing | JUnit 4, Espresso |

</details>

<a name="getting-started"></a>
<details>
<summary><strong>Getting Started</strong></summary>

<br>

### Prerequisites

- Android Studio (recent stable) with the Android SDK
- An emulator or device running Android 7.0 (API 24) or later
- JDK 17+ for Gradle

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Rbndct/TripFit.git
   cd TripFit
   ```
2. Create `local.properties` in the repo root. Android Studio adds `sdk.dir` for you; add the Supabase values:
   ```properties
   SUPABASE_URL=https://<your-project>.supabase.co
   SUPABASE_ANON_KEY=<anon key>
   ```
   `local.properties` is git-ignored. With the values left blank, the app still runs and place search falls back to the bundled offline data.
3. Build and install:
   ```bash
   ./gradlew installDebug
   ```
   Or open the project in Android Studio and press **Run**.

</details>

<a name="usage"></a>
<details>
<summary><strong>Usage</strong></summary>

<br>

1. **Sign in.** In debug builds either sign-in button goes straight to Home; a sample "Tokyo Trip" is created on first launch.
2. **Create a trip** with **New Trip**: pick a city, dates and home currency, then add stops yourself or let **Discover** suggest them.
3. **Build the itinerary.** Switch days with the tabs, tap a stop to edit it, and tap the transport chip to change how you get to the next stop.
4. **Use the toolbar** to optimize the route or open the budget; the overflow menu holds Suggestions, Discover, Calendar Sync, Trip Summary, Group Trip and Journal.

</details>

<a name="project-structure"></a>
<details>
<summary><strong>Project Structure</strong></summary>

<br>

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

</details>

<a name="data-pipeline"></a>
<details>
<summary><strong>Data Pipeline</strong></summary>

<br>

`pipeline/` gathers places for each city from free sources (Nominatim, Overpass, OpenTripMap, Wikidata, Wikivoyage), then normalizes, de-duplicates and cleans them before upserting into Supabase:

```bash
pip install -r pipeline/requirements.txt
SUPABASE_URL=... SUPABASE_SERVICE_KEY=... OPENTRIPMAP_API_KEY=... \
  python -m pipeline.main --cities "Tokyo,JP" "Manila,PH"
```

The GitHub Action `.github/workflows/ingest.yml` runs it weekly using repository secrets. The service key and API keys live only there and never ship in the app. Routing goes through the `ors-proxy` Edge Function, so the OpenRouteService key stays server-side as well.

</details>

<a name="testing"></a>
<details>
<summary><strong>Testing</strong></summary>

<br>

```bash
./gradlew testDebugUnitTest          # unit tests (budget math, route ordering, scheduling, currency, input parsing)
./gradlew lintDebug                  # Android lint
./gradlew connectedAndroidTest       # instrumented tests (needs an emulator or device)
```

Pipeline tests:

```bash
python -m unittest discover -s pipeline/tests -t .
```

</details>

<a name="documentation"></a>
<details>
<summary><strong>Documentation</strong></summary>

<br>

| Document | Contents |
|---|---|
| [`docs/TripFit-Proposal-and-Backlog.md`](docs/TripFit-Proposal-and-Backlog.md) | Proposal, scope, 19-item backlog, sprint plan |
| [`docs/API-Integration-Plan.md`](docs/API-Integration-Plan.md) | Backend architecture and phases |
| [`docs/DEV-HANDOFF.md`](docs/DEV-HANDOFF.md) | Conventions and constraints for the data layer |
| [`docs/api-terms.md`](docs/api-terms.md) | Usage terms of the free APIs |
| [`PRODUCT.md`](PRODUCT.md) | Product brief |

</details>

<a name="team"></a>
<details>
<summary><strong>Team</strong></summary>

<br>

| Name |
|---|
| Cris Jemuel T. Dela Cruz |
| Raphael Benedict C. Maagma |
| Kevin Mathew V. Mariposa |
| Julia Kirsten G. Palomo |

Built for ITISMOB: Mobile Development, Department of Information Technology, College of Computer Studies, De La Salle University.

</details>
