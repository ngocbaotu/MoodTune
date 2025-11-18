from flask import Blueprint, request, jsonify, redirect
from ..moods import AUDIUS_MOODS
from ..services import audius
from ..services.audius import AudiusError

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
