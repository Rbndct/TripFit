"""Final validation pass before upsert: drop invalid rows, clamp fields, dedupe addresses."""
import time

_MIN_LAT, _MAX_LAT = -90.0, 90.0
_MIN_LNG, _MAX_LNG = -180.0, 180.0
_MIN_RATING, _MAX_RATING = 0.0, 5.0
_MIN_PRICE_TIER, _MAX_PRICE_TIER = 1, 4


def clean(places):
    cleaned = []
    seen_addresses = set()

    for place in places:
        if not _has_name_and_coords(place):
            continue
        if not _in_valid_range(place):
            continue

        place = dict(place)
        place["rating"] = _clamp_optional(place.get("rating"), _MIN_RATING, _MAX_RATING)
        place["price_tier"] = _clamp_optional(place.get("price_tier"), _MIN_PRICE_TIER, _MAX_PRICE_TIER)

        address = place.get("address")
        if address:
            key = (place["name"].strip().lower(), address.strip().lower())
            if key in seen_addresses:
                continue
            seen_addresses.add(key)

        place["cleaned_at"] = int(time.time())
        cleaned.append(place)

    return cleaned


def _has_name_and_coords(place):
    return bool(place.get("name")) and place.get("lat") is not None and place.get("lng") is not None


def _in_valid_range(place):
    return (_MIN_LAT <= place["lat"] <= _MAX_LAT) and (_MIN_LNG <= place["lng"] <= _MAX_LNG)


def _clamp_optional(value, low, high):
    if value is None:
        return None
    return max(low, min(high, value))
