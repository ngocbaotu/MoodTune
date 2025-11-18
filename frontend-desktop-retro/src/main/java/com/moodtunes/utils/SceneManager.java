package com.moodtunes.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Manages scene transitions between different views
 */
public class SceneManager {
    
    private static Stage primaryStage;
    
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }
    
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
    /**
     * Switch to a different scene
     * @param sceneName Name of the FXML file (without .fxml extension)
     */
    public static void switchScene(String sceneName) throws IOException {
        String fxmlPath = "/views/" + sceneName + ".fxml";
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
        Parent root = loader.load();
        
        Scene scene = new Scene(root);
        
        // Add CSS styling
        String cssPath = SceneManager.class.getResource("/css/styles.css").toExternalForm();
        scene.getStylesheets().add(cssPath);
        
        primaryStage.setScene(scene);
    }
    
    /**
     * Switch to a scene with data
     * @param sceneName Name of the FXML file
     * @param controller Controller instance with pre-set data
     */
    public static void switchScene(String sceneName, Object controller) throws IOException {
        String fxmlPath = "/views/" + sceneName + ".fxml";
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
        loader.setController(controller);
        Parent root = loader.load();
        
        Scene scene = new Scene(root);
        String cssPath = SceneManager.class.getResource("/css/styles.css").toExternalForm();
        scene.getStylesheets().add(cssPath);
        
        primaryStage.setScene(scene);
    }
}
