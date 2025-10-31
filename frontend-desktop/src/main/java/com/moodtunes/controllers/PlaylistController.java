package com.moodtunes.controllers;

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
import com.moodtunes.network.ApiClient;//new
import java.net.URLEncoder;//new
import java.nio.charset.StandardCharsets;//new
import com.google.gson.*;//new
import com.google.gson.reflect.TypeToken;//new
import javafx.application.Platform;//new

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Controller for the playlist screen
 */
public class PlaylistController implements Initializable {

    @FXML
    private Label moodLabel;

    @FXML
    private ListView<Song> songListView;

    @FXML
    private VBox miniPlayer;

    @FXML
    private Label nowPlayingLabel;

    @FXML
    private Label nowPlayingArtist;

    @FXML
    private ProgressBar progressBar;

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

    private Mood currentMood;
    private List<Song> playlist;
    private Song currentSong;
    private boolean isPlaying = false;
    private int currentSongIndex = -1;

    private final Gson gson = new Gson();
    private final ApiClient api = new ApiClient();//from ApiClient.java

    public PlaylistController(Mood mood) {
        this.currentMood = mood;
    }

    //Load the playlist from backend
    public void loadPlaylistForMood(String moodName) {
    new Thread(() -> {
        try {
            //backend wants POST /playlist with {"feeling": "..."}
            String path = "/playlist";
            JsonObject body = new JsonObject();
            body.addProperty("feeling", moodName); // <-- key must be 'feeling'

            var resp = api.post(path, gson.toJson(body));//HTTP response (status + headers + body as a String).

            if (resp.statusCode() == 200) {//check the response 
                String json = resp.body();
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();

                // Expect: { feeling: "...", count: n, tracks: [...] }
                JsonArray tracks = root.getAsJsonArray("tracks");
                List<Song> songsFromApi = new ArrayList<>();

                if (tracks != null) {
                    for (JsonElement el : tracks) {
                        JsonObject t = el.getAsJsonObject();

                        // --- Defensive extraction (Audius fields can vary) ---
                        // Common Audius-like fields:
                        // id / track_id, title, user{name, handle}, duration (sec), artwork, etc.
                        String id =
                            getString(t, "id",
                                getString(t, "track_id", UUID.randomUUID().toString()));

                        String title = getString(t, "title", "Untitled");

                        // artist can be nested like user.name or user.handle
                        String artist =
                            getString(t, "artist",
                                getFromObj(t, "user", "name",
                                    getFromObj(t, "user", "handle", "Unknown Artist")));

                        // duration could be integer seconds; show mm:ss
                        String durationStr = "3:00"; // fallback
                        if (t.has("duration") && !t.get("duration").isJsonNull()) {
                            try {
                                int secs = t.get("duration").getAsInt();
                                int mm = secs / 60;
                                int ss = secs % 60;
                                durationStr = String.format("%d:%02d", mm, ss);
                            } catch (Exception ignore) {}
                        } else if (t.has("durationText")) {
                            durationStr = getString(t, "durationText", durationStr);
                        }

                        //ADD track to playlist 
                        songsFromApi.add(new Song(id, title, artist, durationStr));
                    }
                }

                Platform.runLater(() -> {
                    playlist = songsFromApi;
                    songListView.getItems().setAll(playlist); //Replaces the items in ListView with the freshly fetched songs.
                });
            } else {
                System.err.println("API error: " + resp.statusCode() + " -> " + resp.body());
                fallbackToLocal();//On failure, swaps in your prebuilt, hardcoded playlist for the current mood—also on the FX thread.
            }
        } catch (Exception e) {
            e.printStackTrace();
            fallbackToLocal();
        }
    }, "playlist-loader").start();
}

