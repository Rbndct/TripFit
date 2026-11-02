import unittest

from pipeline.fetch_nominatim import _clamp_bbox


class ClampBboxTest(unittest.TestCase):
    def test_leaves_city_sized_bbox_untouched(self):
        clamped = _clamp_bbox(14.6, 121.0, south=14.5, north=14.7, west=120.9, east=121.1)
        self.assertEqual(clamped, {"south": 14.5, "north": 14.7, "west": 120.9, "east": 121.1})

    def test_clamps_oversized_admin_boundary_bbox(self):
        # Tokyo-like case: Nominatim's boundary spans ~18x15 degrees (includes
        # remote islands); the clamp should cap each axis to <= 0.6 degrees
        # without ever growing past the original (real) boundary.
        clamped = _clamp_bbox(
            35.68, 139.76, south=20.21, north=35.90, west=135.85, east=154.21
        )
        self.assertLessEqual(clamped["north"] - clamped["south"], 0.6 + 1e-9)
        self.assertLessEqual(clamped["east"] - clamped["west"], 0.6 + 1e-9)
        self.assertGreaterEqual(clamped["south"], 20.21)
        self.assertLessEqual(clamped["north"], 35.90)
        self.assertGreaterEqual(clamped["west"], 135.85)
        self.assertLessEqual(clamped["east"], 154.21)


if __name__ == "__main__":
    unittest.main()
