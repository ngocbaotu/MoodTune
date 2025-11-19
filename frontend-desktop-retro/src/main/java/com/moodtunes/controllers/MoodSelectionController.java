package com.moodtunes.controllers;

import javafx.scene.Node;
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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import com.moodtunes.models.Mood;
import com.moodtunes.utils.SceneManager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the mood selection screen
 */
public class MoodSelectionController implements Initializable {

    //window controls
    @FXML
    private Button closeButton;

    @FXML
    private Button minimizeButton;

    @FXML
    private Button maximizeButton;

    private boolean isMaximized = false;
    private double previousWidth;
    private double previousHeight;
    private double previousX;
    private double previousY;

    //mood options
    @FXML
    private GridPane moodGrid;

    @FXML
    private Button generateButton;

    @FXML
    private Button backButton;

    private List<Mood> moods;
    private Mood selectedMood;
    private VBox selectedCard;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeMoods();

        // Only create mood cards if using the old FXML with GridPane
        if (moodGrid != null) {
            createMoodCards();
        }

        // Hide generate button if it exists
        if (generateButton != null) {
            generateButton.setVisible(false);
            generateButton.setManaged(false);
        }
    }

    //create mood options
    private void initializeMoods() {
        moods = new ArrayList<>();

        moods.add(new Mood("Happy", "😊", "Upbeat & Joyful",
                Color.web("#FFD700"), Color.web("#FFA500")));

        moods.add(new Mood("Sad", "😢", "Melancholic & Reflective",
                Color.web("#4169E1"), Color.web("#1E90FF")));

        moods.add(new Mood("Energetic", "⚡", "High-Energy & Pumped",
                Color.web("#FF4500"), Color.web("#FF6347")));

        moods.add(new Mood("Calm", "😌", "Peaceful & Relaxing",
                Color.web("#20B2AA"), Color.web("#48D1CC")));

        moods.add(new Mood("Romantic", "💕", "Love & Romance",
                Color.web("#FF69B4"), Color.web("#FF1493")));

        moods.add(new Mood("Focus", "🎯", "Concentration Mode",
                Color.web("#9370DB"), Color.web("#8A2BE2")));
    }

    //create mood cards
    private void createMoodCards() {
        int row = 0;
        int col = 0;

        for (Mood mood : moods) {
            VBox card = createMoodCard(mood);
            moodGrid.add(card, col, row);

            col++;
            if (col > 1) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createMoodCard(Mood mood) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setPrefWidth(200);
        card.setPrefHeight(200);

        // Apply gradient background
        card.setStyle(mood.getGradientStyle() +
                "-fx-background-radius: 15; " +
                "-fx-border-radius: 15; " +
                "-fx-border-color: white; " +
                "-fx-border-width: 2; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 3, 3);");

        // Emoji label
        Label emojiLabel = new Label(mood.getEmoji());
        emojiLabel.setFont(Font.font(48));
        emojiLabel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.3); " +
                        "-fx-background-radius: 50%; " +
                        "-fx-padding: 15;");

        // Mood name
        Label nameLabel = new Label(mood.getName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setStyle("-fx-letter-spacing: 1px;");

        // Description
        Label descLabel = new Label(mood.getDescription());
        descLabel.setFont(Font.font(10));
        descLabel.setTextFill(Color.rgb(255, 255, 255, 0.9));
        descLabel.setWrapText(true);
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setMaxWidth(180);

        card.getChildren().addAll(emojiLabel, nameLabel, descLabel);

        // Click handler
        card.setOnMouseClicked(event -> handleMoodSelection(mood, card));

        // Hover effect
        card.setOnMouseEntered(event -> {
            card.setStyle(mood.getGradientStyle() +
                    "-fx-background-radius: 15; " +
                    "-fx-border-radius: 15; " +
                    "-fx-border-color: white; " +
                    "-fx-border-width: 2; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 5, 5); " +
                    "-fx-cursor: hand;");
        });

        card.setOnMouseExited(event -> {
            if (card != selectedCard) {
                card.setStyle(mood.getGradientStyle() +
                        "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15; " +
                        "-fx-border-color: white; " +
                        "-fx-border-width: 2; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 3, 3);");
            }
        });

        return card;
    }

    private void handleMoodSelection(Mood mood, VBox card) {
        // Deselect previous card
        if (selectedCard != null) {
            Mood previousMood = selectedMood;
            selectedCard.setStyle(previousMood.getGradientStyle() +
                    "-fx-background-radius: 15; " +
                    "-fx-border-radius: 15; " +
                    "-fx-border-color: white; " +
                    "-fx-border-width: 2; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 3, 3);");
        }

        // Select new card
        selectedMood = mood;
        selectedCard = card;

        card.setStyle(mood.getGradientStyle() +
                "-fx-background-radius: 15; " +
                "-fx-border-radius: 15; " +
                "-fx-border-color: #8B008B; " +
                "-fx-border-width: 4; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 6, 6);");

        // Show generate button
        if (generateButton != null) {
            generateButton.setVisible(true);
            generateButton.setManaged(true);
        }
    }

     /**
     * Handles generate playlist button click or direct mood selection
     * ADD: Modified to use new loadPlaylistScreen() helper method
     * OLD: Used to create FXMLLoader and scene switching code inline
     * NEW: Delegates to centralized loadPlaylistScreen() method
     */
    @FXML
    private void handleGeneratePlaylist(ActionEvent event) {
        if (selectedMood != null) {
            loadPlaylistScreen(event, selectedMood); // ADD: Now uses helper method
        }
    }

    /**
     * Helper method to load playlist screen with proper mood injection
     * ADD: This entire method is NEW - ensures mood is set AFTER FXML loads
     * 
     * CRITICAL CHANGE:
     * OLD APPROACH:
     *   1. Load FXML
     *   2. Pass mood via constructor: new PlaylistController(mood)
     *   3. Switch scene
     *   Problem: Mood was set BEFORE FXML injection
     * 
     * NEW APPROACH:
     *   1. Load FXML (FXML injection happens here)
     *   2. Get controller instance from FXMLLoader
     *   3. Call controller.setMood(mood) AFTER FXML is loaded
     *   4. This triggers loadPlaylistFromBackend() which calls Flask API
     *   5. Switch scene
     *   Result: Backend API call happens at the right time!
     */
    private void loadPlaylistScreen(ActionEvent event, Mood mood) {
        try {
            // ADD: Log which mood is being loaded
            System.out.println("🎵 Loading playlist for mood: " + mood.getName());
            
            // ADD: Load FXML first - this is when @FXML fields get injected
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/playlist.fxml"));
            Parent root = loader.load(); // ADD: FXML injection happens here
            
            // ADD: Get the controller AFTER FXML is loaded
            PlaylistController controller = loader.getController();
            
            // ADD: CRITICAL - Set mood AFTER FXML is loaded
            // This is different from your original code which used constructor
            // This triggers the backend API call in PlaylistController.setMood()
            controller.setMood(mood);

            // ADD: Get current stage from the event source (button that was clicked)
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // ADD: Create new scene with loaded FXML
            Scene scene = new Scene(root, 1280, 720);
            
            // ADD: Switch to playlist scene
            stage.setScene(scene);
            stage.show();
            
            // ADD: Log success
            System.out.println("✅ Playlist screen loaded");
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Error loading playlist screen: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackButton() {
        try {
            SceneManager.switchScene("welcome");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // === Button-based mood selection methods for retro FXML ===
    // These methods are called directly when mood buttons are clicked
    // ADD: All methods below now use loadPlaylistScreen() instead of inline code
    // OLD: Each method had duplicate FXMLLoader code
    // NEW: All methods delegate to centralized loadPlaylistScreen() helper

    /**
     * ADD: Modified to use loadPlaylistScreen() helper
     * OLD CODE: 
     *   selectedMood = happyMood;
     *   handleGeneratePlaylist(event); // This used old inline approach
     * NEW CODE:
     *   loadPlaylistScreen(event, happyMood); // Directly calls helper
     */
    @FXML
    private void selectHappyMood(ActionEvent event) {
        Mood happyMood = moods.stream()
                .filter(m -> m.getName().equals("Happy"))
                .findFirst()
                .orElse(null);
        if (happyMood != null) {
            loadPlaylistScreen(event, happyMood); // ADD: Changed from handleGeneratePlaylist(event)
        }
    }

    /**
     * ADD: Modified to use loadPlaylistScreen() helper
     */
    @FXML
    private void selectSadMood(ActionEvent event) {
        Mood sadMood = moods.stream()
                .filter(m -> m.getName().equals("Sad"))
                .findFirst()
                .orElse(null);
        if (sadMood != null) {
            loadPlaylistScreen(event, sadMood); // ADD: Changed from handleGeneratePlaylist(event)
        }
    }

    /**
     * ADD: Modified to use loadPlaylistScreen() helper
     */
    @FXML
    private void selectEnergeticMood(ActionEvent event) {
        Mood energeticMood = moods.stream()
                .filter(m -> m.getName().equals("Energetic"))
                .findFirst()
                .orElse(null);
        if (energeticMood != null) {
            loadPlaylistScreen(event, energeticMood); // ADD: Changed from handleGeneratePlaylist(event)
        }
    }

    /**
     * ADD: Modified to use loadPlaylistScreen() helper
     */
    @FXML
    private void selectCalmMood(ActionEvent event) {
        Mood calmMood = moods.stream()
                .filter(m -> m.getName().equals("Calm"))
                .findFirst()
                .orElse(null);
        if (calmMood != null) {
            loadPlaylistScreen(event, calmMood); // ADD: Changed from handleGeneratePlaylist(event)
        }
    }

    /**
     * ADD: Modified to use loadPlaylistScreen() helper
     */
    @FXML
    private void selectRomanticMood(ActionEvent event) {
        Mood romanticMood = moods.stream()
                .filter(m -> m.getName().equals("Romantic"))
                .findFirst()
                .orElse(null);
        if (romanticMood != null) {
            loadPlaylistScreen(event, romanticMood); // ADD: Changed from handleGeneratePlaylist(event)
        }
    }

    /**
     * ADD: Modified to use loadPlaylistScreen() helper
     */
    @FXML
    private void selectFocusMood(ActionEvent event) {
        Mood focusMood = moods.stream()
                .filter(m -> m.getName().equals("Focus"))
                .findFirst()
                .orElse(null);
        if (focusMood != null) {
            loadPlaylistScreen(event, focusMood); // ADD: Changed from handleGeneratePlaylist(event)
        }
    }

    // === Window controls ===

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
            // Save current dimensions and position
            previousWidth = stage.getWidth();
            previousHeight = stage.getHeight();
            previousX = stage.getX();
            previousY = stage.getY();

            // Maximize
            stage.setMaximized(true);
            isMaximized = true;
        } else {
            // Restore
            stage.setMaximized(false);
            stage.setWidth(previousWidth);
            stage.setHeight(previousHeight);
            stage.setX(previousX);
            stage.setY(previousY);
            isMaximized = false;
        }
    }
}