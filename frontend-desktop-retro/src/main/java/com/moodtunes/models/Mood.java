package com.moodtunes.models;

import javafx.scene.paint.Color;

/**
 * Represents a mood category with its properties
 */
public class Mood {
    
    private String name;
    private String emoji;
    private String description;
    private Color primaryColor;
    private Color secondaryColor;
    
    public Mood(String name, String emoji, String description, 
                Color primaryColor, Color secondaryColor) {
        this.name = name;
        this.emoji = emoji;
        this.description = description;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }
    
    // Getters
    public String getName() { return name; }
    public String getEmoji() { return emoji; }
    public String getDescription() { return description; }
    public Color getPrimaryColor() { return primaryColor; }
    public Color getSecondaryColor() { return secondaryColor; }
    
    // Get CSS gradient string
    public String getGradientStyle() {
        return String.format(
            "-fx-background-color: linear-gradient(to bottom right, %s, %s);",
            toHexString(primaryColor),
            toHexString(secondaryColor)
        );
    }
    
    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255)
        );
    }
    
    @Override
    public String toString() {
        return name;
    }
}