        // helpers for safe JSON access
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
                playlist = generatePlaylistForMood(currentMood);
                songListView.getItems().setAll(playlist);
            });
        }
        

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        moodLabel.setText(currentMood.getName() + " Vibes 🎵");

        // Generate playlist for mood
        playlist = generatePlaylistForMood(currentMood);

        songListView.getItems().setAll(playlist);

        // Kick off the backend fetch (will overwrite the list on success)
        loadPlaylistForMood(currentMood.getName());

        songListView.setCellFactory(param -> new SongListCell());
        songListView.setOnMouseClicked(event -> {
            Song selectedSong = songListView.getSelectionModel().getSelectedItem();
            if (selectedSong != null) playSong(selectedSong);
        });

        miniPlayer.setVisible(false);
        miniPlayer.setManaged(false);
        progressBar.setProgress(0.6);
    }

    //     // Populate list view with custom cells
    //     for (Song song : playlist) {
    //         songListView.getItems().add(song);
    //     }

    //     // Custom cell factory for song items
    //     songListView.setCellFactory(param -> new SongListCell());

    //     // Handle song selection
    //     songListView.setOnMouseClicked(event -> {
    //         Song selectedSong = songListView.getSelectionModel().getSelectedItem();
    //         if (selectedSong != null) {
    //             playSong(selectedSong);
    //         }
    //     });

    //     // Hide mini player initially
    //     miniPlayer.setVisible(false);
    //     miniPlayer.setManaged(false);

    //     // Set progress bar to 60%
    //     progressBar.setProgress(0.6);
    // }

    // private List<Song> generatePlaylistForMood(Mood mood) {
    //     List<Song> songs = new ArrayList<>();
    //     String moodName = mood.getName();

    //     // Hardcoded playlists for each mood (similar to Flutter version)
    //     switch (moodName) {
    //         case "Happy":
    //             songs.add(new Song("1", "Happy Song Title", "Artist Name", "3:45"));
    //             songs.add(new Song("2", "Upbeat Track", "Artist Name", "4:12"));
    //             songs.add(new Song("3", "Feel Good Music", "Artist Name", "2:58"));
    //             songs.add(new Song("4", "Joyful Melody", "Artist Name", "3:22"));
    //             songs.add(new Song("5", "Sunshine Vibes", "Artist Name", "3:55"));
    //             break;
    //         case "Sad":
    //             songs.add(new Song("1", "Melancholic Tune", "Artist Name", "4:20"));
    //             songs.add(new Song("2", "Rainy Day Blues", "Artist Name", "3:48"));
    //             songs.add(new Song("3", "Emotional Ballad", "Artist Name", "5:10"));
    //             songs.add(new Song("4", "Tears & Dreams", "Artist Name", "4:05"));
    //             songs.add(new Song("5", "Lonely Night", "Artist Name", "3:33"));
    //             break;
    //         case "Energetic":
    //             songs.add(new Song("1", "Workout Pump", "Artist Name", "3:15"));
    //             songs.add(new Song("2", "High Energy Beat", "Artist Name", "2:58"));
    //             songs.add(new Song("3", "Running Mix", "Artist Name", "3:42"));
    //             songs.add(new Song("4", "Power Hour", "Artist Name", "4:01"));
    //             songs.add(new Song("5", "Adrenaline Rush", "Artist Name", "3:28"));
    //             break;
    //         case "Calm":
    //             songs.add(new Song("1", "Peaceful Morning", "Artist Name", "4:55"));
    //             songs.add(new Song("2", "Zen Garden", "Artist Name", "5:30"));
    //             songs.add(new Song("3", "Soft Whispers", "Artist Name", "4:15"));
    //             songs.add(new Song("4", "Meditation Flow", "Artist Name", "6:20"));
    //             songs.add(new Song("5", "Tranquil Waves", "Artist Name", "5:05"));
    //             break;
    //         case "Romantic":
    //             songs.add(new Song("1", "Love Song", "Artist Name", "3:50"));
    //             songs.add(new Song("2", "Heart Beats", "Artist Name", "4:22"));
    //             songs.add(new Song("3", "Sweet Serenade", "Artist Name", "3:35"));
    //             songs.add(new Song("4", "Romantic Nights", "Artist Name", "4:48"));
    //             songs.add(new Song("5", "Forever Together", "Artist Name", "3:58"));
    //             break;
    //         case "Focus":
    //             songs.add(new Song("1", "Study Mode", "Artist Name", "4:10"));
    //             songs.add(new Song("2", "Deep Concentration", "Artist Name", "5:25"));
    //             songs.add(new Song("3", "Brain Power", "Artist Name", "3:48"));
    //             songs.add(new Song("4", "Productivity Flow", "Artist Name", "4:55"));
    //             songs.add(new Song("5", "Focus Zone", "Artist Name", "5:12"));
    //             break;
    //     }

    //     return songs;
    // }

    private void playSong(Song song) {
        currentSong = song;
        currentSongIndex = playlist.indexOf(song);
        isPlaying = true;

        // Update mini player
        nowPlayingLabel.setText(song.getTitle());
        nowPlayingArtist.setText(song.getArtist());
        playPauseButton.setText("⏸");

        // Show mini player
        miniPlayer.setVisible(true);
        miniPlayer.setManaged(true);

        // TODO: Implement actual audio playback
        System.out.println("Now playing: " + song.getTitle());
    }

    @FXML
    private void handlePlayPause() {
        if (currentSong == null) return;

        isPlaying = !isPlaying;
        playPauseButton.setText(isPlaying ? "⏸" : "▶");

        // TODO: Implement actual audio control
        System.out.println(isPlaying ? "Playing" : "Paused");
    }

    @FXML
    private void handlePrevious() {
        if (currentSongIndex > 0) {
            currentSongIndex--;
            playSong(playlist.get(currentSongIndex));
            songListView.getSelectionModel().select(currentSongIndex);
        }
    }

    @FXML
    private void handleNext() {
        if (currentSongIndex < playlist.size() - 1) {
            currentSongIndex++;
            playSong(playlist.get(currentSongIndex));
            songListView.getSelectionModel().select(currentSongIndex);
        }
    }

    @FXML
    private void handleBack() {
        try {
            SceneManager.switchScene("mood-selection");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSettings() {
        try {
            // Load the settings FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/settings.fxml"));
            Parent root = loader.load();

            // Get the controller
            SettingsController controller = loader.getController();

            // Create a new stage for the dialog
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Settings");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(settingsButton.getScene().getWindow());

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);

            // Set the stage in the controller
            controller.setDialogStage(dialogStage);

            // Show dialog and wait
            dialogStage.showAndWait();

            // Check if save was clicked
            if (controller.isSaveClicked()) {
                System.out.println("Settings were saved!");
                // TODO: Apply the new settings
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading settings dialog: " + e.getMessage());
        }
    }

    // Custom ListCell for song items
    private class SongListCell extends javafx.scene.control.ListCell<Song> {
        @Override
        protected void updateItem(Song song, boolean empty) {
            super.updateItem(song, empty);

            if (empty || song == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox hbox = new HBox(15);
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.setPadding(new Insets(10));
                hbox.setStyle(
                        "-fx-background-color: white; " +
                                "-fx-background-radius: 10; " +
                                "-fx-border-color: #E0BBE4; " +
                                "-fx-border-width: 2; " +
                                "-fx-border-radius: 10;");

                // Music icon
                Label iconLabel = new Label("🎵");
                iconLabel.setFont(Font.font(24));
                iconLabel.setStyle(
                        "-fx-background-color: linear-gradient(to bottom right, #FF69B4, #FF1493); " +
                                "-fx-text-fill: white; " +
                                "-fx-padding: 10; " +
                                "-fx-background-radius: 8;");

                // Song info
                VBox infoBox = new VBox(5);
                Label titleLabel = new Label(song.getTitle());
                titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

                Label artistLabel = new Label(song.getArtist() + " • " + song.getDuration());
                artistLabel.setFont(Font.font(12));
                artistLabel.setTextFill(Color.GRAY);

                infoBox.getChildren().addAll(titleLabel, artistLabel);
                HBox.setHgrow(infoBox, Priority.ALWAYS);

                // Play icon
                Label playIcon = new Label("▶");
                playIcon.setFont(Font.font(20));
                playIcon.setTextFill(Color.web("#FF69B4"));

                hbox.getChildren().addAll(iconLabel, infoBox, playIcon);
                setGraphic(hbox);

                // Hover effect
                hbox.setOnMouseEntered(event -> {
                    hbox.setStyle(
                            "-fx-background-color: #FFD700; " +
                                    "-fx-background-radius: 10; " +
                                    "-fx-border-color: #FF69B4; " +
                                    "-fx-border-width: 2; " +
                                    "-fx-border-radius: 10; " +
                                    "-fx-cursor: hand;");
                });

                hbox.setOnMouseExited(event -> {
                    if (song != currentSong) {
                        hbox.setStyle(
                                "-fx-background-color: white; " +
                                        "-fx-background-radius: 10; " +
                                        "-fx-border-color: #E0BBE4; " +
                                        "-fx-border-width: 2; " +
                                        "-fx-border-radius: 10;");
                    } else {
                        hbox.setStyle(
                                "-fx-background-color: rgba(255, 215, 0, 0.3); " +
                                        "-fx-background-radius: 10; " +
                                        "-fx-border-color: #FF69B4; " +
                                        "-fx-border-width: 2; " +
                                        "-fx-border-radius: 10;");
                    }
                });
            }
        }
    }
}