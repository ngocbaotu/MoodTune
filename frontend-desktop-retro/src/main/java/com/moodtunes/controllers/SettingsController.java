package com.moodtunes.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

/**
 * Controller for the settings dialog
 */
public class SettingsController {

    @FXML
    private Slider volumeSlider;

    @FXML
    private Label volumeLabel;

    @FXML
    private CheckBox shuffleCheckbox;

    @FXML
    private CheckBox repeatCheckbox;

    @FXML
    private RadioButton retroThemeRadio;

    @FXML
    private RadioButton darkThemeRadio;

    @FXML
    private RadioButton lightThemeRadio;

    private Stage dialogStage;
    private boolean saveClicked = false;

    @FXML
    private void initialize() {
        // Set up volume slider listener
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            volumeLabel.setText(String.format("%.0f%%", newValue.doubleValue()));
        });

        // Group theme radio buttons
        ToggleGroup themeGroup = new ToggleGroup();
        retroThemeRadio.setToggleGroup(themeGroup);
        darkThemeRadio.setToggleGroup(themeGroup);
        lightThemeRadio.setToggleGroup(themeGroup);

        // Set default values
        volumeSlider.setValue(70);
        shuffleCheckbox.setSelected(false);
        repeatCheckbox.setSelected(false);
        retroThemeRadio.setSelected(true);
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void handleSave() {
        saveClicked = true;

        // Get values
        double volume = volumeSlider.getValue();
        boolean shuffle = shuffleCheckbox.isSelected();
        boolean repeat = repeatCheckbox.isSelected();
        String theme = retroThemeRadio.isSelected() ? "Retro" :
                darkThemeRadio.isSelected() ? "Dark" : "Light";

        // Print settings (for now - later you'd save these)
        System.out.println("Settings saved:");
        System.out.println("  Volume: " + volume + "%");
        System.out.println("  Shuffle: " + shuffle);
        System.out.println("  Repeat: " + repeat);
        System.out.println("  Theme: " + theme);

        // TODO: Save settings to preferences file or database

        dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}