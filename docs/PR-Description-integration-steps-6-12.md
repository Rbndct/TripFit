# Combine the two Trippings frontends into one flow

**Branch:** `integration/steps-6-12` into `main`
**Size:** 72 commits, 405 files, +44,692 / -649. Merges cleanly into `origin/main` (0 commits behind).
**Contributors:** Rbee (53 commits), Kevin Mariposa (13), KirstenPalomo (5), CJ (1).

## Summary

The prototype branch held two complete frontends that did not share data: our reviewed screens (Home, Itinerary, Budget, and so on, backed by Room) and Kevin's `prototype/` screens (SharedPreferences, stronger planning logic). Login opened Kevin's flow, so ours were unreachable.

This PR makes our screens the single user flow and ports Kevin's logic, persistence and features into them, then removes the duplicate screens. The app is named **Trippings** in all user-facing text. The package, `Theme.TripFit` and `tripfit.db` keep their names.

What a user gets: Login opens Home, a sample Tokyo trip with a persistent itinerary (per-leg buffers, booking conflicts, day balancing, route preview), planned and logged budgets kept separate with live currency rates, one Tokyo and Manila catalog for Discover and Suggestions, a weather screen with indoor swaps, `.ics` and PDF export through the file picker, and local group and photo-journal features that survive restarts.

Design and decision records: `docs/Combine-Prototypes-Plan.md`, `docs/Prototype-Merge-Map.md` (138-item checklist, architect decisions Q1 to Q24, and the findings in section 9).

## Commits grouped by step

| Step | What | Commits |
|---|---|---|
| Base | Ticket-stub Home and day-tab Itinerary redesign, clickable prototype for every backlog feature, per-screen critique passes, README | `9a6b9cc` to `f9f0b7b` |
| Kevin's frontend | Local planning prototype, discovery, budget, weather, group, journal and export features; Supabase-free startup; map pin fixes; merged in | `84f57fa` to `b6d8a50` |
| Rates and amounts | Live Frankfurter rates and currency list, two-decimal amounts | `55e45a2`, `d9b88a5` |
| 1 Merge map | Per-feature merge map and checklist | `7c29337` |
| 2 Persistent store | `PrototypeStore` backed by SharedPreferences (`trippings_store_v2`) | `2f480f7` |
| 3 Single entry flow | Login opens Home; New Trip creates and edits | `aa277b6` |
| 4 Planning logic | One engine: per-leg buffers, conflicts, booking locks, travel modes | `4a41e48` |
| 5 Budget and currency | One budget model, planned vs logged, one rates table | `fb2da5c` |
| 6 Catalog | One Tokyo and Manila catalog | `99a8050` |
| 7 Weather | Saved forecasts, outdoor warnings, indoor alternatives | `a704ad2` |
| 8 Calendar and export | `.ics` and PDF through the file picker, summary sharing | `5d50188` |
| 9 Group and journal | Local companions, votes, editable photo journal | `67e9fde`, `96defcd` |
| 10 to 12 | Remove duplicate screens, naming, final verification | `7fcce7a`, `317242d`, `1979475` |
| Audit fixes | Optimize guard and whole-trip budget allowance, pin name, budget, stop card, keyboard, tagline, trip-settings, snackbar, large-font and label gaps | `27e2013`, `f90cea5`, `8ee4d6f`, `468a6ee`, and the Discover font and label fix |
| Tests and docs | Device test fixes, refreshed captures, merge-map and plan updates | `1a97209`, `3dadafb`, `aab1835`, `b7b21ff`, `1017abf`, `4be9a48`, `4f5611d` |
| Planning docs | Project plans, contributor docs and tooling | `6893b7b` to `d3520ac` |

## Test plan

Run on `integration/steps-6-12` (JDK 25 from Android Studio):

- `./gradlew compileDebugJavaWithJavac testDebugUnitTest lintDebug compileDebugAndroidTestJava assembleDebug`: **BUILD SUCCESSFUL**, **272 unit tests, 0 failures, 0 lint errors**.
- `./gradlew connectedDebugAndroidTest` on a freshly wiped Pixel 3a (API 34): **38 of 38 instrumented tests pass**.
- Emulator walkthrough of every screen (light, dark and 1.3x font captures in `.impeccable/review/`): no ANRs, no layout problems found. The "Budget saved" snackbar, keyboard hiding after submit (Budget calculator, Group companion) and the weather forecast change were exercised by hand.
- Checklist: 138 of 138 items in `docs/Prototype-Merge-Map.md` verified against the code.

## Known limitations

- **Sample data only.** Places, prices, ratings, weather and routing are illustrative. Exchange rates are live from Frankfurter when reachable and fall back to a sample table. Travel times are straight-line estimates.
- **Local storage only.** Trips and stops are in Room v1 (no migration was needed); expenses, budget, journal, votes and per-stop cost are in `PrototypeStore`. Nothing syncs to a server and no invitations are sent.
- **Not merged from `origin/feat/ui-prototype`:** Kevin's later commits `123c8cd` and `8952904` (travel data adapters, `ExchangeRates`, extra tests) edit screens this PR deletes, and `ExchangeRates` would add a second rates table. Two of their rules were ported as tests plus fixes (optimized previews never get worse; suggestions respect the whole-trip budget). The rest needs a follow-up PR from Kevin on top of this one, using the single `CurrencyRates` table.
- Calendar sync is a preview: nothing is sent to Google Calendar; the `.ics` file is saved for manual import.

## Reviewer checklist

- [ ] Login opens Home; no code path starts a deleted `prototype/*Activity`.
- [ ] `./gradlew testDebugUnitTest lintDebug assembleDebug` pass locally.
- [ ] Open the sample Tokyo trip: buffers, a booked stop, route preview and balance days behave as described.
- [ ] Budget shows planned and logged separately, and the rates notice names its source.
- [ ] `.ics` and PDF save through the file picker.
- [ ] Journal photos and companions still appear after force-stopping and reopening the app.
- [ ] User-facing text says Trippings (About dialog, share subject, file names).
- [ ] Decisions Q1 to Q24 in `docs/Prototype-Merge-Map.md` are acceptable, in particular Q4 (`DraftTrip` became `TripSnapshot`) and Q5 (Kevin's travel constants win).
