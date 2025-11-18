# MoodTune backend

Minimal instructions to run the backend locally for development.

Prerequisites
- Python 3.10+ (3.11 recommended)
- Optional: virtualenv / venv

Install dependencies

```bash
cd backend
python3 -m venv .venv    # optional
source .venv/bin/activate
pip install -r requirements.txt
```

Run locally (development server)

```bash
cd backend
pip install -r requirements.txt
python3 run.py
```

Check if actually got the playlist
```bash
cd backend
curl -X POST http://localhost:5000/playlist \
  -H "Content-Type: application/json" \
  -d '{"feeling": "happy"}'
```

Endpoints
- GET / -> JSON index
- GET /health -> health check
- POST /playlist -> body {"feeling": "happy"}
- GET /audius/stream/<track_id> -> 302 redirect to Audius stream (or 502 if Audius unavailable)

Notes
- This is a prototype skeleton. For production, run behind a WSGI server (gunicorn/uvicorn), add tests, CI, auth, and consider proxying streams with Range support or using a CDN.
