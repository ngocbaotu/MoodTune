# API Documentation

Base URL: `http://127.0.0.1:5000`

## Get Playlist
**POST** `/playlist`

Request:
```json
{
  "feeling": "happy"  // or "calm", "energetic", etc.
}
```

Response:
```json
{
  "feeling": "happy",
  "count": 25,
  "tracks": [...]
}
```

## Stream Track
**GET** `/audius/stream/<track_id>`

Returns: 302 redirect to audio stream