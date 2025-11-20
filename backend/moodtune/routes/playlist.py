from flask import Blueprint, request, jsonify, redirect
from ..moods import AUDIUS_MOODS
from ..services import audius
from ..services.audius import AudiusError

bp = Blueprint("playlist", __name__)

@bp.post("/playlist")
def playlist():
    body = request.get_json(force=True, silent=True) or {}
    feeling = (body.get("feeling") or "calm").strip().lower()
    moods = AUDIUS_MOODS.get(feeling, ["Chill"])
    try:
        tracks = audius.search_by_mood(moods, limit=30) #up to 30 tracks
    except AudiusError as e:
        return jsonify({"error": "audius_unavailable", "message": str(e)}), 502

    return jsonify({"feeling": feeling, "count": min(25, len(tracks)), "tracks": tracks[:25]})

@bp.get("/audius/stream/<track_id>")
def audius_stream(track_id):
    try:
        url = audius.resolve_stream_url(track_id)
    except AudiusError as e:
        return jsonify({"error": "audius_unavailable", "message": str(e)}), 502

    if not url:
        return {"error": "Track not streamable"}, 404

    return redirect(url, code=302)
