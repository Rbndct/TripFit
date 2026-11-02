"""Wikivoyage: travel-guide listings embedded in a city's article wikitext.

Wikivoyage articles list POIs with {{see}}/{{do}}/{{eat}}/{{drink}}/{{buy}}/{{sleep}}
templates carrying name/lat/long/content parameters. We parse those directly rather
than assuming separate geo-tagged pages exist (they don't, for most cities).
"""
import re

from pipeline import http_client
from pipeline.config import WIKIVOYAGE_API_URL

_LISTING_TEMPLATE = re.compile(
    r"\{\{\s*(see|do|eat|drink|buy|sleep|listing)\s*\|(.*?)\}\}",
    re.IGNORECASE | re.DOTALL,
)
_PARAM = re.compile(r"(\w+)\s*=\s*([^|}]*)")


def fetch_listings(city_name):
    response = http_client.get(
        WIKIVOYAGE_API_URL,
        params={
            "action": "parse", "page": city_name, "prop": "wikitext",
            "format": "json", "redirects": 1,
        },
        min_interval=1.0,
    )
    payload = response.json()
    if "error" in payload:
        return []
    wikitext = payload["parse"]["wikitext"]["*"]

    places = []
    for match in _LISTING_TEMPLATE.finditer(wikitext):
        template_type, body = match.group(1).lower(), match.group(2)
        params = {k.lower(): v.strip() for k, v in _PARAM.findall(body)}
        name = params.get("name")
        lat, lng = params.get("lat"), params.get("long")
        if not name or not lat or not lng:
            continue
        try:
            places.append({
                "name": name,
                "category": template_type,
                "lat": float(lat),
                "lng": float(lng),
                "address": params.get("address"),
                "description": params.get("content") or params.get("alt"),
                "sources_json": {"wikivoyage": {"page": city_name, "template": template_type}},
            })
        except ValueError:
            continue
    return places
