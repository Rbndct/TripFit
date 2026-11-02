"""Nominatim: used only to geocode the seed city to a center point + bbox.
Not used for POI candidates (that's Overpass) — keeps Nominatim calls to one per city.
"""
from pipeline import http_client
from pipeline.config import MAX_CITY_BBOX_SPAN_DEG, NOMINATIM_SEARCH_URL


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
    lat, lng = float(result["lat"]), float(result["lon"])
    south, north, west, east = (float(v) for v in result["boundingbox"])
    return {
        "name": name,
        "country": country_code,
        "lat": lat,
        "lng": lng,
        "bbox": _clamp_bbox(lat, lng, south, north, west, east),
        "osm_id": f"{result.get('osm_type', 'relation')}/{result.get('osm_id')}",
    }


def _clamp_bbox(lat, lng, south, north, west, east):
    """Caps an oversized admin-boundary bbox to a city-sized box centered on
    the geocoded point (see MAX_CITY_BBOX_SPAN_DEG for why)."""
    half_span = MAX_CITY_BBOX_SPAN_DEG / 2
    return {
        "south": max(south, lat - half_span),
        "north": min(north, lat + half_span),
        "west": max(west, lng - half_span),
        "east": min(east, lng + half_span),
    }
