package com.moodtunes.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

// Imports for audio playback
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

// Import Gson for JSON parsing
import com.google.gson.*;

import com.moodtunes.models.Mood;
import com.moodtunes.models.Song;
import com.moodtunes.utils.SceneManager;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the playlist screen with BACKEND integration
 * Enhanced with volume control, shuffle/repeat controls in mini player
 */
public class PlaylistController implements Initializable {
    // Configuration for backend API
    private static final String BACKEND_BASE = "https://humble-couscous-p46449xqqrgc9p9q-5000.app.github.dev/";
    private static final int REQUEST_TIMEOUT_SECONDS = 30;

    // JSON parser and HTTP client for backend communication
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
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
    private Slider timeSlider;

    @FXML
    private Label currentTimeLabel;

    @FXML
    private Label totalTimeLabel;

    @FXML
    private Button playPauseButton;

    @FXML
    private Button prevButton;

    @FXML
    private Button nextButton;

    @FXML
    private Button repeatButton;

    @FXML
    private Button shuffleToggleButton;

    @FXML
    private Button closeButton;

    @FXML
    private Button minimizeButton;

    @FXML
    private Button maximizeButton;

    // Search and sort components
    @FXML
    private TextField searchField;

    @FXML
    private Button searchClearButton;

    @FXML
    private Label songCountLabel;

    // ComboBox for sort direction
    @FXML
    private ComboBox<String> sortTitleCombo;

    @FXML
    private ComboBox<String> sortArtistCombo;

    @FXML
    private ComboBox<String> sortAlbumCombo;

    @FXML
    private ComboBox<String> sortDurationCombo;

    @FXML
    private Slider volumeSlider;

    @FXML
    private VBox rootPane;

    // === State ===
    private MediaPlayer mediaPlayer;
    private Mood currentMood;
    private List<Song> playlist = new ArrayList<>();
    private List<Song> filteredPlaylist = new ArrayList<>();
    private Song currentSong;
    private boolean isPlaying = false;
    private int currentSongIndex = -1;
    private boolean isMaximized = false;
    private double previousWidth;
    private double previousHeight;
    private double previousX;
    private double previousY;

    // Mini player states
    private boolean miniPlayerShuffleEnabled = false;
    private boolean repeatEnabled = false;

    // Slider control state - to prevent feedback loop
    private boolean isSliderBeingDragged = false;

    // Volume state
    private double currentVolume = 0.7; // Default 70%

    // === Constructors ===
    public PlaylistController() {
    }

    // === Initialization ===
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Hide mini player initially
        if (miniPlayer != null) {
            miniPlayer.setVisible(false);
            miniPlayer.setManaged(false);
        }

        // Initialize time labels
        if (currentTimeLabel != null) {
            currentTimeLabel.setText("0:00");
        }
        if (totalTimeLabel != null) {
            totalTimeLabel.setText("0:00");
        }

