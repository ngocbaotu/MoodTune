import os
import random
import requests
import time
from requests.exceptions import RequestException #catch all network-related errors from requests


class AudiusError(Exception):
    """Raised when an Audius network or protocol error occurs."""
    pass

AUDIUS_DIRECTORY = "https://api.audius.co" #Audius's main API that returns a list of healthy server nodes
AUDIUS_APP_NAME = os.getenv("APP_NAME", "Moodtune")
# legacy/compat name used by the rest of the module
APP_NAME = AUDIUS_APP_NAME

# ======= Host Caching System =======
_cached_host = None
_cached_ts = 0
_CACHE_TTL = 300  # seconds


def _pick_host():  #pick a Audius server 
    """Get a discovery host from https://api.audius.co (cached for a few minutes)."""

    global _cached_host, _cached_ts
    now = time.time()
    if _cached_host and (now - _cached_ts) < _CACHE_TTL: #If a host is cached and still “fresh,” reuse it immediately
        return _cached_host

    # === FETCH 1: ask https://api.audius.co for healthy discovery hosts ===
    try:
        r = requests.get(AUDIUS_DIRECTORY, timeout=5) # If no response is received within 5 seconds, a requests.exceptions.Timeout exception will be raised
        r.raise_for_status() # throws for 4xx/5xx
        hosts = r.json().get("data", []) # expect list of host urls

    except RequestException as exc:
        # wrap as AudiusError so callers don’t deal with raw requests exceptions.
        raise AudiusError(f"failed to fetch discovery hosts: {exc}") from exc

    _cached_host = random.choice(hosts) if hosts else "https://discoveryprovider.audius.co"
    _cached_ts = now
    return _cached_host

# Private helper transforms raw Audius API response into app's standardized format
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
        #ex: https://discoveryprovider3.audius.co/v1/tracks/abc123/stream?app_name=Moodtune
        "stream_url": f"{host}/v1/tracks/{track['id']}/stream?app_name={APP_NAME}",
    }

# ===== SEARCH FOR TRENDING MUSIC =====
def _http_get_data(url, *, params=None, timeout=8):
    """Small helper to GET and return .json()['data'] with nice errors."""
    try:
        r = requests.get(url, params=params, timeout=timeout)
        r.raise_for_status()
        j = r.json()
        return j.get("data", [])
    except RequestException as exc:
        raise AudiusError(f"audius request failed: {exc}") from exc


# Map feelings to search queries and a coarse genre to help freshness/popularity
_FEELING_QUERIES = {
    "happy":      {"query": "happy upbeat",       "genre": "Electronic"},
    "sad":        {"query": "sad melancholy",     "genre": "Ambient"},
    "calm":       {"query": "calm peaceful chill","genre": "Ambient"},
    "energetic":  {"query": "energetic pump",     "genre": "Electronic"},
    "angry":      {"query": "aggressive hard",    "genre": "Rock"},
    "romantic":   {"query": "love romantic",      "genre": "R&B/Soul"},
    "focused":    {"query": "focus study",        "genre": "Ambient"},
}

def search_by_feeling(feeling, limit=25, sort_method="recent"):
    """
    Search Audius by feeling/mood using keywords + (optional) genre.

    sort_method: 'recent' (newest first), 'popular' (popular-in-search), or 'relevant'.
    Defaults to 'recent' to bias toward fresh results.
    """
    host = _pick_host()
    allowed = {"recent", "popular", "relevant"}  # per API docs
    sort = sort_method if sort_method in allowed else "recent"

    fq = _FEELING_QUERIES.get(feeling, {"query": "chill", "genre": "Electronic"})
    params = [
        ("query", fq["query"]),
        ("genre", fq["genre"]),
        ("sort_method", sort),            
        ("limit", str(limit)),
        ("app_name", APP_NAME),
    ]

    url = f"{host}/v1/tracks/search"
    print("GET", url, params)
    data = _http_get_data(url, params=params)
    return [_normalize(t, host) for t in data]

# ------- Popular (Trending) helpers --------
def _trending_tracks(*, genre=None, time_window="week", limit=25):
    """
    Get popular tracks via Audius 'trending' (most popular) endpoint.
    time_window: 'week' | 'month' | 'year' | 'allTime'
    """
    host = _pick_host()
    url = f"{host}/v1/tracks/trending"
    params = [
        ("time", time_window),
        ("app_name", APP_NAME),
    ]
    if genre:
        params.append(("genre", genre))

    # The API returns up to 100; slice client-side.
    data = _http_get_data(url, params=params)
    data = data[:limit]
    return [_normalize(t, host) for t in data]


def search_popular_by_feeling(feeling, limit=25, time_window="week"):
    """
    Popular-by-feeling: pull trending, optionally nudged by genre that matches the feeling.
    """
    genre = _FEELING_QUERIES.get(feeling, {}).get("genre")
    return _trending_tracks(genre=genre, time_window=time_window, limit=limit)


def search_new_and_popular(feeling, limit=25, time_window="week", recent_first=False):
    """
    Blend newest and popular for a 'fresh but not lame' feed.
    - recent_first=True: prioritize newest, then fill with trending.
    - recent_first=False: prioritize trending, then fill with newest.
    """
    recent = search_by_feeling(feeling, limit=limit, sort_method="recent")
    popular = search_popular_by_feeling(feeling, limit=limit, time_window=time_window)

    # de-dupe by track id while preserving order
    def dedupe(seq):
        seen = set()
        out = []
        for t in seq:
            if t["id"] in seen:
                continue
            seen.add(t["id"])
            out.append(t)
        return out

    primary = recent if recent_first else popular
    secondary = popular if recent_first else recent

    merged = dedupe(primary + secondary)
    return merged[:limit]

        

# resolve a track’s direct stream URL (without issuing a redirect)
def resolve_stream_url(track_id):
    """Resolve the final stream URL for a track (no redirect).

    Returns the data payload from Audius which typically contains the final URL.
    """
    host = _pick_host()
    url = f"{host}/v1/tracks/{track_id}/stream" #Gets the actual streaming URL for a specific track

    # 302-redirect : tells browsers apage has been moved to a different URL, but only for a short time
    try:
        # Calls the stream endpoint with no_redirect=true so the API returns JSON containing the final URL instead of 302-redirecting
        r = requests.get(url, params={"no_redirect": "true", "app_name": APP_NAME}, timeout=8)
        r.raise_for_status()

        return r.json().get("data") # typically the resolved URL string
    except RequestException as exc:
        raise AudiusError(f"failed to resolve stream url: {exc}") from exc

