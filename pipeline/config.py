"""Seed-city config and canonical category/threshold constants."""

SEED_CITIES = [
    {"name": "Tokyo", "country": "JP"},
    {"name": "Manila", "country": "PH"},
]

# Secondary SEA expansion targets (DEV-HANDOFF.md §9, decision 2). Not run automatically --
# pass explicitly via `--cities` or the ingest workflow's `cities` dispatch input.
SECONDARY_CITIES = [
    {"name": "Osaka", "country": "JP"},
    {"name": "Kyoto", "country": "JP"},
    {"name": "Cebu City", "country": "PH"},
    {"name": "Bangkok", "country": "TH"},
    {"name": "Singapore", "country": "SG"},
    {"name": "Hanoi", "country": "VN"},
    {"name": "Kuala Lumpur", "country": "MY"},
]

# A Wikidata item with this many or fewer sitelinks, but at least one, reads as
# locally/regionally known rather than globally famous — our "hidden gem" proxy.
# Items with zero Wikidata presence are "unknown", not confirmed hidden gems.
HIDDEN_GEM_MAX_SITELINKS = 3

NOMINATIM_SEARCH_URL = "https://nominatim.openstreetmap.org/search"
OVERPASS_URL = "https://overpass-api.de/api/interpreter"
OPENTRIPMAP_BASE_URL = "https://api.opentripmap.com/0.1/en/places"
WIKIDATA_SPARQL_URL = "https://query.wikidata.org/sparql"
WIKIDATA_ENTITY_URL = "https://www.wikidata.org/w/api.php"
WIKIPEDIA_SUMMARY_URL = "https://en.wikipedia.org/api/rest_v1/page/summary/{title}"
WIKIVOYAGE_API_URL = "https://en.wikivoyage.org/w/api.php"

# Metres within which two candidates are considered the same physical place
# when they can't be matched by a stable external id (wikidata_id/osm_id).
DEDUPE_PROXIMITY_METERS = 50

# Some Nominatim admin-boundary bboxes are far larger than "the city" for POI
# purposes -- e.g. Tokyo's includes the Izu/Ogasawara islands 1000+ km south,
# producing an 18x15 degree box that times out/truncates against Overpass.
# Clamp to a box this wide (degrees), centered on the geocoded point, which
# comfortably covers a city's urban extent without pulling in remote exclaves.
MAX_CITY_BBOX_SPAN_DEG = 0.6
