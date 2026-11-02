"""Normalizes a fetch adapter's raw candidate dict to the full canonical `places` shape.

Every candidate ends up with the same keys so dedupe/enrich/clean don't need to
special-case which source it came from.
"""

_CANONICAL_KEYS = (
    "name", "category", "lat", "lng", "address", "price_tier", "rating",
    "is_hidden_gem", "description", "image_url", "wikidata_id", "osm_id",
    "sources_json",
)


def normalize(raw):
    canonical = {key: raw.get(key) for key in _CANONICAL_KEYS}
    canonical["is_hidden_gem"] = bool(canonical["is_hidden_gem"])
    canonical["sources_json"] = canonical["sources_json"] or {}
    return canonical


def normalize_all(raw_candidates):
    return [normalize(c) for c in raw_candidates]
