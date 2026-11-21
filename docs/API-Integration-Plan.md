# TripFit — Free-API Integration & Data-Pipeline Plan

_As of 2026-10-26 · supersedes the live-API approach in PRODUCT.md §Capabilities for the data layer._

## 0. What changed from the original proposal

The proposal assumed a **client-only app that calls third-party APIs live, per request**, using an 8-API stack that included two paid/keyed services. This plan changes two things:

1. **Free APIs only.** Google Directions (transit) requires billing and is **dropped**. Foursquare Places is replaced by fully-free OpenStreetMap sources. OpenWeather is replaced by keyless Open-Meteo.
2. **Our own accumulated DB + ingestion pipeline.** Instead of every device calling every API, a server-side pipeline ingests from the free sources, **cross-references and cleans** them into a canonical store, and the app reads from *our* DB. This is what keeps free-tier rate limits viable and gives fast, offline-friendly reads.

## 1. Vetted free API stack

| Capability | Service | Free | Key | Notes / limits |
| --- | --- | --- | --- | --- |
| Geocode + place search | OpenStreetMap **Nominatim** | Yes | No | Must send `User-Agent`; ~1 req/s; heavy use → self-host or cache (we cache in our DB) |
| POIs (amenities, cuisine, hours) | **Overpass API** (OSM) | Yes | No | Shared public instances, rate-limited; batch queries, cache results |
| POI ratings / kinds / wikidata link | **OpenTripMap** | Yes | Free key | ~Free tier; key held server-side only |
| Structured facts, images, popularity | **Wikidata** / **Wikipedia** (MediaWiki) | Yes | No | Used to decide "famous" vs "hidden gem" via sitelink/pageview counts |
| Travel-guide descriptions, hidden gems | **Wikivoyage** (MediaWiki) | Yes | No | Community travel content |
| Travel-time matrix + per-leg directions | **OpenRouteService** | Yes (free tier) | 1 free key | ~2000 req/day; **proxied server-side** so no key ships on device. Modes: walk / cycle / drive |
| Per-day weather | **Open-Meteo** | Yes | No | Replaces OpenWeather; keyless, generous non-commercial tier |
| Currency conversion | **Frankfurter** | Yes | No | ECB rates, keyless |
| Calendar sync | **Google Calendar API** | Yes (no billing) | OAuth consent | Only remaining Google piece; Calendar API itself is free |

**Dropped:** Google Directions transit routing (requires a billing account). Transit legs are out of scope; per-leg modes are **walk / cycle / drive** via OpenRouteService. If transit becomes a hard requirement later, evaluate **Navitia.io** (free key, open-data coverage varies) — do not re-add Google.

> **Action for Phase 0:** re-confirm each service's current free terms against its official pricing/usage page before writing ingestion code (limits change). Checklist lives in `docs/api-terms.md`.

## 2. Target architecture

```
          FREE SOURCES                    OUR BACKEND (free tier)                 ANDROID APP (Java)
┌────────────────────────────┐   ┌──────────────────────────────────┐   ┌──────────────────────────┐
│ Nominatim  Overpass         │   │  Ingestion / ETL pipeline        │   │  Retrofit ──► our REST   │
│ OpenTripMap  Wikidata       │──►│   fetch → normalize → cross-ref  │──►│  Room (offline cache)    │
│ Wikivoyage  Open-Meteo      │   │   → enrich → clean → upsert      │   │  ViewModel + LiveData    │
│ Frankfurter                 │   │  Accumulated DB (canonical)      │   │  Repository + Executor   │
│ OpenRouteService (proxied)  │◄──│  ORS proxy (holds the 1 key)     │◄──│  (no API keys on device) │
└────────────────────────────┘   └──────────────────────────────────┘   └──────────────────────────┘
```

**Backend (LOCKED): Supabase free tier** — Postgres (the accumulated DB) + auto-generated REST + Auth + Edge Functions (the ORS proxy). It's free (no credit card), requires no server ops, and gives the shared "users fetch from our DB" model directly. Relational Postgres suits the cross-referencing/dedupe/joins this data layer needs.

- **Why a backend at all:** "users can fetch it all up" implies a *shared* accumulation, not a per-device cache. A free Postgres + REST layer delivers that and keeps the ORS key off-device.
- **Ingestion pipeline host:** run the ETL as a **GitHub Actions scheduled workflow** (free) that writes cleaned rows into Supabase. This keeps the heavy, rate-limited source calls off both devices and Supabase's function quota. (Edge Functions + `pg_cron` are a viable alternative but Actions is simpler to iterate on.)
- **ORS proxy:** a small Supabase Edge Function that holds the ORS key as a backend secret.
- **Client cache:** the app uses **Room** as an offline mirror of what it reads from Supabase REST.

> **Free-tier caveat:** Supabase pauses a project after ~1 week of inactivity (one click to resume) and caps at 500 MB DB / 50k monthly auth users — all fine for this term project and demo.

