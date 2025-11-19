package com.moodtunes.controllers;

import javafx.application.Platform; //ADD
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

// ADD: Imports for audio playback
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

// ADD: Import Gson for JSON parsing
import com.google.gson.*;

import com.moodtunes.models.Mood;
import com.moodtunes.models.Song;
import com.moodtunes.utils.SceneManager;

import java.io.IOException;
// ADD: Imports for HTTP client (backend communication)
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*; // FIX: Import List, ArrayList, UUID

/**
 * Controller for the playlist screen with BACKEND integration
 */
public class PlaylistController implements Initializable {
    // ADD: Configuration for backend API
    private static final String BACKEND_BASE = "http://localhost:5000"; //IMPORTANT: use codespace flask URL if run in codespace
    private static final int REQUEST_TIMEOUT_SECONDS = 30;
    
    // ADD: JSON parser and HTTP client for backend communication
    private final Gson gson = new Gson(); //parser
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS)) // FIX: java.time.Duration
            .build();

    // === FXML Components ===
    @FXML
    private Label playlistTitle;

    @FXML
    private VBox songList;

    @FXML
    private HBox miniPlayer;

    @FXML
    private Label nowPlayingSong;

    @FXML
    private Label nowPlayingArtist;

    @FXML
    private ProgressBar progressBar; // ADD: Progress bar for playback

    @FXML
    private Button playPauseButton;

    @FXML
    private Button prevButton;

    @FXML
    private Button nextButton;

    @FXML
    private Button backButton;

    @FXML
    private Button settingsButton;

    @FXML
    private Button closeButton;

    @FXML
    private Button minimizeButton;

    @FXML
    private Button maximizeButton;

    // === State ===
    private MediaPlayer mediaPlayer; // ADD: For actual audio playback
    private Mood currentMood;
    private List<Song> playlist = new ArrayList<>(); // FIX: Initialize the list
    private Song currentSong;
    private boolean isPlaying = false;
    private int currentSongIndex = -1;
    private boolean isMaximized = false;
    private double previousWidth;
    private double previousHeight;
    private double previousX;
    private double previousY;

    // === Constructors ===
    // Default constructor (required for FXML)
    public PlaylistController() {
    }

    // === Initialization ===
    // FIX: Must implement initialize() method from Initializable interface
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Hide mini player initially
        if (miniPlayer != null) {
            miniPlayer.setVisible(false);
            miniPlayer.setManaged(false);
        }

        // Initialize progress bar
        if (progressBar != null) {
            progressBar.setProgress(0);
        }
    }

    /**
     * Sets the mood and triggers playlist loading from backend
     * ADD: This now calls loadPlaylistFromBackend() instead of generatePlaylistForMood()
     */
    public void setMood(Mood mood) {
        this.currentMood = mood;

        // Set playlist title
        if (playlistTitle != null && mood != null) {
            playlistTitle.setText(mood.getName() + " Vibes ♪");
        }

        // ADD: Show loading state while fetching from backend
        if (songList != null) {
            showLoadingState();
        }

        // ADD: fetch playlist from BACKEND
        if (mood != null) {
            loadPlaylistFromBackend(mood.getName());
        }
    }

    // === UI Helper Methods ===
    // ADD: All these methods are NEW for showing different UI states

    private void showLoadingState() {
        songList.getChildren().clear();
        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(50));

        Label loadingLabel = new Label("⏳ Loading playlist...");
        loadingLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 20));
        loadingLabel.setStyle("-fx-text-fill: #000000;");

        Label waitLabel = new Label("Fetching tracks from Audius");
        waitLabel.setFont(Font.font("Courier New", 14));
        waitLabel.setStyle("-fx-text-fill: #666666;");

        loadingBox.getChildren().addAll(loadingLabel, waitLabel);
        songList.getChildren().add(loadingBox);
    }

    // ADD: if failed to fetch -> display UI
    private void showErrorState(String error) {
        songList.getChildren().clear();

        VBox errorBox = new VBox(20);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setPadding(new Insets(50));

        Label errorLabel = new Label("❌ Failed to load playlist");
        errorLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 20));
        errorLabel.setStyle("-fx-text-fill: #FF0000;");

        Label detailLabel = new Label(error);
        detailLabel.setFont(Font.font("Courier New", 12));
        detailLabel.setStyle("-fx-text-fill: #666666;");
        detailLabel.setWrapText(true);
        detailLabel.setMaxWidth(600);

        Button retryButton = new Button("Retry");
        retryButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #FF69B4 0%, #FF1493 100%); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-family: 'Courier New'; " +
                        "-fx-font-weight: bold; " +
                        "-fx-border-color: #000000; " +
                        "-fx-border-width: 3; " +
                        "-fx-padding: 10 20; " +
                        "-fx-cursor: hand;");
        retryButton.setOnAction(e -> {
            if (currentMood != null) {
                setMood(currentMood);
            }
        });

        errorBox.getChildren().addAll(errorLabel, detailLabel, retryButton);
        songList.getChildren().add(errorBox);
    }

    // === Backend Integration ===
    // ADD: ALL CODE BELOW THIS IS NEW - Backend communication

    /**
     * Loads playlist from Python Flask backend
     * ADD: This entire method is NEW - replaces hardcoded generatePlaylistForMood()
     */
    private void loadPlaylistFromBackend(String moodName) {
        // ADD: Use separate thread to avoid freezing UI
        new Thread(() -> {
            try {
                String feeling = moodName.trim().toLowerCase();
                System.out.println("📡 Fetching playlist for mood: " + feeling);

                // ADD: Build JSON request body
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("feeling", feeling);
                requestBody.addProperty("limit", 30);
                requestBody.addProperty("time_window", "week");
                requestBody.addProperty("recent_first", false);

                String jsonBody = gson.toJson(requestBody); //parser
                System.out.println("📤 Request: " + jsonBody);

                // ADD: Create HTTP POST request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BACKEND_BASE + "/playlist"))
                        .header("Content-Type", "application/json")
                        .timeout(java.time.Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS)) // FIX: java.time.Duration
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                // ADD: Send request and wait for response
                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                System.out.println("📥 Response status: " + response.statusCode()); //200, 300,...

                // ADD: Handle response
                if (response.statusCode() == 200) {
                    parseAndDisplayPlaylist(response.body()); //DISPLAY playlist to UI
                } else {
                    String error = "Backend error: " + response.statusCode();
                    System.err.println("❌ " + error);
                    Platform.runLater(() -> showErrorState(error));
                }

            } catch (InterruptedException e) {
                System.err.println("⚠️ Request interrupted");
                Thread.currentThread().interrupt();
                Platform.runLater(() -> showErrorState("Request was interrupted"));
            } catch (Exception e) {
                System.err.println("❌ Error: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showErrorState("Cannot connect to backend. Make sure it's running on " + BACKEND_BASE));
            }
        }, "playlist-fetcher").start();
    }

    /**
     * Parses JSON response and updates UI
     * ADD: This entire method is NEW - parses Audius API response
     */
    private void parseAndDisplayPlaylist(String jsonResponse) {
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonArray tracksArray = root.getAsJsonArray("tracks"); //start from the root

            if (tracksArray == null || tracksArray.size() == 0) {
                Platform.runLater(() -> showErrorState("No tracks found for this mood"));
                return;
            }

            List<Song> fetchedSongs = new ArrayList<>();

            // ADD: Parse each track from JSON
            for (JsonElement element : tracksArray) {
                JsonObject track = element.getAsJsonObject();
                //info
                String id = getJsonString(track, "id", UUID.randomUUID().toString());
                String title = getJsonString(track, "title", "Unknown Track");
                String artist = extractArtistName(track);
                String duration = formatDuration(track);

                fetchedSongs.add(new Song(id, title, artist, duration));
            }

            // ADD: Update UI on JavaFX thread
            Platform.runLater(() -> {
                playlist = fetchedSongs;
                populateSongList();
                System.out.println("✅ Loaded " + playlist.size() + " tracks");
            });

        } catch (Exception e) {
            System.err.println("❌ Parse error: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> showErrorState("Failed to parse playlist data"));
        }
    }

    // === JSON Helper Methods ===
    // ADD: Helper methods for JSON parsing (ALL NEW)

    private String extractArtistName(JsonObject track) {
        if (track.has("artist") && !track.get("artist").isJsonNull()) {
            return track.get("artist").getAsString();
        }

        if (track.has("user") && track.get("user").isJsonObject()) {
            JsonObject user = track.getAsJsonObject("user");
            if (user.has("name") && !user.get("name").isJsonNull()) {
                return user.get("name").getAsString();
            }
            if (user.has("handle") && !user.get("handle").isJsonNull()) {
                return user.get("handle").getAsString();
            }
        }

        return "Unknown Artist";
    }

    // helper: give a song duration like '3:14'
    private String formatDuration(JsonObject track) {
        if (track.has("duration") && !track.get("duration").isJsonNull()) {
            try {
                int seconds = track.get("duration").getAsInt();
                int minutes = seconds / 60;
                int secs = seconds % 60;
                return String.format("%d:%02d", minutes, secs);
            } catch (Exception ignored) {}
        }

        if (track.has("durationText") && !track.get("durationText").isJsonNull()) {
            return track.get("durationText").getAsString();
        }

        return "3:00";
    }

    // helper
    private String getJsonString(JsonObject obj, String key, String defaultValue) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return defaultValue;
    }

    // === UI Population ===

    private void populateSongList() {
        songList.getChildren().clear();

        if (playlist.isEmpty()) {
            showErrorState("Playlist is empty");
            return;
        }

        for (Song song : playlist) {
            HBox songItem = createSongItem(song);
            songList.getChildren().add(songItem);
        }
    }

    private HBox createSongItem(Song song) {
        HBox hbox = new HBox(15);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(15, 20, 15, 20));
        hbox.setStyle(
                "-fx-background-color: linear-gradient(to right, #FFFFFF 0%, #FFF8F0 100%); " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: #000000; " +
                        "-fx-border-width: 3; " +
                        "-fx-border-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 2, 2);");

        // Music icon
        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #FF69B4 0%, #FF1493 100%); " +
                        "-fx-border-color: #000000; " +
                        "-fx-border-width: 3; " +
                        "-fx-padding: 12; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-radius: 8;");

        Label iconLabel = new Label("♪");
        iconLabel.setFont(Font.font(24));
        iconLabel.setStyle("-fx-text-fill: white;");
        iconBox.getChildren().add(iconLabel);

        // Song info
        VBox infoBox = new VBox(8);
        Label titleLabel = new Label(song.getTitle());
        titleLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: #000000;");
        titleLabel.setMaxWidth(700);

        Label artistLabel = new Label(song.getArtist() + " • " + song.getDuration());
        artistLabel.setFont(Font.font("Courier New", 13));
        artistLabel.setStyle("-fx-text-fill: #666666;");

        infoBox.getChildren().addAll(titleLabel, artistLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Play button
        Button playButton = new Button("▶");
        playButton.setFont(Font.font(20));
        playButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #B3FFD9 0%, #8FFFC4 100%); " +
                        "-fx-text-fill: #000000; " +
                        "-fx-border-color: #000000; " +
                        "-fx-border-width: 3; " +
                        "-fx-padding: 10 15; " +
                        "-fx-background-radius: 5; " +
                        "-fx-border-radius: 5; " +
                        "-fx-cursor: hand;");

        hbox.getChildren().addAll(iconBox, infoBox, playButton);

        // Click handlers
        hbox.setOnMouseClicked(event -> playSong(song));
        playButton.setOnAction(event -> playSong(song));

        // Hover effects
        hbox.setOnMouseEntered(event -> {
            hbox.setStyle(
                    "-fx-background-color: linear-gradient(to right, #FFD700 0%, #FFC700 100%); " +
                            "-fx-background-radius: 10; " +
                            "-fx-border-color: #FF69B4; " +
                            "-fx-border-width: 4; " +
                            "-fx-border-radius: 10; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(255,105,180,0.5), 8, 0, 0, 0);");
        });

        hbox.setOnMouseExited(event -> {
            if (song != currentSong) {
                hbox.setStyle(
                        "-fx-background-color: linear-gradient(to right, #FFFFFF 0%, #FFF8F0 100%); " +
                                "-fx-background-radius: 10; " +
                                "-fx-border-color: #000000; " +
                                "-fx-border-width: 3; " +
                                "-fx-border-radius: 10; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 2, 2);");
            } else {
                hbox.setStyle(
                        "-fx-background-color: linear-gradient(to right, #FFE4E1 0%, #FFD6D1 100%); " +
                                "-fx-background-radius: 10; " +
                                "-fx-border-color: #FF69B4; " +
                                "-fx-border-width: 4; " +
                                "-fx-border-radius: 10; " +
                                "-fx-effect: dropshadow(gaussian, rgba(255,105,180,0.4), 6, 0, 0, 0);");
            }
        });

        return hbox;
    }

    // === Playback Controls ===
    // ADD: Modified to stream from backend instead of local files

    private void playSong(Song song) {
        currentSong = song;
        currentSongIndex = playlist.indexOf(song);

        // Stop previous player
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.dispose();
            mediaPlayer = null;
        }

        // ADD: Build stream URL - streams from backend
        String streamUrl = BACKEND_BASE + "/audius/stream/" +
                URLEncoder.encode(song.getId(), StandardCharsets.UTF_8);

        System.out.println("🎵 Playing: " + song.getTitle());

        try {
            // ADD: Create media player with backend stream URL
            Media media = new Media(streamUrl);
            mediaPlayer = new MediaPlayer(media);

            // ADD: Set up event handlers for playback
            mediaPlayer.setOnReady(() -> {
                isPlaying = true;
                if (nowPlayingSong != null) nowPlayingSong.setText(song.getTitle());
                if (nowPlayingArtist != null) nowPlayingArtist.setText(song.getArtist());
                if (playPauseButton != null) playPauseButton.setText("⏸");

                if (miniPlayer != null) {
                    miniPlayer.setVisible(true);
                    miniPlayer.setManaged(true);
                }

                mediaPlayer.play();
                populateSongList(); // Refresh to highlight current song
                System.out.println("▶️ Now playing: " + song.getTitle());
            });

            mediaPlayer.setOnEndOfMedia(this::handleNext);
            mediaPlayer.setOnError(() -> {
                System.err.println("❌ Playback error: " + mediaPlayer.getError());
            });

            // ADD: Update progress bar as song plays
            if (progressBar != null) {
                mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                    try {
                        Duration total = mediaPlayer.getTotalDuration();
                        if (total != null && total.greaterThan(Duration.ZERO)) {
                            double progress = newTime.toMillis() / total.toMillis();
                            progressBar.setProgress(Math.max(0, Math.min(1, progress)));
                        }
                    } catch (Exception ignored) {}
                });
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to play: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePlayPause() {
        if (mediaPlayer == null) return;

        switch (mediaPlayer.getStatus()) {
            case PLAYING -> {
                mediaPlayer.pause();
                isPlaying = false;
                if (playPauseButton != null) playPauseButton.setText("▶");
                System.out.println("⏸ Paused");
            }
            case PAUSED, READY, STOPPED -> {
                mediaPlayer.play();
                isPlaying = true;
                if (playPauseButton != null) playPauseButton.setText("⏸");
                System.out.println("▶️ Playing");
            }
        }
    }

    @FXML
    private void handlePrevious() {
        if (playlist.isEmpty()) return;

        if (currentSongIndex > 0) {
            currentSongIndex--;
        } else {
            currentSongIndex = playlist.size() - 1; // Loop to end
        }
        playSong(playlist.get(currentSongIndex));
    }

    @FXML
    private void handleNext() {
        if (playlist.isEmpty()) return;

        if (currentSongIndex < playlist.size() - 1) {
            currentSongIndex++;
        } else {
            currentSongIndex = 0; // Loop to start
        }
        playSong(playlist.get(currentSongIndex));
    }

    @FXML
    private void handleBackButton() {
        try {
            disposePlayer(); // ADD: Clean up audio resources
            SceneManager.switchScene("mood-selection");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSettingsButton() {
        showSettingsDialog();
    }

    // === Window Controls ===

    @FXML
    private void handleClose() {
        disposePlayer(); // ADD: Clean up audio before closing
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleMinimize() {
        Stage stage = (Stage) minimizeButton.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void handleMaximize() {
        Stage stage = (Stage) maximizeButton.getScene().getWindow();

        if (!isMaximized) {
            previousWidth = stage.getWidth();
            previousHeight = stage.getHeight();
            previousX = stage.getX();
            previousY = stage.getY();
            stage.setMaximized(true);
            isMaximized = true;
        } else {
            stage.setMaximized(false);
            stage.setWidth(previousWidth);
            stage.setHeight(previousHeight);
            stage.setX(previousX);
            stage.setY(previousY);
            isMaximized = false;
        }
    }

    // === Helper Methods ===
    // ADD: This method is NEW - cleans up MediaPlayer resources

    private void disposePlayer() {
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    private void showSettingsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/settings.fxml"));
            Parent root = loader.load();
            SettingsController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Settings");
            dialogStage.initModality(Modality.WINDOW_MODAL);

            if (settingsButton != null && settingsButton.getScene() != null) {
                dialogStage.initOwner(settingsButton.getScene().getWindow());
            }

            dialogStage.setScene(new Scene(root));
            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                System.out.println("✅ Settings saved");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}