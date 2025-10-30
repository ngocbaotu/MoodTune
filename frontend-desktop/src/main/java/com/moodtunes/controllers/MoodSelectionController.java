package com.moodtunes.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
        createMoodCards();
        generateButton.setVisible(false);
        generateButton.setManaged(false);
    }
    
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
        generateButton.setVisible(true);
        generateButton.setManaged(true);
    }
    
    @FXML
    private void handleGeneratePlaylist() {
        if (selectedMood != null) {
            try {
                PlaylistController controller = new PlaylistController(selectedMood);
                SceneManager.switchScene("playlist", controller);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error loading playlist screen: " + e.getMessage());
            }
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
}
