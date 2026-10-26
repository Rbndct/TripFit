# TripFit — Developer Handoff: Free-API Data Pipeline

**Audience:** the coding agent implementing the API/data layer.
**Read first:** `docs/API-Integration-Plan.md` (architecture + phases), `CLAUDE.md` (build/stack), `PRODUCT.md` (scope), `docs/TripFit-Proposal-and-Backlog.md` (backlog #s).
**Goal of this handoff:** give you everything to build Phase 0 → Phase 1 without guessing. Do **not** start coding past Phase 1 until the human confirms the open decisions below.

---

## 1. Project facts (do not re-derive)

- **App:** native Android, **Java** (not Kotlin, despite `.kts` Gradle scripts). Classic Views — AppCompat, Material, ConstraintLayout, RecyclerView. **No Compose.**
- **Package:** `com.itismob.s01.group01.tripfit`
- **SDK:** `minSdk 24`, `compile/targetSdk 37`, AGP `9.4.1`, Java 11.
- **Build/test:**
  - `./gradlew compileDebugJavaWithJavac` — compile check (use this, not Kotlin tooling)
  - `./gradlew test` — unit tests
  - `./gradlew assembleDebug` — build APK
  - `./gradlew lint` — Android lint
- **Current state:** UI scaffolding only. No networking, persistence, DI, or ViewModel yet. `AddDestinationActivity` shows a `saved_ui_only` Snackbar instead of persisting; `HomeActivity`'s Tokyo card is hardcoded → `MainActivity`; `model/ItineraryItem.java` is a plain immutable POJO (no Room annotations).
- **Auth:** `LoginActivity.DEV_BYPASS_LOGIN = BuildConfig.DEBUG` — a debug-only login bypass. Keep it until Phase 5. `buildConfig = true` is enabled in `app/build.gradle.kts`.

## 2. Non-negotiable constraints

1. **Free APIs only.** Allowed: Nominatim, Overpass, OpenTripMap, Wikidata/Wikipedia, Wikivoyage, OpenRouteService (free tier), Open-Meteo, Frankfurter, Google Calendar (free, OAuth). **Forbidden:** anything requiring a billing account — specifically Google Directions/Maps Platform, Foursquare paid tier, OpenWeather paid tiers.
2. **No API keys on-device.** The one keyed service (OpenRouteService) is called only through a **server-side proxy**. OpenTripMap's key is also server-side (pipeline only).
3. **Respect usage policies.** Nominatim: set a descriptive `User-Agent`, ≤1 req/s, never bulk-call from devices. Overpass: batch, cache. All heavy source calls happen in the **pipeline**, not per-device.
4. **Follow repo conventions** (below). Match existing style; don't introduce a second pattern.

## 3. Conventions to mirror (from existing code)

| Concern | Pattern | Source |
| --- | --- | --- |
| Models | Immutable: `final` fields, constructor, getters, no setters | `model/ItineraryItem.java` |
| Input validation | Validate in the Activity, set `TextInputLayout.setError(...)`, early-return on invalid | `AddDestinationActivity.attemptSave()` |
| User feedback | `Snackbar.make(root, R.string.x, LENGTH_SHORT)` | `HomeActivity.showComingSoon()` |
| Strings | All user-facing text in `res/values/strings.xml`, referenced by `R.string.*` | existing activities |
| Insets | `ViewCompat.setOnApplyWindowInsetsListener` on the root | `HomeActivity.onCreate()` |
| Secrets | `local.properties` (git-ignored) → `buildConfigField` → `BuildConfig.*`. Never hardcode. | `ecc/java/security.md` |
| Errors | Domain exceptions extend `RuntimeException`; no broad `catch (Exception)` except top-level; never log secrets/PII | `ecc/java/security.md` |
| Architecture | Repository interface + constructor injection; `ViewModel` + `LiveData`; background work on an `ExecutorService`; return a `Result<T>`-style envelope | `ecc/java/patterns.md` |

## 4. Architecture to build (Android side)

```
Activity ──observes──► ViewModel ──calls──► Repository (interface)
                                              │
                        ┌─────────────────────┴─────────────────────┐
                   RemoteDataSource (Retrofit → our REST)      LocalDataSource (Room DAO)
```

- **Retrofit** + Gson converter + OkHttp logging interceptor (debug only).
- **Room** entities/DAOs for `trips`, `itinerary_items`, `expenses`, plus cached `cities`/`places`.
- **ViewModel + LiveData**; background on a shared `ExecutorService`; deliver results on main thread.
- **Result envelope:** `Result.success(data)` / `Result.error(message)` — no leaking stack traces to UI.

## 5. Backend / pipeline to build — Supabase (LOCKED)

- **DB:** Supabase **Postgres** with the schema in `API-Integration-Plan.md` §3. Enable row-level security on the per-user tables (`trips`, `itinerary_items`, `expenses`).
- **Reads:** the app calls Supabase's **auto-generated REST** endpoints for `cities` / `places` / per-user tables (no custom read endpoints to write).
- **Ingestion pipeline:** run the ETL as a **GitHub Actions scheduled workflow** (free) — a standalone Python/Node script that does fetch → normalize → cross-reference → enrich → clean → upsert into Supabase (see Plan §4). Keep provenance in `sources_json`. This keeps heavy source calls off devices and off Supabase's function quota.
- **ORS proxy:** a Supabase **Edge Function** that accepts matrix/directions requests from the app, adds the ORS key from a backend secret, forwards, and returns the response. The key never ships in the APK.
- **Auth:** Supabase Auth (email + Google provider) — wired in Phase 5, reused for Calendar OAuth.

## 6. Phase 0 tasks (do these first)

1. **Verify free terms** — open each service's official pricing/usage page and confirm it's still free at our volume. Record date + limit in a short `docs/api-terms.md` table. If any changed, STOP and flag.
2. **Dependencies** — add to `gradle/libs.versions.toml` + `app/build.gradle.kts`: Retrofit, Gson converter, OkHttp logging-interceptor, Room (runtime + compiler via `annotationProcessor`), `lifecycle-viewmodel` + `lifecycle-livedata`.
3. **Secrets plumbing** — add key reads from `local.properties` → `buildConfigField`; confirm `local.properties` is git-ignored (it is).
4. **Room** — create the DB, entities, and DAOs for the full schema; migrate `ItineraryItem` POJO → a Room entity (keep the immutable-getter style).
5. **Repository + ViewModel + Executor** scaffolding with the `Result<T>` envelope.
6. **Make persistence real** — `AddDestinationActivity` inserts a row (replace `saved_ui_only`); `HomeActivity` loads trips from Room (replace the hardcoded Tokyo card, or seed it as DB data).
7. **Confirm the login security fix compiles** — `./gradlew compileDebugJavaWithJavac` (validates the `BuildConfig.DEBUG` gating already added).
8. **Backend skeleton** — create the Supabase project + apply the schema (Plan §3) with RLS on per-user tables + stub the ORS proxy Edge Function (no ingestion logic yet). Set up the GitHub Actions workflow file as a stub (no ETL logic yet).

**Phase 0 done when:** app compiles, a destination added on the emulator survives an app restart (real Room persistence), and the backend has the schema + proxy stub deployed.

## 7. Phase 1 tasks (ingestion pipeline v1)

1. Implement fetch adapters for Nominatim, Overpass, OpenTripMap, Wikidata/Wikipedia, Wikivoyage.
2. Normalize each to the canonical `places` schema.
3. Cross-reference/dedupe (wikidata_id → else fuzzy name + ≤50 m proximity).
4. Enrich with description/image; compute `is_hidden_gem` from Wikidata sitelink/pageview counts.
5. Clean/validate (drop no-coords/no-name; clamp; dedupe addresses); upsert; stamp `cleaned_at`.
6. Run for the seed cities — **Tokyo (JP)** and **Manila (PH)** first (SEA focus); spot-check a sample for correctness. Secondary SEA targets to extend into: Osaka/Kyoto, Cebu, Bangkok, Singapore, Hanoi, Kuala Lumpur.

**Phase 1 done when:** running the pipeline for a seed city populates `cities` + a cleaned, cross-referenced `places` set the app can read, with provenance in `sources_json`.

## 8. Testing expectations

- Unit-test repositories with a **mocked Retrofit response** (don't hit live APIs in tests).
- Unit-test the pipeline's cross-reference/dedupe and cleaning logic with fixed fixtures.
- Target 80%+ on new business logic (per `ecc/common/testing.md`). Skip trivial getters/config.
- Manual: run on emulator per `CLAUDE.md`; capture a before/after of persistence working.

## 9. Decisions

1. **Backend:** ✅ LOCKED — **Supabase** (Postgres + REST + Auth + Edge Function ORS proxy); ETL on GitHub Actions.
2. **Seed cities:** ✅ LOCKED — **SEA focus. Tokyo (JP) + Manila (PH) first**, then Osaka/Kyoto, Cebu, Bangkok, Singapore, Hanoi, Kuala Lumpur.
3. **Transit:** open, defaulting to **walk/cycle/drive only** (Google transit dropped as non-free). Proceed on this default unless the human says transit is required.

## 10. Guardrails

- Don't add paid/keyed services. Don't ship any key in the APK.
- Don't break the debug login bypass gating (`BuildConfig.DEBUG`) or re-enable it for release.
- Don't introduce Kotlin or Compose.
- Commit per `ecc/common/git-workflow.md` (`feat:`/`chore:`…); push only when the human asks. Repo: `https://github.com/Rbndct/TripFit` (private, `main`).
- If a free API's terms turn out to require billing, STOP and surface it — do not work around it with a paid tier.