        // Setup search field listener for real-time filtering
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filterPlaylist(newVal.trim());
            });
        }

        // Initialize ComboBox options
        initializeComboBoxes();

        // Setup interactive time slider
        initializeTimeSlider();

        // Initialize volume slider
        initializeVolumeSlider();
    }

    /**
     * Initialize the time slider for interactive music control
     */
    private void initializeTimeSlider() {
        if (timeSlider == null) return;

        timeSlider.setOnMousePressed(event -> {
            isSliderBeingDragged = true;
        });

        timeSlider.setOnMouseReleased(event -> {
            isSliderBeingDragged = false;
            seekToSliderPosition();
        });

        timeSlider.setOnMouseClicked(event -> {
            if (mediaPlayer != null && mediaPlayer.getTotalDuration().greaterThan(Duration.ZERO)) {
                seekToSliderPosition();
            }
        });

        timeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isSliderBeingDragged && currentTimeLabel != null) {
                if (mediaPlayer != null && mediaPlayer.getTotalDuration().greaterThan(Duration.ZERO)) {
                    double totalSeconds = mediaPlayer.getTotalDuration().toSeconds();
                    double seekSeconds = (newVal.doubleValue() / 100.0) * totalSeconds;
                    currentTimeLabel.setText(formatTime(seekSeconds));
                }
            }
        });
    }

    /**
     * Initialize volume slider
     */
    private void initializeVolumeSlider() {
        if (volumeSlider == null) return;

        // Set default value
        volumeSlider.setValue(70);

        // Listen for slider changes
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Update volume when slider moves
            double volume = newVal.doubleValue() / 100.0;
            currentVolume = volume;
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(volume);
            }
        });
    }

    /**
     * Seek to the position indicated by the slider
     */
    private void seekToSliderPosition() {
        if (mediaPlayer == null) return;

        Duration total = mediaPlayer.getTotalDuration();
        if (total == null || total.lessThanOrEqualTo(Duration.ZERO)) return;

        double sliderValue = timeSlider.getValue();
        double percentage = sliderValue / 100.0;
        double seekTime = percentage * total.toSeconds();

        mediaPlayer.seek(Duration.seconds(seekTime));
        System.out.println("⏩ Seeked to: " + formatTime(seekTime));
    }

    /**
     * Initialize all ComboBox dropdowns with sort options
     */
    private void initializeComboBoxes() {
        // Title ComboBox - A-Z or Z-A
        if (sortTitleCombo != null) {
            sortTitleCombo.setItems(FXCollections.observableArrayList("A - Z", "Z - A"));
            sortTitleCombo.setValue("A - Z");
            sortTitleCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    sortByTitle(newVal.equals("A - Z"));
                }
            });
        }

        // Artist ComboBox - A-Z or Z-A
        if (sortArtistCombo != null) {
            sortArtistCombo.setItems(FXCollections.observableArrayList("A - Z", "Z - A"));
            sortArtistCombo.setValue("A - Z");
            sortArtistCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    sortByArtist(newVal.equals("A - Z"));
                }
            });
        }

        // Album ComboBox - A-Z or Z-A
        if (sortAlbumCombo != null) {
            sortAlbumCombo.setItems(FXCollections.observableArrayList("A - Z", "Z - A"));
            sortAlbumCombo.setValue("A - Z");
            sortAlbumCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    sortByAlbum(newVal.equals("A - Z"));
                }
            });
        }

        // Duration ComboBox - Shortest to Longest or Longest to Shortest
        if (sortDurationCombo != null) {
            sortDurationCombo.setItems(FXCollections.observableArrayList(
                    "Shortest - Longest",
                    "Longest - Shortest"
            ));
            sortDurationCombo.setValue("Shortest - Longest");
            sortDurationCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    sortByDuration(newVal.equals("Shortest - Longest"));
                }
            });
        }
    }

    /**
     * Sets the mood and triggers playlist loading from backend
     */
    public void setMood(Mood mood) {
        this.currentMood = mood;

        if (playlistTitle != null && mood != null) {
            playlistTitle.setText(mood.getName() + " Vibes ♪");
        }

        if (songList != null) {
            showLoadingState();
        }

        if (mood != null) {
            loadPlaylistFromBackend(mood.getName());
        }
    }

    // === UI Helper Methods ===

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

    private void loadPlaylistFromBackend(String moodName) {
        new Thread(() -> {
            try {
                String feeling = moodName.trim().toLowerCase();
                System.out.println("📡 Fetching playlist for mood: " + feeling);

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("feeling", feeling);
                requestBody.addProperty("limit", 30);
                requestBody.addProperty("time_window", "week");
                requestBody.addProperty("recent_first", false);

                String jsonBody = gson.toJson(requestBody);
                System.out.println("📤 Request: " + jsonBody);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BACKEND_BASE + "/playlist"))
                        .header("Content-Type", "application/json")
                        .timeout(java.time.Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                System.out.println("📥 Response status: " + response.statusCode());

                if (response.statusCode() == 200) {
                    parseAndDisplayPlaylist(response.body());
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

    private void parseAndDisplayPlaylist(String jsonResponse) {
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonArray tracksArray = root.getAsJsonArray("tracks");

            if (tracksArray == null || tracksArray.size() == 0) {
                Platform.runLater(() -> showErrorState("No tracks found for this mood"));
                return;
            }

            List<Song> fetchedSongs = new ArrayList<>();

            for (JsonElement element : tracksArray) {
                JsonObject track = element.getAsJsonObject();
                String id = getJsonString(track, "id", UUID.randomUUID().toString());
                String title = getJsonString(track, "title", "Unknown Track");
                String artist = extractArtistName(track);
                String duration = formatDuration(track);

                fetchedSongs.add(new Song(id, title, artist, duration));
            }

            Platform.runLater(() -> {
                playlist = fetchedSongs;
                filteredPlaylist = new ArrayList<>(playlist);
                populateSongList();
                updateSongCount();
                System.out.println("✅ Loaded " + playlist.size() + " tracks");
            });

        } catch (Exception e) {
            System.err.println("❌ Parse error: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> showErrorState("Failed to parse playlist data"));
        }
    }

    // === JSON Helper Methods ===

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

    private String getJsonString(JsonObject obj, String key, String defaultValue) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return defaultValue;
    }

    // === Search and Filter Methods ===

    @FXML
    private void filterPlaylist(String query) {
        if (query == null || query.isEmpty()) {
            filteredPlaylist = new ArrayList<>(playlist);
        } else {
            String lowerQuery = query.toLowerCase();
            filteredPlaylist = playlist.stream()
                    .filter(song -> song.getTitle().toLowerCase().contains(lowerQuery) ||
                            song.getArtist().toLowerCase().contains(lowerQuery))
                    .collect(Collectors.toList());
        }

        populateSongList();
        updateSongCount();
    }

    @FXML
    private void handleClearSearch() {
        if (searchField != null) {
            searchField.clear();
        }
        filteredPlaylist = new ArrayList<>(playlist);
        populateSongList();
        updateSongCount();
    }

    private void updateSongCount() {
        if (songCountLabel != null) {
            int count = filteredPlaylist.size();
            String text = count + " SONG" + (count != 1 ? "S" : "");
            songCountLabel.setText(text);
        }
    }

    /**
     * Handle repeat button toggle in mini player
     */
    @FXML
    private void handleRepeat() {
        repeatEnabled = !repeatEnabled;
        System.out.println("🔁 Repeat: " + (repeatEnabled ? "ON" : "OFF"));

        if (repeatButton != null) {
            if (repeatEnabled) {
                // Highlight the button when enabled
                repeatButton.setStyle(
                        "-fx-font-size: 16px; -fx-background-color: #FFD700; " +
                                "-fx-text-fill: #000000; -fx-border-color: #FF69B4; " +
                                "-fx-border-width: 3; -fx-padding: 6 10; " +
                                "-fx-background-radius: 5; -fx-border-radius: 5; " +
                                "-fx-cursor: hand;");
            } else {
                // Normal style when disabled
                repeatButton.setStyle(
                        "-fx-font-size: 16px; -fx-background-color: #FFFFFF; " +
                                "-fx-text-fill: #000000; -fx-border-color: #000000; " +
                                "-fx-border-width: 2; -fx-padding: 6 10; " +
                                "-fx-background-radius: 5; -fx-border-radius: 5; " +
                                "-fx-cursor: hand;");
            }
        }
    }

    /**
     * Handle shuffle button toggle in mini player
     */
    @FXML
    private void handleShuffleToggle() {
        miniPlayerShuffleEnabled = !miniPlayerShuffleEnabled;
        System.out.println("🔀 Mini player shuffle: " + (miniPlayerShuffleEnabled ? "ON" : "OFF"));

        if (shuffleToggleButton != null) {
            if (miniPlayerShuffleEnabled) {
                // Highlight the button when enabled
                shuffleToggleButton.setStyle(
                        "-fx-font-size: 16px; -fx-background-color: #FFD700; " +
                                "-fx-text-fill: #000000; -fx-border-color: #FF69B4; " +
                                "-fx-border-width: 3; -fx-padding: 6 10; " +
                                "-fx-background-radius: 5; -fx-border-radius: 5; " +
                                "-fx-cursor: hand;");
            } else {
                // Normal style when disabled
                shuffleToggleButton.setStyle(
                        "-fx-font-size: 16px; -fx-background-color: #FFFFFF; " +
                                "-fx-text-fill: #000000; -fx-border-color: #000000; " +
                                "-fx-border-width: 2; -fx-padding: 6 10; " +
                                "-fx-background-radius: 5; -fx-border-radius: 5; " +
                                "-fx-cursor: hand;");
            }
        }
    }

    // === Sort Methods ===

    private void sortByTitle(boolean ascending) {
        filteredPlaylist.sort((a, b) -> {
            int comparison = a.getTitle().compareToIgnoreCase(b.getTitle());
            return ascending ? comparison : -comparison;
        });
        populateSongList();
        System.out.println("📊 Sorted by Title: " + (ascending ? "A-Z" : "Z-A"));
    }

    private void sortByArtist(boolean ascending) {
        filteredPlaylist.sort((a, b) -> {
            int comparison = a.getArtist().compareToIgnoreCase(b.getArtist());
            return ascending ? comparison : -comparison;
        });
        populateSongList();
        System.out.println("📊 Sorted by Artist: " + (ascending ? "A-Z" : "Z-A"));
    }

    private void sortByAlbum(boolean ascending) {
        filteredPlaylist.sort((a, b) -> {
            int comparison = a.getArtist().compareToIgnoreCase(b.getArtist());
            return ascending ? comparison : -comparison;
        });
        populateSongList();
        System.out.println("📊 Sorted by Album: " + (ascending ? "A-Z" : "Z-A"));
    }

    private void sortByDuration(boolean shortestFirst) {
        filteredPlaylist.sort((a, b) -> {
            int durationA = durationToSeconds(a.getDuration());
            int durationB = durationToSeconds(b.getDuration());
            int comparison = Integer.compare(durationA, durationB);
            return shortestFirst ? comparison : -comparison;
        });
        populateSongList();
        System.out.println("📊 Sorted by Duration: " + (shortestFirst ? "Shortest-Longest" : "Longest-Shortest"));
    }

    private int durationToSeconds(String duration) {
        try {
            String[] parts = duration.split(":");
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            return minutes * 60 + seconds;
        } catch (Exception e) {
            return 0;
        }
    }

    // === UI Population ===

    private void populateSongList() {
        songList.getChildren().clear();

        if (filteredPlaylist.isEmpty()) {
            if (searchField != null && !searchField.getText().isEmpty()) {
                showErrorState("No songs match your search");
            } else {
                showErrorState("Playlist is empty");
            }
            return;
        }

        for (Song song : filteredPlaylist) {
            HBox songItem = createSongItem(song);
            songList.getChildren().add(songItem);
        }
    }

    private HBox createSongItem(Song song) {
        HBox hbox = new HBox(15);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(15, 20, 15, 20));

        // Set initial style based on whether this is the currently playing song
        if (song == currentSong) {
            hbox.setStyle(
                    "-fx-background-color: linear-gradient(to right, #FFE4E1 0%, #FFD6D1 100%); " +
                            "-fx-background-radius: 10; " +
                            "-fx-border-color: #FF69B4; " +
                            "-fx-border-width: 4; " +
                            "-fx-border-radius: 10; " +
                            "-fx-effect: dropshadow(gaussian, rgba(255,105,180,0.4), 6, 0, 0, 0);");
        } else {
            hbox.setStyle(
                    "-fx-background-color: linear-gradient(to right, #FFFFFF 0%, #FFF8F0 100%); " +
                            "-fx-background-radius: 10; " +
                            "-fx-border-color: #000000; " +
                            "-fx-border-width: 3; " +
                            "-fx-border-radius: 10; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 2, 2);");
        }

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

        hbox.setOnMouseClicked(event -> playSong(song));
        playButton.setOnAction(event -> playSong(song));

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

    private void playSong(Song song) {
        currentSong = song;
        currentSongIndex = filteredPlaylist.indexOf(song);

        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.dispose();
            mediaPlayer = null;
        }

        String streamUrl = BACKEND_BASE + "/audius/stream/" +
                URLEncoder.encode(song.getId(), StandardCharsets.UTF_8);

        System.out.println("🎵 Playing: " + song.getTitle());

        try {
            Media media = new Media(streamUrl);
            mediaPlayer = new MediaPlayer(media);

            // Apply saved volume to new media player
            mediaPlayer.setVolume(currentVolume);

            mediaPlayer.setOnReady(() -> {
                isPlaying = true;
                if (nowPlayingSong != null) nowPlayingSong.setText(song.getTitle());
                if (nowPlayingArtist != null) nowPlayingArtist.setText(song.getArtist());
                if (playPauseButton != null) playPauseButton.setText("⏸");

                if (miniPlayer != null) {
                    miniPlayer.setVisible(true);
                    miniPlayer.setManaged(true);
                }

                Duration total = mediaPlayer.getTotalDuration();
                if (total != null && total.greaterThan(Duration.ZERO)) {
                    if (totalTimeLabel != null) {
                        totalTimeLabel.setText(formatTime(total.toSeconds()));
                    }
                    if (timeSlider != null) {
                        timeSlider.setMax(100);
                    }
                }

                mediaPlayer.play();
                // Refresh the playlist display to highlight current song
                populateSongList();
                System.out.println("▶️ Now playing: " + song.getTitle());
            });

            mediaPlayer.setOnEndOfMedia(this::handleNext);
            mediaPlayer.setOnError(() -> {
                System.err.println("❌ Playback error: " + mediaPlayer.getError());
            });

            if (timeSlider != null) {
                mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                    if (!isSliderBeingDragged) {
                        try {
                            Duration total = mediaPlayer.getTotalDuration();
                            if (total != null && total.greaterThan(Duration.ZERO)) {
                                double progress = (newTime.toSeconds() / total.toSeconds()) * 100.0;
                                timeSlider.setValue(Math.max(0, Math.min(100, progress)));

                                if (currentTimeLabel != null) {
                                    currentTimeLabel.setText(formatTime(newTime.toSeconds()));
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                });
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to play: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Formats seconds to MM:SS format
     */
    private String formatTime(double seconds) {
        int mins = (int) seconds / 60;
        int secs = (int) seconds % 60;
        return String.format("%d:%02d", mins, secs);
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
        if (filteredPlaylist.isEmpty()) return;

        if (currentSongIndex > 0) {
            currentSongIndex--;
        } else {
            currentSongIndex = filteredPlaylist.size() - 1;
        }
        playSong(filteredPlaylist.get(currentSongIndex));
    }

    @FXML
    private void handleNext() {
        if (filteredPlaylist.isEmpty()) return;

        // If repeat is enabled, replay the current song
        if (repeatEnabled && currentSongIndex >= 0) {
            System.out.println("🔁 Repeating current song...");
            playSong(filteredPlaylist.get(currentSongIndex));
            return;
        }

        if (miniPlayerShuffleEnabled) {
            // Random song if shuffle is enabled in mini player
            currentSongIndex = new Random().nextInt(filteredPlaylist.size());
            playSong(filteredPlaylist.get(currentSongIndex));
        } else if (currentSongIndex < filteredPlaylist.size() - 1) {
            currentSongIndex++;
            playSong(filteredPlaylist.get(currentSongIndex));
        } else {
            // At end of playlist
            System.out.println("⏸ End of playlist reached");
        }
    }

    @FXML
    private void handleBackButton() {
        try {
            disposePlayer();
            SceneManager.switchScene("mood-selection");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleVolumeChange() {
        if (mediaPlayer == null) return;

        double volume = volumeSlider.getValue() / 100.0;
        currentVolume = volume;
        mediaPlayer.setVolume(volume);
        System.out.println("🔊 Volume: " + String.format("%.0f%%", volumeSlider.getValue()));
    }

    // === Window Controls ===

    @FXML
    private void handleClose() {
        disposePlayer();
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

    private void disposePlayer() {
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }
}