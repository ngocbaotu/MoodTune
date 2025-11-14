package com.moodtunes.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

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

        //Change to avoid load null url
        URL fxmlUrl = SceneManager.class.getResource(fxmlPath);
        if (fxmlUrl == null) {
            throw new IllegalStateException("FXML not found on classpath: " + fxmlPath +
                " (Make sure src/main/resources/views/" + sceneName + ".fxml exists and Resources Root is set)");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl); // <-- pass non-null URL
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

        URL fxmlUrl = SceneManager.class.getResource(fxmlPath);
        if (fxmlUrl == null) {
            throw new IllegalStateException("FXML not found on classpath: " + fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setController(controller); // <-- only valid if FXML has no fx:controller
        Parent root = loader.load();
        
        Scene scene = new Scene(root);
        // String cssPath = SceneManager.class.getResource("/css/styles.css").toExternalForm();
        // scene.getStylesheets().add(cssPath);

        URL cssUrl = SceneManager.class.getResource("/css/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        primaryStage.setScene(scene);
        if (!primaryStage.isShowing()) primaryStage.show();
    
    }
}
