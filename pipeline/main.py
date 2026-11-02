"""CLI entrypoint: fetch -> normalize -> dedupe -> enrich -> clean -> upsert, per city.

Usage (from repo root): python -m pipeline.main --cities "Tokyo,JP" "Manila,PH"
"""
import argparse
import logging

from pipeline import (
    clean as clean_mod,
    dedupe as dedupe_mod,
    enrich as enrich_mod,
    fetch_nominatim,
    fetch_opentripmap,
    fetch_overpass,
    fetch_wikidata,
    fetch_wikivoyage,
    normalize as normalize_mod,
    upsert,
)

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")
logger = logging.getLogger(__name__)


def run_city(name, country_code):
    logger.info("Geocoding %s, %s", name, country_code)
    city = fetch_nominatim.geocode_city(name, country_code)

    raw_candidates = []
    raw_candidates.extend(fetch_overpass.fetch_pois(city["bbox"]))
    raw_candidates.extend(fetch_opentripmap.fetch_pois(city["lat"], city["lng"]))
    raw_candidates.extend(fetch_wikidata.fetch_candidates(city["lat"], city["lng"]))
    raw_candidates.extend(fetch_wikivoyage.fetch_listings(name))
    logger.info("%s: %d raw candidates", name, len(raw_candidates))

    normalized = normalize_mod.normalize_all(raw_candidates)
    merged = dedupe_mod.dedupe(normalized)
    logger.info("%s: %d places after dedupe", name, len(merged))

    enriched = enrich_mod.enrich_all(merged)
    cleaned = clean_mod.clean(enriched)
    logger.info("%s: %d places after cleaning", name, len(cleaned))

    city_id = upsert.upsert_city(city)
    inserted = upsert.upsert_places(city_id, cleaned)
    logger.info("%s: upserted %d places (city_id=%s)", name, inserted, city_id)
    return city_id, len(cleaned)


def main():
    parser = argparse.ArgumentParser(description="TripFit places ingestion pipeline")
    parser.add_argument(
        "--cities", nargs="+", required=True,
        help='City,CountryCode pairs, e.g. "Tokyo,JP" "Manila,PH"',
    )
    args = parser.parse_args()

    for spec in args.cities:
        name, country_code = (part.strip() for part in spec.split(","))
        run_city(name, country_code)


if __name__ == "__main__":
    main()