## 3. Canonical data model (accumulated DB)

**Reference / accumulated (shared, pipeline-owned):**

- `cities(id, name, country, lat, lng, bbox, sources_json, updated_at)`
- `places(id, city_id, name, category, lat, lng, address, price_tier, rating, is_hidden_gem, description, image_url, wikidata_id, osm_id, sources_json, cleaned_at)`

**App / per-user (from the proposal's minimum model):**

- `trips(id, user_id, name, start_date, end_date, home_currency, budget)`
- `itinerary_items(id, trip_id, place_id, day, time, duration_min, is_booked, transport_mode, buffer_min, position)`
- `expenses(id, trip_id, itinerary_item_id, amount, currency, converted_amount, category)`

**Cross-referencing (the "mix-and-match"):** one canonical `places` row is assembled from several sources and joined by `wikidata_id` when present, else by fuzzy name + proximity (lat/lng within ~50 m). Provenance is kept in `sources_json` so cleaning is auditable.

## 4. Ingestion pipeline stages

1. **Fetch** — per target city, pull candidates from Nominatim (geocode/bbox), Overpass (POIs in bbox), OpenTripMap (rated POIs), Wikivoyage (article POIs).
2. **Normalize** — map each source's fields to the canonical `places` schema.
3. **Cross-reference / dedupe** — merge rows describing the same place (wikidata_id → else fuzzy).
4. **Enrich** — attach Wikidata/Wikipedia description + image; compute `is_hidden_gem` from popularity signals; (weather and currency are enriched at read-time, not stored, since they change).
5. **Clean / validate** — drop rows missing coordinates or name; clamp fields; dedupe addresses.
6. **Upsert** — write to `places` / `cities`; stamp `cleaned_at`.
7. **Serve** — app reads via REST; ORS routing requests go through the server-side proxy.

## 5. Sequential phases

| Phase | Deliverable | APIs touched | Backlog # |
| --- | --- | --- | --- |
| 0 | Foundations: backend project + canonical schema; Android Retrofit/Room/ViewModel/Executor; **verify free terms**; ORS proxy stub; confirm login security fix builds | — | setup |
| 1 | Ingestion pipeline v1 → cities + cross-referenced, cleaned `places` | Nominatim, Overpass, OpenTripMap, Wikidata, Wikivoyage | 2, 9 (data) |
| 2 | App reads our DB: Add Destination (search our `places`), Discover Mode (famous vs hidden gem), persist trips/itinerary/expenses | our REST | 2, 5, 6, 9 |
| 3 | Route pipeline: ORS Matrix/Directions via proxy; in-house ordering; day-by-day split; per-leg walk/cycle/drive; duration + buffer | OpenRouteService | 3, 4, 7, 8 |
| 4 | Budget + currency live; weather-aware flags | Frankfurter, Open-Meteo | 11, 12, 13, 16 |
| 5 | Auth + Calendar sync (replaces DEV bypass) | Google Calendar (OAuth) | 1, 10 |
| 6 | Enhancements: trip export, group mode, photo/journal | — | 17, 18, 19 |

Each phase is independently shippable; stop or re-prioritize at any phase boundary.

## 6. Risks

| Risk | Likelihood | Mitigation |
| --- | --- | --- |
| Free-tier rate limits (Nominatim, Overpass, ORS) | High | Ingest once into our DB; cron off-peak; cache; per-city batching |
| Nominatim/Overpass usage-policy breach (heavy live calls) | Medium | All heavy calls happen server-side in the pipeline, never per-device |
| Backend adds scope/grading complexity | Medium | Supabase free tier = no ops; fallback to on-device Room if disallowed |
| OSM POI data sparse for some cities | Medium | Layer OpenTripMap + Wikivoyage; flag thin cities in cleaning |
| Google Calendar OAuth setup effort | Medium | Isolated to Phase 5, after the core pipeline works |
| "Famous vs hidden gem" heuristic noisy | Low | Use Wikidata sitelink/pageview counts; tune threshold with a seed city |

## 7. Decisions

1. **Backend** — ✅ **LOCKED: Supabase** (Postgres + REST + Auth + Edge Function ORS proxy); ingestion ETL runs as a GitHub Actions cron.
2. **Seed cities** — ✅ **LOCKED: Southeast Asia focus.** Phase 1 primary seed: **Tokyo (JP)** and **Manila (PH)**. Secondary SEA targets to extend into: Osaka/Kyoto (JP), Cebu (PH), Bangkok (TH), Singapore (SG), Hanoi (VN), Kuala Lumpur (MY). Ingest 1–3 first (Tokyo + Manila), then widen.
3. **Transit** — _open, defaulting to walk/cycle/drive only_ (Google transit dropped as non-free). Confirm if transit is needed; otherwise this default stands.
