import unittest

from pipeline.normalize import normalize, normalize_all


class NormalizeTest(unittest.TestCase):
    def test_normalize_fills_missing_keys_with_none(self):
        result = normalize({"name": "Test Place", "lat": 1.0, "lng": 2.0})

        self.assertIsNone(result["category"])
        self.assertIsNone(result["address"])
        self.assertIsNone(result["wikidata_id"])
        self.assertEqual(result["name"], "Test Place")

    def test_normalize_coerces_is_hidden_gem_to_bool(self):
        result = normalize({"name": "X", "lat": 1, "lng": 1, "is_hidden_gem": None})
        self.assertIs(result["is_hidden_gem"], False)

    def test_normalize_defaults_sources_json_to_empty_dict(self):
        result = normalize({"name": "X", "lat": 1, "lng": 1, "sources_json": None})
        self.assertEqual(result["sources_json"], {})

    def test_normalize_all_preserves_order_and_count(self):
        raw = [{"name": "A", "lat": 1, "lng": 1}, {"name": "B", "lat": 2, "lng": 2}]
        result = normalize_all(raw)
        self.assertEqual([r["name"] for r in result], ["A", "B"])


if __name__ == "__main__":
    unittest.main()
