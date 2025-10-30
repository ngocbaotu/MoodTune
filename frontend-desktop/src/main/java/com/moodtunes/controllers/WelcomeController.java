package com.moodtunes.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import com.moodtunes.utils.SceneManager;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the welcome/splash screen
 */
public class WelcomeController implements Initializable {
    
    @FXML
    private VBox welcomeContainer;
    
    @FXML
    private Button startButton;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Add fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), welcomeContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }
    
    @FXML
    private void handleStartButton() {
        try {
            SceneManager.switchScene("mood-selection");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading mood selection screen: " + e.getMessage());
        }
    }
}
