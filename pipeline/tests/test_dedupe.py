import json
import os
import unittest

from pipeline.dedupe import dedupe
from pipeline.normalize import normalize_all

_FIXTURE_PATH = os.path.join(os.path.dirname(__file__), "fixtures", "raw_candidates.json")


class DedupeTest(unittest.TestCase):
    def setUp(self):
        with open(_FIXTURE_PATH) as f:
            raw = json.load(f)
        self.candidates = normalize_all(raw)

    def test_merges_same_place_by_proximity_and_fuzzy_name(self):
        merged = dedupe(self.candidates)
        senso_ji_matches = [p for p in merged if "sens" in p["name"].lower()]
        self.assertEqual(len(senso_ji_matches), 1)

    def test_merged_place_keeps_wikidata_id_from_either_source(self):
        merged = dedupe(self.candidates)
        senso_ji = next(p for p in merged if "sens" in p["name"].lower())
        self.assertEqual(senso_ji["wikidata_id"], "Q335011")

    def test_merged_place_unions_sources_json(self):
        merged = dedupe(self.candidates)
        senso_ji = next(p for p in merged if "sens" in p["name"].lower())
        self.assertIn("overpass", senso_ji["sources_json"])
        self.assertIn("wikidata", senso_ji["sources_json"])

    def test_distinct_places_stay_separate(self):
        merged = dedupe(self.candidates)
        names = {p["name"] for p in merged}
        self.assertIn("Meiji Shrine", names)

    def test_candidate_without_coords_is_its_own_group(self):
        merged = dedupe(self.candidates)
        self.assertTrue(any(p["name"] == "Random Cafe With No Coords" for p in merged))

    def test_does_not_merge_distant_places_with_similar_names(self):
        a = {"name": "City Park", "lat": 35.0, "lng": 139.0, "sources_json": {}}
        b = {"name": "City Park", "lat": 35.01, "lng": 139.01, "sources_json": {}}
        merged = dedupe(normalize_all([a, b]))
        self.assertEqual(len(merged), 2)


if __name__ == "__main__":
    unittest.main()
