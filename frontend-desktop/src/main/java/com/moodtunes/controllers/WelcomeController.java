package com.moodtunes.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import com.moodtunes.utils.SceneManager;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class WelcomeController implements Initializable {

    // window controls
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize if needed
    }

    @FXML
    private void handleStartButton() {
        try {
            SceneManager.switchScene("mood-selection"); //jump to mood selection page
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //window controls: close, minimize, maximize
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