"""Cross-references candidates describing the same physical place.

Merge key: wikidata_id when present (exact). Otherwise fuzzy name match (ratio >= 0.85)
AND within DEDUPE_PROXIMITY_METERS of an existing group's representative point.
"""
import math
import re
from difflib import SequenceMatcher

from pipeline.config import DEDUPE_PROXIMITY_METERS

_EARTH_RADIUS_M = 6371000

# Generic words that differ between sources for the same place ("Senso-ji" vs.
# "Sensoji Temple") but carry no identifying signal once two candidates are
# already within DEDUPE_PROXIMITY_METERS of each other.
_GENERIC_SUFFIXES = (
    "temple", "shrine", "park", "museum", "tower", "garden", "gardens",
    "market", "zoo", "palace", "castle", "street", "crossing",
)
_NON_ALNUM = re.compile(r"[^a-z0-9]+")


def dedupe(candidates):
    groups = []
    wikidata_index = {}

    for candidate in candidates:
        wikidata_id = candidate.get("wikidata_id")
        if wikidata_id and wikidata_id in wikidata_index:
            groups[wikidata_index[wikidata_id]].append(candidate)
            continue

        match_index = _find_matching_group(candidate, groups)
        if match_index is not None:
            groups[match_index].append(candidate)
            if wikidata_id:
                wikidata_index[wikidata_id] = match_index
        else:
            groups.append([candidate])
            if wikidata_id:
                wikidata_index[wikidata_id] = len(groups) - 1

    return [_merge_group(group) for group in groups]


def _find_matching_group(candidate, groups):
    if candidate.get("lat") is None or candidate.get("lng") is None:
        return None
    for index, group in enumerate(groups):
        representative = group[0]
        if representative.get("lat") is None:
            continue
        distance = _haversine_meters(
            candidate["lat"], candidate["lng"], representative["lat"], representative["lng"]
        )
        if distance <= DEDUPE_PROXIMITY_METERS and _names_match(candidate["name"], representative["name"]):
            return index
    return None


def _names_match(a, b):
    a_norm, b_norm = _normalize_name(a), _normalize_name(b)
    if a_norm == b_norm or a_norm in b_norm or b_norm in a_norm:
        return True
    return SequenceMatcher(None, a_norm, b_norm).ratio() >= 0.85


def _normalize_name(name):
    tokens = _NON_ALNUM.sub(" ", name.lower()).split()
    significant = [t for t in tokens if t not in _GENERIC_SUFFIXES]
    return "".join(significant or tokens)


def _haversine_meters(lat1, lng1, lat2, lng2):
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlambda = math.radians(lng2 - lng1)
    a = math.sin(dphi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(dlambda / 2) ** 2
    return 2 * _EARTH_RADIUS_M * math.asin(math.sqrt(a))


_FILLABLE_FIELDS = (
    "category", "address", "price_tier", "rating", "description", "image_url",
    "wikidata_id", "osm_id",
)


def _merge_group(group):
    merged = dict(group[0])
    sources = {}
    for candidate in group:
        sources.update(candidate.get("sources_json") or {})
        for field in _FILLABLE_FIELDS:
            if not merged.get(field) and candidate.get(field):
                merged[field] = candidate[field]
        if len(candidate.get("name") or "") > len(merged.get("name") or ""):
            merged["name"] = candidate["name"]
    merged["sources_json"] = sources
    return merged
