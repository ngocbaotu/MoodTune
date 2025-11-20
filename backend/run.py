#entry point
import os
from moodtune import create_app
from dotenv import load_dotenv

load_dotenv()  # <-- loads .env at startup
app = create_app()

if __name__ == "__main__":
    app.run(host='127.0.0.1', port=5000, debug=True)