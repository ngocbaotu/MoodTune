package com.moodtunes;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.moodtunes.utils.SceneManager;

/**
 * MoodTunes - Mood-Based Music Recommender
 * JavaFX Desktop Application
 * 
 * Main entry point for the application
 */
public class Main extends Application {
    
    private static Stage primaryStage;
    
    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        
        // Set application title
        primaryStage.setTitle("MoodTunes.exe");
        
        // Set window size
        primaryStage.setWidth(900);
        primaryStage.setHeight(700);
        
        // Don't allow resizing for retro feel
        primaryStage.setResizable(false);
        
        // Initialize SceneManager
        SceneManager.setPrimaryStage(primaryStage);
        
        // Load welcome screen
        try {
            SceneManager.switchScene("welcome");
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading welcome screen: " + e.getMessage());
        }
    }
    
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
