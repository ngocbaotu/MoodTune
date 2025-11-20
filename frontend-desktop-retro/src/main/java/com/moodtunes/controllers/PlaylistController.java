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

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Controller for the playlist screen
 */
public class PlaylistController implements Initializable {

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

    private Mood currentMood;
    private List<Song> playlist;
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

    // Method to set the mood after controller is created
    public void setMood(Mood mood) {
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Hide mini player initially
        if (miniPlayer != null) {
            miniPlayer.setVisible(false);
            miniPlayer.setManaged(false);
        }
    }

    private void populateSongList() {
        songList.getChildren().clear();

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

        // Music icon with retro window style
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

        Label artistLabel = new Label(song.getArtist() + " • " + song.getDuration());
        artistLabel.setFont(Font.font("Courier New", 13));
        artistLabel.setStyle("-fx-text-fill: #666666;");

        infoBox.getChildren().addAll(titleLabel, artistLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Play button (retro style)
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

        // Click handler
        hbox.setOnMouseClicked(event -> playSong(song));
        playButton.setOnAction(event -> playSong(song));

        // Hover effect
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

    private List<Song> generatePlaylistForMood(Mood mood) {
        List<Song> songs = new ArrayList<>();
        String moodName = mood.getName();

        // Hardcoded playlists for each mood
        switch (moodName) {
            case "Happy":
                songs.add(new Song("1", "Happy Song Title", "Artist Name", "3:45"));
                songs.add(new Song("2", "Upbeat Track", "Artist Name", "4:12"));
                songs.add(new Song("3", "Feel Good Music", "Artist Name", "2:58"));
                songs.add(new Song("4", "Joyful Melody", "Artist Name", "3:22"));
                songs.add(new Song("5", "Sunshine Vibes", "Artist Name", "3:55"));
                break;
            case "Sad":
                songs.add(new Song("1", "Melancholic Tune", "Artist Name", "4:20"));
                songs.add(new Song("2", "Rainy Day Blues", "Artist Name", "3:48"));
                songs.add(new Song("3", "Emotional Ballad", "Artist Name", "5:10"));
                songs.add(new Song("4", "Tears & Dreams", "Artist Name", "4:05"));
                songs.add(new Song("5", "Lonely Night", "Artist Name", "3:33"));
                break;
            case "Energetic":
                songs.add(new Song("1", "Workout Pump", "Artist Name", "3:15"));
                songs.add(new Song("2", "High Energy Beat", "Artist Name", "2:58"));
                songs.add(new Song("3", "Running Mix", "Artist Name", "3:42"));
                songs.add(new Song("4", "Power Hour", "Artist Name", "4:01"));
                songs.add(new Song("5", "Adrenaline Rush", "Artist Name", "3:28"));
                break;
            case "Calm":
                songs.add(new Song("1", "Peaceful Morning", "Artist Name", "4:55"));
                songs.add(new Song("2", "Zen Garden", "Artist Name", "5:30"));
                songs.add(new Song("3", "Soft Whispers", "Artist Name", "4:15"));
                songs.add(new Song("4", "Meditation Flow", "Artist Name", "6:20"));
                songs.add(new Song("5", "Tranquil Waves", "Artist Name", "5:05"));
                break;
            case "Romantic":
                songs.add(new Song("1", "Love Song", "Artist Name", "3:50"));
                songs.add(new Song("2", "Heart Beats", "Artist Name", "4:22"));
                songs.add(new Song("3", "Sweet Serenade", "Artist Name", "3:35"));
                songs.add(new Song("4", "Romantic Nights", "Artist Name", "4:48"));
                songs.add(new Song("5", "Forever Together", "Artist Name", "3:58"));
                break;
            case "Focus":
                songs.add(new Song("1", "Study Mode", "Artist Name", "4:10"));
                songs.add(new Song("2", "Deep Concentration", "Artist Name", "5:25"));
                songs.add(new Song("3", "Brain Power", "Artist Name", "3:48"));
                songs.add(new Song("4", "Productivity Flow", "Artist Name", "4:55"));
                songs.add(new Song("5", "Focus Zone", "Artist Name", "5:12"));
                break;
        }

        return songs;
    }

    private void playSong(Song song) {
        currentSong = song;
        currentSongIndex = playlist.indexOf(song);
        isPlaying = true;

        // Update mini player
        if (nowPlayingSong != null) {
            nowPlayingSong.setText(song.getTitle());
        }
        if (nowPlayingArtist != null) {
            nowPlayingArtist.setText(song.getArtist());
        }
        if (playPauseButton != null) {
            playPauseButton.setText("⏸");
        }

        // Show mini player
        if (miniPlayer != null) {
            miniPlayer.setVisible(true);
            miniPlayer.setManaged(true);
        }

        // Update song list styling to highlight current song
        if (songList != null) {
            populateSongList();
        }

        // TODO: Implement actual audio playback
        System.out.println("Now playing: " + song.getTitle());
    }

    @FXML
    private void handlePlayPause() {
        if (currentSong == null) return;

        isPlaying = !isPlaying;
        if (playPauseButton != null) {
            playPauseButton.setText(isPlaying ? "⏸" : "▶");
        }

        // TODO: Implement actual audio control
        System.out.println(isPlaying ? "Playing" : "Paused");
    }

    @FXML
    private void handlePrevious() {
        if (playlist == null || playlist.isEmpty()) return;

        if (currentSongIndex > 0) {
            currentSongIndex--;
            playSong(playlist.get(currentSongIndex));
        } else {
            // Loop to last song
            currentSongIndex = playlist.size() - 1;
            playSong(playlist.get(currentSongIndex));
        }
    }

    @FXML
    private void handleNext() {
        if (playlist == null || playlist.isEmpty()) return;

        if (currentSongIndex < playlist.size() - 1) {
            currentSongIndex++;
            playSong(playlist.get(currentSongIndex));
        } else {
            // Loop back to first song
            currentSongIndex = 0;
            playSong(playlist.get(currentSongIndex));
        }
    }

    @FXML
    private void handleBackButton() {
        try {
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

            if (settingsButton != null && settingsButton.getScene() != null) {
                dialogStage.initOwner(settingsButton.getScene().getWindow());
            }

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);

            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                System.out.println("Settings were saved!");
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading settings dialog: " + e.getMessage());
        }
    }
}