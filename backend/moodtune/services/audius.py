import os
import random
import requests
import time
from requests.exceptions import RequestException


class AudiusError(Exception):
    """Raised when an Audius network or protocol error occurs."""
    pass

AUDIUS_DIRECTORY = "https://api.audius.co"
AUDIUS_APP_NAME = os.getenv("AUDIUS_APP_NAME", "Moodtune")
# legacy/compat name used by the rest of the module
APP_NAME = AUDIUS_APP_NAME

# simple in-process cache for discovery host for 5 minutes
_cached_host = None
_cached_ts = 0
_CACHE_TTL = 300  # seconds


def _pick_host():  #pick a Audius server 
    """Get a discovery host from https://api.audius.co (cached for a few minutes)."""
    global _cached_host, _cached_ts
    now = time.time()
    if _cached_host and (now - _cached_ts) < _CACHE_TTL:
        return _cached_host

    # === FETCH 1: ask the directory for healthy discovery hosts ===
    try:
        r = requests.get(AUDIUS_DIRECTORY, timeout=5) # Randomly picks one and caches it for 5 minutes
        r.raise_for_status()
        hosts = r.json().get("data", [])
    except RequestException as exc:
        # Bubble up a clearer error for callers to handle
        raise AudiusError(f"failed to fetch discovery hosts: {exc}") from exc
    _cached_host = random.choice(hosts) if hosts else "https://discoveryprovider.audius.co"
    _cached_ts = now
    return _cached_host


def _normalize(track, host):
    """Convert Audius JSON to the shape the app returns to the frontend.

    No network call here—just formatting.
    """
    return {
        "id": track["id"],
        "title": track.get("title"),
        "artist": (track.get("user") or {}).get("name"),
        "artwork": (track.get("artwork") or {}).get("480x480"),
        "permalink": track.get("permalink"),
        "source": "audius",
        # can hand this URL directly to an <audio> tag or 302-redirect to it.
        "stream_url": f"{host}/v1/tracks/{track['id']}/stream?app_name={APP_NAME}",
    }

#search for tracks
def search_by_mood(moods, limit=25):
    """Build a /v1/tracks/search query to Audius filtered by mood.

    This function performs the network fetch and normalizes results.
    """
    host = _pick_host()
    params = [("mood[]", m) for m in moods]
    params += [("sort_method", "popular"), ("limit", str(limit)), ("app_name", APP_NAME)]

    # === FETCH 2: hit the Audius Search API ===
    url = f"{host}/v1/tracks/search"
    print("GET", url, params)
    try:
        r = requests.get(url, params=params, timeout=8)
        r.raise_for_status()
        data = r.json().get("data", [])
        return [_normalize(t, host) for t in data]
    except RequestException as exc:
        raise AudiusError(f"audius search failed: {exc}") from exc

# Gets the actual streaming URL for a specific track
def resolve_stream_url(track_id):
    """Resolve the final stream URL for a track (no redirect).

    Returns the data payload from Audius which typically contains the final URL.
    """
    host = _pick_host()
    url = f"{host}/v1/tracks/{track_id}/stream"
    try:
        r = requests.get(url, params={"no_redirect": "true", "app_name": APP_NAME}, timeout=8)
        r.raise_for_status()
        return r.json().get("data")
    except RequestException as exc:
        raise AudiusError(f"failed to resolve stream url: {exc}") from exc
