"""OpenTripMap: rated POIs. Server-side key only (never on-device).

No OPENTRIPMAP_API_KEY is provisioned in this project's secrets yet (confirmed against
`gh secret list` during Phase 1 setup). Per the free-API guardrail ("don't work around a
missing/paid key"), this adapter degrades gracefully: it logs a warning and returns an
empty list rather than failing the pipeline or fabricating a key.
"""
import logging
import os

from pipeline import http_client
from pipeline.config import OPENTRIPMAP_BASE_URL

logger = logging.getLogger(__name__)


def fetch_pois(lat, lng, radius_m=5000, limit=100):
    api_key = os.environ.get("OPENTRIPMAP_API_KEY")
    if not api_key:
        logger.warning(
            "OPENTRIPMAP_API_KEY not set — skipping OpenTripMap source for this run. "
            "Cross-referencing continues with Nominatim/Overpass/Wikidata/Wikivoyage only."
        )
        return []

    response = http_client.get(
        f"{OPENTRIPMAP_BASE_URL}/radius",
        params={
            "radius": radius_m, "lon": lng, "lat": lat, "limit": limit,
            "rate": 2, "format": "json", "apikey": api_key,
        },
        min_interval=1.0,
    )
    places = []
    for item in response.json():
        name = item.get("name")
        if not name:
            continue
        places.append({
            "name": name,
            "category": (item.get("kinds") or "").split(",")[0] or None,
            "lat": item["point"]["lat"],
            "lng": item["point"]["lon"],
            "rating": item.get("rate"),
            "wikidata_id": _extract_wikidata_id(item),
            "osm_id": item.get("osm"),
            "sources_json": {"opentripmap": {"xid": item.get("xid"), "kinds": item.get("kinds")}},
        })
    return places


def _extract_wikidata_id(item):
    wikidata = item.get("wikidata")
    return wikidata if wikidata else None
