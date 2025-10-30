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

Environment
- `AUDIUS_APP_NAME` — optional; a friendly app name passed to Audius (defaults to `Moodtune`).
- `LOG_LEVEL` — logging level (DEBUG/INFO/WARNING/ERROR). Defaults to INFO.

Run locally (development server)

```bash
cd backend
export FLASK_APP=run.py
export FLASK_ENV=development
python3 run.py
```

Endpoints
- GET / -> JSON index
- GET /health -> health check
- POST /playlist -> body {"feeling": "happy"}
- GET /audius/stream/<track_id> -> 302 redirect to Audius stream (or 502 if Audius unavailable)

Notes
- This is a prototype skeleton. For production, run behind a WSGI server (gunicorn/uvicorn), add tests, CI, auth, and consider proxying streams with Range support or using a CDN.
