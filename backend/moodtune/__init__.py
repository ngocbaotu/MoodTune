from flask import Flask #, send_from_directory
from .routes.playlist import bp as playlist_bp
import logging
import os
# from flask_cors import CORS

def create_app():
    app = Flask(__name__)
    app.register_blueprint(playlist_bp)
    
    # Add a simple home route
    @app.route('/')
    def home():
        return {
            "message": "Moodtune API is running!",
            "endpoints": {
                "POST /playlist": "Get mood-based playlist from Audius",
                "GET /audius/stream/<track_id>": "Stream a track"
            }
        }
    
    return app

