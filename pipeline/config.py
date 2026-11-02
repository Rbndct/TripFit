"""Seed-city config and canonical category/threshold constants."""

SEED_CITIES = [
    {"name": "Tokyo", "country": "JP"},
    {"name": "Manila", "country": "PH"},
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
