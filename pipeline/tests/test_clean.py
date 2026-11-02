import unittest

from pipeline.clean import clean


class CleanTest(unittest.TestCase):
    def test_drops_places_missing_name(self):
        places = [{"name": "", "lat": 1, "lng": 1}]
        self.assertEqual(clean(places), [])

    def test_drops_places_missing_coords(self):
        places = [{"name": "X", "lat": None, "lng": None}]
        self.assertEqual(clean(places), [])

    def test_drops_places_with_out_of_range_coords(self):
        places = [{"name": "X", "lat": 999, "lng": 1}]
        self.assertEqual(clean(places), [])

    def test_clamps_rating_into_valid_range(self):
        places = [{"name": "X", "lat": 1, "lng": 1, "rating": 9.5}]
        result = clean(places)
        self.assertEqual(result[0]["rating"], 5.0)

    def test_leaves_missing_rating_as_none(self):
        places = [{"name": "X", "lat": 1, "lng": 1, "rating": None}]
        result = clean(places)
        self.assertIsNone(result[0]["rating"])

    def test_dedupes_same_name_and_address_within_batch(self):
        places = [
            {"name": "Cafe A", "lat": 1, "lng": 1, "address": "123 Main St"},
            {"name": "Cafe A", "lat": 1, "lng": 1, "address": "123 Main St"},
        ]
        result = clean(places)
        self.assertEqual(len(result), 1)

    def test_stamps_cleaned_at(self):
        places = [{"name": "X", "lat": 1, "lng": 1}]
        result = clean(places)
        self.assertIsNotNone(result[0]["cleaned_at"])


if __name__ == "__main__":
    unittest.main()
