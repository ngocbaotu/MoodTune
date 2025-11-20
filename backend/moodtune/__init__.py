from flask import Flask, send_from_directory
from .routes.playlist import bp as playlist_bp
import logging
import os
from flask_cors import CORS

def create_app():
    # Basic logging configuration — can be overridden by environment in production.
    log_level = os.getenv("LOG_LEVEL", "INFO").upper()
    logging.basicConfig(level=getattr(logging, log_level, logging.INFO),
                        format="%(asctime)s %(levelname)s %(name)s: %(message)s")

    # Serve a small example player from the frontend folder so the UI and API are same-origin.
    # You can override the frontend directory with the MOODTUNE_FRONTEND_DIR env var for testing.
    frontend_dir = os.getenv(
        "MOODTUNE_FRONTEND_DIR",
        os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "frontend")),
    )

    # Create the Flask app with the frontend dir as its static folder so send_static_file works reliably.
    app = Flask(__name__, static_folder=frontend_dir)
    app.register_blueprint(playlist_bp)
    CORS(app, resources={r"/playlist": {"origins": ["http://localhost:8080"]}, r"/audius/stream/*": {"origins": ["http://localhost:8080"]}})

    @app.get("/player")
    def player():
        """Return the example player.html served from the repo's frontend folder.

        Useful for development to avoid CORS when testing the simple player.
        """
        try:
            return app.send_static_file("player.html")
        except Exception:
            app.logger.exception("failed to serve player.html from %s", frontend_dir)
            return {"error": "player_not_found"}, 404

    @app.get("/ui")
    def ui():
        """Serve a tiny index page that links to the example player and health check."""
        try:
            return app.send_static_file("index.html")
        except Exception:
            app.logger.exception("failed to serve index.html from %s", frontend_dir)
            return {"error": "index_not_found"}, 404

    @app.get("/")
    def index():
        # Simple index so the root path doesn't 404 during quick checks.
        return {
            "ok": True,
            "message": "MoodTune backend",
            "routes": ["/health", "/playlist", "/audius/stream/<track_id>"]
        }

    @app.get("/health")
    def health():
        return {"ok": True}

    return app