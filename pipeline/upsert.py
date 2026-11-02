"""Upserts cities/places into Supabase via plain PostgREST calls.

Used by the GitHub Actions workflow, which has SUPABASE_URL/SUPABASE_SERVICE_KEY as
secrets. The service_role key bypasses RLS by design — this is the only place that key
is used, and it never leaves the pipeline's environment.
"""
import json
import logging
import os

import requests

logger = logging.getLogger(__name__)


def _headers():
    service_key = os.environ["SUPABASE_SERVICE_KEY"]
    return {
        "apikey": service_key,
        "Authorization": f"Bearer {service_key}",
        "Content-Type": "application/json",
    }


def _base_url():
    return os.environ["SUPABASE_URL"].rstrip("/") + "/rest/v1"


def upsert_city(city):
    url = f"{_base_url()}/cities?on_conflict=name,country"
    headers = {**_headers(), "Prefer": "resolution=merge-duplicates,return=representation"}
    body = [{
        "name": city["name"],
        "country": city["country"],
        "lat": city["lat"],
        "lng": city["lng"],
        "bbox": city.get("bbox"),
        "sources_json": city.get("sources_json", {}),
    }]
    response = requests.post(url, headers=headers, data=json.dumps(body), timeout=30)
    response.raise_for_status()
    return response.json()[0]["id"]


def upsert_places(city_id, places):
    """Splits the batch by which unique key each row can upsert on, per PostgREST's
    one-conflict-target-per-request limitation."""
    with_wikidata = [p for p in places if p.get("wikidata_id")]
    with_osm_only = [p for p in places if not p.get("wikidata_id") and p.get("osm_id")]
    with_neither = [p for p in places if not p.get("wikidata_id") and not p.get("osm_id")]

    inserted = 0
    inserted += _upsert_batch(city_id, with_wikidata, "wikidata_id")
    inserted += _upsert_batch(city_id, with_osm_only, "osm_id")
    inserted += _insert_batch(city_id, with_neither)
    return inserted


def _row(city_id, place):
    return {
        "city_id": city_id,
        "name": place["name"],
        "category": place.get("category"),
        "lat": place["lat"],
        "lng": place["lng"],
        "address": place.get("address"),
        "price_tier": place.get("price_tier"),
        "rating": place.get("rating"),
        "is_hidden_gem": bool(place.get("is_hidden_gem")),
        "description": place.get("description"),
        "image_url": place.get("image_url"),
        "wikidata_id": place.get("wikidata_id"),
        "osm_id": place.get("osm_id"),
        "sources_json": place.get("sources_json", {}),
        "cleaned_at": place.get("cleaned_at"),
    }


def _upsert_batch(city_id, places, conflict_column):
    if not places:
        return 0
    url = f"{_base_url()}/places?on_conflict={conflict_column}"
    headers = {**_headers(), "Prefer": "resolution=merge-duplicates,return=minimal"}
    body = [_row(city_id, p) for p in places]
    response = requests.post(url, headers=headers, data=json.dumps(body), timeout=30)
    response.raise_for_status()
    return len(places)


def _insert_batch(city_id, places):
    if not places:
        return 0
    url = f"{_base_url()}/places"
    headers = {**_headers(), "Prefer": "return=minimal"}
    body = [_row(city_id, p) for p in places]
    response = requests.post(url, headers=headers, data=json.dumps(body), timeout=30)
    response.raise_for_status()
    return len(places)
