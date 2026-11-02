# Free-API Terms Verification

Checked 2026-10-26 against each service's current official policy/pricing page. **No service in this
stack requires a billing account for our volume.** Two services (Nominatim, Overpass) are explicitly
"no heavy/bulk use" — that's exactly why the architecture ingests once into our own DB rather than
calling them per-device (see `API-Integration-Plan.md` §2).

| Service | Free? | Limit (as published) | Key required | Source |
| --- | --- | --- | --- | --- |
| Nominatim | Yes | No published hard quota; policy explicitly forbids "heavy uses", requires descriptive `User-Agent`, ~1 req/s. Public instance is donated infra. | No | https://operations.osmfoundation.org/policies/nominatim/ |
| Overpass API | Yes | No fixed quota on public mirrors; fair-use/rate-limited, batch + cache expected. | No | https://wiki.openstreetmap.org/wiki/Overpass_API |
| OpenTripMap | Likely yes, could not auto-verify | Pricing page is JS-rendered (couldn't scrape); publicly documented free tier has historically been ~1000 req/day with a free key. **Not independently reconfirmed this run** — no `OPENTRIPMAP_API_KEY` is provisioned in GitHub secrets, so the pipeline adapter skips this source gracefully rather than guessing. Human should spot-check before relying on it. | Yes (free signup) | https://opentripmap.io/en/product (JS redirect wall) |
| Wikidata | Yes | No request key; Wikimedia API etiquette (reasonable rate, `User-Agent`) applies, no commercial/billing gate. | No | https://www.wikidata.org/wiki/Wikidata:Data_access |
| Wikipedia (MediaWiki REST) | Yes | Same Wikimedia terms as Wikidata; no billing tier. | No | https://api.wikimedia.org/wiki/Core_REST_API |
| Wikivoyage | Yes | Same MediaWiki API terms (shared infra with Wikipedia). | No | https://en.wikivoyage.org/wiki/Wikivoyage:API |
| OpenRouteService | Yes | Confirmed current: default **2000 req/day**, **40 req/min** on the `directions` endpoint (matrix has its own limit); daily quota resets on a rolling 24h window from first request, shown in the developer dashboard. Sign-up is free, no card required. | Yes (free signup, server-side only) | https://giscience.github.io/openrouteservice/frequently-asked-questions, https://account.heigit.org/signup |
| Open-Meteo | Yes | Free API is **non-commercial use, 10,000 calls/day**, no uptime SLA. (Paid "customer API" exists for commercial/unlimited use — we don't need it.) | No | https://open-meteo.com/en/pricing |
| Frankfurter | Yes | ECB reference rates, no published hard limit, no key. | No | https://www.frankfurter.dev/ |
| Google Calendar API | Yes | Calendar API itself has a generous free quota under standard OAuth consent; no billing account required to enable it (distinct from Maps Platform, which does require billing — not used here). | OAuth (no API key, no billing) | https://developers.google.com/workspace/calendar/api/guides/overview |

## Notes

- **Dropped already (per DEV-HANDOFF.md):** Google Directions/Maps Platform (billing required), Foursquare paid tier, OpenWeather paid tiers. Not re-checked since they're out of scope by design.
- **OpenTripMap is the one open item.** The fetch adapter (`pipeline/fetch_opentripmap.py`) checks `OPENTRIPMAP_API_KEY` at runtime and logs a warning + returns an empty result set if it's absent, rather than failing the pipeline or inventing a key. Cross-referencing still works from Nominatim + Overpass + Wikidata + Wikivoyage alone; OpenTripMap mainly adds POI ratings, which degrade to "unrated" without it.
- Re-verify this table before any term where ingestion volume grows significantly (e.g. expanding past the two SEA seed cities) — free-tier terms can change.
