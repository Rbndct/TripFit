"""Enriches merged places with a Wikipedia description/image and the is_hidden_gem flag.

Only runs for places with a wikidata_id — that's our only reliable path to a popularity
signal (sitelink count) and a Wikipedia article to summarize.
"""
import logging

from pipeline import fetch_wikidata
from pipeline.config import HIDDEN_GEM_MAX_SITELINKS, WIKIPEDIA_SUMMARY_URL
from pipeline import http_client

logger = logging.getLogger(__name__)


def enrich_all(places):
    return [enrich(place) for place in places]


def enrich(place):
    wikidata_id = place.get("wikidata_id")
    if not wikidata_id:
        return place

    try:
        sitelink_count, enwiki_title = fetch_wikidata.fetch_sitelink_count(wikidata_id)
    except Exception:
        logger.warning("Could not fetch sitelinks for %s", wikidata_id, exc_info=True)
        return place

    place["is_hidden_gem"] = 0 < sitelink_count <= HIDDEN_GEM_MAX_SITELINKS

    if enwiki_title and not place.get("description"):
        summary = _fetch_wikipedia_summary(enwiki_title)
        if summary:
            place["description"] = summary.get("extract") or place.get("description")
            thumbnail = summary.get("thumbnail", {}).get("source")
            place["image_url"] = thumbnail or place.get("image_url")

    place.setdefault("sources_json", {})["wikidata"] = {
        **place["sources_json"].get("wikidata", {}),
        "sitelink_count": sitelink_count,
    }
    return place


def _fetch_wikipedia_summary(title):
    try:
        response = http_client.get(WIKIPEDIA_SUMMARY_URL.format(title=title), min_interval=0.5)
        return response.json()
    except Exception:
        logger.warning("Could not fetch Wikipedia summary for %s", title, exc_info=True)
        return None
