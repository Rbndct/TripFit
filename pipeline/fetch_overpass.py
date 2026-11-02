"""Overpass API: POI candidates (amenities, tourism, shops) inside a city bbox.

One batched query per city — never per-POI calls — per the Overpass fair-use
expectation referenced in DEV-HANDOFF.md §2.
"""
from pipeline import http_client
from pipeline.config import OVERPASS_URL

_TOURISM_TAGS = (
    "tourism~\"attraction|museum|gallery|viewpoint|zoo|theme_park|artwork\""
)
_AMENITY_TAGS = "amenity~\"place_of_worship|marketplace\""
_HISTORIC_TAGS = "historic"

_QUERY_TEMPLATE = """
[out:json][timeout:120];
(
  node[{tourism}]({south},{west},{north},{east});
  way[{tourism}]({south},{west},{north},{east});
  node[{amenity}]({south},{west},{north},{east});
  node[{historic}]({south},{west},{north},{east});
);
out center tags;
""".strip()


def fetch_pois(bbox, limit=150):
    query = _QUERY_TEMPLATE.format(
        tourism=_TOURISM_TAGS,
        amenity=_AMENITY_TAGS,
        historic=_HISTORIC_TAGS,
        south=bbox["south"], west=bbox["west"], north=bbox["north"], east=bbox["east"],
    )
    response = http_client.post(OVERPASS_URL, data={"data": query}, min_interval=2.0, timeout=150)
    elements = response.json().get("elements", [])
    return parse_elements(elements, limit)


def parse_elements(elements, limit=150):
    places = []
    for element in elements[:limit]:
        tags = element.get("tags", {})
        name = tags.get("name")
        if not name:
            continue
        lat = element.get("lat") or element.get("center", {}).get("lat")
        lng = element.get("lon") or element.get("center", {}).get("lon")
        if lat is None or lng is None:
            continue
        places.append({
            "name": name,
            "category": tags.get("tourism") or tags.get("amenity") or tags.get("historic"),
            "lat": lat,
            "lng": lng,
            "address": _format_address(tags),
            "osm_id": f"{element['type']}/{element['id']}",
            "sources_json": {"overpass": {"osm_id": f"{element['type']}/{element['id']}", "tags": tags}},
        })
    return places


def _format_address(tags):
    parts = [tags.get("addr:housenumber"), tags.get("addr:street")]
    joined = " ".join(p for p in parts if p)
    return joined or None
