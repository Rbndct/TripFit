# Trippings frontend prototype

This prototype implements the flows in the ITISMOB proposal's Scope of Work. It is a native Android frontend with local drafts, illustrative recommendations and explicit previews for integrations. It does not require Supabase or a Google account.

## Start here

Run the `app` debug configuration on an emulator or Android device. On the welcome screen, leave the fields blank and tap **Continue**. Open the sample Tokyo trip, or create a trip for Tokyo or Manila.

The primary entry flow is `LoginActivity` → `HomeActivity` → `MainActivity`. Prototype state is saved in `trippings_prototype_v1` SharedPreferences (`PrototypeStore`) and Room database. All screens run off the unified root Activity architecture.

## Proposal coverage

| Proposal feature | Where to test | Prototype behavior |
| --- | --- | --- |
| Add destination | Itinerary → Add destination | Search the local sample catalog, type a name, or select a map pin. |
| Discover suggested itinerary | Discover → Build a suggested itinerary | Select a mix of popular attractions and hidden gems, then add selected stops across days. Suggestions can be mixed with manual stops. |
| Route optimization | Itinerary → Preview a shorter route | Review a nearest-neighbor route estimate and current/preview travel totals before applying. This is an illustrative heuristic, not a claim of an optimal street route. |
| Duration per stop | Edit destination | Set visit duration, or use a 60-minute default. Catalog places provide suggested durations. |
| Day-by-day itinerary | Day chips / Trip settings | Set dates and trip length, view each day's schedule, and balance flexible stops across days. |
| Booked activity lock-in | Edit destination → Booked activity | Pick a fixed time; route preview, balancing and weather suggestions preserve bookings. Conflicts are flagged. |
| Smart buffer time | Edit destination | Set a buffer before each incoming leg; the scheduled arrival includes travel and buffer time. |
| Per-leg transport | Edit destination → Transport | Choose walking, cycling, driving, transit, or a distance-based suggestion independently per incoming leg. |
| Calendar sync | Calendar & export → Preview Google Calendar sync | Review event times/locations and a clearly simulated completion state. Export a real `.ics` file for manual import; direct Google sync is not connected. |
| Budget setup | Trip creation / Budget | Overall or daily budget plus Food, Activities, Transport and Other limits. |
| Budget-aware food | Discover → Food + budget filter | Illustrative restaurants show ratings, sample review counts, distance, price tier, meal estimate and remaining category budget. |
| Budget-aware activities | Discover → Activities + budget filter | Attraction recommendations are filtered against the remaining category budget. |
| Currency conversion | Budget / Stop editor | Store original price currency per stop and expense; convert to home currency with labeled sample rates. Includes a calculator. |
| Running budget tracker | Budget | Planned costs, logged expenses, remaining balance, per-category progress and overspending warnings. Estimates and actual spending are not added together. |
| Weather-aware rescheduling | Weather | Change a sample daily forecast, identify outdoor conflicts and swap flexible activities for indoor alternatives. Fixed bookings remain protected. |
| Trip summary export | Calendar & export | Save a multipage itinerary/budget PDF, save a calendar file, or open the Android share sheet for a text summary. |
| Group trip mode | Group | Add local companions, try the sample join code, vote/unvote on destinations and preview equal cost splitting. No invitations, remote voting or payments. |
| Photo / journal log | Journal | Add and edit notes/photos for a day or stop; Android's document picker supplies persistent access to the selected image only. |

The proposal's setup/backlog item is represented by the local prototype store and shared native UI components. The source proposal groups the work differently between sections; this table covers all user-facing Scope of Work items rather than treating API choices as completed integrations.

## Map selection checks

1. Open a trip, tap **Add destination or map pin**, then **Pick or adjust location on map**.
2. Tap anywhere on the map: the marker and coordinate label should move together.
3. Hold/drag the marker to adjust it; zooming and panning remain available.
4. Tap **Use this location**. The destination editor shows the selected coordinates immediately, without reverse geocoding or server credentials.
5. Name the stop and save it. Reopen it: the coordinates are preserved and the picker reopens at that pin.
6. Map tiles need internet; selecting and saving coordinates does not. OpenStreetMap attribution is displayed.

## Data and integration boundaries

- Sample catalog: Tokyo and Manila. Restaurant names, prices, ratings and review counts are illustrative.
- The map can select a coordinate outside those cities, but discovery samples remain limited to the chosen city.
- Travel preview uses straight-line distances with illustrative speeds and a road-distance factor. It does not use OpenRouteService or Google transit routing.
- Currency rates are fixed examples: 1 JPY = 0.38 PHP; 1 USD = 58 PHP; 1 EUR = 63 PHP. These are not live quotes.
- Weather conditions and temperatures are samples. They must not be relied on for an actual trip.
- Google Calendar OAuth, multi-device groups and real account authentication remain separate integration work. The existing debug-only login shortcut is preserved.
- Calendar exports use the destination's local time zone (Tokyo or Manila), encoded as UTC events, with escaped and folded calendar text.
- A PDF or calendar export is saved only after the user selects a destination through Android's file picker. The share button opens a chooser; it does not send anything automatically.

## Verification

Run `testDebugUnitTest` and `assembleDebug` through Gradle/Android Studio. Tests cover missing remote configuration, fixed booking preservation, overlapping booking warnings, per-leg travel/buffer estimates, currency handling, calendar time zones and Unicode escaping, and separation of planned and actual spending.

`PrototypeDeviceTest` additionally checks map-tap results, persisted pin/expense data, a renderable multipage PDF, all ten frontend toolkit screens, and status-bar spacing after returning to the itinerary. Use a dedicated emulator for connected tests because the Android test runner can reinstall the app. The PDF has also been rendered and visually inspected for page layout.

Suggested walkthrough: create a trip → add a map pin → save/reopen the stop → discover/swap a place → optimize/balance → set category limits → log an expense → test a rainy day → preview calendar events → export → add a companion/vote → save a journal entry.
