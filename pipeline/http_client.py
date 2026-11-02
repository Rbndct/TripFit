"""Shared HTTP helper: descriptive User-Agent + per-host rate limiting.

Nominatim's usage policy caps public-instance use at ~1 req/s with a descriptive
User-Agent; Overpass expects batched, polite use. All heavy source calls happen
here, in the pipeline, never per-device (see API-Integration-Plan.md §2/§6).
"""
import time
from urllib.parse import urlparse

import requests

USER_AGENT = "TripFit-ETL/1.0 (DLSU student project; contact rbenedict.maagma@gmail.com)"

_last_request_at = {}


def get(url, params=None, headers=None, min_interval=1.0, timeout=30):
    host = urlparse(url).netloc
    _throttle(host, min_interval)
    hdrs = {"User-Agent": USER_AGENT}
    if headers:
        hdrs.update(headers)
    response = requests.get(url, params=params, headers=hdrs, timeout=timeout)
    _last_request_at[host] = time.monotonic()
    response.raise_for_status()
    return response


def post(url, data=None, json=None, headers=None, min_interval=1.0, timeout=30):
    host = urlparse(url).netloc
    _throttle(host, min_interval)
    hdrs = {"User-Agent": USER_AGENT}
    if headers:
        hdrs.update(headers)
    response = requests.post(url, data=data, json=json, headers=hdrs, timeout=timeout)
    _last_request_at[host] = time.monotonic()
    response.raise_for_status()
    return response


def _throttle(host, min_interval):
    last = _last_request_at.get(host)
    if last is None:
        return
    elapsed = time.monotonic() - last
    if elapsed < min_interval:
        time.sleep(min_interval - elapsed)
