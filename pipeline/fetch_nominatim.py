"""Nominatim: used only to geocode the seed city to a center point + bbox.
Not used for POI candidates (that's Overpass) — keeps Nominatim calls to one per city.
"""
from pipeline import http_client
from pipeline.config import NOMINATIM_SEARCH_URL


def geocode_city(name, country_code):
    response = http_client.get(
        NOMINATIM_SEARCH_URL,
        params={
            "q": f"{name}, {country_code}",
            "format": "jsonv2",
            "limit": 1,
            "addressdetails": 0,
        },
        min_interval=1.0,
    )
    results = response.json()
    if not results:
        raise ValueError(f"Nominatim found no match for {name}, {country_code}")

    result = results[0]
    south, north, west, east = (float(v) for v in result["boundingbox"])
    return {
        "name": name,
        "country": country_code,
        "lat": float(result["lat"]),
        "lng": float(result["lon"]),
        "bbox": {"south": south, "north": north, "west": west, "east": east},
        "osm_id": f"{result.get('osm_type', 'relation')}/{result.get('osm_id')}",
    }
