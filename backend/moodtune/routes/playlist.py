from flask import Blueprint, request, jsonify, redirect, Response
from ..moods import AUDIUS_MOODS
from ..services import audius
from ..services.audius import AudiusError
import requests  
import time

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

import time

@bp.get("/audius/stream/<track_id>")
def audius_stream(track_id):
    """
    Proxy-streams audio from Audius instead of redirecting.
    
    OLD BEHAVIOR (SLOW):
      1. JavaFX requests stream → Flask
      2. Flask resolves URL from Audius (5-10 sec delay)
      3. Flask redirects JavaFX to Audius URL
      4. JavaFX makes new request to Audius (5-10 sec delay)
      Total: 20-30 seconds
    
    NEW BEHAVIOR (FAST - 2-5 seconds):
      1. JavaFX requests stream → Flask
      2. Flask resolves URL from Audius (5-10 sec) BUT...
      3. Flask immediately starts streaming chunks as they arrive
      4. JavaFX plays audio as chunks come in
      Total: 5-10 seconds (and playback starts even sooner!)
    """
    try:
        # From audius.py
        # Calls audius service to get the actual streaming URL
        url = audius.resolve_stream_url(track_id)
    except AudiusError as e:
        return jsonify({"error": "audius_unavailable", "message": str(e)}), 502

    if not url:
        return jsonify({"error": "Track not streamable"}), 404

    try:
        # stream=True prevents downloading the entire file first
        # timeout=10 fails fast if Audius is slow
        audius_response = requests.get(url, stream=True, timeout=10)
        
        # Check if Audius returned an error
        if audius_response.status_code != 200:
            return jsonify({
                "error": "audius_stream_failed",
                "message": f"Audius returned {audius_response.status_code}"
            }), 502

        # Generator function that yields audio chunks as they arrive
        def generate():
            """
            Streams audio in 8KB chunks.
            JavaFX MediaPlayer starts playing as soon as first chunks arrive!
            """
            for chunk in audius_response.iter_content(chunk_size=8192):
                if chunk:  # Filter out keep-alive chunks
                    yield chunk

        # Return streaming response
        # Content-Type: audio/mpeg tells JavaFX this is audio
        # Cache-Control: no-cache prevents caching issues
        return Response(
            generate(),
            content_type=audius_response.headers.get('Content-Type', 'audio/mpeg'),
            headers={
                'Accept-Ranges': 'bytes',
                'Cache-Control': 'no-cache',
                'Connection': 'keep-alive'
            }
        )

    except requests.Timeout:
        return jsonify({
            "error": "audius_timeout",
            "message": "Audius took too long to respond"
        }), 504
    
    except requests.RequestException as e:
        return jsonify({
            "error": "stream_failed",
            "message": str(e)
        }), 502
