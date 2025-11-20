from flask import Blueprint, request, jsonify, redirect
from ..moods import AUDIUS_MOODS
from ..services import audius
from ..services.audius import AudiusError
import requests  
from ..services.audius import _pick_host, _normalize, APP_NAME

bp = Blueprint("playlist", __name__) #define flask endpoint that expose 2 endpoints: 

@bp.post("/playlist") #Creates a Flask Blueprint (a way to organize routes)
def playlist():

    body = request.get_json(force=True, silent=True) or {} #tries to parse JSON even if Content-Type isn’t application/json
    feeling = (body.get("feeling") or "calm").strip().lower()

    try:
        #from audius.py
        tracks = audius.search_new_and_popular(feeling, limit=30, time_window="week", recent_first=False) #Requests up to 30 tracks from Audius
    except AudiusError as e:
        return jsonify({"error": "audius_unavailable", "message": str(e)}), 502

    return jsonify({"feeling": feeling, "count": min(25, len(tracks)), "tracks": tracks[:25]})#Even if up to 30 are fetched, the response caps at 25 tracks

@bp.get("/audius/stream/<track_id>") #302-redirect the client to Audius’ stream URL for a given track
def audius_stream(track_id):
    try:
        #from audius.py
        url = audius.resolve_stream_url(track_id) #Calls audius service to get the actual streaming URL from Audius for that track
    except AudiusError as e:
        return jsonify({"error": "audius_unavailable", "message": str(e)}), 502

    if not url:
        return {"error": "Track not streamable"}, 404

    # Using it as a resource URL (<audio src="/audius/stream/123">) does not change the page. The media element follows the 302 internally and plays in place
    # “The real audio file is at this other URL — go fetch it directly from Audius.”
    return redirect(url, code=302) #Sends a 302(temporal redicrect) Found redirect to the actual Audius streaming URL so the client fetches audio directly from Audius


# === For Searching Tracks ===
# Can search for songs and artists matching a query string
@bp.post('/search')
def search_songs(): #handle POST requests to this endpoint
    """
    Search for songs on Audius by query string.
    
    Request body:
    {
        "query": "song name or artist",
        "limit": 20  (optional, default 10)
    }
    
    Returns:
    {
        "query": "search term",
        "count": 10,
        "tracks": [...]
    }
    or
    {
        "query": "search term",
        "count": 0,
        "message": "No songs found for 'search term'"
    }
    """

    try :
        data = request.get_json() #parse JSON body of the request (from FRONTEND)

        # Validate input
        if not data or 'query' not in data:
            return jsonify({"error": "Missing 'query' in request body"}), 400

        query = data['query'].strip()
        if not query:
            return jsonify({"error": "Search 'query' cannot be empty"}), 400


        # limit = max how many songs return from the search
        limit = int(data.get('limit', 20))
        if not isinstance(limit, int) or limit < 1 or limit > 100:
           limit = 20  # Default to 10 if invalid limit provided

        # for debugging
        print(f"🔍 Searching for: '{query}' (limit: {limit})")

        # Get a random discovery host
        host = _pick_host()

        # Build search url
        # Audius search endpoint: /v1/tracks/search
        #ex: https://discoveryprovider.audius.co/v1/tracks/search
        search_url = f"{host}/v1/tracks/search"
        params = {
            "query": query,
            "limit": limit * 3, #fetch more to filter later
            "app_name": APP_NAME
        }

        # Make the search request to Adius API
        response = requests.get(search_url, params=params, timeout=10)
        response.raise_for_status() # check the HTTP status code of a response 

        # get the response JSON
        result = response.json()

        # Check if we got the results
        # since audius returns something like { "data": {"artwork": {....
        if 'data' not in result or not result['data']:
            return jsonify({
                "query": query,
                "count": 0,
                "message": f"No songs found for '{query}'"
            }), 200

        # else normalize the songs
        # _normalize: Private helper transforms raw Audius API response into app's standardized format
        tracks = [_normalize(track, host) for track in result['data']]

        # ADD -> to make sure return songs that only match either song title or artist name
        filtered_tracks = [
            track for track in tracks
            if query.lower().replace(" ", "") in track['title'].lower().replace(" ", "") or
                query.lower().replace(" ", "") in track['artist'].lower().replace(" ", "")
        ]

        # After filtering, return only the requested limit
        filtered_tracks = filtered_tracks[:limit]

        if not filtered_tracks:
            return jsonify({
                "query": query,
                "count": 0,
                "message": f"No songs found for '{query}'"
            }), 200

        #for debugging
        print(f"✅ Found {len(filtered_tracks)} results for '{query}'")

        #return the search results
        return jsonify( {
            "query": query,
            "count": len(filtered_tracks),
            "tracks": filtered_tracks
        }), 200
    except requests.exceptions.Timeout:
            print("⏱️ Search request timed out")
            return jsonify({"error": "Search request timed out"}), 504
    except requests.exceptions.RequestException as e:
            print(f"❌ Search request failed (Audius API error): {e}")
            return jsonify({"error": f"Failed to search Audius: {str(e)}"}), 502    
    except Exception as e:
            print(f"❌ An unexpected error occurred during search: {e}")
            import traceback
            traceback.print_exc()
            return jsonify({"error": "Internal server error"}), 500

        