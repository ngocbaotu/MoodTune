package com.moodtunes.controllers;

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
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.moodtunes.models.Mood;
import com.moodtunes.models.Song;
import com.moodtunes.utils.SceneManager;
import com.moodtunes.network.ApiClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class PlaylistController implements Initializable {

    // === config / state ===
    private static final String BACKEND_BASE = "http://localhost:5000"; // change if needed
    private final Gson gson = new Gson();
    private final ApiClient api = new ApiClient();

    private MediaPlayer mediaPlayer;

    @FXML
    private Button closeButton;

    @FXML
    private Button minimizeButton;

    @FXML
    private Button maximizeButton;

    private Mood currentMood;
    private List<Song> playlist = new ArrayList<>();
    private Song currentSong;
    private boolean isPlaying = false;
    private int currentSongIndex = -1;
    private boolean isMaximized = false;
    private double previousWidth;
    private double previousHeight;
    private double previousX;
    private double previousY;

    // Default constructor (required for FXML)
    public PlaylistController() {
    }

    // === FXML ===
    @FXML private Label moodLabel;
    @FXML private ListView<Song> songListView;
    @FXML private VBox miniPlayer;
    @FXML private Label nowPlayingLabel;
    @FXML private Label nowPlayingArtist;
    @FXML private ProgressBar progressBar;
    @FXML private Button playPauseButton;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button backButton;
    @FXML private Button settingsButton;

    // === constructors ===
    public PlaylistController() {
        // required for FXMLLoader
    }

    // If you’re using a controller factory, you may still keep this:
    public PlaylistController(Mood mood) {
        this.currentMood = mood;

        // Set playlist title
        if (playlistTitle != null && mood != null) {
            playlistTitle.setText(mood.getName() + " Vibes ♪");
        }

        // Generate and populate playlist
        if (mood != null) {
            playlist = generatePlaylistForMood(mood);
            if (songList != null) {
                populateSongList();
            }
        }
    }

    public void setMood(Mood mood) { // call this after loading FXML if needed
        this.currentMood = mood;
    }

    // === lifecycle ===
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String moodName = (currentMood != null ? currentMood.getName() : "Calm");
        moodLabel.setText(moodName + " Vibes 🎵");

        // UI wiring
        songListView.setCellFactory(param -> new SongListCell());
        songListView.setOnMouseClicked(event -> {
            Song s = songListView.getSelectionModel().getSelectedItem();
            if (s != null) playSong(s);
        });

        // Initial UI state
        miniPlayer.setVisible(false);
        miniPlayer.setManaged(false);
        progressBar.setProgress(0);

        // Placeholder while loading
        songListView.getItems().setAll(
            Collections.singletonList(new Song("loading", "Loading…", "Please wait", ""))
        );

        // Fetch from backend; on success it overwrites list; on error fallbackToLocal()
        loadPlaylistForMood(moodName);
    }

    // === networking ===
    public void loadPlaylistForMood(String moodName) {
        new Thread(() -> {
            try {
                String path = "/playlist";
                JsonObject body = new JsonObject();
                body.addProperty("feeling",
                    moodName == null ? "calm" : moodName.trim().toLowerCase());

                var resp = api.post(path, gson.toJson(body));
                if (resp.statusCode() == 200) {
                    String json = resp.body();
                    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                    JsonArray tracks = root.getAsJsonArray("tracks");

                    List<Song> songsFromApi = new ArrayList<>();
                    if (tracks != null) {
                        for (JsonElement el : tracks) {
                            JsonObject t = el.getAsJsonObject();

                            String id = getString(t, "id",
                                getString(t, "track_id", UUID.randomUUID().toString()));
                            String title = getString(t, "title", "Untitled");
                            String artist = getString(t, "artist",
                                getFromObj(t, "user", "name",
                                getFromObj(t, "user", "handle", "Unknown Artist")));

                            String durationStr = "3:00";
                            if (t.has("duration") && !t.get("duration").isJsonNull()) {
                                try {
                                    int secs = t.get("duration").getAsInt();
                                    durationStr = String.format("%d:%02d", secs / 60, secs % 60);
                                } catch (Exception ignore) {}
                            } else if (t.has("durationText")) {
                                durationStr = getString(t, "durationText", durationStr);
                            }

                            songsFromApi.add(new Song(id, title, artist, durationStr));
                        }
                    }

                    Platform.runLater(() -> {
                        playlist = songsFromApi;
                        songListView.getItems().setAll(playlist);
                    });
                } else {
                    System.err.println("API error: " + resp.statusCode() + " -> " + resp.body());
                    fallbackToLocal();
                }
            } catch (Exception e) {
                e.printStackTrace();
                fallbackToLocal();
            }
        }, "playlist-loader").start();
    }

    private static String getString(JsonObject o, String key, String def) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : def;
    }

    private static String getFromObj(JsonObject o, String objKey, String innerKey, String def) {
        if (o.has(objKey) && o.get(objKey).isJsonObject()) {
            JsonObject inner = o.getAsJsonObject(objKey);
            if (inner.has(innerKey) && !inner.get(innerKey).isJsonNull()) {
                return inner.get(innerKey).getAsString();
            }
        }
        return def;
    }

    private void fallbackToLocal() {
        Platform.runLater(() -> {
            String moodName = (currentMood != null ? currentMood.getName() : "Calm");
            playlist = generatePlaylistForMood(moodName);
            songListView.getItems().setAll(playlist);
        });
    }

    // === playback ===
    private void playSong(Song song) {
        currentSong = song;
        currentSongIndex = playlist.indexOf(song);

        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignore) {}
            mediaPlayer.dispose();
            mediaPlayer = null;
        }

        String streamUrl = BACKEND_BASE + "/audius/stream/" +
            URLEncoder.encode(song.getId(), StandardCharsets.UTF_8);

        try {
            Media media = new Media(streamUrl);
            mediaPlayer = new MediaPlayer(media);

            mediaPlayer.setOnReady(() -> {
                isPlaying = true;
                nowPlayingLabel.setText(song.getTitle());
                nowPlayingArtist.setText(song.getArtist());
                playPauseButton.setText("⏸");
                miniPlayer.setVisible(true);
                miniPlayer.setManaged(true);
                mediaPlayer.play();
                System.out.println("Now playing: " + song.getTitle());
            });

            mediaPlayer.setOnEndOfMedia(this::handleNext);
            mediaPlayer.setOnError(() ->
                System.err.println("Playback error: " + mediaPlayer.getError()));

            mediaPlayer.currentTimeProperty().addListener((obs, oldT, newT) -> {
                try {
                    Duration total = mediaPlayer.getTotalDuration();
                    if (total != null && total.greaterThan(Duration.ZERO)) {
                        double frac = newT.toMillis() / total.toMillis();
                        progressBar.setProgress(Math.max(0, Math.min(1, frac)));
                    }
                } catch (Exception ignore) {}
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Failed to create media: " + streamUrl);
        }
    }

    @FXML
    private void handlePlayPause() {
        if (mediaPlayer == null) return;
        switch (mediaPlayer.getStatus()) {
            case PLAYING -> {
                mediaPlayer.pause();
                isPlaying = false;
                playPauseButton.setText("▶");
            }
            case PAUSED, READY, STOPPED -> {
                mediaPlayer.play();
                isPlaying = true;
                playPauseButton.setText("⏸");
            }
            default -> {}
        }
    }

    @FXML
    private void handlePrevious() {
        if (playlist == null || playlist.isEmpty()) return;

        if (currentSongIndex > 0) {
            currentSongIndex--;
            songListView.getSelectionModel().select(currentSongIndex);
            playSong(playlist.get(currentSongIndex));
        }
    }

    @FXML
    private void handleNext() {
        if (playlist == null || playlist.isEmpty()) return;
        if (currentSongIndex < playlist.size() - 1) {
            currentSongIndex++;
        } else {
            return; // or loop to 0
        }
        songListView.getSelectionModel().select(currentSongIndex);
        playSong(playlist.get(currentSongIndex));
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
    private void handleSettingsButton() {
        showSettingsDialog();
    }

    @FXML
    private void handleClose() {
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

    private void showSettingsDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/settings.fxml"));
            Parent root = loader.load();
            SettingsController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Settings");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(settingsButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));

            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                System.out.println("Settings were saved!");
                // apply settings if needed
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading settings dialog: " + e.getMessage());
        }
    }

    private void disposePlayer() {
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignore) {}
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    // before: private List<Song> generatePlaylistForMood(Mood mood)
    private List<Song> generatePlaylistForMood(String moodName) {
        if (moodName == null || moodName.isBlank()) moodName = "Calm";
        return List.of(
            new Song("local-1", moodName + " Track 1", "Local Artist", "3:00"),
            new Song("local-2", moodName + " Track 2", "Local Artist", "3:10")
        );
    }


    // === cell ===
    private class SongListCell extends javafx.scene.control.ListCell<Song> {
        @Override protected void updateItem(Song song, boolean empty) {
            super.updateItem(song, empty);
            if (empty || song == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            HBox hbox = new HBox(15);
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.setPadding(new Insets(10));
            hbox.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                          "-fx-border-color: #E0BBE4; -fx-border-width: 2; -fx-border-radius: 10;");

            Label iconLabel = new Label("🎵");
            iconLabel.setFont(Font.font(24));
            iconLabel.setStyle("-fx-background-color: linear-gradient(to bottom right, #FF69B4, #FF1493); " +
                               "-fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 8;");

            VBox infoBox = new VBox(5);
            Label titleLabel = new Label(song.getTitle());
            titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            Label artistLabel = new Label(song.getArtist() + " • " + song.getDuration());
            artistLabel.setFont(Font.font(12));
            artistLabel.setTextFill(Color.GRAY);
            infoBox.getChildren().addAll(titleLabel, artistLabel);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            Label playIcon = new Label("▶");
            playIcon.setFont(Font.font(20));
            playIcon.setTextFill(Color.web("#FF69B4"));

            hbox.getChildren().addAll(iconLabel, infoBox, playIcon);
            setGraphic(hbox);

            hbox.setOnMouseEntered(ev -> hbox.setStyle(
                "-fx-background-color: #FFD700; -fx-background-radius: 10; " +
                "-fx-border-color: #FF69B4; -fx-border-width: 2; -fx-border-radius: 10; -fx-cursor: hand;"));
            hbox.setOnMouseExited(ev -> {
                boolean isCurrent = (song == currentSong);
                hbox.setStyle(isCurrent
                    ? "-fx-background-color: rgba(255,215,0,0.3); -fx-background-radius: 10; " +
                      "-fx-border-color: #FF69B4; -fx-border-width: 2; -fx-border-radius: 10;"
                    : "-fx-background-color: white; -fx-background-radius: 10; " +
                      "-fx-border-color: #E0BBE4; -fx-border-width: 2; -fx-border-radius: 10;");
            });
        }
    }
}
