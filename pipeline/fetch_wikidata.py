"""Wikidata: structured POI candidates + the popularity signal used for is_hidden_gem.

Two calls: a SPARQL geo-radius query for candidates, then (in enrich.py) per-item
sitelink counts for already-matched places.
"""
from pipeline import http_client
from pipeline.config import WIKIDATA_ENTITY_URL, WIKIDATA_SPARQL_URL

# Broad "visitable place" classes: tourist attraction, museum, monument, park,
# place of worship, castle, zoo. Kept broad on purpose — dedupe.py and clean.py
# filter the merged result, not this fetch step.
_CLASSES = ("Q570116", "Q33506", "Q4989906", "Q22698", "Q16560", "Q43229", "Q43501")

_SPARQL_TEMPLATE = """
SELECT ?item ?itemLabel ?coord WHERE {{
  SERVICE wikibase:around {{
    ?item wdt:P625 ?coord .
    bd:serviceParam wikibase:center "Point({lng} {lat})"^^geo:wktLiteral .
    bd:serviceParam wikibase:radius "{radius_km}" .
  }}
  ?item wdt:P31 ?class .
  VALUES ?class {{ {classes} }}
  SERVICE wikibase:label {{ bd:serviceParam wikibase:language "en". }}
}}
LIMIT {limit}
""".strip()


def fetch_candidates(lat, lng, radius_km=8, limit=200):
    query = _SPARQL_TEMPLATE.format(
        lat=lat, lng=lng, radius_km=radius_km, limit=limit,
        classes=" ".join(f"wd:{c}" for c in _CLASSES),
    )
    response = http_client.get(
        WIKIDATA_SPARQL_URL,
        params={"query": query, "format": "json"},
        headers={"Accept": "application/sparql-results+json"},
        min_interval=1.0,
        timeout=60,
    )
    bindings = response.json()["results"]["bindings"]

    places = []
    for row in bindings:
        label = row.get("itemLabel", {}).get("value")
        coord = row.get("coord", {}).get("value", "")
        qid = row["item"]["value"].rsplit("/", 1)[-1]
        point = _parse_point(coord)
        if not label or point is None:
            continue
        places.append({
            "name": label,
            "category": None,
            "lat": point[1],
            "lng": point[0],
            "wikidata_id": qid,
            "sources_json": {"wikidata": {"qid": qid}},
        })
    return places


def fetch_sitelink_count(qid):
    response = http_client.get(
        WIKIDATA_ENTITY_URL,
        params={"action": "wbgetentities", "ids": qid, "props": "sitelinks", "format": "json"},
        min_interval=0.5,
    )
    entity = response.json().get("entities", {}).get(qid, {})
    sitelinks = entity.get("sitelinks", {})
    enwiki_title = sitelinks.get("enwiki", {}).get("title")
    return len(sitelinks), enwiki_title


def _parse_point(wkt):
    # "Point(lng lat)"
    if not wkt.startswith("Point(") or not wkt.endswith(")"):
        return None
    lng_str, lat_str = wkt[len("Point("):-1].split(" ")
    return float(lng_str), float(lat_str)
